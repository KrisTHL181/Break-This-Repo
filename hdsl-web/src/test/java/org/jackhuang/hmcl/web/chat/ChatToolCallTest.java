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
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// `search_web` 工具在两种协议上的下发与解析（docs/chat.md §12.5）。
class ChatToolCallTest {

    @Test
    void openAiSendsToolDeclarationsAndToolMessages() throws Exception {
        ChatProtocol.ChatRequest request = new ChatProtocol.ChatRequest(
                "gpt-test", "sys",
                List.of(
                        new ChatProtocol.ChatMessage("user", "现在几点", List.of()),
                        new ChatProtocol.ChatMessage("assistant", "我查一下", List.of(),
                                List.of(new ChatProtocol.ToolCall("call_1", "search_web",
                                        "{\"query\":\"现在几点\"}")), null),
                        new ChatProtocol.ChatMessage("tool", "{\"hits\":[]}", List.of(),
                                List.of(), "call_1")),
                0.2, 512, null, null, List.of(new ChatProtocol.ToolSpec("search_web", "test", ChatSearchService.searchSchema("bocha"))));
        JsonObject body = capture(new OpenAiProtocol(), ChatEndpoints.OPENAI_COMPLETIONS, request);

        JsonObject tool = body.getAsJsonArray("tools").get(0).getAsJsonObject();
        assertEquals("function", tool.get("type").getAsString());
        assertEquals("search_web", tool.getAsJsonObject("function").get("name").getAsString());
        assertEquals("object", tool.getAsJsonObject("function").getAsJsonObject("parameters")
                .get("type").getAsString());

        JsonArray messages = body.getAsJsonArray("messages");
        JsonObject assistant = messages.get(2).getAsJsonObject();
        assertEquals("assistant", assistant.get("role").getAsString());
        JsonObject call = assistant.getAsJsonArray("tool_calls").get(0).getAsJsonObject();
        assertEquals("call_1", call.get("id").getAsString());
        assertEquals("function", call.get("type").getAsString());
        assertEquals("search_web", call.getAsJsonObject("function").get("name").getAsString());

        JsonObject result = messages.get(3).getAsJsonObject();
        assertEquals("tool", result.get("role").getAsString());
        assertEquals("call_1", result.get("tool_call_id").getAsString());
        assertEquals("{\"hits\":[]}", result.get("content").getAsString());
    }

    @Test
    void openAiAccumulatesStreamedToolCalls() throws IOException {
        String sse = """
                data: {"choices":[{"delta":{"content":"我查一下"}}]}

                data: {"choices":[{"delta":{"tool_calls":[{"index":0,"id":"call_9",\
                "function":{"name":"search_web","arguments":"{\\"query\\":"}}]}}]}

                data: {"choices":[{"delta":{"tool_calls":[{"index":0,\
                "function":{"arguments":"\\"今天天气\\"}"}}]}}]}

                data: {"choices":[{"delta":{},"finish_reason":"tool_calls"}]}

                data: [DONE]

                """;
        Recording sink = new Recording();
        new OpenAiProtocol().read(
                new ByteArrayInputStream(sse.getBytes(StandardCharsets.UTF_8)), sink);

        assertEquals("我查一下", sink.text.toString());
        assertEquals(List.of("call_9"), sink.ids);
        assertEquals(List.of("search_web"), sink.names);
        assertEquals(List.of("{\"query\":\"今天天气\"}"), sink.arguments);
        assertEquals("tool_calls", sink.doneReason);
    }

