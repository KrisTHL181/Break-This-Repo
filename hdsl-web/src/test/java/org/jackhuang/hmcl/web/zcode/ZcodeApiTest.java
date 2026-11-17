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
import org.jackhuang.hmcl.web.NodeDshStub;
import org.jackhuang.hmcl.web.TestSupport;
import org.jackhuang.hmcl.web.config.ServerConfigLoader;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The experimental ZCode REST surface over real HTTP: the session gate, the
/// distribution discovery, and the whole lifecycle — create, launch, open,
/// logs, credential injection (the exact `provider_config.json` shape),
/// stop, delete, and the conflicts a running instance raises.
class ZcodeApiTest {

    private static final Path PACKAGE = Path.of("src/test/resources/fake-zcode")
            .toAbsolutePath().normalize();

    @TempDir
    Path dataDir;

    // ------------------------------------------------------------- auth gate --

    @Test
    void unauthenticatedRequestsAreRejected() throws Exception {
        try (TestSupport.RunningServer running = TestSupport.start(dataDir, Map.of(), config -> {
            config.bindHost = "127.0.0.1";
        })) {
            HttpResponse<String> response = get(TestSupport.client(),
                    running.baseUrl() + "/api/zcode/instances");
            assertEquals(401, response.statusCode());
            assertEquals("unauthorized", json(response).get("error").getAsString());
        }
    }

    // -------------------------------------------------------------------- dist --

    @Test
    void distIsAbsentUntilAPackageIsConfigured() throws Exception {
        try (TestSupport.RunningServer running = TestSupport.start(dataDir, Map.of(), this::loopbackAuthOff)) {
            HttpResponse<String> absent = get(TestSupport.client(), running.baseUrl() + "/api/zcode/dist");
            assertEquals(200, absent.statusCode(), absent.body());
            JsonObject body = json(absent);
            assertFalse(body.get("present").getAsBoolean());
            assertTrue(body.get("path").getAsString().contains("zcode"),
                    "the fallback directory is reported: " + body);
            assertTrue(body.get("version").isJsonNull());
        }
    }

    @Test
    void distReportsTheConfiguredPackage() throws Exception {
        try (TestSupport.RunningServer running = TestSupport.start(dataDir, zcodeEnv(), this::loopbackAuthOff)) {
            HttpResponse<String> response = get(TestSupport.client(), running.baseUrl() + "/api/zcode/dist");
            assertEquals(200, response.statusCode(), response.body());
            JsonObject body = json(response);
            assertTrue(body.get("present").getAsBoolean());
            assertEquals(PACKAGE.toString(), body.get("path").getAsString());
            assertEquals("0.0.0-test", body.get("version").getAsString(),
                    "the version comes from the distribution's package.json");
        }
    }

    // -------------------------------------------------------------- lifecycle --

