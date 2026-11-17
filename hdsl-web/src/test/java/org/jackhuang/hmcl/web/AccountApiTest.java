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
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.jackhuang.hmcl.dsh.DshAccount;
import org.jackhuang.hmcl.dsh.DshInstanceManager;
import org.jackhuang.hmcl.dsh.DshProcessManager;
import org.jackhuang.hmcl.dsh.DshVendor;
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
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The account REST surface over real HTTP: the vendor catalogue, CRUD with
/// key masking (the apiKey never leaves the server), live verification against
/// a stub supplier, the per-instance account assignment with its on-disk
/// persistence, reference cleanup on delete, and the `accounts-changed`
/// WebSocket broadcast.
class AccountApiTest {

    private static final String GOOD_KEY = "sk-test-abcdef123456";
    private static final String BAD_KEY = "sk-bad-0000000000";

    private final List<String> createdInstances = new ArrayList<>();
    private final List<Runnable> cleanup = new ArrayList<>();

    private List<DshAccount> savedAccounts;
    private List<DshVendor> savedVendors;

    @TempDir
    Path dataDir;

    @BeforeEach
    void snapshotSettings() {
        savedAccounts = new ArrayList<>(SettingsManager.settings().getAccounts());
        savedVendors = new ArrayList<>(SettingsManager.settings().getCustomVendors());
    }

    @AfterEach
    void tearDown() throws Exception {
        SettingsManager.settings().getAccounts().clear();
        SettingsManager.settings().getAccounts().addAll(savedAccounts);
        SettingsManager.settings().getCustomVendors().clear();
        SettingsManager.settings().getCustomVendors().addAll(savedVendors);
        SettingsManager.save();
        for (Runnable task : cleanup) {
            task.run();
        }
        cleanup.clear();
        for (String id : createdInstances) {
            DshProcessManager.stop(id);
            if (DshInstanceManager.exists(id)) {
                DshInstanceManager.delete(id);
            }
        }
        createdInstances.clear();
    }

    private TestSupport.RunningServer startServer() throws Exception {
        return TestSupport.start(dataDir, Map.of(), config -> {
            config.bindHost = "127.0.0.1";
            config.auth.disabled = true;
        });
    }

    /// A stub supplier: `/models` answers the model list for the good key and
    /// 401 for anything else, the two halves of every verification path.
    private HttpServer vendorStub() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/models", exchange -> {
            String auth = exchange.getRequestHeaders().getFirst("Authorization");
            boolean good = ("Bearer " + GOOD_KEY).equals(auth);
            String body = good
                    ? "{\"data\":[{\"id\":\"model-a\"},{\"id\":\"model-b\"}]}"
                    : "{\"error\":{\"message\":\"Invalid API key\"}}";
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("content-type", "application/json");
            exchange.sendResponseHeaders(good ? 200 : 401, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
        cleanup.add(() -> server.stop(0));
        return server;
    }

    @Test
    void vendorsListIncludesTheBuiltIns() throws Exception {
        try (TestSupport.RunningServer running = startServer()) {
            HttpResponse<String> response = get(TestSupport.client(), running.baseUrl() + "/api/vendors");
            assertEquals(200, response.statusCode());
            JsonArray vendors = json(response).getAsJsonArray("vendors");
            assertTrue(vendors.size() >= 12, "the offered catalogue must be listed");
            JsonObject deepseek = null;
            for (int i = 0; i < vendors.size(); i++) {
                JsonObject vendor = vendors.get(i).getAsJsonObject();
                if ("deepseek".equals(vendor.get("id").getAsString())) {
                    deepseek = vendor;
                }
            }
            assertNotNull(deepseek);
            assertEquals("DeepSeek", deepseek.get("name").getAsString());
            assertEquals("https://api.deepseek.com", deepseek.get("endpoint").getAsString());
            assertEquals("DEEPSEEK_API_KEY", deepseek.get("envVar").getAsString());
            assertEquals("openai-completions",
                    deepseek.getAsJsonArray("kinds").get(0).getAsString());
            assertTrue(deepseek.get("preferred").getAsBoolean());
        }
    }

