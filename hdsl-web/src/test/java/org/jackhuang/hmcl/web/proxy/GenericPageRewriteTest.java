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
package org.jackhuang.hmcl.web.proxy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The generic half of the page rewrite: single-page apps that assume they own
/// the origin (root-absolute asset references, no `<base>` tag) — the Kimi Code
/// and OpenCode web UIs — must come out of the rewrite pointing at the mount,
/// with the runtime shim installed before any module script runs. The fixtures
/// are the real index pages those tools served, captured during the brand
/// integration spike.
class GenericPageRewriteTest {

    private static final String MOUNT = "/i/demo123/";

    @Test
    void kimiIndexGetsTheMountAndTheShim() {
        String rewritten = rewrite(resource("brand-pages/kimi-index.html"));

        assertTrue(rewritten.contains("src=\"" + MOUNT + "boot.js\""), rewritten);
        assertTrue(rewritten.contains("src=\"" + MOUNT + "assets/index-CiJ6FDOC.js\""), rewritten);
        assertTrue(rewritten.contains("href=\"" + MOUNT + "assets/index-zbwnJKhO.css\""), rewritten);
        assertTrue(rewritten.contains("href=\"" + MOUNT + "favicon.ico\""), rewritten);
        assertShim(rewritten);
        assertNoDoublePrefix(rewritten);
    }

    @Test
    void opencodeIndexGetsTheMountAndBothShims() {
        // Path-routed clients get the router shim; the proxy is told so by the
        // brand flag at serve time (see InstanceProxyServlet#needsRouterShim),
        // and which contract that shim carries is what the probe decided.
        String rewritten = InstanceProxyServlet.rewritePage(
                resource("brand-pages/opencode-index.html"), MOUNT, InstanceProxyServlet.RouterShim.MOUNT);

        assertTrue(rewritten.contains("src=\"" + MOUNT + "assets/index-CjbuCoME.js\""), rewritten);
        assertTrue(rewritten.contains("href=\"" + MOUNT + "assets/index-DLiUNAg_.css\""), rewritten);
        assertTrue(rewritten.contains("href=\"" + MOUNT + "favicon-v3.svg\""), rewritten);
        // The manifest is stripped like dsh's: the browser fetches it without
        // credentials, so through the mount it is a 401 from the session gate
        // and nothing else (measured: two 401s per OpenCode page load).
        assertFalse(rewritten.contains("site.webmanifest"), rewritten);
        assertShim(rewritten);
        assertRouterShim(rewritten, InstanceProxyServlet.RouterShim.MOUNT);
        assertNoDoublePrefix(rewritten);
    }

    @Test
    void shimIsInjectedInsideHeadBeforeAnyAsset() {
        String rewritten = rewrite(resource("brand-pages/kimi-index.html"));
        String tag = "<script src=\"" + MOUNT + InstanceProxyServlet.SHIM_RESOURCE + "\"></script>";
        int head = rewritten.indexOf("<head>");
        int shim = rewritten.indexOf(tag);
        int firstAsset = rewritten.indexOf("src=\"" + MOUNT);
        assertTrue(head >= 0 && shim > head, "shim inside <head>: " + rewritten);
        assertTrue(shim < firstAsset, "shim before the first rewritten asset");
    }

    @Test
    void dshShapedPageKeepsItsExactTreatment() {
        String dsh = "<html><head><base href=\"/\">"
                + "<script src=\"/plugins/a/client.js\"></script></head><body></body></html>";
        String rewritten = InstanceProxyServlet.rewritePage(dsh, MOUNT);

        assertTrue(rewritten.contains("<base href=\"" + MOUNT + "\">"), rewritten);
        assertTrue(rewritten.contains("src=\"" + MOUNT + "plugins/a/client.js\""), rewritten);
        // The dsh branch predates the CSP finding and inlines the shim, which
        // dsh's responses (no CSP) accept; the generic branch externalises it.
        assertTrue(rewritten.contains("const mount=\"" + MOUNT + "\""), rewritten);
    }

