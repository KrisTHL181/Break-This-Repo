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
import org.jackhuang.hmcl.web.brand.Brand;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// The Pi Coding Agent's own TUI, from the Pi SDK that a PI WEB release ships.
///
/// `@earendil-works/pi-coding-agent` publishes `pi` as `dist/bundle/cli.js`, so
/// the adapter starts that file with Node directly: starting the `.bin/pi` shim
/// would put `/bin/sh` in the process tree and make the shim, not the agent, the
/// root process.
///
/// The instance home is the child's `HOME`, which is also what PI WEB's daemon
/// gets — so `~/.pi/agent` is one agent directory, and the terminal lists the
/// sessions the web wrote (and the other way round). This is the whole point of
/// the terminal-only and terminal-and-web forms: the panel is a window onto the
/// same Pi, not a second one.
///
/// Session resume is deliberately not wired: the Pi CLI's resume flag is not a
/// contract this panel has verified, and passing an unverified one would turn
/// "open the terminal" into an error. The parameter is ignored until it is.
@NotNullByDefault
public final class PiTui implements BrandTui {

    @Override
    public TerminalTarget target(Path packageDir, Path home, Path workspace,
                                 String instanceId, @Nullable String resumeSession) throws IOException {
        Path entry = packageDir.resolve("node_modules")
                .resolve(Brand.PI_SDK_PACKAGE)
                .resolve("dist/bundle/cli.js");
        if (!Files.isRegularFile(entry)) {
            throw new IOException("Pi entry point missing under " + packageDir
                    + " (expected node_modules/" + Brand.PI_SDK_PACKAGE + "/dist/bundle/cli.js)");
        }
        // The panel's own PATH does not necessarily carry a Node (the managed
        // runtime lives under the data directory), and the Pi SDK's engine floor
        // is the same one the web half was installed against.
        DshNodeRuntime runtime = DshNodeRuntime.detectSupported()
                .or(DshNodeRuntime::detectManaged)
                .orElse(null);
        Map<String, String> environment = new HashMap<>(BrandTui.environment(home));
        Path node;
        if (runtime != null) {
            node = runtime.node();
            environment.put("PATH",
                    node.getParent().toString() + File.pathSeparator
                            + environment.getOrDefault("PATH", ""));
        } else {
            node = Executables.resolve("node")
                    .orElseThrow(() -> new IOException(
                            "node is neither managed nor on PATH; install a Node runtime first"));
        }
        List<String> command = List.of(node.toString(), entry.toString());
        return new TerminalTarget("pi", instanceId, command, workspace, Map.copyOf(environment));
    }
}
