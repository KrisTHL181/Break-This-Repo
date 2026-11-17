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

import org.jetbrains.annotations.NotNullByDefault;

import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/// Everything the terminal needs to start one instance's agent: the exact
/// argv, the working directory and the complete environment.
///
/// The command is a list, never a command line: the adapter decides the
/// program and its arguments, and nothing between the browser and the agent
/// can add one. `command[0]` is expected to be the interpreter or the agent
/// binary itself — never a shell or a package-manager shim (see
/// [BrandTui]).
///
/// @param brand       the brand id (`kimi`, `opencode`, …)
/// @param instanceId  the instance the terminal belongs to
/// @param command     program and arguments
/// @param cwd         the instance workspace
/// @param environment the child's complete environment
@NotNullByDefault
public record TerminalTarget(
        String brand,
        String instanceId,
        List<String> command,
        Path cwd,
        Map<String, String> environment) {

    /// Interpreters whose only job is to read commands and wait for more. A
    /// terminal rooted at one of these is the failure this type exists to
    /// prevent: when the agent exits, the shell stays, prints its prompt, and
    /// the Web Shell shows a `$` where the agent used to be.
    private static final Set<String> SHELL_NAMES = Set.of(
            "sh", "bash", "zsh", "fish", "dash", "ksh", "csh", "tcsh",
            "cmd", "cmd.exe", "command.com",
            "powershell", "powershell.exe", "pwsh", "pwsh.exe");

    /// Shell scripts, including the `.cmd` / `.bat` shims Windows would hand to
    /// `cmd.exe` behind `CreateProcess`'s back. `.com` is deliberately absent:
    /// it is a DOS executable suffix, not a shell, and rejecting it would refuse
    /// a legitimate native program; `command.com` is caught by name instead.
    private static final List<String> SHELL_SUFFIXES = List.of(
            ".sh", ".bash", ".zsh", ".fish", ".cmd", ".bat", ".ps1");

    public TerminalTarget {
        if (command.isEmpty()) {
            throw new IllegalArgumentException("a terminal target needs a command");
        }
        if (isShell(command.get(0))) {
            throw new IllegalArgumentException("refusing to start a shell as the terminal: "
                    + command.get(0) + " — the terminal must run the agent itself, so exiting "
                    + "it cannot fall back to a prompt");
        }
        command = List.copyOf(command);
        environment = Map.copyOf(environment);
    }

    /// Whether `program` names a shell or a shell script.
    ///
    /// Deliberately a string test: the same code runs on every platform, and a
    /// Windows path is not a path on Linux.
    static boolean isShell(String program) {
        String normalized = program.replace('\\', '/');
        int slash = normalized.lastIndexOf('/');
        String name = (slash >= 0 ? normalized.substring(slash + 1) : normalized)
                .toLowerCase(Locale.ROOT);
        if (SHELL_NAMES.contains(name)) {
            return true;
        }
        for (String suffix : SHELL_SUFFIXES) {
            if (name.endsWith(suffix)) {
                return true;
            }
        }
        return false;
    }
}
