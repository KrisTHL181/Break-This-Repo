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

import org.jackhuang.hmcl.util.platform.Executables;
import org.jackhuang.hmcl.util.platform.ManagedProcess;
import org.jackhuang.hmcl.util.platform.OperatingSystem;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/// Runs a short-lived external command and captures its output.
///
/// Used for the npm, node and pnpm invocations that back version management.
/// Long-running processes — booting a profile — use [DshProcess] instead.
@NotNullByDefault
public final class DshCommand {
    /// The programs this launcher has started and is waiting on, mapped to the
    /// worker thread that started them. Ownership by thread is what makes a
    /// cancellation scoped: [TaskService] can stop only the tree its task is
    /// running, instead of every install on the panel.
    private static final java.util.Map<Process, Thread> RUNNING =
            new java.util.concurrent.ConcurrentHashMap<>();

    private DshCommand() {
    }

    /// The result of a finished command.
    ///
    /// @param exitCode the process exit code
    /// @param output   every stdout and stderr line, in arrival order
    /// @param timedOut whether the command was stopped for outliving its deadline
    public record Result(int exitCode, @org.jetbrains.annotations.Unmodifiable List<String> output,
                         boolean timedOut) {

        /// A result of a command that was left to run to its end.
        ///
        /// @param exitCode the process exit code
        /// @param output   every stdout and stderr line, in arrival order
        public Result(int exitCode, @org.jetbrains.annotations.Unmodifiable List<String> output) {
            this(exitCode, output, false);
        }

        /// Returns whether the command exited successfully.
        ///
        /// @return whether the exit code is zero and the command was not stopped for outliving its
        ///         deadline
        public boolean isSuccess() {
            return exitCode == 0 && !timedOut;
        }

        /// Returns the captured output joined by newlines.
        ///
        /// @return the trimmed output text
        public String text() {
            return String.join("\n", output).trim();
        }
    }

    /// Runs a command, waiting for it to finish.
    ///
    /// @param command the program and its arguments
    /// @return the command result
    /// @throws IOException          when the process cannot be started
    /// @throws InterruptedException when the calling thread is interrupted while waiting
    public static Result run(List<String> command) throws IOException, InterruptedException {
        return run(command, null, Map.of(), null);
    }

    /// Runs a command in a directory, waiting for it to finish.
    ///
    /// @param command   the program and its arguments
    /// @param directory the working directory, or `null` for the current one
    /// @param onLine    a consumer notified of every output line, or `null`
    /// @return the command result
    /// @throws IOException          when the process cannot be started
    /// @throws InterruptedException when the calling thread is interrupted while waiting
    public static Result run(List<String> command, @Nullable Path directory, @Nullable Consumer<String> onLine)
            throws IOException, InterruptedException {
        return run(command, directory, Map.of(), onLine);
    }

    /// Runs a command with extra environment variables, waiting for it to finish.
    ///
    /// The variables are added to the inherited environment. Passing them here
    /// rather than relying on the caller's own `ProcessBuilder` matters for
    /// anything that must be scoped to a directory of ours: `dsh` reads
    /// `DSH_HOME` from its environment, and without it the command would quietly
    /// operate on the user's real `~/.dsh`.
    ///
    /// @param command     the program and its arguments
    /// @param directory   the working directory, or `null` for the current one
    /// @param environment extra environment variables
    /// @param onLine      a consumer notified of every output line, or `null`
    /// @return the command result
    /// @throws IOException          when the process cannot be started
    /// @throws InterruptedException when the calling thread is interrupted while waiting
    public static Result run(List<String> command, @Nullable Path directory,
                             Map<String, String> environment, @Nullable Consumer<String> onLine)
            throws IOException, InterruptedException {
        return run(command, null, directory, environment, onLine);
    }

