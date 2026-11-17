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
package org.jackhuang.hmcl.web.developer;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jackhuang.hmcl.web.TestSupport;
import org.jackhuang.hmcl.web.auth.UserStore;
import org.jackhuang.hmcl.web.config.DeveloperGuardScope;
import org.jackhuang.hmcl.web.config.ServerConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.util.Map;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The **agent password**: the lesser secret an operator can hand over instead
/// of the developer one.
///
/// Three properties make it safe to hand over, and each has a test here:
///
/// 1. it cannot elevate a browser session — the two secrets live in different
///    files and are verified by different doors, so typing the agent password
///    into the panel's elevate form has to fail;
/// 2. it only grants what the operator stored with it, never what the caller
///    asks for;
/// 3. it only exists where the guard actually gates `/api/*` — at `off` and
///    `proxy` a session already reaches everything, so a token would add
///    nothing but another way to lose a credential.
class AgentPasswordTest {

    private static final String PANEL_PASSWORD = "panel-password-1";
    private static final String DEV_PASSWORD = "dev-password-1";
    private static final String AGENT_PASSWORD = "agent-password-1";

    @TempDir
    Path dataDir;

    @Test
    void theAgentPasswordCannotElevateABrowserSession() throws Exception {
        try (TestSupport.RunningServer running = start(DeveloperGuardScope.ALL)) {
            HttpClient client = TestSupport.trustAllClient();
            String base = base(running);
            String cookie = elevatedSession(client, base);

            assertEquals(200, post(client, base + "/api/developer/agent-password",
                    "{\"password\":\"" + AGENT_PASSWORD + "\",\"scopes\":[\"read\"]}", cookie).statusCode());

            // The browser's own door does not know this secret.
            HttpResponse<String> elevate = post(client, base + "/api/developer/elevate",
                    "{\"password\":\"" + AGENT_PASSWORD + "\"}", cookie);
            assertEquals(401, elevate.statusCode(), elevate.body());
            assertTrue(elevate.body().contains("invalid developer password"));

            // And the agent door does not know the developer's.
            HttpResponse<String> reverse = post(client, base + "/api/developer/agent-auth",
                    "{\"password\":\"" + DEV_PASSWORD + "\"}", null);
            assertEquals(401, reverse.statusCode(), reverse.body());
        }
    }

    @Test
    void theCapStoredWithThePasswordWins() throws Exception {
        try (TestSupport.RunningServer running = start(DeveloperGuardScope.ALL)) {
            HttpClient client = TestSupport.trustAllClient();
            String base = base(running);
            String cookie = elevatedSession(client, base);

            assertEquals(200, post(client, base + "/api/developer/agent-password",
                    "{\"password\":\"" + AGENT_PASSWORD + "\",\"scopes\":[\"read\"]}", cookie).statusCode());

            // The caller asks for more than the cap; it gets the cap.
            HttpResponse<String> minted = post(client, base + "/api/developer/agent-auth",
                    "{\"password\":\"" + AGENT_PASSWORD + "\",\"label\":\"probe\",\"scopes\":[\"read\",\"mc\"]}",
                    null);
            assertEquals(200, minted.statusCode(), minted.body());
            JsonObject grant = json(minted);
            assertEquals("[\"read\"]", grant.get("scopes").toString());
            String token = grant.get("token").getAsString();

            assertEquals(200, getWithBearer(client, base + "/api/instances", token).statusCode());
            assertEquals(403, post(client, base + "/api/mc/instances/x/start", "{}", null, token).statusCode());

            // Asking for a class the cap does not hold is refused, not silently
            // widened: the caller learns the secret's shape.
            HttpResponse<String> tooWide = post(client, base + "/api/developer/agent-auth",
                    "{\"password\":\"" + AGENT_PASSWORD + "\",\"scopes\":[\"plugins\"]}", null);
            assertEquals(403, tooWide.statusCode(), tooWide.body());
            assertTrue(tooWide.body().contains("agent_scope_denied"));
        }
    }

    @Test
    void theAgentPasswordOnlyAppliesWhereTheGuardGates() throws Exception {
        // Guard off: a session already reaches every /api route, so minting a
        // token would be authority out of nothing.
        try (TestSupport.RunningServer running = start(DeveloperGuardScope.OFF)) {
            HttpClient client = TestSupport.trustAllClient();
            String base = base(running);
            String cookie = elevatedSession(client, base);
            assertEquals(200, post(client, base + "/api/developer/agent-password",
                    "{\"password\":\"" + AGENT_PASSWORD + "\",\"scopes\":[\"read\"]}", cookie).statusCode());

            HttpResponse<String> refused = post(client, base + "/api/developer/agent-auth",
                    "{\"password\":\"" + AGENT_PASSWORD + "\"}", null);
            assertEquals(409, refused.statusCode(), refused.body());
            assertTrue(refused.body().contains("agent_password_not_applicable"));
            assertFalse(json(get(client, base + "/api/developer/agent-password", cookie))
                    .get("applicable").getAsBoolean());
        }
    }

