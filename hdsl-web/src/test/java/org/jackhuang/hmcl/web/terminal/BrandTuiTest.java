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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/// The adapters must produce an argv whose first element is the interpreter
/// or the agent binary — never a shell and never a pnpm shim.
class BrandTuiTest {

    @TempDir
    Path packageDir;

    /// pnpm writes `.bin/kimi` as a `#!/bin/sh` shim rather than a symlink;
    /// the adapter starts the package's own `dist/main.mjs` regardless.
    @Test
    void kimiStartsThePackageEntryNotThePnpmShim() throws Exception {
        Path entry = packageDir.resolve("node_modules/@moonshot-ai/kimi-code/dist/main.mjs");
        Files.createDirectories(entry.getParent());
        Files.writeString(entry, "console.log('kimi');\n");
        Path bin = packageDir.resolve("node_modules/.bin");
        Files.createDirectories(bin);
        Files.writeString(bin.resolve("kimi"), "#!/bin/sh\nexec node ../@moonshot-ai/kimi-code/dist/main.mjs \"$@\"\n");
        Files.setPosixFilePermissions(bin.resolve("kimi"), PosixFilePermissions.fromString("rwx------"));

        TerminalTarget target = new KimiTui().target(packageDir, packageDir.resolve("home"),
                packageDir.resolve("workspace"), "kimi-1", null);

        assertEquals("kimi", target.brand());
        assertEquals(2, target.command().size());
        assertTrue(target.command().get(0).endsWith("node"), "argv[0] is node: " + target.command());
        assertEquals(entry.toAbsolutePath().normalize().toString(), target.command().get(1));
        assertEquals(packageDir.resolve("home").toString(), target.environment().get("HOME"));
        assertEquals("xterm-256color", target.environment().get("TERM"));
    }

    @Test
    void kimiResumeAddsTheFlag() throws Exception {
        Path entry = packageDir.resolve("node_modules/@moonshot-ai/kimi-code/dist/main.mjs");
        Files.createDirectories(entry.getParent());
        Files.writeString(entry, "console.log('kimi');\n");

        TerminalTarget target = new KimiTui().target(packageDir, packageDir.resolve("home"),
                packageDir.resolve("workspace"), "kimi-1", "session-42");

        assertEquals(List.of("--resume", "session-42"),
                target.command().subList(target.command().size() - 2, target.command().size()));
    }