    /// The contract the panel's "add account" dialog depends on: the vendor
    /// path sends a vendor id and a key, and the account kind is inferred.
    /// The catalogue's `kinds` are *wire protocols* — sending one as the kind
    /// is refused, which is exactly the mismatch that made every add fail with
    /// "kind must be one of official, third-party, offline".
    @Test
    void theVendorPathInfersTheKindAndProtocolsAreNotKinds() throws Exception {
        HttpServer vendor = vendorStub();
        String endpoint = "http://127.0.0.1:" + vendor.getAddress().getPort();
        try (TestSupport.RunningServer running = startServer()) {
            HttpClient client = TestSupport.client();
            String base = running.baseUrl();

            // What the fixed panel sends: vendor + endpoint + key, no kind.
            HttpResponse<String> created = post(client, base + "/api/accounts",
                    "{\"vendor\":\"deepseek\",\"endpoint\":\"" + endpoint
                            + "\",\"apiKey\":\"" + GOOD_KEY + "\",\"label\":\"panel\"}");
            assertEquals(201, created.statusCode(), created.body());
            assertEquals("official",
                    json(created).getAsJsonObject("account").get("kind").getAsString());

            // What the broken panel sent: the vendor's protocol as the kind.
            HttpResponse<String> rejected = post(client, base + "/api/accounts",
                    "{\"vendor\":\"deepseek\",\"endpoint\":\"" + endpoint
                            + "\",\"apiKey\":\"" + BAD_KEY + "\",\"kind\":\"openai-completions\"}");
            assertEquals(400, rejected.statusCode(), rejected.body());
            assertTrue(json(rejected).get("error").getAsString().contains("openai-completions"),
                    "the refusal names what arrived: " + rejected.body());
        }
    }

    /// The wire protocol a supplier outside the launcher's own catalogue speaks is the caller's to
    /// state, and what it states is what the route is written with at launch.
    @Test
    void aCustomSuppliersProtocolIsTheOneItWasCreatedWith() throws Exception {
        HttpServer vendor = vendorStub();
        String endpoint = "http://127.0.0.1:" + vendor.getAddress().getPort();
        try (TestSupport.RunningServer running = startServer()) {
            HttpClient client = TestSupport.client();
            String base = running.baseUrl();

            HttpResponse<String> created = post(client, base + "/api/accounts",
                    "{\"vendor\":\"my-anthropic\",\"endpoint\":\"" + endpoint
                            + "\",\"apiKey\":\"" + GOOD_KEY + "\",\"label\":\"plan\","
                            + "\"protocol\":\"anthropic-messages\"}");
            assertEquals(201, created.statusCode(), created.body());

            DshVendor added = null;
            for (DshVendor custom : SettingsManager.settings().getCustomVendors()) {
                if ("my-anthropic".equals(custom.id())) {
                    added = custom;
                }
            }
            assertNotNull(added, "a supplier this launcher had not heard of is recorded: "
                    + created.body());
            assertEquals("anthropic-messages", added.api());
            assertEquals(endpoint, added.baseUrl());
            assertEquals("MY_ANTHROPIC_API_KEY", added.apiKeyEnv());

            // The account dialog reads it back, and it is on disk for the next start.
            JsonArray vendors = json(get(client, base + "/api/vendors")).getAsJsonArray("vendors");
            JsonObject mine = vendorById(vendors, "my-anthropic");
            assertNotNull(mine, "the added supplier must be listed: " + vendors);
            assertEquals("anthropic-messages", mine.getAsJsonArray("kinds").get(0).getAsString());

            JsonObject saved = JsonParser.parseString(Files.readString(
                            org.jackhuang.hmcl.Metadata.HMCL_USER_HOME.resolve("launcher-settings.json")))
                    .getAsJsonObject();
            JsonObject stored = vendorById(saved.getAsJsonArray("vendors"), "my-anthropic");
            assertNotNull(stored, "the added supplier must survive a restart: " + saved);
            assertEquals("anthropic-messages", stored.get("api").getAsString());
        }
    }

