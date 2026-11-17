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
package org.jackhuang.hmcl.web.minecraft;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jackhuang.hmcl.util.logging.Logger;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.ZipFile;

/// Plugins and mods inside one instance: the files on disk, and installing from
/// Modrinth.
///
/// **Disabled means renamed.** A plugin or mod is enabled by its name ending in
/// `.jar`; the panel disables one by renaming it to `.jar.disabled`, which is
/// what the loaders ignore and what a human can undo by hand. No sidecar state,
/// no database.
///
/// **The folder follows the core.** Paper/Folia/Purpur load from `plugins/`,
/// Fabric from `mods/`; vanilla loads neither, so the panel refuses instead of
/// writing files nothing will read.
///
/// **Downloads come from Modrinth, at runtime, with the published hash.** The
/// jar is verified against SHA-512 before it lands in the instance; a file that
/// does not match is deleted, not kept "just in case".
@NotNullByDefault
public final class McMods {

    /// Modrinth's public API; `HDSL_MODRINTH_API` overrides it for mirrors.
    public static final URI DEFAULT_API = URI.create("https://api.modrinth.com/v2");
    /// The domestic Modrinth mirror. Used as a fallback, never instead of the
    /// official API when that one answers: the hash we verify comes from the
    /// same response, so the source has to be the one the operator expects.
    public static final URI MCIM_API = URI.create("https://mod.mcimirror.top/modrinth/v2");
    /// A mod jar is megabytes; anything past this is not one.
    private static final long MAX_JAR_BYTES = 256L * 1024 * 1024;

    private static final Logger LOG = Logger.LOG;

    /// One file in `mods/` or `plugins/`.
    ///
    /// @param name     the file name as it is on disk
    /// @param enabled  whether the loader will read it (`.jar` vs `.jar.disabled`)
    /// @param bytes    the file size
    /// @param clientOnly whether every descriptor inside the jar marks it client-side
    public record Entry(String name, boolean enabled, long bytes, boolean clientOnly,
                        @Nullable String projectId, @Nullable String title,
                        @Nullable String versionNumber) {

        public Entry(String name, boolean enabled, long bytes, boolean clientOnly) {
            this(name, enabled, bytes, clientOnly, null, null, null);
        }
    }

    /// One search hit from Modrinth.
    ///
    /// @param projectId the project id
    /// @param slug      the URL slug
    /// @param title     the display name
    /// @param author    the author
    /// @param downloads total downloads
    /// @param description one-line summary
    public record Hit(String projectId, String slug, String title, String author,
                      long downloads, String description) {
    }

    /// One page of search results.
    ///
    /// @param hits      this page
    /// @param totalHits how many the query matches in total
    /// @param relaxed   the version filter was dropped: these results do not
    ///                  claim compatibility, and install will check again
    public record SearchResult(List<Hit> hits, int totalHits, boolean relaxed) {
    }

    /// What the instance's core loads.
    ///
    /// @param kind   `mods` or `plugins`
    /// @param loader the Modrinth loader name, or `null` when there is none
    public record Folder(String kind, @Nullable String loader) {
    }

    private final URI api;
    private final HttpClient client;

    public McMods() {
        this(apiFromEnvironment());
    }

    /// The same, with an overridable API base (mirrors, tests).
    ///
    /// @param api the Modrinth-compatible API base
    public McMods(URI api) {
        this.api = api;
        this.client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();
    }

    private static URI apiFromEnvironment() {
        String override = System.getenv("HDSL_MODRINTH_API");
        if (override != null && !override.isBlank()) {
            try {
                return URI.create(override.strip());
            } catch (IllegalArgumentException e) {
                LOG.warning("Ignoring HDSL_MODRINTH_API=`" + override + "`: " + e.getMessage());
            }
        }
        return DEFAULT_API;
    }

    /// The folder a core loads from, or `null` when it loads neither.
    ///
    /// @param core the core id
    /// @return the folder description, or `null` for vanilla
    public static @Nullable Folder folderOf(String core) {
        return switch (core) {
            case "paper" -> new Folder("plugins", "paper");
            case "folia" -> new Folder("plugins", "folia");
            case "purpur" -> new Folder("plugins", "purpur");
            case "fabric" -> new Folder("mods", "fabric");
            case "forge" -> new Folder("mods", "forge");
            case "neoforge" -> new Folder("mods", "neoforge");
            default -> null;
        };
    }

