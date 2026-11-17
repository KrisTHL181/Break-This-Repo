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

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Function;
import java.util.regex.Pattern;

/// Tells a directory an interrupted delete left behind from a directory that belongs to
/// somebody.
///
/// A delete that cannot finish renames its target to `<name>.deleting-<millis>` and leaves the
/// rest to a sweep at the next start (see [FileUtils#deleteByTombstone]). The sweep runs over
/// directories whose names come from the user — an instance's id is its directory name — so a
/// suffix in the name proves nothing on its own: an instance may be called `v1.deleting-2`, and
/// a sweep that keyed on the substring would delete it, install and all, with one "removed the
/// leftover" line to show for it.
///
/// So two things must hold at once: the name matches the whole shape this code writes (a
/// non-empty prefix, the `.deleting-` marker, and [System#currentTimeMillis()] at the end), and
/// nothing in the directory is an instance **under that name**. The second half cannot be
/// "there is no manifest", because a delete that failed to empty the tree often leaves the
/// manifest itself behind — that leftover has to be swept too. What tells the two apart is
/// what the manifest records: a leftover still names the id it was created under, which is not
/// the name of the tombstone it now sits in, while an instance somebody made is found exactly
/// where its own id says it is.
@NotNullByDefault
public final class Tombstones {
    private Tombstones() {
    }

    /// A prefix, the marker, and millis. `System.currentTimeMillis()` is thirteen digits today
    /// and only ever grows, so ten is tolerated rather than pinned: the shape must not depend on
    /// the magnitude of the clock.
    private static final Pattern NAME = Pattern.compile(".*\\.deleting-\\d{10,}$");

    /// Whether `entry` is the leftover of an earlier delete, and nothing else.
    ///
    /// @param entry        the candidate directory
    /// @param manifestName the manifest a live instance of that kind carries, for example
    ///                     `instance.json`
    /// @param recordedId   what the manifest inside a directory records as its id, or `null`
    ///                     when there is no readable manifest; an unreadable one counts as live,
    ///                     so a directory nobody could inspect is left alone
    /// @return whether the sweep may remove it
    public static boolean isSweepable(Path entry,
                                      String manifestName,
                                      Function<Path, @Nullable String> recordedId) {
        if (!Files.isDirectory(entry)) {
            return false;
        }
        String name = entry.getFileName().toString();
        if (!NAME.matcher(name).matches()) {
            return false;
        }
        if (!Files.exists(entry.resolve(manifestName))) {
            return true;
        }
        // A manifest that records this very name is an instance, whatever the name looks like.
        return !name.equals(recordedId.apply(entry));
    }
}