    /// A protocol outside the three the harness accepts is refused rather than dropped: a route
    /// written with the wrong wire fails at launch, where nothing says which value was wrong.
    @Test
    void aProtocolOutsideTheThreeWiresIsRefused() throws Exception {
        HttpServer vendor = vendorStub();
        String endpoint = "http://127.0.0.1:" + vendor.getAddress().getPort();
        try (TestSupport.RunningServer running = startServer()) {
            HttpClient client = TestSupport.client();
            String base = running.baseUrl();

            HttpResponse<String> refused = post(client, base + "/api/accounts",
                    "{\"vendor\":\"my-anthropic\",\"endpoint\":\"" + endpoint
                            + "\",\"apiKey\":\"" + GOOD_KEY + "\",\"protocol\":\"nonsense\"}");
            assertEquals(400, refused.statusCode(), refused.body());
            assertTrue(json(refused).get("error").getAsString().contains("protocol must be one of"),
                    "the refusal names the field it is about: " + refused.body());

            // And nothing was written: neither an account nor the supplier it would have added.
            assertEquals(0, json(get(client, base + "/api/accounts")).getAsJsonArray("accounts").size());
            assertNull(vendorById(json(get(client, base + "/api/vendors")).getAsJsonArray("vendors"),
                    "my-anthropic"), "a refused request must not register its supplier");
        }
    }

    /// A supplier the launcher ships with carries the protocol the launcher writes into every route
    /// for it, so a caller stating another one is ignored rather than obeyed.
    @Test
    void aVendorsOwnProtocolIsNotTheCallersToChange() throws Exception {
        HttpServer vendor = vendorStub();
        String endpoint = "http://127.0.0.1:" + vendor.getAddress().getPort();
        try (TestSupport.RunningServer running = startServer()) {
            HttpClient client = TestSupport.client();
            String base = running.baseUrl();

            HttpResponse<String> created = post(client, base + "/api/accounts",
                    "{\"vendor\":\"deepseek\",\"endpoint\":\"" + endpoint
                            + "\",\"apiKey\":\"" + GOOD_KEY + "\",\"protocol\":\"anthropic-messages\"}");
            assertEquals(201, created.statusCode(), created.body());

            JsonObject deepseek = vendorById(
                    json(get(client, base + "/api/vendors")).getAsJsonArray("vendors"), "deepseek");
            assertNotNull(deepseek);
            assertEquals("openai-completions", deepseek.getAsJsonArray("kinds").get(0).getAsString());
            for (DshVendor custom : SettingsManager.settings().getCustomVendors()) {
                assertFalse(custom.id().equalsIgnoreCase("deepseek"),
                        "a supplier the launcher ships with does not become a custom one");
            }
        }
    }

