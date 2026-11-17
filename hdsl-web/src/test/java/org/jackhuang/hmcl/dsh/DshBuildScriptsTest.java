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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Verifies what the launcher reads out of a profile's package-manager settings, and
/// what it writes back.
///
/// The setting decides whether arbitrary install scripts may run with the user's own
/// rights, so what matters is that the launcher only ever says what it was told to
/// say: an entry nobody has answered is reported as waiting, answering one leaves the
/// others alone, and nothing else in the file moves.
class DshBuildScriptsTest {
    /// The instance the tests work in.
    private static final String ID = "build-scripts-test";

    /// Removes the instance the tests made.
    @AfterEach
    void removeInstance() {
        try {
            if (DshInstanceManager.find(ID) != null) {
                DshInstanceManager.delete(ID);
            }
        } catch (Exception ignored) {
            // The test's own cleanup: a failure here would hide the real one.
        }
    }

    @Test
    void anUnansweredEntryIsReportedAsWaiting() throws Exception {
        DshInstance instance = makeInstance("""
                packages:
                  - .

                nodeLinker: hoisted
                allowBuilds:
                  cloudflared: set this to true or false
                  node-pty: set this to true or false
                """);

        List<DshBuildScripts.Pending> pending = DshBuildScripts.pending(instance);

        assertEquals(List.of("cloudflared", "node-pty"),
                pending.stream().map(DshBuildScripts.Pending::name).toList());
        assertNull(pending.get(0).allowed(), "a placeholder is not an answer");
        assertEquals(List.of("cloudflared", "node-pty"), DshBuildScripts.unanswered(instance));
    }

    @Test
    void answeringOneLeavesTheOtherAndTheRestOfTheFileAlone() throws Exception {
        DshInstance instance = makeInstance("""
                packages:
                  - .

                nodeLinker: hoisted
                autoInstallPeers: false
                minimumReleaseAgeExclude:
                  - dshmarket@1.53.0
                allowBuilds:
                  cloudflared: set this to true or false
                  node-pty: set this to true or false
                """);

        assertEquals(1, DshBuildScripts.answer(instance, List.of("node-pty"), true));

        String written = Files.readString(profileFile(instance));
        assertTrue(written.contains("node-pty: true"), written);
        assertTrue(written.contains("cloudflared: set this to true or false"),
                "an entry nobody answered stays a question");
        assertTrue(written.contains("minimumReleaseAgeExclude:"), "the rest of the file is untouched");
        assertTrue(written.contains("autoInstallPeers: false"));
        assertEquals(List.of("cloudflared"), DshBuildScripts.unanswered(instance));
    }

    @Test
    void refusingIsAnAnswerToo() throws Exception {
        DshInstance instance = makeInstance("""
                allowBuilds:
                  node-pty: set this to true or false
                """);

        assertEquals(1, DshBuildScripts.answer(instance, List.of("node-pty"), false));

        assertTrue(Files.readString(profileFile(instance)).contains("node-pty: false"));
        assertEquals(List.of(), DshBuildScripts.unanswered(instance));
        assertFalse(DshBuildScripts.answer(instance, List.of("nothing-here"), true) > 0,
                "a package that was never asked about is not invented");
    }

    @Test
    void aQuestionPnpmNamedIsRecordedAndAnsweredLikeAnyOther() throws Exception {
        // A repository-hosted package is named by the address it was resolved to, so the entry
        // holds colons of its own — reading only up to the first one would lose it.
        String key = "dshmarket@https://codeload.github.com/dsh-market/dsh-market/tar.gz/"
                + "180c3144da8eb4229cacb843aced229ea55912fc";
        DshInstance instance = makeInstance("""
                allowBuilds:
                  node-pty: true
                """);

        assertEquals(1, DshBuildScripts.propose(instance, List.of(key)));
        assertEquals(List.of(key), DshBuildScripts.unanswered(instance),
                "what pnpm named is the question waiting to be answered");
        assertEquals(0, DshBuildScripts.propose(instance, List.of(key)), "asking twice records one question");

        assertEquals(1, DshBuildScripts.answer(instance, List.of(key), true));

        String written = Files.readString(profileFile(instance));
        assertTrue(written.contains(key + ": true"), written);
        assertTrue(written.contains("node-pty: true"), "an entry somebody answered stays as it was");
        assertEquals(List.of(), DshBuildScripts.unanswered(instance));
        assertEquals(0, DshBuildScripts.propose(instance, List.of(key)),
                "an answered entry is not asked about again");
    }

