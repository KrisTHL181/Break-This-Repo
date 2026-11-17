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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jackhuang.hmcl.web.TestSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The shape `GET /api/node` promises, in particular the field that is allowed
/// to be `null`.
///
/// This is not cosmetic: the instances page reads `system.pnpm` after a
/// `system !== null` test, so a **dropped** key is not the same thing as a null
/// one — Gson's default writer removes null members, the page then reads
/// `undefined.pnpm`, and the whole instance list goes blank on every machine
/// without a system Node.js. The endpoint has to spell the null out.
class NodeApiTest {

    @TempDir
    Path dataDir;

    @Test
    void theSystemFieldIsPresentEvenWhenThereIsNoNode() throws Exception {
        try (TestSupport.RunningServer server = TestSupport.start(dataDir, Map.of(), config -> {
            config.bindHost = "127.0.0.1";
            config.auth.disabled = true;
        })) {
            HttpResponse<String> response = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build()
                    .send(HttpRequest.newBuilder(URI.create(server.baseUrl() + "/api/node"))
                            .timeout(Duration.ofSeconds(20))
                            .GET()
                            .build(), HttpResponse.BodyHandlers.ofString());
            assertTrue(response.statusCode() == 200, response.statusCode() + " " + response.body());

            JsonObject body = JsonParser.parseString(response.body()).getAsJsonObject();
            assertTrue(body.has("system"), "the key must survive Gson: " + body);
            JsonElement system = body.get("system");
            assertTrue(system.isJsonNull() || system.isJsonObject(), "system=" + system);
            assertTrue(body.has("managed"), body.toString());
            assertTrue(body.get("managed").isJsonArray(), body.toString());
            assertTrue(body.has("requirement"), body.toString());
            assertFalse(body.get("requirement").getAsString().isBlank(), body.toString());
            assertTrue(body.has("default"), body.toString());
            assertTrue(body.has("channels"), body.toString());
            assertTrue(body.get("channels").isJsonArray(), body.toString());
            assertTrue(body.has("runtimes"), body.toString());
            assertTrue(body.get("runtimes").isJsonArray(), body.toString());
            assertTrue(body.has("consumers"), body.toString());
            assertTrue(body.get("consumers").isJsonArray(), body.toString());
            assertFalse(body.getAsJsonArray("consumers").isEmpty(), body.toString());
            JsonObject consumer = body.getAsJsonArray("consumers").get(0).getAsJsonObject();
            assertTrue(consumer.has("requirement"), consumer.toString());
            assertTrue(consumer.has("lastGood"), consumer.toString());
        }
    }

    @Test
    void resolveAnswersWithTheResolverThePageUses() throws Exception {
        try (TestSupport.RunningServer server = TestSupport.start(dataDir, Map.of(), config -> {
            config.bindHost = "127.0.0.1";
            config.auth.disabled = true;
        })) {
            HttpResponse<String> ok = get(server, "/api/node/resolve?selector=auto");
            assertEquals(200, ok.statusCode(), ok.body());
            JsonObject body = JsonParser.parseString(ok.body()).getAsJsonObject();
            assertTrue(body.has("version"), body.toString());
            assertTrue(body.has("source"), body.toString());
            assertTrue(body.has("compatible"), body.toString());
            assertTrue(body.has("reason"), body.toString());

            HttpResponse<String> invalid = get(server, "/api/node/resolve?selector=banana");
            assertEquals(400, invalid.statusCode(), invalid.body());
        }
    }

    @Test
    void gcAndRevertDoNotInventAnything() throws Exception {
        try (TestSupport.RunningServer server = TestSupport.start(dataDir, Map.of(), config -> {
            config.bindHost = "127.0.0.1";
            config.auth.disabled = true;
        })) {
            HttpResponse<String> gc = post(server, "/api/node/gc", "{}");
            assertEquals(200, gc.statusCode(), gc.body());
            assertTrue(JsonParser.parseString(gc.body()).getAsJsonObject()
                    .get("removed").isJsonArray(), gc.body());

            HttpResponse<String> revert = post(server, "/api/node/revert",
                    "{\"kind\":\"instance\",\"id\":\"nope\"}");
            assertEquals(404, revert.statusCode(), revert.body());
        }
    }

    private static HttpResponse<String> post(TestSupport.RunningServer server, String path, String body)
            throws Exception {
        return HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build()
                .send(HttpRequest.newBuilder(URI.create(server.baseUrl() + path))
                        .timeout(Duration.ofSeconds(20))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .build(), HttpResponse.BodyHandlers.ofString());
    }

    private static HttpResponse<String> get(TestSupport.RunningServer server, String path) throws Exception {
        return HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build()
                .send(HttpRequest.newBuilder(URI.create(server.baseUrl() + path))
                        .timeout(Duration.ofSeconds(20))
                        .GET()
                        .build(), HttpResponse.BodyHandlers.ofString());
    }
}
