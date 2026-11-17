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
package org.jackhuang.hmcl.util;

import org.jackhuang.hmcl.util.platform.OperatingSystem;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/// Processes nobody can see, named by the path they sit on.
///
/// A panel restarted since a launch has no record of the child it started, and on Windows a
/// directory cannot be removed while any process has it as its current directory — so
/// "delete this instance" failed with an empty error while an invisible agent sat on the
/// tree. The match is on the command line, which is where a launcher names the instance.
class ProcessTreeKillReferencingTest {

    @TempDir
    Path temp;

    @Test
    void aDirectoryNothingReferencesKillsNothing() {
        assertEquals(0, ProcessTree.killReferencing(temp));
    }

    @Test
    void anInstanceWhoseIdExtendsTheDeletedOneIsNotMatched() {
        String deleted = "F:\\data\\instances\\demo";
        String working = "F:\\data\\instances\\demo-2\\node_modules\\dsh\\lib\\bin.js";
        assertTrue(ProcessTree.references(deleted + "\\dsh\\node_modules\\a\\bin.js", deleted),
                "a process inside the deleted instance is matched");
        assertTrue(!ProcessTree.references(working, deleted),
                "the instance whose id merely starts with it is not: " + working);
        assertTrue(!ProcessTree.references("node /srv/instances/demo-2/bin.js", "/srv/instances/demo"),
                "the same rule with forward slashes");
        assertTrue(ProcessTree.references("node /srv/instances/demo/bin.js", "/srv/instances/demo"));
    }

    @Test
    void thePanelItselfIsNotMatched() {
        // The panel's own command line names the data directory, never an instance
        // directory, and this test running here proves a live JVM is not swept by a path it
        // happens to share. Nothing in this JVM's command line names `temp`.
        String here = ProcessHandle.current().info().commandLine().orElse("");
        assertTrue(!here.contains(temp.toAbsolutePath().toString()),
                "the test JVM must not reference the temp directory: " + here);
    }

    /// The kill is a Windows repair, and Windows is the only place it may run.
    ///
    /// What it repairs is a Windows rule: a directory cannot be removed while a process has it
    /// as its current directory. POSIX unlinks a directory whatever still holds it open, so
    /// there is nothing to repair — and a command-line match there would take down whatever an
    /// operator runs on the tree: `tail -f`, an `rsync`, a nightly backup.
    @Test
    void aProcessNamingTheDirectoryIsOnlyKilledOnWindows() throws Exception {
        Path directory = Files.createDirectories(temp.resolve("instances").resolve("demo"));
        Process probe = spawnNaming(directory);
        try {
            assumeTrue(isNamed(directory),
                    "this machine does not let a process read other command lines, so the rule cannot be exercised either way");
            int killed = ProcessTree.killReferencing(directory);
            if (OperatingSystem.CURRENT_OS == OperatingSystem.WINDOWS) {
                assertTrue(probe.waitFor(10, TimeUnit.SECONDS),
                        "the stray that names the directory is stopped so the delete can proceed");
                assertTrue(killed >= 1, "and it is reported as stopped");
            } else {
                assertFalse(probe.waitFor(2, TimeUnit.SECONDS),
                        "nothing on this platform is killed for naming a directory");
                assertEquals(0, killed);
            }
        } finally {
            probe.destroyForcibly();
            probe.waitFor(5, TimeUnit.SECONDS);
        }
    }

    /// Whether any process's command line names something under `directory`.
    private static boolean isNamed(Path directory) throws InterruptedException {
        String needle = directory.toAbsolutePath().normalize().toString();
        for (int attempt = 0; attempt < 10; attempt++) {
            boolean found = ProcessHandle.allProcesses()
                    .anyMatch(handle -> handle.info().commandLine()
                            .map(command -> command.contains(needle + "\\") || command.contains(needle + "/"))
                            .orElse(false));
            if (found) {
                return true;
            }
            Thread.sleep(200);
        }
        return false;
    }

    /// A process that sits on `directory` and names it, with a trailing separator, on its own
    /// command line.
    private static Process spawnNaming(Path directory) throws Exception {
        if (OperatingSystem.CURRENT_OS == OperatingSystem.WINDOWS) {
            // `rem` swallows the path, so any characters in it are harmless; the redirect target
            // is what puts `<directory>\` on the command line.
            return new ProcessBuilder("cmd.exe", "/c",
                    "ping -n 60 127.0.0.1 > nul & rem " + directory.resolve("probe.txt"))
                    .redirectErrorStream(true)
                    .start();
        }
        return new ProcessBuilder("sh", "-c",
                "cd \"" + directory + "/\" && sleep 30 && true")
                .redirectErrorStream(true)
                .start();
    }
}
