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
package org.jackhuang.hmcl.web.system;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/// The sandbox classification behind the page's notes: "容器里…" must only be
/// said inside a container, and WSL is a Linux VM with its own wording.
class HostInfoTest {

    @Test
    void dockerAndPodmanAndKubernetesAreContainers() {
        assertEquals("container", HostInfo.environmentFor(true, null, null, null, null));
        assertEquals("container", HostInfo.environmentFor(false, "0::/docker/abc", null, null, null));
        assertEquals("container", HostInfo.environmentFor(false, "0::/kubepods/burstable/pod1", null, null, null));
        assertEquals("container", HostInfo.environmentFor(false, "0::/", null, "podman", null));
    }

    @Test
    void wslIsItsOwnEnvironment() {
        assertEquals("wsl", HostInfo.environmentFor(false, "0::/",
                "Linux version 5.15.153.1-microsoft-standard-WSL2", null, null));
        assertEquals("wsl", HostInfo.environmentFor(false, "0::/", "Linux version 6.2", null, "Ubuntu-24.04"));
    }

    @Test
    void aPlainLinuxBoxIsNative() {
        assertEquals("native", HostInfo.environmentFor(false, "0::/", "Linux version 6.18.18", null, null));
    }

    @Test
    void detectionAlwaysAnswersSomethingUsable() {
        HostInfo info = HostInfo.detect();
        assertNotNull(info.os());
        assertEquals(true, switch (info.environment()) {
            case "container", "wsl", "native" -> true;
            default -> false;
        });
    }
}