    @Test
    void kimiWithoutThePackageEntryFailsLoudly() {
        Path bin = packageDir.resolve("node_modules/.bin");
        try {
            Files.createDirectories(bin);
            Files.writeString(bin.resolve("kimi"), "#!/bin/sh\nexit 0\n");
            Files.setPosixFilePermissions(bin.resolve("kimi"), PosixFilePermissions.fromString("rwx------"));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        IOException expected = assertThrows(IOException.class, () -> new KimiTui().target(
                packageDir, packageDir.resolve("home"), packageDir.resolve("workspace"), "kimi-1", null));
        assertTrue(expected.getMessage().contains("expected node_modules/@moonshot-ai/kimi-code/dist/main.mjs"),
                expected.getMessage());
    }

    /// Since 1.19 the platform binary is shipped by `opencode-ai` itself, as
    /// `bin/opencode.exe` even on POSIX.
    @Test
    void opencodeStartsTheNativeBinaryShippedByTheMainPackage() throws Exception {
        assumeTrue(OperatingSystem.CURRENT_OS == OperatingSystem.LINUX, "binary name is per platform");
        Path binary = packageDir.resolve("node_modules/opencode-ai/bin/opencode.exe");
        Files.createDirectories(binary.getParent());
        Files.writeString(binary, "#!/bin/sh\nexit 0\n");
        Files.setPosixFilePermissions(binary, PosixFilePermissions.fromString("rwx------"));

        TerminalTarget target = new OpencodeTui().target(packageDir, packageDir.resolve("home"),
                packageDir.resolve("workspace"), "oc-1", "ses_123");

        assertEquals(binary.toString(), target.command().get(0));
        assertEquals(List.of("--session", "ses_123"),
                target.command().subList(target.command().size() - 2, target.command().size()));
    }

    /// Older layouts: a platform package, scoped or not.
    @Test
    void opencodeFallsBackToPlatformPackages() throws Exception {
        assumeTrue(OperatingSystem.CURRENT_OS == OperatingSystem.LINUX, "binary name is per platform");
        String arch = System.getProperty("os.arch", "");
        String platform = "aarch64".equals(arch) || "arm64".equals(arch) ? "arm64" : "x64";
        Path binary = packageDir.resolve("node_modules/opencode-linux-" + platform + "/bin/opencode");
        Files.createDirectories(binary.getParent());
        Files.writeString(binary, "#!/bin/sh\nexit 0\n");
        Files.setPosixFilePermissions(binary, PosixFilePermissions.fromString("rwx------"));

        TerminalTarget target = new OpencodeTui().target(packageDir, packageDir.resolve("home"),
                packageDir.resolve("workspace"), "oc-1", null);

        assertEquals(binary.toString(), target.command().get(0));
    }

    @Test
    void opencodeWithoutABinaryFailsLoudly() {
        assumeTrue(OperatingSystem.CURRENT_OS == OperatingSystem.LINUX, "binary name is per platform");
        IOException expected = assertThrows(IOException.class, () -> new OpencodeTui().target(
                packageDir, packageDir.resolve("home"), packageDir.resolve("workspace"), "oc-1", null));
        assertTrue(expected.getMessage().contains("opencode native binary missing"), expected.getMessage());
    }

    @Test
    void hermesStartsTheBundledTuiWithTheInstancesNode() throws Exception {
        Path entry = packageDir.resolve("hermes_cli/tui_dist/entry.js");
        Files.createDirectories(entry.getParent());
        Files.writeString(entry, "// tui\n");
        Path python = packageDir.resolve(".venv/bin/python");
        Files.createDirectories(python.getParent());
        Files.writeString(python, "#!/bin/sh\n");
        Path home = packageDir.resolve("home");
        Path node = home.resolve("tools/node-test/bin/node");
        Files.createDirectories(node.getParent());
        Files.writeString(node, "#!/bin/sh\n");
        Files.setPosixFilePermissions(node, PosixFilePermissions.fromString("rwx------"));
        Path workspace = packageDir.resolve("workspace");
        Files.createDirectories(workspace);

        TerminalTarget target = new HermesTui().target(packageDir, home, workspace, "hermes-1", "session-42");

        assertEquals("hermes", target.brand());
        assertEquals(node.toString(), target.command().get(0));
        assertEquals("--expose-gc", target.command().get(1));
        assertEquals(entry.toString(), target.command().get(2));
        assertEquals(workspace, target.cwd());
        assertEquals(home.toString(), target.environment().get("HOME"));
        assertEquals(home.toString(), target.environment().get("HERMES_HOME"));
        assertEquals(python.toString(), target.environment().get("HERMES_PYTHON"));
        assertEquals(packageDir.toString(), target.environment().get("HERMES_PYTHON_SRC_ROOT"));
        assertEquals(workspace.toString(), target.environment().get("HERMES_CWD"));
        assertEquals("session-42", target.environment().get("HERMES_TUI_RESUME"));
        assertTrue(target.environment().get("PATH").startsWith(node.getParent().toString()),
                target.environment().get("PATH"));
    }

    @Test
    void hermesWithoutTheTuiBundleFailsLoudly() throws Exception {
        Path python = packageDir.resolve(".venv/bin/python");
        Files.createDirectories(python.getParent());
        Files.writeString(python, "#!/bin/sh\n");
        IOException expected = assertThrows(IOException.class, () -> new HermesTui().target(
                packageDir, packageDir.resolve("home"), packageDir.resolve("workspace"), "hermes-1", null));
        assertTrue(expected.getMessage().contains("Hermes TUI entry missing"), expected.getMessage());
    }
}
