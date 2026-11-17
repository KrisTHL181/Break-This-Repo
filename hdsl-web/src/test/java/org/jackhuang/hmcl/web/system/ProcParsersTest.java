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

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The `/proc` parsers, pinned to the shapes measured on this deployment
/// (2026-11: 12 cores, nvme + md arrays, Docker with cgroup v2).
class ProcParsersTest {

    private static final String STAT = """
            cpu  2591425 117 1370384 211335117 310160 0 37861 0 183346 0
            cpu0 220662 8 136949 17507134 29577 0 8102 0 12681 0
            cpu1 225618 5 88919 17664315 17825 0 6428 0 17754 0
            intr 123456 0 0 0
            ctxt 987654
            """;

    @Test
    void statParseAndDelta() {
        Map<String, ProcStat.Times> times = ProcStat.parse(STAT);
        assertEquals(3, times.size(), "aggregate plus two cores; intr/ctxt are not cpu lines");
        assertEquals(220662L + 8 + 136949 + 17507134 + 29577 + 0 + 8102 + 0,
                times.get("cpu0").total(), "user…steal, without the guest columns");

        // A second sample where half the elapsed time was idle.
        ProcStat.Times previous = new ProcStat.Times(100, 40);
        ProcStat.Times current = new ProcStat.Times(200, 90);
        assertEquals(0.5, ProcStat.usage(previous, current), 1e-9);
        assertEquals(-1, ProcStat.usage(null, current), "the first sample has nothing to compare");
        assertEquals(-1, ProcStat.usage(current, previous), "a counter going backwards is not a number");
    }

    @Test
    void diskSectorMathUsesFiveHundredTwelveByteSectors() {
        String text = """
                 259       0 nvme0n1 338830 147109 14971845 98449 578363 1323818 24703762 468909 0 80440 604682
                 259       1 nvme0n1p1 354 1322 17123 736 2 0 2 0 0 88 736
                """;
        Map<String, ProcDiskStats.Counters> devices = ProcDiskStats.parse(text);
        assertEquals(2, devices.size(), "partitions are in the file; the sampler filters them by /sys/block");
        assertEquals(14971845, devices.get("nvme0n1").readSectors());

        ProcDiskStats.Counters before = new ProcDiskStats.Counters(0, 0);
        ProcDiskStats.Counters after = new ProcDiskStats.Counters(2048, 1024);
        assertEquals(2048 * 512, ProcDiskStats.readBytesPerSecond(before, after, 1), 1e-6);
        assertEquals(1024 * 512, ProcDiskStats.writeBytesPerSecond(before, after, 1), 1e-6);
        assertEquals(-1, ProcDiskStats.readBytesPerSecond(null, after, 1));
        assertEquals(-1, ProcDiskStats.readBytesPerSecond(after, before, 1), "a reset counter is not a negative rate");
    }

    @Test
    void netDevSkipsHeadersAndKeepsBothDirections() {
        String text = """
                Inter-|   Receive                                                |  Transmit
                 face |bytes    packets errs drop fifo frame compressed multicast|bytes    packets errs drop fifo colls carrier compressed
                    lo: 238234269  224279    0    0    0     0          0         0 238234269  224279    0    0    0     0       0          0
                  eth0: 4463609213 3195206    0    0    0     0          0         0 1668469042 2079416    0    0    0     0       0          0
                """;
        Map<String, ProcNetDev.Counters> interfaces = ProcNetDev.parse(text);
        assertEquals(2, interfaces.size());
        assertEquals(4463609213L, interfaces.get("eth0").rxBytes());
        assertEquals(1668469042L, interfaces.get("eth0").txBytes());

        ProcNetDev.Counters before = new ProcNetDev.Counters(0, 0);
        ProcNetDev.Counters after = new ProcNetDev.Counters(1000, 4000);
        assertEquals(1000, ProcNetDev.bytesPerSecond(before, after, 1, false), 1e-9);
        assertEquals(4000, ProcNetDev.bytesPerSecond(before, after, 1, true), 1e-9);
    }

    @Test
    void meminfoConvertsKiloBytesAndFallsBackToFree() {
        ProcMeminfo.Memory memory = ProcMeminfo.parse("""
                MemTotal:       32568908 kB
                MemFree:         2517832 kB
                MemAvailable:   11508008 kB
                Buffers:         1239168 kB
                Cached:          6888240 kB
                SwapTotal:       5242876 kB
                SwapFree:        4534212 kB
                """);
        assertEquals(32568908L * 1024, memory.totalBytes());
        assertEquals(11508008L * 1024, memory.availableBytes());
        assertEquals(2517832L * 1024, memory.freeBytes());
        assertEquals((1239168L + 6888240L) * 1024, memory.cachedBytes(),
                "buffers + page cache is what Linux calls cached");
        assertEquals(4534212L * 1024, memory.swapFreeBytes());

        ProcMeminfo.Memory old = ProcMeminfo.parse("""
                MemTotal: 1000 kB
                MemFree: 400 kB
                """);
        assertEquals(400L * 1024, old.availableBytes(), "pre-3.14 kernels only have MemFree");
    }

    @Test
    void cgroupMaxMeansUnlimitedAndQuotaIsCores() {
        assertEquals(2.0, CgroupLimits.parseCpuQuota("200000 100000"), 1e-9);
        assertEquals(-1, CgroupLimits.parseCpuQuota("max 100000"));
        assertEquals(-1, CgroupLimits.parseCpuQuota(""));
        assertEquals(-1, CgroupLimits.parseCpuQuota("garbage"));
        assertEquals(12345, CgroupLimits.statValue("anon 1\nfile 12345\n", "file"),
                "memory.stat is the only place cgroup v2 reports page cache");
        assertEquals(-1, CgroupLimits.statValue(null, "file"));
    }

    @Test
    void loadAverageTakesTheThreeAverages() {
        double[] load = SystemMetrics.loadAverage("0.11 0.21 0.36 2/2586 191000");
        assertEquals(3, load.length);
        assertEquals(0.11, load[0], 1e-9);
        assertEquals(0.36, load[2], 1e-9);
        assertEquals(0, SystemMetrics.loadAverage("garbage").length);
        assertEquals(0, SystemMetrics.loadAverage(null).length);
    }
}
