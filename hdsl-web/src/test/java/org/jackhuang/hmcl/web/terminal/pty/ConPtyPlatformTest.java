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
package org.jackhuang.hmcl.web.terminal.pty;

import org.jackhuang.hmcl.util.platform.OperatingSystem;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/// What the ConPTY path says on a machine that has no ConPTY.
///
/// The message is what a person sees in the terminal dialog and what the 409
/// carries, so its shape is part of the contract: the build the terminal needs,
/// and the build this machine reports.
class ConPtyPlatformTest {

    @Test
    void aNonWindowsMachineHasNoPseudoConsole() {
        assumeTrue(OperatingSystem.CURRENT_OS != OperatingSystem.WINDOWS, "Windows has ConPTY");
        assertFalse(ConPty.platformSupported());
        assertEquals(0, ConPty.buildNumber());
        assertTrue(ConPty.describe().startsWith("UNAVAILABLE - " + ConPty.UNSUPPORTED_MESSAGE),
                ConPty.describe());
    }

    @Test
    void spawningWithoutAPseudoConsoleThrows() {
        assumeTrue(OperatingSystem.CURRENT_OS != OperatingSystem.WINDOWS, "Windows has ConPTY");
        PtyException refused = assertThrows(PtyException.class, () -> ConPty.spawn(
                List.of("/bin/sh"), null, Map.of(), new PtySize(80, 24)));
        assertEquals(ConPty.UNSUPPORTED_MESSAGE, refused.getMessage());
    }

    @Test
    void theMessageNamesTheBuildItNeeds() {
        assertTrue(ConPty.UNSUPPORTED_MESSAGE.contains("Windows 10 1809"), ConPty.UNSUPPORTED_MESSAGE);
        assertTrue(ConPty.UNSUPPORTED_MESSAGE.contains("build " + ConPty.REQUIRED_BUILD),
                ConPty.UNSUPPORTED_MESSAGE);
        assertTrue(ConPty.UNSUPPORTED_MESSAGE.contains("build " + ConPty.buildNumber()),
                ConPty.UNSUPPORTED_MESSAGE);
    }

    @Test
    void describingTheTerminalNeverLoadsTheNativeLibrary() {
        // Printing the banner (and the doctor) must be safe on a machine that has
        // no kernel32 at all: the availability answer is a probe, not a load.
        assumeTrue(OperatingSystem.CURRENT_OS != OperatingSystem.WINDOWS, "Windows has ConPTY");
        assertDoesNotThrow(ConPty::describe);
        assertDoesNotThrow(ConPty::platformSupported);
    }
}
