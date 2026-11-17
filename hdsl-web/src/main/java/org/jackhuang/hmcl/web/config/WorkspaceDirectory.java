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
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/// Where a dsh session's files live: the folder the harness treats as the
/// project, and the working directory of the harness process.
///
/// An instance needs one, and the launcher's own default — the folder it keeps
/// its *instances* in — is not what a deployment wants: the harness is a coding
/// agent, and the point of running it in a container is that the files it edits
/// are the operator's rather than the container's.
///
/// The workspace is therefore the `workspace` folder under the process's home
/// directory — `/home/hdsl/workspace` in the image, where `docker-compose.yml`
/// mounts either a named volume (its default) or a host folder of the
/// operator's choosing. `HDSL_WORKSPACE` names another path when one is wanted.
///
/// Creating it if missing and *selecting* it is what makes it effective: the
/// domain resolves a new instance's workspace from the selected directory, and
/// the panel's `POST /api/instances` follows the same rule.
@NotNullByDefault
public final class WorkspaceDirectory {

    /// The environment variable that names the workspace explicitly.
    public static final String ENV_WORKSPACE = "HDSL_WORKSPACE";

    /// The folder under the home directory a deployment mounts.
    public static final String FOLDER_NAME = "workspace";

    /// What the registered directory is called in the launcher's settings.
    private static final String DISPLAY_NAME = "工作区";

    private WorkspaceDirectory() {
    }

    /// Picks the workspace path without creating anything.
    ///
    /// @param env  the process environment
    /// @param home the process's home directory (its `user.home`)
    /// @return the folder to use, absolute and normalized
    public static Path resolve(Map<String, String> env, Path home) {
        String configured = env.get(ENV_WORKSPACE);
        if (configured != null && !configured.isBlank()) {
            return Path.of(configured.trim()).toAbsolutePath().normalize();
        }
        return home.resolve(FOLDER_NAME).toAbsolutePath().normalize();
    }

    /// Resolves the workspace, creates it, and makes it the launcher's selected
    /// directory — so an instance created through the panel opens on it. Never
    /// throws: a deployment whose workspace cannot be made usable keeps the
    /// launcher's own default folder and says so in the log.
    ///
    /// @param env    the process environment
    /// @param home   the process's home directory
    /// @param logger where the choice is reported
    /// @return the registered directory, or null when the workspace could not
    ///         be made usable
    public static @Nullable GameDirectory install(Map<String, String> env, Path home, Logger logger) {
        Path workspace = resolve(env, home);
        try {
            Files.createDirectories(workspace);
            if (!Files.isDirectory(workspace)) {
                logger.warning("The workspace " + workspace + " is not a directory;"
                        + " instances will open on the launcher's own folder instead");
                return null;
            }
        } catch (IOException | RuntimeException e) {
            logger.warning("Could not create the workspace " + workspace + ";"
                    + " instances will open on the launcher's own folder instead", e);
            return null;
        }

        try {
            GameDirectory directory = GameDirectoryManager.add(workspace, DISPLAY_NAME);
            GameDirectoryManager.select(directory.id());
            logger.info("Workspace: " + workspace);
            return directory;
        } catch (RuntimeException e) {
            logger.warning("Could not register the workspace " + workspace, e);
            return null;
        }
    }
}