    @Test
    void anthropicSendsToolUseAndToolResult() throws Exception {
        ChatProtocol.ChatRequest request = new ChatProtocol.ChatRequest(
                "claude-test", "sys",
                List.of(
                        new ChatProtocol.ChatMessage("user", "现在几点", List.of()),
                        new ChatProtocol.ChatMessage("assistant", "我查一下", List.of(),
                                List.of(new ChatProtocol.ToolCall("toolu_1", "search_web",
                                        "{\"query\":\"现在几点\"}")), null),
                        new ChatProtocol.ChatMessage("tool", "{\"hits\":[]}", List.of(),
                                List.of(), "toolu_1")),
                null, 512, null, null, List.of(new ChatProtocol.ToolSpec("search_web", "test", ChatSearchService.searchSchema("bocha"))));
        JsonObject body = capture(new AnthropicProtocol(), ChatEndpoints.ANTHROPIC_MESSAGES, request);

        JsonObject tool = body.getAsJsonArray("tools").get(0).getAsJsonObject();
        assertEquals("search_web", tool.get("name").getAsString());
        assertEquals("object", tool.getAsJsonObject("input_schema").get("type").getAsString());

        JsonArray messages = body.getAsJsonArray("messages");
        JsonArray blocks = messages.get(1).getAsJsonObject().getAsJsonArray("content");
        JsonObject use = blocks.get(blocks.size() - 1).getAsJsonObject();
        assertEquals("tool_use", use.get("type").getAsString());
        assertEquals("toolu_1", use.get("id").getAsString());
        assertEquals("现在几点", use.getAsJsonObject("input").get("query").getAsString());

        JsonObject result = messages.get(2).getAsJsonObject();
        assertEquals("user", result.get("role").getAsString());
        JsonObject block = result.getAsJsonArray("content").get(0).getAsJsonObject();
        assertEquals("tool_result", block.get("type").getAsString());
        assertEquals("toolu_1", block.get("tool_use_id").getAsString());
    }

    @Test
    void anthropicParsesStreamedToolUse() throws IOException {
        String sse = """
                event: content_block_start
                data: {"index":1,"content_block":{"type":"tool_use","id":"toolu_9",\
                "name":"search_web","input":{}}}

                event: content_block_delta
                data: {"index":1,"delta":{"type":"input_json_delta",\
                "partial_json":"{\\"query\\":\\"天气\\"}"}}

                event: message_stop
                data: {"type":"message_stop"}

                """;
        Recording sink = new Recording();
        new AnthropicProtocol().read(
                new ByteArrayInputStream(sse.getBytes(StandardCharsets.UTF_8)), sink);

        assertEquals(List.of("toolu_9"), sink.ids);
        assertEquals(List.of("search_web"), sink.names);
        assertEquals(List.of("{\"query\":\"天气\"}"), sink.arguments);
        assertEquals("stop", sink.doneReason);
    }

    @Test
    void declarationSaysWhetherSearchIsAvailable() {
        String enabled = ChatSearchService.declaration("bocha", true, true, true, true);
        assertTrue(enabled.contains("博查"));
        assertTrue(enabled.contains("search_web"));
        assertTrue(enabled.contains("预先检索"));

        String off = ChatSearchService.declaration("bocha", true, false, false, false);
        assertTrue(off.contains("没有开启"));
        assertTrue(!off.contains("search_web 工具"), "关闭时不该教模型调用工具");

        String presearched = ChatSearchService.declaration("bocha", true, true, false, true);
        assertTrue(presearched.contains("已经替用户检索"));

        String missing = ChatSearchService.declaration("bocha", false, false, false, false);
        assertTrue(missing.contains("没有配置"));
    }

    @Test
    void exhaustedDeclarationForbidsFurtherSearch() {
        String text = ChatSearchService.exhaustedDeclaration("bocha");
        assertTrue(text.contains("预算已经用完"));
        assertTrue(text.contains("博查"));
        assertTrue(text.contains("不要再请求搜索"));
        assertTrue(text.contains("不要编造"));
    }

    @Test
    void recoversXmlSearchToolCalls() {
        String text = "好，那我去查。<search><query>Open WebUI extraction</query>"
                + "<query>LibreChat parsing</query></search>";
        List<ChatProtocol.ToolCall> calls = ChatTurnManager.xmlCallsFromText(
                text, ChatSearchService.tools(settings("bocha")));
        assertEquals(2, calls.size());
        assertEquals("search_web", calls.get(0).name());
        assertTrue(calls.get(0).arguments().contains("Open WebUI"));
        assertTrue(calls.get(1).arguments().contains("LibreChat"));
    }

    @Test
    void xmlSearchRecoveryHandlesTruncatedBlocks() {
        String text = "<search><query>alpha</query><query>beta";
        List<ChatProtocol.ToolCall> calls = ChatTurnManager.xmlCallsFromText(
                text, ChatSearchService.tools(settings("bocha")));
        assertEquals(1, calls.size());
        assertTrue(calls.get(0).arguments().contains("alpha"));
    }