    @Test
    void aQuestionIsRecordedWhereTheFileHasNoSectionYet() throws Exception {
        String key = "thing@https://codeload.github.com/owner/name/tar.gz/abcdef";
        DshInstance instance = makeInstance("""
                packages:
                  - .

                nodeLinker: hoisted
                """);

        assertEquals(1, DshBuildScripts.propose(instance, List.of(key)));

        String written = Files.readString(profileFile(instance));
        assertTrue(written.contains("allowBuilds:\n  " + key + ": set this to true or false"), written);
        assertTrue(written.contains("nodeLinker: hoisted"), "the rest of the file is untouched");
        assertEquals(List.of(key), DshBuildScripts.unanswered(instance));
    }

    @Test
    void anInstancesOwnAnswerOverridesTheLaunchers() throws Exception {
        DshInstance instance = makeInstance("""
                allowBuilds:
                  node-pty: set this to true or false
                """);

        // Following the launcher: the instance's file says nothing.
        assertNull(DshInstanceSettings.buildScriptPolicy(instance));
        assertEquals(DshBuildScriptPolicy.MANUAL, DshBuildScriptPolicy.of(instance),
                "with no answer of its own, the launcher's applies, and it asks by default");

        DshInstanceSettings.setBuildScriptPolicy(instance, DshBuildScriptPolicy.AUTO.id());
        assertEquals(DshBuildScriptPolicy.AUTO, DshBuildScriptPolicy.of(instance),
                "an instance may allow what the launcher does not");

        DshInstanceSettings.setBuildScriptPolicy(instance, DshBuildScriptPolicy.NEVER.id());
        assertEquals(DshBuildScriptPolicy.NEVER, DshBuildScriptPolicy.of(instance),
                "and may refuse them");

        DshInstanceSettings.setBuildScriptPolicy(instance, null);
        assertNull(DshInstanceSettings.buildScriptPolicy(instance),
                "and may go back to following the launcher");
        assertEquals(DshBuildScriptPolicy.MANUAL, DshBuildScriptPolicy.of(instance));
    }

    /// A fresh instance runs under the launcher-wide answer, whichever of the three it is.
    ///
    /// Installing a pack is what makes the instance, so there is no per-instance answer to find and
    /// the launcher's is the one that decides: run install scripts without asking, ask, or never run
    /// them. A pack that answered for itself — always asking, or answering yes — would be deciding
    /// about running somebody else's code on the person's behalf, which is the one thing this
    /// setting exists to prevent.
    ///
    /// @throws Exception when the instance cannot be made
    @Test
    void aFreshInstanceRunsUnderTheLauncherWideAnswer() throws Exception {
        DshInstance instance = makeInstance("""
                packages:
                  - .
                """);
        var launcher = org.jackhuang.hmcl.setting.SettingsManager.settings();
        DshBuildScriptPolicy before = launcher.buildScriptPolicy();
        try {
            for (DshBuildScriptPolicy launcherWide : DshBuildScriptPolicy.values()) {
                launcher.setBuildScriptPolicy(launcherWide);
                assertEquals(launcherWide, DshBuildScriptPolicy.of(instance),
                        "an instance that has answered nothing runs under " + launcherWide.id());
            }
        } finally {
            launcher.setBuildScriptPolicy(before);
        }
    }

    /// Creates an instance whose profile holds the given workspace file.
    ///
    /// @param workspace the file's contents
    /// @return the instance
    private DshInstance makeInstance(String workspace) throws Exception {
        Path source = Files.createTempDirectory("build-scripts-home");
        DshInstance instance = DshInstanceManager.create(ID, "0.1.6-alpha.2", DshInstance.DEFAULT_PROFILE,
                source, DshHomeMode.ISOLATED, null, List.of(), Map.of());
        Files.createDirectories(profileFile(instance).getParent());
        Files.writeString(profileFile(instance), workspace);
        return instance;
    }

    /// Returns an instance's `pnpm-workspace.yaml`.
    ///
    /// @param instance the instance
    /// @return the file
    private static Path profileFile(DshInstance instance) throws Exception {
        return instance.homeDirectory().resolve("profiles").resolve(instance.profile())
                .resolve("pnpm-workspace.yaml");
    }
}
