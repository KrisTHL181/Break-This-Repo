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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The release-directory bookkeeping of the brand installer: enumeration,
/// newest-first ordering, current-link resolution — the parts that do not
/// need a real pnpm run (that half is covered by the end-to-end run).
class BrandInstallerTest {

    @TempDir
    Path root;

    @Test
    void releasesAreNewestFirstAndFlagTheCurrentLink() throws IOException {
        install(Brand.KIMI, "2.0.2");
        Path current = install(Brand.KIMI, "2.1.1");
        install(Brand.KIMI, "2.1.0");
        // Through the same call the installer makes, not a bare `createSymbolicLink`: a
        // symlink needs a privilege an ordinary Windows account does not have, and the
        // production code writes a pointer file there instead. Setting the fixture up with
        // the production call is what keeps this test about the ordering rather than about
        // which platform it ran on.
        org.jackhuang.hmcl.util.io.Links.pointAt(root.resolve("current"), current);

        List<BrandInstaller.Release> releases = BrandInstaller.releases(Brand.KIMI, root);

        assertEquals(List.of("2.1.1", "2.1.0", "2.0.2"),
                releases.stream().map(BrandInstaller.Release::version).toList());
        assertTrue(releases.get(0).current());
        assertTrue(!releases.get(1).current());
    }

    @Test
    void numericOrderingAppliesToReleasesToo() throws IOException {
        install(Brand.OPENCODE, "1.9.0");
        install(Brand.OPENCODE, "1.18.0");

        List<BrandInstaller.Release> releases = BrandInstaller.releases(Brand.OPENCODE, root);

        assertEquals(List.of("1.18.0", "1.9.0"),
                releases.stream().map(BrandInstaller.Release::version).toList());
    }

    @Test
    void packageDirExistsOnlyForInstalledVersions() throws IOException {
        install(Brand.KIMI, "2.1.1");

        assertTrue(BrandInstaller.packageDir(Brand.KIMI, root, "2.1.1") != null);
        assertNull(BrandInstaller.packageDir(Brand.KIMI, root, "2.1.0"));
    }

    /// Git brands prove their install differently: no executable, but the
    /// built web app must be there.
    @Test
    void gitBrandMarkerIsTheBuiltWebApp() throws IOException {
        install(Brand.CLAUDE_WEB, "0.2.1");

        assertTrue(BrandInstaller.packageDir(Brand.CLAUDE_WEB, root, "0.2.1") != null);
        assertNull(BrandInstaller.packageDir(Brand.CLAUDE_WEB, root, "0.2.0"));
    }

    /// The source zip GitHub serves has everything under one `<repo>-<tag>/`
    /// root; unpacking drops that prefix so the release directory IS the
    /// project root.
    @Test
    void sourceZipUnpacksWithoutItsRootFolder() throws IOException, BrandException {
        byte[] zip = zipOf("claude-web-0.2.1/package.json", "{}",
                "claude-web-0.2.1/server/src/index.ts", "// entry",
                "claude-web-0.2.1/web/dist/index.html", "<html>");
        Path target = root.resolve("zip-target");

        BrandInstaller.extractZip(new java.io.ByteArrayInputStream(zip), target);

        assertTrue(Files.exists(target.resolve("package.json")));
        assertTrue(Files.exists(target.resolve("server/src/index.ts")));
        assertTrue(Files.exists(target.resolve("web/dist/index.html")));
        assertTrue(!Files.exists(target.resolve("claude-web-0.2.1")));
        assertEquals("{}", Files.readString(target.resolve("package.json")));
    }

    @Test
    void sourceZipRefusesEntriesThatEscapeTheTarget() throws IOException {
        byte[] zip = zipOf("claude-web-0.2.1/../../escaped.txt", "gotcha");
        Path target = root.resolve("zip-target");

        assertThrows(BrandException.class,
                () -> BrandInstaller.extractZip(new java.io.ByteArrayInputStream(zip), target));
    }

    /// A minimal zip holding the given path/content pairs.
    private static byte[] zipOf(String... pathsAndContents) throws IOException {
        java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
        try (java.util.zip.ZipOutputStream zip = new java.util.zip.ZipOutputStream(bytes)) {
            for (int i = 0; i < pathsAndContents.length; i += 2) {
                zip.putNextEntry(new java.util.zip.ZipEntry(pathsAndContents[i]));
                zip.write(pathsAndContents[i + 1].getBytes(java.nio.charset.StandardCharsets.UTF_8));
                zip.closeEntry();
            }
        }
        return bytes.toByteArray();
    }

    @Test
    void defaultPackageIsTheCurrentLinkElseTheNewest() throws IOException {
        assertNull(BrandInstaller.defaultPackage(Brand.KIMI, root));

        Path older = install(Brand.KIMI, "2.0.2");
        Path newest = install(Brand.KIMI, "2.1.1");
        // No link yet: the newest release is the default.
        assertEquals(newest.toRealPath(), BrandInstaller.defaultPackage(Brand.KIMI, root).toRealPath());

        // The pointer wins when it exists, even pointing at an older release.
        org.jackhuang.hmcl.util.io.Links.pointAt(root.resolve("current"), older);
        assertEquals(older.toRealPath(), BrandInstaller.defaultPackage(Brand.KIMI, root).toRealPath());
    }

    /// Creates one fake installed release: a directory holding only the marker
    /// the installer checks for — the executable (npm brands) or the built web
    /// app (git brands).
    private Path install(Brand brand, String version) throws IOException {
        Path release = Files.createDirectories(root.resolve("releases").resolve(version));
        Path marker = release.resolve(brand.installMarker());
        Files.createDirectories(marker.getParent());
        Files.createFile(marker);
        return release;
    }
}
