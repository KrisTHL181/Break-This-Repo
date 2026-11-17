/*
 * HDSL-web
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

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The floor an installation puts under pnpm, and what it does about a version it cannot read.
///
/// Below the floor an install does not fail where anyone can see it: pnpm links the tree its own way
/// and what comes out is a harness that cannot start — a failure that looks like the harness's and
/// is diagnosed from the wrong end. So the version is read before the install and refused there.
/// The other half is what must **not** be refused: a version the launcher cannot parse is not
/// evidence of an old package manager, and turning a working machine into a blocked one over a
/// string this program did not write would be the worse mistake.
class DshVersionManagerPnpmTest {

    @Test
    void tenPointNineIsTheFloor() {
        assertFalse(DshVersionManager.isPnpmTooOld("10.9"), "the floor itself is usable");
        assertFalse(DshVersionManager.isPnpmTooOld("10.9.0"));
        assertFalse(DshVersionManager.isPnpmTooOld("10.9.4"));
        assertFalse(DshVersionManager.isPnpmTooOld("11.0.0"));

        assertTrue(DshVersionManager.isPnpmTooOld("10.8.9"), "anything under it is not");
        assertTrue(DshVersionManager.isPnpmTooOld("9.15.0"));
    }

    @Test
    void aVersionIsReadThroughItsPrefix() {
        assertFalse(DshVersionManager.isPnpmTooOld("v10.9.0"), "a `v` is a prefix, not a number");
        assertFalse(DshVersionManager.isPnpmTooOld("V11.7.0"));
        assertFalse(DshVersionManager.isPnpmTooOld(" 10.9.0 "), "and whitespace is not either");
    }

    @Test
    void theNumbersAreComparedAsNumbersAndNotAsText() {
        // `10.10` is above `10.9` whatever a string comparison says about it.
        assertFalse(DshVersionManager.isPnpmTooOld("10.10.0"));
        assertTrue(DshVersionManager.isPnpmTooOld("10.8.10"));
    }

    @Test
    void aPrereleaseOfTheFloorIsUnderIt() {
        // It sorts below the release by the rule the harness orders versions with, so it is under the
        // floor rather than equal to it.
        assertTrue(DshVersionManager.isPnpmTooOld("10.9.0-rc.1"));
        assertTrue(DshVersionManager.isPnpmTooOld("10.9.0-0"));
    }

    @Test
    void aVersionThatCannotBeReadDoesNotBlockAnInstall() {
        assertFalse(DshVersionManager.isPnpmTooOld(null), "no version read is not a version too old");
        assertFalse(DshVersionManager.isPnpmTooOld(""));
        assertFalse(DshVersionManager.isPnpmTooOld("  "));
        assertFalse(DshVersionManager.isPnpmTooOld("latest"));
        assertFalse(DshVersionManager.isPnpmTooOld("v"));
    }

    @Test
    void theRefusalNamesTheVersionThatIsTooOld() {
        DshException refused = assertThrows(DshException.class,
                () -> DshVersionManager.requireSupportedPnpm(runtime("10.8.0")),
                "an install under a pnpm below the floor is refused");

        assertTrue(refused.getMessage().contains("10.8.0"),
                "the refusal names what is installed: " + refused.getMessage());
        assertTrue(refused.getMessage().contains(DshNodeRuntime.MINIMUM_PNPM),
                "and what it has to be: " + refused.getMessage());

        assertDoesNotThrow(() -> DshVersionManager.requireSupportedPnpm(runtime("11.7.0")));
        assertDoesNotThrow(() -> DshVersionManager.requireSupportedPnpm(runtime(null)),
                "a pnpm whose version could not be read is not refused, it is missing at most");
    }

    /// A runtime whose pnpm answers the version given.
    ///
    /// @param pnpmVersion what `pnpm --version` answered, or `null` when it could not be read
    /// @return the runtime
    private static DshNodeRuntime runtime(String pnpmVersion) {
        return new DshNodeRuntime(Path.of("node"), "22.19.0", Path.of("npm"), "10.9.9",
                pnpmVersion == null ? null : Path.of("pnpm"), pnpmVersion);
    }
}