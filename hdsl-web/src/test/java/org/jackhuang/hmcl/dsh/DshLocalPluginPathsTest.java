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

import org.jackhuang.hmcl.util.io.FileUtils;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Verifies that renaming an instance moves the paths it recorded for its local plugins.
///
/// This pins the reported defect: a plugin installed from a file is recorded in the profile
/// as an absolute `file:` specification pointing inside the instance, and every later
/// operation on the profile resolves that path again — so after a rename the instance could
/// not install a plugin, remove one, or repair itself, because the package manager stopped
/// at a tarball that was no longer where the profile said it was. The installation itself was
/// intact; only the name of the directory written down against it had changed.
class DshLocalPluginPathsTest {
    /// A local plugin, as the profile records one: the file the instance holds.
    private static final String INSIDE = "dsh-example";

    /// A plugin installed from a file that is not part of the instance.
    private static final String OUTSIDE = "dsh-elsewhere";

    /// A plugin installed from the registry, which has no path to move.
    private static final String REGISTRY = "dsh-registry";

    /// Where the file outside the instance lives. Nothing creates it: what matters is that
    /// the rewrite leaves a specification it does not own exactly as it found it.
    private static final Path ELSEWHERE = Path.of("/opt/plugins/dsh-elsewhere-9.9.9.tgz");

    /// Renames an instance whose home travels with it.
    @Test
    void aPluginTheInstanceHoldsFollowsItsRename() throws Exception {
        String id = newId("isolated");
        String renamed = id + "-renamed";
        try {
            DshInstance instance = DshInstanceManager.create(id, "1.0.0", DshInstance.DEFAULT_PROFILE,
                    Path.of(System.getProperty("java.io.tmpdir")), DshHomeMode.ISOLATED, null,
                    List.of(), Map.of());
            Path oldDirectory = instance.instanceDirectory();
            Path newDirectory = DshPaths.instanceDirectory(renamed);

            String manifest = installLocalPlugin(instance, oldDirectory);
            Path profile = profileOf(instance);
            Files.writeString(profile.resolve("pnpm-lock.yaml"), lockfileText(oldDirectory));
            Path mirror = profile.resolve("node_modules/.pnpm/lock.yaml");
            Files.createDirectories(mirror.getParent());
            Files.writeString(mirror, lockfileText(oldDirectory));

            Path movedProfile = profileOf(DshInstanceManager.rename(id, renamed));

            assertEquals(expected(manifest, oldDirectory, newDirectory),
                    Files.readString(movedProfile.resolve("package.json")),
                    "the manifest is moved as text: everything but the path comes back unchanged");
            assertEquals(expected(lockfileText(oldDirectory), oldDirectory, newDirectory),
                    Files.readString(movedProfile.resolve("pnpm-lock.yaml")),
                    "the lockfile's specification is the same path, so it moves with it");
            assertEquals(expected(lockfileText(oldDirectory), oldDirectory, newDirectory),
                    Files.readString(movedProfile.resolve("node_modules/.pnpm/lock.yaml")),
                    "the package manager's copy of the lockfile names the path too, until it next runs");

            assertTrue(Files.isRegularFile(newDirectory.resolve("plugins/" + INSIDE + "-1.0.0.tgz")),
                    "the file itself moves with the instance");
            assertResolves(movedProfile.resolve("package.json"), newDirectory);
        } finally {
            remove(id, renamed);
        }
    }

    /// Renames an instance whose home is somebody else's directory.
    ///
    /// The home does not move — but it recorded the instance's own files, so what it recorded
    /// has to move even though nothing around it did.
    @Test
    void aHomeThatStaysPutStillLearnsWhereThePluginWent() throws Exception {
        String id = newId("custom");
        String renamed = id + "-renamed";
        Path home = Files.createTempDirectory("test-local-paths-home-");
        try {
            DshInstance instance = DshInstanceManager.create(id, "1.0.0", DshInstance.DEFAULT_PROFILE,
                    Path.of(System.getProperty("java.io.tmpdir")), DshHomeMode.CUSTOM, home,
                    List.of(), Map.of());
            Path oldDirectory = instance.instanceDirectory();
            Path newDirectory = DshPaths.instanceDirectory(renamed);

            String recorded = installLocalPlugin(instance, oldDirectory);
            DshInstance after = DshInstanceManager.rename(id, renamed);

            Path manifest = home.resolve("profiles").resolve(after.profile()).resolve("package.json");
            assertEquals(expected(recorded, oldDirectory, newDirectory), Files.readString(manifest),
                    "a chosen home records the instance's own files just the same, so what it recorded moves");
            assertResolves(manifest, newDirectory);
        } finally {
            remove(id, renamed);
            FileUtils.deleteDirectoryQuietly(home);
        }
    }

