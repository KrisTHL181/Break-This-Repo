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
package org.jackhuang.hmcl.web.config;

import org.jackhuang.hmcl.setting.GameDirectory;
import org.jackhuang.hmcl.setting.GameDirectoryManager;
import org.jackhuang.hmcl.util.logging.Logger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The workspace rule a deployment depends on: dsh needs a folder to treat as
/// the project, the container mounts one under the user's home
/// (`/home/hdsl/workspace`), and neither "another path was named" nor "the path
/// is unusable" may leave instances on the folder the launcher keeps them in.
class WorkspaceDirectoryTest {

    @TempDir
    Path temp;

    @Test
    void theNamedFolderWinsEvenBeforeItExists() {
        Path named = temp.resolve("named/deeper");
        assertEquals(named,
                WorkspaceDirectory.resolve(
                        Map.of(WorkspaceDirectory.ENV_WORKSPACE, named.toString()), temp.resolve("home")));
    }

    @Test
    void withoutANameItIsTheFolderUnderHome() {
        Path home = temp.resolve("home");
        assertEquals(home.resolve(WorkspaceDirectory.FOLDER_NAME),
                WorkspaceDirectory.resolve(Map.of(), home));
    }

    /// The whole point of the install step: the folder is created, registered
    /// as a directory, and *selected* — a new instance's workspace is whatever
    /// is selected, so this is how the mount reaches the harness.
    @Test
    void installCreatesRegistersAndSelectsTheWorkspace() throws Exception {
        Path workspace = temp.resolve("workspace");
        GameDirectory added = null;
        try {
            added = WorkspaceDirectory.install(
                    Map.of(WorkspaceDirectory.ENV_WORKSPACE, workspace.toString()),
                    temp.resolve("home"), Logger.LOG);

            assertNotNull(added);
            assertTrue(Files.isDirectory(workspace), "the named workspace must be created");
            assertEquals(added.id(), GameDirectoryManager.selected().id());
            assertEquals(workspace, GameDirectoryManager.selected().directory());
        } finally {
            if (added != null) {
                GameDirectoryManager.remove(added.id());
            }
        }
    }

    /// A path that cannot be a directory (here: under a regular file) is
    /// refused, and the launcher keeps its own folder rather than pretending.
    @Test
    void anUnusableWorkspaceIsRefusedRatherThanGuessed() throws Exception {
        Path file = Files.writeString(temp.resolve("a-file"), "x");
        assertNull(WorkspaceDirectory.install(
                Map.of(WorkspaceDirectory.ENV_WORKSPACE, file.resolve("under-a-file").toString()),
                temp.resolve("home"), Logger.LOG));
    }
}
