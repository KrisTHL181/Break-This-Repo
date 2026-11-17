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
import org.jackhuang.hmcl.dsh.DshPluginCatalog;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
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
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The models.dev REST surface over real HTTP with a stub directory: the merged supplier list, one
/// supplier's models, and the four answers the directory can give — served, cached, refreshed and
/// failed.
///
/// The directory is a stub because the point of these tests is what this server does with it: which
/// supplier leads the list, which protocol a supplier is given, and what a page sees when the
/// directory is slow, down, or gone. The document itself is
/// `src/test/resources/models-dev-sample.json`, the same fixture the reading is tested against.
class ModelsApiTest {

    private final List<HttpServer> stubs = new ArrayList<>();
    private final AtomicInteger fetches = new AtomicInteger();

    @TempDir
    Path dataDir;

    @AfterEach
    void tearDown() {
        System.clearProperty("hdsl.modelCatalog");
        for (HttpServer stub : stubs) {
            stub.stop(0);
        }
        stubs.clear();
    }

    /// The stub directory: the fixture, and a count of how often it was asked for.
    private HttpServer directoryStub() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api.json", exchange -> {
            fetches.incrementAndGet();
            byte[] bytes = sampleText().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("content-type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
        stubs.add(server);
        return server;
    }

    private static String stubUrl(HttpServer stub) {
        return "http://127.0.0.1:" + stub.getAddress().getPort() + "/api.json";
    }

    private static String sampleText() {
        try (InputStream stream = ModelsApiTest.class.getResourceAsStream("/models-dev-sample.json")) {
            assertNotNull(stream, "the fixture must be on the test classpath");
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new AssertionError("Could not read the fixture", e);
        }
    }

    private TestSupport.RunningServer startServer() throws Exception {
        return TestSupport.start(dataDir, Map.of(), config -> {
            config.bindHost = "127.0.0.1";
            config.auth.disabled = true;
        });
    }

