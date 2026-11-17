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
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpServer;
import org.jackhuang.hmcl.dsh.DshInstance;
import org.jackhuang.hmcl.dsh.DshInstanceManager;
import org.jackhuang.hmcl.dsh.DshPluginInstaller;
import org.jackhuang.hmcl.dsh.FirstPartyPlugins;
import org.jackhuang.hmcl.dsh.DshProcessManager;
import org.jackhuang.hmcl.dsh.DshTuiPlugins;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/// The plugin REST surface over real HTTP with a plugin-capable stub dsh:
/// the catalogue (served, cached, refreshed, failed), install/remove task
/// flows, a local `.tgz` upload, and the build-script approval dance —
/// `waiting_approval` → `POST /api/tasks/{id}/approve` → done, on both answers.
class PluginApiTest {

    private static final String VERSION = "0.1.7-rc.1";

    private final List<String> created = new ArrayList<>();
    private final List<HttpServer> stubs = new ArrayList<>();

    @TempDir
    Path dataDir;

    @AfterEach
    void tearDown() throws Exception {
        System.clearProperty("hdsl.pluginCatalog");
        for (HttpServer stub : stubs) {
            stub.stop(0);
        }
        stubs.clear();
        for (String id : created) {
            DshProcessManager.stop(id);
            if (DshInstanceManager.exists(id)) {
                DshInstanceManager.delete(id);
            }
        }
        created.clear();
    }

    private TestSupport.RunningServer startServer() throws Exception {
        return TestSupport.start(dataDir, Map.of(), config -> {
            config.bindHost = "127.0.0.1";
            config.auth.disabled = true;
        });
    }

