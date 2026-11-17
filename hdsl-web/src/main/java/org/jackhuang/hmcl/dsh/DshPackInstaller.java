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

import com.google.gson.JsonObject;
import kala.compress.archivers.tar.TarArchiveEntry;
import kala.compress.archivers.tar.TarArchiveInputStream;
import org.jackhuang.hmcl.Metadata;
import org.jackhuang.hmcl.util.DigestUtils;
import org.jackhuang.hmcl.util.gson.JsonUtils;
import org.jackhuang.hmcl.util.io.HttpClients;
import org.jackhuang.hmcl.util.io.SafePaths;
import org.jackhuang.hmcl.util.platform.Privileges;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import static org.jackhuang.hmcl.util.logging.Logger.LOG;

/// Fetches a pack from the market and puts it where it belongs.
///
/// Three steps, and the specification is explicit that none of them may be skipped:
///
/// 1. **Download to a temporary file**, never straight into a profile. A half-written pack in a
///    profile is a broken installation; a half-written pack in the cache is a file to delete.
/// 2. **Check the length** against the index's `size`. This is the cheap check, and it catches the
///    common failure — a truncated transfer — before the expensive one.
/// 3. **Check the digest** against the index's `sha256`, byte for byte. A pack that fails either
///    check is **discarded**, not kept and reported: what the index described is the only thing worth
///    installing, and a file that is not it has no use.
///
/// The three values are the index's *pointers*, and they are the whole reason the market step and the
/// install step can be spoken of separately: the market's job ends by handing over a verified file.
///
/// ## Untrusted archives
///
/// A pack is somebody else's archive. Everything here treats its member names as hostile until shown
/// otherwise: an entry whose path climbs out of the destination is refused, an entry naming a file the
/// launcher must never accept is refused, and an entry that is a symbolic link is dropped rather than
/// followed. The export side already filters all of this, but a pack from the market was not
/// necessarily made by this launcher — and a previous bug in this very project (`sessions/../planted/x`
/// in a session pack) is why the rule is "never trust a member name".
///
/// The destination is not trusted either, and for a different reason: a pack cannot put a link there,
/// but a `link:` or `file:` plugin leaves one behind, so a directory that is already a link out of the
/// destination is refused rather than written through. See [SafePaths].
@NotNullByDefault
public final class DshPackInstaller {
    /// The container's marker file, at the archive's root.
    private static final String MARKER = "dspack.json";

    /// The container format the marker must name.
    private static final String FORMAT = "dspack";

    /// The container version this launcher writes and reads.
    private static final int CURRENT_CONTAINER = 3;

    /// The container version still accepted, which carries manifest v4.
    private static final int OLD_CONTAINER = 2;

    /// The container the older `.tgz` packs are, which has no marker at all.
    ///
    /// They are a flat gzipped tar with the manifest at the root — the format's first container,
    /// replaced by the `.dspack` one and still installable, so it is named here rather than left as
    /// "not a container".
    private static final int TGZ_CONTAINER = 1;

    /// This launcher's own id in the ecosystem's registry.
    ///
    /// Read from `../specs/launcher-registry.md`, where ids are claimed one per launcher, and used to
    /// look this launcher up in a pack's `launchers` declaration. The registry warns that `hdsl` and
    /// `dshl` differ by one letter and are unrelated projects — `dshl` is the PCL-family launcher,
    /// this is HDSL — so the id is written once, here, and never spelled by hand at a call site.
    public static final String LAUNCHER_ID = "hdsl";

    private DshPackInstaller() {
    }

    /// What an archive turned out to be.
    ///
    /// @param containerVersion the container's own version
    /// @param manifestVersion  the manifest version the container carries
    /// @param manifest         the pack's manifest
    /// @param wholeHome        whether the pack replaces an entire `$DSH_HOME`
    public record Container(int containerVersion, int manifestVersion, JsonObject manifest,
                            boolean wholeHome) {

        /// Returns a string field of the manifest.
        ///
        /// @param key the field
        /// @return the text, or `null`
        public @Nullable String text(String key) {
            return manifest.has(key) && manifest.get(key).isJsonPrimitive()
                    ? manifest.get(key).getAsString() : null;
        }

        /// Returns the harness version the pack asks for.
        ///
        /// A v2 manifest wrote a **range** where a v3 one writes the exact version, so what is
        /// installed has to be the floor of it: a pack that says `^0.1.5` is a pack that was made
        /// against 0.1.5 and later patches of it. A range this cannot read a floor from is returned
        /// as it stands, and the caller decides what to do with it — refusing a version it could not
        /// parse would turn a readable pack into an uninstallable one.
        ///
        /// @return the version, or `null` when the manifest does not say
        public @Nullable String dshVersion() {
            String declared = text("dshVersion");
            if (declared == null || declared.isBlank() || manifestVersion >= 3) {
                return declared;
            }
            String floor = DshVersionManager.floorOf(declared);
            return floor == null ? declared : floor;
        }

        /// Returns what the pack is called.
        ///
        /// @param fallback what to answer when the manifest does not say
        /// @return the name
        public String name(String fallback) {
            String name = text("displayName");
            return name == null || name.isBlank() ? fallback : name;
        }

        /// Returns the profile the pack wants to be installed as.
        ///
        /// @param fallback what to answer when the manifest does not say
        /// @return the profile name
        public String profileName(String fallback) {
            String name = text("profileName");
            return name == null || name.isBlank() ? fallback : name;
        }
    }

    /// Where packs are kept between being downloaded and being installed.
    ///
    /// @return the directory
    public static Path cacheDirectory() {
        return DshPluginCatalog.cacheDirectory().resolve("packs");
    }

    /// Returns the file a pack's archive is kept in.
    ///
    /// Named from the pack's id and version rather than from its display name: the display name is
    /// the author's to change and may hold anything, and this has to be a file name.
    ///
    /// @param entry the pack
    /// @return the path
    public static Path archiveFile(DshPackMarket.Entry entry) {
        String safe = entry.id().replaceAll("[^A-Za-z0-9._-]", "-") + "-"
                + entry.version().replaceAll("[^A-Za-z0-9._-]", "-");
        return cacheDirectory().resolve(safe + ".dspack");
    }

