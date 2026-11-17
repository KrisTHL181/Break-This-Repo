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

import org.jackhuang.hmcl.util.io.FileUtils;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// What creating an instance does about a directory that is already there.
///
/// An instance's directory is named after the instance, under the instances root — and a directory
/// under that root is not necessarily an instance's: a delete that was interrupted leaves one behind,
/// and so does anything else that ever put a folder there. Adopting it means a create that reports
/// success over files that were never its own, and a delete of "the instance" that takes them with it.
/// What a create does now is refuse, and the caller that has a name to place — the web layer, which
/// suffixes it — is the one that decides what the name becomes.
class DshInstanceCreationTest {

    @Test
    void creatingAnInstanceNeverAdoptsADirectoryThatIsAlreadyThere() throws Exception {
        String id = "adopt-" + Long.toHexString(System.nanoTime());
        Path directory = Files.createDirectories(DshPaths.instanceDirectory(id));
        Path document = Files.writeString(directory.resolve("notes.txt"), "a file that was already there");

        try {
            DshException refused = assertThrows(DshException.class, () -> DshInstanceManager.create(
                            id, "1.0.0", DshInstance.DEFAULT_PROFILE,
                            Path.of(System.getProperty("java.io.tmpdir")),
                            DshHomeMode.ISOLATED, null, List.of(), Map.of()),
                    "a directory that is already there is not this instance's to take");

            assertTrue(refused.getMessage().contains(directory.toString()),
                    "the refusal names the directory that is in the way: " + refused.getMessage());
            assertTrue(Files.isRegularFile(document), "the directory and what is in it are left alone");
            assertFalse(Files.exists(directory.resolve(DshInstanceManager.MANIFEST_NAME)),
                    "no manifest is written into a directory the launcher did not make");
            assertNull(DshInstanceManager.find(id), "and the id is not registered");
        } finally {
            FileUtils.deleteDirectoryQuietly(directory);
        }
    }

    @Test
    void creatingAnInstanceMakesTheDirectoryItWasGiven() throws Exception {
        // The other half: nothing under the instances root is in the way, so the create is the
        // ordinary one — the directory is made, the manifest is written into it, the instance answers.
        String id = "fresh-" + Long.toHexString(System.nanoTime());
        Path directory = DshPaths.instanceDirectory(id);

        try {
            DshInstance created = DshInstanceManager.create(id, "1.0.0", DshInstance.DEFAULT_PROFILE,
                    Path.of(System.getProperty("java.io.tmpdir")),
                    DshHomeMode.ISOLATED, null, List.of(), Map.of());

            assertTrue(Files.isDirectory(directory), "the directory is made");
            assertTrue(Files.isRegularFile(directory.resolve(DshInstanceManager.MANIFEST_NAME)),
                    "the manifest is written into it");
            assertTrue(Files.isDirectory(directory.resolve("home")), "and an isolated home with it");
            assertEquals(id, DshInstanceManager.find(id).id(), "the instance answers to its id");
            assertEquals(id, created.id());
        } finally {
            FileUtils.deleteDirectoryQuietly(directory);
            try {
                DshInstanceManager.delete(id);
            } catch (Exception ignored) {
                // There was nothing left to remove.
            }
        }
    }
}