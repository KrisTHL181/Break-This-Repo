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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Verifies what an exported log file is called, and where it goes.
///
/// A folder of exported logs is only useful if the files in it can be told apart without opening
/// them, so the name is the feature: which launcher wrote it, which instance it came from, and when
/// it was taken. Where it goes matters for the same reason — the original wrote it beside the
/// process's working directory, which is a place somebody looking for their log has no reason to
/// look in.
class DshLogsTest {
    /// The moment the names below are stamped with.
    private static final LocalDateTime MOMENT = LocalDateTime.of(2026, 9, 27, 21, 58, 30);

    @Test
    void anExportIsNamedForTheLauncherTheInstanceAndTheMoment() {
        assertEquals("hdsl-github-test-2026-09-27T21-58-30.log",
                DshLogs.fileName("github-test", MOMENT));
    }

    @Test
    void anExportWithNoInstanceIsStillNamedForTheLauncherAndTheMoment() {
        assertEquals("hdsl-2026-09-27T21-58-30.log", DshLogs.fileName(null, MOMENT));
        assertEquals("hdsl-2026-09-27T21-58-30.log", DshLogs.fileName("   ", MOMENT));
    }

    @Test
    void anInstanceNameIsMadeIntoOneAFileNameCanHold() {
        assertEquals("hdsl-my-instance-2026-09-27T21-58-30.log", DshLogs.fileName("my/instance", MOMENT),
                "a separator in a name is not a directory in the export");
        assertEquals("hdsl-我的实例-2026-09-27T21-58-30.log", DshLogs.fileName("我的实例", MOMENT),
                "a name in another script is still the name somebody gave it");
        assertFalse(DshLogs.fileName("a:b*c?", MOMENT).contains(":"),
                "a colon cannot be in a file name at all");
        assertEquals("hdsl-2026-09-27T21-58-30.log", DshLogs.fileName("..", MOMENT),
                "a name that is a path is no name to put in a file name");
    }

    @Test
    void anExportIsWrittenWhereItWasAskedFor(@TempDir Path directory) throws Exception {
        Path file = DshLogs.write(directory, "github-test", List.of("one", "two"), MOMENT);

        assertEquals(directory.resolve("hdsl-github-test-2026-09-27T21-58-30.log"), file);
        assertEquals(List.of("one", "two"), Files.readAllLines(file));
    }

    @Test
    void theDirectoryIsMadeWhenItIsNotThere(@TempDir Path directory) throws Exception {
        Path logs = directory.resolve("logs");
        assertFalse(Files.exists(logs));

        Path file = DshLogs.write(logs, "github-test", List.of("one"), MOMENT);

        assertTrue(Files.isRegularFile(file));
    }

    @Test
    void aSecondExportInTheSameSecondDoesNotOverwriteTheFirst(@TempDir Path directory) throws Exception {
        Path first = DshLogs.write(directory, "github-test", List.of("first"), MOMENT);
        Path second = DshLogs.write(directory, "github-test", List.of("second"), MOMENT);

        assertNotEquals(first, second, "the second export gets a name of its own");
        assertEquals(List.of("first"), Files.readAllLines(first),
                "and the first one is what the person is trying to keep");
        assertEquals(List.of("second"), Files.readAllLines(second));
    }
}