    /// Fetches a pack into the cache and checks it against the index.
    ///
    /// The download goes to a file beside the target and is moved over it only once it has been
    /// verified, so an interrupted transfer cannot leave a file that looks like a fetched pack. The
    /// digest is computed **as the bytes arrive** rather than by reading the file again: the same
    /// work, done once, on the pass that is already touching every byte.
    ///
    /// Redirects are followed, and that is not optional here: the ecosystem's archives are GitHub
    /// release assets, and a release asset is a redirect to a storage host. A client that did not
    /// follow them would download a three-hundred-byte HTML page and then report a digest mismatch.
    ///
    /// @param entry  the pack
    /// @param report receives progress lines, or `null`
    /// @return the verified archive
    /// @throws DshException when the pack cannot be fetched or is not what the index described
    public static Path download(DshPackMarket.Entry entry,
                                @Nullable java.util.function.Consumer<String> report) throws DshException {
        Path target = archiveFile(entry);
        if (isCached(entry)) {
            return target;
        }
        try {
            Files.createDirectories(target.getParent());
        } catch (IOException e) {
            throw new DshException("Could not create " + target.getParent(), e);
        }
        Path staging = target.resolveSibling(target.getFileName() + ".hdsl-downloading");

        try {
            java.net.http.HttpRequest request = java.net.http.HttpRequest
                    .newBuilder(java.net.URI.create(entry.downloadUrl()))
                    .timeout(java.time.Duration.ofMinutes(10))
                    .header("Accept", "application/octet-stream")
                    .GET()
                    .build();
            java.net.http.HttpClient client = java.net.http.HttpClient.newBuilder()
                    .connectTimeout(java.time.Duration.ofSeconds(20))
                    .followRedirects(java.net.http.HttpClient.Redirect.NORMAL)
                    .build();
            try {
                java.net.http.HttpResponse<InputStream> response = client.send(request,
                        java.net.http.HttpResponse.BodyHandlers.ofInputStream());
                if (response.statusCode() < 200 || response.statusCode() >= 300) {
                    throw new DshException("The market's address for this pack answered HTTP "
                            + response.statusCode(), null);
                }
                java.security.MessageDigest digest = DigestUtils.getDigest("SHA-256");
                long written = 0;
                try (InputStream body = response.body();
                     java.io.OutputStream out = Files.newOutputStream(staging,
                             java.nio.file.StandardOpenOption.CREATE,
                             java.nio.file.StandardOpenOption.TRUNCATE_EXISTING)) {
                    byte[] buffer = new byte[64 * 1024];
                    int read;
                    while ((read = body.read(buffer)) >= 0) {
                        out.write(buffer, 0, read);
                        digest.update(buffer, 0, read);
                        written += read;
                        if (report != null && entry.size() > 0 && written % (1024 * 1024) < buffer.length) {
                            report.accept("Downloaded " + (written / 1024) + " KB of "
                                    + (entry.size() / 1024) + " KB");
                        }
                    }
                }
                String actual = hex(digest.digest());
                if (!entry.sha256().equalsIgnoreCase(actual)) {
                    throw new DshException("The pack does not match the market's SHA-256 (expected "
                            + entry.sha256() + ", got " + actual + ")", null);
                }
            } finally {
                HttpClients.closeQuietly(client);
            }
            // Moved into place only now: everything before this line was a file that could be thrown
            // away, and the name the rest of the launcher reads is only ever the verified one.
            Files.move(staging, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            verify(entry, target);
            return target;
        } catch (IOException e) {
            throw new DshException("Could not fetch the pack: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DshException("The download was interrupted", e);
        } finally {
            // A failed download leaves nothing behind, not even the half-written file: the cache is
            // the launcher's own directory, and littering it is how a cache becomes a mess.
            try {
                Files.deleteIfExists(staging);
            } catch (IOException e) {
                LOG.warning("Could not remove the partly downloaded " + staging, e);
            }
        }
    }

    /// Writes a digest as lowercase hex.
    ///
    /// @param bytes the digest
    /// @return the text
    private static String hex(byte[] bytes) {
        StringBuilder text = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            text.append(String.format("%02x", value));
        }
        return text.toString();
    }

    /// Reports whether a pack has already been fetched and verified.
    ///
    /// Checked by digest rather than by presence: a file of the right name that is not the right
    /// bytes is worse than no file, because it would be installed without being fetched again.
    ///
    /// @param entry the pack
    /// @return whether the archive is already here and is the one the index described
    public static boolean isCached(DshPackMarket.Entry entry) {
        Path archive = archiveFile(entry);
        try {
            if (!Files.isRegularFile(archive) || Files.size(archive) != entry.size()) {
                return false;
            }
            return entry.sha256().equalsIgnoreCase(
                    DigestUtils.digestToString("SHA-256", archive));
        } catch (IOException | RuntimeException e) {
            return false;
        }
    }

    /// Checks a downloaded archive against the two pointers the index gave.
    ///
    /// @param entry   the pack
    /// @param archive the file
    /// @throws DshException when it is not what the index described
    public static void verify(DshPackMarket.Entry entry, Path archive) throws DshException {
        long length;
        try {
            length = Files.size(archive);
        } catch (IOException e) {
            throw new DshException("Could not read " + archive, e);
        }
        // The length first: it is one call, and a truncated transfer is the failure that actually
        // happens. Reporting "the digest does not match" for a file that is obviously half there
        // sends somebody looking for a forger when the network dropped.
        if (entry.size() > 0 && length != entry.size()) {
            throw new DshException("The pack is " + length + " bytes and the market says it is "
                    + entry.size() + " bytes, so it did not arrive whole", null);
        }
        String actual;
        try {
            actual = DigestUtils.digestToString("SHA-256", archive);
        } catch (IOException e) {
            throw new DshException("Could not read " + archive, e);
        }
        if (!entry.sha256().equalsIgnoreCase(actual)) {
            throw new DshException("The pack does not match the market's SHA-256 (expected "
                    + entry.sha256() + ", got " + actual + ")", null);
        }
    }

    /// Works out what an archive is.
    ///
    /// The container is recognised by its marker at the archive root, and the container's own version
    /// is what decides which manifest versions are expected — **not** the manifest's own
    /// `manifestVersion` field, which an older writer set inconsistently. The specification's table:
    ///
    /// | archive | marker | manifest | action |
    /// |---|---|---|---|
    /// | any ZIP | none | — | not a pack (a plain ZIP, or a broken file) |
    /// | `.dspack` | `version` 3 | 5 | current |
    /// | `.dspack` | `version` 2 | 4 | still read |
    /// | `.tgz` | none at all | 2 or 3 | still read — the format's first container |
    /// | `.tgz` | — | 1 | refused, and for a reason of its own |
    /// | anything else | — | — | refused, by version |
    ///
    /// The `.tgz` case is not a marker this launcher looks for and failed to find: gzip's own magic
    /// bytes say what the file is, and the format's first container had no marker to carry. A pack
    /// older than the marker therefore still installs, which is what "backwards compatible" means
    /// for a reader.
    ///
    /// @param archive the file
    /// @return what it is
    /// @throws DshException when it is not a pack this launcher can install
    public static Container identify(Path archive) throws DshException {
        if (isTarGz(archive)) {
            return identifyTarGz(archive);
        }
        JsonObject marker;
        JsonObject manifest;
        int containerVersion;
        try (ZipFile zip = new ZipFile(archive.toFile(), StandardCharsets.UTF_8)) {
            marker = readJson(zip, MARKER);
            if (marker == null) {
                // Not a refusal of the *format*: a ZIP with no marker is simply not a pack, and the
                // person may have picked the wrong file.
                throw new DshException("That archive is not a DeepSeek Harness pack: it has no "
                        + MARKER + " at its root", null);
            }
            if (!FORMAT.equals(text(marker, "format"))) {
                throw new DshException("That archive says it is a "
                        + text(marker, "format") + " archive, not a pack", null);
            }
            Integer version = number(marker, "version");
            if (version == null || (version != CURRENT_CONTAINER && version != OLD_CONTAINER)) {
                throw new DshException("That pack is container version " + version
                        + ", and this launcher reads versions " + OLD_CONTAINER + " and "
                        + CURRENT_CONTAINER, null);
            }
            containerVersion = version;
            manifest = readJson(zip, "manifest.json");
            if (manifest == null) {
                throw new DshException("That pack has no manifest.json", null);
            }
        } catch (IOException e) {
            throw new DshException("Could not read " + archive + ": " + e.getMessage(), e);
        }

        Integer manifestVersion = number(manifest, "manifestVersion");
        if (manifestVersion == null) {
            manifestVersion = 5;
        }
        refuseVersionOne(manifestVersion);
        // The two versions have to be a pair the specification names. A container 3 holding a
        // manifest 4 is a file assembled wrongly — the writer took one half from each format — and
        // installing it would mean guessing which half to believe.
        int expectedContainer = containerFor(manifestVersion);
        if (expectedContainer != containerVersion) {
            throw new DshException("That pack's container is version " + containerVersion
                    + " and its manifest is version " + manifestVersion + ", which do not go together",
                    null);
        }
        // `collection` is reserved by the specification and refused by its validator; the same is
        // done here rather than pretending to understand it.
        String type = text(manifest, "type");
        if ("collection".equalsIgnoreCase(type)) {
            throw new DshException("That pack is a collection, which this launcher cannot install", null);
        }
        return new Container(containerVersion, manifestVersion, manifest,
                "dshhome".equalsIgnoreCase(type));
    }

    /// Works out what a `.tgz` pack is.
    ///
    /// A flat gzipped tar with `manifest.json` at its root, carrying manifest v2 or v3 and no
    /// `type` — the format's first container, before the `.dspack` one. The version is read from
    /// `manifestVersion`; a manifest that does not write one is judged by its shape, which is what
    /// the writers of that era needed, since some of them left it out.
    ///
    /// @param archive the file
    /// @return what it is
    /// @throws DshException when it is not a pack this launcher can install
    private static Container identifyTarGz(Path archive) throws DshException {
        JsonObject manifest = readRootJsonOfTarGz(archive, "manifest.json");
        if (manifest == null) {
            throw new DshException("That pack has no manifest.json", null);
        }
        Integer manifestVersion = number(manifest, "manifestVersion");
        if (manifestVersion == null) {
            // The earliest writers did not always write the field, and the way it was read then was
            // by shape: a `plugins` list with no `bundles` beside it is the v1 contract.
            manifestVersion = manifest.has("plugins") && !manifest.has("bundles") ? 1 : 2;
        }
        refuseVersionOne(manifestVersion);
        if (manifestVersion != 2 && manifestVersion != 3) {
            throw new DshException("That pack's manifest is version " + manifestVersion
                    + ", and a .tgz pack carries manifest version 2 or 3", null);
        }
        return new Container(TGZ_CONTAINER, manifestVersion, manifest, false);
    }

    /// Refuses the one manifest version that cannot be installed at all.
    ///
    /// v1 flattened the layer stack and the dependencies into one `plugins[]` list, so nothing can
    /// tell which of those plugins the harness would load and which it would merely install — the
    /// profile it describes cannot be rebuilt, and the refusal says that rather than "unsupported".
    ///
    /// @param manifestVersion the manifest's version
    /// @throws DshException when it is version 1
    private static void refuseVersionOne(int manifestVersion) throws DshException {
        if (manifestVersion == 1) {
            throw new DshException("That pack's manifest is version 1, which lists its plugins"
                    + " flattened into one list with no layer stack, so nothing can say which of them"
                    + " the harness would load. That is why the format replaced it; the pack has to be"
                    + " exported again with a current tool, which writes manifest version 5.", null);
        }
    }

    /// Reports whether a file is a gzipped tar.
    ///
    /// Checked by content rather than by the name it was saved under: an upload's file name is what
    /// somebody typed, and the two older containers differ in their own bytes.
    ///
    /// @param archive the file
    /// @return whether it begins with gzip's magic bytes
    private static boolean isTarGz(Path archive) {
        try (InputStream input = Files.newInputStream(archive)) {
            byte[] magic = input.readNBytes(2);
            return magic.length == 2 && (magic[0] & 0xff) == 0x1f && (magic[1] & 0xff) == 0x8b;
        } catch (IOException e) {
            return false;
        }
    }

    /// Returns the container version that carries a manifest version.
    ///
    /// The specification's pair table. An unknown manifest version has no container, which is what
    /// makes a mismatch detectable rather than merely unlikely.
    ///
    /// @param manifestVersion the manifest's version
    /// @return the container version, or `-1` when no container carries that manifest
    private static int containerFor(int manifestVersion) {
        return switch (manifestVersion) {
            case 2, 3 -> TGZ_CONTAINER;
            case 4 -> OLD_CONTAINER;
            case 5 -> CURRENT_CONTAINER;
            default -> -1;
        };
    }

    /// Reads a JSON file out of an archive.
    ///
    /// @param zip the archive
    /// @param name the member's name
    /// @return the object, or `null` when it is not there or is not an object
    private static @Nullable JsonObject readJson(ZipFile zip, String name) {
        ZipEntry entry = zip.getEntry(name);
        if (entry == null) {
            return null;
        }
        try (InputStream stream = zip.getInputStream(entry)) {
            JsonObject object = JsonUtils.fromJson(new String(stream.readAllBytes(),
                    StandardCharsets.UTF_8), JsonObject.class);
            return object;
        } catch (IOException | RuntimeException e) {
            return null;
        }
    }

    /// Reads a number out of an object.
    ///
    /// @param object the object
    /// @param key    the field
    /// @return the number, or `null`
    private static @Nullable Integer number(JsonObject object, String key) {
        return object.has(key) && object.get(key).isJsonPrimitive()
                && object.get(key).getAsJsonPrimitive().isNumber()
                ? object.get(key).getAsInt() : null;
    }

    /// Reads a string out of an object.
    ///
    /// @param object the object
    /// @param key    the field
    /// @return the text, or `null`
    private static @Nullable String text(JsonObject object, String key) {
        return object.has(key) && object.get(key).isJsonPrimitive()
                ? object.get(key).getAsString() : null;
    }

    // ------------------------------------------------------- what the pack asks --

    /// What a pack declares about one launcher.
    ///
    /// The three fields are the whole of the format's `launchers` object, and the format's own
    /// normalisation is applied before anything is read: a list of ids, a `true`, a version and a
    /// full object all become this.
    ///
    /// @param id         the launcher's id, normalised
    /// @param supported  whether the pack says it supports that launcher
    /// @param minVersion the lowest launcher version it supports, or `null`
    /// @param reason     what the author wanted shown, or `null`
    public record LauncherClaim(String id, boolean supported, @Nullable String minVersion,
                                @Nullable String reason) {
    }

    /// Reads a pack's launcher declaration in its normalised form.
    ///
    /// Both spellings the format allows are accepted, because the format says they are the same
    /// thing written two ways: an array of ids is a map of `supported: true`, a string value is that
    /// plus a minimum version, a `false` is a negative declaration. Ids are normalised by case and
    /// surrounding space, which is the whole of what "normalise" can honestly mean here — the format
    /// says entries are kebab-case ids, and it also warns that two of them differ by one letter, so
    /// guessing at a misspelling would be how `dshl` gets taken for `hdsl`.
    ///
    /// @param manifest the manifest
    /// @return the claims, in the order written; empty when the pack declares nothing
    public static List<LauncherClaim> launcherClaims(JsonObject manifest) {
        if (!manifest.has("launchers") || manifest.get("launchers").isJsonNull()) {
            return List.of();
        }
        List<LauncherClaim> claims = new ArrayList<>();
        com.google.gson.JsonElement declared = manifest.get("launchers");
        if (declared.isJsonArray()) {
            for (com.google.gson.JsonElement element : declared.getAsJsonArray()) {
                if (element.isJsonPrimitive()) {
                    String id = normalizeLauncherId(element.getAsString());
                    if (id != null) {
                        claims.add(new LauncherClaim(id, true, null, null));
                    }
                }
            }
            return List.copyOf(claims);
        }
        if (!declared.isJsonObject()) {
            return List.of();
        }
        for (Map.Entry<String, com.google.gson.JsonElement> entry
                : declared.getAsJsonObject().entrySet()) {
            String id = normalizeLauncherId(entry.getKey());
            if (id == null) {
                continue;
            }
            com.google.gson.JsonElement value = entry.getValue();
            if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isBoolean()) {
                claims.add(new LauncherClaim(id, value.getAsBoolean(), null, null));
            } else if (value.isJsonPrimitive()) {
                String version = value.getAsString().trim();
                claims.add(new LauncherClaim(id, true, version.isEmpty() ? null : version, null));
            } else if (value.isJsonObject()) {
                JsonObject object = value.getAsJsonObject();
                claims.add(new LauncherClaim(id,
                        !object.has("supported")
                                || !object.get("supported").isJsonPrimitive()
                                || object.get("supported").getAsBoolean(),
                        text(object, "minVersion"),
                        text(object, "reason")));
            }
        }
        return List.copyOf(claims);
    }

