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

import org.jackhuang.hmcl.util.platform.Executables;
import org.jackhuang.hmcl.util.platform.OperatingSystem;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/// The ZCode adapter must start the distribution's TUI entry — never the web
/// mode, never through a shell.
class ZcodeTuiTest {

    @TempDir
    Path packageDir;

    @Test
    void missingEntryFailsLoudly() {
        IOException expected = assertThrows(IOException.class, () -> ZcodeTui.target(
                packageDir, packageDir.resolve("data"), packageDir.resolve("home"),
                packageDir.resolve("workspace"), "z1", "node"));
        assertTrue(expected.getMessage().contains("bin/zcode.mjs"), expected.getMessage());
    }

    @Test
    void aConfiguredNodeThatDoesNotExistFailsLoudly() throws Exception {
        Path entry = packageDir.resolve("bin/zcode.mjs");
        Files.createDirectories(entry.getParent());
        Files.writeString(entry, "console.log('zcode');\n");
        Path agent = packageDir.resolve("agent/zcode.cjs");
        Files.createDirectories(agent.getParent());
        Files.writeString(agent, "// agent\n");

        IOException expected = assertThrows(IOException.class, () -> ZcodeTui.target(
                packageDir, packageDir.resolve("data"), packageDir.resolve("home"),
                packageDir.resolve("workspace"), "z1",
                packageDir.resolve("no-such-node").toString()));
        assertTrue(expected.getMessage().contains("zcode.nodePath"), expected.getMessage());
    }

    /// A distribution built for the web only has `server/`, `web/` and the
    /// runner; the TUI lives in `agent/zcode.cjs`. Starting that one would fail
    /// inside the PTY, so the adapter refuses it up front.
    @Test
    void aWebOnlyDistributionIsRefusedWithTheReason() throws Exception {
        Path entry = packageDir.resolve("bin/zcode.mjs");
        Files.createDirectories(entry.getParent());
        Files.writeString(entry, "// runner\n");

        IOException expected = assertThrows(IOException.class, () -> ZcodeTui.target(
                packageDir, packageDir.resolve("data"), packageDir.resolve("home"),
                packageDir.resolve("workspace"), "z1", "node"));
        assertTrue(expected.getMessage().contains("agent/zcode.cjs"), expected.getMessage());
    }

    /// The regression this pins: `ZcodeApiServlet` hands the adapter the
    /// configured value with `node` as its default, so a bare name must be
    /// looked up on `PATH`. Requiring a file made every ZCode terminal fail
    /// with "zcode.nodePath does not point at a file: node".
    @Test
    void aBareNodeNameIsResolvedThroughPath() throws Exception {
        IOException missing = assertThrows(IOException.class,
                () -> ZcodeTui.resolveNode("hdsl-no-such-node-6d1f"));
        assertTrue(missing.getMessage().contains("zcode.nodePath"), missing.getMessage());

        assumeTrue(Executables.resolve("node").isPresent(), "node is not installed on this machine");
        assertTrue(Files.isRegularFile(ZcodeTui.resolveNode("node")),
                "the default `node` must resolve through PATH");
    }

    /// The runtime the panel installed is a first-class answer.
    ///
    /// On a bare Windows deployment there is no `node` on `PATH` — the panel installs
    /// one under `<data>/hdsl/runtimes` so nobody has to — and ZCode used to be
    /// launched as a bare `node`, which failed with `CreateProcess error=2` while that
    /// runtime sat right there. The lookups are passed in so this does not depend on
    /// what the machine running the test happens to have.
    @Test
    void thePanelsManagedRuntimeIsUsedWhenNothingIsOnPath(@TempDir Path temp) throws Exception {
        Path managed = Files.createFile(temp.resolve("node.exe"));

        assertEquals(managed, ZcodeTui.resolveNode(null,
                java.util.Optional::<Path>empty, () -> java.util.Optional.of(managed)));
        // …and the system's own node still wins when there is one.
        Path system = Files.createFile(temp.resolve("system-node.exe"));
        assertEquals(system, ZcodeTui.resolveNode(null,
                () -> java.util.Optional.of(system), () -> java.util.Optional.of(managed)));
    }

