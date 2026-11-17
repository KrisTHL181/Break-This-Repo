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
package org.jackhuang.hmcl.web.plain;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jackhuang.hmcl.web.auth.AuthFilter;
import org.jackhuang.hmcl.web.auth.AuthService;
import org.jackhuang.hmcl.web.auth.LoginThrottle;
import org.jackhuang.hmcl.web.audit.AuditLog;
import org.jackhuang.hmcl.web.chat.ChatException;
import org.jackhuang.hmcl.web.chat.ChatService;
import org.jackhuang.hmcl.web.config.ServerConfig;
import org.jackhuang.hmcl.web.developer.DeveloperAuth;
import org.jackhuang.hmcl.web.developer.DeveloperElevation;
import org.jackhuang.hmcl.web.http.RequestSecurity;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.jackhuang.hmcl.util.logging.Logger.LOG;

/// The no-JavaScript chat surface, for text browsers and old machines.
///
/// One HTML page per action, forms and links only: log in, list sessions, read a
/// transcript, send a message, download an attachment. A reply arrives through
/// the same [ChatService] turn the SPA uses; the page offers a manual refresh and,
/// while a turn runs, an optional `<meta http-equiv=refresh>` so "submit, then
/// look at the result" works without a script.
///
/// Browsers that can run eighty lines of ES5 get more: the transcript and the
/// session list are served as fragments (`/plain/f/…`) that `poll.js` long-polls
/// and swaps in place, so content updates by itself without a full page reload
/// and without losing what is typed into the send form. Markdown arrives
/// pre-rendered by the panel's own frontend renderer ([MarkdownSsr]), so the
/// legacy surface shows the same markup the SPA does.
///
/// Sending is gated on developer elevation: this surface is reachable from
/// browsers that ignore `SameSite`, so the login cookie alone is not enough to
/// spend the operator's tokens here. Reading stays login-only.
///
/// Deliberately not behind [AuthFilter]: the filter answers an unauthenticated
/// HTML request with a JSON 401, and a text browser would show that body. Every
/// route here checks the session itself and renders the login form instead.
@NotNullByDefault
public final class PlainChatServlet extends HttpServlet {

    /// The friendly page a browser that cannot run the SPA is sent to.
    static final String OLD_BROWSER_PATH = "/plain/old";

    /// Message text accepted from one form post.
    private static final int MAX_TEXT_CHARS = 64 * 1024;

    /// How often the session page reloads itself while a reply is being written.
    private static final int AUTO_REFRESH_SECONDS = 5;

    /// How long a fragment long-poll holds a request before answering 304.
    private static final long POLL_WAIT_MILLIS = 20_000;

    /// How often a held poll re-checks whether its revision moved.
    private static final long POLL_TICK_MILLIS = 500;

    /// No script at all: the pure no-JS pages. Images are allowed because the
    /// HMCL-style shell wears the panel's default wallpaper.
    private static final String CSP_STRICT =
            "default-src 'none'; style-src 'self' 'unsafe-inline'; img-src 'self';"
                    + " form-action 'self'; base-uri 'none'";

    /// The poller's pages: one first-party script, one first-party stylesheet,
    /// XHR to this origin, forms to this origin, and nothing else.
    private static final String CSP_ENHANCED =
            "default-src 'none'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self';"
                    + " font-src 'self'; connect-src 'self'; form-action 'self'; base-uri 'none'";

    /// The wallpaper the SPA wears by default (`DEFAULT_WALLPAPER` in
    /// `web/src/wallpaper.ts`), served statically from the jar. The legacy
    /// surface has no localStorage to remember a per-device choice, so it wears
    /// the panel's default; a missing file degrades to the flat background color.
    private static final String WALLPAPER_PATH = "/assets-img/wallpapers/2021-08-26.jpg";

    private final @Nullable ChatService chat;
    private final AuthService auth;
    private final AuthFilter authFilter;
    private final boolean authDisabled;
    private final LoginThrottle throttle = new LoginThrottle();
    /// `""` at a root mount, otherwise `/panel` and the like.
    private final String basePath;
    private final DeveloperAuth devAuth;
    private final DeveloperElevation elevation;
    private final java.util.function.BooleanSupplier tlsActive;
    private final ServerConfig config;
    private final MarkdownSsr ssr;

