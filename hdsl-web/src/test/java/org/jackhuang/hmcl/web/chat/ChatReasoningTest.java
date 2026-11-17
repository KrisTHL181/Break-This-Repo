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

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// 思考挡位要落到各家自己的参数上：OpenAI 是 reasoning_effort，百炼是
/// enable_thinking/thinking_budget，智谱是 thinking.type，Anthropic 是
/// thinking.budget_tokens（还必须 max_tokens > budget 且不能带 temperature）。
class ChatReasoningTest {

    @Test
    void genericOpenAiEndpointGetsReasoningEffort() throws Exception {
        JsonObject body = capture("v1", ChatEndpoints.OPENAI_COMPLETIONS, "gpt-5.4-mini", "high");
        assertEquals("high", body.get("reasoning_effort").getAsString());
    }

    @Test
    void dashScopeGetsEnableThinkingAndBudget() throws Exception {
        JsonObject body = capture("dashscope/compatible-mode/v1",
                ChatEndpoints.OPENAI_COMPLETIONS, "qwen3.8-flash", "medium");
        assertTrue(body.get("enable_thinking").getAsBoolean());
        assertEquals(4096, body.get("thinking_budget").getAsInt());
        assertFalse(body.has("reasoning_effort"));
    }

    @Test
    void zhipuGetsThinkingObject() throws Exception {
        JsonObject on = capture("bigmodel/api/paas/v4",
                ChatEndpoints.OPENAI_COMPLETIONS, "glm-5.2", "low");
        assertEquals("enabled", on.getAsJsonObject("thinking").get("type").getAsString());

        JsonObject off = capture("bigmodel/api/paas/v4",
                ChatEndpoints.OPENAI_COMPLETIONS, "glm-5.2", "off");
        assertEquals("disabled", off.getAsJsonObject("thinking").get("type").getAsString());
    }

    @Test
    void offDoesNotPushUnknownFieldsToGenericEndpoints() throws Exception {
        JsonObject body = capture("v1", ChatEndpoints.OPENAI_COMPLETIONS, "qwen3.8-flash", "off");
        assertFalse(body.has("reasoning_effort"));
        assertFalse(body.has("enable_thinking"));
        assertFalse(body.has("thinking"));
    }

    @Test
    void anthropicThinkingCarriesBudgetAndDropsTemperature() throws Exception {
        JsonObject body = capture("v1", ChatEndpoints.ANTHROPIC_MESSAGES,
                "claude-sonnet-4-5", "high", 42.0, 2_000);
        JsonObject thinking = body.getAsJsonObject("thinking");
        assertEquals("enabled", thinking.get("type").getAsString());
        int budget = thinking.get("budget_tokens").getAsInt();
        assertEquals(16_384, budget);
        assertTrue(body.get("max_tokens").getAsInt() > budget);
        assertFalse(body.has("temperature"));

        JsonObject off = capture("v1", ChatEndpoints.ANTHROPIC_MESSAGES,
                "claude-sonnet-4-5", "off", 42.0, 2_000);
        assertFalse(off.has("thinking"));
        assertEquals(42.0, off.get("temperature").getAsDouble());
    }

    @Test
    void declaredEffortsAreClampedAndRespected() throws Exception {
        ChatCapabilities.ReasoningMode onlyHigh = new ChatCapabilities.ReasoningMode(
                "effort", false, List.of("high", "max"), 0, 0, "catalog");
        // 声明里没有 medium/low：向邻近档靠，而不是照发一个模型不认的词
        JsonObject medium = capture("v1", ChatEndpoints.OPENAI_COMPLETIONS, "glm-5.2",
                "medium", null, 512, onlyHigh);
        assertEquals("high", medium.get("reasoning_effort").getAsString());
        // 声明不允许关：off 时一个字段都不发
        JsonObject off = capture("v1", ChatEndpoints.OPENAI_COMPLETIONS, "glm-5.2",
                "off", null, 512, onlyHigh);
        assertFalse(off.has("reasoning_effort"));
        // 声明里允许 none 才发 none
        ChatCapabilities.ReasoningMode withNone = new ChatCapabilities.ReasoningMode(
                "effort", true, List.of("none", "low", "high"), 0, 0, "catalog");
        JsonObject none = capture("v1", ChatEndpoints.OPENAI_COMPLETIONS, "gpt-5.4",
                "off", null, 512, withNone);
        assertEquals("none", none.get("reasoning_effort").getAsString());
        // forced 的模型（DeepSeek reasoner）怎么选都不发
        JsonObject forced = capture("v1", ChatEndpoints.OPENAI_COMPLETIONS, "deepseek-reasoner",
                "high", null, 512, ChatCapabilities.ReasoningMode.FORCED);
        assertFalse(forced.has("reasoning_effort"));
    }