    @Test
    void neitherPathNorAManagedRuntimeSaysWhatToDo() {
        IOException refused = assertThrows(IOException.class, () -> ZcodeTui.resolveNode(null,
                java.util.Optional::<Path>empty, java.util.Optional::<Path>empty));
        assertTrue(refused.getMessage().contains("设置 → Node.js"), refused.getMessage());
    }

    /// The same thing through the public entry: a distribution plus the
    /// servlet's default must produce a runnable argv.
    @Test
    void theDefaultNodeNameFromTheServletProducesARunnableCommand() throws Exception {
        assumeTrue(Executables.resolve("node").isPresent(), "node is not installed on this machine");
        Path entry = packageDir.resolve("bin/zcode.mjs");
        Files.createDirectories(entry.getParent());
        Files.writeString(entry, "// runner\n");
        Path agent = packageDir.resolve("agent/zcode.cjs");
        Files.createDirectories(agent.getParent());
        Files.writeString(agent, "// agent\n");

        TerminalTarget target = ZcodeTui.target(packageDir, packageDir.resolve("data"),
                packageDir.resolve("home"), packageDir.resolve("workspace"), "z1", "node");

        assertEquals(2, target.command().size(), target.command().toString());
        assertTrue(Files.isRegularFile(Path.of(target.command().get(0))),
                "argv[0] must be a startable file: " + target.command().get(0));
    }

    /// The web mode's flags must not leak into the terminal: `--web` in a PTY
    /// would start a second server, `--port 0` would print a readiness line
    /// nobody reads, and `--no-token` belongs to the proxied mount.
    @Test
    void buildsTheTuiCommandWithoutTheWebFlags() throws Exception {
        assumeTrue(OperatingSystem.CURRENT_OS != OperatingSystem.WINDOWS,
                "POSIX execute bit is what makes a file a program");
        Path entry = packageDir.resolve("bin/zcode.mjs");
        Files.createDirectories(entry.getParent());
        Files.writeString(entry, "// runner\n");
        Path agent = packageDir.resolve("agent/zcode.cjs");
        Files.createDirectories(agent.getParent());
        Files.writeString(agent, "// agent\n");
        Path node = packageDir.resolve("node");
        Files.writeString(node, "#!/bin/sh\nexit 0\n");
        Files.setPosixFilePermissions(node, PosixFilePermissions.fromString("rwx------"));

        Path data = packageDir.resolve("data");
        Path home = packageDir.resolve("home");
        Path workspace = packageDir.resolve("workspace");
        TerminalTarget target = ZcodeTui.target(packageDir, data, home, workspace, "z1", node.toString());

        assertEquals(ZcodeTui.BRAND, target.brand());
        assertEquals("z1", target.instanceId());
        assertEquals(2, target.command().size(), "argv is node + entry: " + target.command());
        assertTrue(target.command().get(0).contains("node"), target.command().get(0));
        assertEquals(entry.toString(), target.command().get(1));
        assertFalse(target.command().contains("--web"), target.command().toString());
        assertFalse(target.command().contains("--host"), target.command().toString());
        assertFalse(target.command().contains("--port"), target.command().toString());
        assertFalse(target.command().contains("--no-token"), target.command().toString());
        assertEquals(workspace, target.cwd());
        assertEquals(data.toString(), target.environment().get("ZCODE_DATA_BASE_DIR"));
        // The session database lives under HOME, so it has to be the instance's own
        // home — the same one the web process gets, or the two surfaces would list
        // different sessions.
        assertEquals(home.toString(), target.environment().get("HOME"));
        assertFalse(home.toString().equals(System.getenv("HOME")),
                "the panel's own home must not leak in: " + target.environment().get("HOME"));
        assertEquals("xterm-256color", target.environment().get("TERM"));
        assertEquals("truecolor", target.environment().get("COLORTERM"));
    }
}