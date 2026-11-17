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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.UUID;

/// DeepSeek Files API 的最小封装。
///
/// 图片在扩展模式下必须用 `file_id` 引用，不能直接塞 base64；上传结果按附件缓存，
/// 同一张图在后续轮次不会重传。文档仍走面板的文本抽取，不在这里上传。
@NotNullByDefault
public final class DeepseekFiles {

    private static final String FILES_BETA = "files-api-2025-04-14";
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    private DeepseekFiles() {
    }

    /// 返回可放进 image block 的 `file_id`；已有缓存就直接复用。
    public static String ensure(ChatStore store, ChatEndpoints.Profile profile,
                                ChatStore.Attachment attachment)
            throws IOException, ChatException, java.sql.SQLException {
        String cached = store.remoteFileId(attachment.id());
        if (cached != null && !cached.isBlank()) {
            return cached;
        }
        Path path = Path.of(attachment.path());
        if (!Files.isRegularFile(path)) {
            throw new ChatException(422, "attachment_missing",
                    "附件文件不存在：" + attachment.name());
        }
        byte[] bytes = Files.readAllBytes(path);
        String boundary = "----hdsl-" + UUID.randomUUID();
        byte[] body = multipart(boundary, attachment, bytes);
        String url = profile.base() + "/anthropic/v1/files";
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofMinutes(2))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .header("anthropic-beta", FILES_BETA)
                .header("User-Agent", org.jackhuang.hmcl.Metadata.NAME + "/"
                        + org.jackhuang.hmcl.Metadata.VERSION)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body));
        ChatEndpoints.applyAuth(builder, profile);
        HttpResponse<String> response = send(builder.build());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new ChatException(response.statusCode() == 413 ? 413 : 502,
                    "file_upload_failed",
                    "DeepSeek Files API 返回 HTTP " + response.statusCode() + ": "
                            + excerpt(response.body()));
        }
        String id = fileId(response.body());
        if (id.isBlank()) {
            throw new ChatException(502, "file_upload_failed",
                    "DeepSeek Files API 没有返回 file id");
        }
        store.setRemoteFile(attachment.id(), id, null);
        return id;
    }

    private static HttpResponse<String> send(HttpRequest request) throws IOException {
        try {
            return CLIENT.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("interrupted", e);
        }
    }

    private static byte[] multipart(String boundary, ChatStore.Attachment attachment,
                                    byte[] bytes) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        String safeName = attachment.name() == null || attachment.name().isBlank()
                ? "upload" : attachment.name().replaceAll("[\r\n\"]", "_");
        write(out, "--" + boundary + "\r\n");
        write(out, "Content-Disposition: form-data; name=\"file\"; filename=\"" + safeName + "\"\r\n");
        write(out, "Content-Type: " + (attachment.mime() == null || attachment.mime().isBlank()
                ? "application/octet-stream" : attachment.mime()) + "\r\n\r\n");
        out.write(bytes);
        write(out, "\r\n--" + boundary + "--\r\n");
        return out.toByteArray();
    }

    private static void write(ByteArrayOutputStream out, String text) throws IOException {
        out.write(text.getBytes(StandardCharsets.UTF_8));
    }

    private static String fileId(String body) {
        if (body == null || body.isBlank()) {
            return "";
        }
        try {
            JsonElement parsed = JsonParser.parseString(body);
            if (!parsed.isJsonObject()) {
                return "";
            }
            JsonObject json = parsed.getAsJsonObject();
            JsonElement id = json.get("id");
            return id != null && id.isJsonPrimitive() ? id.getAsString() : "";
        } catch (RuntimeException e) {
            return "";
        }
    }

    private static String excerpt(@Nullable String body) {
        if (body == null || body.isBlank()) {
            return "";
        }
        String text = body.replaceAll("\\s+", " ").trim();
        return text.length() > 300 ? text.substring(0, 300) + "…" : text;
    }
}