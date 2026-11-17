/*
 * Hello Minecraft! Launcher
 * Copyright (C) 2025 huangyuhui <huanghongxun2008@126.com> and contributors
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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;

import static org.jackhuang.hmcl.util.logging.Logger.LOG;

/// Symbolic links where the system allows them, and something that works where it does not.
///
/// On POSIX a symbolic link is one syscall and no permission. On Windows it needs
/// `SeCreateSymbolicLinkPrivilege`, which an ordinary account has only when the machine has
/// Developer Mode on or the process is elevated; without it the call fails with "a required
/// privilege is not held by the client". That is not an exotic machine — it is the default
/// on a stock Windows install — so a launcher that assumes links cannot unpack a Node
/// distribution, cannot adopt one, and cannot record which release is current.
///
/// The two uses of a link want different substitutes, so there are two here:
///
/// - a link **into** a tree (the `bin/npm` a Node distribution points at its own
///   `lib/node_modules`) becomes a **copy** — [createOrCopy] — because what matters is
///   that the file is there and runs;
/// - a link used as a **pointer** (a `current` naming the release in use) becomes a
///   **text file holding the target path** — [pointAt] — because what matters is that
///   the name can be read back, and copying a whole release tree to express "this one"
///   would be absurd.
///
/// [pointedAt] reads both forms, so a directory written on one machine stays readable
/// after the same directory is copied to another.
@NotNullByDefault
public final class Links {
    /// Whether this system let the probe create a link. `null` until measured.
    private static volatile @Nullable Boolean creatable;

    private Links() {
    }

    /// Reports whether this system can create symbolic links.
    ///
    /// Measured rather than inferred: the privilege depends on the account, the group
    /// policy and Developer Mode, none of which a platform check can see. The probe
    /// creates one real link in a temporary directory, so the answer is the one the
    /// actual operation will get — and the answer is cached, because a probe per link
    /// would double the work for no new information.
    ///
    /// @return whether symbolic links can be created here
    public static boolean creatable() {
        Boolean cached = creatable;
        if (cached != null) {
            return cached;
        }
        boolean measured = probe();
        creatable = measured;
        if (!measured) {
            LOG.info("Symbolic links are not permitted on this system; "
                    + "HDSL-web will copy where it would have linked, and write a pointer file "
                    + "where it would have pointed a link at a release.");
        }
        return measured;
    }

    /// Forgets the probe result. Exists for tests, which change what the system permits.
    static void invalidate() {
        creatable = null;
    }

    /// Creates a symbolic link, or copies the target when links are not permitted.
    ///
    /// Used where a link stands for a file that has to be *there* — the `npm` and `npx`
    /// entry points a Node distribution links into its own tree is the case this exists
    /// for. A copy is a faithful substitute for that and nothing else: it will not track
    /// a change in the target, which is why this is not a general-purpose `mv`.
    ///
    /// @param link   the path to create
    /// @param target the link's target, absolute or relative to the link's directory
    /// @throws IOException when neither a link nor a copy can be made
    public static void createOrCopy(Path link, Path target) throws IOException {
        Path parent = link.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        if (creatable()) {
            Files.deleteIfExists(link);
            try {
                Files.createSymbolicLink(link, target);
                return;
            } catch (IOException | UnsupportedOperationException e) {
                // The probe said yes and the real thing said no — a disappearing
                // privilege, or a filesystem (a FAT stick, a network share) that has no
                // links whatever the account may do. The copy below is the answer either
                // way, so this is not worth failing over.
                LOG.warning("Could not link " + link + " to " + target + "; copying instead", e);
            }
        }
        Files.deleteIfExists(link);
        Files.copy(resolvedAgainst(link, target), link,
                StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
        link.toFile().setExecutable(true, false);
    }

    /// Points a `current`-style pointer at a directory.
    ///
    /// Where links are available the pointer is a link, which is what a person reading
    /// the directory expects. Where they are not it is a file holding the target's
    /// absolute path — the same fact, in a form this system can actually write.
    ///
    /// @param pointer the pointer path
    /// @param target  the directory being pointed at
    /// @throws IOException when the pointer cannot be written
    public static void pointAt(Path pointer, Path target) throws IOException {
        Path parent = pointer.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.deleteIfExists(pointer);
        if (creatable()) {
            try {
                Files.createSymbolicLink(pointer, target.toAbsolutePath());
                return;
            } catch (IOException | UnsupportedOperationException e) {
                LOG.warning("Could not point " + pointer + " at " + target + "; writing a pointer file", e);
            }
        }
        Files.writeString(pointer, target.toAbsolutePath().normalize() + System.lineSeparator(),
                java.nio.charset.StandardCharsets.UTF_8);
    }

    /// Reads what [pointAt] wrote, in either form.
    ///
    /// @param pointer the pointer path
    /// @return the directory pointed at, or empty when there is no readable pointer
    public static Optional<Path> pointedAt(Path pointer) {
        if (Files.isSymbolicLink(pointer)) {
            try {
                return Optional.of(pointer.toRealPath());
            } catch (IOException e) {
                return Optional.empty();
            }
        }
        if (!Files.isRegularFile(pointer, LinkOption.NOFOLLOW_LINKS)) {
            return Optional.empty();
        }
        // A pointer file: one line, the target's absolute path. A file holding anything
        // else is not ours, and guessing at it would be worse than reporting none.
        try {
            String text = Files.readString(pointer, java.nio.charset.StandardCharsets.UTF_8).trim();
            if (text.isEmpty()) {
                return Optional.empty();
            }
            Path target = Path.of(text);
            return Files.isDirectory(target) ? Optional.of(target) : Optional.empty();
        } catch (IOException | InvalidPathException e) {
            return Optional.empty();
        }
    }

    /// Creates and deletes a link to find out whether this system allows one.
    ///
    /// @return whether the probe link was created
    private static boolean probe() {
        Path directory = null;
        try {
            directory = Files.createTempDirectory("hdsl-link-probe");
            Path target = Files.createFile(directory.resolve("target"));
            Files.createSymbolicLink(directory.resolve("link"), target);
            return true;
        } catch (IOException | UnsupportedOperationException | SecurityException e) {
            return false;
        } finally {
            if (directory != null) {
                try {
                    FileUtils.deleteDirectory(directory);
                } catch (IOException e) {
                    LOG.warning("Could not clean up the link probe in " + directory, e);
                }
            }
        }
    }

    /// Resolves a possibly-relative link target against the link's own directory.
    ///
    /// A tar entry stores a link target exactly as written, which for `bin/npm` is
    /// `../lib/node_modules/npm/bin/npm-cli.js` — relative to `bin/`, not to wherever
    /// the launcher happens to be running.
    ///
    /// @param link   the link path
    /// @param target the target as written
    /// @return the target resolved against the link's directory
    private static Path resolvedAgainst(Path link, Path target) {
        if (target.isAbsolute()) {
            return target;
        }
        Path parent = link.getParent();
        return parent == null ? target : parent.resolve(target).normalize();
    }
}
