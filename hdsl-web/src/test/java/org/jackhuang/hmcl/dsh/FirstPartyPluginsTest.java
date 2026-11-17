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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.GZIPInputStream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The bundled-plugin index is what makes installing the panel's own terminal work
/// offline, so what is pinned here is that the resource exists in the built jar, that
/// its digest matches the tarball, and that the catalogue and the build agree on the
/// version — a drift between `package.json` and `DshTuiPlugins` would otherwise only
/// show up as a 404 on a user's install page.
class FirstPartyPluginsTest {

    @TempDir
    Path directory;

    @Test
    void thePanelShipsItsTerminal() {
        List<FirstPartyPlugins.Bundled> all = FirstPartyPlugins.all();
        assertFalse(all.isEmpty(), "the build must bundle at least the first-party terminal");
        FirstPartyPlugins.Bundled terminal = FirstPartyPlugins.find("@nas200-timmy/dsh-acp-tui").orElseThrow();
        assertEquals("@nas200-timmy/dsh-acp-tui", terminal.name());
        assertFalse(terminal.version().isBlank());
        assertTrue(terminal.file().endsWith(".tgz"), terminal.file());
        assertNotNull(terminal.sha256(), "the index must record a digest");
        assertEquals(64, terminal.sha256().length());
    }

    @Test
    void lookupIgnoresCaseAndRefusesStrangers() {
        assertTrue(FirstPartyPlugins.find("@NAS200-TIMMY/DSH-ACP-TUI").isPresent());
        assertTrue(FirstPartyPlugins.find("  @nas200-timmy/dsh-acp-tui  ").isPresent());
        assertTrue(FirstPartyPlugins.find("@unknown/plugin").isEmpty());
        assertTrue(FirstPartyPlugins.find(null).isEmpty());
        assertTrue(FirstPartyPlugins.find("").isEmpty());
        assertFalse(FirstPartyPlugins.isBundled("@unknown/plugin"));
    }

    /// The tarball the install route hands to pnpm has to be npm's layout: a `package/`
    /// directory with the manifest and the program inside it. A tarball without that
    /// prefix extracts to nothing pnpm recognises.
    @Test
    void theTarballIsAnNpmPackage() throws IOException, DshException {
        Path tarball = FirstPartyPlugins.materialize("@nas200-timmy/dsh-acp-tui", directory);
        assertTrue(Files.isRegularFile(tarball), tarball.toString());
        FirstPartyPlugins.Bundled bundled = FirstPartyPlugins.find("@nas200-timmy/dsh-acp-tui").orElseThrow();
        assertEquals(bundled.file(), tarball.getFileName().toString());
        assertEquals(bundled.sha256(), FirstPartyPlugins.sha256(Files.readAllBytes(tarball)));

        String tar;
        try (GZIPInputStream in = new GZIPInputStream(Files.newInputStream(tarball));
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            in.transferTo(out);
            tar = out.toString(StandardCharsets.UTF_8);
        }
        assertTrue(tar.contains("package/package.json"), "tarball must be rooted at package/");
        assertTrue(tar.contains("package/bin/dsh-acp-tui.js"), "the program must be inside the tarball");
        assertTrue(tar.contains("package/src/main.js"), "the sources must be inside the tarball");
    }

    @Test
    void materializingTwiceRewritesTheSameFile() throws IOException, DshException {
        Path first = FirstPartyPlugins.materialize("@nas200-timmy/dsh-acp-tui", directory);
        byte[] before = Files.readAllBytes(first);
        Path second = FirstPartyPlugins.materialize("@nas200-timmy/dsh-acp-tui", directory);
        assertArrayEquals(before, Files.readAllBytes(second));
    }

    @Test
    void anUnknownPluginIsRefused() {
        assertThrows(DshException.class,
                () -> FirstPartyPlugins.materialize("@unknown/plugin", directory));
    }

    /// A version number cannot say "this is the same build"; the tarball digest can.
    /// The panel used to answer "installed" and never offer newer files when a source
    /// change shipped under the same version.
    @Test
    void theInstallRecordTellsThisBuildsCopyFromAnOlderOne(@TempDir Path home) throws Exception {
        DshTuiPlugins.Entry entry = DshTuiPlugins.find("@nas200-timmy/dsh-inproc-tui").orElseThrow();
        String digest = FirstPartyPlugins.find(entry.packageName())
                .map(FirstPartyPlugins.Bundled::sha256)
                .orElse(null);
        assertNotNull(digest, "the build records a digest for every bundled plugin");

        // No record is not "up to date": it is an install from a panel that did not
        // write one, and reinstall is what makes the files match the jar.
        assertNull(FirstPartyPlugins.installedDigest(home, entry.packageName()));
        assertTrue(FirstPartyPlugins.isOutdated(home, entry.packageName()));

        FirstPartyPlugins.recordInstalled(home, entry.packageName(), digest);
        assertEquals(digest, FirstPartyPlugins.installedDigest(home, entry.packageName()));
        assertFalse(FirstPartyPlugins.isOutdated(home, entry.packageName()));

        FirstPartyPlugins.recordInstalled(home, entry.packageName(), "deadbeef");
        assertTrue(FirstPartyPlugins.isOutdated(home, entry.packageName()),
                "a different digest is a different build, whatever the version says");

        FirstPartyPlugins.recordInstalled(home, entry.packageName(), null);
        assertNull(FirstPartyPlugins.installedDigest(home, entry.packageName()));

        // The record belongs to one home; another instance is not told anything.
        Path other = home.resolve("other");
        FirstPartyPlugins.recordInstalled(home, entry.packageName(), digest);
        assertNull(FirstPartyPlugins.installedDigest(other, entry.packageName()));
    }

    /// The catalogue pins a version and the build packs one; they are the same number.
    @Test
    void theCatalogueAndTheBuildAgreeOnTheVersion() {
        for (DshTuiPlugins.Entry entry : DshTuiPlugins.ENTRIES) {
            if (!entry.bundled()) {
                continue;
            }
            FirstPartyPlugins.Bundled bundled = FirstPartyPlugins.find(entry.packageName())
                    .orElseThrow(() -> new AssertionError(entry.packageName() + " is bundled but not in the index"));
            assertEquals(bundled.version(), entry.version(),
                    entry.packageName() + ": DshTuiPlugins.ENTRIES and plugins/" + entry.packageName()
                            + "/package.json disagree");
        }
    }
}
