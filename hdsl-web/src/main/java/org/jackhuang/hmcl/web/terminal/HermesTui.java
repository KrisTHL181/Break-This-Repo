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
import org.jackhuang.hmcl.util.platform.Privileges;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/// Hermes Agent's interactive TUI: `node --expose-gc hermes_cli/tui_dist/entry.js`.
///
/// The release ships the prebuilt Ink bundle the dashboard's chat tab also
/// runs. The adapter starts it with the instance's own Node when the installer
/// left one under `home/tools`, falling back to the panel's managed runtime.
///
/// The TUI's gateway bootstrap starts the gateway itself; the environment names
/// the same home and runtime directories the web mode uses, so the sessions
/// written here are the ones the dashboard lists.
@NotNullByDefault
public final class HermesTui implements BrandTui {

    @Override
    public TerminalTarget target(Path packageDir, Path home, Path workspace,
                                 String instanceId, @Nullable String resumeSession) throws IOException {
        Path entry = packageDir.resolve("hermes_cli/tui_dist/entry.js");
        if (!Files.isRegularFile(entry)) {
            throw new IOException("Hermes TUI entry missing: " + entry);
        }
        Path python = packageDir.resolve(".venv/bin/python");
        if (!Files.isRegularFile(python)) {
            throw new IOException("Hermes Python interpreter missing: " + python);
        }
        Path node = resolveNode(home);

        // The terminal can be the first thing an instance runs, before the web
        // plan has created these; the TUI's gateway bootstrap refuses a home
        // owned by root, so prepare them as the agent user here too. The
        // create-then-hand-over rule is Privileges#prepareAgentDirectory.
        Path panel = home.resolve("panel");
        Path locks = panel.resolve("locks");
        Path run = panel.resolve("run");
        Path tools = home.resolve("tools");
        Privileges.prepareAgentDirectory(home, Privileges.AGENT_SHARED_DIRECTORY);
        Privileges.prepareAgentDirectory(panel, Privileges.AGENT_PRIVATE_DIRECTORY);
        Privileges.prepareAgentDirectory(locks, Privileges.AGENT_PRIVATE_DIRECTORY);
        Privileges.prepareAgentDirectory(run, Privileges.AGENT_PRIVATE_DIRECTORY);
        Privileges.prepareAgentDirectory(tools, Privileges.AGENT_SHARED_DIRECTORY);

        Map<String, String> environment = new HashMap<>(BrandTui.environment(home));
        environment.put("HERMES_HOME", home.toString());
        environment.put("HERMES_RUNTIME_DIR", home.resolve("tools").toString());
        environment.put("HERMES_GATEWAY_LOCK_DIR", home.resolve("panel/locks").toString());
        environment.put("XDG_RUNTIME_DIR", home.resolve("panel/run").toString());
        environment.put("PYTHONDONTWRITEBYTECODE", "1");
        environment.put("HERMES_PYTHON", python.toString());
        environment.put("HERMES_PYTHON_SRC_ROOT", packageDir.toString());
        environment.put("PATH",
                node.getParent().toString() + File.pathSeparator
                        + environment.getOrDefault("PATH", ""));
        // The TUI's session.create starts the agent in this directory, and the
        // child copy keeps a resumed session's own cwd out of the way.
        environment.put("HERMES_CWD", workspace.toString());
        environment.put("HERMES_TUI_CWD", workspace.toString());
        if (resumeSession != null && !resumeSession.isBlank()) {
            environment.put("HERMES_TUI_RESUME", resumeSession);
        }
        return new TerminalTarget("hermes", instanceId,
                List.of(node.toString(), "--expose-gc", entry.toString()),
                workspace, Map.copyOf(environment));
    }

    /// The Node the installer left in the instance, then the panel's managed
    /// runtime, then whatever is on PATH.
    private static Path resolveNode(Path home) throws IOException {
        Path tools = home.resolve("tools");
        if (Files.isDirectory(tools)) {
            try (Stream<Path> entries = Files.list(tools)) {
                Path bundled = entries
                        .filter(Files::isDirectory)
                        .filter(path -> path.getFileName().toString().startsWith("node-"))
                        .sorted(Comparator.reverseOrder())
                        .map(path -> path.resolve("bin/node"))
                        .filter(Files::isRegularFile)
                        .findFirst()
                        .orElse(null);
                if (bundled != null) {
                    return bundled;
                }
            }
        }
        DshNodeRuntime runtime = DshNodeRuntime.detectSupported()
                .or(DshNodeRuntime::detectManaged)
                .orElse(null);
        if (runtime != null) {
            return runtime.node();
        }
        return Executables.resolve("node")
                .orElseThrow(() -> new IOException(
                        "node is neither bundled with the Hermes instance nor on PATH"));
    }
}
