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
import org.jetbrains.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Verifies that a plugin is switched off in the profile's patch layer, not in
/// the package.
///
/// Only the rows a package `insert`s are its own: a bundle patch also carries
/// rows that merely reconfigure a neighbour, and writing `disabled: true` onto
/// one of those takes the neighbour down with it. The patch file is the
/// person's, so a disable is an appended block and an enable removes exactly
/// that block — the template's empty-list placeholder goes back when the last
/// one leaves.
class DshPluginPatchTest {
    /// A home to build profiles in.
    @TempDir
    Path home;

    /// The profile every case installs into.
    private static final String PROFILE = "web";

    /// Writes a package with a bundle patch.
    ///
    /// @param name          the package name
    /// @param declaredPatch the path the manifest declares, or `null`
    /// @param patchText     the package's own patch
    private void writePackage(String name, @Nullable String declaredPatch, String patchText) throws Exception {
        Path directory = home.resolve("profiles").resolve(PROFILE).resolve("node_modules").resolve(name);
        Files.createDirectories(directory);
        String manifest = declaredPatch == null
                ? "{\"name\":\"" + name + "\"}"
                : "{\"name\":\"" + name + "\",\"dsh\":{\"bundle\":{\"patch\":\""
                        + declaredPatch + "\"}}}";
        Files.writeString(directory.resolve("package.json"), manifest);
        Files.writeString(directory.resolve("cordis.patch.yml"), patchText);
    }

    /// Writes the profile's own patch layer.
    ///
    /// @param text the patch text
    private void writeProfilePatch(String text) throws Exception {
        Path file = DshPluginPatch.patchFile(home, PROFILE);
        Files.createDirectories(file.getParent());
        Files.writeString(file, text);
    }

    @Test
    void onlyRowsUnderAnInsertBlockBelongToThePackage() {
        String text = """
                - insert:
                    - id: own-row
                      name: 'dsh-demo'
                - id: neighbour
                  disabled: true
                - id: tuned
                  config:
                    value: 1
                """;

        assertEquals(List.of("own-row"), DshPluginPatch.insertedIdsIn(text));
    }

    @Test
    void disablingAndEnablingRestoresTheTemplate() throws Exception {
        writePackage("dsh-demo", "./cordis.patch.yml", """
                - insert:
                    - id: demo
                      name: 'dsh-demo'
                      disabled: !!js "some expression"
                """);
        writeProfilePatch("# A header the person wrote\n[]\n");

        DshPluginPatch.setEnabled(home, PROFILE, "dsh-demo", false);

        String disabled = Files.readString(DshPluginPatch.patchFile(home, PROFILE));
        assertTrue(disabled.contains("- id: demo\n  disabled: true\n"), disabled);
        assertTrue(disabled.contains("# A header"), "the header survives");
        assertEquals(Set.of("demo"), DshPluginPatch.disabledIds(home, PROFILE));

        // Idempotent: a second disable is a no-op, not a second block.
        DshPluginPatch.setEnabled(home, PROFILE, "dsh-demo", false);
        assertEquals(1, Files.readString(DshPluginPatch.patchFile(home, PROFILE))
                .split("- id: demo", -1).length - 1);

        DshPluginPatch.setEnabled(home, PROFILE, "dsh-demo", true);

        assertEquals("# A header the person wrote\n[]\n",
                Files.readString(DshPluginPatch.patchFile(home, PROFILE)),
                "the placeholder goes back, or the profile would refuse to boot");
        assertTrue(DshPluginPatch.disabledIds(home, PROFILE).isEmpty());
    }

    @Test
    void aPackageWithoutADeclaredPatchIsStillRead() throws Exception {
        writePackage("dsh-conventional", null, """
                - insert:
                    - id: conventional
                """);

        assertEquals(List.of("conventional"), DshPluginPatch.insertedIds(home, PROFILE, "dsh-conventional"));
    }

    @Test
    void aForeignDisableRowIsNotClaimed() {
        String text = """
                - id: someone-elses
                  disabled: true
                """;

        assertTrue(DshPluginPatch.insertedIdsIn(text).isEmpty());
        assertFalse(DshPluginPatch.disabledIdsIn(text).isEmpty());
    }
}
