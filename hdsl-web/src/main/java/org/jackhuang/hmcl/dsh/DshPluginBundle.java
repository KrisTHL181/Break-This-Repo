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
import org.jackhuang.hmcl.util.io.SafePaths;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static org.jackhuang.hmcl.util.logging.Logger.LOG;

/// Carries a plugin an instance installed from a file.
///
/// A profile records a local installation as the path it was given, and a path on
/// somebody's disk is not something another machine can install from: a pack that
/// named it would describe an installation nobody can make, and an import would
/// stop there. So a pack carries the files themselves — the tarball the instance
/// keeps, or the copy the package manager took of it — and hands them back to the
/// next instance under the directory [DshLocalPlugins] owns, where they are a local
/// installation like any other.
///
/// A plugin installed from a **directory** is the one that does not travel. That is
/// somebody working on it, and the profile points at their directory rather than
/// holding a copy of it, so there is nothing here that belongs to the instance. The
/// export leaves such a plugin out and says so; see [DshPackForge] for the pack the
/// community's tooling reads, which cannot express it at all.
///
/// A plugin that the registry publishes at the version the instance has is *not*
/// carried: naming `name@version` installs the same thing from the registry, which
/// is smaller and is what a pack should do when it can. Deciding that is the
/// exporter's job — see [DshPackageRegistry#availability] — and what is left for
/// this class is the other case, where the files exist nowhere but here.
@NotNullByDefault
public final class DshPluginBundle {
    /// The directory a pack holds carried plugins under.
    public static final String ENTRY_PREFIX = "plugins/";

    private DshPluginBundle() {
    }

    /// The files of one local plugin, as they are found on the exporting side.
    ///
    /// @param name      the dependency name, which is the key the profile manifest uses
    /// @param version   the package's own version
    /// @param archive   the packed plugin the instance keeps, or `null` for a directory
    /// @param directory the profile's own copy of the plugin's files, or `null` for an archive
    public record Payload(String name, String version, @Nullable Path archive, @Nullable Path directory) {

        /// Returns the name this payload is written under.
        ///
        /// @return the entry name, ending in `/` for a directory
        public String entryName() {
            String safe = DshLocalPlugins.safeName(name) + "-" + DshLocalPlugins.safeName(version);
            return archive != null ? ENTRY_PREFIX + safe + ".tgz" : ENTRY_PREFIX + safe + "/";
        }

        /// Reports how big the payload is.
        ///
        /// @return the size in bytes, or zero when it cannot be measured
        public long size() {
            try {
                if (archive != null) {
                    return Files.size(archive);
                }
                try (var walk = Files.walk(directory)) {
                    return walk.filter(Files::isRegularFile).mapToLong(path -> {
                        try {
                            return Files.size(path);
                        } catch (IOException e) {
                            return 0L;
                        }
                    }).sum();
                }
            } catch (IOException e) {
                return 0L;
            }
        }
    }

    /// Finds the files an instance installed a local plugin from.
    ///
    /// The profile records the specification, so the specification is what is read first: a packed
    /// plugin the launcher copied into the instance is still there, and it is exactly what the other
    /// machine needs. A specification that names no readable file — `link:`, a directory, a file since
    /// deleted — falls back to what the profile *did* install, which is a directory inside its own
    /// `node_modules`.
    ///
    /// **A directory is not that file.** `link:` names a directory, and so does a `file:` that is
    /// not an archive: both install a plugin somebody is working on, and both leave the profile
    /// pointing at their directory rather than holding a copy of it. There are no files of the
    /// instance's own to carry, and reading through the link would carry somebody's working tree
    /// instead. Such a specification is therefore answered with `null`, and the caller says the
    /// plugin is one under development.
    ///
    /// @param profileDirectory the profile the plugin belongs to
    /// @param name             the dependency name
    /// @param declared         what the profile declares for it
    /// @return the payload, or `null` when no files can be found
    public static @Nullable Payload locate(Path profileDirectory, String name, String declared) {
        Path file = fileOf(declared, profileDirectory);
        if (file != null && Files.isRegularFile(file)) {
            try {
                DshLocalPlugins.Package pkg = DshLocalPlugins.inspect(file);
                return new Payload(name, pkg.version(), file, null);
            } catch (DshException e) {
                LOG.warning("Could not read the packed plugin " + file, e);
            }
        }

        if (namesADirectory(declared, profileDirectory) || isLinked(profileDirectory, name)) {
            return null;
        }
        Path directory = profileDirectory.resolve("node_modules").resolve(name);
        if (Files.isDirectory(directory)) {
            String version = versionOf(directory);
            if (version != null) {
                return new Payload(name, version, null, readable(directory));
            }
        }
        return null;
    }

