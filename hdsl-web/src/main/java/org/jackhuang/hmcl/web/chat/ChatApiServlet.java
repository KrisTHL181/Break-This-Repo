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
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import org.jackhuang.hmcl.web.auth.AuthFilter;
import org.jackhuang.hmcl.web.auth.AuthService;
import org.jackhuang.hmcl.web.config.ServerConfig;
import org.jackhuang.hmcl.web.developer.DeveloperElevation;
import org.jackhuang.hmcl.web.http.Json;
import org.jackhuang.hmcl.web.http.RequestSecurity;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

/// `/api/chat/*`（docs/chat.md §10）。
///
/// 会话、消息与附件走 REST；token 流走 `/ws` 的 `chat:<id>` topic，这里不发流。
/// agent ticket 按 `chat-read` / `chat-write` / `chat-search` 三个 scope 开放
/// （见 [org.jackhuang.hmcl.web.developer.AgentScopes]）；设置、备份、端点探测与
/// 能力覆盖仍然要求开发者提权，`chat-write` 的输入 token 上限由聊天设置控制。
@NotNullByDefault
public final class ChatApiServlet extends HttpServlet {

    private static final int MAX_JSON_BYTES = 2 * 1024 * 1024;

    private final ChatService chat;
    private final ServerConfig config;
    private final AuthService auth;
    private final DeveloperElevation elevation;
    private final BooleanSupplier tlsActive;

