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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.jackhuang.hmcl.util.logging.Logger.LOG;

/// The plugins this panel ships inside its own jar.
///
/// A first-party plugin is an ordinary npm package under `plugins/` in this
/// repository. The build packs each one into an npm-layout tarball, hashes it and
/// writes `/first-party-plugins/index.json` next to it, all of which end up in the
/// jar's resources. This class is the read side of that index: it answers which
/// packages are bundled, at which version, and writes the tarball out when the
/// install route asks for a local specification.
///
/// The point is the offline install. `dsh plugin add file:<tarball>` resolves from
/// the file system, so a panel instance can install its terminal interface on a
/// machine with no registry route — which is the normal state of a deployment
/// behind a firewall, and the reason the panel exists at all.
@NotNullByDefault
public final class FirstPartyPlugins {

    /// Where the build puts the index inside the jar.
    static final String INDEX_RESOURCE = "/first-party-plugins/index.json";

    /// One bundled plugin, as the build described it.
    ///
    /// @param name    the npm package name
    /// @param version the package version
    /// @param file    the tarball's file name inside `/first-party-plugins/`
    /// @param sha256  the tarball's digest, or `null` when the index did not record one
    public record Bundled(String name, String version, String file, @Nullable String sha256) {

        /// The classpath resource holding the tarball.
        ///
        /// @return the absolute resource path
        public String resourcePath() {
            return "/first-party-plugins/" + file;
        }
    }

    /// Loaded once: the index belongs to the running jar and cannot change under it.
    private static volatile @Nullable List<Bundled> loaded;

    private FirstPartyPlugins() {
    }

    /// Every bundled plugin, in the order the build listed them.
    ///
    /// @return the entries, empty when this build shipped none
    public static List<Bundled> all() {
        List<Bundled> cached = loaded;
        if (cached != null) {
            return cached;
        }
        synchronized (FirstPartyPlugins.class) {
            if (loaded == null) {
                loaded = readIndex();
            }
            return loaded;
        }
    }

    /// Looks a bundle up by package name, case-insensitively.
    ///
    /// @param packageName the npm package name, or `null`
    /// @return the entry, or empty when this build ships no such plugin
    public static Optional<Bundled> find(@Nullable String packageName) {
        if (packageName == null || packageName.isBlank()) {
            return Optional.empty();
        }
        String wanted = packageName.trim();
        for (Bundled bundled : all()) {
            if (bundled.name().equalsIgnoreCase(wanted)) {
                return Optional.of(bundled);
            }
        }
        return Optional.empty();
    }

    /// Whether this build ships the named plugin.
    ///
    /// @param packageName the npm package name
    /// @return whether it is bundled
    public static boolean isBundled(@Nullable String packageName) {
        return find(packageName).isPresent();
    }

    /// Where the panel remembers which tarball it installed, per instance home.
    ///
    /// The version number cannot answer "is this the same build": a source change
    /// without a version bump produces two different packages that call themselves
    /// `0.2.0`. The tarball's digest can, and it is already in the index.
    static final String INSTALL_RECORD = ".hdsl-web/first-party-installs.json";

