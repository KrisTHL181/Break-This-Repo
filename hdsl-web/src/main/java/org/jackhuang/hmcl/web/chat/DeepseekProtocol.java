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
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/// DeepSeek 官方 Anthropic 兼容扩展模式（chat-demo 的协议形状）。
///
/// 与 [AnthropicProtocol] 的差异：
/// - `thinking` 只有 `enabled` / `disabled`，没有 `budget_tokens`；
/// - 服务端工具（`deepseek_web_search` / `deepseek_read_url`）由服务端执行，
///   结果以 `web_search_tool_result` / `web_fetch_tool_result` 流式回来；
/// - 正文引用、思考签名、服务端标题和 `latest_reminder` 角色都是这条协议的一部分。
@NotNullByDefault
public final class DeepseekProtocol implements ChatProtocol {

    private static final String FILES_BETA = "files-api-2025-04-14";

    @Override
    public String id() {
        return ChatEndpoints.DEEPSEEK_ANTHROPIC;
    }

    @Override
    public boolean supportsDocuments() {
        return false;
    }

    @Override
    public HttpRequest prepare(ChatEndpoints.Profile profile, ChatRequest request)
            throws IOException {
        JsonObject body = new JsonObject();
        body.addProperty("model", request.model());
        body.addProperty("stream", true);
        if (request.system() != null && !request.system().isBlank()) {
            body.addProperty("system", request.system());
        }

        JsonArray messages = new JsonArray();
        for (ChatMessage message : request.messages()) {
            if ("latest_reminder".equals(message.role())) {
                JsonObject row = new JsonObject();
                row.addProperty("role", "latest_reminder");
                row.addProperty("content", message.text() == null ? "" : message.text());
                messages.add(row);
                continue;
            }
            JsonObject row = new JsonObject();
            row.addProperty("role", message.role());
            if ("assistant".equals(message.role())) {
                JsonArray content = new JsonArray();
                if (message.blocks() != null) {
                    for (JsonObject block : message.blocks()) {
                        content.add(block);
                    }
                }
                if (content.isEmpty() && message.text() != null && !message.text().isBlank()) {
                    JsonObject text = new JsonObject();
                    text.addProperty("type", "text");
                    text.addProperty("text", message.text());
                    content.add(text);
                }
                if (content.isEmpty()) {
                    content.add(textBlock(" "));
                }
                row.add("content", content);
            } else {
                row.add("content", userContent(message));
            }
            messages.add(row);
        }
        body.add("messages", messages);

        JsonObject thinking = new JsonObject();
        thinking.addProperty("type", thinkingOn(request.reasoningLevel()) ? "enabled" : "disabled");
        body.add("thinking", thinking);

        JsonArray tools = new JsonArray();
        tools.add(serverTool("deepseek_read_url"));
        if (request.searchEnabled()) {
            tools.add(serverTool("deepseek_web_search"));
        }
        body.add("tools", tools);

        JsonObject safety = new JsonObject();
        safety.addProperty("level", "high");
        body.add("safety_check", safety);

        if (request.titleEnabled()) {
            JsonObject title = new JsonObject();
            title.addProperty("enabled", true);
            body.add("title", title);
        }

        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(profile.chatUri().toString()))
                .timeout(Duration.ofMinutes(10))
                .header("Content-Type", "application/json")
                .header("Accept", "text/event-stream")
                .header("User-Agent", org.jackhuang.hmcl.Metadata.NAME + "/"
                        + org.jackhuang.hmcl.Metadata.VERSION)
                .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8));
        if (hasFileBlocks(request.messages())) {
            builder.header("anthropic-beta", FILES_BETA);
        }
        ChatEndpoints.applyAuth(builder, profile);
        return builder.build();
    }

    private static boolean hasFileBlocks(List<ChatMessage> messages) {
        for (ChatMessage message : messages) {
            for (Attachment attachment : message.attachments()) {
                if (attachment.remoteId() != null && !attachment.remoteId().isBlank()) {
                    return true;
                }
            }
        }
        return false;
    }

    private static JsonObject serverTool(String name) {
        JsonObject tool = new JsonObject();
        tool.addProperty("type", name);
        tool.addProperty("name", name);
        return tool;
    }

    private static JsonObject textBlock(String text) {
        JsonObject block = new JsonObject();
        block.addProperty("type", "text");
        block.addProperty("text", text);
        return block;
    }

    private static JsonElement userContent(ChatMessage message) {
        JsonArray parts = new JsonArray();
        StringBuilder text = new StringBuilder(message.text() == null ? "" : message.text());
        for (Attachment attachment : message.attachments()) {
            if ("image".equals(attachment.kind()) && attachment.remoteId() != null
                    && !attachment.remoteId().isBlank()) {
                JsonObject image = new JsonObject();
                image.addProperty("type", "image");
                JsonObject source = new JsonObject();
                source.addProperty("type", "file");
                source.addProperty("file_id", attachment.remoteId());
                image.add("source", source);
                parts.add(image);
            } else if (attachment.text() != null && !attachment.text().isBlank()) {
                appendText(text, attachment);
            }
        }
        if (parts.isEmpty()) {
            // DeepSeek 的 messages[].content 只接受字符串或数组；纯文本用字符串。
            return new com.google.gson.JsonPrimitive(text.isEmpty() ? " " : text.toString());
        }
        if (!text.isEmpty()) {
            JsonObject block = new JsonObject();
            block.addProperty("type", "text");
            block.addProperty("text", text.toString());
            parts.add(block);
        }
        return parts;
    }

    private static void appendText(StringBuilder text, Attachment attachment) {
        if (!text.isEmpty()) {
            text.append("\n\n");
        }
        text.append("[附件: ").append(attachment.name()).append("]\n");
        text.append(attachment.text());
    }

    private static boolean thinkingOn(@Nullable String level) {
        if (level == null || level.isBlank()) {
            return true;
        }
        String value = level.trim().toLowerCase(Locale.ROOT);
        return !"off".equals(value) && !"none".equals(value) && !"disabled".equals(value);
    }

    @Override
    public void read(InputStream body, ChatEventSink sink) throws IOException {
        Map<Integer, BlockBuilder> builders = new LinkedHashMap<>();
        String[] stop = {null};
        boolean[] done = {false};
        SseParser.parse(body, (event, data) -> {
            if (data.isBlank()) {
                return true;
            }
            JsonObject json;
            try {
                JsonElement parsed = JsonParser.parseString(data);
                if (!parsed.isJsonObject()) {
                    return true;
                }
                json = parsed.getAsJsonObject();
            } catch (RuntimeException e) {
                return true;
            }
            String type = event;
            if (type == null || type.isBlank() || "message".equals(type)) {
                JsonElement declared = json.get("type");
                type = declared != null && declared.isJsonPrimitive()
                        ? declared.getAsString() : "message";
            }
            switch (type) {
                case "message_start" -> readUsage(json, sink);
                case "content_block_start" -> startBlock(json, builders, sink);
                case "content_block_delta" -> readDelta(json, builders, sink);
                case "content_block_stop" -> finishBlock(json, builders, sink);
                case "message_delta" -> {
                    JsonObject delta = objectOf(json.get("delta"));
                    if (delta != null) {
                        String reason = stringOf(delta.get("stop_reason"));
                        if (!reason.isEmpty()) {
                            stop[0] = reason;
                        }
                        String title = stringOf(delta.get("title"));
                        if (!title.isBlank()) {
                            sink.title(title);
                        }
                    }
                    readUsage(json, sink);
                }
                case "message_stop" -> {
                    flush(builders, sink);
                    sink.done(stop[0] == null ? "stop" : stop[0]);
                    done[0] = true;
                    return false;
                }
                case "error" -> {
                    JsonObject error = objectOf(json.get("error"));
                    String message = error == null ? ChatProtocol.errorMessage(data)
                            : stringOf(error.get("message"));
                    String errorType = error == null ? "" : stringOf(error.get("type"));
                    boolean retryable = "overloaded_error".equals(errorType)
                            || "api_error".equals(errorType);
                    sink.error("provider_error",
                            message.isBlank() ? "DeepSeek 返回了一个错误" : message, retryable);
                    return false;
                }
                case "ping" -> {
                    // Keep-alive.
                }
                default -> {
                    // Unknown events are ignored; message bodies can add fields later.
                }
            }
            return true;
        });
        if (!done[0]) {
            flush(builders, sink);
            sink.done(stop[0] == null ? "stop" : stop[0]);
        }
    }

    private static void readUsage(JsonObject json, ChatEventSink sink) {
        JsonObject usage = objectOf(json.get("usage"));
        if (usage == null) {
            JsonObject message = objectOf(json.get("message"));
            usage = message == null ? null : objectOf(message.get("usage"));
        }
        if (usage == null) {
            return;
        }
        int input = intOf(usage, "input_tokens", 0);
        int output = intOf(usage, "output_tokens", 0);
        if (input > 0 || output > 0) {
            sink.usage(input, output);
        }
    }

    private static void startBlock(JsonObject json, Map<Integer, BlockBuilder> builders,
                                   ChatEventSink sink) {
        JsonObject block = objectOf(json.get("content_block"));
        if (block == null) {
            return;
        }
        int index = intOf(json, "index", 0);
        String type = stringOf(block.get("type"));
        switch (type) {
            case "text", "thinking" -> {
                BlockBuilder builder = new BlockBuilder(type);
                builders.put(index, builder);
                sink.blockStart(block);
            }
            case "server_tool_use", "web_search_tool_result", "web_fetch_tool_result" ->
                    sink.block(block);
            default -> {
                // tool_use is a client tool; the preset never declares one.
            }
        }
    }

    private static void readDelta(JsonObject json, Map<Integer, BlockBuilder> builders,
                                  ChatEventSink sink) {
        int index = intOf(json, "index", 0);
        BlockBuilder builder = builders.get(index);
        if (builder == null) {
            return;
        }
        JsonObject delta = objectOf(json.get("delta"));
        if (delta == null) {
            return;
        }
        String type = stringOf(delta.get("type"));
        switch (type) {
            case "text_delta" -> {
                String text = stringOf(delta.get("text"));
                builder.text.append(text);
                if (!text.isEmpty()) {
                    sink.text(text);
                }
            }
            case "thinking_delta" -> {
                String thinking = stringOf(delta.get("thinking"));
                builder.text.append(thinking);
                if (!thinking.isEmpty()) {
                    sink.reasoning(thinking);
                }
            }
            case "signature_delta" -> builder.signature = stringOf(delta.get("signature"));
            case "citations_delta" -> {
                JsonObject citation = objectOf(delta.get("citation"));
                if (citation != null) {
                    builder.citations.add(citation);
                }
            }
            case "input_json_delta" -> {
                // Server tools arrive complete at content_block_start.
            }
            default -> {
                // Ignore unknown deltas.
            }
        }
    }

    private static void finishBlock(JsonObject json, Map<Integer, BlockBuilder> builders,
                                    ChatEventSink sink) {
        int index = intOf(json, "index", 0);
        BlockBuilder builder = builders.remove(index);
        if (builder == null) {
            return;
        }
        JsonObject completed = builder.toBlock();
        sink.blockEnd(completed);
        sink.block(completed);
    }

    private static void flush(Map<Integer, BlockBuilder> builders, ChatEventSink sink) {
        List<Map.Entry<Integer, BlockBuilder>> entries = new ArrayList<>(builders.entrySet());
        builders.clear();
        for (Map.Entry<Integer, BlockBuilder> entry : entries) {
            sink.block(entry.getValue().toBlock());
        }
    }

    private static final class BlockBuilder {
        final String type;
        final StringBuilder text = new StringBuilder();
        final JsonArray citations = new JsonArray();
        String signature = "";

        BlockBuilder(String type) {
            this.type = type;
        }

        JsonObject toBlock() {
            JsonObject block = new JsonObject();
            block.addProperty("type", type);
            if ("thinking".equals(type)) {
                block.addProperty("thinking", text.toString());
                block.addProperty("signature", signature);
            } else {
                block.addProperty("text", text.toString());
                if (!citations.isEmpty()) {
                    block.add("citations", citations);
                }
            }
            return block;
        }
    }

    private static JsonObject objectOf(JsonElement element) {
        return element != null && element.isJsonObject() ? element.getAsJsonObject() : null;
    }

    private static String stringOf(JsonElement element) {
        return element != null && element.isJsonPrimitive() ? element.getAsString() : "";
    }

    private static int intOf(JsonObject object, String name, int fallback) {
        JsonElement element = object.get(name);
        return element != null && element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()
                ? element.getAsInt() : fallback;
    }
}