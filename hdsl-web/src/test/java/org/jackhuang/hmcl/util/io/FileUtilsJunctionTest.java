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

import org.jackhuang.hmcl.util.platform.OperatingSystem;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/// Deleting a tree that contains a link to something that is gone.
///
/// pnpm links its packages with **junctions** on Windows. Java does not report a junction as
/// a symbolic link: it says `isDirectory() == true` **and** `isOther() == true`, so a plain
/// `walkFileTree` descends into it. When the target has been removed the walker then hands
/// `Files.delete` a path that does not exist, throws `NoSuchFileException`, and the delete
/// stops halfway — "deleting this instance never finishes". The same call is the first step
/// of every reinstall (remove the previous tree), so it blocked those too.
class FileUtilsJunctionTest {

    @TempDir
    Path temp;

    @Test
    void aBrokenJunctionDoesNotStopTheDelete() throws Exception {
        assumeTrue(OperatingSystem.CURRENT_OS == OperatingSystem.WINDOWS, "junctions are a Windows notion");

        Path tree = Files.createDirectories(temp.resolve("tree"));
        Files.writeString(tree.resolve("a.txt"), "hello");
        Path target = temp.resolve("gone");
        Files.createDirectories(target);
        Path link = tree.resolve("link");
        ProcessBuilder builder = new ProcessBuilder("cmd", "/c", "mklink", "/J",
                link.toString(), target.toString());
        builder.redirectErrorStream(true);
        Process process = builder.start();
        process.getInputStream().transferTo(java.io.OutputStream.nullOutputStream());
        assumeTrue(process.waitFor() == 0, "this machine cannot create junctions");

        // The link must be there, and the target disappears — exactly the state an interrupted
        // pnpm install leaves behind.
        assertTrue(Files.isDirectory(link));
        FileUtils.deleteDirectory(target);

        FileUtils.deleteDirectory(tree);

        assertFalse(Files.exists(tree, java.nio.file.LinkOption.NOFOLLOW_LINKS), "the whole tree is gone");
    }

    @Test
    void aLiveJunctionIsRemovedAsALinkNotFollowed() throws Exception {
        assumeTrue(OperatingSystem.CURRENT_OS == OperatingSystem.WINDOWS, "junctions are a Windows notion");

        Path outside = Files.createDirectories(temp.resolve("outside"));
        Files.writeString(outside.resolve("keep.txt"), "keep me");
        Path tree = Files.createDirectories(temp.resolve("tree"));
        ProcessBuilder builder = new ProcessBuilder("cmd", "/c", "mklink", "/J",
                tree.resolve("link").toString(), outside.toString());
        builder.redirectErrorStream(true);
        Process process = builder.start();
        process.getInputStream().transferTo(java.io.OutputStream.nullOutputStream());
        assumeTrue(process.waitFor() == 0, "this machine cannot create junctions");

        FileUtils.deleteDirectory(tree);

        assertTrue(Files.isRegularFile(outside.resolve("keep.txt")),
                "the junction's target is not the tree's to delete");
    }
}
