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
package org.jackhuang.hmcl.web.emergency;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Pins the emergency stop's contract, above all the one property that makes it
/// an emergency stop: **nobody lifts it early**, and a second press does not
/// restart the hour.
class EmergencyStopTest {

    private static final Instant T0 = Instant.parse("2026-10-27T10:00:00Z");

    @Test
    void startsDisengaged() {
        EmergencyStop stop = new EmergencyStop();
        assertFalse(stop.isEngaged());
        assertNull(stop.engagedAt());
        assertNull(stop.releasesAt());
        assertEquals(Duration.ZERO, stop.remaining(T0));
        // Releasing a stop that never happened is a no-op, not an error.
        assertNull(stop.release(T0));
    }

    @Test
    void engageFreezesForAnHour() {
        EmergencyStop stop = new EmergencyStop();
        assertTrue(stop.engage("alice", "磁盘在冒烟", T0));
        assertTrue(stop.isEngaged());
        assertEquals(T0, stop.engagedAt());
        assertEquals(T0.plus(EmergencyStop.COOLDOWN), stop.releasesAt());
        assertEquals(Duration.ofHours(1), stop.remaining(T0));
        assertEquals("alice", stop.actor());
        assertEquals("磁盘在冒烟", stop.reason());
    }

    @Test
    void aSecondEngageDoesNotRestartTheHour() {
        EmergencyStop stop = new EmergencyStop();
        stop.engage("alice", null, T0);
        // Ten minutes later someone presses it again: the release time must not
        // move, or a nervous operator could freeze the panel for ever.
        assertFalse(stop.engage("bob", null, T0.plus(Duration.ofMinutes(10))));
        assertEquals(T0.plus(EmergencyStop.COOLDOWN), stop.releasesAt());
        assertEquals("alice", stop.actor());
    }

    @Test
    void nobodyCanReleaseBeforeTheHourIsUp() {
        EmergencyStop stop = new EmergencyStop();
        stop.engage("alice", null, T0);
        assertNull(stop.release(T0.plus(Duration.ofMinutes(59))));
        assertTrue(stop.isEngaged(), "a refused release leaves the stop engaged");
        // One second early is still early.
        assertNull(stop.release(T0.plus(EmergencyStop.COOLDOWN).minusSeconds(1)));
        assertTrue(stop.isEngaged());
    }

    @Test
    void releaseAfterTheHourLiftsItCompletely() {
        EmergencyStop stop = new EmergencyStop();
        stop.engage("alice", "reason", T0);
        Instant endedAt = stop.release(T0.plus(EmergencyStop.COOLDOWN));
        assertNotNull(endedAt);
        assertFalse(stop.isEngaged());
        assertNull(stop.engagedAt());
        assertNull(stop.releasesAt());
        assertNull(stop.actor());
        assertNull(stop.reason());
        assertEquals(Duration.ZERO, stop.remaining(T0.plus(EmergencyStop.COOLDOWN)));
    }

    @Test
    void theStateLivesInMemoryOnly() {
        // A new object is a new panel boot: the stop is gone. This is the
        // documented recovery path ("restart the process or the container"),
        // so it is asserted rather than left to chance.
        EmergencyStop first = new EmergencyStop();
        first.engage("alice", null, T0);
        assertTrue(first.isEngaged());
        assertFalse(new EmergencyStop().isEngaged());
    }
}
