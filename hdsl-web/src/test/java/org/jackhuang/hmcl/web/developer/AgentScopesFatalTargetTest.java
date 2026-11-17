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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The route shapes that would end the caller, across all three families.
///
/// The dsh shape alone once carried this rule, and the brand and ZCode shapes
/// ended the instance the caller was running in — the same death under another
/// URL. This matrix pins every family to every operation, so a new prefix or
/// a new operation has to land here before it can ship.
class AgentScopesFatalTargetTest {

    private static final AgentScopes.Fatal KIMI_STOP = new AgentScopes.Fatal("kimi", "a", false);

    @Test
    void everyFamilyRecognisesItsOwnShapes() {
        // dsh: /api/instances/{id}…
        assertEquals(new AgentScopes.Fatal("dsh", "a", false), AgentScopes.fatalTarget("POST", "/api/instances/a/stop"));
        assertEquals(new AgentScopes.Fatal("dsh", "a", false), AgentScopes.fatalTarget("POST", "/api/instances/a/terminal"));
        assertEquals(new AgentScopes.Fatal("dsh", "a", false), AgentScopes.fatalTarget("DELETE", "/api/instances/a/terminal"));
        assertEquals(new AgentScopes.Fatal("dsh", "a", false), AgentScopes.fatalTarget("DELETE", "/api/instances/a"));
        assertEquals(new AgentScopes.Fatal("dsh", "a", true), AgentScopes.fatalTarget("POST", "/api/instances/a/launch"));

        // brands: /api/brands/{brand}/instances/{id}…
        assertEquals(new AgentScopes.Fatal("kimi", "a", false),
                AgentScopes.fatalTarget("POST", "/api/brands/kimi/instances/a/stop"));
        assertEquals(new AgentScopes.Fatal("kimi", "a", false),
                AgentScopes.fatalTarget("POST", "/api/brands/kimi/instances/a/terminal"));
        assertEquals(new AgentScopes.Fatal("kimi", "a", false),
                AgentScopes.fatalTarget("DELETE", "/api/brands/kimi/instances/a/terminal"));
        assertEquals(new AgentScopes.Fatal("kimi", "a", false),
                AgentScopes.fatalTarget("DELETE", "/api/brands/kimi/instances/a"));
        assertEquals(new AgentScopes.Fatal("kimi", "a", true),
                AgentScopes.fatalTarget("POST", "/api/brands/kimi/instances/a/launch"));

        // ZCode: /api/zcode/instances/{id}…
        assertEquals(new AgentScopes.Fatal("zcode", "a", false),
                AgentScopes.fatalTarget("POST", "/api/zcode/instances/a/stop"));
        assertEquals(new AgentScopes.Fatal("zcode", "a", false),
                AgentScopes.fatalTarget("POST", "/api/zcode/instances/a/terminal"));
        assertEquals(new AgentScopes.Fatal("zcode", "a", false),
                AgentScopes.fatalTarget("DELETE", "/api/zcode/instances/a"));
        assertEquals(new AgentScopes.Fatal("zcode", "a", true),
                AgentScopes.fatalTarget("POST", "/api/zcode/instances/a/launch"));
    }

