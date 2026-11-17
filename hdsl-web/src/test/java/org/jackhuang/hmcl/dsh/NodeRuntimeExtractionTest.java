/*
 * HDSL-web
 * Copyright (C) 2026  HDSL contributors
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

import kala.compress.archivers.tar.TarArchiveEntry;
import kala.compress.archivers.tar.TarArchiveOutputStream;
import kala.compress.archivers.tar.TarConstants;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Verifies what unpacking a Node distribution refuses to write, and what it still writes.
///
/// The distribution reaches `npm` and `npx` through links, so links have to be recreated — that is
/// why the extraction does it at all. What an archive must not be able to do is name a link out of
/// the tree it is being unpacked into: the target travels in the archive, so `bin/npm ->
/// /etc/passwd` is a link this launcher would create and everything afterwards would read through.
/// Neither half has another way in — [NodeRuntimeManager#install] downloads its archive over the
/// network — so the entry point is package-visible for these tests to drive.
class NodeRuntimeExtractionTest {

    @Test
    void aLinkThatPointsOutsideTheArchiveIsNotRecreated(@TempDir Path directory) throws Exception {
        Path archive = directory.resolve("node-v22.19.0-linux-x64.tar.gz");
        try (TarArchiveOutputStream tar = new TarArchiveOutputStream(
                new GZIPOutputStream(Files.newOutputStream(archive)))) {
            tarFile(tar, "node-v22/bin/node", "the runtime");
            tarLink(tar, "node-v22/bin/npm", "/etc/passwd");
            // A link that stays inside is the one the runtime is built on, and the fix must not be
            // able to cost it: `npx` is a link to `npm` in every Node distribution.
            tarLink(tar, "node-v22/bin/npx", "npm");
        }

        Path target = directory.resolve("out");
        NodeRuntimeManager.extractArchive(archive, target);

        Path node = target.resolve("node-v22").resolve("bin").resolve("node");
        assertTrue(Files.isRegularFile(node), "a plain file is unpacked as itself");
        assertEquals("the runtime", Files.readString(node));

        Path npm = target.resolve("node-v22").resolve("bin").resolve("npm");
        assertFalse(Files.isSymbolicLink(npm),
                "a link pointing out of the archive must not be recreated: " + npm);
        assertTrue(Files.exists(npm, LinkOption.NOFOLLOW_LINKS),
                "the entry is written as the plain file it is instead");
        assertEquals(0, Files.size(npm), "a link entry carries no content of its own");

        Path npx = target.resolve("node-v22").resolve("bin").resolve("npx");
        assertTrue(Files.isSymbolicLink(npx), "a link that stays inside is still a link");
        assertEquals(Path.of("npm"), Files.readSymbolicLink(npx));
    }

    @Test
    void aZipEntryThatNamesAPathAboveTheTargetIsRefused(@TempDir Path directory) throws Exception {
        Path archive = directory.resolve("node-v22.19.0-win-x64.zip");
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(archive))) {
            zip.putNextEntry(new ZipEntry("node-v22/node.exe"));
            zip.write("the runtime".getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
            zip.putNextEntry(new ZipEntry("../outside.txt"));
            zip.write("not this archive's to write".getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }

        Path target = directory.resolve("out");
        IOException refused = assertThrows(IOException.class,
                () -> NodeRuntimeManager.extractArchive(archive, target));

        assertEquals("the archive names a path outside its own root: ../outside.txt", refused.getMessage());
        assertFalse(Files.exists(directory.resolve("outside.txt")),
                "an entry naming a path above the target is not written above it");
        assertTrue(Files.isRegularFile(target.resolve("node-v22").resolve("node.exe")),
                "and the entries before it are still unpacked");
    }

    /// A zip entry whose parent is a link out of the target is not written through it.
    ///
    /// The entry's own name is the half the check above covers; this is the other half, and it is
    /// not about the archive at all: the target already holds a link, `Files.createDirectories`
    /// treats it as the directory it points at, and the entry would be written on the far side of
    /// it. A zip carries no links, so the link cannot have come from the archive.
    ///
    /// @param directory where the target and the archive are built
    /// @throws Exception when the fixture cannot be built
    @Test
    void aZipEntryUnderAnExistingLinkIsNotWrittenThroughIt(@TempDir Path directory) throws Exception {
        Path outside = Files.createDirectory(directory.resolve("outside"));
        Path target = Files.createDirectory(directory.resolve("out"));
        try {
            Files.createSymbolicLink(target.resolve("sub"), outside);
        } catch (UnsupportedOperationException | IOException e) {
            return; // A filesystem without links cannot test this part.
        }

        Path archive = directory.resolve("node-v22.19.0-win-x64.zip");
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(archive))) {
            zip.putNextEntry(new ZipEntry("sub/inside.txt"));
            zip.write("planted".getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }

        assertThrows(IOException.class, () -> NodeRuntimeManager.extractArchive(archive, target));
        assertFalse(Files.exists(outside.resolve("inside.txt")),
                "the entry was written through the link, outside the target");
    }

    /// And the `.tar.gz` the Linux build ships as, where the link is the top-level directory itself.
    ///
    /// The tar reader refuses a `..` component before extraction starts, so the name half of this is
    /// already decided; the destination half is the same question, and the link is the directory the
    /// whole distribution unpacks into.
    ///
    /// @param directory where the target and the archive are built
    /// @throws Exception when the fixture cannot be built
    @Test
    void aTarEntryUnderAnExistingLinkIsNotWrittenThroughIt(@TempDir Path directory) throws Exception {
        Path outside = Files.createDirectory(directory.resolve("outside"));
        Path target = Files.createDirectory(directory.resolve("out"));
        try {
            Files.createSymbolicLink(target.resolve("node-v22"), outside);
        } catch (UnsupportedOperationException | IOException e) {
            return; // A filesystem without links cannot test this part.
        }

        Path archive = directory.resolve("node-v22.19.0-linux-x64.tar.gz");
        try (TarArchiveOutputStream tar = new TarArchiveOutputStream(
                new GZIPOutputStream(Files.newOutputStream(archive)))) {
            tarFile(tar, "node-v22/sub/inside.txt", "planted");
        }

        assertThrows(IOException.class, () -> NodeRuntimeManager.extractArchive(archive, target));
        assertFalse(Files.exists(outside.resolve("sub/inside.txt")),
                "the entry was written through the link, outside the target");
    }

    /// Adds a plain file to an archive.
    ///
    /// @param tar     the archive being written
    /// @param name    the entry name
    /// @param content what the file holds
    private static void tarFile(TarArchiveOutputStream tar, String name, String content) throws IOException {
        byte[] body = content.getBytes(StandardCharsets.UTF_8);
        TarArchiveEntry entry = new TarArchiveEntry(name);
        entry.setSize(body.length);
        tar.putArchiveEntry(entry);
        tar.write(body);
        tar.closeArchiveEntry();
    }

    /// Adds a symbolic link to an archive.
    ///
    /// @param tar    the archive being written
    /// @param name   the entry name
    /// @param target where the link points, which the archive carries verbatim
    private static void tarLink(TarArchiveOutputStream tar, String name, String target) throws IOException {
        TarArchiveEntry entry = new TarArchiveEntry(name, TarConstants.LF_SYMLINK);
        entry.setLinkName(target);
        tar.putArchiveEntry(entry);
        tar.closeArchiveEntry();
    }
}