    @Test
    void aRelativeOnlyPageIsUntouched() {
        String plain = "<html><head><script src=\"./app.js\"></script></head>"
                + "<body><a href=\"page\">x</a></body></html>";
        assertEquals(plain, InstanceProxyServlet.rewritePage(plain, MOUNT));
    }

    /// Claude Web's page is the mirror image of Kimi's: every reference is
    /// relative (Vite `base: './'`), so neither rewrite shape matches — but its
    /// one channel is an absolute `wss://<host>/ws` built at runtime, which
    /// needs the shim. Measured 2026-10-12: without this, the page renders and
    /// the UI reports "connection closed" while the panel's relay never sees
    /// the upgrade at all.
    @Test
    void aRelativeBrandPageStillGetsTheRuntimeShim() {
        String page = "<html><head>"
                + "<link rel=\"manifest\" href=\"manifest.webmanifest\" />"
                + "<script type=\"module\" crossorigin src=\"./assets/index-DY0CX39q.js\"></script>"
                + "</head><body></body></html>";

        String rewritten = InstanceProxyServlet.rewritePage(
                page, MOUNT, InstanceProxyServlet.RouterShim.NONE, true);

        assertShim(rewritten);
        // Relative references stay untouched: they already resolve in the mount.
        assertTrue(rewritten.contains("src=\"./assets/index-DY0CX39q.js\""), rewritten);
        // The manifest is dropped for the same reason the other branch drops it:
        // through the mount it is a credential-less fetch answered 401.
        assertFalse(rewritten.contains("manifest.webmanifest"), rewritten);
        assertNoDoublePrefix(rewritten);
    }

    @Test
    void theRuntimeShimIsNotInjectedForNonBrandPages() {
        String page = "<html><head><script src=\"./app.js\"></script></head></html>";

        assertEquals(page, InstanceProxyServlet.rewritePage(
                page, MOUNT, InstanceProxyServlet.RouterShim.NONE, false));
    }

    /// The real Claude Web page (0.2.1, captured 2026-10-12) — the shape the
    /// third rewrite branch exists for: Vite `base: './'`, so every reference
    /// is relative and neither earlier branch matches, while the client's one
    /// channel is an absolute `wss://<host>/ws` that only the shim can keep
    /// inside the mount.
    @Test
    void claudeWebIndexGetsTheRuntimeShimWithoutTouchingItsRelativeReferences() {
        String rewritten = InstanceProxyServlet.rewritePage(
                resource("brand-pages/claude-web-index.html"), MOUNT,
                InstanceProxyServlet.RouterShim.NONE, true);

        assertShim(rewritten);
        // No reference rewriting happened: they were relative to begin with.
        assertTrue(rewritten.contains("src=\"./assets/index-DY0CX39q.js\""), rewritten);
        assertTrue(rewritten.contains("href=\"./assets/index-BuyiUOyq.css\""), rewritten);
        // The manifest is dropped for the same reason the other branch drops it.
        assertFalse(rewritten.contains("manifest.webmanifest"), rewritten);
        assertNoDoublePrefix(rewritten);
    }

    @Test
    void theRouterShimIsReferencedOnlyWhenAsked() {
        String page = "<html><head><script src=\"/assets/a.js\"></script></head></html>";

        assertFalse(InstanceProxyServlet.rewritePage(page, MOUNT, InstanceProxyServlet.RouterShim.NONE)
                .contains(InstanceProxyServlet.ROUTER_RESOURCE));
        String mounted = InstanceProxyServlet.rewritePage(page, MOUNT, InstanceProxyServlet.RouterShim.MOUNT);
        assertTrue(mounted.contains(InstanceProxyServlet.ROUTER_RESOURCE + "\"></script>"), mounted);
        assertFalse(mounted.contains("?mode=root"), mounted);
        // The fallback names the same resource, with the mode that gives the
        // address bar back to the panel root.
        assertTrue(InstanceProxyServlet.rewritePage(page, MOUNT, InstanceProxyServlet.RouterShim.ROOT)
                .contains(InstanceProxyServlet.ROUTER_RESOURCE + "?mode=root\"></script>"));
    }

