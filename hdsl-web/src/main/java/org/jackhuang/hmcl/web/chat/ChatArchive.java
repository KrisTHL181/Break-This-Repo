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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/// 会话导入的解析与校验（导出格式见 [ChatStore#exportJson]）。
///
/// 支持两种输入，按文件头自动识别：
///
/// - **DeepSeek 官方导出**（`deepseek_data-*.zip`）：`manifest.json` +
///   `chat_sessions/<id>.json`。消息是一棵树（`parent_id`），按
///   `current_message_id` 回溯成线性对话；`REQUEST`/`RESPONSE` 拼正文，
///   `THINK` 拼思考。格式版本只接受 3。
/// - **HSDL 自己的单会话 JSON**（`format: hdsl-chat-session`）。
///
/// 校验先于落库，且全部有上限：文件、条目、会话数、消息数、片段数、单段文本。
/// 解析失败只产生"跳过"的原因，不会把半截会话写进库。
@NotNullByDefault
public final class ChatArchive {

    /// 上传文件的上限。
    public static final int MAX_FILE_BYTES = 32 * 1024 * 1024;
    /// 单个 zip 条目解压后的上限。
    static final int MAX_ENTRY_BYTES = 16 * 1024 * 1024;
    /// 一个 zip 所有条目解压后的总上限。
    static final int MAX_TOTAL_BYTES = 64 * 1024 * 1024;
    static final int MAX_ENTRIES = 5000;
    static final int MAX_SESSIONS = 100;
    static final int MAX_NODES = 2000;
    static final int MAX_FRAGMENTS = 5000;
    static final int MAX_TEXT = 2 * 1024 * 1024;

    /// DeepSeek 导出的格式版本；其它版本字段语义可能不同，宁可拒绝。
    static final int DEEPSEEK_FORMAT_VERSION = 3;

    /// 一条导入进来的消息（已是最终正文，没有 variants）。
    public record ImportedNode(String role, String content, String reasoning, String status,
                               long createdAt) {
    }

    /// 一个待导入的会话。
    public record ImportedSession(String title, @Nullable String model,
                                  @Nullable String account, @Nullable String systemPrompt,
                                  List<ImportedNode> nodes) {
    }

    /// 解析结果；`skipped` 是逐会话给出的原因，`format` 是识别出的来源。
    public record Parsed(String format, List<ImportedSession> sessions, List<String> skipped) {
    }

    private ChatArchive() {
    }

    /// @throws IOException 当文件不是受支持的格式、或结构/版本校验失败
    public static Parsed parse(byte[] file) throws IOException {
        if (file.length == 0) {
            throw new IOException("文件是空的");
        }
        if (file.length > MAX_FILE_BYTES) {
            throw new IOException("文件超过 " + (MAX_FILE_BYTES / (1024 * 1024)) + " MB");
        }
        if (file.length >= 4 && file[0] == 'P' && file[1] == 'K'
                && (file[2] == 3 || file[2] == 5) && (file[3] == 4 || file[3] == 6)) {
            return parseDeepSeekZip(file);
        }
        return parseHdslJson(new String(file, StandardCharsets.UTF_8));
    }

    // ------------------------------------------------------------ DeepSeek --

