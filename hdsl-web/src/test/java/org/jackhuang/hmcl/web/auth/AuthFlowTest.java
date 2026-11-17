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
package org.jackhuang.hmcl.web.auth;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jackhuang.hmcl.web.TestSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The whole login dance over real HTTP: first boot from HDSL_ADMIN_PASSWORD,
/// wrong password rejected, session cookie issued, whoami with and without
/// the cookie, logout kills the session.
class AuthFlowTest {

    private static final String PASSWORD = "correct horse battery staple";

    @TempDir
    Path dataDir;

    /// 子路径挂载时 Cookie 的 Path 跟着挂载点走：同一个域名下挂两个面板
    /// （`/a/`、`/b/`）时，Path=/ 的同名 Cookie 会让「登录 A 之后 B 掉线」。
    @Test
    void theSessionCookieIsScopedToTheMountPoint() throws Exception {
        try (TestSupport.RunningServer running = TestSupport.start(dataDir,
                Map.of(UserStore.ENV_ADMIN_PASSWORD, PASSWORD),
                config -> {
                    config.bindHost = "127.0.0.1";
                    config.basePath = "/hdsl";
                })) {
            HttpResponse<String> login = post(TestSupport.client(), running.baseUrl() + "/api/auth/login",
                    "{\"username\":\"admin\",\"password\":\"" + PASSWORD + "\"}");
            assertEquals(200, login.statusCode(), login.body());
            String setCookie = login.headers().firstValue("set-cookie").orElseThrow();
            assertTrue(setCookie.contains("Path=/hdsl"), setCookie);
        }
    }

    /// 票没有会话，拿到就能用；所以它只被接受来自本机的请求。地址判断要和
    /// `InetAddress` 同一口径，IPv4-mapped 的 `::ffff:127.0.0.1` 也算回环。
    @Test
    void agentTokensAreOnlyHonouredFromLoopback() {
        assertTrue(AuthFilter.isLoopback("127.0.0.1"));
        assertTrue(AuthFilter.isLoopback("127.0.0.8"));
        assertTrue(AuthFilter.isLoopback("::1"));
        assertTrue(AuthFilter.isLoopback("::ffff:127.0.0.1"));
        assertFalse(AuthFilter.isLoopback("192.168.1.10"));
        assertFalse(AuthFilter.isLoopback("172.17.0.1"));
        assertFalse(AuthFilter.isLoopback("10.0.0.5"));
        assertFalse(AuthFilter.isLoopback("8.8.8.8"));
        assertFalse(AuthFilter.isLoopback(null));
        assertFalse(AuthFilter.isLoopback(""));
        assertFalse(AuthFilter.isLoopback("not-an-address"));
    }

    @Test
    void loginLogoutAndWhoamiFlow() throws Exception {
        try (TestSupport.RunningServer running = TestSupport.start(dataDir,
                Map.of(UserStore.ENV_ADMIN_PASSWORD, PASSWORD),
                config -> config.bindHost = "127.0.0.1")) {
            HttpClient client = TestSupport.client();
            String base = running.baseUrl();

            // The users file exists with the admin account, owner-only.
            Path usersFile = dataDir.resolve("auth/users.json");
            assertTrue(Files.exists(usersFile), "first boot must create auth/users.json");
            org.jackhuang.hmcl.util.PosixPermissions.assertOwnerOnly(usersFile, "users.json must be 0600");

            // Wrong password: 401, uniform error shape.
            HttpResponse<String> bad = post(client, base + "/api/auth/login",
                    "{\"username\":\"admin\",\"password\":\"wrong\"}");
            assertEquals(401, bad.statusCode());
            assertEquals("invalid username or password",
                    JsonParser.parseString(bad.body()).getAsJsonObject().get("error").getAsString());

            // Right password: 200 + session cookie with the right attributes.
            HttpResponse<String> good = post(client, base + "/api/auth/login",
                    "{\"username\":\"admin\",\"password\":\"" + PASSWORD + "\"}");
            assertEquals(200, good.statusCode());
            JsonObject body = JsonParser.parseString(good.body()).getAsJsonObject();
            assertTrue(body.get("ok").getAsBoolean());
            assertEquals("admin", body.get("username").getAsString());
            String setCookie = good.headers().firstValue("set-cookie").orElseThrow();
            assertTrue(setCookie.startsWith(AuthFilter.SESSION_COOKIE + "="), setCookie);
            assertTrue(setCookie.contains("HttpOnly"), setCookie);
            assertTrue(setCookie.contains("SameSite=Strict"), setCookie);
            assertTrue(setCookie.contains("Path=/"), setCookie);
            String cookie = setCookie.split(";", 2)[0];

            // whoami with the cookie: the username comes back.
            HttpResponse<String> whoami = get(client, base + "/api/auth/whoami", cookie);
            assertEquals(200, whoami.statusCode());
            assertEquals("admin", JsonParser.parseString(whoami.body()).getAsJsonObject().get("username").getAsString());

            // whoami without a cookie: the session gate answers 401.
            HttpResponse<String> anonymous = get(client, base + "/api/auth/whoami", null);
            assertEquals(401, anonymous.statusCode());
            assertEquals("unauthorized", JsonParser.parseString(anonymous.body()).getAsJsonObject().get("error").getAsString());

            // Logout clears the cookie and the session.
            HttpResponse<String> logout = post(client, base + "/api/auth/logout", "", cookie);
            assertEquals(200, logout.statusCode());
            String clear = logout.headers().firstValue("set-cookie").orElseThrow();
            assertTrue(clear.contains(AuthFilter.SESSION_COOKIE) && clear.contains("Max-Age=0"), clear);

            // The old token no longer verifies.
            HttpResponse<String> after = get(client, base + "/api/auth/whoami", cookie);
            assertEquals(401, after.statusCode());
        }
    }

