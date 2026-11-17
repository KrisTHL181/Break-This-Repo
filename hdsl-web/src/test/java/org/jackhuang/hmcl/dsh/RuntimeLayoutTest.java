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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The two shapes a Node.js distribution is unpacked in.
///
/// The Windows shape is asserted **on any machine**: [RuntimeLayout] takes the platform
/// as a parameter, which is why it does. The names are not cosmetic — a Windows build has
/// `node.exe` at the root and `npm.cmd` beside it, while a POSIX build keeps them in
/// `bin/`; asking for `bin/npm` there does not fail loudly, it simply finds nothing.
class RuntimeLayoutTest {

    @Test
    void posixKeepsEverythingUnderBin(@TempDir Path distribution) throws IOException {
        RuntimeLayout layout = RuntimeLayout.POSIX;
        assertEquals(distribution.resolve("bin/node"), layout.node(distribution));
        assertEquals(distribution.resolve("bin/npm"), layout.npm(distribution));
        assertEquals(distribution.resolve("bin/npx"), layout.npx(distribution));
        assertEquals(distribution.resolve("bin/pnpm"), layout.pnpm(distribution));
        assertEquals(distribution.resolve("bin"), layout.binDirectory(distribution),
                "a POSIX distribution finds its executables through bin/");

        assertFalse(layout.isUsable(distribution), "an empty directory is not a runtime");
        Files.createDirectories(distribution.resolve("bin"));
        Files.createFile(distribution.resolve("bin/node"));
        assertTrue(layout.isUsable(distribution));
    }

    @Test
    void windowsPutsThemAtTheRoot(@TempDir Path distribution) throws IOException {
        RuntimeLayout layout = RuntimeLayout.WINDOWS;
        assertEquals(distribution.resolve("node.exe"), layout.node(distribution));
        assertEquals(distribution.resolve("npm.cmd"), layout.npm(distribution));
        assertEquals(distribution.resolve("npx.cmd"), layout.npx(distribution));
        assertEquals(distribution.resolve("pnpm.cmd"), layout.pnpm(distribution));
        assertEquals(distribution, layout.binDirectory(distribution),
                "a Windows shim finds node.exe beside itself, so the root goes on PATH");

        assertFalse(layout.isUsable(distribution));
        Files.createFile(distribution.resolve("node.exe"));
        assertTrue(layout.isUsable(distribution));
    }

    @Test
    void aPosixDistributionIsNotUsableThroughTheWindowsLayout(@TempDir Path distribution)
            throws IOException {
        Files.createDirectories(distribution.resolve("bin"));
        Files.createFile(distribution.resolve("bin/node"));
        assertTrue(RuntimeLayout.POSIX.isUsable(distribution));
        assertFalse(RuntimeLayout.WINDOWS.isUsable(distribution),
                "the Windows layout must not mistake bin/node for a runtime it can run");
    }
}
