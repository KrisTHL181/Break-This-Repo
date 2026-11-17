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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The three TUI-brand adapters: direct argv, instance-scoped data dirs, and
/// resume flags. None may start a shell or a package-manager shim.
class TuiBrandAdaptersTest {

    @TempDir
    Path packageDir;

    private Path writePackageEntry(String relative) throws Exception {
        Path entry = packageDir.resolve(relative);
        Files.createDirectories(entry.getParent());
        Files.writeString(entry, "// entry\n");
        return entry;
    }

    @Test
    void qwenRunsItsEntryAndPinsTheDataDir() throws Exception {
        Path entry = writePackageEntry("node_modules/@qwen-code/qwen-code/cli-entry.js");

        TerminalTarget target = new QwenTui().target(packageDir, packageDir.resolve("home"),
                packageDir.resolve("workspace"), "qwen-1", "session-9");

        assertTrue(target.command().get(0).endsWith("node"), "argv[0] is node: " + target.command());
        assertEquals(entry.toString(), target.command().get(1));
        assertEquals(List.of("--resume", "session-9"),
                target.command().subList(target.command().size() - 2, target.command().size()));
        assertEquals(packageDir.resolve("home/.qwen").toString(),
                target.environment().get("QWEN_HOME"));
        assertFalse(TerminalTarget.isShell(target.command().get(0)));
    }

    @Test
    void minimaxRunsItsEntryAndPinsBothDataDirAliases() throws Exception {
        Path entry = writePackageEntry("node_modules/@minimax-ai/code/cli.js");

        TerminalTarget target = new MiniMaxTui().target(packageDir, packageDir.resolve("home"),
                packageDir.resolve("workspace"), "minimax-1", "session-7");

        assertTrue(target.command().get(0).endsWith("node"), "argv[0] is node: " + target.command());
        assertEquals(entry.toString(), target.command().get(1));
        assertEquals(List.of("--session", "session-7"),
                target.command().subList(target.command().size() - 2, target.command().size()));
        assertEquals(packageDir.resolve("home/.minimax").toString(),
                target.environment().get("MINIMAX_DATA_DIR"));
        assertEquals(packageDir.resolve("home/.minimax").toString(),
                target.environment().get("MAVIS_DATA_DIR"));
    }

    @Test
    void mimoFallsBackToItsLauncherAndThenPrefersTheFetchedBinary() throws Exception {
        Path launcher = writePackageEntry("node_modules/@mimo-ai/cli/mimo.js");

        TerminalTarget launcherTarget = new MimoTui().target(packageDir, packageDir.resolve("home"),
                packageDir.resolve("workspace"), "mimo-1", "session-3");

        assertTrue(launcherTarget.command().get(0).endsWith("node"),
                "argv[0] is node: " + launcherTarget.command());
        assertEquals(launcher.toString(), launcherTarget.command().get(1));
        assertEquals(List.of("--session", "session-3"),
                launcherTarget.command().subList(launcherTarget.command().size() - 2,
                        launcherTarget.command().size()));
        assertEquals(packageDir.resolve("home/.mimocode").toString(),
                launcherTarget.environment().get("MIMOCODE_HOME"));

        String binaryName = OperatingSystem.CURRENT_OS == OperatingSystem.WINDOWS ? "mimo.exe" : "mimo";
        Path binary = packageDir.resolve("node_modules/@mimo-ai/cli/vendor").resolve(binaryName);
        Files.createDirectories(binary.getParent());
        Files.writeString(binary, "binary\n");

        TerminalTarget binaryTarget = new MimoTui().target(packageDir, packageDir.resolve("home"),
                packageDir.resolve("workspace"), "mimo-1", null);
        if (OperatingSystem.CURRENT_OS == OperatingSystem.WINDOWS) {
            // Windows goes through the launcher so the copied-out binary, not
            // vendor/mimo.exe itself, is the running PE.
            assertTrue(binaryTarget.command().get(0).endsWith("node"),
                    "argv[0] is node on Windows: " + binaryTarget.command());
        } else {
            assertEquals(binary.toString(), binaryTarget.command().get(0));
        }
        assertFalse(TerminalTarget.isShell(binaryTarget.command().get(0)));
    }

    @Test
    void aShellIsRefusedAsTheTerminalRoot() {
        for (String shell : List.of("sh", "/bin/bash", "C:\\Windows\\System32\\cmd.exe",
                "C:\\Windows\\System32\\command.com", "pwsh",
                "/usr/local/bin/agent.sh", "C:\\tools\\run.bat", "tool.cmd")) {
            assertTrue(TerminalTarget.isShell(shell), shell);
            assertThrows(IllegalArgumentException.class, () -> new TerminalTarget(
                    "qwen", "i1", List.of(shell), packageDir, Map.of()), shell);
        }
        assertFalse(TerminalTarget.isShell("/opt/node/bin/node"));
        assertFalse(TerminalTarget.isShell("C:\\node\\node.exe"));
        // `.com` is a DOS executable suffix, not a shell; only command.com is.
        assertFalse(TerminalTarget.isShell("C:\\tools\\native.com"));
    }
}
