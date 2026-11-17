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
package org.jackhuang.hmcl.web.transcode.preset;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PresetStoreTest {

    @Test
    void builtInsAreReadOnlyAndListedFirst(@TempDir Path dir) {
        PresetStore store = new PresetStore(dir.resolve("presets"));
        var list = store.list();
        assertTrue(list.size() >= 5);
        assertTrue(list.get(0).builtin());
        assertTrue(list.stream().anyMatch(entry -> entry.id().equals("h264-quality")));

        TranscodePreset copy = store.get("h264-quality").orElseThrow().preset();
        copy.name = "mine";
        assertThrows(IllegalArgumentException.class, () -> store.save("h264-quality", copy));
    }

    @Test
    void aUserPresetRoundTrips(@TempDir Path dir) throws Exception {
        PresetStore store = new PresetStore(dir.resolve("presets"));
        TranscodePreset preset = new TranscodePreset();
        preset.name = "我的预设";
        preset.videoEncoder = "libx265";
        preset.qualityValue = "24";
        store.save("mine", preset);

        PresetStore reopened = new PresetStore(dir.resolve("presets"));
        var entry = reopened.get("mine").orElseThrow();
        assertFalse(entry.builtin());
        assertEquals("我的预设", entry.preset().name);
        assertEquals("libx265", entry.preset().videoEncoder);
        assertEquals("24", entry.preset().qualityValue);

        assertTrue(reopened.delete("mine"));
        assertTrue(reopened.get("mine").isEmpty());
    }

    @Test
    void idsAreConstrained(@TempDir Path dir) {
        PresetStore store = new PresetStore(dir.resolve("presets"));
        assertThrows(IllegalArgumentException.class, () -> store.save("../escape", new TranscodePreset()));
        assertThrows(IllegalArgumentException.class, () -> store.save("builtin:x", new TranscodePreset()));
    }
}