    @Test
    void anthropicBudgetIsClampedToTheDeclaredRange() throws Exception {
        ChatCapabilities.ReasoningMode narrow = new ChatCapabilities.ReasoningMode(
                "budget", true, List.of(), 2_048, 8_192, "catalog");
        JsonObject body = capture("v1", ChatEndpoints.ANTHROPIC_MESSAGES, "claude-sonnet-4-5",
                "high", null, 2_000, narrow);
        assertEquals(8_192, body.getAsJsonObject("thinking").get("budget_tokens").getAsInt());
        assertTrue(body.get("max_tokens").getAsInt() > 8_192);
    }

    @Test
    void reasoningModesFollowVendorSyntaxAndCatalogDeclaration() throws Exception {
        java.nio.file.Path dir = Files.createTempDirectory("hdsl-reasoning-catalog");
        Files.writeString(dir.resolve("models.dev.json"), """
                {"updatedAt": 1, "models": {
                  "glm-5.2": {"vision": false, "tools": true, "reasoning": true, "context": 128000,
                              "output": 16384, "reasoningKind": "effort",
                              "reasoningValues": ["high", "max"]},
                  "claude-sonnet-4-5": {"vision": true, "tools": true, "reasoning": true,
                              "context": 200000, "output": 32000, "reasoningKind": "budget",
                              "reasoningMin": 2048, "reasoningMax": 8192}
                }}""");
        ChatModelCatalog.install(dir);
        try {
            ChatCapabilities.Capabilities reasoning = new ChatCapabilities.Capabilities(
                    false, false, true, true, 128_000, 16_384, "catalog");
            ChatCapabilities.ReasoningMode effortMode = ChatCapabilities.reasoningMode(
                    reasoning, ChatEndpoints.OPENAI_COMPLETIONS, "http://127.0.0.1/v1", "glm-5.2");
            assertEquals("effort", effortMode.kind());
            assertFalse(effortMode.canDisable());
            assertEquals(List.of("high", "max"), effortMode.efforts());

            ChatCapabilities.ReasoningMode anthropic = ChatCapabilities.reasoningMode(
                    reasoning, ChatEndpoints.ANTHROPIC_MESSAGES, "https://api.anthropic.com", "claude-sonnet-4-5");
            assertEquals("budget", anthropic.kind());
            assertEquals(2_048, anthropic.minBudget());
            assertEquals(8_192, anthropic.maxBudget());

            ChatCapabilities.ReasoningMode zhipu = ChatCapabilities.reasoningMode(
                    reasoning, ChatEndpoints.OPENAI_COMPLETIONS, "https://open.bigmodel.cn/api/paas/v4", "glm-5.2");
            assertEquals("toggle", zhipu.kind());

            ChatCapabilities.ReasoningMode deepseek = ChatCapabilities.reasoningMode(
                    reasoning, ChatEndpoints.OPENAI_COMPLETIONS, "https://api.deepseek.com", "deepseek-reasoner");
            assertEquals("forced", deepseek.kind());

            ChatCapabilities.ReasoningMode none = ChatCapabilities.reasoningMode(
                    new ChatCapabilities.Capabilities(false, false, false, true, 32_768, 4_096, "unknown"),
                    ChatEndpoints.OPENAI_COMPLETIONS, null, "plain-model");
            assertEquals("none", none.kind());
        } finally {
            ChatModelCatalog.install(Files.createTempDirectory("hdsl-reasoning-empty"));
        }
    }

    @Test
    void deepseekGetsReasoningEffortInItsOwnVocabulary() {
        ChatCapabilities.ReasoningMode mode = new ChatCapabilities.ReasoningMode(
                "effort", true, List.of("none", "low", "high", "max"), 0, 0, "vendor");
        for (String[] pair : new String[][]{
                {"low", "low"}, {"medium", "high"}, {"high", "high"},
                {"xhigh", "high"}, {"max", "max"}, {"minimal", "low"}}) {
            JsonObject body = new JsonObject();
            OpenAiProtocol.applyReasoning(body, "deepseek", "deepseek-v4.1-flash-1017", pair[0], mode);
            assertEquals(pair[1], body.get("reasoning_effort").getAsString(), pair[0]);
        }
        JsonObject off = new JsonObject();
        OpenAiProtocol.applyReasoning(off, "deepseek", "deepseek-v4.1-flash-1017", "off", mode);
        assertEquals("none", off.get("reasoning_effort").getAsString());

        // 老的 reasoner 是纯思考模型：怎么选都不发
        JsonObject forced = new JsonObject();
        OpenAiProtocol.applyReasoning(forced, "deepseek", "deepseek-reasoner", "high",
                ChatCapabilities.ReasoningMode.FORCED);
        assertFalse(forced.has("reasoning_effort"));

        assertEquals("deepseek", OpenAiProtocol.vendorFamily("https://api.deepseek.com"));
        assertEquals("dashscope", OpenAiProtocol.vendorFamily("https://dashscope.aliyuncs.com/compatible-mode/v1"));
        assertEquals("generic", OpenAiProtocol.vendorFamily("http://127.0.0.1:3199/v1"));
    }

