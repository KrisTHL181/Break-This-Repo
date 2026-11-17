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
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jackhuang.hmcl.util.gson.JsonUtils;
import org.jackhuang.hmcl.util.logging.Logger;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;

/// The Minecraft version manifest and `server.jar` download.
///
/// **The jar is fetched at runtime and never bundled** (`docs/minecraft.md`
/// §17.3): it is Mojang's program, the panel only runs it. The download is
/// verified against the SHA-1 the manifest publishes; a mismatch deletes the
/// temporary file and fails, because a truncated jar only shows up later as a
/// mysterious crash.
///
/// Versions are shared: one download serves every instance that wants that
/// version, and the instance's `server.jar` is a hard link (a copy where the
/// filesystem refuses links, e.g. across volumes on Windows).
@NotNullByDefault
public final class McVersions {

    /// The official manifest; `HDSL_MC_MANIFEST` overrides it for mirrors.
    public static final URI DEFAULT_MANIFEST = URI.create("https://launchermeta.mojang.com/mc/game/version_manifest_v2.json");
    /// How long the parsed manifest is reused before it is fetched again.
    private static final Duration MANIFEST_TTL = Duration.ofMinutes(10);
    private static final long MAX_JAR_BYTES = 512L * 1024 * 1024;

    private static final Logger LOG = Logger.LOG;

    /// One entry of the manifest, as the panel shows it.
    ///
    /// @param id   the version id (`1.21.4`)
    /// @param type `release`, `snapshot`, …
    public record VersionEntry(String id, String type) {
    }

    /// The downloaded server program's location and hash.
    ///
    /// @param id    the version id
    /// @param url   where the jar came from
    /// @param sha1  the manifest's SHA-1, lowercase hex
    /// @param size  the manifest's size in bytes, or `-1` when it did not say
    private record ServerDownload(String id, String url, String sha1, long size, int javaMajor) {
    }

    private final URI manifestUri;
    private final Path versionsRoot;
    private final HttpClient client;

    private volatile @Nullable List<VersionEntry> cachedVersions;
    private volatile Instant cachedAt = Instant.EPOCH;

    /// @param versionsRoot the directory shared by every instance
    ///                     (`<data>/minecraft/versions`)
    public McVersions(Path versionsRoot) {
        this(versionsRoot, DEFAULT_MANIFEST);
    }

    /// The same, with an overridable manifest (mirrors, tests).
    ///
    /// @param versionsRoot the shared versions directory
    /// @param manifestUri  the manifest to read
    public McVersions(Path versionsRoot, URI manifestUri) {
        this.versionsRoot = versionsRoot.toAbsolutePath().normalize();
        this.manifestUri = manifestUri;
        this.client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();
    }

