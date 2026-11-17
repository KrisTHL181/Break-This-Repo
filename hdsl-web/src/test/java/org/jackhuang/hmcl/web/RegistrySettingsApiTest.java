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
package org.jackhuang.hmcl.web;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpServer;
import org.jackhuang.hmcl.Metadata;
import org.jackhuang.hmcl.dsh.NpmRegistry;
import org.jackhuang.hmcl.setting.SettingsManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The download-source endpoint over real HTTP: the mirrors the page offers, a choice that is saved
/// to the settings file *and* written into pnpm's own configuration, a hand-typed address that is
/// normalised, and a refused address that changes nothing.
///
/// The refusals are the part worth a server: the value is written into a YAML file and handed to a
/// child process, and the endpoint has to answer 400 without leaving half a choice behind — a save
/// that half-worked is worse than one that did not.
class RegistrySettingsApiTest {

    @TempDir
    Path dataDir;

    /// Where pnpm's configuration is redirected: the endpoint writes it on every save, and the real
    /// file belongs to whatever machine this runs on.
    @TempDir
    Path configDir;

    private Path pnpmConfig() {
        return configDir.resolve("pnpm").resolve("config.yaml");
    }

    private static Path settingsFile() {
        return Metadata.HMCL_USER_HOME.resolve("launcher-settings.json");
    }

    /// The process-wide settings are shared with every other test class in this JVM, and the
    /// endpoint changes them: both the test and the teardown put them back to the default.
    private static void chooseNothing() {
        SettingsManager.settings().setNpmRegistryPreset(NpmRegistry.ENVIRONMENT);
        SettingsManager.settings().setNpmRegistry("");
    }

    @BeforeEach
    void isolateTheWriterAndTheSetting() {
        System.setProperty("hdsl.pnpmConfig", pnpmConfig().toString());
        chooseNothing();
    }

    @AfterEach
    void restoreTheDefaults() {
        System.clearProperty("hdsl.pnpmConfig");
        chooseNothing();
    }

    private TestSupport.RunningServer startServer() throws Exception {
        return TestSupport.start(dataDir, Map.of(), config -> {
            config.bindHost = "127.0.0.1";
            config.auth.disabled = true;
        });
    }

    @Test
    void thePageIsToldWhatIsInForceAndWhatItMayChoose() throws Exception {
        try (TestSupport.RunningServer running = startServer()) {
            HttpResponse<String> response = get(TestSupport.client(), running.baseUrl() + "/api/settings/registry");
            assertEquals(200, response.statusCode(), response.body());
            JsonObject body = json(response);

            assertEquals(NpmRegistry.ENVIRONMENT, body.get("preset").getAsString(),
                    "a launcher nobody configured follows its deployment");
            assertEquals("", body.get("registry").getAsString(), "nothing was typed by hand yet");
            assertTrue(body.get("effective").getAsString().startsWith("http"), body.toString());
            assertTrue(Set.of("environment", "default").contains(body.get("source").getAsString()),
                    body.toString());

            JsonArray presets = body.getAsJsonArray("presets");
            assertEquals(6, presets.size(), presets.toString());
            List<String> ids = new ArrayList<>();
            for (JsonElement element : presets) {
                JsonObject preset = element.getAsJsonObject();
                ids.add(preset.get("id").getAsString());
                assertFalse(preset.get("label").getAsString().isBlank(), preset.toString());
                assertFalse(preset.get("note").getAsString().isBlank(), preset.toString());
                assertNotNull(preset.get("url"));
            }
            assertTrue(ids.containsAll(List.of(
                            NpmRegistry.ENVIRONMENT, "npmjs", "npmmirror", "ustc", "tencent", "huawei")),
                    ids.toString());
        }
    }

