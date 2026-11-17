/*
 * HDSL
 * Copyright (C) 2026  HDSL contributors
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
package org.jackhuang.hmcl.dsh;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/// Tests for working out what an instance's profile actually boots.
///
/// This is the difference between an instance that starts and an instance that appears to do
/// nothing: the surface decides whether a port is reserved, whether `--no-open` is passed, and
/// whether the launcher waits for a readiness line and opens a browser. Answering it from the
/// profile's *name* is right until somebody writes a pack, because a pack's profile is named after
/// the pack — `pokemon`, `codex` — and boots the browser app all the same.
class DshSurfaceTest {
    /// Writes a profile manifest naming the given bundles.
    ///
    /// @param home    the `DSH_HOME`
    /// @param profile the profile
    /// @param bundles the bundles
    private static void writeProfile(Path home, String profile, List<String> bundles) throws Exception {
        Path directory = home.resolve("profiles").resolve(profile);
        Files.createDirectories(directory);
        StringBuilder json = new StringBuilder(
                "{\"name\":\"dsh-profile-" + profile + "\",\"dsh\":{\"profile\":{\"bundles\":[");
        for (int i = 0; i < bundles.size(); i++) {
            json.append(i == 0 ? "" : ",").append('"').append(bundles.get(i)).append('"');
        }
        json.append("]}}}");
        Files.writeString(directory.resolve("package.json"), json.toString());
    }

    /// Builds an instance whose home is a temporary directory.
    ///
    /// @param home    the home
    /// @param profile the profile
    /// @return the instance
    private static DshInstance instance(Path home, String profile) {
        return new DshInstance("test", "0.1.6-alpha.2", profile, home.toString(), "system",
                DshHomeMode.CUSTOM, home.toString(), List.of(), Map.of(), null, null, null, 0, 0L);
    }

    @Test
    void aPackProfileNamedAfterThePackStillBootsTheBrowser() throws Exception {
        // The reported case: a pack's profile is called `pokemon` and layers the web app.
        Path home = Files.createTempDirectory("home");
        writeProfile(home, "pokemon", List.of("@deepseek-ai/dsh-base",
                "@deepseek-ai/dsh-web-app", "dsh-wildmon"));

        assertEquals(DshSurface.WEB, DshSurface.of(instance(home, "pokemon")));
    }

    @Test
    void theAppDecidesAndNotTheName() throws Exception {
        // The converse, which is what makes the rule a rule rather than a special case for packs:
        // a profile *called* `web` that does not layer the web app is not the browser surface.
        Path home = Files.createTempDirectory("home");
        writeProfile(home, "web", List.of("@deepseek-ai/dsh-base", "@deepseek-ai/dsh-headless"));

        assertEquals(DshSurface.HEADLESS, DshSurface.of(instance(home, "web")));
    }

    @Test
    void eachAppIsRecognisedByItsBundle() {
        assertEquals(DshSurface.WEB,
                DshSurface.ofBundles(List.of("@deepseek-ai/dsh-web-app")));
        assertEquals(DshSurface.HEADLESS,
                DshSurface.ofBundles(List.of("dsh-headless")));
        assertEquals(DshSurface.ACP,
                DshSurface.ofBundles(List.of("@deepseek-ai/dsh-acp-app")));
        assertEquals(DshSurface.SDK,
                DshSurface.ofBundles(List.of("@deepseek-ai/dsh-sdk-app")));
        // Addressed without a scope, which is how a pack may name it.
        assertEquals(DshSurface.WEB, DshSurface.ofBundles(List.of("dsh-web-app")));
        // A profile with plugins and no app is not a surface.
        assertEquals(DshSurface.OTHER, DshSurface.ofBundles(List.of("@deepseek-ai/dsh-base", "dsh-pets")));
        assertEquals(DshSurface.OTHER, DshSurface.ofBundles(List.of()));
    }

    @Test
    void aProfileWithNoManifestFallsBackToItsName() throws Exception {
        // An instance made but not yet installed into has no profile manifest at all, and the name
        // is then the only thing there is to go on.
        Path home = Files.createTempDirectory("home");
        assertEquals(DshSurface.WEB, DshSurface.of(instance(home, "web")));
        assertEquals(DshSurface.OTHER, DshSurface.of(instance(home, "pokemon")));
    }
}
