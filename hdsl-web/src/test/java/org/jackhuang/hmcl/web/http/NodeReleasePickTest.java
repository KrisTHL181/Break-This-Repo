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
package org.jackhuang.hmcl.web.http;

import org.jackhuang.hmcl.dsh.NodeRelease;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Which release the one-click Node.js install picks.
///
/// `nodejs.org/dist` lists pre-releases in the same index and above every
/// stable line — on 2026-10-30 the first entry was `v27.0.0-alpha.0.0.0` — so
/// "the newest supported release" without a pre-release filter hands every new
/// dsh instance an alpha runtime. That is what these tests pin down.
class NodeReleasePickTest {

    private static NodeRelease release(String version) {
        return new NodeRelease(version, null, "2026-10-30");
    }

    @Test
    void skipsPrereleasesAboveTheStableLine() {
        assertEquals("26.13.0", NodeApiServlet.pickNewestSupported(List.of(
                release("27.0.0-alpha.0.0.0"),
                release("27.0.0-rc.1"),
                release("26.13.0"),
                release("26.12.1"))));
    }

    @Test
    void skipsVersionsTheHarnessCannotUse() {
        // 23.x satisfies neither ^22.19 nor >=24.
        assertEquals("24.9.0", NodeApiServlet.pickNewestSupported(List.of(
                release("24.9.0"),
                release("23.11.0"),
                release("22.18.0"))));
    }

    @Test
    void takesTheNewest22WhenItIsTheOnlySupportedLine() {
        assertEquals("22.19.0", NodeApiServlet.pickNewestSupported(List.of(
                release("22.19.0"),
                release("22.18.1"),
                release("20.19.0"))));
    }

    @Test
    void refusesWhenOnlyPrereleasesOrUnsupportedVersionsAreListed() {
        IllegalStateException thrown = assertThrows(IllegalStateException.class,
                () -> NodeApiServlet.pickNewestSupported(List.of(
                        release("27.0.0-alpha.0.0.0"),
                        release("20.19.0"))));
        assertTrue(thrown.getMessage().contains("no supported Node.js release"));
    }

    @Test
    void recognisesPrereleaseSuffixes() {
        assertTrue(NodeApiServlet.isPrerelease("27.0.0-alpha.0.0.0"));
        assertTrue(NodeApiServlet.isPrerelease("26.0.0-rc.1"));
        assertTrue(!NodeApiServlet.isPrerelease("26.13.0"));
    }
}