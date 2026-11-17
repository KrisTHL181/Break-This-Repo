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
package org.jackhuang.hmcl.util.platform;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The kernel watch budget: the limit is read where it can be, the headroom is
/// measured by asking the kernel for watches, and neither answer is ever turned
/// into a refusal (an unmeasurable host reads as unknown, not as none left).
class WatcherBudgetTest {

    /// The directory names the probe uses, in the temporary directory.
    private static Set<String> probes() throws IOException {
        Path tmp = Path.of(System.getProperty("java.io.tmpdir"));
        try (Stream<Path> entries = Files.list(tmp)) {
            Set<String> names = new HashSet<>();
            entries.filter(path -> path.getFileName().toString().startsWith("hdsl-watch-probe"))
                    .forEach(path -> names.add(path.getFileName().toString()));
            return names;
        }
    }

    @Test
    void theLimitIsReadWholeOnAHostThatHasOne() {
        int limit = WatcherBudget.limit();

        // A procfs node reports a size of zero, and a size-based read of it
        // returns its first byte — "6" for 65536. No kernel is configured with
        // a limit in the single digits, so that is what this catches.
        assertTrue(limit == -1 || limit >= 100,
                "the whole number, not its first byte: " + limit);
    }

    @Test
    void aLimitFileIsParsedAndItsAbsenceIsNotAnError(@TempDir Path dir) throws IOException {
        assertEquals(65536, WatcherBudget.limit(Files.writeString(dir.resolve("watches"), "65536\n")));
        assertEquals(65536, WatcherBudget.limit(Files.writeString(dir.resolve("no-newline"), "65536")));
        assertEquals(-1, WatcherBudget.limit(dir.resolve("absent")),
                "an unreadable knob is unknown, which is not zero");
        assertEquals(-1, WatcherBudget.limit(Files.writeString(dir.resolve("junk"), "not a number\n")));
    }

    @Test
    void theProbeAnswersWithACountAndCleansUpAfterItself() throws IOException {
        Set<String> before = probes();

        int free = WatcherBudget.headroom();

        assertEquals(before, probes(), "the probe's scratch directory is removed again");
        assertTrue(free >= -1, "the answer is a count or the unknown marker: " + free);
        if (Files.isReadable(Path.of(WatcherBudget.LIMIT_PATH))) {
            // The knob is exposed, so the question can be asked: an exhausted
            // budget answers 0, and nothing else can answer -1.
            assertTrue(free >= 0, "probed where the knob is readable: " + free);
        }
    }

    @Test
    void theProbeNeverAsksForMoreThanItSaysItDoes() {
        int free = WatcherBudget.headroom();

        assertTrue(free >= -1 && free <= WatcherBudget.PROBE,
                "at most PROBE registrations are attempted: " + free);
    }

    @Test
    void tooSmallFollowsTheThresholdAndLeavesUnknownAlone() {
        assertTrue(WatcherBudget.tooSmall(0), "nothing left is too small");
        assertTrue(WatcherBudget.tooSmall(WatcherBudget.MIN_FREE - 1));
        assertFalse(WatcherBudget.tooSmall(WatcherBudget.MIN_FREE));
        assertFalse(WatcherBudget.tooSmall(Integer.MAX_VALUE));
        assertFalse(WatcherBudget.tooSmall(-1), "unknown is not a shortage");
    }
}
