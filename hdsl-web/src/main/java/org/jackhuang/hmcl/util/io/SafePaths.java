/*
 * Hello Minecraft! Launcher
 * Copyright (C) 2026 huangyuhui <huanghongxun2008@126.com> and contributors
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
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;

/// Where something out of an archive may land, and where it may not.
///
/// The check this exists for is the one that is **not** made of strings. `a/../../b` is caught by
/// normalising and comparing, and must still be, because the name is the half an attacker writes —
/// but a name that stays inside lexically can still *land* outside: if `dest/sub` is a symbolic link
/// to somewhere else, then `dest/sub/inside.txt` begins with `dest` and every write through it
/// happens on the other side of the link. `Files.createDirectories` does not help either — it treats
/// an existing link to a directory as the directory it points at and steps through it.
///
/// A pack cannot plant such a link: a tar's link entries are dropped, `java.util.zip` reads no mode
/// bits, and neither importer calls `Files.createSymbolicLink`. It does not have to plant one. A
/// `link:` or `file:` plugin leaves symlink directories behind, and those are found rather than made.
///
/// So both halves are asked, and the real half is asked of the deepest ancestor that **exists**: that
/// is the one component a link could be hiding in, and everything below it is a name this can create.
///
/// A link that points back **inside** the root is allowed. It is a legitimate arrangement — a
/// `skills/` directory kept on another disk with a link to it is the case this must not break — and
/// what the check is about is leaving the root, not the link.
@NotNullByDefault
public final class SafePaths {

    private SafePaths() {
    }

    /// Returns where a member may land inside a root, or `null` when it may not.
    ///
    /// @param root the directory the path must stay inside
    /// @param name the member's name, which is the untrusted half
    /// @return where it lands, or `null` when that is not inside `root`
    public static @Nullable Path resolveInside(Path root, String name) {
        return resolveInside(root, root.toAbsolutePath().normalize().resolve(name));
    }

    /// Returns a path when it may be written inside a root, or `null` when it may not.
    ///
    /// Both halves are checked: the normalised path has to begin with the root — which is what
    /// answers `a/../../b`, and what makes the *name* the thing that decides — and the deepest
    /// component of it that already exists has to really be inside the root, which is what answers a
    /// link standing in the way.
    ///
    /// @param root      the directory the path must stay inside
    /// @param candidate where it would be written
    /// @return the normalised path, or `null` when it is not inside `root`
    public static @Nullable Path resolveInside(Path root, Path candidate) {
        Path base = root.toAbsolutePath().normalize();
        Path target = candidate.isAbsolute() ? candidate.normalize() : base.resolve(candidate).normalize();
        if (!target.startsWith(base)) {
            return null;
        }
        return reallyInside(base, target) ? target : null;
    }

    /// Creates a directory and its parents, refusing to leave the root on the way down.
    ///
    /// The parents are walked one component at a time rather than handed to
    /// `Files.createDirectories`, because that call cannot be asked whether a component it found was
    /// a directory or a link to one. A component that exists is checked instead of assumed, and only
    /// a component that does not exist is created — so a link that leaves the root is refused even
    /// when it appeared after [#resolveInside] looked.
    ///
    /// @param root      the directory the path must stay inside
    /// @param directory the directory to create
    /// @throws IOException when the directory cannot be created, or when a component of it leaves
    ///                     the root
    public static void createDirectories(Path root, Path directory) throws IOException {
        Path base = root.toAbsolutePath().normalize();
        Path target = directory.isAbsolute() ? directory.normalize() : base.resolve(directory).normalize();
        if (!target.startsWith(base)) {
            throw new IOException("Refusing to create " + directory + " outside " + base);
        }

        Path current = base;
        if (!Files.exists(current, LinkOption.NOFOLLOW_LINKS)) {
            // The root itself is the caller's own choice, so it is created as a directory rather than
            // judged: what this refuses is a *link* between it and the target, not the root.
            Files.createDirectories(current);
        }
        for (Path segment : base.relativize(target)) {
            current = current.resolve(segment);
            if (Files.exists(current, LinkOption.NOFOLLOW_LINKS)) {
                if (!reallyInside(base, current) || !Files.isDirectory(current)) {
                    throw new IOException("Refusing to create " + directory + ": " + current
                            + " is not a directory inside " + base);
                }
            } else {
                try {
                    Files.createDirectory(current);
                } catch (FileAlreadyExistsException e) {
                    // Created between the check above and here. Asked again rather than assumed, so a
                    // racer's link is refused by the same rule as one that was there all along.
                    if (!Files.isDirectory(current, LinkOption.NOFOLLOW_LINKS) || !reallyInside(base, current)) {
                        throw e;
                    }
                }
            }
        }
    }

    /// Reports whether the part of a path that already exists really is inside a root.
    ///
    /// The deepest existing ancestor of the path is the one question worth asking of the filesystem:
    /// `toRealPath` on it follows every link it is made of, so a link anywhere in the chain shows up
    /// in the answer, while the components below it do not exist and therefore cannot be links — they
    /// are names about to be created.
    ///
    /// What it is compared against is the deepest ancestor of the **root**, not the root spelled out
    /// in full: a destination that is itself not there yet — a profile the pack is about to create —
    /// has no real path, and everything between the ancestor and the root is, by the same argument,
    /// names rather than links. A root that is reached through a link is not a false alarm either,
    /// since both ends of the comparison are resolved the same way.
    ///
    /// A path whose existing part cannot be resolved — a link whose target is not there — is not
    /// inside anything.
    ///
    /// @param base   the root, absolute and normalised
    /// @param target the path, already known to begin with `base` lexically
    /// @return whether what exists of it really is inside the root
    private static boolean reallyInside(Path base, Path target) {
        Path existing = existingPartOf(target);
        Path root = existingPartOf(base);
        if (existing == null || root == null) {
            return false;
        }
        try {
            return existing.toRealPath().startsWith(root.toRealPath());
        } catch (IOException e) {
            return false;
        }
    }

    /// Returns the deepest ancestor of a path that is there, including as a link.
    ///
    /// @param path the path
    /// @return that ancestor, or `null` when not even the filesystem root is readable
    private static @Nullable Path existingPartOf(Path path) {
        Path existing = path;
        while (existing != null && !Files.exists(existing, LinkOption.NOFOLLOW_LINKS)) {
            existing = existing.getParent();
        }
        return existing;
    }
}