    public PlainChatServlet(@Nullable ChatService chat, AuthService auth, AuthFilter authFilter,
                            boolean authDisabled, @Nullable String basePath,
                            DeveloperAuth devAuth, DeveloperElevation elevation,
                            java.util.function.BooleanSupplier tlsActive, ServerConfig config,
                            MarkdownSsr ssr) {
        this.chat = chat;
        this.auth = auth;
        this.authFilter = authFilter;
        this.authDisabled = authDisabled;
        String normalized = basePath == null ? "" : basePath.trim();
        if ("/".equals(normalized)) {
            normalized = "";
        }
        if (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        this.basePath = normalized;
        this.devAuth = devAuth;
        this.elevation = elevation;
        this.tlsActive = tlsActive;
        this.config = config;
        this.ssr = ssr;
    }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws IOException {
        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.setContentType("text/html; charset=UTF-8");
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-Frame-Options", "DENY");

        String path = request.getPathInfo() == null || request.getPathInfo().isEmpty()
                ? "/" : request.getPathInfo();
        if (path.length() > 1 && path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }

        try {
            route(request, response, path);
        } catch (ChatException e) {
            error(response, e.status(), e.getMessage());
        } catch (SQLException e) {
            LOG.warning("Plain chat store failed", e);
            error(response, 500, "聊天数据库暂时不可用");
        }
    }

    private void route(HttpServletRequest request, HttpServletResponse response, String path)
            throws IOException, SQLException, ChatException {
        boolean post = "POST".equals(request.getMethod());

        if ("/old".equals(path)) {
            oldBrowser(response);
            return;
        }
        if ("/poll.js".equals(path) && !post) {
            staticResource(response, "/web-legacy/poll.js", "application/javascript; charset=UTF-8");
            return;
        }
        if ("/legacy.css".equals(path) && !post) {
            staticResource(response, "/web-legacy/legacy.css", "text/css; charset=UTF-8");
            return;
        }
        if (path.startsWith("/katex/") && !post) {
            String tail = path.substring("/katex/".length());
            if (!tail.matches("[A-Za-z0-9._-]+(?:/[A-Za-z0-9._-]+)*")) {
                error(response, 404, "这个地址不存在");
                return;
            }
            staticResource(response, "/web-legacy/katex/" + tail, katexContentType(tail));
            return;
        }
        if ("/login".equals(path)) {
            if (post) {
                login(request, response);
            } else {
                loginPage(response, null);
            }
            return;
        }
        if ("/logout".equals(path)) {
            if (!post) {
                methodNotAllowed(response, "POST");
                return;
            }
            response.addCookie(authFilter.clearedSessionCookie(request));
            redirect(response, url("/plain"));
            return;
        }

        AuthService.Session user = user(request);
        if (user == null) {
            if (post) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            }
            loginPage(response, post ? "请先登录" : null);
            return;
        }

        if ("/".equals(path)) {
            home(request, response, user);
            return;
        }
        if ("/new".equals(path)) {
            if (!post) {
                methodNotAllowed(response, "POST");
                return;
            }
            if (!sameOrigin(request)) {
                error(response, 403, "跨站表单被拒绝");
                return;
            }
            if (!chatGate(request, response, url("/plain"))) {
                return;
            }
            newSession(request, response);
            return;
        }
        if ("/elevate".equals(path)) {
            if (post) {
                handleElevate(request, response);
            } else {
                elevatePage(request, response, null, null);
            }
            return;
        }
        if ("/elevate/extend".equals(path)) {
            if (!post) {
                methodNotAllowed(response, "POST");
                return;
            }
            handleExtend(request, response);
            return;
        }
        if ("/elevate/stop".equals(path)) {
            if (!post) {
                methodNotAllowed(response, "POST");
                return;
            }
            if (!sameOrigin(request)) {
                error(response, 403, "跨站表单被拒绝");
                return;
            }
            elevation.clear(elevKey(request));
            AuditLog.log(AuditLog.Channel.WEB, "developer.elevation", "stop", "success",
                    user.username(), request.getRemoteAddr());
            redirect(response, url("/plain"));
            return;
        }
        if ("/f/sessions".equals(path)) {
            if (!enhancedLegacy()) {
                error(response, 404, "纯文本版增强已关闭：到「设置 → 通用」打开后再用自动更新");
                return;
            }
            if (post) {
                methodNotAllowed(response, "GET");
                return;
            }
            fragmentSessions(request, response);
            return;
        }
        if (path.startsWith("/f/s/")) {
            if (!enhancedLegacy()) {
                error(response, 404, "纯文本版增强已关闭：到「设置 → 通用」打开后再用自动更新");
                return;
            }
            if (post) {
                methodNotAllowed(response, "GET");
                return;
            }
            fragmentSession(request, response, path.substring("/f/s/".length()));
            return;
        }
        if (path.startsWith("/s/")) {
            String rest = path.substring(3);
            int slash = rest.indexOf('/');
            String sessionId = slash < 0 ? rest : rest.substring(0, slash);
            String action = slash < 0 ? "" : rest.substring(slash);
            if (sessionId.isEmpty()) {
                error(response, 404, "没有这个会话");
                return;
            }
            if (action.isEmpty()) {
                if (post) {
                    methodNotAllowed(response, "GET");
                    return;
                }
                sessionPage(request, response, sessionId);
                return;
            }
            if ("/model".equals(action)) {
                if (!post) {
                    methodNotAllowed(response, "POST");
                    return;
                }
                if (!sameOrigin(request)) {
                    error(response, 403, "跨站表单被拒绝");
                    return;
                }
                if (!chatGate(request, response, url("/plain/s/" + sessionId))) {
                    return;
                }
                String model = trimToNull(request.getParameter("model"));
                if (model != null) {
                    JsonObject patch = new JsonObject();
                    patch.addProperty("model", model);
                    requireChat().updateSession(sessionId, patch);
                }
                redirect(response, url("/plain/s/" + sessionId));
                return;
            }
            if ("/fork".equals(action)) {
                if (!post) {
                    methodNotAllowed(response, "POST");
                    return;
                }
                if (!sameOrigin(request)) {
                    error(response, 403, "跨站表单被拒绝");
                    return;
                }
                if (!chatGate(request, response, url("/plain/s/" + sessionId))) {
                    return;
                }
                JsonObject forked = requireChat().forkSession(sessionId, new JsonObject());
                redirect(response, url("/plain/s/" + string(forked, "id") + "#bottom"));
                return;
            }
            if ("/send".equals(action)) {
                if (!post) {
                    methodNotAllowed(response, "POST");
                    return;
                }
                if (!sameOrigin(request)) {
                    error(response, 403, "跨站表单被拒绝");
                    return;
                }
                if (!chatGate(request, response, url("/plain/s/" + sessionId))) {
                    return;
                }
                send(request, response, sessionId);
                return;
            }
        }

        error(response, 404, "这个地址不存在");
    }

    // ------------------------------------------------------------------ pages --

    private void loginPage(HttpServletResponse response, @Nullable String message) throws IOException {
        StringBuilder body = new StringBuilder();
        body.append("<h1>HDSL 面板 · 纯文本版</h1>\n");
        body.append("<p class=\"muted\">不需要 JavaScript，w3m / lynx 或旧浏览器都能用。</p>\n");
        if (message != null) {
            body.append("<p class=\"error\">").append(PlainHtml.escape(message)).append("</p>\n");
        }
        body.append("<form method=\"post\" action=\"").append(url("/plain/login")).append("\">\n")
                .append("<div class=\"row\"><label>用户名<br>")
                .append("<input type=\"text\" name=\"username\" autocomplete=\"username\" size=\"32\"></label></div>\n")
                .append("<div class=\"row\"><label>密码<br>")
                .append("<input type=\"password\" name=\"password\" autocomplete=\"current-password\"></label></div>\n")
                .append("<div class=\"row\"><input type=\"submit\" value=\"登录\"></div>\n")
                .append("</form>\n")
                .append("<p class=\"muted\"><a href=\"").append(url("/")).append("\">用现代浏览器打开面板</a></p>\n");
        page(response, 200, "登录 · HDSL 纯文本版", null, shell(null, body.toString()), false);
    }

