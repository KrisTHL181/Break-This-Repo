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
import org.jackhuang.hmcl.util.platform.OperatingSystem;
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

/// MiMo Code's interactive TUI: the downloaded native binary when present,
/// otherwise its Node launcher `node <mimo.js> [--session <id>]`.
///
/// The npm package is a thin launcher: `postinstall` (best effort) and the
/// first run fetch the platform binary into `vendor/`. Starting the binary
/// directly keeps Node out of the process tree when it is already there; the
/// launcher remains the fallback for an install that has not fetched it yet.
@NotNullByDefault
public final class MimoTui implements BrandTui {

    @Override
    public TerminalTarget target(Path packageDir, Path home, Path workspace,
                                 String instanceId, @Nullable String resumeSession) throws IOException {
        Path packageRoot = packageDir.resolve("node_modules/@mimo-ai/cli");
        Path binary = packageRoot.resolve("vendor")
                .resolve(OperatingSystem.CURRENT_OS == OperatingSystem.WINDOWS ? "mimo.exe" : "mimo");

        DshNodeRuntime runtime = BrandTui.node(packageDir, Brand.MIMO);
        Map<String, String> environment = new HashMap<>(BrandTui.environment(home));
        environment.put("MIMOCODE_HOME", home.resolve(".mimocode").toString());
        environment.put("MIMOCODE_CONFIG_DIR", home.resolve(".mimocode").toString());
        BrandTui.applyNode(environment, runtime);

        List<String> command;
        // On Windows the launcher stages a copy of the binary outside the
        // package before running it: a running PE locks its own file, which
        // would block an upgrade or uninstall. Starting `vendor/mimo.exe` in
        // place bypasses that, so Windows always goes through `mimo.js`.
        if (OperatingSystem.CURRENT_OS != OperatingSystem.WINDOWS && Files.isRegularFile(binary)) {
            command = new ArrayList<>(List.of(binary.toString()));
        } else {
            Path launcher = packageRoot.resolve("mimo.js");
            if (!Files.isRegularFile(launcher)) {
                throw new IOException("mimo entry point missing under " + packageDir
                        + " (expected node_modules/@mimo-ai/cli/mimo.js or vendor/mimo)");
            }
            command = new ArrayList<>(List.of(runtime.node().toString(), launcher.toString()));
        }
        if (resumeSession != null && !resumeSession.isBlank()) {
            command.add("--session");
            command.add(resumeSession);
        }
        return new TerminalTarget("mimo", instanceId, command, workspace, Map.copyOf(environment));
    }
}
