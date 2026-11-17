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
package org.jackhuang.hmcl.web.zcode;

import org.jackhuang.hmcl.util.ProcessTree;
import org.jackhuang.hmcl.util.platform.OperatingSystem;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import static org.jackhuang.hmcl.util.logging.Logger.LOG;

/// The process side of the experimental ZCode category: one OS process per
/// instance, launched as
/// `node <package>/bin/zcode.mjs --web --no-open --host 127.0.0.1 --port 0
/// --no-token --workspace <dir>` with `ZCODE_DATA_BASE_DIR` pointing at
/// the instance's own `data/` directory and `HOME` at its `home/` — the
/// second one is what keeps the session database on the data volume (see
/// [ZcodeInstanceManager#prepareHome]). Loopback only and tokenless on
/// purpose: the browser reaches the instance through the panel's `/i/<id>/`
/// mount, where the session gate (and TLS, when enabled) already applies —
/// do not "align" this with the old loopback-less documentation, that posture
/// was deliberately replaced.
///
/// ZCode's web mode announces itself on stdout: a `ZCode Web is running` line,
/// then `Local:   http://127.0.0.1:<port>/` carrying the real port (`--port 0`
/// asks the process to pick a free one). The pump thread below watches for
/// that line; until it arrives, or the deadline passes, or the process dies,
/// the instance is `starting`. Port liveness is the ground truth for
/// `running`: the moment the process exits, the instance becomes `error`
/// (with the log's tail as the reason) unless it was asked to stop, in which
/// case it becomes `stopped`.
///
/// Everything here is in-memory. A panel restart forgets every process; an
/// instance that was left running behind the panel's back simply reads as
/// `stopped` afterwards, and the orphaned process is reclaimed by whoever
/// owns the machine. That is accepted for an experimental category.
@NotNullByDefault
public final class ZcodeRuntime {

    /// The lifecycle states an instance can be in.
    public enum State {
        CREATED, STARTING, RUNNING, STOPPED, ERROR
    }

    /// What [status] reports: the state plus, for `error`, the log tail reason.
    public record Status(State state, @Nullable String error) {
    }

    /// How long a launch waits for the readiness line before giving up.
    public static final int READY_TIMEOUT_SECONDS = 60;

    /// The `Local:` line is printed before host services finish initializing.
    /// After it arrives, wait for a real HTTP answer before declaring RUNNING.
    private static final int HEALTH_WAIT_MILLIS = 10_000;
    private static final int HEALTH_CONNECT_TIMEOUT_MILLIS = 250;
    private static final int HEALTH_READ_TIMEOUT_MILLIS = 750;

    /// The readiness line's port: `Local:   http://127.0.0.1:<port>/`.
    private static final Pattern LOCAL_PORT =
            Pattern.compile("Local:\\s+http://127\\.0\\.0\\.1:(\\d+)/");

    private static final int TAIL_LINES = 20;
    private static final int STOP_WAIT_SECONDS = 10;

    private static final Map<String, Process> PROCESSES = new ConcurrentHashMap<>();
    private static final Map<String, State> STATES = new ConcurrentHashMap<>();
    private static final Map<String, String> ERRORS = new ConcurrentHashMap<>();
    private static final Map<String, CompletableFuture<Integer>> READY = new ConcurrentHashMap<>();
    private static final Set<String> STOPPING = ConcurrentHashMap.newKeySet();
    private static final Map<String, Object> LOCKS = new ConcurrentHashMap<>();

    /// The loopback port each instance announced, kept so the `/i/<id>/` proxy
    /// can resolve a target without reading a manifest. Stale entries are
    /// harmless — every reader asks [isRunning] first, and a relaunch overwrites
    /// the port.
    private static final Map<String, Integer> PORTS = new ConcurrentHashMap<>();

    private ZcodeRuntime() {
    }

    /// What the instance is in right now, process liveness first.
    ///
    /// @param id the instance id
    /// @return the status; `created` when nothing is known about the instance
    public static Status status(String id) {
        Process process = PROCESSES.get(id);
        if (process != null && process.isAlive()) {
            State state = STATES.getOrDefault(id, State.STARTING);
            if (state == State.STOPPED || state == State.ERROR) {
                // Winding down; until the waiters notice the exit it is still up.
                state = State.RUNNING;
            }
            return new Status(state, state == State.ERROR ? ERRORS.get(id) : null);
        }
        State state = STATES.getOrDefault(id, State.CREATED);
        return new Status(state, state == State.ERROR ? ERRORS.get(id) : null);
    }

