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
package org.jackhuang.hmcl.web.terminal.pty;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The argv rules that keep a shell (and a typo) out of the terminal chain.
///
/// These run on every platform: what is being pinned is the string decision, not
/// a Windows call.
class ConPtyGuardTest {

    @Test
    void aCommandFileIsRefused() {
        assertRefused("C:\\tools\\npm.cmd", ".cmd");
        assertRefused("C:\\tools\\build.bat", ".bat");
        assertRefused("C:\\tools\\run.ps1", ".ps1");
        assertRefused("C:\\tools\\thing.com", ".com");
    }

    @Test
    void theSuffixTestIsCaseInsensitive() {
        assertRefused("C:\\Tools\\NPM.CMD", ".cmd");
    }

    @Test
    void theRefusalNamesTheFileAndTheReason() throws IOException {
        PtyException refused = assertThrows(PtyException.class,
                () -> ConPty.validateProgram("C:\\tools\\npm.cmd"));
        assertTrue(refused.getMessage().contains("npm.cmd"), refused.getMessage());
        assertTrue(refused.getMessage().contains("cmd.exe"), refused.getMessage());
        assertTrue(refused.getMessage().contains("shell"), refused.getMessage());
    }

    @Test
    void aMissingProgramIsRefused() {
        Path missing = Path.of(System.getProperty("java.io.tmpdir")).toAbsolutePath()
                .resolve("hdsl-conpty-no-such-program-" + System.nanoTime());
        assertTrue(missing.isAbsolute(), missing.toString());
        PtyException refused = assertThrows(PtyException.class,
                () -> ConPty.validateProgram(missing.toString()));
        assertTrue(refused.getMessage().contains("no such program"), refused.getMessage());
    }

    @Test
    void aBareNameIsRefused() {
        // CreateProcess would search PATH for it; the terminal does not, because
        // an argv the adapter resolved is the only program this may start.
        PtyException refused = assertThrows(PtyException.class, () -> ConPty.validateProgram("node.exe"));
        assertTrue(refused.getMessage().contains("absolute"), refused.getMessage());
    }

    @Test
    void anExistingAbsoluteProgramIsAccepted() throws IOException {
        Path program = Files.createTempFile("hdsl-conpty-program-", ".exe");
        try {
            ConPty.validateProgram(program.toAbsolutePath().toString());
        } finally {
            Files.deleteIfExists(program);
        }
    }

    /// The guard must also be reachable through `spawn`, which is what the
    /// terminal route calls: on a machine without ConPTY the platform check runs
    /// first, so this only asserts the refusal is a [PtyException] either way.
    @Test
    void spawnRefusesACommandFileBeforeStartingAnything() {
        PtyException refused = assertThrows(PtyException.class, () -> ConPty.spawn(
                java.util.List.of("C:\\tools\\npm.cmd"), null, Map.of(), new PtySize(80, 24)));
        assertTrue(refused.getMessage().contains("npm.cmd")
                        || refused.getMessage().contains("Windows 10 1809"),
                refused.getMessage());
    }

    private static void assertRefused(String program, String suffix) {
        PtyException refused = assertThrows(PtyException.class, () -> ConPty.validateProgram(program));
        assertTrue(refused.getMessage().contains(program), refused.getMessage());
        assertTrue(refused.getMessage().contains(suffix), refused.getMessage());
    }
}