    @Test
    void theShimResourceIsServedByThePanelItself(@TempDir Path dataDir) throws Exception {
        // No instance named "whatever" exists — the endpoint must still answer,
        // because the page referencing it is only ever served alongside a
        // running instance, and CSP pages need the script before anything else.
        try (org.jackhuang.hmcl.web.TestSupport.RunningServer running =
                     org.jackhuang.hmcl.web.TestSupport.start(dataDir, java.util.Map.of(), config -> {
                         config.bindHost = "127.0.0.1";
                         config.auth.disabled = true;
                     })) {
            java.net.http.HttpResponse<String> response = java.net.http.HttpClient.newHttpClient().send(
                    java.net.http.HttpRequest.newBuilder(java.net.URI.create(
                                    running.baseUrl() + "/i/whatever/" + InstanceProxyServlet.SHIM_RESOURCE))
                            .GET().build(),
                    java.net.http.HttpResponse.BodyHandlers.ofString());

            assertEquals(200, response.statusCode(), response.body());
            assertTrue(response.headers().firstValue("Content-Type").orElse("")
                    .contains("javascript"), response.headers().toString());
            assertTrue(response.body().contains("const mount=\"/i/whatever/\""), response.body());

            // The router shim answers with the contract that keeps the mount: it
            // defines what the patched bundle calls, and puts the mount back on
            // what the client writes.
            java.net.http.HttpResponse<String> router = get(running.baseUrl(),
                    "/i/whatever/" + InstanceProxyServlet.ROUTER_RESOURCE);
            assertEquals(200, router.statusCode(), router.body());
            assertTrue(router.body().contains(
                    "__hdslUnmount=(path)=>path.indexOf(mount)===0?path.slice(mount.length-1):path"),
                    router.body());
            assertTrue(router.body().contains("history.pushState=wrap(push)"), router.body());

            // …and, asked for the fallback, with the one that hands the address
            // back to the panel root instead.
            java.net.http.HttpResponse<String> rooted = get(running.baseUrl(),
                    "/i/whatever/" + InstanceProxyServlet.ROUTER_RESOURCE + "?mode=root");
            assertEquals(200, rooted.statusCode(), rooted.body());
            assertTrue(rooted.body().contains("history.pushState=swallow(push)"), rooted.body());
        }
    }

    @Test
    void theRouterReadIsMadeMountBlindAndNothingElseIsTouched() {
        // The expression the patch is anchored on is Solid Router's, measured in
        // OpenCode 1.18.32 and 1.18.35 — see InstanceProxyServlet#patchBrandScript.
        // The second half is the client's boot-time `auth_token` cleanup: it reads
        // `location.pathname` too — bare, no `window.` — and has to come out of the
        // patch untouched, because stripping the mount there would drop it from the
        // very address that cleanup writes back.
        byte[] bundle = ("const r=window.location.pathname.replace(/^\\/+/,\"/\")+window.location.search;"
                + "const clean=()=>history.replaceState(null,\"\",location.pathname+location.hash);")
                .getBytes(StandardCharsets.UTF_8);

        byte[] patched = InstanceProxyServlet.patchBrandScript(bundle, MOUNT);

        assertEquals("const r=(window.__hdslUnmount"
                        + "?window.__hdslUnmount(window.location.pathname):window.location.pathname)"
                        + ".replace(/^\\/+/,\"/\")+window.location.search;"
                        + "const clean=()=>history.replaceState(null,\"\",location.pathname+location.hash);",
                new String(patched, StandardCharsets.UTF_8));
    }

