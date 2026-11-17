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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jackhuang.hmcl.util.gson.JsonUtils;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.jackhuang.hmcl.util.logging.Logger.LOG;

/// Reads and migrates the sessions stored in a DeepSeek Harness home.
///
/// ## Layout
///
/// ```text
/// <home>/sessions/<workspace-slug>/<session-id>/
///     session.lock
///     session.v3.jsonl.zstd
/// <home>/storages/session_projcache/sessions/<session-id>.json
/// ```
///
/// The projection cache is derived data, so it is treated as optional: a session
/// is listed from its directory alone, and the cache only supplies the title and
/// the working directory. Migration copies it along so the target does not have
/// to rebuild it, but never fails when it is missing.
///
/// ## Compatibility
///
/// The log format version is part of the log's file name, and it is the only
/// compatibility signal DeepSeek Harness publishes. Migration is refused unless
/// the target can already read that version, because a home whose sessions are
/// rewritten in place is exactly the failure that makes sharing one `DSH_HOME`
/// across versions unsafe.
@NotNullByDefault
public final class DshSessions {
    private DshSessions() {
    }

    /// Reports whether a file name is one of the harness's own log names.
    ///
    /// Package-visible so that the session-pack code copies exactly the files the
    /// harness would read, and nothing else that happens to sit in a session
    /// directory.
    ///
    /// @param fileName the file name
    /// @return whether the harness reads a file with that name
    static boolean isLogName(String fileName) {
        return LOG_FILE.matcher(fileName).matches();
    }

    /// Matches the harness's own log names, capturing the generation.
    ///
    /// The names the harness reads are `session.jsonl`, `session.jsonl.zstd`,
    /// `session.v<N>.jsonl` and `session.v<N>.jsonl.zstd`, and nothing else: a
    /// file that merely starts with one of them belongs to something that is not
    /// the session. `dsh-cost-meter`, for instance, leaves
    /// `session.v3.jsonl.zstd.cost-meter-backup-<stamp>-<uuid>` copies in the
    /// directory, and a pattern that accepted any suffix would report one of
    /// those as the session — or, in a directory holding two generations, report
    /// the older one, which is what the migration refusal is decided on.
    private static final Pattern LOG_FILE = Pattern.compile("^session(?:\\.v([1-9][0-9]*))?\\.jsonl(?:\\.zstd)?$");

    /// Returns the sessions stored in a home, newest first.
    ///
    /// @param home the `DSH_HOME` to scan
    /// @return the sessions, newest first
    /// @throws DshException when the home cannot be read
    public static @Unmodifiable List<DshSession> list(Path home) throws DshException {
        Path root = home.resolve("sessions");
        if (!Files.isDirectory(root)) {
            return List.of();
        }

        List<DshSession> sessions = new ArrayList<>();
        try (Stream<Path> slugs = Files.list(root)) {
            for (Path slugDirectory : slugs.filter(Files::isDirectory).toList()) {
                try (Stream<Path> children = Files.list(slugDirectory)) {
                    for (Path sessionDirectory : children.filter(Files::isDirectory).toList()) {
                        DshSession session = read(slugDirectory, sessionDirectory);
                        if (session != null) {
                            sessions.add(session);
                        }
                    }
                }
            }
        } catch (IOException e) {
            throw new DshException("Failed to read the sessions in " + root, e);
        }

        sessions.sort(Comparator.comparingLong(DshSession::modifiedAt).reversed());
        return List.copyOf(sessions);
    }

