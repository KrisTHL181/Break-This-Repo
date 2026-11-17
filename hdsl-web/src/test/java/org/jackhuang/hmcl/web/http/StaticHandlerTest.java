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
package org.jackhuang.hmcl.web.http;

import org.jackhuang.hmcl.web.TestSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The SPA shell: `/` serves the built React bundle's index.html, and any
/// client-side route falls back to it too. Unauthenticated by design — the
/// login page must be reachable before a session exists.
class StaticHandlerTest {

    @TempDir
    Path dataDir;

    /// HDSL_BASE_PATH（面板被反代在子路径挂载点下）：注入的 <base> 与
    /// window.__HDSL_BASE__ 让 SPA 的相对路径解析进挂载点，index.html 里
    /// 根绝对的资产引用必须被加前缀——否则子路径部署白屏。
    @Test
    void subpathMountInjectsBaseMachinery() throws Exception {
        try (TestSupport.RunningServer running = TestSupport.start(dataDir,
                Map.of("HDSL_BASE_PATH", "/panel/"),
                config -> config.bindHost = "127.0.0.1")) {
            var client = TestSupport.client();
            HttpResponse<String> index = client.send(HttpRequest.newBuilder()
                    .uri(URI.create(running.baseUrl() + "/")).GET().build(), HttpResponse.BodyHandlers.ofString());
            assertEquals(200, index.statusCode());
            // 归一化：尾斜杠被剥掉
            assertTrue(index.body().contains("<base href=\"/panel/\">"), "base href must be injected");
            assertTrue(index.body().contains("window.__HDSL_BASE__=\"/panel\";"), "base script must be injected");
            assertFalse(index.body().contains("href=\"/assets"), "asset refs must leave the root");
            assertTrue(index.body().contains("href=\"/panel/assets"), "asset refs must carry the base");
        }
    }

    /// 根挂载（默认）：base 标签给前端相对路径兜底，资产引用保持根绝对不变。
    @Test
    void rootMountKeepsRootAbsoluteAssets() throws Exception {
        try (TestSupport.RunningServer running = TestSupport.start(dataDir, Map.of(),
                config -> config.bindHost = "127.0.0.1")) {
            var client = TestSupport.client();
            HttpResponse<String> index = client.send(HttpRequest.newBuilder()
                    .uri(URI.create(running.baseUrl() + "/")).GET().build(), HttpResponse.BodyHandlers.ofString());
            assertEquals(200, index.statusCode());
            assertTrue(index.body().contains("<base href=\"/\">"));
            assertTrue(index.body().contains("window.__HDSL_BASE__=\"\";"));
            assertTrue(index.body().contains("href=\"/assets"), "root mount must not rewrite asset refs");
        }
    }

    @Test
    void indexAndSpaFallbacksServeTheShell() throws Exception {
        try (TestSupport.RunningServer running = TestSupport.start(dataDir, Map.of(),
                config -> config.bindHost = "127.0.0.1")) {
            var client = TestSupport.client();
            String base = running.baseUrl();

            HttpResponse<String> index = client.send(
                    HttpRequest.newBuilder(URI.create(base + "/")).GET().build(),
                    HttpResponse.BodyHandlers.ofString());
            assertEquals(200, index.statusCode());
            assertTrue(index.headers().firstValue("content-type").orElse("").startsWith("text/html"));
            assertTrue(index.body().contains("Hello DeepSeek"), "the panel shell must be served");
            assertTrue(index.body().contains("id=\"root\""), "the React mount point must be present");

            for (String route : new String[]{"/login", "/instances/3", "/some/deep/spa/route"}) {
                HttpResponse<String> fallback = client.send(
                        HttpRequest.newBuilder(URI.create(base + route)).GET().build(),
                        HttpResponse.BodyHandlers.ofString());
                assertEquals(200, fallback.statusCode(), route + " must fall back to index.html");
                assertEquals(index.body(), fallback.body(), route + " must serve the same shell");
                assertTrue(fallback.headers().firstValue("content-type").orElse("").startsWith("text/html"),
                        route + " must fall back as HTML, not as octet-stream");
            }

            // The real bundles must come back as their own content. Mounted at
            // "/", the path arrives as servletPath (pathInfo is null); reading
            // only getPathInfo() used to resolve every request to the shell —
            // the panel rendered as a black page because the JS was HTML.
            // The panel is served from the root (vite `base: "/"`), so the shell
            // references its bundles with root-absolute paths.
            var asset = java.util.regex.Pattern
                    .compile("src=\"(/assets/[^\"]+)\"")
                    .matcher(index.body());
            assertTrue(asset.find(), "index.html must reference the built JS bundle");
            HttpResponse<String> bundle = client.send(
                    HttpRequest.newBuilder(URI.create(base + asset.group(1))).GET().build(),
                    HttpResponse.BodyHandlers.ofString());
            assertEquals(200, bundle.statusCode(), "the referenced bundle must be served");
            assertTrue(bundle.headers().firstValue("content-type").orElse("").startsWith("text/javascript"),
                    "a real asset must be served as JavaScript, not as the HTML shell");
            assertTrue(bundle.body().length() > index.body().length(),
                    "the bundle must be its own file, not a copy of the shell");

            // A stale hashed name after a deploy is expected: it falls back to
            // the shell, which is how the SPA recovers.
            HttpResponse<String> stale = client.send(
                    HttpRequest.newBuilder(URI.create(base + "/assets/index-gone.js")).GET().build(),
                    HttpResponse.BodyHandlers.ofString());
            assertEquals(200, stale.statusCode(), "a stale asset name must fall back to the shell");

            // API paths must not fall through to the SPA shell.
            HttpResponse<String> api = client.send(
                    HttpRequest.newBuilder(URI.create(base + "/api/health")).GET().build(),
                    HttpResponse.BodyHandlers.ofString());
            assertEquals(200, api.statusCode());
            assertTrue(api.headers().firstValue("content-type").orElse("").startsWith("application/json"));
        }
    }

    /// Images the panel ships under `assets-img/`. The content-type table once
    /// lacked `jpg`/`jpeg`, so every wallpaper went out as
    /// `application/octet-stream` — browsers sniff and still painted it, which is
    /// exactly why it went unnoticed until a deployment looked wrong.
    @Test
    void imagesKeepTheirOwnContentType() throws Exception {
        try (TestSupport.RunningServer running = TestSupport.start(dataDir, Map.of(),
                config -> config.bindHost = "127.0.0.1")) {
            var client = TestSupport.client();
            String base = running.baseUrl();

            HttpResponse<byte[]> wall = client.send(
                    HttpRequest.newBuilder(URI.create(base + "/assets-img/wallpapers/2021-08-26.jpg")).GET().build(),
                    HttpResponse.BodyHandlers.ofByteArray());
            assertEquals(200, wall.statusCode(), "the default wallpaper must be served");
            assertTrue(wall.headers().firstValue("content-type").orElse("").startsWith("image/jpeg"),
                    "a .jpg asset must be served as image/jpeg, not as octet-stream");
            assertTrue(wall.body().length > 1024,
                    "the wallpaper must be its own bytes, not the HTML shell");

            HttpResponse<byte[]> icon = client.send(
                    HttpRequest.newBuilder(URI.create(base + "/assets-img/icon.png")).GET().build(),
                    HttpResponse.BodyHandlers.ofByteArray());
            assertEquals(200, icon.statusCode(), "the launcher icon must be served");
            assertTrue(icon.headers().firstValue("content-type").orElse("").startsWith("image/png"),
                    "a .png asset must keep image/png");
        }
    }
}
