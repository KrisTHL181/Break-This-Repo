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
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The last-resort switches: defaults, survivor semantics, validation and the
/// brand-wide Node selector.
class BrandPolicyTest {

    @Test
    void defaultsAreOffAndEveryInstanceIsEnabled(@TempDir Path dataDir) {
        BrandPolicy policy = new BrandPolicy(dataDir);
        BrandPolicy.Entry entry = policy.get(Brand.OCTOP);
        assertFalse(entry.singleInstance());
        assertNull(entry.survivor());
        assertFalse(entry.lockedRoute());
        assertFalse(entry.sourceBuild());
        assertNull(entry.nodeRuntime());
        assertFalse(policy.disabled(Brand.OCTOP, "any-token"));
        assertNull(policy.lockedSurvivor(Brand.OCTOP));
        assertNull(policy.lockedSurvivor(Brand.OPENCODE));
    }

    @Test
    void singleInstanceRequiresASurvivorAndDisablesTheRest(@TempDir Path dataDir) throws Exception {
        BrandPolicy policy = new BrandPolicy(dataDir);
        assertThrows(IllegalArgumentException.class,
                () -> policy.set(Brand.OCTOP,
                        new BrandPolicy.Entry(true, null, false, false, null)));

        policy.set(Brand.OCTOP, new BrandPolicy.Entry(true, "keep-me", false, false, null));
        assertFalse(policy.disabled(Brand.OCTOP, "keep-me"));
        assertTrue(policy.disabled(Brand.OCTOP, "other"));
        // Other brands are independent.
        assertFalse(policy.disabled(Brand.KIMI, "other"));
    }

    @Test
    void lockedRouteRequiresSingleInstanceAndASurvivor(@TempDir Path dataDir) {
        BrandPolicy policy = new BrandPolicy(dataDir);
        assertThrows(IllegalArgumentException.class,
                () -> policy.set(Brand.OCTOP,
                        new BrandPolicy.Entry(false, null, true, false, null)));
        assertThrows(IllegalArgumentException.class,
                () -> policy.set(Brand.OCTOP,
                        new BrandPolicy.Entry(true, null, true, false, null)));
    }

    @Test
    void lockedRouteSurvivesAReadBack(@TempDir Path dataDir) throws Exception {
        BrandPolicy policy = new BrandPolicy(dataDir);
        policy.set(Brand.OCTOP, new BrandPolicy.Entry(true, "keep-me", true, false, null));
        assertEquals("keep-me", policy.lockedSurvivor(Brand.OCTOP));
        assertEquals("keep-me", new BrandPolicy(dataDir).lockedSurvivor(Brand.OCTOP));
    }

    @Test
    void writerKeepsOtherBrands(@TempDir Path dataDir) throws Exception {
        BrandPolicy policy = new BrandPolicy(dataDir);
        policy.set(Brand.OPENCODE, new BrandPolicy.Entry(true, "oc-one", true, false, null));
        policy.set(Brand.OCTOP, new BrandPolicy.Entry(true, "octop-one", false, false, null));
        assertEquals("oc-one", policy.get(Brand.OPENCODE).survivor());
        assertEquals("octop-one", policy.get(Brand.OCTOP).survivor());
    }

    /// The last-resort switches exist only for the two brands whose own agent
    /// can wedge them; a policy file written before that narrowing is read back
    /// with the switches dropped, so no instance stays disabled without a
    /// control to undo it.
    @Test
    void theLastResortSwitchesBelongToOctopAndOpencode(@TempDir Path dataDir) throws Exception {
        BrandPolicy policy = new BrandPolicy(dataDir);
        assertThrows(IllegalArgumentException.class,
                () -> policy.set(Brand.KIMI, new BrandPolicy.Entry(true, "kimi-one", false, false, null)));
        assertThrows(IllegalArgumentException.class,
                () -> policy.set(Brand.HERMES, new BrandPolicy.Entry(false, null, true, false, null)));

        Files.createDirectories(dataDir.resolve("brands"));
        Files.writeString(dataDir.resolve("brands").resolve("policy.json"), """
                {"kimi":{"singleInstance":true,"survivor":"kimi-one","lockedRoute":true}}
                """);
        BrandPolicy.Entry kimi = new BrandPolicy(dataDir).get(Brand.KIMI);
        assertFalse(kimi.singleInstance());
        assertNull(kimi.survivor());
        assertFalse(kimi.lockedRoute());
        assertFalse(new BrandPolicy(dataDir).disabled(Brand.KIMI, "other"));
        assertNull(new BrandPolicy(dataDir).lockedSurvivor(Brand.KIMI));
    }

    @Test
    void nodeRuntimeSurvivesAReadBackAndIsValidated(@TempDir Path dataDir) throws Exception {
        BrandPolicy policy = new BrandPolicy(dataDir);
        policy.set(Brand.KIMI, BrandPolicy.Entry.DEFAULTS.withNodeRuntime("22"));
        assertEquals("22", new BrandPolicy(dataDir).get(Brand.KIMI).nodeRuntime());

        assertThrows(IllegalArgumentException.class,
                () -> policy.set(Brand.KIMI, BrandPolicy.Entry.DEFAULTS.withNodeRuntime("not a selector")));
    }
}
