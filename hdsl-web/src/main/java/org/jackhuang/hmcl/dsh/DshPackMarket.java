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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import org.jackhuang.hmcl.util.gson.JsonUtils;
import org.jackhuang.hmcl.util.io.HttpClients;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.jackhuang.hmcl.util.logging.Logger.LOG;

/// The community's modpack market, as published.
///
/// The ecosystem is **not centralised**: an author only uploads to GitHub, tags the repository
/// `dsh-pack`, and attaches the archive to a release. A collector walks that tag once a day and
/// writes what it finds into one index, which is what this reads. So there is no submission step
/// anywhere in this file — the index is a directory of things that already exist, and the most this
/// can do with it is list them and fetch them.
///
/// Two documents, and the split matters:
///
/// - **the index** (`index.json`) — one line per pack version, with the three things needed to fetch
///   it: `downloadUrl`, `sha256`, and `size`. That is all a list needs, and it is all this class
///   reads.
/// - **the manifest** (`packs/<id>/manifest.json`) — the pack's full description, fetched one pack at
///   a time when somebody opens it. The index deliberately does **not** carry it: three packs would
///   not matter, three thousand would.
///
/// The index is on GitHub Pages, which this launcher may not be able to reach — the same network that
/// cannot reach it cannot reach the plugin catalogue either, and that one already falls back to a
/// copy on disk. The same fallback is here, for the same reason: a launcher that cannot reach the
/// market can still show what it showed yesterday.
@NotNullByDefault
public final class DshPackMarket {
    /// Where the ecosystem publishes its index.
    public static final String INDEX_URL =
            "https://dsh-packforge.github.io/dsh-pack-market/index.json";

    /// The contract version this launcher knows how to read.
    ///
    /// Checked rather than assumed: the field exists so that a future index can change shape without
    /// older launchers reading it wrongly, and the one thing a reader must not do with such a field
    /// is ignore it.
    private static final int SUPPORTED_SCHEMA = 2;

    private DshPackMarket() {
    }