    /// Leaves a profile that names nothing inside the instance completely alone.
    @Test
    void aProfileWithNoLocalPluginIsNotTouched() throws Exception {
        String id = newId("untouched");
        String renamed = id + "-renamed";
        try {
            DshInstance instance = DshInstanceManager.create(id, "1.0.0", DshInstance.DEFAULT_PROFILE,
                    Path.of(System.getProperty("java.io.tmpdir")), DshHomeMode.ISOLATED, null,
                    List.of(), Map.of());
            Path profile = profileOf(instance);
            Files.createDirectories(profile);
            Path manifest = profile.resolve("package.json");
            Files.writeString(manifest, """
                    {
                      "name": "dsh-profile-web",
                      "dependencies": {
                        "%s": "1.2.3"
                      }
                    }
                    """.formatted(REGISTRY));
            var written = Files.getLastModifiedTime(manifest);

            DshInstance after = DshInstanceManager.rename(id, renamed);

            assertEquals(written, Files.getLastModifiedTime(profileOf(after).resolve("package.json")),
                    "a record with no path in it is not a record to rewrite");
        } finally {
            remove(id, renamed);
        }
    }

    /// Names the packages whose records moved, and only those.
    @Test
    void onlyThePluginsTheInstanceHoldsAreReported() throws Exception {
        String id = newId("reported");
        String renamed = id + "-renamed";
        try {
            DshInstance instance = DshInstanceManager.create(id, "1.0.0", DshInstance.DEFAULT_PROFILE,
                    Path.of(System.getProperty("java.io.tmpdir")), DshHomeMode.ISOLATED, null,
                    List.of(), Map.of());
            Path oldDirectory = instance.instanceDirectory();
            installLocalPlugin(instance, oldDirectory);

            List<String> moved = DshLocalPluginPaths.relocate(instance, oldDirectory,
                    DshPaths.instanceDirectory(renamed));

            assertEquals(List.of(INSIDE), moved,
                    "the file outside the instance and the registry package are not this instance's to move");
        } finally {
            remove(id, renamed);
        }
    }

    /// Does not mistake another instance's directory for this one's.
    ///
    /// A recorded path is a path and not a string: an instance called `a` being renamed must
    /// not reach into a record that names `a-b`, for the same reason a package called `dsh-a`
    /// must not take `dsh-ab`'s files.
    @Test
    void aDirectoryThatMerelyBeginsWithTheOldNameIsLeftAlone() {
        String from = "/home/someone/.local/share/hdsl/instances/a";
        String to = "/home/someone/.local/share/hdsl/instances/a-renamed";

        assertEquals("file:" + to + "/plugins/x.tgz",
                DshLocalPluginPaths.rewrite("file:" + from + "/plugins/x.tgz", from, to),
                "a record that names the instance itself moves");
        assertEquals("file:" + from + "-b/plugins/x.tgz",
                DshLocalPluginPaths.rewrite("file:" + from + "-b/plugins/x.tgz", from, to),
                "a record that names a neighbouring instance does not");
        assertEquals("specifier: file:" + to + "\n",
                DshLocalPluginPaths.rewrite("specifier: file:" + from + "\n", from, to),
                "a record that names the directory and nothing in it moves too");
        assertEquals("", DshLocalPluginPaths.rewrite("", from, to));
    }

    /// Renders a path the way a package manager records one.
    ///
    /// Always with `/`, on every platform: a record goes into a JSON string, where a
    /// Windows `\` is an escape character — the fixture used to embed the platform's own
    /// separator, which on Windows produced a manifest that was not valid JSON at all, and
    /// the diff that names the moved plugins could not read it.
    ///
    /// @param path the path
    /// @return the path as a record spells it
    private static String asRecorded(Path path) {
        return path.toString().replace('\\', '/');
    }

