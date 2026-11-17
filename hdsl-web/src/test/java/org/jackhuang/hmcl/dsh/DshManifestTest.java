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

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Tests the manifest an install is described by.
///
/// The pinning is not a preference. DeepSeek Harness and its boot library are
/// published together and a release whose libraries disagree with it fails at
/// import, so a manifest that fails to hold them together produces a runtime that
/// installs cleanly and does not start. It did: the override was written where npm
/// reads it and pnpm does not, and nothing said so.
class DshManifestTest {
    /// A temporary directory standing in for the install prefix.
    @TempDir
    private Path prefix;

    /// The launcher is asked for the exact version, not a range.
    ///
    /// A caret cannot express what these packages promise: the whole family is
    /// published in lockstep, so `^0.1.6-alpha.1` resolves to alpha.2's libraries
    /// and the tree comes out inconsistent.
    ///
    /// @throws IOException  when the manifest cannot be read back
    /// @throws DshException when the manifest cannot be written
    @Test
    void pinsTheLauncherExactly() throws IOException, DshException {
        DshVersionManager.writeManifest(prefix, "0.1.6-alpha.1", "0.1.6-alpha.1", null,
                DshDependencyPolicy.LATEST);

        JsonObject manifest = JsonParser.parseString(
                Files.readString(prefix.resolve("package.json"))).getAsJsonObject();
        assertEquals("0.1.6-alpha.1",
                manifest.getAsJsonObject("dependencies").get("\u0040deepseek-ai/dsh").getAsString(),
                "the launcher should be pinned to the exact version");
    }

    /// The boot library is pinned where pnpm actually reads it.
    ///
    /// Since pnpm 10 the settings it used to take from `package.json` live in
    /// `pnpm-workspace.yaml`; a `pnpm` field in the manifest is ignored, and npm's
    /// `overrides` field is not read by pnpm at all. Writing either leaves the
    /// boot library free to resolve to whatever the launcher's own dependencies
    /// ask for, which is a different version as soon as the two have diverged.
    ///
    /// @throws IOException when the file cannot be read back
    @Test
    void pinsTheBootLibraryWherePnpmReadsIt() throws IOException, DshException {
        DshVersionManager.writeManifest(prefix, "0.1.6-alpha.1", "0.1.6-alpha.1", null,
                DshDependencyPolicy.LATEST);

        Path workspace = prefix.resolve("pnpm-workspace.yaml");
        assertTrue(Files.isRegularFile(workspace),
                "pnpm reads its settings from pnpm-workspace.yaml, so the override belongs there");
        String text = Files.readString(workspace);
        assertTrue(text.contains("overrides:"), "the override section should be present");
        assertTrue(text.contains("\u0040deepseek-ai/dsh-app-boot"), "the boot library should be named");
        assertTrue(text.contains("0.1.6-alpha.1"), "held to the version asked for");
    }

    /// A boot library other than the launcher's own is written as given.
    ///
    /// @throws IOException when the file cannot be read back
    @Test
    void writesTheBootLibraryItIsGiven() throws IOException, DshException {
        DshVersionManager.writeManifest(prefix, "0.1.6-alpha.1", "0.1.5-rc.2", null,
                DshDependencyPolicy.LATEST);

        assertTrue(Files.readString(prefix.resolve("pnpm-workspace.yaml")).contains("0.1.5-rc.2"),
                "the override should say what was asked for, not the launcher's own version");
    }

    /// The pin is read from its own entry, not from a list that merely names the package.
    ///
    /// `pnpm-workspace.yaml` holds both: a list of packages spelled `'@scope/name@1.2.3'` and the
    /// `overrides:` mapping. The reader used to take the first line that contained the package name,
    /// which is one of the list entries — and what it answered with still carried the `@` that
    /// separated the two halves of that entry. An instance pinned that way exported a pack whose
    /// pin was `@0.1.7-rc.1`, and installing that pack wrote a workspace file pnpm refused with
    /// `bad indentation of a mapping entry`, because a plain scalar cannot begin with `@`.
    @Test
    void thePinIsReadFromItsOwnEntryAndNotFromAListOfPackages() {
        String workspace = """
                onlyBuiltDependencies:
                  - '@deepseek-ai/dsh-api-job-controller@0.1.7-rc.1'
                  - '@deepseek-ai/dsh-app-boot@0.1.7-rc.1'
                overrides:
                  '@deepseek-ai/dsh-app-boot': 0.1.7-rc.1
                  '@deepseek-ai/cordis': 4.0.4
                """;
        assertEquals("0.1.7-rc.1", DshVersionManager.appBootPin(workspace),
                "the list entry names the package and a version, but it is not the pin");

        assertNull(DshVersionManager.appBootPin("  - '@deepseek-ai/dsh-app-boot@0.1.7-rc.1'\n"),
                "a file that only lists the package pins nothing");
        assertEquals("0.1.5-alpha.2", DshVersionManager.appBootPin(
                "overrides:\n  \"@deepseek-ai/dsh-app-boot\": '0.1.5-alpha.2'\n"),
                "quotes around either half are the spelling, not the value");
    }

