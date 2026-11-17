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
package org.jackhuang.hmcl.web.zcode;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jackhuang.hmcl.web.TestSupport;
import org.jackhuang.hmcl.web.config.ServerConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The ZCode terminal route shares its gates with dsh and the brand categories
/// — one implementation ([org.jackhuang.hmcl.web.terminal.InstanceTerminal]) —
/// so this checks the order a caller can actually reach without a PTY: instance
/// lookup, the web shell switch, HTTPS, the developer password.
class ZcodeTerminalApiTest {

    @TempDir
    Path dataDir;

    @Test
    void anUnknownInstanceIsANotFoundBeforeAnyGate() throws Exception {
        try (TestSupport.RunningServer running = TestSupport.start(dataDir, Map.of(), this::loopbackAuthOff)) {
            HttpResponse<String> response = post(running.baseUrl() + "/api/zcode/instances/nope/terminal", null);
            assertEquals(404, response.statusCode(), response.body());
        }
    }

    @Test
    void theWebShellSwitchIsTheFirstRefusal() throws Exception {
        try (TestSupport.RunningServer running = TestSupport.start(dataDir, Map.of(), config -> {
            loopbackAuthOff(config);
            config.webShell.enabled = false;
        })) {
            String id = createInstance(running);
            HttpResponse<String> response = post(running.baseUrl() + "/api/zcode/instances/" + id + "/terminal", null);
            assertEquals(403, response.statusCode(), response.body());
            assertTrue(error(response).startsWith("web_shell_disabled"), error(response));
        }
    }

    @Test
    void plainHttpIsRefusedWithInsecureTransport() throws Exception {
        try (TestSupport.RunningServer running = TestSupport.start(dataDir, Map.of(), this::loopbackAuthOff)) {
            String id = createInstance(running);
            HttpResponse<String> response = post(running.baseUrl() + "/api/zcode/instances/" + id + "/terminal", null);
            assertEquals(403, response.statusCode(), response.body());
            assertTrue(error(response).startsWith("insecure_transport"), error(response));
        }
    }

    /// Behind a TLS-terminating proxy the panel accepts `X-Forwarded-Proto`
    /// once `https.trust_forwarded_proto` is on; the next gate is the developer
    /// password, which a fresh panel has not set.
    @Test
    void anHttpsProxyStillNeedsADeveloperPassword() throws Exception {
        try (TestSupport.RunningServer running = TestSupport.start(dataDir, Map.of(), config -> {
            loopbackAuthOff(config);
            config.https.trustForwardedProto = true;
        })) {
            String id = createInstance(running);
            HttpResponse<String> response = post(running.baseUrl() + "/api/zcode/instances/" + id + "/terminal", "https");
            assertEquals(409, response.statusCode(), response.body());
            assertTrue(error(response).startsWith("developer_password_not_set"), error(response));
        }
    }

    /// Stopping a terminal is deliberately not behind the elevation gate: a
    /// stuck agent must be killable by whoever is logged in.
    @Test
    void deleteNeedsNoElevationAndReportsWhetherAnythingRan() throws Exception {
        try (TestSupport.RunningServer running = TestSupport.start(dataDir, Map.of(), this::loopbackAuthOff)) {
            String id = createInstance(running);
            HttpResponse<String> response = delete(running.baseUrl() + "/api/zcode/instances/" + id + "/terminal");
            assertEquals(200, response.statusCode(), response.body());
            JsonObject body = JsonParser.parseString(response.body()).getAsJsonObject();
            assertTrue(body.get("ok").getAsBoolean());
            assertEquals(false, body.get("killed").getAsBoolean());
        }
    }

    // ----------------------------------------------------------------- helpers --

    private void loopbackAuthOff(ServerConfig config) {
        config.bindHost = "127.0.0.1";
        config.auth.disabled = true;
    }

    private static String createInstance(TestSupport.RunningServer running) throws Exception {
        HttpResponse<String> created = post(running.baseUrl() + "/api/zcode/instances", null);
        assertEquals(201, created.statusCode(), created.body());
        return JsonParser.parseString(created.body()).getAsJsonObject()
                .getAsJsonObject("instance").get("id").getAsString();
    }

    private static HttpResponse<String> post(String url, String forwardedProto) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url))
                .header("Content-Type", "application/json; charset=utf-8")
                .POST(HttpRequest.BodyPublishers.ofString("{\"name\":\"lab\"}"));
        if (forwardedProto != null) {
            builder.header("X-Forwarded-Proto", forwardedProto);
        }
        return TestSupport.client().send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private static HttpResponse<String> delete(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url)).DELETE().build();
        return TestSupport.client().send(request, HttpResponse.BodyHandlers.ofString());
    }

    private static String error(HttpResponse<String> response) {
        return JsonParser.parseString(response.body()).getAsJsonObject().get("error").getAsString();
    }
}