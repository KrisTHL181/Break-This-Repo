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

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The alarm rules: sustained, cooled down, and closed with a recovery line.
class AnomalyDetectorTest {

    private static final Instant T0 = Instant.parse("2026-11-05T00:00:00Z");

    private static SystemSnapshot snapshot(double cpu, double memoryRatio, double diskRatio, double load) {
        return new SystemSnapshot(
                T0.toString(), 1000,
                new SystemSnapshot.Host("linux", "native"),
                new SystemSnapshot.Capabilities(true, false, false, false),
                new SystemSnapshot.Cpu(cpu, 8, -1, new double[]{cpu}, new double[]{load, load, load}),
                new SystemSnapshot.Memory(1000, (long) (memoryRatio * 1000), 1000 - (long) (memoryRatio * 1000),
                        0, 0, 0, 0, "host"),
                List.of(),
                List.of(new SystemSnapshot.MountUsage("/data", 1000, (long) (diskRatio * 1000))),
                List.of(),
                List.of(),
                new SystemSnapshot.Panel(0.01, 1, 1, 1));
    }

    private static List<AnomalyDetector.Alert> check(AnomalyDetector detector, SystemSnapshot snapshot, int second) {
        return detector.check(snapshot, T0.plusSeconds(second));
    }

    @Test
    void cpuMustStayHighForThirtySecondsAndThenCoolsDown() {
        AnomalyDetector detector = new AnomalyDetector();
        SystemSnapshot hot = snapshot(0.95, 0.2, 0.2, 1);

        assertTrue(check(detector, hot, 0).isEmpty());
        assertTrue(check(detector, hot, 20).isEmpty(), "twenty seconds is not sustained");
        List<AnomalyDetector.Alert> first = check(detector, hot, 30);
        assertEquals(1, first.size(), "the line is written once the threshold is sustained");
        assertEquals("cpu", first.get(0).metric());
        assertTrue(check(detector, hot, 60).isEmpty(), "no repeat inside the cooldown");
        assertEquals(1, check(detector, hot, 10 * 60 + 30).size(), "a repeat after the cooldown");

        List<AnomalyDetector.Alert> recovery = check(detector, snapshot(0.2, 0.2, 0.2, 1), 10 * 60 + 31);
        assertEquals(1, recovery.size());
        assertTrue(recovery.get(0).recovery());
    }

    @Test
    void memoryNeedsAMinuteAndDiskFiresImmediately() {
        AnomalyDetector detector = new AnomalyDetector();
        SystemSnapshot tight = snapshot(0.1, 0.95, 0.95, 1);

        List<AnomalyDetector.Alert> first = check(detector, tight, 0);
        assertEquals(1, first.size(), "a 95%-full disk is worth a line on its own");
        assertEquals("disk", first.get(0).metric());
        assertTrue(check(detector, tight, 30).isEmpty(), "memory has only been tight for half a minute");
        List<AnomalyDetector.Alert> at60 = check(detector, tight, 60);
        assertTrue(at60.stream().anyMatch(alert -> alert.metric().equals("memory")));
    }

    @Test
    void aHealthyMachineSaysNothing() {
        AnomalyDetector detector = new AnomalyDetector();
        SystemSnapshot calm = snapshot(0.2, 0.3, 0.3, 0.5);
        for (int second = 0; second <= 600; second += 30) {
            assertTrue(check(detector, calm, second).isEmpty());
        }
    }

    @Test
    void valuesThatCouldNotBeMeasuredNeverAlarm() {
        AnomalyDetector detector = new AnomalyDetector();
        SystemSnapshot unknown = new SystemSnapshot(
                T0.toString(), 1000,
                new SystemSnapshot.Host("linux", "native"),
                new SystemSnapshot.Capabilities(false, false, false, false),
                new SystemSnapshot.Cpu(-1, 8, -1, new double[0], new double[0]),
                new SystemSnapshot.Memory(0, 0, 0, 0, 0, 0, 0, "host"),
                List.of(), List.of(), List.of(), List.of(),
                new SystemSnapshot.Panel(-1, -1, 1, 1));
        assertTrue(check(detector, unknown, 600).isEmpty());
    }
}
