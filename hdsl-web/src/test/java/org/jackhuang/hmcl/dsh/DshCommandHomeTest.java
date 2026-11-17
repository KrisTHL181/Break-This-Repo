/*
 * HDSL
 * Copyright (C) 2026  HDSL contributors
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
package org.jackhuang.hmcl.dsh;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/// A bare deployment (systemd, `env -i`) has no `HOME`, and npm/pnpm/node
/// lifecycle scripts fail in ways that hide the cause: koffi's cnoke.cjs throws
/// "Missing HOME environment variable" and pnpm only reports
/// ERR_PNPM_EXECUTOR_LIFECYCLE_SCRIPT_FAILED. These tests pin the fallback that
/// keeps such a launcher able to install anything.
class DshCommandHomeTest {

    @Test
    void homeFallbackUsesTheJvmHome() {
        String expected = System.getProperty("user.home");
        Map<String, String> fallback = DshCommand.homeFallback();
        if (expected == null || expected.isBlank()) {
            assertFalse(fallback.containsKey("HOME"), "no JVM home means no fallback");
            return;
        }
        assertEquals(expected, fallback.get("HOME"));
    }

    @Test
    void homeFallbackIsASingleEntry() {
        Map<String, String> fallback = DshCommand.homeFallback();
        assertEquals(fallback.isEmpty() ? 0 : 1, fallback.size());
    }
}
