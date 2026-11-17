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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/// [HostHeaders#stripPort]: the brand servlet used to cut at the first colon,
/// which turns `[::1]:8080` into `[` — a garbage host allowlist entry.
class HostHeadersTest {

    @Test
    void plainHostWithPort() {
        assertEquals("example.com", HostHeaders.stripPort("example.com:8080"));
        assertEquals("example.com", HostHeaders.stripPort("example.com:80"));
    }

    @Test
    void ipv6Bracketed() {
        assertEquals("[::1]", HostHeaders.stripPort("[::1]:8080"));
        assertEquals("[2001:db8::1]", HostHeaders.stripPort("[2001:db8::1]:443"));
    }

    @Test
    void ipv6BareLiteralStaysIntact() {
        assertEquals("::1", HostHeaders.stripPort("::1"));
        assertEquals("2001:db8::1", HostHeaders.stripPort("2001:db8::1"));
    }

    @Test
    void noPortAndOddSuffixes() {
        assertEquals("example.com", HostHeaders.stripPort("example.com"));
        // Not all digits: not a port, keep as-is.
        assertEquals("example.com:abc", HostHeaders.stripPort("example.com:abc"));
    }

    @Test
    void absentHeader() {
        assertNull(HostHeaders.stripPort(null));
        assertNull(HostHeaders.stripPort(""));
        assertNull(HostHeaders.stripPort("   "));
    }
}
