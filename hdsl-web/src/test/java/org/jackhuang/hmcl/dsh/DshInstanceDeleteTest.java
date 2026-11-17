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

import org.jackhuang.hmcl.util.io.FileUtils;
import org.jackhuang.hmcl.util.platform.OperatingSystem;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/// What deleting an instance does while something inside it is still held.
///
/// The failure this guards against is not "the delete failed" — it is a delete that reported
/// success and left `instance.json` in place after the install next to it was already emptied:
/// the instance stays in the list and fails the moment it is started. The directory name is
/// taken away before any of its content is, so either the row is gone or the instance is whole.
class DshInstanceDeleteTest {

    @Test
    void anInstanceGoesOutOfTheListEvenWhenAFileInsideCannotBeRemoved() throws Exception {
        assumeTrue(OperatingSystem.CURRENT_OS == OperatingSystem.WINDOWS,
                "the read-only attribute that refuses a delete here is a Windows notion");

        String id = "locked-" + Long.toHexString(System.nanoTime());
        DshInstance instance = create(id);
        Path directory = DshPaths.instanceDirectory(id);
        Path kept = instance.homeDirectory().resolve("session.log");
        Files.writeString(kept, "still here");
        // A file the delete is refused over while the rename is still allowed. An *open* handle
        // blocks the rename too and produces no tombstone — that case is the test below.
        Files.setAttribute(kept, "dos:readonly", true);

        try {
            assertDoesNotThrow(() -> DshInstanceManager.delete(id),
                    "a file that cannot be removed must not turn the delete into an error");

            assertNull(DshInstanceManager.find(id), "the instance is no longer in the list");
            assertFalse(Files.isDirectory(directory), "the instance directory is gone under its own name");
            assertFalse(Files.exists(directory.resolve(DshInstanceManager.MANIFEST_NAME)),
                    "no manifest survives under the instance's name — that is the row that could not start");
            assertEquals(1, leftoversOf(id).length, "what could not be removed was renamed aside");
        } finally {
            Path[] leftovers = leftoversOf(id);
            for (Path leftover : leftovers) {
                Files.setAttribute(leftover.resolve("home").resolve("session.log"), "dos:readonly", false);
            }
        }

        try {
            DshInstanceManager.sweepTombstones();

            assertEquals(0, leftoversOf(id).length, "the startup sweep finishes what the delete could not");
            assertNull(DshInstanceManager.find(id), "and the sweep never brings the instance back");
        } finally {
            sweepLeftoversOf(id);
        }
    }

    /// The reported bug, made deterministic: a delete that cannot take the name away and cannot
    /// finish the content either must not have emptied the install on its way out.
    ///
    /// Two things stop a delete here, and they are the two the old order could not survive: the
    /// open handle refuses the rename, and the read-only file refuses its own deletion — so the old
    /// "empty the tree, rename to hide the failure" order emptied `dsh/` first and only then found
    /// out it could not rename. What was left was a manifest describing an install that no longer
    /// existed: the instance still in the list, failing the moment it was started.
    @Test
    void aDeleteThatFailsLeavesTheInstallAndTheManifestBothThere() throws Exception {
        assumeTrue(OperatingSystem.CURRENT_OS == OperatingSystem.WINDOWS,
                "an open handle blocking a directory rename and a read-only file refusing deletion are Windows behaviours");

        String id = "stuck-" + Long.toHexString(System.nanoTime());
        DshInstance instance = create(id);
        Path directory = DshPaths.instanceDirectory(id);
        Path install = Files.writeString(
                Files.createDirectories(directory.resolve("dsh")).resolve("harness.js"), "installed");
        Path held = instance.homeDirectory().resolve("session.log");
        Path locked = instance.homeDirectory().resolve("locked.txt");
        Files.writeString(locked, "a lock that outlived whoever took it");
        Files.setAttribute(locked, "dos:readonly", true);

        FileChannel channel = FileChannel.open(held, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        try {
            channel.write(ByteBuffer.wrap(new byte[]{'x'}));
            channel.force(true);

            assertThrows(DshException.class, () -> DshInstanceManager.delete(id),
                    "an instance that cannot be renamed is reported as such");

            assertTrue(Files.isRegularFile(install), "the install was not gutted on the way to the failure");
            assertTrue(Files.isRegularFile(directory.resolve(DshInstanceManager.MANIFEST_NAME)),
                    "and the manifest is still next to it, so the instance is whole rather than half-deleted");
            assertNotNull(DshInstanceManager.find(id), "which means it is still a row the panel can start");
            assertTrue(Files.exists(directory), "the directory still has its own name");
            assertEquals(0, leftoversOf(id).length, "no tombstone was made either");
        } finally {
            channel.close();
            Files.setAttribute(locked, "dos:readonly", false);
            try {
                DshInstanceManager.delete(id);
            } catch (DshException ignored) {
                // The delete under test may have removed it already; the assertions above are the point.
            }
            sweepLeftoversOf(id);
        }

        assertNull(DshInstanceManager.find(id), "once nothing holds it the ordinary delete works");
    }

    private static DshInstance create(String id) throws Exception {
        return DshInstanceManager.create(id, "1.0.0", DshInstance.DEFAULT_PROFILE,
                Path.of(System.getProperty("java.io.tmpdir")),
                DshHomeMode.ISOLATED, null, List.of(), Map.of());
    }

    /// Every tombstone a delete of `id` left under the instances root.
    private static Path[] leftoversOf(String id) throws Exception {
        if (!Files.isDirectory(DshPaths.INSTANCES)) {
            return new Path[0];
        }
        try (Stream<Path> entries = Files.list(DshPaths.INSTANCES)) {
            return entries
                    .filter(entry -> entry.getFileName().toString().startsWith(id + ".deleting-"))
                    .toArray(Path[]::new);
        }
    }

    private static void sweepLeftoversOf(String id) throws Exception {
        for (Path leftover : leftoversOf(id)) {
            FileUtils.deleteDirectoryQuietly(leftover);
        }
    }
}
