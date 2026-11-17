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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Pins the PI WEB release model: a release is a **pair** (pi-web + the Pi SDK
/// it was verified against), a terminal-only release is the SDK alone, and the
/// launch plan starts the session daemon before the web gateway.
class PiWebBrandTest {

    @TempDir
    Path brandRoot;

    private static void writeSdk(Path release, String version) throws IOException {
        Path manifest = release.resolve("node_modules").resolve(Brand.PI_SDK_PACKAGE).resolve("package.json");
        Files.createDirectories(manifest.getParent());
        Files.writeString(manifest, "{\"name\":\"" + Brand.PI_SDK_PACKAGE + "\",\"version\":\"" + version + "\"}\n",
                StandardCharsets.UTF_8);
    }

    private static void writeWebBinary(Path release) throws IOException {
        Path bin = release.resolve("node_modules/.bin/pi-web");
        Files.createDirectories(bin.getParent());
        Files.writeString(bin, "#!/bin/sh\n", StandardCharsets.UTF_8);
    }

    @Test
    void onlyTheVerifiedPairIsOffered() {
        assertTrue(Brand.PI_WEB.offersVersion("2.202611.8"));
        assertFalse(Brand.PI_WEB.offersVersion("2.202610.1"),
                "a version without a verified Pi has no install the panel can complete");
        assertEquals("1.4.0", Brand.PI_WEB.pairedSdkVersion("2.202611.8"));
        assertNull(Brand.PI_WEB.pairedSdkVersion("2.202610.1"));
        // Other brands are not filtered by this table.
        assertTrue(Brand.KIMI.offersVersion("anything"));
    }

    @Test
    void theClientIsDeploymentRelativeAndTheTerminalMayCoexist() {
        assertTrue(Brand.PI_WEB.relativeClient());
        assertTrue(Brand.PI_WEB.terminalCoexistsWithWeb());
        assertTrue(Brand.PI_WEB.supportsTerminalOnly());
        assertFalse(Brand.KIMI.relativeClient());
        assertFalse(Brand.KIMI.terminalCoexistsWithWeb());
    }

    @Test
    void aTerminalOnlyReleaseIsStillARelease() throws IOException {
        Path release = brandRoot.resolve(BrandInstaller.RELEASES_DIR).resolve("2.202611.8");
        writeSdk(release, "1.4.0");
        Files.writeString(release.resolve("release.json"),
                "{\"piWeb\":\"2.202611.8\",\"piSdk\":\"1.4.0\",\"webAvailable\":false,"
                        + "\"ptyAvailable\":false,\"installedAt\":1}\n",
                StandardCharsets.UTF_8);

        assertNotNull(BrandInstaller.packageDir(Brand.PI_WEB, brandRoot, "2.202611.8"),
                "the Pi SDK alone is a usable terminal release");
        List<BrandInstaller.Release> releases = BrandInstaller.releases(Brand.PI_WEB, brandRoot);
        assertEquals(1, releases.size());

        BrandInstaller.ReleaseInfo info = BrandInstaller.releaseInfo(release);
        assertNotNull(info);
        assertEquals("1.4.0", info.pairedSdk());
        assertFalse(info.webAvailable());
        assertFalse(info.ptyAvailable());
    }

    @Test
    void aFullReleaseNeedsTheWebExecutable() throws IOException {
        Path release = brandRoot.resolve(BrandInstaller.RELEASES_DIR).resolve("2.202611.8");
        writeSdk(release, "1.4.0");
        assertNotNull(BrandInstaller.packageDir(Brand.PI_WEB, brandRoot, "2.202611.8"));
        // A release with neither the web executable nor release.json is a
        // terminal-only shape as far as the list is concerned, but a full
        // install that lost its executable is still caught by the installer's
        // own post-check; the listing only refuses incomplete trees (no SDK).
        Path incomplete = brandRoot.resolve(BrandInstaller.RELEASES_DIR).resolve("9.9.9");
        Files.createDirectories(incomplete);
        assertNull(BrandInstaller.packageDir(Brand.PI_WEB, brandRoot, "9.9.9"));
        assertTrue(BrandInstaller.releases(Brand.PI_WEB, brandRoot).stream()
                .noneMatch(item -> item.version().equals("9.9.9")));
    }

    @Test
    void theLaunchPlanStartsBothHalvesBehindOneWrapper() throws Exception {
        Path release = brandRoot.resolve(BrandInstaller.RELEASES_DIR).resolve("2.202611.8");
        writeSdk(release, "1.4.0");
        writeWebBinary(release);
        Path server = release.resolve("node_modules/@chainingintention/pi-web-cn/dist/server");
        Files.createDirectories(server);
        Files.writeString(server.resolve("sessiond.js"), "", StandardCharsets.UTF_8);
        Files.writeString(server.resolve("index.js"), "", StandardCharsets.UTF_8);

        BrandInstanceManager manager = new BrandInstanceManager(Brand.PI_WEB,
                brandRoot.resolve("instances"));
        BrandInstance instance = manager.create("test", "2.202611.8");

        BrandRuntime.LaunchPlan plan = BrandRuntime.piWebPlan(manager, instance, release);

        assertEquals(5, plan.command().size(), plan.command().toString());
        Path wrapper = Path.of(plan.command().get(1));
        assertEquals(".pi-web-wrapper.mjs", wrapper.getFileName().toString());
        assertTrue(Files.isRegularFile(wrapper));
        String script = Files.readString(wrapper, StandardCharsets.UTF_8);
        assertTrue(script.contains("/health"), "the wrapper waits for the daemon before the gateway");
        assertTrue(script.contains("process.execPath"));

        assertTrue(plan.port() > 0);
        assertTrue(plan.companionPort() > 0);
        assertNotEquals(plan.port(), plan.companionPort(), "web and daemon never share a port");

        String daemonUrl = plan.environment().get("PI_WEB_SESSIOND_URL");
        assertNotNull(daemonUrl);
        assertTrue(daemonUrl.endsWith(":" + plan.companionPort()));
        assertEquals("127.0.0.1", plan.environment().get("PI_WEB_HOST"));
        assertEquals(String.valueOf(plan.port()), plan.environment().get("PI_WEB_PORT"));
        assertEquals("1", plan.environment().get("PI_WEB_SKIP_VERSION_CHECK"),
                "the panel owns the release; pi-web must not update itself");
        // The tool's state lives under the instance home, which the launcher hands
        // to the agent; a root-created data directory is the uid bug this avoids.
        assertFalse(plan.environment().containsKey("PI_WEB_DATA_DIR"));
        assertTrue(wrapper.startsWith(manager.homeDirectory(instance)));
        assertEquals("rw-r--r--", java.nio.file.attribute.PosixFilePermissions.toString(
                Files.getPosixFilePermissions(wrapper)), "node reads the wrapper as the agent");
        assertTrue(plan.environment().get("PI_WEB_PI_PACKAGE_DIR").contains(Brand.PI_SDK_PACKAGE));
    }

    @Test
    void theTerminalAdapterRefusesAReleaseWithoutPi() {
        assertThrows(IOException.class, () -> new org.jackhuang.hmcl.web.terminal.PiTui()
                .target(brandRoot, brandRoot.resolve("home"), brandRoot.resolve("workspace"), "id", null));
    }
}
