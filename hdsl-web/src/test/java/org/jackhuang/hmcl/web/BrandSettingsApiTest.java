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
import org.jackhuang.hmcl.Metadata;
import org.jackhuang.hmcl.setting.SettingsManager;
import org.jackhuang.hmcl.web.auth.UserStore;
import org.jackhuang.hmcl.web.brand.Brand;
import org.jackhuang.hmcl.web.brand.BrandSources;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The brand page's endpoint over real HTTPS: read without the developer password, refuse every
/// write until it is typed, then save the sources all-or-nothing and describe them back.
///
/// The refusals are the part worth a server. The value ends up in a child process's environment,
/// and a save that half-worked is worse than one that did not — so a body with one bad address must
/// leave every other choice exactly where it was.
class BrandSettingsApiTest {

    private static final String PANEL_PASSWORD = "panel-password-1";
    private static final String DEV_PASSWORD = "dev-password-1";

    @TempDir
    Path dataDir;

    private static Path settingsFile() {
        return Metadata.HMCL_USER_HOME.resolve("launcher-settings.json");
    }

    /// The settings are process-wide and shared with every other test class in this JVM.
    private static void chooseNothing() {
        SettingsManager.settings().setBrandPypiIndexPreset(BrandSources.ENVIRONMENT);
        SettingsManager.settings().setBrandPypiIndex("");
        SettingsManager.settings().setBrandPythonMirrorPreset(BrandSources.ENVIRONMENT);
        SettingsManager.settings().setBrandPythonMirror("");
        SettingsManager.settings().setBrandArchivePreset(BrandSources.ENVIRONMENT);
        SettingsManager.settings().setBrandArchiveBase("");
        SettingsManager.settings().setBrandReleasePreset(Brand.OCTOP.id(), BrandSources.ENVIRONMENT);
        SettingsManager.settings().setBrandReleaseBase(Brand.OCTOP.id(), "");
    }

    @BeforeEach
    void resetBefore() {
        chooseNothing();
    }

    @AfterEach
    void resetAfter() {
        chooseNothing();
    }

    private TestSupport.RunningServer startServer() throws Exception {
        return TestSupport.start(dataDir, Map.of(UserStore.ENV_ADMIN_PASSWORD, PANEL_PASSWORD), config -> {
            config.bindHost = "127.0.0.1";
            config.https.enabled = true;
        });
    }

    private static String login(HttpClient client, String base) throws Exception {
        HttpResponse<String> response = post(client, base + "/api/auth/login",
                "{\"username\":\"admin\",\"password\":\"" + PANEL_PASSWORD + "\"}", null);
        assertEquals(200, response.statusCode(), response.body());
        return response.headers().firstValue("set-cookie").orElseThrow().split(";", 2)[0];
    }

    private static void setDeveloperPassword(HttpClient client, String base, String cookie) throws Exception {
        HttpResponse<String> set = post(client, base + "/api/developer/password",
                "{\"adminPassword\":\"" + PANEL_PASSWORD + "\",\"password\":\"" + DEV_PASSWORD
                        + "\",\"confirm\":\"" + DEV_PASSWORD + "\"}", cookie);
        assertEquals(200, set.statusCode(), set.body());
    }

    private static void elevate(HttpClient client, String base, String cookie) throws Exception {
        HttpResponse<String> elevated = post(client, base + "/api/developer/elevate",
                "{\"password\":\"" + DEV_PASSWORD + "\"}", cookie);
        assertEquals(200, elevated.statusCode(), elevated.body());
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
        return send(client, "POST", url, body, cookie);
    }

    private static HttpResponse<String> put(HttpClient client, String url, String body, String cookie)
            throws Exception {
        return send(client, "PUT", url, body, cookie);
    }