    @Test
    void choosingAMirrorIsSavedAndWrittenIntoPnpmsOwnConfiguration() throws Exception {
        try (TestSupport.RunningServer running = startServer()) {
            HttpClient client = TestSupport.client();
            String url = running.baseUrl() + "/api/settings/registry";

            HttpResponse<String> posted = postJson(client, url, "{\"preset\":\"npmmirror\"}");
            assertEquals(200, posted.statusCode(), posted.body());
            JsonObject body = json(posted);
            assertEquals("npmmirror", body.get("preset").getAsString());
            assertEquals("https://registry.npmmirror.com", body.get("effective").getAsString());
            assertEquals("setting", body.get("source").getAsString(),
                    "a chosen mirror answers before the deployment does");

            // The choice is on disk, where the next start finds it.
            assertTrue(Files.isRegularFile(settingsFile()), settingsFile().toString());
            String saved = Files.readString(settingsFile(), StandardCharsets.UTF_8);
            assertTrue(saved.contains("npmRegistryPreset"), saved);
            assertEquals("npmmirror", JsonParser.parseString(saved).getAsJsonObject()
                    .get("npmRegistryPreset").getAsString());

            // …and in pnpm's own file, which is the one that decides what the next install fetches:
            // pnpm does not read the environment.
            assertTrue(Files.isRegularFile(pnpmConfig()), pnpmConfig().toString());
            String pnpm = Files.readString(pnpmConfig(), StandardCharsets.UTF_8);
            assertTrue(pnpm.contains("registry: \"https://registry.npmmirror.com\""), pnpm);

            // The endpoint describes the new choice from then on.
            JsonObject after = json(get(client, url));
            assertEquals("npmmirror", after.get("preset").getAsString());
            assertEquals("https://registry.npmmirror.com", after.get("effective").getAsString());
            assertEquals("setting", after.get("source").getAsString());
        }
    }

    @Test
    void aHandTypedAddressIsStoredWithoutItsTrailingSlash() throws Exception {
        try (TestSupport.RunningServer running = startServer()) {
            HttpClient client = TestSupport.client();
            String url = running.baseUrl() + "/api/settings/registry";

            HttpResponse<String> posted = postJson(client, url,
                    "{\"preset\":\"custom\",\"registry\":\"https://nexus.example.com/repository/npm/\"}");
            assertEquals(200, posted.statusCode(), posted.body());
            JsonObject body = json(posted);
            assertEquals("custom", body.get("preset").getAsString());
            assertEquals("https://nexus.example.com/repository/npm", body.get("registry").getAsString(),
                    "the echo is the spelling that was stored, without the slash corepack would double");
            assertEquals("https://nexus.example.com/repository/npm", body.get("effective").getAsString());
            assertEquals("setting", body.get("source").getAsString());

            JsonObject after = json(get(client, url));
            assertEquals("custom", after.get("preset").getAsString());
            assertEquals("https://nexus.example.com/repository/npm", after.get("registry").getAsString());
        }
    }

    @Test
    void anAddressThatWouldInjectASecondSettingIsRefusedWithoutSavingAnything() throws Exception {
        try (TestSupport.RunningServer running = startServer()) {
            HttpClient client = TestSupport.client();
            String url = running.baseUrl() + "/api/settings/registry";

            assertEquals(200, postJson(client, url, "{\"preset\":\"npmmirror\"}").statusCode());
            JsonElement before = JsonParser.parseString(get(client, url).body());
            String pnpmBefore = Files.readString(pnpmConfig(), StandardCharsets.UTF_8);

            JsonObject bad = new JsonObject();
            bad.addProperty("preset", "custom");
            // The newline is the injection: written through, the rest becomes a second setting in
            // pnpm's configuration file.
            bad.addProperty("registry", "https://x/\nstoreDir: /etc");
            HttpResponse<String> refused = postJson(client, url, bad.toString());
            assertEquals(400, refused.statusCode(), refused.body());
            String error = json(refused).get("error").getAsString();
            assertTrue(error.matches(".*[\\u4e00-\\u9fff].*"),
                    "the message is shown to whoever typed the address: " + error);

            assertEquals(before, JsonParser.parseString(get(client, url).body()),
                    "a refused address must leave the choice exactly as it was");
            assertEquals(pnpmBefore, Files.readString(pnpmConfig(), StandardCharsets.UTF_8),
                    "and must not reach pnpm's configuration file either");
            assertFalse(Files.readString(pnpmConfig(), StandardCharsets.UTF_8).contains("/etc"));
        }
    }

    @Test
    void aPresetNobodyPublishesIsRefused() throws Exception {
        try (TestSupport.RunningServer running = startServer()) {
            HttpClient client = TestSupport.client();
            String url = running.baseUrl() + "/api/settings/registry";
            JsonElement before = JsonParser.parseString(get(client, url).body());

            HttpResponse<String> refused = postJson(client, url, "{\"preset\":\"nonsense\"}");
            assertEquals(400, refused.statusCode(), refused.body());
            assertFalse(json(refused).get("error").getAsString().isBlank(), refused.body());

            assertEquals(before, JsonParser.parseString(get(client, url).body()));
        }
    }

