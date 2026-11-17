/*
 * HDSL
 * Copyright (C) 2026  HDSL contributors
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
package org.jackhuang.hmcl.dsh;

import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Tests for reading the models.dev directory: what is kept of a supplier and a model, which of the
/// two names a supplier answers to, and which wire a published npm package says it speaks.
///
/// The document is written by many hands — three thousand models from two hundred suppliers — so
/// most of these tests are about what happens when a member is missing, or spelled with the wrong
/// type: one odd field must cost that field, never the supplier around it. The fixture is a
/// miniature of the published shape (`src/test/resources/models-dev-sample.json`), carrying a
/// supplier for each of the three ways a supplier is matched plus one that cannot be matched at all.
class DshModelCatalogTest {

    @AfterEach
    void clearCatalogProperty() {
        System.clearProperty("hdsl.modelCatalog");
    }

    private static DshModelCatalog.Catalog sample() {
        return DshModelCatalog.parse(sampleText());
    }

    private static String sampleText() {
        try (InputStream stream = DshModelCatalogTest.class
                .getResourceAsStream("/models-dev-sample.json")) {
            assertNotNull(stream, "the fixture must be on the test classpath");
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new AssertionError("Could not read the fixture", e);
        }
    }

    @Test
    void whatTheDirectorySaysAboutAModelIsTrimmedToWhatThePageShows() {
        DshModelCatalog.Catalog catalog = sample();

        // Sorted by id rather than left in the document's order, and the member that is not a
        // supplier is not one.
        assertEquals(List.of("azure", "deepseek", "fireworks-ai", "togetherai", "vercel"),
                catalog.providers().stream().map(DshModelCatalog.Provider::id).toList());

        DshModelCatalog.Provider deepseek = catalog.byId("deepseek");
        assertNotNull(deepseek);
        assertEquals("DeepSeek", deepseek.name());
        assertEquals("https://api.deepseek.com", deepseek.endpoint());
        assertEquals(List.of("DEEPSEEK_API_KEY"), deepseek.env());
        assertEquals("@ai-sdk/openai-compatible", deepseek.npm());
        assertEquals("https://api-docs.deepseek.com", deepseek.doc());

        // Two models, although the fixture names three: an entry that is a string is not a model.
        assertEquals(2, deepseek.models().size());

        DshModelCatalog.Model chat = deepseek.models().get(0);
        assertEquals("deepseek-chat", chat.id());
        assertEquals("DeepSeek Chat", chat.name());
        assertEquals(128000, chat.context().intValue());
        assertEquals(8192, chat.output().intValue());
        assertFalse(chat.reasoning());
        assertTrue(chat.reasoningEfforts().isEmpty());
        assertTrue(chat.toolCall());
        assertTrue(chat.attachment());
        assertEquals(0.27, chat.costInput().doubleValue(), 0.0);
        assertEquals(1.1, chat.costOutput().doubleValue(), 0.0);
        assertEquals("beta", chat.status());
    }

    @Test
    void aMemberOfTheWrongTypeCostsThatMemberRatherThanTheModel() {
        // The reason this reading is defensive: the field a person needs — the price, the context
        // window — is only worth having if one publisher's typo does not take the whole page down.
        DshModelCatalog.Provider deepseek = sample().byId("deepseek");
        assertNotNull(deepseek);
        DshModelCatalog.Model reasoner = deepseek.models().get(1);

        // Named by the key of the `models` map, since the entry itself carries no id.
        assertEquals("deepseek-reasoner", reasoner.id());
        assertEquals("DeepSeek Reasoner", reasoner.name());

        assertNull(reasoner.context(), "a `limit` that is not an object states no context window");
        assertNull(reasoner.output());

        assertTrue(reasoner.reasoning());
        assertEquals(List.of("low", "high"), reasoner.reasoningEfforts(),
                "the effort option is the one that names efforts, and a blank effort is not one");

        assertFalse(reasoner.toolCall(), "a `tool_call` of the wrong type is not a tool call");
        assertFalse(reasoner.attachment());

        assertNull(reasoner.costInput(), "a price of the wrong type is no price");
        assertNull(reasoner.costOutput());
        assertNull(reasoner.status());
    }

    @Test
    void anEntryWithoutAnIdIsNamedByItsKeyAndMayStateNoAddress() {
        DshModelCatalog.Catalog catalog = sample();

        DshModelCatalog.Provider fireworks = catalog.byId("fireworks-ai");
        assertNotNull(fireworks, "the key of the `providers` map names the supplier");
        assertEquals("Fireworks AI", fireworks.name());
        assertEquals("https://api.fireworks.ai/inference/v1", fireworks.endpoint());
        assertEquals(DshVendor.hostOf("https://api.fireworks.ai/inference"),
                DshVendor.hostOf(fireworks.endpoint()),
                "the address is the one the launcher's own catalogue calls `fireworks`");
        assertEquals(1, fireworks.models().size());

        DshModelCatalog.Provider together = catalog.byId("togetherai");
        assertNull(together.endpoint(), "a supplier the directory publishes no address for");
        assertTrue(together.env().isEmpty(), "an `env` of the wrong type names no variable");
        assertNull(together.doc(), "a `doc` of the wrong type is no document");
        assertEquals("@ai-sdk/togetherai", together.npm());
        assertTrue(together.models().isEmpty());
    }

    @Test
    void theProtocolIsReadFromTheNpmPackageAndANullMeansThisLauncherCannotRouteIt() {
        // The whole map, because it is the one place a wire protocol is guessed: a package that
        // gains a line here gains a supplier the harness can be handed a route for.
        assertEquals("openai-completions", DshModelCatalog.protocolOf("@ai-sdk/openai-compatible"));
        assertEquals("openai-completions", DshModelCatalog.protocolOf("@ai-sdk/openai"));
        assertEquals("openai-completions", DshModelCatalog.protocolOf("@ai-sdk/cerebras"));
        assertEquals("openai-completions", DshModelCatalog.protocolOf("@ai-sdk/google"));
        assertEquals("openai-completions", DshModelCatalog.protocolOf("@ai-sdk/groq"));
        assertEquals("openai-completions", DshModelCatalog.protocolOf("@ai-sdk/mistral"));
        assertEquals("openai-completions", DshModelCatalog.protocolOf("@ai-sdk/xai"));
        assertEquals("openai-completions", DshModelCatalog.protocolOf("@openrouter/ai-sdk-provider"));
        assertEquals("openai-completions", DshModelCatalog.protocolOf("ai-gateway-provider"));
        assertEquals("anthropic-messages", DshModelCatalog.protocolOf("@ai-sdk/anthropic"));

        // What it answers is always one of the wires the harness accepts.
        assertTrue(DshVendor.APIS.contains(DshModelCatalog.protocolOf("@ai-sdk/anthropic")));

        // Named rather than guessed: an SDK signing its requests with a signature this launcher
        // cannot produce is a supplier it must not write a route for.
        assertNull(DshModelCatalog.protocolOf("@ai-sdk/azure"));
        assertNull(DshModelCatalog.protocolOf("@ai-sdk/amazon-bedrock"));
        assertNull(DshModelCatalog.protocolOf("@ai-sdk/togetherai"));
        assertNull(DshModelCatalog.protocolOf("some-package-nobody-mapped"));
        assertNull(DshModelCatalog.protocolOf(""));
        assertNull(DshModelCatalog.protocolOf(null));
    }

    @Test
    void aSupplierIsFoundByIdByHostAndByAlias() {
        DshModelCatalog.Catalog catalog = sample();

        // By the same id.
        assertEquals("deepseek", matchId(catalog, "deepseek", null));
        assertEquals("deepseek", matchId(catalog, "deepseek", "https://api.deepseek.com"));

        // By the host of the address: the launcher calls this supplier `fireworks` and the
        // directory calls it `fireworks-ai`, and the id alone would miss it.
        assertNull(catalog.byId("fireworks"));
        DshVendor fireworks = DshVendor.catalogue().stream()
                .filter(vendor -> vendor.id().equals("fireworks"))
                .findFirst().orElseThrow();
        assertEquals("fireworks-ai", matchId(catalog, fireworks.id(), fireworks.baseUrl()));

        // By the alias file: the gateway's address is not the one the directory publishes, so
        // neither the id nor the host would find it.
        assertNull(catalog.byId("vercel-ai-gateway"));
        assertNull(DshModelCatalog.match(catalog, null, "https://ai-gateway.vercel.sh"),
                "no address in the fixture answers to the gateway's, so only the alias can");
        assertEquals("vercel", matchId(catalog, "vercel-ai-gateway", "https://ai-gateway.vercel.sh"));
        assertEquals("togetherai", matchId(catalog, "together", null));

        // What no id, alias or host names is not a supplier.
        assertNull(DshModelCatalog.match(catalog, "no-such-supplier",
                "https://no-such-host.example.com/v1"));
        assertNull(DshModelCatalog.match(catalog, null, null));

        // The alias file itself, since it is data the matching above depends on.
        Map<String, String> aliases = DshModelCatalog.aliases();
        assertEquals("vercel", aliases.get("vercel-ai-gateway"));
        assertEquals("togetherai", aliases.get("together"));
        assertEquals("zai-coding-plan", aliases.get("zai-coding-cn"));
    }

    @Test
    void theKeptCopyIsNamedAfterTheAddressItWasReadFrom() {
        // Pointing the launcher at another directory must not overwrite the copy of this one, which
        // is what makes the two files differ.
        System.setProperty("hdsl.modelCatalog", "https://models.example.test/one/api.json");
        Path one = DshModelCatalog.cachedFile();
        System.setProperty("hdsl.modelCatalog", "https://models.example.test/two/api.json");
        Path two = DshModelCatalog.cachedFile();

        assertNotEquals(one, two);
        assertEquals(DshPluginCatalog.cacheDirectory(), one.getParent());
        assertTrue(one.getFileName().toString().startsWith("models-dev-"));
        assertTrue(one.getFileName().toString().endsWith(".json"));

        // The same address is the same copy, however many times it is asked for.
        System.setProperty("hdsl.modelCatalog", "https://models.example.test/one/api.json");
        assertEquals(one, DshModelCatalog.cachedFile());
    }

    @Test
    void aGzippedDirectoryIsFetchedAndDecoded() throws Exception {
        // 服务器上 api.json 默认 gzip 后约 460 KB，未压缩 6 MB。JDK 客户端既不会自己要
        // gzip 也不会解，之前那条超时线（20s）在慢网络上永远拉不完；这条用例把
        // 「要 gzip、解 gzip」钉住，免得哪天又退回明文下载。
        byte[] gzipped;
        try (java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
             java.util.zip.GZIPOutputStream gzip = new java.util.zip.GZIPOutputStream(buffer)) {
            gzip.write(sampleText().getBytes(StandardCharsets.UTF_8));
            gzip.finish();
            gzipped = buffer.toByteArray();
        }
        com.sun.net.httpserver.HttpServer server = com.sun.net.httpserver.HttpServer
                .create(new java.net.InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api.json", exchange -> {
            exchange.getResponseHeaders().add("Content-Encoding", "gzip");
            exchange.sendResponseHeaders(200, gzipped.length);
            exchange.getResponseBody().write(gzipped);
            exchange.close();
        });
        server.start();
        try {
            System.setProperty("hdsl.modelCatalog",
                    "http://127.0.0.1:" + server.getAddress().getPort() + "/api.json");
            DshModelCatalog.Catalog catalog = DshModelCatalog.fetch();
            assertFalse(catalog.providers().isEmpty(), "the gzipped body must decode into suppliers");
        } finally {
            server.stop(0);
        }
    }

    /// Finds a supplier and fails with what was asked for rather than with a null pointer.
    private static String matchId(DshModelCatalog.Catalog catalog, @Nullable String vendorId,
                                  @Nullable String endpoint) {
        DshModelCatalog.Provider matched = DshModelCatalog.match(catalog, vendorId, endpoint);
        assertNotNull(matched, "expected a supplier for `" + vendorId + "` / `" + endpoint + "`");
        return matched.id();
    }
}
