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
package org.jackhuang.hmcl.web.proxy;

import org.jackhuang.hmcl.dsh.DshHomeMode;
import org.jackhuang.hmcl.dsh.DshInstance;
import org.jackhuang.hmcl.dsh.DshInstanceManager;
import org.jackhuang.hmcl.dsh.DshProcess;
import org.jackhuang.hmcl.dsh.DshProcessManager;
import org.jackhuang.hmcl.setting.GameDirectory;
import org.jackhuang.hmcl.web.NodeDshStub;
import org.jackhuang.hmcl.web.TestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// What the proxy does when an upstream attempt dies, and what a cancellation
/// reaches.
///
/// The upstream is [StubUpstream] on the loopback port the instance's readiness
/// line reported — the passive node stub announces the port the launcher
/// allocated and idles, exactly as in
/// [org.jackhuang.hmcl.web.ProxyBehaviorTest#websocketUpgradeIsRelayedWithFramesAndPingPong].
/// Two of the three tests put a request through the panel's own server; the
/// third drives [InstanceProxyServlet#attempt] directly, because the event it is
/// about — a client that goes away — is not one this container delivers on its
/// own (see [ProxyUpstreamFailureTest#cancellationReachesTheAttemptInFlight]).
class ProxyUpstreamFailureTest {

    private static final String INSTANCE = "proxy-upstream-failure-test";

    private final List<AutoCloseable> cleanup = new ArrayList<>();

    @TempDir
    Path dataDir;

    @AfterEach
    void tearDown() throws Exception {
        DshProcessManager.stop(INSTANCE);
        if (DshInstanceManager.exists(INSTANCE)) {
            DshInstanceManager.delete(INSTANCE);
        }
        for (AutoCloseable task : cleanup) {
            task.close();
        }
        cleanup.clear();
    }

    /// A `POST` whose first attempt died is **not** sent a second time: the
    /// client is answered with the failure. A body that arrives twice is a
    /// second write none of these clients asked for, and nothing here can tell
    /// an instance that acted before its connection died from one that never saw
    /// the request at all.
    @Test
    void aFailedPostIsAnsweredWithTheFailureAndNotSentAgain() throws Exception {
        NodeDshStub.assumeNode();
        try (TestSupport.RunningServer running = startServer()) {
            try (StubUpstream upstream = startInstance(false)) {
                HttpResponse<String> response = TestSupport.client().send(
                        HttpRequest.newBuilder(mount(running.baseUrl(), "/flaky"))
                                .header("Content-Type", "application/json")
                                .POST(HttpRequest.BodyPublishers.ofString("{\"key\":\"sk-secret\"}"))
                                .build(),
                        HttpResponse.BodyHandlers.ofString());

                assertEquals(1, upstream.requests().size(),
                        "a POST must not reach the instance twice: " + upstream.log());
                assertEquals(502, response.statusCode(),
                        "the client has to be told the attempt failed: " + response.body());
            }
        }
    }

    /// A `GET` whose first attempt died is still retried — the request a retry
    /// exists for, unchanged.
    @Test
    void aFailedGetIsStillRetried() throws Exception {
        NodeDshStub.assumeNode();
        try (TestSupport.RunningServer running = startServer()) {
            try (StubUpstream upstream = startInstance(false)) {
                HttpResponse<String> response = TestSupport.client().send(
                        HttpRequest.newBuilder(mount(running.baseUrl(), "/flaky")).GET().build(),
                        HttpResponse.BodyHandlers.ofString());

                assertEquals(200, response.statusCode(), response.body() + " — " + upstream.log());
                assertTrue(response.body().contains("\"/flaky\""), response.body());
                assertEquals(2, upstream.requests().size(),
                        "one failed attempt and one retry: " + upstream.log());
            }
        }
    }