    @Test
    void choosingTheEnvironmentPresetGivesTheChoiceBack() throws Exception {
        try (TestSupport.RunningServer running = startServer()) {
            HttpClient client = TestSupport.client();
            String url = running.baseUrl() + "/api/settings/registry";

            assertEquals(200, postJson(client, url, "{\"preset\":\"npmmirror\"}").statusCode());
            HttpResponse<String> posted = postJson(client, url, "{\"preset\":\"environment\"}");
            assertEquals(200, posted.statusCode(), posted.body());
            JsonObject body = json(posted);
            assertEquals(NpmRegistry.ENVIRONMENT, body.get("preset").getAsString());
            assertTrue(Set.of("environment", "default").contains(body.get("source").getAsString()),
                    body.toString());
            assertEquals("environment", JsonParser.parseString(
                            Files.readString(settingsFile(), StandardCharsets.UTF_8))
                    .getAsJsonObject().get("npmRegistryPreset").getAsString());
        }
    }

    @Test
    void probingMeasuresTheAddressItIsGivenAndNothingElse() throws Exception {
        HttpServer stub = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        stub.createContext("/is-number", exchange -> {
            byte[] bytes = "{\"name\":\"is-number\"}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("content-type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        stub.start();
        try (TestSupport.RunningServer running = startServer()) {
            HttpClient client = TestSupport.client();
            String save = running.baseUrl() + "/api/settings/registry";
            String url = save + "/test";

            // The probe is the whole point of the setting: a source that answers and a source that
            // does not are told apart without waiting for an install. The stub is local, so no test
            // touches a real mirror.
            String live = "http://127.0.0.1:" + stub.getAddress().getPort() + "/";
            HttpResponse<String> measured = postJson(client, url, "{\"registry\":\"" + live + "\"}");
            assertEquals(200, measured.statusCode(), measured.body());
            JsonObject body = json(measured);
            assertEquals("http://127.0.0.1:" + stub.getAddress().getPort(),
                    body.get("registry").getAsString());
            assertTrue(body.get("ok").getAsBoolean(), measured.body());
            assertEquals(200, body.get("status").getAsInt());
            assertTrue(body.get("millis").isJsonPrimitive() && body.get("millis").getAsLong() >= 0,
                    measured.body());

            // With no address in the body it measures what is in force — the page's "test the one
            // that is set" button. The choice is pointed at the stub first, so this stays local too.
            assertEquals(200, postJson(client, save,
                    "{\"preset\":\"custom\",\"registry\":\"" + live + "\"}").statusCode());
            JsonObject current = json(postJson(client, url, "{}"));
            assertEquals("http://127.0.0.1:" + stub.getAddress().getPort(),
                    current.get("registry").getAsString());
            assertTrue(current.get("ok").getAsBoolean(), current.toString());

            // A port nothing listens on is an answer too, not a hang: ok false with no status.
            HttpResponse<String> dead = postJson(client, url, "{\"registry\":\"http://127.0.0.1:1/\"}");
            assertEquals(200, dead.statusCode(), dead.body());
            JsonObject deadBody = json(dead);
            assertFalse(deadBody.get("ok").getAsBoolean(), dead.body());
            assertTrue(deadBody.get("status").isJsonNull(), dead.body());
            // The reason is reported when the failure carries one: a connection refused has a
            // message on this JVM and none on another, so a null member is an answer too.
            assertTrue(deadBody.has("error"), dead.body());
            JsonElement reason = deadBody.get("error");
            assertTrue(reason.isJsonNull() || !reason.getAsString().isBlank(), dead.body());

            // An address that will not be used is not measured.
            HttpResponse<String> refused = postJson(client, url, "{\"registry\":\"not an address\"}");
            assertEquals(400, refused.statusCode(), refused.body());
            assertFalse(json(refused).get("error").getAsString().isBlank(), refused.body());
        } finally {
            stub.stop(0);
        }
    }

    // ----------------------------------------------------------------- helpers --

    private static HttpResponse<String> get(HttpClient client, String url) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create(url)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private static HttpResponse<String> postJson(HttpClient client, String url, String body) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create(url))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8)).build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private static JsonObject json(HttpResponse<String> response) {
        return JsonParser.parseString(response.body()).getAsJsonObject();
    }
}