    /// Writes the profile records a local installation leaves behind.
    ///
    /// @param instance     the instance
    /// @param oldDirectory the instance directory the records name
    /// @return the manifest that was written
    /// @throws Exception when the files cannot be written
    private static String installLocalPlugin(DshInstance instance, Path oldDirectory) throws Exception {
        Path profile = profileOf(instance);
        Path plugins = oldDirectory.resolve(DshLocalPlugins.DIRECTORY);
        Files.createDirectories(plugins);
        Files.writeString(plugins.resolve(INSIDE + "-1.0.0.tgz"), "not really a package");

        String manifest = """
                {
                  "name": "dsh-profile-web",
                  "private": true,
                  "dependencies": {
                    "%s": "file:%s",
                    "%s": "file:%s",
                    "%s": "1.2.3"
                  },
                  "dsh": {
                    "profile": {
                      "bundles": [
                        "%s",
                        "%s"
                      ]
                    }
                  }
                }
                """.formatted(INSIDE, asRecorded(plugins.resolve(INSIDE + "-1.0.0.tgz")),
                OUTSIDE, asRecorded(ELSEWHERE), REGISTRY, INSIDE, REGISTRY);
        Files.createDirectories(profile);
        Files.writeString(profile.resolve("package.json"), manifest);
        return manifest;
    }

    /// Returns the profile directory of an instance.
    ///
    /// @param instance the instance
    /// @return the directory holding its profile
    /// @throws Exception when the home cannot be resolved
    private static Path profileOf(DshInstance instance) throws Exception {
        return instance.homeDirectory().resolve("profiles").resolve(instance.profile());
    }

    /// Returns what a package manager records for a local installation.
    ///
    /// The specification is the path as it was given, which is what a rename invalidates. The
    /// resolution beside it is relative to the profile, which is why the rewrite has nothing
    /// to do about it.
    ///
    /// @param directory the instance directory
    /// @return the lockfile's text
    private static String lockfileText(Path directory) {
        return """
                lockfileVersion: '9.0'
                importers:
                  .:
                    dependencies:
                      %s:
                        specifier: file:%s
                        version: file:../../../plugins/%s-1.0.0.tgz
                      %s:
                        specifier: file:%s
                packages:
                  %s@file:../../../plugins/%s-1.0.0.tgz:
                    resolution: {tarball: file:../../../plugins/%s-1.0.0.tgz}
                """.formatted(INSIDE, asRecorded(directory.resolve("plugins/" + INSIDE + "-1.0.0.tgz")), INSIDE,
                OUTSIDE, asRecorded(ELSEWHERE),
                INSIDE, INSIDE, INSIDE);
    }

    /// Returns the text a rewrite is expected to produce.
    ///
    /// @param text the text as it was
    /// @param from the directory the paths were recorded against
    /// @param to   the directory they belong to now
    /// @return the text with those paths moved
    private static String expected(String text, Path from, Path to) {
        return text.replace(asRecorded(from), asRecorded(to));
    }

    /// Asserts that every path the manifest records inside the instance exists.
    ///
    /// This is the defect stated plainly: the profile has to keep naming a file that is there,
    /// because that is what the next plugin operation resolves.
    ///
    /// @param manifest  the profile manifest
    /// @param directory the instance directory whose paths are checked
    /// @throws Exception when it cannot be read
    private static void assertResolves(Path manifest, Path directory) throws Exception {
        String marker = "file:" + asRecorded(directory) + "/";
        int checked = 0;
        for (String line : Files.readAllLines(manifest, StandardCharsets.UTF_8)) {
            int at = line.indexOf(marker);
            if (at < 0) {
                continue;
            }
            String spec = line.substring(at + "file:".length()).replaceAll("[\",\\s]+$", "");
            assertTrue(Files.isRegularFile(Path.of(spec)),
                    "the profile has to keep naming a file that is there, not " + spec);
            checked++;
        }
        assertTrue(checked > 0, "the manifest has to keep naming the plugin the instance holds");
    }

    /// Removes the instances a test made, under every id it may have given them.
    ///
    /// @param ids the ids
    private static void remove(String... ids) {
        for (String id : ids) {
            try {
                DshInstanceManager.delete(id);
            } catch (Exception ignored) {
                // There was nothing under that id, which is what a rename leaves behind.
            }
        }
    }

    /// Returns an id no other test is using.
    ///
    /// @param what what the test is about
    /// @return the id
    private static String newId(String what) {
        return "test-local-paths-" + what + "-" + Long.toHexString(System.nanoTime());
    }
}
