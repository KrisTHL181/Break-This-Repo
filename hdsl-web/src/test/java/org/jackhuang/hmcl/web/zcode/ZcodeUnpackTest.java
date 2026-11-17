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
package org.jackhuang.hmcl.web.zcode;

import kala.compress.archivers.tar.TarArchiveEntry;
import kala.compress.archivers.tar.TarArchiveOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.GZIPOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Unpacking a source tarball, which is where the Windows build broke.
///
/// ZCode's repository holds `docs/ZCode 项目架构分析与改进建议.md`. The GitHub tarball
/// stores that name as UTF-8 and the system's tar on Windows (libarchive) decodes it
/// through the ANSI code page, which on a GBK machine fails the whole extraction with
/// `Invalid empty pathname` after most of the tree is written. The panel unpacks with
/// its own reader now; this pins that a non-ASCII name survives, that the archive's
/// top-level directory is dropped, and that a name climbing out of the tree is refused.
class ZcodeUnpackTest {

    @TempDir
    Path temp;

    @Test
    void aChineseNameIsUnpackedWithThatName() throws Exception {
        String name = "docs/ZCode 项目架构分析与改进建议.md";
        Path tarball = tar(temp.resolve("source.tar.gz"), "ZCode-main/" + name, "架构说明\n");

        Path target = temp.resolve("src");
        ZcodeBuilder.unpack(tarball, target);

        Path unpacked = target.resolve(name);
        assertTrue(Files.isRegularFile(unpacked), "unpacked tree: " + listing(target));
        assertEquals("架构说明\n", Files.readString(unpacked, StandardCharsets.UTF_8));
    }

    @Test
    void theArchivesTopLevelDirectoryIsDropped() throws Exception {
        Path tarball = tar(temp.resolve("source.tar.gz"), "ZCode-main/package.json", "{}\n");
        Path target = temp.resolve("src");
        ZcodeBuilder.unpack(tarball, target);

        assertTrue(Files.isRegularFile(target.resolve("package.json")), listing(target));
        assertFalse(Files.exists(target.resolve("ZCode-main")),
                "the wrapper directory is what --strip-components=1 removed");
    }

    @Test
    void aNameThatClimbsOutOfTheTreeIsRefused() throws Exception {
        Path tarball = tar(temp.resolve("source.tar.gz"), "ZCode-main/../../evil.txt", "x");
        assertThrows(IOException.class, () -> ZcodeBuilder.unpack(tarball, temp.resolve("src")));
        assertFalse(Files.exists(temp.resolve("evil.txt")));
    }

    /// Writes one-entry `.tar.gz` with a UTF-8 name, the way GitHub's tarballs store it.
    private static Path tar(Path file, String name, String content) throws IOException {
        try (OutputStream out = Files.newOutputStream(file);
             GZIPOutputStream gzip = new GZIPOutputStream(out);
             TarArchiveOutputStream tar = new TarArchiveOutputStream(gzip, StandardCharsets.UTF_8)) {
            byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
            TarArchiveEntry entry = new TarArchiveEntry(name);
            entry.setSize(bytes.length);
            tar.putArchiveEntry(entry);
            tar.write(bytes, 0, bytes.length);
            tar.closeArchiveEntry();
        }
        return file;
    }

    private static String listing(Path root) {
        try (var stream = Files.walk(root)) {
            return stream.map(path -> root.relativize(path).toString()).toList().toString();
        } catch (IOException e) {
            return "(unreadable: " + e.getMessage() + ")";
        }
    }
}
