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
package org.jackhuang.hmcl.web.system;

import com.sun.jna.Memory;
import com.sun.jna.Native;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/// The Windows readers' buffer layouts, tested without Windows.
///
/// The native calls themselves cannot run here; what can be pinned is the
/// arithmetic: the 48-byte per-processor block, the 24-byte PDH item, and the
/// struct sizes the APIs are handed. A wrong size is the failure mode that
/// corrupts memory rather than throwing.
class WindowsMetricsTest {

    @Test
    void perProcessorTimesAreSixSixtyFourBitValues() {
        assumeTrue(Native.POINTER_SIZE == 8, "the layout assumes a 64-bit process");
        Memory buffer = new Memory(2L * 48);
        // Processor 0: idle=10, kernel=30, user=20 → total 50.
        buffer.setLong(0, 10);
        buffer.setLong(8, 30);
        buffer.setLong(16, 20);
        // Processor 1: idle=5, kernel=15, user=10 → total 25.
        buffer.setLong(48, 5);
        buffer.setLong(56, 15);
        buffer.setLong(64, 10);

        List<WindowsMetrics.CpuTimes> times = WindowsMetrics.parseCpuTimes(buffer, 2);
        assertEquals(2, times.size());
        assertEquals(10, times.get(0).idle());
        assertEquals(50, times.get(0).total());
        assertEquals(25, times.get(1).total());
    }

    @Test
    void aPdhItemIsANamePointerAndAFormattedDouble() {
        assumeTrue(Native.POINTER_SIZE == 8, "the layout assumes a 64-bit process");
        Memory names = new Memory(64);
        names.setWideString(0, "eth0");
        Memory buffer = new Memory(24L * 2);
        buffer.setPointer(0, names);
        buffer.setInt(8, 0);          // CStatus = PDH_CSTATUS_VALID_DATA
        buffer.setDouble(16, 1234.5);
        Memory names2 = new Memory(64);
        names2.setWideString(0, "Loopback");
        buffer.setPointer(24, names2);
        buffer.setInt(32, 0);
        buffer.setDouble(40, 7.0);

        Map<String, double[]> rates = new LinkedHashMap<>();
        WindowsMetrics.readCounterArray(buffer, 2, 0, rates);
        WindowsMetrics.readCounterArray(buffer, 2, 1, rates);

        assertEquals(2, rates.size());
        assertEquals(1234.5, rates.get("eth0")[0], 1e-9);
        assertEquals(7.0, rates.get("Loopback")[0], 1e-9);
    }

    @Test
    void theStructsHaveTheSizesTheApisExpect() {
        assumeTrue(Native.POINTER_SIZE == 8, "the layout assumes a 64-bit process");
        WindowsMetrics.MemoryStatusEx status = new WindowsMetrics.MemoryStatusEx();
        assertEquals(64, status.size(), "two DWORDs + seven DWORDLONGs");
        assertEquals(64, status.dwLength);
        assertEquals(16, new WindowsMetrics.PdhCounterValue().size(), "DWORD + padding + double");
    }
}
