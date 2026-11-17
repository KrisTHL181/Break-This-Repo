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
package org.jackhuang.hmcl.web.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Pins the developer guard's scope contract: parsing, the waterfall of the
/// three "covers" answers, and the two path tables (what counts as an instance
/// mutation, what stays reachable at every scope).
///
/// The gate itself lives in
/// [org.jackhuang.hmcl.web.developer.DeveloperGuardFilter]; this class is the
/// half that must stay true no matter how the filter is wired.
class DeveloperGuardScopeTest {

    @Test
    void parsesTheFourDocumentedValues() {
        assertEquals(DeveloperGuardScope.OFF, DeveloperGuardScope.parse("off"));
        assertEquals(DeveloperGuardScope.PROXY, DeveloperGuardScope.parse("proxy"));
        assertEquals(DeveloperGuardScope.INSTANCE, DeveloperGuardScope.parse("instance"));
        assertEquals(DeveloperGuardScope.ALL, DeveloperGuardScope.parse("all"));
        // Tolerated: surrounding blanks and a different case, because both come
        // from hand-edited files and environment variables.
        assertEquals(DeveloperGuardScope.PROXY, DeveloperGuardScope.parse("  PROXY "));
        // Anything else is a configuration error, not a silent fallback.
        assertNull(DeveloperGuardScope.parse("on"));
        assertNull(DeveloperGuardScope.parse(""));
        assertNull(DeveloperGuardScope.parse(null));
    }

    @Test
    void eachLevelIncludesThePreviousOne() {
        assertFalse(DeveloperGuardScope.OFF.coversProxy());
        assertFalse(DeveloperGuardScope.OFF.coversInstanceActions());
        assertFalse(DeveloperGuardScope.OFF.coversAllApi());

        assertTrue(DeveloperGuardScope.PROXY.coversProxy());
        assertFalse(DeveloperGuardScope.PROXY.coversInstanceActions());
        assertFalse(DeveloperGuardScope.PROXY.coversAllApi());

        assertTrue(DeveloperGuardScope.INSTANCE.coversProxy());
        assertTrue(DeveloperGuardScope.INSTANCE.coversInstanceActions());
        assertFalse(DeveloperGuardScope.INSTANCE.coversAllApi());

        assertTrue(DeveloperGuardScope.ALL.coversProxy());
        assertTrue(DeveloperGuardScope.ALL.coversInstanceActions());
        assertTrue(DeveloperGuardScope.ALL.coversAllApi());
    }

    @Test
    void instanceLevelGatesTheMutationVerbsOnly() {
        // Starting, stopping, deleting, installing: all mutations.
        assertTrue(DeveloperGuardScope.instanceMutation("/api/instances/dsh-1/start", "POST"));
        assertTrue(DeveloperGuardScope.instanceMutation("/api/instances/dsh-1", "DELETE"));
        assertTrue(DeveloperGuardScope.instanceMutation("/api/brands/kimi/instances/x", "POST"));
        assertTrue(DeveloperGuardScope.instanceMutation("/api/zcode/instances/x/start", "POST"));
        assertTrue(DeveloperGuardScope.instanceMutation("/api/packs/install", "POST"));
        assertTrue(DeveloperGuardScope.instanceMutation("/api/settings/tls", "PUT"));
        // Reads stay open: the list and the logs are how an operator sees what
        // is happening.
        assertFalse(DeveloperGuardScope.instanceMutation("/api/instances", "GET"));
        assertFalse(DeveloperGuardScope.instanceMutation("/api/instances/dsh-1/logs", "GET"));
        // And unrelated surfaces are not instance mutations.
        assertFalse(DeveloperGuardScope.instanceMutation("/api/accounts", "POST"));
    }

    @Test
    void theAllowlistIsTheMinimumABrowserNeeds() {
        assertTrue(DeveloperGuardScope.alwaysAllowed("/api/health"));
        assertTrue(DeveloperGuardScope.alwaysAllowed("/api/auth/login"));
        assertTrue(DeveloperGuardScope.alwaysAllowed("/api/auth/logout"));
        assertTrue(DeveloperGuardScope.alwaysAllowed("/api/developer/status"));
        assertTrue(DeveloperGuardScope.alwaysAllowed("/api/developer/elevate"));
        // The host readings are what a shell in the same container can read, so
        // gating them behind a password would be theatre; the monitor card must
        // also work at `all` or the general page looks broken.
        assertTrue(DeveloperGuardScope.alwaysAllowed("/api/system/stats"));
        assertTrue(DeveloperGuardScope.alwaysAllowed("/api/system/info"));
        // Everything else is gated at `all` — including the instance list, which
        // is exactly what the scope is for.
        assertFalse(DeveloperGuardScope.alwaysAllowed("/api/instances"));
        assertFalse(DeveloperGuardScope.alwaysAllowed("/api/instances/dsh-1"));
        assertEquals("all", DeveloperGuardScope.ALL.wireName());
    }
}
