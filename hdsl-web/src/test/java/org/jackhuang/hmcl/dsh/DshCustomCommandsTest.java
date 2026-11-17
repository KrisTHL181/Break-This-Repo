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
package org.jackhuang.hmcl.dsh;

import org.jackhuang.hmcl.util.platform.OperatingSystem;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/// The interpreter a custom instance command is handed to — both systems' forms, from
/// either machine.
class DshCustomCommandsTest {

    @Test
    void windowsUsesCmd() {
        assertEquals(List.of("cmd", "/c", "echo hi && dir"),
                DshCustomCommands.shellCommand("echo hi && dir", OperatingSystem.WINDOWS),
                "cmd /c takes the whole line as one argument; the line is not re-quoted");
    }

    @Test
    void posixUsesSh() {
        assertEquals(List.of("/bin/sh", "-c", "echo hi && dir"),
                DshCustomCommands.shellCommand("echo hi && dir", OperatingSystem.LINUX),
                "unchanged from before the Windows work");
    }
}