    @Test
    void theChunkBaseAndTheAssetLiteralsAreGivenTheMount() {
        // Both anchors as OpenCode 1.18.35 carries them: Vite's preload helper —
        // `const ote="modulepreload",ate=function(e){return"/"+e}` — which turns
        // every lazily imported dialog into a request at the panel root, and the
        // root-absolute asset literals the bundle keeps for its worker and icons.
        byte[] bundle = ("const ate=function(e){return\"/\"+e};"
                + "const worker=\"/assets/markdown.worker-Bu_Dc9RP.js\";")
                .getBytes(StandardCharsets.UTF_8);

        byte[] patched = InstanceProxyServlet.patchBrandScript(bundle, MOUNT);

        assertEquals("const ate=function(e){return\"" + MOUNT + "\"+e};"
                        + "const worker=\"" + MOUNT + "assets/markdown.worker-Bu_Dc9RP.js\";",
                new String(patched, StandardCharsets.UTF_8));
    }

    @Test
    void zcodeMountDetectionIsRelaxedWithoutTouchingAnythingElse() {
        // ZCode / Kimi Code 的 web 端原生认识 /i/<id>/ 挂载（API base 来自
        // location.pathname.match(/^\/i\/[^/]+\//)），但正则锚在路径开头——
        // 面板在子路径后（/hdsl-web/i/<id>/）检测失配，API/WS 打到源站根。
        // 只放宽这一个正则，同包里的其它代码（包括第二个不同的挂载检测实现）不动。
        byte[] bundle = ("function Y4n(){let e=window.location.pathname.match(/^\\/i\\/[^/]+\\//);"
                + "return e?e[0].slice(0,-1):``}"
                + "const other=window.location.pathname.replace(/^\\/+/,\"/\");")
                .getBytes(StandardCharsets.UTF_8);

        byte[] patched = InstanceProxyServlet.patchZcodeScript(bundle);

        assertEquals("function Y4n(){let e=window.location.pathname.match(/^.*?\\/i\\/[^/]+\\//);"
                        + "return e?e[0].slice(0,-1):``}"
                        + "const other=window.location.pathname.replace(/^\\/+/,\"/\");",
                new String(patched, StandardCharsets.UTF_8));
    }

    @Test
    void aBundleWithNoneOfThoseAnchorsComesBackUntouched() {
        byte[] plain = "const a=1;".getBytes(StandardCharsets.UTF_8);
        assertArrayEquals(plain, InstanceProxyServlet.patchBrandScript(plain, MOUNT));

        // Two router reads is not a bundle this servlet understands: guessing
        // which of them the router takes would be worse than handing the address
        // back — so that one anchor stays untouched. The asset edits still apply
        // on their own: a client that cannot load its chunks has no app at all,
        // whatever its address bar does.
        byte[] ambiguous = ("window.location.pathname.replace( and window.location.pathname.replace("
                + "const w=\"/assets/x.js\";").getBytes(StandardCharsets.UTF_8);
        assertFalse(InstanceProxyServlet.hasRouterPathRead(ambiguous));
        assertEquals("window.location.pathname.replace( and window.location.pathname.replace("
                        + "const w=\"" + MOUNT + "assets/x.js\";",
                new String(InstanceProxyServlet.patchBrandScript(ambiguous, MOUNT), StandardCharsets.UTF_8));
    }

    @Test
    void stylesheetRootUrlsAreGivenTheMount() {
        byte[] css = ("@font-face{src:url(/assets/Inter.ttf)}"
                + "a{background:url(\"/img/a.png\")}"
                + "b{background:url('//cdn.example.com/b.png')}"
                + "c{background:url(data:image/png;base64,AAAA)}").getBytes(StandardCharsets.UTF_8);

        String patched = new String(InstanceProxyServlet.patchBrandStylesheet(css, MOUNT), StandardCharsets.UTF_8);

        assertTrue(patched.contains("url(" + MOUNT + "assets/Inter.ttf)"), patched);
        assertTrue(patched.contains("url(\"" + MOUNT + "img/a.png\")"), patched);
        // Protocol-relative and data URLs are not the root's to move.
        assertTrue(patched.contains("url('//cdn.example.com/b.png')"), patched);
        assertTrue(patched.contains("url(data:image/png;base64,AAAA)"), patched);
    }

