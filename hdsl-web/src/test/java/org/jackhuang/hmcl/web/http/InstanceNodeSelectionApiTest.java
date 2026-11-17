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
import com.google.gson.JsonParser;
import org.jackhuang.hmcl.dsh.DshHomeMode;
import org.jackhuang.hmcl.dsh.DshInstanceManager;
import org.jackhuang.hmcl.web.TestSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The per-instance Node selector: the PATCH contract the dropdown uses, the
/// resolution echoed back with it, and the refusal of a value the resolver
/// does not understand.
class InstanceNodeSelectionApiTest {

    @TempDir
    Path dataDir;

    @Test
    void theNodeSelectorCanBePatchedAndIsExposedBack() throws Exception {
        try (TestSupport.RunningServer server = TestSupport.start(dataDir, Map.of(), config -> {
            config.bindHost = "127.0.0.1";
            config.auth.disabled = true;
        })) {
            Path workspace = Files.createDirectories(dataDir.resolve("workspace"));
            DshInstanceManager.create("probe", "0.2.1", "web", workspace, null,
                    DshHomeMode.ISOLATED, null, List.of(), Map.of());

            JsonObject auto = patch(server, "probe", "{\"nodeRuntime\":\"auto\"}");
            JsonObject instance = auto.getAsJsonObject("instance");
            assertEquals("auto", instance.get("nodeRuntime").getAsString());
            assertTrue(instance.has("nodeResolved"), instance.toString());
            assertTrue(instance.getAsJsonObject("nodeResolved").has("reason"), instance.toString());

            JsonObject channel = patch(server, "probe", "{\"nodeRuntime\":\"22\"}");
            assertEquals("22", channel.getAsJsonObject("instance").get("nodeRuntime").getAsString());

            HttpResponse<String> refused = patchRaw(server, "probe", "{\"nodeRuntime\":\"banana\"}");
            assertEquals(400, refused.statusCode(), refused.body());
        }
    }

    private static JsonObject patch(TestSupport.RunningServer server, String id, String body) throws Exception {
        HttpResponse<String> response = patchRaw(server, id, body);
        assertEquals(200, response.statusCode(), response.body());
        return JsonParser.parseString(response.body()).getAsJsonObject();
    }

    private static HttpResponse<String> patchRaw(TestSupport.RunningServer server, String id, String body)
            throws Exception {
        return HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build()
                .send(HttpRequest.newBuilder(URI.create(server.baseUrl() + "/api/instances/" + id))
                        .timeout(Duration.ofSeconds(20))
                        .header("Content-Type", "application/json")
                        .method("PATCH", HttpRequest.BodyPublishers.ofString(body))
                        .build(), HttpResponse.BodyHandlers.ofString());
    }
}
