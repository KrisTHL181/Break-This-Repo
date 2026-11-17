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

import com.sun.jna.Native;
import org.jackhuang.hmcl.util.platform.OperatingSystem;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/// The Win32 structures this file hands to the kernel, sized the way Windows
/// expects them.
///
/// A wrong `cb` in `STARTUPINFOEXW` is not a compile error and barely an error
/// at all: `CreateProcessW` fails with `ERROR_INVALID_PARAMETER`, which reads
/// like "the program is broken". The numbers are the documentation's
/// (`sizeof(STARTUPINFOEXW)` is 112 on 64-bit and 72 on 32-bit) and pinning them
/// here is what keeps a JNA layout change from turning into that.
class StartupInfoExLayoutTest {

    @Test
    void startupInfoExIsTheSizeCreateProcessChecks() {
        assumeTrue(OperatingSystem.CURRENT_OS == OperatingSystem.WINDOWS,
                "the layout is exercised where the struct is handed over");
        int expected = Native.POINTER_SIZE == 8 ? 112 : 72;
        assertEquals(expected, new ConPty.StartupInfoEx().size());
        assertEquals(expected, new ConPty.StartupInfoEx().cb);
    }

    @Test
    void theSmallStructuresMatchTheirC() {
        assumeTrue(OperatingSystem.CURRENT_OS == OperatingSystem.WINDOWS,
                "the layout is exercised where the struct is handed over");
        assertEquals(4, new ConPty.Coord().size(), "COORD is two shorts");
        assertEquals(Native.POINTER_SIZE == 8 ? 24 : 16, new ConPty.ProcessInformation().size());
        assertEquals(Native.POINTER_SIZE == 8 ? 24 : 16, new ConPty.SecurityAttributes().size());
        if (Native.POINTER_SIZE == 8) {
            // JOBOBJECT_EXTENDED_LIMIT_INFORMATION: 64 bytes of basic limits,
            // 48 of IO counters, four SIZE_Ts.
            assertEquals(144, new ConPty.JobObjectExtendedLimitInformation().size());
        }
    }
}
