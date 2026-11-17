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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The skill REST surface over real HTTP: the list (flat skills and directory
/// bundles, enabled state from the frontmatter), removal, and the install
/// endpoint's request validation. The install's fetch half needs GitHub and
/// the catalogues, so its full run is covered by the manual smoke rather
/// than here.
class SkillApiTest {

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
    void skillsAreListedWithTheirFrontmatterAndRemovedById() throws Exception {
        try (TestSupport.RunningServer running = startServer()) {
            HttpClient client = TestSupport.client();
            String base = running.baseUrl();
            String id = createInstance(client, base, "skills");

            HttpResponse<String> empty = get(client, base + "/api/instances/" + id + "/skills");
            assertEquals(200, empty.statusCode());
            assertEquals(0, json(empty).getAsJsonArray("skills").size());

            DshInstance instance = DshInstanceManager.find(id);
            Path skills = instance.homeDirectory().resolve("skills");
            Files.createDirectories(skills);
            Files.writeString(skills.resolve("helper.md"), """
                    ---
                    name: helper
                    description: Helps with things
                    ---
                    Do the helping.
                    """);
            Files.writeString(skills.resolve("muted.md"), """
                    ---
                    name: muted
                    description: Switched off
                    disable-model-invocation: true
                    ---
                    Do nothing.
                    """);
            Path bundle = skills.resolve("pack");
            Files.createDirectories(bundle);
            Files.writeString(bundle.resolve("SKILL.md"), """
                    ---
                    name: pack
                    description: A bundled skill
                    ---
                    Bundle up.
                    """);

            HttpResponse<String> listed = get(client, base + "/api/instances/" + id + "/skills");
            assertEquals(200, listed.statusCode());
            JsonArray list = json(listed).getAsJsonArray("skills");
            assertEquals(3, list.size());

            JsonObject helper = find(list, "helper.md");
            assertEquals("helper", helper.get("name").getAsString());
            assertEquals("Helps with things", helper.get("description").getAsString());
            assertTrue(helper.get("enabled").getAsBoolean());
            assertFalse(helper.get("bundle").getAsBoolean());

            JsonObject muted = find(list, "muted.md");
            assertFalse(muted.get("enabled").getAsBoolean(),
                    "disable-model-invocation means disabled");

            JsonObject pack = find(list, "pack");
            assertEquals("A bundled skill", pack.get("description").getAsString());
            assertTrue(pack.get("bundle").getAsBoolean());

            // Removal by id (the on-disk name), and the frontmatter name as an alias.
            HttpResponse<String> removed = send(client, base + "/api/instances/" + id + "/skills/helper.md",
                    "DELETE", null);
            assertEquals(200, removed.statusCode(), removed.body());
            HttpResponse<String> removedByName = send(client, base + "/api/instances/" + id + "/skills/muted",
                    "DELETE", null);
            assertEquals(200, removedByName.statusCode(), removedByName.body());
            assertEquals(404, send(client, base + "/api/instances/" + id + "/skills/helper.md",
                    "DELETE", null).statusCode(), "already gone");

            JsonArray after = json(get(client, base + "/api/instances/" + id + "/skills"))
                    .getAsJsonArray("skills");
            assertEquals(1, after.size());
            assertEquals("pack", after.get(0).getAsJsonObject().get("id").getAsString());

            assertEquals(404, get(client, base + "/api/instances/nope/skills").statusCode());
        }
    }

    @Test
    void skillInstallValidatesItsRequest() throws Exception {
        try (TestSupport.RunningServer running = startServer()) {
            HttpClient client = TestSupport.client();
            String base = running.baseUrl();
            String id = createInstance(client, base, "skill-install");

            assertEquals(400, post(client, base + "/api/instances/" + id + "/skills/install",
                    "{}").statusCode());
            assertEquals(400, post(client, base + "/api/instances/" + id + "/skills/install",
                    "{\"source\":\"  \"}").statusCode());
            assertEquals(404, post(client, base + "/api/instances/nope/skills/install",
                    "{\"source\":\"owner/repo\"}").statusCode());
        }
    }

    // ----------------------------------------------------------------- helpers --

    private static JsonObject find(JsonArray skills, String id) {
        for (int i = 0; i < skills.size(); i++) {
            JsonObject skill = skills.get(i).getAsJsonObject();
            if (id.equals(skill.get("id").getAsString())) {
                return skill;
            }
        }
        throw new AssertionError("no skill " + id + " in " + skills);
    }

    private String createInstance(HttpClient client, String base, String name) throws Exception {
        HttpResponse<String> response = post(client, base + "/api/instances",
                "{\"name\":\"" + name + "\",\"version\":\"" + VERSION + "\",\"autoInstall\":false}");
        assertEquals(201, response.statusCode(), response.body());
        String id = json(response).getAsJsonObject("instance").get("id").getAsString();
        created.add(id);
        return id;
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
}