    @Test
    void accountCrudMasksTheKeyAndVerifiesLive() throws Exception {
        HttpServer vendor = vendorStub();
        String endpoint = "http://127.0.0.1:" + vendor.getAddress().getPort();
        try (TestSupport.RunningServer running = startServer()) {
            HttpClient client = TestSupport.client();
            String base = running.baseUrl();

            HttpResponse<String> created = post(client, base + "/api/accounts",
                    "{\"endpoint\":\"" + endpoint + "\",\"apiKey\":\"" + GOOD_KEY + "\",\"label\":\"main\"}");
            assertEquals(201, created.statusCode(), created.body());
            assertFalse(created.body().contains(GOOD_KEY), "the apiKey must never be returned");
            JsonObject account = json(created).getAsJsonObject("account");
            String name = account.get("name").getAsString();
            assertTrue(name.endsWith("|main"), "the key is vendorId|label: " + name);
            assertEquals(endpoint, account.get("endpoint").getAsString());
            assertEquals("third-party", account.get("kind").getAsString());
            assertEquals("sk-test-…", account.get("maskedKey").getAsString());
            assertTrue(account.get("verified").getAsBoolean(),
                    "the stub accepts the good key: " + account);

            // The list masks too.
            HttpResponse<String> list = get(client, base + "/api/accounts");
            assertEquals(200, list.statusCode());
            assertFalse(list.body().contains(GOOD_KEY), "the apiKey must never be listed");
            JsonArray accounts = json(list).getAsJsonArray("accounts");
            assertEquals(1, accounts.size());

            // A duplicate name is a conflict.
            HttpResponse<String> duplicate = post(client, base + "/api/accounts",
                    "{\"endpoint\":\"" + endpoint + "\",\"apiKey\":\"" + GOOD_KEY + "\",\"label\":\"main\"}");
            assertEquals(409, duplicate.statusCode());

            // Live verification names the models the supplier serves.
            String encoded = name.replace("|", "%7C");
            HttpResponse<String> verified = get(client, base + "/api/accounts/" + encoded + "/verify");
            assertEquals(200, verified.statusCode(), verified.body());
            JsonObject verifyBody = json(verified);
            assertTrue(verifyBody.get("ok").getAsBoolean(), verifyBody.toString());
            JsonArray models = verifyBody.getAsJsonArray("models");
            assertEquals(2, models.size());
            assertEquals("model-a", models.get(0).getAsString());

            // A wrong key swaps the verification answer but is still saved on edit.
            HttpResponse<String> rekeyed = send(client, base + "/api/accounts/" + encoded, "PATCH",
                    "{\"apiKey\":\"" + BAD_KEY + "\"}");
            assertEquals(200, rekeyed.statusCode(), rekeyed.body());
            assertFalse(rekeyed.body().contains(BAD_KEY));
            HttpResponse<String> rejected = get(client, base + "/api/accounts/" + encoded + "/verify");
            JsonObject rejectedBody = json(rejected);
            assertFalse(rejectedBody.get("ok").getAsBoolean());
            assertNotNull(rejectedBody.get("error"));

            // A rename re-keys the account.
            HttpResponse<String> renamed = send(client, base + "/api/accounts/" + encoded, "PATCH",
                    "{\"label\":\"renamed\"}");
            assertEquals(200, renamed.statusCode());
            String newName = json(renamed).getAsJsonObject("account").get("name").getAsString();
            assertTrue(newName.endsWith("|renamed"));
            assertEquals(404, get(client, base + "/api/accounts/" + encoded + "/verify").statusCode(),
                    "the old name is gone after a rename");

            // Delete removes it.
            String encodedNew = newName.replace("|", "%7C");
            HttpResponse<String> deleted = send(client, base + "/api/accounts/" + encodedNew, "DELETE", null);
            assertEquals(200, deleted.statusCode());
            JsonArray remaining = json(get(client, base + "/api/accounts")).getAsJsonArray("accounts");
            assertEquals(0, remaining.size());
        }
    }

    @Test
    void rejectedKeyStillSavesWithVerifyErrorAndOfflineNeedsNoKey() throws Exception {
        HttpServer vendor = vendorStub();
        String endpoint = "http://127.0.0.1:" + vendor.getAddress().getPort();
        try (TestSupport.RunningServer running = startServer()) {
            HttpClient client = TestSupport.client();
            String base = running.baseUrl();

            HttpResponse<String> created = post(client, base + "/api/accounts",
                    "{\"endpoint\":\"" + endpoint + "\",\"apiKey\":\"" + BAD_KEY + "\",\"label\":\"bad\"}");
            assertEquals(201, created.statusCode(), created.body());
            JsonObject account = json(created).getAsJsonObject("account");
            assertFalse(account.get("verified").getAsBoolean());
            assertNotNull(account.get("verifyError"), "a rejected key explains itself: " + account);

            // A supplier nobody can reach is a bad request, not a saved account.
            HttpResponse<String> unknown = post(client, base + "/api/accounts",
                    "{\"vendor\":\"no-such-vendor\",\"apiKey\":\"x\"}");
            assertEquals(400, unknown.statusCode());

            // An offline account: a name and no key, verified trivially.
            HttpResponse<String> offline = post(client, base + "/api/accounts",
                    "{\"kind\":\"offline\",\"label\":\"solo\"}");
            assertEquals(201, offline.statusCode(), offline.body());
            JsonObject offlineAccount = json(offline).getAsJsonObject("account");
            assertEquals("offline", offlineAccount.get("kind").getAsString());
            assertEquals("offline|solo", offlineAccount.get("name").getAsString());
            assertTrue(offlineAccount.get("maskedKey").isJsonNull());
            assertTrue(offlineAccount.get("verified").getAsBoolean());

            HttpResponse<String> verify = get(client, base + "/api/accounts/offline%7Csolo/verify");
            assertEquals(200, verify.statusCode());
            assertTrue(json(verify).get("ok").getAsBoolean());
            assertEquals(0, json(verify).getAsJsonArray("models").size());
        }
    }

