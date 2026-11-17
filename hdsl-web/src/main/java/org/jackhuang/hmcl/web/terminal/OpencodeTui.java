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
package org.jackhuang.hmcl.web.terminal;

import org.jackhuang.hmcl.util.platform.OperatingSystem;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/// OpenCode's interactive TUI: `<native binary> [--session <id>]`.
///
/// The npm package arrives in two layouts, both of which ship
/// `bin/opencode.cjs` as a launcher around a native binary. Since 1.19 the
/// platform binary comes from `opencode-ai` itself —
/// `node_modules/opencode-ai/bin/opencode.exe` (the `.exe` name is used on
/// POSIX too; it is an ELF) — with unscoped `opencode-<os>-<arch>` platform
/// packages as the fallback. Older releases used scoped
/// `@opencode/cli-<os>-<arch>` packages.
///
/// Starting through the launcher would make the Node launcher the root
/// process, so the adapter resolves the native binary itself, trying the same
/// variants the launcher would (baseline / musl on x64).
///
/// Sessions live under the XDG data directory (`~/.local/share/opencode`),
/// which follows the instance `HOME` the target carries.
@NotNullByDefault
public final class OpencodeTui implements BrandTui {

    @Override
    public TerminalTarget target(Path packageDir, Path home, Path workspace,
                                 String instanceId, @Nullable String resumeSession) throws IOException {
        Path binary = resolveBinary(packageDir);
        List<String> command = new ArrayList<>(List.of(binary.toString()));
        if (resumeSession != null && !resumeSession.isBlank()) {
            command.add("--session");
            command.add(resumeSession);
        }
        return new TerminalTarget("opencode", instanceId, command, workspace, BrandTui.environment(home));
    }

    /// The native CLI for this platform, tried in the order the shipped
    /// launcher would try it.
    static Path resolveBinary(Path packageDir) throws IOException {
        String os = switch (OperatingSystem.CURRENT_OS) {
            case LINUX -> "linux";
            case MACOS -> "darwin";
            case WINDOWS -> "windows";
            default -> null;
        };
        if (os == null) {
            throw new IOException("opencode has no native build for " + OperatingSystem.CURRENT_OS);
        }
        String arch = architecture();
        // Relative to node_modules, in the order the launcher would try them.
        List<String> candidates = new ArrayList<>();
        // Since 1.19: the main package carries the platform binary. The
        // postinstall always names it opencode.exe, POSIX included.
        candidates.add("opencode-ai/bin/opencode.exe");
        candidates.add("opencode-ai/bin/opencode");
        for (String suffix : platformSuffixes(arch)) {
            // Unscoped platform packages: opencode-<os>-<arch>[-baseline][-musl].
            candidates.add("opencode-" + os + "-" + arch + suffix + "/bin/opencode");
            candidates.add("opencode-" + os + "-" + arch + suffix + "/bin/opencode.exe");
        }
        for (String suffix : platformSuffixes(arch)) {
            // Older releases: @opencode/cli-<os>-<arch>[-baseline][-musl].
            candidates.add("@opencode/cli-" + os + "-" + arch + suffix + "/bin/opencode");
            candidates.add("@opencode/cli-" + os + "-" + arch + suffix + "/bin/opencode.exe");
        }
        for (String relative : candidates) {
            Path candidate = packageDir.resolve("node_modules").resolve(relative);
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
        }
        throw new IOException("opencode native binary missing under " + packageDir
                + " (expected node_modules/opencode-ai/bin/opencode.exe or opencode-"
                + os + "-" + arch + "/bin/opencode)");
    }

    /// The platform package variants the launcher would try, in order.
    private static List<String> platformSuffixes(String arch) {
        List<String> suffixes = new ArrayList<>(List.of(""));
        if ("x64".equals(arch)) {
            suffixes.add("-baseline");
            suffixes.add("-musl");
            suffixes.add("-baseline-musl");
        } else {
            suffixes.add("-musl");
        }
        return suffixes;
    }

    private static String architecture() throws IOException {
        String arch = System.getProperty("os.arch", "").toLowerCase(Locale.ROOT);
        return switch (arch) {
            case "x86_64", "amd64" -> "x64";
            case "aarch64", "arm64" -> "arm64";
            case "arm" -> "arm";
            default -> throw new IOException("unsupported CPU architecture for opencode: " + arch);
        };
    }
}
