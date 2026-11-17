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
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/// Which `bash.exe` an agent on Windows may be pointed at.
///
/// The interesting half is the one that must **not** be picked: Windows ships
/// `%SystemRoot%\System32\bash.exe` on every machine, Git or no Git, and it
/// starts a WSL distribution instead of a shell in the instance workspace. An
/// agent handed that one runs somewhere else entirely.
class GitBashTest {

    @TempDir
    Path temp;

    @Test
    void aRealBashIsPicked() throws Exception {
        assumeTrue(OperatingSystem.CURRENT_OS == OperatingSystem.WINDOWS, "the search is Windows-only");
        Path bash = Files.createFile(temp.resolve("bash.exe"));
        assertEquals(bash.toAbsolutePath(), GitBash.pick(List.of(bash)).orElseThrow());
    }

    @Test
    void theWslLauncherIsNeverPicked() {
        assumeTrue(OperatingSystem.CURRENT_OS == OperatingSystem.WINDOWS, "the shims are Windows-only");
        String systemRoot = System.getenv("SystemRoot");
        assumeTrue(systemRoot != null && !systemRoot.isBlank(), "Windows without SystemRoot");
        Path shim = Path.of(systemRoot, "System32", "bash.exe");
        assertTrue(Files.isRegularFile(shim), "Windows ships this one: " + shim);
        assertTrue(GitBash.pick(List.of(shim)).isEmpty(),
                "System32\\bash.exe is the WSL launcher, not a shell in the workspace");
    }

    @Test
    void aMissingFileIsSkipped() {
        assumeTrue(OperatingSystem.CURRENT_OS == OperatingSystem.WINDOWS, "the search is Windows-only");
        Path missing = temp.resolve("bash.exe");
        assertFalse(Files.exists(missing));
        assertTrue(GitBash.pick(List.of(missing)).isEmpty());
    }

    @Test
    void aFileWithAnotherNameIsSkipped() throws Exception {
        assumeTrue(OperatingSystem.CURRENT_OS == OperatingSystem.WINDOWS, "the search is Windows-only");
        Path other = Files.createFile(temp.resolve("bash")); // no .exe: not startable here
        assertTrue(GitBash.pick(List.of(other)).isEmpty());
    }

    /// On a machine that has Git for Windows, the answer is that Git's bash — never
    /// a shim, and never something that does not exist.
    @Test
    void findAnswersWithSomethingStartableOrNothing() {
        assumeTrue(OperatingSystem.CURRENT_OS == OperatingSystem.WINDOWS, "the search is Windows-only");
        GitBash.find().ifPresent(bash -> {
            assertTrue(Files.isRegularFile(bash), bash.toString());
            assertTrue(bash.getFileName().toString().equalsIgnoreCase("bash.exe"), bash.toString());
            String normalized = bash.toString().toLowerCase(java.util.Locale.ROOT).replace('\\', '/');
            assertFalse(normalized.contains("/windows/system32/"), bash.toString());
            assertFalse(normalized.contains("/windowsapps/"), bash.toString());
        });
    }

    @Test
    void otherPlatformsHaveNoSearch() {
        assumeTrue(OperatingSystem.CURRENT_OS != OperatingSystem.WINDOWS, "Windows has the search");
        assertTrue(GitBash.find().isEmpty());
    }
}
