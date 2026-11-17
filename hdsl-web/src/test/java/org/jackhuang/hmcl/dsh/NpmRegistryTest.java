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

import org.jackhuang.hmcl.setting.LauncherSettings;
import org.jackhuang.hmcl.setting.SettingsManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The download source: the mirrors the panel offers, the gate every address goes through, and the
/// order the setting, the deployment and the published registry answer in.
///
/// [NpmRegistry#normalize] is the gate, and what it protects is where the value ends up rather than
/// where it came from — pnpm's own `config.yaml`, and the `--registry=` argument on child processes.
/// A value carrying a newline writes a second setting into the file; one carrying a quote closes the
/// scalar it was meant to be. So the refusals below are the feature, not the error path.
class NpmRegistryTest {

    /// The tests below set the process-wide settings, which every other test class in this JVM
    /// shares: the choice is put back to the default so nothing leaks out of this one.
    @AfterEach
    void restoreTheDefaultChoice() {
        LauncherSettings settings = SettingsManager.settings();
        settings.setNpmRegistryPreset(NpmRegistry.ENVIRONMENT);
        settings.setNpmRegistry("");
    }

    @Test
    void everyOfferedMirrorIsAnAddressThisLauncherWouldUse() throws Exception {
        List<NpmRegistry.Preset> presets = NpmRegistry.presets();
        List<String> ids = new ArrayList<>();
        for (NpmRegistry.Preset preset : presets) {
            ids.add(preset.id());
            assertFalse(preset.label().isBlank(), preset.id());
            assertFalse(preset.note().isBlank(), preset.id());
            if (NpmRegistry.ENVIRONMENT.equals(preset.id())) {
                // The deployment's own source names no address of its own; choosing it is choosing
                // to read NPM_CONFIG_REGISTRY.
                assertEquals("", preset.url(), "the environment preset names no mirror");
                assertNull(preset.electronMirror(), "nothing is known about the environment's Electron copy");
                continue;
            }
            assertFalse(preset.url().isEmpty(), preset.id());
            assertTrue(NpmRegistry.isValid(preset.url()), preset.url());
            assertEquals(preset.url(), NpmRegistry.normalize(preset.url()),
                    "a published address is already the one spelling the launcher uses");
        }
        assertEquals(6, presets.size(), ids.toString());
        assertTrue(ids.containsAll(List.of(
                        NpmRegistry.ENVIRONMENT, "npmjs", "npmmirror", "ustc", "tencent", "huawei")),
                ids.toString());
    }

    @Test
    void aTrailingSlashIsDropped() throws Exception {
        // Corepack joins its own `/pnpm/latest` to this value, and mirrors answer two slashes with a
        // 404 — so the one spelling the launcher keeps has none.
        assertEquals("https://registry.npmmirror.com",
                NpmRegistry.normalize("https://registry.npmmirror.com/"));
        assertEquals("https://registry.npmmirror.com",
                NpmRegistry.normalize("  https://registry.npmmirror.com/  "),
                "surrounding space is trimmed before anything else looks at the value");
        assertEquals("https://a.example", NpmRegistry.normalize("https://a.example///"));
    }

    @Test
    void aRegistryOnTheLocalNetworkIsAccepted() throws Exception {
        // The case the setting exists for after the mirrors: a Nexus or Verdaccio inside the house.
        assertEquals("http://192.0.2.1:4873", NpmRegistry.normalize("http://192.0.2.1:4873/"));
        assertEquals("http://192.0.2.1:4873", NpmRegistry.normalize("http://192.0.2.1:4873"));
        assertEquals("https://mirrors.cloud.tencent.com/npm",
                NpmRegistry.normalize("https://mirrors.cloud.tencent.com/npm"),
                "an address with a path keeps it");
        assertEquals("http://127.0.0.1:4873/repository/npm-group",
                NpmRegistry.normalize("http://127.0.0.1:4873/repository/npm-group/"),
                "a Nexus group URL keeps its path and loses the slash");
        assertTrue(NpmRegistry.isValid(NpmRegistry.DEFAULT));
        assertFalse(NpmRegistry.isValid(null), "no address is not an address");
    }

