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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The curated terminal-plugin list is a constant somebody maintains by hand, so what is
/// pinned here is the shape it has to keep: a page can render every entry without asking
/// the network, the profile each one belongs in is always named and never an app profile,
/// and the refusal that keeps a terminal plugin out of the web profile really fires.
class DshTuiPluginsTest {

    /// The harness line a plugin needs is the half of "can this run here" that the manifest
    /// cannot answer: a terminal interface's tool surface is the agent-preset layer, which
    /// only exists from 0.2.0. Pinning that per entry is what lets the panel refuse the
    /// combination instead of installing a window with no tools behind it.
    @Test
    void everyEntryThatNeedsANewerHarnessSaysSo() {
        for (DshTuiPlugins.Entry entry : DshTuiPlugins.ENTRIES) {
            assertTrue(DshTuiPlugins.fitsHost(entry, "0.2.1-rc.3"),
                    entry.packageName() + " must run on the current line");
        }
        // The ones that need the agent-preset layer refuse the older line.
        for (String name : List.of("@huiliyi37/dsh-tianshu-tui", "@tomowang/dsh-tui", "@xmoon76/dsh-pi-tui")) {
            assertFalse(DshTuiPlugins.fitsHost(DshTuiPlugins.find(name).orElseThrow(), "0.1.7-rc.1"), name);
        }
        // The ACP terminal is the absolute compatibility option; the in-process one is not.
        DshTuiPlugins.Entry acp = DshTuiPlugins.find("@nas200-timmy/dsh-acp-tui").orElseThrow();
        assertTrue(DshTuiPlugins.fitsHost(acp, "0.1.7-rc.1"));
        assertFalse(DshTuiPlugins.fitsHost(acp, "0.1.0-rc.6"),
                "ACP's profile template arrived in 0.1.2-rc.1");
        DshTuiPlugins.Entry inproc = DshTuiPlugins.find("@nas200-timmy/dsh-inproc-tui").orElseThrow();
        assertFalse(DshTuiPlugins.fitsHost(inproc, "0.1.7-rc.1"));
        assertTrue(DshTuiPlugins.fitsHost(inproc, "0.2.0-rc.2"));
        // The gate admits the whole 0.2 line, not a list of the rcs that existed when
        // this build was cut. Enumerating exact prereleases is what left `dsh@next`
        // (0.2.2-rc.3) with no terminal: every new rc fell outside the list.
        assertTrue(DshTuiPlugins.fitsHost(inproc, "0.2.2-alpha.2"));
        assertTrue(DshTuiPlugins.fitsHost(inproc, "0.2.1-rc.3"));
        assertTrue(DshTuiPlugins.fitsHost(inproc, "0.2.1-rc.4"));
        assertTrue(DshTuiPlugins.fitsHost(inproc, "0.2.1-rc.5"));
        assertTrue(DshTuiPlugins.fitsHost(inproc, "0.2.1"));
        assertTrue(DshTuiPlugins.fitsHost(inproc, "0.2.2-rc.1"));
        assertTrue(DshTuiPlugins.fitsHost(inproc, "0.2.2-rc.3"), "0.2.2-rc.3 is dsh@next");
        // 0.3 is a new line: those seams have to be re-verified before the gate opens.
        assertFalse(DshTuiPlugins.fitsHost(inproc, "0.3.0-rc.1"));
        assertFalse(DshTuiPlugins.fitsHost(inproc, "0.3.0"));
        String mismatch = DshTuiPlugins.hostMismatch(inproc, "0.3.0-rc.1");
        assertTrue(mismatch.contains("0.3.0-rc.1"), mismatch);
        assertTrue(mismatch.contains("@nas200-timmy/dsh-acp-tui"), mismatch);
        String fallback = DshTuiPlugins.hostMismatch(inproc, "0.1.7-rc.1");
        assertTrue(fallback.contains("@nas200-timmy/dsh-acp-tui"), fallback);
    }

    /// An unknown version is not refused: the version comes from the instance record, and a
    /// guess would block an instance that is probably fine.
    @Test
    void anUnknownHarnessVersionIsNotRefused() {
        for (DshTuiPlugins.Entry entry : DshTuiPlugins.ENTRIES) {
            assertTrue(DshTuiPlugins.fitsHost(entry, null), entry.packageName());
            assertTrue(DshTuiPlugins.fitsHost(entry, ""), entry.packageName());
        }
    }

    /// The refusal names the line it needs, the line the instance has, why the pair cannot
    /// work, and which entry does — one sentence, because the install route and the terminal
    /// route both say it.
    @Test
    void theMismatchSentenceSaysWhatToDo() {
        DshTuiPlugins.Entry entry = DshTuiPlugins.find("@tomowang/dsh-tui").orElseThrow();
        String said = DshTuiPlugins.hostMismatch(entry, "0.1.7-rc.1");
        assertTrue(said.contains("@tomowang/dsh-tui"), said);
        assertTrue(said.contains(">=0.2.1-rc.3"), said);
        assertTrue(said.contains("0.1.7-rc.1"), said);
        assertTrue(said.contains("agent-preset"), said);
        assertTrue(said.contains("@deepseek-harness-tui/dsh-tui"), said);
    }

