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
package org.jackhuang.hmcl.web.brand;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jackhuang.hmcl.util.platform.OperatingSystem;
import org.jackhuang.hmcl.web.TestSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The REST surface of the brand categories over a real (loopback, auth-off)
/// server, with fake tool executables standing in for the installed releases:
/// one lifecycle per brand, covering create → launch → readiness → open →
/// logs → guards → stop → delete.
class BrandApiTest {

    @TempDir
    Path dataDir;

    @Test
    void unknownBrandAnswers404() throws Exception {
        try (TestSupport.RunningServer running = start()) {
            HttpResponse<String> response = get(running, "/api/brands/nope/versions");
            assertEquals(404, response.statusCode(), response.body());
        }
    }

    /// The tab row asks this before opening a brand, so "Hermes cannot be
    /// installed here" is visible without waiting for a version list.
    @Test
    void capabilitiesComeFromTheSameBrandDecision() throws Exception {
        try (TestSupport.RunningServer running = start()) {
            HttpResponse<String> response = get(running, "/api/brands/capabilities");
            assertEquals(200, response.statusCode(), response.body());
            JsonObject body = JsonParser.parseString(response.body()).getAsJsonObject();
            JsonArray brands = body.getAsJsonArray("brands");
            assertEquals(Brand.values().length, brands.size(), response.body());

            for (int i = 0; i < brands.size(); i++) {
                JsonObject entry = brands.get(i).getAsJsonObject();
                Brand brand = Brand.fromId(entry.get("id").getAsString());
                assertTrue(brand != null, entry.toString());
                String reason = brand.notInstallableReason(OperatingSystem.CURRENT_OS);
                assertEquals(reason == null, entry.get("installable").getAsBoolean(), entry.toString());
                if (reason != null) {
                    assertEquals(reason, entry.get("reason").getAsString());
                }
            }
        }
    }

    /// The plugin channel is a reserved slot: it answers the frozen shape with
    /// `supported: false` and no plugins, and still 404s for an instance that
    /// does not exist.
    @Test
    void pluginChannelIsReservedButEmpty() throws Exception {
        try (TestSupport.RunningServer running = start()) {
            // A release counts as installed once its marker file exists — the
            // panel never inspects the tool itself.
            Path release = dataDir.resolve("brands/kimi/releases/2.1.1");
            Files.createDirectories(release.resolve("node_modules/.bin"));
            Files.writeString(release.resolve("node_modules/.bin/kimi"), "#!/bin/sh\n");

            HttpResponse<String> created = post(TestSupport.client(), running,
                    "/api/brands/kimi/instances", "{\"name\":\"Kimi Code\",\"version\":\"2.1.1\"}");
            assertEquals(201, created.statusCode(), created.body());
            String id = JsonParser.parseString(created.body()).getAsJsonObject()
                    .getAsJsonObject("instance").get("id").getAsString();

            HttpResponse<String> response = get(running, "/api/brands/kimi/instances/" + id + "/plugins");
            assertEquals(200, response.statusCode(), response.body());
            JsonObject body = JsonParser.parseString(response.body()).getAsJsonObject();
            assertFalse(body.get("supported").getAsBoolean());
            assertTrue(body.getAsJsonArray("plugins").isEmpty());

            assertEquals(404, get(running, "/api/brands/kimi/instances/nope/plugins").statusCode());
        }
    }