    private void home(HttpServletRequest request, HttpServletResponse response, AuthService.Session user)
            throws IOException, SQLException {
        StringBuilder content = new StringBuilder();
        content.append("<p class=\"muted\">登录为 ").append(PlainHtml.escape(user.username()))
                .append("；回答生成时可用手动刷新或开启自动刷新。</p>\n");

        if (chat == null) {
            content.append("<p class=\"error\">聊天功能在这个实例上不可用（数据库未能打开）。</p>\n");
            page(response, 200, "会话 · HDSL 纯文本版", null,
                    shell(nav(request), content.toString()), true);
            return;
        }

        String elev = elevState(request);
        String region = sessionsRegion(request);
        if (enhancedLegacy()) {
            String rev = revOf(chat.sessions(null).toString(), elev);
            content.append("<div id=\"frag-sessions\" data-rev=\"").append(rev)
                    .append("\" data-elev=\"").append(elev)
                    .append("\" data-poll-url=\"").append(url("/plain/f/sessions")).append("\">\n")
                    .append(region)
                    .append("</div>\n");
        } else {
            content.append(region);
        }
        page(response, 200, "会话 · HDSL 纯文本版", null,
                shell(nav(request), content.toString()), true);
    }

    /// The session list plus the new-session form: the region the poller swaps.
    private String sessionsRegion(HttpServletRequest request) throws SQLException {
        StringBuilder body = new StringBuilder();
        body.append("<h1>会话</h1>\n");
        JsonArray sessions = chat.sessions(null);
        if (sessions.isEmpty()) {
            body.append("<p class=\"muted\">还没有会话。先在下面新建一个。</p>\n");
        } else {
            body.append("<table width=\"100%\"><tr><th width=\"60%\">标题</th><th width=\"25%\">更新时间</th><th width=\"15%\">消息</th></tr>\n");
            for (JsonElement element : sessions) {
                JsonObject session = element.getAsJsonObject();
                String id = string(session, "id");
                String title = string(session, "title");
                body.append("<tr><td><a href=\"").append(url("/plain/s/" + id + "#bottom")).append("\">")
                        .append(PlainHtml.escape(title.isBlank() ? "新会话" : title)).append("</a></td>")
                        .append("<td>").append(PlainHtml.time(longOf(session, "updatedAt"))).append("</td>")
                        .append("<td>").append(longOf(session, "messageCount")).append("</td></tr>\n");
            }
            body.append("</table>\n");
        }

        body.append("<h2>新建会话</h2>\n");
        if (!chatAllowed(request)) {
            body.append("<p class=\"muted\">新建会话需要先<a href=\"").append(url("/plain/elevate"))
                    .append("\">开发者提权</a>。</p>\n");
            return body.toString();
        }
        JsonArray assistants = chat.assistants();
        if (assistants.isEmpty()) {
            body.append("<p class=\"muted\">没有可用的助手。</p>\n");
            return body.toString();
        }
        body.append("<form method=\"post\" action=\"").append(url("/plain/new")).append("\">\n")
                .append("<div class=\"row\"><label>助手 <select name=\"assistantId\">\n");
        for (JsonElement element : assistants) {
            JsonObject assistant = element.getAsJsonObject();
            body.append("<option value=\"").append(PlainHtml.escape(string(assistant, "id")))
                    .append("\">").append(PlainHtml.escape(string(assistant, "name")))
                    .append("</option>\n");
        }
        body.append("</select></label></div>\n")
                .append("<div class=\"row\"><input type=\"submit\" value=\"创建并打开\"></div>\n")
                .append("</form>\n");
        return body.toString();
    }

    private void sessionPage(HttpServletRequest request, HttpServletResponse response, String sessionId)
            throws IOException, SQLException, ChatException {
        JsonObject session = requireChat().sessionJson(sessionId);
        boolean streaming = session.has("streaming") && session.get("streaming").getAsBoolean();
        boolean auto = "1".equals(request.getParameter("auto"));
        String error = request.getParameter("error");
        String elev = elevState(request);
        String rev = revOf(session.toString(), elev);

        StringBuilder content = new StringBuilder();
        content.append("<div class=\"toolbar\"><a href=\"").append(url("/plain")).append("\">← 会话列表</a> · ")
                .append("<a href=\"").append(url("/plain/s/" + sessionId)).append("\">刷新</a> · ")
                .append(auto
                        ? "<a href=\"" + url("/plain/s/" + sessionId) + "\">关闭自动刷新</a>"
                        : "<a href=\"" + url("/plain/s/" + sessionId) + "?auto=1\">自动刷新（"
                        + AUTO_REFRESH_SECONDS + " 秒）</a>")
                .append(" · <a href=\"").append(url("/api/chat/sessions/" + sessionId + "/export"))
                .append("\">导出</a>")
                .append(" · <form method=\"post\" action=\"").append(url("/plain/s/" + sessionId + "/fork"))
                .append("\"><input type=\"submit\" value=\"分叉\"></form>")
                .append(" · <a href=\"#bottom\">跳到末尾 ↓</a>")
                .append("</div>\n");

        content.append("<h1>").append(PlainHtml.escape(string(session, "title"))).append("</h1>\n");
        String model = string(session, "model");
        content.append("<p class=\"muted\">模型 ")
                .append(PlainHtml.escape(model.isBlank() ? "默认" : model))
                .append(" · 助手 ").append(PlainHtml.escape(assistantName(string(session, "assistantId"))))
                .append(streaming ? " · <strong>正在生成回答…</strong>" : "")
                .append("</p>\n");
        content.append(modelForm(session));

        if (error != null && !error.isBlank()) {
            content.append("<p class=\"error\">").append(PlainHtml.escape(errorText(error))).append("</p>\n");
        }

        if (enhancedLegacy()) {
            content.append("<div id=\"frag-transcript\" data-rev=\"").append(rev)
                    .append("\" data-elev=\"").append(elev)
                    .append("\" data-poll-url=\"").append(url("/plain/f/s/" + sessionId)).append("\">\n")
                    .append(transcript(session))
                    .append("</div>\n");
            content.append("<div id=\"frag-sendform\">\n")
                    .append(sendForm(request, sessionId))
                    .append("</div>\n");
        } else {
            content.append(transcript(session));
            content.append(sendForm(request, sessionId));
        }

        content.append("<a id=\"bottom\"></a>\n");
        String head = streaming && auto
                ? "<noscript><meta http-equiv=\"refresh\" content=\"" + AUTO_REFRESH_SECONDS + "\"></noscript>"
                : null;
        page(response, 200, string(session, "title") + " · HDSL 纯文本版",
                head, shell(nav(request), content.toString()), true);
    }

