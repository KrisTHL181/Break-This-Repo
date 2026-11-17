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
package org.jackhuang.hmcl.util;

import org.jackhuang.hmcl.util.platform.OperatingSystem;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The difference between stopping a launcher and stopping what it launched: a
/// shell that keeps a child alive must take the child down too, or the child goes
/// on holding the port. The case is reproduced with `/bin/sh`; the Windows shape
/// (`cmd.exe` → `opencode.exe`) cannot lend its child PID the same way, so that
/// half is verified on a real machine (docs/windows-bare-deploy.md §9.2).
class ProcessTreeTest {

    @Test
    void stopsTheChildTheLauncherLeftBehind() throws Exception {
        Assumptions.assumeTrue(OperatingSystem.CURRENT_OS != OperatingSystem.WINDOWS,
                "needs a POSIX shell to leave a child behind");

        Process process = new ProcessBuilder("/bin/sh", "-c", "sleep 60 & echo $!; wait").start();
        ProcessHandle child;
        try (BufferedReader output = process.inputReader()) {
            String line = output.readLine();
            assertTrue(line != null, "the shell reports the child's pid");
            child = ProcessHandle.of(Long.parseLong(line.trim())).orElseThrow();
        }
        assertTrue(child.isAlive(), "the child runs before the stop");

        ProcessTree.terminate(process, 2);

        assertFalse(process.isAlive(), "the launcher itself is gone");
        assertFalse(child.isAlive(), "and so is the child it left behind");
    }
}
