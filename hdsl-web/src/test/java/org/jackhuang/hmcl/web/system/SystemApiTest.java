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
package org.jackhuang.hmcl.web.system;

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
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/// `/api/system/*` over a real server: a plain session may read it even with
/// the developer guard at `all`, and the first statistics request gets a
/// snapshot within a couple of seconds of boot.
class SystemApiTest {

    private static final String PANEL_PASSWORD = "panel-password-1";

    @TempDir
    Path dataDir;

    @Test
    void theMachineAndThePanelsCoordinatesNeedOnlyASession() throws Exception {
        try (TestSupport.RunningServer running = TestSupport.start(dataDir,
                Map.of(UserStore.ENV_ADMIN_PASSWORD, PANEL_PASSWORD), config -> {
                    config.bindHost = "127.0.0.1";
                    config.https.enabled = true;
                    // The paranoid level: everything else would now demand the
                    // developer password. These reads must not.
                    config.developerGuard.scope =
                            org.jackhuang.hmcl.web.config.DeveloperGuardScope.ALL;
                })) {
            HttpClient client = TestSupport.trustAllClient();
            String base = "https://127.0.0.1:" + running.port();
            String cookie = login(client, base);

            HttpResponse<String> info = get(client, base + "/api/system/info", cookie);
            assertEquals(200, info.statusCode(), info.body());
            JsonObject fields = json(info);
            assertEquals("127.0.0.1", fields.get("bindHost").getAsString());
            assertEquals(running.dataDir().toAbsolutePath().normalize().toString(),
                    fields.get("dataDir").getAsString());
            assertTrue(fields.get("port").getAsInt() > 0);
            assertTrue(fields.has("version"));
            assertTrue(fields.has("https"));

            JsonObject stats = awaitSnapshot(client, base + "/api/system/stats?history=5", cookie);
            JsonObject snapshot = stats.getAsJsonObject("snapshot");
            assertTrue(snapshot.has("cpu"), stats.toString());
            assertTrue(snapshot.has("memory"), stats.toString());
            assertTrue(snapshot.has("capabilities"), stats.toString());
            assertTrue(stats.has("history"), stats.toString());

            HttpResponse<String> storage = get(client, base + "/api/system/storage", cookie);
            assertEquals(200, storage.statusCode(), storage.body());
            JsonObject report = json(storage);
            assertEquals(running.dataDir().toAbsolutePath().normalize().toString(),
                    report.get("dataDir").getAsString());
            assertTrue(report.has("buckets"), report.toString());
            assertTrue(report.has("mount"), report.toString());
        }
    }

    /// The stats body is built once per sample and reused, so the two things a
    /// cache can get wrong are its shape (`history` trimmed to `max`, empty when
    /// not asked for) and its length (written from the cached bytes, not from a
    /// second serialisation).
    @Test
    void theCachedStatsBodyKeepsItsShapeAndItsLength() throws Exception {
        try (TestSupport.RunningServer running = TestSupport.start(dataDir,
                Map.of(UserStore.ENV_ADMIN_PASSWORD, PANEL_PASSWORD), config -> {
                    config.bindHost = "127.0.0.1";
                    config.https.enabled = true;
                })) {
            HttpClient client = TestSupport.trustAllClient();
            String base = "https://127.0.0.1:" + running.port();
            String cookie = login(client, base);
            awaitSnapshot(client, base + "/api/system/stats?history=5", cookie);

            HttpResponse<String> small = get(client, base + "/api/system/stats?history=2", cookie);
            assertEquals(200, small.statusCode(), small.body());
            assertTrue(json(small).getAsJsonArray("history").size() <= 2, small.body());
            assertEquals(small.body().getBytes(StandardCharsets.UTF_8).length,
                    Integer.parseInt(small.headers().firstValue("content-length").orElse("-1")),
                    "the response is whatever was cached, so its length has to be the cached length");

            HttpResponse<String> none = get(client, base + "/api/system/stats", cookie);
            assertEquals(200, none.statusCode(), none.body());
            assertEquals(0, json(none).getAsJsonArray("history").size(),
                    "history is opt-in: no parameter, no samples");
            assertTrue(!json(none).get("snapshot").isJsonNull(),
                    "the newest sample is in the body either way");
        }
    }

    /// The sampler starts at boot and its first tick is immediate; poll briefly
    /// so the test does not depend on request timing.
    private static JsonObject awaitSnapshot(HttpClient client, String url, String cookie) throws Exception {
        for (int attempt = 0; attempt < 40; attempt++) {
            HttpResponse<String> response = get(client, url, cookie);
            assertEquals(200, response.statusCode(), response.body());
            JsonObject body = json(response);
            if (body.has("snapshot") && !body.get("snapshot").isJsonNull()) {
                return body;
            }
            Thread.sleep(50);
        }
        fail("the sampler never produced a snapshot");
        return new JsonObject();
    }

    private static String login(HttpClient client, String base) throws Exception {
        HttpResponse<String> response = post(client, base + "/api/auth/login",
                "{\"username\":\"admin\",\"password\":\"" + PANEL_PASSWORD + "\"}");
        assertEquals(200, response.statusCode(), response.body());
        return response.headers().firstValue("set-cookie").orElseThrow().split(";", 2)[0];
    }

    private static HttpResponse<String> get(HttpClient client, String url, String cookie) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create(url))
                .header("Cookie", cookie)
                .GET().build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private static HttpResponse<String> post(HttpClient client, String url, String body) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private static JsonObject json(HttpResponse<String> response) {
        return JsonParser.parseString(response.body()).getAsJsonObject();
    }
}