    @Test
    void usernameChangeRenamesLoginAndRevokesOtherSessions() throws Exception {
        try (TestSupport.RunningServer running = TestSupport.start(dataDir,
                Map.of(UserStore.ENV_ADMIN_PASSWORD, PASSWORD),
                config -> config.bindHost = "127.0.0.1")) {
            HttpClient client = TestSupport.client();
            String base = running.baseUrl();

            // Two sessions of the same user: the one that renames survives,
            // the other one is revoked (same semantics as a password change).
            HttpResponse<String> login1 = post(client, base + "/api/auth/login",
                    "{\"username\":\"admin\",\"password\":\"" + PASSWORD + "\"}");
            String cookie1 = login1.headers().firstValue("set-cookie").orElseThrow().split(";", 2)[0];
            HttpResponse<String> login2 = post(client, base + "/api/auth/login",
                    "{\"username\":\"admin\",\"password\":\"" + PASSWORD + "\"}");
            String cookie2 = login2.headers().firstValue("set-cookie").orElseThrow().split(";", 2)[0];

            // Without a session the endpoint is unreachable.
            HttpResponse<String> anonymous = post(client, base + "/api/auth/username",
                    "{\"newUsername\":\"alice\"}");
            assertEquals(401, anonymous.statusCode());

            // An invalid name is rejected before anything is written.
            HttpResponse<String> invalid = post(client, base + "/api/auth/username",
                    "{\"newUsername\":\"has space\"}", cookie1);
            assertEquals(400, invalid.statusCode());

            // Rename: 200 and the new username comes back.
            HttpResponse<String> renamed = post(client, base + "/api/auth/username",
                    "{\"newUsername\":\"alice\"}", cookie1);
            assertEquals(200, renamed.statusCode());
            JsonObject body = JsonParser.parseString(renamed.body()).getAsJsonObject();
            assertTrue(body.get("ok").getAsBoolean());
            assertEquals("alice", body.get("username").getAsString());

            // The renaming session now answers whoami with the new name…
            HttpResponse<String> whoami = get(client, base + "/api/auth/whoami", cookie1);
            assertEquals(200, whoami.statusCode());
            assertEquals("alice", JsonParser.parseString(whoami.body()).getAsJsonObject().get("username").getAsString());

            // …the other session is gone…
            HttpResponse<String> revoked = get(client, base + "/api/auth/whoami", cookie2);
            assertEquals(401, revoked.statusCode());

            // …the old name no longer logs in, the new one does with the same password.
            HttpResponse<String> oldLogin = post(client, base + "/api/auth/login",
                    "{\"username\":\"admin\",\"password\":\"" + PASSWORD + "\"}");
            assertEquals(401, oldLogin.statusCode());
            HttpResponse<String> newLogin = post(client, base + "/api/auth/login",
                    "{\"username\":\"alice\",\"password\":\"" + PASSWORD + "\"}");
            assertEquals(200, newLogin.statusCode());

            // The rename reached disk: users.json now records "alice".
            String stored = Files.readString(dataDir.resolve("auth/users.json"));
            assertTrue(stored.contains("\"username\": \"alice\""), stored);
        }
    }

