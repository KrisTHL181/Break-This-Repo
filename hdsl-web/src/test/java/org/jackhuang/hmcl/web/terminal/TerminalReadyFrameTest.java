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
package org.jackhuang.hmcl.web.terminal;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/// The control-frame contract the terminal CLI is written against
/// (docs/terminal-protocol.md). The byte channel is the PTY itself; these JSON
/// frames carry the version, the instance and the brand.
class TerminalReadyFrameTest {

    @Test
    void theReadyFrameCarriesProtocolInstanceAndBrand() {
        JsonObject ready = JsonParser.parseString(
                TerminalWebSocket.readyJson("DClDN1J3w0C_", "kimi")).getAsJsonObject();

        assertEquals("ready", ready.get("type").getAsString());
        assertEquals(TerminalWebSocket.PROTOCOL, ready.get("protocol").getAsInt());
        assertEquals("DClDN1J3w0C_", ready.get("instance").getAsString());
        assertEquals("kimi", ready.get("brand").getAsString());
    }

    @Test
    void theProtocolNumberIsPinned() {
        // The CLI refuses to guess: a change here is a contract change and must
        // ship with docs/terminal-protocol.md.
        assertEquals(1, TerminalWebSocket.PROTOCOL);
    }
}