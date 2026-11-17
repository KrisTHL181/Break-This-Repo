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
import org.jackhuang.hmcl.web.auth.UserStore;
import org.jackhuang.hmcl.web.config.ServerConfigLoader;
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

/// The experimental ZCode category behind the panel's own mount.
///
/// An instance binds loopback, so the browser reaches it through `/i/<id>/` —
/// the same mount dsh uses — which means one origin, one certificate and one
/// session gate for both categories. The stub distribution really serves HTTP,
/// so the proxy's forwarding, prefix stripping, redirect and 404/502 answers are
/// all exercised for real.
class ZcodeProxyTest {

    private static final String PASSWORD = "zcode-proxy-password";

    private static final Path PACKAGE = Path.of("src/test/resources/fake-zcode")
            .toAbsolutePath().normalize();

    @TempDir
    Path dataDir;

    @Test
    void aRunningInstanceIsReachableThroughTheMount() throws Exception {
        Map<String, String> env = Map.of(
                UserStore.ENV_ADMIN_PASSWORD, PASSWORD,
                ServerConfigLoader.ENV_ZCODE_PACKAGE, PACKAGE.toString());
        try (TestSupport.RunningServer running = TestSupport.start(dataDir, env,
                config -> config.bindHost = "127.0.0.1")) {
            HttpClient client = TestSupport.client();
            String base = running.baseUrl();

            // The mount sits behind the same session gate as every other route.
            assertEquals(401, get(client, base + "/i/whatever/", null).statusCode());

            String cookie = login(client, base);
            String id = createAndLaunch(client, base, cookie);

            HttpResponse<String> page = get(client, base + "/i/" + id + "/", cookie);
            assertEquals(200, page.statusCode(), page.body());
            assertTrue(page.body().contains("fake zcode"), page.body());

            // Deep paths reach the upstream with the mount stripped, query and all.
            HttpResponse<String> deep = get(client, base + "/i/" + id + "/some/route?x=1", cookie);
            assertEquals(200, deep.statusCode(), deep.body());
            assertTrue(deep.body().contains("/some/route?x=1"), deep.body());

            // A bare mount redirects to the directory form, exactly as for dsh.
            assertEquals(301, get(client, base + "/i/" + id, cookie).statusCode());

            // An id nobody knows is 404; a known but stopped instance is 502.
            assertEquals(404, get(client, base + "/i/no-such-instance/", cookie).statusCode());
            HttpResponse<String> stopped = post(client, base + "/api/zcode/instances/" + id + "/stop", "{}", cookie);
            assertEquals("stopped", json(stopped).get("state").getAsString());
            assertEquals(502, get(client, base + "/i/" + id + "/", cookie).statusCode());
        }
    }

    // ----------------------------------------------------------------- helpers --

    private static String login(HttpClient client, String base) throws Exception {
        HttpResponse<String> response = post(client, base + "/api/auth/login",
                "{\"username\":\"admin\",\"password\":\"" + PASSWORD + "\"}", null);
        assertEquals(200, response.statusCode(), response.body());
        return response.headers().firstValue("set-cookie").orElseThrow().split(";", 2)[0];
    }

    private static String createAndLaunch(HttpClient client, String base, String cookie) throws Exception {
        HttpResponse<String> created = post(client, base + "/api/zcode/instances",
                "{\"name\":\"proxied\"}", cookie);
        assertEquals(201, created.statusCode(), created.body());
        String id = json(created).getAsJsonObject("instance").get("id").getAsString();
        HttpResponse<String> launched = post(client, base + "/api/zcode/instances/" + id + "/launch", "{}", cookie);
        assertEquals(200, launched.statusCode(), launched.body());
        assertEquals("running", json(launched).get("state").getAsString(), launched.body());
        return id;
    }

    private static JsonObject json(HttpResponse<String> response) {
        return JsonParser.parseString(response.body()).getAsJsonObject();
    }

    private static HttpResponse<String> get(HttpClient client, String url, String cookie) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url)).GET();
        if (cookie != null) {
            builder.header("Cookie", cookie);
        }
        return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private static HttpResponse<String> post(HttpClient client, String url, String body, String cookie)
            throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body));
        if (cookie != null) {
            builder.header("Cookie", cookie);
        }
        return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }
}