    /// Whether the instance currently holds a live process.
    ///
    /// @param id the instance id
    /// @return whether it is running
    public static boolean isRunning(String id) {
        Process process = PROCESSES.get(id);
        return process != null && process.isAlive();
    }

    /// The live ZCode instance roots as the panel's caller identification sees
    /// them ([org.jackhuang.hmcl.web.auth.PeerIdentity]).
    ///
    /// @return `pid → instance id`, never `null`
    public static Map<Long, String> liveRootPids() {
        Map<Long, String> roots = new java.util.HashMap<>();
        for (Map.Entry<String, Process> entry : PROCESSES.entrySet()) {
            Process process = entry.getValue();
            if (process.isAlive()) {
                roots.put(process.pid(), entry.getKey());
            }
        }
        return roots;
    }

    /// Whether this runtime has ever seen the id — the `/i/<id>/` proxy uses it
    /// to tell "no such instance" (404) from "known but not running" (502).
    ///
    /// @param id the instance id
    /// @return whether a launch was attempted for it in this panel run
    public static boolean known(String id) {
        return STATES.containsKey(id);
    }

    /// The loopback port the instance announced, or `0` when it never came up.
    ///
    /// @param id the instance id
    /// @return the port, or `0`
    public static int portOf(String id) {
        return PORTS.getOrDefault(id, 0);
    }

