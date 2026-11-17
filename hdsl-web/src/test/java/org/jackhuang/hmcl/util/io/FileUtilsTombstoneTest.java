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
package org.jackhuang.hmcl.util.io;

import org.jackhuang.hmcl.util.platform.OperatingSystem;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/// Deleting a tree by taking its name away first.
///
/// What this has to guarantee is that a removal never leaves a directory that still looks
/// like what it was: an instance whose install has been emptied but whose manifest survives
/// is a row that cannot start. The name goes first, so whatever is still held inside is a
/// leftover nobody lists, and the next sweep can finish it — and when even the rename is
/// refused, nothing has been touched at all and saying "cannot remove" is the truth.
class FileUtilsTombstoneTest {

    @TempDir
    Path temp;

    @Test
    void aTreeNobodyHoldsIsRemovedCompletely() throws Exception {
        Path directory = Files.createDirectories(temp.resolve("instance"));
        Files.writeString(directory.resolve("instance.json"), "{}");
        Files.writeString(Files.createDirectories(directory.resolve("home")).resolve("a.txt"), "hi");

        Path leftover = FileUtils.deleteByTombstone(directory, ".deleting-1");

        assertNull(leftover, "nothing is left when nothing was held");
        assertFalse(Files.exists(directory), "the directory is gone, under its own name");
        try (Stream<Path> entries = Files.list(temp)) {
            assertEquals(0, entries.count(), "and no tombstone is left next to it");
        }
    }

    @Test
    void aDirectoryThatIsNotThereIsSuccess() throws Exception {
        assertNull(FileUtils.deleteByTombstone(temp.resolve("gone"), ".deleting-1"),
                "deleting something that is not there is not a failure");
        assertFalse(Files.exists(temp.resolve("gone.deleting-1")));
    }

    @Test
    void aFileThatCannotBeRemovedLeavesATombstoneNotAHalfDeletedDirectory() throws Exception {
        assumeTrue(OperatingSystem.CURRENT_OS == OperatingSystem.WINDOWS,
                "the read-only attribute that refuses a delete here is a Windows notion");

        Path directory = Files.createDirectories(temp.resolve("instance"));
        Files.writeString(directory.resolve("instance.json"), "{}");
        Path kept = Files.createDirectories(directory.resolve("home")).resolve("session.log");
        Files.writeString(kept, "still here");
        // A delete Windows refuses while the rename is still allowed — a read-only file, the same
        // shape as a lock that outlives the process that took it. An *open* handle blocks the
        // rename as well (see the test below), so it produces no tombstone at all.
        Files.setAttribute(kept, "dos:readonly", true);

        Path leftover = FileUtils.deleteByTombstone(directory, ".deleting-1");

        assertNotNull(leftover, "a tree that could not be emptied must report what is left");
        assertFalse(Files.exists(directory),
                "the name is gone even though the content is not — this is what keeps a half-deleted instance out of the list");
        assertTrue(Files.isDirectory(leftover), "and what could not be removed is still there to be swept");
        assertEquals("instance.deleting-1", leftover.getFileName().toString(),
                "the tombstone keeps the shape the sweep looks for");

        Files.setAttribute(leftover.resolve("home").resolve("session.log"), "dos:readonly", false);
        assertTrue(FileUtils.deleteDirectoryQuietly(leftover),
                "once the lock is gone the leftover can be swept");
        assertFalse(Files.exists(leftover));
    }

    @Test
    void aDirectoryThatCannotEvenBeRenamedIsLeftUntouched() throws Exception {
        assumeTrue(OperatingSystem.CURRENT_OS == OperatingSystem.WINDOWS,
                "an open handle blocking a directory rename is a Windows behaviour");

        Path directory = Files.createDirectories(temp.resolve("instance"));
        Path manifest = Files.writeString(directory.resolve("instance.json"), "{}");
        Path held = Files.createDirectories(directory.resolve("home")).resolve("session.log");

        FileChannel channel = FileChannel.open(held, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        try {
            channel.write(ByteBuffer.wrap(new byte[]{'x'}));
            channel.force(true);

            assertThrows(IOException.class, () -> FileUtils.deleteByTombstone(directory, ".deleting-1"),
                    "a directory whose name cannot be taken away is reported, not half-deleted");

            assertTrue(Files.isRegularFile(manifest), "nothing inside it has been touched");
            assertTrue(Files.exists(directory), "and it still has its own name");
            assertFalse(Files.exists(temp.resolve("instance.deleting-1")), "no tombstone was made either");
        } finally {
            channel.close();
        }
    }
}
