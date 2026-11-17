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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Tests for reading the address out of the upstream readiness line.
///
/// The address is not merely a sign that the server is up — it is **what the launcher opens**, and
/// since the browser-trust fence it carries the credential the browser has to present. So the value
/// matters, not just the match: a pattern that recognises the line and drops the token gives a
/// launcher that waits correctly, reports the instance ready, and then opens a page that is refused.
/// That is what happened, and it was invisible for as long as nothing looked at the value.
class DshReadinessLineTest {
    /// The token a real 0.1.5-alpha.2 instance printed, kept verbatim: base64url, so it exercises the
    /// `-` and `_` in the character class rather than only alphanumerics.
    private static final String TOKEN = "dcuszlMez30wtcJpMkS1-VosQcMaTfgE7g4i_mc6Tq8";

    @Test
    void theAddressKeepsTheTokenItWasPrintedWith() {
        String line = "dsh web: http://127.0.0.1:3229/?token=" + TOKEN;

        java.util.Optional<java.net.URI> parsed = DshProcess.parseWebUrl(line);

        assertTrue(parsed.isPresent(), "a readiness line must be recognised");
        assertEquals("http://127.0.0.1:3229/?token=" + TOKEN, parsed.get().toString());
        assertEquals(TOKEN, parsed.get().getQuery().substring("token=".length()),
                "the token is the credential the browser presents; losing it opens a page that is refused");
    }

    @Test
    void theOlderSpellingsAreStillRead() {
        // Releases before the fence print the address on its own, with or without a trailing slash,
        // and those have to keep working: they are what every instance older than the fence says.
        assertEquals("http://127.0.0.1:4061",
                DshProcess.parseWebUrl("dsh web: http://127.0.0.1:4061").orElseThrow().toString());
        assertEquals("http://127.0.0.1:4061/",
                DshProcess.parseWebUrl("dsh web: http://127.0.0.1:4061/").orElseThrow().toString());
    }

    @Test
    void aLanSuffixIsNotMistakenForTheAddress() {
        String line = "dsh web: http://127.0.0.1:3229/?token=" + TOKEN
                + " (LAN: http://192.0.2.5:3229/?token=" + TOKEN + ")";

        assertEquals("http://127.0.0.1:3229/?token=" + TOKEN,
                DshProcess.parseWebUrl(line).orElseThrow().toString());
    }

    @Test
    void whatIsNotAReadinessLineSaysSo() {
        // The child prints a great deal that is not this. An address invented for a line that is not
        // a readiness line would be a launcher opening a browser at nothing.
        assertTrue(DshProcess.parseWebUrl("").isEmpty());
        assertTrue(DshProcess.parseWebUrl("[INFO] starting up").isEmpty());
        assertTrue(DshProcess.parseWebUrl("dsh web: http://0.0.0.0:3229/?token=" + TOKEN).isEmpty(),
                "only the loopback address is the one the launcher opens");
        assertTrue(DshProcess.parseWebUrl("dsh web: http://127.0.0.1:not-a-port").isEmpty());
    }
}
