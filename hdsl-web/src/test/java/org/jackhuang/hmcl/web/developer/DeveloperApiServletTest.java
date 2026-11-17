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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The developer second factor: the password routes and the elevation endpoint
/// are always present, and the web terminal depends on both.
class DeveloperApiServletTest {

    private static final String PANEL_PASSWORD = "panel-password-1";
    private static final String DEV_PASSWORD = "dev-password-1";

    @TempDir
    Path dataDir;

    @Test
    void passwordLifecycleAndElevationWork() throws Exception {
        try (TestSupport.RunningServer running = TestSupport.start(dataDir,
                Map.of(UserStore.ENV_ADMIN_PASSWORD, PANEL_PASSWORD), config -> {
                    config.bindHost = "127.0.0.1";
                    config.https.enabled = true;
                })) {
            HttpClient client = TestSupport.trustAllClient();
            String base = "https://127.0.0.1:" + running.port();
            String cookie = login(client, base);

            // No developer password yet: elevate says so, status says so.
            HttpResponse<String> notSet = post(client, base + "/api/developer/elevate",
                    "{\"password\":\"" + DEV_PASSWORD + "\"}", cookie);
            assertEquals(409, notSet.statusCode(), notSet.body());
            assertTrue(json(notSet).get("error").getAsString().contains("developer_password_not_set"));

            // Set it with the panel password as confirmation.
            HttpResponse<String> set = post(client, base + "/api/developer/password",
                    "{\"adminPassword\":\"" + PANEL_PASSWORD + "\",\"password\":\"" + DEV_PASSWORD
                            + "\",\"confirm\":\"" + DEV_PASSWORD + "\"}", cookie);
            assertEquals(200, set.statusCode(), set.body());
            assertTrue(json(set).get("devPasswordSet").getAsBoolean());

            JsonObject status = json(get(client, base + "/api/developer/status", cookie));
            assertTrue(status.get("devPasswordSet").getAsBoolean());
            assertFalse(status.get("elevated").getAsBoolean());

            // The password elevates this session.
            HttpResponse<String> elevated = post(client, base + "/api/developer/elevate",
                    "{\"password\":\"" + DEV_PASSWORD + "\"}", cookie);
            assertEquals(200, elevated.statusCode(), elevated.body());
            assertTrue(json(get(client, base + "/api/developer/status", cookie)).get("elevated").getAsBoolean());

            // Clearing needs the panel password and drops every elevation.
            HttpResponse<String> cleared = post(client, base + "/api/developer/password/clear",
                    "{\"adminPassword\":\"" + PANEL_PASSWORD + "\"}", cookie);
            assertEquals(200, cleared.statusCode(), cleared.body());
            assertFalse(json(get(client, base + "/api/developer/status", cookie)).get("elevated").getAsBoolean());
        }
    }

    @Test
    void theWebShellSwitchNeedsElevationAndACorrectCurrentPassword() throws Exception {
        try (TestSupport.RunningServer running = TestSupport.start(dataDir,
                Map.of(UserStore.ENV_ADMIN_PASSWORD, PANEL_PASSWORD), config -> {
                    config.bindHost = "127.0.0.1";
                    config.https.enabled = true;
                })) {
            HttpClient client = TestSupport.trustAllClient();
            String base = "https://127.0.0.1:" + running.port();
            String cookie = login(client, base);

            HttpResponse<String> set = post(client, base + "/api/developer/password",
                    "{\"adminPassword\":\"" + PANEL_PASSWORD + "\",\"password\":\"" + DEV_PASSWORD
                            + "\",\"confirm\":\"" + DEV_PASSWORD + "\"}", cookie);
            assertEquals(200, set.statusCode(), set.body());

            // The switch starts on and cannot be flipped without the second factor.
            JsonObject initial = json(get(client, base + "/api/developer/status", cookie));
            assertTrue(initial.get("webShellEnabled").getAsBoolean());
            HttpResponse<String> refused = post(client, base + "/api/developer/web-shell",
                    "{\"enabled\":false}", cookie);
            assertEquals(403, refused.statusCode(), refused.body());
            assertTrue(refused.body().contains("developer_elevation_required"));

            HttpResponse<String> elevated = post(client, base + "/api/developer/elevate",
                    "{\"password\":\"" + DEV_PASSWORD + "\"}", cookie);
            assertEquals(200, elevated.statusCode(), elevated.body());

            HttpResponse<String> off = post(client, base + "/api/developer/web-shell",
                    "{\"enabled\":false}", cookie);
            assertEquals(200, off.statusCode(), off.body());
            assertFalse(json(off).get("enabled").getAsBoolean());
            assertFalse(json(get(client, base + "/api/developer/status", cookie))
                    .get("webShellEnabled").getAsBoolean());
            String yaml = Files.readString(running.dataDir().resolve("server.yaml"));
            assertTrue(yaml.contains("web_shell"), yaml);
            assertTrue(yaml.contains("enabled: false"), yaml);

            // Changing the password needs the current one; without it the call fails.
            HttpResponse<String> wrongCurrent = post(client, base + "/api/developer/password",
                    "{\"adminPassword\":\"" + PANEL_PASSWORD + "\",\"password\":\"new-dev-password-1\""
                            + ",\"confirm\":\"new-dev-password-1\",\"currentDevPassword\":\"not-it\"}", cookie);
            assertEquals(403, wrongCurrent.statusCode(), wrongCurrent.body());

            HttpResponse<String> changed = post(client, base + "/api/developer/password",
                    "{\"adminPassword\":\"" + PANEL_PASSWORD + "\",\"password\":\"new-dev-password-1\""
                            + ",\"confirm\":\"new-dev-password-1\",\"currentDevPassword\":\"" + DEV_PASSWORD + "\"}",
                    cookie);
            assertEquals(200, changed.statusCode(), changed.body());
            // A new secret kills every elevation minted against the old one.
            assertFalse(json(get(client, base + "/api/developer/status", cookie)).get("elevated").getAsBoolean());
        }
    }

