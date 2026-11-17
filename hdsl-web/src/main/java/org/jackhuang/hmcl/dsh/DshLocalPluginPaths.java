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
import org.jackhuang.hmcl.util.platform.Privileges;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.jackhuang.hmcl.util.logging.Logger.LOG;

/// The paths a profile recorded for the plugins it installed from a file, and what
/// happens to them when the instance they belong to is renamed.
///
/// [DshLocalPlugins] copies a plugin into the instance before installing it, so that the
/// profile records a path inside the instance rather than wherever the file the user
/// picked happened to be. That is the right answer to *that* file moving — but the
/// instance moves as well: its directory is named after it, so renaming an instance
/// renames every path its profile recorded, and a copy is only ever as stable as the name
/// of the directory holding it.
///
/// A package manager resolves a local dependency through the path it was given, and
/// re-resolves the whole manifest on every later operation, so one stale path stops all of
/// them: adding the next plugin fails with a message about a missing tarball rather than
/// about the plugin, and taking one out fails the same way. Nothing about the installation
/// is wrong — only the name of the directory it is written down against. This is what
/// rewrites those records.
///
/// Two things make the rewrite smaller than it sounds:
///
/// - A local dependency is recorded twice. The *specification* is the path as given —
///   `file:/…/instances/<id>/plugins/x.tgz` — and that is the one that moves. The
///   *resolution* derived from it is written relative to the profile
///   (`file:../../../plugins/x.tgz`), so it survives the move on its own, and the virtual
///   store's directory names and every link under `node_modules` are built from that
///   relative form too.
/// - Only records naming the instance's own directory have to change. A plugin installed
///   from a file outside the instance, or from the registry, is left exactly as it is.
///
/// The rewrite is a substitution of the instance directory, and an occurrence counts only
/// where that directory is a whole path segment: renaming `a` must not touch a record that
/// names `a-b`, which is the rule [DshPluginBundle] applies to a package whose name is a
/// prefix of another's.
@NotNullByDefault
public final class DshLocalPluginPaths {
    /// The profile manifest, which is what a person and the harness read.
    private static final String MANIFEST = "package.json";

    /// The records a profile keeps of a local installation.
    ///
    /// The manifest is the one the launcher writes and reads. The two lockfiles are the
    /// package manager's own account of the same installation: its lockfile, and the copy
    /// it keeps under `node_modules/.pnpm`, which it refreshes on its next run — but until
    /// then it names a file that is not there, which is the thing this exists to avoid.
    ///
    /// The workspace state beside them is deliberately not here. It is a cache keyed by
    /// directory, and a package manager that does not find its project in it validates the
    /// project again and writes the file out itself; rewriting a cache would be writing for
    /// no effect, on a file this launcher has no other business in.
    private static final List<String> RECORDS = List.of(
            MANIFEST,
            "pnpm-lock.yaml",
            "node_modules/.pnpm/lock.yaml");

    private DshLocalPluginPaths() {
    }

    /// Rewrites what the instance's profiles recorded inside the instance's old directory.
    ///
    /// Every profile under the instance's home is read, not only the one the instance
    /// boots: a path that names the directory which just moved is stale whichever profile
    /// it is written in, and a home shared with other instances is exactly where that is
    /// easiest to leave behind. A record that names nothing inside the old directory is not
    /// written at all.
    ///
    /// @param instance the instance, under the id it now has
    /// @param from     the instance directory the paths were recorded against
    /// @param to       the instance directory they belong to now
    /// @return the names of the packages whose recorded paths moved, in the order found
    /// @throws DshException when a record cannot be read or written; every record already
    ///                      rewritten is put back first, so a failure leaves them as they were
    public static List<String> relocate(DshInstance instance, Path from, Path to) throws DshException {
        Path oldDirectory = from.toAbsolutePath().normalize();
        Path newDirectory = to.toAbsolutePath().normalize();
        if (oldDirectory.equals(newDirectory)) {
            return List.of();
        }

        Path home;
        try {
            home = instance.homeDirectory();
        } catch (DshException e) {
            // An instance that cannot name its home has no profile this could find, so
            // there is nothing recorded to move. Renaming it must not fail for that.
            LOG.warning("Could not locate the home of " + instance.id()
                    + " to move the paths it recorded for local plugins", e);
            return List.of();
        }

        List<Record> written = new ArrayList<>();
        Set<String> moved = new LinkedHashSet<>();
        try {
            for (Path profile : profilesIn(home)) {
                for (String name : RECORDS) {
                    Path file = profile.resolve(name);
                    if (!Files.isRegularFile(file)) {
                        continue;
                    }
                    String text = Files.readString(file, StandardCharsets.UTF_8);
                    String rewritten = rewrite(text, oldDirectory.toString(), newDirectory.toString());
                    if (rewritten.equals(text)) {
                        continue;
                    }
                    if (name.equals(MANIFEST)) {
                        moved.addAll(changedDependencies(text, rewritten));
                    }
                    Set<PosixFilePermission> permissions = permissionsOf(file);
                    written.add(new Record(file, text, permissions));
                    write(file, rewritten, permissions);
                }
            }
        } catch (IOException | RuntimeException e) {
            restore(written);
            throw new DshException("Failed to move the local plugin paths recorded in " + home
                    + " to " + newDirectory, e);
        }

        if (!moved.isEmpty()) {
            LOG.info("Moved the recorded paths of " + String.join(", ", moved)
                    + " into " + newDirectory);
        }
        return List.copyOf(moved);
    }