    @Test
    void nothingElseIsFatal() {
        // A read is never fatal, whatever it names.
        assertNull(AgentScopes.fatalTarget("GET", "/api/instances/a/stop"));
        assertNull(AgentScopes.fatalTarget("GET", "/api/zcode/instances/a/launch"));
        // The terminal route is the one exception among reads: POST opens it
        // (stopping the web runtime first) and DELETE kills it, so every verb
        // that reaches the route is fatal — the others are never served.
        assertEquals(new AgentScopes.Fatal("kimi", "a", false),
                AgentScopes.fatalTarget("GET", "/api/brands/kimi/instances/a/terminal"));
        // A stop with the wrong verb is not the route the servlet serves.
        assertNull(AgentScopes.fatalTarget("DELETE", "/api/instances/a/stop"));
        assertNull(AgentScopes.fatalTarget("PATCH", "/api/brands/kimi/instances/a"));
        // Creating, patching, reading and plugin work end nobody.
        assertNull(AgentScopes.fatalTarget("POST", "/api/instances"));
        assertNull(AgentScopes.fatalTarget("POST", "/api/instances/a/plugins/install"));
        assertNull(AgentScopes.fatalTarget("POST", "/api/instances/a/sessions/x/resume"));
        assertNull(AgentScopes.fatalTarget("POST", "/api/brands/kimi/instances/a/reveal"));
        assertNull(AgentScopes.fatalTarget("GET", "/api/brands/kimi/instances/a/terminals"));
        // An MC instance is not the caller's home; stopping one is ordinary work.
        assertNull(AgentScopes.fatalTarget("POST", "/api/mc/instances/a/stop"));
        // Outside the API there is no target at all.
        assertNull(AgentScopes.fatalTarget("POST", "/i/a/stop"));
        assertNull(AgentScopes.fatalTarget("POST", "/api"));
    }

    @Test
    void aTrailingSlashIsTheSameRoute() {
        assertEquals(new AgentScopes.Fatal("dsh", "a", false), AgentScopes.fatalTarget("POST", "/api/instances/a/stop/"));
        assertEquals(new AgentScopes.Fatal("kimi", "a", false),
                AgentScopes.fatalTarget("POST", "/api/brands/kimi/instances/a/terminal/"));
    }

    @Test
    void aBindingNamesItsTargetByBareIdOrByFamilyAndId() {
        // A claim from the mint body is a bare id; the panel's own derivation
        // is family/id. Both must name the target; neither may name another.
        assertTrue(KIMI_STOP.matchesBound("a"));
        assertTrue(KIMI_STOP.matchesBound("kimi/a"));
        assertFalse(KIMI_STOP.matchesBound("kimi/b"));
        assertFalse(KIMI_STOP.matchesBound("dsh/a"));
        assertFalse(KIMI_STOP.matchesBound(null));
        assertFalse(KIMI_STOP.matchesBound(""));

        AgentScopes.Fatal derived = AgentScopes.fatalTarget("POST", "/api/brands/kimi/instances/a/stop");
        assertEquals(KIMI_STOP, derived);
        assertTrue(derived.matchesBound("kimi/a"));
        assertFalse(derived.matchesBound("zcode/a"));

        // An identity the panel derived names the target only when both
        // halves agree — the family keeps two id spaces apart.
        assertTrue(KIMI_STOP.matches("kimi", "a"));
        assertFalse(KIMI_STOP.matches("dsh", "a"));
        assertFalse(KIMI_STOP.matches("kimi", "b"));
    }

    @Test
    void selfFatalCoversAllThreeFamiliesAndExcludesLaunch() {
        // The bound fallback of the gate: refusing on a match, per family.
        assertTrue(AgentScopes.selfFatal("POST", "/api/instances/a/stop", "a"));
        assertTrue(AgentScopes.selfFatal("POST", "/api/brands/kimi/instances/a/stop", "a"));
        assertTrue(AgentScopes.selfFatal("POST", "/api/zcode/instances/a/stop", "a"));
        assertTrue(AgentScopes.selfFatal("DELETE", "/api/brands/kimi/instances/a", "kimi/a"));
        assertTrue(AgentScopes.selfFatal("POST", "/api/zcode/instances/a/terminal", "zcode/a"));

        // Launch is conditionally fatal, so the static answer stays no.
        assertFalse(AgentScopes.selfFatal("POST", "/api/instances/a/launch", "a"));
        assertFalse(AgentScopes.selfFatal("POST", "/api/brands/kimi/instances/a/launch", "kimi/a"));

        // Another instance, and no binding at all, are not this rule's to refuse.
        assertFalse(AgentScopes.selfFatal("POST", "/api/instances/b/stop", "a"));
        assertFalse(AgentScopes.selfFatal("POST", "/api/brands/kimi/instances/b/stop", "kimi/a"));
        assertFalse(AgentScopes.selfFatal("POST", "/api/instances/a/stop", null));
    }
}
