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
package org.jackhuang.hmcl.web.http;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Locale;
import java.util.Set;

/// The `<HDSL_DATA>/exports` directory every export endpoint lands its
/// artifact in, and the rules that keep it safe to serve files back out of:
///
/// - the directory is owner-only (`0700` — a directory must be executable to
///   be listable, so the stricter `0600` a file gets would make it unusable);
/// - every artifact is owner-read/write only (`0600`): a pack can hold a
///   profile's own files, and those are nobody else's business;
/// - a download name is normalized and confined to the directory, so
///   `../../etc/passwd` is a 400, not a file.
///
/// The POSIX permissions are best-effort: on a filesystem that does not
/// understand them the files are still written, just without the mode bits.
@NotNullByDefault
final class PackExports {

    private PackExports() {
    }

    /// Creates the exports directory (and parents), tightening it to
    /// owner-only where the filesystem understands POSIX permissions.
    ///
    /// @param dir the directory
    /// @return the same directory
    /// @throws IOException when it cannot be created
    static Path ensureDirectory(Path dir) throws IOException {
        Files.createDirectories(dir);
        try {
            Files.setPosixFilePermissions(dir, Set.of(PosixFilePermission.OWNER_READ,
                    PosixFilePermission.OWNER_WRITE, PosixFilePermission.OWNER_EXECUTE));
        } catch (UnsupportedOperationException | SecurityException | IOException e) {
            // Best effort: the mode bits are a hardening, not a precondition.
        }
        return dir;
    }

    /// Tightens a written artifact to owner-read/write only, best-effort.
    ///
    /// @param file the artifact
    static void privateFile(Path file) {
        try {
            Files.setPosixFilePermissions(file, Set.of(PosixFilePermission.OWNER_READ,
                    PosixFilePermission.OWNER_WRITE));
        } catch (UnsupportedOperationException | SecurityException | IOException e) {
            // Best effort, as above.
        }
    }

    /// Returns the file `base.ext` inside the directory, never clobbering an
    /// existing one: a second export of the same name is `name-2.ext`.
    ///
    /// @param dir       the exports directory
    /// @param base      the wanted base name, sanitized to a file-safe spelling
    /// @param extension the extension, dot included
    /// @return a name that does not exist yet
    static Path uniqueFile(Path dir, String base, String extension) {
        String safe = sanitize(base);
        Path candidate = dir.resolve(safe + extension);
        int suffix = 2;
        while (Files.exists(candidate)) {
            candidate = dir.resolve(safe + "-" + suffix + extension);
            suffix++;
        }
        return candidate;
    }

    /// The file-name-safe spelling of a base name.
    ///
    /// @param base the wanted name
    /// @return the safe spelling, `"export"` when nothing usable survives
    static String sanitize(String base) {
        String cleaned = base == null ? "" : base.trim().toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9._-]+", "-")
                .replaceAll("-{2,}", "-")
                .replaceAll("^-|-$", "");
        return cleaned.isEmpty() ? "export" : cleaned;
    }

    /// Resolves a download's file name inside the exports directory, or
    /// `null` when the name tries to leave it. Only a bare file name is
    /// accepted — no slashes, no `.`/`..` tricks.
    ///
    /// @param dir  the exports directory
    /// @param name the requested file name, already URL-decoded
    /// @return the file, or `null` when the name is not a plain child
    static @Nullable Path resolveDownload(Path dir, String name) {
        if (name == null || name.isBlank() || name.contains("/") || name.contains("\\")
                || name.equals(".") || name.equals("..") || name.startsWith(".")) {
            return null;
        }
        Path base = dir.toAbsolutePath().normalize();
        Path target = base.resolve(name).normalize();
        if (!target.startsWith(base) || !target.getParent().equals(base)) {
            return null;
        }
        return Files.isRegularFile(target) ? target : null;
    }
}