    /// Normalises a launcher id.
    ///
    /// @param id the id as the manifest spelled it
    /// @return the id, or `null` when there is nothing usable in it
    static @Nullable String normalizeLauncherId(@Nullable String id) {
        if (id == null) {
            return null;
        }
        String cleaned = id.trim().toLowerCase(Locale.ROOT);
        return cleaned.isEmpty() ? null : cleaned;
    }

    /// Reads the pack's `dshVersions` set.
    ///
    /// An enumeration of what the author measured, not a range, and read as one: entries are trimmed,
    /// blank ones dropped and duplicates collapsed, with the order the author wrote kept.
    ///
    /// @param manifest the manifest
    /// @return the versions, in the order written
    public static List<String> measuredVersions(JsonObject manifest) {
        List<String> versions = new ArrayList<>();
        com.google.gson.JsonElement declared = manifest.get("dshVersions");
        if (declared != null && declared.isJsonArray()) {
            for (com.google.gson.JsonElement element : declared.getAsJsonArray()) {
                if (element.isJsonPrimitive()) {
                    String version = element.getAsString().trim();
                    if (!version.isEmpty() && !versions.contains(version)) {
                        versions.add(version);
                    }
                }
            }
        }
        return List.copyOf(versions);
    }

    /// Returns the harness versions this machine has installed, newest first.
    ///
    /// Asked of the instances rather than of a directory, because that is what this launcher keeps:
    /// each instance carries its own copy of the harness, and a version is installed when some
    /// instance has it and every one of its files is there.
    ///
    /// @return the versions, newest first
    public static List<String> installedVersions() {
        Set<String> versions = new java.util.LinkedHashSet<>();
        for (DshInstance instance : DshInstanceManager.list()) {
            if (!versions.contains(instance.version()) && DshVersionManager.isInstalled(instance)) {
                versions.add(instance.version());
            }
        }
        List<String> sorted = new ArrayList<>(versions);
        sorted.sort((left, right) -> DshVersionManager.compareVersions(right, left));
        return List.copyOf(sorted);
    }

