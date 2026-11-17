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
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Verifies what the launcher reads out of the package manager's output.
///
/// One failure is worth translating rather than relaying: a plugin fetched from a repository is
/// built by its `prepare` script, and pnpm refuses to run it until the profile allows that,
/// naming the exact entry it needs. Reading that entry is what lets the launcher ask about it
/// like any other build script — and reading it wrongly would ask about a package that does not
/// exist, so the shape is pinned here against the output pnpm really prints.
class DshPluginInstallerTest {
    /// The entry pnpm named in a real refusal, printed as one line.
    private static final String NAMED =
            "  dshmarket@https://codeload.github.com/dsh-market/dsh-market/tar.gz/180c3144da8eb4229cacb843aced229ea55912fc: true";

    @Test
    void theEntryPnpmNamesForARepositoryHostedPackageIsRead() {
        List<String> output = List.of(
                "[ERR_PNPM_GIT_DEP_PREPARE_NOT_ALLOWED] Failed to prepare git-hosted package fetched from"
                        + " \"https://codeload.github.com/dsh-market/dsh-market/tar.gz/180c3144da8eb4229cacb843aced229ea55912fc\":"
                        + " The git-hosted package \"dshmarket@1.66.2\" needs to execute build scripts but is not in the"
                        + " \"allowBuilds\" allowlist.",
                "",
                "This error happened while installing a direct dependency of /tmp/profile",
                "",
                "Add the package to \"allowBuilds\" in your project's pnpm-workspace.yaml to allow it to run"
                        + " scripts. For example:",
                "allowBuilds:",
                NAMED);

        assertEquals(List.of("dshmarket@https://codeload.github.com/dsh-market/dsh-market/tar.gz/"
                        + "180c3144da8eb4229cacb843aced229ea55912fc"),
                DshPluginInstaller.gitBuildKeys(output));
    }

    @Test
    void aDifferentFailureNamesNothing() {
        assertEquals(List.of(), DshPluginInstaller.gitBuildKeys(List.of(
                        " ERR_PNPM_NO_MATCHING_VERSION  No matching version found for dsh-thing@9.9.9")),
                "a package that does not exist is not a package waiting to be allowed to build");

        assertEquals(List.of(), DshPluginInstaller.gitBuildKeys(List.of(
                        "allowBuilds:",
                        "  node-pty: set this to true or false")),
                "the placeholder pnpm writes itself is a different flow, and is not read as one of these");
    }

    /// The profile a terminal plugin may not go into is refused by the installer itself,
    /// so every route that installs a package is covered by one rule — and the refusal
    /// comes before the runtime is resolved, so being told "not into the web profile" does
    /// not depend on node and pnpm being there.
    @Test
    void aTerminalPluginCannotBeInstalledIntoTheWebProfile(@TempDir Path home) {
        DshInstance instance = new DshInstance("installer-test", "0.1.7-rc.1", "web",
                home.resolve("workspace").toString(), null, DshHomeMode.CUSTOM, home.toString(),
                List.of(), Map.of(), null, null, null, 0, 0L);

        DshException refused = assertThrows(DshException.class, () -> DshPluginInstaller.installSpecs(
                instance, "web", List.of("@xmoon76/dsh-pi-tui@0.5.3"), null));
        assertTrue(refused.getMessage().contains("不能装进 web profile"), refused.getMessage());

        // The plugin's own profile is not refused here; that it fails later (a runtime, or
        // an instance with no harness of its own) is what says the guard did not fire.
        DshException later = assertThrows(DshException.class, () -> DshPluginInstaller.installSpecs(
                instance, "pi-tui", List.of("@xmoon76/dsh-pi-tui@0.5.3"), null));
        assertFalse(later.getMessage().contains("不能装进"), later.getMessage());
    }

    /// A manifest cannot say a package is there when nothing is, and that gap is the whole
    /// reason the launcher asks the disk as well: the dependency is written before the fetch
    /// happens, so a run that could not fetch leaves a profile claiming a plugin it does not
    /// have.
    @Test
    void aDeclaredPackageIsOnlyThereWhenItsFilesAre(@TempDir Path home) throws Exception {
        assertFalse(DshPluginInstaller.holdsPackage(home, "tui", "@tomowang/dsh-tui"),
                "nothing on disk yet");

        // What pnpm leaves: the files in the store, and a link from node_modules into it.
        Path store = home.resolve("profiles/tui/node_modules/.pnpm/"
                + "@tomowang+dsh-tui@0.10.2/node_modules/@tomowang/dsh-tui");
        Files.createDirectories(store);
        Path nodeModules = DshPluginInstaller.profileNodeModules(home, "tui");
        Files.createDirectories(nodeModules.resolve("@tomowang"));
        Files.createSymbolicLink(nodeModules.resolve("@tomowang/dsh-tui"),
                Path.of("../.pnpm/@tomowang+dsh-tui@0.10.2/node_modules/@tomowang/dsh-tui"));
        assertTrue(DshPluginInstaller.holdsPackage(home, "tui", "@tomowang/dsh-tui"),
                "the link pnpm writes counts; the question is whether the files behind it are there");

        // The same link in a profile with nothing behind it: what a fetch that never
        // happened leaves, and the state that used to be started as if it were installed.
        Path broken = DshPluginInstaller.profileNodeModules(home, "broken");
        Files.createDirectories(broken.resolve("@tomowang"));
        Files.createSymbolicLink(broken.resolve("@tomowang/dsh-tui"),
                Path.of("../.pnpm/@tomowang+dsh-tui@0.10.2/node_modules/@tomowang/dsh-tui"));
        assertFalse(DshPluginInstaller.holdsPackage(home, "broken", "@tomowang/dsh-tui"),
                "a dangling link is the half-installed state, not an install");
    }

    /// A run that reported success without leaving the package behind is not a successful
    /// install, and saying so at install time is what keeps the failure out of the terminal:
    /// the alternative is a profile the panel calls installed, booted into an instance that
    /// dies without printing anything.
    @Test
    void aRunThatLeftNothingBehindIsNotAnInstall(@TempDir Path home) {
        DshException failed = assertThrows(DshException.class,
                () -> DshPluginInstaller.requireLanded(home, "tui", "@tomowang/dsh-tui@0.10.2"));
        assertTrue(failed.getMessage().contains("@tomowang/dsh-tui"), failed.getMessage());
        assertTrue(failed.getMessage().contains("node_modules"), failed.getMessage());
    }

    /// Specs whose name cannot be read from the text are not verified this way: a false
    /// failure there would break a working local install.
    @Test
    void aSpecThatIsNotARegistryPackageIsNotVerifiedByName(@TempDir Path home) {
        for (String spec : List.of("/tmp/thing.tgz", "./local", "file:/tmp/thing.tgz",
                "link:../plugin", "github:owner/repo", "https://example.invalid/pack.tgz")) {
            assertFalse(DshPluginInstaller.isRegistrySpec(spec), spec);
            assertDoesNotThrow(() -> DshPluginInstaller.requireLanded(home, "tui", spec), spec);
        }
        for (String spec : List.of("left-pad", "left-pad@1.0.0", "@tomowang/dsh-tui",
                "@tomowang/dsh-tui@0.10.2")) {
            assertTrue(DshPluginInstaller.isRegistrySpec(spec), spec);
        }
    }
}