    /// Runs a command for at most a deadline, and stops it when it outlives it.
    ///
    /// A command that answers is what most of these are, and one that does not answer has no bound
    /// of its own: a program waiting for something that is not coming holds the thread that asked
    /// for it for as long as it likes. A deadline is what turns that into an answer — a result that
    /// says it ran out of time, rather than a call that never comes back.
    ///
    /// @param command     the program and its arguments
    /// @param timeout     how long to wait, or `null` to wait for as long as it takes
    /// @param directory   the working directory, or `null` for the current one
    /// @param environment extra environment variables
    /// @param onLine      a consumer notified of every output line, or `null`
    /// @return the command result, with `timedOut` set when the deadline stopped it
    /// @throws IOException          when the process cannot be started
    /// @throws InterruptedException when the calling thread is interrupted while waiting
    public static Result run(List<String> command, @Nullable Duration timeout, @Nullable Path directory,
                             Map<String, String> environment, @Nullable Consumer<String> onLine)
            throws IOException, InterruptedException {
        // Every npm, node and pnpm this launcher runs lands here, which is why the
        // program name is completed by platform in this one place: on Windows those
        // tools are `npm.cmd` / `node.exe`, and the bare names are either not found at
        // all or — for the extensionless `npm` a Node.js installation also ships — a
        // POSIX shell script the OS refuses to start.
        // Resolved against the PATH the child will see, not the panel's own: a managed
        // runtime is prepended by the caller through `environment`, and on Windows a bare
        // `npm` is not a file at all — resolving it against this process's PATH would miss
        // the runtime's `npm.cmd` and fail the launch.
        ProcessBuilder builder = new ProcessBuilder(
                org.jackhuang.hmcl.util.platform.Privileges.asAgent(
                        Executables.startable(command, environment.get("PATH"))));
        // What the launcher is configured to download through travels with every child, so a
        // proxy covers the version lists and the plugin installs alike.
        builder.environment().putAll(DshNetworkSettings.environment());
        if (directory != null) {
            builder.directory(directory.toFile());
        }
        builder.environment().putAll(environment);
        // Only when nobody provided one: an inherited or explicitly passed
        // HOME always wins.
        String home = builder.environment().get("HOME");
        if (home == null || home.isBlank()) {
            builder.environment().putAll(homeFallback());
        }
        builder.redirectErrorStream(true);

        ManagedProcess process = new ManagedProcess(builder);
        // Registered while it runs, so an install the user cancels can stop the
        // program it started. Interrupting the thread waiting on a process does
        // not stop the process: npm keeps writing to a directory nothing is
        // watching any more.
        Process running = process.getProcess();
        RUNNING.put(running, Thread.currentThread());
        List<String> output = Collections.synchronizedList(new ArrayList<>());
        Charset charset = OperatingSystem.NATIVE_CHARSET;

        process.pumpInputStream(line -> {
            output.add(line);
            if (onLine != null) {
                onLine.accept(line);
            }
        });
        process.pumpErrorStream(line -> {
            output.add(line);
            if (onLine != null) {
                onLine.accept(line);
            }
        });

        try {
            int exitCode;
            if (timeout == null) {
                exitCode = running.waitFor();
            } else if (running.waitFor(timeout.toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS)) {
                exitCode = running.exitValue();
            } else {
                // Killed rather than asked to stop. What a command that outlived its deadline is
                // doing is waiting for something that is not coming, and a request to stop is one it
                // can outlive just as well — so the deadline is answered with the kill, and the
                // result says why rather than pretending the program exited.
                killTree(running);
                process.destroyRelatedThreads();
                return new Result(0, List.of(), true);
            }
            process.destroyRelatedThreads();
            return new Result(exitCode, List.copyOf(output));
        } catch (InterruptedException e) {
            // The wait was cut short, which is what cancelling an install does.
            // Stopping the wait does not stop the program: it carries on writing
            // into a directory whose owner has gone. It is stopped here, where it
            // was started — the caller cannot do it, because by the time it sees
            // the interrupt this block has already removed the process from the
            // registry.
            stopTree(running);
            Thread.currentThread().interrupt();
            throw e;
        } finally {
            RUNNING.remove(running);
        }
    }

    /// Stops a program and everything it started.
    ///
    /// npm is a launcher that runs node, so stopping the launcher leaves the
    /// work running. What is being stopped is the work.
    ///
    /// @param process the program to stop
    private static void stopTree(Process process) {
        process.descendants().forEach(ProcessHandle::destroy);
        process.destroy();
        try {
            if (!process.waitFor(5, java.util.concurrent.TimeUnit.SECONDS)) {
                process.descendants().forEach(ProcessHandle::destroyForcibly);
                process.destroyForcibly();
            }
        } catch (InterruptedException e) {
            process.descendants().forEach(ProcessHandle::destroyForcibly);
            process.destroyForcibly();
            Thread.currentThread().interrupt();
        }
    }

    /// Kills a program and everything it started, without asking first.
    ///
    /// The difference from [stopTree] is the whole point of it: that one asks, and a program that
    /// has stopped listening is not stopped by being asked. A deadline that has already passed is not
    /// the moment to start a conversation.
    ///
    /// @param process the program to kill
    private static void killTree(Process process) {
        process.descendants().forEach(ProcessHandle::destroyForcibly);
        process.destroyForcibly();
        try {
            process.waitFor(1, java.util.concurrent.TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }


    /// Stops the commands one worker thread started, and waits for them to stop.
    ///
    /// Called when a task is cancelled. Only that thread's trees are stopped —
    /// `MAX_INSTALLS` allows two installs to run at once, and killing the
    /// sibling's pnpm along with the cancelled one corrupts a healthy install.
    /// It waits, because the caller deletes what the command was writing: a
    /// program that has been asked to stop is still writing until it has.
    ///
    /// @param owner the worker thread whose processes are stopped
    public static void stopRunning(Thread owner) {
        for (Map.Entry<Process, Thread> entry : RUNNING.entrySet()) {
            if (entry.getValue() == owner) {
                stopTree(entry.getKey());
            }
        }
    }

    /// How many launched programs are registered right now. Exists so tests
    /// can wait for a process to appear before cancelling its task.
    public static int runningCount() {
        return RUNNING.size();
    }

    /// The `HOME` a child needs when the launcher was started without one.
    ///
    /// systemd does not inject `HOME` into a service environment, and a bare
    /// deployment started with `env -i` has none either. npm, pnpm and node
    /// lifecycle scripts still expect it: koffi's `cnoke.cjs` looks for its
    /// cache under `XDG_CACHE_HOME` → `HOME` and throws
    /// `Missing HOME environment variable` otherwise, which pnpm reports only
    /// as `ERR_PNPM_EXECUTOR_LIFECYCLE_SCRIPT_FAILED`. The JVM always knows a
    /// home directory, so it is used as the fallback.
    ///
    /// @return a one-entry map, or an empty one when the JVM has no home
    static Map<String, String> homeFallback() {
        String home = System.getProperty("user.home");
        if (home == null || home.isBlank()) {
            return Map.of();
        }
        return Map.of("HOME", home);
    }
}
