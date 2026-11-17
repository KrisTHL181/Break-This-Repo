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
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.function.LongSupplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class McFeatureStateTest {

    private static final class Clock implements LongSupplier {
        private long now;

        @Override
        public long getAsLong() {
            return now;
        }

        private void advance(Duration duration) {
            now += duration.toNanos();
        }
    }

    private static McConsent consent() {
        return new McConsent(3, "sha256:terms", "sha256:eula",
                "2026-10-29T00:58:00Z", "2026-10-29T01:00:00Z", "2026-10-29T01:00:00Z",
                "tester", "127.0.0.1", "scroll+checkbox", true, "1.4.0",
                "Mozilla/5.0", "abc1234", null);
    }

    private static McFeatureState state(Path dir, Clock clock) {
        return new McFeatureState(dir.resolve("feature.json"), clock, McFeatureState.LOCK_DURATION);
    }

    @Test
    void startsDisabled(@TempDir Path dir) {
        McFeatureState state = state(dir, new Clock());
        assertEquals(McFeatureState.State.DISABLED, state.state());
        assertFalse(state.enabled());
        assertFalse(state.locked());
        assertTrue(state.consent().isEmpty());
    }

    @Test
    void anEnabledFeatureAndItsConsentSurviveAReopen(@TempDir Path dir) throws Exception {
        Clock clock = new Clock();
        McFeatureState state = state(dir, clock);
        state.enable(consent());

        McFeatureState reopened = new McFeatureState(dir.resolve("feature.json"));
        assertTrue(reopened.enabled());
        assertEquals(3, reopened.consent().orElseThrow().termsVersion());
        assertEquals("tester", reopened.consent().orElseThrow().acceptedBy());
    }

    @Test
    void theLockRefusesAnEnableAndCountsDown(@TempDir Path dir) throws Exception {
        Clock clock = new Clock();
        McFeatureState state = state(dir, clock);
        state.enable(consent());

        assertTrue(state.stopFeature());
        assertTrue(state.locked());
        assertEquals(3600, state.lockRemainingSeconds());

        McFeatureState.LockedException refused = assertThrows(McFeatureState.LockedException.class,
                () -> state.enable(consent()));
        assertTrue(refused.remainingSeconds() > 3500 && refused.remainingSeconds() <= 3600);

        clock.advance(Duration.ofMinutes(30));
        assertEquals(1800, state.lockRemainingSeconds());
    }

    @Test
    void keepingTheFeatureDoesNotShortenTheLock(@TempDir Path dir) throws Exception {
        Clock clock = new Clock();
        McFeatureState state = state(dir, clock);
        state.enable(consent());
        state.stopFeature();

        state.decideKeep();
        assertTrue(state.enabled(), "keeping the feature leaves the state as it was");
        assertTrue(state.locked(), "the hour still runs");
        assertEquals(Boolean.TRUE, state.keepFeatureChoice());

        clock.advance(McFeatureState.LOCK_DURATION);
        assertFalse(state.locked());
        assertNull(state.keepFeatureChoice(), "the decision is dropped with the lock");
        assertTrue(state.enabled());
    }

    @Test
    void closingDuringTheLockKeepsTheStateDisabledUntilItExpires(@TempDir Path dir) throws Exception {
        Clock clock = new Clock();
        McFeatureState state = state(dir, clock);
        state.enable(consent());
        state.stopFeature();
        state.confirmClose();

        assertEquals(McFeatureState.State.DISABLED, state.state());
        assertTrue(state.locked());
        assertThrows(McFeatureState.LockedException.class, () -> state.enable(consent()));

        clock.advance(McFeatureState.LOCK_DURATION);
        assertFalse(state.locked());
        state.enable(consent());
        assertTrue(state.enabled());
    }

    @Test
    void pressingStopTwiceDoesNotRestartTheHour(@TempDir Path dir) throws Exception {
        Clock clock = new Clock();
        McFeatureState state = state(dir, clock);
        state.enable(consent());
        assertTrue(state.stopFeature());

        clock.advance(Duration.ofMinutes(30));
        assertFalse(state.stopFeature(), "a second press must not extend the lock");
        clock.advance(Duration.ofMinutes(30));
        assertFalse(state.locked());
    }

    @Test
    void aRestartClearsTheLock(@TempDir Path dir) throws Exception {
        Clock clock = new Clock();
        McFeatureState state = state(dir, clock);
        state.enable(consent());
        state.stopFeature();
        assertTrue(state.locked());

        McFeatureState afterRestart = state(dir, clock);
        assertFalse(afterRestart.locked(), "the lock lives in memory only");
        assertTrue(afterRestart.enabled(), "the persisted state is untouched");
    }

    @Test
    void thePersistedFileCarriesNoLockFields(@TempDir Path dir) throws Exception {
        Clock clock = new Clock();
        McFeatureState state = state(dir, clock);
        state.enable(consent());
        state.stopFeature();
        state.confirmClose();

        String json = Files.readString(dir.resolve("feature.json"));
        assertTrue(json.contains("DISABLED"));
        assertFalse(json.contains("lock"), "the lock must never be written down");
        assertFalse(json.contains("lockedAt"));
    }
}