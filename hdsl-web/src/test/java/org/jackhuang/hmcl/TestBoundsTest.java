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
package org.jackhuang.hmcl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/// Pins that the bounds the build puts on this suite are bounds JUnit reads.
///
/// The mistake this exists for: the first version of these bounds was set as
/// `junit.jupiter.execution.timeout.method.default`, which is not a name JUnit knows. JUnit's own
/// names are `junit.jupiter.execution.timeout.default` and its `…testable.method…`,
/// `…lifecycle.method…` and `…beforeall.method…` siblings — `org.junit.jupiter.engine.Constants`
/// lists them. A setting nobody reads is worse than one nobody wrote, because the build file reads
/// as though the suite were bounded: a runner whose JavaFX toolkit never came up then held a build
/// for seventy-six minutes, and nothing in the log said the bound was missing. `assertNull` below is
/// the tripwire for that name in particular.
class TestBoundsTest {

    @Test
    void everyTestAndLifecycleMethodIsBounded() {
        String bound = System.getProperty("junit.jupiter.execution.timeout.default");

        assertNotNull(bound, "the suite has no timeout bound, so a wedged test holds the build");
        assertEquals("120s", bound, "the bound is the one the build sets, in JUnit's own spelling");
    }

    @Test
    void aTestThatReachesItsBoundIsReportedWithItsStacks() {
        assertEquals("true", System.getProperty("junit.jupiter.execution.timeout.threaddump.enabled"),
                "a timeout without a thread dump says that something hung and not what did");
    }

    @Test
    void theNameJUnitDoesNotReadIsNotTheOneUsed() {
        assertNull(System.getProperty("junit.jupiter.execution.timeout.method.default"),
                "`junit.jupiter.execution.timeout.method.default` is not a key JUnit reads: it is ignored "
                        + "in silence. The names it does read are `junit.jupiter.execution.timeout.default` "
                        + "and its `…testable.method…`, `…lifecycle.method…`, `…beforeall.method…` siblings.");
    }
}
