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
package org.jackhuang.hmcl.dsh;

import org.jackhuang.hmcl.util.io.FileUtils;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Verifies that renaming an instance takes everything named after it along.
///
/// This pins the reported defect: renaming made the instance vanish from the
/// list while its folder stayed on disk, because the manifest was written to the
/// new folder and then deleted from it. The folder the instance owns is named
/// after the instance, so the instance's own files have to move with it.
class DshInstanceRenameTest {
    /// Renames an instance in one home mode and checks what moved and what did not.
    ///
    /// @param mode the home mode the instance is made with
    /// @throws Exception when the filesystem refuses
    private static void renamingTakesTheFolderAlong(DshHomeMode mode) throws Exception {
        String id = "test-rename-" + mode.name().toLowerCase() + "-" + Long.toHexString(System.nanoTime());
        String newId = id + "-renamed";
        Path customHome = mode == DshHomeMode.CUSTOM
                ? Files.createTempDirectory("test-rename-home-") : null;

        DshInstance created = DshInstanceManager.create(id, "1.0.0", DshInstance.DEFAULT_PROFILE,
                Path.of(System.getProperty("java.io.tmpdir")), mode, customHome, List.of(), Map.of());
        assertNotNull(created, "the instance has to be made before it can be renamed");

        Path oldDirectory = DshPaths.instanceDirectory(id);
        Path newDirectory = DshPaths.instanceDirectory(newId);

        try {
            Files.createDirectories(oldDirectory.resolve("dsh"));
            Files.writeString(oldDirectory.resolve("dsh/marker.txt"), "runtime");

            DshInstance renamed = DshInstanceManager.rename(id, newId);

            assertEquals(newId, renamed.id(), "the instance has to come back under its new id");
            assertNotNull(DshInstanceManager.find(newId), "the new id has to be readable");
            assertNull(DshInstanceManager.find(id), "the old id must hold nothing");
            assertTrue(Files.isRegularFile(newDirectory.resolve(DshInstanceManager.MANIFEST_NAME)),
                    "the manifest has to move with the instance instead of being deleted");
            assertTrue(Files.isRegularFile(newDirectory.resolve("dsh/marker.txt")),
                    "the runtime has to move with the instance");
            assertFalse(Files.exists(oldDirectory), "the old folder has to be gone");
            assertTrue(DshInstanceManager.list().stream().anyMatch(i -> i.id().equals(newId)),
                    "the list has to show the new id");

            if (mode == DshHomeMode.ISOLATED) {
                assertTrue(Files.isDirectory(newDirectory.resolve("home")),
                        "an isolated home is part of the folder and moves with it");
            }
            if (mode == DshHomeMode.CUSTOM) {
                assertTrue(Files.exists(customHome), "a home the person chose is left where it is");
            }
        } finally {
            deleteQuietly(id);
            deleteQuietly(newId);
            if (customHome != null) {
                FileUtils.deleteDirectoryQuietly(customHome);
            }
        }
    }

    /// Removes an instance without letting a cleanup failure mask the assertions.
    ///
    /// @param id the instance id
    private static void deleteQuietly(String id) {
        try {
            DshInstanceManager.delete(id);
        } catch (Exception ignored) {
            // There was nothing left to remove.
        }
    }

    @Test
    void renamingNeverDeletesWhateverAlreadyHoldsTheNewName() throws Exception {
        // What resolves to the new name is not necessarily an instance's directory: it can be what a
        // deleted instance left behind, or a folder somebody put there. Renaming used to empty the
        // destination first, so that directory and everything in it was deleted with nothing said —
        // the row changed name and the files that were in the way were gone.
        String id = "renaming-over-" + Long.toHexString(System.nanoTime());
        String newId = "occupied-name-" + Long.toHexString(System.nanoTime());
        DshInstanceManager.create(id, "1.0.0", DshInstance.DEFAULT_PROFILE,
                Path.of(System.getProperty("java.io.tmpdir")), DshHomeMode.ISOLATED, null, List.of(), Map.of());
        Path occupied = Files.createDirectories(DshPaths.instanceDirectory(newId));
        Path document = Files.writeString(occupied.resolve("notes.txt"), "a file that was already there");

        try {
            DshException refused = assertThrows(DshException.class,
                    () -> DshInstanceManager.rename(id, newId));

            assertTrue(refused.getMessage().contains(occupied.toString()),
                    "the refusal names the directory that is in the way: " + refused.getMessage());
            assertTrue(Files.isDirectory(occupied), "the directory that held the name is left where it is");
            assertTrue(Files.isRegularFile(document), "and so is everything inside it");
            assertTrue(Files.isDirectory(DshPaths.instanceDirectory(id)),
                    "the instance keeps its own directory, because the rename did not happen");
            assertEquals(DshPaths.instanceDirectory(id),
                    DshInstanceManager.find(id).instanceDirectory());
        } finally {
            deleteQuietly(id);
            FileUtils.deleteDirectoryQuietly(occupied);
        }
    }

    @Test
    void anIsolatedInstanceMovesItsOwnFolderAndHome() throws Exception {
        renamingTakesTheFolderAlong(DshHomeMode.ISOLATED);
    }

    @Test
    void aSharedHomeInstanceMovesOnlyItsOwnFolder() throws Exception {
        renamingTakesTheFolderAlong(DshHomeMode.GLOBAL);
    }

    @Test
    void aCustomHomeInstanceKeepsItsChosenHome() throws Exception {
        renamingTakesTheFolderAlong(DshHomeMode.CUSTOM);
    }
}