    /// Validates a caller-supplied folder name.
    ///
    /// @param kind `mods` or `plugins`
    /// @return the normalised name
    /// @throws McException when it is neither
    public static String requireKind(String kind) throws McException {
        if (kind == null) {
            throw new McException("kind must be `mods` or `plugins`");
        }
        String normalised = kind.trim().toLowerCase(Locale.ROOT);
        if (!normalised.equals("mods") && !normalised.equals("plugins")) {
            throw new McException("kind must be `mods` or `plugins`");
        }
        return normalised;
    }

    /// Lists the jars and disabled jars in one folder.
    ///
    /// @param instanceDir the instance directory
    /// @param kind        `mods` or `plugins`
    /// @return the entries, enabled first and then by name
    /// @throws McException when the folder cannot be read
    public List<Entry> list(Path instanceDir, String kind) throws McException {
        String folder = requireKind(kind);
        Path directory = instanceDir.resolve(folder);
        List<Entry> entries = new ArrayList<>();
        if (!Files.isDirectory(directory)) {
            return entries;
        }
        try (var stream = Files.list(directory)) {
            for (Path file : stream.filter(Files::isRegularFile).sorted().toList()) {
                String name = file.getFileName().toString();
                boolean enabled;
                if (name.endsWith(".jar")) {
                    enabled = true;
                } else if (name.endsWith(".jar.disabled")) {
                    enabled = false;
                } else {
                    continue;
                }
                JsonObject meta = readSidecar(file);
                entries.add(new Entry(name, enabled, Files.size(file), clientOnly(file),
                        meta == null ? null : string(meta, "projectId"),
                        meta == null ? null : string(meta, "title"),
                        meta == null ? null : string(meta, "versionNumber")));
            }
        } catch (IOException e) {
            throw new McException("Failed to read " + directory + ": " + e.getMessage(), e);
        }
        entries.sort(Comparator.comparing(Entry::enabled).reversed()
                .thenComparing(Entry::name, String.CASE_INSENSITIVE_ORDER));
        return List.copyOf(entries);
    }

    /// Turns one file on or off by renaming it.
    ///
    /// @param instanceDir the instance directory
    /// @param kind        `mods` or `plugins`
    /// @param name        the file name as listed
    /// @param enabled     the wanted state
    /// @return the new name
    /// @throws McException when the file is not there or cannot be renamed
    public String toggle(Path instanceDir, String kind, String name, boolean enabled) throws McException {
        Path file = resolve(instanceDir, kind, name);
        String current = file.getFileName().toString();
        boolean isEnabled = current.endsWith(".jar");
        if (isEnabled == enabled) {
            return current;
        }
        String next = enabled
                ? current.substring(0, current.length() - ".disabled".length())
                : current + ".disabled";
        Path target = file.resolveSibling(next);
        if (Files.exists(target)) {
            throw new McException("`" + next + "` already exists");
        }
        try {
            Files.move(file, target, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            try {
                Files.move(file, target);
            } catch (IOException again) {
                throw new McException("Failed to rename `" + current + "`: " + again.getMessage(), again);
            }
        }
        return next;
    }

    /// Deletes one file.
    ///
    /// @param instanceDir the instance directory
    /// @param kind        `mods` or `plugins`
    /// @param name        the file name
    /// @throws McException when the file is not there or cannot be removed
    public void delete(Path instanceDir, String kind, String name) throws McException {
        Path file = resolve(instanceDir, kind, name);
        try {
            if (!Files.deleteIfExists(file)) {
                throw new McException("`" + name + "` is not there");
            }
            Files.deleteIfExists(sidecarOf(file));
        } catch (IOException e) {
            throw new McException("Failed to delete `" + name + "`: " + e.getMessage(), e);
        }
    }

    /// Stores an uploaded jar under its given name, refusing to overwrite.
    ///
    /// @param instanceDir the instance directory
    /// @param kind        `mods` or `plugins`
    /// @param name        the file name (must end in `.jar`)
    /// @param in          the body
    /// @return the stored name
    /// @throws McException when the name is wrong, the file exists, or writing fails
    public String upload(Path instanceDir, String kind, String name, InputStream in) throws McException {
        String folder = requireKind(kind);
        String safe = requireJarName(name);
        Path directory = instanceDir.resolve(folder);
        Path target = directory.resolve(safe);
        if (!target.normalize().startsWith(directory.normalize())) {
            throw new McException("invalid file name");
        }
        try {
            Files.createDirectories(directory);
            if (Files.exists(target)) {
                throw new McException("`" + safe + "` already exists; rename or delete it first");
            }
            Path temp = Files.createTempFile(directory, "upload", ".part");
            try {
                long copied = 0;
                try (OutputStream out = Files.newOutputStream(temp)) {
                    byte[] buffer = new byte[64 * 1024];
                    int read;
                    while ((read = in.read(buffer)) >= 0) {
                        copied += read;
                        if (copied > MAX_JAR_BYTES) {
                            throw new McException("the upload is larger than the panel accepts");
                        }
                        out.write(buffer, 0, read);
                    }
                }
                requireZip(temp);
                Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE);
            } finally {
                Files.deleteIfExists(temp);
            }
            return safe;
        } catch (IOException e) {
            throw new McException("Failed to store `" + safe + "`: " + e.getMessage(), e);
        }
    }

