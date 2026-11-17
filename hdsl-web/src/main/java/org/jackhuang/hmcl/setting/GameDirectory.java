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
package org.jackhuang.hmcl.setting;

import com.google.gson.annotations.SerializedName;
import org.jackhuang.hmcl.dsh.DshPaths;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.UUID;

/// A folder the launcher looks for instances in.
///
/// HMCL's equivalent is its game directory, and the role is the same: it is the
/// unit the instance list is filtered by, so a user with several collections of
/// instances switches between them rather than seeing one long list.
///
/// Nothing is ever moved into or out of one. Adding a directory makes the
/// launcher look inside it; removing one stops looking. The files are the
/// user's, and a launcher that silently relocated them would be doing something
/// the original does not.
///
/// @param id         the stable identifier
/// @param path       the folder
/// @param customName the name to show, or `null` to derive one from the path
@NotNullByDefault
public record GameDirectory(
        @SerializedName("id") String id,
        @SerializedName("path") String path,
        @SerializedName("customName") @Nullable String customName) {

    /// The identifier of the directory the launcher owns and starts with.
    public static final String DEFAULT_ID = "default";

    /// Returns the folder this directory points at.
    ///
    /// @return the path
    public Path directory() {
        return Path.of(path).toAbsolutePath().normalize();
    }

    /// Returns the name to show for this directory.
    ///
    /// @return the custom name, the folder's own name, or the path
    public String displayName() {
        if (customName != null && !customName.isBlank()) {
            return customName;
        }
        Path name = directory().getFileName();
        return name == null ? path : name.toString();
    }

    /// Reports whether this is the directory the launcher owns.
    ///
    /// The default one is where instances live when the user has never chosen a
    /// directory, and it is the only one the launcher may write into without
    /// being asked.
    ///
    /// @return whether this is the default directory
    public boolean isDefault() {
        return DEFAULT_ID.equals(id);
    }

    /// Returns the directory instances live in when none has been chosen.
    ///
    /// @return the default directory
    public static GameDirectory defaultDirectory() {
        return new GameDirectory(DEFAULT_ID, DshPaths.INSTANCES.toString(), null);
    }

    /// Creates a new directory entry for a folder.
    ///
    /// @param path the folder
    /// @return the entry
    public static GameDirectory of(Path path) {
        return of(path, null);
    }

    /// Creates an entry for a folder, recorded as given.
    ///
    /// The path is kept as the caller wrote it rather than made absolute, so that
    /// a folder recorded relative to the launcher stays relative — which is the
    /// point of recording it that way.
    ///
    /// @param path       the folder
    /// @param customName the name to show, or `null` to derive one from the path
    /// @return the entry
    public static GameDirectory of(Path path, @Nullable String customName) {
        return new GameDirectory(UUID.randomUUID().toString(), path.toString(),
                customName == null || customName.isBlank() ? null : customName);
    }
}
