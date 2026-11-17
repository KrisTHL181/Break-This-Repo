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

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Verifies where a pack's members land, and what a member may not reach.
///
/// A pack cannot plant a link of its own — a tar's link entries are dropped and `java.util.zip` reads
/// no mode bits, so the importer never calls `Files.createSymbolicLink`. A link that is **already
/// there** is a different matter, and that is what these drive: a component of the destination that
/// is a symlink out of it, with the member underneath it. The check that was defeated was lexical, so
/// it saw `dest/sub/inside.txt` beginning with `dest` and wrote through the link; `land` is the
/// public entry point that performs the write, and it is the one driven here.
class DshPackLandingTest {

    /// A member whose parent is a link out of the destination leaves nothing outside it.
    ///
    /// Written as "run it, then look outside" rather than as a `assertThrows`: the assertion that
    /// must hold either way is that the bytes are not on the other side of the link, and a test that
    /// only asked for a refusal would pass on a refusal that came too late.
    ///
    /// @param temp where the profile and the pack are built
    /// @throws Exception when the fixture cannot be built
    @Test
    void aMemberUnderAnExistingLinkLeavesNothingOutside(@TempDir Path temp) throws Exception {
        Path outside = Files.createTempDirectory("dshpack-outside");
        Path destination = temp.resolve("profile");
        Files.createDirectories(destination);
        Files.createSymbolicLink(destination.resolve("sub"), outside);

        Path pack = packOf(temp, "overrides/sub/inside.txt", "planted");

        try {
            DshPackInstaller.land(pack, destination, null, EnumSet.of(DshPackInstaller.Part.OVERRIDES));
        } catch (DshException refused) {
            // The fix refuses the member; that is one of the two answers this pins. What neither
            // answer may be is a write through the link.
        }

        assertFalse(Files.exists(outside.resolve("inside.txt")),
                "the member was written through the link, outside the profile: "
                        + outside.resolve("inside.txt"));
    }

    /// And it is refused, rather than written somewhere else instead.
    ///
    /// @param temp where the profile and the pack are built
    /// @throws Exception when the fixture cannot be built
    @Test
    void aMemberUnderAnExistingLinkIsRefused(@TempDir Path temp) throws Exception {
        Path outside = Files.createTempDirectory("dshpack-outside");
        Path destination = temp.resolve("profile");
        Files.createDirectories(destination);
        Files.createSymbolicLink(destination.resolve("sub"), outside);

        Path pack = packOf(temp, "overrides/sub/inside.txt", "planted");

        assertThrows(DshException.class,
                () -> DshPackInstaller.land(pack, destination, null,
                        EnumSet.of(DshPackInstaller.Part.OVERRIDES)),
                "a member under a link that leaves the destination is refused");
    }

    /// The home half of the same check.
    ///
    /// `home/` lands at the `$DSH_HOME` root rather than in the profile, and it is the second place
    /// the import order reaches last — so the same member under a link has to be refused there too.
    ///
    /// @param temp where the home and the pack are built
    /// @throws Exception when the fixture cannot be built
    @Test
    void aHomeMemberUnderAnExistingLinkLeavesNothingOutside(@TempDir Path temp) throws Exception {
        Path outside = Files.createTempDirectory("dshpack-outside");
        Path home = temp.resolve("home");
        Files.createDirectories(home);
        Files.createSymbolicLink(home.resolve("skills"), outside);

        Path pack = packOf(temp, "home/skills/mine/SKILL.md", "planted");

        try {
            DshPackInstaller.land(pack, temp.resolve("profile"), home,
                    EnumSet.of(DshPackInstaller.Part.HOME));
        } catch (DshException refused) {
            // Either answer is fine; writing through the link is not.
        }

        assertFalse(Files.exists(outside.resolve("mine/SKILL.md")),
                "the home member was written through the link, outside the home");
    }

    /// What the check must not cost: nested directories, and a step that stays inside.
    ///
    /// `a/b/c` is the ordinary case, and `a/../d` is the same file spelled with a step — the lexical
    /// half of the check is what answers that one, since `..` on the *name* is the attacker-controlled
    /// half and no file exists for a real-path check to look at.
    ///
    /// @param temp where the profile and the pack are built
    /// @throws Exception when the fixture cannot be built
    @Test
    void nestedDirectoriesAndAStepInsideTheRootStillLand(@TempDir Path temp) throws Exception {
        Path destination = temp.resolve("profile");
        Files.createDirectories(destination);

        Path pack = temp.resolve("pack.dspack");
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(pack))) {
            zip.putNextEntry(new ZipEntry("overrides/a/b/c.txt"));
            zip.write("nested".getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
            zip.putNextEntry(new ZipEntry("overrides/a/../d.txt"));
            zip.write("step inside".getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }

        DshPackInstaller.land(pack, destination, null, EnumSet.of(DshPackInstaller.Part.OVERRIDES));

        assertTrue(Files.isRegularFile(destination.resolve("a/b/c.txt")),
                "a member in nested directories still lands");
        assertTrue(Files.isRegularFile(destination.resolve("d.txt")),
                "and one that steps back inside the root still lands");
    }

    /// The lexical escape is still refused, which is the half that is about the name.
    ///
    /// @param temp where the profile and the pack are built
    /// @throws Exception when the fixture cannot be built
    @Test
    void aNameThatClimbsOutOfTheRootIsStillRefused(@TempDir Path temp) throws Exception {
        Path destination = temp.resolve("profile");
        Files.createDirectories(destination);

        Path pack = packOf(temp, "overrides/../../PWNED.txt", "planted");

        assertThrows(DshException.class,
                () -> DshPackInstaller.land(pack, destination, null,
                        EnumSet.of(DshPackInstaller.Part.OVERRIDES)));
        assertFalse(Files.exists(temp.resolve("PWNED.txt")),
                "a name that climbs out of the root is not written above it");
    }

    /// A file the export side refuses and the import side must not, which is the other direction.
    ///
    /// The two sides are not one list, and the shape of the difference is what keeps a pack
    /// installable: `*token*` in a file name is a credential to publish and ordinary prose inside a
    /// package — `tokenizer.js` is a plugin's own file — so the export's token, api-key and
    /// certificate shapes stay the export's. A reconciliation that made the import side as strict as
    /// the export side would refuse packs that are perfectly ordinary.
    ///
    /// @param temp where the profile and the pack are built
    /// @throws Exception when the fixture cannot be built
    @Test
    void aFileOnlyTheExportSideRefusesStillLands(@TempDir Path temp) throws Exception {
        Path destination = Files.createDirectories(temp.resolve("profile"));
        Path pack = packOf(temp, "overrides/tokenizer.js", "module.exports = 1;\n");

        assertNotNull(DshPackForge.exclusionOf("tokenizer.js"),
                "the export side refuses a name with `token` in it");

        DshPackInstaller.land(pack, destination, null, EnumSet.of(DshPackInstaller.Part.OVERRIDES));

        assertTrue(Files.isRegularFile(destination.resolve("tokenizer.js")),
                "and the import side does not, because that is how a plugin names its own file");
    }

    /// Builds a one-member pack.
    ///
    /// @param directory where the pack is written
    /// @param member    the member's name
    /// @param content   what it holds
    /// @return the pack
    /// @throws Exception when the pack cannot be written
    private static Path packOf(Path directory, String member, String content) throws Exception {
        Path pack = directory.resolve("pack-" + member.replace('/', '-') + ".dspack");
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(pack))) {
            zip.putNextEntry(new ZipEntry(member));
            zip.write(content.getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }
        return pack;
    }
}