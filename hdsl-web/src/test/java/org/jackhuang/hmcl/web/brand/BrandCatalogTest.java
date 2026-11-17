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
package org.jackhuang.hmcl.web.brand;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The registry-document parsing behind the brand version picker.
class BrandCatalogTest {

    /// OpenCode publishes over twelve thousand CI tags for a few hundred real
    /// releases; only strict `x.y.z` versions may reach the picker.
    @Test
    void snapshotAndCiNoiseNeverReachThePicker() {
        String body = """
                {"name":"opencode-ai","dist-tags":{"latest":"1.18.35"},
                 "versions":{
                   "0.0.0-snapshot-proxy-202512080622":{},
                   "0.0.0-ci-202601291809":{},
                   "0.0.0-next-202606270058":{},
                   "0.0.0-tui-v2-202606261840":{},
                   "1.0.142":{}, "1.1.4":{}, "1.18.34":{}, "1.18.35":{},
                   "1.9.0":{}, "2.10.0":{}, "2.9.0":{}
                 }}""";

        BrandCatalog.Snapshot snapshot = BrandCatalog.parse(body);

        assertEquals(List.of("2.10.0", "2.9.0", "1.18.35", "1.18.34", "1.9.0", "1.1.4", "1.0.142"),
                snapshot.versions());
        assertEquals("1.18.35", snapshot.latest());
    }

    @Test
    void numericOrderingBeatsStringOrdering() {
        String body = """
                {"dist-tags":{"latest":"2.10.0"},
                 "versions":{"2.9.0":{},"2.10.0":{},"10.0.0":{},"1.2.3":{}}}""";

        BrandCatalog.Snapshot snapshot = BrandCatalog.parse(body);

        assertEquals(List.of("10.0.0", "2.10.0", "2.9.0", "1.2.3"), snapshot.versions());
        assertEquals("2.10.0", snapshot.latest());
    }

    @Test
    void aLatestTagPointingAtNoiseFallsBackToTheNewestCleanVersion() {
        String body = """
                {"dist-tags":{"latest":"0.0.0-dev-202610080611"},
                 "versions":{"0.0.0-dev-202610080611":{},"2.1.1":{},"2.0.2":{}}}""";

        BrandCatalog.Snapshot snapshot = BrandCatalog.parse(body);

        assertEquals(List.of("2.1.1", "2.0.2"), snapshot.versions());
        assertEquals("2.1.1", snapshot.latest());
    }

    @Test
    void garbageYieldsAnEmptySnapshot() {
        assertTrue(BrandCatalog.parse("not json").versions().isEmpty());
        assertTrue(BrandCatalog.parse("{}").versions().isEmpty());
        assertTrue(BrandCatalog.parse("{\"versions\":{}}").versions().isEmpty());
    }

    /// GitHub brands list versions from `/repos/<slug>/tags`: release tags
    /// carry a `v` prefix, and a repo may tag non-releases (the engine,
    /// nightlies) that must not reach the picker.
    @Test
    void githubTagsBecomeCleanVersionsNewestFirst() {
        String body = """
                [{"name":"v0.2.1","commit":{"sha":"49795f9"},"zipball_url":"https://…"},
                 {"name":"v0.2.0","commit":{"sha":"aaaa111"},"zipball_url":"https://…"},
                 {"name":"engine-v2.8.4-cw.2","commit":{"sha":"cccc333"}},
                 {"name":"v0.10.0","commit":{"sha":"dddd444"}},
                 {"name":"v0.9.0","commit":{"sha":"eeee555"}},
                 {"name":"nightly","commit":{"sha":"ffff666"}}]""";

        BrandCatalog.Snapshot snapshot = BrandCatalog.parseGitHubTags(body);

        assertEquals(List.of("0.10.0", "0.9.0", "0.2.1", "0.2.0"), snapshot.versions());
        assertEquals("0.10.0", snapshot.latest());
    }

    @Test
    void githubTagsWithoutACleanTagYieldAnEmptySnapshot() {
        assertTrue(BrandCatalog.parseGitHubTags("[{\"name\":\"nightly\"}]").versions().isEmpty());
        assertTrue(BrandCatalog.parseGitHubTags("").versions().isEmpty());
        assertTrue(BrandCatalog.parseGitHubTags("{\"message\":\"Not Found\"}").versions().isEmpty());
    }
}