    /// Lists the versions the manifest knows, newest first.
    ///
    /// @return the entries
    /// @throws McException when the manifest cannot be read
    public List<VersionEntry> list() throws McException {
        List<VersionEntry> cached = cachedVersions;
        if (cached != null && Instant.now().isBefore(cachedAt.plus(MANIFEST_TTL))) {
            return cached;
        }
        JsonObject root = fetchJson(manifestUri);
        JsonArray versions = root.has("versions") && root.get("versions").isJsonArray()
                ? root.getAsJsonArray("versions") : new JsonArray();
        List<VersionEntry> entries = new ArrayList<>();
        for (var element : versions) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject version = element.getAsJsonObject();
            String id = string(version, "id");
            String type = string(version, "type");
            if (id != null) {
                entries.add(new VersionEntry(id, type == null ? "release" : type));
            }
        }
        cachedVersions = List.copyOf(entries);
        cachedAt = Instant.now();
        return cachedVersions;
    }

    /// Downloads one version's `server.jar` (if this panel does not have it
    /// yet) and puts it beside the instance manifest.
    ///
    /// @param instance the instance that wants it
    /// @param manager  the instance manager (for the target path)
    /// @param versionId the version to install
    /// @return the instance with its version updated
    /// @throws McException when the version is unknown, the download fails, or
    ///                      the hash does not match
    public McInstance install(McInstance instance, McInstanceManager manager, String versionId) throws McException {
        return install(instance, manager, versionId, null);
    }

    /// The same with progress: `progress` is called while the jar downloads,
    /// roughly once per megabyte (and once at the end).
    ///
    /// @param instance  the instance
    /// @param manager   the instance manager
    /// @param versionId the version
    /// @param progress  the sink, or `null`
    /// @return the instance with its version updated
    /// @throws McException when the download fails or the hash does not match
    public McInstance install(McInstance instance, McInstanceManager manager, String versionId,
                              @Nullable Progress progress) throws McException {
        ServerDownload download = resolve(versionId);
        Path shared = versionsRoot.resolve(versionId).resolve(McInstanceManager.SERVER_JAR_NAME);
        if (!reusable(shared, download)) {
            downloadAndVerify(download, shared, progress);
        }
        link(shared, manager.serverJar(instance));
        McInstance updated = instance.withVersion(versionId);
        manager.update(updated);
        return updated;
    }

    /// Download progress in bytes; `total` is `-1` when the manifest did not say.
    @FunctionalInterface
    public interface Progress {
        void update(long copied, long total);

        /// A coarse phase label ("Downloading …", "Installing …").
        ///
        /// The byte counter alone cannot tell a 9 MB installer download from the
        /// installer's own work: the UI said "Installing" for five minutes of a
        /// slow download. Implementations that only care about bytes can ignore
        /// this — it has a default so every existing lambda still compiles.
        default void stage(String stage) {
        }
    }

    private ServerDownload resolve(String versionId) throws McException {
        VersionEntry entry = list().stream()
                .filter(candidate -> candidate.id().equals(versionId))
                .findFirst()
                .orElseThrow(() -> new McException("unknown Minecraft version `" + versionId + "`"));
        // The list caches only id/type; the per-version JSON carries the download.
        JsonObject root = fetchJson(manifestUri);
        JsonArray versions = root.has("versions") && root.get("versions").isJsonArray()
                ? root.getAsJsonArray("versions") : new JsonArray();
        String url = null;
        for (var element : versions) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject version = element.getAsJsonObject();
            if (entry.id().equals(string(version, "id"))) {
                url = string(version, "url");
                break;
            }
        }
        if (url == null) {
            throw new McException("the manifest has no metadata URL for `" + versionId + "`");
        }
        JsonObject meta = fetchJson(URI.create(url));
        JsonObject downloads = meta.has("downloads") && meta.get("downloads").isJsonObject()
                ? meta.getAsJsonObject("downloads") : null;
        JsonObject server = downloads != null && downloads.has("server") && downloads.get("server").isJsonObject()
                ? downloads.getAsJsonObject("server") : null;
        if (server == null) {
            throw new McException("version `" + versionId + "` has no server download (only the client exists?)");
        }
        String jarUrl = string(server, "url");
        String sha1 = string(server, "sha1");
        if (jarUrl == null || sha1 == null || sha1.isBlank()) {
            throw new McException("version `" + versionId + "` publishes an incomplete server download");
        }
        long size = server.has("size") ? server.get("size").getAsLong() : -1;
        // `javaVersion.majorVersion` is what the version needs; 0 when absent.
        int javaMajor = 0;
        if (meta.has("javaVersion") && meta.get("javaVersion").isJsonObject()) {
            JsonObject javaVersion = meta.getAsJsonObject("javaVersion");
            if (javaVersion.has("majorVersion")) {
                javaMajor = javaVersion.get("majorVersion").getAsInt();
            }
        }
        return new ServerDownload(versionId, jarUrl, sha1.toLowerCase(Locale.ROOT), size, javaMajor);
    }

    private void downloadAndVerify(ServerDownload download, Path target, @Nullable Progress progress) throws McException {
        long lastReport = 0;
        try {
            Files.createDirectories(target.getParent());
            Path temp = Files.createTempFile(target.getParent(), "server", ".jar.part");
            try {
                HttpRequest request = HttpRequest.newBuilder(URI.create(download.url()))
                        .timeout(Duration.ofMinutes(10))
                        .GET()
                        .build();
                HttpResponse<InputStream> answer = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
                if (answer.statusCode() != 200) {
                    throw new McException("the server download answered HTTP " + answer.statusCode());
                }
                // Upstream publishes only SHA-1. We verify that (the hash comes
                // from Mojang over HTTPS, not from the mirror) **and** compute a
                // SHA-256 of our own, which is what later reuses are checked
                // against. An operator can pin an even stronger value by
                // dropping `trusted.sha256` next to the version.
                MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
                MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
                long copied = 0;
                try (InputStream in = answer.body();
                     OutputStream out = Files.newOutputStream(temp)) {
                    byte[] buffer = new byte[64 * 1024];
                    int read;
                    while ((read = in.read(buffer)) >= 0) {
                        if (Thread.currentThread().isInterrupted()) {
                            throw new McException("the server download was cancelled");
                        }
                        copied += read;
                        if (copied > MAX_JAR_BYTES) {
                            throw new McException("the server download is larger than the panel accepts");
                        }
                        sha1.update(buffer, 0, read);
                        sha256.update(buffer, 0, read);
                        out.write(buffer, 0, read);
                        if (progress != null && copied - lastReport >= 1024 * 1024) {
                            lastReport = copied;
                            progress.update(copied, download.size());
                        }
                    }
                }
                String actualSha1 = HexFormat.of().formatHex(sha1.digest());
                if (!actualSha1.equalsIgnoreCase(download.sha1())) {
                    throw new McException("the server download failed its SHA-1 check (expected "
                            + download.sha1() + ", got " + actualSha1 + ")");
                }
                if (download.size() >= 0 && copied != download.size()) {
                    throw new McException("the server download is " + copied + " bytes, the manifest says "
                            + download.size());
                }
                String actualSha256 = HexFormat.of().formatHex(sha256.digest());
                requireZip(temp);
                requirePinned(target.getParent().resolve("trusted.sha256"), actualSha256);

                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
                // A trimmed record of where this jar came from and what we saw:
                // the panel never re-publishes it, it only explains what it runs.
                JsonObject record = new JsonObject();
                record.addProperty("id", download.id());
                record.addProperty("url", download.url());
                record.addProperty("sha1", download.sha1());
                record.addProperty("sha256", actualSha256);
                record.addProperty("size", copied);
                record.addProperty("javaMajor", download.javaMajor());
                if (progress != null) {
                    progress.update(copied, download.size());
                }
                JsonUtils.writeToJsonFile(target.getParent().resolve("version.json"), record);
                LOG.info("Downloaded Minecraft server " + download.id() + " (" + copied + " bytes)");
            } finally {
                Files.deleteIfExists(temp);
            }
        } catch (IOException e) {
            throw new McException("Failed to download the server jar: " + e.getMessage(), e);
        } catch (NoSuchAlgorithmException e) {
            throw new McException("SHA-1/SHA-256 is not available in this JVM", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new McException("the server download was interrupted", e);
        }
    }

    /// Whether the shared jar can be reused instead of downloaded again: the
    /// manifest's SHA-1, our recorded SHA-256, the operator's pin, and a zip
    /// header all have to agree.
    private boolean reusable(Path shared, ServerDownload download) {
        if (!Files.isRegularFile(shared)) {
            return false;
        }
        try {
            if (!sha1Of(shared).equalsIgnoreCase(download.sha1())) {
                return false;
            }
            String sha256 = sha256Of(shared);
            JsonObject record = Files.isRegularFile(shared.getParent().resolve("version.json"))
                    ? JsonUtils.fromJsonFile(shared.getParent().resolve("version.json"), JsonObject.class)
                    : null;
            String recorded = record == null ? null : string(record, "sha256");
            if (recorded != null && !recorded.equalsIgnoreCase(sha256)) {
                return false;
            }
            Path pin = shared.getParent().resolve("trusted.sha256");
            if (Files.isRegularFile(pin)) {
                requirePinned(pin, sha256);
            }
            requireZip(shared);
            return true;
        } catch (McException | IOException e) {
            return false;
        }
    }

    /// Refuses a file that is not a zip: a proxy's HTML error page saved under
    /// the right name is the classic way a "downloaded jar" turns out to be junk.
    private static void requireZip(Path file) throws McException {
        try (InputStream in = Files.newInputStream(file)) {
            byte[] magic = in.readNBytes(4);
            if (magic.length < 4 || magic[0] != 'P' || magic[1] != 'K') {
                throw new McException("the downloaded server.jar is not a zip archive");
            }
        } catch (IOException e) {
            throw new McException("Failed to inspect " + file, e);
        }
    }

    /// An operator-placed pin (`<version>/trusted.sha256`, first token on the
    /// first line) wins over everything else: it is the only value a mirror
    /// cannot influence.
    private static void requirePinned(Path pin, String sha256) throws McException {
        if (!Files.isRegularFile(pin)) {
            return;
        }
        try {
            String text = Files.readString(pin).strip();
            String expected = text.split("\\s+")[0].toLowerCase(Locale.ROOT);
            if (!expected.equalsIgnoreCase(sha256)) {
                throw new McException("the server jar does not match trusted.sha256 (expected "
                        + expected + ", got " + sha256 + ")");
            }
        } catch (IOException e) {
            throw new McException("Failed to read " + pin, e);
        }
    }

    /// Points the instance at the shared jar: hard link first, copy when the
    /// filesystem refuses (a different volume, Windows without privileges).
    private static void link(Path shared, Path instanceJar) throws McException {
        try {
            Files.deleteIfExists(instanceJar);
            try {
                Files.createLink(instanceJar, shared);
            } catch (IOException | UnsupportedOperationException e) {
                Files.copy(shared, instanceJar, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new McException("Failed to place server.jar in the instance: " + e.getMessage(), e);
        }
    }

    private JsonObject fetchJson(URI uri) throws McException {
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
            return JsonParser.parseString(answer.body()).getAsJsonObject();
        } catch (IOException e) {
            throw new McException("Failed to read " + uri + ": " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new McException("Reading " + uri + " was interrupted", e);
        } catch (RuntimeException e) {
            throw new McException(uri + " did not answer a JSON object", e);
        }
    }

    private static @Nullable String string(JsonObject object, String name) {
        return object.has(name) && object.get(name).isJsonPrimitive() ? object.get(name).getAsString() : null;
    }

    private static String sha1Of(Path file) throws McException {
        return digestOf(file, "SHA-1");
    }

    private static String sha256Of(Path file) throws McException {
        return digestOf(file, "SHA-256");
    }

    private static String digestOf(Path file, String algorithm) throws McException {
        try (InputStream in = Files.newInputStream(file)) {
            MessageDigest digest = MessageDigest.getInstance(algorithm);
            byte[] buffer = new byte[64 * 1024];
            int read;
            while ((read = in.read(buffer)) >= 0) {
                digest.update(buffer, 0, read);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (IOException e) {
            throw new McException("Failed to hash " + file, e);
        } catch (NoSuchAlgorithmException e) {
            throw new McException(algorithm + " is not available in this JVM", e);
        }
    }
}