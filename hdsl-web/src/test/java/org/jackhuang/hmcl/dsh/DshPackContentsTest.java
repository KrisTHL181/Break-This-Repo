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

import com.google.gson.JsonObject;
import org.jackhuang.hmcl.util.gson.JsonUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Verifies that what a pack says it carries is what it carries.
///
/// The choosing happens in a tree, and a box there has to mean something: a pack that named a plugin
/// it left out would describe an installation nobody can make from it, and an import would try.
class DshPackContentsTest {
    /// The instance these tests build a pack from.
    private static final String INSTANCE_ID = "pack-contents-test";

    @Test
    void aBundleLeftOutIsNeitherCarriedNorNamed(@TempDir Path temp) throws Exception {
        Path home = Files.createTempDirectory("pack-contents-home");
        DshInstance instance = DshInstanceManager.find(INSTANCE_ID);
        if (instance == null) {
            instance = DshInstanceManager.create(INSTANCE_ID, "0.1.6-alpha.2",
                    DshInstance.DEFAULT_PROFILE, home, DshHomeMode.ISOLATED, null,
                    List.of(), Map.of());
        }

        try {
            Path profile = instance.homeDirectory().resolve("profiles").resolve(instance.profile());
            Files.createDirectories(profile);
            Files.writeString(profile.resolve("package.json"), """
                    {"name":"dsh-profile-web","private":true,
                     "dependencies":{"dshmarket":"1.52.0","dsh-cost-meter":"1.7.30"},
                     "dsh":{"profile":{"bundles":["@deepseek-ai/dsh-base","dshmarket","dsh-cost-meter"]}}}
                    """);

            Path target = temp.resolve("pack.zip");
            DshModpacks.Options options = new DshModpacks.Options("test", "1.0", "", "", "", "", false,
                    Set.of("dsh-cost-meter"), Set.of(), Set.of());
            DshModpacks.export(instance, target, options, null);

            JsonObject manifest;
            try (ZipFile zip = new ZipFile(target.toFile())) {
                manifest = JsonUtils.fromJson(new String(
                        zip.getInputStream(zip.getEntry("manifest.json")).readAllBytes(),
                        StandardCharsets.UTF_8), JsonObject.class);
            }

            List<String> bundles = manifest.getAsJsonArray("bundles").asList().stream()
                    .map(com.google.gson.JsonElement::getAsString).toList();
            assertEquals(List.of("@deepseek-ai/dsh-base", "dshmarket"), bundles,
                    "a bundle left out does not travel, and the order of the rest is kept");

            List<String> named = manifest.getAsJsonArray("plugins").asList().stream()
                    .map(com.google.gson.JsonElement::getAsJsonObject)
                    .map(element -> element.get("name").getAsString()).toList();
            assertTrue(named.contains("dshmarket"), "what travels is named");
            assertFalse(named.contains("dsh-cost-meter"),
                    "and what does not travel is not named either, or an import would fetch it");

            // What else the archive holds is the export's own business; what this test is about is
            // that the manifest describes what is in it.
        } finally {
            // A test that registers an instance has to remove it whatever happens: leaving one behind
            // is what makes the next test fail for a reason that has nothing to do with it.
            DshInstanceManager.delete(INSTANCE_ID);
        }
    }
}
