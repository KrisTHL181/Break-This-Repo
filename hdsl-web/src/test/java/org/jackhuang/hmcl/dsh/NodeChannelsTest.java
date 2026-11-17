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
package org.jackhuang.hmcl.dsh;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/// The channel registry the `22`/`26` pointers live in.
class NodeChannelsTest {

    @Test
    void roundTripsThroughTheFile() throws Exception {
        NodeChannels.record("22", "22.23.1");
        try {
            assertEquals("22.23.1", NodeChannels.version("22"));

            NodeChannels.forget("22.23.1");
            assertNull(NodeChannels.version("22"));
        } finally {
            NodeChannels.forget("22.23.1");
        }
    }
}
