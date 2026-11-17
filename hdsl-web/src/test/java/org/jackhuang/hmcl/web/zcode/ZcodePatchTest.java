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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The ZCode build's source patch: every anchor it edits really moves, and an
/// anchor upstream has moved fails the build instead of producing a
/// distribution that asks the panel's own root for its bundle.
class ZcodePatchTest {

    @TempDir
    Path dir;

    @Test
    void appliesEveryEditToTheFakeSources() throws Exception {
        Path source = TestTrees.copy(Path.of("src/test/resources/fake-zcode-source"), dir.resolve("src"));

        ZcodePatch.apply(source);

        String vite = Files.readString(source.resolve("packages/web/vite.config.ts"));
        assertTrue(vite.contains("base: \"./\","), vite);

        String main = Files.readString(source.resolve("packages/web/src/main.tsx"));
        assertTrue(main.contains("function hdslBasePath(): string {"), main);
        assertTrue(main.contains("${window.location.host}${hdslBasePath()}"), main);
        assertTrue(main.contains("fetch(`${hdslBasePath()}/api/server-info`"), main);

        String oauth = Files.readString(source.resolve("packages/web/src/auth/webZaiOAuthConfig.ts"));
        assertTrue(oauth.contains("window.location.pathname.match(/^\\/i\\/[^/]+/)"), oauth);
        assertTrue(oauth.contains("/api/v1/oauth/token"), oauth);
        assertTrue(oauth.contains("[\"\"])[0] +"), oauth);

        // The build script spawns `pnpm` by bare name; on Windows only a shell
        // resolves that to `pnpm.cmd`, so the edit has to keep the guard.
        String build = Files.readString(source.resolve("scripts/build-zcode.mjs"));
        assertTrue(build.contains("shell: process.platform === \"win32\","), build);
    }

    @Test
    void refusesToPatchWhenAnAnchorMoved() throws Exception {
        Path source = TestTrees.copy(Path.of("src/test/resources/fake-zcode-source"), dir.resolve("moved"));
        Path main = source.resolve("packages/web/src/main.tsx");
        Files.writeString(main, Files.readString(main)
                .replace("function resolveDefaultWsOrigin", "function renamedWsOrigin"));

        ZcodeException failure = assertThrows(ZcodeException.class, () -> ZcodePatch.apply(source));
        assertTrue(failure.getMessage().contains("no longer applies"), failure.getMessage());
        assertTrue(failure.getMessage().contains("main.tsx"), failure.getMessage());
    }

    @Test
    void refusesASourceTreeThatIsNotZCode() {
        ZcodeException failure = assertThrows(ZcodeException.class, () -> ZcodePatch.apply(dir));
        assertTrue(failure.getMessage().contains("cannot read"), failure.getMessage());
    }

    @Test
    void reportsHowManyEditsItPerforms() {
        assertEquals(6, ZcodePatch.size());
    }

    /// Copies a directory tree — the test resources are read-only fixtures.
    static final class TestTrees {

        private TestTrees() {
        }

        static Path copy(Path from, Path to) throws IOException {
            try (var walk = Files.walk(from)) {
                for (Path path : walk.toList()) {
                    Path target = to.resolve(from.relativize(path).toString());
                    if (Files.isDirectory(path)) {
                        Files.createDirectories(target);
                    } else {
                        Files.createDirectories(target.getParent());
                        Files.copy(path, target);
                    }
                }
            }
            return to;
        }
    }
}
