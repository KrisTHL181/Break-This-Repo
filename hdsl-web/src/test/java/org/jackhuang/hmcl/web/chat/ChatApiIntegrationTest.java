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
package org.jackhuang.hmcl.web.chat;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.jackhuang.hmcl.dsh.DshAccount;
import org.jackhuang.hmcl.setting.SettingsManager;
import org.jackhuang.hmcl.web.TestSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// 一次真正的端到端：面板重启后的 SQLite、REST、turn 线程与 OpenAI SSE 解析
/// 全部走一遍，而不是只测零件。
class ChatApiIntegrationTest {

    @Test
    void aMessageStreamsThroughARESTTurnAndLandsInTheStore(@TempDir Path dataDir) throws Exception {
        HttpServer provider = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        provider.createContext("/v1/chat/completions", ChatApiIntegrationTest::stream);
        provider.start();
        try (TestSupport.RunningServer running = TestSupport.start(dataDir, Map.of(),
                config -> {
                    config.auth.disabled = true;
                    config.bindHost = "127.0.0.1";
                })) {
            SettingsManager.settings().getAccounts().add(new DshAccount(
                    DshAccount.AccountKind.THIRD_PARTY, "fake", "test-key",
                    "http://127.0.0.1:" + provider.getAddress().getPort() + "/v1",
                    null, "fake-model", null));

            JsonObject session = post(running, "/api/chat/sessions", new JsonObject());
            String sessionId = session.get("id").getAsString();
            assertNotNull(sessionId);

            JsonObject body = new JsonObject();
            body.addProperty("text", "hello");
            JsonObject outcome = post(running, "/api/chat/sessions/" + sessionId + "/messages", body);
            assertTrue(outcome.has("turnId"));

            String content = waitForContent(running, sessionId, Duration.ofSeconds(15));
            assertEquals("hello there", content);

            JsonObject detail = get(running, "/api/chat/sessions/" + sessionId);
            JsonArray nodes = detail.getAsJsonArray("nodes");
            assertEquals(2, nodes.size());
            assertEquals("user", nodes.get(0).getAsJsonObject().get("role").getAsString());
            assertEquals("assistant", nodes.get(1).getAsJsonObject().get("role").getAsString());

            HttpRequest shell = HttpRequest.newBuilder(URI.create(running.baseUrl() + "/chat"))
                    .GET().build();
            HttpResponse<String> page = TestSupport.client().send(shell,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            assertEquals(200, page.statusCode());
            assertTrue(page.body().contains("id=\"root\""), "the SPA shell must answer /chat");
        } finally {
            provider.stop(0);
        }
    }

    @Test
    void textAttachmentDownloadDeclaresItsCharset(@TempDir Path dataDir) throws Exception {
        try (TestSupport.RunningServer running = TestSupport.start(dataDir, Map.of(),
                config -> {
                    config.auth.disabled = true;
                    config.bindHost = "127.0.0.1";
                })) {
            JsonObject session = post(running, "/api/chat/sessions", new JsonObject());
            String sessionId = session.get("id").getAsString();
            String text = "粘贴的长文本：中文、emoji 🙂 和换行\n第二行";
            String boundary = "----hdslTestBoundary";
            String multipart = "--" + boundary + "\r\n"
                    + "Content-Disposition: form-data; name=\"file\"; filename=\"pasted.txt\"\r\n"
                    + "Content-Type: text/plain\r\n\r\n"
                    + text + "\r\n--" + boundary + "--\r\n";
            HttpRequest upload = HttpRequest.newBuilder(URI.create(running.baseUrl()
                            + "/api/chat/attachments?sessionId=" + sessionId))
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .POST(HttpRequest.BodyPublishers.ofString(multipart, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> uploaded = TestSupport.client().send(upload,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            assertEquals(201, uploaded.statusCode(), uploaded.body());
            String id = JsonParser.parseString(uploaded.body()).getAsJsonObject().get("id").getAsString();

            HttpRequest download = HttpRequest.newBuilder(URI.create(running.baseUrl()
                    + "/api/chat/attachments/" + id)).GET().build();
            HttpResponse<String> response = TestSupport.client().send(download,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            assertEquals(200, response.statusCode());
            String contentType = response.headers().firstValue("Content-Type").orElse("");
            assertTrue(contentType.toLowerCase().contains("charset=utf-8"), contentType);
            assertEquals(text, response.body());
        }
    }

    @Test
    void attachmentDownloadCanForceAttachmentDisposition(@TempDir Path dataDir) throws Exception {
        try (TestSupport.RunningServer running = TestSupport.start(dataDir, Map.of(),
                config -> {
                    config.auth.disabled = true;
                    config.bindHost = "127.0.0.1";
                })) {
            String id = uploadText(running, "hello 世界");
            HttpRequest request = HttpRequest.newBuilder(URI.create(running.baseUrl()
                            + "/api/chat/attachments/" + id + "?download=1"))
                    .GET().build();
            HttpResponse<String> response = TestSupport.client().send(request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            assertEquals(200, response.statusCode());
            String disposition = response.headers().firstValue("Content-Disposition").orElse("");
            assertTrue(disposition.startsWith("attachment;"), disposition);
        }
    }

    @Test
    void attachmentDownloadHonoursASingleByteRange(@TempDir Path dataDir) throws Exception {
        try (TestSupport.RunningServer running = TestSupport.start(dataDir, Map.of(),
                config -> {
                    config.auth.disabled = true;
                    config.bindHost = "127.0.0.1";
                })) {
            String id = uploadText(running, "0123456789");
            HttpRequest request = HttpRequest.newBuilder(URI.create(running.baseUrl()
                            + "/api/chat/attachments/" + id))
                    .header("Range", "bytes=2-5")
                    .GET().build();
            HttpResponse<String> response = TestSupport.client().send(request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            assertEquals(206, response.statusCode());
            assertEquals("2345", response.body());
            assertEquals("bytes 2-5/10",
                    response.headers().firstValue("Content-Range").orElse(""));
            assertEquals("bytes", response.headers().firstValue("Accept-Ranges").orElse(""));
        }
    }

    @Test
    void attachmentTextEndpointPaginatesByCharacters(@TempDir Path dataDir) throws Exception {
        try (TestSupport.RunningServer running = TestSupport.start(dataDir, Map.of(),
                config -> {
                    config.auth.disabled = true;
                    config.bindHost = "127.0.0.1";
                })) {
            String id = uploadText(running, "ABCDEFGHIJ");
            JsonObject first = get(running,
                    "/api/chat/attachments/" + id + "/text?offset=2&limit=3");
            assertEquals("CDE", first.get("text").getAsString());
            assertEquals(5, first.get("nextOffset").getAsInt());
            assertTrue(first.get("hasMore").getAsBoolean());

            JsonObject last = get(running,
                    "/api/chat/attachments/" + id + "/text?offset=8&limit=3");
            assertEquals("IJ", last.get("text").getAsString());
            assertEquals(10, last.get("nextOffset").getAsInt());
            assertTrue(!last.get("hasMore").getAsBoolean());
            assertEquals("UTF-8", last.get("charset").getAsString());
        }
    }

    private static String uploadText(TestSupport.RunningServer running, String content) throws Exception {
        JsonObject session = post(running, "/api/chat/sessions", new JsonObject());
        String sessionId = session.get("id").getAsString();
        String boundary = "----hdslTestBoundary";
        String multipart = "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"file\"; filename=\"pasted.txt\"\r\n"
                + "Content-Type: text/plain\r\n\r\n"
                + content + "\r\n--" + boundary + "--\r\n";
        HttpRequest upload = HttpRequest.newBuilder(URI.create(running.baseUrl()
                        + "/api/chat/attachments?sessionId=" + sessionId))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofString(multipart, StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> uploaded = TestSupport.client().send(upload,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(201, uploaded.statusCode(), uploaded.body());
        return JsonParser.parseString(uploaded.body()).getAsJsonObject().get("id").getAsString();
    }

    private static void stream(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().add("Content-Type", "text/event-stream");
        exchange.sendResponseHeaders(200, 0);
        try (var output = exchange.getResponseBody()) {
            output.write(("data: {\"choices\":[{\"delta\":{\"content\":\"hello \"},\"finish_reason\":null}]}\n\n")
                    .getBytes(StandardCharsets.UTF_8));
            output.flush();
            output.write(("data: {\"choices\":[{\"delta\":{\"content\":\"there\"},\"finish_reason\":\"stop\"}],"
                    + "\"usage\":{\"prompt_tokens\":2,\"completion_tokens\":2}}\n\n")
                    .getBytes(StandardCharsets.UTF_8));
            output.write("data: [DONE]\n\n".getBytes(StandardCharsets.UTF_8));
            output.flush();
        }
    }

    private static String waitForContent(TestSupport.RunningServer running, String sessionId,
                                         Duration timeout) throws Exception {
        long deadline = System.nanoTime() + timeout.toNanos();
        String last = "";
        while (System.nanoTime() < deadline) {
            JsonObject detail = get(running, "/api/chat/sessions/" + sessionId);
            JsonArray nodes = detail.getAsJsonArray("nodes");
            if (nodes.size() >= 2) {
                JsonArray variants = nodes.get(1).getAsJsonObject().getAsJsonArray("variants");
                if (variants.size() > 0) {
                    JsonObject message = variants.get(0).getAsJsonObject();
                    last = message.get("content").getAsString();
                    if ("done".equals(message.get("status").getAsString())) {
                        return last;
                    }
                    if ("error".equals(message.get("status").getAsString())) {
                        throw new AssertionError("turn failed: " + message);
                    }
                }
            }
            Thread.sleep(100);
        }
        throw new AssertionError("turn did not finish; last content: " + last);
    }

    private static JsonObject get(TestSupport.RunningServer running, String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(running.baseUrl() + path))
                .timeout(Duration.ofSeconds(10)).GET().build();
        HttpResponse<String> response = TestSupport.client().send(request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(200, response.statusCode(), response.body());
        return JsonParser.parseString(response.body()).getAsJsonObject();
    }

    private static JsonObject post(TestSupport.RunningServer running, String path, JsonObject body)
            throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(running.baseUrl() + path))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> response = TestSupport.client().send(request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertTrue(response.statusCode() >= 200 && response.statusCode() < 300, response.body());
        return JsonParser.parseString(response.body()).getAsJsonObject();
    }
}