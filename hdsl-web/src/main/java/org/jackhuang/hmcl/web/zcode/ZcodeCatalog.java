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
package org.jackhuang.hmcl.web.zcode;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jackhuang.hmcl.web.brand.GitHubToken;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.jackhuang.hmcl.util.logging.Logger.LOG;

/// ZCode's version catalog: official GitHub Releases, tag-only releases, and
/// the unreleased `main` snapshot.
///
/// ZCode publishes desktop installers, not a CLI/server package; every entry
/// here is still installed by building its source archive. The catalog exists
/// so the panel can offer the same version picker as the other brands, and so
/// the credential-format boundary (3.18.x) is visible before an instance is
/// moved onto it.
@NotNullByDefault
public final class ZcodeCatalog {

    /// One selectable upstream version.
    ///
    /// @param ref             what the builder accepts (`v3.17.2`, a tag, `main`)
    /// @param version         display version (`3.17.2`, `main@abc1234`)
    /// @param channel         `release`, `tag`, or `main`
    /// @param commit          the release/tag commit when known
    /// @param publishedAt     release date, or the commit date for `main`; 0 for tag-only
    /// @param credentialFormat 1 = legacy credentials, 2 = system credential storage
    /// @param requiresSecret  whether the standalone server needs `credential-secret`
    /// @param installed       whether a build exists under `releases/`
    /// @param buildState      `installed`, `not-installed`, `building`, or `failed`
    public record Entry(
            String ref,
            String version,
            String channel,
            @Nullable String commit,
            long publishedAt,
            int credentialFormat,
            boolean requiresSecret,
            boolean installed,
            String buildState,
            boolean prerelease,
            String notes) {
    }

    /// The full catalog plus when it was fetched.
    public record Snapshot(List<Entry> entries, long fetchedAt) {
        public @Nullable Entry find(String refOrVersion) {
            for (Entry entry : entries) {
                if (entry.ref().equals(refOrVersion) || entry.version().equals(refOrVersion)) {
                    return entry;
                }
            }
            return null;
        }
    }

    private static final String API = "https://api.github.com/repos/zai-org/ZCode";
    private static final long TTL_MILLIS = 30 * 60 * 1000L;
    private static final Duration TIMEOUT = Duration.ofSeconds(15);
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(TIMEOUT)
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();
    private static final Object LOCK = new Object();
    private static final AtomicReference<Snapshot> CACHE = new AtomicReference<>();

    private ZcodeCatalog() {
    }

    /// Loads the catalog, reusing a 30-minute snapshot unless `force`.
    ///
    /// @param root  `<dataDir>/zcode`
    /// @param force bypass the cache
    /// @return the snapshot
    /// @throws ZcodeException when GitHub cannot be reached and no cache exists
    public static Snapshot load(Path root, boolean force) throws ZcodeException {
        Snapshot cached = CACHE.get();
        if (!force && cached != null
                && System.currentTimeMillis() - cached.fetchedAt() < TTL_MILLIS) {
            return cached;
        }
        synchronized (LOCK) {
            cached = CACHE.get();
            if (!force && cached != null
                    && System.currentTimeMillis() - cached.fetchedAt() < TTL_MILLIS) {
                return cached;
            }
            try {
                Snapshot fetched = fetch(root);
                CACHE.set(fetched);
                return fetched;
            } catch (IOException e) {
                if (cached != null) {
                    LOG.warning("Could not refresh ZCode catalog; serving the previous snapshot", e);
                    return cached;
                }
                throw new ZcodeException("无法读取 ZCode 版本目录: " + e.getMessage(), e);
            }
        }
    }

