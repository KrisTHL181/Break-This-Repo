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
import org.jackhuang.hmcl.dsh.DshInstance;
import org.jackhuang.hmcl.dsh.DshInstanceManager;
import org.jackhuang.hmcl.dsh.DshProcessManager;
import org.jackhuang.hmcl.setting.GameDirectory;
import org.jackhuang.hmcl.setting.GameDirectoryManager;
import org.junit.jupiter.api.AfterEach;
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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The instance REST surface over real HTTP, and the full lifecycle a user
/// walks through: create → (stub installed as the harness) → launch →
/// RUNNING with the tokenized URL → logs → open → stop → STOPPED → proxy 502.
///
/// The "dsh" is the node stub of [NodeDshStub]; without node on PATH these
/// tests are skipped, but everything else about them is real.
class InstanceApiTest {

    private static final String VERSION = "0.1.7-rc.1";

    private final java.util.List<String> created = new java.util.ArrayList<>();

    @TempDir
    Path dataDir;

    @AfterEach
    void tearDown() throws Exception {
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

    private String create(String name) throws Exception {
        created.add(name);
        return name;
    }

    private static HttpResponse<String> send(HttpClient client, String url, String method, String body)
            throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url));
        if (body == null) {
            builder.method(method, HttpRequest.BodyPublishers.noBody());
        } else {
            builder.header("Content-Type", "application/json")
                    .method(method, HttpRequest.BodyPublishers.ofString(body));
        }
        return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private static JsonObject json(HttpResponse<String> response) {
        return JsonParser.parseString(response.body()).getAsJsonObject();
    }

    @Test
    void aDirectoryTheNameResolvesToIsNotAnInstanceAndIsNotAdopted() throws Exception {
        // What a delete that was interrupted leaves behind: a directory under the instances root with
        // no manifest in it. The id an instance gets is made from the name the user typed, so the name
        // resolves to that directory — and since a create no longer adopts a directory that is already
        // there, the answer has to be the next free id rather than an error: a name is not something a
        // user should have to type differently because something else left a folder behind.
        try (TestSupport.RunningServer running = startServer()) {
            // Made **after** the server is up: booting wipes the shared home the tests run in, so a
            // directory made before it would be gone before the request that has to find it.
            Path leftover = Files.createDirectories(
                    org.jackhuang.hmcl.dsh.DshPaths.instanceDirectory("leftover"));
            Path document = Files.writeString(leftover.resolve("notes.txt"), "left by something else");

            try {
                HttpClient client = TestSupport.client();

                HttpResponse<String> created = send(client, running.baseUrl() + "/api/instances", "POST",
                        "{\"name\":\"Leftover\",\"version\":\"" + VERSION + "\",\"autoInstall\":false}");
                assertEquals(201, created.statusCode(), created.body());
                String id = json(created).getAsJsonObject("instance").get("id").getAsString();
                create(id);

                assertNotEquals("leftover", id, "an id whose directory exists is not the one handed out");
                assertTrue(Files.isRegularFile(document),
                        "the directory that held the name is left alone, with what is in it");
            } finally {
                org.jackhuang.hmcl.util.io.FileUtils.deleteDirectoryQuietly(leftover);
            }
        }
    }

    @Test
    void instanceCrudAndLifetimePortBinding() throws Exception {
        try (TestSupport.RunningServer running = startServer()) {
            HttpClient client = TestSupport.client();
            String base = running.baseUrl();

            // `autoInstall:false` asks for the bare manifest. The launcher's
            // default would start installing right away, and this test is
            // about the CRUD surface, not about pnpm.
            HttpResponse<String> createdResponse = send(client, base + "/api/instances", "POST",
                    "{\"name\":\"Crud One\",\"version\":\"" + VERSION + "\",\"autoInstall\":false}");
            assertEquals(201, createdResponse.statusCode());
            JsonObject one = json(createdResponse).getAsJsonObject("instance");
            String id = one.get("id").getAsString();
            create(id);
            assertEquals("crud-one", id);
            assertEquals(VERSION, one.get("version").getAsString());
            assertEquals("isolated", one.get("homeMode").getAsString());
            assertEquals("auto", one.get("portMode").getAsString());
            // Nothing is installed and no install was asked for: the state says
            // so instead of pretending the instance is merely stopped.
            assertEquals("NOT_INSTALLED", one.get("state").getAsString());
            assertTrue(json(createdResponse).get("installTaskId") == null);
            int port = one.get("port").getAsInt();
            assertTrue(port >= 3081 && port <= 4081, "auto port must come from the 3081-4081 pool: " + port);

            // The list answers with the same shape.
            HttpResponse<String> list = send(client, base + "/api/instances", "GET", null);
            assertEquals(200, list.statusCode());
            JsonArray instances = json(list).getAsJsonArray("instances");
            assertTrue(instances.size() >= 1);
            JsonObject listed = instances.asList().stream()
                    .map(element -> element.getAsJsonObject())
                    .filter(object -> id.equals(object.get("id").getAsString()))
                    .findFirst().orElseThrow();
            assertEquals("NOT_INSTALLED", listed.get("state").getAsString());

            // A second instance gets a different, lifetime-bound port.
            HttpResponse<String> secondResponse = send(client, base + "/api/instances", "POST",
                    "{\"name\":\"Crud Two\",\"version\":\"" + VERSION + "\",\"autoInstall\":false}");
            assertEquals(201, secondResponse.statusCode());
            String secondId = json(secondResponse).getAsJsonObject("instance").get("id").getAsString();
            create(secondId);
            DshInstance secondInstance = DshInstanceManager.find(secondId);
            assertNotEquals(port, secondInstance.portOrDefault());

            // Editing a running instance is refused; stopped it works.
            HttpResponse<String> detail = send(client, base + "/api/instances/" + id, "GET", null);
            assertEquals(200, detail.statusCode());
            JsonObject detailJson = json(detail);
            assertTrue(detailJson.has("installProgress"));
            assertEquals("none", detailJson.getAsJsonObject("installProgress").get("state").getAsString());

            HttpResponse<String> renamed = send(client, base + "/api/instances/" + id, "PATCH",
                    "{\"name\":\"Crud One Renamed\"}");
            assertEquals(200, renamed.statusCode());
            String renamedId = json(renamed).getAsJsonObject("instance").get("id").getAsString();
            assertEquals("crud-one-renamed", renamedId);
            created.remove(id);
            create(renamedId);

            // The old id is gone, the new one is there.
            assertEquals(404, send(client, base + "/api/instances/" + id, "GET", null).statusCode());
            assertEquals(200, send(client, base + "/api/instances/" + renamedId, "GET", null).statusCode());

            // Missing name/version are 400.
            assertEquals(400, send(client, base + "/api/instances", "POST",
                    "{\"name\":\"No Version\"}").statusCode());

            // Deleting a stopped instance removes it.
            HttpResponse<String> deleted = send(client, base + "/api/instances/" + renamedId, "DELETE", null);
            assertEquals(200, deleted.statusCode());
            created.remove(renamedId);
            assertEquals(404, send(client, base + "/api/instances/" + renamedId, "GET", null).statusCode());
        }
    }

    @Test
    void launchRunsUntilReadyThenStopKillsIt() throws Exception {
        NodeDshStub.assumeNode();
        try (TestSupport.RunningServer running = startServer()) {
            HttpClient client = TestSupport.client();
            String base = running.baseUrl();

            HttpResponse<String> createdResponse = send(client, base + "/api/instances", "POST",
                    "{\"name\":\"Lifecycle\",\"version\":\"" + VERSION + "\",\"autoInstall\":false}");
            String id = json(createdResponse).getAsJsonObject("instance").get("id").getAsString();
            create(id);

            // Not installed yet: launch refuses — but only because this call
            // opts out of the launcher's install-and-start behaviour.
            HttpResponse<String> refused = send(client, base + "/api/instances/" + id + "/launch", "POST",
                    "{\"publicHost\":\"panel.example.com\",\"autoInstall\":false}");
            assertEquals(409, refused.statusCode());
            assertEquals("not installed", json(refused).get("error").getAsString());

            // Installing the same version it already records (the stub counts
            // as installed) is refused.
            NodeDshStub.install(DshInstanceManager.find(id), true);
            HttpResponse<String> alreadyInstalled = send(client, base + "/api/instances/" + id + "/install", "POST",
                    "{\"version\":\"" + VERSION + "\"}");
            assertEquals(409, alreadyInstalled.statusCode());
            assertEquals("已安装该版本", json(alreadyInstalled).get("error").getAsString());

            HttpResponse<String> launch = send(client, base + "/api/instances/" + id + "/launch", "POST",
                    "{\"publicHost\":\"panel.example.com\"}");
            assertEquals(202, launch.statusCode());
            assertEquals("starting", json(launch).get("state").getAsString());

            // The state climbs to RUNNING and the URL carries the token.
            JsonObject runningJson = awaitState(client, base, id, "RUNNING");
            assertEquals("/i/" + id + "/?token=" + NodeDshStub.TOKEN, runningJson.get("url").getAsString());
            assertTrue(runningJson.get("uptimeSec").getAsLong() >= 0);
            assertTrue(runningJson.get("port").getAsInt() >= 3081);

            // The log window has the launch line and the readiness line, and
            // the launch carried the injected `--no-open`/`--trusted-host`.
            HttpResponse<String> logs = send(client, base + "/api/instances/" + id + "/logs?tail=200", "GET", null);
            assertEquals(200, logs.statusCode());
            JsonArray lines = json(logs).getAsJsonArray("lines");
            assertTrue(lines.size() > 0, "expected log lines");
            String joined = lines.asList().stream()
                    .map(element -> element.getAsJsonObject().get("text").getAsString())
                    .reduce("", (a, b) -> a + "\n" + b);
            assertTrue(joined.contains("dsh web: http://127.0.0.1:"), joined);
            assertTrue(joined.contains("--no-open"), joined);
            assertTrue(joined.contains("--trusted-host panel.example.com"), joined);

            // open answers with the tokenized URL.
            HttpResponse<String> open = send(client, base + "/api/instances/" + id + "/open", "GET", null);
            assertEquals(200, open.statusCode());
            assertEquals("/i/" + id + "/?token=" + NodeDshStub.TOKEN, json(open).get("url").getAsString());

            // The proxy serves the instance through the mount.
            HttpResponse<String> proxied = client.send(
                    HttpRequest.newBuilder(URI.create(base + "/i/" + id + "/")).GET().build(),
                    HttpResponse.BodyHandlers.ofString());
            assertEquals(200, proxied.statusCode());
            assertTrue(proxied.body().startsWith("stub ok"));

            // Launching again is idempotent.
            HttpResponse<String> again = send(client, base + "/api/instances/" + id + "/launch", "POST", "{}");
            assertEquals(202, again.statusCode());
            assertEquals("running", json(again).get("state").getAsString());

            // Stop kills the process tree; the mount goes dark.
            HttpResponse<String> stop = send(client, base + "/api/instances/" + id + "/stop", "POST", null);
            assertEquals(202, stop.statusCode());
            assertEquals("stopping", json(stop).get("state").getAsString());
            awaitState(client, base, id, "STOPPED");

            HttpResponse<String> gone = client.send(
                    HttpRequest.newBuilder(URI.create(base + "/i/" + id + "/")).GET().build(),
                    HttpResponse.BodyHandlers.ofString());
            assertEquals(502, gone.statusCode());
            assertEquals("instance not running", json(gone).get("error").getAsString());

            // Stopped instances refuse another stop, and refuse deletion while
            // ... they are deletable once stopped.
            assertEquals(409, send(client, base + "/api/instances/" + id + "/stop", "POST", null).statusCode());
            assertEquals(200, send(client, base + "/api/instances/" + id, "DELETE", null).statusCode());
            created.remove(id);
        }
    }

    /// A new instance opens on the *selected* directory: that is how a mounted
    /// workspace reaches the harness, and why the launcher selects one at
    /// start-up (`WorkspaceDirectory#install`).
    @Test
    void aNewInstanceOpensOnTheSelectedDirectory() throws Exception {
        Path workspace = Files.createTempDirectory("hdsl-workspace");
        try (TestSupport.RunningServer running = startServer()) {
            GameDirectory added = GameDirectoryManager.add(workspace, "工作区");
            try {
                GameDirectoryManager.select(added.id());

                HttpClient client = TestSupport.client();
                String base = running.baseUrl();
                HttpResponse<String> created = send(client, base + "/api/instances", "POST",
                        "{\"name\":\"On Workspace\",\"version\":\"" + VERSION + "\",\"autoInstall\":false}");
                assertEquals(201, created.statusCode(), created.body());
                String id = json(created).getAsJsonObject("instance").get("id").getAsString();
                create(id);

                DshInstance instance = DshInstanceManager.find(id);
                assertNotNull(instance);
                assertEquals(workspace.toAbsolutePath().normalize(), instance.workspacePath());
            } finally {
                GameDirectoryManager.remove(added.id());
            }
        }
    }

    // --------------------------------------------------------------- installs --

    @Test
    void createInstallsTheVersionItNames() throws Exception {
        NodeDshStub.assumeNode();
        TestSupport.enableFakePnpm();
        try (TestSupport.RunningServer running = startServer()) {
            HttpClient client = TestSupport.client();
            String base = running.baseUrl();

            HttpResponse<String> createdResponse = send(client, base + "/api/instances", "POST",
                    "{\"name\":\"Auto Install\",\"version\":\"" + VERSION + "\"}");
            assertEquals(201, createdResponse.statusCode(), createdResponse.body());
            JsonObject created = json(createdResponse);
            String id = created.getAsJsonObject("instance").get("id").getAsString();
            create(id);
            assertNotNull(created.get("installTaskId"),
                    "creating an instance must hand back the install it started");

            // The task names its instance: that field is how the panel tells
            // two concurrent installs apart.
            JsonObject task = awaitTask(client, base, created.get("installTaskId").getAsString(), "done");
            assertEquals(id, task.get("instanceId").getAsString());

            // Installed, not running — and the manifest describes the disk.
            JsonObject detail = awaitState(client, base, id, "STOPPED");
            assertEquals(VERSION, detail.get("version").getAsString());
            assertEquals(VERSION, installedVersion(id));
        } finally {
            TestSupport.disableFakePnpm();
        }
    }

    @Test
    void launchInstallsWhatIsMissingThenStarts() throws Exception {
        NodeDshStub.assumeNode();
        TestSupport.enableFakePnpm();
        try (TestSupport.RunningServer running = startServer()) {
            HttpClient client = TestSupport.client();
            String base = running.baseUrl();

            HttpResponse<String> createdResponse = send(client, base + "/api/instances", "POST",
                    "{\"name\":\"Install Then Start\",\"version\":\"" + VERSION + "\",\"autoInstall\":false}");
            String id = json(createdResponse).getAsJsonObject("instance").get("id").getAsString();
            create(id);

            // The launcher behaviour: "start" on a version that is not here yet
            // installs it and starts it, in one task.
            HttpResponse<String> launch = send(client, base + "/api/instances/" + id + "/launch", "POST", "{}");
            assertEquals(202, launch.statusCode(), launch.body());
            JsonObject launchJson = json(launch);
            assertEquals("installing", launchJson.get("state").getAsString());
            assertNotNull(launchJson.get("taskId"));

            JsonObject runningJson = awaitState(client, base, id, "RUNNING");
            assertEquals("/i/" + id + "/?token=packstubtoken", runningJson.get("url").getAsString());
        } finally {
            TestSupport.disableFakePnpm();
        }
    }

    @Test
    void installShowsInstallingWhileItRuns() throws Exception {
        NodeDshStub.assumeNode();
        TestSupport.enableFakePnpm();
        try (TestSupport.RunningServer running = startServer()) {
            HttpClient client = TestSupport.client();
            String base = running.baseUrl();
            String slow = "9.9.9-slow"; // the fake pnpm waits three seconds on this one

            HttpResponse<String> createdResponse = send(client, base + "/api/instances", "POST",
                    "{\"name\":\"Slow Install\",\"version\":\"" + slow + "\",\"autoInstall\":false}");
            String id = json(createdResponse).getAsJsonObject("instance").get("id").getAsString();
            create(id);
            assertEquals("NOT_INSTALLED",
                    json(createdResponse).getAsJsonObject("instance").get("state").getAsString());

            HttpResponse<String> install = send(client, base + "/api/instances/" + id + "/install", "POST",
                    "{\"version\":\"" + slow + "\"}");
            assertEquals(202, install.statusCode(), install.body());

            // The panel can see the install for what it is while it runs, and
            // the instance is installed (not running) once it is over.
            awaitState(client, base, id, "INSTALLING");
            awaitState(client, base, id, "STOPPED");
            assertEquals(slow, installedVersion(id));
        } finally {
            TestSupport.disableFakePnpm();
        }
    }

    @Test
    void installRecordsWhatPnpmActuallyLanded() throws Exception {
        NodeDshStub.assumeNode();
        TestSupport.enableFakePnpm();
        try (TestSupport.RunningServer running = startServer()) {
            HttpClient client = TestSupport.client();
            String base = running.baseUrl();
            String requested = "9.9.9-mismatch"; // the fake pnpm lays down 0.0.0-other

            HttpResponse<String> createdResponse = send(client, base + "/api/instances", "POST",
                    "{\"name\":\"Mismatch\",\"version\":\"" + requested + "\",\"autoInstall\":false}");
            String id = json(createdResponse).getAsJsonObject("instance").get("id").getAsString();
            create(id);

            HttpResponse<String> install = send(client, base + "/api/instances/" + id + "/install", "POST",
                    "{\"version\":\"" + requested + "\"}");
            assertEquals(202, install.statusCode());
            JsonObject task = awaitTask(client, base, json(install).get("taskId").getAsString(), "failed");
            String error = task.get("error").getAsString();
            assertTrue(error.contains(requested), error);
            assertTrue(error.contains("0.0.0-other"), error);

            // The manifest describes the disk even when that is not what was
            // asked for: an instance that lies about its version is worse than
            // one that tells the truth about a bad install.
            JsonObject detail = awaitState(client, base, id, "STOPPED");
            assertEquals("0.0.0-other", detail.get("version").getAsString());
            assertEquals("0.0.0-other", installedVersion(id));
        } finally {
            TestSupport.disableFakePnpm();
        }
    }

    private static JsonObject awaitTask(HttpClient client, String base, String taskId, String state)
            throws Exception {
        long deadline = System.nanoTime() + Duration.ofSeconds(60).toNanos();
        JsonObject last = null;
        while (System.nanoTime() < deadline) {
            HttpResponse<String> response = send(client, base + "/api/tasks/" + taskId, "GET", null);
            assertEquals(200, response.statusCode());
            last = json(response);
            if (state.equals(last.get("state").getAsString())) {
                return last;
            }
            Thread.sleep(100);
        }
        assertNotNull(last);
        assertEquals(state, last.get("state").getAsString(), "task never reached " + state);
        return last;
    }

    /// The version of the harness actually sitting in the instance's `dsh`
    /// directory — the truth the manifest is supposed to describe.
    private static String installedVersion(String id) throws Exception {
        DshInstance instance = DshInstanceManager.find(id);
        assertNotNull(instance);
        Path manifest = instance.dshDirectory()
                .resolve(org.jackhuang.hmcl.dsh.DshVersion.PACKAGE_PATH)
                .resolve("package.json");
        return JsonParser.parseString(java.nio.file.Files.readString(manifest))
                .getAsJsonObject().get("version").getAsString();
    }

    private static JsonObject awaitState(HttpClient client, String base, String id, String state)
            throws Exception {
        // A minute, not thirty seconds: an install + first launch spawns node
        // and pnpm, and a machine running anything else alongside the suite
        // (a container build, say) has been seen to take longer than half of
        // what a quiet one needs.
        long deadline = System.nanoTime() + Duration.ofSeconds(60).toNanos();
        JsonObject last = null;
        while (System.nanoTime() < deadline) {
            HttpResponse<String> detail = send(client, base + "/api/instances/" + id, "GET", null);
            assertEquals(200, detail.statusCode());
            last = json(detail);
            if (state.equals(last.get("state").getAsString())) {
                return last;
            }
            Thread.sleep(100);
        }
        assertNotNull(last);
        assertEquals(state, last.get("state").getAsString(), "state never reached " + state);
        return last;
    }
}
