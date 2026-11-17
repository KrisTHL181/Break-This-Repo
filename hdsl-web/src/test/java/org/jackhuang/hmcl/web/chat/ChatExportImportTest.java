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
import org.jackhuang.hmcl.web.TestSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// 导入/导出：DeepSeek 官方导出包能被解析成会话，导出再导入能原样回来，
/// 坏的包在落库前就被拒绝。
class ChatExportImportTest {

    @Test
    void deepseekArchivesImportExportAndRoundTrip(@TempDir Path dataDir) throws Exception {
        try (TestSupport.RunningServer running = TestSupport.start(dataDir, Map.of(),
                config -> {
                    config.auth.disabled = true;
                    config.bindHost = "127.0.0.1";
                })) {
            // 1. DeepSeek 导出包（manifest v3 + 一条两轮对话，含思考）
            byte[] archive = deepseekZip(3);
            HttpResponse<String> imported = importBytes(running, archive, null, null);
            assertEquals(201, imported.statusCode(), imported.body());
            JsonObject result = JsonParser.parseString(imported.body()).getAsJsonObject();
            assertEquals("deepseek", result.get("format").getAsString());
            String sessionId = result.getAsJsonArray("imported").get(0).getAsJsonObject()
                    .get("id").getAsString();

            JsonObject detail = get(running, "/api/chat/sessions/" + sessionId);
            assertEquals(2, detail.getAsJsonArray("nodes").size());
            assertEquals("导出的会话", detail.get("title").getAsString());
            JsonObject assistant = activeMessage(detail, 1);
            assertEquals("你好呀", assistant.get("content").getAsString());
            assertEquals("想一想", assistant.get("reasoning").getAsString());
            assertEquals("done", assistant.get("status").getAsString());

            // 2. 导出成 HSDL 信封
            HttpResponse<String> exported = getRaw(running, "/api/chat/sessions/" + sessionId + "/export");
            assertEquals(200, exported.statusCode(), exported.body());
            assertTrue(exported.headers().firstValue("content-disposition").orElse("")
                    .startsWith("attachment;"), "export must download, not open inline");
            JsonObject envelope = JsonParser.parseString(exported.body()).getAsJsonObject();
            assertEquals("hdsl-chat-session", envelope.get("format").getAsString());
            assertEquals(1, envelope.get("version").getAsInt());
            assertEquals(2, envelope.getAsJsonArray("nodes").size());

            // 3. 再导回来，正文一致、会话是新的
            HttpResponse<String> reimported = importBytes(running,
                    exported.body().getBytes(StandardCharsets.UTF_8), null, null);
            assertEquals(201, reimported.statusCode(), reimported.body());
            JsonObject hedge = JsonParser.parseString(reimported.body()).getAsJsonObject();
            assertEquals("hdsl", hedge.get("format").getAsString());
            String copyId = hedge.getAsJsonArray("imported").get(0).getAsJsonObject()
                    .get("id").getAsString();
            assertNotEquals(sessionId, copyId);
            JsonObject copy = get(running, "/api/chat/sessions/" + copyId);
            assertEquals(activeContents(detail), activeContents(copy));

            // 4. 校验：版本不对 / 不是 JSON 都在落库前拒绝
            HttpResponse<String> oldVersion = importBytes(running, deepseekZip(2), null, null);
            assertEquals(400, oldVersion.statusCode(), oldVersion.body());
            assertTrue(oldVersion.body().contains("版本"), oldVersion.body());
            HttpResponse<String> garbage = importBytes(running,
                    "not a chat archive".getBytes(StandardCharsets.UTF_8), null, null);
            assertEquals(400, garbage.statusCode(), garbage.body());

            // 5. 导入到指定助手
            JsonObject target = new JsonObject();
            target.addProperty("name", "导入目标");
            String targetId = post(running, "/api/chat/assistants", target).get("id").getAsString();
            HttpResponse<String> toTarget = importBytes(running, archive, targetId, "自定义标题");
            assertEquals(201, toTarget.statusCode(), toTarget.body());
            JsonObject importedTarget = JsonParser.parseString(toTarget.body()).getAsJsonObject()
                    .getAsJsonArray("imported").get(0).getAsJsonObject();
            assertEquals("自定义标题", importedTarget.get("title").getAsString());
            assertEquals(targetId, get(running, "/api/chat/sessions/"
                    + importedTarget.get("id").getAsString()).get("assistantId").getAsString());
        }
    }

