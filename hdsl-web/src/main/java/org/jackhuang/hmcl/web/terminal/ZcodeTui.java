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

import org.jackhuang.hmcl.dsh.DshNodeRuntime;
import org.jackhuang.hmcl.util.platform.Executables;
import org.jackhuang.hmcl.util.platform.OperatingSystem;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.jackhuang.hmcl.util.logging.Logger.LOG;

/// ZCode's interactive TUI: `node <release>/bin/zcode.mjs`, nothing else.
///
/// The distribution starts its TUI when no `--web` is passed; the web mode's
/// flags (`--web --no-open --host --port --no-token`) must not leak in here,
/// or the terminal would start a second web server inside a PTY.
///
/// The TUI must land on exactly the state the web mode writes, and the
/// distribution splits that state over two roots:
///
/// - `ZCODE_DATA_BASE_DIR` holds the credentials and the provider config
///   (`<base>/.zcode/v2/credentials.json`, `…/provider_config.json` — the file
///   [org.jackhuang.hmcl.web.zcode.ZcodeInstanceManager] writes), so it points
///   at the instance's `data/` directory, exactly as in the web launch;
/// - the session database follows `homedir()` (`<home>/.zcode/cli/db/db.sqlite`,
///   see `apps/zcode-cli/packages/adapters/src/storage/session-store/paths.ts`),
///   so `HOME` points at the instance's `home/` directory — the same one the web
///   launch passes, which is what keeps the two surfaces on one set of sessions.
///   Pointing it at the panel's own home instead is what used to put the
///   database on the container's writable layer, where a rebuild erased it; see
///   [org.jackhuang.hmcl.web.zcode.ZcodeInstanceManager#prepareHome].
///
/// `--workspace` is deliberately not passed: the working directory already is
/// the instance workspace, and the runner only understands that flag in web
/// mode (`scripts/zcode-distribution/runner.mjs`).
@NotNullByDefault
public final class ZcodeTui {

    /// The brand id the protocol, the tickets and the audit trail use.
    public static final String BRAND = "zcode";

    /// Builds the target for one ZCode instance.
    ///
    /// @param packageDir the distribution holding `bin/zcode.mjs`
    /// @param dataDir    the instance's data directory (`ZCODE_DATA_BASE_DIR`)
    /// @param homeDir    the instance's home directory (`HOME`, where the session
    ///                   database lives — must be the one the web launch uses)
    /// @param workspace  the instance workspace (the child's cwd)
    /// @param instanceId the instance id
    /// @param nodePath   the configured Node executable, may be blank
    /// @return the argv, cwd and environment to spawn
    /// @throws IOException when the distribution has no entry point or no Node can be resolved
    public static TerminalTarget target(Path packageDir, Path dataDir, Path homeDir, Path workspace,
                                        String instanceId, @Nullable String nodePath) throws IOException {
        Path entry = packageDir.resolve("bin/zcode.mjs");
        if (!Files.isRegularFile(entry)) {
            throw new IOException("ZCode distribution has no bin/zcode.mjs: " + packageDir);
        }
        // Without an argument the runner imports the agent, which is the TUI.
        // A distribution built for the web only has the server and the web
        // assets; starting it would fail inside the PTY instead of here.
        Path agent = packageDir.resolve("agent/zcode.cjs");
        if (!Files.isRegularFile(agent)) {
            throw new IOException("ZCode distribution has no agent/zcode.cjs (no TUI): " + packageDir
                    + " — rebuild it with pnpm build:zcode");
        }
        Path node = resolveNode(nodePath);
        Map<String, String> environment = new HashMap<>(System.getenv());
        // The session database follows HOME, so it has to be the instance's own
        // home — the same one the web process gets, or the terminal would list a
        // different set of sessions.
        environment.put("ZCODE_DATA_BASE_DIR", dataDir.toString());
        environment.put("HOME", homeDir.toString());
        if (OperatingSystem.CURRENT_OS == OperatingSystem.WINDOWS) {
            // Node resolves `~` through USERPROFILE on Windows and ignores HOME.
            environment.put("USERPROFILE", homeDir.toString());
        }
        environment.put("TERM", "xterm-256color");
        environment.put("COLORTERM", "truecolor");
        environment.put("PATH", node.getParent() + File.pathSeparator
                + environment.getOrDefault("PATH", ""));
        return new TerminalTarget(BRAND, instanceId, List.of(node.toString(), entry.toString()),
                workspace, Map.copyOf(environment));
    }

    /// The Node the panel was told to use (`zcode.nodePath`), else `node` from
    /// `PATH`.
    ///
    /// A **bare name** is looked up on `PATH` — the same treatment the web
    /// runtime gives it, Windows `.exe`/`.cmd` completion included. Requiring a
    /// file here is what broke the default: `ZcodeApiServlet` passes the
    /// configured value through with `node` as its default, and `node` is a
    /// name, not a path, so every ZCode terminal failed with
    /// "zcode.nodePath does not point at a file: node".
    ///
    /// @param configured the configured Node, may be blank
    /// @return a program that can actually be started
    /// @throws IOException when nothing startable can be resolved
    public static Path resolveNode(@Nullable String configured) throws IOException {
        return resolveNode(configured,
                () -> Executables.resolve("node"),
                () -> DshNodeRuntime.detectManaged().map(DshNodeRuntime::node));
    }

    /// The same decision with its two lookups passed in, so a machine's own state
    /// (does it have a Node? does the panel have a managed one?) is not what the
    /// test depends on.
    ///
    /// The order is the point: what the operator configured, then the system's own
    /// `node`, then the runtime the panel installed under `<data>/hdsl/runtimes`.
    /// The last one is what a bare Windows deployment has — the panel installs it
    /// so nobody has to install Node — and skipping it is why a ZCode instance used
    /// to fail with `CreateProcess error=2` while a perfectly good runtime sat in
    /// the data directory.
    ///
    /// @param configured the configured Node, may be blank
    /// @param onPath     what `node` on `PATH` resolves to
    /// @param managed    the panel's own runtime, if one is installed
    /// @return a program that can actually be started
    /// @throws IOException when nothing startable can be resolved
    static Path resolveNode(@Nullable String configured,
                            java.util.function.Supplier<java.util.Optional<Path>> onPath,
                            java.util.function.Supplier<java.util.Optional<Path>> managed)
            throws IOException {
        if (configured != null && !configured.isBlank()) {
            return Executables.resolve(configured).orElseThrow(() -> new IOException(
                    "zcode.nodePath does not point at a startable program: " + configured));
        }
        java.util.Optional<Path> system = onPath.get();
        if (system.isPresent()) {
            return system.get();
        }
        java.util.Optional<Path> panel = managed.get();
        if (panel.isPresent()) {
            LOG.info("ZCode: no node on PATH; using the panel's managed runtime " + panel.get());
            return panel.get();
        }
        throw new IOException("Node.js 不在 PATH 上，面板也没有托管运行时；"
                + "到「设置 → Node.js」一键安装（或安装 Node.js LTS 后重启面板）");
    }
}