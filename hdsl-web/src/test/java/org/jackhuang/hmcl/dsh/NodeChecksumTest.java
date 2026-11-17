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

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/// The checksum half: parsing a `SHASUMS256.txt` line and hashing the archive.
class NodeChecksumTest {

    @Test
    void findsTheLineForTheArchive() {
        String text = "aaa  node-v22.1.0-linux-x64.tar.xz\n"
                + "bbb  node-v22.1.0-win-x64.zip\n";
        assertEquals("aaa", NodeRuntimeManager.checksumFrom(text, "node-v22.1.0-linux-x64.tar.xz"));
        assertEquals("bbb", NodeRuntimeManager.checksumFrom(text, "node-v22.1.0-win-x64.zip"));
        assertNull(NodeRuntimeManager.checksumFrom(text, "node-v22.1.0-darwin-arm64.tar.gz"));
        // Some lists mark binary entries with a leading asterisk.
        assertEquals("ccc", NodeRuntimeManager.checksumFrom("ccc *node.tgz\n", "node.tgz"));
    }

    @Test
    void hashesAFile(@TempDir Path directory) throws Exception {
        Path file = directory.resolve("abc.txt");
        Files.writeString(file, "abc");
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
                NodeRuntimeManager.sha256(file));
    }
}