    @Test
    void everyEntryIsSelfContainedEnoughToBeShownOffline() {
        Set<String> ids = new HashSet<>();
        Set<String> packages = new HashSet<>();
        for (DshTuiPlugins.Entry entry : DshTuiPlugins.ENTRIES) {
            assertTrue(ids.add(entry.id()), "two entries share an id: " + entry.id());
            assertTrue(packages.add(entry.packageName()), "two entries name one package: " + entry.packageName());
            assertEquals(entry.packageName() + "@" + entry.version(), entry.spec(),
                    "the installer is given a pinned version, not a floating range");
            assertFalse(entry.displayName().isBlank());
            assertFalse(entry.license().isBlank(), entry.packageName() + " must name its licence");
            assertTrue(entry.repository() != null && entry.repository().startsWith("https://github.com/"),
                    entry.packageName() + " must link to a real repository");
            assertFalse(entry.description().isBlank());
        }
    }

    /// The profile is the field this list exists for, and the rule **inverts** by kind.
    ///
    /// A terminal interface is an app plugin: it is installed into the profile its
    /// authors document and started there — never into one of the profiles the harness
    /// ships an app for, because two app plugins in one profile parse the same argv and
    /// the second one exits.
    ///
    /// A capability plugin is the opposite: it has no screen, and it must live in the
    /// instance's own `web` profile so the app that renders its settings page is the one
    /// that loads it. Pointing it at a terminal profile would be the same collision from
    /// the other side.
    @Test
    void aProfileMatchesTheKindOfEntry() {
        for (DshTuiPlugins.Entry entry : DshTuiPlugins.ENTRIES) {
            assertFalse(entry.profile().isBlank(), entry.packageName() + " must name its profile");
            if (entry.terminal()) {
                assertEquals(DshSurface.OTHER, DshSurface.ofProfile(entry.profile()),
                        entry.packageName() + " points at an app profile, which is the combination that does not boot");
            } else {
                assertEquals(DshSurface.WEB, DshSurface.ofProfile(entry.profile()),
                        entry.packageName() + " is a capability and belongs in the web profile");
            }
        }
    }


    /// The refusal for an old host has to make sense for the kind of entry it is about.
    /// A capability cannot be told to "use the ACP terminal instead" — it is not a
    /// terminal, and what is missing on 0.1.x is the settings page that authorises it.
    @Test
    void aCapabilityRefusalTalksAboutAuthorisationNotTerminals() {
        // 这一版不再随面板发布能力插件（面板桥已移除），但 `terminal` 这个区分还在：
        // 用一条测试用的 Entry 守住 `hostMismatch` 的另一个分支。
        DshTuiPlugins.Entry capability = new DshTuiPlugins.Entry(
                "probe-capability",
                "@probe/capability",
                "1.0.0",
                "probe",
                null,
                "GPL-3.0-or-later",
                "a capability plugin used by this test",
                null,
                DshTuiPlugins.WEB_PROFILE,
                ">=0.2.0-rc.2",
                true,
                null,
                false);
        String text = DshTuiPlugins.hostMismatch(capability, "0.1.7-rc.2");
        assertTrue(text.contains("能力插件"), text);
        assertTrue(text.contains("0.2.x") || text.contains("0.2.0"), text);
        assertFalse(text.contains("ACP"), "a capability is not a terminal, so ACP is not the answer: " + text);
        // 不能写成"装得上但用不了"——那是在邀请人去试。
        assertFalse(text.contains("装得上") || text.contains("也用不了"), text);
        assertFalse(DshTuiPlugins.fitsHost(capability, "0.1.7-rc.2"), "0.1.x is not offered");
        assertTrue(DshTuiPlugins.fitsHost(capability, "0.2.1-alpha.1"));
        assertTrue(DshTuiPlugins.fitsHost(capability, "0.2.2-alpha.2"));
    }

    @Test
    void theKnownPackagesAreTheOnesCurated() {
        assertEquals(List.of(
                        "@nas200-timmy/dsh-inproc-tui",
                        "@nas200-timmy/dsh-acp-tui",
                        "@deepseek-harness-tui/dsh-tui",
                        "@huiliyi37/dsh-tianshu-tui",
                        "@tomowang/dsh-tui",
                        "@xmoon76/dsh-pi-tui"),
                DshTuiPlugins.ENTRIES.stream().map(DshTuiPlugins.Entry::packageName).toList());

        assertTrue(DshTuiPlugins.find("@xmoon76/dsh-pi-tui").isPresent());
        assertTrue(DshTuiPlugins.find("@unknown/terminal").isEmpty());
        assertTrue(DshTuiPlugins.find(null).isEmpty());

        // The two that are no longer offered: one needs Bun to run at all, and the other
        // is superseded in the list.
        assertTrue(DshTuiPlugins.find("@ruhooai/dsh-tui").isEmpty());
        assertTrue(DshTuiPlugins.find("@zhangweiii/dsh-tui").isEmpty());
    }

