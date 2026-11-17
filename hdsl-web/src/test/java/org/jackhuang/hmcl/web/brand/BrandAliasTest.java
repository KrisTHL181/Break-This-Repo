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
package org.jackhuang.hmcl.web.brand;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/// The reserved `<brand>` mount ids: `/i/octop/` and `/i/opencode/` forward to
/// their survivor, and only the two brands that own a last-resort door can
/// claim one.
class BrandAliasTest {

    @Test
    void aLockedBrandAnswersItsReservedMount() {
        try {
            BrandRuntime.setLockedSurvivor(Brand.OPENCODE, "oc-token");
            assertEquals(Brand.OPENCODE, BrandRuntime.brandForMount("opencode"));
            assertEquals("oc-token", BrandRuntime.targetForMount("opencode"),
                    "the alias must resolve to the survivor for both the page and its WebSocket");
        } finally {
            BrandRuntime.setLockedSurvivor(Brand.OPENCODE, null);
        }
    }

    @Test
    void aBrandWithoutTheCapabilityCannotClaimAnAlias() {
        try {
            BrandRuntime.setLockedSurvivor(Brand.KIMI, "kimi-token");
            assertNull(BrandRuntime.brandForMount("kimi"));
            assertEquals("kimi", BrandRuntime.targetForMount("kimi"),
                    "the id keeps meaning an instance id, not a door");
        } finally {
            BrandRuntime.setLockedSurvivor(Brand.KIMI, null);
        }
    }

    @Test
    void closingTheDoorRestoresTheTokenMeaning() {
        try {
            BrandRuntime.setLockedSurvivor(Brand.OCTOP, null);
            assertNull(BrandRuntime.brandForMount("octop"));
            assertEquals("octop", BrandRuntime.targetForMount("octop"),
                    "with the route off, the id is not reserved by the runtime");
        } finally {
            BrandRuntime.setLockedSurvivor(Brand.OCTOP, null);
        }
    }
}
