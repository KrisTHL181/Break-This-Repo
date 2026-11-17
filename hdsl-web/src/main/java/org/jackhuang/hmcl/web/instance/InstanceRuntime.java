/*
 * HDSL-web
 * Copyright (C) 2026  HDSL-web contributors
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
package org.jackhuang.hmcl.web.instance;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.jackhuang.hmcl.dsh.DshAccount;
import org.jackhuang.hmcl.dsh.DshException;
import org.jackhuang.hmcl.dsh.DshInstance;
import org.jackhuang.hmcl.dsh.DshInstanceManager;
import org.jackhuang.hmcl.dsh.DshInstanceSettings;
import org.jackhuang.hmcl.dsh.DshLauncher;
import org.jackhuang.hmcl.dsh.DshProcess;
import org.jackhuang.hmcl.dsh.DshProcessManager;
import org.jackhuang.hmcl.dsh.DshVersionManager;
import org.jackhuang.hmcl.dsh.NodeLastGood;
import org.jackhuang.hmcl.setting.SettingsManager;
import org.jackhuang.hmcl.util.logging.LogLine;
import org.jackhuang.hmcl.web.event.EventBus;
import org.jackhuang.hmcl.web.task.TaskService;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/// The live half of the instance lifecycle: launches, stops, and the stream of
/// WebSocket events that turn those into the panel's progress view.
///
/// The domain layer owns the processes ([DshProcessManager]) and the manifests
/// ([DshInstanceManager]); this class is the web server's eyes on them. It
/// wires the two hooks Phase 0 left for exactly this purpose —
/// [DshProcessManager#setCrashHandler] for process endings and
/// [DshProcess#setLogSink] for live output — and translates them into the
/// events of the WebSocket protocol (`instance-state`, `log`, `instance-*`).
///
/// Two facts about the hooks shape the wiring:
///
/// - [DshProcess] supports exactly one log sink and one state listener. The
///   manager occupies the state listener for its registry bookkeeping, so the
///   readiness transition (STARTING → RUNNING) is detected from the log sink
///   instead: the readiness line passes through the sink after
///   [DshProcess#webUrl] has been set, and the same check runs once at attach
///   time to cover a process that became ready in between.
/// - The manager forgets a process the moment it ends, so the process handles
///   of ended instances are remembered here — that is what the logs endpoint
///   and the FAILED state read from.
@NotNullByDefault
public final class InstanceRuntime {

    /// The end of an instance's last process, or of a launch that never got
    /// one: the handle to read logs and exit state from, or the error message
    /// that explains why there is none.
    private record EndedState(@Nullable DshProcess process, @Nullable String launchError) {
    }

    /// How many tail lines a crash report carries.
    private static final int CRASH_TAIL_LINES = 20;

    /// The EventBus topics instance events are published on: the aggregate
    /// `instances` feed plus the per-instance one.
    private static final String INSTANCES_TOPIC = "instances";

    private final EventBus bus;
    private final TaskService tasks;
    /// The panel-wide emergency stop; a launch while it is engaged is refused
    /// rather than started and immediately killed.
    private final org.jackhuang.hmcl.web.emergency.EmergencyStop emergency;
    private final ConcurrentHashMap<String, EndedState> ended = new ConcurrentHashMap<>();
    /// Instances whose launch has been accepted but has no process yet. A launch
    /// runs off the request thread (two probes happen before the spawn), so
    /// between the `202` and the process appearing there is a window in which
    /// [stateOf] had nothing to report and answered STOPPED — a state the
    /// instance had already left, and one that made a poller (or a script) read
    /// "it stopped by itself" a moment before RUNNING.
    private final Set<String> launching = ConcurrentHashMap.newKeySet();
    /// Processes whose READY transition has been announced, so a readiness line
    /// (or the attach-time check) only fires the RUNNING event once.
    private final Set<DshProcess> announcedReady = ConcurrentHashMap.newKeySet();
    /// Processes whose log sink this runtime attached first — the second
    /// attachment (a launch of an already-running instance) must not replay
    /// the buffer, or every line would arrive twice.
    private final Set<DshProcess> attachedSinks = ConcurrentHashMap.newKeySet();

    public InstanceRuntime(EventBus bus, TaskService tasks) {
        this(bus, tasks, new org.jackhuang.hmcl.web.emergency.EmergencyStop());
    }

    /// @param bus       the event bus
    /// @param tasks     the task pool
    /// @param emergency the panel's emergency stop, so a frozen panel cannot be
    ///                  made to start an instance by a queued task
    public InstanceRuntime(EventBus bus, TaskService tasks,
                           org.jackhuang.hmcl.web.emergency.EmergencyStop emergency) {
        this.bus = bus;
        this.tasks = tasks;
        this.emergency = emergency;
    }

    /// The emergency stop this runtime obeys.
    public org.jackhuang.hmcl.web.emergency.EmergencyStop emergency() {
        return emergency;
    }

    /// Every process the panel is currently running, newest first — the
    /// preview the E-Stop confirmation lists before anything is killed.
    public java.util.List<DshProcess> runningProcesses() {
        return DshProcessManager.running();
    }

    /// Kills every instance's process tree, for the emergency stop.
    ///
    /// @return how many processes were signalled, parents and descendants
    public int killAllForcibly() {
        return DshProcessManager.killAllForcibly();
    }

    /// Connects the runtime to the domain layer's event hooks. Call once, at
    /// server assembly time.
    public void wire() {
        DshProcessManager.setCrashHandler(this::onProcessEnded);
    }

    /// Reports what an instance is doing, in the wire spelling of the REST and
    /// WebSocket contracts: NOT_INSTALLED / INSTALLING / STOPPED / STARTING /
    /// RUNNING / STOPPING / FAILED.
    ///
    /// The order of the checks is the precedence of the answers. A live
    /// process outranks an install task, because the launch path installs and
    /// then starts inside one task: while the just-installed process is coming
    /// up the truthful answer is STARTING, not INSTALLING. FAILED is the one
    /// state [DshProcessManager#stateOf] cannot answer — a failed process is
    /// already out of its registry, so it comes from the ending this runtime
    /// remembered. INSTALLING is not a process state either (it is the install
    /// task the runtime was told about), and NOT_INSTALLED is read from the
    /// disk: an instance whose own dsh entry point is missing has nothing to
    /// launch yet. That check comes before FAILED, so a stale ending never
    /// masks "there is nothing here to run".
    ///
    /// @param instanceId the instance id
    /// @return the state, never `null`
    public String stateOf(String instanceId) {
        Optional<DshProcess> running = DshProcessManager.find(instanceId);
        if (running.isPresent()) {
            return running.get().state() == DshProcess.State.STARTING ? "STARTING" : "RUNNING";
        }
        // A launch that has been accepted but has not produced a process yet. The
        // window is real — the task pool runs two probes before the spawn — and
        // answering STOPPED in it is answering about the past.
        if (launching.contains(instanceId)) {
            return "STARTING";
        }
        if (DshProcessManager.isStopping(instanceId)) {
            return "STOPPING";
        }
        if (tasks.hasActiveInstallLike(instanceId)) {
            return "INSTALLING";
        }
        DshInstance instance = DshInstanceManager.find(instanceId);
        if (instance == null || !DshVersionManager.isInstalled(instance)) {
            return "NOT_INSTALLED";
        }
        EndedState last = ended.get(instanceId);
        if (last != null && (last.launchError() != null
                || (last.process() != null && last.process().state() == DshProcess.State.FAILED))) {
            return "FAILED";
        }
        return "STOPPED";
    }

    /// Re-announces an instance's current state on its topics.
    ///
    /// Installs are the reason this exists: their progress travels as `task`
    /// events, so without a nudge the state change (into INSTALLING, and out
    /// of it when the install ends) would only be noticed by the next poll.
    ///
    /// @param instanceId the instance id
    public void announceState(String instanceId) {
        bus.publish(topics(instanceId), statePayload(instanceId, stateOf(instanceId)));
    }

    /// Returns the running process of an instance, if any.
    ///
    /// @param instanceId the instance id
    /// @return the process
    public Optional<DshProcess> runningProcess(String instanceId) {
        return DshProcessManager.find(instanceId);
    }

    /// Launches an instance on the task pool, broadcasting the lifecycle
    /// events as they happen.
    ///
    /// The launch itself is fast except for two probes (Node detection and the
    /// `--no-open` capability check), so it runs off the request thread and the
    /// caller answers 202 right away. A launch that fails before a process
    /// exists is reported as an `instance-state` FAILED event carrying the
    /// error as its crash tail — without that, the panel would wait on a
    /// readiness line that can never come.
    ///
    /// @param instance   the instance to launch
    /// @param publicHost the external host to hand dsh as `--trusted-host`, or
    ///                   `null` to leave the flag out
    public void launchAsync(DshInstance instance, @Nullable String publicHost) {
        String instanceId = instance.id();
        // Marked before the task is queued, not inside it: the `202` has already
        // gone out by the time anything runs, so the mark has to exist by then.
        launching.add(instanceId);
        tasks.execute(() -> {
            try {
                DshProcess process = launchSync(instance, publicHost);
                ended.remove(instanceId);
                wireProcess(instanceId, process);
            } catch (DshException | RuntimeException e) {
                ended.put(instanceId, new EndedState(null, e.getMessage()));
                JsonObject payload = statePayload(instanceId, "FAILED");
                payload.addProperty("crashTail", String.valueOf(e.getMessage()));
                bus.publish(topics(instanceId), payload);
            } finally {
                // Cleared after the process was wired (or the failure recorded):
                // from here on `stateOf` reads the process, which knows better.
                launching.remove(instanceId);
            }
        });
    }

    /// Launches an instance synchronously, injecting `--trusted-host` into the
    /// command when a host was given.
    ///
    /// The plan is built by [DshLauncher#plan], which already applies the
    /// `--no-open` probe-and-cache semantics; the trusted host is appended to
    /// its command list. The account the instance explicitly names (the
    /// `account` field of the REST contract) is handed to the plan, which
    /// writes the supplier route and puts the key in the child's environment;
    /// the key is never written anywhere. Unlike the desktop's
    /// [DshAccount#forInstance] there is deliberately **no global fallback**:
    /// the web contract spells `account: null` as "launch with no account".
    ///
    /// @param instance   the instance
    /// @param publicHost the host, or `null`
    /// @return the running process
    /// @throws DshException when the launch fails
    public DshProcess launchSync(DshInstance instance, @Nullable String publicHost) throws DshException {
        if (emergency.isEngaged()) {
            // The request path is refused by the guard filter, but a queued task
            // (install-then-launch) reaches this method directly.
            throw new DshException("emergency_stop: the panel is in emergency stop; instance starts are frozen");
        }
        DshLauncher.LaunchPlan plan = DshLauncher.plan(instance, accountOf(instance));
        if (publicHost != null && !publicHost.isBlank()) {
            List<String> command = new ArrayList<>(plan.command());
            command.add("--trusted-host");
            command.add(publicHost.trim());
            plan = new DshLauncher.LaunchPlan(plan.instance(), plan.surface(), List.copyOf(command),
                    plan.workingDirectory(), plan.environment(), plan.homeDirectory(), plan.port());
        }
        return DshProcessManager.launch(instance, null, plan);
    }

    /// The account an instance explicitly names, or `null` when it names none
    /// (or names one that is gone — launching with the wrong key is worse than
    /// launching with none).
    ///
    /// The same answer the launch path gives, and for the same reason: the ACP
    /// console of an instance is that instance running, so it works with the
    /// account the instance names and with no other. See
    /// [org.jackhuang.hmcl.web.acp.AcpAccountBridge].
    public static @Nullable DshAccount accountOf(DshInstance instance) {
        String key = DshInstanceSettings.accountKey(instance);
        if (key == null || key.isBlank()) {
            return null;
        }
        for (DshAccount account : SettingsManager.settings().getAccounts()) {
            if (account.matchesKey(key)) {
                return account;
            }
        }
        // 指着一个已经不存在的账户：不拦启动（错 key 比没 key 更糟），但要说一声——
        // 否则表现为“启动成功、harness 里没有供应商”，人只会看到一片空白。
        org.jackhuang.hmcl.util.logging.Logger.LOG.warning("Instance " + instance.id()
                + " names account `" + key + "`, which no longer exists; launching with no account"
                + " (no key and no route will be injected)");
        return null;
    }

    /// Stops an instance on the task pool; the STOPPED event arrives through
    /// the crash handler once the process tree is really gone.
    ///
    /// @param instanceId the instance id
    public void stopAsync(String instanceId) {
        tasks.execute(() -> DshProcessManager.stop(instanceId));
    }

    /// Attaches the log sink that drives the `log` stream and the READY
    /// detection, then announces the current state.
    private void wireProcess(String instanceId, DshProcess process) {
        process.setLogSink(line -> onLogLine(instanceId, process, line));
        // A process that printed its first lines between start and attach —
        // the readiness line among them for a fast starter — never ran them
        // past the sink. Replay the retained window once, on first attach
        // only, so no line is lost and none is doubled.
        if (attachedSinks.add(process)) {
            var window = process.windowLogs();
            for (int i = 0; i < window.size(); i++) {
                onLogLine(instanceId, process, window.get(i).getLog());
            }
        }
        announceIfReady(instanceId, process);
        if (process.state() == DshProcess.State.STARTING) {
            bus.publish(topics(instanceId), statePayload(instanceId, "STARTING"));
        }
    }

    /// The crash handler: publishes the ending of a process, with the exit
    /// code, and the log tail when it ended badly.
    private void onProcessEnded(DshProcess process) {
        String instanceId = process.instance().id();
        boolean failed = process.state() == DshProcess.State.FAILED;
        ended.put(instanceId, new EndedState(process, null));
        announcedReady.remove(process);

        JsonObject payload = statePayload(instanceId, failed ? "FAILED" : "STOPPED");
        process.exitCode().ifPresent(code -> payload.addProperty("exitCode", code));
        if (failed) {
            payload.addProperty("crashTail", tail(process));
        }
        bus.publish(topics(instanceId), payload);
    }

    /// The log sink: every stdout/stderr line becomes a `log` event, and the
    /// readiness line (whose parse has already filled [DshProcess#webUrl] by
    /// the time the sink sees it) flips the state to RUNNING once.
    private void onLogLine(String instanceId, DshProcess process, String line) {
        JsonObject payload = new JsonObject();
        payload.addProperty("type", "log");
        payload.addProperty("instance", instanceId);
        payload.addProperty("level", levelOf(process, line));
        payload.addProperty("line", line);
        bus.publish(List.of(topicOf(instanceId)), payload);

        announceIfReady(instanceId, process);
    }

    /// Publishes RUNNING with the external URL the first time the process is
    /// observed ready.
    private void announceIfReady(String instanceId, DshProcess process) {
        if (process.state() != DshProcess.State.READY || !announcedReady.add(process)) {
            return;
        }
        DshInstance instance = DshInstanceManager.find(instanceId);
        if (instance != null) {
            try {
                NodeLastGood.record(NodeLastGood.dshKey(instanceId),
                        DshLauncher.resolveRuntime(instance).nodeVersion());
            } catch (DshException e) {
                org.jackhuang.hmcl.util.logging.Logger.LOG.warning(
                        "Could not record the last good Node.js for " + instanceId, e);
            }
        }
        JsonObject payload = statePayload(instanceId, "RUNNING");
        process.webUrl().ifPresent(webUrl -> payload.addProperty("url", externalUrl(instanceId, webUrl)));
        bus.publish(topics(instanceId), payload);
    }

    /// The level of a log line: read from the window entry the process wrote
    /// just before invoking the sink, so stdout and stderr stay apart.
    private static String levelOf(DshProcess process, String line) {
        var window = process.windowLogs();
        int size = window.size();
        for (int i = size - 1; i >= 0; i--) {
            LogLine entry = window.get(i);
            if (entry.getLog().equals(line)) {
                return entry.getLevel().name();
            }
        }
        return "INFO";
    }

    private static String tail(DshProcess process) {
        List<String> lines = process.logLines();
        int from = Math.max(0, lines.size() - CRASH_TAIL_LINES);
        return String.join("\n", lines.subList(from, lines.size()));
    }

    private static JsonObject statePayload(String instanceId, String state) {
        JsonObject payload = new JsonObject();
        payload.addProperty("type", "instance-state");
        payload.addProperty("instance", instanceId);
        payload.addProperty("state", state);
        return payload;
    }

    private static List<String> topics(String instanceId) {
        return List.of(INSTANCES_TOPIC, topicOf(instanceId));
    }

    private static String topicOf(String instanceId) {
        return "instance:" + instanceId;
    }

    /// The address the panel opens in a new tab: the instance mount with the
    /// token the readiness line printed. The token is part of the address,
    /// not decoration on it — the fenced releases refuse a request without it.
    ///
    /// @param instanceId the instance id
    /// @param webUrl     the address dsh printed
    /// @return the external path, e.g. `/i/work/?token=…`
    public static String externalUrl(String instanceId, URI webUrl) {
        String query = webUrl.getQuery();
        return "/i/" + instanceId + "/" + (query == null || query.isEmpty() ? "" : "?" + query);
    }

    /// The port an instance is really serving on: the readiness line is the
    /// truth, the manifest only what was asked for.
    ///
    /// @param instance the instance
    /// @return the port
    public int portOf(DshInstance instance) {
        Optional<URI> webUrl = runningProcess(instance.id()).flatMap(DshProcess::webUrl);
        if (webUrl.isPresent()) {
            int port = webUrl.get().getPort();
            if (port > 0) {
                return port;
            }
        }
        return instance.portOrDefault();
    }

    /// The URL to open for a running instance, or empty when it is not running.
    ///
    /// @param instanceId the instance id
    /// @return the external path
    public Optional<String> openUrl(String instanceId) {
        return runningProcess(instanceId)
                .filter(process -> process.state() == DshProcess.State.READY)
                .flatMap(DshProcess::webUrl)
                .map(webUrl -> externalUrl(instanceId, webUrl));
    }

    /// Builds the instance JSON of the REST contract.
    ///
    /// `name` is the id: [DshInstance] has no separate display name, and HDSL's
    /// own interface shows the id as the name. `url` is present only while the
    /// instance is running, and is a path the browser prefixes with its own
    /// origin.
    ///
    /// @param instance the instance
    /// @return the JSON object
    public JsonObject toJson(DshInstance instance) {
        JsonObject json = new JsonObject();
        json.addProperty("id", instance.id());
        json.addProperty("name", instance.id());
        json.addProperty("version", instance.version());
        if (instance.nodeRuntime() == null) {
            json.add("nodeRuntime", com.google.gson.JsonNull.INSTANCE);
        } else {
            json.addProperty("nodeRuntime", instance.nodeRuntime());
        }
        org.jackhuang.hmcl.dsh.NodeSelection.Resolution node = org.jackhuang.hmcl.dsh.NodeSelection.resolve(
                org.jackhuang.hmcl.dsh.NodeSelection.parse(instance.nodeRuntime()),
                org.jackhuang.hmcl.dsh.DshEnvironment.nodeRuntimeDefault(), false);
        com.google.gson.JsonObject nodeJson = new com.google.gson.JsonObject();
        nodeJson.addProperty("version", node.version());
        nodeJson.addProperty("source", node.source().name().toLowerCase(java.util.Locale.ROOT));
        nodeJson.addProperty("compatible", node.compatible());
        nodeJson.addProperty("reason", node.reason());
        json.add("nodeResolved", nodeJson);
        json.addProperty("homeMode", instance.homeMode().name().toLowerCase(java.util.Locale.ROOT));
        json.addProperty("port", portOf(instance));
        json.addProperty("portMode", instance.portModeOrDefault().name().toLowerCase(java.util.Locale.ROOT));
        String state = stateOf(instance.id());
        json.addProperty("state", state);
        // Why it failed, in the same answer that says it failed. The crash tail was only
        // ever published on the instance's event stream, so a list/detail reader saw
        // "FAILED" and nothing else — including for a launch refused before any process
        // started (a half-installed tree, a missing runtime), where there is no process to
        // carry a tail.
        if ("FAILED".equals(state)) {
            EndedState last = ended.get(instance.id());
            String reason = last == null ? null
                    : last.launchError() != null ? last.launchError() : tail(last.process());
            json.add("error", reason == null
                    ? com.google.gson.JsonNull.INSTANCE
                    : new com.google.gson.JsonPrimitive(reason));
        }
        Optional<DshProcess> running = runningProcess(instance.id());
        if (running.isPresent() && running.get().state() == DshProcess.State.READY) {
            running.get().webUrl().ifPresent(webUrl -> json.addProperty("url", externalUrl(instance.id(), webUrl)));
            json.addProperty("uptimeSec", running.get().uptime().getSeconds());
        }
        json.addProperty("icon", instance.iconOrDefault().id());
        String accountKey = DshInstanceSettings.accountKey(instance);
        if (accountKey != null && !accountKey.isBlank()) {
            json.addProperty("account", accountKey);
        } else {
            json.add("account", com.google.gson.JsonNull.INSTANCE);
        }
        String selectedDirectory = SettingsManager.settings().getSelectedGameDirectoryId();
        if (selectedDirectory != null) {
            json.addProperty("selectedGameDirectoryId", selectedDirectory);
        }
        return json;
    }

    /// Builds the `installProgress` object of the instance detail contract.
    ///
    /// @param instanceId the instance id
    /// @return the progress JSON
    public JsonObject installProgress(String instanceId) {
        JsonObject json = new JsonObject();
        Optional<TaskService.TaskInfo> active = tasks.activeInstall(instanceId);
        if (active.isPresent()) {
            json.addProperty("state", "running");
            json.addProperty("message", active.get().message());
            json.addProperty("fraction", active.get().fraction());
            return json;
        }
        Optional<TaskService.TaskInfo> last = tasks.lastInstall(instanceId);
        if (last.isPresent()) {
            json.addProperty("state", last.get().state());
            json.addProperty("message", last.get().message());
            json.addProperty("fraction", last.get().fraction());
            return json;
        }
        json.addProperty("state", "none");
        json.addProperty("message", "");
        json.addProperty("fraction", -1);
        return json;
    }

    /// Builds the log JSON of the logs endpoint: the retained lines of the
    /// running process, or of the last one that ran, newest last.
    ///
    /// A process that never got started still has one line to show: why.
    ///
    /// @param instanceId the instance id
    /// @param maxLines   the tail bound
    /// @return the `lines` array
    public JsonArray logs(String instanceId, int maxLines) {
        JsonArray lines = new JsonArray();
        Optional<DshProcess> running = runningProcess(instanceId);
        DshProcess process = running.orElseGet(() -> {
            EndedState last = ended.get(instanceId);
            return last == null ? null : last.process();
        });
        if (process != null) {
            String now = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
            var window = process.windowLogs();
            int from = Math.max(0, window.size() - maxLines);
            for (int i = from; i < window.size(); i++) {
                LogLine entry = window.get(i);
                JsonObject line = new JsonObject();
                line.addProperty("level", entry.getLevel().name());
                line.addProperty("time", now);
                line.addProperty("text", entry.getLog());
                lines.add(line);
            }
            return lines;
        }
        EndedState last = ended.get(instanceId);
        if (last != null && last.launchError() != null) {
            JsonObject line = new JsonObject();
            line.addProperty("level", "ERROR");
            line.addProperty("time", LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
            line.addProperty("text", "launch failed: " + last.launchError());
            lines.add(line);
        }
        return lines;
    }

    /// Forgets the runtime state of a deleted instance.
    ///
    /// @param instanceId the instance id
    public void forget(String instanceId) {
        ended.remove(instanceId);
    }

    /// Publishes an `instance-created` event with the instance JSON attached.
    ///
    /// @param instance the created instance
    public void announceCreated(DshInstance instance) {
        bus.publish(INSTANCES_TOPIC, instanceEvent("instance-created", instance));
    }

    /// Publishes an `instance-updated` event with the instance JSON attached.
    ///
    /// @param instance the updated instance
    public void announceUpdated(DshInstance instance) {
        bus.publish(INSTANCES_TOPIC, instanceEvent("instance-updated", instance));
    }

    /// Publishes an `instance-deleted` event with the instance JSON attached.
    ///
    /// @param instance the deleted instance
    public void announceDeleted(DshInstance instance) {
        bus.publish(INSTANCES_TOPIC, instanceEvent("instance-deleted", instance));
    }

    private JsonObject instanceEvent(String type, DshInstance instance) {
        JsonObject payload = new JsonObject();
        payload.addProperty("type", type);
        payload.add("instance", toJson(instance));
        return payload;
    }
}
