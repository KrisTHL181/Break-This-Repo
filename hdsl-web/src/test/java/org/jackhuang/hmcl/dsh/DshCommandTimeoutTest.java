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

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies the deadline a bounded run puts on a command.
 *
 * The child outlives every deadline here, so a run that returned in time was stopped.
 */
class DshCommandTimeoutTest {
    /**
     * A command that outlives its deadline is stopped, and the result says the deadline was what
     * stopped it.
     */
    @Test
    void stopsACommandThatOutlivesItsDeadline() throws IOException, InterruptedException {
        long start = System.nanoTime();
        DshCommand.Result result = DshCommand.run(
                Sleeper.command("120"), Duration.ofMillis(300), null, Map.of(), null);
        long elapsed = Duration.ofNanos(System.nanoTime() - start).toMillis();

        assertTrue(result.timedOut(), "a command stopped for running out of time says so");
        assertFalse(result.isSuccess(), "a command that was stopped has not succeeded");
        // The deadline plus the one second a killed process is given to disappear.
        assertTrue(elapsed < 3_000, "the deadline bounds the whole wait, took " + elapsed + "ms");
    }

    /**
     * A command that answers in time is not reported as having run out of it.
     */
    @Test
    void leavesACommandThatAnswersAlone() throws IOException, InterruptedException {
        DshCommand.Result result = DshCommand.run(
                Sleeper.command("0", "0"), Duration.ofSeconds(60), null, Map.of(), null);

        assertFalse(result.timedOut(), "a command that answered did not run out of time");
        assertEquals(0, result.exitCode());
        assertTrue(result.isSuccess());
    }

    /**
     * A failing exit code is the program's own, not a deadline.
     */
    @Test
    void tellsAFailedCommandFromOneThatRanOutOfTime() throws IOException, InterruptedException {
        DshCommand.Result result = DshCommand.run(
                Sleeper.command("0", "3"), Duration.ofSeconds(60), null, Map.of(), null);

        assertFalse(result.timedOut(), "the command ran to its own end and chose its own code");
        assertEquals(3, result.exitCode());
        assertFalse(result.isSuccess());
    }

    /**
     * A command given no deadline is waited for, whatever it takes.
     */
    @Test
    void waitsOutACommandWithNoDeadline() throws IOException, InterruptedException {
        DshCommand.Result result = DshCommand.run(Sleeper.command("0", "0"), null, Map.of(), null);

        assertFalse(result.timedOut());
        assertTrue(result.isSuccess());
    }

    /**
     * A command that does not exist is an error, not a timeout.
     */
    @Test
    void reportsACommandItCannotStartAsAFailure() {
        assertThrows(IOException.class, () ->
                DshCommand.run(List.of("no-such-program-anywhere", "x"), Duration.ofSeconds(5), null, Map.of(), null));
    }
}
