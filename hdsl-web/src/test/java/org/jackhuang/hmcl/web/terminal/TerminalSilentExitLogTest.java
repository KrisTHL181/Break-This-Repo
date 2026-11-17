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
package org.jackhuang.hmcl.web.terminal;

import org.jackhuang.hmcl.util.logging.Logger;
import org.jackhuang.hmcl.util.platform.OperatingSystem;
import org.jackhuang.hmcl.web.terminal.pty.Pty;
import org.jackhuang.hmcl.web.terminal.pty.PtySize;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/// The app-log half of the silent-failure diagnostic: a child that exits
/// non-zero without ever writing a byte must leave one line naming the instance
/// and the exit code, because the audit line and the "WebSocket closed" line
/// leave a person with nothing to act on. A child that printed something first
/// is an ordinary non-zero exit and must not be flagged.
///
/// The diagnostic goes through the process-wide [Logger#LOG], and this project
/// has no logger injection, so the test starts that log on a temporary folder —
/// which is why this is a class of its own rather than a case beside the other
/// session tests.
class TerminalSilentExitLogTest {

    private static final Map<String, String> ENV = Map.of(
            "PATH", "/usr/local/bin:/usr/bin:/bin",
            "TERM", "xterm-256color");

    @BeforeAll
    static void linuxOnly() {
        assumeTrue(OperatingSystem.CURRENT_OS == OperatingSystem.LINUX, "PTY tests run on Linux");
    }

    @Test
    void aSilentNonZeroExitIsDiagnosedInTheAppLog(@TempDir Path logFolder) throws Exception {
        Logger.LOG.start(logFolder);
        try {
            assertEquals(7, run("exit 7", "silent-instance"));
            assertEquals(7, run("printf hi; exit 7", "chatty-instance"));

            // getLogs flushes the raw log before handing it over, and both
            // diagnostics were queued before their exits were reported.
            String logs = Logger.LOG.getLogs();
            assertTrue(logs.contains("Terminal silent-instance exited with code 7 without producing any output"),
                    "the silent failure must leave one actionable line: " + logs);
            assertFalse(logs.contains("chatty-instance"),
                    "a child that printed something is an ordinary exit and must not be flagged: " + logs);
        } finally {
            Logger.LOG.shutdown();
        }
    }

    /// Runs one child to its end and returns its exit code.
    private static int run(String command, String instanceId) throws IOException {
        Pty pty = Pty.spawn(List.of("/bin/sh", "-c", command), Path.of("/tmp"), ENV, new PtySize(80, 24));
        TerminalSession session = new TerminalSession(instanceId, pty, 4096);
        try {
            return session.waitFor();
        } finally {
            session.close();
        }
    }
}