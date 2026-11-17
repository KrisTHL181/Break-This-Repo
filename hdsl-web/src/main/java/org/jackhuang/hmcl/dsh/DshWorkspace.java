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
import java.util.List;

/// One working directory's sessions, as DeepSeek Harness groups them.
///
/// Sessions are stored under `sessions/<workspace-slug>/`, so the slug **is**
/// the harness's own grouping: every session recorded in one working directory
/// shares it, and sessions are never split across slugs. The slug is opaque —
/// it encodes the path, but the encoding is lossy, so the path cannot be read
/// back out of it — which is why the readable [path] and [title] come from the
/// sessions' own projection-cache rows instead, and fall back to the slug when
/// no session has one.
///
/// @param slug     the on-disk grouping every session in this workspace shares
/// @param path     the working directory a session named, or `null` when none did
/// @param title    the name to show, from the path, or the slug when there is no path
/// @param sessions the workspace's sessions, newest first
@NotNullByDefault
public record DshWorkspace(
        String slug,
        @Nullable String path,
        String title,
        List<DshSession> sessions) {

    /// Returns the total size of the workspace's sessions.
    ///
    /// @return the size in bytes
    public long sizeBytes() {
        long total = 0L;
        for (DshSession session : sessions) {
            total += session.sizeBytes();
        }
        return total;
    }

    /// Returns the workspace's last activity.
    ///
    /// @return the newest session's modification time, in epoch milliseconds, or `0`
    public long modifiedAt() {
        long newest = 0L;
        for (DshSession session : sessions) {
            newest = Math.max(newest, session.modifiedAt());
        }
        return newest;
    }

    /// Returns whether a session belongs to this workspace.
    ///
    /// @param session the session
    /// @return whether the session is one of this workspace's
    public boolean contains(DshSession session) {
        return slug.equals(session.workspaceSlug());
    }

    /// Returns the name to show for a working directory.
    ///
    /// The harness calls a workspace by the last segment of its path, and that
    /// is what a person recognises; the whole path is shown beneath it. A slug
    /// is shown only when no session ever named a path, which is the one case
    /// where nothing better exists.
    ///
    /// @param path the working directory, or `null`
    /// @param slug the on-disk grouping
    /// @return the name
    static String titleOf(@Nullable String path, String slug) {
        if (path != null && !path.isBlank()) {
            Path name = Path.of(path).getFileName();
            if (name != null && !name.toString().isBlank()) {
                return name.toString();
            }
            return path;
        }
        return slug;
    }
}
