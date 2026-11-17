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
package org.jackhuang.hmcl.web.http;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jackhuang.hmcl.web.audit.AuditLog;
import org.jackhuang.hmcl.web.auth.AuthFilter;
import org.jackhuang.hmcl.web.auth.AuthService;
import org.jackhuang.hmcl.web.auth.LoginThrottle;
import org.jackhuang.hmcl.web.auth.UserStore;
import org.jackhuang.hmcl.web.terminal.TerminalWebSocket;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.function.LongSupplier;

/// The whole Phase 1 REST surface, mapped at `/api/*`:
///
/// - `GET  /api/health`        — liveness, public
/// - `POST /api/auth/login`    — credentials in, session cookie out, public
/// - `GET  /api/auth/status`   — first-run flag (`setupRequired`), public
/// - `POST /api/auth/setup`    — creates the first user, session cookie out,
///                               public but refuses to run twice (409)
/// - `POST /api/auth/password` — password change, revokes the user's other
///                               sessions, needs a session
/// - `POST /api/auth/username` — username change, the calling session keeps
///                               working under the new name, other sessions
///                               are revoked, needs a session
/// - `POST /api/auth/logout`   — clears the cookie and the session
/// - `GET  /api/auth/whoami`   — the session's username
///
/// Phase 2 grows this into a router with a handler per resource; for seven
/// endpoints a switch is the honest size.
@NotNullByDefault
public final class ApiServlet extends HttpServlet {

    private final AuthService authService;
    private final AuthFilter authFilter;

    /// 每个来源地址的登录预算（见 [LoginThrottle]）。挂在 servlet 上而不是静态上：一个进程一份，
    /// 测试各自起自己的服务，也就各自有一份记录，互不干扰。
    private final LoginThrottle loginThrottle = new LoginThrottle();
    private final boolean authDisabled;
    private final String version;
    private final LongSupplier uptimeStartMillis;
    /// 开发者提权：登出要一并结束它。提权本身绑在 session token 上，而
    /// `AuthService.logout` 只删 session——不清这里，登出后 30 分钟内
    /// `/api/developer/agent-auth` 仍能凭 agent 密码发票。
    private final org.jackhuang.hmcl.web.developer.DeveloperElevation elevation;

    public ApiServlet(AuthService authService, AuthFilter authFilter, boolean authDisabled,
                      String version, LongSupplier uptimeStartMillis,
                      org.jackhuang.hmcl.web.developer.DeveloperElevation elevation) {
        this.authService = authService;
        this.authFilter = authFilter;
        this.authDisabled = authDisabled;
        this.version = version;
        this.uptimeStartMillis = uptimeStartMillis;
        this.elevation = elevation;
    }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String path = request.getPathInfo() == null ? "/" : request.getPathInfo();
        String method = request.getMethod();

