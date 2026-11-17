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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The clock behind the fast gear: authenticated use for five minutes, then
/// back to the slow one.
class SystemActivityTest {

    @BeforeEach
    @AfterEach
    void reset() {
        SystemActivity.reset();
        SystemActivity.install(null);
    }

    @Test
    void anAuthenticatedRequestKeepsTheFastGearForFiveMinutes() {
        Instant start = Instant.now();
        assertFalse(SystemActivity.active(start), "nobody has used the panel yet");

        SystemActivity.touch();
        Instant touched = Instant.now();
        assertTrue(SystemActivity.active(touched));
        assertTrue(SystemActivity.active(touched.plus(Duration.ofMinutes(4))));
        assertFalse(SystemActivity.active(touched.plus(Duration.ofMinutes(5)).plusSeconds(1)),
                "five minutes of silence is the end of the fast gear");
    }

    @Test
    void touchingWakesTheSampler() {
        int[] woke = {0};
        SystemActivity.install(() -> woke[0]++);
        SystemActivity.touch();
        SystemActivity.touch();
        assertTrue(woke[0] == 2);
    }
}
