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
import com.google.gson.JsonParser;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.function.Consumer;
import java.util.zip.GZIPInputStream;

import static org.jackhuang.hmcl.util.logging.Logger.LOG;

/// Installs a plugin from a file the user has.
///
/// A profile records the specification it was installed from, and a package manager
/// takes a local path as just that: the path. Nothing copies the file into the
/// project, so a plugin installed from `../my-plugin.tgz` stays a plugin whose
/// installation depends on that file still being at that path — and every later
/// operation on the profile resolves the whole manifest again, so the day the file
/// moves, *all* of them fail, with a message about a missing tarball rather than
/// about the plugin. That is what this exists to avoid.
///
/// A file is therefore copied into the instance first, under a directory the
/// launcher owns, and installed from there. The profile then depends on a file
/// inside the instance — which is also the answer to where the plugin lives: it is
/// part of the instance now, and the interface says so when it is installed.
///
/// A directory is not accepted, only a packed archive. A directory install is a
/// live link to the directory: the harness reads it from where it sits, so editing
/// it changes a running installation and deleting it stops the instance from
/// starting. Packing one is `pnpm pack`, which the caller can do; taking the file
/// keeps this simple and the instance self-contained.
///
/// Keeping the file inside the instance is what makes the recorded path stable, but
/// not immovable: the instance's own directory is named after the instance, so a
/// renamed instance carries its plugin files to a new path and the records have to
/// follow — see [DshLocalPluginPaths].
@NotNullByDefault
public final class DshLocalPlugins {
    /// Where an instance's local plugin files are kept.
    public static final String DIRECTORY = "plugins";

    private DshLocalPlugins() {
    }

    /// What a plugin file holds.
    ///
    /// @param name        the package name
    /// @param version     the package version
    /// @param bundlePatch the patch file the package declares, or `null` when it
    ///                    declares no bundle and would be installed but inactive
    /// @param sizeBytes   the file's size
    public record Package(String name, String version, @Nullable String bundlePatch, long sizeBytes) {
        /// Returns whether the package would be an active plugin.
        ///
        /// @return whether it declares a bundle patch
        public boolean isBundle() {
            return bundlePatch != null && !bundlePatch.isBlank();
        }
    }

    /// What an installation did.
    ///
    /// @param pkg        what the file held
    /// @param installed  the file inside the instance, which is what the profile
    ///                   depends on now
    /// @param spec       the specification the profile recorded
    public record Result(Package pkg, Path installed, String spec) {
    }

    /// Reads what a plugin file holds, without installing anything.
    ///
    /// @param file the archive
    /// @return the package
    /// @throws DshException when the file is not a readable package
    public static Package inspect(Path file) throws DshException {
        if (!Files.isRegularFile(file)) {
            throw new DshException("There is no file at " + file);
        }
        if (!file.getFileName().toString().toLowerCase(java.util.Locale.ROOT).endsWith(".tgz")
                && !file.getFileName().toString().toLowerCase(java.util.Locale.ROOT).endsWith(".tar.gz")) {
            throw new DshException(file.getFileName() + " is not a packed plugin (.tgz)");
        }

        String manifest = readPackedManifest(file);
        if (manifest == null) {
            throw new DshException(file.getFileName() + " holds no package.json, so it is not a package");
        }
        return packageOf(manifest, sizeOf(file), file.getFileName().toString());
    }

