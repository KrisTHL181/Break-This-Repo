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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Which requests keep the browser's `Host`, and which do not.
///
/// The two answers are opposites and both are load-bearing:
///
/// - dsh compares the browser's `Origin` with the `Host` it was reached through
///   and refuses a request whose pair does not match, so the header has to pass
///   through untouched;
/// - a prefix-aware server (Hermes) answers **400** to a `Host` it does not
///   serve (it only serves loopback), so there the header has to be dropped and
///   left for the HTTP client to derive from the loopback URI.
///
/// Measured against both upstreams during their integration spikes; this test
/// pins the switch so a future "cleanup" cannot collapse the two cases.
class ProxyHostHeaderTest {

    @Test
    void aPrefixAwareServerNeverSeesTheBrowsersHost() {
        assertTrue(InstanceProxyServlet.upperIsSkipped("host", true));
        assertFalse(InstanceProxyServlet.upperIsSkipped("origin", true),
                "the WebSocket relay drops Origin, the HTTP proxy does not need to");
        assertFalse(InstanceProxyServlet.upperIsSkipped("cookie", true));
    }

    @Test
    void everyOtherBrandKeepsTheHeaderDshComparesAgainst() {
        assertFalse(InstanceProxyServlet.upperIsSkipped("host", false));
        assertFalse(InstanceProxyServlet.upperIsSkipped("origin", false));
    }
}
