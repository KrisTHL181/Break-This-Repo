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

import org.jackhuang.hmcl.util.platform.Architecture;
import org.jackhuang.hmcl.util.platform.OperatingSystem;
import org.jackhuang.hmcl.util.platform.Platform;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/// The Node.js artifact naming, pinned **without Windows**: the platform is a parameter,
/// so the Windows name a bare deploy downloads is asserted here on any machine.
///
/// These are the functions the Windows fix rests on — `platformTag` used to answer
/// `linux-x64` for every platform but macOS, and the managed runtime on Windows therefore
/// installed a Linux build (see `docs/windows-bare-deploy.md`).
class NodeArtifactNamingTest {

    @Test
    void windowsBuildsAreNamedWinX64AndZip() throws DshException {
        assertEquals("win-x64", NodeRuntimeManager.platformTag(Platform.WINDOWS_X86_64));
        assertEquals("win-arm64", NodeRuntimeManager.platformTag(Platform.WINDOWS_ARM64));
        assertEquals("win-x64-zip", NodeRuntimeManager.indexTag("win-x64"));
        assertEquals("win-arm64-zip", NodeRuntimeManager.indexTag("win-arm64"));
        assertEquals(".zip", NodeRuntimeManager.archiveExtension("win-x64"));
        assertEquals("node-v22.19.0-win-x64.zip",
                NodeRuntimeManager.archiveFileName("22.19.0", "win-x64"));
    }

    @Test
    void linuxAndMacKeepTheirOldNames() throws DshException {
        assertEquals("linux-x64", NodeRuntimeManager.platformTag(Platform.LINUX_X86_64));
        assertEquals("linux-arm64", NodeRuntimeManager.platformTag(Platform.LINUX_ARM64));
        assertEquals("linux-x64", NodeRuntimeManager.indexTag("linux-x64"),
                "the index calls a Linux build by its plain tag");
        assertEquals(".tar.xz", NodeRuntimeManager.archiveExtension("linux-x64"));
        assertEquals("node-v22.19.0-linux-x64.tar.xz",
                NodeRuntimeManager.archiveFileName("22.19.0", "linux-x64"));

        assertEquals("darwin-x64", NodeRuntimeManager.platformTag(Platform.MACOS_X86_64));
        assertEquals("darwin-arm64", NodeRuntimeManager.platformTag(Platform.MACOS_ARM64));
        assertEquals("osx-x64-tar", NodeRuntimeManager.indexTag("darwin-x64"));
        assertEquals("osx-arm64-tar", NodeRuntimeManager.indexTag("darwin-arm64"));
        assertEquals(".tar.gz", NodeRuntimeManager.archiveExtension("darwin-arm64"));
        assertEquals("node-v22.19.0-darwin-arm64.tar.gz",
                NodeRuntimeManager.archiveFileName("22.19.0", "darwin-arm64"));
    }

    @Test
    void aPlatformNodeDoesNotPublishIsRefused() {
        assertThrows(DshException.class,
                () -> NodeRuntimeManager.platformTag(Platform.WINDOWS_X86));
        assertThrows(DshException.class,
                () -> NodeRuntimeManager.platformTag(Platform.LINUX_RISCV64));
        // FreeBSD used to fall into the `default` branch and hand out `linux-x64`,
        // i.e. an ELF the system then could not exec. It must refuse instead.
        assertThrows(DshException.class,
                () -> NodeRuntimeManager.platformTag(Platform.FREEBSD_X86_64));
        assertThrows(DshException.class,
                () -> NodeRuntimeManager.platformTag(
                        new Platform(OperatingSystem.MACOS, Architecture.X86)));
    }
}
