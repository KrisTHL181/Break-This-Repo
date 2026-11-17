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
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The password change endpoint over real HTTP: the old password must match
/// (403 otherwise), the new one takes effect for logins, the users file keeps
/// its 0600 permissions, and the sessions other than the one that made the
/// change are revoked while it survives.
class PasswordChangeTest {

    private static final String OLD_PASSWORD = "original-pass-1";
    private static final String NEW_PASSWORD = "replacement-2";

    @TempDir
    Path dataDir;

    @Test
    void passwordChangeUpdatesLoginAndRevokesOtherSessions() throws Exception {
        try (TestSupport.RunningServer running = TestSupport.start(dataDir,
                Map.of(UserStore.ENV_ADMIN_PASSWORD, OLD_PASSWORD),
                config -> config.bindHost = "127.0.0.1")) {
            HttpClient client = TestSupport.client();
            String base = running.baseUrl();

            // Two sessions of the same user: the browser that will make the
            // change, and another "device" that must be kicked out.
            String current = loginCookie(client, base, "admin", OLD_PASSWORD);
            String other = loginCookie(client, base, "admin", OLD_PASSWORD);
            assertEquals(200, get(client, base + "/api/auth/whoami", other).statusCode());

            // Wrong current password: 403, nothing changes.
            HttpResponse<String> wrong = post(client, base + "/api/auth/password",
                    "{\"currentPassword\":\"nope-nope\",\"newPassword\":\"" + NEW_PASSWORD + "\"}", current);
            assertEquals(403, wrong.statusCode());
            assertEquals("current password is incorrect",
                    JsonParser.parseString(wrong.body()).getAsJsonObject().get("error").getAsString());
            assertEquals(200, loginCookieStatus(client, base, "admin", OLD_PASSWORD));

            // Too-short new password: 400.
            assertEquals(400, post(client, base + "/api/auth/password",
                    "{\"currentPassword\":\"" + OLD_PASSWORD + "\",\"newPassword\":\"12345\"}", current).statusCode());

            // The real change: 200.
            HttpResponse<String> changed = post(client, base + "/api/auth/password",
                    "{\"currentPassword\":\"" + OLD_PASSWORD + "\",\"newPassword\":\"" + NEW_PASSWORD + "\"}", current);
            assertEquals(200, changed.statusCode());
            assertTrue(JsonParser.parseString(changed.body()).getAsJsonObject().get("ok").getAsBoolean());

            // The other session is revoked; the current one still works.
            assertEquals(401, get(client, base + "/api/auth/whoami", other).statusCode());
            HttpResponse<String> whoami = get(client, base + "/api/auth/whoami", current);
            assertEquals(200, whoami.statusCode());
            assertEquals("admin", JsonParser.parseString(whoami.body()).getAsJsonObject().get("username").getAsString());

            // Logins follow the new password exclusively.
            assertEquals(401, loginCookieStatus(client, base, "admin", OLD_PASSWORD));
            assertEquals(200, loginCookieStatus(client, base, "admin", NEW_PASSWORD));

            // The rewrite kept the file owner-only.
            org.jackhuang.hmcl.util.PosixPermissions.assertOwnerOnly(dataDir.resolve("auth/users.json"),
                    "users.json must stay 0600 after a password change");
        }
    }

    @Test
    void passwordEndpointNeedsASession() throws Exception {
        try (TestSupport.RunningServer running = TestSupport.start(dataDir,
                Map.of(UserStore.ENV_ADMIN_PASSWORD, OLD_PASSWORD),
                config -> config.bindHost = "127.0.0.1")) {
            HttpResponse<String> anonymous = post(TestSupport.client(), running.baseUrl() + "/api/auth/password",
                    "{\"currentPassword\":\"" + OLD_PASSWORD + "\",\"newPassword\":\"" + NEW_PASSWORD + "\"}", null);
            assertEquals(401, anonymous.statusCode());
        }
    }

    private static String loginCookie(HttpClient client, String base, String username, String password) throws Exception {
        HttpResponse<String> response = post(client, base + "/api/auth/login",
                "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}", null);
        assertEquals(200, response.statusCode());
        return response.headers().firstValue("set-cookie").orElseThrow().split(";", 2)[0];
    }

    private static int loginCookieStatus(HttpClient client, String base, String username, String password)
            throws Exception {
        HttpResponse<String> response = post(client, base + "/api/auth/login",
                "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}", null);
        return response.statusCode();
    }

    private static HttpResponse<String> post(HttpClient client, String url, String json, String cookie)
            throws Exception {
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
}
