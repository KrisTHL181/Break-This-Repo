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

import org.jackhuang.hmcl.util.io.FileUtils;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The startup sweep, against the directory names it actually meets.
///
/// A delete that cannot finish renames its target to `<id>.deleting-<millis>` and leaves the
/// rest to the next start. The instances root holds directories named after ids the **user**
/// chose, so a sweep that keyed on the `.deleting-` substring deleted the instance a user had
/// called `v1.deleting-2` — its whole install with it — and said only "removed the leftover".
class DshInstanceSweepTest {

    @Test
    void anInstanceNamedLikeALeftoverSurvivesTheSweep() throws Exception {
        String id = "v1.deleting-2";
        Path directory = DshPaths.instanceDirectory(id);
        // A leftover of the shape the sweep writes, so the run has something to remove too: a
        // sweep that removed nothing would pass this test for the wrong reason.
        String stale = "ghost-" + Long.toHexString(System.nanoTime()) + ".deleting-1793409532000";
        Path leftover = DshPaths.INSTANCES.resolve(stale);

        try {
            create(id);
            Files.writeString(Files.createDirectories(leftover.resolve("home")).resolve("session.log"),
                    "a delete that never finished");

            DshInstanceManager.sweepTombstones();

            assertNotNull(DshInstanceManager.find(id), "the instance the user named is still listed");
            assertTrue(Files.isRegularFile(directory.resolve(DshInstanceManager.MANIFEST_NAME)));
            assertTrue(Files.isDirectory(directory.resolve("home")), "and its tree was not emptied");
            assertFalse(Files.exists(leftover), "while the leftover of a real delete is gone");
        } finally {
            sweepUp(directory, leftover);
        }
    }

    @Test
    void aLiveManifestOutranksTheName() throws Exception {
        // This name matches the sweep's own shape exactly. What makes it an instance rather than
        // a leftover is its manifest, and that has to be enough on its own.
        String id = "v9.deleting-1793409532000";
        Path directory = DshPaths.instanceDirectory(id);

        try {
            create(id);

            DshInstanceManager.sweepTombstones();

            assertNotNull(DshInstanceManager.find(id), "an instance with a manifest is never a leftover");
            assertTrue(Files.isDirectory(directory.resolve("home")));
        } finally {
            sweepUp(directory);
        }
    }

    private static void create(String id) throws Exception {
        DshInstanceManager.create(id, "1.0.0", DshInstance.DEFAULT_PROFILE,
                Path.of(System.getProperty("java.io.tmpdir")),
                DshHomeMode.ISOLATED, null, List.of(), Map.of());
    }

    private static void sweepUp(Path... directories) {
        for (Path directory : directories) {
            FileUtils.deleteDirectoryQuietly(directory);
        }
    }
}
