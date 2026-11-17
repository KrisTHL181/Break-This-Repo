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
package org.jackhuang.hmcl.web.http;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.ee10.servlet.FilterHolder;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.servlet.ServletHolder;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// A servlet that lets an exception escape used to hand the caller Jetty's
/// HTML error page; [ApiGuardFilter] makes it the JSON shape the API contract
/// promises.
class ApiGuardFilterTest {

    @Test
    void uncaughtExceptionBecomesJson500() throws Exception {
        Server server = new Server(0);
        ServletContextHandler context = new ServletContextHandler();
        context.setContextPath("/");
        context.addServlet(new ServletHolder(new HttpServlet() {
            @Override
            protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
                throw new NullPointerException("boom");
            }
        }), "/api/throw");
        context.addFilter(new FilterHolder(new ApiGuardFilter()), "/api/*",
                EnumSet.of(DispatcherType.REQUEST));
        server.setHandler(context);
        server.start();
        try {
            int port = ((ServerConnector) server.getConnectors()[0]).getLocalPort();
            HttpResponse<String> response = HttpClient.newHttpClient().send(
                    HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/throw")).GET().build(),
                    HttpResponse.BodyHandlers.ofString());
            assertEquals(500, response.statusCode());
            assertEquals("{\"error\":\"internal error\"}", response.body());
            assertTrue(response.headers().firstValue("Content-Type").orElse("")
                    .startsWith("application/json"));
        } finally {
            server.stop();
        }
    }

    /// The cap exists because every JSON handler reads its whole body into memory. It must
    /// not swallow the real uploads on the same mapping — a pack is documented at 200 MiB,
    /// a plugin at 50 MiB — but the exemption is the **route**, not the `Content-Type`: a
    /// caller who labels a body `multipart/form-data` must not decide for himself that the
    /// cap does not apply.
    ///
    /// The refused cases announce a 2 MiB body but send none: the filter answers from the
    /// declared length, and a server that answers 413 while the client is still writing the
    /// body can close the connection under it. The handler case drains the body first for
    /// the same reason.
    @Test
    void theBodyCapFollowsTheRouteAndNotTheContentType() throws Exception {
        Server server = new Server(0);
        ServletContextHandler context = new ServletContextHandler();
        context.setContextPath("/");
        context.addServlet(new ServletHolder(new HttpServlet() {
            @Override
            protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
                // Read it all before answering: a response written while the client is still
                // sending lets the connection die under the request.
                req.getInputStream().transferTo(OutputStream.nullOutputStream());
                resp.setStatus(200);
                resp.getWriter().write("reached");
            }

            @Override
            protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
                doPost(req, resp);
            }
        }), "/api/*");
        context.addFilter(new FilterHolder(new ApiGuardFilter()), "/api/*",
                EnumSet.of(DispatcherType.REQUEST));
        server.setHandler(context);
        server.start();
        try {
            int port = ((ServerConnector) server.getConnectors()[0]).getLocalPort();
            int announced = 2 * 1024 * 1024;

            assertTrue(statusLineOnly(port, "/api/upload", "Content-Type: application/json; charset=utf-8",
                            announced).startsWith("HTTP/1.1 413"),
                    "a 2 MiB JSON body must be refused before the handler");

            // The handlers parse whatever they are handed, so a body mislabelled as text must
            // not slip past the cap.
            assertTrue(statusLineOnly(port, "/api/upload", "Content-Type: text/plain", announced)
                            .startsWith("HTTP/1.1 413"),
                    "mislabelling the body must not bypass the cap");

            // The same 2 MiB labelled multipart on a JSON route is still refused: the route
            // decides, not the header the caller chose.
            assertTrue(statusLineOnly(port, "/api/upload",
                            "Content-Type: multipart/form-data; boundary=XyZ", announced)
                            .startsWith("HTTP/1.1 413"),
                    "a multipart label must not exempt a JSON route");

            // …while the real upload routes stream through.
            HttpResponse<String> multipart = HttpClient.newHttpClient().send(HttpRequest.newBuilder(
                            URI.create("http://127.0.0.1:" + port + "/api/packs/install"))
                    .header("Content-Type", "multipart/form-data; boundary=XyZ")
                    .POST(HttpRequest.BodyPublishers.ofByteArray(multipartBody(announced)))
                    .build(), HttpResponse.BodyHandlers.ofString());
            assertEquals(200, multipart.statusCode(), "a 2 MiB multipart upload must reach the handler");

            HttpResponse<String> world = HttpClient.newHttpClient().send(HttpRequest.newBuilder(
                            URI.create("http://127.0.0.1:" + port + "/api/mc/instances/x/world/import"))
                    .header("Content-Type", "application/zip")
                    .POST(HttpRequest.BodyPublishers.ofByteArray(new byte[announced]))
                    .build(), HttpResponse.BodyHandlers.ofString());
            assertEquals(200, world.statusCode(), "a 2 MiB world zip must reach the handler");

            // The plugin/mod upload is a raw jar on a PUT. Before this route was exempt, any
            // real plugin (over 1 MiB) died here with 413 and never reached `McMods.upload`.
            HttpResponse<String> mod = HttpClient.newHttpClient().send(HttpRequest.newBuilder(
                            URI.create("http://127.0.0.1:" + port
                                    + "/api/mc/instances/x/files/mods?name=example.jar"))
                    .header("Content-Type", "application/java-archive")
                    .PUT(HttpRequest.BodyPublishers.ofByteArray(new byte[announced]))
                    .build(), HttpResponse.BodyHandlers.ofString());
            assertEquals(200, mod.statusCode(), "a 2 MiB plugin upload must reach the handler");
        } finally {
            server.stop();
        }
    }

    /// `Content-Length` is absent on a chunked request and `getContentLengthLong()` answers
    /// -1; before this rule the check was `> 1 MiB`, so a chunked body of any size passed.
    @Test
    void aChunkedBodyWithoutLengthIsRefusedOnAJsonRoute() throws Exception {
        Server server = new Server(0);
        ServletContextHandler context = new ServletContextHandler();
        context.setContextPath("/");
        context.addServlet(new ServletHolder(new HttpServlet() {
            @Override
            protected void doPost(HttpServletRequest req, HttpServletResponse resp) {
                resp.setStatus(200);
            }
        }), "/api/*");
        context.addFilter(new FilterHolder(new ApiGuardFilter()), "/api/*",
                EnumSet.of(DispatcherType.REQUEST));
        server.setHandler(context);
        server.start();
        try {
            int port = ((ServerConnector) server.getConnectors()[0]).getLocalPort();
            assertTrue(statusLineOnlyChunked(port, "/api/upload").startsWith("HTTP/1.1 411"),
                    "an unknown-length body on a JSON route must be refused, not trusted");
        } finally {
            server.stop();
        }
    }

    @Test
    void onlyUploadRoutesSkipTheJsonCap() {
        assertTrue(ApiGuardFilter.streamed("/api/settings/tls"));
        assertTrue(ApiGuardFilter.streamed("/api/wallpapers"));
        assertTrue(ApiGuardFilter.streamed("/api/wallpapers/3"));
        assertTrue(ApiGuardFilter.streamed("/api/packs/install"));
        assertTrue(ApiGuardFilter.streamed("/api/plugins/install"));
        assertTrue(ApiGuardFilter.streamed("/api/instances/abc/plugins/install"));
        assertTrue(ApiGuardFilter.streamed("/api/mc/instances/abc/world/import"));
        assertTrue(ApiGuardFilter.streamed("/api/mc/instances/abc/files/mods"));
        assertTrue(ApiGuardFilter.streamed("/api/mc/instances/abc/files/plugins"));
        assertTrue(ApiGuardFilter.streamed("/api/chat/attachments"));
        assertTrue(ApiGuardFilter.streamed("/api/chat/avatars"));
    }

    @Test
    void jsonRoutesStayUnderTheCap() {
        assertFalse(ApiGuardFilter.streamed("/api/settings/logs"));
        assertFalse(ApiGuardFilter.streamed("/api/settings/registry"));
        assertFalse(ApiGuardFilter.streamed("/api/auth/login"));
        assertFalse(ApiGuardFilter.streamed("/api/mc/instances"));
        assertFalse(ApiGuardFilter.streamed("/api/mc/instances/abc/backups"));
        assertFalse(ApiGuardFilter.streamed("/api/mc/instances/abc/start"));
        assertFalse(ApiGuardFilter.streamed("/api/instances"));
        assertFalse(ApiGuardFilter.streamed("/api/wallpapers-backup"));
        assertFalse(ApiGuardFilter.streamed("/api/chat/sessions/abc/messages"));
    }

    @Test
    void onlyMethodsWithBodiesRequireALength() {
        assertTrue(ApiGuardFilter.expectsBody("POST"));
        assertTrue(ApiGuardFilter.expectsBody("PUT"));
        assertTrue(ApiGuardFilter.expectsBody("PATCH"));
        assertFalse(ApiGuardFilter.expectsBody("GET"));
        assertFalse(ApiGuardFilter.expectsBody("HEAD"));
        assertFalse(ApiGuardFilter.expectsBody("DELETE"));
    }

    /// Sends the request line and headers only, then reads the status line: the body the
    /// headers announce is never written, so nothing races the answer.
    private static String statusLineOnly(int port, String path, String contentType, int announcedBytes)
            throws IOException {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("127.0.0.1", port), 5_000);
            socket.setSoTimeout(5_000);
            String request = "POST " + path + " HTTP/1.1\r\n"
                    + "Host: 127.0.0.1\r\n"
                    + contentType + "\r\n"
                    + "Content-Length: " + announcedBytes + "\r\n"
                    + "Connection: close\r\n\r\n";
            socket.getOutputStream().write(request.getBytes(StandardCharsets.US_ASCII));
            socket.getOutputStream().flush();
            return new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.US_ASCII))
                    .readLine();
        }
    }

    /// Same, but with `Transfer-Encoding: chunked` and no body at all.
    private static String statusLineOnlyChunked(int port, String path) throws IOException {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("127.0.0.1", port), 5_000);
            socket.setSoTimeout(5_000);
            String request = "POST " + path + " HTTP/1.1\r\n"
                    + "Host: 127.0.0.1\r\n"
                    + "Content-Type: application/json\r\n"
                    + "Transfer-Encoding: chunked\r\n"
                    + "Connection: close\r\n\r\n";
            socket.getOutputStream().write(request.getBytes(StandardCharsets.US_ASCII));
            socket.getOutputStream().flush();
            return new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.US_ASCII))
                    .readLine();
        }
    }

    private static byte[] multipartBody(int payloadBytes) {
        byte[] head = ("--XyZ\r\nContent-Disposition: form-data; name=\"file\"; filename=\"x.dspack\"\r\n"
                + "Content-Type: application/octet-stream\r\n\r\n").getBytes(StandardCharsets.UTF_8);
        byte[] tail = "\r\n--XyZ--\r\n".getBytes(StandardCharsets.UTF_8);
        byte[] body = new byte[head.length + payloadBytes + tail.length];
        System.arraycopy(head, 0, body, 0, head.length);
        System.arraycopy(tail, 0, body, head.length + payloadBytes, tail.length);
        return body;
    }
}
