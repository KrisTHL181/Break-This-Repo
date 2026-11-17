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
package org.jackhuang.hmcl.dsh;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.jackhuang.hmcl.util.logging.Logger.LOG;

/// The pointer a numeric channel (`22`, `26`) resolves to.
///
/// `NodeSelection` can already answer a channel from what is installed, but that
/// answer is "the highest 22.x on disk" — installing 26.13.0 does not tell a `22`
/// consumer anything, and there is no record that 22.23.1 was the version the
/// operator chose to track. This registry is that record: `channels.json` beside
/// the runtimes, written when an update finishes and read on every resolution.
///
/// A pointer is only a preference. If the version it names is uninstalled, the
/// resolver falls back to the highest installed version on the line, so deleting
/// a runtime never leaves a channel dangling.
@NotNullByDefault
public final class NodeChannels {

    private static final Path FILE = DshPaths.RUNTIMES.resolve("channels.json");

    private static final Object LOCK = new Object();

    private NodeChannels() {
    }

    /// The version a channel currently points at, or `null`.
    ///
    /// @param name the channel name (`22`, `26`)
    /// @return the version, or `null`
    public static @Nullable String version(String name) {
        JsonObject root = read();
        if (!root.has(name) || !root.get(name).isJsonObject()) {
            return null;
        }
        JsonObject entry = root.getAsJsonObject(name);
        return entry.has("version") && entry.get("version").isJsonPrimitive()
                ? entry.get("version").getAsString()
                : null;
    }

    /// Every recorded pointer.
    ///
    /// @return a copy, never `null`
    public static Map<String, String> all() {
        Map<String, String> versions = new LinkedHashMap<>();
        JsonObject root = read();
        for (String name : root.keySet()) {
            String version = version(name);
            if (version != null) {
                versions.put(name, version);
            }
        }
        return versions;
    }

    /// Moves a channel's pointer. Called after an update has installed and
    /// health-checked the new version — never before.
    ///
    /// @param name    the channel
    /// @param version the exact version
    /// @throws DshException when the file cannot be written
    public static void record(String name, String version) throws DshException {
        synchronized (LOCK) {
            JsonObject root = read();
            JsonObject entry = new JsonObject();
            entry.addProperty("version", version);
            entry.addProperty("updatedAt", java.time.Instant.now().toString());
            root.add(name, entry);
            write(root);
        }
    }

    /// Drops every pointer that names a version that is no longer installed.
    /// Called by `NodeRuntimeManager.uninstall`; stale entries are harmless but
    /// would make the UI claim a channel is installed when it is not.
    ///
    /// @param removed the removed version
    public static void forget(String removed) {
        synchronized (LOCK) {
            try {
                JsonObject root = read();
                boolean changed = false;
                for (String name : new java.util.ArrayList<>(root.keySet())) {
                    if (removed.equals(NodeChannels.version(name))) {
                        root.remove(name);
                        changed = true;
                    }
                }
                if (changed) {
                    write(root);
                }
            } catch (DshException e) {
                LOG.warning("Could not clean the channel pointers for Node.js " + removed, e);
            }
        }
    }

    private static JsonObject read() {
        if (!Files.isRegularFile(FILE)) {
            return new JsonObject();
        }
        try {
            JsonElement parsed = JsonParser.parseString(Files.readString(FILE));
            return parsed.isJsonObject() ? parsed.getAsJsonObject() : new JsonObject();
        } catch (IOException | RuntimeException e) {
            LOG.warning("Could not read " + FILE, e);
            return new JsonObject();
        }
    }

    private static void write(JsonObject root) throws DshException {
        try {
            Files.createDirectories(DshPaths.RUNTIMES);
            Path temp = FILE.resolveSibling(FILE.getFileName() + ".tmp");
            Files.writeString(temp, root.toString());
            try {
                Files.move(temp, FILE, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException e) {
                Files.move(temp, FILE, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new DshException("Failed to write " + FILE, e);
        }
    }
}