    @Test
    void theListLeadsWithTheLaunchersOwnSuppliersAndMarksTheRestUnknown() throws Exception {
        HttpServer stub = directoryStub();
        System.setProperty("hdsl.modelCatalog", stubUrl(stub));
        try (TestSupport.RunningServer running = startServer()) {
            HttpClient client = TestSupport.client();
            String base = running.baseUrl();

            HttpResponse<String> response = get(client, base + "/api/models/providers?refresh=1");
            assertEquals(200, response.statusCode(), response.body());
            JsonObject body = json(response);
            assertEquals("remote", body.get("source").getAsString());
            assertTrue(body.get("fetchedAt").getAsLong() > 0, "a fetched list says when: " + body);
            JsonArray providers = body.getAsJsonArray("providers");

            // The launcher's own supplier leads, and the directory supplies its models.
            JsonObject deepseek = find(providers, "deepseek");
            assertNotNull(deepseek, "the launcher's own supplier must be listed: " + body);
            assertEquals("deepseek", providers.get(0).getAsJsonObject().get("id").getAsString());
            assertEquals("DeepSeek", deepseek.get("name").getAsString());
            assertEquals("openai-completions", deepseek.get("protocol").getAsString());
            assertEquals("dsh", deepseek.get("protocolSource").getAsString());
            assertTrue(deepseek.get("known").getAsBoolean());
            assertTrue(deepseek.get("preferred").getAsBoolean());
            assertFalse(deepseek.get("custom").getAsBoolean());
            assertEquals("deepseek", deepseek.get("catalogId").getAsString());
            assertEquals(2, deepseek.get("modelCount").getAsInt());

            // A supplier the launcher's own catalogue records no protocol for is left out of the
            // list rather than offered as a card nobody can pick, and the directory's own view of
            // it — which does state a protocol — is listed instead.
            assertNull(find(providers, "fireworks"), "ids: " + ids(providers));
            JsonObject fireworks = find(providers, "fireworks-ai");
            assertNotNull(fireworks, "ids: " + ids(providers));
            assertFalse(fireworks.get("known").getAsBoolean());
            assertEquals("openai-completions", fireworks.get("protocol").getAsString());
            assertEquals("directory", fireworks.get("protocolSource").getAsString());
            assertEquals("@ai-sdk/openai-compatible", fireworks.get("npm").getAsString());
            assertEquals("fireworks-ai", fireworks.get("catalogId").getAsString());
            assertEquals(1, fireworks.get("modelCount").getAsInt());

            // And one found only through the alias file: the directory calls the gateway `vercel`,
            // and both the name the launcher shows and its protocol are the launcher's own.
            JsonObject gateway = find(providers, "vercel-ai-gateway");
            assertNotNull(gateway, "ids: " + ids(providers));
            assertTrue(gateway.get("known").getAsBoolean());
            assertEquals("Vercel AI Gateway", gateway.get("name").getAsString());
            assertEquals("vercel", gateway.get("catalogId").getAsString());
            assertEquals(1, gateway.get("modelCount").getAsInt());
            assertEquals("openai-completions", gateway.get("protocol").getAsString());
            assertEquals("dsh", gateway.get("protocolSource").getAsString());

            // A supplier only the directory knows: offered, but said to be unknown.
            JsonObject azure = find(providers, "azure");
            assertNotNull(azure, "a supplier only the directory knows is still offered: " + body);
            assertFalse(azure.get("known").getAsBoolean());
            assertFalse(azure.get("custom").getAsBoolean());
            assertFalse(azure.get("preferred").getAsBoolean());
            assertEquals("Azure OpenAI", azure.get("name").getAsString());
            assertEquals("https://example.openai.azure.com", azure.get("endpoint").getAsString());
            assertEquals("AZURE_API_KEY", azure.get("envVar").getAsString());
            assertEquals("@ai-sdk/azure", azure.get("npm").getAsString());
            assertEquals("azure", azure.get("catalogId").getAsString());
            assertEquals(1, azure.get("modelCount").getAsInt());
            assertTrue(azure.get("protocol").isJsonNull(),
                    "an SDK this launcher cannot sign for is offered but not routable");
            assertTrue(azure.get("protocolSource").isJsonNull());

            // Every supplier the launcher can route sorts before every one it cannot.
            int unknown = 0;
            for (int i = 0; i < providers.size(); i++) {
                if (!providers.get(i).getAsJsonObject().get("known").getAsBoolean()) {
                    unknown++;
                    assertTrue(i >= providers.size() - 2,
                            "the directory's own suppliers sort last: " + providers);
                }
            }
            assertEquals(2, unknown, "the fixture has exactly two suppliers dsh cannot claim");
            assertEquals("Azure OpenAI", providers.get(providers.size() - 2)
                    .getAsJsonObject().get("name").getAsString(), "and they sort by name");
            assertEquals("Fireworks AI", providers.get(providers.size() - 1)
                    .getAsJsonObject().get("name").getAsString());
        }
    }

    @Test
    void oneSuppliersModelsCarryTheirWindowAndTheirPrice() throws Exception {
        HttpServer stub = directoryStub();
        System.setProperty("hdsl.modelCatalog", stubUrl(stub));
        try (TestSupport.RunningServer running = startServer()) {
            HttpClient client = TestSupport.client();
            String base = running.baseUrl();

            HttpResponse<String> response = get(client, base + "/api/models/providers/deepseek?refresh=1");
            assertEquals(200, response.statusCode(), response.body());
            JsonObject body = json(response);
            assertEquals("deepseek", body.getAsJsonObject("vendor").get("id").getAsString());

            JsonArray models = body.getAsJsonArray("models");
            assertEquals(2, models.size(), body.toString());

            JsonObject chat = models.get(0).getAsJsonObject();
            assertEquals("deepseek-chat", chat.get("id").getAsString());
            assertEquals("DeepSeek Chat", chat.get("name").getAsString());
            assertEquals(128000, chat.get("context").getAsInt());
            assertEquals(8192, chat.get("output").getAsInt());
            assertFalse(chat.get("reasoning").getAsBoolean());
            assertEquals(0, chat.getAsJsonArray("reasoningEfforts").size());
            assertTrue(chat.get("toolCall").getAsBoolean());
            assertTrue(chat.get("attachment").getAsBoolean());
            assertTrue(chat.get("costInput").getAsJsonPrimitive().isNumber());
            assertEquals(0.27, chat.get("costInput").getAsDouble(), 0.0001);
            assertEquals(1.1, chat.get("costOutput").getAsDouble(), 0.0001);
            assertEquals("beta", chat.get("status").getAsString());

            // The model the directory described incompletely is still a model: what it did not say
            // is a null, not a zero.
            JsonObject reasoner = models.get(1).getAsJsonObject();
            assertEquals("deepseek-reasoner", reasoner.get("id").getAsString());
            assertEquals("DeepSeek Reasoner", reasoner.get("name").getAsString());
            assertTrue(reasoner.get("context").isJsonNull());
            assertTrue(reasoner.get("output").isJsonNull());
            assertTrue(reasoner.get("costInput").isJsonNull());
            assertTrue(reasoner.get("costOutput").isJsonNull());
            assertTrue(reasoner.get("status").isJsonNull());
            assertTrue(reasoner.get("reasoning").getAsBoolean());
            assertEquals(2, reasoner.getAsJsonArray("reasoningEfforts").size());
            assertEquals("low", reasoner.getAsJsonArray("reasoningEfforts").get(0).getAsString());
            assertEquals("high", reasoner.getAsJsonArray("reasoningEfforts").get(1).getAsString());
            assertFalse(reasoner.get("toolCall").getAsBoolean());
        }
    }

