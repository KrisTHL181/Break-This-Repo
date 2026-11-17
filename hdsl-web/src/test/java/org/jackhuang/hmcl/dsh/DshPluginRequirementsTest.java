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

import org.jackhuang.hmcl.dsh.DshPluginRequirements;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Verifies the rule that decides whether a plugin is shown for an instance.
///
/// The ranges are the ones plugins really publish, prereleases and alternatives
/// included, and getting them wrong hides plugins from the list — which is why the
/// rule this launcher uses is the one npm itself uses.
class DshPluginRequirementsTest {
    @Test
    void alternativesAreTriedInTurn() {
        // What the marketplace plugin publishes.
        String range = "^0.1.0-rc.7 || ^0.1.1-rc.2 || ^0.1.2-alpha.2";

        assertTrue(DshPluginRequirements.satisfies(range, "0.1.2-alpha.2"));
        assertTrue(DshPluginRequirements.satisfies(range, "0.1.1-rc.2"));
        assertTrue(DshPluginRequirements.satisfies(range, "0.1.0-rc.7"));
        assertTrue(DshPluginRequirements.satisfies(range, "0.1.0-rc.8"), "a later rc of the same line");
        assertTrue(DshPluginRequirements.satisfies(range, "0.1.3"),
                "0.1.3 is a release above the bound, so the caret that names 0.1.2-alpha.2 holds it");
        assertFalse(DshPluginRequirements.satisfies(range, "0.2.0"), "past the line every part of it names");
    }

    @Test
    void aPrereleaseIsNotTheReleaseItLeadsTo() {
        assertFalse(DshPluginRequirements.satisfies("^0.1.2", "0.1.2-alpha.2"),
                "npm does not let a prerelease satisfy a range that does not name one");
        assertTrue(DshPluginRequirements.satisfies("^0.1.2", "0.1.2"));
        assertTrue(DshPluginRequirements.satisfies("^0.1.2-alpha.2", "0.1.2-alpha.3"));
    }

    @Test
    void aPluginFitsWhenEveryJudgeableRequirementIsMet() {
        JsonObject peers = JsonParser.parseString("""
                {"@deepseek-ai/dsh-settings": "^0.1.0-rc.7 || ^0.1.1-rc.2 || ^0.1.2-alpha.2",
                 "@deepseek-ai/cordis": "^4.0.1",
                 "@deepseek-ai/schemastery": "^3.18.1"}
                """).getAsJsonObject();

        assertTrue(DshPluginRequirements.fits(peers, Map.of("@deepseek-ai/dsh-settings", "0.1.2-alpha.2")),
                "a package the instance does not have cannot be judged, and is not held against it");
        assertFalse(DshPluginRequirements.fits(peers, Map.of("@deepseek-ai/dsh-settings", "0.2.0")),
                "a package it does have is judged");
        assertTrue(DshPluginRequirements.fits(null, Map.of()), "a plugin that asks for nothing fits");
    }

    @Test
    void agreeingOnTheNumbersIsNotEnoughWhenThePrereleaseDiffers() {
        // What the remote web UI publishes from 0.4.2 on. The harness refuses the installation
        // over exactly this, so the page must not offer the version as one that fits.
        assertFalse(DshPluginRequirements.satisfies(">=0.1.7-rc.2", "0.1.7-rc.1"));
        assertFalse(DshPluginRequirements.satisfies("^0.1.7-rc.2", "0.1.7-rc.1"));
        assertFalse(DshPluginRequirements.satisfies("~0.1.7-rc.2", "0.1.7-rc.1"));

        assertTrue(DshPluginRequirements.satisfies(">=0.1.7-rc.1", "0.1.7-rc.1"), "the version it names");
        assertTrue(DshPluginRequirements.satisfies(">=0.1.7-rc.2", "0.1.7-rc.2"));
        assertTrue(DshPluginRequirements.satisfies(">=0.1.7-rc.2", "0.1.7-rc.3"));
        assertTrue(DshPluginRequirements.satisfies(">=0.1.7-rc.2", "0.1.7"),
                "a release comes after the prereleases of its own numbers");
        assertTrue(DshPluginRequirements.satisfies("^0.1.7-rc.2", "0.1.7-rc.3"));
    }

    @Test
    void aPrereleaseCanSatisfyARangeThatDoesNotNameOne() {
        // The harness asks npm with `includePrerelease`, so a bound that names no prerelease
        // is about the numbers alone. A launcher that read npm's default rule instead turned
        // away every plugin declaring `>=0.1.0-rc.0 <0.2.0`, which is the commonest of all.
        assertTrue(DshPluginRequirements.satisfies(">=0.1.0-rc.0 <0.2.0", "0.1.7-rc.1"));
        assertTrue(DshPluginRequirements.satisfies("^0.1.0", "0.1.7-rc.1"));
        assertTrue(DshPluginRequirements.satisfies(">=0.1.5", "0.1.7-rc.1"));
        assertTrue(DshPluginRequirements.satisfies(">=1.0.0 <2.0.0", "1.2.0-rc.1"));

        assertFalse(DshPluginRequirements.satisfies(">=0.1.0-rc.0 <0.2.0", "0.2.0"),
                "and the numbers still have to be inside it");
    }