    @Test
    void elevationCanBeExtendedToThreeHoursAndRevokedEarly() throws Exception {
        try (TestSupport.RunningServer running = TestSupport.start(dataDir,
                Map.of(UserStore.ENV_ADMIN_PASSWORD, PANEL_PASSWORD), config -> {
                    config.bindHost = "127.0.0.1";
                    config.https.enabled = true;
                })) {
            HttpClient client = TestSupport.trustAllClient();
            String base = "https://127.0.0.1:" + running.port();
            String cookie = login(client, base);
            HttpResponse<String> set = post(client, base + "/api/developer/password",
                    "{\"adminPassword\":\"" + PANEL_PASSWORD + "\",\"password\":\"" + DEV_PASSWORD
                            + "\",\"confirm\":\"" + DEV_PASSWORD + "\"}", cookie);
            assertEquals(200, set.statusCode(), set.body());

            // Extending without an elevation would mint authority for free.
            HttpResponse<String> refused = post(client, base + "/api/developer/extend", "{}", cookie);
            assertEquals(403, refused.statusCode(), refused.body());
            assertTrue(refused.body().contains("developer_elevation_required"));

            HttpResponse<String> elevated = post(client, base + "/api/developer/elevate",
                    "{\"password\":\"" + DEV_PASSWORD + "\"}", cookie);
            assertEquals(200, elevated.statusCode(), elevated.body());
            JsonObject status = json(get(client, base + "/api/developer/status", cookie));
            assertTrue(status.get("elevated").getAsBoolean());
            Instant initialExpiry = Instant.parse(status.get("expiresAt").getAsString());
            assertTrue(initialExpiry.isBefore(Instant.now().plus(Duration.ofMinutes(31))));

            // The extension answers ~3h out — far beyond the 30-minute TTL,
            // so this proves the stretch and not a slide.
            HttpResponse<String> extended = post(client, base + "/api/developer/extend", "{}", cookie);
            assertEquals(200, extended.statusCode(), extended.body());
            Instant newExpiry = Instant.parse(json(extended).get("expiresAt").getAsString());
            assertTrue(newExpiry.isAfter(Instant.now().plus(Duration.ofHours(2))),
                    "extended expiry should be ~3h out, got " + newExpiry);
            assertEquals(newExpiry, Instant.parse(json(get(client, base + "/api/developer/status", cookie))
                    .get("expiresAt").getAsString()));

            // The extension coupon is one-shot: a second stretch is refused
            // until the password is re-proven.
            HttpResponse<String> twice = post(client, base + "/api/developer/extend", "{}", cookie);
            assertEquals(403, twice.statusCode(), twice.body());
            assertTrue(twice.body().contains("developer_elevation_extended"));

            // Re-proving mints a fresh coupon and keeps the time still on the
            // clock — the 3-hour window must not be cut back to 30 minutes.
            HttpResponse<String> reElevated = post(client, base + "/api/developer/elevate",
                    "{\"password\":\"" + DEV_PASSWORD + "\"}", cookie);
            assertEquals(200, reElevated.statusCode(), reElevated.body());
            Instant keptExpiry = Instant.parse(json(get(client, base + "/api/developer/status", cookie))
                    .get("expiresAt").getAsString());
            assertTrue(keptExpiry.isAfter(Instant.now().plus(Duration.ofHours(2))),
                    "re-verifying must keep the unspent extension time, got " + keptExpiry);
            HttpResponse<String> extendedAgain = post(client, base + "/api/developer/extend", "{}", cookie);
            assertEquals(200, extendedAgain.statusCode(), extendedAgain.body());

            // Revoking drops the elevation immediately and is safe to repeat.
            HttpResponse<String> revoked = post(client, base + "/api/developer/revoke", "{}", cookie);
            assertEquals(200, revoked.statusCode(), revoked.body());
            assertFalse(json(revoked).get("elevated").getAsBoolean());
            JsonObject after = json(get(client, base + "/api/developer/status", cookie));
            assertFalse(after.get("elevated").getAsBoolean());
            assertFalse(after.has("expiresAt"));
            HttpResponse<String> again = post(client, base + "/api/developer/revoke", "{}", cookie);
            assertEquals(200, again.statusCode(), again.body());
        }
    }

