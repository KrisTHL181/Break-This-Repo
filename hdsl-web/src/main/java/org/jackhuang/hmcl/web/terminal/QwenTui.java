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

/// Qwen Code's interactive TUI: `node <cli-entry.js> [--resume <id>]`.
///
/// The published package exposes `cli-entry.js` at its root. Sessions live
/// under `$QWEN_HOME` (default `~/.qwen`), which the instance home supplies —
/// and the explicit variable keeps the TUI and `qwen serve` inside the
/// instance even when a launcher has already exported one.
@NotNullByDefault
public final class QwenTui implements BrandTui {

    @Override
    public TerminalTarget target(Path packageDir, Path home, Path workspace,
                                 String instanceId, @Nullable String resumeSession) throws IOException {
        Path entry = packageDir.resolve("node_modules/@qwen-code/qwen-code/cli-entry.js");
        if (!Files.isRegularFile(entry)) {
            entry = packageDir.resolve("node_modules/@qwen-code/qwen-code/dist/index.js");
        }
        if (!Files.isRegularFile(entry)) {
            Path shim = Executables.resolve(
                    packageDir.resolve("node_modules/.bin/qwen").toString()).orElse(null);
            String name = shim == null ? "" : shim.getFileName().toString();
            if (!name.endsWith(".js") && !name.endsWith(".mjs")) {
                throw new IOException("qwen entry point missing under " + packageDir
                        + " (expected node_modules/@qwen-code/qwen-code/cli-entry.js)");
            }
            entry = shim;
        }

        DshNodeRuntime runtime = BrandTui.node(packageDir, Brand.QWEN);
        Map<String, String> environment = new HashMap<>(BrandTui.environment(home));
        environment.put("QWEN_HOME", home.resolve(".qwen").toString());
        BrandTui.applyNode(environment, runtime);

        List<String> command = new ArrayList<>(List.of(runtime.node().toString(), entry.toString()));
        if (resumeSession != null && !resumeSession.isBlank()) {
            command.add("--resume");
            command.add(resumeSession);
        }
        return new TerminalTarget("qwen", instanceId, command, workspace, Map.copyOf(environment));
    }
}
