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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// What "copy this instance" is made of.
///
/// A duplicate is written by describing the original as a pack and installing that pack into the
/// copy, so "does the copy have the plugins" and "does the copy have the sessions" are answered by
/// what a pack carries and refuses to carry. Installing a pack needs a package manager and a
/// registry, which a test cannot assume; *describing* one needs neither, and that is the half where
/// the decision lives — a copy that was made empty was made empty here.
class DshInstanceDuplicateTest {
    /// The instance a copy is made from.
    private static final String SOURCE_ID = "duplicate-source";

    /// The instance a copy is made into.
    private static final String TARGET_ID = "duplicate-target";

    /// The workspace slug the fixture session lives under.
    private static final String SLUG = "-home-user-project";

    /// Removes the instances the tests made.
    @AfterEach
    void removeInstances() {
        for (String id : List.of(SOURCE_ID, TARGET_ID)) {
            try {
                if (DshInstanceManager.find(id) != null) {
                    DshInstanceManager.delete(id);
                }
            } catch (Exception ignored) {
                // The test's own cleanup: a failure here would hide the real one.
            }
        }
    }

    @Test
    void aCopyIsTheConfigurationAndNotTheSessions() throws Exception {
        DshInstance source = makeInstance(SOURCE_ID);
        writeProfile(source, """
                {
                  "name": "dsh-profile-web",
                  "private": true,
                  "dependencies": {"dsh-context": "0.54.2", "dshmarket": "1.52.0"},
                  "dsh": {"profile": {"bundles": ["dsh-context", "dshmarket"]}}
                }
                """);
        Files.writeString(profileDirectory(source).resolve("cordis.patch.yml"), "- id: title\n");
        // A plugin's own settings, which is most of what "the same environment" means for it.
        Files.writeString(source.homeDirectory().resolve("settings.yaml"), """
                dshmarket:
                  registry: https://example.invalid
                """);
        Path skills = DshSkills.directory(source.homeDirectory());
        Files.createDirectories(skills.resolve("alpha"));
        Files.writeString(skills.resolve("alpha").resolve("SKILL.md"),
                "---\nname: alpha\ndescription: First\n---\n");
        writeSession(source.homeDirectory());
        assertFalse(DshSessions.list(source.homeDirectory()).isEmpty(),
                "the source has something to leave behind");

        Path pack = Files.createTempFile("duplicate", DshModpacks.FILE_EXTENSION);
        try {
            // Exactly what a duplicate writes: `export(instance, target, report)` is `Options.of(instance)`.
            DshModpacks.export(source, pack, null);
            DshModpacks.Manifest manifest = DshModpacks.readManifest(pack);

            assertEquals(List.of("dsh-context", "dshmarket"), manifest.bundles(),
                    "the load order the original boots in is the order the copy boots in");
            assertTrue(manifest.hasPatch(), "the patch layer is composition, so the copy gets it");
            assertEquals(List.of("alpha"), manifest.skills(), "the skill packs travel");
            assertEquals(List.of("dshmarket"), manifest.settings(),
                    "a plugin's own settings travel, or the copy is not the same environment");
            assertEquals(0, manifest.sessionCount(), "session history stays behind");
            try (ZipFile zip = new ZipFile(pack.toFile())) {
                assertTrue(zip.getEntry("cordis.patch.yml") != null,
                        "and the patch layer is really in the archive");
            }
        } finally {
            Files.deleteIfExists(pack);
        }
    }

    @Test
    void anIdTakenByAnInstanceThatIsWholeIsRefused() throws Exception {
        // What must not happen: a duplicate quietly adopting an instance that is already somebody's,
        // and installing over it. Only a half-made copy — this call's own earlier attempt, kept
        // because pnpm asked about an install script — is continued.
        makeInstance(SOURCE_ID);
        DshInstance taken = makeInstance(TARGET_ID);
        Path entry = taken.dshEntryPoint();
        Files.createDirectories(entry.getParent());
        Files.writeString(entry, "// the harness that was already there\n");

        DshException refused = assertThrows(DshException.class,
                () -> DshInstanceManager.duplicate(SOURCE_ID, TARGET_ID, null));

        assertTrue(refused.getMessage().contains("already exists"), refused.getMessage());
        assertEquals("// the harness that was already there\n", Files.readString(entry),
                "and nothing was written over it");
    }

    @Test
    void copyingAnInstanceOntoItselfIsRefusedAndLeavesItAlone() throws Exception {
        // The name of the copy is the name of the source, so the copy is installed over the original:
        // `find(normalized)` answers with the source itself, and every step after that treats the
        // original as this call's own earlier attempt — including the cleanup a failed install runs,
        // which is what would take the source's files away. Refused before any of that is decided.
        DshInstance source = makeInstance(SOURCE_ID);
        Path entry = source.dshEntryPoint();
        Files.createDirectories(entry.getParent());
        Files.writeString(entry, "// the harness that was already there\n");
        Path document = Files.writeString(source.instanceDirectory().resolve("notes.txt"),
                "a file that belongs to the source");

        DshException refused = assertThrows(DshException.class,
                () -> DshInstanceManager.duplicate(SOURCE_ID, SOURCE_ID, null));

        // The refusal has to be **this** refusal. Without the guard the call still fails here — there is
        // no package manager to install the copy with — so the assertions below hold either way and it is
        // this one that says which failure happened. What follows pins what must not be true whatever
        // the refusal was: the source is still where it was, with what was in it.
        assertTrue(refused.getMessage().contains("onto itself"),
                "a copy of an instance onto itself is refused as that: " + refused.getMessage());
        assertTrue(Files.isRegularFile(document), "the source is left where it is, with what is in it");
        assertEquals("// the harness that was already there\n", Files.readString(entry),
                "and its harness is untouched");
        assertTrue(DshInstanceManager.find(SOURCE_ID) != null, "the source is still an instance");
    }

    /// Creates an instance whose home the tests can fill.
    ///
    /// @param id the instance id
    /// @return the instance
    private static DshInstance makeInstance(String id) throws Exception {
        Path workspace = Files.createTempDirectory("duplicate-home");
        return DshInstanceManager.create(id, "0.1.6-alpha.2", DshInstance.DEFAULT_PROFILE,
                workspace, DshHomeMode.ISOLATED, null, List.of(), Map.of());
    }

    /// Returns an instance's profile directory.
    ///
    /// @param instance the instance
    /// @return the directory
    private static Path profileDirectory(DshInstance instance) throws Exception {
        return instance.homeDirectory().resolve("profiles").resolve(instance.profile());
    }

    /// Writes a profile manifest.
    ///
    /// @param instance the instance
    /// @param body     the manifest
    private static void writeProfile(DshInstance instance, String body) throws Exception {
        Path directory = profileDirectory(instance);
        Files.createDirectories(directory);
        Files.writeString(directory.resolve("package.json"), body);
    }

    /// Writes a session into a home, in the shape the harness keeps them in.
    ///
    /// @param home the `DSH_HOME`
    private static void writeSession(Path home) throws Exception {
        Path directory = home.resolve("sessions").resolve(SLUG).resolve("session-1");
        Files.createDirectories(directory);
        Files.writeString(directory.resolve("session.v4.jsonl.zstd"), "fixture-log");
        Files.writeString(directory.resolve("session.lock"), "");
    }
}
