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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// When a resource read fails, the answer has to say whether the file moved under
/// the running process — replacing a jar on Windows succeeds silently and then
/// breaks every not-yet-loaded entry.
class PanelJarTest {

    @TempDir
    Path temp;

    @AfterEach
    void forget() {
        PanelJar.remember((Path) null);
    }

    @Test
    void anUnchangedJarHasNothingToSay() throws Exception {
        Path jar = Files.write(temp.resolve("panel.jar"), new byte[]{1, 2, 3});
        PanelJar.remember(jar);
        assertTrue(PanelJar.replacedHint().isEmpty(), PanelJar.replacedHint().orElse(""));
    }

    @Test
    void aReplacedJarIsReportedWithBothFacts() throws Exception {
        Path jar = Files.write(temp.resolve("panel.jar"), new byte[64]);
        PanelJar.remember(jar);
        Files.write(jar, new byte[128]);
        Files.setLastModifiedTime(jar, FileTime.fromMillis(System.currentTimeMillis() + 2000));

        String hint = PanelJar.replacedHint().orElseThrow();
        assertTrue(hint.contains(jar.toString()), hint);
        assertTrue(hint.contains("重启面板"), hint);
    }

    @Test
    void aDeletedJarCountsAsReplaced() throws Exception {
        Path jar = Files.write(temp.resolve("panel.jar"), new byte[8]);
        PanelJar.remember(jar);
        Files.delete(jar);
        assertTrue(PanelJar.replacedHint().isPresent());
    }

    /// A panel that never ran from a jar (tests, an exploded classes directory) has
    /// nothing to compare, so the hint must stay silent rather than guess.
    @Test
    void nothingIsClaimedWhenNoJarWasRecorded() {
        PanelJar.remember((Path) null);
        assertFalse(PanelJar.replacedHint().isPresent());
    }
}