    /// A value YAML would read as an indicator is quoted, and an ordinary version is not.
    ///
    /// The file is built by hand, so this is the one place that decides whether what it writes is
    /// YAML at all. A package name begins with `@` and a pin may be a range or a protocol; both are
    /// values a reader would otherwise take for syntax.
    ///
    /// @throws IOException  when the file cannot be read back
    /// @throws DshException when the manifest cannot be written
    @Test
    void aValueYamlWouldReadAsAnIndicatorIsQuoted() throws IOException, DshException {
        DshVersionManager.writeManifest(prefix, "0.1.6-alpha.1", "@0.1.7-rc.1", null,
                DshDependencyPolicy.LATEST);

        String text = Files.readString(prefix.resolve("pnpm-workspace.yaml"));
        assertTrue(text.contains("  '@deepseek-ai/dsh-app-boot': '@0.1.7-rc.1'"),
                "a key and a value that start with @ are both quoted, not written as syntax: " + text);
    }

    /// A version needs no quotes, so the file keeps the shape a person reads.
    ///
    /// @throws IOException  when the file cannot be read back
    /// @throws DshException when the manifest cannot be written
    @Test
    void anOrdinaryVersionIsWrittenPlainly() throws IOException, DshException {
        DshVersionManager.writeManifest(prefix, "0.1.6-alpha.1", "0.1.6-alpha.1", null,
                DshDependencyPolicy.LATEST);

        String text = Files.readString(prefix.resolve("pnpm-workspace.yaml"));
        assertTrue(text.contains(": 0.1.6-alpha.1"), "the version itself is a plain scalar");
        assertFalse(text.contains("'0.1.6-alpha.1'"), "and is not quoted for no reason");
    }

    /// The boot library is named once, even though the policy also holds it.
    ///
    /// It is one of the vendor's packages, so a policy that holds those names it — and the file then
    /// carried `'@deepseek-ai/dsh-app-boot'` twice. pnpm refuses the whole workspace with
    /// `duplicated mapping key`, which is a launcher that cannot install at all. The two are
    /// therefore combined into one map before either file is written, and the caller's choice wins.
    @Test
    void theBootLibraryIsNamedOnceEvenWhenThePolicyAlsoHoldsIt() {
        java.util.Map<String, String> held = new java.util.LinkedHashMap<>();
        held.put("\u0040deepseek-ai/cordis", "4.0.2");
        held.put("\u0040deepseek-ai/dsh-app-boot", "0.1.6-alpha.1");

        java.util.Map<String, String> overrides = DshVersionManager.mergeOverrides(
                "0.1.5-alpha.2", held);

        assertEquals(2, overrides.size(), "two packages, and no key written twice");
        assertEquals("0.1.5-alpha.2", overrides.get("\u0040deepseek-ai/dsh-app-boot"),
                "the version the caller chose is the one that stands");
        assertEquals("4.0.2", overrides.get("\u0040deepseek-ai/cordis"));
    }

    /// npm answers about a specifier with an array, and that is what has to be read.
    ///
    /// Captured verbatim from `npm view @deepseek-ai/dsh@0.1.5-alpha.2 dependencies --json`, because
    /// the shape is the whole point: the first version of this read only the object form, found
    /// nothing in the array, and would have applied the policy to precisely nothing.
    @Test
    void theDependenciesAreReadFromTheShapeNpmActuallyAnswersWith() {
        String answer = "[{\"commander\":\"^15.0.0\","
                + "\"@deepseek-ai/cordis\":\"^4.0.2\","
                + "\"@deepseek-ai/cordis-plugin-loader\":\"^1.0.3\"}]";

        com.google.gson.JsonObject declared = DshVersionManager.declaredDependencies(answer);

        assertNotNull(declared, "an array of one object is npm's answer for a named version");
        assertEquals("^4.0.2", declared.get("\u0040deepseek-ai/cordis").getAsString());
        assertEquals("4.0.2", DshVersionManager.floorOf(
                declared.get("\u0040deepseek-ai/cordis").getAsString()));
    }

