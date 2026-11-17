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
package org.jackhuang.hmcl.web.acp;

import com.google.gson.JsonObject;
import org.jackhuang.hmcl.dsh.DshAcpClient;
import org.jackhuang.hmcl.dsh.DshException;
import org.jackhuang.hmcl.dsh.DshHomeMode;
import org.jackhuang.hmcl.dsh.DshInstance;
import org.jackhuang.hmcl.dsh.DshInstanceManager;
import org.jackhuang.hmcl.web.event.EventBus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The ACP console bridge against a stub ACP peer: start/reuse, the streaming
/// prompt path, cancel, stop, and the two refusals (an instance with no
/// harness, a second start while one is starting).
///
/// The peer is a separate JVM running [AcpStubPeer] — the same process pattern
/// as `org.jackhuang.hmcl.dsh.StubAcpServer`, with cancel receipts and a PID
/// file added so the tests can observe what the bridge did to it.
class AcpBridgeTest {

    private final List<AcpSessionManager> managers = new ArrayList<>();
    private final List<String> instances = new ArrayList<>();

    @TempDir
    Path tempDir;

    @AfterEach
    void tearDown() throws Exception {
        for (AcpSessionManager manager : managers) {
            manager.closeAll();
        }
        managers.clear();
        for (String id : instances) {
            if (DshInstanceManager.exists(id)) {
                DshInstanceManager.delete(id);
            }
        }
        instances.clear();
    }

    @Test
    void startThenPromptStreamsUpdatesAndFinishes() throws Exception {
        String id = "acp-bridge-stream";
        Path artifacts = artifactDir(id);
        AcpFixture fixture = fixture(id, true, artifacts, 0);

        AcpSessionManager manager = manager(fixture);
        List<JsonObject> events = events(id, fixture.bus());

        manager.start(id, null);
        JsonObject ready = await(events, "acp-ready", 30_000);
        assertNotNull(ready, "no acp-ready for an installed instance");
        String session = ready.get("session").getAsString();
        assertFalse(session.isBlank(), "acp-ready must carry the session id");
        assertEquals(DshAcpClient.PROTOCOL_VERSION, ready.get("protocolVersion").getAsInt());
        assertEquals(id, ready.get("instance").getAsString());

        manager.prompt(id, session, "say hello");
        JsonObject finished = await(events, "acp-finished", 30_000);
        assertNotNull(finished, "no acp-finished after a prompt");
        assertEquals(AcpStubPeer.STOP_REASON, finished.get("stopReason").getAsString());
        assertEquals(session, finished.get("session").getAsString());

        List<JsonObject> updates = matching(events, "acp-update");
        assertEquals(AcpStubPeer.CHUNKS.length, updates.size(),
                "one acp-update per streamed chunk: " + updates);
        for (int i = 0; i < AcpStubPeer.CHUNKS.length; i++) {
            JsonObject update = updates.get(i).getAsJsonObject("update");
            assertEquals("agent_message_chunk", update.get("kind").getAsString(),
                    "the panel reads `kind`");
            assertEquals("agent_message_chunk", update.get("sessionUpdate").getAsString(),
                    "the raw protocol field must survive the pass-through");
            assertEquals(AcpStubPeer.CHUNKS[i],
                    update.getAsJsonObject("content").get("text").getAsString());
            assertEquals(session, updates.get(i).get("session").getAsString());
        }
    }

    @Test
    void cancelReachesTheRunningPrompt() throws Exception {
        String id = "acp-bridge-cancel";
        Path artifacts = artifactDir(id);
        AcpFixture fixture = fixture(id, true, artifacts, 8_000);

        AcpSessionManager manager = manager(fixture);
        List<JsonObject> events = events(id, fixture.bus());

        manager.start(id, null);
        JsonObject ready = await(events, "acp-ready", 30_000);
        assertNotNull(ready, "no acp-ready");
        String session = ready.get("session").getAsString();

        manager.prompt(id, session, "a long one");
        assertNotNull(await(events, "acp-update", 30_000), "the prompt never started streaming");

        manager.cancel(id, session);
        assertTrue(awaitFileContains(fixture.cancelLog(), "cancel " + session, 15_000),
                "session/cancel never reached the stub");
    }