    /// A cancellation has to reach the attempt that is really in flight. Once
    /// the first attempt has failed and the request is being retried, the stage
    /// the pump waits on is a derived future, and cancelling that one reaches the
    /// attempt it was derived *from* — the dead one — leaving the retry running
    /// at the instance with nobody left to answer.
    ///
    /// The cancellation is driven here exactly as the servlet's async listener
    /// and its pump drive it (both call [InstanceProxyServlet.Upstream#cancel]),
    /// rather than by a client that disconnects and waits for the container to
    /// say so. Jetty 12 reports a client that went away either by failing a
    /// write to it or by failing to read its request body; this servlet writes
    /// nothing while the instance is silent, and the request it holds has been
    /// read in full — measured 2026-10, a bare Jetty 12.0.39 connector with a
    /// one-second idle timeout reported nothing at all for twenty-five seconds
    /// after the client's socket closed. Waiting for an event the container does
    /// not produce would test the clock, not the proxy.
    @Test
    void cancellationReachesTheAttemptInFlight() throws Exception {
        try (StubUpstream upstream = new StubUpstream(0, true)) {
            HttpClient client = TestSupport.client();
            InstanceProxyServlet.Upstream attempt = InstanceProxyServlet.attempt(client,
                    HttpRequest.newBuilder(upstream.mount("/hang")).GET().build(), true);

            assertTrue(upstream.awaitRequests(2, 20, TimeUnit.SECONDS), upstream.log());
            StubUpstream.StubConnection retry = upstream.lastConnection();

            // The stage the pump holds is the composition, and this is what
            // cancelling it does: the retry outlives it.
            assertTrue(attempt.answer().cancel(true), upstream.log());
            assertFalse(retry.awaitClosedByPeer(1, TimeUnit.SECONDS),
                    "cancelling the derived stage must not be mistaken for cancelling the retry: "
                            + upstream.log());

            // The attempt in flight is what the servlet cancels.
            attempt.cancel();
            assertTrue(retry.awaitClosedByPeer(20, TimeUnit.SECONDS),
                    "the upstream request must be cancelled, not abandoned: " + upstream.log());
        }
    }

    // ---------------------------------------------------------------- helpers --

    private TestSupport.RunningServer startServer() throws Exception {
        TestSupport.RunningServer running = TestSupport.start(dataDir, Map.of(), config -> {
            config.bindHost = "127.0.0.1";
            config.auth.disabled = true;
        });
        cleanup.add(running::close);
        return running;
    }

    private static URI mount(String baseUrl, String path) {
        return URI.create(baseUrl + "/i/" + INSTANCE + path);
    }

    /// Announces the instance's loopback port with the passive stub, then binds
    /// that port with [StubUpstream] — the order the WebSocket relay test uses,
    /// and for its reason: the launcher's port check needs the port free while
    /// it launches.
    private StubUpstream startInstance(boolean hangAfterTheFirstFailure) throws Exception {
        DshInstance instance = DshInstanceManager.create(INSTANCE, "0.1.7-rc.1", "web",
                GameDirectory.defaultDirectory().directory(), DshHomeMode.ISOLATED, null, List.of(), Map.of());
        int port = instance.portOrDefault();
        NodeDshStub.install(instance, false);
        DshProcess process = DshProcessManager.launch(instance);
        cleanup.add(process::stop);
        cleanup.add(() -> DshProcessManager.stop(INSTANCE));
        awaitReady(process);
        StubUpstream upstream = new StubUpstream(port, hangAfterTheFirstFailure);
        cleanup.add(upstream);
        return upstream;
    }

