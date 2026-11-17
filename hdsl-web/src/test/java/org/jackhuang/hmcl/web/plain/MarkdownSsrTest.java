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
package org.jackhuang.hmcl.web.plain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The managed Node lookup: a bare Windows install has Node under the panel's
/// data directory and nothing on PATH, so this is what makes SSR work there.
class MarkdownSsrTest {

    @Test
    void managedNodePicksTheNewestExecutable(@TempDir Path dataDir) throws Exception {
        Path older = node(dataDir, "22.19.0", Instant.parse("2026-01-01T00:00:00Z"));
        Path newer = node(dataDir, "24.4.0", Instant.parse("2026-06-01T00:00:00Z"));

        assertEquals(newer, MarkdownSsr.managedNode(dataDir));
        assertTrue(Files.isRegularFile(older), "the older runtime is still a valid file");
    }

    @Test
    void managedNodeIgnoresDirectoriesWithoutAnExecutable(@TempDir Path dataDir) throws Exception {
        Files.createDirectories(dataDir.resolve("hdsl/runtimes/26.0.0"));
        assertNull(MarkdownSsr.managedNode(dataDir));
    }

    @Test
    void managedNodeIsNullWithoutTheRuntimeDirectory(@TempDir Path dataDir) {
        assertNull(MarkdownSsr.managedNode(dataDir));
    }

    private static Path node(Path dataDir, String version, Instant modified) throws Exception {
        Path dir = dataDir.resolve("hdsl").resolve("runtimes").resolve(version);
        Files.createDirectories(dir);
        Path executable = dir.resolve("node");
        Files.writeString(executable, "#!/bin/sh\nexit 0\n");
        if (!executable.toFile().setExecutable(true)) {
            throw new IllegalStateException("could not mark " + executable + " executable");
        }
        Files.setLastModifiedTime(executable, FileTime.from(modified));
        return executable;
    }
}