    @Test
    void instanceAccountAssignmentPersistsAndIsClearedOnDelete() throws Exception {
        HttpServer vendor = vendorStub();
        String endpoint = "http://127.0.0.1:" + vendor.getAddress().getPort();
        try (TestSupport.RunningServer running = startServer()) {
            HttpClient client = TestSupport.client();
            String base = running.baseUrl();

            HttpResponse<String> createdAccount = post(client, base + "/api/accounts",
                    "{\"endpoint\":\"" + endpoint + "\",\"apiKey\":\"" + GOOD_KEY + "\",\"label\":\"main\"}");
            String name = json(createdAccount).getAsJsonObject("account").get("name").getAsString();

            HttpResponse<String> createdInstance = post(client, base + "/api/instances",
                    "{\"name\":\"with-account\",\"version\":\"0.1.7-rc.1\",\"autoInstall\":false}");
            assertEquals(201, createdInstance.statusCode());
            JsonObject instanceJson = json(createdInstance).getAsJsonObject("instance");
            String id = instanceJson.get("id").getAsString();
            createdInstances.add(id);
            assertTrue(instanceJson.get("account").isJsonNull(),
                    "a fresh instance has no account");

            // An unknown account is a 400.
            assertEquals(400, send(client, base + "/api/instances/" + id, "PATCH",
                    "{\"account\":\"no-such-account\"}").statusCode());

            // Assigning persists into the instance's own settings file.
            HttpResponse<String> assigned = send(client, base + "/api/instances/" + id, "PATCH",
                    "{\"account\":\"" + name + "\"}");
            assertEquals(200, assigned.statusCode(), assigned.body());
            assertEquals(name, json(assigned).getAsJsonObject("instance").get("account").getAsString());
            Path settingsFile = DshInstanceManager.find(id).instanceDirectory().resolve("settings.json");
            JsonObject settingsJson = JsonParser.parseString(Files.readString(settingsFile)).getAsJsonObject();
            assertEquals(name, settingsJson.get("accountKey").getAsString(),
                    "the instance settings must hold the account key");

            HttpResponse<String> detail = get(client, base + "/api/instances/" + id);
            assertEquals(name, json(detail).get("account").getAsString());

            // Deleting the account clears every reference.
            String encoded = name.replace("|", "%7C");
            assertEquals(200, send(client, base + "/api/accounts/" + encoded, "DELETE", null).statusCode());
            HttpResponse<String> afterDelete = get(client, base + "/api/instances/" + id);
            assertTrue(json(afterDelete).get("account").isJsonNull(),
                    "a deleted account must not linger in instance settings");
            settingsJson = JsonParser.parseString(Files.readString(settingsFile)).getAsJsonObject();
            assertFalse(settingsJson.has("accountKey"));

            // Assigning then clearing with null works too.
            HttpResponse<String> recreated = post(client, base + "/api/accounts",
                    "{\"endpoint\":\"" + endpoint + "\",\"apiKey\":\"" + GOOD_KEY + "\",\"label\":\"main\"}");
            String recreatedName = json(recreated).getAsJsonObject("account").get("name").getAsString();
            assertEquals(200, send(client, base + "/api/instances/" + id, "PATCH",
                    "{\"account\":\"" + recreatedName + "\"}").statusCode());
            HttpResponse<String> cleared = send(client, base + "/api/instances/" + id, "PATCH",
                    "{\"account\":null}");
            assertEquals(200, cleared.statusCode());
            assertTrue(json(cleared).getAsJsonObject("instance").get("account").isJsonNull());
            settingsJson = JsonParser.parseString(Files.readString(settingsFile)).getAsJsonObject();
            assertFalse(settingsJson.has("accountKey"));
        }
    }