    @Test
    void healthIsPublicAndReportsVersionAndUptime() throws Exception {
        try (TestSupport.RunningServer running = TestSupport.start(dataDir, Map.of(),
                config -> config.bindHost = "127.0.0.1")) {
            HttpResponse<String> response = get(TestSupport.client(), running.baseUrl() + "/api/health", null);
            assertEquals(200, response.statusCode());
            JsonObject body = JsonParser.parseString(response.body()).getAsJsonObject();
            assertEquals("ok", body.get("status").getAsString());
            assertNotNull(body.get("version"));
            assertTrue(body.get("uptimeSec").getAsLong() >= 0);
        }
    }

    @Test
    void unknownApiPathsAreUniformJson404() throws Exception {
        // Auth is off on a loopback bind so the request reaches the router;
        // with the gate on, an unauthenticated /api/nope is a 401, which the
        // main flow test already pins.
        try (TestSupport.RunningServer running = TestSupport.start(dataDir, Map.of(),
                config -> {
                    config.bindHost = "127.0.0.1";
                    config.auth.disabled = true;
                })) {
            HttpResponse<String> response = get(TestSupport.client(), running.baseUrl() + "/api/nope", null);
            assertEquals(404, response.statusCode());
            assertEquals("not found", JsonParser.parseString(response.body()).getAsJsonObject().get("error").getAsString());
        }
    }

    private static HttpResponse<String> post(HttpClient client, String url, String json) throws Exception {
        return post(client, url, json, null);
    }

    private static HttpResponse<String> post(HttpClient client, String url, String json, String cookie) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json));
        if (cookie != null) {
            builder.header("Cookie", cookie);
        }
        return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private static HttpResponse<String> get(HttpClient client, String url, String cookie) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url)).GET();
        if (cookie != null) {
            builder.header("Cookie", cookie);
        }
        return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    /// 票的来源规则：本机不需要人；远程要么票是浏览器签的，要么带着有效会话。
    @Test
    void anAgentGrantOffTheLoopbackNeedsAPersonBehindIt() {
        // 开关关着：维持现状，哪儿都能用。
        assertTrue(AuthFilter.remoteAllowed(false, false, false, false));
        // 本机：不需要人。
        assertTrue(AuthFilter.remoteAllowed(true, true, false, false));
        // 浏览器签发的票：操作员已经打开过面板并复制过 token。
        assertTrue(AuthFilter.remoteAllowed(true, false, true, false));
        // 密码签发的票：非本机 + 无会话 = 拒。
        assertFalse(AuthFilter.remoteAllowed(true, false, false, false));
        // 密码签发的票 + 有效会话 = 放。
        assertTrue(AuthFilter.remoteAllowed(true, false, false, true));
    }

    /// 默认姿态：票只认本机。关掉回环限制之后才轮到「远程要有人」。
    @Test
    void ticketsAreLoopbackOnlyUntilTheOperatorSaysOtherwise() {
        // 本机永远放行，两条规则都不看来源证明。
        assertTrue(AuthFilter.grantUsable(true, true, true, false, false));
        // 回环限制开着：远程无论是浏览器签的票还是带会话，都拒。
        assertFalse(AuthFilter.grantUsable(true, false, false, true, true));
        assertFalse(AuthFilter.grantUsable(true, false, false, false, false));
        // 关掉之后：remote_needs_session 关 = 远程放行。
        assertTrue(AuthFilter.grantUsable(false, false, false, false, false));
        // 打开 remote_needs_session：远程要有浏览器票或会话。
        assertFalse(AuthFilter.grantUsable(false, true, false, false, false));
        assertTrue(AuthFilter.grantUsable(false, true, false, true, false));
        assertTrue(AuthFilter.grantUsable(false, true, false, false, true));
    }

    @Test
    void loopbackParsingCoversTheFormsTheKernelReports() {
        assertTrue(AuthFilter.isLoopback("127.0.0.1"));
        assertTrue(AuthFilter.isLoopback("127.0.0.53"));
        assertTrue(AuthFilter.isLoopback("::1"));
        assertTrue(AuthFilter.isLoopback("::ffff:127.0.0.1"));
        assertFalse(AuthFilter.isLoopback("172.17.0.1"));
        assertFalse(AuthFilter.isLoopback("10.0.0.5"));
        assertFalse(AuthFilter.isLoopback(null));
        assertFalse(AuthFilter.isLoopback(""));
    }
}