    /// Claude Web ships two Claude Code kernels; the picker asks this endpoint so
    /// it never offers one that would fail. Other categories answer 404: they
    /// have exactly one TUI and nothing to choose.
    @Test
    void claudeWebListsItsTwoKernels() throws Exception {
        try (TestSupport.RunningServer running = start()) {
            Path release = dataDir.resolve("brands/claude-web/releases/0.3.2");
            Files.createDirectories(release.resolve("web/dist"));
            Files.writeString(release.resolve("web/dist/index.html"), "<!doctype html>\n");

            HttpResponse<String> created = post(TestSupport.client(), running,
                    "/api/brands/claude-web/instances", "{\"name\":\"Workbench\",\"version\":\"0.3.2\"}");
            assertEquals(201, created.statusCode(), created.body());
            String id = JsonParser.parseString(created.body()).getAsJsonObject()
                    .getAsJsonObject("instance").get("id").getAsString();

            HttpResponse<String> response = get(running, "/api/brands/claude-web/instances/" + id + "/terminals");
            assertEquals(200, response.statusCode(), response.body());
            JsonObject body = JsonParser.parseString(response.body()).getAsJsonObject();
            assertEquals(2, body.getAsJsonArray("kernels").size(), response.body());
            assertEquals("community", body.getAsJsonArray("kernels").get(0)
                    .getAsJsonObject().get("id").getAsString());
            assertEquals("official", body.getAsJsonArray("kernels").get(1)
                    .getAsJsonObject().get("id").getAsString());

            // A category with one TUI has no kernel list.
            Path kimi = dataDir.resolve("brands/kimi/releases/2.1.1");
            Files.createDirectories(kimi.resolve("node_modules/.bin"));
            Files.writeString(kimi.resolve("node_modules/.bin/kimi"), "#!/bin/sh\n");
            HttpResponse<String> kimiCreated = post(TestSupport.client(), running,
                    "/api/brands/kimi/instances", "{\"name\":\"Kimi\",\"version\":\"2.1.1\"}");
            assertEquals(201, kimiCreated.statusCode(), kimiCreated.body());
            String kimiId = JsonParser.parseString(kimiCreated.body()).getAsJsonObject()
                    .getAsJsonObject("instance").get("id").getAsString();
            assertEquals(404, get(running, "/api/brands/kimi/instances/" + kimiId + "/terminals").statusCode());
        }
    }

    /// Claude Web's endpoint override: accepted for that brand, refused for the
    /// others, cleared by an empty value, and only over http/https.
    @Test
    void claudeWebStoresAnEndpointOverrideOtherBrandsRefuseIt() throws Exception {
        try (TestSupport.RunningServer running = start()) {
            Path release = dataDir.resolve("brands/claude-web/releases/0.3.2");
            Files.createDirectories(release.resolve("web/dist"));
            Files.writeString(release.resolve("web/dist/index.html"), "<!doctype html>\n");

            HttpResponse<String> created = post(TestSupport.client(), running,
                    "/api/brands/claude-web/instances", "{\"name\":\"Workbench\",\"version\":\"0.3.2\"}");
            assertEquals(201, created.statusCode(), created.body());
            String id = JsonParser.parseString(created.body()).getAsJsonObject()
                    .getAsJsonObject("instance").get("id").getAsString();
            String path = "/api/brands/claude-web/instances/" + id;

            HttpResponse<String> set = request(TestSupport.client(), "PATCH", running, path,
                    "{\"baseUrl\":\"https://gateway.example.com\"}");
            assertEquals(200, set.statusCode(), set.body());
            assertEquals("https://gateway.example.com", instance(set).get("baseUrl").getAsString());

            // A bare host is not something a client could talk to.
            HttpResponse<String> bad = request(TestSupport.client(), "PATCH", running, path,
                    "{\"baseUrl\":\"gateway.example.com\"}");
            assertEquals(400, bad.statusCode(), bad.body());

            // Empty clears it, and the field stays visible as null.
            HttpResponse<String> cleared = request(TestSupport.client(), "PATCH", running, path,
                    "{\"baseUrl\":\"\"}");
            assertEquals(200, cleared.statusCode(), cleared.body());
            assertTrue(instance(cleared).get("baseUrl").isJsonNull(), cleared.body());

            // Kimi has no ANTHROPIC_BASE_URL.
            Path kimi = dataDir.resolve("brands/kimi/releases/2.1.1");
            Files.createDirectories(kimi.resolve("node_modules/.bin"));
            Files.writeString(kimi.resolve("node_modules/.bin/kimi"), "#!/bin/sh\n");
            HttpResponse<String> kimiCreated = post(TestSupport.client(), running,
                    "/api/brands/kimi/instances", "{\"name\":\"Kimi\",\"version\":\"2.1.1\"}");
            assertEquals(201, kimiCreated.statusCode(), kimiCreated.body());
            String kimiId = JsonParser.parseString(kimiCreated.body()).getAsJsonObject()
                    .getAsJsonObject("instance").get("id").getAsString();
            HttpResponse<String> refused = request(TestSupport.client(), "PATCH", running,
                    "/api/brands/kimi/instances/" + kimiId, "{\"baseUrl\":\"https://gateway.example.com\"}");
            assertEquals(400, refused.statusCode(), refused.body());
        }
    }