    /// The credential format for a ref. `main` and 3.18.0+ use the system
    /// credential storage; everything older keeps the legacy format.
    public static int formatOf(@Nullable String ref) {
        if (ref == null || ref.isBlank()) {
            return 0;
        }
        if (isMain(ref)) {
            return 2;
        }
        String version = stripV(ref);
        int dash = version.indexOf('-');
        if (dash >= 0) {
            version = version.substring(0, dash);
        }
        String[] parts = version.split("\\.");
        if (parts.length < 3) {
            return 1;
        }
        try {
            int major = Integer.parseInt(parts[0]);
            int minor = Integer.parseInt(parts[1]);
            int patch = Integer.parseInt(parts[2]);
            return major > 3 || (major == 3 && (minor > 18 || (minor == 18 && patch >= 0))) ? 2 : 1;
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    public static boolean isMain(@Nullable String ref) {
        return ref != null && ("main".equals(ref) || ref.startsWith("main@"));
    }

    /// A human reason why `target` must not be selected for `instance`, or null.
    public static @Nullable String downgradeReason(ZcodeInstance instance, Entry target) {
        if (instance.credentialFormat() > 0
                && target.credentialFormat() < instance.credentialFormat()) {
            return "该实例已使用凭据格式 v" + instance.credentialFormat()
                    + "，不能切换到只支持 v" + target.credentialFormat() + " 的版本";
        }
        String floor = instance.minVersion();
        if (floor == null || floor.isBlank()) {
            return null;
        }
        if (isMain(floor)) {
            if (!isMain(target.ref())
                    && (target.publishedAt() <= 0 || target.publishedAt() < instance.minVersionAt())) {
                return "该实例已使用 " + floor + "，不能回退到更早的正式版 " + target.ref();
            }
            return null;
        }
        if (isMain(target.ref())) {
            return null;
        }
        int comparison = compareSemver(target.version(), floor);
        if (comparison < 0) {
            return "该实例最低可切换版本是 " + floor;
        }
        return null;
    }

    /// Semver comparison of the numeric core; prereleases sort before release.
    public static int compareSemver(String left, String right) {
        String[] a = core(left);
        String[] b = core(right);
        for (int i = 0; i < 3; i++) {
            int x = parseInt(a[i]);
            int y = parseInt(b[i]);
            if (x != y) {
                return Integer.compare(x, y);
            }
        }
        boolean aPre = left != null && left.contains("-");
        boolean bPre = right != null && right.contains("-");
        if (aPre != bPre) {
            return aPre ? -1 : 1;
        }
        return 0;
    }

    private static String[] core(String version) {
        String value = stripV(version == null ? "" : version);
        int dash = value.indexOf('-');
        if (dash >= 0) {
            value = value.substring(0, dash);
        }
        String[] parts = value.split("\\.");
        String[] result = {"0", "0", "0"};
        for (int i = 0; i < Math.min(3, parts.length); i++) {
            result[i] = parts[i].replaceAll("[^0-9].*$", "");
            if (result[i].isEmpty()) {
                result[i] = "0";
            }
        }
        return result;
    }

    private static int parseInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static String stripV(String version) {
        return version.startsWith("v") || version.startsWith("V")
                ? version.substring(1) : version;
    }

    private static Snapshot fetch(Path root) throws IOException {
        String token = GitHubToken.get();
        JsonArray releases = array(get(API + "/releases?per_page=30", token));
        JsonArray tags = array(get(API + "/tags?per_page=100", token));
        JsonObject main = object(get(API + "/commits/main", token));

        Set<String> installedVersions = new HashSet<>();
        Set<String> installedRefs = new HashSet<>();
        for (ZcodeBuilder.Release release : ZcodeBuilder.releases(root)) {
            installedVersions.add(release.version());
            Path manifest = Path.of(release.path()).resolve("release.json");
            if (Files.isRegularFile(manifest)) {
                try {
                    JsonObject parsed = JsonParser.parseString(
                            Files.readString(manifest, StandardCharsets.UTF_8)).getAsJsonObject();
                    if (parsed.has("ref") && parsed.get("ref").isJsonPrimitive()) {
                        installedRefs.add(parsed.get("ref").getAsString());
                    }
                } catch (RuntimeException ignored) {
                    // An old release without a manifest is still installed by version.
                }
            }
        }

        ZcodeBuilder.Status build = ZcodeBuilder.status();
        String building = build.state() == ZcodeBuilder.State.RUNNING ? build.version() : null;
        String failed = build.state() == ZcodeBuilder.State.ERROR ? build.version() : null;

        Map<String, Entry> byRef = new HashMap<>();
        for (JsonElement element : releases) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject release = element.getAsJsonObject();
            if (bool(release, "draft")) {
                continue;
            }
            String tag = string(release, "tag_name");
            if (tag == null || tag.isBlank()) {
                continue;
            }
            String version = stripV(tag);
            boolean installed = installedVersions.contains(version) || installedRefs.contains(tag);
            byRef.put(tag, new Entry(tag, version, "release",
                    null, instant(string(release, "published_at")),
                    formatOf(tag), formatOf(tag) >= 2, installed,
                    buildState(tag, version, installed, building, failed),
                    bool(release, "prerelease"), notes(string(release, "body"))));
        }

        for (JsonElement element : tags) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject tag = element.getAsJsonObject();
            String name = string(tag, "name");
            if (name == null || name.isBlank() || byRef.containsKey(name)) {
                continue;
            }
            String version = stripV(name);
            String commit = null;
            JsonObject commitObject = object(tag.get("commit"));
            if (commitObject != null) {
                commit = string(commitObject, "sha");
            }
            boolean installed = installedVersions.contains(version) || installedRefs.contains(name);
            byRef.put(name, new Entry(name, version, "tag", commit, 0,
                    formatOf(name), formatOf(name) >= 2, installed,
                    buildState(name, version, installed, building, failed),
                    false, ""));
        }

        if (main != null) {
            String sha = string(main, "sha");
            String shortSha = sha == null || sha.length() < 7 ? sha : sha.substring(0, 7);
            String version = "main@" + (shortSha == null ? "unknown" : shortSha);
            long publishedAt = 0;
            String notes = "";
            JsonObject commit = object(main.get("commit"));
            if (commit != null) {
                JsonObject author = object(commit.get("author"));
                if (author != null) {
                    publishedAt = instant(string(author, "date"));
                }
                notes = notes(string(commit, "message"));
            }
            boolean installed = installedRefs.contains("main");
            byRef.put("main", new Entry("main", version, "main", sha, publishedAt,
                    formatOf("main"), true, installed,
                    buildState("main", version, installed, building, failed),
                    false, notes));
        }

        List<Entry> entries = new ArrayList<>(byRef.values());
        entries.sort(Comparator
                .comparingInt((Entry entry) -> "main".equals(entry.channel()) ? 0
                        : "release".equals(entry.channel()) ? 1 : 2)
                .thenComparing(Comparator.comparingLong(Entry::publishedAt).reversed())
                .thenComparing(Entry::version, Comparator.reverseOrder()));
        return new Snapshot(List.copyOf(entries), System.currentTimeMillis());
    }