    private static HttpResponse<String> send(HttpClient client, String method, String url, String body,
                                             String cookie) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url))
                .header("Content-Type", "application/json; charset=utf-8")
                .method(method, HttpRequest.BodyPublishers.ofString(body));
        if (cookie != null) {
            builder.header("Cookie", cookie);
        }
        return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private static JsonObject json(HttpResponse<String> response) {
        return JsonParser.parseString(response.body()).getAsJsonObject();
    }

    private static JsonObject brandEntry(JsonObject policy, String brand) {
        JsonArray brands = policy.getAsJsonArray("brands");
        for (JsonElement element : brands) {
            JsonObject entry = element.getAsJsonObject();
            if (brand.equals(entry.get("brand").getAsString())) {
                return entry;
            }
        }
        throw new AssertionError("no " + brand + " in " + policy);
    }

    @Test
    void thePageIsReadableWithoutTheDeveloperPassword() throws Exception {
        try (TestSupport.RunningServer running = startServer()) {
            HttpClient client = TestSupport.trustAllClient();
            String base = "https://127.0.0.1:" + running.port();
            String cookie = login(client, base);

            HttpResponse<String> response = get(client, base + "/api/settings/brands", cookie);
            assertEquals(200, response.statusCode(), response.body());
            JsonObject body = json(response);

            JsonObject sources = body.getAsJsonObject("sources");
            JsonArray pypiPresets = sources.getAsJsonObject("pypi").getAsJsonArray("presets");
            assertTrue(pypiPresets.size() >= 5, pypiPresets.toString());
            JsonArray pythonPresets = sources.getAsJsonObject("python").getAsJsonArray("presets");
            assertEquals(3, pythonPresets.size(), pythonPresets.toString());
            JsonArray archivePresets = sources.getAsJsonObject("archive").getAsJsonArray("presets");
            assertEquals(2, archivePresets.size(), archivePresets.toString());

            JsonObject release = sources.getAsJsonObject("release");
            assertTrue(release.has(Brand.OCTOP.id()), release.toString());
            assertEquals(2, release.getAsJsonObject(Brand.OCTOP.id()).getAsJsonArray("presets").size());

            JsonObject octop = brandEntry(body.getAsJsonObject("policy"), Brand.OCTOP.id());
            List<String> capabilities = new ArrayList<>();
            for (JsonElement element : octop.getAsJsonArray("capabilities")) {
                capabilities.add(element.getAsString());
            }
            assertEquals(List.of("singleInstance", "lockedRoute", "sourceBuild"), capabilities);

            JsonObject opencode = brandEntry(body.getAsJsonObject("policy"), Brand.OPENCODE.id());
            List<String> opencodeCapabilities = new ArrayList<>();
            for (JsonElement element : opencode.getAsJsonArray("capabilities")) {
                opencodeCapabilities.add(element.getAsString());
            }
            assertEquals(List.of("singleInstance", "lockedRoute"), opencodeCapabilities,
                    "OpenCode has the last-resort door but no source build");

            JsonObject kimi = brandEntry(body.getAsJsonObject("policy"), Brand.KIMI.id());
            assertEquals(0, kimi.getAsJsonArray("capabilities").size(),
                    "an npm brand has none of those switches");

            // Read is open; the page still says where each source came from.
            assertTrue(List.of("environment", "default").contains(
                    sources.getAsJsonObject("pypi").get("source").getAsString()), body.toString());
        }
    }

    @Test
    void writesAreRefusedUntilTheDeveloperPasswordIsTyped() throws Exception {
        try (TestSupport.RunningServer running = startServer()) {
            HttpClient client = TestSupport.trustAllClient();
            String base = "https://127.0.0.1:" + running.port();
            String cookie = login(client, base);
            setDeveloperPassword(client, base, cookie);

            HttpResponse<String> policy = put(client, base + "/api/settings/brands/policy/octop",
                    "{\"singleInstance\":false,\"survivor\":null,\"lockedRoute\":false,\"sourceBuild\":false}",
                    cookie);
            assertEquals(403, policy.statusCode(), policy.body());
            assertTrue(policy.body().contains("developer_elevation_required"), policy.body());

            HttpResponse<String> sources = post(client, base + "/api/settings/brands/sources",
                    "{\"pypi\":{\"preset\":\"aliyun\"},\"python\":{\"preset\":\"nju\"}}", cookie);
            assertEquals(403, sources.statusCode(), sources.body());
            assertTrue(sources.body().contains("developer_elevation_required"), sources.body());

            HttpResponse<String> probe = post(client, base + "/api/settings/brands/test",
                    "{\"kind\":\"pypi\"}", cookie);
            assertEquals(403, probe.statusCode(), probe.body());
        }
    }

    @Test
    void chosenMirrorsAreSavedAndDescribed() throws Exception {
        try (TestSupport.RunningServer running = startServer()) {
            HttpClient client = TestSupport.trustAllClient();
            String base = "https://127.0.0.1:" + running.port();
            String cookie = login(client, base);
            setDeveloperPassword(client, base, cookie);
            elevate(client, base, cookie);

            HttpResponse<String> saved = post(client, base + "/api/settings/brands/sources",
                    "{\"pypi\":{\"preset\":\"aliyun\"},\"python\":{\"preset\":\"nju\"},"
                            + "\"archive\":{\"preset\":\"codeload\"},"
                            + "\"release\":{\"octop\":{\"preset\":\"github\"}}}", cookie);
            assertEquals(200, saved.statusCode(), saved.body());
            JsonObject body = json(saved);
            JsonObject sources = body.getAsJsonObject("sources");
            assertEquals("https://mirrors.aliyun.com/pypi/simple",
                    sources.getAsJsonObject("pypi").get("effective").getAsString());
            assertEquals("setting", sources.getAsJsonObject("pypi").get("source").getAsString());
            assertEquals("https://mirror.nju.edu.cn/github-release/astral-sh/python-build-standalone",
                    sources.getAsJsonObject("python").get("effective").getAsString());
            assertEquals("https://codeload.github.com",
                    sources.getAsJsonObject("archive").get("effective").getAsString());
            assertEquals("https://github.com/TencentCloud/Octop/releases/download",
                    sources.getAsJsonObject("release").getAsJsonObject("octop").get("effective").getAsString());

            // On disk, where the next start (and the installer) find it.
            assertTrue(Files.isRegularFile(settingsFile()), settingsFile().toString());
            String json = Files.readString(settingsFile(), StandardCharsets.UTF_8);
            assertTrue(json.contains("\"brandPypiIndexPreset\": \"aliyun\""), json);
            assertTrue(json.contains("brandReleasePresets"), json);

            // The description follows the choice.
            JsonObject after = json(get(client, base + "/api/settings/brands", cookie));
            assertEquals("aliyun", after.getAsJsonObject("sources").getAsJsonObject("pypi")
                    .get("preset").getAsString());
        }
    }

    @Test
    void aBadAddressChangesNothing() throws Exception {
        try (TestSupport.RunningServer running = startServer()) {
            HttpClient client = TestSupport.trustAllClient();
            String base = "https://127.0.0.1:" + running.port();
            String cookie = login(client, base);
            setDeveloperPassword(client, base, cookie);
            elevate(client, base, cookie);

            assertEquals(200, post(client, base + "/api/settings/brands/sources",
                    "{\"pypi\":{\"preset\":\"aliyun\"},\"python\":{\"preset\":\"nju\"}}", cookie).statusCode());

            // The Python choice in the same body is valid; it must not be applied either.
            HttpResponse<String> refused = post(client, base + "/api/settings/brands/sources",
                    "{\"pypi\":{\"preset\":\"custom\",\"url\":\"https://user:pass@mirror.example.com/simple\"},"
                            + "\"python\":{\"preset\":\"github\"}}", cookie);
            assertEquals(400, refused.statusCode(), refused.body());

            JsonObject after = json(get(client, base + "/api/settings/brands", cookie))
                    .getAsJsonObject("sources");
            assertEquals("aliyun", after.getAsJsonObject("pypi").get("preset").getAsString(),
                    "the refused save must not have touched the PyPI choice");
            assertEquals("nju", after.getAsJsonObject("python").get("preset").getAsString(),
                    "the refused save must not have applied the valid half either");
        }
    }

    @Test
    void aCustomAddressIsStoredWithoutItsTrailingSlash() throws Exception {
        try (TestSupport.RunningServer running = startServer()) {
            HttpClient client = TestSupport.trustAllClient();
            String base = "https://127.0.0.1:" + running.port();
            String cookie = login(client, base);
            setDeveloperPassword(client, base, cookie);
            elevate(client, base, cookie);

            HttpResponse<String> saved = post(client, base + "/api/settings/brands/sources",
                    "{\"pypi\":{\"preset\":\"custom\",\"url\":\"https://nexus.example.com/repository/pypi/\"},"
                            + "\"python\":{\"preset\":\"environment\"}}", cookie);
            assertEquals(200, saved.statusCode(), saved.body());
            JsonObject pypi = json(saved).getAsJsonObject("sources").getAsJsonObject("pypi");
            assertEquals("custom", pypi.get("preset").getAsString());
            assertEquals("https://nexus.example.com/repository/pypi", pypi.get("url").getAsString());
            assertEquals("https://nexus.example.com/repository/pypi", pypi.get("effective").getAsString());
        }
    }

    @Test
    void theArchiveSourceIsSavedAndCanBeCustomised() throws Exception {
        try (TestSupport.RunningServer running = startServer()) {
            HttpClient client = TestSupport.trustAllClient();
            String base = "https://127.0.0.1:" + running.port();
            String cookie = login(client, base);
            setDeveloperPassword(client, base, cookie);
            elevate(client, base, cookie);

            HttpResponse<String> saved = post(client, base + "/api/settings/brands/sources",
                    "{\"pypi\":{\"preset\":\"aliyun\"},\"python\":{\"preset\":\"nju\"},"
                            + "\"archive\":{\"preset\":\"custom\",\"url\":\"https://mirror.example.com/codeload/\"}}",
                    cookie);
            assertEquals(200, saved.statusCode(), saved.body());
            JsonObject archive = json(saved).getAsJsonObject("sources").getAsJsonObject("archive");
            assertEquals("custom", archive.get("preset").getAsString());
            assertEquals("https://mirror.example.com/codeload", archive.get("effective").getAsString(),
                    "the trailing slash must not survive: the installer appends its own path");
            assertEquals("setting", archive.get("source").getAsString());

            // A save that says nothing about the archive leaves it where it was: older callers
            // predate this source, and refusing their whole save would be a worse answer.
            HttpResponse<String> withoutArchive = post(client, base + "/api/settings/brands/sources",
                    "{\"pypi\":{\"preset\":\"tuna\"},\"python\":{\"preset\":\"nju\"}}", cookie);
            assertEquals(200, withoutArchive.statusCode(), withoutArchive.body());
            assertEquals("custom", json(withoutArchive).getAsJsonObject("sources")
                    .getAsJsonObject("archive").get("preset").getAsString());
        }
    }

    @Test
    void singleInstanceNeedsASurvivorAndUnsupportedSwitchesAreRefused() throws Exception {
        try (TestSupport.RunningServer running = startServer()) {
            HttpClient client = TestSupport.trustAllClient();
            String base = "https://127.0.0.1:" + running.port();
            String cookie = login(client, base);
            setDeveloperPassword(client, base, cookie);
            elevate(client, base, cookie);

            HttpResponse<String> noSurvivor = put(client, base + "/api/settings/brands/policy/octop",
                    "{\"singleInstance\":true,\"survivor\":null,\"lockedRoute\":false,\"sourceBuild\":false}",
                    cookie);
            assertEquals(400, noSurvivor.statusCode(), noSurvivor.body());

            HttpResponse<String> wrongBrand = put(client, base + "/api/settings/brands/policy/kimi",
                    "{\"singleInstance\":false,\"survivor\":null,\"lockedRoute\":true,\"sourceBuild\":false}",
                    cookie);
            assertEquals(400, wrongBrand.statusCode(), wrongBrand.body());
            assertTrue(wrongBrand.body().contains("lockedRoute"), wrongBrand.body());

            // 单实例也只对那两个品牌开放：老页面即使发上来也得拒绝。
            HttpResponse<String> singleOnKimi = put(client, base + "/api/settings/brands/policy/kimi",
                    "{\"singleInstance\":true,\"survivor\":\"x\",\"lockedRoute\":false}", cookie);
            assertEquals(400, singleOnKimi.statusCode(), singleOnKimi.body());
            assertTrue(singleOnKimi.body().contains("singleInstance"), singleOnKimi.body());

            // OpenCode 有最后手段，同一个请求在这里是合法的（实例不存在时由 survivor 校验拒绝）。
            HttpResponse<String> opencode = put(client, base + "/api/settings/brands/policy/opencode",
                    "{\"singleInstance\":false,\"survivor\":null,\"lockedRoute\":false}", cookie);
            assertEquals(200, opencode.statusCode(), opencode.body());

            HttpResponse<String> unknown = put(client, base + "/api/settings/brands/policy/nope",
                    "{}", cookie);
            assertEquals(404, unknown.statusCode(), unknown.body());
        }
    }

    @Test
    void theProbeRefusesAnUnknownKind() throws Exception {
        try (TestSupport.RunningServer running = startServer()) {
            HttpClient client = TestSupport.trustAllClient();
            String base = "https://127.0.0.1:" + running.port();
            String cookie = login(client, base);
            setDeveloperPassword(client, base, cookie);
            elevate(client, base, cookie);

            HttpResponse<String> refused = post(client, base + "/api/settings/brands/test",
                    "{\"kind\":\"garbage\",\"url\":\"https://mirror.example.com/\"}", cookie);
            assertEquals(400, refused.statusCode(), refused.body());
        }
    }
}