    /// Groups a home's sessions by the workspace they were recorded in.
    ///
    /// The grouping is the harness's own — the slug directory a session lives
    /// under — rather than a second opinion derived from the working-directory
    /// string, so one workspace can never be split by two spellings of one path.
    /// Sessions arrive newest first, and so do the workspaces.
    ///
    /// @param home the `DSH_HOME` to read
    /// @return the workspaces, newest activity first
    /// @throws DshException when the home cannot be read
    public static @Unmodifiable List<DshWorkspace> workspaces(Path home) throws DshException {
        Map<String, List<DshSession>> bySlug = new LinkedHashMap<>();
        for (DshSession session : list(home)) {
            bySlug.computeIfAbsent(session.workspaceSlug(), slug -> new ArrayList<>()).add(session);
        }

        List<DshWorkspace> workspaces = new ArrayList<>();
        for (Map.Entry<String, List<DshSession>> entry : bySlug.entrySet()) {
            String path = null;
            for (DshSession session : entry.getValue()) {
                if (session.workingDirectory() != null && !session.workingDirectory().isBlank()) {
                    path = session.workingDirectory();
                    break;
                }
            }
            workspaces.add(new DshWorkspace(entry.getKey(), path,
                    DshWorkspace.titleOf(path, entry.getKey()), List.copyOf(entry.getValue())));
        }
        workspaces.sort(Comparator.comparingLong(DshWorkspace::modifiedAt).reversed());
        return List.copyOf(workspaces);
    }

    /// Reads one session directory.
    ///
    /// @param slugDirectory    the workspace-slug directory
    /// @param sessionDirectory the session directory
    /// @return the session, or `null` when the directory holds no recognisable log
    private static @Nullable DshSession read(Path slugDirectory, Path sessionDirectory) {
        Path log = findLog(sessionDirectory);
        if (log == null) {
            return null;
        }

        Matcher matcher = LOG_FILE.matcher(log.getFileName().toString());
        if (!matcher.matches()) {
            return null;
        }

        // An unversioned name is the first generation the harness wrote.
        int formatVersion;
        try {
            formatVersion = matcher.group(1) == null ? 0 : Integer.parseInt(matcher.group(1));
        } catch (NumberFormatException e) {
            return null;
        }

        String id = sessionDirectory.getFileName().toString();
        CacheRow cache = readCache(slugDirectory.getParent().getParent(), id);
        long modifiedAt = cache == null ? lastModified(sessionDirectory) : Math.max(cache.createdAt(), lastModified(sessionDirectory));

        return new DshSession(id, slugDirectory.getFileName().toString(), sessionDirectory,
                formatVersion,
                cache == null ? null : cache.cwd(),
                cache == null ? null : cache.title(),
                directorySize(sessionDirectory),
                modifiedAt,
                isHeld(sessionDirectory.resolve("session.lock")));
    }