    @Test
    void xmlSearchRecoveryNeedsTheTool() {
        String text = "<search><query>alpha</query></search>";
        assertEquals(List.of(), ChatTurnManager.xmlCallsFromText(text, List.of()));
    }

    @Test
    void xmlSearchRecoveryIgnoresPlainMentions() {
        String text = "正文里提到 `<search>` 标签，但没有 query，不该当成调用";
        assertEquals(List.of(), ChatTurnManager.xmlCallsFromText(
                text, ChatSearchService.tools(settings("bocha"))));
    }

    @Test
    void toolSetDegradesToSearchOnly() {
        assertEquals(List.of("search_web"),
                ChatSearchService.tools(settings("bocha")).stream()
                        .map(ChatProtocol.ToolSpec::name).toList());
        assertEquals(List.of("search_web", "scrape_web"),
                ChatSearchService.tools(settings("deepseek")).stream()
                        .map(ChatProtocol.ToolSpec::name).toList());
    }

    @Test
    void searchResultCarriesStableIdsAndRetrievalTime() {
        ChatSearchService.Result result = new ChatSearchService.Result("q", "bocha",
                List.of(new ChatSearchService.Hit("T", "https://example.com/a", "text", "Site",
                        "2026-01-01")));
        String text = ChatSearchService.toolResult(result);
        JsonObject json = JsonParser.parseString(text.substring(text.indexOf('{'))).getAsJsonObject();
        JsonObject item = json.getAsJsonArray("items").get(0).getAsJsonObject();
        assertEquals(ChatSearchService.shortId("https://example.com/a"),
                item.get("id").getAsString());
        assertEquals(1, item.get("index").getAsInt());
        assertEquals("2026-01-01", item.get("publishedDate").getAsString());
        assertTrue(json.has("retrievedAt"));
    }

    @Test
    void bochaHasNoScrapeTool() {
        assertEquals(false, ChatSearchService.scrapeSupported("bocha"));
        assertEquals(true, ChatSearchService.scrapeSupported("deepseek"));
        assertEquals(true, ChatSearchService.scrapeSupported("exa"));
    }

    private static ChatSettings.Settings settings(String provider) {
        return new ChatSettings.Settings("forever", 0, true, "03:17", 7, "/tmp", "allow",
                List.of(), 4000, 0, 40, null, null, provider, Map.of(), 5, true);
    }

    private static JsonObject capture(ChatProtocol protocol, String protocolId,
                                      ChatProtocol.ChatRequest request) throws Exception {
        AtomicReference<String> captured = new AtomicReference<>("");
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            captured.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        });
        server.start();
        try {
            String base = "http://127.0.0.1:" + server.getAddress().getPort();
            ChatEndpoints.Profile profile = new ChatEndpoints.Profile(
                    "test", protocolId, base, "/models",
                    ChatEndpoints.ANTHROPIC_MESSAGES.equals(protocolId)
                            ? "/messages" : "/chat/completions",
                    "bearer", Map.of(), List.of(request.model()), null, null);
            HttpClient.newHttpClient().send(protocol.prepare(profile, request),
                    HttpResponse.BodyHandlers.discarding());
            return JsonParser.parseString(captured.get()).getAsJsonObject();
        } finally {
            server.stop(0);
        }
    }

    private static final class Recording implements ChatProtocol.ChatEventSink {
        final StringBuilder text = new StringBuilder();
        final List<String> ids = new ArrayList<>();
        final List<String> names = new ArrayList<>();
        final List<String> arguments = new ArrayList<>();
        String doneReason;

        @Override
        public void text(String delta) {
            text.append(delta);
        }

        @Override
        public void reasoning(String delta) {
        }

        @Override
        public void usage(int input, int output) {
        }

        @Override
        public void done(String finishReason) {
            doneReason = finishReason;
        }

        @Override
        public void error(String code, String message, boolean retryable) {
            throw new AssertionError(code + ": " + message);
        }

        @Override
        public void toolCalls(List<ChatProtocol.ToolCall> calls) {
            for (ChatProtocol.ToolCall call : calls) {
                ids.add(call.id());
                names.add(call.name());
                arguments.add(call.arguments());
            }
        }
    }
}