    /// One pack version, as the index describes it.
    ///
    /// Everything is optional except the seven the index requires, because a field the collector
    /// stops writing must not stop the launcher from listing what is still there. The three that
    /// matter for fetching — [`downloadUrl`][#downloadUrl], [`sha256`][#sha256], [`size`][#size] —
    /// are the ones the specification calls the pointers, and they are the ones the download is
    /// checked against.
    ///
    /// @param id              `<owner>.<repo>`, which is also the directory its manifest lives in
    /// @param name            the slug the author chose
    /// @param version         the version, a semantic version
    /// @param displayName     what to show, already resolved from whatever form the index used
    /// @param description     the one-line description, likewise
    /// @param downloadUrl     where the archive is
    /// @param sha256          the whole archive's digest, lowercase hex
    /// @param size            the archive's length in bytes
    /// @param owner           the GitHub account
    /// @param repo            the GitHub repository
    /// @param manifestVersion which manifest the pack carries, or `null` when unstated
    /// @param type            `profile` or `dshhome`, or `null` when unstated
    /// @param author          who made it, or `null`
    /// @param category        the collector's category, or `null`
    /// @param dshVersion      the harness version it was made for, or `null`
    /// @param dshVersions     the harness versions the author measured it against, or empty when the
    ///                        index did not flatten them (they are then only in the lazy manifest)
    /// @param launcherRestricted whether the pack carries a `launchers` declaration, which the index
    ///                        publishes as a derived flag so a list can filter without fetching
    /// @param profileName     what the installed profile is called, or `null`
    /// @param updatedAt       when it last changed, or `null`
    /// @param bundleCount     how many bundles it layers, or `null`
    /// @param depCount        how many packages it depends on, or `null`
    public record Entry(
            String id,
            String name,
            String version,
            String displayName,
            @Nullable String description,
            String downloadUrl,
            String sha256,
            long size,
            String owner,
            String repo,
            @Nullable Integer manifestVersion,
            @Nullable String type,
            @Nullable String author,
            @Nullable String category,
            @Nullable String dshVersion,
            List<String> dshVersions,
            boolean launcherRestricted,
            @Nullable String profileName,
            @Nullable String updatedAt,
            @Nullable Integer bundleCount,
            @Nullable Integer depCount) {

        /// Returns what the interface calls this pack.
        ///
        /// The display name when there is one, and the id when there is not: a card with no title is
        /// worse than a card titled `owner.repo`, and the id is at least true.
        ///
        /// @return the title
        public String title() {
            return displayName == null || displayName.isBlank() ? id : displayName;
        }

        /// Reports whether this is the whole of a `$DSH_HOME` rather than one profile.
        ///
        /// Defaults to `profile`, which the specification says is what an unstated type means.
        ///
        /// @return whether it replaces the whole home
        public boolean isWholeHome() {
            return "dshhome".equalsIgnoreCase(type);
        }

        /// Returns where this pack's icon is, or `null` when there is none to be had.
        ///
        /// The index carries no icon and neither does a manifest — the specification has no field for
        /// one — so the picture is the **author's GitHub avatar**, which is the one image the
        /// ecosystem already publishes about a pack. Every entry has an `owner`, and GitHub serves
        /// `<owner>.png` for it, so this is derived rather than invented: it is a real picture of the
        /// person who made the pack, not a placeholder pretending to be the pack's own logo.
        ///
        /// A pack whose author has no avatar, or whose avatar cannot be fetched, is drawn without
        /// one — the row keeps the space so the list stays aligned, and shows nothing in it.
        ///
        /// @return the address
        public @Nullable String iconUrl() {
            if (owner == null || owner.isBlank()) {
                return null;
            }
            return "https://github.com/" + owner.trim() + ".png?size=64";
        }

        /// Returns the version line shown under the title.
        ///
        /// @return the author, version, and harness version, as many as are known
        public String subtitle() {
            StringBuilder line = new StringBuilder();
            if (author != null && !author.isBlank()) {
                line.append(author).append(" · ");
            }
            line.append(version);
            if (dshVersion != null && !dshVersion.isBlank()) {
                line.append(" · dsh ").append(dshVersion);
            }
            return line.toString();
        }
    }

    /// The index, as read.
    ///
    /// @param generatedAt when the collector last wrote it, or `null`
    /// @param entries     the packs, newest first as the collector orders them
    public record Index(@Nullable String generatedAt, List<Entry> entries) {
    }

    /// Reads the index.
    ///
    /// @return the index
    /// @throws DshException when it cannot be read or does not parse
    public static Index fetch() throws DshException {
        String url = indexUrl();
        Path cached = cachedFile(url);
        String body;
        try {
            body = org.jackhuang.hmcl.util.io.NetworkUtils.doGet(URI.create(url));
            keepCopy(cached, body);
        } catch (IOException | RuntimeException direct) {
            // The last copy is worth more than nothing: a market that cannot be reached is not a
            // reason to show an empty list when yesterday's is on the disk.
            String kept = readCached(cached);
            if (kept == null) {
                throw new DshException("Failed to read the modpack index from " + url
                        + " (" + direct.getMessage() + ")", direct);
            }
            LOG.info("The modpack index could not be fetched; using the copy kept at " + cached);
            body = kept;
        }
        return parse(body);
    }