    /// Reports whether a local specification names a directory rather than a packed plugin.
    ///
    /// `link:` always does: it is a promise about a directory, not about a file. A `file:` may
    /// name either, and so may a bare path, which is why the answer is looked for rather than
    /// assumed — where the path is still there it is asked, and where it is not, a name that is
    /// not an archive is a directory.
    ///
    /// This is what a plugin under development is installed with, and it is the half a pack cannot
    /// carry: the entry in `node_modules` is a link to wherever somebody is working, so the
    /// instance holds none of the files of its own. Which is why the question is asked of the
    /// specification rather than of the link: what the package manager did with the directory is
    /// its business, and the answer has to be the same whichever layout it chose.
    ///
    /// @param declared         the specification
    /// @param profileDirectory the profile, for a specification that is relative
    /// @return whether it names a directory
    public static boolean namesADirectory(String declared, Path profileDirectory) {
        String raw = declared == null ? "" : declared.trim();
        if (raw.startsWith("link:")) {
            return true;
        }
        Path path = fileOf(raw, profileDirectory);
        if (path == null) {
            // Not a local path at all: a registry version, a repository, or a tarball over HTTP.
            return false;
        }
        if (Files.isDirectory(path)) {
            return true;
        }
        return !DshPluginSources.ARCHIVE.matcher(path.getFileName().toString()).matches();
    }

    /// Returns the directory a walk of an entry actually reads.
    ///
    /// A package manager may keep a dependency's files behind a link of its own: pnpm resolves
    /// every dependency into `node_modules/.pnpm` inside the profile, and a directory `file:`
    /// install is one of them. A walk does not descend into the path it starts at when that path
    /// is a link — it reports the link itself and nothing under it — so what is handed out is
    /// where the files are rather than where the profile points at them. Without this, such a
    /// plugin would travel as the one file that is read without walking: its `package.json`.
    ///
    /// A plain directory is handed back unchanged, and so is one that cannot be resolved.
    ///
    /// @param directory the entry the profile holds
    /// @return the directory to read
    private static Path readable(Path directory) {
        try {
            Path parent = directory.getParent().toRealPath();
            Path real = directory.toRealPath();
            return real.equals(parent.resolve(directory.getFileName())) ? directory : real;
        } catch (IOException | RuntimeException e) {
            return directory;
        }
    }

    /// Reports whether the profile's copy of a package is a link out of the profile.
    ///
    /// A link is asked about twice — once when the files are looked for and once when the reason
    /// they are not there is said out loud — and both times the answer has to be the same one, so
    /// it is given in one place.
    ///
    /// **Where it resolves to is the question, not whether it is a link.** A package manager's own
    /// `node_modules` is made of links: pnpm puts every dependency behind one, resolving into
    /// `node_modules/.pnpm` inside the profile, so asking only whether the entry is a link would
    /// report the whole profile as somebody's working tree. What matters is whether the files are
    /// the profile's at all, and the answer to that is whether they live inside it.
    ///
    /// Resolving is also what makes this hold on Windows, where the package manager makes a
    /// *junction* — a link to every other program on the machine, a plain directory to Java, and
    /// not what [Files#isSymbolicLink] reports on.
    ///
    /// @param profileDirectory the profile the plugin belongs to
    /// @param name             the dependency name
    /// @return whether the entry names a directory outside the profile
    public static boolean isLinked(Path profileDirectory, String name) {
        Path directory = profileDirectory.resolve("node_modules").resolve(name);
        if (!Files.isDirectory(directory)) {
            // Nothing to resolve and nothing to carry; the callers have a different sentence for
            // a package the profile no longer has.
            return false;
        }
        try {
            return !directory.toRealPath().startsWith(profileDirectory.toRealPath());
        } catch (IOException | RuntimeException e) {
            // Unreadable is not linked, for the same reason.
            return false;
        }
    }

