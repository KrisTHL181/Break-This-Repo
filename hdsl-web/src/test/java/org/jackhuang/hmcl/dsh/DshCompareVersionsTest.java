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
package org.jackhuang.hmcl.dsh;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// [DshVersionManager#compareVersions] feeds the version picker, the lockstep
/// "oldest version" decision, the plugin sort and the Node runtime sort — a
/// wrong ordering misanswers "what is newest" in all of them. The previous
/// implementation compared same-prefix pre-releases lexicographically.
class DshCompareVersionsTest {

    private static void assertOrder(String smaller, String larger) {
        assertTrue(DshVersionManager.compareVersions(smaller, larger) < 0,
                smaller + " must sort before " + larger);
        assertTrue(DshVersionManager.compareVersions(larger, smaller) > 0,
                larger + " must sort after " + smaller);
    }

    @Test
    void prereleaseNumericIdentifiersCompareNumerically() {
        // The reported bug: lexicographic ordering puts alpha.10 below alpha.2.
        assertOrder("1.0.0-alpha.2", "1.0.0-alpha.10");
        assertOrder("0.2.0-rc.1", "0.2.0-rc.2");
    }

    @Test
    void releaseOutranksSamePrefixPrerelease() {
        assertOrder("0.2.0-rc.2", "0.2.0");
        assertOrder("0.2.1-alpha.1", "0.2.1");
    }

    @Test
    void numericPrefixDominatesPrerelease() {
        assertOrder("0.1.5-alpha.2", "0.2.1-alpha.1");
        assertOrder("2.1.0", "2.1.1");
    }

    @Test
    void nodeRuntimeStyleVersionsSortByNumber() {
        assertOrder("v20.11.0", "v22.0.0");
        assertOrder("v18.19.0", "v20.11.0");
    }

    @Test
    void canonicalFormsCompareEqual() {
        assertEquals(0, DshVersionManager.compareVersions("1.0", "1.0.0"));
        assertEquals(0, DshVersionManager.compareVersions("2.1.1", "2.1.1"));
    }
}