    /// Searches Modrinth for something the instance can load.
    ///
    /// @param query   the search text
    /// @param folder  what the instance loads
    /// @param version the instance's game version
    /// @return the hits, best first
    /// @throws McException when the API cannot be read
    public SearchResult search(String query, Folder folder, String version) throws McException {
        return search(query, folder, version, 0, 20);
    }

    /// One page of search results.
    ///
    /// The version facet is dropped when it would hide everything: plugins often
    /// list only the releases they tested against, and "no results" is a worse
    /// answer than "these may work, install will check". The second page is
    /// marked [SearchResult#relaxed] so the UI can say so.
    ///
    /// @param query   the search text; Chinese terms are translated first
    /// @param folder  what the instance loads
    /// @param version the instance's game version
    /// @param offset  how many hits to skip
    /// @param limit   how many to return (1–50)
    /// @return the page
    /// @throws McException when the API cannot be read
    public SearchResult search(String query, Folder folder, String version, int offset, int limit)
            throws McException {
        int size = Math.max(1, Math.min(50, limit));
        String text = translate(query);
        SearchResult strict = searchOnce(text, folder, version, offset, size, false);
        if (!strict.hits().isEmpty() || version == null || version.isBlank()) {
            return strict;
        }
        SearchResult relaxed = searchOnce(text, folder, version, offset, size, true);
        return new SearchResult(relaxed.hits(), relaxed.totalHits(), true);
    }

