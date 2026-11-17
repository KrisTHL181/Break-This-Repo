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
import org.eclipse.jetty.websocket.api.Callback;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.jackhuang.hmcl.dsh.DshInstance;
import org.jackhuang.hmcl.dsh.DshInstanceManager;
import org.jackhuang.hmcl.dsh.DshProcessManager;
import org.jackhuang.hmcl.web.auth.UserStore;
import org.junit.jupiter.api.AfterEach;
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
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The `/ws` gateway over a real server: unauthenticated handshakes answer
/// 401, a logged-in client subscribes to topics, and the events of the
/// lifecycle — instance CRUD, states, logs, tasks — arrive filtered by the
/// subscription.
class WsGatewayTest {

    private static final String INSTANCE = "ws-gateway-test";
    private static final String PASSWORD = "correct horse battery staple";

    private final List<String> created = new ArrayList<>();
    private final List<Runnable> cleanup = new ArrayList<>();

    @TempDir
    Path dataDir;

    @AfterEach
    void tearDown() throws Exception {
        for (String id : created) {
            DshProcessManager.stop(id);
            if (DshInstanceManager.exists(id)) {
                DshInstanceManager.delete(id);
            }
        }
        created.clear();
        for (Runnable task : cleanup) {
            task.run();
        }
        cleanup.clear();
    }

    private record TestSession(WebSocketClient client, Session session,
                                BlockingQueue<JsonObject> events, List<JsonObject> seen) {
    }