    /// Launches the instance's process and waits for its readiness line.
    ///
    /// The launch is idempotent: an instance that already holds a live process
    /// is left alone and its current status is returned. Otherwise the log is
    /// truncated, the process is started with `ZCODE_DATA_BASE_DIR` injected
    /// on top of `envMap`, and this method blocks until the process announces
    /// its port (which is persisted into the instance manifest), fails, exits
    /// early, or the 60-second deadline passes.
    ///
    /// @param manager   the instance manager, for paths and the manifest write
    /// @param instance  the instance to launch
    /// @param envMap    extra environment variables for the process
    /// @param nodePath  the node executable
    /// @param packageDir the ZCode distribution holding `bin/zcode.mjs`
    /// @return the status after the attempt
    public static Status launch(ZcodeInstanceManager manager, ZcodeInstance instance,
                                Map<String, String> envMap, String nodePath, Path packageDir) {
        String id = instance.id();
        synchronized (lockFor(id)) {
            Process existing = PROCESSES.get(id);
            if (existing != null && existing.isAlive()) {
                return status(id);
            }

            STOPPING.remove(id);
            ERRORS.remove(id);
            STATES.put(id, State.STARTING);

            Path logFile = manager.logFile(instance);
            Path workspace = manager.workspaceDirectory(instance);
            Process process;
            OutputStream log;
            try {
                Files.createDirectories(logFile.getParent());
                Files.createDirectories(workspace);
                // 3.18.x's standalone server refuses to open its services without
                // a credential-secret. The panel is the deployment operator, so it
                // provisions one per instance — unless the operator supplied an
                // explicit env/file override.
                try {
                    ZcodeCredentials.ensure(manager, instance, envMap);
                } catch (ZcodeException e) {
                    STATES.put(id, State.ERROR);
                    ERRORS.put(id, e.getMessage());
                    return new Status(State.ERROR, e.getMessage());
                }
                // The workbench only has a workspace when the server owns its
                // project store locally — see ZcodeServerPatch. The patch travels
                // in every release this panel builds; a launch only reads it back,
                // because the same bytes must behave the same way whoever starts
                // them.
                ZcodeServerPatch.report(packageDir, ZcodeServerPatch.verify(packageDir));
                log = Files.newOutputStream(logFile,
                        StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
                ProcessBuilder builder = new ProcessBuilder(
                        org.jackhuang.hmcl.util.platform.Privileges.asAgent(java.util.List.of(
                        org.jackhuang.hmcl.util.platform.Executables.startableProgram(nodePath),
                        packageDir.resolve("bin/zcode.mjs").toString(),
                        "--web", "--no-open",
                        // Loopback only, no token: the browser reaches this
                        // instance through the panel's `/i/<id>/` mount, where
                        // the session gate (and TLS, when enabled) already
                        // applies. Nothing else can reach the port.
                        "--host", "127.0.0.1",
                        "--port", "0",
                        "--no-token",
                        "--workspace", workspace.toString())));
                builder.directory(workspace.toFile());
                builder.redirectErrorStream(true);
                builder.environment().putAll(envMap);
                builder.environment().put("ZCODE_DATA_BASE_DIR", manager.dataDirectory(instance).toString());
                // ZCode keeps the session database under `homedir()`, which no
                // environment variable of its own can redirect: `HOME` is the
                // only lever, and without it the database lands on the
                // container's writable layer and dies with the next rebuild.
                Path home = manager.prepareHome(instance);
                builder.environment().put("HOME", home.toString());
                if (OperatingSystem.CURRENT_OS == OperatingSystem.WINDOWS) {
                    // Node resolves `~` through USERPROFILE on Windows and ignores HOME.
                    builder.environment().put("USERPROFILE", home.toString());
                }
                process = builder.start();
            } catch (IOException e) {
                STATES.put(id, State.ERROR);
                String reason = "Failed to start the ZCode process: " + e.getMessage();
                ERRORS.put(id, reason);
                LOG.warning("Could not launch ZCode instance " + id, e);
                return new Status(State.ERROR, reason);
            }

            PROCESSES.put(id, process);
            CompletableFuture<Integer> ready = new CompletableFuture<>();
            READY.put(id, ready);
            Thread pump = new Thread(
                    () -> pumpLines(id, process, log, logFile, ready), "zcode-log-" + id);
            pump.setDaemon(true);
            pump.start();

            Integer port = awaitReady(id, process, ready);
            READY.remove(id);
            if (port != null && !awaitHealthy(process, port)) {
                String reason = "ZCode announced port " + port + " but did not become healthy"
                        + logTail(logFile);
                ERRORS.put(id, reason);
                STATES.put(id, State.ERROR);
                process.destroyForcibly();
                PROCESSES.remove(id);
                return new Status(State.ERROR, reason);
            }
            if (port != null) {
                PORTS.put(id, port);
                ZcodeInstance updated = instance.withLastPort(port);
                try {
                    manager.update(updated);
                    STATES.put(id, State.RUNNING);
                    LOG.info("ZCode instance " + id + " is running on port " + port);
                    return new Status(State.RUNNING, null);
                } catch (ZcodeException e) {
                    LOG.warning("ZCode instance " + id + " came up but its port could not be saved", e);
                    // Running all the same; the next open will not know the port,
                    // which is what lastPort == 0 already means elsewhere.
                    STATES.put(id, State.RUNNING);
                    return new Status(State.RUNNING, null);
                }
            }

            Process removed = PROCESSES.remove(id);
            if (removed != null && removed.isAlive()) {
                removed.destroyForcibly();
            }
            State state = STATES.getOrDefault(id, State.ERROR);
            return new Status(state, state == State.ERROR ? ERRORS.get(id) : null);
        }
    }

    /// Asks the instance's process to terminate and waits briefly for it.
    ///
    /// @param id the instance id
    public static void stop(String id) {
        STOPPING.add(id);
        Process process = PROCESSES.get(id);
        if (process == null) {
            // No output pump is watching this instance, so nothing else will
            // consume the flag.
            STOPPING.remove(id);
            STATES.put(id, State.STOPPED);
            PROCESSES.remove(id);
            return;
        }
        if (!process.isAlive()) {
            // The pump may still be on its way to the end of the output; it
            // consumes the flag and records STOPPED.
            STATES.put(id, State.STOPPED);
            PROCESSES.remove(id);
            return;
        }
        // ZCode is launched as `node .../zcode.mjs`, but the same shim shape applies:
        // stop the tree so nothing keeps the port after the handle we kept is gone.
        ProcessTree.terminate(process, STOP_WAIT_SECONDS);
        PROCESSES.remove(id);
        // Leave STOPPING for the pump: it reaches EOF after the last holder of
        // the pipe is gone, which can be after this method returns.
        STATES.put(id, State.STOPPED);
    }

    private static Object lockFor(String id) {
        return LOCKS.computeIfAbsent(id, key -> new Object());
    }

    /// Waits for the readiness future, the process's death or the deadline —
    /// whichever comes first. Returns the announced port, or `null`.
    private static @Nullable Integer awaitReady(String id, Process process, CompletableFuture<Integer> ready) {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(READY_TIMEOUT_SECONDS);
        while (System.nanoTime() < deadline) {
            Integer port = ready.getNow(null);
            if (port != null) {
                return port;
            }
            if (!process.isAlive()) {
                // Give the pump a moment to record the exit reason.
                ready.getNow(null);
                return null;
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
        }
        ERRORS.put(id, "ZCode did not become ready within " + READY_TIMEOUT_SECONDS + " seconds");
        STATES.put(id, State.ERROR);
        return null;
    }

    /// A `Local:` line only means the CLI picked a port; 3.18.x prints it before
    /// host services initialize, and can exit immediately afterwards. Treat a
    /// real HTTP answer (any 2xx–4xx) as readiness, and fail fast if the process
    /// dies in the meantime.
    private static boolean awaitHealthy(Process process, int port) {
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(HEALTH_WAIT_MILLIS);
        while (System.nanoTime() < deadline) {
            if (!process.isAlive()) {
                return false;
            }
            HttpURLConnection connection = null;
            try {
                connection = (HttpURLConnection) URI.create(
                        "http://127.0.0.1:" + port + "/").toURL().openConnection();
                connection.setConnectTimeout(HEALTH_CONNECT_TIMEOUT_MILLIS);
                connection.setReadTimeout(HEALTH_READ_TIMEOUT_MILLIS);
                connection.setRequestMethod("GET");
                connection.setInstanceFollowRedirects(false);
                int status = connection.getResponseCode();
                if (status >= 200 && status < 500) {
                    return true;
                }
            } catch (IOException ignored) {
                // Not up yet; retry until the deadline.
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }

    private static String logTail(Path logFile) {
        try {
            List<String> tail = tail(logFile, TAIL_LINES);
            return tail.isEmpty() ? "" : " — log tail: " + String.join(" | ", tail);
        } catch (IOException e) {
            return "";
        }
    }

    /// Copies the process's merged output into its log file, watching every
    /// line for the readiness announcement; on EOF (crash or stop) records the
    /// terminal state, with the log's tail as the reason for an unexpected exit.
    private static void pumpLines(String id, Process process, OutputStream log,
                                  Path logFile, CompletableFuture<Integer> ready) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                byte[] bytes = (line + System.lineSeparator()).getBytes(StandardCharsets.UTF_8);
                synchronized (log) {
                    log.write(bytes);
                    log.flush();
                }
                java.util.regex.Matcher matcher = LOCAL_PORT.matcher(line);
                if (matcher.find()) {
                    try {
                        ready.complete(Integer.parseInt(matcher.group(1)));
                    } catch (NumberFormatException ignored) {
                        // Not a port; keep waiting for a better line.
                    }
                }
            }
        } catch (IOException e) {
            if (!ready.isDone()) {
                ready.completeExceptionally(e);
            }
        } finally {
            try {
                log.close();
            } catch (IOException ignored) {
                // The log is best-effort diagnostics.
            }
            if (!ready.isDone()) {
                ready.complete(null);
            }
            PROCESSES.remove(id, process);
            if (STOPPING.remove(id)) {
                STATES.put(id, State.STOPPED);
            } else {
                String reason = "The ZCode process exited unexpectedly";
                try {
                    List<String> tail = tail(logFile, TAIL_LINES);
                    if (!tail.isEmpty()) {
                        reason = reason + " — log tail: " + String.join(" | ", tail);
                    }
                } catch (IOException ignored) {
                    // The generic reason stands.
                }
                ERRORS.put(id, reason);
                STATES.put(id, State.ERROR);
            }
        }
    }

    /// Reads the last lines of a log file.
    private static List<String> tail(Path file, int maxLines) throws IOException {
        if (!Files.isRegularFile(file)) {
            return List.of();
        }
        try (var lines = Files.lines(file, StandardCharsets.UTF_8)) {
            List<String> all = lines.toList();
            return all.subList(Math.max(0, all.size() - maxLines), all.size());
        }
    }
}