    /// Returns what a pack's launcher declaration has to say, read out of the archive itself.
    ///
    /// @param archive the pack
    /// @return the lines to report; empty when the archive cannot be read or declares nothing
    static List<String> launcherWarningsOf(Path archive) {
        try {
            return launcherWarnings(identify(archive).manifest());
        } catch (DshException | RuntimeException e) {
            // A pack this launcher cannot read is refused by the caller with the reason it gave, and
            // there is nothing this can add to that.
            return List.of();
        }
    }

    /// Decides which harness version a pack should be installed with.
    ///
    /// The specification's rule, and it has a unique answer for every pack:
    ///
    /// | the pack says | the machine has | answer |
    /// |---|---|---|
    /// | `dshVersions` meeting what is installed | | the pinned `dshVersion` when it is among them, otherwise the newest of the meeting set |
    /// | a set meeting nothing | | the pinned `dshVersion`, to be installed |
    /// | `dshVersions` and no `dshVersion` | anything | the first version it lists, which the author wrote first |
    /// | nothing | versions | the newest installed |
    /// | nothing | nothing | nothing — the caller has no version to work from |
    ///
    /// **A pinned version the machine does not have is never a failure.** That is the point of the
    /// set: it says which versions the pack was measured against, and one of them being here is
    /// enough. Only a pack whose set meets nothing at all asks for an install.
    ///
    /// @param manifest  the manifest
    /// @param installed what the machine has installed, newest first
    /// @return the version to use, or `null` when nothing can be decided, and what to warn about
    public static Decision decide(JsonObject manifest, Collection<String> installed) {
        List<String> measured = measuredVersions(manifest);
        String pinned = text(manifest, "dshVersion");
        String version = null;

        List<String> meeting = new ArrayList<>();
        for (String candidate : measured) {
            for (String have : installed) {
                if (candidate.equals(have) || DshVersionManager.compareVersions(candidate, have) == 0) {
                    meeting.add(candidate);
                    break;
                }
            }
        }
        if (!meeting.isEmpty()) {
            version = meeting.contains(pinned) ? pinned : newest(meeting);
        } else if (pinned != null && !pinned.isBlank()) {
            version = pinned;
        } else if (!measured.isEmpty()) {
            version = measured.get(0);
        } else if (!installed.isEmpty()) {
            version = newest(installed);
        }
        return new Decision(version, launcherWarnings(manifest));
    }

    /// Decides with this machine's own installed versions.
    ///
    /// @param manifest the manifest
    /// @return the decision
    public static Decision decide(JsonObject manifest) {
        return decide(manifest, installedVersions());
    }

    /// Returns the newest of a set of versions.
    ///
    /// @param versions the versions
    /// @return the newest
    private static String newest(Collection<String> versions) {
        String newest = null;
        for (String version : versions) {
            if (newest == null || DshVersionManager.compareVersions(version, newest) > 0) {
                newest = version;
            }
        }
        return newest;
    }

    /// What to install a pack with, and what to say about it first.
    ///
    /// @param version  the harness version to use, or `null` when nothing can be decided
    /// @param warnings what the pack's launcher declaration has to say, in order
    public record Decision(@Nullable String version, List<String> warnings) {
    }