    /// The transcript region: every node's active variant, markdown pre-rendered
    /// server-side for finalized assistant replies.
    private String transcript(JsonObject session) {
        StringBuilder body = new StringBuilder();
        JsonArray nodes = session.has("nodes") && session.getAsJsonArray("nodes") != null
                ? session.getAsJsonArray("nodes") : new JsonArray();
        for (JsonElement element : nodes) {
            JsonObject node = element.getAsJsonObject();
            JsonObject message = activeVariant(node);
            if (message == null) {
                continue;
            }
            boolean assistant = "assistant".equals(string(node, "role"));
            String status = string(message, "status");
            String content = string(message, "content");
            body.append("<div class=\"row\"><strong>").append(assistant ? "助手" : "你")
                    .append("</strong> <span class=\"muted\">")
                    .append(PlainHtml.time(longOf(message, "createdAt")))
                    .append(" · ").append(statusText(status, assistant)).append("</span></div>\n");
            if (assistant && "done".equals(status) && enhancedLegacy()) {
                // Sources make [citation:N] into links; without them the renderer
                // would leave the markers in the body.
                String sourcesJson = message.has("sources") && message.get("sources").isJsonArray()
                        ? message.getAsJsonArray("sources").toString() : null;
                String rendered = ssr.render(string(message, "id"), content, sourcesJson);
                if (rendered != null) {
                    body.append(rendered).append("\n");
                    continue;
                }
            }
            if (content.isBlank() && "streaming".equals(status)) {
                body.append("<pre class=\"msg\">（正在生成…）</pre>\n");
            } else {
                body.append("<pre class=\"msg\">").append(PlainHtml.escape(content)).append("</pre>\n");
            }
            JsonArray attachments = message.has("attachments")
                    && message.get("attachments").isJsonArray()
                    ? message.getAsJsonArray("attachments") : new JsonArray();
            if (!attachments.isEmpty()) {
                body.append("<div>附件：");
                for (int i = 0; i < attachments.size(); i++) {
                    JsonObject attachment = attachments.get(i).getAsJsonObject();
                    String id = string(attachment, "id");
                    String name = string(attachment, "name");
                    if (i > 0) {
                        body.append(" · ");
                    }
                    body.append("<a href=\"").append(url("/api/chat/attachments/" + id))
                            .append("\">").append(PlainHtml.escape(name)).append("</a>\n");
                }
                body.append("</div>\n");
            }
        }
        return body.toString();
    }

