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
package org.jackhuang.hmcl.dsh;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// What happens to the version an instance already had when a new install does not
/// finish.
///
/// This is the guarantee the old staging directory provided ("an interrupted install can
/// never look like a usable runtime") and that installing in place — required on Windows,
/// where pnpm writes junctions with absolute targets — has to provide another way. A
/// failed install must leave the instance with the version it had, not with nothing.
class DshVersionManagerRollbackTest {

    @Test
    void aFailedInstallKeepsThePreviousVersion(@TempDir Path instance) throws IOException {
        Path target = Files.createDirectories(instance.resolve("dsh"));
        Path previous = instance.resolve("dsh.previous");
        Files.writeString(target.resolve("keep.txt"), "old");

        DshVersionManager.moveAside(target, previous);
        assertFalse(Files.exists(target), "the working tree is moved, not deleted");
        assertEquals("old", Files.readString(previous.resolve("keep.txt")));

        // The new tree is half-written when the install fails.
        Files.createDirectories(target);
        Files.writeString(target.resolve("half.txt"), "partial");
        DshVersionManager.restorePrevious(previous, target);

        assertEquals("old", Files.readString(target.resolve("keep.txt")),
                "the version that worked is back on the path the instance uses");
        assertFalse(Files.exists(target.resolve("half.txt")), "nothing half-written survives");
        assertFalse(Files.exists(previous), "the backup is consumed by the restore");
    }

    @Test
    void anInterruptedInstallRestoresTheKeptVersion(@TempDir Path instance) throws IOException {
        Path target = instance.resolve("dsh");
        Path flag = Files.createDirectories(instance.resolve("dsh.installing"));
        Path previous = Files.createDirectories(instance.resolve("dsh.previous"));
        Files.createDirectories(target);
        Files.writeString(target.resolve("half.txt"), "partial");
        Files.writeString(previous.resolve("keep.txt"), "old");

        DshVersionManager.recoverInterruptedInstall("demo", target, flag, previous);

        assertFalse(Files.exists(flag), "the flag comes down");
        assertEquals("old", Files.readString(target.resolve("keep.txt")));
        assertFalse(Files.exists(target.resolve("half.txt")));
        assertFalse(Files.exists(previous));
    }

    @Test
    void aPublishedInstallDiscardsTheStaleBackup(@TempDir Path instance) throws IOException {
        Path target = Files.createDirectories(instance.resolve("dsh"));
        Path previous = Files.createDirectories(instance.resolve("dsh.previous"));
        Files.writeString(target.resolve("new.txt"), "new");
        Files.writeString(previous.resolve("old.txt"), "old");

        DshVersionManager.recoverInterruptedInstall("demo", target,
                instance.resolve("dsh.installing"), previous);

        assertEquals("new", Files.readString(target.resolve("new.txt")),
                "no flag means the tree at the target is the published one");
        assertFalse(Files.exists(previous), "the backup is a leftover");
    }

    @Test
    void aFlagWithoutABackupRemovesThePartialTree(@TempDir Path instance) throws IOException {
        Path target = Files.createDirectories(instance.resolve("dsh"));
        Path flag = Files.createDirectories(instance.resolve("dsh.installing"));
        Files.writeString(target.resolve("half.txt"), "partial");

        DshVersionManager.recoverInterruptedInstall("demo", target, flag,
                instance.resolve("dsh.previous"));

        assertFalse(Files.exists(target), "a partial tree must never be launched");
        assertFalse(Files.exists(flag));
    }

    @Test
    void restoringWithoutABackupChangesNothing(@TempDir Path instance) throws IOException {
        Path target = Files.createDirectories(instance.resolve("dsh"));
        Files.writeString(target.resolve("new.txt"), "new");

        DshVersionManager.restorePrevious(instance.resolve("dsh.previous"), target);

        assertEquals("new", Files.readString(target.resolve("new.txt")));
    }
}
