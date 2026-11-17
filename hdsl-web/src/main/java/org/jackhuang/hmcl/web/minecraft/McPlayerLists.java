/*
 * HDSL-web
 * Copyright (C) 2026  HDSL-web Contributors
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
package org.jackhuang.hmcl.web.minecraft;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.jetbrains.annotations.NotNullByDefault;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/// Imports the player lists a server keeps **beside** its world: `whitelist.json`,
/// `ops.json`, `banned-players.json`, `banned-ips.json`.
///
/// This is the data half of adopting an existing server (docs/minecraft.md §7.3):
/// the world travels as a zip through [McWorlds], and these four files are the
/// only other thing an operator cannot rebuild from memory. Nothing here is
/// code — every file is plain JSON the server reads at startup, and only those
/// four names are accepted, so a zip of a server root cannot smuggle a jar in.
@NotNullByDefault
public final class McPlayerLists {

    /// The files this importer knows. Anything else in the zip is ignored.
    public static final List<String> NAMES = List.of(
            "whitelist.json", "ops.json", "banned-players.json", "banned-ips.json");

    /// A zip of four short arrays is tiny; this only stops a mistake.
    public static final long MAX_BYTES = 16L * 1024 * 1024;

    private McPlayerLists() {
    }

    /// Imports whichever of [NAMES] the zip carries.
    ///
    /// Each file is validated as a JSON array before it replaces the one on
    /// disk — a truncated upload must not leave the server with a file it
    /// cannot parse at the next start.
    ///
    /// @param instance the instance
    /// @param manager  the instance manager
    /// @param in       the zip
    /// @return how many entries each imported file holds
    /// @throws McException when the zip is too large, malformed, or a list is not a JSON array
    public static Map<String, Integer> importZip(McInstance instance, McInstanceManager manager, InputStream in)
            throws McException {
        Path directory = manager.instanceDirectory(instance.id());
        Path archive = directory.resolve(".player-lists.zip");
        try {
            long copied = copyCapped(in, archive);
            Map<String, Integer> counts = new LinkedHashMap<>();
            try (InputStream file = Files.newInputStream(archive);
                 ZipInputStream zip = new ZipInputStream(file, StandardCharsets.UTF_8)) {
                ZipEntry entry;
                while ((entry = zip.getNextEntry()) != null) {
                    String name = entry.getName().replace('\\', '/');
                    if (entry.isDirectory()) {
                        continue;
                    }
                    if (name.contains("/") || name.contains("..")) {
                        throw new McException("the player-list zip must hold the files at its root: " + name);
                    }
                    if (!NAMES.contains(name)) {
                        continue;
                    }
                    String text = new String(zip.readAllBytes(), StandardCharsets.UTF_8);
                    JsonElement parsed;
                    try {
                        parsed = JsonParser.parseString(text);
                    } catch (RuntimeException e) {
                        throw new McException(name + " is not valid JSON: " + e.getMessage());
                    }
                    if (!parsed.isJsonArray()) {
                        throw new McException(name + " must be a JSON array");
                    }
                    // Write beside the target first: a half-written list is worse
                    // than the previous one, which is still on disk at this point.
                    Path target = directory.resolve(name);
                    Path temp = directory.resolve("." + name + ".tmp");
                    Files.writeString(temp, text, StandardCharsets.UTF_8,
                            StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING,
                            StandardOpenOption.WRITE);
                    try {
                        Files.move(temp, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                                java.nio.file.StandardCopyOption.ATOMIC_MOVE);
                    } catch (java.nio.file.AtomicMoveNotSupportedException e) {
                        Files.move(temp, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    }
                    counts.put(name, parsed.getAsJsonArray().size());
                }
            }
            if (counts.isEmpty()) {
                throw new McException("the zip carries none of " + String.join(", ", NAMES));
            }
            return counts;
        } catch (IOException e) {
            throw new McException("Failed to import the player lists: " + e.getMessage(), e);
        } finally {
            try {
                Files.deleteIfExists(archive);
            } catch (IOException ignored) {
                // Best effort.
            }
        }
    }

    private static long copyCapped(InputStream in, Path target) throws IOException, McException {
        long copied = 0;
        try (OutputStream out = Files.newOutputStream(target)) {
            byte[] buffer = new byte[64 * 1024];
            int read;
            while ((read = in.read(buffer)) >= 0) {
                copied += read;
                if (copied > MAX_BYTES) {
                    throw new McException("the player-list zip is larger than the panel accepts");
                }
                out.write(buffer, 0, read);
            }
        }
        return copied;
    }
}