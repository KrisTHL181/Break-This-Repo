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
package org.jackhuang.hmcl.web.developer;

import org.jackhuang.hmcl.web.config.DeveloperGuardScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Pins what the two gates cover. The filter class is the only place that
/// knows, so the table is asserted here rather than through a booted server —
/// these are the questions an operator asks ("does `instance` cover the start
/// button? does `off` really change nothing?"), and the answers must not drift
/// silently.
class DeveloperGuardFilterTest {

    @Test
    void offGatesNothing() {
        assertFalse(DeveloperGuardFilter.gated(DeveloperGuardScope.OFF, "/i/dsh-1/", "GET"));
        assertFalse(DeveloperGuardFilter.gated(DeveloperGuardScope.OFF, "/api/instances/dsh-1/start", "POST"));
        assertFalse(DeveloperGuardFilter.gated(DeveloperGuardScope.OFF, "/api/accounts", "POST"));
    }

    @Test
    void proxyGatesOnlyTheInstanceWebPages() {
        assertTrue(DeveloperGuardFilter.gated(DeveloperGuardScope.PROXY, "/i/dsh-1/", "GET"));
        assertTrue(DeveloperGuardFilter.gated(DeveloperGuardScope.PROXY, "/i/dsh-1/api/session", "POST"));
        // The instance APIs are not the proxy: they keep working.
        assertFalse(DeveloperGuardFilter.gated(DeveloperGuardScope.PROXY, "/api/instances/dsh-1/start", "POST"));
    }

    @Test
    void instanceLevelAddsTheMutationVerbs() {
        assertTrue(DeveloperGuardFilter.gated(DeveloperGuardScope.INSTANCE, "/i/dsh-1/", "GET"));
        assertTrue(DeveloperGuardFilter.gated(DeveloperGuardScope.INSTANCE, "/api/instances/dsh-1/start", "POST"));
        assertTrue(DeveloperGuardFilter.gated(DeveloperGuardScope.INSTANCE, "/api/instances/dsh-1", "DELETE"));
        // Reads stay open, and so does the developer page that explains the gate.
        assertFalse(DeveloperGuardFilter.gated(DeveloperGuardScope.INSTANCE, "/api/instances", "GET"));
        assertFalse(DeveloperGuardFilter.gated(DeveloperGuardScope.INSTANCE, "/api/developer/status", "GET"));
    }

    @Test
    void allLevelGatesEveryApiExceptTheAllowlist() {
        assertTrue(DeveloperGuardFilter.gated(DeveloperGuardScope.ALL, "/api/instances", "GET"));
        assertTrue(DeveloperGuardFilter.gated(DeveloperGuardScope.ALL, "/api/accounts", "POST"));
        // The minimum a browser needs to log in, elevate and read the status.
        assertFalse(DeveloperGuardFilter.gated(DeveloperGuardScope.ALL, "/api/health", "GET"));
        assertFalse(DeveloperGuardFilter.gated(DeveloperGuardScope.ALL, "/api/auth/login", "POST"));
        assertFalse(DeveloperGuardFilter.gated(DeveloperGuardScope.ALL, "/api/developer/elevate", "POST"));
    }

    @Test
    void emergencyStopFreezesPagesTerminalsAndStarts() {
        assertTrue(DeveloperGuardFilter.blockedByEmergency("/i/dsh-1/", "GET"));
        assertTrue(DeveloperGuardFilter.blockedByEmergency("/api/instances/dsh-1/terminal", "POST"));
        assertTrue(DeveloperGuardFilter.blockedByEmergency("/api/instances/dsh-1/start", "POST"));
        assertTrue(DeveloperGuardFilter.blockedByEmergency("/api/packs/install", "POST"));
        // Reads and the developer routes stay reachable: the status has to be
        // readable and the release button has to work, or the stop is a brick.
        assertFalse(DeveloperGuardFilter.blockedByEmergency("/api/instances", "GET"));
        assertFalse(DeveloperGuardFilter.blockedByEmergency("/api/developer/estop/release", "POST"));
        assertFalse(DeveloperGuardFilter.blockedByEmergency("/api/health", "GET"));
        // Static assets are not under /api and not under /i: the panel still loads.
        assertFalse(DeveloperGuardFilter.blockedByEmergency("/", "GET"));
    }
}
