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

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.jackhuang.hmcl.util.logging.Logger.LOG;

/// The per-brand fallback policy, stored next to the brand instances in
/// `<data>/brands/policy.json`.
///
/// Every switch defaults off, so an untouched panel behaves exactly like a
/// normal multi-instance brand. The last-resort switches exist only for the
/// brands whose own agent can wedge the tool hard enough that the panel needs
/// a fixed door back in ([Brand#supportsLastResort] — Octop and OpenCode):
///
/// - `singleInstance`: only one instance of this brand may run. Enabling it
///   requires naming the survivor; the others are disabled (not deleted) and
///   the API refuses to launch, open or change them.
/// - `lockedRoute`: serve the survivor at the fixed `/i/<brand>/` mount. It
///   requires `singleInstance`, because a fixed path can only name one target.
/// - `sourceBuild`: install releases by compiling the upstream source instead
///   of using the prebuilt portable archive. Windows refuses it. (Octop only;
///   this is not a last-resort switch.)
///
/// A file written by an older build may still name a survivor for a brand that
/// no longer offers the switches; [#get] drops those fields on read, so the
/// policy cannot outlive the capability.
///
/// The file is read on every access rather than cached: the brand API and the
/// developer API are different servlets, and a stale cache in one would make
/// the other's changes invisible.
@NotNullByDefault
public final class BrandPolicy {

    /// One brand's policy. `survivor` is an instance id (an opaque token), not
    /// a display name. `nodeRuntime` is the brand-wide Node.js selector a
    /// `BrandInstance` follows when its own is `null`/`inherit`.
    public record Entry(boolean singleInstance, @Nullable String survivor,
                        boolean lockedRoute, boolean sourceBuild,
                        @Nullable String nodeRuntime) {
        public static final Entry DEFAULTS = new Entry(false, null, false, false, null);

        public Entry withSingleInstance(boolean value) {
            return new Entry(value, survivor, lockedRoute, sourceBuild, nodeRuntime);
        }

        public Entry withSurvivor(@Nullable String value) {
            return new Entry(singleInstance, value, lockedRoute, sourceBuild, nodeRuntime);
        }

        public Entry withLockedRoute(boolean value) {
            return new Entry(singleInstance, survivor, value, sourceBuild, nodeRuntime);
        }

        public Entry withSourceBuild(boolean value) {
            return new Entry(singleInstance, survivor, lockedRoute, value, nodeRuntime);
        }

        public Entry withNodeRuntime(@Nullable String value) {
            return new Entry(singleInstance, survivor, lockedRoute, sourceBuild, value);
        }
    }

    private final Path file;

    public BrandPolicy(Path dataDir) {
        this.file = dataDir.resolve("brands").resolve("policy.json");
    }

    /// The current policy for a brand; defaults when the file or entry is
    /// missing (a fresh panel, or a brand added after the file was written).
    ///
    /// A brand that no longer offers the last-resort switches cannot carry
    /// them: a file written before that capability was narrowed still names a
    /// survivor, and honouring it would keep the instance disabled with no
    /// switch in the page to turn it off.
    public Entry get(Brand brand) {
        Entry entry = load().get(brand.id());
        if (entry == null) {
            return Entry.DEFAULTS;
        }
        if (!brand.supportsLastResort()
                && (entry.singleInstance() || entry.lockedRoute() || entry.survivor() != null)) {
            return entry.withSingleInstance(false).withSurvivor(null).withLockedRoute(false);
        }
        return entry;
    }

    /// Whether an instance is disabled by the single-instance policy. The
    /// survivor is always enabled; every other id is not.
    public boolean disabled(Brand brand, String instanceId) {
        Entry entry = get(brand);
        return entry.singleInstance() && !instanceId.equals(entry.survivor());
    }

    /// The survivor that should answer `/i/<brand>/`, or `null` when the fixed
    /// route is off.
    public @Nullable String lockedSurvivor(Brand brand) {
        if (!brand.supportsLastResort()) {
            return null;
        }
        Entry entry = get(brand);
        return entry.lockedRoute() ? entry.survivor() : null;
    }

    /// Every brand currently publishing a fixed route, as the runtime needs it
    /// at boot.
    ///
    /// @return brand → survivor id, in enum order
    public Map<Brand, String> lockedSurvivors() {
        Map<Brand, String> survivors = new LinkedHashMap<>();
        for (Brand brand : Brand.values()) {
            String survivor = lockedSurvivor(brand);
            if (survivor != null) {
                survivors.put(brand, survivor);
            }
        }
        return survivors;
    }

