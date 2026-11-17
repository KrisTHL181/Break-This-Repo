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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// What the tombstone sweep is allowed to delete.
///
/// An instance's directory is named after an id the user chose, and the id keeps its dots, so
/// `v1.deleting-2` is an ordinary instance. A sweep that keyed on the `.deleting-` substring
/// deleted it whole, with one "removed the leftover" line to show for it.
class TombstonesTest {

    private static final String MANIFEST = "instance.json";

    /// Reads the `id` out of a manifest fixture — all the sweep asks of the real readers.
    private static final Function<Path, String> ID = directory -> {
        Path manifest = directory.resolve(MANIFEST);
        if (!Files.isRegularFile(manifest)) {
            return null;
        }
        try {
            String json = Files.readString(manifest);
            int at = json.indexOf("\"id\":\"");
            return at < 0 ? null : json.substring(at + 6, json.indexOf('"', at + 6));
        } catch (IOException e) {
            return null;
        }
    };

    @TempDir
    Path temp;

    @Test
    void anInstanceCalledLikeALeftoverIsNotSwept() throws Exception {
        Path live = instance("v1.deleting-2", "v1.deleting-2");

        assertFalse(Tombstones.isSweepable(live, MANIFEST, ID), "the user named it; it is an instance");
    }

    @Test
    void aManifestNamingTheDirectoryProtectsEvenTheExactShapeOfATombstone() throws Exception {
        // The name alone cannot settle it: this matches everything the sweep writes, and it is
        // still an instance because its manifest is found exactly where its id says.
        Path live = instance("v9.deleting-1793409532000", "v9.deleting-1793409532000");

        assertFalse(Tombstones.isSweepable(live, MANIFEST, ID));
    }

    @Test
    void aRealLeftoverIsSwept() throws Exception {
        Path leftover = Files.createDirectories(temp.resolve("demo.deleting-1793409532000"));
        Files.writeString(leftover.resolve("kept-by-someone-else.log"), "still held");

        assertTrue(Tombstones.isSweepable(leftover, MANIFEST, ID));
    }

    @Test
    void aLeftoverThatKeptItsManifestIsStillSwept() throws Exception {
        // The delete emptied what it could and gave up before the manifest: the tree is gone but
        // the manifest still names the id it was created under, which is not the tombstone it now
        // sits in. It has to be swept, or it stays forever.
        Path leftover = instance("demo.deleting-1793409532000", "demo");

        assertTrue(Tombstones.isSweepable(leftover, MANIFEST, ID));
    }

    @Test
    void theTreeInsideAnInstanceIsSweptToo() throws Exception {
        Path nested = Files.createDirectories(temp.resolve("dsh.previous.deleting-1793409532000"));

        assertTrue(Tombstones.isSweepable(nested, MANIFEST, ID));
    }

    @Test
    void aNameThatIsNotOurShapeIsNotSwept() throws Exception {
        for (String name : new String[]{
                "demo.deleting-",           // no stamp at all
                "demo.deleting-2",          // the reported id: one digit is a user's number
                "demo.deleting-123456789",  // a nine-digit stamp is not `currentTimeMillis`
                "demo.deleting-abc",
                "deleting-1793409532000",   // no name in front of the marker
                "demo.deleting-1793409532000.bak",
                "demo.deleting",
        }) {
            Path directory = Files.createDirectories(temp.resolve(name));
            assertFalse(Tombstones.isSweepable(directory, MANIFEST, ID), name + " is not a tombstone we wrote");
        }
    }

    @Test
    void tenDigitsAreEnough() throws Exception {
        // `currentTimeMillis()` is thirteen digits today; the shape must not depend on that.
        Path leftover = Files.createDirectories(temp.resolve("demo.deleting-1234567890"));

        assertTrue(Tombstones.isSweepable(leftover, MANIFEST, ID));
    }

    @Test
    void aFileIsNotALeftover() throws Exception {
        Path file = Files.writeString(temp.resolve("demo.deleting-1793409532000"), "not a directory");

        assertFalse(Tombstones.isSweepable(file, MANIFEST, ID), "a delete leaves a directory behind, never a file");
    }

    /// A directory with a manifest recording `id`.
    private Path instance(String directoryName, String id) throws Exception {
        Path directory = Files.createDirectories(temp.resolve(directoryName));
        Files.writeString(directory.resolve(MANIFEST), "{\"id\":\"" + id + "\"}");
        return directory;
    }
}
