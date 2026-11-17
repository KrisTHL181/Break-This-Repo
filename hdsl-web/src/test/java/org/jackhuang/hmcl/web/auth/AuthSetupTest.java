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
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The first-run setup over real HTTP: with no users on disk and no
/// `HDSL_ADMIN_PASSWORD`, the server reports `setupRequired`, refuses to log
/// in, and `POST /api/auth/setup` creates the first user and hands out a
/// session immediately. A second setup is a 409, and a server booted with the
/// environment variable set never offers setup.
class AuthSetupTest {

    private static final String PASSWORD = "setup-secret-1";

    @TempDir
    Path dataDir;

    @Test
    void freshServerWaitsForSetupAndSetupCreatesTheFirstUser() throws Exception {
        try (TestSupport.RunningServer running = TestSupport.start(dataDir, Map.of(),
                config -> config.bindHost = "127.0.0.1")) {
            HttpClient client = TestSupport.client();
            String base = running.baseUrl();

            // No password generated, no users file written.
            assertFalse(Files.exists(dataDir.resolve("auth/users.json")),
                    "without HDSL_ADMIN_PASSWORD no users.json may appear");

            // The status endpoint is public and reports the pending setup.
            HttpResponse<String> status = get(client, base + "/api/auth/status", null);
            assertEquals(200, status.statusCode());
            assertTrue(JsonParser.parseString(status.body()).getAsJsonObject().get("setupRequired").getAsBoolean());

            // Login is impossible before any user exists.
            HttpResponse<String> login = post(client, base + "/api/auth/login",
                    "{\"username\":\"admin\",\"password\":\"" + PASSWORD + "\"}");
            assertEquals(401, login.statusCode());

            // Setup validates its input.
            assertEquals(400, post(client, base + "/api/auth/setup",
                    "{\"username\":\"\",\"password\":\"" + PASSWORD + "\"}").statusCode());
            assertEquals(400, post(client, base + "/api/auth/setup",
                    "{\"username\":\"has space\",\"password\":\"" + PASSWORD + "\"}").statusCode());
            String tooLong = "u".repeat(UserStore.USERNAME_MAX_LENGTH + 1);
            assertEquals(400, post(client, base + "/api/auth/setup",
                    "{\"username\":\"" + tooLong + "\",\"password\":\"" + PASSWORD + "\"}").statusCode());
            assertEquals(400, post(client, base + "/api/auth/setup",
                    "{\"username\":\"root\",\"password\":\"12345\"}").statusCode());

            // A valid setup: 201, cookie, body names the new user.
            HttpResponse<String> setup = post(client, base + "/api/auth/setup",
                    "{\"username\":\"alice\",\"password\":\"" + PASSWORD + "\"}");
            assertEquals(201, setup.statusCode());
            JsonObject body = JsonParser.parseString(setup.body()).getAsJsonObject();
            assertTrue(body.get("ok").getAsBoolean());
            assertEquals("alice", body.get("username").getAsString());
            String setCookie = setup.headers().firstValue("set-cookie").orElseThrow();
            assertTrue(setCookie.startsWith(AuthFilter.SESSION_COOKIE + "="), setCookie);
            String cookie = setCookie.split(";", 2)[0];

            // The setup session works right away — no second login needed.
            HttpResponse<String> whoami = get(client, base + "/api/auth/whoami", cookie);
            assertEquals(200, whoami.statusCode());
            assertEquals("alice", JsonParser.parseString(whoami.body()).getAsJsonObject().get("username").getAsString());

            // And the server is initialized now: status flips, setup 409s.
            HttpResponse<String> statusAfter = get(client, base + "/api/auth/status", null);
            assertFalse(JsonParser.parseString(statusAfter.body()).getAsJsonObject().get("setupRequired").getAsBoolean());
            HttpResponse<String> again = post(client, base + "/api/auth/setup",
                    "{\"username\":\"mallory\",\"password\":\"hunter2hunter\"}");
            assertEquals(409, again.statusCode());
            assertEquals("already initialized",
                    JsonParser.parseString(again.body()).getAsJsonObject().get("error").getAsString());
        }
    }

    @Test
    void envPasswordBootSkipsSetup() throws Exception {
        try (TestSupport.RunningServer running = TestSupport.start(dataDir,
                Map.of(UserStore.ENV_ADMIN_PASSWORD, PASSWORD),
                config -> config.bindHost = "127.0.0.1")) {
            HttpClient client = TestSupport.client();
            String base = running.baseUrl();

            HttpResponse<String> status = get(client, base + "/api/auth/status", null);
            assertEquals(200, status.statusCode());
            assertFalse(JsonParser.parseString(status.body()).getAsJsonObject().get("setupRequired").getAsBoolean());

            HttpResponse<String> setup = post(client, base + "/api/auth/setup",
                    "{\"username\":\"mallory\",\"password\":\"hunter2hunter\"}");
            assertEquals(409, setup.statusCode());

            // The env-created admin logs in as before.
            HttpResponse<String> login = post(client, base + "/api/auth/login",
                    "{\"username\":\"admin\",\"password\":\"" + PASSWORD + "\"}");
            assertEquals(200, login.statusCode());
        }
    }

    private static HttpResponse<String> post(HttpClient client, String url, String json) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private static HttpResponse<String> get(HttpClient client, String url, String cookie) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url)).GET();
        if (cookie != null) {
            builder.header("Cookie", cookie);
        }
        return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }
}