    @Test
    void aSupplierTheDirectoryDoesNotHoldIsAnEmptyListRatherThanA404() throws Exception {
        HttpServer stub = directoryStub();
        System.setProperty("hdsl.modelCatalog", stubUrl(stub));
        try (TestSupport.RunningServer running = startServer()) {
            HttpClient client = TestSupport.client();
            String base = running.baseUrl();

            HttpResponse<String> response = get(client, base + "/api/models/providers/ant-ling?refresh=1");
            assertEquals(200, response.statusCode(), response.body());
            JsonObject body = json(response);
            JsonObject vendor = body.getAsJsonObject("vendor");
            assertEquals("ant-ling", vendor.get("id").getAsString());
            assertTrue(vendor.get("known").getAsBoolean());
            assertTrue(vendor.get("catalogId").isJsonNull());
            assertEquals(0, vendor.get("modelCount").getAsInt());
            assertEquals("openai-completions", vendor.get("protocol").getAsString());
            assertEquals(0, body.getAsJsonArray("models").size(),
                    "a supplier with no models to offer is a text box, not an error");
        }
    }

    @Test
    void anIdNobodyKnowsIs404() throws Exception {
        HttpServer stub = directoryStub();
        System.setProperty("hdsl.modelCatalog", stubUrl(stub));
        try (TestSupport.RunningServer running = startServer()) {
            HttpClient client = TestSupport.client();
            HttpResponse<String> response = get(client,
                    running.baseUrl() + "/api/models/providers/no-such-supplier");
            assertEquals(404, response.statusCode(), response.body());
            assertNotNull(json(response).get("error"));
        }
    }

    @Test
    void theListIsServedFromMemoryAndRefreshAsksTheDirectoryAgain() throws Exception {
        HttpServer stub = directoryStub();
        System.setProperty("hdsl.modelCatalog", stubUrl(stub));
        try (TestSupport.RunningServer running = startServer()) {
            HttpClient client = TestSupport.client();
            String base = running.baseUrl();

            assertEquals(200, get(client, base + "/api/models/providers?refresh=1").statusCode());
            assertEquals(1, fetches.get(), "the first read asks the directory");

            HttpResponse<String> again = get(client, base + "/api/models/providers");
            assertEquals(200, again.statusCode());
            assertEquals("remote", json(again).get("source").getAsString());
            assertEquals(1, fetches.get(), "a second read inside the twelve-hour window is served from memory");

            HttpResponse<String> refreshed = get(client, base + "/api/models/providers?refresh=1");
            assertEquals(200, refreshed.statusCode(), refreshed.body());
            assertEquals(2, fetches.get(), "refresh=1 really asks the directory again");
            assertEquals("remote", json(refreshed).get("source").getAsString());
        }
    }

