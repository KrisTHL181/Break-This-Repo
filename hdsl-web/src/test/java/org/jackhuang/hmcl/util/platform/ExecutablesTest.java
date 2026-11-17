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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// How a program name becomes a file to start, under both naming rules.
///
/// The Windows rules are asserted **on this machine** because the rules are a parameter:
/// what is executable by extension, and that a `.cmd` is preferred over the extensionless
/// POSIX script beside it, are exactly the two things a Linux-only CI could never see.
/// The POSIX rules are asserted too, because they are the behaviour that must not change.
class ExecutablesTest {

    private static final Executables.NameRules WINDOWS =
            new Executables.NameRules(true, List.of(".exe", ".cmd"));
    private static final Executables.NameRules POSIX =
            new Executables.NameRules(false, List.of());

    // ---------------------------------------------------------------- Windows --

    @Test
    void windowsPrefersTheCmdOverTheExtensionlessScript(@TempDir Path bin) throws IOException {
        // A Node.js installation ships both: `npm` is the POSIX shell script and `npm.cmd`
        // the Windows shim. Picking the former is `CreateProcess error=193`.
        Files.writeString(bin.resolve("npm"), "#!/bin/sh\n");
        Files.writeString(bin.resolve("npm.cmd"), "@echo off\r\n");

        Optional<Path> resolved = Executables.resolve("npm", bin.toString(), WINDOWS);
        assertTrue(resolved.isPresent(), "npm.cmd is startable");
        assertEquals(bin.resolve("npm.cmd"), resolved.orElseThrow());
    }

    @Test
    void windowsFallsBackToAnExtensionlessBinary(@TempDir Path bin) throws IOException {
        Files.writeString(bin.resolve("mytool"), "binary");
        assertEquals(Optional.of(bin.resolve("mytool")),
                Executables.resolve("mytool", bin.toString(), WINDOWS),
                "a native binary that happens to have no extension is the last resort");
    }

    @Test
    void windowsSearchesEveryPathEntry(@TempDir Path first, @TempDir Path second)
            throws IOException {
        Files.writeString(second.resolve("node.exe"), "MZ");
        assertEquals(Optional.of(second.resolve("node.exe")),
                Executables.resolve("node",
                        first + java.io.File.pathSeparator + second, WINDOWS));
    }

    @Test
    void aNameWithADirectoryIsNotSearchedOnPath(@TempDir Path bin) throws IOException {
        Files.writeString(bin.resolve("node.exe"), "MZ");
        assertFalse(Executables.resolve("./does-not-exist", bin.toString(), WINDOWS).isPresent(),
                "a name carrying a separator is a path; it must not be appended to PATH entries");
    }

    @Test
    void theChildsPathIsWhatCounts(@TempDir Path runtimeBin) throws IOException {
        // A managed runtime is prepended through the child's environment; resolving
        // against the panel's own PATH would miss it entirely.
        Path npm = runtimeBin.resolve("npm");
        Files.writeString(npm, "#!/bin/sh\n");
        assertTrue(npm.toFile().setExecutable(true),
                "a real runtime's npm is executable");

        assertEquals(List.of(npm.toRealPath().toString(), "ci", "--no-audit"),
                Executables.startable(List.of("npm", "ci", "--no-audit"),
                        runtimeBin.toString()),
                "the program is rewritten to the one the child will find");
        assertEquals(List.of("definitely-not-on-any-path", "--version"),
                Executables.startable(List.of("definitely-not-on-any-path", "--version"),
                        runtimeBin.toString()),
                "an unresolvable name is left untouched, so the error names what was asked for");
    }

    // ------------------------------------------------------------------ POSIX --

    @Test
    void posixRequiresTheExecuteBit(@TempDir Path bin) throws IOException {
        Path plain = Files.writeString(bin.resolve("npm"), "#!/bin/sh\n");
        assertFalse(Executables.resolve("npm", bin.toString(), POSIX).isPresent(),
                "a file without the execute bit is not a program");

        assertTrue(plain.toFile().setExecutable(true));
        assertEquals(Optional.of(plain.toRealPath()),
                Executables.resolve("npm", bin.toString(), POSIX));
    }

    @Test
    void posixAnswersWithTheRealPath(@TempDir Path bin) throws IOException {
        Path real = Files.writeString(bin.resolve("real-tool"), "#!/bin/sh\n");
        assertTrue(real.toFile().setExecutable(true));
        Path link = bin.resolve("tool");
        try {
            Files.createSymbolicLink(link, real.getFileName());
        } catch (IOException | UnsupportedOperationException e) {
            org.junit.jupiter.api.Assumptions.assumeTrue(false,
                    "this filesystem does not support symbolic links");
        }

        assertEquals(Optional.of(real.toRealPath()),
                Executables.resolve("tool", bin.toString(), POSIX),
                "a link resolves to its target — what `which` has always returned");
    }

    @Test
    void posixDoesNotInventExtensions(@TempDir Path bin) throws IOException {
        Path cmd = Files.writeString(bin.resolve("npm.cmd"), "@echo off\r\n");
        assertTrue(cmd.toFile().setExecutable(true));
        assertEquals(Optional.of(cmd.toRealPath()),
                Executables.resolve("npm.cmd", bin.toString(), POSIX),
                "an explicitly named file is taken as it is");
        assertFalse(Executables.resolve("npm", bin.toString(), POSIX).isPresent(),
                "a POSIX name is completed by nothing");
    }
}
