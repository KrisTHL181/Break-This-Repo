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
package org.jackhuang.hmcl.web.chat;

import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// 搜索接的是两家真实形状：博查 data.webPages.value，DeepSeek results；失败要说人话。
class ChatSearchServiceTest {

    @AfterEach
    void clear() {
        System.clearProperty("hdsl.search.bocha");
        System.clearProperty("hdsl.search.deepseek");
        System.clearProperty("hdsl.fetch.deepseek");
    }

    @Test
    void bochaResultsAreNormalisedAndCached() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        AtomicReference<String> body = new AtomicReference<>("");
        HttpServer server = server(exchange -> {
            calls.incrementAndGet();
            body.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            String json = """
                    {"code":200,"data":{"webPages":{"value":[
                      {"name":"DeepSeek V4","url":"https://example.com/v4","snippet":"short",
                       "summary":"long summary","siteName":"Example","datePublished":"2026-06-01"}
                    ]}}}""";
            exchange.sendResponseHeaders(200, json.getBytes(StandardCharsets.UTF_8).length);
            exchange.getResponseBody().write(json.getBytes(StandardCharsets.UTF_8));
            exchange.close();
        });
        System.setProperty("hdsl.search.bocha", url(server, "/search"));
        try {
            ChatSearchService service = new ChatSearchService();
            ChatSettings.Settings settings = settings("bocha", "bocha-key");
            ChatSearchService.Result result = service.search(settings, "DeepSeek V4");
            assertEquals(1, result.hits().size());
            ChatSearchService.Hit hit = result.hits().get(0);
            assertEquals("DeepSeek V4", hit.title());
            assertEquals("https://example.com/v4", hit.url());
            assertEquals("long summary", hit.snippet());
            assertEquals("Example", hit.site());
            assertTrue(body.get().contains("\"count\":5"));
            assertTrue(body.get().contains("\"query\":\"DeepSeek V4\""));

            // 第二句相同查询命中缓存，不再发请求
            service.search(settings, "DeepSeek V4");
            assertEquals(1, calls.get());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void deepseekResultsAreNormalised() throws Exception {
        HttpServer server = server(exchange -> {
            String json = """
                    {"results":[{"title":"V4 released","url":"https://example.com/release",
                      "snippet":"s","summary":"summary text","site_name":"Example News",
                      "date":"2026-06-01T00:00:00+00:00"}]}""";
            exchange.sendResponseHeaders(200, json.getBytes(StandardCharsets.UTF_8).length);
            exchange.getResponseBody().write(json.getBytes(StandardCharsets.UTF_8));
            exchange.close();
        });
        System.setProperty("hdsl.search.deepseek", url(server, "/search"));
        try {
            ChatSearchService.Result result = new ChatSearchService()
                    .search(settings("deepseek", "deepseek-key"), "v4");
            assertEquals(1, result.hits().size());
            assertEquals("summary text", result.hits().get(0).snippet());
            assertEquals("Example News", result.hits().get(0).site());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void zhipuTavilyExaAndBraveShapesAreNormalised() throws Exception {
        HttpServer zhipu = server(exchange -> {
            String json = """
                    {"search_result":[{"title":"智谱结果","link":"https://example.com/z",
                      "content":"智谱摘要","media":"搜狐","publish_date":"2025-05-23"}]}""";
            exchange.sendResponseHeaders(200, json.getBytes(StandardCharsets.UTF_8).length);
            exchange.getResponseBody().write(json.getBytes(StandardCharsets.UTF_8));
            exchange.close();
        });
        System.setProperty("hdsl.search.zhipu", url(zhipu, "/search"));
        try {
            ChatSearchService.Hit hit = new ChatSearchService()
                    .search(settings("zhipu", "zhipu-key"), "财经").hits().get(0);
            assertEquals("智谱结果", hit.title());
            assertEquals("https://example.com/z", hit.url());
            assertEquals("搜狐", hit.site());
        } finally {
            zhipu.stop(0);
        }

        HttpServer tavily = server(exchange -> {
            String json = """
                    {"results":[{"title":"T","url":"https://example.com/t","content":"tavily 正文",
                      "published_date":"2026-01-01"}]}""";
            exchange.sendResponseHeaders(200, json.getBytes(StandardCharsets.UTF_8).length);
            exchange.getResponseBody().write(json.getBytes(StandardCharsets.UTF_8));
            exchange.close();
        });
        System.setProperty("hdsl.search.tavily", url(tavily, "/search"));
        try {
            assertEquals("tavily 正文", new ChatSearchService()
                    .search(settings("tavily", "k"), "q").hits().get(0).snippet());
        } finally {
            tavily.stop(0);
        }

        HttpServer exa = server(exchange -> {
            String json = """
                    {"results":[{"title":"E","url":"https://example.com/e","text":"exa 正文",
                      "publishedDate":"2026-01-02"}]}""";
            exchange.sendResponseHeaders(200, json.getBytes(StandardCharsets.UTF_8).length);
            exchange.getResponseBody().write(json.getBytes(StandardCharsets.UTF_8));
            exchange.close();
        });
        System.setProperty("hdsl.search.exa", url(exa, "/search"));
        try {
            assertEquals("exa 正文", new ChatSearchService()
                    .search(settings("exa", "k"), "q").hits().get(0).snippet());
        } finally {
            exa.stop(0);
        }

        HttpServer brave = server(exchange -> {
            String json = """
                    {"web":{"results":[{"title":"B","url":"https://example.com/b",
                      "description":"brave 摘要"}]}}""";
            exchange.sendResponseHeaders(200, json.getBytes(StandardCharsets.UTF_8).length);
            exchange.getResponseBody().write(json.getBytes(StandardCharsets.UTF_8));
            exchange.close();
        });
        System.setProperty("hdsl.search.brave", url(brave, "/search"));
        try {
            ChatSearchService.Hit hit = new ChatSearchService()
                    .search(settings("brave", "k"), "q").hits().get(0);
            assertEquals("brave 摘要", hit.snippet());
            assertEquals("example.com", hit.site());
        } finally {
            brave.stop(0);
        }
    }

    @Test
    void keysAreKeptPerProvider() throws Exception {
        java.nio.file.Path dir = Files.createTempDirectory("hdsl-search-keys");
        ChatSettings.Settings defaults = ChatSettings.defaults(dir);
        JsonObject bocha = new JsonObject();
        bocha.addProperty("searchProvider", "bocha");
        bocha.addProperty("searchApiKey", "bocha-key");
        ChatSettings.Settings first = ChatSettings.merge(defaults, bocha, defaults);
        assertEquals("bocha-key", first.searchKey("bocha"));

        JsonObject zhipu = new JsonObject();
        zhipu.addProperty("searchProvider", "zhipu");
        zhipu.addProperty("searchApiKey", "zhipu-key");
        ChatSettings.Settings second = ChatSettings.merge(first, zhipu, defaults);
        assertEquals("zhipu-key", second.searchKey("zhipu"));
        assertEquals("bocha-key", second.searchKey("bocha"), "切服务商不该覆盖另一家的 key");
    }

    @Test
    void blockedKeysAndEmptyResultsAreReported() throws Exception {
        HttpServer server = server(exchange -> {
            String json = "{\"error\":{\"message\":\"invalid api key\"}}";
            exchange.sendResponseHeaders(401, json.getBytes(StandardCharsets.UTF_8).length);
            exchange.getResponseBody().write(json.getBytes(StandardCharsets.UTF_8));
            exchange.close();
        });
        System.setProperty("hdsl.search.bocha", url(server, "/search"));
        try {
            ChatSearchService service = new ChatSearchService();
            ChatException error = assertThrows(ChatException.class,
                    () -> service.search(settings("bocha", "bad-key"), "x"));
            assertEquals(400, error.status());
            assertTrue(error.getMessage().contains("401"));
        } finally {
            server.stop(0);
        }

        ChatException missing = assertThrows(ChatException.class,
                () -> new ChatSearchService().search(settings("bocha", null), "x"));
        assertEquals("search_not_configured", missing.code());
    }

    @Test
    void theSystemBlockMarksResultsAsUntrustedAndNumbersThem() {
        ChatSearchService.Result result = new ChatSearchService.Result("q", "bocha", java.util.List.of(
                new ChatSearchService.Hit("A", "https://a.example", "sa", "A 站", null),
                new ChatSearchService.Hit("B", "https://b.example", "sb", null, null)));
        String block = ChatSearchService.systemBlock(result);
        assertTrue(block.contains("不可信"));
        assertTrue(block.contains("1. A"));
        assertTrue(block.contains("2. B"));
        assertTrue(block.contains("https://a.example"));
        assertTrue(block.length() < 8_000);
    }

    @Test
    void deepSeekFetchReturnsPageText() throws Exception {
        HttpServer server = server(exchange -> {
            String json = "{\"title\":\"Example\",\"content\":\"这是正文内容。\"}";
            exchange.sendResponseHeaders(200, json.getBytes(StandardCharsets.UTF_8).length);
            exchange.getResponseBody().write(json.getBytes(StandardCharsets.UTF_8));
            exchange.close();
        });
        System.setProperty("hdsl.fetch.deepseek", url(server, "/fetch"));
        String result = new ChatSearchService()
                .scrape(settings("deepseek", "k"), "https://example.com/a");
        assertTrue(result.contains("这是正文内容"));
        assertTrue(result.contains("Example"));
    }

    @Test
    void bochaRefusesToScrape() throws Exception {
        ChatException error = assertThrows(ChatException.class,
                () -> new ChatSearchService().scrape(settings("bocha", "k"),
                        "https://example.com/a"));
        assertEquals("scrape_not_supported", error.code());
    }

    @Test
    void scrapeRejectsNonHttpUrls() throws Exception {
        ChatException error = assertThrows(ChatException.class,
                () -> new ChatSearchService().scrape(settings("deepseek", "k"), "file:///etc/passwd"));
        assertEquals("bad_url", error.code());
    }

    private static ChatSettings.Settings settings(String provider, String key) throws Exception {
        java.nio.file.Path dir = Files.createTempDirectory("hdsl-search-settings");
        ChatSettings.Settings defaults = ChatSettings.defaults(dir);
        JsonObject patch = new JsonObject();
        patch.addProperty("searchProvider", provider);
        patch.addProperty("searchMaxResults", 5);
        if (key == null) {
            patch.add("searchApiKey", com.google.gson.JsonNull.INSTANCE);
        } else {
            patch.addProperty("searchApiKey", key);
        }
        return ChatSettings.merge(defaults, patch, defaults);
    }

    private interface Handler {
        void handle(com.sun.net.httpserver.HttpExchange exchange) throws java.io.IOException;
    }

    private static HttpServer server(Handler handler) throws java.io.IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            try {
                handler.handle(exchange);
            } catch (RuntimeException e) {
                exchange.sendResponseHeaders(500, -1);
                exchange.close();
            }
        });
        server.start();
        return server;
    }

    private static String url(HttpServer server, String path) {
        return "http://127.0.0.1:" + server.getAddress().getPort() + path;
    }
}