    /// The in-process terminal declares `^0.2.0-rc.1` and the panel gates it with
    /// `>=0.2.0-rc.2 <0.3.0-0`. Both must admit every 0.2.x prerelease — an enumerated
    /// list is what left `dsh@next` without a terminal — and both must stop at 0.3.
    @Test
    void theInProcessTerminalsRangesSpanTheWholeZeroTwoLine() {
        String peers = "^0.2.0-rc.1";
        assertTrue(DshPluginRequirements.satisfies(peers, "0.2.0-rc.2"));
        assertTrue(DshPluginRequirements.satisfies(peers, "0.2.1-rc.5"));
        assertTrue(DshPluginRequirements.satisfies(peers, "0.2.2-rc.3"));
        assertFalse(DshPluginRequirements.satisfies(peers, "0.3.0-rc.1"));

        String gate = ">=0.2.0-rc.2 <0.3.0-0";
        assertTrue(DshPluginRequirements.satisfies(gate, "0.2.1-rc.5"));
        assertTrue(DshPluginRequirements.satisfies(gate, "0.2.2-rc.3"));
        assertFalse(DshPluginRequirements.satisfies(gate, "0.2.0-rc.1"), "below the tested floor");
        assertFalse(DshPluginRequirements.satisfies(gate, "0.3.0"));
    }

    @Test
    void aPrereleaseIsOrderedByNameAndThenByNumber() {
        assertFalse(DshPluginRequirements.satisfies(">=0.1.0-rc.0", "0.1.0-alpha.1"),
                "`alpha` is under `rc`, whatever number follows it");
        assertTrue(DshPluginRequirements.satisfies(">=0.1.0-rc.2", "0.1.0-rc.10"),
                "prerelease numbers are numbers, not text");
    }

    @Test
    void theHarnesssOwnWorkspaceRangesAreSatisfied() {
        // The harness reads these as its own version, whatever that is.
        assertTrue(DshPluginRequirements.satisfies("workspace:*", "0.1.7-rc.1"));
        assertTrue(DshPluginRequirements.satisfies("workspace:^", "0.1.7-rc.1"));
        assertTrue(DshPluginRequirements.satisfies("workspace:~", "0.1.7-rc.1"));

        assertFalse(DshPluginRequirements.satisfies("", "0.1.7-rc.1"),
                "an empty requirement is one the harness refuses outright");
    }

    @Test
    void aBoundThatNamesFewerNumbersStartsBelowTheRelease() {
        // The floor a range makes up is a prerelease of the version it writes, which is what makes
        // `0.1` about the 0.1 line rather than about 0.1.0 onwards, and what lets `*` hold the
        // harness's own prereleases.
        assertTrue(DshPluginRequirements.satisfies("0.1", "0.1.7-rc.1"));
        assertFalse(DshPluginRequirements.satisfies("0.1", "0.2.0"));
        assertTrue(DshPluginRequirements.satisfies("*", "0.1.7-rc.1"));
        assertFalse(DshPluginRequirements.satisfies("<*", "0.1.7-rc.1"), "npm reads `<*` as `<0.0.0-0`");
    }

    @Test
    void aRangeNothingCouldMeanSatisfiesNothing() {
        // npm answers false for a range it cannot read, and the harness refuses the plugin,
        // so nothing here may be read as "it fits".
        assertFalse(DshPluginRequirements.satisfies("latest", "0.1.7-rc.1"));
        assertFalse(DshPluginRequirements.satisfies("1.2.3.4", "1.2.3"), "a fourth number is not a range");
        assertFalse(DshPluginRequirements.satisfies("1.2.3-", "1.2.3"), "a dash with nothing after it");
        assertFalse(DshPluginRequirements.satisfies("^", "0.1.7-rc.1"));
        assertFalse(DshPluginRequirements.satisfies(">=", "0.1.7-rc.1"));
    }

    @Test
    void theHarnesssOwnPackageIsReadFromAnInstallation(@TempDir Path home) throws IOException {
        Path manifest = home.resolve("package.json");
        Files.writeString(manifest, """
                {"name": "@deepseek-ai/dsh", "version": "0.1.7-rc.1",
                 "dependencies": {"@deepseek-ai/dsh-settings": "0.1.7-rc.1",
                                  "@deepseek-ai/cordis": "4.0.1"}}
                """);

        Map<String, String> core = DshPluginRequirements.readCoreVersions(manifest);

        assertEquals("0.1.7-rc.1", core.get("@deepseek-ai/dsh"),
                "a package does not depend on itself, so its version is read from its own manifest");
        assertEquals("0.1.7-rc.1", core.get("@deepseek-ai/dsh-settings"));
        assertEquals("4.0.1", core.get("@deepseek-ai/cordis"));
    }

    @Test
    void anInstallationThatIsNotThereHoldsNothing(@TempDir Path home) throws IOException {
        assertTrue(DshPluginRequirements.readCoreVersions(home.resolve("package.json")).isEmpty());
    }

    @Test
    void aDeclarationOnTheHarnessItselfIsJudged() {
        JsonObject peers = JsonParser.parseString("""
                {"@deepseek-ai/dsh": ">=0.1.7-rc.2"}
                """).getAsJsonObject();

        assertFalse(DshPluginRequirements.fits(peers, Map.of("@deepseek-ai/dsh", "0.1.7-rc.1")),
                "the package plugins declare most is the one the harness judges against its own version");
        assertTrue(DshPluginRequirements.fits(peers, Map.of("@deepseek-ai/dsh", "0.1.7-rc.2")));
    }
}