    @Test
    void aTrustedProxyCanTerminateTls() throws Exception {
        try (TestSupport.RunningServer running = TestSupport.start(dataDir,
                Map.of(UserStore.ENV_ADMIN_PASSWORD, PANEL_PASSWORD), config -> {
                    config.bindHost = "127.0.0.1";
                    config.https.trustForwardedProto = true;
                })) {
            HttpClient client = TestSupport.trustAllClient();
            String base = "http://127.0.0.1:" + running.port();

            // The header is ignored without the opt-in, so the login cookie is
            // a plain one; with it the same request counts as HTTPS.
            HttpResponse<String> plain = post(client, base + "/api/auth/login",
                    "{\"username\":\"admin\",\"password\":\"" + PANEL_PASSWORD + "\"}", null);
            assertEquals(200, plain.statusCode(), plain.body());
            assertFalse(plain.headers().firstValue("set-cookie").orElseThrow().contains("Secure"));

            HttpResponse<String> proxied = postWithProto(client, base + "/api/auth/login",
                    "{\"username\":\"admin\",\"password\":\"" + PANEL_PASSWORD + "\"}", null, "https");
            assertEquals(200, proxied.statusCode(), proxied.body());
            String cookie = proxied.headers().firstValue("set-cookie").orElseThrow().split(";", 2)[0];
            assertTrue(proxied.headers().firstValue("set-cookie").orElseThrow().contains("Secure"));

            // Without the header the panel honestly says "not HTTPS"; the
            // developer password refuses to travel.
            assertFalse(json(get(client, base + "/api/developer/status", cookie))
                    .get("https").getAsBoolean());
            HttpResponse<String> refused = post(client, base + "/api/developer/password",
                    "{\"adminPassword\":\"" + PANEL_PASSWORD + "\",\"password\":\"" + DEV_PASSWORD
                            + "\",\"confirm\":\"" + DEV_PASSWORD + "\"}", cookie);
            assertEquals(409, refused.statusCode(), refused.body());
            assertTrue(refused.body().contains("tls_required"));

            // Behind the proxy (header present) the same call goes through.
            assertTrue(json(getWithProto(client, base + "/api/developer/status", cookie, "https"))
                    .get("https").getAsBoolean());
            HttpResponse<String> allowed = postWithProto(client, base + "/api/developer/password",
                    "{\"adminPassword\":\"" + PANEL_PASSWORD + "\",\"password\":\"" + DEV_PASSWORD
                            + "\",\"confirm\":\"" + DEV_PASSWORD + "\"}", cookie, "https");
            assertEquals(200, allowed.statusCode(), allowed.body());
        }
    }

    private static String login(HttpClient client, String base) throws Exception {
        HttpResponse<String> response = post(client, base + "/api/auth/login",
                "{\"username\":\"admin\",\"password\":\"" + PANEL_PASSWORD + "\"}", null);
        assertEquals(200, response.statusCode(), response.body());
        return response.headers().firstValue("set-cookie").orElseThrow().split(";", 2)[0];
    }

    private static HttpResponse<String> get(HttpClient client, String url, String cookie) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url)).GET();
        builder.header("Cookie", cookie);
        return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private static HttpResponse<String> getWithProto(HttpClient client, String url, String cookie, String proto)
            throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url))
                .header("Cookie", cookie)
                .header("X-Forwarded-Proto", proto)
                .GET();
        return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private static HttpResponse<String> post(HttpClient client, String url, String body, String cookie)
            throws Exception {
        return postWithProto(client, url, body, cookie, null);
    }

    private static HttpResponse<String> postWithProto(HttpClient client, String url, String body,
                                                      String cookie, @org.jetbrains.annotations.Nullable String proto)
            throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url))
                .header("Content-Type", "application/json; charset=utf-8")
                .POST(HttpRequest.BodyPublishers.ofString(body));
        if (cookie != null) {
            builder.header("Cookie", cookie);
        }
        if (proto != null) {
            builder.header("X-Forwarded-Proto", proto);
        }
        return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private static JsonObject json(HttpResponse<String> response) {
        return JsonParser.parseString(response.body()).getAsJsonObject();
    }
}