    /// Reports whether a session's write lease is currently held.
    ///
    /// The lock file itself is left behind when a lease is released, so its mere
    /// existence means nothing. DeepSeek Harness takes a non-blocking `flock(2)`
    /// on it, and `flock(2)` is a **different lock namespace** from the `fcntl`
    /// locks Java exposes through [java.nio.channels.FileChannel#tryLock], so
    /// the JVM cannot observe it. The util-linux `flock(1)` command can.
    ///
    /// When `flock` is unavailable the check degrades to "not held"; the caller
    /// separately refuses to migrate out of a running instance, which covers the
    /// case that matters most.
    ///
    /// @param lockFile the session's lock file
    /// @return whether another process holds the lease
    private static boolean isHeld(Path lockFile) {
        if (!Files.exists(lockFile)) {
            return false;
        }
        if (org.jackhuang.hmcl.util.platform.OperatingSystem.CURRENT_OS
                == org.jackhuang.hmcl.util.platform.OperatingSystem.WINDOWS) {
            // `flock` and `true` are util-linux and coreutils, and Windows has neither.
            // The lease is taken with `flock(2)`, a syscall that cannot be observed
            // through a file handle here, so this question is unanswerable on this
            // platform and every session reads as free. Saying so once is better than
            // paying for a spawn that is certain to fail on every check.
            return false;
        }
        try {
            Process process = new ProcessBuilder(
                    org.jackhuang.hmcl.util.platform.Executables.startable(
                            List.of("flock", "-n", lockFile.toString(), "true")))
                    .redirectErrorStream(true)
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .start();
            if (!process.waitFor(5, java.util.concurrent.TimeUnit.SECONDS)) {
                process.destroyForcibly();
                return false;
            }
            // `flock -n` exits non-zero precisely when the lease is taken.
            return process.exitValue() != 0;
        } catch (IOException e) {
            LOG.info("flock(1) is unavailable, so session leases cannot be observed: " + e);
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /// Finds the session log inside a session directory.
    ///
    /// A continued session can leave more than one generation behind, because a
    /// write publishes the current one beside the one it read; the harness reads
    /// the highest, so this does too. Between two spellings of one generation the
    /// compressed one is taken, which is the one a home is normally configured
    /// for.
    ///
    /// @param sessionDirectory the session directory
    /// @return the log file, or `null` when there is none
    private static @Nullable Path findLog(Path sessionDirectory) {
        try (Stream<Path> files = Files.list(sessionDirectory)) {
            return files.filter(Files::isRegularFile)
                    .filter(path -> LOG_FILE.matcher(path.getFileName().toString()).matches())
                    .max(Comparator.comparingInt(DshSessions::generationOf)
                            .thenComparing(path -> path.getFileName().toString().endsWith(".zstd")))
                    .orElse(null);
        } catch (IOException e) {
            return null;
        }
    }

    /// Returns the generation a log file's name carries.
    ///
    /// @param log the log file
    /// @return the generation, zero for an unversioned name
    private static int generationOf(Path log) {
        Matcher matcher = LOG_FILE.matcher(log.getFileName().toString());
        if (!matcher.matches() || matcher.group(1) == null) {
            return 0;
        }
        try {
            return Integer.parseInt(matcher.group(1));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /// Returns the last modification time of a directory tree.
    ///
    /// @param directory the directory
    /// @return the newest modification time, or `0` when it cannot be read
    private static long lastModified(Path directory) {
        try {
            return Files.getLastModifiedTime(directory).toMillis();
        } catch (IOException e) {
            return 0L;
        }
    }

    /// Returns the total size of a directory tree.
    ///
    /// @param directory the directory
    /// @return the size in bytes
    private static long directorySize(Path directory) {
        long[] total = {0L};
        try {
            Files.walkFileTree(directory, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) {
                    total[0] += attributes.size();
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException e) {
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            LOG.warning("Failed to size " + directory, e);
        }
        return total[0];
    }

    /// Returns the format versions already present in a home.
    ///
    /// @param home the `DSH_HOME` to scan
    /// @return the format versions, in ascending order
    /// @throws DshException when the home cannot be read
    public static @Unmodifiable Set<Integer> formatVersions(Path home) throws DshException {
        Set<Integer> versions = new LinkedHashSet<>();
        for (DshSession session : list(home)) {
            versions.add(session.formatVersion());
        }
        return java.util.Collections.unmodifiableSet(versions);
    }

    /// Explains why a session cannot be migrated into a home, if it cannot.
    ///
    /// A home with no sessions yet cannot answer the question from data, so the
    /// instances' pinned versions are compared instead: the same version
    /// certainly writes the same format.
    ///
    /// @param sourceInstance the instance the session belongs to
    /// @param session        the session being migrated
    /// @param targetInstance the instance receiving it
    /// @return the reason it is refused, or `null` when it is allowed
    /// @throws DshException when the target home cannot be read
    public static @Nullable String refusalReason(DshInstance sourceInstance,
                                                 DshSession session,
                                                 DshInstance targetInstance) throws DshException {
        if (sourceInstance.id().equals(targetInstance.id())) {
            return "A session cannot be migrated into its own instance";
        }
        if (session.locked()) {
            return "This session is open; close it in DeepSeek Harness before migrating it";
        }

        Path targetHome = targetInstance.homeDirectory();
        Path existing = targetHome.resolve("sessions")
                .resolve(session.workspaceSlug())
                .resolve(session.id());
        if (Files.exists(existing)) {
            return "The target instance already has a session with this id";
        }

        Set<Integer> targetVersions = formatVersions(targetHome);
        if (!targetVersions.isEmpty()) {
            if (!targetVersions.contains(session.formatVersion())) {
                return "The target instance uses session format "
                        + describe(targetVersions) + ", but this session is format v" + session.formatVersion();
            }
            return null;
        }

        if (!sourceInstance.version().equals(targetInstance.version())) {
            return "The target instance has no sessions yet, so its format is unknown; it runs DeepSeek Harness "
                    + targetInstance.version() + " while this session was written by " + sourceInstance.version();
        }
        return null;
    }

    /// Renders a set of format versions for a message.
    ///
    /// @param versions the versions
    /// @return the rendered text
    private static String describe(Set<Integer> versions) {
        StringBuilder builder = new StringBuilder();
        for (Integer version : versions) {
            if (!builder.isEmpty()) {
                builder.append(", ");
            }
            builder.append('v').append(version);
        }
        return builder.toString();
    }

    /// Migrates a session between two instances' homes.
    ///
    /// The session directory and its projection-cache row are transferred. The
    /// cache is derived data, so its absence is not an error; the durable log is
    /// required, and a session without one is not migrated.
    ///
    /// Attachment blobs live in one shared `attachments/` store per home and are
    /// **not** transferred, so an image referenced by a migrated session may no
    /// longer resolve in the target instance.
    ///
    /// @param sourceInstance the instance the session belongs to
    /// @param session        the session to migrate
    /// @param targetInstance the instance receiving it
    /// @param move           whether to delete the source afterwards
    /// @throws DshException when the migration is refused or fails
    public static void migrate(DshInstance sourceInstance,
                               DshSession session,
                               DshInstance targetInstance,
                               boolean move) throws DshException {
        String refusal = refusalReason(sourceInstance, session, targetInstance);
        if (refusal != null) {
            throw new DshException(refusal);
        }

        copyInto(sourceInstance.homeDirectory(), session, targetInstance, move);
    }

    /// Copies a session from any home into an instance.
    ///
    /// Used to take sessions from an installation the launcher does not manage —
    /// the user's own `~/.dsh` — where nothing may be written. The copy is
    /// therefore always one-way: the source is only ever read.
    ///
    /// @param sourceHome the home the session belongs to
    /// @param session    the session to copy
    /// @param target     the instance receiving it
    /// @throws DshException when the session cannot be read or copied
    public static void importFrom(Path sourceHome, DshSession session, DshInstance target)
            throws DshException {
        if (session.locked()) {
            throw new DshException("This session is open; close it in DeepSeek Harness before importing it");
        }
        Path existing = target.homeDirectory().resolve("sessions")
                .resolve(session.workspaceSlug())
                .resolve(session.id());
        if (Files.exists(existing)) {
            throw new DshException("The instance already has a session with this id");
        }
        copyInto(sourceHome, session, target, false);
    }

    /// Reads a home that the launcher does not manage.
    ///
    /// The same listing as [#list(Path)], named for the case it is used in so a
    /// reader can see that the home is a source and not a destination.
    ///
    /// @param home the home to read
    /// @return the sessions it holds, newest first
    /// @throws DshException when the home cannot be read
    public static @Unmodifiable List<DshSession> readForeignHome(Path home) throws DshException {
        return list(home);
    }

    /// Copies a session directory and its cache row into an instance.
    ///
    /// @param sourceHome the home the session belongs to
    /// @param session    the session to copy
    /// @param target     the instance receiving it
    /// @param move       whether to delete the source afterwards
    /// @throws DshException when the copy fails
    private static void copyInto(Path sourceHome, DshSession session, DshInstance target, boolean move)
            throws DshException {
        Path targetHome = target.homeDirectory();

        Path targetSessionDirectory = targetHome.resolve("sessions")
                .resolve(session.workspaceSlug())
                .resolve(session.id());

        try {
            copyTree(session.directory(), targetSessionDirectory);

            // The lock belongs to the source process, not to the migrated copy.
            Files.deleteIfExists(targetSessionDirectory.resolve("session.lock"));

            Path sourceCache = cacheFile(sourceHome, session.id());
            if (Files.isRegularFile(sourceCache)) {
                Path targetCache = cacheFile(targetHome, session.id());
                Files.createDirectories(targetCache.getParent());
                Files.copy(sourceCache, targetCache, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            // Leave nothing half-written behind: a partial session directory
            // would be listed by the target as a broken session. Cleanup
            // failures are swallowed so they cannot mask the real error.
            try {
                deleteTree(targetSessionDirectory);
                Files.deleteIfExists(cacheFile(targetHome, session.id()));
            } catch (IOException cleanupFailure) {
                LOG.warning("Failed to clean up after a failed migration", cleanupFailure);
            }
            throw new DshException("Failed to migrate the session: " + e.getMessage(), e);
        }

        if (move) {
            try {
                deleteTree(session.directory());
                Files.deleteIfExists(cacheFile(sourceHome, session.id()));
            } catch (IOException e) {
                // The copy is already in place, so this is a leftover rather than
                // a lost session.
                throw new DshException("The session was copied, but the original could not be removed: "
                        + e.getMessage(), e);
            }
        }
    }

    /// Deletes a session and its projection-cache row.
    ///
    /// The cache is derived, but it is removed with the log so the target does
    /// not keep a title for a history that no longer exists.
    ///
    /// @param instance the instance the session belongs to
    /// @param session  the session to delete
    /// @throws DshException when the session cannot be removed
    public static void delete(DshInstance instance, DshSession session) throws DshException {
        if (session.locked()) {
            throw new DshException("This session is open; close it in DeepSeek Harness before deleting it");
        }
        try {
            deleteTree(session.directory());
            Files.deleteIfExists(cacheFile(instance.homeDirectory(), session.id()));
            // Remove the workspace directory when it held nothing else, so an
            // emptied slug does not linger in the home.
            Path slug = session.directory().getParent();
            if (slug != null && Files.isDirectory(slug)) {
                try (var children = Files.list(slug)) {
                    if (children.findAny().isEmpty()) {
                        Files.deleteIfExists(slug);
                    }
                }
            }
        } catch (IOException e) {
            throw new DshException("Failed to delete the session: " + e.getMessage(), e);
        }
    }

    /// Asks the harness to work out this home's project grouping again.
    ///
    /// The grouping of a history is derived, not stored with the sessions: when a
    /// home's registry is not marked initialized, the harness rebuilds it from the
    /// sessions' own headers, creating one project per directory the conversations
    /// were started in and filing each session under the project it belongs to.
    /// That is why a home which has been run once and then imported into shows every
    /// imported conversation in the ungrouped bucket: the registry is marked
    /// initialized, so nothing goes looking at the sessions that arrived after it.
    ///
    /// Clearing the marker is therefore all an import has to do. Writing the
    /// registry ourselves is the wrong shape twice over — the grouping is the
    /// harness's to derive, and its file is validated against a schema that a
    /// launcher has no business reproducing (a registry missing one required field
    /// refuses the whole plugin tree, and the instance then cannot start at all).
    ///
    /// A home with no registry needs nothing: the harness writes one, uninitialized,
    /// the first time it runs.
    ///
    /// @param home the home whose sessions were added to
    /// @throws DshException when the registry cannot be read or written
    public static void regroup(Path home) throws DshException {
        Path registryFile = workspaceFile(home);
        if (!Files.isRegularFile(registryFile)) {
            return;
        }

        JsonObject registry;
        try {
            registry = JsonUtils.fromJsonFile(registryFile, JsonObject.class);
        } catch (Exception e) {
            throw new DshException("Failed to read the workspace list of " + home, e);
        }
        if (registry == null) {
            return;
        }

        JsonObject global = member(registry, "global") instanceof JsonObject existing ? existing : new JsonObject();
        if (!registry.has("global")) {
            registry.add("global", global);
        }
        if (global.has("initialized") && global.get("initialized").isJsonPrimitive()
                && !global.get("initialized").getAsBoolean()) {
            return;
        }
        global.addProperty("initialized", false);

        try {
            Files.createDirectories(registryFile.getParent());
            JsonUtils.writeToJsonFile(registryFile, registry);
        } catch (IOException e) {
            throw new DshException("Failed to write the workspace list of " + home, e);
        }
        LOG.info("Asked for the sessions of " + home + " to be grouped again");
    }

    /// Returns the workspace registry file of a home.
    ///
    /// @param home the `DSH_HOME`
    /// @return the file path
    private static Path workspaceFile(Path home) {
        return home.resolve("storages").resolve("workspace.json");
    }

    /// Returns a member of an object, or `null` when it has none.
    ///
    /// @param object the object, or `null`
    /// @param name   the member's name
    /// @return the member, or `null`
    private static @Nullable JsonElement member(@Nullable JsonElement object, String name) {
        if (object == null || !object.isJsonObject()) {
            return null;
        }
        JsonObject holder = object.getAsJsonObject();
        return holder.has(name) ? holder.get(name) : null;
    }

    /// Returns the projection-cache file for a session.
    ///
    /// @param home      the `DSH_HOME`
    /// @param sessionId the session id
    /// @return the cache file path
    private static Path cacheFile(Path home, String sessionId) {
        return home.resolve("storages").resolve("session_projcache").resolve("sessions")
                .resolve(sessionId + ".json");
    }

    /// Copies a directory tree.
    ///
    /// @param source      the directory to copy
    /// @param destination the destination
    /// @throws IOException when the copy fails
    private static void copyTree(Path source, Path destination) throws IOException {
        Files.createDirectories(destination);
        Files.walkFileTree(source, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path directory, BasicFileAttributes attributes)
                    throws IOException {
                Files.createDirectories(destination.resolve(source.relativize(directory).toString()));
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) throws IOException {
                Files.copy(file, destination.resolve(source.relativize(file).toString()),
                        StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    /// Deletes a directory tree, ignoring a missing root.
    ///
    /// @param directory the directory to delete
    /// @throws IOException when the deletion fails
    static void deleteTree(Path directory) throws IOException {
        if (!Files.exists(directory)) {
            return;
        }
        try (Stream<Path> paths = Files.walk(directory)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        }
    }

    /// Reads the projection-cache row for a session.
    ///
    /// @param home      the `DSH_HOME`
    /// @param sessionId the session id
    /// @return the row, or `null` when there is none or it is unreadable
    private static @Nullable CacheRow readCache(Path home, String sessionId) {
        Path file = cacheFile(home, sessionId);
        if (!Files.isRegularFile(file)) {
            return null;
        }
        try {
            JsonElement parsed = JsonParser.parseString(Files.readString(file));
            if (!parsed.isJsonObject()) {
                return null;
            }
            JsonObject record = parsed.getAsJsonObject().getAsJsonObject("record");
            if (record == null) {
                return null;
            }

            String cwd = null;
            long createdAt = 0L;
            JsonObject identity = record.getAsJsonObject("identity");
            if (identity != null) {
                JsonElement cwdElement = identity.get("cwd");
                if (cwdElement != null && cwdElement.isJsonPrimitive()) {
                    cwd = cwdElement.getAsString();
                }
                JsonElement createdAtElement = identity.get("createdAt");
                if (createdAtElement != null && createdAtElement.isJsonPrimitive()) {
                    createdAt = createdAtElement.getAsLong();
                }
            }

            return new CacheRow(cwd, titleOf(record), createdAt);
        } catch (IOException | RuntimeException e) {
            LOG.info("Unreadable session cache row for " + sessionId + ": " + e);
            return null;
        }
    }

    /// Extracts the title from a projection-cache record.
    ///
    /// The title is a projected row, so it is read from `rows.title.val` and
    /// only when that value is already a plain string.
    ///
    /// @param record the cache record
    /// @return the title, or `null` when the row is absent or still structured
    private static @Nullable String titleOf(JsonObject record) {
        JsonObject rows = record.getAsJsonObject("rows");
        if (rows == null) {
            return null;
        }
        JsonObject title = rows.getAsJsonObject("title");
        if (title == null) {
            return null;
        }
        JsonElement value = title.get("val");
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
            return null;
        }
        String text = value.getAsString().trim();
        return text.isEmpty() ? null : text;
    }

    /// Formats a byte count for display.
    ///
    /// @param bytes the byte count
    /// @return the formatted text
    public static String formatSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        if (bytes < 1024 * 1024) {
            return String.format(Locale.ROOT, "%.1f KiB", bytes / 1024.0);
        }
        return String.format(Locale.ROOT, "%.1f MiB", bytes / (1024.0 * 1024.0));
    }

    /// The parts of a projection-cache row the launcher uses.
    ///
    /// @param cwd       the working directory the session was created in
    /// @param title     the projected title
    /// @param createdAt the creation time, in epoch milliseconds
    private record CacheRow(@Nullable String cwd, @Nullable String title, long createdAt) {
    }
}
