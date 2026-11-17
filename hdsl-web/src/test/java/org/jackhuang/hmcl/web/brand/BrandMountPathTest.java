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

/// Pins the URL the panel hands out for an instance.
///
/// The fixed `/i/octop/` mount used to exist only as an alias in the proxy: the
/// "open" button always returned the token path, so the fixed route was
/// unreachable from the UI (the audit reproduced exactly that).
class BrandMountPathTest {

    @Test
    void anOrdinaryInstanceGetsItsTokenMount() {
        assertEquals("/i/abc123/", BrandApiServlet.mountPath("", Brand.OCTOP, "abc123", null));
        // The survivor itself still gets the token mount while the alias is off.
        assertEquals("/i/abc123/", BrandApiServlet.mountPath("", Brand.OCTOP, "abc123", "other"));
    }

    @Test
    void theLockedSurvivorGetsTheFixedMount() {
        assertEquals("/i/octop/", BrandApiServlet.mountPath("", Brand.OCTOP, "abc123", "abc123"));
        assertEquals("/i/opencode/", BrandApiServlet.mountPath("", Brand.OPENCODE, "abc123", "abc123"));
    }

    @Test
    void aSubPathMountIsCarriedThrough() {
        assertEquals("/hdsl/i/abc123/", BrandApiServlet.mountPath("/hdsl", Brand.OCTOP, "abc123", null));
        assertEquals("/hdsl/i/octop/", BrandApiServlet.mountPath("/hdsl", Brand.OCTOP, "abc123", "abc123"));
        assertEquals("/hdsl/i/opencode/", BrandApiServlet.mountPath("/hdsl", Brand.OPENCODE, "abc123", "abc123"));
    }

    @Test
    void theAliasBelongsToTheTwoLastResortBrands() {
        // Only the brands whose policy can name a survivor publish a fixed door;
        // every other brand keeps its token mount whatever the policy claims.
        for (Brand brand : Brand.values()) {
            String expected = brand.supportsLastResort()
                    ? "/i/" + brand.id() + "/"
                    : "/i/abc123/";
            assertEquals(expected, BrandApiServlet.mountPath("", brand, "abc123", "abc123"),
                    brand.id());
        }
    }
}