    /// Reads an index out of its text.
    ///
    /// @param body the document
    /// @return the index
    /// @throws DshException when it is not an index this launcher can read
    public static Index parse(String body) throws DshException {
        JsonObject root;
        try {
            root = JsonUtils.fromJson(body, JsonObject.class);
        } catch (RuntimeException e) {
            throw new DshException("The modpack index is not JSON", e);
        }
        if (root == null) {
            throw new DshException("The modpack index is empty", null);
        }

        int schema = root.has("schemaVersion") && root.get("schemaVersion").isJsonPrimitive()
                ? root.get("schemaVersion").getAsInt() : -1;
        if (schema != SUPPORTED_SCHEMA) {
            // Both halves are said: which contract arrived and which this launcher knows. "Unsupported"
            // alone leaves somebody guessing whether to update the launcher or the index.
            throw new DshException("The modpack index is version " + schema
                    + ", and this launcher reads version " + SUPPORTED_SCHEMA, null);
        }

        // The key is `modpacks`, though the ecosystem's own README says `packs`. The file is what
        // the collector writes and what the market's own web page reads, so the file is what is
        // read here.
        JsonElement packs = root.get("modpacks");
        if (packs == null || !packs.isJsonArray()) {
            throw new DshException("The modpack index has no list of packs", null);
        }

        List<Entry> entries = new java.util.ArrayList<>();
        for (JsonElement element : packs.getAsJsonArray()) {
            if (!element.isJsonObject()) {
                continue;
            }
            Entry entry = readEntry(element.getAsJsonObject());
            if (entry != null) {
                entries.add(entry);
            }
        }
        String generated = root.has("generatedAt") && root.get("generatedAt").isJsonPrimitive()
                ? root.get("generatedAt").getAsString() : null;
        return new Index(generated, List.copyOf(entries));
    }

    /// Reads one entry, or `null` when it is missing something a pack cannot be fetched without.
    ///
    /// An entry without all three pointers is dropped rather than shown: it could be listed and not
    /// installed, and a row that cannot be acted on is worse than a row that is not there. The three
    /// are exactly what the specification requires of an entry, so nothing legitimate is dropped.
    ///
    /// @param object the entry
    /// @return the entry, or `null`
    private static @Nullable Entry readEntry(JsonObject object) {
        String id = string(object, "id");
        String downloadUrl = string(object, "downloadUrl");
        String sha256 = string(object, "sha256");
        JsonElement size = object.get("size");
        if (id == null || downloadUrl == null || sha256 == null
                || size == null || !size.isJsonPrimitive()) {
            LOG.warning("An entry in the modpack index has no id, download address, digest or size; skipped");
            return null;
        }
        return new Entry(
                id,
                stringOr(object, "name", id),
                stringOr(object, "version", ""),
                // Both of these may be a plain string or a map keyed by language; resolved here so
                // that nothing downstream has to know that.
                localized(object, "displayName", id),
                localized(object, "description", null),
                downloadUrl,
                sha256,
                size.getAsLong(),
                stringOr(object, "owner", ""),
                stringOr(object, "repo", ""),
                integer(object, "manifestVersion"),
                string(object, "type"),
                string(object, "author"),
                string(object, "category"),
                string(object, "dshVersion"),
                strings(object, "dshVersions"),
                bool(object, "launcherRestricted"),
                string(object, "profileName"),
                string(object, "updatedAt"),
                integer(object, "bundleCount"),
                integer(object, "depCount"));
    }

