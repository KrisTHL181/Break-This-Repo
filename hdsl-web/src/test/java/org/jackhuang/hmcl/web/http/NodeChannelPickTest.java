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

import org.jackhuang.hmcl.dsh.NodeRelease;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/// Which release a channel update picks from the published index.
class NodeChannelPickTest {

    private static final List<NodeRelease> RELEASES = List.of(
            new NodeRelease("27.0.0-alpha.0.0.0", null, "2026-10-01"),
            new NodeRelease("26.13.0", "Krypton", "2026-09-01"),
            new NodeRelease("24.14.0", null, "2026-08-01"),
            new NodeRelease("22.23.1", "Jod", "2026-07-01"));

    @Test
    void picksTheNewestStableReleaseOnTheLine() {
        assertEquals("26.13.0", NodeApiServlet.pickForChannel(RELEASES, "26"));
        assertEquals("22.23.1", NodeApiServlet.pickForChannel(RELEASES, "22"));
        assertEquals("26.13.0", NodeApiServlet.pickForChannel(RELEASES, "current"));
        assertEquals("26.13.0", NodeApiServlet.pickForChannel(RELEASES, "lts"));
    }

    @Test
    void skipsPrereleasesAndMissingLines() {
        assertNull(NodeApiServlet.pickForChannel(RELEASES, "27"));
        assertNull(NodeApiServlet.pickForChannel(RELEASES, "20"));
    }
}
