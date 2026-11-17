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

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Verifies the two questions [SafePaths] asks, and what each of them answers.
///
/// The lexical half is about the name and the real half is about the destination, and the tests are
/// split the same way: a name that climbs out is refused without any file existing to look at, while
/// a link that leaves is only visible by resolving what is already there. The cases that must keep
/// working are as pinned as the ones that must not — a link back inside is a legitimate arrangement,
/// and a destination that does not exist yet has no real path to be compared with.
class SafePathsTest {

    @Test
    void aNameThatStaysInsideResolves(@TempDir Path directory) {
        Path root = directory.resolve("profile");

        assertEquals(root.resolve("a/b.txt"), SafePaths.resolveInside(root, "a/b.txt"));
        assertEquals(root.resolve("b.txt"), SafePaths.resolveInside(root, "a/../b.txt"),
                "a step that lands back inside is not an escape");
        assertEquals(root, SafePaths.resolveInside(root, ""));
    }

    @Test
    void aNameThatClimbsOutIsRefused(@TempDir Path directory) {
        Path root = directory.resolve("profile");

        assertNull(SafePaths.resolveInside(root, "../b.txt"));
        assertNull(SafePaths.resolveInside(root, "a/../../b.txt"));
        assertNull(SafePaths.resolveInside(root, "/etc/passwd"),
                "an absolute name is not inside anything relative to the root");
    }

    @Test
    void aLinkOutOfTheRootIsRefused(@TempDir Path directory) throws Exception {
        Path outside = Files.createDirectory(directory.resolve("outside"));
        Path root = Files.createDirectory(directory.resolve("profile"));
        makeLink(root.resolve("sub"), outside);

        assertNull(SafePaths.resolveInside(root, "sub/inside.txt"),
                "the name is inside the root lexically and the write would not be");
        assertNull(SafePaths.resolveInside(root, root.resolve("sub/inside.txt")));
        assertNull(SafePaths.resolveInside(root, "sub"),
                "and the link itself is not a file to be replaced inside the root");
    }

    @Test
    void aLinkDeeperInTheChainIsFoundToo(@TempDir Path directory) throws Exception {
        Path outside = Files.createDirectory(directory.resolve("outside"));
        Path profile = Files.createDirectories(directory.resolve("profile/a"));
        makeLink(profile.resolve("b"), outside);

        assertNull(SafePaths.resolveInside(directory.resolve("profile"), "a/b/c.txt"));
    }

    @Test
    void aLinkBackInsideTheRootIsStillAllowed(@TempDir Path directory) throws Exception {
        Path root = Files.createDirectory(directory.resolve("profile"));
        Path kept = Files.createDirectories(root.resolve("kept"));
        makeLink(root.resolve("sub"), kept);

        assertEquals(root.resolve("sub/inside.txt"), SafePaths.resolveInside(root, "sub/inside.txt"),
                "a link that stays inside is a legitimate arrangement, and what matters is leaving");
    }

    @Test
    void aDestinationThatIsNotThereYetIsStillInside(@TempDir Path directory) {
        Path root = directory.resolve("profile");

        assertEquals(root.resolve("a/b.txt"), SafePaths.resolveInside(root, "a/b.txt"),
                "a profile the pack is about to create has no real path to compare with");
    }

    @Test
    void createDirectoriesMakesThePath(@TempDir Path directory) throws Exception {
        Path root = directory.resolve("profile/not-there-yet");

        SafePaths.createDirectories(root, root.resolve("a/b/c"));

        assertTrue(Files.isDirectory(root.resolve("a/b/c")));
    }

    @Test
    void createDirectoriesRefusesALinkOutOfTheRoot(@TempDir Path directory) throws Exception {
        Path outside = Files.createDirectory(directory.resolve("outside"));
        Path root = Files.createDirectory(directory.resolve("profile"));
        makeLink(root.resolve("sub"), outside);

        assertThrows(IOException.class,
                () -> SafePaths.createDirectories(root, root.resolve("sub/deep")));
        assertFalse(Files.exists(outside.resolve("deep")),
                "nothing was made on the other side of the link");
    }

    @Test
    void createDirectoriesRefusesAPathOutsideTheRoot(@TempDir Path directory) throws Exception {
        Path root = Files.createDirectory(directory.resolve("profile"));

        assertThrows(IOException.class,
                () -> SafePaths.createDirectories(root, directory.resolve("elsewhere")));
        assertFalse(Files.exists(directory.resolve("elsewhere")));
    }

    /// Makes a symbolic link, skipping the test where this system will not.
    ///
    /// A Windows without Developer Mode refuses the call itself, and a filesystem with no links
    /// whatever does the same — neither is a system where the answer below would be wrong, so
    /// neither is a failure.
    ///
    /// @param link   the link to create
    /// @param target where it points
    private static void makeLink(Path link, Path target) {
        boolean made = false;
        try {
            Files.createSymbolicLink(link, target);
            made = true;
        } catch (UnsupportedOperationException | IOException e) {
            // Asked of the assumption below rather than reported here.
        }
        Assumptions.assumeTrue(made, "this filesystem will not make a symbolic link");
    }
}