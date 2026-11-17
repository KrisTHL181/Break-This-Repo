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

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Verifies how the registry's document is read and arranged.
///
/// The arrangement is what the page draws: which versions fit the harness an instance runs, which were
/// written for another one, and which said nothing at all. The last of those is the reason this is
/// pinned rather than eyeballed — a version that declares nothing must not be counted as fitting, and
/// must not disappear either, because the harness has an exemption for a version that does not fit,
/// which makes "does not fit" something to show rather than something to hide.
class DshPluginVersionsTest {
    /// The harness version the instance in these tests runs.
    private static final String HARNESS = "0.1.7-rc.1";

    /// The packages that harness ships, as an instance's installation holds them.
    private static final Map<String, String> CORE = Map.of("@deepseek-ai/dsh-settings", "0.1.7-rc.1");

    /// A document in the shape the registry serves, holding the cases that matter: a version written
    /// for a later harness, versions written for this one, one that named only the framework, and one
    /// whose declaration spans two older harness versions — which is the one that has to be listed
    /// under both.
    private static final String DOCUMENT = """
            {
              "dist-tags": { "latest": "0.24.1", "beta": "0.22.0", "dev": "0.21.0-dev.1" },
              "time": {
                "0.24.1": "2026-09-28T15:37:38.360Z",
                "0.22.0": "2026-09-27T14:40:46.276Z",
                "0.21.0-dev.1": "2026-09-25T04:00:00.000Z",
                "0.20.0-alpha.1": "2026-09-20T04:00:00.000Z",
                "0.12.2": "2026-08-15T13:45:44.628Z"
              },
              "versions": {
                "0.24.1": {
                  "version": "0.24.1",
                  "peerDependencies": {
                    "@deepseek-ai/cordis": "^4.0.4",
                    "@deepseek-ai/dsh-settings": "^0.2.0-rc.1"
                  }
                },
                "0.22.0": {
                  "version": "0.22.0",
                  "peerDependencies": {
                    "@deepseek-ai/dsh-settings": "^0.1.7-rc.1 || ^0.2.0-rc.1"
                  }
                },
                "0.21.0-dev.1": {
                  "version": "0.21.0-dev.1",
                  "peerDependencies": {
                    "@deepseek-ai/dsh-settings": "^0.1.5 || ^0.1.7-rc.1"
                  }
                },
                "0.20.0-alpha.1": {
                  "version": "0.20.0-alpha.1",
                  "peerDependencies": { "@deepseek-ai/cordis": "^4.0.4" }
                },
                "0.12.2": {
                  "version": "0.12.2",
                  "peerDependencies": {
                    "@deepseek-ai/dsh-settings": "^0.1.0 || ^0.1.1"
                  }
                }
              }
            }
            """;

    @Test
    void aDocumentBecomesTheVersionsItDescribesNewestFirst() {
        List<DshPluginVersions.Published> versions = DshPluginVersions.read(document());

        assertEquals(List.of("0.24.1", "0.22.0", "0.21.0-dev.1", "0.20.0-alpha.1", "0.12.2"),
                versions.stream().map(DshPluginVersions.Published::version).toList());
        assertEquals("2026-09-28T15:37:38.360Z", versions.get(0).published().toString(),
                "when a version was published is what the row says under its name");
    }

    @Test
    void whatIsDeclaredAboutTheHarnessIsTheHarnesssOwnPackagesOnly() {
        List<DshPluginVersions.Published> versions = DshPluginVersions.read(document());

        assertEquals(1, versions.get(0).harnessPeers().size(),
                "the framework's packages say nothing about which harness a plugin runs on");
        assertTrue(versions.get(0).harnessPeers().has("@deepseek-ai/dsh-settings"));
        assertTrue(versions.get(3).harnessPeers().isEmpty(),
                "a version that named only the framework declared nothing");
        assertEquals(List.of("0.2.0-rc.1", "0.1.7-rc.1"), versions.get(1).claimed(),
                "the versions inside a range are read, newest first, and are what names the groups");
    }

    @Test
    void aVersionIsMarkedByItsOwnNameAndByTheTagsThatPointAtIt() {
        JsonObject tags = document().getAsJsonObject("dist-tags");

        assertEquals(DshPluginVersions.Channel.STABLE, DshPluginVersions.channelOf("0.24.1", tags));
        assertEquals(DshPluginVersions.Channel.BETA, DshPluginVersions.channelOf("0.22.0", tags),
                "a version the beta tag points at is a beta even though its name does not say so");
        assertEquals(DshPluginVersions.Channel.OTHER, DshPluginVersions.channelOf("0.21.0-dev.1", tags));
        assertEquals(DshPluginVersions.Channel.ALPHA, DshPluginVersions.channelOf("0.20.0-alpha.1", tags));
        assertEquals(DshPluginVersions.Channel.RC, DshPluginVersions.channelOf("0.19.0-rc.1", tags));
        assertEquals(DshPluginVersions.Channel.STABLE, DshPluginVersions.channelOf("0.12.2", tags));
    }