    @Test
    void accountMutationsBroadcastAccountsChanged() throws Exception {
        HttpServer vendor = vendorStub();
        String endpoint = "http://127.0.0.1:" + vendor.getAddress().getPort();
        try (TestSupport.RunningServer running = startServer()) {
            WebSocketClient wsClient = new WebSocketClient();
            wsClient.start();
            cleanup.add(() -> {
                try {
                    wsClient.stop();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            BlockingQueue<JsonObject> events = new LinkedBlockingQueue<>();
            Session session = wsClient.connect(new Recording(events),
                    URI.create("ws://127.0.0.1:" + running.port() + "/ws"),
                    new ClientUpgradeRequest()).get(10, TimeUnit.SECONDS);
            assertNotNull(session);

            HttpClient client = TestSupport.client();
            String base = running.baseUrl();
            HttpResponse<String> created = post(client, base + "/api/accounts",
                    "{\"endpoint\":\"" + endpoint + "\",\"apiKey\":\"" + GOOD_KEY + "\",\"label\":\"main\"}");
            assertEquals(201, created.statusCode());
            String name = json(created).getAsJsonObject("account").get("name").getAsString();

            assertNotNull(await(events, "accounts-changed"),
                    "creating an account must broadcast accounts-changed");

            String encoded = name.replace("|", "%7C");
            assertEquals(200, send(client, base + "/api/accounts/" + encoded, "PATCH",
                    "{\"label\":\"renamed\"}").statusCode());
            assertNotNull(await(events, "accounts-changed"), "editing must broadcast");

            String newEncoded = name.replace("|", "%7C").replace("main", "renamed");
            assertEquals(200, send(client, base + "/api/accounts/" + newEncoded, "DELETE", null).statusCode());
            assertNotNull(await(events, "accounts-changed"), "deleting must broadcast");
        }
    }

    /// The test's WS endpoint. Named and public because Jetty's client binds
    /// endpoint methods through method handles.
    public static final class Recording implements Session.Listener.AutoDemanding {
        private final BlockingQueue<JsonObject> events;

        public Recording(BlockingQueue<JsonObject> events) {
            this.events = events;
        }

        @Override
        public void onWebSocketText(String message) {
            events.add(JsonParser.parseString(message).getAsJsonObject());
        }
    }

    private static JsonObject await(BlockingQueue<JsonObject> events, String type) throws InterruptedException {
        long deadline = System.nanoTime() + Duration.ofSeconds(15).toNanos();
        while (System.nanoTime() < deadline) {
            JsonObject json = events.poll(200, TimeUnit.MILLISECONDS);
            if (json != null && type.equals(json.get("type").getAsString())) {
                return json;
            }
        }
        return null;
    }

    private static HttpResponse<String> get(HttpClient client, String url) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create(url)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private static JsonObject vendorById(JsonArray vendors, String id) {
        for (int i = 0; i < vendors.size(); i++) {
            JsonObject vendor = vendors.get(i).getAsJsonObject();
            if (id.equals(vendor.get("id").getAsString())) {
                return vendor;
            }
        }
        return null;
    }

    private static HttpResponse<String> post(HttpClient client, String url, String body) throws Exception {
        return send(client, url, "POST", body);
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
