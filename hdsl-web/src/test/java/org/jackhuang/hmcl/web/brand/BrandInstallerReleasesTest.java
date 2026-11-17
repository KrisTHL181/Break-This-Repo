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
package org.jackhuang.hmcl.web.brand;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Pins what counts as an installed Octop release and how a killed source
/// build is repaired.
///
/// The audit reproduced the failure this guards against: a panel killed
/// mid-build left `<version>` without the install marker next to a complete
/// `<version>.previous`, and the release list offered the `.previous` name as
/// if it were installable. New instances were then pinned to a version that
/// could never be installed.
class BrandInstallerReleasesTest {

    @TempDir
    Path brandRoot;

    private Path releasesDir() throws IOException {
        Path dir = brandRoot.resolve(BrandInstaller.RELEASES_DIR);
        Files.createDirectories(dir);
        return dir;
    }

    private Path release(String name, boolean complete) throws IOException {
        Path dir = releasesDir().resolve(name);
        Files.createDirectories(dir);
        if (complete) {
            Files.writeString(dir.resolve("octop-install.json"),
                    "{\"mode\":\"package\",\"version\":\"" + name + "\"}\n", StandardCharsets.UTF_8);
        }
        return dir;
    }

    @Test
    void onlyCompleteReleasesAreListed() throws IOException {
        release("1.0.3b1", true);
        release("2.0.0", false);                       // half-installed: no marker
        release("1.0.3b1" + BrandInstaller.PREVIOUS_SUFFIX, true);  // staging name

        List<BrandInstaller.Release> releases = BrandInstaller.releases(Brand.OCTOP, brandRoot);
        assertEquals(1, releases.size(), releases.toString());
        assertEquals("1.0.3b1", releases.get(0).version());
    }

    @Test
    void reconcileRestoresTheStagedReleaseWhenTheNewOneIsIncomplete() throws IOException {
        Path staged = release("1.0.3b1" + BrandInstaller.PREVIOUS_SUFFIX, true);
        release("1.0.3b1", false);                     // the build that died

        BrandInstaller.reconcileOctopReleases(brandRoot);

        Path target = releasesDir().resolve("1.0.3b1");
        assertTrue(Files.isRegularFile(target.resolve("octop-install.json")),
                "the complete copy is back under its real name");
        assertFalse(Files.exists(staged), "the staging directory is gone");
        // The half-built tree is kept for diagnosis, not silently deleted.
        try (var entries = Files.list(releasesDir())) {
            assertTrue(entries.anyMatch(path -> path.getFileName().toString().startsWith("1.0.3b1.broken-")),
                    "the broken tree is moved aside");
        }
        assertEquals("1.0.3b1", BrandInstaller.releases(Brand.OCTOP, brandRoot).get(0).version());
    }

    @Test
    void reconcileLeavesAFinishedReleaseAlone() throws IOException {
        Path target = release("1.0.3b1", true);
        Path staged = release("1.0.3b1" + BrandInstaller.PREVIOUS_SUFFIX, true);

        BrandInstaller.reconcileOctopReleases(brandRoot);

        assertTrue(Files.isRegularFile(target.resolve("octop-install.json")), "the finished release stays");
        assertTrue(Files.exists(staged), "a stale staging copy is not this method's to delete");
    }

    @Test
    void reconcileDoesNotRestoreAnIncompleteStagingCopy() throws IOException {
        // Two interrupted builds in a row: `.previous` is broken too. Renaming it
        // back would just put a half-installed tree under the real name.
        Path staged = release("1.0.3b1" + BrandInstaller.PREVIOUS_SUFFIX, false);
        release("1.0.3b1", false);

        BrandInstaller.reconcileOctopReleases(brandRoot);

        assertTrue(Files.exists(staged), "the staged copy stays where it is");
        assertFalse(Files.exists(releasesDir().resolve("1.0.3b1").resolve("octop-install.json")),
                "nothing claims to be installed");
        assertTrue(BrandInstaller.releases(Brand.OCTOP, brandRoot).isEmpty());
    }

    @Test
    void onlyTheNewestBrokenCopyIsKept() throws IOException {
        Path old = release("1.0.3b1.broken-1", true);
        release("1.0.3b1" + BrandInstaller.PREVIOUS_SUFFIX, true);
        release("1.0.3b1", false);

        BrandInstaller.reconcileOctopReleases(brandRoot);

        assertFalse(Files.exists(old), "the previous diagnostic copy is pruned");
        try (var entries = Files.list(releasesDir())) {
            long broken = entries
                    .filter(Files::isDirectory)
                    .filter(path -> path.getFileName().toString().startsWith("1.0.3b1.broken-"))
                    .count();
            assertEquals(1, broken, "exactly one broken tree is kept");
        }
    }

    @Test
    void reconcileOnAnAbsentBrandDirectoryIsANoOp() {
        // First boot: nothing installed yet, and the boot path must not create
        // anything or throw.
        BrandInstaller.reconcileOctopReleases(brandRoot.resolve("nothing-here"));
        assertFalse(Files.exists(brandRoot.resolve("nothing-here")));
    }
}
