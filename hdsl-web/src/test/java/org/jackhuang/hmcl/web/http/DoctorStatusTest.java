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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/// The status a report section's own words imply. Tested here rather than
/// through the endpoint: a host that reports a shortage is not one a test can
/// conjure, and the mapping is what decides whether the panel shows the row as
/// something to act on.
class DoctorStatusTest {

    @Test
    void aShortageIsAWarningRatherThanSilence() {
        String body = "  inotify watches: 1 free of 65536  [SHORTAGE]\n"
                + "    sysctl -w /proc/sys/fs/inotify/max_user_watches=524288";

        assertEquals("warn", DoctorApiServlet.statusOf(body),
                "the file-watcher row must read as something to act on");
    }

    @Test
    void theMarkersTheReportAlreadyUsesKeepTheirMeanings() {
        assertEquals("fail", DoctorApiServlet.statusOf("  node:  NOT FOUND"));
        assertEquals("fail", DoctorApiServlet.statusOf("  node:  22.0.0  [UNSUPPORTED]"));
        assertEquals("fail", DoctorApiServlet.statusOf("  packages:  FAILED: timeout"));
        assertEquals("warn", DoctorApiServlet.statusOf("  dsh-1  dsh 0.2.0  [INCOMPLETE]"));
        assertEquals("warn", DoctorApiServlet.statusOf("  skipped (toolchain incomplete)"));
        assertEquals("warn", DoctorApiServlet.statusOf("  plugin management unavailable"));
        assertEquals("ok", DoctorApiServlet.statusOf("  inotify watches: 3000 free of 65536  [ok]"));
        assertEquals("ok", DoctorApiServlet.statusOf("  node:  22.0.0  (/opt/node/bin/node)"));
    }
}