    /// Reads the file a local specification names.
    ///
    /// @param declared         the specification
    /// @param profileDirectory the profile, for a specification that is relative
    /// @return the file, or `null` when the specification does not name one
    private static @Nullable Path fileOf(String declared, Path profileDirectory) {
        String raw = declared.trim();
        if (raw.startsWith("link:")) {
            // A link is a promise about a directory, not a file, and the directory is not part of
            // the instance: what the profile installed is read from its own node_modules instead.
            return null;
        }
        if (raw.startsWith("file:")) {
            raw = raw.substring("file:".length());
            // `file:///x` and `file:/x` are both spellings of an absolute path.
            if (raw.startsWith("//")) {
                raw = raw.substring(2);
            }
        }
        if (raw.isBlank() || raw.startsWith("http:") || raw.startsWith("https:")) {
            return null;
        }
        Path path = null;
        try {
            path = Path.of(raw);
        } catch (RuntimeException e) {
            // A specification this is not a path at all; it was handled as a non-local one elsewhere.
            return null;
        }
        if (!path.isAbsolute()) {
            path = profileDirectory.resolve(path);
        }
        if (!Files.isRegularFile(path) && raw.contains("%")) {
            // npm records a path as a URI when it contains characters that need escaping.
            try {
                Path decoded = Path.of(java.net.URLDecoder.decode(raw, java.nio.charset.StandardCharsets.UTF_8));
                if (Files.isRegularFile(decoded)) {
                    return decoded;
                }
            } catch (RuntimeException e) {
                return path;
            }
        }
        return path;
    }

    /// Reads a package's version from its directory.
    ///
    /// @param directory the package directory
    /// @return the version, or `null` when the directory holds no readable manifest
    private static @Nullable String versionOf(Path directory) {
        Path manifest = directory.resolve("package.json");
        if (!Files.isRegularFile(manifest)) {
            return null;
        }
        try {
            JsonElement parsed = JsonParser.parseString(Files.readString(manifest));
            if (!parsed.isJsonObject()) {
                return null;
            }
            JsonObject object = parsed.getAsJsonObject();
            JsonElement version = object.get("version");
            return version == null || version.isJsonNull() ? null : version.getAsString();
        } catch (IOException | RuntimeException e) {
            LOG.warning("Could not read the version of " + directory, e);
            return null;
        }
    }

