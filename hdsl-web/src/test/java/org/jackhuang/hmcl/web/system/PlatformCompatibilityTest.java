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
package org.jackhuang.hmcl.web.system;

import org.jackhuang.hmcl.util.platform.Architecture;
import org.jackhuang.hmcl.util.platform.OperatingSystem;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The platform declaration shown on the settings page. It is a fact sheet, so
/// the assertions pin which facts it carries — a silent regression here would
/// turn a known limitation back into a surprise during an install.
class PlatformCompatibilityTest {

    private static PlatformCompatibility.Item item(PlatformCompatibility.Report report, String id) {
        return report.items().stream().filter(candidate -> candidate.id().equals(id)).findFirst()
                .orElseThrow(() -> new AssertionError("no " + id + " in " + report.items()));
    }

    private static List<String> ids(PlatformCompatibility.Report report) {
        return report.items().stream().map(PlatformCompatibility.Item::id).toList();
    }

    @Test
    void freebsdDeclaresTheKnownGaps() {
        PlatformCompatibility.Report report = PlatformCompatibility.of(
                OperatingSystem.FREEBSD, Architecture.X86_64, "15.1-RELEASE", false);

        assertEquals(PlatformCompatibility.Level.LIMITED, report.level());
        assertEquals("freebsd", report.os());
        assertEquals("x86_64", report.arch());
        assertNotNull(report.summary());

        assertTrue(ids(report).containsAll(List.of(
                "core", "terminal", "dsh", "node", "opencode", "zcode", "mc", "privileges")));
        // The terminal is the thing this platform *gained*, so it must not be
        // filed as a limitation.
        assertEquals(PlatformCompatibility.Level.SUPPORTED, item(report, "terminal").level());
        assertEquals(PlatformCompatibility.Level.UNSUPPORTED, item(report, "dsh").level());
        assertEquals(PlatformCompatibility.Level.LIMITED, item(report, "node").level());
        assertEquals(PlatformCompatibility.Level.UNSUPPORTED, item(report, "opencode").level());
        assertEquals(PlatformCompatibility.Level.UNSUPPORTED, item(report, "zcode").level());
        assertEquals(PlatformCompatibility.Level.LIMITED, item(report, "mc").level());
    }

    @Test
    void procfsChangesOnlyTheMetricsLine() {
        PlatformCompatibility.Report withProc = PlatformCompatibility.of(
                OperatingSystem.FREEBSD, Architecture.X86_64, "15.1-RELEASE", true);
        assertFalse(ids(withProc).contains("metrics"), "procfs is mounted, so metrics are not a gap");

        PlatformCompatibility.Report withoutProc = PlatformCompatibility.of(
                OperatingSystem.FREEBSD, Architecture.X86_64, "15.1-RELEASE", false);
        assertEquals(PlatformCompatibility.Level.LIMITED, item(withoutProc, "metrics").level());
    }

    @Test
    void releasePlatformsHaveNoNotes() {
        for (OperatingSystem os : List.of(OperatingSystem.LINUX, OperatingSystem.MACOS,
                OperatingSystem.WINDOWS)) {
            PlatformCompatibility.Report report = PlatformCompatibility.of(
                    os, Architecture.X86_64, "test", false);
            assertEquals(PlatformCompatibility.Level.SUPPORTED, report.level(), os.toString());
            assertTrue(report.items().isEmpty(), os + " should not carry FreeBSD notes");
        }
    }

    @Test
    void anUnknownPlatformIsUnverifiedRatherThanSupported() {
        PlatformCompatibility.Report report = PlatformCompatibility.of(
                OperatingSystem.UNKNOWN, Architecture.UNKNOWN, "", false);
        assertEquals(PlatformCompatibility.Level.UNSUPPORTED, report.level());
        assertFalse(report.items().isEmpty());
    }
}