    /// Writes one brand's policy. Validation lives here so the HTTP layer and
    /// any future caller cannot persist an impossible combination.
    ///
    /// @throws IllegalArgumentException when the entry contradicts itself
    /// @throws IOException when the file cannot be written
    public synchronized void set(Brand brand, Entry entry) throws IOException {
        String survivor = entry.survivor() == null ? null : entry.survivor().trim();
        if (survivor != null && survivor.isEmpty()) {
            survivor = null;
        }
        if (!brand.supportsLastResort() && (entry.singleInstance() || entry.lockedRoute())) {
            throw new IllegalArgumentException(
                    "singleInstance and lockedRoute are only available for octop and opencode");
        }
        if (entry.singleInstance() && survivor == null) {
            throw new IllegalArgumentException("singleInstance requires a survivor instance");
        }
        if (entry.lockedRoute() && (!entry.singleInstance() || survivor == null)) {
            throw new IllegalArgumentException("lockedRoute requires singleInstance and a survivor");
        }
        String nodeRuntime = entry.nodeRuntime() == null ? null : entry.nodeRuntime().trim();
        if (nodeRuntime != null && nodeRuntime.isEmpty()) {
            nodeRuntime = null;
        }
        if (nodeRuntime != null
                && org.jackhuang.hmcl.dsh.NodeSelection.parse(nodeRuntime).kind()
                        == org.jackhuang.hmcl.dsh.NodeSelection.Kind.INVALID) {
            throw new IllegalArgumentException("unknown nodeRuntime selector: " + nodeRuntime);
        }
        Entry normalized = new Entry(entry.singleInstance(), survivor, entry.lockedRoute(),
                entry.sourceBuild(), nodeRuntime);
        Map<String, Entry> all = load();
        all.put(brand.id(), normalized);
        write(all);
    }

    private Map<String, Entry> load() {
        Map<String, Entry> entries = new LinkedHashMap<>();
        if (!Files.isRegularFile(file)) {
            return entries;
        }
        try {
            String text = Files.readString(file, StandardCharsets.UTF_8);
            JsonObject root = JsonParser.parseString(text).getAsJsonObject();
            for (String id : root.keySet()) {
                if (!root.get(id).isJsonObject()) {
                    continue;
                }
                JsonObject object = root.getAsJsonObject(id);
                String survivor = object.has("survivor") && object.get("survivor").isJsonPrimitive()
                        ? object.get("survivor").getAsString()
                        : null;
                String nodeRuntime = object.has("nodeRuntime") && object.get("nodeRuntime").isJsonPrimitive()
                        ? object.get("nodeRuntime").getAsString()
                        : null;
                entries.put(id, new Entry(
                        bool(object, "singleInstance"),
                        survivor == null || survivor.isBlank() ? null : survivor,
                        bool(object, "lockedRoute"),
                        bool(object, "sourceBuild"),
                        nodeRuntime == null || nodeRuntime.isBlank() ? null : nodeRuntime));
            }
        } catch (RuntimeException | IOException e) {
            LOG.warning("Could not read " + file + "; using default brand policy", e);
        }
        return entries;
    }

    private static boolean bool(JsonObject object, String key) {
        return object.has(key) && object.get(key).isJsonPrimitive()
                && object.get(key).getAsBoolean();
    }

    private void write(Map<String, Entry> entries) throws IOException {
        Files.createDirectories(file.getParent());
        JsonObject root = new JsonObject();
        for (Map.Entry<String, Entry> item : entries.entrySet()) {
            Entry entry = item.getValue();
            JsonObject object = new JsonObject();
            object.addProperty("singleInstance", entry.singleInstance());
            if (entry.survivor() == null) {
                object.add("survivor", null);
            } else {
                object.addProperty("survivor", entry.survivor());
            }
            object.addProperty("lockedRoute", entry.lockedRoute());
            object.addProperty("sourceBuild", entry.sourceBuild());
            if (entry.nodeRuntime() == null) {
                object.add("nodeRuntime", null);
            } else {
                object.addProperty("nodeRuntime", entry.nodeRuntime());
            }
            root.add(item.getKey(), object);
        }
        Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
        Files.writeString(tmp, root.toString(), StandardCharsets.UTF_8);
        try {
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