    // ---------------------------------------------------------------- fixtures --

    private static byte[] deepseekZip(int version) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
            zip.putNextEntry(new ZipEntry("manifest.json"));
            zip.write(("{\"export_format_version\":" + version + "}").getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
            zip.putNextEntry(new ZipEntry("chat_sessions/s1.json"));
            zip.write(deepseekSession().getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }
        return bytes.toByteArray();
    }

    private static String deepseekSession() {
        return """
                {"id":"s1","title":"导出的会话","pinned":false,"current_message_id":2,
                 "inserted_at":"2026-11-10T00:00:00.000Z","updated_at":"2026-11-10T00:01:00.000Z",
                 "messages":[
                   {"id":1,"parent_id":null,"role":"USER","status":"FINISHED",
                    "thinking_enabled":false,"search_enabled":false,"accumulated_token_usage":0,
                    "inserted_at":"2026-11-10T00:00:00.000Z",
                    "fragments":[{"type":"REQUEST","content":"你好"}]},
                   {"id":2,"parent_id":1,"role":"ASSISTANT","status":"FINISHED",
                    "thinking_enabled":true,"search_enabled":false,"accumulated_token_usage":10,
                    "inserted_at":"2026-11-10T00:00:30.000Z",
                    "fragments":[
                      {"type":"THINK","content":"想一想","elapsed_secs":1},
                      {"type":"RESPONSE","content":"你好呀"}
                    ]}
                 ]}""";
    }

    // ---------------------------------------------------------------- helpers --

    private static HttpResponse<String> importBytes(TestSupport.RunningServer running, byte[] file,
                                                    @org.jetbrains.annotations.Nullable String assistantId,
                                                    @org.jetbrains.annotations.Nullable String title)
            throws Exception {
        StringBuilder path = new StringBuilder("/api/chat/import?assistantId=")
                .append(assistantId == null ? "default" : assistantId);
        if (title != null) {
            path.append("&title=").append(java.net.URLEncoder.encode(title, StandardCharsets.UTF_8));
        }
        HttpRequest request = HttpRequest.newBuilder(URI.create(running.baseUrl() + path))
                .timeout(Duration.ofSeconds(20))
                .header("Content-Type", "application/octet-stream")
                .POST(HttpRequest.BodyPublishers.ofByteArray(file))
                .build();
        return TestSupport.client().send(request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private static JsonObject activeMessage(JsonObject session, int nodeIndex) {
        JsonObject node = session.getAsJsonArray("nodes").get(nodeIndex).getAsJsonObject();
        String active = node.get("activeVariantId").getAsString();
        for (var variant : node.getAsJsonArray("variants")) {
            JsonObject message = variant.getAsJsonObject();
            if (message.get("id").getAsString().equals(active)) {
                return message;
            }
        }
        throw new AssertionError("no active variant on node " + nodeIndex);
    }

    private static JsonArray activeContents(JsonObject session) {
        JsonArray out = new JsonArray();
        for (int i = 0; i < session.getAsJsonArray("nodes").size(); i++) {
            out.add(activeMessage(session, i).get("content").getAsString());
        }
        return out;
    }

    private static JsonObject get(TestSupport.RunningServer running, String path) throws Exception {
        HttpResponse<String> response = getRaw(running, path);
        assertEquals(200, response.statusCode(), response.body());
        return JsonParser.parseString(response.body()).getAsJsonObject();
    }

    private static HttpResponse<String> getRaw(TestSupport.RunningServer running, String path)
            throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(running.baseUrl() + path))
                .timeout(Duration.ofSeconds(15)).GET().build();
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