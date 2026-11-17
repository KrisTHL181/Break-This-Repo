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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The `exit` frame is the one place a viewer learns how the agent ended
/// (docs/terminal-protocol.md). A child that died without writing a byte is
/// indistinguishable from one that printed nothing usable, so the frame carries
/// that fact and the browser can say it instead of showing a bare exit code.
class TerminalExitFrameTest {

    @Test
    void theExitFrameCarriesTheCodeAndTheSilentFailureFlag() {
        JsonObject exit = JsonParser.parseString(TerminalWebSocket.exitJson(7, true)).getAsJsonObject();

        assertEquals("exit", exit.get("type").getAsString());
        assertEquals(7, exit.get("code").getAsInt());
        assertTrue(exit.get("failedWithoutOutput").getAsBoolean());
    }

    @Test
    void anOrdinaryExitFrameKeepsTheOldShape() {
        // The new field is optional (docs/terminal-protocol.md §4), so a viewer
        // written before it existed keeps parsing the frame: it is only present
        // when it is true.
        JsonObject exit = JsonParser.parseString(TerminalWebSocket.exitJson(0, false)).getAsJsonObject();

        assertEquals("exit", exit.get("type").getAsString());
        assertEquals(0, exit.get("code").getAsInt());
        assertFalse(exit.has("failedWithoutOutput"));
    }
}