    private SearchResult searchOnce(String query, Folder folder, String version, int offset,
                                    int limit, boolean withoutVersion) throws McException {
        String projectType = folder.kind().equals("plugins") ? "plugin" : "mod";
        List<String> facets = new ArrayList<>();
        facets.add("[\"project_type:" + projectType + "\"]");
        if (folder.loader() != null) {
            if ("plugins".equals(folder.kind())) {
                // Paper runs Bukkit and Spigot plugins too; filtering on `paper`
                // alone hides most of the catalogue. Inner array is OR.
                facets.add("[\"categories:paper\",\"categories:spigot\","
                        + "\"categories:bukkit\",\"categories:purpur\",\"categories:folia\"]");
            } else {
                facets.add("[\"categories:" + folder.loader() + "\"]");
            }
        }
        if (!withoutVersion && version != null && !version.isBlank()) {
            facets.add("[\"versions:" + version.trim() + "\"]");
        }
        String path = "/search?limit=" + limit + "&offset=" + Math.max(0, offset)
                + "&index=relevance&query="
                + URLEncoder.encode(query == null ? "" : query, StandardCharsets.UTF_8)
                + "&facets=" + URLEncoder.encode("[" + String.join(",", facets) + "]",
                        StandardCharsets.UTF_8);
        JsonObject root = readWithFallback(path);
        JsonArray hits = root.has("hits") && root.get("hits").isJsonArray()
                ? root.getAsJsonArray("hits") : new JsonArray();
        List<Hit> results = new ArrayList<>();
        for (JsonElement element : hits) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject hit = element.getAsJsonObject();
            String projectId = string(hit, "project_id");
            String slug = string(hit, "slug");
            String title = string(hit, "title");
            if (projectId == null || title == null) {
                continue;
            }
            String author = string(hit, "author");
            long downloads = hit.has("downloads") ? hit.get("downloads").getAsLong() : 0;
            String description = string(hit, "description");
            results.add(new Hit(projectId, slug == null ? projectId : slug, title,
                    author == null ? "" : author, downloads, description == null ? "" : description));
        }
        int total = root.has("total_hits") ? root.get("total_hits").getAsInt() : results.size();
        return new SearchResult(List.copyOf(results), total, withoutVersion);
    }

    /// A handful of Chinese terms for the queries a Chinese operator actually
    /// types. Modrinth has no Chinese metadata, so "经济" would otherwise return
    /// nothing useful — this is a dictionary, not a translation service.
    private static final Map<String, String> ZH = Map.ofEntries(
            Map.entry("经济", "economy"), Map.entry("商店", "shop"),
            Map.entry("领地", "claim"), Map.entry("权限", "permissions"),
            Map.entry("传送", "teleport"), Map.entry("小地图", "minimap"),
            Map.entry("优化", "optimization"), Map.entry("反作弊", "anticheat"),
            Map.entry("菜单", "menu"), Map.entry("聊天", "chat"),
            Map.entry("世界", "world"), Map.entry("备份", "backup"),
            Map.entry("任务", "quest"), Map.entry("技能", "skills"),
            Map.entry("附魔", "enchant"), Map.entry("家具", "furniture"),
            Map.entry("枪械", "gun"), Map.entry("载具", "vehicle"),
            Map.entry("建筑", "building"), Map.entry("箱子", "chest"),
            Map.entry("刷怪", "mob"), Map.entry("经济系统", "economy"),
            Map.entry("出生点", "spawn"), Map.entry("保护", "protection"));

    static String translate(@Nullable String query) {
        if (query == null || query.isBlank()) {
            return query == null ? "" : query;
        }
        String text = query;
        for (Map.Entry<String, String> entry : ZH.entrySet()) {
            text = text.replace(entry.getKey(), entry.getValue());
        }
        return text.strip();
    }

    /// Installs the newest compatible version of a project into the instance.
    ///
    /// @param instanceDir the instance directory
    /// @param folder      what the instance loads
    /// @param projectId   the Modrinth project id
    /// @param version     the game version
    /// @return the stored file name
    /// @throws McException when no compatible version exists, the download
    ///                      fails, or the hash does not match
    public String install(Path instanceDir, Folder folder, String projectId, String version)
            throws McException {
        StringBuilder path = new StringBuilder("/project/")
                .append(URLEncoder.encode(projectId, StandardCharsets.UTF_8))
                .append("/version");
        List<String> params = new ArrayList<>();
        if (folder.loader() != null) {
            params.add("loaders=" + URLEncoder.encode("[\"" + folder.loader() + "\"]",
                    StandardCharsets.UTF_8));
        }
        if (version != null && !version.isBlank()) {
            params.add("game_versions=" + URLEncoder.encode("[\"" + version.trim() + "\"]",
                    StandardCharsets.UTF_8));
        }
        if (!params.isEmpty()) {
            path.append("?").append(String.join("&", params));
        }
        JsonElement root = readElementWithFallback(path.toString());
        JsonArray versions = root.isJsonArray() ? root.getAsJsonArray() : null;
        if (versions == null || versions.isEmpty()) {
            throw new McException("no version of " + projectId + " works with this core and game version");
        }
        JsonObject first = versions.get(0).getAsJsonObject();
        JsonArray files = first.has("files") && first.get("files").isJsonArray()
                ? first.getAsJsonArray("files") : new JsonArray();
        if (files.isEmpty()) {
            throw new McException("the project's version publishes no file");
        }
        JsonObject file = files.get(0).getAsJsonObject();
        String filename = string(file, "filename");
        String download = string(file, "url");
        if (filename == null || download == null) {
            throw new McException("the project's version publishes an incomplete file");
        }
        String sha512 = null;
        if (file.has("hashes") && file.get("hashes").isJsonObject()) {
            sha512 = string(file.getAsJsonObject("hashes"), "sha512");
        }
        String safe = requireJarName(filename);
        Path directory = instanceDir.resolve(folder.kind());
        try {
            Files.createDirectories(directory);
        } catch (IOException e) {
            throw new McException("Failed to create " + directory + ": " + e.getMessage(), e);
        }
        Path target = directory.resolve(safe);
        try {
            Path temp = Files.createTempFile(directory, "mod", ".part");
            try {
                download(download, temp, "SHA-512", sha512);
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
            } finally {
                Files.deleteIfExists(temp);
            }
        } catch (IOException e) {
            throw new McException("Failed to install `" + safe + "`: " + e.getMessage(), e);
        }
        // The sidecar is what makes the file list useful later: without it the
        // UI can only show `SomePlugin-1.2.3.jar` and has no idea what project
        // it came from or which version it is.
        JsonObject meta = new JsonObject();
        meta.addProperty("projectId", projectId);
        addIfPresent(meta, "versionId", string(first, "id"));
        addIfPresent(meta, "versionNumber", string(first, "version_number"));
        try {
            JsonObject project = readWithFallback("/project/"
                    + URLEncoder.encode(projectId, StandardCharsets.UTF_8));
            addIfPresent(meta, "slug", string(project, "slug"));
            addIfPresent(meta, "title", string(project, "title"));
        } catch (McException e) {
            LOG.warning("Could not read the project metadata for " + projectId + ": "
                    + e.getMessage());
        }
        meta.addProperty("installedAt", java.time.Instant.now().toString());
        writeSidecar(target, meta);
        return safe;
    }

    private static void addIfPresent(JsonObject object, String name, @Nullable String value) {
        if (value != null && !value.isBlank()) {
            object.addProperty(name, value);
        }
    }

    private void download(String url, Path target, String algorithm, @Nullable String digest)
            throws McException {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofMinutes(15))
                    .header("User-Agent", "HDSL-web")
                    .GET()
                    .build();
            HttpResponse<InputStream> answer = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (answer.statusCode() != 200) {
                throw new McException("the " + url + " download answered HTTP " + answer.statusCode());
            }
            MessageDigest hasher = digest == null ? null : MessageDigest.getInstance(algorithm);
            long copied = 0;
            try (InputStream in = answer.body(); OutputStream out = Files.newOutputStream(target)) {
                byte[] buffer = new byte[64 * 1024];
                int read;
                while ((read = in.read(buffer)) >= 0) {
                    copied += read;
                    if (copied > MAX_JAR_BYTES) {
                        throw new McException("the download is larger than the panel accepts");
                    }
                    if (hasher != null) {
                        hasher.update(buffer, 0, read);
                    }
                    out.write(buffer, 0, read);
                }
            }
            if (hasher != null) {
                String actual = HexFormat.of().formatHex(hasher.digest());
                if (!actual.equalsIgnoreCase(digest)) {
                    throw new McException("the download failed its " + algorithm + " check");
                }
            }
            requireZip(target);
        } catch (IOException e) {
            throw new McException("Failed to download " + url + ": " + e.getMessage(), e);
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new McException(algorithm + " is not available in this JVM", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new McException("the download was interrupted", e);
        }
    }

    /// Resolves one file name inside the instance's folder, refusing escapes.
    private static Path resolve(Path instanceDir, String kind, String name) throws McException {
        String folder = requireKind(kind);
        String safe = requireJarName(name);
        Path directory = instanceDir.resolve(folder).normalize();
        Path file = directory.resolve(safe).normalize();
        if (!file.startsWith(directory) || !Files.isRegularFile(file)) {
            throw new McException("`" + safe + "` is not there");
        }
        return file;
    }

    /// File names the loaders read: a jar, optionally with `.disabled` suffixed.
    private static String requireJarName(@Nullable String name) throws McException {
        if (name == null || name.isBlank() || name.contains("/") || name.contains("\\")
                || name.contains("..") || name.indexOf('\0') >= 0) {
            throw new McException("invalid file name");
        }
        String safe = name.strip();
        if (!safe.endsWith(".jar") && !safe.endsWith(".jar.disabled")) {
            throw new McException("only `.jar` files belong here");
        }
        return safe;
    }

    private static void requireZip(Path file) throws McException {
        try (InputStream in = Files.newInputStream(file)) {
            byte[] magic = in.readNBytes(4);
            if (magic.length < 4 || magic[0] != 'P' || magic[1] != 'K') {
                throw new McException("the downloaded file is not a jar (zip) archive");
            }
        } catch (IOException e) {
            throw new McException("Failed to inspect " + file, e);
        }
    }

    /// Whether a jar says, by its own metadata, that it is client-side only.
    ///
    /// This reads the descriptor the loader will read (`fabric.mod.json`,
    /// `mods.toml`, `quilt.mod.json`); a jar with none of them is reported as
    /// not client-only, because guessing would disable something that works.
    private static boolean clientOnly(Path file) {
        try (ZipFile zip = new ZipFile(file.toFile())) {
            var fabric = zip.getEntry("fabric.mod.json");
            if (fabric != null) {
                String text = new String(zip.getInputStream(fabric).readAllBytes(), StandardCharsets.UTF_8);
                JsonObject root = JsonParser.parseString(text).getAsJsonObject();
                JsonObject environment = root.has("environment") && root.get("environment").isJsonObject()
                        ? root.getAsJsonObject("environment") : null;
                if (environment != null) {
                    String client = string(environment, "client");
                    if ("required".equals(client)) {
                        return true;
                    }
                    String server = string(environment, "server");
                    if ("unsupported".equals(server)) {
                        return true;
                    }
                }
            }
            var quilt = zip.getEntry("quilt.mod.json");
            if (quilt != null) {
                String text = new String(zip.getInputStream(quilt).readAllBytes(), StandardCharsets.UTF_8);
                return text.contains("\"minecraft\"") && text.contains("\"client\"");
            }
            var toml = zip.getEntry("META-INF/mods.toml");
            if (toml == null) {
                toml = zip.getEntry("META-INF/neoforge.mods.toml");
            }
            if (toml != null) {
                String text = new String(zip.getInputStream(toml).readAllBytes(), StandardCharsets.UTF_8);
                return text.toLowerCase(Locale.ROOT).contains("side = \"client\"")
                        || text.toLowerCase(Locale.ROOT).contains("side=\"client\"");
            }
        } catch (IOException | RuntimeException e) {
            // A jar that cannot be read is not evidence either way.
            return false;
        }
        return false;
    }

    private String base() {
        return base(api);
    }

    private static String base(URI uri) {
        return uri.toString().replaceAll("/+$", "");
    }

    /// Reads a path from the official API, then from the mirror if the official
    /// one is unreachable. The response (and therefore the hash) still comes
    /// from one place; the fallback changes the host, not the trust model.
    private JsonElement readElementWithFallback(String path) throws McException {
        try {
            return fetchJsonElement(URI.create(base() + path));
        } catch (McException first) {
            if (api.equals(MCIM_API)) {
                throw first;
            }
            LOG.warning("The Modrinth API failed (" + first.getMessage()
                    + "); trying the mirror at " + MCIM_API);
            try {
                return fetchJsonElement(URI.create(base(MCIM_API) + path));
            } catch (McException second) {
                LOG.warning("The Modrinth mirror failed too: " + second.getMessage());
                throw first;
            }
        }
    }

    private JsonObject readWithFallback(String path) throws McException {
        JsonElement element = readElementWithFallback(path);
        if (!element.isJsonObject()) {
            throw new McException(path + " did not answer a JSON object");
        }
        return element.getAsJsonObject();
    }

    /// The `.panel.json` beside a jar, shared by its enabled and disabled names.
    static Path sidecarOf(Path jar) {
        String name = jar.getFileName().toString();
        if (name.endsWith(".disabled")) {
            name = name.substring(0, name.length() - ".disabled".length());
        }
        return jar.resolveSibling(name + ".panel.json");
    }

    private static void writeSidecar(Path jar, JsonObject meta) {
        try {
            Files.writeString(sidecarOf(jar), meta.toString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOG.warning("Could not record the installed metadata for " + jar, e);
        }
    }

    private static @Nullable JsonObject readSidecar(Path jar) {
        Path file = sidecarOf(jar);
        if (!Files.isRegularFile(file)) {
            return null;
        }
        try {
            JsonElement element = JsonParser.parseString(Files.readString(file,
                    StandardCharsets.UTF_8));
            return element.isJsonObject() ? element.getAsJsonObject() : null;
        } catch (IOException | RuntimeException e) {
            LOG.warning("Could not read the installed metadata " + file, e);
            return null;
        }
    }

    private JsonObject fetchJson(URI uri) throws McException {
        JsonElement element = fetchJsonElement(uri);
        if (!element.isJsonObject()) {
            throw new McException(uri + " did not answer a JSON object");
        }
        return element.getAsJsonObject();
    }

    private JsonElement fetchJsonElement(URI uri) throws McException {
        try {
            HttpRequest request = HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofSeconds(30))
                    .header("User-Agent", "HDSL-web")
                    .GET()
                    .build();
            HttpResponse<String> answer = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (answer.statusCode() != 200) {
                throw new McException(uri + " answered HTTP " + answer.statusCode());
            }
            return JsonParser.parseString(answer.body());
        } catch (IOException e) {
            throw new McException("Failed to read " + uri + ": " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new McException("Reading " + uri + " was interrupted", e);
        } catch (RuntimeException e) {
            throw new McException(uri + " did not answer JSON", e);
        }
    }

    private static @Nullable String string(JsonObject object, String name) {
        return object.has(name) && object.get(name).isJsonPrimitive()
                ? object.get(name).getAsString() : null;
    }
}
