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

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;

/// One persisted DeepSeek Harness session, as seen on disk.
///
/// A session is a directory under `sessions/<workspace-slug>/<id>/` holding a
/// compressed event log. The log's **format** is part of its file name —
/// `session.v3.jsonl.zstd` — which is the only compatibility signal DeepSeek
/// Harness publishes, so it is what migration is gated on.
///
/// @param id            the session id, which is also its directory name
/// @param workspaceSlug the directory grouping sessions by working directory
/// @param directory     the session directory
/// @param formatVersion the log format version
/// @param workingDirectory the working directory the session was created in, when known
/// @param title         the session title, when a projection cache row exists
/// @param sizeBytes     the size of the session directory's contents
/// @param modifiedAt    the last modification time, in epoch milliseconds
/// @param locked        whether a lock file is present, meaning a process may hold it
@NotNullByDefault
public record DshSession(
        String id,
        String workspaceSlug,
        Path directory,
        int formatVersion,
        @Nullable String workingDirectory,
        @Nullable String title,
        long sizeBytes,
        long modifiedAt,
        boolean locked) {

    /// Returns the name to show for the session.
    ///
    /// The title is DeepSeek Harness's own, and it is absent for a session that
    /// was opened but never used — the projection cache marks those `blank`.
    /// Falling back to a shortened id produces rows reading `session-`, which
    /// says nothing; the working directory's own name at least distinguishes one
    /// such session from another.
    ///
    /// @return the title, the working directory's name, or a shortened id
    public String label() {
        if (title != null && !title.isBlank()) {
            return title;
        }
        if (workingDirectory != null && !workingDirectory.isBlank()) {
            Path path = Path.of(workingDirectory);
            Path name = path.getFileName();
            if (name != null && !name.toString().isBlank()) {
                return name.toString();
            }
        }
        return id.length() > 8 ? id.substring(0, 8) : id;
    }

    /// Returns the file name the log must have for this format version.
    ///
    /// @return the expected log file name
    public String logFileName() {
        return "session.v" + formatVersion + ".jsonl.zstd";
    }
}
