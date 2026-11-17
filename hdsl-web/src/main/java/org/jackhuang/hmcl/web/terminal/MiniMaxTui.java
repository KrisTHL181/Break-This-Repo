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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// MiniMax Code's interactive TUI: `node <cli.js> [--session <id>]`.
///
/// The published package exposes `cli.js` at its root. User data defaults to
/// `~/.minimax`; `MINIMAX_DATA_DIR` and its legacy alias `MAVIS_DATA_DIR` are
/// pinned to the instance home so a shared host cannot leak accounts or
/// sessions between instances.
@NotNullByDefault
public final class MiniMaxTui implements BrandTui {

    @Override
    public TerminalTarget target(Path packageDir, Path home, Path workspace,
                                 String instanceId, @Nullable String resumeSession) throws IOException {
        Path entry = packageDir.resolve("node_modules/@minimax-ai/code/cli.js");
        if (!Files.isRegularFile(entry)) {
            Path shim = Executables.resolve(
                    packageDir.resolve("node_modules/.bin/mcode").toString()).orElse(null);
            String name = shim == null ? "" : shim.getFileName().toString();
            if (!name.endsWith(".js") && !name.endsWith(".mjs")) {
                throw new IOException("mcode entry point missing under " + packageDir
                        + " (expected node_modules/@minimax-ai/code/cli.js)");
            }
            entry = shim;
        }

        DshNodeRuntime runtime = BrandTui.node(packageDir, Brand.MINIMAX);
        Map<String, String> environment = new HashMap<>(BrandTui.environment(home));
        environment.put("MINIMAX_DATA_DIR", home.resolve(".minimax").toString());
        environment.put("MAVIS_DATA_DIR", home.resolve(".minimax").toString());
        BrandTui.applyNode(environment, runtime);

        List<String> command = new ArrayList<>(List.of(runtime.node().toString(), entry.toString()));
        if (resumeSession != null && !resumeSession.isBlank()) {
            command.add("--session");
            command.add(resumeSession);
        }
        return new TerminalTarget("minimax", instanceId, command, workspace, Map.copyOf(environment));
    }
}
