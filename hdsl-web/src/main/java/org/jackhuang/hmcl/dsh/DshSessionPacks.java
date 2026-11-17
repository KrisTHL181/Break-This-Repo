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

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static org.jackhuang.hmcl.util.logging.Logger.LOG;

/// Moves a set of conversations between installations as one file.
///
/// A session is a directory holding a self-describing log, and the harness can
/// read one from any home it is put in — but only if three things hold, and all
/// three fail loudly rather than degrading: the directory's name must be the one
/// the log's own working directory identifies, the log's compression must match
/// what the home is configured for, and the log's first compressed frame must be
/// the header line by itself. A pack therefore copies the logs **byte for byte**
/// and never re-encodes them, keeps each session's directory name exactly as it
/// was, and records the working directory the sessions expect so that a pack
/// arriving somewhere else can say so before it is unpacked.
///
/// What a session needs besides its log is the attachment store: images and files
/// a conversation referred to are kept once per home, by content, and a session
/// whose attachments did not come along shows broken references. Those are copied
/// too — the ones the sessions name, when `zstd` is available to read the logs, or
/// the whole store when it is not, because a pack that silently loses images is
/// worse than a large one.
///
/// The projection cache is carried when it exists, although it is derived: it
/// holds the title a conversation is listed under, and a pack that arrives with
/// its titles is a pack that looks like what it came from. A target home refuses a
/// cache row it does not recognise rather than trusting it, so carrying one is
/// never a risk to the harness.
@NotNullByDefault
public final class DshSessionPacks {
    /// What a pack says it is, so that it is not mistaken for anything else.
    public static final String FORMAT = "hdsl-session-pack";

    /// The file extension a session pack is written with.
    ///
    /// `sspack` reads as "session pack", and a name of its own is what keeps a session pack from
    /// looking like any other archive — it is a record of conversations, not a set of files.
    ///
    /// @see DshModpacks#FILE_EXTENSION for why a launcher's own packs do not use `.zip`
    public static final String FILE_EXTENSION = ".sspack";

    /// The extensions an import accepts, for the reason [DshModpacks#ACCEPTED_EXTENSIONS] gives.
    public static final java.util.List<String> ACCEPTED_EXTENSIONS = java.util.List.of(".sspack", ".zip");

    /// The pack format's version.
    public static final int FORMAT_VERSION = 1;

    /// The manifest's name inside the archive.
    public static final String MANIFEST = "manifest.json";

    /// Where the sessions live inside the archive.
    private static final String SESSIONS = "sessions/";

    /// Where the attachment store lives inside the archive.
    private static final String ATTACHMENTS = "attachments/";

    /// Where the projection cache lives inside the archive.
    private static final String PROJCACHE = "projcache/";

    /// A content-addressed object's name: the harness names attachments by their
    /// sha-256, which is what makes copying the ones a session names possible at
    /// all.
    private static final Pattern OBJECT_ID = Pattern.compile("\\b[0-9a-f]{64}\\b");

    /// The directory names inside an attachment store that hold objects.
    private static final List<String> STORES = List.of("objects", "file-objects", "request-images");

    private DshSessionPacks() {
    }

    /// One conversation in a pack.
    ///
    /// @param id            the session id
    /// @param slug          the project directory the session lives in
    /// @param cwd           the working directory the session was recorded in
    /// @param formatVersion the log's format generation
    /// @param title         the title the cache carried, or `null`
    /// @param sizeBytes     the size of the session's logs
    public record Entry(String id, String slug, @Nullable String cwd, int formatVersion,
                        @Nullable String title, long sizeBytes) {
    }

    /// What a pack holds.
    ///
    /// @param format          the format identifier
    /// @param version         the format version
    /// @param createdAt       when the pack was written
    /// @param instanceId      the instance the sessions came from
    /// @param dshVersion      the DeepSeek Harness version that wrote them
    /// @param workingDirectory the working directory the sessions expect, or `null`
    /// @param sessions        the sessions
    /// @param attachments     whether the referenced attachments, or all of them, came along
    /// @param attachmentCount how many attachment objects the pack holds
    public record Manifest(String format, int version, String createdAt, String instanceId,
                           String dshVersion, @Nullable String workingDirectory, List<Entry> sessions,
                           String attachments, int attachmentCount) {
    }