    @Test
    void deepseekCanAlwaysBeTurnedOffEvenWhenTheDirectoryForgetsNone() throws Exception {
        java.nio.file.Path dir = Files.createTempDirectory("hdsl-deepseek-catalog");
        Files.writeString(dir.resolve("models.dev.json"), """
                {"updatedAt": 1, "models": {
                  "deepseek-v4.1-flash": {"vision": true, "tools": true, "reasoning": true,
                              "context": 1000000, "output": 64000, "reasoningKind": "effort",
                              "reasoningValues": ["low", "high", "max"]}
                }}""");
        ChatModelCatalog.install(dir);
        try {
            ChatCapabilities.ReasoningMode mode = ChatCapabilities.reasoningMode(
                    new ChatCapabilities.Capabilities(true, false, true, true, 1_000_000, 64_000, "catalog"),
                    ChatEndpoints.OPENAI_COMPLETIONS, "https://api.deepseek.com", "deepseek-v4.1-flash");
            assertEquals("effort", mode.kind());
            assertTrue(mode.canDisable(), "官方文档保证 reasoning_effort=none 能关思考");
            assertTrue(mode.efforts().contains("none"));
            assertTrue(mode.efforts().contains("max"));
        } finally {
            ChatModelCatalog.install(Files.createTempDirectory("hdsl-deepseek-empty"));
        }

        // 没有目录时也要给全四档，不然菜单只剩一个「关」
        ChatCapabilities.ReasoningMode offline = ChatCapabilities.reasoningMode(
                new ChatCapabilities.Capabilities(true, false, true, true, 1_000_000, 64_000, "builtin"),
                ChatEndpoints.OPENAI_COMPLETIONS, "https://api.deepseek.com", "deepseek-v4.1-flash");
        assertEquals(List.of("none", "low", "high", "max"), offline.efforts());
    }

    @Test
    void sessionLevelPersistsAndCanBeCleared() throws Exception {
        java.nio.file.Path dir = Files.createTempDirectory("hdsl-session-level");
        ChatStore store = new ChatStore(dir);
        try {
            String assistantId = store.assistants().get(0).id();
            ChatStore.Session session = store.createSession(assistantId, "t", null, null);
            assertNull(session.reasoningLevel());
            store.updateSession(session.id(), null, null, null, "high");
            assertEquals("high", store.session(session.id()).reasoningLevel());
            store.updateSession(session.id(), null, null, null, "");
            assertNull(store.session(session.id()).reasoningLevel());
        } finally {
            store.close();
        }
    }

    private static JsonObject capture(String pathPrefix, String protocolId, String model,
                                      String level) throws Exception {
        return capture(pathPrefix, protocolId, model, level, null, 512, null);
    }

    private static JsonObject capture(String pathPrefix, String protocolId, String model,
                                      String level, Double temperature, int maxTokens) throws Exception {
        return capture(pathPrefix, protocolId, model, level, temperature, maxTokens, null);
    }

    private static JsonObject capture(String pathPrefix, String protocolId, String model,
                                      String level, Double temperature, int maxTokens,
                                      ChatCapabilities.ReasoningMode mode) throws Exception {
        AtomicReference<String> captured = new AtomicReference<>("");
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            captured.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        });
        server.start();
        try {
            // 本地假端点 + 在路径里带上厂商标记：prepare() 只按 base 里的关键字选参数
            String base = "http://127.0.0.1:" + server.getAddress().getPort() + "/" + pathPrefix;
            ChatEndpoints.Profile profile = new ChatEndpoints.Profile(
                    "test", protocolId, base,
                    "/models", ChatEndpoints.ANTHROPIC_MESSAGES.equals(protocolId)
                            ? "/messages" : "/chat/completions",
                    "bearer", Map.of(), List.of(model), null, null);
            ChatProtocol protocol = ChatEndpoints.ANTHROPIC_MESSAGES.equals(protocolId)
                    ? new AnthropicProtocol() : new OpenAiProtocol();
            ChatProtocol.ChatRequest request = new ChatProtocol.ChatRequest(
                    model, null,
                    List.of(new ChatProtocol.ChatMessage("user", "hi", List.of())),
                    temperature, maxTokens, level, mode);
            HttpClient.newHttpClient().send(protocol.prepare(profile, request),
                    HttpResponse.BodyHandlers.discarding());
            return JsonParser.parseString(captured.get()).getAsJsonObject();
        } finally {
            server.stop(0);
        }
    }
}