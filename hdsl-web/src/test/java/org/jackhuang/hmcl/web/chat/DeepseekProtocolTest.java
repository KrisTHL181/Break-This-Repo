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
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// DeepSeek 官方扩展模式：请求形状、服务端工具、引用、思考签名和标题。
class DeepseekProtocolTest {

    @Test
    void requestCarriesServerToolsThinkingReminderAndTitle() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        AtomicReference<String> captured = new AtomicReference<>();
        server.createContext("/anthropic/v1/messages", exchange -> {
            captured.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] response = "{}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        try {
            ChatEndpoints.Profile profile = new ChatEndpoints.Profile("missing", 
                    ChatEndpoints.DEEPSEEK_ANTHROPIC,
                    "http://127.0.0.1:" + server.getAddress().getPort(),
                    "/models", "/anthropic/v1/messages", "bearer", Map.of(),
                    List.of(), null, null);

            JsonObject readLink = JsonParser.parseString("""
                    {"type":"server_tool_use","id":"rl1","name":"deepseek_read_url",
                     "input":{"action":"read_link","url":"https://example.com"},
                     "kind":"read_link"}""").getAsJsonObject();
            ChatProtocol.ChatRequest request = new ChatProtocol.ChatRequest(
                    "deepseek-flash", "system", List.of(
                    new ChatProtocol.ChatMessage("latest_reminder", "2026-11-14,Friday,China,Web,English",
                            List.of()),
                    new ChatProtocol.ChatMessage("user", "hello", List.of()),
                    new ChatProtocol.ChatMessage("assistant", "", List.of(), List.of(), null,
                            List.of(readLink)),
                    new ChatProtocol.ChatMessage("user", "again", List.of())),
                    null, null, null, null, List.of(), true, true);
            HttpRequest httpRequest = new DeepseekProtocol().prepare(profile, request);
            HttpClient.newHttpClient().send(httpRequest, HttpResponse.BodyHandlers.discarding());

            JsonObject body = JsonParser.parseString(captured.get()).getAsJsonObject();
            assertEquals("deepseek-flash", body.get("model").getAsString());
            assertEquals("enabled", body.getAsJsonObject("thinking").get("type").getAsString());
            assertEquals("high", body.getAsJsonObject("safety_check").get("level").getAsString());
            assertTrue(body.getAsJsonObject("title").get("enabled").getAsBoolean());
            assertFalse(body.has("max_tokens"));
            assertFalse(body.has("temperature"));

            JsonArray tools = body.getAsJsonArray("tools");
            List<String> names = new ArrayList<>();
            for (var tool : tools) {
                names.add(tool.getAsJsonObject().get("name").getAsString());
            }
            assertTrue(names.contains("deepseek_web_search"));
            assertTrue(names.contains("deepseek_read_url"));

            JsonArray messages = body.getAsJsonArray("messages");
            assertEquals("latest_reminder", messages.get(0).getAsJsonObject().get("role").getAsString());
            assertEquals("user", messages.get(1).getAsJsonObject().get("role").getAsString());
            assertTrue(messages.get(1).getAsJsonObject().get("content").isJsonPrimitive());
            JsonObject assistant = messages.get(2).getAsJsonObject();
            assertEquals("assistant", assistant.get("role").getAsString());
            assertEquals("server_tool_use",
                    assistant.getAsJsonArray("content").get(0).getAsJsonObject().get("type").getAsString());
            JsonObject secondUser = messages.get(3).getAsJsonObject();
            assertEquals("user", secondUser.get("role").getAsString());
            assertTrue(secondUser.get("content").isJsonPrimitive());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void readsThinkingSearchCitationsAndTitle() throws Exception {
        String stream = """
                event: message_start
                data: {"type":"message_start","message":{"usage":{"input_tokens":10,"output_tokens":0}}}

                event: content_block_start
                data: {"type":"content_block_start","index":0,"content_block":{"type":"thinking","thinking":""}}

                event: content_block_delta
                data: {"type":"content_block_delta","index":0,"delta":{"type":"thinking_delta","thinking":"想"}}

                event: content_block_delta
                data: {"type":"content_block_delta","index":0,"delta":{"type":"signature_delta","signature":"sig"}}

                event: content_block_stop
                data: {"type":"content_block_stop","index":0}

                event: content_block_start
                data: {"type":"content_block_start","index":1,"content_block":{"type":"server_tool_use","id":"s1","name":"deepseek_web_search","input":{"action":"search","queries":["q"]},"kind":"search"}}

                event: content_block_start
                data: {"type":"content_block_start","index":2,"content_block":{"type":"web_search_tool_result","tool_use_id":"s1","kind":"search","content":[{"type":"web_search_result","title":"T","url":"https://e.com","snippet":"S","site_name":"e.com","site_icon":null,"page_age":null,"encrypted_content":""}]}}

                event: content_block_start
                data: {"type":"content_block_start","index":3,"content_block":{"type":"text","text":""}}

                event: content_block_delta
                data: {"type":"content_block_delta","index":3,"delta":{"type":"text_delta","text":"答案"}}

                event: content_block_delta
                data: {"type":"content_block_delta","index":3,"delta":{"type":"citations_delta","citation":{"type":"web_search_result_location","url":"https://e.com","title":"T","cited_text":"答案","encrypted_index":""}}}

                event: content_block_stop
                data: {"type":"content_block_stop","index":3}

                event: message_delta
                data: {"type":"message_delta","delta":{"stop_reason":"end_turn","title":"标题"},"usage":{"output_tokens":5}}

                event: message_stop
                data: {"type":"message_stop"}

                """;

        Sink sink = new Sink();
        new DeepseekProtocol().read(
                new ByteArrayInputStream(stream.getBytes(StandardCharsets.UTF_8)), sink);

        assertEquals("答案", sink.text.toString());
        assertEquals("想", sink.reasoning.toString());
        assertEquals("end_turn", sink.stopReason);
        assertEquals("标题", sink.title);
        assertEquals(4, sink.blocks.size());
        JsonObject text = sink.blocks.get(3).getAsJsonObject();
        assertEquals("text", text.get("type").getAsString());
        assertEquals(1, text.getAsJsonArray("citations").size());
        assertEquals(10, sink.input);
        assertEquals(5, sink.output);
    }

    @Test
    void thinkingSegmentsAreDelimitedAroundServerTools() throws Exception {
        String stream = """
                event: content_block_start
                data: {"type":"content_block_start","index":0,"content_block":{"type":"thinking","thinking":""}}

                event: content_block_delta
                data: {"type":"content_block_delta","index":0,"delta":{"type":"thinking_delta","thinking":"先想"}}

                event: content_block_stop
                data: {"type":"content_block_stop","index":0}

                event: content_block_start
                data: {"type":"content_block_start","index":1,"content_block":{"type":"server_tool_use","id":"s1","name":"deepseek_web_search","input":{"action":"search","queries":["q"]},"kind":"search"}}

                event: content_block_start
                data: {"type":"content_block_start","index":2,"content_block":{"type":"thinking","thinking":""}}

                event: content_block_delta
                data: {"type":"content_block_delta","index":2,"delta":{"type":"thinking_delta","thinking":"再想"}}

                event: content_block_stop
                data: {"type":"content_block_stop","index":2}

                event: message_stop
                data: {"type":"message_stop"}

                """;

        Sink sink = new Sink();
        new DeepseekProtocol().read(
                new ByteArrayInputStream(stream.getBytes(StandardCharsets.UTF_8)), sink);

        assertEquals(List.of("thinking", "thinking"), sink.blockStarts);
        assertEquals(List.of("thinking", "thinking"), sink.blockEnds);
        assertEquals("先想再想", sink.reasoning.toString());
    }

    @Test
    void deepseekReasoningIsAToggle() {
        ChatCapabilities.Capabilities caps = new ChatCapabilities.Capabilities(
                true, false, true, true, 1_000_000, 64_000, "vendor");
        ChatCapabilities.ReasoningMode mode = ChatCapabilities.reasoningMode(
                caps, ChatEndpoints.DEEPSEEK_ANTHROPIC, "https://api.deepseek.com", "deepseek-flash");
        assertEquals("toggle", mode.kind());
        assertTrue(mode.canDisable());
    }

    @Test
    void latestReminderAlwaysHasFiveFields() {
        String reminder = ChatTurnManager.latestReminder();
        String[] fields = reminder.split(",", -1);
        assertEquals(5, fields.length, reminder);
        assertTrue(fields[0].matches("\\d{4}-\\d{2}-\\d{2}"), reminder);
        assertFalse(fields[1].isBlank(), reminder);
        assertFalse(fields[2].isBlank(), reminder);
        assertEquals("Web", fields[3]);
        assertFalse(fields[4].isBlank(), reminder);
    }

    @Test
    void latestReminderUsesTheLanguageNameTheServerAccepts() {
        assertEquals(
                "2026-11-15,星期日,中国,Web,中文",
                ChatTurnManager.latestReminder(Locale.SIMPLIFIED_CHINESE, LocalDate.of(2026, 11, 15)));
        assertEquals(
                "2026-11-15,Sunday,United States,Web,English",
                ChatTurnManager.latestReminder(Locale.US, LocalDate.of(2026, 11, 15)));
    }

    @Test
    void legacyReminderIsRewrittenBeforeReplay() {
        assertEquals(
                "2026-11-14,星期六,中国,Web,中文",
                ChatTurnManager.replayableReminder(
                        "2026-11-14,Saturday,China,Web,Chinese", Locale.SIMPLIFIED_CHINESE));
        assertEquals(
                "2026-11-14,Saturday,United States,Web,English",
                ChatTurnManager.replayableReminder(
                        "2026-11-14,Saturday,United States,Web,English", Locale.US));
    }

    private static final class Sink implements ChatProtocol.ChatEventSink {
        final StringBuilder text = new StringBuilder();
        final StringBuilder reasoning = new StringBuilder();
        final List<com.google.gson.JsonElement> blocks = new ArrayList<>();
        final List<String> blockStarts = new ArrayList<>();
        final List<String> blockEnds = new ArrayList<>();
        String stopReason;
        String title;
        int input;
        int output;

        @Override
        public void text(String delta) {
            text.append(delta);
        }

        @Override
        public void reasoning(String delta) {
            reasoning.append(delta);
        }

        @Override
        public void usage(int input, int output) {
            if (input > 0) {
                this.input = input;
            }
            if (output > 0) {
                this.output = output;
            }
        }

        @Override
        public void done(String finishReason) {
            this.stopReason = finishReason;
        }

        @Override
        public void error(String code, String message, boolean retryable) {
            throw new AssertionError(code + ": " + message);
        }

        @Override
        public void block(JsonObject block) {
            blocks.add(block);
        }

        @Override
        public void blockStart(JsonObject block) {
            blockStarts.add(block.get("type").getAsString());
        }

        @Override
        public void blockEnd(JsonObject block) {
            blockEnds.add(block.get("type").getAsString());
        }

        @Override
        public void title(String title) {
            this.title = title;
        }
    }
}