    private static boolean pnpmAvailable() {
        try {
            Process process = new ProcessBuilder("pnpm", "--version").redirectErrorStream(true).start();
            return process.waitFor(30, TimeUnit.SECONDS) && process.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    private static void assumePluginTooling() {
        NodeDshStub.assumeNode();
        assumeTrue(pnpmAvailable(), "pnpm is required on PATH for plugin management");
    }

    // ---------------------------------------------------------------- catalog --

    private HttpServer catalogStub(String body) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/plugins.json", exchange -> {
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("content-type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
        stubs.add(server);
        return server;
    }

    private static final String CATALOG_BODY = """
            {"updated":"2026-01-01","plugins":[
              {"name":"alpha","owner":"someone","url":"https://github.com/someone/alpha",
               "category":"tools","description":{"en":"A plugin","zh":"一个插件"},
               "npm":"@someone/alpha","version":"1.2.3","stars":42,"downloads":1000,
               "added":"2025-01-01"}
            ]}
            """;

    @Test
    void catalogIsServedCachedAndRefreshed() throws Exception {
        HttpServer stub = catalogStub(CATALOG_BODY);
        System.setProperty("hdsl.pluginCatalog",
                "http://127.0.0.1:" + stub.getAddress().getPort() + "/plugins.json");
        try (TestSupport.RunningServer running = startServer()) {
            HttpClient client = TestSupport.client();
            String base = running.baseUrl();

            HttpResponse<String> first = get(client, base + "/api/plugins/catalog");
            assertEquals(200, first.statusCode(), first.body());
            JsonArray plugins = json(first).getAsJsonArray("plugins");
            assertEquals(1, plugins.size());
            JsonObject alpha = plugins.get(0).getAsJsonObject();
            assertEquals("alpha", alpha.get("name").getAsString());
            assertEquals("someone", alpha.get("owner").getAsString());
            assertEquals("https://github.com/someone/alpha", alpha.get("url").getAsString());
            assertEquals("tools", alpha.get("category").getAsString());
            assertEquals("A plugin", alpha.get("description").getAsString());
            assertEquals("一个插件", alpha.get("descriptionZh").getAsString());
            assertEquals("@someone/alpha", alpha.get("npm").getAsString());
            assertEquals("1.2.3", alpha.get("version").getAsString());
            assertEquals(42, alpha.get("stars").getAsInt());
            assertEquals(1000, alpha.get("downloads").getAsInt());
            assertTrue(alpha.get("tarball").isJsonNull());
            assertEquals("2025-01-01", alpha.get("added").getAsString());

            // The upstream now speaks garbage; the ten-minute cache hides that.
            stub.stop(0);
            stubs.remove(stub);
            HttpServer garbage = catalogStub("this is not a catalogue");
            System.setProperty("hdsl.pluginCatalog",
                    "http://127.0.0.1:" + garbage.getAddress().getPort() + "/plugins.json");
            HttpResponse<String> cached = get(client, base + "/api/plugins/catalog");
            assertEquals(200, cached.statusCode(), "the in-memory cache must answer inside its TTL");
            assertEquals("alpha", json(cached).getAsJsonArray("plugins").get(0)
                    .getAsJsonObject().get("name").getAsString());

            // A forced refresh really asks upstream, and its failure is a 502.
            HttpResponse<String> refreshed = get(client, base + "/api/plugins/catalog?refresh=1");
            assertEquals(502, refreshed.statusCode(), refreshed.body());
            assertNotNull(json(refreshed).get("error"));
        }
    }

    // ------------------------------------------------- install / remove / list --

    private String createInstance(HttpClient client, String base, String name) throws Exception {
        HttpResponse<String> response = post(client, base + "/api/instances",
                "{\"name\":\"" + name + "\",\"version\":\"" + VERSION + "\",\"autoInstall\":false}");
        assertEquals(201, response.statusCode(), response.body());
        String id = json(response).getAsJsonObject("instance").get("id").getAsString();
        created.add(id);
        return id;
    }

    @Test
    void pluginInstallRemoveAndListLifecycle() throws Exception {
        assumePluginTooling();
        try (TestSupport.RunningServer running = startServer()) {
            HttpClient client = TestSupport.client();
            String base = running.baseUrl();
            String id = createInstance(client, base, "plugins");

            // Not installed yet: installs are a conflict.
            HttpResponse<String> refused = post(client, base + "/api/instances/" + id + "/plugins/install",
                    "{\"specs\":[\"@acme/tool\"]}");
            assertEquals(409, refused.statusCode());

            NodeDshStub.installPluginCapable(DshInstanceManager.find(id), false);

            // An empty specs array is a 400.
            assertEquals(400, post(client, base + "/api/instances/" + id + "/plugins/install",
                    "{\"specs\":[]}").statusCode());

            HttpResponse<String> install = post(client, base + "/api/instances/" + id + "/plugins/install",
                    "{\"specs\":[\"@acme/tool@1.2.3\",\"plain\"]}");
            assertEquals(202, install.statusCode(), install.body());
            String taskId = json(install).get("taskId").getAsString();
            JsonObject task = awaitTask(client, base, taskId);
            assertEquals("done", task.get("state").getAsString(),
                    "install task must finish: " + task);

            HttpResponse<String> listed = get(client, base + "/api/instances/" + id + "/plugins");
            assertEquals(200, listed.statusCode());
            JsonArray plugins = json(listed).getAsJsonArray("plugins");
            assertEquals(2, plugins.size());
            JsonObject pinned = findPlugin(plugins, "@acme/tool");
            assertNotNull(pinned, "the pinned spec must be a dependency");
            assertEquals("1.2.3", pinned.get("version").getAsString());
            JsonObject plain = findPlugin(plugins, "plain");
            assertNotNull(plain);
            assertEquals("*", plain.get("version").getAsString());
            assertTrue(json(listed).has("bundles"));

            HttpResponse<String> remove = post(client, base + "/api/instances/" + id + "/plugins/remove",
                    "{\"specs\":[\"@acme/tool\"]}");
            assertEquals(202, remove.statusCode());
            JsonObject removed = awaitTask(client, base, json(remove).get("taskId").getAsString());
            assertEquals("done", removed.get("state").getAsString(), "remove task must finish");

            JsonArray after = json(get(client, base + "/api/instances/" + id + "/plugins"))
                    .getAsJsonArray("plugins");
            assertEquals(1, after.size());
            assertEquals("plain", after.get(0).getAsJsonObject().get("name").getAsString());
        }
    }

    private static JsonObject findPlugin(JsonArray plugins, String name) {
        for (int i = 0; i < plugins.size(); i++) {
            JsonObject plugin = plugins.get(i).getAsJsonObject();
            if (name.equals(plugin.get("name").getAsString())) {
                return plugin;
            }
        }
        return null;
    }

    // ------------------------------------------------------------- local upload --

    @Test
    void localPluginUploadInstallsFromTheInstancesOwnCopy() throws Exception {
        assumePluginTooling();
        try (TestSupport.RunningServer running = startServer()) {
            HttpClient client = TestSupport.client();
            String base = running.baseUrl();
            String id = createInstance(client, base, "local-plugins");
            NodeDshStub.installPluginCapable(DshInstanceManager.find(id), false);

            byte[] tgz = pack("dsh-example", "1.2.3", "./cordis.patch.yml");
            HttpResponse<String> upload = multipart(client, base + "/api/instances/" + id + "/plugins/local",
                    "file", "whatever-the-user-called-it.tgz", tgz);
            assertEquals(202, upload.statusCode(), upload.body());
            JsonObject task = awaitTask(client, base, json(upload).get("taskId").getAsString());
            assertEquals("done", task.get("state").getAsString(), "upload install must finish: " + task);

            // The profile records the instance's own copy as a file: dependency.
            JsonArray plugins = json(get(client, base + "/api/instances/" + id + "/plugins"))
                    .getAsJsonArray("plugins");
            assertEquals(1, plugins.size());
            JsonObject installed = plugins.get(0).getAsJsonObject();
            assertTrue(installed.get("version").getAsString().startsWith("file:"),
                    "a local plugin is recorded as a file: dependency: " + installed);
            assertTrue(installed.get("version").getAsString().contains("dsh-example"),
                    "the instance's own copy is named after the package: " + installed);

            // The copy really is inside the instance.
            assertTrue(Files.isRegularFile(DshInstanceManager.find(id).instanceDirectory()
                    .resolve("plugins").resolve("dsh-example-1.2.3.tgz")));

            // A non-archive upload is a 400, not a failed task.
            HttpResponse<String> bad = multipart(client, base + "/api/instances/" + id + "/plugins/local",
                    "file", "notes.zip", "not a plugin".getBytes(StandardCharsets.UTF_8));
            assertEquals(400, bad.statusCode());
        }
    }

    // ---------------------------------------------------------- approval dance --

    @Test
    void buildScriptApprovalParksTheTaskUntilAllowed() throws Exception {
        assumePluginTooling();
        try (TestSupport.RunningServer running = startServer()) {
            HttpClient client = TestSupport.client();
            String base = running.baseUrl();
            String id = createInstance(client, base, "approval");
            NodeDshStub.installPluginCapable(DshInstanceManager.find(id), true);

            HttpResponse<String> install = post(client, base + "/api/instances/" + id + "/plugins/install",
                    "{\"specs\":[\"node-pty\"]}");
            assertEquals(202, install.statusCode());
            String taskId = json(install).get("taskId").getAsString();

            // The task parks with the pending keys spelled out.
            JsonObject parked = awaitTaskState(client, base, taskId, "waiting_approval");
            JsonObject approval = parked.getAsJsonObject("approval");
            assertNotNull(approval, "a parked task must carry its approval: " + parked);
            assertEquals("build-scripts", approval.get("kind").getAsString());
            JsonArray keys = approval.getAsJsonArray("keys");
            assertEquals(1, keys.size());
            assertEquals("node-pty", keys.get(0).getAsString());

            // Approve endpoints validate their input.
            assertEquals(404, post(client, base + "/api/tasks/no-such-task/approve",
                    "{\"allow\":true}").statusCode());
            assertEquals(400, post(client, base + "/api/tasks/" + taskId + "/approve", "{}").statusCode());

            HttpResponse<String> approved = post(client, base + "/api/tasks/" + taskId + "/approve",
                    "{\"allow\":true}");
            assertEquals(200, approved.statusCode(), approved.body());
            JsonObject done = awaitTask(client, base, taskId);
            assertEquals("done", done.get("state").getAsString(), "the retry must finish: " + done);

            // The answer was written into the profile's workspace file.
            Path workspace = DshInstanceManager.find(id).homeDirectory()
                    .resolve("profiles").resolve("web").resolve("pnpm-workspace.yaml");
            assertTrue(Files.readString(workspace).contains("node-pty: true"));

            // Approving a finished task is a conflict.
            assertEquals(409, post(client, base + "/api/tasks/" + taskId + "/approve",
                    "{\"allow\":true}").statusCode());

            // And the plugin is really installed.
            JsonArray plugins = json(get(client, base + "/api/instances/" + id + "/plugins"))
                    .getAsJsonArray("plugins");
            assertNotNull(findPlugin(plugins, "node-pty"));
            assertFalse(findPlugin(plugins, "node-pty").has("pending")
                            && findPlugin(plugins, "node-pty").get("pending").getAsBoolean(),
                    "an answered script is no longer pending");
        }
    }

    @Test
    void buildScriptDenialCompletesTheInstallWithoutRunningScripts() throws Exception {
        assumePluginTooling();
        try (TestSupport.RunningServer running = startServer()) {
            HttpClient client = TestSupport.client();
            String base = running.baseUrl();
            String id = createInstance(client, base, "denial");
            NodeDshStub.installPluginCapable(DshInstanceManager.find(id), true);

            HttpResponse<String> install = post(client, base + "/api/instances/" + id + "/plugins/install",
                    "{\"specs\":[\"node-pty\"]}");
            String taskId = json(install).get("taskId").getAsString();
            awaitTaskState(client, base, taskId, "waiting_approval");

            HttpResponse<String> denied = post(client, base + "/api/tasks/" + taskId + "/approve",
                    "{\"allow\":false}");
            assertEquals(200, denied.statusCode(), denied.body());
            JsonObject done = awaitTask(client, base, taskId);
            assertEquals("done", done.get("state").getAsString(),
                    "NEVER semantics install the package without running its scripts: " + done);

            Path workspace = DshInstanceManager.find(id).homeDirectory()
                    .resolve("profiles").resolve("web").resolve("pnpm-workspace.yaml");
            assertTrue(Files.readString(workspace).contains("node-pty: false"));
        }
    }

    // -------------------------------------------------------- terminal plugins --

    /// The curated terminal-interface list answers without a network — it is a
    /// constant, because it has to explain itself offline — and every row carries
    /// what the page must show next to the button: the pinned version, the licence,
    /// the repository, the **profile it must go into**, and whether it has an
    /// executable of its own.
    @Test
    void terminalPluginsAreListedWithTheirPinnedVersionAndProfile() throws Exception {
        try (TestSupport.RunningServer running = startServer()) {
            HttpClient client = TestSupport.client();
            String base = running.baseUrl();

            HttpResponse<String> response = get(client, base + "/api/dsh/tui-plugins");
            assertEquals(200, response.statusCode(), response.body());
            JsonArray plugins = json(response).getAsJsonArray("plugins");
            assertEquals(DshTuiPlugins.ENTRIES.size(), plugins.size(), "the list is the curated one");
            for (int i = 0; i < plugins.size(); i++) {
                JsonObject entry = plugins.get(i).getAsJsonObject();
                DshTuiPlugins.Entry known = DshTuiPlugins.ENTRIES.get(i);
                assertEquals(known.packageName(), entry.get("packageName").getAsString());
                assertEquals(known.spec(), entry.get("spec").getAsString(),
                        "the panel installs a pinned version, and says which");
                assertEquals(known.version(), entry.get("version").getAsString());
                assertEquals(known.license(), entry.get("license").getAsString());
                assertTrue(entry.get("repository").getAsString().startsWith("https://github.com/"),
                        "the page links to where the plugin comes from: " + entry);
                assertFalse(entry.get("description").getAsString().isBlank());
                assertEquals(known.profile(), entry.get("profile").getAsString(),
                        "the profile is where the package goes");
                // The rule inverts by kind: a terminal is an app plugin and must stay out
                // of the instance's own profile; a capability lives there on purpose.
                if (known.terminal()) {
                    assertNotEquals("web", entry.get("profile").getAsString(),
                            "a terminal plugin never shares the web profile with the web app");
                    assertEquals("terminal", entry.get("kind").getAsString());
                } else {
                    assertEquals("web", entry.get("profile").getAsString(),
                            "a capability plugin belongs in the instance's own profile");
                    assertEquals("capability", entry.get("kind").getAsString());
                }
            }

            JsonObject recommended = plugins.get(0).getAsJsonObject();
            assertEquals("@nas200-timmy/dsh-inproc-tui", recommended.get("packageName").getAsString(),
                    "the panel's fuller in-process terminal comes first");
            assertTrue(recommended.get("bundled").getAsBoolean(),
                    "and the page can say it ships with the panel");
            assertTrue(recommended.get("bin").isJsonNull(), "a mode bundle ships no program");
            assertEquals("dsh-inproc-tui", recommended.get("profile").getAsString());

            // The ACP terminal is the second first-party entry, and it does ship one.
            JsonObject acp = plugins.get(1).getAsJsonObject();
            assertEquals("@nas200-timmy/dsh-acp-tui", acp.get("packageName").getAsString());
            assertEquals("dsh-acp-tui", acp.get("bin").getAsString());
            assertTrue(acp.get("bundled").getAsBoolean());

            // The community entry the panel used to recommend is still offered, not gone.
            JsonObject community = plugins.get(2).getAsJsonObject();
            assertEquals("@deepseek-harness-tui/dsh-tui", community.get("packageName").getAsString());
            assertFalse(community.get("bundled").getAsBoolean());
            // The one that needs Bun to run is no longer offered.
            for (int i = 0; i < plugins.size(); i++) {
                assertNotEquals("@ruhooai/dsh-tui", plugins.get(i).getAsJsonObject().get("packageName").getAsString());
            }

            // The prefix is mapped, not the single path: anything else under it is a 404.
            assertEquals(404, get(client, base + "/api/dsh/unknown").statusCode());
        }
    }

    /// The per-instance half, end to end: a terminal plugin really is installed into the
/// profile the catalogue names — not the instance's web profile — its state is read back
/// from that profile, and the ordinary install route refuses the combination that would
/// stop the instance from starting.
    @Test
    void terminalPluginsInstallIntoTheirOwnProfileAndNeverIntoWeb() throws Exception {
        assumePluginTooling();
        try (TestSupport.RunningServer running = startServer()) {
            HttpClient client = TestSupport.client();
            String base = running.baseUrl();
            String id = createInstance(client, base, "tui");
            DshInstance instance = DshInstanceManager.find(id);

            HttpResponse<String> state = get(client, base + "/api/instances/" + id + "/tui-plugins");
            assertEquals(200, state.statusCode(), state.body());
            JsonArray rows = json(state).getAsJsonArray("plugins");
            assertEquals(DshTuiPlugins.ENTRIES.size(), rows.size(),
                    "one row per candidate: what is not installed is what the page offers");
            for (int i = 0; i < rows.size(); i++) {
                JsonObject row = rows.get(i).getAsJsonObject();
                assertEquals(DshTuiPlugins.ENTRIES.get(i).packageName(), row.get("packageName").getAsString());
                assertEquals(DshTuiPlugins.ENTRIES.get(i).profile(), row.get("profile").getAsString());
                assertFalse(row.get("installed").getAsBoolean(), "a fresh instance has none: " + row);
            }

            // A package that is not one of these is refused, and the refusal names the ones
            // that are — the route cannot be talked into an arbitrary specification.
            HttpResponse<String> unknown = post(client, base + "/api/instances/" + id + "/tui-plugins/install",
                    "{\"package\":\"@someone/else\"}");
            assertEquals(400, unknown.statusCode(), unknown.body());
            assertTrue(json(unknown).get("error").getAsString().contains("@xmoon76/dsh-pi-tui"),
                    json(unknown).toString());

            NodeDshStub.installPluginCapable(instance, false);

            // Installing goes through `dsh plugin --profile dsh-tui add <pinned spec>`.
            HttpResponse<String> install = post(client, base + "/api/instances/" + id + "/tui-plugins/install",
                    "{\"package\":\"@deepseek-harness-tui/dsh-tui\"}");
            assertEquals(202, install.statusCode(), install.body());
            JsonObject installed = awaitTask(client, base, json(install).get("taskId").getAsString());
            assertEquals("done", installed.get("state").getAsString(), installed.toString());

            // It landed in its own profile, and the version that landed is the pinned one.
            assertTrue(Files.isRegularFile(instance.homeDirectory()
                            .resolve("profiles/dsh-tui/package.json")),
                    "the plugin's own profile is what was written");
            JsonObject row = null;
            JsonArray afterInstall = json(get(client, base + "/api/instances/" + id + "/tui-plugins"))
                    .getAsJsonArray("plugins");
            for (int i = 0; i < afterInstall.size(); i++) {
                JsonObject candidate = afterInstall.get(i).getAsJsonObject();
                if ("@deepseek-harness-tui/dsh-tui".equals(candidate.get("packageName").getAsString())) {
                    row = candidate;
                }
            }
            assertNotNull(row, "the installed one is a row: " + afterInstall);
            assertTrue(row.get("installed").getAsBoolean(), row.toString());
            assertEquals("0.17.0", row.get("installedVersion").getAsString(),
                    "the catalogue's pinned version is what was installed");

            // And the instance's own profile — the one the web app boots — is untouched,
            // which is the whole point: two app plugins in one profile do not start.
            assertEquals(0, json(get(client, base + "/api/instances/" + id + "/plugins"))
                    .getAsJsonArray("plugins").size(), "a terminal plugin is not a web-profile plugin");

            // The ordinary route targets the instance's profile, so it must refuse the
            // combination instead of bricking the instance.
            HttpResponse<String> intoWeb = post(client, base + "/api/instances/" + id + "/plugins/install",
                    "{\"specs\":[\"@xmoon76/dsh-pi-tui@0.5.3\"]}");
            assertEquals(202, intoWeb.statusCode(), intoWeb.body());
            JsonObject refused = awaitTask(client, base, json(intoWeb).get("taskId").getAsString());
            assertEquals("failed", refused.get("state").getAsString(), refused.toString());
            assertTrue(refused.get("error").getAsString().contains("不能装进 web profile"), refused.toString());
            assertFalse(Files.exists(instance.homeDirectory().resolve("profiles/web/package.json")),
                    "the refusal happens before any command runs, so the web profile was not touched");

            // Declared is not landed. Deleting the package behind the profile's back is the
            // shape a fetch that failed half-way leaves; the list has to say so, because the
            // page's only other signal is `installed`, which is what the manifest claims.
            Path packageDirectory = instance.homeDirectory().resolve("profiles/dsh-tui/node_modules/@deepseek-harness-tui/dsh-tui");
            deleteRecursively(packageDirectory);
            JsonObject half = null;
            JsonArray halfInstalled = json(get(client, base + "/api/instances/" + id + "/tui-plugins"))
                    .getAsJsonArray("plugins");
            for (int i = 0; i < halfInstalled.size(); i++) {
                JsonObject candidate = halfInstalled.get(i).getAsJsonObject();
                if ("@deepseek-harness-tui/dsh-tui".equals(candidate.get("packageName").getAsString())) {
                    half = candidate;
                }
            }
            assertNotNull(half, halfInstalled.toString());
            assertTrue(half.get("installed").getAsBoolean(), "the manifest still declares it: " + half);
            assertFalse(half.get("filesPresent").getAsBoolean(),
                    "the package is gone, so the terminal would die at once: " + half);
            assertFalse(half.get("active").getAsBoolean(), "nothing startable claims to be active: " + half);

            // Reinstalling is what the page offers for that state; it has to work without a
            // remove first, and it has to bring the files back.
            HttpResponse<String> reinstall = post(client, base + "/api/instances/" + id + "/tui-plugins/install",
                    "{\"package\":\"@deepseek-harness-tui/dsh-tui\"}");
            assertEquals(202, reinstall.statusCode(), reinstall.body());
            JsonObject reinstalled = awaitTask(client, base, json(reinstall).get("taskId").getAsString());
            assertEquals("done", reinstalled.get("state").getAsString(), reinstalled.toString());
            JsonArray afterReinstall = json(get(client, base + "/api/instances/" + id + "/tui-plugins"))
                    .getAsJsonArray("plugins");
            for (int i = 0; i < afterReinstall.size(); i++) {
                JsonObject candidate = afterReinstall.get(i).getAsJsonObject();
                if ("@deepseek-harness-tui/dsh-tui".equals(candidate.get("packageName").getAsString())) {
                    assertTrue(candidate.get("filesPresent").getAsBoolean(),
                            "the files are back: " + candidate);
                }
            }

            // And the same question one level down: a directory that is still there but has
            // lost its manifest. (For this package the missing entry file would already fail
            // the check; `DshTuiTest.anEmptyPackageDirectoryIsNotAnInstalledBundle` pins the
            // pure-bundle case, where the manifest is the only thing there is to check.)
            Path bundleDirectory = instance.homeDirectory()
                    .resolve("profiles/dsh-tui/node_modules/@deepseek-harness-tui/dsh-tui");
            Files.delete(bundleDirectory.resolve("package.json"));
            JsonArray afterManifestGone = json(get(client, base + "/api/instances/" + id + "/tui-plugins"))
                    .getAsJsonArray("plugins");
            for (int i = 0; i < afterManifestGone.size(); i++) {
                JsonObject candidate = afterManifestGone.get(i).getAsJsonObject();
                if ("@deepseek-harness-tui/dsh-tui".equals(candidate.get("packageName").getAsString())) {
                    assertTrue(candidate.get("installed").getAsBoolean(), candidate.toString());
                    assertFalse(candidate.get("filesPresent").getAsBoolean(),
                            "a manifest-less directory holds nothing to boot: " + candidate);
                }
            }

            // Reinstalling puts the manifest back, which is what the page offers next to the
            // state above.
            HttpResponse<String> restore = post(client, base + "/api/instances/" + id + "/tui-plugins/install",
                    "{\"package\":\"@deepseek-harness-tui/dsh-tui\"}");
            assertEquals(202, restore.statusCode(), restore.body());
            JsonObject restored = awaitTask(client, base, json(restore).get("taskId").getAsString());
            assertEquals("done", restored.get("state").getAsString(), restored.toString());

            // Taking it out again is never refused, and the state goes back.
            HttpResponse<String> remove = post(client, base + "/api/instances/" + id + "/tui-plugins/remove",
                    "{\"package\":\"@deepseek-harness-tui/dsh-tui\"}");
            assertEquals(202, remove.statusCode(), remove.body());
            JsonObject removed = awaitTask(client, base, json(remove).get("taskId").getAsString());
            assertEquals("done", removed.get("state").getAsString(), removed.toString());
            JsonArray afterRemove = json(get(client, base + "/api/instances/" + id + "/tui-plugins"))
                    .getAsJsonArray("plugins");
            for (int i = 0; i < afterRemove.size(); i++) {
                assertFalse(afterRemove.get(i).getAsJsonObject().get("installed").getAsBoolean(),
                        "the removal is what the state now says: " + afterRemove);
            }
        }
    }

    /// A first-party plugin is identified by the tarball that landed, not by its
    /// version: a source change under the same `0.2.0` used to look "installed" and
    /// the panel never offered the newer files. The list now answers "is this the
    /// copy this build ships", from the digest the install recorded.
    @Test
    void aBundledPluginSaysWhenTheInstalledCopyIsNotThisBuilds() throws Exception {
        assumePluginTooling();
        try (TestSupport.RunningServer running = startServer()) {
            HttpClient client = TestSupport.client();
            String base = running.baseUrl();
            String id = createInstance(client, base, "outdated");
            DshInstance instance = DshInstanceManager.find(id);
            DshTuiPlugins.Entry entry = DshTuiPlugins.find("@nas200-timmy/dsh-acp-tui").orElseThrow();
            String name = entry.packageName();
            String digest = FirstPartyPlugins.find(name)
                    .map(FirstPartyPlugins.Bundled::sha256)
                    .orElseThrow();

            // The state an older panel leaves behind: the package is installed and on
            // disk, but nothing recorded which tarball it came from.
            Path profile = instance.homeDirectory().resolve("profiles").resolve(entry.profile());
            Files.createDirectories(profile.resolve("node_modules").resolve(name).resolve("lib"));
            Files.writeString(profile.resolve("package.json"),
                    "{\"dependencies\":{\"" + name + "\":\"file:/tmp/x.tgz\"}}");
            Files.writeString(profile.resolve("node_modules").resolve(name).resolve("lib/main.js"), "//\n");
            Files.writeString(profile.resolve("node_modules").resolve(name).resolve("package.json"),
                    "{\"name\":\"" + name + "\",\"version\":\"0.2.0\",\"bin\":{\"dsh-acp-tui\":\"lib/main.js\"}}");

            JsonObject unknown = tuiRow(base, client, id);
            assertEquals(true, unknown.get("installed").getAsBoolean(), unknown.toString());
            assertEquals(true, unknown.get("filesPresent").getAsBoolean(), unknown.toString());
            assertEquals(true, unknown.get("outdated").getAsBoolean(),
                    "no record means reinstall once, not 'up to date': " + unknown);

            // What a finished install writes.
            FirstPartyPlugins.recordInstalled(instance.homeDirectory(), name, digest);
            assertEquals(false, tuiRow(base, client, id).get("outdated").getAsBoolean());

            // A tarball from a different build: same version string, different files.
            FirstPartyPlugins.recordInstalled(instance.homeDirectory(), name, "deadbeef");
            JsonObject older = tuiRow(base, client, id);
            assertEquals(true, older.get("outdated").getAsBoolean(), older.toString());
            assertEquals("0.2.0", older.get("installedVersion").getAsString(),
                    "the version alone would have said nothing: " + older);
        }
    }

    /// The ACP terminal's row, by package name.
    private JsonObject tuiRow(String base, HttpClient client, String id) throws Exception {
        JsonArray rows = json(get(client, base + "/api/instances/" + id + "/tui-plugins"))
                .getAsJsonArray("plugins");
        for (int index = 0; index < rows.size(); index += 1) {
            JsonObject row = rows.get(index).getAsJsonObject();
            if ("@nas200-timmy/dsh-acp-tui".equals(row.get("packageName").getAsString())) {
                return row;
            }
        }
        throw new AssertionError("no row for @nas200-timmy/dsh-acp-tui: " + rows);
    }

    // An install that reports success without leaving the package behind is not an
    /// install, and it has to fail **here** rather than in the terminal: the dependency is
    /// written into the manifest before pnpm fetches anything, so a fetch that never
    /// happened leaves a profile the panel would call installed — and booting it is what
    /// dies with an empty screen. This is the field failure the check exists for.
    @Test
    void anInstallThatLeavesNoFilesBehindFailsTheTask() throws Exception {
        assumePluginTooling();
        try (TestSupport.RunningServer running = startServer()) {
            HttpClient client = TestSupport.client();
            String base = running.baseUrl();
            String id = createInstance(client, base, "nofiles");

            NodeDshStub.installPluginCapableLayingNothing(DshInstanceManager.find(id));

            HttpResponse<String> install = post(client, base + "/api/instances/" + id + "/tui-plugins/install",
                    "{\"package\":\"@deepseek-harness-tui/dsh-tui\"}");
            assertEquals(202, install.statusCode(), install.body());
            JsonObject task = awaitTask(client, base, json(install).get("taskId").getAsString());

            assertEquals("failed", task.get("state").getAsString(),
                    "a package that never landed is not installed: " + task);
            assertTrue(task.get("error").getAsString().contains("node_modules"), task.toString());
            assertFalse(DshPluginInstaller.holdsPackage(
                            DshInstanceManager.find(id).homeDirectory(), "dsh-tui", "@deepseek-harness-tui/dsh-tui"),
                    "and nothing is on disk to call installed");
        }
    }

    /// A terminal plugin written for the 0.2.0 harness cannot give an older instance a tool
    /// surface: its whole tool face comes from the agent-preset layer, which 0.1.x does not
    /// have. Installing it there produces a window whose agent has no tools at all — the
    /// field symptom is the plugin's own `agent-presets 未装配` warning — so the route
    /// refuses the combination instead of submitting a task for it.
    @Test
    void aTuiPluginForANewerHarnessIsRefusedOnAnOlderInstance() throws Exception {
        assumePluginTooling();
        try (TestSupport.RunningServer running = startServer()) {
            HttpClient client = TestSupport.client();
            String base = running.baseUrl();
            String id = createInstance(client, base, "old-host");
            // The instances here run 0.1.7-rc.1; @tomowang/dsh-tui asks for >=0.2.1-rc.3.
            NodeDshStub.installPluginCapable(DshInstanceManager.find(id), false);

            HttpResponse<String> refused = post(client,
                    base + "/api/instances/" + id + "/tui-plugins/install",
                    "{\"package\":\"@tomowang/dsh-tui\"}");
            assertEquals(409, refused.statusCode(), refused.body());
            String error = json(refused).get("error").getAsString();
            assertTrue(error.contains("@tomowang/dsh-tui"), error);
            assertTrue(error.contains(">=0.2.1-rc.3"), error);
            assertTrue(error.contains("agent-preset"), "the reason travels with the refusal: " + error);
            assertTrue(error.contains("@deepseek-harness-tui/dsh-tui"),
                    "and it names the entry that does work here: " + error);

            // The row is still listed — greyed out is not the same as hidden — and it says
            // why it cannot be installed.
            JsonArray rows = json(get(client, base + "/api/instances/" + id + "/tui-plugins"))
                    .getAsJsonArray("plugins");
            JsonObject row = null;
            for (int i = 0; i < rows.size(); i++) {
                JsonObject candidate = rows.get(i).getAsJsonObject();
                if ("@tomowang/dsh-tui".equals(candidate.get("packageName").getAsString())) {
                    row = candidate;
                }
            }
            assertNotNull(row, rows.toString());
            assertFalse(row.get("fitsHost").getAsBoolean(), row.toString());
            assertEquals(">=0.2.1-rc.3", row.get("requiresDsh").getAsString());
        }
    }

    // ----------------------------------------------------------------- helpers --

    private static JsonObject awaitTask(HttpClient client, String base, String taskId) throws Exception {
        JsonObject task = awaitTaskState(client, base, taskId, null);
        assertNotNull(task);
        return task;
    }

    /// Polls the task until it leaves `running` (any terminal or waiting
    /// state), or until it reaches `wanted` when one is named.
    private static JsonObject awaitTaskState(HttpClient client, String base, String taskId,
                                             String wanted) throws Exception {
        long deadline = System.nanoTime() + Duration.ofSeconds(60).toNanos();
        JsonObject last = null;
        while (System.nanoTime() < deadline) {
            HttpResponse<String> poll = client.send(
                    HttpRequest.newBuilder(URI.create(base + "/api/tasks/" + taskId)).GET().build(),
                    HttpResponse.BodyHandlers.ofString());
            assertEquals(200, poll.statusCode());
            last = json(poll);
            String state = last.get("state").getAsString();
            if (wanted != null) {
                if (wanted.equals(state)) {
                    return last;
                }
            } else if (!"running".equals(state) && !"waiting_approval".equals(state)) {
                return last;
            }
            Thread.sleep(100);
        }
        throw new AssertionError("task " + taskId + " never reached the wanted state; last: " + last);
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

    private static HttpResponse<String> multipart(HttpClient client, String url, String field,
                                                  String fileName, byte[] content) throws Exception {
        String boundary = "----hdsl-test-boundary";
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        body.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"" + field
                + "\"; filename=\"" + fileName + "\"\r\n"
                + "Content-Type: application/gzip\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        body.write(content);
        body.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
        return client.send(HttpRequest.newBuilder(URI.create(url))
                        .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                        .POST(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray())).build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private static JsonObject json(HttpResponse<String> response) {
        return JsonParser.parseString(response.body()).getAsJsonObject();
    }

    /// Builds a gzipped tar holding a `package/package.json`, the shape
    /// [org.jackhuang.hmcl.dsh.DshLocalPlugins] reads.
    private static byte[] pack(String name, String version, String patch) throws Exception {
        String manifest = """
                {"name":"%s","version":"%s"%s}
                """.formatted(name, version,
                patch == null ? "" : ",\"dsh\":{\"bundle\":{\"patch\":\"" + patch + "\"}}");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(out)) {
            gzip.write(entry("package/package.json", manifest.getBytes(StandardCharsets.UTF_8)));
            gzip.write(new byte[1024]);
        }
        return out.toByteArray();
    }

    private static byte[] entry(String name, byte[] body) {
        byte[] block = new byte[512];
        byte[] nameBytes = name.getBytes(StandardCharsets.UTF_8);
        System.arraycopy(nameBytes, 0, block, 0, Math.min(nameBytes.length, 100));
        byte[] size = Long.toOctalString(body.length).getBytes(StandardCharsets.US_ASCII);
        System.arraycopy(size, 0, block, 124, size.length);
        block[156] = '0';
        for (int i = 148; i < 156; i++) {
            block[i] = ' ';
        }
        int padding = (512 - (body.length % 512)) % 512;
        byte[] result = new byte[512 + body.length + padding];
        System.arraycopy(block, 0, result, 0, 512);
        System.arraycopy(body, 0, result, 512, body.length);
        return result;
    }

    /// Removes a directory tree, ignoring what is already gone.
    ///
    /// @param root the directory to remove
    private static void deleteRecursively(Path root) throws java.io.IOException {
        if (!java.nio.file.Files.exists(root)) {
            return;
        }
        try (java.util.stream.Stream<Path> walk = java.nio.file.Files.walk(root)) {
            java.util.List<Path> entries = walk.sorted(java.util.Comparator.reverseOrder()).toList();
            for (Path entry : entries) {
                java.nio.file.Files.deleteIfExists(entry);
            }
        }
    }
}