    private static JsonObject instance(HttpResponse<String> response) {
        return JsonParser.parseString(response.body()).getAsJsonObject().getAsJsonObject("instance");
    }

    /// The Octop credential route: Octop keeps only a hash, so the panel can show what it recorded
    /// (its own `octop-admin.txt` or Octop's `credential.txt`) and otherwise says "not readable";
    /// every reset goes through `octop user passwd` and needs the second factor.
    @Test
    void octopAdminPasswordIsReadableFromItsOwnFileAndResetsNeedAConfirmation() throws Exception {
        try (TestSupport.RunningServer running = start()) {
            HttpClient client = TestSupport.client();
            Path release = dataDir.resolve("brands/octop/releases/1.0.3b1");
            Files.createDirectories(release);
            Files.writeString(release.resolve("octop-install.json"),
                    "{\"mode\":\"package\",\"version\":\"1.0.3b1\"}\n");
            HttpResponse<String> created = post(client, running, "/api/brands/octop/instances",
                    "{\"name\":\"A\",\"version\":\"1.0.3b1\"}");
            assertEquals(201, created.statusCode(), created.body());
            String id = JsonParser.parseString(created.body()).getAsJsonObject()
                    .getAsJsonObject("instance").get("id").getAsString();
            String path = "/api/brands/octop/instances/" + id + "/admin-password";

            // Nothing recorded yet: "not readable" is an answer, not an error.
            JsonObject empty = JsonParser.parseString(
                    post(client, running, path, "{}").body()).getAsJsonObject();
            assertEquals(false, empty.has("password") && !empty.get("password").isJsonNull());
            assertEquals("admin", empty.get("username").getAsString());

            // Octop's own credential file is what its docs tell people to cat.
            Path home = dataDir.resolve("brands/octop/instances").resolve(id).resolve("home");
            Files.createDirectories(home);
            Files.writeString(home.resolve("credential.txt"),
                    "Octop Login Credential\n======================\n"
                            + "Username: admin\nPassword: s3cret-pass1\n");
            JsonObject read = JsonParser.parseString(
                    post(client, running, path, "{}").body()).getAsJsonObject();
            assertEquals("s3cret-pass1", read.get("password").getAsString());
            assertEquals(true, read.get("readable").getAsBoolean());

            // A reset is never in the first-run window, and this panel has no HTTPS: the panel
            // password is the only second factor it can ask for.
            HttpResponse<String> refused = post(client, running, path, "{\"password\":\"newpass123\"}");
            assertEquals(401, refused.statusCode(), refused.body());
            assertTrue(refused.body().contains("panel_password_required"), refused.body());

            // Other brands do not have an Octop admin account.
            assertEquals(400, post(client, running,
                    "/api/brands/kimi/instances/" + id + "/admin-password", "{}").statusCode());
        }
    }

    /// Octop's `credential.txt` carries two shapes: a login block, or the "Setup Required" hint with
    /// a wizard password. The panel must tell them apart — "finish the wizard" is not "log in".
    @Test
    void octopSetupHintIsReportedAsAWizardPasswordNotAsAnAdminLogin() throws Exception {
        try (TestSupport.RunningServer running = start()) {
            HttpClient client = TestSupport.client();
            Path release = dataDir.resolve("brands/octop/releases/1.0.3b1");
            Files.createDirectories(release);
            Files.writeString(release.resolve("octop-install.json"),
                    "{\"mode\":\"package\",\"version\":\"1.0.3b1\"}\n");
            HttpResponse<String> created = post(client, running, "/api/brands/octop/instances",
                    "{\"name\":\"B\",\"version\":\"1.0.3b1\"}");
            assertEquals(201, created.statusCode(), created.body());
            String id = JsonParser.parseString(created.body()).getAsJsonObject()
                    .getAsJsonObject("instance").get("id").getAsString();
            Path home = dataDir.resolve("brands/octop/instances").resolve(id).resolve("home");
            Files.createDirectories(home);
            Files.writeString(home.resolve("credential.txt"),
                    "Octop Setup Required\n====================\n"
                            + "No admin account has been created yet.\n"
                            + "Wizard password: wiz-123456\n");

            JsonObject read = JsonParser.parseString(post(client, running,
                    "/api/brands/octop/instances/" + id + "/admin-password", "{}").body())
                    .getAsJsonObject();
            assertEquals("wizard", read.get("mode").getAsString());
            assertEquals("wiz-123456", read.get("password").getAsString());
        }
    }

