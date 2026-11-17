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
package org.jackhuang.hmcl.web.zcode;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/// The source edits the experimental ZCode category applies before building a
/// distribution.
///
/// ZCode's web client writes its root path into everything it emits — `/assets`
/// in the built HTML, `/api/server-info`, `wss://host/ws`, the OAuth token URL —
/// so an instance served by the panel's `/i/<id>/` proxy would ask the panel's
/// own root for its bundle and its API. Four small edits fix that:
///
/// - `vite.config.ts` gets `base: "./"`, so the HTML refers to its bundle
///   relatively and the proxy's injected `<base href="/i/<id>/">` resolves it;
/// - `main.tsx` gets a `hdslBasePath()` helper that reads the `/i/<id>/` prefix
///   off the browser's own path, and the WebSocket origin plus the
///   `/api/server-info` fetch go through it;
/// - the Z.ai OAuth config's `tokenUrl` gets the same prefix.
///
/// These are plain find/replace edits with a **unique-anchor requirement**, not
/// a `patch(1)` file: when upstream moves one of the anchors the build fails
/// with a message naming the file, instead of producing a distribution that
/// silently serves the wrong URLs. Keeping the patch honest is the whole point —
/// see `docs/zcode-experimental.md`.
public final class ZcodePatch {

    private ZcodePatch() {
    }

    /// One edit: `find` must occur exactly once in `file`.
    private record Edit(String file, String find, String replace) {
    }

    private static final List<Edit> EDITS = List.of(
            new Edit("packages/web/vite.config.ts",
                    "  return {\n    plugins: [pdfJsCMapsPlugin(), react(), tailwindcss(), thirdPartyNoticesVitePlugin()],",
                    "  return {\n"
                            + "    // HDSL-web patch: relative asset URLs; the panel's proxy injects the <base> tag\n"
                            + "    // that resolves them under /i/<id>/.\n"
                            + "    base: \"./\",\n"
                            + "    plugins: [pdfJsCMapsPlugin(), react(), tailwindcss(), thirdPartyNoticesVitePlugin()],"),

            new Edit("packages/web/src/main.tsx",
                    "function resolveDefaultWsOrigin(): string {",
                    "// HDSL-web patch: the panel serves this app under /i/<id>/, so every URL the\n"
                            + "// browser builds for its own server has to carry that prefix.\n"
                            + "function hdslBasePath(): string {\n"
                            + "  const match = window.location.pathname.match(/^\\/i\\/[^/]+\\//);\n"
                            + "  return match ? match[0].slice(0, -1) : \"\";\n"
                            + "}\n"
                            + "\n"
                            + "function resolveDefaultWsOrigin(): string {"),

            new Edit("packages/web/src/main.tsx",
                    "  return `${window.location.protocol === \"https:\" ? \"wss:\" : \"ws:\"}//${window.location.host}`;",
                    "  return `${window.location.protocol === \"https:\" ? \"wss:\" : \"ws:\"}//${window.location.host}${hdslBasePath()}`;"),

            new Edit("packages/web/src/main.tsx",
                    "const response = await fetch(\"/api/server-info\", {",
                    "const response = await fetch(`${hdslBasePath()}/api/server-info`, {"),

            new Edit("packages/web/src/auth/webZaiOAuthConfig.ts",
                    "    tokenUrl: \"/api/v1/oauth/token\",",
                    "    tokenUrl: (window.location.pathname.match(/^\\/i\\/[^/]+/) ?? [\"\"])[0]"
                            + " + \"/api/v1/oauth/token\","),

            // Not a proxy edit: the build script spawns `pnpm` (and `tar`) by bare
            // name, and Windows has no file called either — they exist as
            // `pnpm.cmd` / `tar.exe`, and CreateProcess resolves neither through
            // PATHEXT without a shell. Measured on Windows 10: `pnpm install` and
            // `tsc -b` finish, then `spawnSync pnpm ENOENT` kills `build:zcode`.
            // `shell` is false off Windows, so the POSIX build is unchanged.
            new Edit("scripts/build-zcode.mjs",
                    "  const result = spawnSync(command, args, {\n    cwd: root,",
                    "  const result = spawnSync(command, args, {\n"
                            + "    // HDSL-web patch: bare `pnpm`/`tar` are not files on Windows; cmd.exe\n"
                            + "    // (through PATHEXT) is what finds `pnpm.cmd` there.\n"
                            + "    shell: process.platform === \"win32\",\n"
                            + "    cwd: root,")
    );

    /// Applies every edit to the checked-out sources.
    ///
    /// @param sourceRoot the extracted ZCode source tree
    /// @throws ZcodeException when a file is missing, unreadable, or no longer
    /// contains the anchor the edit relies on — upstream moved it
    public static void apply(Path sourceRoot) throws ZcodeException {
        for (Edit edit : EDITS) {
            Path file = sourceRoot.resolve(edit.file());
            String text;
            try {
                text = Files.readString(file, StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new ZcodeException("the ZCode patch cannot read " + edit.file()
                        + " — the sources are not the layout this patch was written for", e);
            }
            int first = text.indexOf(edit.find());
            if (first < 0 || text.indexOf(edit.find(), first + 1) >= 0) {
                throw new ZcodeException("the ZCode patch no longer applies to " + edit.file()
                        + " (anchor missing or ambiguous) — upstream changed it; update ZcodePatch"
                        + " and docs/zcode-experimental.md");
            }
            try {
                Files.writeString(file, text.replace(edit.find(), edit.replace()), StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new ZcodeException("the ZCode patch could not write " + edit.file(), e);
            }
        }
    }

    /// How many edits [apply] performs — for tests and diagnostics.
    public static int size() {
        return EDITS.size();
    }
}