    /// Copies a plugin file into an instance and installs it from there.
    ///
    /// @param instance the instance to install into
    /// @param file     the archive the user has
    /// @param onLine   receives progress lines, or `null`
    /// @return what was installed
    /// @throws DshException when the file is not a plugin, or the installation fails
    public static Result install(DshInstance instance, Path file, @Nullable Consumer<String> onLine)
            throws DshException {
        Package pkg = inspect(file);

        Path directory = instance.instanceDirectory().resolve(DIRECTORY);
        String fileName = safeFileName(pkg.name()) + "-" + safeFileName(pkg.version()) + ".tgz";
        Path target = directory.resolve(fileName);
        try {
            Files.createDirectories(directory);
            if (Files.exists(target) && Files.mismatch(target, file) == -1) {
                report(onLine, "Already holding " + fileName);
            } else {
                report(onLine, "Copying " + file.getFileName() + " into the instance");
                Files.copy(file, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new DshException("Failed to copy " + file + " into " + directory, e);
        }

        if (!pkg.isBundle()) {
            // Worth saying out loud: the harness installs it, reconciliation leaves
            // it out of the bundle list because it contributes no layer, and the
            // plugin then does nothing at all.
            report(onLine, "Note: " + pkg.name() + " declares no dsh.bundle, so it will be installed but not active");
        }

        // The absolute path of the copy, not the file the user picked: the profile
        // records this specification, and it has to keep pointing at something.
        String spec = target.toAbsolutePath().toString();
        report(onLine, "Installing " + pkg.name() + " " + pkg.version() + " from the instance's own copy");
        DshPluginInstaller.installSpecs(instance, List.of(spec), onLine);

        LOG.info("Installed the local plugin " + pkg.name() + " into " + instance.id() + " from " + target);
        return new Result(pkg, target, spec);
    }

    /// Removes a local plugin file an instance no longer needs.
    ///
    /// @param instance the instance
    /// @param pkg      the package that was installed from it
    /// @return whether a file was removed
    public static boolean discard(DshInstance instance, Package pkg) {
        Path target;
        try {
            target = instance.instanceDirectory().resolve(DIRECTORY)
                    .resolve(safeFileName(pkg.name()) + "-" + safeFileName(pkg.version()) + ".tgz");
        } catch (DshException e) {
            LOG.warning("Failed to locate the instance directory of " + instance.id(), e);
            return false;
        }
        try {
            return Files.deleteIfExists(target);
        } catch (IOException e) {
            LOG.warning("Failed to remove " + target, e);
            return false;
        }
    }

    /// Reads the `package.json` out of a packed archive.
    ///
    /// A gzipped tar is read here without a library: the format's header is a
    /// fixed layout, and finding one named entry in it needs a few dozen lines
    /// rather than a dependency as large as the launcher.
    ///
    /// @param file the archive
    /// @return the manifest's text, or `null` when the archive holds none
    /// @throws DshException when the archive cannot be read
    static @Nullable String readPackedManifest(Path file) throws DshException {
        try (InputStream raw = Files.newInputStream(file);
             InputStream gzip = new GZIPInputStream(raw)) {
            byte[] header = new byte[512];
            while (true) {
                int read = gzip.readNBytes(header, 0, 512);
                if (read < 512) {
                    return null;
                }
                if (isZero(header)) {
                    // Two zero blocks end an archive; one is enough to stop looking
                    // for a manifest that is not there.
                    return null;
                }

                String name = string(header, 0, 100);
                long size = octal(header, 124, 12);
                int type = header[156] & 0xff;
                String prefix = string(header, 345, 155);
                String full = prefix.isEmpty() ? name : prefix + "/" + name;

                if (type == '0' || type == 0) {
                    if (full.equals("package/package.json") || full.equals("package.json")) {
                        return new String(gzip.readNBytes((int) size), StandardCharsets.UTF_8);
                    }
                }
                long remaining = size;
                while (remaining > 0) {
                    long skipped = gzip.skip(remaining);
                    if (skipped <= 0) {
                        return null;
                    }
                    remaining -= skipped;
                }
                long padding = (512 - (size % 512)) % 512;
                while (padding > 0) {
                    long skipped = gzip.skip(padding);
                    if (skipped <= 0) {
                        break;
                    }
                    padding -= skipped;
                }
            }
        } catch (IOException e) {
            throw new DshException("Failed to read " + file, e);
        }
    }

    /// Reads a package manifest.
    ///
    /// @param body     the manifest's text
    /// @param size     the file's size
    /// @param fileName the file's name, for messages
    /// @return the package
    /// @throws DshException when the manifest names no package
    static Package packageOf(String body, long size, String fileName) throws DshException {
        JsonObject root;
        try {
            JsonElement parsed = JsonParser.parseString(body);
            if (!parsed.isJsonObject()) {
                throw new DshException(fileName + " holds a package.json that is not an object");
            }
            root = parsed.getAsJsonObject();
        } catch (RuntimeException e) {
            throw new DshException(fileName + " holds a package.json that cannot be read", e);
        }

        String name = asString(root, "name");
        if (name == null) {
            throw new DshException(fileName + " holds a package with no name");
        }

        String patch = null;
        JsonElement dsh = root.get("dsh");
        if (dsh != null && dsh.isJsonObject()) {
            JsonElement bundle = dsh.getAsJsonObject().get("bundle");
            if (bundle != null && bundle.isJsonObject()) {
                patch = asString(bundle.getAsJsonObject(), "patch");
            }
        }

        return new Package(name, asString(root, "version") == null ? "0.0.0" : asString(root, "version"),
                patch, size);
    }

    /// Returns a member as a string.
    ///
    /// @param object the object
    /// @param name   the member
    /// @return the string, or `null`
    private static @Nullable String asString(JsonObject object, String name) {
        JsonElement element = object.get(name);
        if (element == null || !element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
            return null;
        }
        String value = element.getAsString();
        return value.isBlank() ? null : value;
    }

    /// Returns a name that can be used as a file name.
    ///
    /// @param value the package name or version
    /// @return the sanitised value
    private static String safeFileName(String value) {
        return value.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    /// Returns a name that can be used as a file name, for a package a pack carries.
    ///
    /// @param value the package name or version
    /// @return the sanitised value
    static String safeName(String value) {
        return safeFileName(value);
    }

    /// Reports whether a tar header block is all zeroes.
    ///
    /// @param block the block
    /// @return whether it is empty
    private static boolean isZero(byte[] block) {
        for (byte b : block) {
            if (b != 0) {
                return false;
            }
        }
        return true;
    }

    /// Returns a NUL-terminated string from a tar header block.
    ///
    /// @param block  the block
    /// @param offset where the string starts
    /// @param length how long it may be
    /// @return the string
    private static String string(byte[] block, int offset, int length) {
        int end = offset;
        while (end < offset + length && block[end] != 0) {
            end++;
        }
        return new String(block, offset, end - offset, StandardCharsets.UTF_8).trim();
    }

    /// Returns an octal number from a tar header block.
    ///
    /// @param block  the block
    /// @param offset where the number starts
    /// @param length how long it may be
    /// @return the number, or zero
    private static long octal(byte[] block, int offset, int length) {
        String text = string(block, offset, length).trim();
        if (text.isEmpty()) {
            return 0;
        }
        try {
            return Long.parseLong(text, 8);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /// Returns a file's size.
    ///
    /// @param file the file
    /// @return the size, or zero
    private static long sizeOf(Path file) {
        try {
            return Files.size(file);
        } catch (IOException e) {
            return 0;
        }
    }

    /// Reports progress.
    ///
    /// @param onStage the sink, or `null`
    /// @param message the message
    private static void report(@Nullable Consumer<String> onStage, String message) {
        if (onStage != null) {
            onStage.accept(message);
        }
    }
}