        if ("/health".equals(path) && "GET".equals(method)) {
            health(response);
        } else if ("/auth/login".equals(path) && "POST".equals(method)) {
            login(request, response);
        } else if ("/auth/status".equals(path) && "GET".equals(method)) {
            status(response);
        } else if ("/auth/setup".equals(path) && "POST".equals(method)) {
            setup(request, response);
        } else if ("/auth/password".equals(path) && "POST".equals(method)) {
            password(request, response);
        } else if ("/auth/username".equals(path) && "POST".equals(method)) {
            username(request, response);
        } else if ("/auth/logout".equals(path) && "POST".equals(method)) {
            logout(request, response);
        } else if ("/auth/whoami".equals(path) && "GET".equals(method)) {
            whoami(request, response);
        } else {
            Json.error(response, HttpServletResponse.SC_NOT_FOUND, "not found");
        }
    }

    private void health(HttpServletResponse response) throws IOException {
        long uptimeSec = Math.max(0, (System.currentTimeMillis() - uptimeStartMillis.getAsLong()) / 1000);
        // `java` 是「关于」页要显示的运行时版本：面板自己跑在哪个 JDK 上，
        // 和实例用的 JDK 是两回事，混在一起会让人以为实例也换了运行时。
        // 只给大版本：`/api/health` 是免认证探针，把发行版与补丁号（`21.0.12.1+1-deb13u1`）
        // 告诉任何路过的人，等于免费送一份指纹。
        Json.write(response, Map.of(
                "status", "ok",
                "version", version,
                "java", String.valueOf(Runtime.version().feature()),
                "uptimeSec", uptimeSec));
    }

    private void login(HttpServletRequest request, HttpServletResponse response) throws IOException {
        JsonObject body;
        try {
            body = JsonParser.parseString(new String(request.getInputStream().readAllBytes(), StandardCharsets.UTF_8))
                    .getAsJsonObject();
        } catch (JsonParseException | IllegalStateException e) {
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST, "request body must be a JSON object");
            return;
        }
        String username = stringField(body, "username");
        String password = stringField(body, "password");
        if (username == null || password == null) {
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST, "username and password are required");
            return;
        }

        String address = request.getRemoteAddr() == null ? "unknown" : request.getRemoteAddr();
        // 拒绝发生在核对口令之前：核对口令本身就是攻击者想花掉的那笔开销（PBKDF2 二十一万次），
        // 而且被拒绝的地址不该还能让服务端为它算一次。
        if (loginThrottle.isBlocked(address, Instant.now())) {
            AuditLog.log(AuditLog.Channel.WEB, "login", null, "blocked", username, address);
            refuseTooManyAttempts(response, LoginThrottle.BLOCK);
            return;
        }

        Optional<String> token = authService.login(username, password);
        if (token.isEmpty()) {
            Duration refusedFor = loginThrottle.recordFailure(address, Instant.now());
            if (!refusedFor.isZero()) {
                // 就是这一次把预算花完的，所以这一次就告诉对方；此前十五次什么都不说。
                AuditLog.log(AuditLog.Channel.WEB, "login", null, "blocked", username, address);
                refuseTooManyAttempts(response, refusedFor);
                return;
            }
            AuditLog.log(AuditLog.Channel.WEB, "login", null, "failure", username, address);
            Json.error(response, HttpServletResponse.SC_UNAUTHORIZED, "invalid username or password");
            return;
        }

        // 口令是对的，此前那些失败也就不再算数——这个记录问的正是这件事。
        loginThrottle.clear(address);
        AuditLog.log(AuditLog.Channel.WEB, "login", null, "success", username, address);
        org.jackhuang.hmcl.web.system.SystemActivity.touch();
        response.addCookie(authFilter.sessionCookie(request, token.get()));
        Json.write(response, new LoginBody(true, username));
    }

    /// 拒绝一次登录，并说明多久之后可以再来。
    ///
    /// @param response   the response
    /// @param refusedFor how long the refusal lasts
    /// @throws IOException when the response cannot be written
    private static void refuseTooManyAttempts(HttpServletResponse response, Duration refusedFor) throws IOException {
        response.setHeader("Retry-After", Long.toString(Math.max(1, refusedFor.toSeconds())));
        // 429 Too Many Requests：jakarta 的 HttpServletResponse 没有这个常量（只到 4xx 的一部分），
        // 所以这里直接写数字。
        Json.error(response, 429, "too many failed sign-in attempts; try again later");
    }

    private void status(HttpServletResponse response) throws IOException {
        Json.write(response, Map.of("setupRequired", !authService.userStore().initialized()));
    }

    private void setup(HttpServletRequest request, HttpServletResponse response) throws IOException {
        UserStore users = authService.userStore();
        if (users.initialized()) {
            Json.error(response, HttpServletResponse.SC_CONFLICT, "already initialized");
            return;
        }
        JsonObject body;
        try {
            body = JsonParser.parseString(new String(request.getInputStream().readAllBytes(), StandardCharsets.UTF_8))
                    .getAsJsonObject();
        } catch (JsonParseException | IllegalStateException e) {
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST, "request body must be a JSON object");
            return;
        }
        String username = stringField(body, "username");
        String password = stringField(body, "password");
        if (!UserStore.isValidUsername(username) || !UserStore.isValidPassword(password)) {
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST,
                    "username must be 1-32 characters without whitespace and password at least "
                            + UserStore.PASSWORD_MIN_LENGTH + " characters");
            return;
        }

        try {
            users.createUser(username, password);
        } catch (IllegalStateException race) {
            // A concurrent setup won the race after the check above.
            Json.error(response, HttpServletResponse.SC_CONFLICT, "already initialized");
            return;
        }
        Optional<String> token = authService.createSession(username);
        if (token.isEmpty()) {
            Json.error(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "failed to open a session");
            return;
        }
        response.addCookie(authFilter.sessionCookie(request, token.get()));
        org.jackhuang.hmcl.web.system.SystemActivity.touch();
        AuditLog.log(AuditLog.Channel.WEB, "auth.setup", null, "success", username, request.getRemoteAddr());
        Json.write(response, HttpServletResponse.SC_CREATED, new SetupBody(true, username));
    }

    private void password(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Object session = request.getAttribute(AuthFilter.SESSION_ATTRIBUTE);
        if (!(session instanceof AuthService.Session s)) {
            // The filter already 401s without a session when auth is on; a
            // reachable request with no attribute means auth is off, where a
            // password change has nothing to protect.
            Json.error(response, authDisabled
                    ? HttpServletResponse.SC_BAD_REQUEST
                    : HttpServletResponse.SC_UNAUTHORIZED, authDisabled ? "authentication is disabled" : "unauthorized");
            return;
        }

        JsonObject body;
        try {
            body = JsonParser.parseString(new String(request.getInputStream().readAllBytes(), StandardCharsets.UTF_8))
                    .getAsJsonObject();
        } catch (JsonParseException | IllegalStateException e) {
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST, "request body must be a JSON object");
            return;
        }
        String currentPassword = stringField(body, "currentPassword");
        String newPassword = stringField(body, "newPassword");
        if (currentPassword == null || newPassword == null) {
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST, "currentPassword and newPassword are required");
            return;
        }
        if (!UserStore.isValidPassword(newPassword)) {
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST,
                    "password must be at least " + UserStore.PASSWORD_MIN_LENGTH + " characters");
            return;
        }

        UserStore users = authService.userStore();
        if (!users.verify(s.username(), currentPassword)) {
            AuditLog.log(AuditLog.Channel.WEB, "auth.password", null, "failure",
                    s.username(), request.getRemoteAddr());
            Json.error(response, HttpServletResponse.SC_FORBIDDEN, "current password is incorrect");
            return;
        }
        if (!users.updatePassword(s.username(), newPassword)) {
            Json.error(response, HttpServletResponse.SC_CONFLICT, "user no longer exists");
            return;
        }
        AuditLog.log(AuditLog.Channel.WEB, "auth.password", null, "success",
                s.username(), request.getRemoteAddr());
        // The session that made the change survives; every other session of
        // this user (other browsers, other devices) is logged out.
        authService.revokeOtherSessions(s.username(), cookieValue(request, AuthFilter.SESSION_COOKIE));
        Json.write(response, Map.of("ok", true));
    }

    private void username(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Object session = request.getAttribute(AuthFilter.SESSION_ATTRIBUTE);
        if (!(session instanceof AuthService.Session s)) {
            // Same reasoning as the password endpoint: with auth off there is
            // nothing for a rename to protect.
            Json.error(response, authDisabled
                    ? HttpServletResponse.SC_BAD_REQUEST
                    : HttpServletResponse.SC_UNAUTHORIZED, authDisabled ? "authentication is disabled" : "unauthorized");
            return;
        }

        JsonObject body;
        try {
            body = JsonParser.parseString(new String(request.getInputStream().readAllBytes(), StandardCharsets.UTF_8))
                    .getAsJsonObject();
        } catch (JsonParseException | IllegalStateException e) {
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST, "request body must be a JSON object");
            return;
        }
        String newUsername = stringField(body, "newUsername");
        if (!UserStore.isValidUsername(newUsername)) {
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST,
                    "username must be 1-" + UserStore.USERNAME_MAX_LENGTH + " characters without whitespace");
            return;
        }

        UserStore users = authService.userStore();
        String oldUsername = s.username();
        boolean renamed;
        try {
            renamed = users.updateUsername(newUsername);
        } catch (IllegalArgumentException e) {
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
            return;
        }
        if (!renamed) {
            Json.error(response, HttpServletResponse.SC_CONFLICT, "user no longer exists");
            return;
        }
        // The session that made the change survives under the new name;
        // every other session of the old name is logged out.
        authService.renameUser(oldUsername, newUsername, cookieValue(request, AuthFilter.SESSION_COOKIE));
        Json.write(response, new UsernameBody(true, newUsername));
    }

    private void logout(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String token = cookieValue(request, AuthFilter.SESSION_COOKIE);
        authService.logout(token);
        // 已建立的终端 WS 不再复查会话，不掐的话登出后的浏览器还留着一条
        // 活动的终端通道。只断连接、不动 PTY：detach 语义，agent 继续跑
        // （docs/web-shell.md §3），下次提权后重连即可接回。
        if (token != null && !token.isBlank()) {
            TerminalWebSocket.closeForSession(token);
            // 提权是 session 的，session 没了就不该留着。不清的话，
            // `latestHumanExpiry` 仍然非空，agent 密码在登出后还能继续发票。
            // 票本身由 `DeveloperElevation#agentGrant` 的实时检查兜住：最后
            // 一个提权结束，手里的票立即失效。
            elevation.clear(org.jackhuang.hmcl.web.developer.DeveloperElevation.key(
                    token, request.getRemoteAddr()));
        }
        response.addCookie(authFilter.clearedSessionCookie(request));
        Json.write(response, Map.of("ok", true));
    }

    private void whoami(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Object session = request.getAttribute(AuthFilter.SESSION_ATTRIBUTE);
        if (session instanceof AuthService.Session s) {
            Json.write(response, new WhoamiBody(s.username()));
        } else if (authDisabled) {
            // Without a session there is no username to report; with auth
            // switched off the single admin is the answer.
            Json.write(response, new WhoamiBody("admin"));
        } else {
            Json.error(response, HttpServletResponse.SC_UNAUTHORIZED, "unauthorized");
        }
    }

    private static @Nullable String stringField(JsonObject object, String name) {
        return object.has(name) && object.get(name).isJsonPrimitive() ? object.get(name).getAsString() : null;
    }

    private static @Nullable String cookieValue(HttpServletRequest request, String name) {
        var cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (var cookie : cookies) {
            if (name.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private record LoginBody(boolean ok, String username) {
    }

    private record SetupBody(boolean ok, String username) {
    }

    private record WhoamiBody(String username) {
    }

    private record UsernameBody(boolean ok, String username) {
    }
}