    @Test
    void rotatingTheAgentPasswordKillsItsGrants() throws Exception {
        try (TestSupport.RunningServer running = start(DeveloperGuardScope.ALL)) {
            HttpClient client = TestSupport.trustAllClient();
            String base = base(running);
            String cookie = elevatedSession(client, base);
            assertEquals(200, post(client, base + "/api/developer/agent-password",
                    "{\"password\":\"" + AGENT_PASSWORD + "\",\"scopes\":[\"read\"]}", cookie).statusCode());

            String token = json(post(client, base + "/api/developer/agent-auth",
                    "{\"password\":\"" + AGENT_PASSWORD + "\",\"label\":\"probe\"}", null))
                    .get("token").getAsString();
            assertEquals(200, getWithBearer(client, base + "/api/instances", token).statusCode());

            // Clearing the secret ends every grant it minted — the operator's own
            // elevation stays, because it was proved with the other secret.
            assertEquals(200, post(client, base + "/api/developer/agent-password/clear", "{}", cookie).statusCode());
            assertEquals(401, getWithBearer(client, base + "/api/instances", token).statusCode());
            assertTrue(json(get(client, base + "/api/developer/status", cookie)).get("elevated").getAsBoolean());
        }
    }


    @Test
    void theAgentPasswordIsWorthlessUntilSomeoneElevates() throws Exception {
        try (TestSupport.RunningServer running = start(DeveloperGuardScope.ALL)) {
            HttpClient client = TestSupport.trustAllClient();
            String base = base(running);
            String cookie = elevatedSession(client, base);
            assertEquals(200, post(client, base + "/api/developer/agent-password",
                    "{\"password\":\"" + AGENT_PASSWORD + "\",\"scopes\":[\"read\"]}", cookie).statusCode());

            // The operator hands the elevation back. The lesser secret survives
            // in its file, but it is now worth nothing: it was never an
            // independent way in.
            assertEquals(200, post(client, base + "/api/developer/revoke", "{}", cookie).statusCode());
            assertFalse(json(get(client, base + "/api/developer/status", cookie)).get("elevated").getAsBoolean(),
                    "revoke must actually drop the elevation");

            HttpResponse<String> refused = post(client, base + "/api/developer/agent-auth",
                    "{\"password\":\"" + AGENT_PASSWORD + "\"}", null);
            assertEquals(409, refused.statusCode(), refused.body());
            assertTrue(refused.body().contains("agent_password_not_elevated"));

            // A fresh proof on any session makes it work again.
            assertEquals(200, post(client, base + "/api/developer/elevate",
                    "{\"password\":\"" + DEV_PASSWORD + "\"}", cookie).statusCode());
            assertEquals(200, post(client, base + "/api/developer/agent-auth",
                    "{\"password\":\"" + AGENT_PASSWORD + "\"}", null).statusCode());
        }
    }

    @Test
    void aGrantNeverOutlivesTheElevationThatAuthorizedIt() throws Exception {
        try (TestSupport.RunningServer running = start(DeveloperGuardScope.ALL)) {
            HttpClient client = TestSupport.trustAllClient();
            String base = base(running);
            String cookie = elevatedSession(client, base);
            assertEquals(200, post(client, base + "/api/developer/agent-password",
                    "{\"password\":\"" + AGENT_PASSWORD + "\",\"scopes\":[\"read\"]}", cookie).statusCode());

            java.time.Instant elevationExpiry = java.time.Instant.parse(
                    json(get(client, base + "/api/developer/status", cookie))
                            .get("expiresAt").getAsString());

            // Asked for three hours from a thirty-minute window: the answer is
            // the window, not the wish.
            HttpResponse<String> minted = post(client, base + "/api/developer/agent-auth",
                    "{\"password\":\"" + AGENT_PASSWORD + "\",\"ttlSeconds\":10800}", null);
            assertEquals(200, minted.statusCode(), minted.body());
            java.time.Instant grantExpiry = java.time.Instant.parse(
                    json(minted).get("expiresAt").getAsString());
            assertFalse(grantExpiry.isAfter(elevationExpiry.plusSeconds(1)),
                    "grant " + grantExpiry + " outlived the elevation " + elevationExpiry);
        }
    }