    @Test
    void anAddressThatWouldWriteASecondSettingOrBreakTheLineIsRefused() {
        Map<String, String> refused = new LinkedHashMap<>();
        refused.put("", "empty");
        refused.put("   ", "blank");
        refused.put("javascript:alert(1)", "a scheme that is not http");
        refused.put("file:///etc/passwd", "a scheme that is not http");
        refused.put("https://user:pass@host/", "credentials in the address");
        refused.put("https://x/\nstoreDir: /etc", "a newline writes a second setting into pnpm's file");
        refused.put("https://x/\rstoreDir: /etc", "a carriage return does the same");
        refused.put("https://x/\tstoreDir: /etc", "so does a tab");
        refused.put("https://a b/", "a space");
        refused.put("https://a.example/\u007f", "a DEL character");
        for (char character : new char[]{'"', '\'', '`', '\\', '#', '$', ';', '|', '&', '@'}) {
            refused.put("https://a.example/" + character, "the character " + character);
        }
        refused.put("https:///x", "no host");
        refused.put("/etc/passwd", "not an address at all");
        refused.put("https://a.example/" + "a".repeat(200), "longer than the cap");

        for (Map.Entry<String, String> one : refused.entrySet()) {
            NpmRegistry.Invalid refusedAddress = assertThrows(NpmRegistry.Invalid.class,
                    () -> NpmRegistry.normalize(one.getKey()), one.getValue());
            assertNotNull(refusedAddress.getMessage(), one.getValue());
            assertFalse(refusedAddress.getMessage().isBlank(), one.getValue());
            assertFalse(NpmRegistry.isValid(one.getKey()), one.getValue());
        }
    }

    @Test
    void theSettingDecidesBeforeTheDeploymentDoes() {
        LauncherSettings settings = SettingsManager.settings();

        settings.setNpmRegistryPreset("npmmirror");
        NpmRegistry.Effective chosen = NpmRegistry.effective();
        assertEquals("https://registry.npmmirror.com", chosen.registry());
        assertEquals("setting", chosen.source(), "a chosen mirror is the setting's answer, whatever the container says");

        settings.setNpmRegistryPreset(NpmRegistry.CUSTOM);
        settings.setNpmRegistry("https://nexus.example.com/repository/npm/");
        NpmRegistry.Effective typed = NpmRegistry.effective();
        assertEquals("https://nexus.example.com/repository/npm", typed.registry(),
                "the address in force is the normalised one, so the file and the argument agree");
        assertEquals("setting", typed.source());
    }

    @Test
    void withNoSettingTheDeploymentOrThePublishedRegistryAnswers() throws Exception {
        LauncherSettings settings = SettingsManager.settings();
        settings.setNpmRegistryPreset(NpmRegistry.ENVIRONMENT);
        settings.setNpmRegistry("");

        NpmRegistry.Effective effective = NpmRegistry.effective();
        // The test JVM may or may not carry NPM_CONFIG_REGISTRY: both answers are correct, and the
        // one that is not taken must be visibly not taken rather than asserted away.
        assertTrue(Set.of("environment", "default").contains(effective.source()), effective.toString());
        String environment = System.getenv("NPM_CONFIG_REGISTRY");
        if ("environment".equals(effective.source())) {
            assertNotNull(environment);
            assertEquals(NpmRegistry.normalize(environment), effective.registry());
        } else {
            assertEquals(NpmRegistry.DEFAULT, effective.registry());
        }
    }

    @Test
    void anAddressAHandEditedFileCannotUseFallsBackRatherThanBeingHandedToPnpm() {
        LauncherSettings settings = SettingsManager.settings();
        settings.setNpmRegistryPreset(NpmRegistry.CUSTOM);
        settings.setNpmRegistry("https://x/\nstoreDir: /etc");

        NpmRegistry.Effective effective = NpmRegistry.effective();
        assertNotEquals("setting", effective.source(),
                "the settings API refuses this, so reaching here means the file was edited by hand");
        assertNotEquals("https://x/\nstoreDir: /etc", effective.registry());
        assertFalse(effective.registry().contains("\n"));
    }

    @Test
    void onlyTheMirrorsThatNameAnAddressArePresets() {
        assertNull(NpmRegistry.preset(NpmRegistry.CUSTOM), "the hand-typed address is not a published mirror");
        assertNull(NpmRegistry.preset(NpmRegistry.ENVIRONMENT), "the environment names no address of its own");
        assertNull(NpmRegistry.preset("nonsense"));
        assertNull(NpmRegistry.preset(null));

        NpmRegistry.Preset mirror = NpmRegistry.preset("npmmirror");
        assertNotNull(mirror);
        assertEquals("https://registry.npmmirror.com", mirror.url());
    }

    @Test
    void theElectronMirrorComesWithTheMirrorThatPublishesIt() {
        LauncherSettings settings = SettingsManager.settings();

        settings.setNpmRegistryPreset("npmmirror");
        String mirror = NpmRegistry.electronMirror();
        assertNotNull(mirror, "npmmirror publishes Electron's binaries beside itself");
        assertTrue(mirror.startsWith("https://"), mirror);
        assertFalse(mirror.isBlank());

        settings.setNpmRegistryPreset("npmjs");
        assertNull(NpmRegistry.electronMirror(), "the published registry knows nothing about Electron's binaries");

        settings.setNpmRegistryPreset(NpmRegistry.ENVIRONMENT);
        assertNull(NpmRegistry.electronMirror(), "neither does the deployment's own source");
    }
}
