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

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The assets base a **prefix-aware** bundle resolves its lazy chunks with.
///
/// Vite bakes it at build time, so it is the one reference that does not learn
/// the mount from `X-Forwarded-Prefix`. Left at `/`, a page under `/i/<id>/`
/// asks the panel's own root for every lazy chunk; the panel answers with
/// `index.html` and the browser refuses the module or stylesheet — a failed CSS
/// preload takes the whole page down (measured 2026-11: Hermes 0.22.5's Chat
/// page rendered nothing but the theme background).
///
/// The helper is matched by shape and only its literal is replaced: a build
/// whose helper changed shape must be served untouched, never guessed at.
class PrefixAssetsPatchTest {

    private static byte[] utf8(String text) {
        return text.getBytes(StandardCharsets.UTF_8);
    }

    private static String text(byte[] bytes) {
        return new String(bytes, StandardCharsets.UTF_8);
    }

    @Test
    void theTemplateLiteralHelperLearnsTheMount() {
        // Rolldown / Vite 8 shape, measured 2026-11 in Hermes 0.22.5:
        //   var Yt=`modulepreload`,Xt=function(e){return`/`+e},Zt={}
        String bundle = "var Yt=`modulepreload`,Xt=function(e){return`/`+e},Zt={}";

        String patched = text(InstanceProxyServlet.patchPrefixScript(utf8(bundle), "/i/abc/"));

        assertTrue(patched.contains("=function(e){return`/i/abc/`+e}"), patched);
        assertTrue(patched.contains("var Yt=`modulepreload`"), patched);
    }

    @Test
    void theDoubleQuotedHelperLearnsTheMount() {
        String bundle = "const assetsURL=function(dep){return\"/\"+dep};";

        String patched = text(InstanceProxyServlet.patchPrefixScript(utf8(bundle), "/i/abc/"));

        assertEquals("const assetsURL=function(dep){return\"/i/abc/\"+dep};", patched);
    }

    @Test
    void anUnrecognisedHelperIsServedUntouched() {
        // A reformatted helper is a different build, not a licence to guess.
        byte[] bundle = utf8("=function (dep) { return \"/\" + dep }");

        assertArrayEquals(bundle, InstanceProxyServlet.patchPrefixScript(bundle, "/i/abc/"));
    }

    @Test
    void aBundleWithoutTheHelperIsServedUntouched() {
        byte[] bundle = utf8("console.log(\"no preload helper here\")");

        assertArrayEquals(bundle, InstanceProxyServlet.patchPrefixScript(bundle, "/i/abc/"));
    }
}