    /// Returns what a pack's launcher declaration has to say to this launcher.
    ///
    /// The specification's table, whose one fixed point is that **nothing here refuses an install**:
    /// a pack that declares it does not support this launcher still installs, and what comes back is
    /// what to tell the person while it does. The five cases are the five the table names, and the
    /// two that say nothing say nothing rather than a reassuring line.
    ///
    /// @param manifest        the manifest
    /// @param launcherId      which launcher to look up
    /// @param launcherVersion this launcher's own version, or `null` when it cannot report one
    /// @return the lines to report, in order
    public static List<String> launcherWarnings(JsonObject manifest, String launcherId,
                                                @Nullable String launcherVersion) {
        String id = normalizeLauncherId(launcherId);
        List<LauncherClaim> claims = launcherClaims(manifest);
        if (id == null || claims.isEmpty()) {
            return List.of();
        }
        List<String> lines = new ArrayList<>();
        LauncherClaim mine = null;
        for (LauncherClaim claim : claims) {
            if (claim.id().equals(id)) {
                mine = claim;
            }
        }
        if (mine == null) {
            // Not listed. A declaration that supports somebody is a whitelist and this is worth
            // saying; one that only turns launchers off is a blacklist, and this is on nobody's bad
            // side of it, so it says nothing.
            List<String> supported = new ArrayList<>();
            for (LauncherClaim claim : claims) {
                if (claim.supported()) {
                    supported.add(claim.id());
                }
            }
            if (!supported.isEmpty()) {
                lines.add("The pack declares support for " + String.join(", ", supported)
                        + " and does not name " + id + ". Installing anyway.");
            }
            return List.copyOf(lines);
        }
        if (!mine.supported()) {
            lines.add("The pack says it does not support " + id
                    + (mine.reason() == null || mine.reason().isBlank() ? "" : ": " + mine.reason())
                    + ". Installing anyway.");
            return List.copyOf(lines);
        }
        if (mine.minVersion() != null && !mine.minVersion().isBlank()) {
            if (launcherVersion == null) {
                lines.add("The pack asks for " + id + " " + mine.minVersion()
                        + " or newer, and this launcher cannot report its own version."
                        + " Installing anyway.");
            } else if (compareLauncherVersions(launcherVersion, mine.minVersion()) < 0) {
                lines.add("The pack asks for " + id + " " + mine.minVersion() + " or newer, and this"
                        + " launcher is " + launcherVersion + ". Installing anyway.");
            }
        }
        return List.copyOf(lines);
    }

    /// Returns what a pack's launcher declaration has to say to **this** launcher.
    ///
    /// @param manifest the manifest
    /// @return the lines to report, in order
    public static List<String> launcherWarnings(JsonObject manifest) {
        return launcherWarnings(manifest, LAUNCHER_ID, Metadata.VERSION);
    }

    /// Compares two launcher versions, the way the registry says to.
    ///
    /// Split on `.`, compared segment by segment **numerically**, with a missing segment read as
    /// zero — so `1.2` is `1.2.0`. There is no prerelease semantics: the registry reserves that for
    /// harness versions, and a launcher reports a plain number like `0.1.1.2`.
    ///
    /// @param left  a version
    /// @param right a version
    /// @return negative when the left is older, zero when they are the same, positive when newer
    static int compareLauncherVersions(String left, String right) {
        String[] leftParts = left.trim().split("\\.", -1);
        String[] rightParts = right.trim().split("\\.", -1);
        int segments = Math.max(leftParts.length, rightParts.length);
        for (int i = 0; i < segments; i++) {
            long a = leadingNumber(i < leftParts.length ? leftParts[i] : "0");
            long b = leadingNumber(i < rightParts.length ? rightParts[i] : "0");
            if (a != b) {
                return Long.compare(a, b);
            }
        }
        return 0;
    }

