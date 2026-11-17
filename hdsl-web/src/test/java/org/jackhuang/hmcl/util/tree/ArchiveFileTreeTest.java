/*
 * HDSL
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
package org.jackhuang.hmcl.util.tree;

import kala.compress.archivers.tar.TarArchiveEntry;
import kala.compress.archivers.tar.TarArchiveOutputStream;
import kala.compress.archivers.tar.TarArchiveReader;
import org.jackhuang.hmcl.util.io.CompressingUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Verifies that an archive cannot name an entry that lands outside what is being unpacked.
///
/// An extractor walks the tree this class builds and writes each name under the directory it is
/// unpacking into, so a name is a **path**, and a path is what an archive chooses for itself. What
/// made this a defect is the platform difference: `..\\..\\Startup\\x.bat` carries no `/` at all, so
/// it was one "file name" to the split — and three directories to the Windows path parser the
/// extractor then handed it to. The same archive is inert on Linux and macOS, which is why nothing
/// caught it.
///
/// The names below are the shapes that reach the platform's parser as more than a name. A malformed
/// archive is not an error anywhere: the entry is left out and the rest of the archive still
/// unpacks, which is what the valid entry in each case is here to pin.
class ArchiveFileTreeTest {

    @Test
    void anEntryThatWouldLeaveTheDirectoryIsNotInTheTree(@TempDir Path directory) throws Exception {
        Map<String, String> entries = escaping();
        entries.put("node-v22/bin/node", "the runtime");
        entries.put("a/./b.txt", "a walk that stays inside");
        Path archive = zip(directory, entries);

        // Read through the tree rather than through `getEntry`: a zip tree answers a name it knows
        // straight out of the archive, so a `getEntry` that returns something says what the archive
        // holds and not what an extractor would walk — which is the whole question here.
        List<String> names = new ArrayList<>();
        try (ArchiveFileTree<?, ?> tree = CompressingUtils.openZipTree(archive)) {
            collect(tree.getRoot(), "", names);
        }

        assertTrue(names.contains("node-v22/bin/node"),
                "an entry that stays inside is in the tree for an extractor to write: " + names);
        for (String name : escaping().keySet()) {
            assertFalse(names.contains(name),
                    name + " is a path that leaves the directory being unpacked, so it must not be"
                            + " in the tree for an extractor to write: " + names);
        }
    }

    @Test
    void everyNameInTheTreeIsOneComponentOfAPath(@TempDir Path directory) throws Exception {
        // What the extractors rely on: they resolve each name under the current directory, so a name
        // that is itself a path — or that steps out of one — is the whole of the defect. Asked of
        // every name in the tree rather than of the names above, because the archive is what chooses
        // them and the walk here is what an extractor does.
        Map<String, String> entries = escaping();
        entries.put("node-v22/bin/node", "the runtime");
        entries.put("a/./b.txt", "a walk that stays inside");
        Path archive = zip(directory, entries);

        List<String> names = new ArrayList<>();
        try (ArchiveFileTree<?, ?> tree = CompressingUtils.openZipTree(archive)) {
            collect(tree.getRoot(), "", names);
        }

        // The walk reads the names a directory at a time, and each one is the single name of an entry
        // under the directory it was found in. `.` is the one component that is allowed to travel —
        // the walk steps over it, as it always has — so what may not travel is a step out of the
        // directory, and a `\` that Windows reads as one where this split does not. That is the
        // property every extractor leans on when it resolves a name.
        Path root = Path.of("root").toAbsolutePath();
        assertTrue(names.contains("node-v22/bin/node"), "the walk reads the names: " + names);
        assertTrue(names.contains("a/./b.txt"), "and carries a `.` through as it always has: " + names);
        for (String name : names) {
            assertFalse(name.split("/", -1)[0].isEmpty() || name.equals(".."),
                    name + " is not a name under the directory it was found in");
            for (String component : name.split("/", -1)) {
                assertFalse(component.equals(".."), name + " steps out of the directory being unpacked");
                assertFalse(component.contains("\\"),
                        name + " carries a component a Windows path parser reads as a separator");
            }
            assertTrue(root.resolve(name).normalize().startsWith(root),
                    name + " resolves outside the directory being unpacked");
        }
    }

    /// PORT-side case: the zip reader this project uses refuses a name carrying `..` or `\` before the
    /// tree ever sees it — the archive keeps the name while the tree never holds it, which the test
    /// above pins — so a zip cannot show what the whole-name check is for. A tar archive can, and it is
    /// the same walk that reads both: before the check, `..\..\evil.bat` entered the tree as one "file
    /// name" that Windows then read as three directories.
    ///
    /// @param directory where to write the archive
    /// @throws Exception when the archive cannot be written or read
    @Test
    void aTarEntryWhoseNameHidesADirectoryNeverEntersTheTree(@TempDir Path directory) throws Exception {
        Map<String, String> entries = escaping();
        entries.put("node-v22/bin/node", "the runtime");
        entries.put("a/./b.txt", "a walk that stays inside");
        Path archive = directory.resolve("archive.tar");
        try (TarArchiveOutputStream tar = new TarArchiveOutputStream(Files.newOutputStream(archive))) {
            for (Map.Entry<String, String> entry : entries.entrySet()) {
                byte[] body = entry.getValue().getBytes(StandardCharsets.UTF_8);
                TarArchiveEntry tarEntry = new TarArchiveEntry(entry.getKey());
                tarEntry.setSize(body.length);
                tar.putArchiveEntry(tarEntry);
                tar.write(body);
                tar.closeArchiveEntry();
            }
        }

        // The archive really holds these names. A reader that filtered them would make the walk below
        // pass while pinning nothing — which is what happens to the zip case in this environment.
        List<String> held = new ArrayList<>();
        try (TarArchiveReader reader = new TarArchiveReader(archive)) {
            reader.getEntries().forEach(entry -> held.add(entry.getName()));
        }
        assertTrue(held.contains("..\\..\\evil.bat"),
                "the archive holds a name that is three directories to Windows: " + held);

        List<String> names = new ArrayList<>();
        try (TarFileTree tree = TarFileTree.open(archive)) {
            collect(tree.getRoot(), "", names);
        }

        assertTrue(names.contains("node-v22/bin/node"),
                "the walk still reads a name that stays inside: " + names);
        assertTrue(names.contains("a/./b.txt"), "and carries a `.` through as it always has: " + names);
        for (String name : names) {
            for (String component : name.split("/", -1)) {
                assertFalse(component.equals(".."),
                        name + " steps out of the directory being unpacked");
                assertFalse(component.indexOf('\\') >= 0,
                        name + " carries a component a Windows path parser reads as a separator");
            }
        }
    }

    /// The names an archive uses to try to write outside the directory it is unpacked into.
    ///
    /// The two spellings of a separator are both here, because only one of them is a separator to
    /// every platform: `..\..\evil.bat` is three directories to Windows and one file name to a
    /// parser that only knows `/`.
    ///
    /// @return the names and what each one is trying to do
    private static Map<String, String> escaping() {
        Map<String, String> names = new HashMap<>();
        names.put("..\\..\\evil.bat", "escapes with backslashes");
        names.put("../../evil.txt", "escapes with slashes");
        names.put("a/../../evil.txt", "escapes through a directory that is real");
        return names;
    }

    /// Collects every name in one directory of a tree, and then of its children.
    ///
    /// The path an extractor writes is the one built here — each directory's own name joined to the
    /// path that reached it — so the walk carries that path down rather than reading a directory's
    /// name on its own. Reading it on its own loses the directories above: a walk that does that
    /// reports `bin/node` for an entry named `node-v22/bin/node`, which is the shape of the defect
    /// this file is about and not something to reproduce in the check for it.
    ///
    /// @param dir    the directory to read
    /// @param prefix the path from the root to this directory, ending in `/`, or empty at the root
    /// @param names  receives each name, as a path relative to the root
    private static void collect(ArchiveFileTree.Dir<?> dir, String prefix, List<String> names) {
        for (String name : dir.getFiles().keySet()) {
            names.add(prefix + name);
        }
        for (Map.Entry<String, ? extends ArchiveFileTree.Dir<?>> sub : dir.getSubDirs().entrySet()) {
            collect(sub.getValue(), prefix + sub.getKey() + "/", names);
        }
    }

    /// Writes a zip holding one text entry per name.
    ///
    /// @param directory where to write it
    /// @param entries   the entry names and their contents
    /// @return the archive
    private static Path zip(Path directory, Map<String, String> entries) throws IOException {
        Path archive = directory.resolve("archive.zip");
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(archive))) {
            for (Map.Entry<String, String> entry : entries.entrySet()) {
                zip.putNextEntry(new ZipEntry(entry.getKey()));
                zip.write(entry.getValue().getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
            }
        }
        return archive;
    }
}