    /// A package that declares nothing, and an answer that is not JSON, both say "nothing to hold".
    @Test
    void anAnswerWithNoDependenciesIsNotAnError() {
        assertNull(DshVersionManager.declaredDependencies("[]"));
        assertNull(DshVersionManager.declaredDependencies("null"));
        assertNull(DshVersionManager.declaredDependencies("not json"));
    }

    /// Only the ranges that have a floor are read; the rest are left alone.
    ///
    /// A range whose floor this cannot name must not be guessed at, and a dependency with no override
    /// simply resolves freely — which is the policy's own answer for anything it does not hold.
    @Test
    void aFloorIsReadFromTheRangesThatHaveOne() {
        assertEquals("4.0.2", DshVersionManager.floorOf("^4.0.2"));
        assertEquals("1.0.3", DshVersionManager.floorOf("~1.0.3"));
        assertEquals("2.0.0", DshVersionManager.floorOf(">=2.0.0"));
        assertEquals("0.1.5-alpha.2", DshVersionManager.floorOf("0.1.5-alpha.2"),
                "an exact version is its own floor");
        assertEquals("0.1.5-alpha.2", DshVersionManager.floorOf("^0.1.5-alpha.2"),
                "a prerelease floor is still a floor");
    }

    /// What cannot be read is not guessed at.
    @Test
    void aRangeWithNoReadableFloorIsLeftAlone() {
        assertNull(DshVersionManager.floorOf("*"));
        assertNull(DshVersionManager.floorOf("latest"));
        assertNull(DshVersionManager.floorOf(">=1.0.0 <2.0.0"));
        assertNull(DshVersionManager.floorOf("github:owner/repo#sha"));
        assertNull(DshVersionManager.floorOf("workspace:*"));
        assertNull(DshVersionManager.floorOf(""));
        assertNull(DshVersionManager.floorOf(null));
    }

    /// The default holds the vendor's own packages and nothing else.
    ///
    /// The plugin framework and the harness's libraries are published together in lockstep and have
    /// broken a launch twice between them; a pack's own plugins are the author's business and are
    /// left free.
    @Test
    void theDefaultPolicyHoldsOnlyTheVendorsOwnPackages() {
        DshDependencyPolicy policy = DshDependencyPolicy.CORE_PINNED;

        assertTrue(policy.pins("\u0040deepseek-ai/cordis"));
        assertTrue(policy.pins("\u0040deepseek-ai/cordis-plugin-loader"));
        assertTrue(policy.pins("\u0040deepseek-ai/dsh-app-boot"));
        assertFalse(policy.pins("dsh-wildmon"));
        assertFalse(policy.pins("\u0040hellosz/dsh-pets"));

        assertFalse(DshDependencyPolicy.LATEST.pins("\u0040deepseek-ai/cordis"),
                "following upstream holds nothing");
        assertTrue(DshDependencyPolicy.LOCKED.pins("\u0040hellosz/dsh-pets"),
                "the strictest policy holds everything declared");
    }

    /// dsh cannot run on FreeBSD: its `node-addon-require-builtin` native addon has
    /// no FreeBSD build, and `dsh-app-boot` calls it while installing the profile
    /// resolver. The gate lives here rather than in the UI so every install path
    /// (create, repair, version change) refuses before downloading a tree that can
    /// never boot.
    @Test
    void freebsdCannotRunDeepSeekHarness() {
        assertFalse(DshVersionManager.installableOn(
                org.jackhuang.hmcl.util.platform.OperatingSystem.FREEBSD));
        assertTrue(DshVersionManager.installableOn(
                org.jackhuang.hmcl.util.platform.OperatingSystem.LINUX));
        assertTrue(DshVersionManager.installableOn(
                org.jackhuang.hmcl.util.platform.OperatingSystem.MACOS));
        assertTrue(DshVersionManager.installableOn(
                org.jackhuang.hmcl.util.platform.OperatingSystem.WINDOWS));
    }
}