    /// Reads the number a version segment begins with.
    ///
    /// A segment a launcher version should not contain — anything but digits — counts as zero rather
    /// than as a reason to refuse the comparison: the string comes from another program, and what
    /// this decides is a warning, never a refusal.
    ///
    /// @param segment the segment
    /// @return its leading digits, or zero
    private static long leadingNumber(String segment) {
        int end = 0;
        while (end < segment.length() && Character.isDigit(segment.charAt(end))) {
            end++;
        }
        if (end == 0) {
            return 0;
        }
        try {
            return Long.parseLong(segment.substring(0, end));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /// What landing a pack's files came to.
    ///
    /// @param files      how many files were written
    /// How long the installed pack is given to come up, and to go down again, before the check
    /// gives up on it. An instance answers in seconds; one that answers by hanging is not one that
    /// would have worked for a person either.
    private static final java.time.Duration VERIFY_TIMEOUT = java.time.Duration.ofSeconds(90);

    /// How many lines of a failed composition are shown. Enough for the reason and the package or
    /// route it names, and not the plugin tree above it.
    private static final int FAILURE_LINES = 12;

    /// @param overrides  how many came from `overrides/`
    /// @param home       how many came from `home/`
    /// @param machine    how many were the archive's own machine files
    public record Landed(int files, int overrides, int home, int machine) {
    }

    /// Writes a pack's files into a destination.
    ///
    /// Two places, and which is which is the container's own layout:
    ///
    /// - `overrides/` goes to the **destination root**, which for a `profile` pack is the profile's
    ///   directory and for a `dshhome` pack is the whole home.
    /// - `home/` goes to the home, and is only meaningful for a `profile` pack — a `dshhome` pack
    ///   already covers the home with its overrides.
    ///
    /// The machine files at the archive's root (`package.json`, the lock file, the workspace file)
    /// are copied to the destination root as well. They are *not* part of `overrides/`, because they
    /// describe the pack rather than overriding anything — but a profile without them is not the
    /// profile the pack describes.
    ///
    /// The order is the specification's, and it matters: overrides are landed so that the user's own
    /// files — `cordis.patch.yml` among them — end up on top of whatever a dependency would install.
    ///
    /// @param archive     the pack
    /// @param destination where `overrides/` lands
    /// @param home        where `home/` lands, or `null` for a pack that has none
    /// @return what was written
    /// @throws DshException when the archive cannot be read, or holds a member it may not write
    public static Landed land(Path archive, Path destination, @Nullable Path home) throws DshException {
        return land(archive, destination, home, java.util.EnumSet.allOf(Part.class));
    }

    /// Which of a pack's three sets of files to write.
    ///
    /// They are separable because the specification's order interleaves them with work that is not
    /// file copying: the pack's dependencies are installed **between** its machine files and its
    /// overrides, so that a `cordis.patch.yml` the pack carries lands on top of whatever the
    /// dependency install wrote. Copying all three at once cannot express that order.
    public enum Part {
        /// The archive root's `package.json`, lock file and workspace file.
        MACHINE,
        /// `overrides/`, which lands at the destination root.
        OVERRIDES,
        /// `home/`, which lands at the `DSH_HOME` root.
        HOME
    }

    /// Writes the parts of a pack that were asked for.
    ///
    /// @param archive     the pack
    /// @param destination where `overrides/` lands
    /// @param home        where `home/` lands, or `null` for a pack that has none
    /// @param parts       which sets to write
    /// @return what was written
    /// @throws DshException when the archive cannot be read, or holds a member it may not write
    public static Landed land(Path archive, Path destination, @Nullable Path home,
                              java.util.Set<Part> parts) throws DshException {
        if (isTarGz(archive)) {
            return landTarGz(archive, destination, parts);
        }
        int files = 0;
        int overrides = 0;
        int fromHome = 0;
        int machine = 0;
        try (ZipInputStream zip = new ZipInputStream(Files.newInputStream(archive),
                StandardCharsets.UTF_8)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                String name = entry.getName();
                if (entry.isDirectory()) {
                    continue;
                }
                // Symbolic links need no special handling here, and that is worth writing down
                // because it looks like an omission. A ZIP stores a link as an entry whose *content*
                // is the target's path and whose Unix mode marks it as a link; `java.util.zip` does
                // not expose the mode, so this cannot tell one from a small text file — and it does
                // not have to. Nothing here calls `Files.createSymbolicLink`, so such an entry is
                // written out as an ordinary file holding the text of a path, and the link is never
                // created.
                //
                // That is a statement about the entries of *this* archive, and no more. It does not
                // mean a member can never be written through a link: a link that was already in the
                // destination — what a `link:` or `file:` plugin leaves behind — is found rather
                // than made, and it is [#resolve]'s second question (see [#SafePaths]) that refuses
                // the member, not the fact that the archive cannot carry one.

                Path root;
                Path target;
                if (name.startsWith("overrides/")) {
                    if (!parts.contains(Part.OVERRIDES)) {
                        continue;
                    }
                    root = destination;
                    target = resolve(root, name.substring("overrides/".length()));
                    overrides++;
                } else if (name.startsWith("home/") && home != null) {
                    if (!parts.contains(Part.HOME)) {
                        continue;
                    }
                    root = home;
                    target = resolve(root, name.substring("home/".length()));
                    fromHome++;
                } else if (isMachineFile(name)) {
                    if (!parts.contains(Part.MACHINE)) {
                        continue;
                    }
                    root = destination;
                    target = resolve(root, name);
                    machine++;
                } else {
                    // `manifest.json`, `dspack.json`, and anything else the pack carries for its own
                    // sake are not part of the installation.
                    continue;
                }

                refuseIfUnsafe(name, target);
                SafePaths.createDirectories(root, target.getParent());
                Files.copy(zip, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                // The extraction runs as the panel; the harness runs as the agent and rewrites
                // `cordis.yml` beside the patch this pack just landed. See `Privileges#handWrittenFile`.
                Privileges.handWrittenFile(target, root);
                files++;
            }
        } catch (IOException e) {
            throw new DshException("Could not unpack " + archive + ": " + e.getMessage(), e);
        }
        return new Landed(files, overrides, fromHome, machine);
    }

    /// Writes a `.tgz` pack's files into a destination.
    ///
    /// The flat layout of the first container: every member but the manifest is a file of the
    /// profile, so each lands at the destination root. The distinction the `.dspack` container draws
    /// with `overrides/` does not exist there, and the one it draws with machine files does — a
    /// `package.json` at the root is still the pack's own, and still has to be written before the
    /// dependency install reads it. Nothing here is a `home/`: that level was added with the
    /// `.dspack` container, and in a flat pack a directory called `home` is a directory the author
    /// put in their profile.
    ///
    /// Links are dropped rather than written, which the reader makes possible here: a tar records
    /// what an entry is, and the one thing a pack must never do is create a link into a machine.
    ///
    /// @param archive     the pack
    /// @param destination where the files land
    /// @param parts       which sets to write
    /// @return what was written
    /// @throws DshException when the archive cannot be read, or holds a member it may not write
    private static Landed landTarGz(Path archive, Path destination, java.util.Set<Part> parts)
            throws DshException {
        int overrides = 0;
        int machine = 0;
        try (TarArchiveInputStream tar = openTarGz(archive)) {
            TarArchiveEntry entry;
            while ((entry = tar.getNextEntry()) != null) {
                String member = normalizeMember(entry.getName());
                if (member.isEmpty() || entry.isDirectory() || entry.isSymbolicLink()
                        || entry.isLink()) {
                    continue;
                }
                // The archive's own manifest describes the pack; installing it would put a file in
                // the profile that no profile has.
                if (member.equals("manifest.json")) {
                    continue;
                }
                boolean isMachine = isMachineFile(member);
                if (!parts.contains(isMachine ? Part.MACHINE : Part.OVERRIDES)) {
                    continue;
                }
                Path target = resolve(destination, member);
                refuseIfUnsafe(member, target);
                SafePaths.createDirectories(destination, target.getParent());
                Files.copy(tar, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                Privileges.handWrittenFile(target, destination);
                if (isMachine) {
                    machine++;
                } else {
                    overrides++;
                }
            }
        } catch (IOException e) {
            throw new DshException("Could not unpack " + archive + ": " + e.getMessage(), e);
        }
        return new Landed(overrides + machine, overrides, 0, machine);
    }

    /// Returns where a member lands, refusing anything that would leave the destination.
    ///
    /// The check is [#SafePaths#resolveInside]'s, and it asks two questions because one of them is
    /// not enough. Normalising the name and comparing it with the destination is what catches
    /// `a/../../b` and `a/./../../b` — the same escape written two ways, both leaving the
    /// destination — but it is made of strings, and a destination that already holds a symlink
    /// directory beginning with it passes it while the write lands wherever the link points. The
    /// second question is asked of the filesystem instead: the deepest component of the resolved
    /// path that exists has to resolve, links and all, to somewhere inside the destination.
    ///
    /// @param root the directory the member must stay inside
    /// @param name the member's name, already stripped of its prefix
    /// @return where it lands
    /// @throws DshException when it would land outside
    private static Path resolve(Path root, String name) throws DshException {
        Path target = SafePaths.resolveInside(root, name);
        if (target == null) {
            throw new DshException("That pack tries to write outside the folder it is installing "
                    + "into (" + name + ")", null);
        }
        return target;
    }

    /// Refuses a member that names something a pack must never write.
    ///
    /// The credential rules are [DshPackForge]'s rather than a second list kept here, and the reason
    /// is the direction they guard. This side guards the person's machine from being written to and
    /// the export side guards the author's secrets from being published, so neither can lean on the
    /// other having run — but that makes one list *more* important, not less. As two hand-written
    /// lists they had drifted: this side refused `.netrc`, `.pypirc`, `.yarnrc` and `.yarnrc.yml`
    /// while the export side published them, and a key named `id_ed25519` escaped both. What the
    /// export side knows and this does not — `*token*`, `*api[_-]?key*`, `*.crt` — is an addition on
    /// its side only, so what is read here is always a subset of what it refuses.
    ///
    /// @param name   the member's name
    /// @param target where it would land
    /// @throws DshException when it may not be written
    private static void refuseIfUnsafe(String name, Path target) throws DshException {
        String file = target.getFileName().toString();
        String lower = file.toLowerCase(java.util.Locale.ROOT);
        for (String extension : DshPackForge.CREDENTIAL_EXTENSIONS) {
            if (lower.endsWith(extension)) {
                throw new DshException("That pack tries to write " + file
                        + ", which is a key file and is never installed from a pack", null);
            }
        }
        if (DshPackForge.isCredentialName(file)) {
            throw new DshException("That pack tries to write " + file
                    + ", which holds credentials and is never installed from a pack", null);
        }
        // A nested archive is refused too: it is the one member whose contents cannot be checked
        // before they are on the disk, and nothing a pack needs is stored as one.
        for (String extension : List.of(".zip", ".tar", ".tgz", ".gz", ".bz2", ".xz", ".7z", ".rar")) {
            if (lower.endsWith(extension) && !name.startsWith("overrides/plugins/")) {
                throw new DshException("That pack holds a nested archive (" + name
                        + "), which is never installed from a pack", null);
            }
        }
    }

    // ------------------------------------------------ the older .tgz container --

    /// Reads one file out of a `.tgz` pack's root.
    ///
    /// The archive is streamed rather than held: a pack may be tens of megabytes, and the one file
    /// wanted is at its root. The reader is the same one the catalogue and the file trees use, so
    /// the pack format's older container is read by code that has been reading tars all along.
    ///
    /// @param archive the pack
    /// @param name    the member's name at the root
    /// @return the object, or `null` when it is not there or is not an object
    private static @Nullable JsonObject readRootJsonOfTarGz(Path archive, String name) {
        try (TarArchiveInputStream tar = openTarGz(archive)) {
            TarArchiveEntry entry;
            while ((entry = tar.getNextEntry()) != null) {
                if (!entry.isDirectory() && normalizeMember(entry.getName()).equals(name)) {
                    return JsonUtils.fromJson(new String(tar.readAllBytes(), StandardCharsets.UTF_8),
                            JsonObject.class);
                }
            }
        } catch (IOException | RuntimeException e) {
            return null;
        }
        return null;
    }

    /// Opens a `.tgz` pack for reading.
    ///
    /// @param archive the pack
    /// @return the reader, which reads one entry at a time
    /// @throws IOException when the file cannot be read
    private static TarArchiveInputStream openTarGz(Path archive) throws IOException {
        return new TarArchiveInputStream(new GZIPInputStream(Files.newInputStream(archive)));
    }

    /// Strips the spelling a tar writer may put in front of a name.
    ///
    /// The format's first writer recorded paths as `./x`, which is the same member as `x`, and a
    /// name that is compared literally would miss it.
    ///
    /// @param name the member's name
    /// @return the path, with `/` separators and no leading `./`
    private static String normalizeMember(String name) {
        String cleaned = name.replace('\\', '/');
        while (cleaned.startsWith("./")) {
            cleaned = cleaned.substring(2);
        }
        return cleaned.replaceAll("^/+", "");
    }

    /// Finishes an install that a build-script question stopped, with the same promise as [#installNew].
    ///
    /// [#installNew] keeps the instance when pnpm asks for permission to run a package's install
    /// scripts: the question is written into that instance's profile, and the answer has to be
    /// written to the same one. This is the other half — the pack is put into that instance once the
    /// answer is in — and a failure that is not another question takes the half-made instance away,
    /// exactly as the first attempt would have.
    ///
    /// @param archive  the pack
    /// @param instance the instance the question was about
    /// @param report   receives progress lines, or `null`
    /// @return the instance, filled in
    /// @throws DshException when the pack cannot be installed
    public static DshInstance finish(Path archive, DshInstance instance,
                                     @Nullable java.util.function.Consumer<String> report)
            throws DshException {
        try {
            installInto(archive, instance, report);
            return instance;
        } catch (DshException | RuntimeException failed) {
            if (failed instanceof DshPluginInstaller.DshBuildScriptApprovalRequired) {
                // Another package is waiting; the instance stays for the same reason it stayed the
                // first time, and the next answer finishes it.
                throw failed;
            }
            org.jackhuang.hmcl.util.logging.Logger.LOG.warning(
                    "Could not finish the pack in " + instance.id() + "; removing it", failed);
            DshVersionManager.discardPartial(instance);
            try {
                DshInstanceManager.delete(instance.id());
            } catch (DshException | RuntimeException cleanupFailure) {
                org.jackhuang.hmcl.util.logging.Logger.LOG.warning(
                        "Could not remove the half-made instance " + instance.id(), cleanupFailure);
            }
            throw failed;
        }
    }

    /// Reports whether a name at the archive's root is one of the pack's machine files.
    ///
    /// The exporter writes by the same rule, through this same method: a file that describes the
    /// profile belongs at the archive root and is copied to the destination **before** the
    /// dependencies are resolved, and everything else belongs under `overrides/` and lands after.
    /// A pack that put its `package.json` under `overrides/` installs and then boots with none of
    /// its plugins, because the resolve ran while the profile was still empty.
    ///
    /// @param name the member's name
    /// @return whether it is copied to the destination
    static boolean isMachineFile(String name) {
        return name.equals("package.json") || name.equals("pnpm-lock.yaml")
                || name.equals("pnpm-workspace.yaml");
    }

    /// Puts a recognised pack into an instance that is already at the right version.
    ///
    /// **The order is the specification's, and each step is in it for a reason** (§3.2.2):
    ///
    /// 1. the pack's own machine files — without them there is no manifest to install from;
    /// 2. the dependency install, which is what turns a list of package names into a profile that
    ///    boots;
    /// 3. `overrides/` and `home/`, **last**, so a `cordis.patch.yml` the pack carries lands on top of
    ///    whatever the dependency install wrote rather than being replaced by it. Landing the
    ///    overrides first — which is what a single pass does — leaves the pack's own patch layer
    ///    overwritten by its own dependencies, and the pack then behaves as though the patch were not
    ///    there.
    ///
    /// @param archive the pack
    /// @param instance the instance to fill
    /// @param report  receives progress lines, or `null`
    /// @return what was written
    /// @throws DshException when the archive cannot be read, or the install fails
    public static Landed installInto(Path archive, DshInstance instance,
                                     @Nullable java.util.function.Consumer<String> report)
            throws DshException {
        Path home = instance.homeDirectory();
        Path profiles = home.resolve("profiles").resolve(instance.profile());
        // The pack's files land as root, and `DshPluginInstaller.resolve` runs pnpm as the agent
        // right after them. A root-owned profile directory is where that install dies — before
        // the pack is even usable — so the directory is prepared here and every file that lands
        // is handed over beside the write (see `land`).
        Privileges.prepareAgentDirectory(profiles, Privileges.AGENT_SHARED_DIRECTORY);

        // What the pack declares about launchers is said here rather than at the gate, because there
        // is no gate: the specification is explicit that a pack it does not declare support for is
        // installed with a warning and not refused. Said by every path that installs a pack — the
        // wizard, the market, an upload — so all of them report the same thing.
        for (String warning : launcherWarningsOf(archive)) {
            say(report, warning);
            LOG.info("Pack " + archive.getFileName() + ": " + warning);
        }

        Landed machine = land(archive, profiles, home, java.util.EnumSet.of(Part.MACHINE));
        if (machine.machine() > 0) {
            say(report, "Installing the pack's dependencies");
        } else {
            // A pack with no machine files leaves the profile to the harness, whose own
            // `dsh plugin install` writes the manifest, the patch template and the package-manager
            // settings exactly as upstream writes them. Writing them here would be reproducing
            // upstream's file formats, which is the kind of guessing that has broken an instance
            // before.
            say(report, "Initializing profile " + instance.profile());
        }
        DshPluginInstaller.resolve(instance, report);

        Landed rest = land(archive, profiles, home,
                java.util.EnumSet.of(Part.OVERRIDES, Part.HOME));

        // A pack that both lists a plugin as a bundle and inserts it applies that plugin twice, and a
        // plugin applied twice cannot claim its routes the second time — the profile then fails to
        // load at all. The duplicate is taken out here: after the patch has landed, so it is the
        // pack's own file being repaired, and before anything tries to boot the profile.
        java.nio.file.Path patch = profiles.resolve("cordis.patch.yml");
        java.util.List<String> keptWhole = DshProfilePatch.dropRedundantInserts(patch,
                DshPluginInstaller.readBundles(home, instance.profile()));
        if (!keptWhole.isEmpty()) {
            say(report, "These are both a bundle and an insert, and carry settings that would be "
                    + "lost if the duplicate were dropped: " + keptWhole);
        }

        // And then the harness is asked what it makes of the profile, because a pack can be broken in
        // ways this cannot see — a plugin the patch inserts that nothing provides, a configuration
        // the schema refuses. Failing here means failing while the pack is still in hand.
        say(report, "Checking that the pack starts");
        verifyBoots(instance);

        return new Landed(machine.files() + rest.files(), rest.overrides(), rest.home(),
                machine.machine());
    }

    /// Starts the instance the way a launch does, and stops it again, to find out whether it starts
    /// at all.
    ///
    /// **Not `--dump-config`, which was the first attempt at this and does not work.** The dump
    /// composes the patch layers and prints the tree; it resolves nothing and applies nothing, so it
    /// exits 0 for a pack whose patch inserts a package nobody provides and for one that applies a
    /// plugin twice — measured, all three of those cases green. What fails at boot is exactly those
    /// things: a module that cannot be found, a plugin claiming a route another copy already claimed.
    ///
    /// So the check is a boot: the profile is started on its own port with nothing attached, waited
    /// for, and stopped — the same thing a person would do by hand to answer "will this pack run?".
    /// The instance was made a moment ago and has no state worth keeping yet, which is what makes
    /// booting it safe here.
    ///
    /// @param instance the instance the pack was installed into
    /// @throws DshException carrying the harness's own output when the profile does not come up
    static void verifyBoots(DshInstance instance) throws DshException {
        DshLauncher.LaunchPlan plan = DshLauncher.plan(instance, null);
        StringBuilder output = new StringBuilder();
        DshProcess process = DshProcess.startPrepared(instance, plan);
        process.setLogSink(line -> output.append(line).append('\n'));
        try {
            long deadline = System.currentTimeMillis() + VERIFY_TIMEOUT.toMillis();
            while (System.currentTimeMillis() < deadline && process.state() == DshProcess.State.STARTING) {
                Thread.sleep(200);
            }
            if (process.state() != DshProcess.State.READY) {
                throw new DshException("The installed pack does not start:\n"
                        + failureOf(output.toString()));
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DshException("Interrupted while starting the installed pack", e);
        } finally {
            process.stop();
            long gone = System.currentTimeMillis() + VERIFY_TIMEOUT.toMillis();
            while (process.isRunning() && System.currentTimeMillis() < gone) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    /// Returns the lines of a failed composition that say what went wrong.
    ///
    /// The harness prints a plugin tree before it fails, so the whole output is a page of rows with
    /// one or two lines of reason in it. What a person needs is those lines: the first ones that name
    /// an error, and the ones under them, which carry the package or the route at fault.
    ///
    /// @param output everything the harness printed
    /// @return the part worth showing somebody
    static String failureOf(String output) {
        List<String> lines = new ArrayList<>();
        boolean started = false;
        for (String line : output.split("\n")) {
            String trimmed = line.strip();
            if (!started) {
                // The harness tags its own lines — `[ERROR] Error: dsh: plugin tree failed to load: …`
                // — so the reason is not always the first thing on the line.
                started = trimmed.contains("Error") || trimmed.contains("Cannot find package");
            }
            if (started && !trimmed.isEmpty()) {
                lines.add(trimmed);
            }
            if (lines.size() >= FAILURE_LINES) {
                break;
            }
        }
        return lines.isEmpty() ? output.strip() : String.join("\n", lines);
    }

    /// Says something if anybody is listening.
    ///
    /// @param report where to say it, or `null`
    /// @param line   what to say
    private static void say(@Nullable java.util.function.Consumer<String> report, String line) {
        if (report != null) {
            report.accept(line);
        }
    }

    /// Makes an instance for a pack and installs it into that instance.
    ///
    /// **One method rather than a sequence each caller repeats**, and the reason is a failure that was
    /// left on somebody's disk: the instance is created before the harness can be installed into it —
    /// it has to be, because the runtime goes inside it — so a failure between the two leaves an
    /// instance that is in the list and cannot start. The wizard has always cleaned that up; the two
    /// pages that install a pack from the market and from a file each wrote their own sequence and
    /// neither did, so a failed install left `pokemon` sitting in the list with no harness in it.
    ///
    /// A sequence written twice is a sequence fixed once, so it is written here once instead.
    ///
    /// @param archive the verified pack
    /// @param id      the instance id, which the caller has already made unique
    /// @param profile the profile the pack wants
    /// @param version the harness version the pack pins
    /// @param report  receives progress lines, or `null`
    /// @return the instance, filled in
    /// @throws DshException when the harness or the pack cannot be installed
    public static DshInstance installNew(Path archive, String id, String profile, String version,
                                         @Nullable java.util.function.Consumer<String> report)
            throws DshException {
        DshInstance instance = DshInstanceManager.create(id, version, profile,
                org.jackhuang.hmcl.util.platform.UserHome.directory(),
                org.jackhuang.hmcl.dsh.DshHomeMode.ISOLATED, null, List.of(), java.util.Map.of());
        try {
            say(report, "Installing DeepSeek Harness " + version);
            DshVersionManager.install(instance, version, report);

            say(report, "Writing the pack's files into "
                    + instance.homeDirectory().resolve("profiles").resolve(instance.profile()));
            installInto(archive, instance, report);
            return instance;
        } catch (DshException | RuntimeException failed) {
            if (failed instanceof org.jackhuang.hmcl.dsh.DshPluginInstaller.DshBuildScriptApprovalRequired) {
                // The one failure that must **keep** what it made. pnpm writes the packages it is
                // waiting to be told about into the profile's own settings and refuses the install;
                // the answer is written to that same profile by whoever asked the person, and the
                // install then finishes the instance it already has. Removing it here would take
                // the question away with it and the next attempt would ask it again from nothing.
                throw failed;
            }
            // Every other failure takes what was made for this attempt with it. A half-made instance
            // is worse than no instance: it appears in the list, it can be launched, and it fails
            // for a reason that is no longer on screen.
            org.jackhuang.hmcl.util.logging.Logger.LOG.warning(
                    "Could not install the pack into " + id + "; removing the instance made for it", failed);
            DshVersionManager.discardPartial(instance);
            try {
                DshInstanceManager.delete(id);
            } catch (DshException | RuntimeException cleanupFailure) {
                org.jackhuang.hmcl.util.logging.Logger.LOG.warning(
                        "Could not remove the half-made instance " + id, cleanupFailure);
            }
            throw failed;
        }
    }

    /// Lists an archive's members, for a caller that wants to show what a pack holds.
    ///
    /// @param archive the file
    /// @return the names, in the archive's own order
    /// @throws DshException when it cannot be read
    public static List<String> list(Path archive) throws DshException {
        List<String> names = new ArrayList<>();
        try {
            if (isTarGz(archive)) {
                try (TarArchiveInputStream tar = openTarGz(archive)) {
                    TarArchiveEntry entry;
                    while ((entry = tar.getNextEntry()) != null) {
                        names.add(entry.getName());
                    }
                }
                return List.copyOf(names);
            }
            try (ZipInputStream zip = new ZipInputStream(Files.newInputStream(archive),
                    StandardCharsets.UTF_8)) {
                ZipEntry entry;
                while ((entry = zip.getNextEntry()) != null) {
                    names.add(entry.getName());
                }
            }
        } catch (IOException e) {
            throw new DshException("Could not read " + archive, e);
        }
        return List.copyOf(names);
    }
}
