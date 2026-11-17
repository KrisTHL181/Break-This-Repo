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

import java.net.ServerSocket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class McPortsTest {

    @Test
    void validateRejectsOutOfRangeAndEqualPorts() {
        McPorts.validate(25565, 25575);
        assertThrows(IllegalArgumentException.class, () -> McPorts.validate(80, 25575));
        assertThrows(IllegalArgumentException.class, () -> McPorts.validate(25565, 70000));
        assertThrows(IllegalArgumentException.class, () -> McPorts.validate(25565, 25565));
    }

    @Test
    void allocateSkipsTakenPortsAndKeepsThePairTogether() {
        McPorts.Pair pair = McPorts.allocate(port -> port == 25565, 25565, 25570);
        assertEquals(25566, pair.game());
        assertEquals(25567, pair.rcon());
    }

    @Test
    void allocateNeverReturnsTheSameNumberTwice() {
        // Only two numbers are free, and they are not adjacent: the wrap must find the other one.
        McPorts.Pair pair = McPorts.allocate(port -> port != 40001 && port != 40003, 40000, 40003);
        assertEquals(40001, pair.game());
        assertEquals(40003, pair.rcon());
        assertTrue(pair.game() != pair.rcon());
    }

    @Test
    void allocateFailsLoudlyWhenTheRangeIsFull() {
        assertThrows(IllegalStateException.class, () -> McPorts.allocate(port -> true, 25565, 25566));
    }

    @Test
    void aPortWithAListenerIsNotFree() throws Exception {
        try (ServerSocket socket = new ServerSocket(0)) {
            assertFalse(McPorts.isFree(socket.getLocalPort()),
                    "a real listener must be seen even though no instance claims the port");
        }
    }
}