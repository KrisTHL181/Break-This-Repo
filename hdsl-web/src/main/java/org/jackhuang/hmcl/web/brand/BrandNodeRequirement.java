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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.jackhuang.hmcl.util.logging.Logger.LOG;

/// The `engines.node` a brand release declares, read from its own
/// `package.json`.
///
/// The panel's global range is DeepSeek Harness's; a brand may need something
/// else, and the disagreement is not cosmetic: **Hermes 0.22.7 declares `>=26`**
/// while 0.22.5 declared `^22.22.0 || ^24.11.0 || >=26.0.0`, which is exactly how
/// "install the latest Hermes" ended up launching a tool on a Node it cannot run
/// on. The declaration lives in the release the operator installed, so that is
/// where this reads it — not from a table in the panel that would drift.
///
/// A release without a `package.json` or without `engines.node` answers `null`,
/// which means "no declared constraint", not "anything goes": the caller still
/// applies the panel's own range.
@NotNullByDefault
public final class BrandNodeRequirement {

    private record Cached(long modified, @Nullable String range) {
    }

    private static final Map<String, Cached> CACHE = new ConcurrentHashMap<>();

    private BrandNodeRequirement() {
    }

    /// The declared range, or `null`.
    ///
    /// @param dataDir the panel's data directory
    /// @param brand   the brand
    /// @param version the installed release version
    /// @return the range, or `null` when the release declares none
    public static @Nullable String of(Path dataDir, Brand brand, String version) {
        Path file = dataDir.resolve("brands")
                .resolve(brand.id())
                .resolve("releases")
                .resolve(version)
                .resolve("package.json");
        if (!Files.isRegularFile(file)) {
            return null;
        }
        String key = file.toString();
        try {
            long modified = Files.getLastModifiedTime(file).toMillis();
            Cached cached = CACHE.get(key);
            if (cached != null && cached.modified() == modified) {
                return cached.range();
            }
            String range = read(file);
            CACHE.put(key, new Cached(modified, range));
            return range;
        } catch (IOException | RuntimeException e) {
            LOG.warning("Could not read the Node requirement of " + file, e);
            return null;
        }
    }

    /// The declared range of a release directory that is not installed yet
    /// (the installer reads it right after unpacking the source).
    ///
    /// @param releaseDir the unpacked release directory
    /// @return the range, or `null` when the source declares none
    public static @Nullable String ofDirectory(Path releaseDir) {
        Path file = releaseDir.resolve("package.json");
        if (!Files.isRegularFile(file)) {
            return null;
        }
        try {
            return read(file);
        } catch (IOException | RuntimeException e) {
            LOG.warning("Could not read the Node requirement of " + file, e);
            return null;
        }
    }

    private static @Nullable String read(Path file) throws IOException {
        JsonElement parsed = JsonParser.parseString(Files.readString(file));
        if (!parsed.isJsonObject()) {
            return null;
        }
        JsonObject engines = parsed.getAsJsonObject().getAsJsonObject("engines");
        if (engines == null || !engines.has("node") || !engines.get("node").isJsonPrimitive()) {
            return null;
        }
        String range = engines.get("node").getAsString().trim();
        return range.isEmpty() ? null : range;
    }
}