    /// Lists the profiles a home holds.
    ///
    /// @param home the `DSH_HOME`
    /// @return the profile directories, or none when the home has not been used yet
    /// @throws IOException when the home cannot be listed
    private static List<Path> profilesIn(Path home) throws IOException {
        Path profiles = home.resolve("profiles");
        if (!Files.isDirectory(profiles)) {
            return List.of();
        }
        try (Stream<Path> entries = Files.list(profiles)) {
            return entries.filter(Files::isDirectory).sorted().toList();
        }
    }

    /// Replaces the directory an instance used to live in with the one it lives in now.
    ///
    /// An occurrence counts only where the recorded directory is a whole path segment —
    /// followed by a separator, or ending the value — because a record holds a path and not
    /// a string. Renaming `a` must not rewrite a record that names `a-b`, for the same
    /// reason [DshPluginBundle] will not let `dsh-a` take `dsh-ab`'s files.
    ///
    /// Both spellings of the directory are moved. A record is written by a package manager,
    /// and those separate with `/` everywhere — including on Windows, where a `\` inside a
    /// JSON string is an escape character and would make the manifest unparsable — while a
    /// `Path` renders a Windows directory with `\`. The same directory therefore appears as
    /// `file:C:/…/instances/<id>/…` in the record and as `C:\…\instances\<id>` in this
    /// class's hands. Knowing only the second spelling left the record naming a directory
    /// that no longer exists, which is exactly the state this class exists to prevent.
    ///
    /// @param text the record
    /// @param from the directory the paths were recorded against
    /// @param to   the directory they belong to now
    /// @return the record, with those paths moved
    static String rewrite(String text, String from, String to) {
        if (from.isEmpty() || text.isEmpty()) {
            return text;
        }
        String moved = rewriteOne(text, from, to);
        String slashedFrom = from.replace('\\', '/');
        if (!slashedFrom.equals(from)) {
            moved = rewriteOne(moved, slashedFrom, to.replace('\\', '/'));
        }
        return moved;
    }

    /// [rewrite] for one spelling of the directory.
    ///
    /// @param text the record
    /// @param from the directory, spelled as it is to be found
    /// @param to   the directory, spelled as it is to be written
    /// @return the record, with those paths moved
    private static String rewriteOne(String text, String from, String to) {
        StringBuilder rewritten = new StringBuilder(text.length());
        int index = 0;
        while (true) {
            int at = text.indexOf(from, index);
            if (at < 0) {
                return rewritten.append(text, index, text.length()).toString();
            }
            int end = at + from.length();
            if (end == text.length() || isSegmentEnd(text.charAt(end))) {
                rewritten.append(text, index, at).append(to);
            } else {
                rewritten.append(text, index, end);
            }
            index = end;
        }
    }

    /// Reports whether a character ends the directory in a recorded path.
    ///
    /// @param character the character after the directory
    /// @return whether the directory is a whole segment there
    private static boolean isSegmentEnd(char character) {
        return character == '/' || character == '\\' || character == '"' || character == '\''
                || Character.isWhitespace(character);
    }

