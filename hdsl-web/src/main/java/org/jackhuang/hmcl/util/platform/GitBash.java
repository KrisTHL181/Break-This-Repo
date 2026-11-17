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
package org.jackhuang.hmcl.util.platform;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/// Finds the bash a Windows-hosted agent wants for its own tools.
///
/// Agents ported from POSIX run their shell tools through a bash even on Windows
/// (kimi-code says so in its own error message, and honours
/// `KIMI_SHELL_PATH`). The panel can find Git for Windows for them: it is the
/// one bash a Windows machine is likely to have, it behaves like the bash the
/// agent was written against, and — unlike the `bash.exe` Windows itself puts on
/// `PATH` — it is not the WSL launcher.
///
/// That last point is the whole reason this class exists rather than a call to
/// [Executables#resolve]: `C:\Windows\System32\bash.exe` and the Windows Store's
/// `WindowsApps\bash.exe` are WSL shims. They exist on machines with no Git Bash
/// at all, they start a Linux distribution instead of a shell in the workspace,
/// and handing one to an agent is worse than saying "no bash found".
@NotNullByDefault
public final class GitBash {

    private GitBash() {
    }

    /// The Git Bash on this machine, if there is one.
    ///
    /// @return an absolute path to a `bash.exe` that is not a WSL shim, or empty
    public static Optional<Path> find() {
        if (OperatingSystem.CURRENT_OS != OperatingSystem.WINDOWS) {
            return Optional.empty();
        }
        List<Path> candidates = new ArrayList<>();
        // Where the Git for Windows installer puts it, for the three ways it can
        // be installed (per-machine, per-machine 32-bit, per-user).
        for (String root : List.of("ProgramFiles", "ProgramW6432", "ProgramFiles(x86)")) {
            String directory = System.getenv(root);
            if (directory != null && !directory.isBlank()) {
                candidates.add(Path.of(directory, "Git", "bin", "bash.exe"));
            }
        }
        String localAppData = System.getenv("LOCALAPPDATA");
        if (localAppData != null && !localAppData.isBlank()) {
            candidates.add(Path.of(localAppData, "Programs", "Git", "bin", "bash.exe"));
        }
        // …and whatever `PATH` answers, for an installation somewhere else.
        Executables.resolve("bash").ifPresent(candidates::add);
        return pick(candidates);
    }

    /// Names the Git Bash this machine has, for a client that refuses to work
    /// without one.
    ///
    /// Two clients ask for it under their own variable — Kimi Code
    /// (`KIMI_SHELL_PATH`) and Claude Code (`CLAUDE_CODE_GIT_BASH_PATH`) — and the
    /// rule is the same for both: an address the operator set, in the panel's own
    /// environment or in the child's, is the one that is used. Nothing is written
    /// when this machine has no Git Bash: a wrong address is worse than none, and
    /// the client's own message says what to install.
    ///
    /// @param environment the child environment to amend
    /// @param variable    the client's variable name
    /// @return whether an address was written
    public static boolean applyTo(Map<String, String> environment, String variable) {
        if (environment.containsKey(variable) || System.getenv().containsKey(variable)) {
            return false;
        }
        return find().map(bash -> {
            environment.put(variable, bash.toString());
            return true;
        }).orElse(false);
    }

    /// The first candidate that is a real `bash.exe` and not one of the WSL shims.
    ///
    /// Package-private because the filter is the part that matters, and a machine
    /// that never had Git installed still has the shims to try it against.
    ///
    /// @param candidates the paths to consider, in order
    /// @return the first acceptable one, or empty
    static Optional<Path> pick(List<@Nullable Path> candidates) {
        for (Path candidate : candidates) {
            if (candidate == null) {
                continue;
            }
            Path name = candidate.getFileName();
            if (name == null || !name.toString().equalsIgnoreCase("bash.exe")) {
                continue;
            }
            if (isWslShim(candidate)) {
                continue;
            }
            try {
                if (Files.isRegularFile(candidate)) {
                    return Optional.of(candidate.toAbsolutePath());
                }
            } catch (InvalidPathException ignored) {
                // A malformed candidate is one unusable path, not an unusable search.
            }
        }
        return Optional.empty();
    }

    /// Whether a path is the WSL launcher rather than a shell in the workspace.
    ///
    /// Both spellings are shims: `%SystemRoot%\System32\bash.exe` ships with
    /// Windows itself, and the Store launcher lives under `WindowsApps`.
    private static boolean isWslShim(Path candidate) {
        String normalized = candidate.toAbsolutePath().toString()
                .toLowerCase(Locale.ROOT).replace('\\', '/');
        return normalized.contains("/windows/system32/") || normalized.contains("/windowsapps/");
    }
}