    @Test
    void stoppingTheElevationStopsItsAgents() throws Exception {
        try (TestSupport.RunningServer running = start(DeveloperGuardScope.ALL)) {
            HttpClient client = TestSupport.trustAllClient();
            String base = base(running);
            String cookie = elevatedSession(client, base);
            assertEquals(200, post(client, base + "/api/developer/agent-password",
                    "{\"password\":\"" + AGENT_PASSWORD + "\",\"scopes\":[\"read\"]}", cookie).statusCode());
            String token = json(post(client, base + "/api/developer/agent-auth",
                    "{\"password\":\"" + AGENT_PASSWORD + "\",\"label\":\"probe\"}", null))
                    .get("token").getAsString();
            assertEquals(200, getWithBearer(client, base + "/api/instances", token).statusCode());

            // 「停止提权」 is the operator pulling the plug, not merely logging
            // their own session out of the developer pages.
            assertEquals(200, post(client, base + "/api/developer/revoke", "{}", cookie).statusCode());
            assertEquals(401, getWithBearer(client, base + "/api/instances", token).statusCode());
        }
    }


    @Test
    void theScopeCapIsEditedWithoutRetypingThePassword() throws Exception {
        try (TestSupport.RunningServer running = start(DeveloperGuardScope.ALL)) {
            HttpClient client = TestSupport.trustAllClient();
            String base = base(running);
            String cookie = elevatedSession(client, base);
            assertEquals(200, post(client, base + "/api/developer/agent-password",
                    "{\"password\":\"" + AGENT_PASSWORD + "\",\"scopes\":[\"read\",\"mc\"]}", cookie).statusCode());

            String token = json(post(client, base + "/api/developer/agent-auth",
                    "{\"password\":\"" + AGENT_PASSWORD + "\",\"scopes\":[\"read\",\"mc\"]}", null))
                    .get("token").getAsString();

            // Narrowing what a secret may grant is a policy edit: it must not need the
            // secret again, or the page shows a selection that never reaches the server.
            HttpResponse<String> narrowed = post(client, base + "/api/developer/agent-password/scopes",
                    "{\"scopes\":[\"read\"]}", cookie);
            assertEquals(200, narrowed.statusCode(), narrowed.body());
            assertEquals("[\"read\"]", json(narrowed).get("scopes").toString());

            // The ticket minted under the wider cap is gone with it.
            assertEquals(401, getWithBearer(client, base + "/api/instances", token).statusCode());

            // The password itself still works, and now mints only the narrowed cap.
            HttpResponse<String> minted = post(client, base + "/api/developer/agent-auth",
                    "{\"password\":\"" + AGENT_PASSWORD + "\",\"scopes\":[\"read\",\"mc\"]}", null);
            assertEquals(200, minted.statusCode(), minted.body());
            assertEquals("[\"read\"]", json(minted).get("scopes").toString());
        }
    }

    private TestSupport.RunningServer start(DeveloperGuardScope scope) throws Exception {
        Consumer<ServerConfig> config = server -> {
            server.bindHost = "127.0.0.1";
            server.https.enabled = true;
            server.developerGuard.scope = scope;
        };
        return TestSupport.start(dataDir, Map.of(UserStore.ENV_ADMIN_PASSWORD, PANEL_PASSWORD), config);
    }

    private static String base(TestSupport.RunningServer running) {
        return "https://127.0.0.1:" + running.port();
    }

    private static String elevatedSession(HttpClient client, String base) throws Exception {
        HttpResponse<String> login = post(client, base + "/api/auth/login",
                "{\"username\":\"admin\",\"password\":\"" + PANEL_PASSWORD + "\"}", null);
        assertEquals(200, login.statusCode(), login.body());
        String cookie = login.headers().firstValue("set-cookie").orElseThrow().split(";", 2)[0];
        assertEquals(200, post(client, base + "/api/developer/password",
                "{\"adminPassword\":\"" + PANEL_PASSWORD + "\",\"password\":\"" + DEV_PASSWORD
                        + "\",\"confirm\":\"" + DEV_PASSWORD + "\"}", cookie).statusCode());
        assertEquals(200, post(client, base + "/api/developer/elevate",
                "{\"password\":\"" + DEV_PASSWORD + "\"}", cookie).statusCode());
        return cookie;
    }

    private static HttpResponse<String> get(HttpClient client, String url, String cookie) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create(url)).header("Cookie", cookie).GET().build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private static HttpResponse<String> getWithBearer(HttpClient client, String url, String bearer)
            throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create(url))
                        .header("Authorization", "Bearer " + bearer).GET().build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private static HttpResponse<String> post(HttpClient client, String url, String body, String cookie)
            throws Exception {
        return post(client, url, body, cookie, null);
    }

    private static HttpResponse<String> post(HttpClient client, String url, String body,
                                             String cookie, String bearer) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url))
                .header("Content-Type", "application/json; charset=utf-8")
                .POST(HttpRequest.BodyPublishers.ofString(body));
        if (cookie != null) {
            builder.header("Cookie", cookie);
        }
        if (bearer != null) {
            builder.header("Authorization", "Bearer " + bearer);
        }
        return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private static JsonObject json(HttpResponse<String> response) {
        return JsonParser.parseString(response.body()).getAsJsonObject();
    }
}