    @Test
    void theEntryScriptOfARealPageIsWhatTheProbeWouldFetch() {
        assertEquals("/assets/index-CjbuCoME.js",
                InstanceProxyServlet.entryScriptSrc(resource("brand-pages/opencode-index.html")));
        // A page naming none, or naming one on another host, cannot be probed:
        // the caller falls back to the panel root rather than patching blind.
        assertNull(InstanceProxyServlet.entryScriptSrc("<html><head></head></html>"));
        assertNull(InstanceProxyServlet.entryScriptSrc(
                "<html><head><script src=\"https://cdn.example.com/app.js\"></script></head></html>"));
    }

    private static java.net.http.HttpResponse<String> get(String baseUrl, String path) throws Exception {
        return java.net.http.HttpClient.newHttpClient().send(
                java.net.http.HttpRequest.newBuilder(java.net.URI.create(baseUrl + path)).GET().build(),
                java.net.http.HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void octopPageGetsARuntimeBaseAndNoManifest() {
        String page = """
                <!doctype html><html><head>
                <link rel="manifest" href="./manifest.json" />
                <script type="module" src="./assets/index.js"></script>
                </head><body></body></html>
                """;
        String rewritten = InstanceProxyServlet.rewriteOctopPage(page, MOUNT);
        assertTrue(rewritten.contains("<base href=\"" + MOUNT + "\">"), rewritten);
        assertTrue(rewritten.contains("window.__OCTOP_BASE__=\"" + MOUNT + "\""), rewritten);
        // Relative assets stay relative; <base> makes them resolve per deep link.
        assertTrue(rewritten.contains("src=\"./assets/index.js\""), rewritten);
        assertFalse(rewritten.contains("rel=\"manifest\""), rewritten);
    }

    private static String rewrite(String html) {
        return InstanceProxyServlet.rewritePage(html, MOUNT);
    }

    /// The router shim is referenced for path-routed clients only, under the
    /// contract the page was served with — see [InstanceProxyServlet#rewritePage]
    /// and the OpenCode brand flag.
    private static void assertRouterShim(String rewritten, InstanceProxyServlet.RouterShim shim) {
        String tag = "<script src=\"" + MOUNT + InstanceProxyServlet.ROUTER_RESOURCE
                + (shim == InstanceProxyServlet.RouterShim.ROOT ? "?mode=root" : "") + "\"></script>";
        assertTrue(rewritten.contains(tag), "router shim referenced: " + tag + " in " + rewritten);
    }

    private static void assertShim(String rewritten) {
        // The generic rewrite references the per-mount shim as an external
        // same-origin script (CSP-safe), it does not inline it.
        assertTrue(rewritten.contains(
                "<script src=\"" + MOUNT + InstanceProxyServlet.SHIM_RESOURCE + "\"></script>"),
                "shim script referenced: " + rewritten);
    }

    /// The mount prefix must appear exactly once per rewritten reference —
    /// a double prefix would mean the rewrite ran twice or matched its own
    /// output.
    private static void assertNoDoublePrefix(String rewritten) {
        assertFalse(rewritten.contains(MOUNT + MOUNT.substring(1)),
                "no double mount prefix: " + rewritten);
    }

    private static String resource(String name) {
        try (var stream = GenericPageRewriteTest.class.getClassLoader().getResourceAsStream(name)) {
            if (stream == null) {
                throw new IllegalStateException("missing test resource " + name);
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
