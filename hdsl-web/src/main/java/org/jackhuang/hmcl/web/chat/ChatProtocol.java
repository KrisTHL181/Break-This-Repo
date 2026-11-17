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
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

/// 协议适配的公共模型（docs/chat.md §5）。
///
/// 会话、turn 和存储只说这一套结构；OpenAI 与 Anthropic 的差异全部留在各自的
/// adapter 里。
@NotNullByDefault
public interface ChatProtocol {

    String id();

    HttpRequest prepare(ChatEndpoints.Profile profile, ChatRequest request) throws IOException;

    void read(InputStream body, ChatEventSink sink) throws IOException;

    boolean supportsDocuments();

    record ChatRequest(
            String model,
            String system,
            List<ChatMessage> messages,
            Double temperature,
            Integer maxTokens,
            String reasoningLevel,
            @Nullable ChatCapabilities.ReasoningMode reasoning,
            List<ToolSpec> tools,
            boolean searchEnabled,
            boolean titleEnabled) {

        public ChatRequest(String model, String system, List<ChatMessage> messages,
                           Double temperature, Integer maxTokens, String reasoningLevel,
                           @Nullable ChatCapabilities.ReasoningMode reasoning) {
            this(model, system, messages, temperature, maxTokens, reasoningLevel, reasoning,
                    List.of(), false, false);
        }

        public ChatRequest(String model, String system, List<ChatMessage> messages,
                           Double temperature, Integer maxTokens, String reasoningLevel,
                           @Nullable ChatCapabilities.ReasoningMode reasoning,
                           List<ToolSpec> tools) {
            this(model, system, messages, temperature, maxTokens, reasoningLevel, reasoning,
                    tools, false, false);
        }

        public boolean hasTools() {
            return !tools.isEmpty();
        }
    }

    /// 下发给模型的工具声明。`parametersJson` 是 JSON Schema 文本，由适配器转成
    /// 各家自己的形状（OpenAI 的 `function.parameters`、Anthropic 的 `input_schema`）。
    record ToolSpec(String name, String description, String parametersJson) {
    }

    /// 模型请求的一次工具调用。`arguments` 是模型给的原始 JSON 文本，可能不是合法 JSON，
    /// 执行方自己兜底。
    record ToolCall(String id, String name, String arguments) {
    }

    record ChatMessage(String role, String text, List<Attachment> attachments,
                       List<ToolCall> toolCalls, @Nullable String toolCallId,
                       List<JsonObject> blocks) {

        public ChatMessage(String role, String text, List<Attachment> attachments) {
            this(role, text, attachments, List.of(), null, List.of());
        }

        public ChatMessage(String role, String text, List<Attachment> attachments,
                           List<ToolCall> toolCalls, @Nullable String toolCallId) {
            this(role, text, attachments, toolCalls, toolCallId, List.of());
        }

        public boolean hasToolCalls() {
            return !toolCalls.isEmpty();
        }
    }

    /// 一条附件在发送时的形态。`text` 是抽取出来的文字（Office/PDF/长文本），
    /// `path` 是原文件（图片与原生 document 用）；`remoteId` 是 DeepSeek Files API
    /// 返回的 `file_id`，有它时图片按 file block 引用。
    record Attachment(
            String name, String mime, String kind, long size, Path path, String text,
            @Nullable String remoteId) {

        public Attachment(String name, String mime, String kind, long size, Path path, String text) {
            this(name, mime, kind, size, path, text, null);
        }
    }

    interface ChatEventSink {
        void text(String delta);

        void reasoning(String delta);

        void usage(int input, int output);

        void done(String finishReason);

        void error(String code, String message, boolean retryable);

        /// 本轮结束时模型请求的工具调用。默认空实现，适配器之外没人需要关心。
        default void toolCalls(List<ToolCall> calls) {
        }

        /// 一个完整的内容块。DeepSeek 的服务端工具、思考签名和正文引用都走这里；
        /// 其他协议的适配器不会调用。
        default void block(JsonObject block) {
        }

        /// 内容块的开始与结束。DeepSeek 用它们标记思考段的边界；
        /// 前端据此把“思考 → 搜索 → 思考”画成多段，其他协议不调用。
        default void blockStart(JsonObject block) {
        }

        default void blockEnd(JsonObject block) {
        }

        /// 服务端生成的会话标题。
        default void title(String title) {
        }
    }

    /// 把非 2xx 的应答体压成一句话；key 已经不在 URL 上，正文里的版本号之类不需要。
    static String httpError(int status, InputStream body) {
        String text;
        try {
            text = new String(body.readNBytes(8192), StandardCharsets.UTF_8)
                    .replaceAll("\\s+", " ").trim();
        } catch (IOException e) {
            text = "";
        }
        if (text.length() > 300) {
            text = text.substring(0, 300) + "…";
        }
        String code;
        boolean retryable;
        if (status == 401 || status == 403) {
            code = "auth_failed";
            retryable = false;
        } else if (status == 429) {
            code = "rate_limited";
            retryable = true;
        } else if (status == 408 || status >= 500) {
            code = "provider_unavailable";
            retryable = true;
        } else if (status == 404) {
            code = "endpoint_not_found";
            retryable = false;
        } else {
            code = "bad_request";
            retryable = false;
        }
        String message = "provider answered HTTP " + status
                + (text.isEmpty() ? "" : ": " + text);
        return code + "\u0000" + retryable + "\u0000" + message;
    }

    static void emitHttpError(int status, InputStream body, ChatEventSink sink) {
        String encoded = httpError(status, body);
        String[] parts = encoded.split("\u0000", 3);
        sink.error(parts[0], parts[2], Boolean.parseBoolean(parts[1]));
    }

    /// 提取 `error.message` / `message` / 字符串 `error`，给 SSE 内的错误事件用。
    static String errorMessage(String json) {
        try {
            com.google.gson.JsonElement parsed = com.google.gson.JsonParser.parseString(json);
            if (!parsed.isJsonObject()) {
                return json.length() > 300 ? json.substring(0, 300) : json;
            }
            com.google.gson.JsonObject root = parsed.getAsJsonObject();
            com.google.gson.JsonElement error = root.get("error");
            if (error != null) {
                if (error.isJsonObject()) {
                    com.google.gson.JsonElement message = error.getAsJsonObject().get("message");
                    if (message != null && message.isJsonPrimitive()) {
                        return message.getAsString();
                    }
                    com.google.gson.JsonElement code = error.getAsJsonObject().get("code");
                    if (code != null && code.isJsonPrimitive()) {
                        return code.getAsString();
                    }
                } else if (error.isJsonPrimitive()) {
                    return error.getAsString();
                }
            }
            com.google.gson.JsonElement message = root.get("message");
            if (message != null && message.isJsonPrimitive()) {
                return message.getAsString();
            }
        } catch (RuntimeException ignored) {
            // Fall through.
        }
        return json.length() > 300 ? json.substring(0, 300) : json;
    }
}