    @Test
    void everyHarnessVersionANamesIsGivenItsOwnList() {
        List<DshPluginVersions.Group> groups = DshPluginVersions.group(
                DshPluginVersions.read(document()), HARNESS);

        assertEquals(List.of(HARNESS, "0.2.0-rc.1", "0.1.5", "0.1.1", "0.1.0"),
                groups.subList(0, 5).stream().map(DshPluginVersions.Group::harnessVersion).toList(),
                "the instance's harness first, then every harness version a version named, newest first");
        assertTrue(groups.get(0).recommended(), "what the page was opened for comes first");
        assertTrue(groups.subList(1, 5).stream().noneMatch(DshPluginVersions.Group::recommended));

        assertEquals(List.of("0.22.0", "0.21.0-dev.1", "0.12.2"), versionsOf(groups.get(0)),
                "the versions that admit the harness the instance runs, a caret over `0.1.0` included:"
                        + " npm reads a prerelease into a range that does not name one, and so does the harness");
        assertEquals(List.of("0.24.1", "0.22.0"), versionsOf(groups.get(1)),
                "and one written for a later harness is under that harness, not hidden");
        assertEquals(List.of("0.21.0-dev.1", "0.12.2"), versionsOf(groups.get(2)),
                "a caret over the 0.1 line admits every 0.1 patch, so both are in this list");
        assertEquals(List.of("0.12.2"), versionsOf(groups.get(3)));
        assertEquals(List.of("0.12.2"), versionsOf(groups.get(4)),
                "a version that admits two harness versions is listed under both, as a mod file is");
    }

    @Test
    void theVersionsThatSaidNothingComeLastInAGroupOfTheirOwn() {
        List<DshPluginVersions.Group> groups = DshPluginVersions.group(
                DshPluginVersions.read(document()), HARNESS);

        DshPluginVersions.Group last = groups.get(groups.size() - 1);
        assertNull(last.harnessVersion());
        assertEquals(List.of("0.20.0-alpha.1"), versionsOf(last));
    }

    @Test
    void aVersionThatSaysNothingIsNeitherJudgedNorHidden() {
        List<DshPluginVersions.Published> versions = DshPluginVersions.read(document());
        DshPluginVersions.Published silent = versions.stream()
                .filter(version -> version.version().equals("0.20.0-alpha.1"))
                .findFirst()
                .orElseThrow();

        assertEquals(DshPluginVersions.Fit.UNCLAIMED,
                DshPluginVersions.fitOf(silent, HARNESS, CORE));

        List<DshPluginVersions.Group> groups = DshPluginVersions.group(versions, HARNESS);
        assertTrue(groups.stream().flatMap(group -> group.versions().stream()).anyMatch(silent::equals),
                "it is in a group of its own rather than dropped: it may still be installed");
    }

    @Test
    void withoutTheHarnesssPackagesARangeIsReadAgainstTheHarnessVersion() {
        List<DshPluginVersions.Published> versions = DshPluginVersions.read(document());
        DshPluginVersions.Published later = versions.get(0);
        DshPluginVersions.Published alternatives = versions.get(1);

        assertEquals(DshPluginVersions.Fit.NOT_FITS,
                DshPluginVersions.fitOf(later, HARNESS, null));
        assertEquals(DshPluginVersions.Fit.FITS,
                DshPluginVersions.fitOf(alternatives, HARNESS, null),
                "the rule the harness applies itself: the range against the version it is running");
    }

    @Test
    void withoutAnInstanceNothingIsRecommended() {
        List<DshPluginVersions.Group> groups = DshPluginVersions.group(
                DshPluginVersions.read(document()), null);

        assertTrue(groups.stream().noneMatch(DshPluginVersions.Group::recommended),
                "a page with no instance has nothing to recommend a version for");
        assertEquals(List.of("0.2.0-rc.1", "0.1.7-rc.1", "0.1.5", "0.1.1", "0.1.0"),
                groups.subList(0, 5).stream().map(DshPluginVersions.Group::harnessVersion).toList());
        assertNull(groups.get(5).harnessVersion(), "and the version that said nothing comes last");
    }

    @Test
    void aVersionForALaterPrereleaseOfTheSameNumbersIsNotListedForThisOne() {
        // What the remote web UI publishes from 0.4.2 on: the numbers are the instance's own and
        // only the prerelease is later, which is exactly what the harness refuses an installation
        // over — so the page must not offer it among the versions that fit.
        JsonObject document = JsonParser.parseString("""
                {
                  "versions": {
                    "0.4.3": { "version": "0.4.3",
                               "peerDependencies": { "@deepseek-ai/dsh": ">=0.1.7-rc.2" } },
                    "0.4.1": { "version": "0.4.1",
                               "peerDependencies": { "@deepseek-ai/dsh": ">=0.1.7-rc.1" } }
                  }
                }
                """).getAsJsonObject();

        List<DshPluginVersions.Group> groups = DshPluginVersions.group(
                DshPluginVersions.read(document), HARNESS);

        assertEquals(HARNESS, groups.get(0).harnessVersion());
        assertEquals(List.of("0.4.1"), versionsOf(groups.get(0)),
                "0.4.3 asks for a later prerelease, which the harness will not run on this one");
        assertTrue(groups.stream().anyMatch(group -> "0.1.7-rc.2".equals(group.harnessVersion())
                        && versionsOf(group).contains("0.4.3")),
                "and it is still listed under the harness version it does ask for");
    }

    /// Returns the versions one group holds.
    ///
    /// @param group the group
    /// @return the versions
    private static List<String> versionsOf(DshPluginVersions.Group group) {
        return group.versions().stream().map(DshPluginVersions.Published::version).toList();
    }

    /// Reads the document these tests work on.
    ///
    /// @return the document
    private static JsonObject document() {
        return JsonParser.parseString(DOCUMENT).getAsJsonObject();
    }
}