    public ChatApiServlet(ChatService chat, ServerConfig config, AuthService auth,
                          DeveloperElevation elevation, BooleanSupplier tlsActive) {
        this.chat = chat;
        this.config = config;
        this.auth = auth;
        this.elevation = elevation;
        this.tlsActive = tlsActive;
    }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String path = request.getPathInfo() == null ? "/" : request.getPathInfo();
        String method = request.getMethod();
        try {
            route(request, response, path, method);
        } catch (ChatException e) {
            JsonObject body = new JsonObject();
            body.addProperty("error", e.getMessage());
            body.addProperty("code", e.code());
            Json.write(response, e.status(), body);
        } catch (SQLException e) {
            org.jackhuang.hmcl.util.logging.Logger.LOG.warning("Chat store failed", e);
            Json.error(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "chat store failed: " + e.getMessage());
        } catch (IOException e) {
            // A client that left mid-download is not an API failure worth a 500 body.
            if (!response.isCommitted()) {
                Json.error(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
            }
        } catch (RuntimeException e) {
            org.jackhuang.hmcl.util.logging.Logger.LOG.warning("Chat request failed", e);
            Json.error(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
        }
    }

    private void route(HttpServletRequest request, HttpServletResponse response,
                       String path, String method) throws IOException, SQLException, ChatException {
        String route = path.length() > 1 && path.endsWith("/")
                ? path.substring(0, path.length() - 1) : path;

        if ("/sessions".equals(route)) {
            if ("GET".equals(method)) {
                String assistant = request.getParameter("assistant");
                JsonObject body = new JsonObject();
                body.add("sessions", chat.sessions(assistant == null || assistant.isBlank()
                        ? null : assistant));
                Json.write(response, body);
                return;
            }
            if ("POST".equals(method)) {
                JsonObject body = readBody(request, response);
                if (body != null) {
                    Json.write(response, HttpServletResponse.SC_CREATED, chat.createSession(body));
                }
                return;
            }
            method(response, "GET, POST");
            return;
        }

        if (route.startsWith("/sessions/")) {
            String rest = route.substring("/sessions/".length());
            int slash = rest.indexOf('/');
            String sessionId = slash < 0 ? rest : rest.substring(0, slash);
            String tail = slash < 0 ? "" : rest.substring(slash);
            if (tail.isEmpty()) {
                if ("GET".equals(method)) {
                    Json.write(response, chat.sessionJson(sessionId));
                    return;
                }
                if ("PATCH".equals(method)) {
                    JsonObject body = readBody(request, response);
                    if (body != null) {
                        Json.write(response, chat.updateSession(sessionId, body));
                    }
                    return;
                }
                if ("DELETE".equals(method)) {
                    chat.deleteSession(sessionId);
                    Json.write(response, java.util.Map.of("ok", true));
                    return;
                }
                method(response, "GET, PATCH, DELETE");
                return;
            }
            if ("/messages".equals(tail) && "POST".equals(method)) {
                JsonObject body = readBody(request, response);
                if (body == null) {
                    return;
                }
                String text = string(body, "text", "");
                List<String> attachments = strings(body, "attachments");
                boolean search = body.has("search") && body.get("search").isJsonPrimitive()
                        && body.get("search").getAsBoolean();
                if (search && !agentCanSearch(request)) {
                    Json.error(response, HttpServletResponse.SC_FORBIDDEN,
                            "agent_scope_denied: search requires the chat-search scope");
                    return;
                }
                int agentLimit = AuthFilter.isAgent(request)
                        ? chat.settings().agentChatMaxInputTokens() : 0;
                ChatTurnManager.Outcome outcome = chat.turns().send(sessionId, text, attachments,
                        nullable(body, "model"), nullable(body, "account"),
                        nullable(body, "requestId"), search, agentLimit,
                        nullable(body, "searchProvider"));
                Json.write(response, outcomeJson(outcome));
                return;
            }
            if ("/regenerate".equals(tail) && "POST".equals(method)) {
                JsonObject body = readBody(request, response);
                if (body == null) {
                    return;
                }
                String nodeId = required(body, "nodeId");
                String mode = string(body, "mode", "truncate_after");
                Boolean search = body.has("search") && body.get("search").isJsonPrimitive()
                        ? body.get("search").getAsBoolean() : null;
                ChatTurnManager.Outcome outcome = chat.turns().regenerate(sessionId, nodeId, mode,
                        nullable(body, "requestId"), search, nullable(body, "searchProvider"));
                Json.write(response, outcomeJson(outcome));
                return;
            }
            if ("/edit".equals(tail) && "POST".equals(method)) {
                JsonObject body = readBody(request, response);
                if (body == null) {
                    return;
                }
                String nodeId = required(body, "nodeId");
                String text = string(body, "text", "");
                int agentLimit = AuthFilter.isAgent(request)
                        ? chat.settings().agentChatMaxInputTokens() : 0;
                ChatTurnManager.Outcome outcome = chat.turns().edit(sessionId, nodeId, text,
                        nullable(body, "requestId"), agentLimit);
                Json.write(response, outcomeJson(outcome));
                return;
            }
            if ("/export".equals(tail) && "GET".equals(method)) {
                JsonObject envelope = chat.exportSession(sessionId);
                if (envelope == null) {
                    Json.error(response, HttpServletResponse.SC_NOT_FOUND, "session not found");
                    return;
                }
                response.setHeader("Content-Disposition",
                        "attachment; filename=\"hdsl-session-" + sessionId + ".json\"");
                Json.write(response, HttpServletResponse.SC_OK, envelope);
                return;
            }
            if ("/fork".equals(tail) && "POST".equals(method)) {
                JsonObject body = readBody(request, response);
                if (body != null) {
                    Json.write(response, HttpServletResponse.SC_CREATED,
                            chat.forkSession(sessionId, body));
                }
                return;
            }
            if (tail.startsWith("/nodes/")) {
                String nodeRest = tail.substring("/nodes/".length());
                int nodeSlash = nodeRest.indexOf('/');
                String nodeId = nodeSlash < 0 ? nodeRest : nodeRest.substring(0, nodeSlash);
                String action = nodeSlash < 0 ? "" : nodeRest.substring(nodeSlash);
                if (action.isEmpty() && "DELETE".equals(method)) {
                    Json.write(response, chat.deleteNode(sessionId, nodeId));
                    return;
                }
                if ("/variant".equals(action) && "POST".equals(method)) {
                    JsonObject body = readBody(request, response);
                    if (body != null) {
                        Json.write(response, chat.switchVariant(sessionId, nodeId,
                                required(body, "messageId")));
                    }
                    return;
                }
            }
            Json.error(response, HttpServletResponse.SC_NOT_FOUND, "not found");
            return;
        }

        if ("/import".equals(route) && "POST".equals(method)) {
            importSessions(request, response);
            return;
        }

        if (route.startsWith("/turns/") && route.endsWith("/cancel") && "POST".equals(method)) {
            String rest = route.substring("/turns/".length(), route.length() - "/cancel".length());
            if (rest.isEmpty() || rest.contains("/")) {
                Json.error(response, HttpServletResponse.SC_NOT_FOUND, "not found");
                return;
            }
            Json.write(response, java.util.Map.of("ok", chat.turns().cancel(rest)));
            return;
        }

        if ("/attachments".equals(route)) {
            if (!"POST".equals(method)) {
                method(response, "POST");
                return;
            }
            String sessionId = request.getParameter("sessionId");
            if (sessionId == null || sessionId.isBlank()) {
                throw new ChatException(400, "session_required", "sessionId is required");
            }
            Part part;
            try {
                part = request.getPart("file");
            } catch (jakarta.servlet.ServletException e) {
                throw new ChatException(400, "multipart_error", e.getMessage());
            }
            if (part == null || part.getSize() == 0) {
                throw new ChatException(400, "file_required", "a file part is required");
            }
            Json.write(response, HttpServletResponse.SC_CREATED,
                    chat.uploadAttachment(sessionId, part));
            return;
        }

        if (route.startsWith("/attachments/") && "GET".equals(method)) {
            String tail = route.substring("/attachments/".length());
            if (tail.endsWith("/text")) {
                attachmentText(request, response, tail.substring(0, tail.length() - "/text".length()));
                return;
            }
            download(request, response, tail, "1".equals(request.getParameter("download")));
            return;
        }

        if ("/search/status".equals(route) && "GET".equals(method)) {
            Json.write(response, chat.searchStatus());
            return;
        }

        if ("/search/test".equals(route) && "POST".equals(method)) {
            // The agent scope `chat-search` is checked by AuthFilter; a human
            // still needs the developer password for this provider-backed call.
            if (!AuthFilter.isAgent(request) && !requireElevation(request, response)) {
                return;
            }
            JsonObject body = readBody(request, response);
            if (body != null) {
                Json.write(response, chat.searchTest(string(body, "query", "")));
            }
            return;
        }

        if ("/profile".equals(route)) {
            if ("GET".equals(method)) {
                Json.write(response, chat.profile(usernameOf(request)));
                return;
            }
            if ("PUT".equals(method) || "PATCH".equals(method)) {
                JsonObject body = readBody(request, response);
                if (body != null) {
                    Json.write(response, chat.updateProfile(body, usernameOf(request)));
                }
                return;
            }
            method(response, "GET, PUT");
            return;
        }

        if ("/avatars".equals(route)) {
            if (!"POST".equals(method)) {
                method(response, "POST");
                return;
            }
            Part part;
            try {
                part = request.getPart("file");
            } catch (jakarta.servlet.ServletException e) {
                throw new ChatException(400, "multipart_error", e.getMessage());
            }
            String id = chat.uploadAvatar(part);
            JsonObject body = new JsonObject();
            body.addProperty("id", id);
            body.addProperty("url", "/api/chat/avatars/" + id);
            Json.write(response, HttpServletResponse.SC_CREATED, body);
            return;
        }

        if (route.startsWith("/avatars/") && "GET".equals(method)) {
            serveAvatar(response, route.substring("/avatars/".length()));
            return;
        }

        if ("/settings".equals(route)) {
            if ("GET".equals(method)) {
                if (!requireElevation(request, response)) {
                    return;
                }
                Json.write(response, chat.settingsJson());
                return;
            }
            if ("PATCH".equals(method) || "PUT".equals(method)) {
                if (!requireElevation(request, response)) {
                    return;
                }
                JsonObject body = readBody(request, response);
                if (body != null) {
                    Json.write(response, chat.updateSettings(body));
                }
                return;
            }
            method(response, "GET, PATCH");
            return;
        }

        if ("/backups".equals(route) && "GET".equals(method)) {
            if (!requireElevation(request, response)) {
                return;
            }
            JsonObject body = new JsonObject();
            body.add("backups", chat.backups());
            Json.write(response, body);
            return;
        }
        if ("/backup".equals(route) && "POST".equals(method)) {
            if (!requireElevation(request, response)) {
                return;
            }
            Json.write(response, chat.backupNow());
            return;
        }

        if ("/models".equals(route) && "GET".equals(method)) {
            String account = request.getParameter("account");
            if (account == null || account.isBlank()) {
                throw new ChatException(400, "account_required", "account is required");
            }
            Json.write(response, chat.models(account));
            return;
        }

        if ("/accounts".equals(route) && "GET".equals(method)) {
            JsonObject body = new JsonObject();
            body.add("accounts", chat.accounts());
            Json.write(response, body);
            return;
        }

        if (route.startsWith("/accounts/")) {
            String rest = route.substring("/accounts/".length());
            int slash = rest.indexOf('/');
            String account = slash < 0 ? rest : rest.substring(0, slash);
            String action = slash < 0 ? "" : rest.substring(slash);
            if (action.isEmpty() && ("PATCH".equals(method) || "PUT".equals(method))) {
                if (!requireElevation(request, response)) {
                    return;
                }
                JsonObject body = readBody(request, response);
                if (body != null) {
                    Json.write(response, chat.saveAccountProfile(account, body));
                }
                return;
            }
            if ("/probe".equals(action) && "POST".equals(method)) {
                if (!requireElevation(request, response)) {
                    return;
                }
                Json.write(response, chat.probe(account));
                return;
            }
            if ("/chat-test".equals(action) && "POST".equals(method)) {
                if (!requireElevation(request, response)) {
                    return;
                }
                JsonObject body = readBody(request, response);
                if (body != null) {
                    Json.write(response, chat.chatTest(account, nullable(body, "model")));
                }
                return;
            }
            if ("/vision-test".equals(action) && "POST".equals(method)) {
                if (!requireElevation(request, response)) {
                    return;
                }
                JsonObject body = readBody(request, response);
                String model = body == null ? null : nullable(body, "model");
                if (model == null || model.isBlank()) {
                    Json.error(response, HttpServletResponse.SC_BAD_REQUEST, "model is required");
                    return;
                }
                Json.write(response, chat.visionTest(account, model));
                return;
            }
            if ("/models/refresh".equals(action) && "POST".equals(method)) {
                if (!requireElevation(request, response)) {
                    return;
                }
                JsonObject body = new JsonObject();
                JsonArray models = new JsonArray();
                chat.refreshModels(account).forEach(models::add);
                body.add("models", models);
                Json.write(response, body);
                return;
            }
            if ("/capabilities".equals(action) && "POST".equals(method)) {
                if (!requireElevation(request, response)) {
                    return;
                }
                JsonObject body = readBody(request, response);
                if (body != null) {
                    Json.write(response, chat.saveCapability(account, body));
                }
                return;
            }
            Json.error(response, HttpServletResponse.SC_NOT_FOUND, "not found");
            return;
        }

        if ("/assistants".equals(route)) {
            if ("GET".equals(method)) {
                JsonObject body = new JsonObject();
                body.add("assistants", chat.assistants());
                Json.write(response, body);
                return;
            }
            if ("POST".equals(method)) {
                JsonObject body = readBody(request, response);
                if (body != null) {
                    Json.write(response, HttpServletResponse.SC_CREATED,
                            chat.assistantJson(chat.createAssistant(body)));
                }
                return;
            }
            method(response, "GET, POST");
            return;
        }

        if (route.startsWith("/assistants/")) {
            String rest = route.substring("/assistants/".length());
            int slash = rest.indexOf('/');
            String id = slash < 0 ? rest : rest.substring(0, slash);
            String action = slash < 0 ? "" : rest.substring(slash);
            if (action.isEmpty()) {
                if ("PATCH".equals(method) || "PUT".equals(method)) {
                    JsonObject body = readBody(request, response);
                    if (body != null) {
                        Json.write(response, chat.assistantJson(chat.updateAssistant(id, body)));
                    }
                    return;
                }
                if ("DELETE".equals(method)) {
                    chat.deleteAssistant(id);
                    Json.write(response, java.util.Map.of("ok", true));
                    return;
                }
                method(response, "PATCH, DELETE");
                return;
            }
            if ("/duplicate".equals(action) && "POST".equals(method)) {
                Json.write(response, HttpServletResponse.SC_CREATED,
                        chat.assistantJson(chat.duplicateAssistant(id)));
                return;
            }
            Json.error(response, HttpServletResponse.SC_NOT_FOUND, "not found");
            return;
        }

        Json.error(response, HttpServletResponse.SC_NOT_FOUND, "not found");
    }

    /// 头像文件：服务端重编码过的 PNG，文件名是内容哈希，可以长缓存。
    private void serveAvatar(HttpServletResponse response, String id) throws IOException {
        Path path = chat.avatarFile(id);
        if (path == null) {
            Json.error(response, HttpServletResponse.SC_NOT_FOUND, "avatar not found");
            return;
        }
        response.setStatus(200);
        response.setContentType("image/png");
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("Cache-Control", "private, max-age=86400, immutable");
        response.setHeader("Content-Security-Policy", "default-src 'none'");
        try (InputStream input = Files.newInputStream(path)) {
            input.transferTo(response.getOutputStream());
        }
    }

    /// 当前登录用户；auth 关闭时面板只有一个 admin，和 `/api/auth/whoami` 保持一致。
    private static String usernameOf(HttpServletRequest request) {
        Object session = request.getAttribute(AuthFilter.SESSION_ATTRIBUTE);
        return session instanceof AuthService.Session value ? value.username() : "admin";
    }

    /// 文本类附件要显式声明 UTF-8：中文 Windows 的浏览器在 `text/plain` 没有 charset 时
    /// 会按 GBK 猜，UTF-8 的粘贴文本因此显示成乱码。
    private static boolean textual(String mime) {
        String lower = mime.toLowerCase(java.util.Locale.ROOT);
        return lower.startsWith("text/") || "application/json".equals(lower)
                || "application/xml".equals(lower) || lower.endsWith("+json");
    }

    /// 附件预览/下载：`?download=1` 强制下载，其余保持 inline。
    ///
    /// 支持单区间 `Range`：浏览器内置 PDF 查看器和 `<video>` 拖动进度都依赖 206。
    /// 多区间只处理第一段，不可满足的区间回 416。
    private void download(HttpServletRequest request, HttpServletResponse response,
                          String id, boolean asDownload) throws IOException, SQLException {
        ChatStore.Attachment attachment = chat.attachment(id);
        if (attachment == null) {
            Json.error(response, HttpServletResponse.SC_NOT_FOUND, "attachment not found");
            return;
        }
        Path path = Path.of(attachment.path());
        if (!Files.isRegularFile(path)) {
            Json.error(response, HttpServletResponse.SC_NOT_FOUND, "attachment file is missing");
            return;
        }
        long size = Files.size(path);
        ByteRange range;
        try {
            range = parseRange(request.getHeader("Range"), size);
        } catch (RangeNotSatisfiable e) {
            response.setStatus(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
            response.setHeader("Content-Range", "bytes */" + size);
            return;
        }
        long start = range == null ? 0 : range.start();
        long length = range == null ? size : range.end() - range.start() + 1;

        response.setStatus(range == null ? 200 : 206);
        if (textual(attachment.mime())) {
            response.setContentType(attachment.mime() + "; charset="
                    + ChatAttachments.sniffTextCharset(path).name());
        } else {
            response.setContentType(attachment.mime());
        }
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("Accept-Ranges", "bytes");
        response.setHeader("Cache-Control", "private, max-age=3600");
        response.setContentLengthLong(length);
        if (range != null) {
            response.setHeader("Content-Range", "bytes " + start + "-" + range.end() + "/" + size);
        }
        String encoded = URLEncoder.encode(attachment.name(), StandardCharsets.UTF_8)
                .replace("+", "%20");
        response.setHeader("Content-Disposition",
                (asDownload ? "attachment" : "inline") + "; filename*=UTF-8''" + encoded);
        try (InputStream input = Files.newInputStream(path)) {
            input.skipNBytes(start);
            byte[] buffer = new byte[64 * 1024];
            long remaining = length;
            while (remaining > 0) {
                int read = input.read(buffer, 0, (int) Math.min(buffer.length, remaining));
                if (read < 0) {
                    break;
                }
                response.getOutputStream().write(buffer, 0, read);
                remaining -= read;
            }
        }
    }

    /// 文本预览：只服务 text 与 document（读抽取文本），按字符分页，
    /// 避免 50 MB 日志每预览一次就整个读进内存。
    private void attachmentText(HttpServletRequest request, HttpServletResponse response, String id)
            throws IOException, SQLException {
        ChatStore.Attachment attachment = chat.attachment(id);
        if (attachment == null) {
            Json.error(response, HttpServletResponse.SC_NOT_FOUND, "attachment not found");
            return;
        }
        Path source;
        java.nio.charset.Charset charset;
        boolean extracted;
        if ("document".equals(attachment.kind())) {
            if (attachment.extractedTextPath() == null) {
                Json.error(response, HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE,
                        "attachment_text_unavailable: this document has no extracted text");
                return;
            }
            source = Path.of(attachment.extractedTextPath());
            charset = StandardCharsets.UTF_8;
            extracted = true;
        } else if ("text".equals(attachment.kind())) {
            source = Path.of(attachment.path());
            charset = ChatAttachments.sniffTextCharset(source);
            extracted = false;
        } else {
            Json.error(response, HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE,
                    "attachment_text_unavailable: only text and document attachments have a text view");
            return;
        }
        if (!Files.isRegularFile(source)) {
            Json.error(response, HttpServletResponse.SC_NOT_FOUND, "attachment file is missing");
            return;
        }
        int offset = intParameter(request, "offset", 0, 0, 10_000_000);
        int limit = intParameter(request, "limit", 65_536, 1, 200_000);
        StringBuilder text = new StringBuilder();
        boolean hasMore;
        try (Reader reader = Files.newBufferedReader(source, charset)) {
            long skipped = 0;
            while (skipped < offset) {
                long step = reader.skip(offset - skipped);
                if (step <= 0) {
                    break;
                }
                skipped += step;
            }
            char[] buffer = new char[8192];
            int want = limit;
            while (want > 0) {
                int read = reader.read(buffer, 0, Math.min(buffer.length, want));
                if (read < 0) {
                    break;
                }
                text.append(buffer, 0, read);
                want -= read;
            }
            hasMore = want == 0 && reader.read() >= 0;
        }
        JsonObject body = new JsonObject();
        body.addProperty("text", text.toString());
        body.addProperty("offset", offset);
        body.addProperty("nextOffset", offset + text.length());
        body.addProperty("hasMore", hasMore);
        body.addProperty("charset", charset.name());
        body.addProperty("extracted", extracted);
        Json.write(response, body);
    }

    private static int intParameter(HttpServletRequest request, String name, int fallback, int min, int max) {
        String raw = request.getParameter(name);
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            return Math.max(min, Math.min(max, Integer.parseInt(raw.trim())));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    record ByteRange(long start, long end) {
    }

    static final class RangeNotSatisfiable extends Exception {
    }

    static @Nullable ByteRange parseRange(@Nullable String header, long size)
            throws RangeNotSatisfiable {
        if (header == null || !header.startsWith("bytes=") || size <= 0) {
            return null;
        }
        String spec = header.substring("bytes=".length()).trim();
        int comma = spec.indexOf(',');
        if (comma >= 0) {
            spec = spec.substring(0, comma).trim();
        }
        int dash = spec.indexOf('-');
        if (dash < 0) {
            throw new RangeNotSatisfiable();
        }
        String from = spec.substring(0, dash).trim();
        String to = spec.substring(dash + 1).trim();
        long start;
        long end;
        try {
            if (from.isEmpty()) {
                long suffix = Long.parseLong(to);
                if (suffix <= 0) {
                    throw new RangeNotSatisfiable();
                }
                start = Math.max(0, size - suffix);
                end = size - 1;
            } else {
                start = Long.parseLong(from);
                end = to.isEmpty() ? size - 1 : Long.parseLong(to);
            }
        } catch (NumberFormatException e) {
            throw new RangeNotSatisfiable();
        }
        if (start < 0 || start >= size || start > end) {
            throw new RangeNotSatisfiable();
        }
        return new ByteRange(start, Math.min(end, size - 1));
    }

    /// 导入入口：multipart 的 `file`，或直接的请求体（自动化脚本更方便）。
    /// 解析失败回 400 并带上原因，成功回 201 + 每个会话的新 id。
    private void importSessions(HttpServletRequest request, HttpServletResponse response)
            throws IOException, SQLException, ChatException {
        String assistantId = request.getParameter("assistantId");
        if (assistantId == null || assistantId.isBlank()) {
            assistantId = "default";
        }
        String title = request.getParameter("title");
        byte[] file;
        String contentType = request.getContentType() == null ? "" : request.getContentType();
        if (contentType.startsWith("multipart/")) {
            Part part;
            try {
                part = request.getPart("file");
            } catch (jakarta.servlet.ServletException e) {
                Json.error(response, HttpServletResponse.SC_BAD_REQUEST, "multipart 解析失败");
                return;
            }
            if (part == null) {
                Json.error(response, HttpServletResponse.SC_BAD_REQUEST, "缺少 file 字段");
                return;
            }
            try (InputStream in = part.getInputStream()) {
                file = in.readNBytes(ChatArchive.MAX_FILE_BYTES + 1);
            }
        } else {
            try (InputStream in = request.getInputStream()) {
                file = in.readNBytes(ChatArchive.MAX_FILE_BYTES + 1);
            }
        }
        if (file.length > ChatArchive.MAX_FILE_BYTES) {
            Json.error(response, HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE,
                    "文件超过 " + (ChatArchive.MAX_FILE_BYTES / (1024 * 1024)) + " MB");
            return;
        }
        try {
            Json.write(response, HttpServletResponse.SC_CREATED,
                    chat.importSessions(file, assistantId, title));
        } catch (IOException e) {
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        }
    }

    private static JsonObject outcomeJson(ChatTurnManager.Outcome outcome) {
        JsonObject json = new JsonObject();
        json.addProperty("turnId", outcome.turnId());
        if (outcome.userMessage() != null) {
            json.add("userMessage", messageJson(outcome.userMessage()));
        }
        if (outcome.assistantMessage() != null) {
            json.add("assistantMessage", messageJson(outcome.assistantMessage()));
        }
        if (outcome.assistantNode() != null) {
            json.addProperty("assistantNodeId", outcome.assistantNode().id());
        }
        return json;
    }

    private static JsonObject messageJson(ChatStore.Message message) {
        JsonObject json = new JsonObject();
        json.addProperty("id", message.id());
        json.addProperty("nodeId", message.nodeId());
        json.addProperty("variantIndex", message.variantIndex());
        json.addProperty("content", message.content());
        json.addProperty("status", message.status());
        json.addProperty("createdAt", message.createdAt());
        return json;
    }

    /// Search is a separate scope because it spends a provider quota; a
    /// `chat-write` ticket alone may send plain messages but not turn search on.
    private static boolean agentCanSearch(HttpServletRequest request) {
        if (!AuthFilter.isAgent(request)) {
            return true;
        }
        Object grant = request.getAttribute(AuthFilter.AGENT_GRANT_ATTRIBUTE);
        return grant instanceof DeveloperElevation.AgentGrant value
                && value.scopes().contains(
                        org.jackhuang.hmcl.web.developer.AgentScopes.CHAT_SEARCH);
    }

    /// 设置、备份、端点探测与能力覆盖要求开发者提权。
    private boolean requireElevation(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        if (!RequestSecurity.isSecure(request, tlsActive.getAsBoolean(), config)) {
            Json.error(response, HttpServletResponse.SC_CONFLICT,
                    "tls_required: the developer password is only accepted over HTTPS");
            return false;
        }
        if (!AuthFilter.elevationSatisfied(request, elevation, keyOf(request), sessionAlive(request))) {
            Json.error(response, HttpServletResponse.SC_FORBIDDEN,
                    "developer_elevation_required: elevate with the developer password first");
            return false;
        }
        return true;
    }

    private static String keyOf(HttpServletRequest request) {
        Object token = request.getAttribute(AuthFilter.SESSION_TOKEN_ATTRIBUTE);
        return DeveloperElevation.key(token instanceof String value ? value : null,
                request.getRemoteAddr());
    }

    private boolean sessionAlive(HttpServletRequest request) {
        Object token = request.getAttribute(AuthFilter.SESSION_TOKEN_ATTRIBUTE);
        return !(token instanceof String value) || value.isBlank() || auth.isAlive(value);
    }

    private static void method(HttpServletResponse response, String allowed) throws IOException {
        response.setHeader("Allow", allowed);
        Json.error(response, HttpServletResponse.SC_METHOD_NOT_ALLOWED, "method not allowed");
    }

    private static @Nullable JsonObject readBody(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        byte[] bytes = request.getInputStream().readNBytes(MAX_JSON_BYTES + 1);
        if (bytes.length > MAX_JSON_BYTES) {
            Json.error(response, 413, "body too large");
            return null;
        }
        if (bytes.length == 0) {
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST, "a JSON body is required");
            return null;
        }
        try {
            JsonElement parsed = JsonParser.parseString(new String(bytes, StandardCharsets.UTF_8));
            if (!parsed.isJsonObject()) {
                Json.error(response, HttpServletResponse.SC_BAD_REQUEST, "a JSON object is required");
                return null;
            }
            return parsed.getAsJsonObject();
        } catch (RuntimeException e) {
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST, "malformed JSON");
            return null;
        }
    }

    private static String required(JsonObject body, String name) throws ChatException {
        String value = nullable(body, name);
        if (value == null || value.isBlank()) {
            throw new ChatException(400, "invalid_request", name + " is required");
        }
        return value;
    }

    private static String string(JsonObject body, String name, String fallback) {
        String value = nullable(body, name);
        return value == null ? fallback : value;
    }

    private static @Nullable String nullable(JsonObject body, String name) {
        JsonElement element = body.get(name);
        if (element == null || element.isJsonNull() || !element.isJsonPrimitive()) {
            return null;
        }
        return element.getAsString();
    }

    private static List<String> strings(JsonObject body, String name) {
        List<String> result = new ArrayList<>();
        JsonElement element = body.get(name);
        if (element != null && element.isJsonArray()) {
            for (JsonElement item : element.getAsJsonArray()) {
                if (item.isJsonPrimitive()) {
                    result.add(item.getAsString());
                }
            }
        }
        return result;
    }
}