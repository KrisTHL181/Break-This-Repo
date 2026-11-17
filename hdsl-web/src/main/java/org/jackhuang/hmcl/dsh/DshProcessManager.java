/*
 * HDSL
 * Copyright (C) 2026  HDSL contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package org.jackhuang.hmcl.dsh;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static org.jackhuang.hmcl.util.logging.Logger.LOG;

/// Tracks the DeepSeek Harness processes this launcher started.
///
/// The registry exists so the user interface can show what is currently
/// running, and so the launcher can guarantee it leaves nothing behind: a
/// JVM shutdown hook stops every child, because an abandoned `dsh web` would
/// keep holding its port and its `DSH_HOME`.
@NotNullByDefault
public final class DshProcessManager {
    private DshProcessManager() {
    }

    /// What an instance is doing, as far as starting and stopping it goes.
    ///
    /// One answer for every place that offers to start or stop something: the
    /// home page's launch button, the instance list's rocket, an instance's own
    /// page and the instance picker all show what this says, so no two of them
    /// can disagree about whether an instance is running — which is what decides
    /// whether pressing them starts a server or stops the one already there.
    public enum LaunchState {
        /// Nothing is running and nothing is on its way up.
        STOPPED,
        /// Started, still coming up: the browser surface has not reported yet.
        STARTING,
        /// Up and serving.
        RUNNING,
        /// Asked to stop, still draining.
        STOPPING
    }

    /// Running processes, keyed by instance id.
    private static final Map<String, DshProcess> RUNNING = new ConcurrentHashMap<>();

    /// The instances the launcher has asked to stop and which have not finished
    /// stopping.
    ///
    /// An instance is not startable again until it is out of this set: DeepSeek
    /// Harness holds its port and its home until it has exited, so starting a
    /// second one in the meantime is what let a stopped instance come back on a
    /// different port, and let two servers race one home.
    private static final java.util.Set<String> STOPPING = ConcurrentHashMap.newKeySet();

    /// Serialises the check-then-start sequence in [#launch].
    ///
    /// Without it, two quick activations of the launch button both observe an
    /// empty registry and start a second server on the same home, which races
    /// the profile files and leaves the extra process untracked.
    private static final Object LAUNCH_LOCK = new Object();

    /// Receives the process log tail when a process that had been running ends.
    ///
    /// The desktop launcher popped a crash dialog here; the web server has no
    /// dialog, so the ending is published as an event instead, with the whole
    /// process handle attached — the tail of its log, the plan it ran from and
    /// its exit state are all on the handle, which is what a WebSocket client
    /// needs to render the same facts.
    @FunctionalInterface
    public interface CrashHandler {
        /// Called on the thread that noticed the end, once per ending.
        ///
        /// @param process the process that ended
        void onCrash(DshProcess process);
    }

    /// Who hears about crashed processes, or `null` when nobody has registered.
    private static volatile @Nullable CrashHandler crashHandler;

    /// Registers who hears about crashed processes.
    ///
    /// @param handler the handler, or `null` to unregister
    public static void setCrashHandler(@Nullable CrashHandler handler) {
        crashHandler = handler;
    }

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(DshProcessManager::stopAll, "HDSL shutdown"));
    }

    /// Launches an instance, or returns the one already running for it.
    ///
    /// One process per instance is enforced here: DeepSeek Harness has no
    /// single-instance lock of its own, and a second `dsh web` on the same home
    /// would race the profile files.
    ///
    /// @param instance the instance to launch
    /// @return the running handle
    /// @throws DshException when the instance is still stopping, the runtime is
    ///                       missing, or the process cannot start
    public static DshProcess launch(DshInstance instance) throws DshException {
        return launch(instance, null);
    }

    /// Launches an instance with an account.
    ///
    /// @param instance the instance
    /// @param account  the account to hand the harness, or `null` for none
    /// @return the process
    /// @throws DshException when it cannot be started
    public static DshProcess launch(DshInstance instance, @Nullable DshAccount account)
            throws DshException {
        return launch(instance, account, null);
    }

    /// Launches an instance from a plan that has already been built.
    ///
    /// Building a plan is not free of consequence: it writes the account's route into the profile's
    /// own patch layer and asks the supplier for its models. A caller that built one — to print what
    /// was about to run, which is what the command line interface does — hands that same plan over
    /// rather than paying for a second one.
    ///
    /// @param instance the instance
    /// @param account  the account, or `null` for none
    /// @param prepared the plan to use, or `null` to build one
    /// @return the process
    /// @throws DshException when it cannot be started
    public static DshProcess launch(DshInstance instance, @Nullable DshAccount account,
                                    @Nullable DshLauncher.LaunchPlan prepared) throws DshException {
        synchronized (LAUNCH_LOCK) {
            DshProcess existing = RUNNING.get(instance.id());
            if (existing != null) {
                // An instance that is still draining must not be started again:
                // it holds its port and its home until it is gone, so a second
                // server would come up on another port, or race the first one
                // through the profile files.
                if (STOPPING.contains(instance.id())) {
                    throw new DshException("Instance " + instance.id()
                            + " is still stopping; wait for it to exit before starting it again");
                }
                if (existing.isRunning()) {
                    return existing;
                }
                // It exited without the registry having noticed yet.
                RUNNING.remove(instance.id(), existing);
            }

            // The runtime is resolved inside the launcher, so an instance pinned
            // to a managed Node runtime is honoured here too.
            DshProcess process = prepared == null
                    ? DshProcess.start(instance, account)
                    : DshProcess.startPrepared(instance, prepared);
            RUNNING.put(instance.id(), process);
            process.setStateListener(state -> {
                if (state == DshProcess.State.STOPPED || state == DshProcess.State.FAILED) {
                    RUNNING.remove(instance.id(), process);
                    // Whatever this launch put into the home's own settings goes back to what it
                    // was, so the next launch — of any kind, with an account or with none — starts
                    // on the person's own configuration rather than on this launch's leftovers.
                    try {
                        DshInjectedSettings.settle(process.plan().instance());
                    } catch (DshException e) {
                        org.jackhuang.hmcl.util.logging.Logger.LOG.warning(
                                "Could not put back what the launch of " + process.plan().instance().id()
                                        + " left in its settings", e);
                    }
                    CrashHandler handler = crashHandler;
                    if (handler != null) {
                        handler.onCrash(process);
                    }
                }
            });
            return process;
        }
    }

    /// Reports what an instance is doing.
    ///
    /// @param instanceId the instance id
    /// @return the state, never `null`
    public static LaunchState stateOf(String instanceId) {
        if (STOPPING.contains(instanceId)) {
            return LaunchState.STOPPING;
        }
        DshProcess process = RUNNING.get(instanceId);
        if (process == null || !process.isRunning()) {
            return LaunchState.STOPPED;
        }
        return process.state() == DshProcess.State.STARTING ? LaunchState.STARTING : LaunchState.RUNNING;
    }

    /// Returns the running process for an instance.
    ///
    /// @param instanceId the instance id
    /// @return the process, or empty when the instance is not running
    public static Optional<DshProcess> find(String instanceId) {
        DshProcess process = RUNNING.get(instanceId);
        return process == null || !process.isRunning() ? Optional.empty() : Optional.of(process);
    }

    /// Lists every instance that is currently running.
    ///
    /// @return the running handles, newest first
    public static List<DshProcess> running() {
        List<DshProcess> processes = new ArrayList<>();
        for (DshProcess process : RUNNING.values()) {
            if (process.isRunning()) {
                processes.add(process);
            }
        }
        processes.sort((a, b) -> b.uptime().compareTo(a.uptime()));
        return processes;
    }

    /// Stops the process for an instance, if any.
    ///
    /// The instance stays in the registry for as long as it takes the child to
    /// exit, and is only then forgotten. Dropping it first would leave a window
    /// in which the launcher believed nothing was running while a server was
    /// still holding its port and its home — which is exactly the window a quick
    /// stop-then-start used to slip through.
    ///
    /// @param instanceId the instance id
    /// @return whether a running process was stopped
    public static boolean stop(String instanceId) {
        // M14 (glm-5.2): set STOPPING *before* reading RUNNING so a concurrent
        // launch() that holds LAUNCH_LOCK sees the flag and refuses — the
        // original race was between RUNNING.get() and STOPPING.add(). stop()
        // itself does not hold LAUNCH_LOCK: process.stop() blocks for seconds,
        // and holding the lock that long would starve every launch.
        STOPPING.add(instanceId);
        DshProcess process = RUNNING.get(instanceId);
        if (process == null) {
            STOPPING.remove(instanceId);
            return false;
        }
        try {
            process.stop();
        } finally {
            STOPPING.remove(instanceId);
            RUNNING.remove(instanceId, process);
        }
        return true;
    }

    /// Reports whether an instance has been asked to stop and has not finished.
    ///
    /// @param instanceId the instance id
    /// @return whether it is stopping
    public static boolean isStopping(String instanceId) {
        return STOPPING.contains(instanceId);
    }

    /// Stops every running process.
    ///
    /// Called from the JVM shutdown hook; also safe to call directly.
    /// The live dsh instance roots as the panel's caller identification sees
    /// them ([org.jackhuang.hmcl.web.auth.PeerIdentity]).
    ///
    /// @return `pid → instance id`, never `null`
    public static Map<Long, String> liveRootPids() {
        Map<Long, String> roots = new java.util.HashMap<>();
        for (Map.Entry<String, DshProcess> entry : RUNNING.entrySet()) {
            if (!entry.getValue().isRunning()) {
                continue;
            }
            long pid = entry.getValue().pid();
            if (pid > 0) {
                roots.put(pid, entry.getKey());
            }
        }
        return roots;
    }

    public static void stopAll() {        for (DshProcess process : List.copyOf(RUNNING.values())) {
            try {
                process.stop();
            } catch (RuntimeException e) {
                LOG.warning("Failed to stop " + process.instance().id(), e);
            }
        }
        RUNNING.clear();
    }

    /// Kills every running process tree at once, for the emergency stop.
    ///
    /// Each process is removed from the registry as it is killed, so a later
    /// start sees "nothing running" rather than a corpse. Unlike [stopAll] this
    /// is the brake: no drain window, no `post-exit` command.
    ///
    /// @return how many processes were signalled, parent and descendants
    public static int killAllForcibly() {
        int killed = 0;
        for (Map.Entry<String, DshProcess> entry : List.copyOf(RUNNING.entrySet())) {
            DshProcess process = entry.getValue();
            try {
                killed += process.killNow();
            } catch (RuntimeException e) {
                LOG.warning("Failed to kill " + process.instance().id(), e);
            } finally {
                RUNNING.remove(entry.getKey(), process);
            }
        }
        return killed;
    }

    /// Returns the process for an instance without checking liveness.
    ///
    /// @param instanceId the instance id
    /// @return the handle, or `null`
    static @Nullable DshProcess raw(String instanceId) {
        return RUNNING.get(instanceId);
    }
}