    @Test
    void stopEndsTheProcess() throws Exception {
        String id = "acp-bridge-stop";
        Path artifacts = artifactDir(id);
        AcpFixture fixture = fixture(id, true, artifacts, 0);

        AcpSessionManager manager = manager(fixture);
        List<JsonObject> events = events(id, fixture.bus());

        manager.start(id, null);
        assertNotNull(await(events, "acp-ready", 30_000), "no acp-ready");
        long pid = awaitPid(fixture.pidFile(), 15_000);
        assertTrue(pid > 0, "the stub never wrote its pid");
        assertTrue(isAlive(pid), "the stub should be running");

        manager.stop(id);
        assertTrue(awaitDead(pid, 15_000), "acp-stop must end the process");
    }

    @Test
    void repeatedStartReusesAReadyConsole() throws Exception {
        String id = "acp-bridge-reuse";
        Path artifacts = artifactDir(id);
        AcpFixture fixture = fixture(id, true, artifacts, 0);

        AcpSessionManager manager = manager(fixture);
        List<JsonObject> events = events(id, fixture.bus());

        manager.start(id, null);
        JsonObject first = await(events, "acp-ready", 30_000);
        assertNotNull(first, "no first acp-ready");
        long pid = awaitPid(fixture.pidFile(), 15_000);
        assertTrue(pid > 0, "the stub never wrote its pid");

        manager.start(id, null);
        JsonObject second = awaitNth(events, "acp-ready", 2, 15_000);
        assertNotNull(second, "a repeated acp-start on a ready console must answer acp-ready");
        assertEquals(first.get("session").getAsString(), second.get("session").getAsString(),
                "reuse must hand back the live session");
        assertEquals(pid, readPid(fixture.pidFile()), "reuse must not spawn a second process");
        assertNull(await(events, "acp-error", 300), "reuse is not an error");
    }

    @Test
    void repeatedStartWhileStartingIsRefused() throws Exception {
        String id = "acp-bridge-starting";
        Path artifacts = artifactDir(id);
        AcpFixture fixture = fixture(id, true, artifacts, 0);

        CountDownLatch release = new CountDownLatch(1);
        AcpSessionManager.Connector blocking = (instance, cwd, listener) -> {
            try {
                release.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new DshException("interrupted while starting", e);
            }
            return DshAcpClient.connect(instance, stubCommand(), cwd, fixture.environment(), listener);
        };
        AcpSessionManager manager = new AcpSessionManager(fixture.bus(), blocking);
        managers.add(manager);
        List<JsonObject> events = events(id, fixture.bus());

        manager.start(id, null);
        manager.start(id, null);
        JsonObject error = await(events, "acp-error", 10_000);
        assertNotNull(error, "a second start while one is starting must be refused");
        assertTrue(error.get("error").getAsString().contains("already starting"), error.toString());

        release.countDown();
        assertNotNull(await(events, "acp-ready", 30_000), "the first start must still come up");
    }

    @Test
    void instanceWithoutAHarnessAnswersError() throws Exception {
        String id = "acp-bridge-missing";
        AcpFixture fixture = fixture(id, false, artifactDir(id), 0);

        // The production connector: nothing here needs node, because the bridge
        // refuses the instance before a process is ever considered.
        AcpSessionManager manager = new AcpSessionManager(fixture.bus());
        managers.add(manager);
        List<JsonObject> events = events(id, fixture.bus());

        manager.start(id, null);
        JsonObject error = await(events, "acp-error", 10_000);
        assertNotNull(error, "an instance with no harness must answer acp-error");
        assertTrue(error.get("error").getAsString().contains("no DeepSeek Harness"), error.toString());
        assertNull(await(events, "acp-ready", 300), "no console can come up without a harness");
    }

    /// The bus a stub-backed manager publishes on, plus the paths the peer's
    /// observations land in.
    private record AcpFixture(EventBus bus, Path cancelLog, Path pidFile,
                              Map<String, String> environment) {
    }