    /// The two first-party terminals are alternatives at **run time**: both may be installed
    /// (the ACP one is the fallback for an instance whose version moves), but only the first
    /// fitting one is started. Community entries declare no group.
    @Test
    void theTwoFirstPartyTerminalsShareARuntimeGroup() {
        DshTuiPlugins.Entry inproc = DshTuiPlugins.find("@nas200-timmy/dsh-inproc-tui").orElseThrow();
        DshTuiPlugins.Entry acp = DshTuiPlugins.find("@nas200-timmy/dsh-acp-tui").orElseThrow();
        assertEquals("dsh-terminal", inproc.runtimeGroup());
        assertEquals("dsh-terminal", acp.runtimeGroup());
        assertEquals(inproc, DshTuiPlugins.ENTRIES.get(0), "the in-process one is listed first");
        assertEquals(acp, DshTuiPlugins.ENTRIES.get(1), "the ACP one is its fallback");
        assertTrue(inproc.bundled());
        assertTrue(acp.bundled());
        assertTrue(acp.hasExecutable());
        assertFalse(inproc.hasExecutable());
        assertNull(DshTuiPlugins.find("@tomowang/dsh-tui").orElseThrow().runtimeGroup());
    }

    @Test
    void anEntryIsInstalledOnlyInItsOwnProfile() {
        DshTuiPlugins.Entry entry = DshTuiPlugins.find("@deepseek-harness-tui/dsh-tui").orElseThrow();

        assertTrue(DshTuiPlugins.isInstalled(entry, List.of("@DeepSeek-Harness-TUI/dsh-tui")),
                "matched case-insensitively");
        assertFalse(DshTuiPlugins.isInstalled(entry, List.of("some-other-plugin")));
    }

    /// A specification names a package plus a version, and the version is not part of the
    /// name: `@scope/name@1.2.3` keeps its leading `@` and loses the second one.
    @Test
    void aSpecificationLosesItsVersion() {
        DshTuiPlugins.Entry entry = DshTuiPlugins.find("@deepseek-harness-tui/dsh-tui").orElseThrow();

        assertEquals("@deepseek-harness-tui/dsh-tui", DshTuiPlugins.packageNameOf(entry.spec()));
        assertEquals("@deepseek-harness-tui/dsh-tui",
                DshTuiPlugins.packageNameOf("@deepseek-harness-tui/dsh-tui"));
        assertEquals("dsh-tui", DshTuiPlugins.packageNameOf("dsh-tui@0.1.0"));
        assertEquals("github:owner/name", DshTuiPlugins.packageNameOf("github:owner/name"));
        assertNull(DshTuiPlugins.packageNameOf(null));
        assertNull(DshTuiPlugins.packageNameOf("  "));
    }

    /// The refusal is the hard constraint, so both halves are pinned: it fires for the
    /// profiles that ship an app, and it stays out of the way for the profiles a terminal
    /// plugin belongs in and for packages that are not terminal plugins at all.
    @Test
    void aTerminalPluginIsRefusedByAnAppProfile() throws Exception {
        for (String profile : List.of("web", "headless", "acp", "sdk")) {
            DshException refused = assertThrows(DshException.class,
                    () -> DshTuiPlugins.refuseAppProfile("@huiliyi37/dsh-tianshu-tui@1.0.0-rc.2", profile),
                    profile + " ships an app, so a terminal plugin cannot share it");
            assertTrue(refused.getMessage().contains(profile), refused.getMessage());
            assertTrue(refused.getMessage().contains("tui"),
                    "the refusal names the profile to use instead: " + refused.getMessage());
        }

        // The plugin's own profile, and any other profile somebody made up: allowed.
        DshTuiPlugins.refuseAppProfile("@huiliyi37/dsh-tianshu-tui@1.0.0-rc.2", "tui");
        DshTuiPlugins.refuseAppProfile("@deepseek-harness-tui/dsh-tui", "dsh-tui");
        DshTuiPlugins.refuseAppProfile("@xmoon76/dsh-pi-tui", "pi-tui");
        DshTuiPlugins.refuseAppProfile("some-other-plugin", "web");
        DshTuiPlugins.refuseAppProfile(null, "web");
        DshTuiPlugins.refuseAppProfile("@tomowang/dsh-tui", null);
    }
}