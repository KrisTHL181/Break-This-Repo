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
import org.jackhuang.hmcl.dsh.DshProcessManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The modpack REST surface over real HTTP: the market (index served, cached,
/// refreshed, failed; detail with versions and README), a market install and
/// an upload install run end-to-end through the fake-pnpm harness stub, and
/// the export → exports list → download chain, including the credential
/// filter (no `.key`/`.pem`/token files travel) and path-traversal refusal.
class PackApiTest {

    private static final String VERSION = "0.1.7-rc.1";

    /// How many times the export race regression case runs (see
    /// [PackApiTest#everyExportTaskFinishesWithTheResultItAttached]).
    ///
    /// The race needs repetition, not three hundred rounds: the work either sees the handle or it
    /// does not, and eighty runs catch it just as surely. Three hundred did not fit in the 120 s
    /// per-test budget the build sets (`junit.jupiter.execution.timeout.default`), and the JDK 21
    /// leg of CI failed on exactly that: a timeout, not a broken export.
    private static final int EXPORT_ATTEMPTS = 80;

    private final List<String> created = new ArrayList<>();
    private final List<HttpServer> stubs = new ArrayList<>();

    @TempDir
    Path dataDir;

    @AfterEach
    void tearDown() throws Exception {
        System.clearProperty("hdsl.packMarket");
        TestSupport.disableFakePnpm();
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

    // ---------------------------------------------------------------- market --

    /// A stub market: the index, one `.dspack`, and the pack's own documents.
    private HttpServer marketStub(byte[] packBytes) throws Exception {
        String sha256 = sha256(packBytes);
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/pack.dspack", exchange -> {
            exchange.getResponseHeaders().set("content-type", "application/octet-stream");
            exchange.sendResponseHeaders(200, packBytes.length);
            exchange.getResponseBody().write(packBytes);
            exchange.close();
        });
        server.createContext("/packs/acme.tools/manifest.json", exchange -> {
            byte[] bytes = ("{\"manifestVersion\":5,\"type\":\"profile\",\"name\":\"acme-tools\","
                    + "\"version\":\"1.2.3\",\"dshVersion\":\"" + VERSION + "\",\"dependencies\":{}}")
                    .getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("content-type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.createContext("/packs/acme.tools/README.md", exchange -> {
            byte[] bytes = "# Acme Tools\n\nA test pack.\n".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("content-type", "text/markdown");
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
        int port = server.getAddress().getPort();
        String index = """
                {"schemaVersion":2,"generatedAt":"2026-01-01T00:00:00Z","modpacks":[
                  {"id":"acme.tools","name":"acme-tools","version":"1.2.3",
                   "displayName":"Acme Tools","description":"A test pack",
                   "downloadUrl":"http://127.0.0.1:%d/pack.dspack",
                   "sha256":"%s","size":%d,
                   "owner":"acme","repo":"tools","author":"Acme",
                   "dshVersion":"%s","profileName":"web","updatedAt":"2026-01-01"}
                ]}
                """.formatted(port, sha256, packBytes.length, VERSION);
        server.createContext("/index.json", exchange -> {
            byte[] bytes = index.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("content-type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        stubs.add(server);
        return server;
    }

    private static void pointMarketAt(HttpServer stub) {
        System.setProperty("hdsl.packMarket",
                "http://127.0.0.1:" + stub.getAddress().getPort() + "/index.json");
    }

    @Test
    void marketIsListedCachedRefreshedAndFailedLoudly() throws Exception {
        HttpServer stub = marketStub(dspack("acme-tools"));
        pointMarketAt(stub);
        try (TestSupport.RunningServer running = startServer()) {
            HttpClient client = TestSupport.client();
            String base = running.baseUrl();

            HttpResponse<String> first = get(client, base + "/api/packs/market");
            assertEquals(200, first.statusCode(), first.body());
            JsonArray packs = json(first).getAsJsonArray("packs");
            assertEquals(1, packs.size());
            JsonObject pack = packs.get(0).getAsJsonObject();
            assertEquals("acme.tools", pack.get("id").getAsString());
            assertEquals("Acme Tools", pack.get("name").getAsString());
            assertEquals("Acme", pack.get("author").getAsString());
            assertEquals("A test pack", pack.get("description").getAsString());
            assertEquals("1.2.3", pack.get("version").getAsString());
            assertEquals("https://github.com/acme.png?size=64", pack.get("avatarUrl").getAsString());
            assertEquals("2026-01-01", pack.get("updatedAt").getAsString());

            // The detail lists the versions and carries the fetched documents.
            HttpResponse<String> detail = get(client, base + "/api/packs/market/acme.tools");
            assertEquals(200, detail.statusCode(), detail.body());
            JsonObject full = json(detail).getAsJsonObject("pack");
            assertEquals("acme.tools", full.get("id").getAsString());
            JsonArray versions = full.getAsJsonArray("versions");
            assertEquals(1, versions.size());
            assertEquals("1.2.3", versions.get(0).getAsJsonObject().get("version").getAsString());
            assertTrue(versions.get(0).getAsJsonObject().get("sizeBytes").getAsLong() > 0);
            assertEquals("5", full.getAsJsonObject("manifest").get("manifestVersion").getAsString());
            assertTrue(full.get("readme").getAsString().contains("Acme Tools"));
            assertEquals(404, get(client, base + "/api/packs/market/nobody.nothing").statusCode());

            // The upstream dies; the ten-minute memory cache still answers.
            stub.stop(0);
            stubs.remove(stub);
            HttpResponse<String> cached = get(client, base + "/api/packs/market");
            assertEquals(200, cached.statusCode(), "the in-memory cache must answer inside its TTL");

            // A forced refresh against an address that was never fetched (so
            // no disk copy exists either) is a 502.
            System.setProperty("hdsl.packMarket", "http://127.0.0.1:1/index.json");
            HttpResponse<String> refreshed = get(client, base + "/api/packs/market?refresh=1");
            assertEquals(502, refreshed.statusCode(), refreshed.body());
            assertNotNull(json(refreshed).get("error"));
        }
    }

    // ---------------------------------------------------------------- install --

    @Test
    void marketInstallRunsEndToEndAndCarriesTheInstanceId() throws Exception {
        NodeDshStub.assumeNode();
        TestSupport.enableFakePnpm();
        HttpServer stub = marketStub(dspack("acme-tools"));
        pointMarketAt(stub);
        try (TestSupport.RunningServer running = startServer()) {
            HttpClient client = TestSupport.client();
            String base = running.baseUrl();

            assertEquals(404, post(client, base + "/api/packs/install",
                    "{\"packId\":\"nobody.nothing\"}").statusCode());
            assertEquals(400, post(client, base + "/api/packs/install", "{}").statusCode());

            HttpResponse<String> install = post(client, base + "/api/packs/install",
                    "{\"packId\":\"acme.tools\"}");
            assertEquals(202, install.statusCode(), install.body());
            String taskId = json(install).get("taskId").getAsString();
            JsonObject task = awaitTask(client, base, taskId);
            assertEquals("done", task.get("state").getAsString(), "install task must finish: " + task);

            // The done task carries the created instance's id as its result.
            assertNotNull(task.get("result"), "a done pack install carries result.instanceId: " + task);
            String instanceId = task.getAsJsonObject("result").get("instanceId").getAsString();
            assertEquals("acme-tools", instanceId);
            created.add(instanceId);

            // The instance exists, is installed, and holds the pack's files.
            HttpResponse<String> detail = get(client, base + "/api/instances/" + instanceId);
            assertEquals(200, detail.statusCode());
            assertEquals(VERSION, json(detail).get("version").getAsString());
            DshInstance instance = DshInstanceManager.find(instanceId);
            assertNotNull(instance);
            Path profile = instance.homeDirectory().resolve("profiles").resolve("web");
            assertTrue(Files.isRegularFile(profile.resolve("package.json")), "machine file must land");
            assertTrue(Files.isRegularFile(profile.resolve("notes.txt")), "overrides must land");
            assertTrue(Files.isRegularFile(profile.resolve("cordis.patch.yml")), "overrides must land");
        }
    }

    @Test
    void uploadInstallRunsTheSameFlowFromTheStagedFile() throws Exception {
        NodeDshStub.assumeNode();
        TestSupport.enableFakePnpm();
        try (TestSupport.RunningServer running = startServer()) {
            HttpClient client = TestSupport.client();
            String base = running.baseUrl();

            byte[] pack = dspack("uploaded-tools");
            HttpResponse<String> upload = multipart(client, base + "/api/packs/upload",
                    "Uploaded Pack", "whatever.dspack", pack);
            assertEquals(202, upload.statusCode(), upload.body());
            JsonObject task = awaitTask(client, base, json(upload).get("taskId").getAsString());
            assertEquals("done", task.get("state").getAsString(), "upload install must finish: " + task);
            String instanceId = task.getAsJsonObject("result").get("instanceId").getAsString();
            assertEquals("uploaded-pack", instanceId, "the name field names the instance");
            created.add(instanceId);
            assertTrue(DshInstanceManager.exists(instanceId));

            // A non-pack upload is a 400, and a wrong extension is too.
            HttpResponse<String> notAPack = multipart(client, base + "/api/packs/upload",
                    null, "bad.dspack", "not a zip at all".getBytes(StandardCharsets.UTF_8));
            assertEquals(400, notAPack.statusCode(), notAPack.body());
            HttpResponse<String> wrongExtension = multipart(client, base + "/api/packs/upload",
                    null, "notes.zip", pack);
            assertEquals(400, wrongExtension.statusCode(), wrongExtension.body());
        }
    }

    // ----------------------------------------------------------------- export --

    @Test
    void theExportChoicesAreOfferedAndTheChosenHomeTravels() throws Exception {
        NodeDshStub.assumeNode();
        try (TestSupport.RunningServer running = startServer()) {
            HttpClient client = TestSupport.client();
            String base = running.baseUrl();
            String id = createInstance(client, base, "homely");
            NodeDshStub.install(DshInstanceManager.find(id), false);
            DshInstance instance = DshInstanceManager.find(id);

            Path profile = instance.homeDirectory().resolve("profiles").resolve("web");
            Files.createDirectories(profile);
            Files.writeString(profile.resolve("package.json"), "{\"dependencies\":{}}\n");
            Files.writeString(profile.resolve("cordis.patch.yml"), "plugins: {}\n");
            Path home = instance.homeDirectory();
            Files.createDirectories(home.resolve("skills/keep"));
            Files.writeString(home.resolve("skills/keep/SKILL.md"), "# keep me\n");
            Files.writeString(home.resolve("AGENTS.md"), "global instructions\n");
            Files.writeString(home.resolve(".credentials.yaml"), "token: secret\n");
            Files.createDirectories(home.resolve("profiles/other"));
            Files.writeString(home.resolve("profiles/other/package.json"), "{\"name\":\"other\"}\n");

            // A GET is what a form asks for the choices: the home content the filter allows, and the
            // harness versions this machine has. Neither is guessed at by the caller.
            HttpResponse<String> choices = get(client, base + "/api/instances/" + id + "/export");
            assertEquals(200, choices.statusCode(), choices.body());
            List<String> offered = new ArrayList<>();
            for (com.google.gson.JsonElement file : json(choices).getAsJsonArray("home")) {
                offered.add(file.getAsJsonObject().get("path").getAsString());
            }
            assertTrue(offered.contains("skills/keep/SKILL.md"), offered.toString());
            assertTrue(offered.contains("AGENTS.md"), offered.toString());
            assertTrue(offered.stream().noneMatch(path -> path.startsWith("profiles/")),
                    "every other profile in the home is not offered: " + offered);
            assertTrue(offered.stream().noneMatch(path -> path.contains("credentials")),
                    "and the filter has already refused the credentials: " + offered);
            List<String> versions = new ArrayList<>();
            json(choices).getAsJsonArray("dshVersions").forEach(v -> versions.add(v.getAsString()));
            assertEquals(List.of(VERSION), versions, "the versions this machine has, offered to choose from");

            // Asking for the home travels: what was named and allowed goes under `home/`, and naming
            // a credential does not carry it.
            HttpResponse<String> export = post(client, base + "/api/instances/" + id + "/export",
                    "{\"home\":[\"skills/keep/SKILL.md\",\"AGENTS.md\",\".credentials.yaml\"],"
                            + "\"dshVersions\":[\"0.1.6-alpha.2\",\"not a version\"]}");
            assertEquals(202, export.statusCode(), export.body());
            JsonObject task = awaitTask(client, base, json(export).get("taskId").getAsString());
            assertEquals("done", task.get("state").getAsString(), "export task must finish: " + task);
            String filename = task.getAsJsonObject("result").get("filename").getAsString();

            byte[] pack = Files.readAllBytes(dataDir.resolve("exports").resolve(filename));
            List<String> entries = zipEntries(pack);
            assertTrue(entries.contains("home/skills/keep/SKILL.md"), entries.toString());
            assertTrue(entries.contains("home/AGENTS.md"), entries.toString());
            assertTrue(entries.stream().noneMatch(entry -> entry.contains("credentials")),
                    "a credential is refused even when it is named: " + entries);
            assertTrue(entries.contains("overrides/cordis.patch.yml"), entries.toString());

            JsonObject manifest = JsonParser.parseString(zipEntry(pack, "manifest.json")).getAsJsonObject();
            List<String> measured = new ArrayList<>();
            manifest.getAsJsonArray("dshVersions").forEach(v -> measured.add(v.getAsString()));
            assertEquals(List.of(VERSION, "0.1.6-alpha.2"), measured,
                    "the measured set travels with the pin first, and what is not a version does not");

            // An export that asks for nothing carries no home, which is what it has always done.
            HttpResponse<String> plain = post(client, base + "/api/instances/" + id + "/export", "{}");
            assertEquals(202, plain.statusCode(), plain.body());
            JsonObject plainTask = awaitTask(client, base, json(plain).get("taskId").getAsString());
            assertEquals("done", plainTask.get("state").getAsString(), plainTask.toString());
            String plainFile = plainTask.getAsJsonObject("result").get("filename").getAsString();
            assertTrue(zipEntries(Files.readAllBytes(dataDir.resolve("exports").resolve(plainFile)))
                            .stream().noneMatch(entry -> entry.startsWith("home/")),
                    "nothing from the home travels unasked");
        }
    }

    /// The handle a task's work needs — to attach the export's result to — is
    /// there while the work runs, not only once the submission has returned.
    /// An export used to read a one-slot holder the submitter filled in after
    /// `submit` returned, so the work could reach the result while the holder
    /// was still empty, and lose the race to a null dereference: the artifact
    /// was written and the task failed anyway. This runs the shortest path that
    /// attaches a result, several times, so passing it cannot be luck.
    @Test
    @org.junit.jupiter.api.Timeout(180)
    void everyExportTaskFinishesWithTheResultItAttached() throws Exception {
        NodeDshStub.assumeNode();
        try (TestSupport.RunningServer running = startServer()) {
            HttpClient client = TestSupport.client();
            String base = running.baseUrl();
            String id = createInstance(client, base, "raced");
            NodeDshStub.install(DshInstanceManager.find(id), false);
            Path profile = DshInstanceManager.find(id).homeDirectory().resolve("profiles").resolve("web");
            Files.createDirectories(profile);
            Files.writeString(profile.resolve("package.json"), "{\"dependencies\":{}}\n");
            Files.writeString(profile.resolve("cordis.patch.yml"), "plugins: {}\n");

            for (int attempt = 0; attempt < EXPORT_ATTEMPTS; attempt++) {
                HttpResponse<String> export = post(client, base + "/api/instances/" + id + "/export", "{}");
                assertEquals(202, export.statusCode(), export.body());
                JsonObject task = awaitTask(client, base, json(export).get("taskId").getAsString());
                String where = "attempt " + (attempt + 1) + " of " + EXPORT_ATTEMPTS + ": " + task;
                assertEquals("done", task.get("state").getAsString(),
                        "the export must not lose the race to its own handle: " + where);
                assertNotNull(task.get("result"), "a finished export carries the file it wrote: " + where);
                assertTrue(Files.isRegularFile(dataDir.resolve("exports")
                                .resolve(task.getAsJsonObject("result").get("filename").getAsString())),
                        "and the artifact it names is on disk: " + where);
            }
        }
    }

    @Test
    void exportFiltersCredentialsAndDownloadsRoundTrip() throws Exception {
        NodeDshStub.assumeNode();
        try (TestSupport.RunningServer running = startServer()) {
            HttpClient client = TestSupport.client();
            String base = running.baseUrl();
            String id = createInstance(client, base, "secrets");
            NodeDshStub.install(DshInstanceManager.find(id), false);

            // A profile with content worth keeping and credentials worth not exporting.
            Path profile = DshInstanceManager.find(id).homeDirectory().resolve("profiles").resolve("web");
            Files.createDirectories(profile);
            Files.writeString(profile.resolve("package.json"), "{\"dependencies\":{}}\n");
            Files.writeString(profile.resolve("cordis.patch.yml"), "plugins: {}\n");
            Files.writeString(profile.resolve("normal.txt"), "ordinary content\n");
            Files.writeString(profile.resolve("id_rsa"), "PRIVATE KEY\n");
            Files.writeString(profile.resolve("server.key"), "KEY\n");
            Files.writeString(profile.resolve("cert.pem"), "PEM\n");
            Files.writeString(profile.resolve(".env"), "SECRET=1\n");
            Files.writeString(profile.resolve("api_token.txt"), "TOKEN\n");
            Files.writeString(profile.resolve("nested.zip"), "not really a zip\n");

            HttpResponse<String> export = post(client, base + "/api/instances/" + id + "/export", "{}");
            assertEquals(202, export.statusCode(), export.body());
            JsonObject task = awaitTask(client, base, json(export).get("taskId").getAsString());
            assertEquals("done", task.get("state").getAsString(), "export task must finish: " + task);
            String filename = task.getAsJsonObject("result").get("filename").getAsString();
            assertEquals(id + ".dspack", filename);

            // The exports list names it, and the download serves the same bytes.
            HttpResponse<String> listed = get(client, base + "/api/exports");
            assertEquals(200, listed.statusCode());
            JsonArray exports = json(listed).getAsJsonArray("exports");
            assertEquals(1, exports.size());
            JsonObject row = exports.get(0).getAsJsonObject();
            assertEquals(filename, row.get("filename").getAsString());
            assertTrue(row.get("sizeBytes").getAsLong() > 0);
            assertTrue(row.get("createdAt").getAsLong() > 0);

            HttpResponse<byte[]> downloaded = client.send(
                    HttpRequest.newBuilder(URI.create(base + "/api/exports/" + filename)).GET().build(),
                    HttpResponse.BodyHandlers.ofByteArray());
            assertEquals(200, downloaded.statusCode());
            assertTrue(downloaded.headers().firstValue("content-disposition").orElse("")
                    .contains("attachment"));
            byte[] onDisk = Files.readAllBytes(dataDir.resolve("exports").resolve(filename));
            assertEquals(onDisk.length, downloaded.body().length, "download must serve the artifact");

            // The credential filter held: no keys, no tokens, no env files,
            // no nested archives — and the profile's real files travelled.
            List<String> entries = zipEntries(downloaded.body());
            assertTrue(entries.contains("dspack.json"), entries::toString);
            assertTrue(entries.contains("manifest.json"), entries::toString);
            assertTrue(entries.contains("overrides/cordis.patch.yml"), entries::toString);
            assertTrue(entries.contains("overrides/normal.txt"), entries::toString);
            for (String entry : entries) {
                String lower = entry.toLowerCase(java.util.Locale.ROOT);
                assertFalse(lower.endsWith(".key") || lower.endsWith(".pem"),
                        "key material must not travel: " + entry);
                assertFalse(lower.contains("id_rsa"), "private keys must not travel: " + entry);
                assertFalse(lower.contains(".env"), "env files must not travel: " + entry);
                assertFalse(lower.contains("token"), "tokens must not travel: " + entry);
                assertFalse(lower.endsWith(".zip"), "nested archives must not travel: " + entry);
            }
            String manifest = zipEntry(downloaded.body(), "manifest.json");
            assertNotNull(manifest);
            assertEquals(VERSION, JsonParser.parseString(manifest).getAsJsonObject()
                    .get("dshVersion").getAsString());

            // Directory and artifact are owner-only where POSIX modes exist.
            assertOwnerOnlyPermissions(dataDir.resolve("exports"),
                    dataDir.resolve("exports").resolve(filename));

            // Path traversal and unknown names are refused.
            assertNotEquals(200, get(client, base + "/api/exports/..%2F..%2Fbuild.gradle.kts").statusCode());
            assertEquals(404, get(client, base + "/api/exports/nope.dspack").statusCode());
        }
    }

    /// An instance that was installed but never launched has no profile tree —
    /// the harness creates it on first boot. Exporting it must not be refused:
    /// the profile directory is materialised, and the pack carries what exists.
    @Test
    void exportSucceedsBeforeTheFirstLaunch() throws Exception {
        NodeDshStub.assumeNode();
        try (TestSupport.RunningServer running = startServer()) {
            HttpClient client = TestSupport.client();
            String base = running.baseUrl();
            String id = createInstance(client, base, "fresh");
            NodeDshStub.install(DshInstanceManager.find(id), false);
            assertFalse(Files.exists(DshInstanceManager.find(id).homeDirectory()
                    .resolve("profiles").resolve("web")), "the profile tree appears only on first boot");

            HttpResponse<String> export = post(client, base + "/api/instances/" + id + "/export", "{}");
            assertEquals(202, export.statusCode(), export.body());
            JsonObject task = awaitTask(client, base, json(export).get("taskId").getAsString());
            assertEquals("done", task.get("state").getAsString(), "export task must finish: " + task);
            assertEquals(id + ".dspack", task.getAsJsonObject("result").get("filename").getAsString());
        }
    }

    @Test
    void exportWithSessionsCarriesTheConversationLogs() throws Exception {
        NodeDshStub.assumeNode();
        try (TestSupport.RunningServer running = startServer()) {
            HttpClient client = TestSupport.client();
            String base = running.baseUrl();
            String id = createInstance(client, base, "with-sessions");
            NodeDshStub.install(DshInstanceManager.find(id), false);

            DshInstance instance = DshInstanceManager.find(id);
            Path profile = instance.homeDirectory().resolve("profiles").resolve("web");
            Files.createDirectories(profile);
            Files.writeString(profile.resolve("cordis.patch.yml"), "plugins: {}\n");
            Path sessionDir = instance.homeDirectory().resolve("sessions").resolve("proj").resolve("s-1");
            Files.createDirectories(sessionDir);
            Files.writeString(sessionDir.resolve("session.v3.jsonl"), "{\"type\":\"header\"}\n");

            HttpResponse<String> export = post(client, base + "/api/instances/" + id + "/export",
                    "{\"includeSessions\":true}");
            assertEquals(202, export.statusCode(), export.body());
            JsonObject task = awaitTask(client, base, json(export).get("taskId").getAsString());
            assertEquals("done", task.get("state").getAsString(), "export task must finish: " + task);
            String filename = task.getAsJsonObject("result").get("filename").getAsString();

            List<String> entries = zipEntries(Files.readAllBytes(dataDir.resolve("exports").resolve(filename)));
            assertTrue(entries.contains("dspack.json"), entries::toString);
            assertTrue(entries.contains("overrides/cordis.patch.yml"), entries::toString);
            assertTrue(entries.contains("sessions/proj/s-1/session.v3.jsonl"),
                    "the conversation log must travel: " + entries);

            // An instance with no profile cannot be exported.
            String bare = createInstance(client, base, "bare");
            HttpResponse<String> refused = post(client, base + "/api/instances/" + bare + "/export", "{}");
            assertEquals(409, refused.statusCode(), refused.body());
        }
    }

    @Test
    void anUploadedTgzOfTheOldContractInstallsToo() throws Exception {
        NodeDshStub.assumeNode();
        TestSupport.enableFakePnpm();
        try (TestSupport.RunningServer running = startServer()) {
            HttpClient client = TestSupport.client();
            String base = running.baseUrl();

            // The format's first container: a flat gzipped tar with the manifest at its root, which
            // is what packs were before the `.dspack` marker existed.
            byte[] pack = tgz(
                    "manifest.json", "{\"manifestVersion\":3,\"name\":\"old-tools\",\"version\":\"1.0.0\","
                            + "\"dshVersion\":\"" + VERSION + "\",\"profileName\":\"web\","
                            + "\"bundles\":[],\"dependencies\":{}}",
                    "package.json", "{\"dependencies\":{}}\n",
                    "cordis.patch.yml", "# pack patch\n",
                    "notes.txt", "hello from the old pack\n");

            HttpResponse<String> upload = multipart(client, base + "/api/packs/upload",
                    "Old Tools", "old-tools.tgz", pack);
            assertEquals(202, upload.statusCode(), upload.body());
            JsonObject task = awaitTask(client, base, json(upload).get("taskId").getAsString());
            assertEquals("done", task.get("state").getAsString(), "the old container must install: " + task);
            String instanceId = task.getAsJsonObject("result").get("instanceId").getAsString();
            created.add(instanceId);

            DshInstance instance = DshInstanceManager.find(instanceId);
            assertNotNull(instance);
            assertEquals(VERSION, instance.version());
            Path profile = instance.homeDirectory().resolve("profiles").resolve("web");
            assertTrue(Files.isRegularFile(profile.resolve("package.json")),
                    "the machine file lands first, where the install reads it");
            assertTrue(Files.isRegularFile(profile.resolve("cordis.patch.yml")));
            assertTrue(Files.isRegularFile(profile.resolve("notes.txt")));
            assertFalse(Files.exists(profile.resolve("manifest.json")),
                    "the archive's own manifest is not part of the installation");
        }
    }

    /// A `.tgz` pack, written by a real tar writer: the old container is one this launcher has to be
    /// able to read, and a renamed ZIP would test nothing.
    private static byte[] tgz(String... members) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (kala.compress.archivers.tar.TarArchiveOutputStream tar =
                     new kala.compress.archivers.tar.TarArchiveOutputStream(
                             new java.util.zip.GZIPOutputStream(bytes))) {
            for (int i = 0; i < members.length; i += 2) {
                byte[] content = members[i + 1].getBytes(StandardCharsets.UTF_8);
                kala.compress.archivers.tar.TarArchiveEntry entry =
                        new kala.compress.archivers.tar.TarArchiveEntry(members[i]);
                entry.setSize(content.length);
                tar.putArchiveEntry(entry);
                tar.write(content);
                tar.closeArchiveEntry();
            }
        }
        return bytes.toByteArray();
    }

    // ----------------------------------------------------------------- helpers --

    @Test
    void aPackThatDeclaresItDoesNotSupportThisLauncherStillInstallsAndSaysSo() throws Exception {
        NodeDshStub.assumeNode();
        TestSupport.enableFakePnpm();
        try (TestSupport.RunningServer running = startServer()) {
            HttpClient client = TestSupport.client();
            String base = running.baseUrl();

            // `supported: false` with the author's own words. The format is explicit that this is a
            // warning and not a gate, so the instance has to exist afterwards — and what the author
            // wanted shown has to be somewhere the person can see it.
            byte[] pack = dspack("pickier", "\"launchers\":{\"hdsl\":{\"supported\":false,"
                    + "\"reason\":\"needs the desktop settings screen\"}},");
            HttpResponse<String> upload = multipart(client, base + "/api/packs/upload",
                    "Pickier", "pickier.dspack", pack);
            assertEquals(202, upload.statusCode(), upload.body());
            JsonObject task = awaitTask(client, base, json(upload).get("taskId").getAsString());
            assertEquals("done", task.get("state").getAsString(),
                    "a launcher declaration never refuses an install: " + task);
            created.add(task.getAsJsonObject("result").get("instanceId").getAsString());
            assertTrue(task.get("message").getAsString()
                            .contains("needs the desktop settings screen"),
                    "and the import result carries what the pack said: " + task.get("message"));
        }
    }

    private String createInstance(HttpClient client, String base, String name) throws Exception {
        HttpResponse<String> response = post(client, base + "/api/instances",
                "{\"name\":\"" + name + "\",\"version\":\"" + VERSION + "\",\"autoInstall\":false}");
        assertEquals(201, response.statusCode(), response.body());
        String id = json(response).getAsJsonObject("instance").get("id").getAsString();
        created.add(id);
        return id;
    }

    /// A minimal but legitimate `.dspack`: the v3 marker, a v5 manifest, one
    /// machine file and two override files.
    private static byte[] dspack(String name) throws Exception {
        // The manifest is the same except for whatever the caller adds (a launcher declaration, for
        // one), which is easier to read here than a second copy of the whole document.
        return dspack(name, "");
    }

    /// A `.dspack` whose manifest carries extra fields.
    ///
    /// @param name  the pack's identifier
    /// @param extra manifest fields, written after `version` and ending with a comma
    private static byte[] dspack(String name, String extra) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
            put(zip, "dspack.json", "{\"format\":\"dspack\",\"version\":3}");
            put(zip, "manifest.json", "{\"manifestVersion\":5,\"type\":\"profile\",\"name\":\"" + name
                    + "\",\"version\":\"1.0.0\"," + extra + "\"dshVersion\":\"" + VERSION
                    + "\",\"profileName\":\"web\",\"dependencies\":{}}");
            put(zip, "package.json", "{\"dependencies\":{}}\n");
            put(zip, "overrides/cordis.patch.yml", "# pack patch\n");
            put(zip, "overrides/notes.txt", "hello from the pack\n");
        }
        return bytes.toByteArray();
    }

    private static void put(ZipOutputStream zip, String name, String body) throws Exception {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(body.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    private static String sha256(byte[] bytes) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes);
        StringBuilder hex = new StringBuilder();
        for (byte value : digest) {
            hex.append(String.format("%02x", value));
        }
        return hex.toString();
    }

    private static List<String> zipEntries(byte[] bytes) throws Exception {
        List<String> names = new ArrayList<>();
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(bytes))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                names.add(entry.getName());
            }
        }
        return names;
    }

    private static String zipEntry(byte[] bytes, String wanted) throws Exception {
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(bytes))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (entry.getName().equals(wanted)) {
                    return new String(zip.readAllBytes(), StandardCharsets.UTF_8);
                }
            }
        }
        return null;
    }

    private static void assertOwnerOnlyPermissions(Path dir, Path file) {
        try {
            java.util.Set<java.nio.file.attribute.PosixFilePermission> dirPerms =
                    Files.getPosixFilePermissions(dir);
            assertEquals(java.util.Set.of(
                    java.nio.file.attribute.PosixFilePermission.OWNER_READ,
                    java.nio.file.attribute.PosixFilePermission.OWNER_WRITE,
                    java.nio.file.attribute.PosixFilePermission.OWNER_EXECUTE), dirPerms,
                    "the exports directory must be owner-only");
            java.util.Set<java.nio.file.attribute.PosixFilePermission> filePerms =
                    Files.getPosixFilePermissions(file);
            assertEquals(java.util.Set.of(
                    java.nio.file.attribute.PosixFilePermission.OWNER_READ,
                    java.nio.file.attribute.PosixFilePermission.OWNER_WRITE), filePerms,
                    "an artifact must be owner-read/write only");
        } catch (UnsupportedOperationException e) {
            // A filesystem without POSIX modes; the hardening does not apply.
        } catch (java.io.IOException e) {
            throw new AssertionError("could not read permissions", e);
        }
    }

    private static JsonObject awaitTask(HttpClient client, String base, String taskId) throws Exception {
        long deadline = System.nanoTime() + Duration.ofSeconds(90).toNanos();
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

    /// A multipart POST with the pack bytes as `file` and, optionally, a
    /// plain `name` field.
    private static HttpResponse<String> multipart(HttpClient client, String url, String name,
                                                  String fileName, byte[] content) throws Exception {
        String boundary = "----hdsl-test-boundary";
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        if (name != null) {
            body.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"name\"\r\n\r\n"
                    + name + "\r\n").getBytes(StandardCharsets.UTF_8));
        }
        body.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"file\""
                + "; filename=\"" + fileName + "\"\r\n"
                + "Content-Type: application/octet-stream\r\n\r\n").getBytes(StandardCharsets.UTF_8));
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
}