    private static HttpResponse<String> get(TestSupport.RunningServer running, String path)
            throws Exception {
        return TestSupport.client().send(HttpRequest.newBuilder(
                        URI.create(running.baseUrl() + path)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private TestSupport.RunningServer start() throws Exception {
        return TestSupport.start(dataDir, Map.of(), config -> {
            config.bindHost = "127.0.0.1";
            config.auth.disabled = true;
        });
    }

    private static HttpResponse<String> post(HttpClient client, TestSupport.RunningServer running,
                                             String path, String json) throws Exception {
        return request(client, "POST", running, path, json);
    }

    private static HttpResponse<String> request(HttpClient client, String method,
                                                TestSupport.RunningServer running, String path,
                                                String json) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(running.baseUrl() + path));
        if (json == null) {
            builder.method(method, HttpRequest.BodyPublishers.noBody());
        } else {
            builder.method(method, HttpRequest.BodyPublishers.ofString(json))
                    .header("Content-Type", "application/json");
        }
        return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    /// A version counts only when its install marker is on disk: that is what the create form is
    /// built from, so a finished build that lost its marker (or a half-installed directory left by
    /// a killed JVM) must not be offered, and one that has it must be creatable — including when
    /// the version string arrives with padding.
    @Test
    void aReleaseIsCreatableExactlyWhenItsMarkerIsThere() throws Exception {
        try (TestSupport.RunningServer running = start()) {
            HttpClient client = TestSupport.client();
            Path release = dataDir.resolve("brands/octop/releases/1.0.3b1");
            Files.createDirectories(release);

            // Half-installed: the directory exists, the marker does not.
            JsonObject before = JsonParser.parseString(get(running, "/api/brands/octop/versions").body())
                    .getAsJsonObject();
            assertTrue(before.getAsJsonArray("releases").isEmpty(), before.toString());
            HttpResponse<String> refused = post(client, running, "/api/brands/octop/instances",
                    "{\"name\":\"A\",\"version\":\"1.0.3b1\"}");
            assertEquals(400, refused.statusCode(), refused.body());
            assertTrue(refused.body().contains("is not installed"), refused.body());

            // The marker is the whole difference.
            Files.writeString(release.resolve("octop-install.json"),
                    "{\"mode\":\"package\",\"version\":\"1.0.3b1\"}\n");
            JsonObject after = JsonParser.parseString(get(running, "/api/brands/octop/versions").body())
                    .getAsJsonObject();
            assertEquals(1, after.getAsJsonArray("releases").size(), after.toString());
            assertEquals("1.0.3b1", after.get("default").getAsString());

            HttpResponse<String> created = post(client, running, "/api/brands/octop/instances",
                    "{\"name\":\"A\",\"version\":\" 1.0.3b1 \"}");
            assertEquals(201, created.statusCode(), created.body());
        }
    }

    /// The per-instance Node selector: stored, echoed with its resolution, and
    /// refused when the resolver does not understand it.
    @Test
    void aBrandInstanceNodeSelectorIsStoredAndResolved() throws Exception {
        try (TestSupport.RunningServer running = start()) {
            Path release = dataDir.resolve("brands/claude-web/releases/0.3.2");
            Files.createDirectories(release.resolve("web/dist"));
            Files.writeString(release.resolve("web/dist/index.html"), "<!doctype html>\n");

            HttpResponse<String> created = post(TestSupport.client(), running,
                    "/api/brands/claude-web/instances", "{\"name\":\"Node\",\"version\":\"0.3.2\"}");
            assertEquals(201, created.statusCode(), created.body());
            String id = instance(created).get("id").getAsString();
            String path = "/api/brands/claude-web/instances/" + id;

            HttpResponse<String> set = request(TestSupport.client(), "PATCH", running, path,
                    "{\"nodeRuntime\":\"22\"}");
            assertEquals(200, set.statusCode(), set.body());
            assertEquals("22", instance(set).get("nodeRuntime").getAsString());
            assertTrue(instance(set).has("nodeResolved"), set.body());

            HttpResponse<String> bad = request(TestSupport.client(), "PATCH", running, path,
                    "{\"nodeRuntime\":\"banana\"}");
            assertEquals(400, bad.statusCode(), bad.body());
        }
    }
}
