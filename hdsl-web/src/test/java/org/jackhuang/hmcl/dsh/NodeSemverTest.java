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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The range evaluator against the declarations that actually appear in brand
/// packages — including the Hermes line that changes from 24-compatible to
/// 26-only between 0.22.5 and 0.22.7.
class NodeSemverTest {

    @Test
    void hermes0225AcceptsTheTwentyTwoAndTwentyFourLines() {
        String range = "^22.22.0 || ^24.11.0 || >=26.0.0";
        assertTrue(NodeSemver.satisfies("22.22.0", range));
        assertTrue(NodeSemver.satisfies("22.23.1", range));
        assertTrue(NodeSemver.satisfies("24.11.0", range));
        assertTrue(NodeSemver.satisfies("24.14.0", range));
        assertTrue(NodeSemver.satisfies("26.0.0", range));
        assertFalse(NodeSemver.satisfies("22.21.9", range));
        assertFalse(NodeSemver.satisfies("24.10.9", range));
        assertFalse(NodeSemver.satisfies("23.0.0", range));
    }

    @Test
    void hermes0227RequiresTwentySix() {
        assertTrue(NodeSemver.satisfies("26.13.0", ">=26"));
        assertTrue(NodeSemver.satisfies("v26.13.0", ">=26"));
        assertFalse(NodeSemver.satisfies("24.14.0", ">=26"));
        assertFalse(NodeSemver.satisfies("22.23.1", ">=26"));
    }

    @Test
    void caretTildeAndBareForms() {
        assertTrue(NodeSemver.satisfies("22.23.1", "^22.19.0"));
        assertFalse(NodeSemver.satisfies("23.0.0", "^22.19.0"));
        assertTrue(NodeSemver.satisfies("22.20.5", "~22.20.0"));
        assertFalse(NodeSemver.satisfies("22.23.1", "~22.20.0"));
        assertTrue(NodeSemver.satisfies("22.23.1", "22"));
        assertTrue(NodeSemver.satisfies("22.23.1", "22.23"));
        assertTrue(NodeSemver.satisfies("22.23.1", "22.23.1"));
        assertFalse(NodeSemver.satisfies("22.23.1", "22.23.0"));
        assertTrue(NodeSemver.satisfies("22.23.1", "*"));
    }

    @Test
    void anUnreadableRangeIsNotTreatedAsSatisfied() {
        assertFalse(NodeSemver.satisfies("22.23.1", "banana"));
        assertFalse(NodeSemver.satisfies(null, ">=22"));
        assertFalse(NodeSemver.satisfies("", ">=22"));
    }
}