    /// The model picker: a plain form with a `<select>`, so IE11 and w3m both
    /// can switch the session's model without any script.
    private String modelForm(JsonObject session) throws SQLException {
        String sessionId = string(session, "id");
        String current = string(session, "model");
        String account = string(session, "account");
        JsonArray models = new JsonArray();
        if (!account.isBlank()) {
            try {
                JsonElement list = requireChat().models(account).get("models");
                if (list != null && list.isJsonArray()) {
                    models = list.getAsJsonArray();
                }
            } catch (ChatException e) {
                // The picker still shows the current model; a missing account
                // list must not keep the composer from rendering.
            }
        }
        StringBuilder out = new StringBuilder("<form method=\"post\" action=\"")
                .append(url("/plain/s/" + sessionId + "/model"))
                .append("\" class=\"modelform\"><label>模型 <select name=\"model\">");
        if (current.isBlank() || models.isEmpty()) {
            out.append("<option value=\"").append(PlainHtml.escape(current))
                    .append("\" selected>")
                    .append(PlainHtml.escape(current.isBlank() ? "默认" : current))
                    .append("</option>");
        }
        for (JsonElement element : models) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject model = element.getAsJsonObject();
            String id = string(model, "id");
            if (id.isBlank()) {
                continue;
            }
            out.append("<option value=\"").append(PlainHtml.escape(id)).append("\"")
                    .append(id.equals(current) ? " selected" : "").append(">")
                    .append(PlainHtml.escape(id)).append(capsLabel(model)).append("</option>");
        }
        out.append("</select></label> <input type=\"submit\" value=\"切换\"></form>\n");
        return out.toString();
    }

    private static String capsLabel(JsonObject model) {
        JsonElement caps = model.get("caps");
        if (caps == null || !caps.isJsonObject()) {
            return "";
        }
        JsonObject properties = caps.getAsJsonObject();
        StringBuilder notes = new StringBuilder();
        appendCap(notes, properties, "reasoning", "推理");
        appendCap(notes, properties, "vision", "视觉");
        appendCap(notes, properties, "files", "文件");
        return notes.length() == 0 ? "" : "（" + notes + "）";
    }

    private static void appendCap(StringBuilder notes, JsonObject caps, String name, String label) {
        JsonElement value = caps.get(name);
        if (value != null && value.isJsonPrimitive() && value.getAsBoolean()) {
            if (notes.length() > 0) {
                notes.append("·");
            }
            notes.append(label);
        }
    }

    /// The send form, or the elevation notice that replaces it. Kept in its own
    /// region so the poller can swap it only when its markup actually changes —
    /// replacing it on every new message would wipe what is being typed.
    private String sendForm(HttpServletRequest request, String sessionId) {
        if (!chatAllowed(request)) {
            return "<p class=\"muted\">发送消息需要先<a href=\"" + url("/plain/elevate")
                    + "\">开发者提权</a>。</p>\n";
        }
        return "<h2>发送消息</h2>\n"
                + "<form method=\"post\" action=\"" + url("/plain/s/" + sessionId + "/send") + "\">\n"
                + "<textarea name=\"text\" rows=\"6\" cols=\"80\"></textarea>\n"
                + "<input type=\"hidden\" name=\"auto\" value=\"1\">\n"
                + "<div class=\"row\"><input type=\"submit\" value=\"发送\"> "
                + "<span class=\"muted\">提交后会自动回到本页；回答生成中请刷新查看。</span></div>\n"
                + "</form>\n"
                + "<p><a id=\"bottom\" href=\"" + url("/plain/s/" + sessionId)
                + "?auto=1\">刷新看结果</a></p>\n";
    }

    private String nav(HttpServletRequest request) {
        Instant expiry = elevation.expiryOf(elevKey(request), true);
        // <br> and submit inputs, not CSS: w3m and lynx ignore the stylesheet and
        // would flow every link into one line. The expiry is left to the elevation
        // page; a nav label that grows with the time squeezes the text column.
        return "<div class=\"navlist\"><strong>HDSL 纯文本版</strong><br>\n"
                + "<a href=\"" + url("/plain") + "\">会话</a><br>\n"
                + "<a href=\"" + url("/") + "\">完整面板</a><br>\n"
                + "<a href=\"" + url("/plain/elevate") + "\">"
                + (expiry == null ? "提权" : "提权中")
                + "</a><br>\n"
                + "<form method=\"post\" action=\"" + url("/plain/logout")
                + "\"><input type=\"submit\" value=\"退出\"></form>"
                + "</div>\n";
    }

    private void oldBrowser(HttpServletResponse response) throws IOException {
        String body = "<h1>这个浏览器太旧了</h1>\n"
                + "<p>完整面板需要 JavaScript、fetch、WebSocket 和 CSS flex 布局。"
                + "当前浏览器缺少其中至少一项，所以没有加载那套界面——不是服务器挂了。</p>\n"
                + "<p>两个选择：</p>\n"
                + "<ul>"
                + "<li><a href=\"" + url("/plain") + "\"><strong>继续用纯文本版</strong></a>："
                + "登录、看会话、发消息、下载附件都能用，适合 w3m / lynx / 老机器。</li>"
                + "<li>换一个现代浏览器（Chrome / Edge / Firefox / Safari）打开 "
                + "<a href=\"" + url("/") + "\">完整面板</a>。</li>"
                + "</ul>"
                + "<p class=\"muted\">Windows 7 上如果还在用 IE，建议装 Firefox ESR 115 或 Chrome 109。</p>\n";
        page(response, 200, "浏览器太旧 · HDSL", null, shell(null, body), false);
    }

    // ----------------------------------------------------------------- actions --

    private void login(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!sameOrigin(request)) {
            error(response, 403, "跨站表单被拒绝");
            return;
        }
        if (authDisabled) {
            redirect(response, url("/plain"));
            return;
        }
        String username = trimToNull(request.getParameter("username"));
        String password = request.getParameter("password");
        if (username == null || password == null || password.isEmpty()) {
            loginPage(response, "用户名和密码都要填");
            return;
        }

        String address = request.getRemoteAddr() == null ? "unknown" : request.getRemoteAddr();
        if (throttle.isBlocked(address, Instant.now())) {
            AuditLog.log(AuditLog.Channel.WEB, "login", null, "blocked", username, address);
            loginPage(response, "尝试次数过多，请 " + minutes(throttle.block()) + " 分钟后再试");
            return;
        }
        Optional<String> token = auth.login(username, password);
        if (token.isEmpty()) {
            Duration refused = throttle.recordFailure(address, Instant.now());
            AuditLog.log(AuditLog.Channel.WEB, "login", null, "failure", username, address);
            loginPage(response, refused.isZero() ? "用户名或密码不对"
                    : "尝试次数过多，请 " + minutes(refused) + " 分钟后再试");
            return;
        }
        throttle.clear(address);
        AuditLog.log(AuditLog.Channel.WEB, "login", null, "success", username, address);
        org.jackhuang.hmcl.web.system.SystemActivity.touch();
        response.addCookie(authFilter.sessionCookie(request, token.get()));
        redirect(response, url("/plain"));
    }

    private void newSession(HttpServletRequest request, HttpServletResponse response)
            throws IOException, SQLException, ChatException {
        ChatService service = requireChat();
        JsonObject body = new JsonObject();
        String assistantId = trimToNull(request.getParameter("assistantId"));
        body.addProperty("assistantId", assistantId == null ? "default" : assistantId);
        JsonObject session = service.createSession(body);
        redirect(response, url("/plain/s/" + string(session, "id")));
    }

    private void send(HttpServletRequest request, HttpServletResponse response, String sessionId)
            throws IOException, SQLException, ChatException {
        String text = request.getParameter("text");
        if (text == null || text.isBlank()) {
            redirect(response, url("/plain/s/" + sessionId + "?error=empty#bottom"));
            return;
        }
        if (text.length() > MAX_TEXT_CHARS) {
            text = text.substring(0, MAX_TEXT_CHARS);
        }
        try {
            requireChat().turns().send(sessionId, text.strip(), List.of(), null, null, null, false);
        } catch (ChatException e) {
            if ("chat_busy".equals(e.code())) {
                redirect(response, url("/plain/s/" + sessionId + "?error=busy#bottom"));
                return;
            }
            throw e;
        }
        redirect(response, url("/plain/s/" + sessionId + "?auto=1#bottom"));
    }

    // -------------------------------------------------------------- elevation --

    /// The gate in front of everything that spends tokens: a live developer
    /// elevation. Reading stays open to any logged-in session. A panel with
    /// authentication disabled altogether is loopback-only by config validation
    /// and has no login to elevate from, so it is its own proof.
    private boolean chatGate(HttpServletRequest request, HttpServletResponse response, String next)
            throws IOException {
        if (authDisabled || elevated(request, true)) {
            return true;
        }
        elevatePage(request, response, "发送消息与新建会话需要先提权。", next);
        return false;
    }

    /// Whether the send form and the new-session form may show at all.
    private boolean chatAllowed(HttpServletRequest request) {
        return authDisabled || elevated(request, false);
    }

    private void handleElevate(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        // Only a path inside this panel's plain surface is a valid return trip;
        // a posted `next` must not turn the elevate form into an open redirect.
        String next = safeNext(request.getParameter("next"));
        if (!sameOrigin(request)) {
            error(response, 403, "跨站表单被拒绝");
            return;
        }
        if (!requireTls(request, response)) {
            return;
        }
        if (!devAuth.isSet()) {
            error(response, 409, "还没有设置开发者密码：请在完整面板的「设置 → 开发者模式」里设置后再提权");
            return;
        }
        String password = request.getParameter("password");
        if (password == null || password.isEmpty()) {
            elevatePage(request, response, "请输入开发者密码。", next);
            return;
        }
        String key = elevKey(request);
        Instant now = Instant.now();
        if (elevation.isLocked(key, now)) {
            elevatePage(request, response,
                    "尝试次数过多，请 " + DeveloperElevation.LOCK.toMinutes() + " 分钟后再试。", next);
            return;
        }
        if (!devAuth.verify(password)) {
            boolean locked = elevation.recordFailure(key, now);
            AuditLog.log(AuditLog.Channel.WEB, "developer.elevation", "elevate", "failure",
                    actor(request), request.getRemoteAddr());
            elevatePage(request, response, locked
                    ? "开发者密码不对；尝试次数过多，本会话已锁 "
                    + DeveloperElevation.LOCK.toMinutes() + " 分钟。"
                    : "开发者密码不对。", next);
            return;
        }
        elevation.clearFailures(key);
        elevation.elevate(key);
        AuditLog.log(AuditLog.Channel.WEB, "developer.elevation", "elevate", "success",
                actor(request), request.getRemoteAddr());
        redirect(response, next == null ? url("/plain") : next);
    }

    private void handleExtend(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        if (!sameOrigin(request)) {
            error(response, 403, "跨站表单被拒绝");
            return;
        }
        if (!requireTls(request, response)) {
            return;
        }
        String key = elevKey(request);
        if (!elevation.require(key, true)) {
            elevatePage(request, response, "先用开发者密码提权，才能延长。", null);
            return;
        }
        Instant expiry = elevation.extend(key, DeveloperElevation.EXTENDED_TTL);
        if (expiry == null) {
            elevatePage(request, response,
                    "这次提权已经延长过一次；重新输入开发者密码后可再延长。", null);
            return;
        }
        AuditLog.log(AuditLog.Channel.WEB, "developer.elevation", "extend", "success",
                actor(request), request.getRemoteAddr());
        redirect(response, url("/plain/elevate"));
    }

    /// A return path that stays on this surface, or `null`.
    private @Nullable String safeNext(@Nullable String raw) {
        if (raw == null) {
            return null;
        }
        String value = raw.trim();
        String prefix = basePath + "/plain";
        if (!value.startsWith(prefix) || value.contains("\\")) {
            return null;
        }
        return value;
    }

    /// The elevation page: status, the password form, the one-shot extension
    /// coupon and the stop button.
    private void elevatePage(HttpServletRequest request, HttpServletResponse response,
                             @Nullable String notice, @Nullable String next) throws IOException {
        String key = elevKey(request);
        Instant expiry = elevation.expiryOf(key, true);
        StringBuilder content = new StringBuilder();
        content.append("<h1>开发者提权 · 纯文本版</h1>\n");
        if (notice != null) {
            content.append("<p class=\"error\">").append(PlainHtml.escape(notice)).append("</p>\n");
        }
        if (expiry == null) {
            content.append("<p class=\"muted\">当前未提权。提权窗口 ")
                    .append(DeveloperElevation.TTL.toMinutes())
                    .append(" 分钟，可延长一次到 ")
                    .append(DeveloperElevation.EXTENDED_TTL.toHours())
                    .append(" 小时；过期后发送表单会自动收回。</p>\n");
            content.append("<form method=\"post\" action=\"").append(url("/plain/elevate")).append("\">\n")
                    .append("<div class=\"row\"><label>开发者密码<br>")
                    .append("<input type=\"password\" name=\"password\" autocomplete=\"current-password\">")
                    .append("</label></div>\n");
            if (next != null) {
                content.append("<input type=\"hidden\" name=\"next\" value=\"").append(PlainHtml.escape(next))
                        .append("\">\n");
            }
            content.append("<div class=\"row\"><input type=\"submit\" value=\"提权\"></div>\n")
                    .append("</form>\n");
        } else {
            content.append("<p>已提权，到 ").append(PlainHtml.time(expiry.toEpochMilli())).append("。</p>\n");
            content.append("<form method=\"post\" action=\"").append(url("/plain/elevate/extend"))
                    .append("\" style=\"display:inline\"><input type=\"submit\" value=\"延长到 3 小时（仅一次）\"></form>\n");
            content.append("<form method=\"post\" action=\"").append(url("/plain/elevate/stop"))
                    .append("\" style=\"display:inline\"><input type=\"submit\" value=\"结束提权\"></form>\n");
        }
        content.append("<p><a href=\"").append(next == null ? url("/plain") : next).append("\">返回</a></p>\n");
        page(response, 200, "开发者提权 · HDSL 纯文本版", null,
                shell(nav(request), content.toString()), false);
    }

    private boolean requireTls(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (RequestSecurity.isSecure(request, tlsActive.getAsBoolean(), config)) {
            return true;
        }
        error(response, 409, "面板不在 HTTPS 下：开发者密码只通过 HTTPS 接受");
        return false;
    }

    // -------------------------------------------------------------- fragments --

    private void fragmentSessions(HttpServletRequest request, HttpServletResponse response)
            throws IOException, SQLException {
        String clientRev = request.getParameter("rev");
        boolean wait = request.getParameter("wait") != null;
        long deadline = System.currentTimeMillis() + POLL_WAIT_MILLIS;
        String html;
        String rev;
        String elev;
        while (true) {
            elev = elevState(request);
            String payload = chat.sessions(null).toString();
            rev = revOf(payload, elev);
            html = "<div id=\"frag-sessions\" data-rev=\"" + rev + "\" data-elev=\"" + elev
                    + "\" data-poll-url=\"" + url("/plain/f/sessions") + "\">\n"
                    + sessionsRegion(request)
                    + "</div>\n";
            if (!wait || !rev.equals(clientRev)) {
                break;
            }
            if (System.currentTimeMillis() >= deadline) {
                response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
                response.setHeader("X-Rev", rev);
                return;
            }
            if (!sleep()) {
                break;
            }
        }
        response.setHeader("X-Rev", rev);
        write(response, 200, html);
    }

    private void fragmentSession(HttpServletRequest request, HttpServletResponse response, String sessionId)
            throws IOException, SQLException, ChatException {
        String clientRev = request.getParameter("rev");
        boolean wait = request.getParameter("wait") != null;
        long deadline = System.currentTimeMillis() + POLL_WAIT_MILLIS;
        String html;
        String rev;
        while (true) {
            String elev = elevState(request);
            JsonObject session = requireChat().sessionJson(sessionId);
            rev = revOf(session.toString(), elev);
            html = "<div id=\"frag-transcript\" data-rev=\"" + rev + "\" data-elev=\"" + elev
                    + "\" data-poll-url=\"" + url("/plain/f/s/" + sessionId) + "\">\n"
                    + transcript(session)
                    + "</div>\n"
                    + "<div id=\"frag-sendform\">\n"
                    + sendForm(request, sessionId)
                    + "</div>\n";
            if (!wait || !rev.equals(clientRev)) {
                break;
            }
            if (System.currentTimeMillis() >= deadline) {
                response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
                response.setHeader("X-Rev", rev);
                return;
            }
            if (!sleep()) {
                break;
            }
        }
        response.setHeader("X-Rev", rev);
        write(response, 200, html);
    }

    /// @return false when the wait was interrupted and the current rev should answer
    private static boolean sleep() {
        try {
            Thread.sleep(POLL_TICK_MILLIS);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /// A short stable revision of one rendered region: enough to tell "nothing
    /// changed" from "something did", which is all a poll needs.
    static String revOf(String payload, String elev) {
        return Integer.toHexString(payload.hashCode() * 31 + elev.hashCode());
    }

    // ---------------------------------------------------------------- helpers --

    private ChatService requireChat() throws ChatException {
        if (chat == null) {
            throw new ChatException(503, "chat_unavailable", "聊天功能在这个实例上不可用");
        }
        return chat;
    }

    /// The assistant's display name, or the id when it is unknown.
    private String assistantName(String assistantId) throws SQLException {
        if (chat == null) {
            return assistantId;
        }
        for (JsonElement element : chat.assistants()) {
            JsonObject assistant = element.getAsJsonObject();
            if (assistantId.equals(string(assistant, "id"))) {
                return string(assistant, "name");
            }
        }
        return assistantId;
    }

    private @Nullable AuthService.Session user(HttpServletRequest request) {
        if (authDisabled) {
            return new AuthService.Session("admin", Instant.MAX);
        }
        String token = cookie(request, AuthFilter.SESSION_COOKIE);
        return token == null ? null : auth.verify(token).orElse(null);
    }

    private String actor(HttpServletRequest request) {
        AuthService.Session session = user(request);
        return session == null ? "unknown" : session.username();
    }

    /// The elevation key of this request's session, exactly the key the JSON
    /// developer routes use, so one elevation serves both surfaces.
    private String elevKey(HttpServletRequest request) {
        return DeveloperElevation.key(cookie(request, AuthFilter.SESSION_COOKIE),
                request.getRemoteAddr());
    }

    private boolean elevated(HttpServletRequest request, boolean slide) {
        boolean alive = user(request) != null;
        String key = elevKey(request);
        return slide ? elevation.require(key, alive) : elevation.isElevated(key, alive);
    }

    /// `ok` while elevated, `no` otherwise. The poller reads this on every
    /// fragment: a page that started `ok` and receives `no` has just lost its
    /// elevation, which is the kick-out signal.
    private String elevState(HttpServletRequest request) {
        return elevated(request, false) ? "ok" : "no";
    }

    private static @Nullable String cookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie candidate : cookies) {
            if (name.equals(candidate.getName())) {
                return candidate.getValue();
            }
        }
        return null;
    }

    /// Whether a form post came from this origin.
    ///
    /// The session cookie is `SameSite=Strict`, so a cross-site form does not
    /// carry it; this is the second lock, and the one that also covers a browser
    /// that ignores SameSite. A request with neither header (w3m, curl) is let
    /// through: those clients only post what a person typed.
    private static boolean sameOrigin(HttpServletRequest request) {
        String host = request.getHeader("Host");
        if (host == null || host.isBlank()) {
            return false;
        }
        String origin = request.getHeader("Origin");
        if (origin != null && !origin.isBlank()) {
            if ("null".equals(origin)) {
                return false;
            }
            return matchesHost(origin, host);
        }
        String referer = request.getHeader("Referer");
        return referer == null || referer.isBlank() || matchesHost(referer, host);
    }

    private static boolean matchesHost(String url, String host) {
        try {
            String authority = URI.create(url).getRawAuthority();
            return authority != null && host.equalsIgnoreCase(authority);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private String url(String path) {
        return basePath + path;
    }

    private static void redirect(HttpServletResponse response, String location) {
        response.setStatus(HttpServletResponse.SC_SEE_OTHER);
        response.setHeader("Location", location);
    }

    private static void write(HttpServletResponse response, int status, String html) throws IOException {
        response.setStatus(status);
        response.getWriter().write(html);
    }

    /// A whole page, with the CSP profile of its class: the poller's pages allow
    /// their one script and stylesheet, everything else stays script-free. With
    /// the enhancement switched off (`plain.enhanced: false`) every page is the
    /// strict class, so the surface is the plain degraded fallback again.
    private void page(HttpServletResponse response, int status, String title,
                      @Nullable String headExtra, String body, boolean enhanced) throws IOException {
        boolean on = enhanced && enhancedLegacy();
        response.setHeader("Content-Security-Policy", on ? CSP_ENHANCED : CSP_STRICT);
        // The HMCL-style shell lives in legacy.css in both modes — the enhancement
        // switch decides whether the surface updates itself, not whether it looks
        // like the panel. The wallpaper rides along as an inline style because its
        // URL carries the mount point.
        String head = "<link rel=\"stylesheet\" href=\"" + url("/plain/legacy.css") + "\">\n"
                + (on ? "<link rel=\"stylesheet\" href=\"" + url("/plain/katex/katex.min.css")
                        + "\">\n" : "")
                + "<style>body{background-color:#202a36;background-image:url('"
                + url(WALLPAPER_PATH) + "');background-position:center center;"
                + "background-size:cover;background-attachment:fixed;"
                + "background-repeat:no-repeat}</style>\n"
                + (headExtra == null ? "" : headExtra + "\n");
        String withScript = on
                ? body + "<script src=\"" + url("/plain/poll.js") + "\"></script>\n"
                : body;
        write(response, status, PlainHtml.page(title, head, withScript));
    }

    /// The HMCL-style shell: a two-column table — navigation card on the left,
    /// content card on the right — over the wallpaper. Tables, not flex: IE11's
    /// flexbox is broken, and a table is what w3m renders as a readable grid.
    /// Pages with nothing to navigate (login, errors, the too-old notice) pass a
    /// `null` nav and get the single content card.
    private static String shell(@Nullable String nav, String content) {
        StringBuilder out = new StringBuilder();
        if (nav == null) {
            // Login, errors and the too-old notice: HDSL's login card is a compact
            // centred panel, not a full-width content card.
            out.append("<div class=\"shell-single\">");
            out.append("<table class=\"shell shell-card\" width=\"420\" cellspacing=\"0\" cellpadding=\"0\"><tr>");
            out.append("<td class=\"shell-main\">").append(content).append("</td>");
            out.append("</tr></table></div>\n");
            return out.toString();
        }
        out.append("<table class=\"shell\" width=\"100%\" cellspacing=\"0\" cellpadding=\"0\"><tr>");
        out.append("<td class=\"shell-nav\" width=\"14%\">").append(nav).append("</td>");
        out.append("<td class=\"shell-main\">").append(content).append("</td>");
        out.append("</tr></table>\n");
        return out.toString();
    }

    /// Whether the legacy surface is a full citizen or only the degraded
    /// fallback; the operator flips it in 设置 → 通用 (`plain.enhanced`).
    private boolean enhancedLegacy() {
        return config.plain.enhanced;
    }

    private static String katexContentType(String tail) {
        if (tail.endsWith(".css")) {
            return "text/css; charset=UTF-8";
        }
        if (tail.endsWith(".woff2")) {
            return "font/woff2";
        }
        if (tail.endsWith(".woff")) {
            return "font/woff";
        }
        if (tail.endsWith(".ttf")) {
            return "font/ttf";
        }
        return "application/octet-stream";
    }

    private void staticResource(HttpServletResponse response, String resource, String contentType)
            throws IOException {
        try (InputStream in = PlainChatServlet.class.getResourceAsStream(resource)) {
            if (in == null) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                response.getWriter().write("not found");
                return;
            }
            response.setContentType(contentType);
            try (OutputStream out = response.getOutputStream()) {
                in.transferTo(out);
            }
        }
    }

    private void error(HttpServletResponse response, int status, @Nullable String message) throws IOException {
        String body = "<h1>出错了</h1>\n<p class=\"error\">"
                + PlainHtml.escape(message == null ? "请求失败" : message) + "</p>\n"
                + "<p><a href=\"" + url("/plain") + "\">返回会话列表</a></p>\n";
        page(response, status, "出错了 · HDSL 纯文本版", null, shell(null, body), false);
    }

    private void methodNotAllowed(HttpServletResponse response, String allowed) throws IOException {
        response.setHeader("Allow", allowed);
        String body = "<h1>不支持这个请求方式</h1>\n"
                + "<p>这里只接受 " + PlainHtml.escape(allowed) + "。</p>\n";
        page(response, 405, "405 · HDSL 纯文本版", null, shell(null, body), false);
    }

    private static String errorText(String code) {
        return switch (code) {
            case "empty" -> "消息不能为空";
            case "busy" -> "上一条回答还在生成，等它结束再发（或者先刷新看看）";
            default -> code;
        };
    }

    private static String statusText(String status, boolean assistant) {
        if (!assistant) {
            return "已发送";
        }
        return switch (status) {
            case "streaming" -> "生成中";
            case "error" -> "生成失败";
            case "stopped" -> "已停止";
            default -> "完成";
        };
    }

    private static @Nullable JsonObject activeVariant(JsonObject node) {
        JsonArray variants = node.has("variants") && node.get("variants").isJsonArray()
                ? node.getAsJsonArray("variants") : new JsonArray();
        if (variants.isEmpty()) {
            return null;
        }
        String active = string(node, "activeVariantId");
        for (JsonElement element : variants) {
            JsonObject message = element.getAsJsonObject();
            if (string(message, "id").equals(active)) {
                return message;
            }
        }
        return variants.get(0).getAsJsonObject();
    }

    private static String string(JsonObject object, String name) {
        JsonElement element = object.get(name);
        return element != null && element.isJsonPrimitive() ? element.getAsString() : "";
    }

    private static long longOf(JsonObject object, String name) {
        JsonElement element = object.get(name);
        return element != null && element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()
                ? element.getAsLong() : 0L;
    }

    private static @Nullable String trimToNull(@Nullable String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static long minutes(Duration duration) {
        return Math.max(1, duration.toMinutes());
    }
}
