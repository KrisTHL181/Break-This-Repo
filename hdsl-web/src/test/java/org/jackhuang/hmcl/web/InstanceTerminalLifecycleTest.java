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
package org.jackhuang.hmcl.web;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jackhuang.hmcl.dsh.DshInstanceManager;
import org.jackhuang.hmcl.util.platform.OperatingSystem;
import org.jackhuang.hmcl.web.terminal.TerminalSession;
import org.jackhuang.hmcl.web.terminal.TerminalTarget;
import org.jackhuang.hmcl.web.terminal.pty.PtySize;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/// The instance lifecycle against the one resource the web runtime does not
/// know about: the instance's terminal.
///
/// The terminal is a process of the instance, started on demand and killed by
/// the panel — the only party that can kill it, because the record it is keyed
/// by lives in the panel. Delete, stop and launch therefore all have to take
/// the terminal down themselves; leaving it up means an agent writing to a home
/// that is about to be deleted, and, after a delete, no route left that could
/// reach the process at all.
///
/// The terminal here is a real PTY running `/bin/cat`, registered in the
/// running server's own registry under the brand the dsh routes use. No dsh
/// install, no TUI plugin and no browser are involved: what is under test is
/// the order of the two calls a lifecycle route makes.
class InstanceTerminalLifecycleTest {

    private static final String BRAND = "dsh";

    private static final Map<String, String> ENV = Map.of(
            "PATH", "/usr/local/bin:/usr/bin:/bin",
            "TERM", "xterm-256color");

    private final List<String> created = new ArrayList<>();

    @TempDir
    Path dataDir;

    @BeforeAll
    static void linuxOnly() {
        assumeTrue(OperatingSystem.CURRENT_OS == OperatingSystem.LINUX, "PTY tests run on Linux");
    }

    @AfterEach
    void tearDown() throws Exception {
        for (String id : created) {
            if (DshInstanceManager.exists(id)) {
                DshInstanceManager.delete(id);
            }
        }
        created.clear();
    }

    @Test
    void deletingAnInstanceKillsItsTerminal() throws Exception {
        try (TestSupport.RunningServer running = startServer()) {
            String id = createInstance(running);
            TerminalSession session = openTerminal(running, id);

            HttpResponse<String> deleted = send(TestSupport.client(),
                    running.baseUrl() + "/api/instances/" + id, "DELETE", null);
            assertEquals(200, deleted.statusCode(), deleted.body());
            created.remove(id);

            assertTerminalGone(running, id, session);
        }
    }

    /// A stop has to reach the terminal even though the web runtime never
    /// started: `runningProcess` is empty, and before this was fixed the route
    /// read that as "instance not running" and refused to do anything — with a
    /// live agent process on the other side of the refusal.
    @Test
    void stoppingAnInstanceWithOnlyATerminalKillsItAndSaysStopped() throws Exception {
        try (TestSupport.RunningServer running = startServer()) {
            String id = createInstance(running);
            TerminalSession session = openTerminal(running, id);

            HttpResponse<String> stopped = send(TestSupport.client(),
                    running.baseUrl() + "/api/instances/" + id + "/stop", "POST", null);
            assertEquals(200, stopped.statusCode(), stopped.body());
            assertEquals("stopped", json(stopped).get("state").getAsString(),
                    "nothing was left to stop asynchronously, so the answer is the state, not a promise");

            assertTerminalGone(running, id, session);
        }
    }

    /// A stop of an instance that has nothing running is still a conflict: the
    /// terminal case widened what counts as running, not what an answer means.
    @Test
    void stoppingAnInstanceWithNothingRunningIsStillAConflict() throws Exception {
        try (TestSupport.RunningServer running = startServer()) {
            String id = createInstance(running);

            HttpResponse<String> stopped = send(TestSupport.client(),
                    running.baseUrl() + "/api/instances/" + id + "/stop", "POST", null);
            assertEquals(409, stopped.statusCode(), stopped.body());
            assertEquals("instance not running", json(stopped).get("error").getAsString());
        }
    }

    /// The web runtime and the TUI share DSH_HOME, so a launch that left the
    /// terminal up would put two writers on one home. The launch is refused for
    /// the missing install here, which happens *after* the takeover — the
    /// terminal has to be gone either way, because the refusal is not a reason
    /// to leave the agent running.
    @Test
    void launchingAnInstanceKillsItsTerminalBeforeAnythingElse() throws Exception {
        try (TestSupport.RunningServer running = startServer()) {
            String id = createInstance(running);
            TerminalSession session = openTerminal(running, id);

            HttpResponse<String> launched = send(TestSupport.client(),
                    running.baseUrl() + "/api/instances/" + id + "/launch", "POST",
                    "{\"autoInstall\":false}");
            assertEquals(409, launched.statusCode(), launched.body());
            assertEquals("not installed", json(launched).get("error").getAsString());

            assertTerminalGone(running, id, session);
        }
    }

    // ----------------------------------------------------------------- helpers --

    private TestSupport.RunningServer startServer() throws Exception {
        return TestSupport.start(dataDir, Map.of(), config -> {
            config.bindHost = "127.0.0.1";
            config.auth.disabled = true;
        });
    }

    /// A real PTY on the running server's registry, under the brand the dsh
    /// lifecycle routes close.
    private static TerminalSession openTerminal(TestSupport.RunningServer running, String id) throws Exception {
        return running.server().terminals().open(
                new TerminalTarget(BRAND, id, List.of("/bin/cat"),
                        Path.of(System.getProperty("java.io.tmpdir")), ENV),
                new PtySize(80, 24));
    }

    private String createInstance(TestSupport.RunningServer running) throws Exception {
        HttpResponse<String> created = send(TestSupport.client(),
                running.baseUrl() + "/api/instances", "POST",
                "{\"name\":\"lifecycle\",\"version\":\"0.1.7-rc.1\",\"autoInstall\":false}");
        assertEquals(201, created.statusCode(), created.body());
        String id = json(created).getAsJsonObject("instance").get("id").getAsString();
        this.created.add(id);
        return id;
    }

    /// Both halves of "the terminal is gone": the registry no longer holds it —
    /// so a later `DELETE .../terminal` has nothing to reach, and the route that
    /// deletes an instance is the only chance to kill it — and the child is
    /// reaped rather than left as an orphan.
    private static void assertTerminalGone(TestSupport.RunningServer running, String id, TerminalSession session)
            throws InterruptedException {
        assertTrue(running.server().terminals().find(BRAND, id).isEmpty(),
                "the instance's terminal must not survive the lifecycle route");

        long deadline = System.nanoTime() + Duration.ofSeconds(5).toNanos();
        while (session.state() != TerminalSession.State.EXITED && System.nanoTime() < deadline) {
            Thread.sleep(20);
        }
        assertEquals(TerminalSession.State.EXITED, session.state(), "the agent process must be reaped");
        assertNotEquals(Integer.MIN_VALUE, session.exitCode(), "a reaped process has an exit status");
    }

    private static HttpResponse<String> send(HttpClient client, String url, String method, String body)
            throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url));
        if (body == null) {
            builder.method(method, HttpRequest.BodyPublishers.noBody());
        } else {
            builder.header("Content-Type", "application/json")
                    .method(method, HttpRequest.BodyPublishers.ofString(body));
        }
        return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private static JsonObject json(HttpResponse<String> response) {
        return JsonParser.parseString(response.body()).getAsJsonObject();
    }
}