    /// Returns the dependencies whose recorded specification a rewrite changed.
    ///
    /// The manifest is read as JSON here so that the answer names packages rather than
    /// counting occurrences — what the log has to say is which plugins moved. It is
    /// *written* as text rather than by serialising this: everything else in the file,
    /// including the fields a harness version adds and the order they are written in, has
    /// to come back exactly as it was.
    ///
    /// @param before the manifest as it was
    /// @param after  the manifest as it will be written
    /// @return the dependency names, or none when the manifest cannot be read
    private static List<String> changedDependencies(String before, String after) {
        JsonObject was = dependenciesOf(before);
        JsonObject is = dependenciesOf(after);
        if (was == null || is == null) {
            return List.of();
        }
        List<String> changed = new ArrayList<>();
        for (var entry : was.entrySet()) {
            JsonElement now = is.get(entry.getKey());
            if (now == null || !now.equals(entry.getValue())) {
                changed.add(entry.getKey());
            }
        }
        return changed;
    }

    /// Reads every field a profile may declare a direct dependency in.
    ///
    /// A package manager installs through any of them, so a local plugin can be recorded
    /// in any of them.
    ///
    /// @param manifest the manifest's text
    /// @return the declared dependencies, or `null` when the manifest cannot be read
    private static @Nullable JsonObject dependenciesOf(String manifest) {
        try {
            JsonElement parsed = JsonParser.parseString(manifest);
            if (!parsed.isJsonObject()) {
                return null;
            }
            JsonObject root = parsed.getAsJsonObject();
            JsonObject all = new JsonObject();
            for (String field : List.of("dependencies", "devDependencies", "optionalDependencies")) {
                JsonElement element = root.get(field);
                if (element != null && element.isJsonObject()) {
                    for (var entry : element.getAsJsonObject().entrySet()) {
                        all.add(entry.getKey(), entry.getValue());
                    }
                }
            }
            return all;
        } catch (RuntimeException e) {
            return null;
        }
    }

    /// Writes a record, keeping the mode it had.
    ///
    /// Written beside itself and moved into place: a half-written lockfile is a profile
    /// that cannot be resolved at all, which is worse than the stale path being replaced.
    ///
    /// @param file        the file to write
    /// @param text        what to put in it
    /// @param permissions the mode to keep, or `null` when its filesystem has none
    /// @throws IOException when the file cannot be written
    private static void write(Path file, String text, @Nullable Set<PosixFilePermission> permissions)
            throws IOException {
        Path staging = file.resolveSibling(file.getFileName() + ".local-paths");
        try {
            Files.writeString(staging, text, StandardCharsets.UTF_8);
            if (permissions != null) {
                // The harness writes its manifest private, and rewriting a file must not be
                // what loosens it.
                Files.setPosixFilePermissions(staging, permissions);
            }
            Files.move(staging, file, StandardCopyOption.REPLACE_EXISTING);
            Privileges.handWrittenFile(file, DshPaths.ROOT);
        } finally {
            // A move that went through left nothing behind. One that did not must not leave
            // half a record beside the record still in place.
            deleteQuietly(staging);
        }
    }

    /// Removes a file, ignoring anything that goes wrong.
    ///
    /// @param path the file
    private static void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            LOG.warning("Failed to remove " + path, e);
        }
    }

    /// Reads a file's mode, when its filesystem has one.
    ///
    /// @param file the file
    /// @return the permissions, or `null` when they cannot be read
    private static @Nullable Set<PosixFilePermission> permissionsOf(Path file) {
        try {
            return Files.getPosixFilePermissions(file);
        } catch (IOException | UnsupportedOperationException e) {
            return null;
        }
    }

    /// Puts back the records a failed move had already rewritten.
    ///
    /// @param written the records as they were
    private static void restore(List<Record> written) {
        for (Record record : written) {
            try {
                write(record.file(), record.text(), record.permissions());
            } catch (IOException e) {
                LOG.warning("Failed to put " + record.file() + " back", e);
            }
        }
    }

    /// One record of a local installation, as it was before the rewrite.
    ///
    /// @param file        the file
    /// @param text        its text
    /// @param permissions its mode, or `null` when its filesystem has none
    private record Record(Path file, String text,
                          @Nullable Set<PosixFilePermission> permissions) {
    }
}