    private static void awaitReady(DshProcess process) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 30_000;
        while (System.currentTimeMillis() < deadline) {
            if (process.webUrl().isPresent()) {
                return;
            }
            Thread.sleep(50);
        }
        assertNotNull(process.webUrl().orElse(null), "the stub never printed its readiness line");
    }

    /// A stand-in for an instance: a raw `ServerSocket` that answers `200` with a
    /// JSON body — and, for the first request it ever sees, answers one byte and
    /// closes.
    ///
    /// That first-request failure is deliberately *not* a silent close: a
    /// connection closed before a single response byte is what java.net.http
    /// treats as an expired pooled connection, and it resends the request itself
    /// before anything above it ever sees a failure (measured 2026-10: a stub
    /// that only closed the socket produced two attempts for one `sendAsync`).
    /// One byte is enough for the answer to be unparsable and for the failure to
    /// be reported, which is the failure the servlet's retry is about.
    static final class StubUpstream implements AutoCloseable {

        private final ServerSocket listen;
        private final List<StubConnection> connections = new CopyOnWriteArrayList<>();
        private final boolean hangAfterTheFirstFailure;

        StubUpstream(int port, boolean hangAfterTheFirstFailure) throws IOException {
            this.hangAfterTheFirstFailure = hangAfterTheFirstFailure;
            this.listen = new ServerSocket(port, 50, InetAddress.getByName("127.0.0.1"));
            Thread accept = new Thread(this::acceptLoop, "stub-upstream");
            accept.setDaemon(true);
            accept.start();
        }

        /// The address a request reaches this stub at, under the given path.
        URI mount(String path) {
            return URI.create("http://127.0.0.1:" + listen.getLocalPort() + path);
        }

        private void acceptLoop() {
            try {
                while (true) {
                    Socket socket = listen.accept();
                    StubConnection connection = new StubConnection(connections.size() + 1);
                    connections.add(connection);
                    Thread handler = new Thread(() -> handle(socket, connection),
                            "stub-upstream-" + connection.number);
                    handler.setDaemon(true);
                    handler.start();
                }
            } catch (IOException e) {
                // The test closed the socket; the accept loop is over.
            }
        }

        private void handle(Socket socket, StubConnection connection) {
            try (socket) {
                socket.setSoTimeout(30_000);
                InputStream in = socket.getInputStream();
                String head = readHead(in);
                if (head.isEmpty()) {
                    return;
                }
                String[] line = head.split("\r\n", 2)[0].split(" ");
                connection.request = line.length > 1 ? line[0] + " " + line[1] : head;
                int contentLength = contentLength(head);
                if (contentLength > 0) {
                    in.readNBytes(contentLength);
                }
                if (connection.number == 1) {
                    socket.getOutputStream().write('H');
                    socket.getOutputStream().flush();
                    connection.note = "answered one byte and closed";
                    return;
                }
                if (hangAfterTheFirstFailure) {
                    connection.note = "holding the connection open";
                    if (in.read() < 0) {
                        connection.note = "closed by the proxy";
                    }
                    connection.closedByPeer.countDown();
                    return;
                }
                respond(socket, "{\"path\":\"" + line[1] + "\"}");
                connection.note = "answered 200";
            } catch (IOException e) {
                connection.note = "failed: " + e;
            }
        }

        /// @return whether `count` requests have arrived yet
        boolean awaitRequests(int count, long timeout, TimeUnit unit) throws InterruptedException {
            long deadline = System.nanoTime() + unit.toNanos(timeout);
            while (System.nanoTime() < deadline) {
                if (connections.size() >= count && connections.get(count - 1).request != null) {
                    return true;
                }
                Thread.sleep(25);
            }
            return false;
        }

        /// The connection the last request arrived on — captured before the
        /// cancellation, because a closed connection is no guide to which one was
        /// hanging.
        StubConnection lastConnection() {
            return connections.get(connections.size() - 1);
        }

        /// @return the request lines the stub saw, in the order they arrived
        List<String> requests() {
            List<String> lines = new ArrayList<>();
            connections.forEach(connection -> {
                if (connection.request != null) {
                    lines.add(connection.request);
                }
            });
            return lines;
        }

        /// The whole exchange, for an assertion message that says what happened.
        String log() {
            StringBuilder log = new StringBuilder("the instance saw: [port=" + listen.getLocalPort() + "]");
            connections.forEach(connection -> log.append("\n  #").append(connection.number)
                    .append(' ').append(connection.request)
                    .append(" — ").append(connection.note));
            return log.toString();
        }

        @Override
        public void close() throws IOException {
            listen.close();
        }

        private static String readHead(InputStream in) throws IOException {
            StringBuilder head = new StringBuilder();
            int read;
            while ((read = in.read()) >= 0) {
                head.append((char) read);
                if (head.length() >= 4 && head.substring(head.length() - 4).equals("\r\n\r\n")) {
                    break;
                }
            }
            return head.toString();
        }

        private static int contentLength(String head) {
            for (String line : head.split("\r\n")) {
                int colon = line.indexOf(':');
                if (colon > 0 && line.substring(0, colon).toLowerCase(Locale.ROOT).equals("content-length")) {
                    try {
                        return Integer.parseInt(line.substring(colon + 1).trim());
                    } catch (NumberFormatException e) {
                        return 0;
                    }
                }
            }
            return 0;
        }

        private static void respond(Socket socket, String body) throws IOException {
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            OutputStream out = socket.getOutputStream();
            out.write(("HTTP/1.1 200 OK\r\n"
                    + "Content-Type: application/json\r\n"
                    + "Content-Length: " + bytes.length + "\r\n"
                    + "Connection: close\r\n\r\n").getBytes(StandardCharsets.US_ASCII));
            out.write(bytes);
            out.flush();
        }

        /// One connection to the stub, and what became of it.
        static final class StubConnection {

            final int number;
            final CountDownLatch closedByPeer = new CountDownLatch(1);
            volatile String request;
            volatile String note = "connected";

            StubConnection(int number) {
                this.number = number;
            }

            /// @return whether the proxy let go of the connection within the
            ///         timeout
            boolean awaitClosedByPeer(long timeout, TimeUnit unit) throws InterruptedException {
                return closedByPeer.await(timeout, unit);
            }
        }
    }
}