    /// Returns the names of the plugins a pack carries.
    ///
    /// @param pack the archive
    /// @return the entry names, in the order the archive holds them
    /// @throws DshException when the archive cannot be read
    public static List<String> carried(Path pack) throws DshException {
        List<String> names = new ArrayList<>();
        try (ZipInputStream zip = new ZipInputStream(Files.newInputStream(pack))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (entry.getName().startsWith(ENTRY_PREFIX)) {
                    names.add(entry.getName());
                }
            }
        } catch (IOException e) {
            throw new DshException("Failed to read " + pack, e);
        }
        return names;
    }

    /// Writes a plugin's files into a pack.
    ///
    /// @param zip     the archive being written
    /// @param payload the files
    /// @param onStage receives progress lines, or `null`
    /// @throws IOException when the archive cannot be written
    public static void writeInto(ZipOutputStream zip, Payload payload,
                                 @Nullable Consumer<String> onStage) throws IOException {
        if (payload.archive() != null) {
            ZipEntry entry = new ZipEntry(payload.entryName());
            entry.setTime(0L);
            zip.putNextEntry(entry);
            Files.copy(payload.archive(), zip);
            zip.closeEntry();
            report(onStage, "Carrying " + payload.name() + " " + payload.version()
                    + " as the plugin file this instance keeps");
            return;
        }

        Path directory = payload.directory();
        if (directory == null) {
            return;
        }
        String prefix = payload.entryName();
        int files = 0;
        try (var walk = Files.walk(directory)) {
            for (Path path : walk.filter(Files::isRegularFile).toList()) {
                String relative = directory.relativize(path).toString().replace('\\', '/');
                if (relative.startsWith("node_modules/") || relative.contains("/node_modules/")
                        || relative.startsWith(".git/") || relative.contains("/.git/")) {
                    continue;
                }
                // The manifest goes in last: it is what makes a directory a package, so an archive
                // that was cut short holds files no package manager will pick up rather than half a
                // plugin.
                if (relative.equals("package.json")) {
                    continue;
                }
                ZipEntry entry = new ZipEntry(prefix + relative);
                entry.setTime(0L);
                zip.putNextEntry(entry);
                Files.copy(path, zip);
                zip.closeEntry();
                files++;
            }
        }
        // The manifest goes in last: it is what makes the directory a package, so an archive that
        // was cut short holds files that no package manager will pick up rather than half a plugin.
        Path manifest = directory.resolve("package.json");
        if (Files.isRegularFile(manifest)) {
            ZipEntry entry = new ZipEntry(prefix + "package.json");
            entry.setTime(0L);
            zip.putNextEntry(entry);
            Files.copy(manifest, zip);
            zip.closeEntry();
            files++;
        }
        report(onStage, "Carrying " + payload.name() + " " + payload.version() + " from the directory it was"
                + " installed from, " + files + " file(s)");
    }

    /// Puts a pack's copy of a plugin back into an instance.
    ///
    /// The files are written under the directory [DshLocalPlugins] keeps local plugin files in, so
    /// the instance that receives them has a local installation exactly like the one the pack was
    /// made from — including a file the interface can later remove or replace.
    ///
    /// @param pack             the archive
    /// @param pluginsDirectory the instance's plugin directory
    /// @param name             the dependency name
    /// @param version          the version the pack recorded
    /// @return where the files were written, or `null` when the pack carries none
    /// @throws DshException when the archive cannot be read or written out
    public static @Nullable Path release(Path pack, Path pluginsDirectory, String name, String version)
            throws DshException {
        String safe = DshLocalPlugins.safeName(name);
        String wanted = ENTRY_PREFIX + safe + "-" + DshLocalPlugins.safeName(version) + ".tgz";
        Path released = null;
        try {
            Files.createDirectories(pluginsDirectory);
            try (ZipInputStream zip = new ZipInputStream(Files.newInputStream(pack))) {
                ZipEntry entry;
                while ((entry = zip.getNextEntry()) != null) {
                    String entryName = entry.getName();
                    if (entry.isDirectory() || !entryName.startsWith(ENTRY_PREFIX)) {
                        continue;
                    }
                    // What the pack called the payload is what the instance calls it too, so a pack
                    // whose recorded version differs from the name it was written under still comes
                    // back out whole.
                    String rest = entryName.substring(ENTRY_PREFIX.length());
                    int slash = rest.indexOf('/');
                    String carried = slash < 0 ? rest : rest.substring(0, slash);
                    if (!isOurs(carried, safe)) {
                        continue;
                    }
                    if (slash < 0) {
                        if (!entryName.endsWith(".tgz")) {
                            continue;
                        }
                        Path target = pluginsDirectory.resolve(carried);
                        Files.copy(zip, target, StandardCopyOption.REPLACE_EXISTING);
                        return target.toAbsolutePath().normalize();
                    }
                    Path base = pluginsDirectory.toAbsolutePath().normalize();
                    Path target = SafePaths.resolveInside(base, base.resolve(carried)
                            .resolve(rest.substring(slash + 1).replace('/', java.io.File.separatorChar)));
                    // A pack is an archive somebody downloaded, so where each of its members lands is
                    // checked rather than assumed: the name a payload carries is a path, and a path
                    // is what an archive chooses for itself. `plugins/<name>-<version>/../../x.yml`
                    // passes the check above — the payload is this package's — and would otherwise be
                    // written over whatever the two `..` reach, which for a plugin directory inside an
                    // instance is the instance, the profile and, from there, anything the user owns.
                    //
                    // `..` is only the half of it that is made of strings. A plugin installed with
                    // `link:` leaves a symlink directory beside these, and a member whose parent is
                    // one begins with the plugin directory while the write lands wherever the link
                    // points — which is why the check is [SafePaths]'s and asks the filesystem as
                    // well.
                    if (target == null) {
                        LOG.warning("Not unpacking " + entryName + " out of " + pack.getFileName()
                                + ": it would be written outside " + base);
                        throw new IOException("That pack tries to write outside the plugin directory ("
                                + entryName + ")");
                    }
                    SafePaths.createDirectories(base, target.getParent());
                    // Read straight out of the archive: closing anything here would close the
                    // archive, and the entries after this one would be gone.
                    Files.copy(zip, target, StandardCopyOption.REPLACE_EXISTING);
                    released = pluginsDirectory.resolve(carried).toAbsolutePath().normalize();
                }
            }
        } catch (IOException e) {
            throw new DshException("Failed to put the pack's copy of " + name + " into " + pluginsDirectory, e);
        }
        return released;
    }

    /// Reports whether a carried payload belongs to a package.
    ///
    /// The name a payload is written under is the package's name and version, so it starts with the
    /// package's name and a separator. A package whose name is a prefix of another's — `dsh-a` beside
    /// `dsh-ab` — must not take the other's files, which is what the separator is for.
    ///
    /// @param carried the name the payload is carried under
    /// @param safe    the package's safe name
    /// @return whether the payload is this package's
    private static boolean isOurs(String carried, String safe) {
        if (carried.equals(safe) || carried.equals(safe + ".tgz")) {
            return true;
        }
        return carried.startsWith(safe + "-") || carried.startsWith(safe + ".");
    }

    /// Reports a line of progress.
    ///
    /// @param onStage receives the line, or `null`
    /// @param line    the line
    private static void report(@Nullable Consumer<String> onStage, String line) {
        if (onStage != null) {
            onStage.accept(line);
        }
    }
}
