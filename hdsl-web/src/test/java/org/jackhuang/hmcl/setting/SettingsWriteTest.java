/*
 * HDSL-web
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
package org.jackhuang.hmcl.setting;

import com.google.gson.JsonObject;
import org.jackhuang.hmcl.Metadata;
import org.jackhuang.hmcl.util.gson.JsonUtils;
import org.jackhuang.hmcl.util.platform.OperatingSystem;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Verifies how the settings file is put on disk, rather than what it holds.
///
/// The file holds the accounts and their API keys, and it is read at start-up by everything that
/// starts beside it. Writing it in place truncates it first, and a reader that arrives in that window
/// opens a file that is empty or half written — for this file that is no accounts and no keys at all.
/// What the write has to be is a move: the file beside it is written whole and put in place, so a
/// reader opens one version or the other.
///
/// The move is asserted through the file's identity: a write that goes through the file it is
/// replacing keeps its inode, and a move gives it a new one. That is the mechanism, and it is the one
/// thing about this that can be checked without racing a reader — the window a reader could see is
/// exactly what is not observable from the outside once the write is a move.
class SettingsWriteTest {

    private static final Path SETTINGS = Metadata.HMCL_USER_HOME.resolve("launcher-settings.json");

    @Test
    void savingReplacesTheSettingsFileInsteadOfWritingThroughIt() throws IOException {
        Assumptions.assumeFalse(OperatingSystem.CURRENT_OS == OperatingSystem.WINDOWS,
                "NTFS has no inode to compare");

        SettingsManager.save();
        assertTrue(Files.isRegularFile(SETTINGS), "saving writes the file");
        Object writtenInPlace = Files.getAttribute(SETTINGS, "unix:ino");

        SettingsManager.save();

        assertNotEquals(writtenInPlace, Files.getAttribute(SETTINGS, "unix:ino"),
                "the second save replaced the file rather than writing through it, so a reader opens"
                        + " one whole version of it and never a truncated one");
    }

    @Test
    void theFileWrittenBesideItIsGoneAfterwards() throws IOException {
        SettingsManager.save();

        List<String> leftovers;
        try (Stream<Path> files = Files.list(SETTINGS.getParent())) {
            leftovers = files.map(file -> file.getFileName().toString())
                    .filter(name -> name.startsWith("launcher-settings") && !name.equals("launcher-settings.json"))
                    .toList();
        }

        assertEquals(List.of(), leftovers, "the file written beside it is not left behind");
    }

    @Test
    void whatIsSavedIsWholeAndStillOnlyReadableByItsOwner() throws IOException {
        SettingsManager.save();

        JsonObject saved = JsonUtils.fromJsonFile(SETTINGS, JsonObject.class);
        assertTrue(saved != null && saved.size() > 0, "what is on disk is a whole settings file");

        Assumptions.assumeFalse(OperatingSystem.CURRENT_OS == OperatingSystem.WINDOWS,
                "NTFS has no POSIX mode to check");
        Set<PosixFilePermission> permissions = Files.getPosixFilePermissions(SETTINGS);
        assertEquals(Set.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE), permissions,
                "the move must carry the restriction the file was given while it was being written");
    }
}