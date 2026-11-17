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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The session and workspace REST surface over real HTTP: the list (empty and
/// with a titled session), the workspace grouping, the session-pack export
/// into `exports/`, and the request validation around it.
class SessionApiTest {

    private static final String VERSION = "0.1.7-rc.1";

    private final List<String> created = new ArrayList<>();

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

    @Test
    void sessionsListAndWorkspacesFollowTheHomesLayout() throws Exception {
        try (TestSupport.RunningServer running = startServer()) {
            HttpClient client = TestSupport.client();
            String base = running.baseUrl();
            String id = createInstance(client, base, "sessions");

            // Nothing recorded yet: empty lists, not errors.
            HttpResponse<String> empty = get(client, base + "/api/instances/" + id + "/sessions");
            assertEquals(200, empty.statusCode());
            assertEquals(0, json(empty).getAsJsonArray("sessions").size());
            HttpResponse<String> noWorkspaces = get(client, base + "/api/instances/" + id + "/workspaces");
            assertEquals(200, noWorkspaces.statusCode());
            assertEquals(0, json(noWorkspaces).getAsJsonArray("workspaces").size());

            // One session with a projection-cache row: the title and the
            // working directory come from the cache, the size from the disk.
            DshInstance instance = DshInstanceManager.find(id);
            Path sessionDir = instance.homeDirectory().resolve("sessions").resolve("proj-x").resolve("s-1");
            Files.createDirectories(sessionDir);
            Files.writeString(sessionDir.resolve("session.v3.jsonl"), "{\"type\":\"header\"}\n");
            Path cache = instance.homeDirectory().resolve("storages").resolve("session_projcache")
                    .resolve("sessions");
            Files.createDirectories(cache);
            Files.writeString(cache.resolve("s-1.json"), """
                    {"record":{"identity":{"cwd":"/tmp/proj-x","createdAt":1700000000000},
                     "rows":{"title":{"val":"My chat"}}}}
                    """);

            HttpResponse<String> listed = get(client, base + "/api/instances/" + id + "/sessions");
            assertEquals(200, listed.statusCode());
            JsonArray sessions = json(listed).getAsJsonArray("sessions");
            assertEquals(1, sessions.size());
            JsonObject session = sessions.get(0).getAsJsonObject();
            assertEquals("s-1", session.get("id").getAsString());
            assertEquals("My chat", session.get("title").getAsString());
            assertEquals("proj-x", session.get("workspaceSlug").getAsString());
            assertTrue(session.get("updatedAt").getAsLong() > 0);
            assertTrue(session.get("sizeBytes").getAsLong() > 0);

            HttpResponse<String> workspaces = get(client, base + "/api/instances/" + id + "/workspaces");
            assertEquals(200, workspaces.statusCode());
            JsonArray workspaceList = json(workspaces).getAsJsonArray("workspaces");
            assertEquals(1, workspaceList.size());
            JsonObject workspace = workspaceList.get(0).getAsJsonObject();
            assertEquals("proj-x", workspace.get("id").getAsString());
            assertEquals("proj-x", workspace.get("name").getAsString());
            assertEquals("/tmp/proj-x", workspace.get("path").getAsString());
            assertEquals(1, workspace.get("sessionCount").getAsInt());
            assertTrue(workspace.get("updatedAt").getAsLong() > 0);

            // Unknown instances are 404s.
            assertEquals(404, get(client, base + "/api/instances/nope/sessions").statusCode());
            assertEquals(404, get(client, base + "/api/instances/nope/workspaces").statusCode());
        }
    }

    @Test
    void sessionExportPacksIntoExportsAndValidatesIds() throws Exception {
        try (TestSupport.RunningServer running = startServer()) {
            HttpClient client = TestSupport.client();
            String base = running.baseUrl();
            String id = createInstance(client, base, "packing");

            DshInstance instance = DshInstanceManager.find(id);
            Path sessionDir = instance.homeDirectory().resolve("sessions").resolve("proj").resolve("s-1");
            Files.createDirectories(sessionDir);
            Files.writeString(sessionDir.resolve("session.v3.jsonl"), "{\"type\":\"header\"}\n");

            // Validation: a missing array, an empty one, and an unknown id.
            assertEquals(400, post(client, base + "/api/instances/" + id + "/sessions/export",
                    "{}").statusCode());
            assertEquals(400, post(client, base + "/api/instances/" + id + "/sessions/export",
                    "{\"sessionIds\":[]}").statusCode());
            assertEquals(400, post(client, base + "/api/instances/" + id + "/sessions/export",
                    "{\"sessionIds\":[\"nope\"]}").statusCode());

            HttpResponse<String> export = post(client, base + "/api/instances/" + id + "/sessions/export",
                    "{\"sessionIds\":[\"s-1\"]}");
            assertEquals(202, export.statusCode(), export.body());
            JsonObject task = awaitTask(client, base, json(export).get("taskId").getAsString());
            assertEquals("done", task.get("state").getAsString(), "session export must finish: " + task);
            String filename = task.getAsJsonObject("result").get("filename").getAsString();
            assertTrue(filename.endsWith(".sspack"), "a session pack is a .sspack: " + filename);

            // The artifact is listed and downloadable.
            HttpResponse<String> listed = get(client, base + "/api/exports");
            assertEquals(200, listed.statusCode());
            JsonArray exports = json(listed).getAsJsonArray("exports");
            assertEquals(1, exports.size());
            assertEquals(filename, exports.get(0).getAsJsonObject().get("filename").getAsString());
            HttpResponse<byte[]> downloaded = client.send(
                    HttpRequest.newBuilder(URI.create(base + "/api/exports/" + filename)).GET().build(),
                    HttpResponse.BodyHandlers.ofByteArray());
            assertEquals(200, downloaded.statusCode());
            assertTrue(downloaded.body().length > 0);
            List<String> entries = new ArrayList<>();
            try (var zip = new java.util.zip.ZipInputStream(
                    new java.io.ByteArrayInputStream(downloaded.body()))) {
                java.util.zip.ZipEntry entry;
                while ((entry = zip.getNextEntry()) != null) {
                    entries.add(entry.getName());
                }
            }
            assertTrue(entries.contains("sessions/proj/s-1/session.v3.jsonl"), entries::toString);
            assertTrue(entries.contains("manifest.json"), entries::toString);
        }
    }

    // ----------------------------------------------------------------- helpers --

    private String createInstance(HttpClient client, String base, String name) throws Exception {
        HttpResponse<String> response = post(client, base + "/api/instances",
                "{\"name\":\"" + name + "\",\"version\":\"" + VERSION + "\",\"autoInstall\":false}");
        assertEquals(201, response.statusCode(), response.body());
        String id = json(response).getAsJsonObject("instance").get("id").getAsString();
        created.add(id);
        return id;
    }

    private static JsonObject awaitTask(HttpClient client, String base, String taskId) throws Exception {
        long deadline = System.nanoTime() + Duration.ofSeconds(60).toNanos();
        JsonObject last = null;
        while (System.nanoTime() < deadline) {
            HttpResponse<String> poll = client.send(
                    HttpRequest.newBuilder(URI.create(base + "/api/tasks/" + taskId)).GET().build(),
                    HttpResponse.BodyHandlers.ofString());
            assertEquals(200, poll.statusCode());
            last = json(poll);
            String state = last.get("state").getAsString();
            if (!"running".equals(state) && !"waiting_approval".equals(state)) {
                return last;
            }
            Thread.sleep(100);
        }
        throw new AssertionError("task " + taskId + " never finished; last: " + last);
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

    private static JsonObject json(HttpResponse<String> response) {
        return JsonParser.parseString(response.body()).getAsJsonObject();
    }
}