    @Test
    void fullLifecycleOverHttp() throws Exception {
        NodeDshStub.assumeNode();
        try (TestSupport.RunningServer running = TestSupport.start(dataDir, zcodeEnv(), this::loopbackAuthOff)) {
            HttpClient client = TestSupport.client();
            String base = running.baseUrl();

            // Create: the answer carries state, masked credentials and a token.
            HttpResponse<String> created = post(client, base + "/api/zcode/instances",
                    "{\"name\":\"lab\"}");
            assertEquals(201, created.statusCode(), created.body());
            JsonObject instance = json(created).getAsJsonObject("instance");
            String id = instance.get("id").getAsString();
            assertEquals("lab", instance.get("name").getAsString());
            assertEquals("created", instance.get("state").getAsString());
            assertFalse(instance.get("hasApiKey").getAsBoolean());
            String token = instance.get("token").getAsString();
            assertNotNull(token);
            assertFalse(token.isBlank());

            // Launch: the stub announces its port and the instance is running.
            HttpResponse<String> launch = post(client, base + "/api/zcode/instances/" + id + "/launch", "{}");
            assertEquals(200, launch.statusCode(), launch.body());
            assertEquals("running", json(launch).get("state").getAsString());
            JsonObject runningInstance = json(get(client, base + "/api/zcode/instances/" + id))
                    .getAsJsonObject("instance");
            assertEquals("running", runningInstance.get("state").getAsString());
            int port = runningInstance.get("lastPort").getAsInt();
            assertTrue(port > 0, "the announced port is persisted: " + runningInstance);

            // Credentials cannot change while the process is up.
            HttpResponse<String> refused = patch(client, base + "/api/zcode/instances/" + id,
                    "{\"apiKey\":\"sk-test-key\"}");
            assertEquals(409, refused.statusCode());

            // Open: the instance is reached through the panel's own mount —
            // same origin, same certificate, same session gate as dsh.
            HttpResponse<String> open = get(client, base + "/api/zcode/instances/" + id + "/open");
            assertEquals(200, open.statusCode(), open.body());
            assertEquals("/i/" + id + "/", json(open).get("url").getAsString());

            // Logs: the process output is there.
            HttpResponse<String> logs = get(client, base + "/api/zcode/instances/" + id + "/logs?tail=50");
            assertEquals(200, logs.statusCode(), logs.body());
            assertTrue(json(logs).getAsJsonArray("lines").size() > 0);

            // Stop, then the credentials save injects the provider config.
            assertEquals("stopped", json(post(client, base + "/api/zcode/instances/" + id + "/stop", "{}"))
                    .get("state").getAsString());
            HttpResponse<String> patched = patch(client, base + "/api/zcode/instances/" + id,
                    "{\"apiKey\":\"sk-test-key\",\"baseUrl\":\"https://api.example.com/v1\"}");
            assertEquals(200, patched.statusCode(), patched.body());
            assertTrue(json(patched).getAsJsonObject("instance").get("hasApiKey").getAsBoolean());
            assertProviderConfig(id);

            // Delete works once stopped; a running instance refuses.
            assertEquals(204, delete(client, base + "/api/zcode/instances/" + id).statusCode());
            assertEquals(404, get(client, base + "/api/zcode/instances/" + id).statusCode());

            // A running instance refuses to be deleted.
            HttpResponse<String> recreated = post(client, base + "/api/zcode/instances",
                    "{\"name\":\"lab-again\",\"apiKey\":\"sk-other\"}");
            assertEquals(201, recreated.statusCode(), recreated.body());
            String secondId = json(recreated).getAsJsonObject("instance").get("id").getAsString();
            assertEquals("running", json(post(client, base + "/api/zcode/instances/" + secondId + "/launch", "{}"))
                    .get("state").getAsString());
            assertEquals(409, delete(client, base + "/api/zcode/instances/" + secondId).statusCode());
            assertEquals("stopped", json(post(client, base + "/api/zcode/instances/" + secondId + "/stop", "{}"))
                    .get("state").getAsString());
            assertEquals(204, delete(client, base + "/api/zcode/instances/" + secondId).statusCode());
        }
    }

    /// A new instance comes with a workspace, built before the first launch and
    /// placed where the operator can see it — next to the instances, not inside
    /// one. The listing advertises that root so the UI can show the path.
    @Test
    void creatingAnInstanceComesWithAWorkspace() throws Exception {
        try (TestSupport.RunningServer running = TestSupport.start(dataDir, Map.of(), this::loopbackAuthOff)) {
            HttpClient client = TestSupport.client();
            String base = running.baseUrl();
            Path workspacesRoot = dataDir.resolve("zcode").resolve("workspaces");

            HttpResponse<String> created = post(client, base + "/api/zcode/instances", "{\"name\":\"demo\"}");
            assertEquals(201, created.statusCode(), created.body());
            String workspacePath = json(created).getAsJsonObject("instance").get("workspacePath").getAsString();
            assertFalse(workspacePath.isBlank(), "the workspace is reported: " + workspacePath);
            assertTrue(Files.isDirectory(Path.of(workspacePath)),
                    "the workspace is created with the instance: " + workspacePath);
            assertTrue(Path.of(workspacePath).startsWith(workspacesRoot),
                    "the workspace lives under " + workspacesRoot + ", was " + workspacePath);

            HttpResponse<String> listed = get(client, base + "/api/zcode/instances");
            assertEquals(200, listed.statusCode(), listed.body());
            assertEquals(workspacesRoot.toString(), json(listed).get("workspacesRoot").getAsString());
        }
    }

