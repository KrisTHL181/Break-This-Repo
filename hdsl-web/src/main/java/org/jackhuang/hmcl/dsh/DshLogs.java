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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/// Writes a log window's contents to a file somebody can find again.
///
/// Three things go into the name, because a directory of exported logs is only useful if the files
/// in it can be told apart without opening them: **the launcher** that wrote them, **the instance**
/// they came from, and **the moment** they were exported. What comes out is
/// `hdsl-<instance>-2026-09-27T21-58-30.log`.
///
/// They are written into [#DshPaths.LOGS], which is the launcher's own directory. The original
/// wrote them beside the process's working directory — a path with nothing to do with this launcher,
/// and one a person has no reason to look in.
///
/// An instance name is whatever somebody typed, so it is made into something a file name can hold
/// rather than refused: the export is a diagnostic, and a name is not worth losing one over.
@NotNullByDefault
public final class DshLogs {
    /// What every export's name begins with.
    private static final String PREFIX = "hdsl";

    /// The suffix every export carries.
    private static final String SUFFIX = ".log";

    /// How the moment is written: sortable, and free of the colon a file name may not hold.
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH-mm-ss");

    /// How much of an instance's name is kept.
    private static final int NAME_LIMIT = 64;

    /// The characters a file name may not hold, on any of the systems this runs on.
    private static final String FORBIDDEN = "/\\:*?\"<>|";

    private DshLogs() {
    }

    /// Writes lines into the launcher's log directory, named for the launcher, the instance and now.
    ///
    /// @param instanceId the instance the lines came from, or `null` when they came from none
    /// @param lines      the lines
    /// @return the file that was written
    /// @throws IOException when the directory or the file cannot be written
    public static Path write(@Nullable String instanceId, List<String> lines) throws IOException {
        return write(DshPaths.LOGS, instanceId, lines, LocalDateTime.now());
    }

    /// Writes lines into a directory, named for the launcher, the instance and the moment.
    ///
    /// @param directory  where to write
    /// @param instanceId the instance the lines came from, or `null` when they came from none
    /// @param lines      the lines
    /// @param at         the moment the export is stamped with
    /// @return the file that was written
    /// @throws IOException when the directory or the file cannot be written
    static Path write(Path directory, @Nullable String instanceId, List<String> lines, LocalDateTime at)
            throws IOException {
        Files.createDirectories(directory);
        Path file = unused(directory.resolve(fileName(instanceId, at)));
        Files.write(file, lines);
        return file;
    }

    /// Returns the name one export carries.
    ///
    /// @param instanceId the instance the lines came from, or `null` when they came from none
    /// @param at         the moment the export is stamped with
    /// @return the file name
    static String fileName(@Nullable String instanceId, LocalDateTime at) {
        String instance = usableName(instanceId);
        String stamp = at.format(STAMP);
        return instance.isEmpty()
                ? PREFIX + "-" + stamp + SUFFIX
                : PREFIX + "-" + instance + "-" + stamp + SUFFIX;
    }

    /// Returns a name no export has taken yet.
    ///
    /// Two exports a second apart is unlikely and two in the same second is not impossible, and the
    /// second one overwriting the first would lose exactly the log somebody is trying to keep.
    ///
    /// @param wanted the name the moment asks for
    /// @return a path under the same name, or under that name and a number
    private static Path unused(Path wanted) {
        if (!Files.exists(wanted)) {
            return wanted;
        }
        String name = wanted.getFileName().toString();
        String stem = name.substring(0, name.length() - SUFFIX.length());
        for (int i = 2; ; i++) {
            Path candidate = wanted.resolveSibling(stem + "-" + i + SUFFIX);
            if (!Files.exists(candidate)) {
                return candidate;
            }
        }
    }

    /// Returns an instance name a file name can hold, or an empty string.
    ///
    /// @param instanceId the instance, or `null`
    /// @return the name
    private static String usableName(@Nullable String instanceId) {
        String trimmed = instanceId == null ? "" : instanceId.trim();
        if (trimmed.isEmpty() || trimmed.equals(".") || trimmed.equals("..")) {
            return "";
        }
        if (trimmed.length() > NAME_LIMIT) {
            trimmed = trimmed.substring(0, NAME_LIMIT);
        }

        StringBuilder safe = new StringBuilder(trimmed.length());
        for (int i = 0; i < trimmed.length(); i++) {
            char c = trimmed.charAt(i);
            safe.append(Character.isISOControl(c) || FORBIDDEN.indexOf(c) >= 0 ? '-' : c);
        }
        // A name that ends in a dot or a space is a name Windows quietly rewrites.
        while (safe.length() > 0 && (safe.charAt(safe.length() - 1) == '.' || safe.charAt(safe.length() - 1) == ' ')) {
            safe.setLength(safe.length() - 1);
        }
        return safe.toString().trim();
    }
}