    private AcpFixture fixture(String id, boolean installed, Path artifacts, long promptDelay)
            throws Exception {
        Path workspace = Files.createDirectories(tempDir.resolve(id + "-workspace"));
        if (DshInstanceManager.exists(id)) {
            DshInstanceManager.delete(id);
        }
        DshInstance instance = DshInstanceManager.create(id, "1.0.0", DshInstance.DEFAULT_PROFILE,
                workspace, DshHomeMode.ISOLATED, null, List.of(), Map.of());
        instances.add(id);
        if (installed) {
            Path bin = instance.dshEntryPoint();
            Files.createDirectories(bin.getParent());
            Files.writeString(bin, "// the bridge tests never execute this file\n");
        }
        Path cancelLog = artifacts.resolve("cancel.log");
        Path pidFile = artifacts.resolve("pid");
        Map<String, String> environment = Map.of(
                AcpStubPeer.CANCEL_LOG_ENV, cancelLog.toString(),
                AcpStubPeer.PID_FILE_ENV, pidFile.toString(),
                AcpStubPeer.PROMPT_DELAY_ENV, Long.toString(promptDelay));
        return new AcpFixture(new EventBus(), cancelLog, pidFile, environment);
    }

    private AcpSessionManager manager(AcpFixture fixture) {
        AcpSessionManager manager = new AcpSessionManager(fixture.bus(),
                (instance, cwd, listener) ->
                        DshAcpClient.connect(instance, stubCommand(), cwd, fixture.environment(), listener));
        managers.add(manager);
        return manager;
    }

    private static List<JsonObject> events(String id, EventBus bus) {
        List<JsonObject> events = new CopyOnWriteArrayList<>();
        // The manager publishes on the instance topic; the subscription itself
        // is the only thing that needs the bus here.
        bus.subscribe("instance:" + id, (topic, payload) -> events.add(payload));
        return events;
    }

    private Path artifactDir(String id) throws Exception {
        return Files.createDirectories(tempDir.resolve(id + "-artifacts"));
    }

    private static List<String> stubCommand() {
        return List.of(
                Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                "-cp", System.getProperty("java.class.path"),
                AcpStubPeer.class.getName());
    }

    private static List<JsonObject> matching(List<JsonObject> events, String type) {
        List<JsonObject> result = new ArrayList<>();
        for (JsonObject event : events) {
            if (type.equals(event.get("type").getAsString())) {
                result.add(event);
            }
        }
        return result;
    }

    private static JsonObject await(List<JsonObject> events, String type, long millis)
            throws InterruptedException {
        return awaitNth(events, type, 1, millis);
    }

    private static JsonObject awaitNth(List<JsonObject> events, String type, int nth, long millis)
            throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(millis);
        while (System.nanoTime() < deadline) {
            List<JsonObject> matches = matching(events, type);
            if (matches.size() >= nth) {
                return matches.get(nth - 1);
            }
            Thread.sleep(25);
        }
        return null;
    }

    private static boolean awaitFileContains(Path file, String needle, long millis)
            throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(millis);
        while (System.nanoTime() < deadline) {
            if (readText(file).contains(needle)) {
                return true;
            }
            Thread.sleep(25);
        }
        return false;
    }

    private static String readText(Path file) {
        try {
            return Files.isRegularFile(file) ? Files.readString(file) : "";
        } catch (java.io.IOException e) {
            return "";
        }
    }

    private static long awaitPid(Path pidFile, long millis) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(millis);
        while (System.nanoTime() < deadline) {
            long pid = readPid(pidFile);
            if (pid > 0) {
                return pid;
            }
            Thread.sleep(25);
        }
        return -1;
    }

    private static long readPid(Path pidFile) {
        try {
            if (!Files.isRegularFile(pidFile)) {
                return -1;
            }
            return Long.parseLong(Files.readString(pidFile).trim());
        } catch (Exception e) {
            return -1;
        }
    }

    private static boolean isAlive(long pid) {
        return ProcessHandle.of(pid).map(ProcessHandle::isAlive).orElse(false);
    }

    private static boolean awaitDead(long pid, long millis) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(millis);
        while (System.nanoTime() < deadline) {
            if (!isAlive(pid)) {
                return true;
            }
            Thread.sleep(50);
        }
        return false;
    }
}