    @Test
    void afailedFetchAnswersFromTheKeptCopyAndSaysSo() throws Exception {
        HttpServer stub = directoryStub();
        System.setProperty("hdsl.modelCatalog", stubUrl(stub));
        try (TestSupport.RunningServer running = startServer()) {
            HttpClient client = TestSupport.client();
            String base = running.baseUrl();

            HttpResponse<String> first = get(client, base + "/api/models/providers?refresh=1");
            assertEquals(200, first.statusCode(), first.body());
            assertEquals("remote", json(first).get("source").getAsString());
            long fetchedAt = json(first).get("fetchedAt").getAsLong();

            // The directory goes away. The copy the fetch left on disk answers instead, and says
            // that is what it is — the models are still worth showing.
            stub.stop(0);
            stubs.remove(stub);

            HttpResponse<String> cached = get(client, base + "/api/models/providers?refresh=1");
            assertEquals(200, cached.statusCode(), cached.body());
            JsonObject body = json(cached);
            assertEquals("cache", body.get("source").getAsString());
            long keptAt = body.get("fetchedAt").getAsLong();
            assertTrue(Math.abs(keptAt - fetchedAt) < 60_000,
                    "the copy says when it was fetched, not when it was served");
            JsonObject deepseek = find(body.getAsJsonArray("providers"), "deepseek");
            assertNotNull(deepseek);
            assertEquals(2, deepseek.get("modelCount").getAsInt());

            // A degraded answer is held for minutes rather than for the twelve hours a good one
            // is: upstream is tried again soon, and until then the page is not asked to wait.
            HttpResponse<String> again = get(client, base + "/api/models/providers");
            assertEquals("cache", json(again).get("source").getAsString());
            assertEquals(keptAt, json(again).get("fetchedAt").getAsLong(),
                    "the degraded copy answers without asking upstream again");
        }
    }

    @Test
    void theFirstReadAnswersFromTheBuiltinListAndRefreshesBehindIt() throws Exception {
        HttpServer stub = directoryStub();
        System.setProperty("hdsl.modelCatalog", stubUrl(stub));
        try (TestSupport.RunningServer running = startServer()) {
            HttpClient client = TestSupport.client();
            String base = running.baseUrl();

            // No copy anywhere: the page gets the built-in suppliers now, and the five-megabyte
            // document is fetched by a thread nobody waits for.
            HttpResponse<String> first = get(client, base + "/api/models/providers");
            assertEquals(200, first.statusCode(), first.body());
            assertEquals("builtin", json(first).get("source").getAsString());
            assertNotNull(find(json(first).getAsJsonArray("providers"), "deepseek"));

            String source = "";
            for (int i = 0; i < 100; i++) {
                HttpResponse<String> second = get(client, base + "/api/models/providers");
                source = json(second).get("source").getAsString();
                if ("remote".equals(source)) {
                    break;
                }
                Thread.sleep(50);
            }
            assertEquals("remote", source, "the background refresh must land on the next read");
        }
    }

    @Test
    void aDirectoryThatWasNeverReadAndCannotBeReachedServesTheBuiltinList() throws Exception {
        // A port nothing listens on and an address never fetched: there is no copy on disk to fall
        // back on. That used to be a 502, which took the account page's whole supplier list with
        // it; the built-in suppliers are still worth showing, and the fetch is retried in the
        // background.
        int dead = TestSupport.freePort();
        System.setProperty("hdsl.modelCatalog", "http://127.0.0.1:" + dead + "/never-fetched.json");
        try (TestSupport.RunningServer running = startServer()) {
            HttpResponse<String> response = get(TestSupport.client(),
                    running.baseUrl() + "/api/models/providers");
            assertEquals(200, response.statusCode(), response.body());
            JsonObject body = json(response);
            assertEquals("builtin", body.get("source").getAsString());
            assertEquals(0L, body.get("fetchedAt").getAsLong());
            assertNull(body.get("error"), "a degraded list is not an error answer");
            JsonObject deepseek = find(body.getAsJsonArray("providers"), "deepseek");
            assertNotNull(deepseek, "the built-in suppliers must survive an unreachable directory");
            assertEquals(0, deepseek.get("modelCount").getAsInt());
        }
    }

    private static String ids(JsonArray providers) {
        List<String> ids = new ArrayList<>();
        for (int i = 0; i < providers.size(); i++) {
            ids.add(providers.get(i).getAsJsonObject().get("id").getAsString());
        }
        return ids.toString();
    }

    private static JsonObject find(JsonArray providers, String id) {
        for (int i = 0; i < providers.size(); i++) {
            JsonObject provider = providers.get(i).getAsJsonObject();
            if (id.equals(provider.get("id").getAsString())) {
                return provider;
            }
        }
        return null;
    }

    private static HttpResponse<String> get(HttpClient client, String url) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create(url)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private static JsonObject json(HttpResponse<String> response) {
        return JsonParser.parseString(response.body()).getAsJsonObject();
    }
}
