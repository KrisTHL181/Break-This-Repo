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

import org.jackhuang.hmcl.util.platform.WatcherBudget;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The file-watcher row of the diagnostics report: a shortage has to name the
/// knob and the command that raises it, because the panel's own symptom (an
/// instance that will not start, or starts without an address) never mentions
/// either.
class DshDoctorWatchersTest {

    /// Renders the row with a fixed reading, so all three branches can be
    /// checked without moving the machine's real budget.
    private static String row(int limit, int free) {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (PrintStream out = new PrintStream(buffer, true, StandardCharsets.UTF_8)) {
            DshDoctor.printFileWatchers(out, limit, free);
        }
        return buffer.toString(StandardCharsets.UTF_8);
    }

    @Test
    void shortageNamesTheLimitAndTheSysctlThatRaisesIt() {
        String row = row(65536, 1);

        assertTrue(row.contains("[SHORTAGE]"), row);
        assertTrue(row.contains("1 free of 65536"), row);
        assertTrue(row.contains(WatcherBudget.LIMIT_PATH), row);
        assertTrue(row.contains(WatcherBudget.LIMIT_PATH + "=524288"),
                "the command has to be copy-pasteable: " + row);
    }

    @Test
    void roomLeftIsReportedAsOkAndSaysNothingAlarming() {
        String row = row(65536, WatcherBudget.PROBE);

        assertTrue(row.contains("at least " + WatcherBudget.PROBE + " free of 65536"),
                "a probe that got everything it asked for shows a floor, not a total: " + row);
        assertTrue(row.contains("[ok]"), row);
        assertFalse(row.contains("[SHORTAGE]"), row);
        assertFalse(row.contains("sysctl"), "nothing to do, nothing to print: " + row);
    }

    @Test
    void anUnmeasurableBudgetIsNeitherOkNorShortage() {
        String row = row(-1, -1);

        assertTrue(row.contains("unmeasurable"), row);
        assertFalse(row.contains("[SHORTAGE]"), "unknown must not be read as none left: " + row);
        assertFalse(row.contains("[ok]"), row);
    }

    @Test
    void anUnknownLimitStillReportsTheHeadroom() {
        String row = row(-1, WatcherBudget.PROBE);

        assertTrue(row.contains("at least " + WatcherBudget.PROBE + " free of an unknown limit"), row);
        assertTrue(row.contains("[ok]"), row);
    }
}
