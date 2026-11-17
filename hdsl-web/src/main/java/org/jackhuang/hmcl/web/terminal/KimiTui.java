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
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// Kimi Code's interactive TUI: `node <dist/main.mjs> [--resume <id>]`.
///
/// pnpm installs `.bin/kimi` as a shell shim, not a symlink, so the adapter
/// starts the package's own `dist/main.mjs` with Node directly. Starting
/// through the shim would put `/bin/sh` (and `env`) in the process tree and
/// make the shim, not the agent, the root process.
///
/// Sessions live under `<HOME>/.kimi-code/sessions`, which the instance home
/// supplies — so the TUI lists exactly the sessions `kimi web` wrote.
@NotNullByDefault
public final class KimiTui implements BrandTui {

    /// The variable kimi-code reads for the shell its own tools run in.
    ///
    /// On Windows it does not look for a bash by itself: without this it refuses
    /// with `failed to start shell: Git Bash was not found on this Windows host`
    /// the first time the agent wants to run a command. The panel is the one that
    /// can see the machine, so it says where Git for Windows is.
    static final String SHELL_VARIABLE = "KIMI_SHELL_PATH";

    /// Points kimi-code at a Git Bash, unless the operator already said which shell
    /// to use (their own `KIMI_SHELL_PATH`, from the panel's environment or the
    /// instance's).
    ///
    /// A no-op on every other platform, and on a Windows machine without Git for
    /// Windows: there is nothing to point at, and guessing at some other bash is
    /// how an agent ends up running WSL.
    ///
    /// @param environment the child's environment, modified in place
    public static void applyShell(Map<String, String> environment) {
        org.jackhuang.hmcl.util.platform.GitBash.applyTo(environment, SHELL_VARIABLE);
    }

    @Override
    public TerminalTarget target(Path packageDir, Path home, Path workspace,
                                 String instanceId, @Nullable String resumeSession) throws IOException {
        // pnpm does not link `.bin/kimi` to the JS file: it writes a shell
        // shim (`#!/bin/sh`) and leaves the real entry in the package. Start
        // from the package's own `bin` target and only fall back to the shim
        // when it happens to be an npm symlink to a JS file.
        Path entry = packageDir.resolve("node_modules/@moonshot-ai/kimi-code/dist/main.mjs");
        if (!Files.isRegularFile(entry)) {
            Path shim = Executables.resolve(packageDir.resolve("node_modules/.bin/kimi").toString())
                    .orElse(null);
            String name = shim == null ? "" : shim.getFileName().toString();
            if (!name.endsWith(".mjs") && !name.endsWith(".js")) {
                throw new IOException("kimi entry point missing under " + packageDir
                        + " (expected node_modules/@moonshot-ai/kimi-code/dist/main.mjs)");
            }
            entry = shim;
        }
        // The panel may run without node on its own PATH (the managed runtime
        // lives under <data>/hdsl/runtimes); prefer that runtime, then fall
        // back to the system PATH. "Which node" is a version question too: kimi-code
        // 2.4.1 imports `createZstdDecompress` from `node:zlib` (Node 22.15+), and a
        // distribution Node 20 dies inside the ESM loader with a message about the module
        // rather than the version — so a supported runtime wins even when PATH has one.
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

        List<String> command = new ArrayList<>(List.of(node.toString(), entry.toString()));
        if (resumeSession != null && !resumeSession.isBlank()) {
            command.add("--resume");
            command.add(resumeSession);
        }
        applyShell(environment);
        return new TerminalTarget("kimi", instanceId, command, workspace, Map.copyOf(environment));
    }
}