    // ---------------------------------------------------------------- plugins --

    /// The plugin channel is a reserved slot with the brand categories' shape.
    @Test
    void pluginChannelIsReservedButEmpty() throws Exception {
        try (TestSupport.RunningServer running = TestSupport.start(dataDir, Map.of(), this::loopbackAuthOff)) {
            HttpClient client = TestSupport.client();
            String base = running.baseUrl();

            HttpResponse<String> created = post(client, base + "/api/zcode/instances", "{\"name\":\"plugins\"}");
            assertEquals(201, created.statusCode(), created.body());
            String id = json(created).getAsJsonObject("instance").get("id").getAsString();

            HttpResponse<String> response = get(client, base + "/api/zcode/instances/" + id + "/plugins");
            assertEquals(200, response.statusCode(), response.body());
            JsonObject body = json(response);
            assertFalse(body.get("supported").getAsBoolean());
            assertTrue(body.getAsJsonArray("plugins").isEmpty());

            assertEquals(404, get(client, base + "/api/zcode/instances/nope/plugins").statusCode());
        }
    }

    /// Asserts the injected `provider_config.json` field by field, against the
    /// shape verified in the ZCode v3.14.3 sources.
    private void assertProviderConfig(String id) throws Exception {
        Path file = dataDir.resolve("zcode").resolve("instances").resolve(id)
                .resolve("data")
                .resolve(ZcodeInstanceManager.PROVIDER_CONFIG_FILE);
        assertTrue(Files.isRegularFile(file), "the provider config must be written: " + file);
        JsonObject root = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
        assertEquals(1, root.get("schemaVersion").getAsInt());

        JsonObject config = root.getAsJsonObject("config");
        JsonObject rule = config.getAsJsonObject("providerConfigRules")
                .getAsJsonArray("providerRules").get(0).getAsJsonObject();
        assertEquals("hdsl-injected", rule.get("providerId").getAsString());
        JsonObject provider = rule.getAsJsonObject("config");
        assertEquals("standard-personal", provider.get("group").getAsString());
        assertEquals("visible", provider.get("visibility").getAsString());
        assertEquals(0, provider.getAsJsonArray("personalModelIds").size());
        JsonObject access = provider.getAsJsonObject("access");
        assertEquals("api-key", access.get("type").getAsString());
        assertEquals("sk-test-key", access.get("apiKey").getAsString());
        JsonObject api = provider.getAsJsonObject("api");
        assertEquals("openai-chat-completions", api.get("type").getAsString());
        assertEquals("https://api.example.com/v1", api.get("baseUrl").getAsString());

        // personalModelConfigRulesSchema is .strict() and requires both arrays.
        JsonObject models = config.getAsJsonObject("modelConfigRules");
        assertEquals(0, models.getAsJsonArray("providerModelRules").size());
        assertEquals(0, models.getAsJsonArray("manualProviderModelRules").size());
        assertNull(models.get("providerOrder"));
    }

    // ----------------------------------------------------------------- helpers --

    private void loopbackAuthOff(org.jackhuang.hmcl.web.config.ServerConfig config) {
        config.bindHost = "127.0.0.1";
        config.auth.disabled = true;
    }

    private static Map<String, String> zcodeEnv() {
        return Map.of(ServerConfigLoader.ENV_ZCODE_PACKAGE, PACKAGE.toString());
    }

    private static JsonObject json(HttpResponse<String> response) {
        return JsonParser.parseString(response.body()).getAsJsonObject();
    }

    private static HttpResponse<String> get(HttpClient client, String url) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create(url)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private static HttpResponse<String> post(HttpClient client, String url, String body) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create(url))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(body)).build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private static HttpResponse<String> patch(HttpClient client, String url, String body) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create(url))
                        .header("Content-Type", "application/json")
                        .method("PATCH", HttpRequest.BodyPublishers.ofString(body)).build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private static HttpResponse<String> delete(HttpClient client, String url) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create(url))
                        .DELETE().build(),
                HttpResponse.BodyHandlers.ofString());
    }
}
