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
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// P1：助手提示词在会话创建时冻结。之后改助手只影响新会话，老会话保持原行为。
class ChatPromptSnapshotTest {

    @Test
    void changingAnAssistantDoesNotRewriteOldSessions(@TempDir Path dataDir) throws Exception {
        AtomicReference<String> lastSystemPrompt = new AtomicReference<>();
        HttpServer provider = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        provider.createContext("/v1/chat/completions", exchange -> {
            lastSystemPrompt.set(systemPromptOf(exchange));
            stream(exchange);
        });
        provider.start();
        DshAccount account = new DshAccount(
                DshAccount.AccountKind.THIRD_PARTY, "snapshot-fake", "test-key",
                "http://127.0.0.1:" + provider.getAddress().getPort() + "/v1",
                null, "fake-model", null);
        SettingsManager.settings().getAccounts().add(account);
        try (TestSupport.RunningServer running = TestSupport.start(dataDir, Map.of(),
                config -> {
                    config.auth.disabled = true;
                    config.bindHost = "127.0.0.1";
                })) {
            JsonObject draft = new JsonObject();
            draft.addProperty("name", "快照助手");
            draft.addProperty("systemPrompt", "冻结的提示词");
            String assistantId = post(running, "/api/chat/assistants", draft)
                    .get("id").getAsString();

            String oldSession = createSession(running, assistantId);
            JsonObject patch = new JsonObject();
            patch.addProperty("systemPrompt", "新的提示词");
            assertEquals(200, patch(running, "/api/chat/assistants/" + assistantId, patch)
                    .statusCode());

            send(running, oldSession, "hi");
            waitForDone(running, oldSession);
            assertTrue(lastSystemPrompt.get().startsWith("冻结的提示词"),
                    "the old session must keep the prompt it was created with: "
                            + lastSystemPrompt.get());

            String newSession = createSession(running, assistantId);
            send(running, newSession, "hi");
            waitForDone(running, newSession);
            assertTrue(lastSystemPrompt.get().startsWith("新的提示词"),
                    "a new session takes the assistant's current prompt: "
                            + lastSystemPrompt.get());
        } finally {
            SettingsManager.settings().getAccounts().remove(account);
            provider.stop(0);
        }
    }

    // ---------------------------------------------------------------- helpers --

    /// Reads the request body and returns the system message's content.
    private static String systemPromptOf(HttpExchange exchange) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        try {
            JsonArray messages = JsonParser.parseString(body).getAsJsonObject()
                    .getAsJsonArray("messages");
            for (var element : messages) {
                JsonObject message = element.getAsJsonObject();
                if ("system".equals(message.get("role").getAsString())) {
                    return message.get("content").getAsString();
                }
            }
        } catch (RuntimeException ignored) {
            // A malformed request is a test failure elsewhere.
        }
        return "";
    }

    private static void stream(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().add("Content-Type", "text/event-stream");
        exchange.sendResponseHeaders(200, 0);
        try (var output = exchange.getResponseBody()) {
            output.write(("data: {\"choices\":[{\"delta\":{\"content\":\"ok\"},"
                    + "\"finish_reason\":\"stop\"}],\"usage\":{\"prompt_tokens\":2,"
                    + "\"completion_tokens\":1}}\n\n").getBytes(StandardCharsets.UTF_8));
            output.write("data: [DONE]\n\n".getBytes(StandardCharsets.UTF_8));
            output.flush();
        }
    }

    private static String createSession(TestSupport.RunningServer running, String assistantId)
            throws Exception {
        JsonObject body = new JsonObject();
        body.addProperty("assistantId", assistantId);
        body.addProperty("account", "snapshot-fake");
        body.addProperty("model", "fake-model");
        return post(running, "/api/chat/sessions", body).get("id").getAsString();
    }

    private static void send(TestSupport.RunningServer running, String sessionId, String text)
            throws Exception {
        JsonObject body = new JsonObject();
        body.addProperty("text", text);
        post(running, "/api/chat/sessions/" + sessionId + "/messages", body);
    }

    private static void waitForDone(TestSupport.RunningServer running, String sessionId)
            throws Exception {
        long deadline = System.nanoTime() + Duration.ofSeconds(15).toNanos();
        while (System.nanoTime() < deadline) {
            JsonObject detail = get(running, "/api/chat/sessions/" + sessionId);
            JsonArray nodes = detail.getAsJsonArray("nodes");
            if (nodes.size() >= 2) {
                JsonArray variants = nodes.get(1).getAsJsonObject().getAsJsonArray("variants");
                if (!variants.isEmpty()) {
                    String status = variants.get(0).getAsJsonObject().get("status").getAsString();
                    if ("done".equals(status)) {
                        return;
                    }
                    if ("error".equals(status)) {
                        throw new AssertionError("turn failed: " + variants.get(0));
                    }
                }
            }
            Thread.sleep(100);
        }
        throw new AssertionError("turn did not finish");
    }

    private static JsonObject get(TestSupport.RunningServer running, String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(running.baseUrl() + path))
                .timeout(Duration.ofSeconds(10)).GET().build();
        HttpResponse<String> response = TestSupport.client().send(request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(200, response.statusCode(), response.body());
        return JsonParser.parseString(response.body()).getAsJsonObject();
    }

    private static HttpResponse<String> patch(TestSupport.RunningServer running, String path,
                                              JsonObject body) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(running.baseUrl() + path))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .method("PATCH", HttpRequest.BodyPublishers.ofString(body.toString(),
                        StandardCharsets.UTF_8))
                .build();
        return TestSupport.client().send(request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
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