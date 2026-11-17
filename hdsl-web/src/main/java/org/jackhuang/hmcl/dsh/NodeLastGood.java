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

/// The last runtime a consumer actually came up on, per consumer.
///
/// A selector is a wish; this is the last wish that came true. When a switch to
/// a new Node version turns out badly, the operator needs the previous exact
/// version without reconstructing it from logs — and the panel needs it to offer
/// "回退" next to the selector instead of making them guess.
///
/// The anchor is written when the process is observed **ready** (the brand
/// readiness wait, or the DSH `RUNNING` announcement), never when the launch is
/// merely requested: a runtime that crashes on boot must not become the thing a
/// revert returns to.
@NotNullByDefault
public final class NodeLastGood {

    private static final Path FILE = DshPaths.RUNTIMES.resolve("last-good.json");

    private static final Object LOCK = new Object();

    private NodeLastGood() {
    }

    /// The key for a DSH instance.
    ///
    /// @param id the instance id
    /// @return the key
    public static String dshKey(String id) {
        return "dsh/" + id;
    }

    /// The key for a brand instance.
    ///
    /// @param brand the brand id
    /// @param id    the instance id
    /// @return the key
    public static String brandKey(String brand, String id) {
        return brand + "/" + id;
    }

    /// The last ready version for a key, or `null`.
    ///
    /// @param key the consumer key
    /// @return the exact version, or `null`
    public static @Nullable String version(String key) {
        JsonObject root = read();
        if (!root.has(key) || !root.get(key).isJsonObject()) {
            return null;
        }
        JsonObject entry = root.getAsJsonObject(key);
        return entry.has("version") && entry.get("version").isJsonPrimitive()
                ? entry.get("version").getAsString()
                : null;
    }

    /// Records a successful start. Best effort: a launch must not fail because
    /// its anchor could not be written.
    ///
    /// @param key     the consumer key
    /// @param version the exact version
    public static void record(String key, String version) {
        synchronized (LOCK) {
            try {
                JsonObject root = read();
                JsonObject entry = new JsonObject();
                entry.addProperty("version", version);
                entry.addProperty("at", java.time.Instant.now().toString());
                root.add(key, entry);
                write(root);
            } catch (DshException e) {
                LOG.warning("Could not record the last good Node.js for " + key, e);
            }
        }
    }

    /// Removes a consumer's anchor (its instance was deleted).
    ///
    /// @param key the consumer key
    public static void forget(String key) {
        synchronized (LOCK) {
            try {
                JsonObject root = read();
                if (root.remove(key) != null) {
                    write(root);
                }
            } catch (DshException e) {
                LOG.warning("Could not forget the last good Node.js for " + key, e);
            }
        }
    }

    /// Every anchor, for the Node page.
    ///
    /// @return a copy, never `null`
    public static Map<String, String> all() {
        Map<String, String> versions = new LinkedHashMap<>();
        JsonObject root = read();
        for (String key : root.keySet()) {
            String version = version(key);
            if (version != null) {
                versions.put(key, version);
            }
        }
        return versions;
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