    /// What an export wrote.
    ///
    /// @param sessions    how many sessions were written
    /// @param attachments how many attachment objects were written
    /// @param bytes       the size of the pack
    public record ExportResult(int sessions, int attachments, long bytes) {
    }

    /// What an import read.
    ///
    /// @param imported    how many sessions were added
    /// @param skipped     how many were already present
    /// @param attachments how many attachment objects were added
    /// @param manifest    the pack's manifest
    public record ImportResult(int imported, int skipped, int attachments, Manifest manifest) {
    }

    /// Writes the given sessions into a pack.
    ///
    /// @param instance the instance whose sessions they are
    /// @param sessions the sessions to write
    /// @param target  the archive to create
    /// @param onStage receives progress lines, or `null`
    /// @return what was written
    /// @throws DshException when a session cannot be read or the pack cannot be written
    public static ExportResult export(DshInstance instance, List<DshSession> sessions, Path target,
                                      @Nullable Consumer<String> onStage) throws DshException {
        Path home = instance.homeDirectory();
        if (sessions.isEmpty()) {
            throw new DshException("There are no sessions to write into a pack");
        }

        List<DshSession> ordered = sessions.stream()
                .sorted(Comparator.comparing(DshSession::id))
                .toList();

        // The attachments first, because whether they are the referenced ones or
        // all of them is something the manifest has to say.
        Set<String> referenced = referencedAttachments(home, ordered);
        List<Path> attachments = referenced == null
                ? allAttachments(home)
                : referenced.stream()
                        .map(id -> findAttachment(home, id))
                        .filter(java.util.Objects::nonNull)
                        .toList();

        report(onStage, "Writing " + ordered.size() + " session(s)");
        Path parent = target.toAbsolutePath().getParent();
        try {
            if (parent != null) {
                Files.createDirectories(parent);
            }
        } catch (IOException e) {
            throw new DshException("Failed to prepare " + target, e);
        }

        List<Entry> entries = new ArrayList<>();
        long bytes = 0;
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(target))) {
            entries.addAll(writeInto(zip, home, ordered, onStage));
            for (Path attachment : attachments) {
                copyInto(zip, attachment, ATTACHMENTS + relativeToStore(home, attachment));
            }

            Manifest manifest = new Manifest(FORMAT, FORMAT_VERSION, Instant.now().toString(),
                    instance.id(), instance.version(), commonWorkingDirectory(entries), List.copyOf(entries),
                    referenced == null ? "all" : "referenced", attachments.size());
            zip.putNextEntry(new ZipEntry(MANIFEST));
            zip.write(JsonUtils.GSON.toJson(manifest).getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        } catch (IOException e) {
            throw new DshException("Failed to write " + target, e);
        }

        long size = sizeOf(target);
        report(onStage, "Wrote " + entries.size() + " session(s) and " + attachments.size()
                + " attachment(s), " + (size / 1024 / 1024) + " MiB");
        LOG.info("Wrote a session pack of " + entries.size() + " session(s) to " + target);
        return new ExportResult(entries.size(), attachments.size(), size);
    }

    /// Writes sessions into an archive that is already open.
    ///
    /// The layout is the one a session pack uses, which is also the one a home uses:
    /// a modpack that carries conversations carries exactly these entries, and
    /// whoever reads them applies the same rules either way — a log is copied byte
    /// for byte, named as the harness names it, and put in the directory its own
    /// working directory identifies.
    ///
    /// @param zip      the archive
    /// @param home     the `DSH_HOME` they live in
    /// @param sessions the sessions, in the order to write them
    /// @param onStage  receives progress lines, or `null`
    /// @return the entries describing what was written
    /// @throws DshException  when a session holds no log
    /// @throws IOException   when the archive cannot be written
    public static List<Entry> writeInto(ZipOutputStream zip, Path home, List<DshSession> sessions,
                                        @Nullable Consumer<String> onStage) throws DshException, IOException {
        List<Entry> entries = new ArrayList<>();
        for (DshSession session : sessions) {
            Path slugDirectory = session.directory().getParent();
            String slug = slugDirectory.getFileName().toString();
            long size = 0;
            for (Path log : logsOf(session.directory())) {
                String name = SESSIONS + slug + "/" + session.id() + "/" + log.getFileName();
                size += copyInto(zip, log, name);
            }
            if (size == 0) {
                throw new DshException("Session " + session.id() + " holds no log to write");
            }

            Path cache = cacheFile(home, session.id());
            if (cache != null) {
                copyInto(zip, cache, PROJCACHE + "sessions/" + session.id() + ".json");
            }

            entries.add(new Entry(session.id(), slug, session.workingDirectory(),
                    session.formatVersion(), session.title(), size));
        }
        report(onStage, "Wrote " + entries.size() + " session(s)");
        return entries;
    }

    /// Writes the attachments the given sessions name into an archive that is
    /// already open.
    ///
    /// @param zip      the archive
    /// @param home     the `DSH_HOME`
    /// @param sessions the sessions
    /// @param onStage  receives progress lines, or `null`
    /// @return how many attachment objects were written
    /// @throws IOException when the archive cannot be written
    public static int writeAttachmentsInto(ZipOutputStream zip, Path home, List<DshSession> sessions,
                                           @Nullable Consumer<String> onStage) throws IOException {
        Set<String> referenced = referencedAttachments(home, sessions);
        List<Path> attachments = referenced == null
                ? allAttachments(home)
                : referenced.stream().map(id -> findAttachment(home, id))
                        .filter(java.util.Objects::nonNull).toList();
        for (Path attachment : attachments) {
            copyInto(zip, attachment, ATTACHMENTS + relativeToStore(home, attachment));
        }
        report(onStage, "Wrote " + attachments.size() + " attachment(s)");
        return attachments.size();
    }

    /// Reads a pack into a home.
    ///
    /// Sessions the home already has are left alone: a pack is a way of bringing
    /// conversations somewhere, and re-importing one that is already there must
    /// not touch it — the harness would refuse a second log for one id, and the
    /// right answer is to keep the one in place.
    ///
    /// @param home   the `DSH_HOME` to read into
    /// @param pack   the archive to read
    /// @param onStage receives progress lines, or `null`
    /// @return what was read
    /// @throws DshException when the pack is not a pack, or cannot be read
    public static ImportResult importFrom(Path home, Path pack, @Nullable Consumer<String> onStage)
            throws DshException {
        Manifest manifest = readManifest(pack);
        if (!FORMAT.equals(manifest.format())) {
            throw new DshException("This file is not a session pack (" + manifest.format() + ")");
        }
        if (manifest.version() > FORMAT_VERSION) {
            throw new DshException("This pack was written by a newer launcher (format "
                    + manifest.version() + "), which this one cannot read");
        }

        warnAboutWorkingDirectory(manifest);

        Set<String> held = new LinkedHashSet<>(DshSessions.list(home).stream().map(DshSession::id).toList());
        List<Entry> wanted = manifest.sessions().stream().filter(entry -> !held.contains(entry.id())).toList();

        report(onStage, "Reading " + wanted.size() + " session(s)");
        int imported;
        int attachments;
        try (ZipInputStream zip = new ZipInputStream(Files.newInputStream(pack))) {
            imported = copyEntries(zip, home, wanted);
            attachments = copiedAttachments;
        } catch (IOException e) {
            throw new DshException("Failed to read " + pack, e);
        }

        // The grouping is the harness's to derive from the sessions' own headers;
        // asking for it again is what files the imported conversations under their
        // projects instead of leaving them ungrouped.
        if (imported > 0) {
            DshSessions.regroup(home);
        }

        report(onStage, "Imported " + imported + " session(s), skipped " + (manifest.sessions().size() - imported)
                + " already present");
        LOG.info("Imported " + imported + " session(s) from " + pack);
        return new ImportResult(imported, manifest.sessions().size() - imported, attachments, manifest);
    }


    /// Reads sessions out of an archive that holds them beside something else.
    ///
    /// A modpack carries a conversation history the same way a session pack does —
    /// the same entries, the same rules, including the one that a log is never
    /// written into a directory its own working directory does not identify — and it
    /// does not carry a session manifest, because it has one of its own. What is
    /// written is therefore everything the archive holds that the home does not
    /// already have.
    ///
    /// @param archive the archive
    /// @param home    the `DSH_HOME` to read into
    /// @param onStage receives progress lines, or `null`
    /// @return how many sessions were restored
    /// @throws DshException when the archive cannot be read
    public static int restoreInto(Path archive, Path home, @Nullable Consumer<String> onStage) throws DshException {
        int imported;
        try (ZipInputStream zip = new ZipInputStream(Files.newInputStream(archive))) {
            imported = copyEntries(zip, home, null);
        } catch (IOException e) {
            throw new DshException("Failed to read " + archive, e);
        }
        if (imported > 0) {
            DshSessions.regroup(home);
        }
        report(onStage, "Restored " + imported + " session(s) from the pack");
        return imported;
    }

    /// How many attachment objects the last copy added.
    private static int copiedAttachments;

    /// Copies the session, cache and attachment entries of an open archive.
    ///
    /// @param zip    the archive
    /// @param home   the `DSH_HOME` to read into
    /// @param wanted the sessions to take, or `null` to take every one the archive
    ///               holds that the home does not already have
    /// @return how many session logs were written
    /// @throws IOException when the archive cannot be read
    private static int copyEntries(ZipInputStream zip, Path home, @Nullable List<Entry> wanted) throws IOException {
        Set<String> wantedIds = wanted == null
                ? Set.of()
                : new LinkedHashSet<>(wanted.stream().map(Entry::id).toList());
        int imported = 0;
        copiedAttachments = 0;

        ZipEntry entry;
        while ((entry = zip.getNextEntry()) != null) {
            String name = entry.getName();
            if (entry.isDirectory()) {
                continue;
            }

            if (name.startsWith(SESSIONS)) {
                String[] parts = name.substring(SESSIONS.length()).split("/");
                // A pack is an archive somebody may have edited, and every part of this path is
                // interpolated into a `resolve`. A member named `sessions/../planted/session.jsonl`
                // leaves the sessions directory and lands in the home — the prefix is matched before
                // the path is resolved, and `..` is a legal element. Each part is checked on its own
                // because any one of them is the one that can escape.
                // An archive is something somebody may have edited, and every part of this path is
                // interpolated into a `resolve`. A member named `sessions/../planted/session.jsonl`
                // passes the prefix test and is a legal path, so it leaves the sessions directory
                // and lands in the home — anywhere the archive says. Each part is checked on its
                // own, because any one of them is the one that can climb out.
                if (parts.length != 3
                        || !safeRelative(parts[0]) || !safeRelative(parts[1]) || !safeRelative(parts[2])) {
                    LOG.warning("Skipping " + name + " in a session pack: unsafe path");
                    continue;
                }
                if (wanted != null) {
                    if (!wantedIds.contains(parts[1])) {
                        continue;
                    }
                    String recorded = slugOf(wanted, parts[1]);
                    if (recorded == null || !recorded.equals(parts[0])) {
                        // The harness matches a log's header against the directory it
                        // is in and refuses the whole home when they disagree, so a
                        // session is only written where the manifest says it came from.
                        LOG.warning("Skipping " + name + " in a session pack: the manifest puts session "
                                + parts[1] + " in " + recorded + ", not " + parts[0]);
                        continue;
                    }
                }
                if (!DshSessions.isLogName(parts[2])) {
                    LOG.warning("Skipping " + name + " in a session pack: not a session log name");
                    continue;
                }
                if (!matchesCompression(home, parts[2])) {
                    LOG.warning("Skipping " + name + " in a session pack: this home reads "
                            + (expectsCompressed(home) ? "compressed" : "uncompressed") + " logs");
                    continue;
                }
                Path target = home.resolve("sessions").resolve(parts[0]).resolve(parts[1]).resolve(parts[2]);
                boolean fresh = !Files.exists(target);
                Files.createDirectories(target.getParent());
                write(zip, target);
                if (fresh) {
                    imported++;
                }
            } else if (name.startsWith(PROJCACHE)) {
                String file = name.substring(PROJCACHE.length());
                if (!file.startsWith("sessions/") || !file.endsWith(".json")) {
                    continue;
                }
                String id = file.substring("sessions/".length(), file.length() - ".json".length());
                if (!safeRelative(id)) {
                    LOG.warning("Skipping " + name + " in a session pack: unsafe path");
                    continue;
                }
                if (!wantedIds.isEmpty() && !wantedIds.contains(id)) {
                    continue;
                }
                Path target = home.resolve("storages").resolve("session_projcache").resolve(file);
                Files.createDirectories(target.getParent());
                write(zip, target);
            } else if (name.startsWith(ATTACHMENTS)) {
                String relative = name.substring(ATTACHMENTS.length());
                if (!safeRelative(relative)) {
                    LOG.warning("Skipping " + name + " in a session pack: unsafe path");
                    continue;
                }
                Path target = home.resolve("attachments").resolve(relative);
                if (Files.exists(target)) {
                    continue;
                }
                Files.createDirectories(target.getParent());
                write(zip, target);
                copiedAttachments++;
            }
        }
        return imported;
    }

    /// Reads a pack's manifest.
    ///
    /// @param pack the archive
    /// @return the manifest
    /// @throws DshException when the archive holds none
    public static Manifest readManifest(Path pack) throws DshException {
        try (ZipInputStream zip = new ZipInputStream(Files.newInputStream(pack))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (MANIFEST.equals(entry.getName())) {
                    String body = new String(zip.readAllBytes(), StandardCharsets.UTF_8);
                    JsonElement parsed = JsonParser.parseString(body);
                    if (!parsed.isJsonObject()) {
                        throw new DshException("The pack's manifest is not an object");
                    }
                    return manifestOf(parsed.getAsJsonObject());
                }
            }
        } catch (IOException e) {
            throw new DshException("Failed to read " + pack, e);
        }
        throw new DshException("This file holds no session pack manifest");
    }

    /// Reads a manifest object.
    ///
    /// @param root the manifest
    /// @return the manifest record
    private static Manifest manifestOf(JsonObject root) {
        List<Entry> entries = new ArrayList<>();
        JsonElement sessions = root.get("sessions");
        if (sessions != null && sessions.isJsonArray()) {
            for (JsonElement element : sessions.getAsJsonArray()) {
                if (!element.isJsonObject()) {
                    continue;
                }
                JsonObject object = element.getAsJsonObject();
                String id = string(object, "id");
                String slug = string(object, "slug");
                if (id == null || slug == null) {
                    continue;
                }
                entries.add(new Entry(id, slug, string(object, "cwd"), integer(object, "formatVersion"),
                        string(object, "title"), integer(object, "sizeBytes")));
            }
        }

        return new Manifest(
                string(root, "format") == null ? "" : string(root, "format"),
                integer(root, "version"),
                string(root, "createdAt") == null ? "" : string(root, "createdAt"),
                string(root, "instanceId") == null ? "" : string(root, "instanceId"),
                string(root, "dshVersion") == null ? "" : string(root, "dshVersion"),
                string(root, "workingDirectory"),
                List.copyOf(entries),
                string(root, "attachments") == null ? "unknown" : string(root, "attachments"),
                integer(root, "attachmentCount"));
    }

    /// Says so when a pack's sessions were recorded somewhere else.
    ///
    /// This is a warning rather than a refusal: a session whose working directory
    /// does not exist still lists and still opens, it simply belongs to no
    /// project, and resuming it needs that exact directory. Saying which
    /// directory it was is the useful part.
    ///
    /// @param manifest the pack's manifest
    private static void warnAboutWorkingDirectory(Manifest manifest) {
        String cwd = manifest.workingDirectory();
        if (cwd == null || cwd.isBlank()) {
            return;
        }
        if (!Files.isDirectory(Path.of(cwd))) {
            LOG.warning("The pack's sessions were recorded in " + cwd
                    + ", which does not exist here: they will list ungrouped and cannot be resumed");
        }
    }

    /// Returns the attachment ids the given sessions name, or `null` when the
    /// logs cannot be read here.
    ///
    /// A log is compressed, and this launcher has no decompressor of its own; the
    /// `zstd` command is used when it is installed, which is the honest way to ask
    /// rather than to guess at the compressed bytes. When it is not, the caller
    /// takes the whole store instead.
    ///
    /// @param home     the `DSH_HOME`
    /// @param sessions the sessions
    /// @return the ids, or `null` when they cannot be determined
    private static @Nullable Set<String> referencedAttachments(Path home, List<DshSession> sessions) {
        Path zstd = org.jackhuang.hmcl.util.platform.SystemUtils.which("zstd");
        if (zstd == null) {
            // Without a decompressor the referenced ids of a compressed log cannot be read, and a
            // log that cannot be read is not a log with no attachments. Returning null says "not
            // known", which the caller answers by carrying the whole store — the safe direction,
            // because a pack that carries too much still restores, and one that carries too little
            // loses pictures with nothing to say so.
            LOG.info("zstd is not installed, so a session pack carries the whole attachment store");
            return null;
        }

        Set<String> ids = new LinkedHashSet<>();
        for (DshSession session : sessions) {
            for (Path log : logsOf(session.directory())) {
                // A home may hold either spelling — the harness decides per root — and this used to
                // read only the compressed one. An uncompressed log was skipped, so the ids it named
                // were never collected and its attachments were left out of the pack **silently**:
                // the export reported success and the pictures were simply gone at the other end.
                // Which spelling a log has says nothing about whether it names attachments.
                boolean compressed = log.getFileName().toString().endsWith(".zstd");
                try {
                    String text;
                    if (compressed) {
                        List<String> output = new ArrayList<>();
                        int exit = DshCommand.run(List.of(zstd.toString(), "-d", "-c", log.toString()),
                                null, output::add).exitCode();
                        if (exit != 0) {
                            LOG.warning("zstd could not read " + log + ", so the whole store is carried");
                            return null;
                        }
                        text = String.join("\n", output);
                    } else {
                        text = Files.readString(log, java.nio.charset.StandardCharsets.UTF_8);
                    }
                    Matcher matcher = OBJECT_ID.matcher(text);
                    while (matcher.find()) {
                        ids.add(matcher.group());
                    }
                } catch (IOException e) {
                    LOG.warning("Failed to read " + log + ", so the whole store is carried", e);
                    return null;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return null;
                }
            }
        }
        return ids;
    }

    /// Returns every object in a home's attachment store.
    ///
    /// @param home the `DSH_HOME`
    /// @return the files
    private static List<Path> allAttachments(Path home) {
        Path store = home.resolve("attachments");
        if (!Files.isDirectory(store)) {
            return List.of();
        }
        List<Path> files = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(store)) {
            walk.filter(Files::isRegularFile)
                    .filter(path -> OBJECT_ID.matcher(path.getFileName().toString()).matches())
                    .forEach(files::add);
        } catch (IOException e) {
            LOG.warning("Failed to list the attachment store", e);
        }
        return files;
    }

    /// Returns the store file holding one object, if there is one.
    ///
    /// @param home the `DSH_HOME`
    /// @param id   the object's id
    /// @return the file, or `null` when the store does not hold it
    private static @Nullable Path findAttachment(Path home, String id) {
        for (String store : STORES) {
            Path file = home.resolve("attachments").resolve("v1").resolve(store)
                    .resolve(id.substring(0, 2)).resolve(id);
            if (Files.isRegularFile(file)) {
                return file;
            }
        }
        return null;
    }

    /// Returns a store file's path relative to the store, which is how it is
    /// named inside a pack.
    ///
    /// @param home the `DSH_HOME`
    /// @param file the store file
    /// @return the relative path, with forward slashes
    private static String relativeToStore(Path home, Path file) {
        Path store = home.resolve("attachments");
        String relative = store.relativize(file).toString();
        return relative.replace('\\', '/');
    }

    /// Reports whether a home's sessions are compressed.
    ///
    /// The harness decides this per root and refuses a root that mixes the two, so
    /// a pack has to write the spelling the home already uses. A home with no
    /// sessions yet is taken to be compressed, which is the harness's own default.
    ///
    /// @param home the `DSH_HOME`
    /// @return whether the home reads compressed logs
    private static boolean expectsCompressed(Path home) {
        Path root = home.resolve("sessions");
        if (!Files.isDirectory(root)) {
            return true;
        }
        try (Stream<Path> walk = Files.walk(root, 3)) {
            for (Path file : walk.filter(Files::isRegularFile).toList()) {
                String name = file.getFileName().toString();
                if (DshSessions.isLogName(name)) {
                    return name.endsWith(".zstd");
                }
            }
        } catch (IOException e) {
            LOG.warning("Failed to look at the existing sessions of " + home, e);
        }
        return true;
    }

    /// Reports whether a log name is written the way a home reads them.
    ///
    /// @param home     the `DSH_HOME`
    /// @param fileName the log's name
    /// @return whether the compression matches
    private static boolean matchesCompression(Path home, String fileName) {
        boolean compressed = fileName.endsWith(".zstd");
        return compressed == expectsCompressed(home);
    }

    /// Returns the project directory a pack's manifest records for a session.
    ///
    /// @param entries the pack's sessions
    /// @param id      the session id
    /// @return the directory, or `null` when the manifest does not describe it
    private static @Nullable String slugOf(List<Entry> entries, String id) {
        for (Entry entry : entries) {
            if (entry.id().equals(id)) {
                return entry.slug();
            }
        }
        return null;
    }

    /// Returns the cache file of a session, if the home has one.
    ///
    /// @param home the `DSH_HOME`
    /// @param id   the session id
    /// @return the file, or `null`
    private static @Nullable Path cacheFile(Path home, String id) {
        Path file = home.resolve("storages").resolve("session_projcache").resolve("sessions").resolve(id + ".json");
        return Files.isRegularFile(file) ? file : null;
    }

    /// Returns the canonical logs in a session directory, oldest generation first.
    ///
    /// Every generation is carried, not only the newest: a directory can hold two
    /// after a session was continued, and which one a future harness reads is not
    /// this launcher's decision to make.
    ///
    /// @param directory the session directory
    /// @return the logs
    private static List<Path> logsOf(Path directory) {
        try (Stream<Path> files = Files.list(directory)) {
            return files.filter(Files::isRegularFile)
                    .filter(path -> DshSessions.isLogName(path.getFileName().toString()))
                    .sorted()
                    .toList();
        } catch (IOException e) {
            return List.of();
        }
    }

    /// Returns the working directory every session in a pack shares, if they share
    /// one.
    ///
    /// @param entries the pack's sessions
    /// @return the directory, or `null` when they do not share one
    private static @Nullable String commonWorkingDirectory(List<Entry> entries) {
        String common = null;
        for (Entry entry : entries) {
            String cwd = entry.cwd();
            if (cwd == null) {
                return null;
            }
            if (common == null) {
                common = cwd;
            } else if (!common.equals(cwd)) {
                return null;
            }
        }
        return common;
    }

    /// Copies a file into the archive.
    ///
    /// @param zip  the archive
    /// @param file the file
    /// @param name the entry's name
    /// @return the file's size
    /// @throws IOException when the file cannot be read or written
    private static long copyInto(ZipOutputStream zip, Path file, String name) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        try (InputStream input = Files.newInputStream(file)) {
            input.transferTo(zip);
        }
        zip.closeEntry();
        return Files.size(file);
    }

    /// Writes one archive entry to a file.
    ///
    /// @param zip    the archive
    /// @param target the file
    /// @throws IOException when the file cannot be written
    private static void write(ZipInputStream zip, Path target) throws IOException {
        Path staging = target.resolveSibling(target.getFileName() + ".importing");
        try (java.io.OutputStream output = Files.newOutputStream(staging)) {
            zip.transferTo(output);
        }
        Files.move(staging, target, StandardCopyOption.REPLACE_EXISTING);
    }

    /// Reports whether a path from an archive stays inside the store.
    ///
    /// @param relative the path
    /// @return whether it is safe to resolve
    private static boolean safeRelative(String relative) {
        if (relative.isBlank() || relative.startsWith("/") || relative.contains("\\")) {
            return false;
        }
        if (relative.equals(".") || relative.equals("..")) {
            return false;
        }
        for (String part : relative.split("/")) {
            if (part.isEmpty() || part.equals(".") || part.equals("..")) {
                return false;
            }
        }
        return true;
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

    /// Returns a string member, or `null`.
    ///
    /// @param object the object
    /// @param name   the member
    /// @return the string, or `null`
    private static @Nullable String string(JsonObject object, String name) {
        JsonElement element = object.get(name);
        return element == null || !element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()
                ? null : element.getAsString();
    }

    /// Returns an integer member, or zero.
    ///
    /// @param object the object
    /// @param name   the member
    /// @return the number, or zero
    private static int integer(JsonObject object, String name) {
        JsonElement element = object.get(name);
        if (element == null || !element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber()) {
            return 0;
        }
        return element.getAsInt();
    }
}