    /// Reads a field that the collector may write as a string or as a map of language to string.
    ///
    /// The specification allows both because the collector takes what the author's manifest says, and
    /// an author writing for several languages writes a map. A reader that understood only one of the
    /// two would show a raw JSON object as a title.
    ///
    /// @param object       the entry
    /// @param key          the field's name
    /// @param fallback     what to answer when there is nothing
    /// @return the text
    private static @Nullable String localized(JsonObject object, String key, @Nullable String fallback) {
        JsonElement element = object.get(key);
        if (element == null || element.isJsonNull()) {
            return fallback;
        }
        if (element.isJsonPrimitive()) {
            String text = element.getAsString();
            return text.isBlank() ? fallback : text;
        }
        if (!element.isJsonObject()) {
            return fallback;
        }
        Map<String, JsonElement> byLanguage = new java.util.LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> pair : element.getAsJsonObject().entrySet()) {
            byLanguage.put(pair.getKey(), pair.getValue());
        }
        // The launcher's own language first, then its language without a region, then English, then
        // whatever the author wrote first. The last is the point: a pack that describes itself in
        // Japanese only should still show its description rather than nothing.
        java.util.Locale display = org.jackhuang.hmcl.util.i18n.I18n.getLocale().getDisplayLocale();
        for (String candidate : List.of(
                display.toString(), display.toString().replace('_', '-'), display.getLanguage(), "en")) {
            JsonElement value = byLanguage.get(candidate);
            if (value != null && value.isJsonPrimitive() && !value.getAsString().isBlank()) {
                return value.getAsString();
            }
        }
        for (JsonElement value : byLanguage.values()) {
            if (value.isJsonPrimitive() && !value.getAsString().isBlank()) {
                return value.getAsString();
            }
        }
        return fallback;
    }

    /// Reads a string field.
    ///
    /// @param object the entry
    /// @param key    the field's name
    /// @return the text, or `null` when it is absent or blank
    private static @Nullable String string(JsonObject object, String key) {
        JsonElement element = object.get(key);
        if (element == null || !element.isJsonPrimitive()) {
            return null;
        }
        String text = element.getAsString();
        return text.isBlank() ? null : text;
    }

    /// Reads a string field, with a fallback.
    ///
    /// @param object   the entry
    /// @param key      the field's name
    /// @param fallback what to answer when there is nothing
    /// @return the text
    private static String stringOr(JsonObject object, String key, String fallback) {
        String text = string(object, key);
        return text == null ? fallback : text;
    }

    /// Reads an integer field.
    ///
    /// @param object the entry
    /// @param key    the field's name
    /// @return the number, or `null` when it is absent or not a number
    private static @Nullable Integer integer(JsonObject object, String key) {
        JsonElement element = object.get(key);
        if (element == null || !element.isJsonPrimitive()
                || !element.getAsJsonPrimitive().isNumber()) {
            return null;
        }
        return element.getAsInt();
    }

    /// Reads an array of strings.
    ///
    /// The index is allowed to flatten the author's `dshVersions` set and may also leave it to the
    /// lazy manifest, so an entry without it is an ordinary entry and not a broken one. Entries that
    /// are not strings are dropped rather than shown: this list is only ever displayed.
    ///
    /// @param object the entry
    /// @param key    the field's name
    /// @return the strings, in the order written
    private static List<String> strings(JsonObject object, String key) {
        JsonElement element = object.get(key);
        if (element == null || !element.isJsonArray()) {
            return List.of();
        }
        List<String> values = new java.util.ArrayList<>();
        for (JsonElement item : element.getAsJsonArray()) {
            if (item.isJsonPrimitive()) {
                String text = item.getAsString().trim();
                if (!text.isEmpty() && !values.contains(text)) {
                    values.add(text);
                }
            }
        }
        return List.copyOf(values);
    }

    /// Reads a boolean field.
    ///
    /// @param object the entry
    /// @param key    the field's name
    /// @return the value, or `false` when it is absent or not a boolean
    private static boolean bool(JsonObject object, String key) {
        JsonElement element = object.get(key);
        return element != null && element.isJsonPrimitive()
                && element.getAsJsonPrimitive().isBoolean() && element.getAsBoolean();
    }

    /// Returns the address of the index to read.
    ///
    /// Three sources, most specific first, exactly as the plugin catalogue has: the property is for a
    /// developer who wants one run to read somewhere else, the setting is for a network that cannot
    /// reach the community host, and the constant is what the ecosystem publishes.
    ///
    /// @return the address
    public static String indexUrl() {
        String property = System.getProperty("hdsl.packMarket");
        if (property != null && !property.isBlank()) {
            return property.trim();
        }
        try {
            String setting = org.jackhuang.hmcl.setting.SettingsManager.settings()
                    .getPackMarketUrl();
            if (setting != null && !setting.isBlank()) {
                return setting.trim();
            }
        } catch (RuntimeException e) {
            LOG.warning("Could not read the market address from the settings", e);
        }
        return INDEX_URL;
    }

    /// Returns where the index is published, for a pack's own reference material.
    ///
    /// @param entry the pack
    /// @return the address of its manifest in the index's lazy directory
    public static String manifestUrl(Entry entry) {
        return lazyUrl(entry, "manifest.json");
    }

    /// Returns where a pack's README is published.
    ///
    /// @param entry the pack
    /// @return the address
    public static String readmeUrl(Entry entry) {
        return lazyUrl(entry, "README.md");
    }

    /// Builds the address of one of a pack's own documents.
    ///
    /// Resolved **against** the index's address rather than concatenated onto it, and that is not
    /// tidiness: the index is a file (`…/index.json`), so appending to it produces
    /// `…/index.json/packs/…`, which is a directory that does not exist. Resolving drops the last
    /// segment the way a browser does, and it keeps working for an index published at a directory
    /// address, which a mirror may well do.
    ///
    /// The id is used as it stands and never taken apart: an owner has no dot and a repository may,
    /// so the one thing that must not happen is splitting it back into two halves.
    ///
    /// @param entry the pack
    /// @param file  the document's name
    /// @return the address
    private static String lazyUrl(Entry entry, String file) {
        return java.net.URI.create(indexUrl()).resolve("packs/" + entry.id() + "/" + file).toString();
    }

    /// Reads one of a pack's own documents, or answers `null` when it is not there.
    ///
    /// The status is checked, which the launcher's own reader does not do: a host that answers a
    /// missing page with a **200 and an apology** — GitHub Pages answers 404, but a mirror or a proxy
    /// need not — would otherwise put an HTML error page where a README belongs, which is exactly what
    /// happened before this existed.
    ///
    /// @param url the address
    /// @return the document, or `null`
    public static @Nullable String document(String url) {
        try {
            java.net.http.HttpRequest request = java.net.http.HttpRequest
                    .newBuilder(java.net.URI.create(url))
                    .timeout(java.time.Duration.ofSeconds(20))
                    .header("Accept", "text/plain, application/json, text/markdown, */*")
                    .GET()
                    .build();
            java.net.http.HttpClient client = java.net.http.HttpClient.newBuilder()
                    .connectTimeout(java.time.Duration.ofSeconds(15))
                    .followRedirects(java.net.http.HttpClient.Redirect.NORMAL)
                    .build();
            try {
                java.net.http.HttpResponse<String> response = client.send(request,
                        java.net.http.HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() < 200 || response.statusCode() >= 300) {
                    return null;
                }
                return response.body();
            } finally {
                HttpClients.closeQuietly(client);
            }
        } catch (IOException | InterruptedException | RuntimeException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            LOG.info("Could not read " + url + ": " + e.getMessage());
            return null;
        }
    }

    /// Returns where the index is kept on disk between runs.
    ///
    /// @param url the address it was read from
    /// @return the file
    static Path cachedFile(String url) {
        // Named from the address rather than fixed, so that a launcher pointed at a mirror does not
        // overwrite the copy it kept from the community one: the two are different documents.
        return DshPluginCatalog.cacheDirectory().resolve("market-" + hashedName(url) + ".json");
    }

    /// Hashes an address into a file name.
    ///
    /// @param url the address
    /// @return the name
    private static String hashedName(String url) {
        try {
            byte[] digest = java.security.MessageDigest.getInstance("SHA-256")
                    .digest(url.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (int i = 0; i < 8; i++) {
                hex.append(String.format("%02x", digest[i]));
            }
            return hex.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            // Every JVM has SHA-256; a name that cannot collide is still needed so this never throws.
            return "index-" + Integer.toHexString(url.hashCode());
        }
    }

    /// Keeps a copy of the index so a later fetch has something to fall back on.
    ///
    /// @param cached where to keep it
    /// @param body   the document
    private static void keepCopy(Path cached, String body) {
        try {
            Files.createDirectories(cached.getParent());
            Files.writeString(cached, body);
        } catch (IOException | RuntimeException e) {
            LOG.warning("Could not keep a copy of the modpack index at " + cached, e);
        }
    }

    /// Reads the kept copy, if there is one.
    ///
    /// @param cached where it is kept
    /// @return the document, or `null`
    private static @Nullable String readCached(Path cached) {
        try {
            return Files.isRegularFile(cached) ? Files.readString(cached) : null;
        } catch (IOException | RuntimeException e) {
            return null;
        }
    }
}
