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
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The sampler against a fake `/proc`: two samples, one second apart in
/// counter terms, must turn cumulative counters into rates.
class SystemMetricsTest {

    @TempDir
    Path temp;

    private Path write(String relative, String text) throws Exception {
        Path file = temp.resolve(relative);
        Files.createDirectories(file.getParent());
        Files.writeString(file, text);
        return file;
    }

    private void stat(long user0, long idle0, long user1, long idle1) throws Exception {
        write("proc/stat", """
                cpu  %d 0 0 %d 0 0 0 0 0 0
                cpu0 %d 0 0 %d 0 0 0 0 0 0
                cpu1 %d 0 0 %d 0 0 0 0 0 0
                """.formatted(user0 * 2, idle0 * 2, user0, idle0, user1, idle1));
    }

    @Test
    void cumulativeCountersBecomeRatesOnTheSecondSample() throws Exception {
        stat(100, 100, 100, 100);
        write("proc/meminfo", """
                MemTotal: 1000 kB
                MemFree: 200 kB
                MemAvailable: 400 kB
                Cached: 200 kB
                SwapTotal: 200 kB
                SwapFree: 100 kB
                """);
        write("proc/diskstats", """
                 8 0 sda 1 0 1000 0 1 0 500 0 0 0 0
                """);
        write("proc/net/dev", """
                Inter-|   Receive                                                |  Transmit
                 face |bytes    packets errs drop fifo frame compressed multicast|bytes    packets errs drop fifo colls carrier compressed
                  eth0: 1000 1 0 0 0 0 0 0 2000 1 0 0 0 0 0 0
                """);
        write("proc/loadavg", "0.50 0.40 0.30 1/100 1234");
        write("proc/self/status", "VmRSS:\t  1234 kB\n");
        Files.createDirectories(temp.resolve("sys/block/sda"));
        Files.createDirectories(temp.resolve("sys/block/sr0"));

        SystemMetrics metrics = new SystemMetrics(temp.resolve("data"), temp.resolve("proc"),
                temp.resolve("sys/block"), HostInfo.NATIVE_LINUX);
        try {
            SystemSnapshot first = metrics.sampleNow();
            assertEquals("linux", first.host().os(), "the snapshot says where it was taken");
            assertEquals("native", first.host().environment());
            assertTrue(first.capabilities().perCore());
            assertEquals(2, first.cpu().count());
            assertEquals(-1, first.cpu().load(), "the first sample has nothing to compare");
            assertEquals(-1, first.cpu().cores()[0]);
            assertEquals(1, first.disks().size(), "only the whole device, never its partition");
            assertEquals("sda", first.disks().get(0).name(), "an optical drive is not a disk");
            assertEquals(-1, first.disks().get(0).readBytesPerSec());
            assertEquals(1, first.networks().size());
            assertEquals(-1, first.networks().get(0).rxBytesPerSec());
            assertEquals(1234L * 1024, first.panel().rssBytes());
            assertEquals(1000L * 1024, first.memory().totalBytes());
            assertEquals(600L * 1024, first.memory().usedBytes());
            assertEquals(400L * 1024, first.memory().availableBytes());
            assertEquals(200L * 1024, first.memory().cachedBytes());
            assertEquals(200L * 1024, first.memory().freeBytes());
            assertEquals(first.memory().totalBytes(),
                    first.memory().usedBytes() + first.memory().availableBytes());
            assertEquals(first.memory().availableBytes(),
                    first.memory().cachedBytes() + first.memory().freeBytes(),
                    "the bar's three segments add up to the whole memory");
            assertEquals(3, first.cpu().loadAverage().length);

            Thread.sleep(250);

            // Both cores did 50% more work; the disk read 1000 more sectors and
            // the interface 1000 more bytes.
            stat(1000, 1000, 1000, 1000);
            write("proc/diskstats", """
                     8 0 sda 1 0 2000 0 1 0 500 0 0 0 0
                    """);
            write("proc/net/dev", """
                    Inter-|   Receive                                                |  Transmit
                     face |bytes    packets errs drop fifo frame compressed multicast|bytes    packets errs drop fifo colls carrier compressed
                      eth0: 2000 1 0 0 0 0 0 0 3000 1 0 0 0 0 0 0
                    """);

            SystemSnapshot second = metrics.sampleNow();
            assertEquals(0.5, second.cpu().load(), 0.05);
            assertEquals(0.5, second.cpu().cores()[0], 0.05);
            assertTrue(second.disks().get(0).readBytesPerSec() > 0);
            assertTrue(second.networks().get(0).rxBytesPerSec() > 0);
            assertTrue(second.networks().get(0).txBytesPerSec() > 0);

            assertEquals(2, metrics.history(240).size(), "both samples are in the ring");
            // GPU capability is platform-dependent now (sysfs / nvidia-smi); the
            // per-host reader has its own test, so this sample only checks that the
            // field is populated without turning the whole suite red on a CI box.
        } finally {
            metrics.close();
        }
    }

    /// The API caches its serialised ring keyed on this number, so a frame must
    /// carry the sequence of the newest sample it contains — read under one
    /// lock, or the cache files a list under the wrong sample.
    @Test
    void theHistoryFrameCarriesTheSequenceOfItsNewestSample() {
        SystemMetrics metrics = new SystemMetrics(temp.resolve("data"), temp.resolve("nope"),
                temp.resolve("nope-block"));
        try {
            SystemMetrics.Frame empty = metrics.historyFrame(240);
            assertEquals(0, empty.sequence(), "nothing has been sampled yet");
            assertTrue(empty.samples().isEmpty());

            metrics.sampleNow();
            SystemMetrics.Frame first = metrics.historyFrame(240);
            assertEquals(1, first.sequence());
            assertEquals(1, first.samples().size());

            metrics.sampleNow();
            SystemMetrics.Frame second = metrics.historyFrame(240);
            assertEquals(2, second.sequence(), "the sequence moves once per sample");
            assertEquals(2, second.samples().size());
            assertEquals(1, metrics.historyFrame(1).samples().size(), "max still trims the tail");
        } finally {
            metrics.close();
        }
    }

    @Test
    void aMissingProcTreeDegradesInsteadOfThrowing() {
        SystemMetrics metrics = new SystemMetrics(temp.resolve("data"), temp.resolve("nope"),
                temp.resolve("nope-block"));
        try {
            SystemSnapshot snapshot = metrics.sampleNow();
            assertFalse(snapshot.capabilities().perCore());
            assertFalse(snapshot.capabilities().diskIo());
            assertFalse(snapshot.capabilities().network());
            assertTrue(snapshot.cpu().count() > 0, "the JVM still knows how many processors it has");
            assertTrue(snapshot.memory().totalBytes() > 0);
        } finally {
            metrics.close();
        }
    }
}
