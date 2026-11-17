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

import org.jackhuang.hmcl.util.gson.JsonUtils;
import org.jetbrains.annotations.NotNullByDefault;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/// Built-in and user presets.
///
/// Built-ins live in memory and can never be edited or deleted; a user preset is one
/// JSON file under `<data>/transcode/presets/<id>.json`. Writes are atomic, so a
/// killed panel leaves either the old file or the new one, never half a preset.
@NotNullByDefault
public final class PresetStore {

    private static final Pattern ID = Pattern.compile("[A-Za-z0-9._-]{1,64}");

    private final Path dir;

    public PresetStore(Path dir) {
        this.dir = dir.toAbsolutePath().normalize();
    }

    /// Built-ins first (stable order), then user presets by id.
    public synchronized List<PresetEntry> list() {
        List<PresetEntry> result = new ArrayList<>();
        for (Map.Entry<String, TranscodePreset> builtin : BuiltInPresets.all().entrySet()) {
            result.add(new PresetEntry(builtin.getKey(), true, builtin.getValue().copy()));
        }
        List<Path> files = userFiles();
        files.sort(Comparator.comparing(path -> path.getFileName().toString(), String.CASE_INSENSITIVE_ORDER));
        for (Path file : files) {
            String id = idOf(file);
            try {
                TranscodePreset preset = JsonUtils.fromJsonFile(file, TranscodePreset.class);
                if (preset != null) {
                    result.add(new PresetEntry(id, false, preset));
                }
            } catch (Exception ignored) {
                // One broken user preset must not hide the rest of the list.
            }
        }
        return List.copyOf(result);
    }

    public synchronized Optional<PresetEntry> get(String id) {
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }
        TranscodePreset builtin = BuiltInPresets.all().get(id);
        if (builtin != null) {
            return Optional.of(new PresetEntry(id, true, builtin.copy()));
        }
        Path file = fileFor(id);
        if (!Files.isRegularFile(file)) {
            return Optional.empty();
        }
        try {
            TranscodePreset preset = JsonUtils.fromJsonFile(file, TranscodePreset.class);
            return preset == null ? Optional.empty() : Optional.of(new PresetEntry(id, false, preset));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /// Saves a user preset. Built-in ids are refused.
    public synchronized PresetEntry save(String id, TranscodePreset preset) throws IOException {
        if (!validId(id)) {
            throw new IllegalArgumentException("preset id must be 1-64 chars of [A-Za-z0-9._-]");
        }
        if (BuiltInPresets.all().containsKey(id) || id.startsWith("builtin:")) {
            throw new IllegalArgumentException("built-in presets are read-only; save a copy under another id");
        }
        if (preset == null) {
            throw new IllegalArgumentException("preset is required");
        }
        preset.validate();
        if (preset.name == null || preset.name.isBlank()) {
            throw new IllegalArgumentException("preset name is required");
        }
        Files.createDirectories(dir);
        Path target = fileFor(id);
        Path temp = Files.createTempFile(dir, "preset", ".json");
        try {
            JsonUtils.writeToJsonFile(temp, preset);
            move(temp, target);
        } finally {
            Files.deleteIfExists(temp);
        }
        return new PresetEntry(id, false, preset.copy());
    }

    public synchronized boolean delete(String id) throws IOException {
        if (!validId(id)) {
            return false;
        }
        if (BuiltInPresets.all().containsKey(id)) {
            return false;
        }
        return Files.deleteIfExists(fileFor(id));
    }

    private List<Path> userFiles() {
        if (!Files.isDirectory(dir)) {
            return new ArrayList<>();
        }
        try (var stream = Files.list(dir)) {
            List<Path> result = new ArrayList<>();
            stream.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".json"))
                    .forEach(result::add);
            return result;
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }

    private Path fileFor(String id) {
        return dir.resolve(id + ".json").normalize();
    }

    private static String idOf(Path file) {
        String name = file.getFileName().toString();
        return name.endsWith(".json") ? name.substring(0, name.length() - 5) : name;
    }

    private static boolean validId(String id) {
        return id != null && ID.matcher(id).matches() && !id.startsWith("builtin:");
    }

    private static void move(Path from, Path to) throws IOException {
        try {
            Files.move(from, to, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(from, to, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /// A preset plus where it came from.
    public record PresetEntry(String id, boolean builtin, TranscodePreset preset) {
    }
}