    private static Parsed parseDeepSeekZip(byte[] file) throws IOException {
        Map<String, byte[]> entries = readZip(file);
        byte[] manifest = entries.get("manifest.json");
        if (manifest == null) {
            throw new IOException("不是 DeepSeek 导出包：缺少 manifest.json");
        }
        JsonObject meta;
        try {
            meta = JsonParser.parseString(new String(manifest, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (RuntimeException e) {
            throw new IOException("manifest.json 不是 JSON 对象");
        }
        JsonElement version = meta.get("export_format_version");
        if (version == null || !version.isJsonPrimitive()
                || !version.getAsJsonPrimitive().isNumber()
                || version.getAsInt() != DEEPSEEK_FORMAT_VERSION) {
            throw new IOException("不支持的导出格式版本（需要 "
                    + DEEPSEEK_FORMAT_VERSION + "）");
        }

        List<ImportedSession> sessions = new ArrayList<>();
        List<String> skipped = new ArrayList<>();
        int seen = 0;
        for (Map.Entry<String, byte[]> entry : entries.entrySet()) {
            String name = entry.getKey();
            if (!name.startsWith("chat_sessions/") || !name.endsWith(".json")) {
                continue;
            }
            if (++seen > MAX_SESSIONS) {
                skipped.add("会话数量超过 " + MAX_SESSIONS + "，其余未导入");
                break;
            }
            try {
                sessions.add(readDeepSeekSession(name, entry.getValue()));
            } catch (IOException e) {
                skipped.add(name + "：" + e.getMessage());
            }
        }
        if (sessions.isEmpty() && skipped.isEmpty()) {
            throw new IOException("导出包里没有 chat_sessions/*.json");
        }
        if (sessions.isEmpty()) {
            throw new IOException("没有可导入的会话：" + String.join("；", skipped));
        }
        return new Parsed("deepseek", sessions, skipped);
    }

    private static Map<String, byte[]> readZip(byte[] file) throws IOException {
        Map<String, byte[]> entries = new LinkedHashMap<>();
        long total = 0;
        int count = 0;
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(file),
                StandardCharsets.UTF_8)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }
                if (++count > MAX_ENTRIES) {
                    throw new IOException("压缩包条目过多");
                }
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] buffer = new byte[8192];
                int read;
                while ((read = zip.read(buffer)) > 0) {
                    out.write(buffer, 0, read);
                    if (out.size() > MAX_ENTRY_BYTES) {
                        throw new IOException("条目 " + entry.getName() + " 解压后超过 "
                                + (MAX_ENTRY_BYTES / (1024 * 1024)) + " MB");
                    }
                }
                total += out.size();
                if (total > MAX_TOTAL_BYTES) {
                    throw new IOException("压缩包解压后总量超过 "
                            + (MAX_TOTAL_BYTES / (1024 * 1024)) + " MB");
                }
                entries.put(entry.getName(), out.toByteArray());
            }
        }
        return entries;
    }

    private static ImportedSession readDeepSeekSession(String name, byte[] bytes) throws IOException {
        if (bytes.length > MAX_ENTRY_BYTES) {
            throw new IOException("会话文件过大");
        }
        JsonObject root;
        try {
            root = JsonParser.parseString(new String(bytes, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (RuntimeException e) {
            throw new IOException("不是 JSON 对象");
        }
        String title = string(root, "title");
        JsonElement current = root.get("current_message_id");
        if (current == null || !current.isJsonPrimitive() || !current.getAsJsonPrimitive().isNumber()) {
            throw new IOException("缺少 current_message_id");
        }
        long currentId = current.getAsLong();

        JsonArray messages = array(root, "messages");
        if (messages == null) {
            throw new IOException("缺少 messages");
        }
        if (messages.size() > MAX_NODES) {
            throw new IOException("消息超过 " + MAX_NODES + " 条");
        }
        Map<Long, JsonObject> byId = new HashMap<>();
        for (JsonElement element : messages) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject message = element.getAsJsonObject();
            JsonElement id = message.get("id");
            if (id != null && id.isJsonPrimitive() && id.getAsJsonPrimitive().isNumber()) {
                byId.put(id.getAsLong(), message);
            }
        }
        // 从 current_message_id 沿 parent_id 回溯到根，再反转成线性顺序。
        List<JsonObject> path = new ArrayList<>();
        long cursor = currentId;
        int guard = 0;
        while (byId.containsKey(cursor) && guard++ <= MAX_NODES) {
            JsonObject message = byId.get(cursor);
            path.add(0, message);
            JsonElement parent = message.get("parent_id");
            if (parent == null || parent.isJsonNull() || !parent.isJsonPrimitive()
                    || !parent.getAsJsonPrimitive().isNumber()) {
                break;
            }
            cursor = parent.getAsLong();
        }
        if (path.isEmpty()) {
            throw new IOException("current_message_id 不在消息树里");
        }

        List<ImportedNode> nodes = new ArrayList<>();
        for (JsonObject message : path) {
            String role = string(message, "role");
            boolean assistant = "ASSISTANT".equals(role);
            if (!assistant && !"USER".equals(role)) {
                continue;
            }
            StringBuilder content = new StringBuilder();
            StringBuilder thinking = new StringBuilder();
            JsonArray fragments = array(message, "fragments");
            if (fragments != null) {
                if (fragments.size() > MAX_FRAGMENTS) {
                    throw new IOException("单条消息的片段过多");
                }
                for (JsonElement element : fragments) {
                    if (!element.isJsonObject()) {
                        continue;
                    }
                    JsonObject fragment = element.getAsJsonObject();
                    String type = string(fragment, "type");
                    String text = string(fragment, "content");
                    if (text.isBlank()) {
                        continue;
                    }
                    switch (type) {
                        case "REQUEST", "RESPONSE" -> append(content, text);
                        case "THINK" -> append(thinking, text);
                        // TEMPLATE_RESPONSE / TIP / CONTENT_RISK / TOOL_SEARCH /
                        // PARTIAL_FILE_READ 是展示附属物，不是对话正文。
                        default -> {
                        }
                    }
                }
            }
            String body = content.toString();
            String reason = thinking.toString();
            if (body.isBlank() && reason.isBlank()) {
                continue;
            }
            if (body.length() > MAX_TEXT || reason.length() > MAX_TEXT) {
                throw new IOException("单条消息正文超过 " + (MAX_TEXT / 1024 / 1024) + " MB");
            }
            nodes.add(new ImportedNode(assistant ? "assistant" : "user", body, reason,
                    statusOf(string(message, "status")),
                    timeOf(string(message, "inserted_at"))));
        }
        if (nodes.isEmpty()) {
            throw new IOException("没有可用的对话内容");
        }
        return new ImportedSession(title.isBlank() ? "导入会话" : title, null, null, null, nodes);
    }

    private static void append(StringBuilder target, String text) {
        if (target.length() > 0) {
            target.append('\n');
        }
        target.append(text);
    }

    private static String statusOf(String deepseek) {
        return switch (deepseek) {
            case "WIP" -> "interrupted";
            case "FAILED", "TIMEOUT", "CONTENT_FILTER", "CONTEXT_LENGTH_EXCEEDED" -> "error";
            default -> "done";
        };
    }

    private static long timeOf(String iso) {
        try {
            return Instant.parse(iso).toEpochMilli();
        } catch (DateTimeParseException | NullPointerException e) {
            return System.currentTimeMillis();
        }
    }

    // ---------------------------------------------------------------- HSDL --

    private static Parsed parseHdslJson(String body) throws IOException {
        JsonObject root;
        try {
            root = JsonParser.parseString(body).getAsJsonObject();
        } catch (RuntimeException e) {
            throw new IOException("文件既不是 DeepSeek 导出包，也不是 HSDL 会话 JSON");
        }
        if (!"hdsl-chat-session".equals(string(root, "format"))) {
            throw new IOException("不认识的 JSON 格式（缺少 format: hdsl-chat-session）");
        }
        JsonElement version = root.get("version");
        if (version == null || !version.isJsonPrimitive() || version.getAsInt() != 1) {
            throw new IOException("不支持的 HSDL 会话版本");
        }
        JsonObject session = root.has("session") && root.get("session").isJsonObject()
                ? root.getAsJsonObject("session") : new JsonObject();
        JsonArray nodeArray = array(root, "nodes");
        if (nodeArray == null) {
            throw new IOException("缺少 nodes");
        }
        if (nodeArray.size() > MAX_NODES) {
            throw new IOException("消息超过 " + MAX_NODES + " 条");
        }
        List<ImportedNode> nodes = new ArrayList<>();
        for (JsonElement element : nodeArray) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject node = element.getAsJsonObject();
            String role = string(node, "role");
            boolean assistant = "assistant".equals(role);
            if (!assistant && !"user".equals(role)) {
                continue;
            }
            JsonArray variants = array(node, "variants");
            if (variants == null || variants.isEmpty()) {
                continue;
            }
            int active = 0;
            JsonElement index = node.get("activeVariantIndex");
            if (index != null && index.isJsonPrimitive() && index.getAsJsonPrimitive().isNumber()) {
                active = Math.max(0, Math.min(variants.size() - 1, index.getAsInt()));
            }
            JsonElement chosen = variants.get(active);
            if (!chosen.isJsonObject()) {
                continue;
            }
            JsonObject variant = chosen.getAsJsonObject();
            String content = string(variant, "content");
            String reasoning = string(variant, "reasoning");
            if (content.length() > MAX_TEXT || reasoning.length() > MAX_TEXT) {
                throw new IOException("单条消息正文超过 " + (MAX_TEXT / 1024 / 1024) + " MB");
            }
            String status = string(variant, "status");
            nodes.add(new ImportedNode(assistant ? "assistant" : "user", content, reasoning,
                    status.isBlank() ? "done" : status,
                    longOr(variant, "createdAt", System.currentTimeMillis())));
        }
        if (nodes.isEmpty()) {
            throw new IOException("没有可用的对话内容");
        }
        String title = string(session, "title");
        return new Parsed("hdsl", List.of(new ImportedSession(
                title.isBlank() ? "导入会话" : title,
                nullableString(session, "model"),
                nullableString(session, "account"),
                nullableString(session, "systemPrompt"),
                nodes)), List.of());
    }

    // --------------------------------------------------------------- utils --

    private static JsonArray array(JsonObject object, String name) {
        JsonElement element = object.get(name);
        return element != null && element.isJsonArray() ? element.getAsJsonArray() : null;
    }

    private static String string(JsonObject object, String name) {
        JsonElement element = object.get(name);
        return element != null && element.isJsonPrimitive() ? element.getAsString() : "";
    }

    private static @Nullable String nullableString(JsonObject object, String name) {
        JsonElement element = object.get(name);
        return element != null && element.isJsonPrimitive() ? element.getAsString() : null;
    }

    private static long longOr(JsonObject object, String name, long fallback) {
        JsonElement element = object.get(name);
        return element != null && element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()
                ? element.getAsLong() : fallback;
    }
}