    /// The digest this instance recorded for a bundled plugin, when it has one.
    ///
    /// A missing record is not "up to date": it is a panel that installed the
    /// plugin before this record existed, so the caller should treat the package as
    /// unknown and offer a reinstall (see [#isOutdated]).
    ///
    /// @param home        the instance's `DSH_HOME`
    /// @param packageName the npm package name
    /// @return the recorded digest, or `null`
    public static @Nullable String installedDigest(Path home, @Nullable String packageName) {
        if (packageName == null || packageName.isBlank()) {
            return null;
        }
        Path file = home.resolve(INSTALL_RECORD);
        if (!Files.isRegularFile(file)) {
            return null;
        }
        try {
            JsonElement parsed = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8));
            if (!parsed.isJsonObject()) {
                return null;
            }
            JsonElement value = parsed.getAsJsonObject().get(packageName);
            return value != null && value.isJsonPrimitive() ? value.getAsString() : null;
        } catch (IOException | RuntimeException e) {
            LOG.warning("Could not read the first-party install record " + file, e);
            return null;
        }
    }

    /// Records the digest of the tarball that was just installed.
    ///
    /// Written beside the instance home, atomically: a half-written file would be
    /// read as "no record", which is the safe direction but a confusing one.
    ///
    /// @param home        the instance's `DSH_HOME`
    /// @param packageName the npm package name
    /// @param sha256      the tarball digest, or `null` to clear the record
    /// @throws DshException when the file cannot be written
    public static void recordInstalled(Path home, String packageName, @Nullable String sha256)
            throws DshException {
        Path file = home.resolve(INSTALL_RECORD);
        Map<String, String> records = new LinkedHashMap<>();
        if (Files.isRegularFile(file)) {
            try {
                JsonElement parsed = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8));
                if (parsed.isJsonObject()) {
                    for (Map.Entry<String, JsonElement> entry : parsed.getAsJsonObject().entrySet()) {
                        if (entry.getValue().isJsonPrimitive()) {
                            records.put(entry.getKey(), entry.getValue().getAsString());
                        }
                    }
                }
            } catch (IOException | RuntimeException e) {
                LOG.warning("Could not read the first-party install record " + file, e);
            }
        }
        if (sha256 == null) {
            records.remove(packageName);
        } else {
            records.put(packageName, sha256);
        }
        JsonObject json = new JsonObject();
        for (Map.Entry<String, String> entry : records.entrySet()) {
            json.addProperty(entry.getKey(), entry.getValue());
        }
        try {
            Files.createDirectories(file.getParent());
            Path staging = file.resolveSibling(file.getFileName() + ".tmp");
            Files.writeString(staging, json + "\n", StandardCharsets.UTF_8);
            try {
                Files.move(staging, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(staging, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new DshException("Could not write " + file, e);
        }
    }

    /// Whether an installed bundled package is not the one this build ships.
    ///
    /// The question is the digest, not the version: a source change that forgot to
    /// bump the version produces the same `0.2.0` twice, and the panel used to see
    /// "installed" and never offer the newer files. An instance with no record
    /// counts as outdated — reinstalling once is what fills the record in.
    ///
    /// @param home        the instance's `DSH_HOME`
    /// @param packageName the npm package name
    /// @return whether a reinstall would change the files
    public static boolean isOutdated(Path home, String packageName) {
        Bundled bundled = find(packageName).orElse(null);
        if (bundled == null || bundled.sha256() == null) {
            return false;
        }
        String installed = installedDigest(home, packageName);
        return installed == null || !bundled.sha256().equalsIgnoreCase(installed);
    }

    /// Writes a bundled plugin's tarball out under `directory`.
    ///
    /// The digest is verified against the index before the file is written, so a
    /// truncated or repacked jar is reported here rather than as a pnpm error the
    /// user cannot read.
    ///
    /// @param packageName the npm package name
    /// @param directory   the directory to write into; created when missing
    /// @return the tarball's path
    /// @throws DshException when the plugin is not bundled, the resource is gone, or
    ///                      the digest does not match
    public static Path materialize(String packageName, Path directory) throws DshException {
        Bundled bundled = find(packageName).orElseThrow(() -> new DshException(
                packageName + " 不在这个面板自带的插件清单里。"));
        byte[] bytes;
        try (InputStream in = FirstPartyPlugins.class.getResourceAsStream(bundled.resourcePath())) {
            if (in == null) {
                throw new DshException("这个面板打出来的 jar 里没有 " + bundled.file()
                        + "（自带插件清单提到了它，资源却不在）。请重新构建面板。");
            }
            bytes = in.readAllBytes();
        } catch (IOException e) {
            // The one failure that looks like a broken build but is not: the jar was
            // replaced while this panel was running, so the ZIP index it holds points
            // into bytes that have moved. Say that instead of blaming the file.
            throw new DshException("读取自带插件 " + bundled.file() + " 失败"
                    + org.jackhuang.hmcl.util.PanelJar.replacedHint()
                            .map(hint -> "：" + hint).orElse(""), e);
        }
        String recorded = bundled.sha256();
        if (recorded != null && !recorded.isBlank()) {
            String actual = sha256(bytes);
            if (!recorded.equalsIgnoreCase(actual)) {
                throw new DshException("自带插件 " + bundled.file() + " 的摘要不对（清单 "
                        + recorded + "，实际 " + actual + "）。面板的 jar 可能被改动过。");
            }
        }
        try {
            Files.createDirectories(directory);
            Path target = directory.resolve(bundled.file());
            Files.write(target, bytes);
            return target;
        } catch (IOException e) {
            throw new DshException("写入自带插件 " + bundled.file() + " 失败", e);
        }
    }

    /// Reads and validates the index from the classpath.
    private static List<Bundled> readIndex() {
        try (InputStream in = FirstPartyPlugins.class.getResourceAsStream(INDEX_RESOURCE)) {
            if (in == null) {
                // A plain `gradle build` in an IDE can run classes without the
                // resources task; an empty list keeps the panel working and says so.
                LOG.info("No first-party plugin index in this build (" + INDEX_RESOURCE + ')');
                return List.of();
            }
            JsonElement parsed = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            if (!parsed.isJsonObject()) {
                LOG.warning("Malformed first-party plugin index: not an object");
                return List.of();
            }
            JsonArray plugins = parsed.getAsJsonObject().getAsJsonArray("plugins");
            if (plugins == null) {
                return List.of();
            }
            List<Bundled> entries = new ArrayList<>();
            for (JsonElement element : plugins) {
                if (!element.isJsonObject()) {
                    continue;
                }
                JsonObject json = element.getAsJsonObject();
                String name = string(json, "name");
                String version = string(json, "version");
                String file = string(json, "file");
                if (name == null || version == null || file == null) {
                    LOG.warning("Skipping an incomplete first-party plugin entry: " + json);
                    continue;
                }
                entries.add(new Bundled(name, version, file, string(json, "sha256")));
            }
            return List.copyOf(entries);
        } catch (IOException | RuntimeException e) {
            LOG.warning("Could not read the first-party plugin index", e);
            return List.of();
        }
    }

    /// A string member, or `null` when absent or not a primitive.
    private static @Nullable String string(JsonObject json, String field) {
        JsonElement element = json.get(field);
        return element != null && element.isJsonPrimitive() ? element.getAsString() : null;
    }

    /// The lower-case hex SHA-256 of a byte array.
    static String sha256(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(bytes);
            StringBuilder builder = new StringBuilder(hash.length * 2);
            for (byte value : hash) {
                builder.append(Character.forDigit((value >> 4) & 0xf, 16));
                builder.append(Character.forDigit(value & 0xf, 16));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException e) {
            // Every Java platform is required to ship SHA-256.
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }
}
