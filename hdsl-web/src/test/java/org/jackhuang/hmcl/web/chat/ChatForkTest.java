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
import static org.junit.jupiter.api.Assertions.assertTrue;

/// 分叉是"真的复制"：节点、变体、附件行都新一份，原会话不动；目标助手可以换。
/// 附件文件是内容寻址共享的，删除任一侧都不能把另一侧的文件剪掉。
class ChatForkTest {

    @Test
    void forksAWholeSessionIntoAnotherAssistant(@TempDir Path dataDir) throws Exception {
        HttpServer provider = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        provider.createContext("/v1/chat/completions", ChatForkTest::stream);
        provider.start();
        DshAccount account = new DshAccount(
                DshAccount.AccountKind.THIRD_PARTY, "fork-fake", "test-key",
                "http://127.0.0.1:" + provider.getAddress().getPort() + "/v1",
                null, "fake-model", null);
        SettingsManager.settings().getAccounts().add(account);
        try (TestSupport.RunningServer running = TestSupport.start(dataDir, Map.of(),
                config -> {
                    config.auth.disabled = true;
                    config.bindHost = "127.0.0.1";
                })) {
            JsonObject create = new JsonObject();
            create.addProperty("assistantId", "default");
            create.addProperty("account", "fork-fake");
            create.addProperty("model", "fake-model");
            String sessionId = post(running, "/api/chat/sessions", create).get("id").getAsString();

            String attachmentId = uploadText(running, sessionId, "附件正文");
            JsonObject send = new JsonObject();
            send.addProperty("text", "hello");
            JsonArray attachments = new JsonArray();
            attachments.add(attachmentId);
            send.add("attachments", attachments);
            post(running, "/api/chat/sessions/" + sessionId + "/messages", send);
            assertEquals("hello there", waitForContent(running, sessionId, Duration.ofSeconds(15)));

            JsonObject target = new JsonObject();
            target.addProperty("name", "分叉目标");
            String targetId = post(running, "/api/chat/assistants", target).get("id").getAsString();

            JsonObject forkBody = new JsonObject();
            forkBody.addProperty("assistantId", targetId);
            JsonObject fork = post(running, "/api/chat/sessions/" + sessionId + "/fork", forkBody);

            assertEquals(targetId, fork.get("assistantId").getAsString());
            assertTrue(fork.get("title").getAsString().endsWith("（分叉）"),
                    fork.get("title").getAsString());
            assertEquals(2, fork.getAsJsonArray("nodes").size());

            // 正文逐节点一致（active variant）。
            JsonObject source = get(running, "/api/chat/sessions/" + sessionId);
            assertEquals(activeContents(source).toString(), activeContents(fork).toString(),
                    "forked transcript must equal the source");

            // 附件行也被复制：从副本下载，内容与源一致。
            JsonArray userVariants = fork.getAsJsonArray("nodes").get(0).getAsJsonObject()
                    .getAsJsonArray("variants");
            JsonObject userMessage = userVariants.get(0).getAsJsonObject();
            String copiedAttachment = userMessage.getAsJsonArray("attachments").get(0)
                    .getAsJsonObject().get("id").getAsString();
            assertTrue(!"".equals(copiedAttachment));
            assertEquals("附件正文", downloadText(running, copiedAttachment));

            // 不指定助手时分叉回同一个助手，原会话保持原样。
            JsonObject sameAssistant = post(running,
                    "/api/chat/sessions/" + sessionId + "/fork", new JsonObject());
            assertEquals("default", sameAssistant.get("assistantId").getAsString());
            assertEquals(2, get(running, "/api/chat/sessions/" + sessionId)
                    .getAsJsonArray("nodes").size());
        } finally {
            SettingsManager.settings().getAccounts().remove(account);
            provider.stop(0);
        }
    }

    // ---------------------------------------------------------------- helpers --

    private static String activeContents(JsonObject session) {
        JsonArray out = new JsonArray();
        for (var nodeElement : session.getAsJsonArray("nodes")) {
            JsonObject node = nodeElement.getAsJsonObject();
            String active = node.get("activeVariantId").getAsString();
            for (var variant : node.getAsJsonArray("variants")) {
                JsonObject message = variant.getAsJsonObject();
                if (message.get("id").getAsString().equals(active)) {
                    out.add(message.get("content").getAsString());
                    break;
                }
            }
        }
        return out.toString();
    }

    private static String uploadText(TestSupport.RunningServer running, String sessionId, String text)
            throws Exception {
        String boundary = "----hdslForkBoundary";
        String multipart = "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"file\"; filename=\"fork.txt\"\r\n"
                + "Content-Type: text/plain\r\n\r\n"
                + text + "\r\n--" + boundary + "--\r\n";
        HttpRequest request = HttpRequest.newBuilder(URI.create(running.baseUrl()
                        + "/api/chat/attachments?sessionId=" + sessionId))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofString(multipart, StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> response = TestSupport.client().send(request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(201, response.statusCode(), response.body());
        return JsonParser.parseString(response.body()).getAsJsonObject().get("id").getAsString();
    }

    private static String downloadText(TestSupport.RunningServer running, String attachmentId)
            throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(running.baseUrl()
                + "/api/chat/attachments/" + attachmentId)).GET().build();
        HttpResponse<String> response = TestSupport.client().send(request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(200, response.statusCode(), response.body());
        return response.body();
    }

    private static void stream(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().add("Content-Type", "text/event-stream");
        exchange.sendResponseHeaders(200, 0);
        try (var output = exchange.getResponseBody()) {
            output.write(("data: {\"choices\":[{\"delta\":{\"content\":\"hello \"},"
                    + "\"finish_reason\":null}]}\n\n").getBytes(StandardCharsets.UTF_8));
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
            JsonArray nodes = get(running, "/api/chat/sessions/" + sessionId).getAsJsonArray("nodes");
            if (nodes.size() >= 2) {
                JsonArray variants = nodes.get(1).getAsJsonObject().getAsJsonArray("variants");
                if (variants.size() > 0) {
                    JsonObject message = variants.get(0).getAsJsonObject();
                    last = message.get("content").getAsString();
                    String status = message.get("status").getAsString();
                    if ("done".equals(status)) {
                        return last;
                    }
                    if ("error".equals(status)) {
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