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
package org.jackhuang.hmcl.util.platform;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Which home directory the panel resolves for its own state.
///
/// The regression this pins: the container runs the panel as root, so
/// `user.home` answers `/root` while every child process (and the operator's
/// mounts) live under `/home/hdsl`. The default workspace became `/root/workspace`
/// — 0750, invisible to the agent — and the terminal died 44 ms after opening
/// because the child could not even `cd` into it. `HOME` is what the children
/// see, so `HOME` is the answer.
class UserHomeTest {

    @Test
    void theEnvironmentWinsOverTheAccountDatabase() {
        Path expected = Path.of("/home/hdsl").toAbsolutePath().normalize();
        assertEquals(expected, UserHome.directory(Map.of("HOME", "/home/hdsl")));
    }

    @Test
    void aBlankHomeFallsBackToUserHome() {
        String userHome = System.getProperty("user.home");
        assertEquals(Path.of(userHome).toAbsolutePath().normalize(),
                UserHome.directory(Map.of("HOME", "   ")));
        assertEquals(Path.of(userHome).toAbsolutePath().normalize(), UserHome.directory(Map.of()));
    }

    @Test
    void theAnswerIsAbsoluteAndNormalized() {
        Path resolved = UserHome.directory(Map.of("HOME", "/home//hdsl/./"));
        assertTrue(resolved.isAbsolute(), resolved.toString());
        assertEquals(Path.of("/home/hdsl").toAbsolutePath().normalize(), resolved);
    }
}
