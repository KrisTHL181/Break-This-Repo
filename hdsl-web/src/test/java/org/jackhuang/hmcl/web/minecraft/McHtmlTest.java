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
package org.jackhuang.hmcl.web.minecraft;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class McHtmlTest {

    @Test
    void theAgreementStartsAtItsTitleAndTheShellIsDropped() {
        String html = """
                <html><head><title>EULA | Minecraft</title>
                <script>window.tracker = "should not appear";</script></head>
                <body>
                <div class="banner">Playing Minecraft in China? Go to NetEase!</div>
                <button>Continue browsing this site</button>
                <h1>Minecraft End(er)-User License Agreement (&ldquo;EULA&rdquo;)</h1>
                <p>This EULA is a legal agreement between you and us.</p>
                <p>Do not distribute anything we&rsquo;ve made.</p>
                <footer>&copy; 2026 Mojang</footer>
                </body></html>
                """;

        String text = McHtml.legalText(html);
        assertTrue(text.startsWith("Minecraft End(er)-User License Agreement"), text);
        assertTrue(text.contains("Do not distribute anything"), text);
        assertFalse(text.contains("NetEase"), text);
        assertFalse(text.contains("window.tracker"), text);
        assertFalse(text.contains("<"), text);
        assertFalse(text.contains("&ldquo;"), text);
    }

    @Test
    void withoutTheTitleTheWholeStrippedPageIsUsed() {
        String text = McHtml.legalText("<p>line one</p><p>line two</p>");
        assertTrue(text.contains("line one"));
        assertTrue(text.contains("line two"));
    }
}