    private static String buildState(String ref, String version, boolean installed,
                                     @Nullable String building, @Nullable String failed) {
        if (building != null && (building.equals(ref) || building.equals(version))) {
            return "building";
        }
        if (failed != null && (failed.equals(ref) || failed.equals(version))) {
            return "failed";
        }
        return installed ? "installed" : "not-installed";
    }

    private static @Nullable JsonElement get(String url, @Nullable String token) throws IOException {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url))
                .timeout(TIMEOUT)
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "HDSL-web")
                .GET();
        if (token != null && !token.isBlank()) {
            builder.header("Authorization", "Bearer " + token);
        }
        try {
            HttpResponse<String> response = CLIENT.send(builder.build(),
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() == 404) {
                return null;
            }
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IOException("GitHub answered " + response.statusCode() + " for " + url);
            }
            return JsonParser.parseString(response.body());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("interrupted while reading " + url, e);
        }
    }

    private static JsonArray array(@Nullable JsonElement element) {
        return element != null && element.isJsonArray() ? element.getAsJsonArray() : new JsonArray();
    }

    private static @Nullable JsonObject object(@Nullable JsonElement element) {
        return element != null && element.isJsonObject() ? element.getAsJsonObject() : null;
    }

    private static @Nullable String string(@Nullable JsonObject object, String name) {
        if (object == null) {
            return null;
        }
        JsonElement element = object.get(name);
        return element != null && element.isJsonPrimitive() ? element.getAsString() : null;
    }

    private static boolean bool(JsonObject object, String name) {
        JsonElement element = object.get(name);
        return element != null && element.isJsonPrimitive() && element.getAsBoolean();
    }

    private static long instant(@Nullable String value) {
        if (value == null || value.isBlank()) {
            return 0;
        }
        try {
            return Instant.parse(value).toEpochMilli();
        } catch (DateTimeParseException e) {
            return 0;
        }
    }

    private static String notes(@Nullable String value) {
        if (value == null) {
            return "";
        }
        String line = value.strip().replaceAll("\\s+", " ");
        return line.length() <= 240 ? line : line.substring(0, 240) + "…";
    }
}
