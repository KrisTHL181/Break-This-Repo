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

import org.jackhuang.hmcl.util.platform.OperatingSystem;
import org.jetbrains.annotations.NotNullByDefault;

import java.nio.file.Files;
import java.nio.file.Path;

/// Where the panel is running, in the two dimensions the page's copy cares
/// about: the operating system, and whether it sees a host or a sandbox.
///
/// The page used to say "容器里显示的是宿主机核心" unconditionally, which is
/// wrong on a native Windows box and wrong in WSL. The notes now switch on this
/// record instead of guessing.
///
/// @param os          `linux`, `windows`, `macos` or `other`
/// @param environment `container` (Docker/Podman/LXC), `wsl` (a Linux VM under
///                    Windows) or `native`
@NotNullByDefault
public record HostInfo(String os, String environment) {

    public static final HostInfo NATIVE_LINUX = new HostInfo("linux", "native");

    /// Detects the current process. Read once at startup: neither the OS nor
    /// the sandbox changes while the panel runs.
    ///
    /// @return the description
    public static HostInfo detect() {
        String os = switch (OperatingSystem.CURRENT_OS) {
            case WINDOWS -> "windows";
            case MACOS -> "macos";
            case LINUX -> "linux";
            default -> "other";
        };
        if (!"linux".equals(os)) {
            return new HostInfo(os, "native");
        }
        boolean dockerEnv = Files.exists(Path.of("/.dockerenv"));
        String cgroup = read(Path.of("/proc/1/cgroup"));
        String procVersion = read(Path.of("/proc/version"));
        return new HostInfo(os, environmentFor(dockerEnv, cgroup, procVersion,
                System.getenv("container"), System.getenv("WSL_DISTRO_NAME")));
    }

    /// The sandbox decision, split out so the classification can be tested
    /// without being inside each kind of sandbox.
    ///
    /// @param dockerEnv    whether `/.dockerenv` exists
    /// @param cgroup       `/proc/1/cgroup`, or `null`
    /// @param procVersion  `/proc/version`, or `null`
    /// @param containerEnv the `container` environment variable, or `null`
    /// @param wslEnv       the `WSL_DISTRO_NAME` environment variable, or `null`
    /// @return `container`, `wsl` or `native`
    static String environmentFor(boolean dockerEnv, String cgroup, String procVersion,
                                 String containerEnv, String wslEnv) {
        String groups = cgroup == null ? "" : cgroup;
        if (dockerEnv
                || (containerEnv != null && !containerEnv.isBlank())
                || groups.contains("docker") || groups.contains("kubepods")
                || groups.contains("containerd") || groups.contains("lxc")) {
            return "container";
        }
        String version = procVersion == null ? "" : procVersion.toLowerCase(java.util.Locale.ROOT);
        if ((wslEnv != null && !wslEnv.isBlank())
                || version.contains("microsoft") || version.contains("wsl")) {
            return "wsl";
        }
        return "native";
    }

    private static String read(Path file) {
        try {
            return Files.isRegularFile(file) ? Files.readString(file) : null;
        } catch (RuntimeException | java.io.IOException e) {
            return null;
        }
    }
}