    private TestSession connect(int port, String cookie) throws Exception {
        WebSocketClient client = new WebSocketClient();
        client.start();
        cleanup.add(() -> {
            try {
                client.stop();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        BlockingQueue<JsonObject> events = new LinkedBlockingQueue<>();
        List<JsonObject> seen = new ArrayList<>();
        ClientUpgradeRequest request = new ClientUpgradeRequest();
        if (cookie != null) {
            request.setHeader("Cookie", cookie);
        }
        Session session = client.connect(new Recording(seen, events),
                URI.create("ws://127.0.0.1:" + port + "/ws"), request)
                .get(10, TimeUnit.SECONDS);
        return new TestSession(client, session, events, seen);
    }

    /// The test's client endpoint. Named and public because Jetty's client
    /// binds endpoint methods through method handles, which cannot see into
    /// anonymous classes.
    public static final class Recording implements Session.Listener.AutoDemanding {
        private final List<JsonObject> seen;
        private final BlockingQueue<JsonObject> events;

        public Recording(List<JsonObject> seen, BlockingQueue<JsonObject> events) {
            this.seen = seen;
            this.events = events;
        }

        @Override
        public void onWebSocketText(String message) {
            JsonObject json = JsonParser.parseString(message).getAsJsonObject();
            seen.add(json);
            events.add(json);
        }
    }

    private static String login(String base) throws Exception {
        HttpClient client = TestSupport.client();
        HttpResponse<String> response = client.send(HttpRequest.newBuilder(
                        URI.create(base + "/api/auth/login"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(
                                "{\"username\":\"admin\",\"password\":\"" + PASSWORD + "\"}")).build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        String setCookie = response.headers().firstValue("set-cookie").orElseThrow();
        return setCookie.split(";", 2)[0];
    }

    private static HttpResponse<String> api(String base, String cookie, String method, String path, String body)
            throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(base + path))
                .header("Cookie", cookie);
        if (body == null) {
            builder.method(method, HttpRequest.BodyPublishers.noBody());
        } else {
            builder.header("Content-Type", "application/json")
                    .method(method, HttpRequest.BodyPublishers.ofString(body));
        }
        return TestSupport.client().send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private JsonObject await(TestSession session, String type) throws InterruptedException {
        long deadline = System.nanoTime() + Duration.ofSeconds(30).toNanos();
        while (System.nanoTime() < deadline) {
            for (JsonObject json : session.seen()) {
                if (type.equals(json.get("type").getAsString())) {
                    return json;
                }
            }
            JsonObject json = session.events().poll(200, TimeUnit.MILLISECONDS);
            if (json != null && type.equals(json.get("type").getAsString())) {
                return json;
            }
        }
        return null;
    }

    /// Waits up to `millis` for an event of the given type among the events
    /// seen since `since`; `null` when none arrives in time.
    private JsonObject awaitWithin(TestSession session, String type, long millis) throws InterruptedException {
        long deadline = System.nanoTime() + Duration.ofMillis(millis).toNanos();
        while (System.nanoTime() < deadline) {
            JsonObject json = session.events().poll(200, TimeUnit.MILLISECONDS);
            if (json != null && type.equals(json.get("type").getAsString())) {
                return json;
            }
        }
        return null;
    }

    @Test
    void unauthenticatedHandshakeIsRejected() throws Exception {
        try (TestSupport.RunningServer running = TestSupport.start(dataDir,
                Map.of(UserStore.ENV_ADMIN_PASSWORD, PASSWORD),
                config -> config.bindHost = "127.0.0.1")) {
            WebSocketClient client = new WebSocketClient();
            client.start();
            cleanup.add(() -> {
                try {
                    client.stop();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            ExecutionException refused = assertThrows(ExecutionException.class,
                    () -> client.connect(new Session.Listener.AutoDemanding() {
                    }, URI.create("ws://127.0.0.1:" + running.port() + "/ws"), new ClientUpgradeRequest())
                            .get(10, TimeUnit.SECONDS));
            assertTrue(refused.getCause() instanceof org.eclipse.jetty.websocket.api.exceptions.UpgradeException,
                    "expected an UpgradeException, got " + refused.getCause());
            assertEquals(401, ((org.eclipse.jetty.websocket.api.exceptions.UpgradeException) refused.getCause())
                    .getResponseStatusCode(), "the refused handshake must answer 401");
        }
    }

    @Test
    void subscribesAndReceivesLifecycleEvents() throws Exception {
        NodeDshStub.assumeNode();
        try (TestSupport.RunningServer running = TestSupport.start(dataDir,
                Map.of(UserStore.ENV_ADMIN_PASSWORD, PASSWORD),
                config -> config.bindHost = "127.0.0.1")) {
            String base = running.baseUrl();
            String cookie = login(base);

            // A connection with no subscribe frame gets everything.
            TestSession all = connect(running.port(), cookie);

            HttpResponse<String> createdResponse = api(base, cookie, "POST", "/api/instances",
                    "{\"name\":\"" + INSTANCE + "\",\"version\":\"0.1.7-rc.1\",\"autoInstall\":false}");
            assertEquals(201, createdResponse.statusCode());
            String id = JsonParser.parseString(createdResponse.body()).getAsJsonObject().getAsJsonObject("instance").get("id").getAsString();
            created.add(id);

            JsonObject createdEvent = await(all, "instance-created");
            assertNotNull(createdEvent, "no instance-created event");
            assertEquals(id, createdEvent.getAsJsonObject("instance").get("id").getAsString());

            // Rename while only the unfiltered connection is listening.
            HttpResponse<String> renamed = api(base, cookie, "PATCH", "/api/instances/" + id,
                    "{\"name\":\"ws-gateway-renamed\"}");
            assertEquals(200, renamed.statusCode());
            String renamedId = JsonParser.parseString(renamed.body()).getAsJsonObject().getAsJsonObject("instance").get("id").getAsString();
            created.remove(id);
            created.add(renamedId);
            assertNotNull(await(all, "instance-updated"), "no instance-updated event");

            // A filtered connection hears the instance state and logs, but not
            // the CRUD events that only travel on `instances`.
            TestSession filtered = connect(running.port(), cookie);
            filtered.session().sendText(
                    "{\"type\":\"subscribe\",\"topics\":[\"instance:" + renamedId + "\"]}", Callback.NOOP);
            Thread.sleep(200);

            // A no-op edit still announces an update — on `instances` only.
            HttpResponse<String> touched = api(base, cookie, "PATCH", "/api/instances/" + renamedId,
                    "{\"homeMode\":\"isolated\"}");
            assertEquals(200, touched.statusCode());
            assertNull(awaitWithin(filtered, "instance-updated", 1500),
                    "CRUD events must not reach a connection subscribed only to instance:<id>");

            // Launch the stub and watch the states and logs stream in.
            NodeDshStub.install(DshInstanceManager.find(renamedId), true);
            HttpResponse<String> launch = api(base, cookie, "POST", "/api/instances/" + renamedId + "/launch",
                    "{\"publicHost\":\"panel.example.com\"}");
            assertEquals(202, launch.statusCode());

            JsonObject runningEvent = awaitWithin(filtered, "instance-state", 30_000);
            assertNotNull(runningEvent, "no instance-state event");
            assertEquals(renamedId, runningEvent.get("instance").getAsString());
            long stateDeadline = System.nanoTime() + Duration.ofSeconds(30).toNanos();
            while (!"RUNNING".equals(runningEvent.get("state").getAsString())
                    && System.nanoTime() < stateDeadline) {
                JsonObject next = awaitWithin(filtered, "instance-state", 5000);
                if (next == null) {
                    break;
                }
                runningEvent = next;
            }
            assertEquals("RUNNING", runningEvent.get("state").getAsString(),
                    "the instance never reported ready: " + runningEvent);
            assertTrue(runningEvent.has("url"));
            assertEquals("/i/" + renamedId + "/?token=" + NodeDshStub.TOKEN,
                    runningEvent.get("url").getAsString());

            // Log events stream on instance:<id>; the state-wait loops above
            // may have consumed them from the queue, so look in the full
            // record.
            long logDeadline = System.nanoTime() + Duration.ofSeconds(10).toNanos();
            JsonObject log = null;
            while (System.nanoTime() < logDeadline) {
                for (JsonObject json : filtered.seen()) {
                    if ("log".equals(json.get("type").getAsString())) {
                        log = json;
                        break;
                    }
                }
                if (log != null) {
                    break;
                }
                Thread.sleep(50);
            }
            assertNotNull(log, "no log event");
            assertEquals(renamedId, log.get("instance").getAsString());

            // Stop; the STOPPED event closes the circle.
            HttpResponse<String> stop = api(base, cookie, "POST", "/api/instances/" + renamedId + "/stop", null);
            assertEquals(202, stop.statusCode());
            JsonObject stoppedEvent = awaitWithin(filtered, "instance-state", 30_000);
            assertNotNull(stoppedEvent);
            long stopDeadline = System.nanoTime() + Duration.ofSeconds(30).toNanos();
            while (!"STOPPED".equals(stoppedEvent.get("state").getAsString())
                    && System.nanoTime() < stopDeadline) {
                JsonObject next = awaitWithin(filtered, "instance-state", 5000);
                if (next == null) {
                    break;
                }
                stoppedEvent = next;
            }
            assertEquals("STOPPED", stoppedEvent.get("state").getAsString(),
                    "the instance never reported stopped: " + stoppedEvent);

            // The unfiltered connection saw the CRUD events.
            assertNotNull(await(all, "instance-updated"));
            assertNotNull(await(all, "instance-created"));
        }
    }

    @Test
    void taskEventsArriveOnTheTasksTopic() throws Exception {
        org.junit.jupiter.api.Assumptions.assumeTrue(NodeDshStub.nodeAvailable(),
                "node on PATH is required for the install path");
        try (TestSupport.RunningServer running = TestSupport.start(dataDir,
                Map.of(UserStore.ENV_ADMIN_PASSWORD, PASSWORD),
                config -> config.bindHost = "127.0.0.1")) {
            String base = running.baseUrl();
            String cookie = login(base);
            TestSession tasks = connect(running.port(), cookie);
            tasks.session().sendText("{\"type\":\"subscribe\",\"topics\":[\"tasks\"]}", Callback.NOOP);
            Thread.sleep(200);

            HttpResponse<String> createdResponse = api(base, cookie, "POST", "/api/instances",
                    "{\"name\":\"" + INSTANCE + "\",\"version\":\"0.1.7-rc.1\",\"autoInstall\":false}");
            assertEquals(201, createdResponse.statusCode());
            String id = JsonParser.parseString(createdResponse.body()).getAsJsonObject().getAsJsonObject("instance").get("id").getAsString();
            created.add(id);

            HttpResponse<String> submitted = api(base, cookie, "POST", "/api/instances/" + id + "/install",
                    "{\"version\":\"0.0.0-does-not-exist\"}");
            assertEquals(202, submitted.statusCode());
            String taskId = JsonParser.parseString(submitted.body()).getAsJsonObject().get("taskId").getAsString();

            JsonObject event = await(tasks, "task");
            assertNotNull(event, "no task event");
            assertEquals(taskId, event.get("taskId").getAsString());
            assertEquals(id, event.get("instance").getAsString());
            String state = event.get("state").getAsString();
            assertTrue(state.equals("running") || state.equals("failed"), state);
        }
    }

    @Test
    void unsubscribeNarrowsAndExcludes() throws Exception {
        try (TestSupport.RunningServer running = TestSupport.start(dataDir,
                Map.of(UserStore.ENV_ADMIN_PASSWORD, PASSWORD),
                config -> config.bindHost = "127.0.0.1")) {
            String base = running.baseUrl();
            String cookie = login(base);

            TestSession conn = connect(running.port(), cookie);
            conn.session().sendText("{\"type\":\"subscribe\",\"topics\":[\"instances\"]}", Callback.NOOP);
            Thread.sleep(200);

            // Subscribed: the creation arrives.
            String first = createInstance(base, cookie, "ws-unsub-one");
            JsonObject firstEvent = awaitWithin(conn, "instance-created", 10_000);
            assertNotNull(firstEvent, "no instance-created for the subscribed connection");
            assertEquals(first, firstEvent.getAsJsonObject("instance").get("id").getAsString());

            // Unsubscribed from the only topic: nothing arrives.
            conn.session().sendText("{\"type\":\"unsubscribe\",\"topics\":[\"instances\"]}", Callback.NOOP);
            Thread.sleep(200);
            createInstance(base, cookie, "ws-unsub-two");
            assertNull(awaitWithin(conn, "instance-created", 1500),
                    "unsubscribed topics must go quiet");

            // A bare subscribe re-subscribes to everything.
            conn.session().sendText("{\"type\":\"subscribe\"}", Callback.NOOP);
            Thread.sleep(200);
            String third = createInstance(base, cookie, "ws-unsub-three");
            JsonObject thirdEvent = awaitWithin(conn, "instance-created", 10_000);
            assertNotNull(thirdEvent, "no instance-created after re-subscribing to all");
            assertEquals(third, thirdEvent.getAsJsonObject("instance").get("id").getAsString());

            // In all-mode an unsubscribe names exclusions; the next subscribe
            // without topics lifts them again.
            conn.session().sendText("{\"type\":\"unsubscribe\",\"topics\":[\"instances\"]}", Callback.NOOP);
            Thread.sleep(200);
            createInstance(base, cookie, "ws-unsub-four");
            assertNull(awaitWithin(conn, "instance-created", 1500),
                    "excluded topics must stay quiet in all-mode");
            conn.session().sendText("{\"type\":\"subscribe\"}", Callback.NOOP);
            Thread.sleep(200);
            String fifth = createInstance(base, cookie, "ws-unsub-five");
            JsonObject fifthEvent = awaitWithin(conn, "instance-created", 10_000);
            assertNotNull(fifthEvent, "no instance-created after the exclusions were lifted");
            assertEquals(fifth, fifthEvent.getAsJsonObject("instance").get("id").getAsString());
        }
    }

    @Test
    void acpStartForAnUnknownInstanceAnswersError() throws Exception {
        try (TestSupport.RunningServer running = TestSupport.start(dataDir,
                Map.of(UserStore.ENV_ADMIN_PASSWORD, PASSWORD),
                config -> config.bindHost = "127.0.0.1")) {
            String cookie = login(running.baseUrl());
            TestSession session = connect(running.port(), cookie);

            session.session().sendText(
                    "{\"type\":\"acp-start\",\"instance\":\"ws-gateway-no-such-instance\"}", Callback.NOOP);

            JsonObject error = await(session, "acp-error");
            assertNotNull(error, "no acp-error for an ACP start on an unknown instance");
            assertEquals("ws-gateway-no-such-instance", error.get("instance").getAsString());
            assertTrue(error.get("error").getAsString().contains("does not exist"), error.toString());
        }
    }

    private String createInstance(String base, String cookie, String name) throws Exception {
        HttpResponse<String> response = api(base, cookie, "POST", "/api/instances",
                "{\"name\":\"" + name + "\",\"version\":\"0.1.7-rc.1\",\"autoInstall\":false}");
        assertEquals(201, response.statusCode());
        String id = JsonParser.parseString(response.body()).getAsJsonObject().getAsJsonObject("instance").get("id").getAsString();
        created.add(id);
        return id;
    }
}
