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
package org.jackhuang.hmcl.web.transcode.media;

import com.google.gson.JsonObject;
import org.jackhuang.hmcl.web.transcode.runtime.TranscodeRuntime;
import org.jetbrains.annotations.NotNullByDefault;

import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/// Lists and probes files below the configured media root.
///
/// Every path is resolved with [Path#toRealPath()] and required to stay below the root,
/// so `..`, absolute paths and symlinks that escape the share are all refused at one
/// place. The browser shows directories and media files; other files are still listed
/// as `other` when the caller explicitly asks for a directory listing, which keeps a
/// user from thinking a file disappeared.
@NotNullByDefault
public final class MediaBrowser {

    private static final Set<String> VIDEO = Set.of(
            "mp4", "mkv", "mov", "avi", "flv", "ts", "m2ts", "webm", "wmv", "mpg", "mpeg", "m4v", "3gp", "ogv", "mxf");
    private static final Set<String> AUDIO = Set.of(
            "mp3", "aac", "flac", "wav", "m4a", "ogg", "opus", "wma", "ac3", "eac3", "dts", "aiff", "ape");
    private static final Set<String> SUBTITLE = Set.of("srt", "ass", "ssa", "vtt", "sub", "sup", "smi");
    private static final Set<String> IMAGE = Set.of("jpg", "jpeg", "png", "webp", "bmp", "tif", "tiff", "avif", "jxl");

    private final TranscodeRuntime runtime;

    public MediaBrowser(TranscodeRuntime runtime) {
        this.runtime = runtime;
    }

    /// A page of entries under `relative` (empty means the root itself).
    public Listing list(Path root, String relative, int page, int pageSize) throws IOException {
        int size = Math.max(1, Math.min(500, pageSize));
        int index = Math.max(0, page);
        Path base = realRoot(root);
        Path directory = resolve(base, relative);
        if (!Files.isDirectory(directory)) {
            throw new IOException("not a directory: " + relative);
        }
        List<Entry> entries = new ArrayList<>();
        try (var stream = Files.list(directory)) {
            for (Path child : (Iterable<Path>) stream::iterator) {
                String name = child.getFileName().toString();
                boolean isDirectory = Files.isDirectory(child);
                long fileSize = isDirectory ? -1 : safeSize(child);
                FileTime modified = safeModified(child);
                entries.add(new Entry(name,
                        relativeOf(base, child),
                        isDirectory,
                        fileSize,
                        modified.toInstant().toString(),
                        isDirectory ? "directory" : kind(name)));
            }
        }
        entries.sort(Comparator
                .comparing((Entry entry) -> !entry.directory())
                .thenComparing(Entry::name, String.CASE_INSENSITIVE_ORDER));
        int from = Math.min(entries.size(), index * size);
        int to = Math.min(entries.size(), from + size);
        return new Listing(relativeOf(base, directory), from, entries.size(), List.copyOf(entries.subList(from, to)));
    }

    /// Recursively lists video/audio files below a directory, for "add folder" in the
    /// preparation list. Hidden directories, recycle bins and symlinks escaping the root
    /// are skipped. `limit` caps the result so one huge library does not freeze the page.
    public Scan scan(Path root, String relative, boolean recursive, int limit) throws IOException {
        Path base = realRoot(root);
        Path directory = resolve(base, relative);
        if (!Files.isDirectory(directory)) {
            throw new IOException("not a directory: " + relative);
        }
        int max = Math.max(1, Math.min(5000, limit));
        List<Entry> entries = new ArrayList<>();
        boolean[] truncated = {false};
        int depth = recursive ? Integer.MAX_VALUE : 1;
        Files.walkFileTree(directory, EnumSet.noneOf(FileVisitOption.class), depth,
                new SimpleFileVisitor<>() {
                    @Override
                    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                        if (!dir.equals(directory)) {
                            String name = dir.getFileName().toString();
                            if (name.startsWith(".") || "@eaDir".equals(name) || "#recycle".equals(name)) {
                                return FileVisitResult.SKIP_SUBTREE;
                            }
                        }
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                        if (!attrs.isRegularFile()) {
                            return FileVisitResult.CONTINUE;
                        }
                        String name = file.getFileName().toString();
                        if (name.startsWith(".")) {
                            return FileVisitResult.CONTINUE;
                        }
                        String kind = kind(name);
                        if (!"video".equals(kind) && !"audio".equals(kind)) {
                            return FileVisitResult.CONTINUE;
                        }
                        if (entries.size() >= max) {
                            truncated[0] = true;
                            return FileVisitResult.TERMINATE;
                        }
                        try {
                            Path real = file.toRealPath();
                            if (!real.startsWith(base)) {
                                return FileVisitResult.CONTINUE;
                            }
                            entries.add(new Entry(name, relativeOf(base, real), false, safeSize(real),
                                    safeModified(real).toInstant().toString(), kind));
                        } catch (IOException ignored) {
                            // A disappearing file is not a reason to fail the whole scan.
                        }
                        return FileVisitResult.CONTINUE;
                    }
                });
        entries.sort(Comparator.comparing(Entry::path, String.CASE_INSENSITIVE_ORDER));
        return new Scan(relativeOf(base, directory), entries.size(), truncated[0], List.copyOf(entries));
    }

    /// Probes one file and returns ffprobe's JSON, with the relative path attached.
    public JsonObject probe(Path root, String relative) throws IOException, InterruptedException {
        Path base = realRoot(root);
        Path file = resolve(base, relative);
        if (!Files.isRegularFile(file)) {
            throw new IOException("not a file: " + relative);
        }
        JsonObject result = runtime.probeMedia(file);
        result.addProperty("relativePath", relativeOf(base, file));
        return result;
    }

    /// Resolves a relative path inside the root, refusing anything that escapes it.
    public Path resolve(Path root, String relative) throws IOException {
        Path base = realRoot(root);
        if (relative == null || relative.isBlank() || ".".equals(relative) || "/".equals(relative)) {
            return base;
        }
        Path target;
        try {
            target = base.resolve(relative).normalize();
        } catch (RuntimeException e) {
            throw new IOException("invalid path: " + relative);
        }
        if (!target.startsWith(base)) {
            throw new IOException("path escapes the media root: " + relative);
        }
        if (!Files.exists(target)) {
            throw new IOException("not found: " + relative);
        }
        Path real = target.toRealPath();
        if (!real.startsWith(base)) {
            throw new IOException("path escapes the media root through a link: " + relative);
        }
        return real;
    }

    /// Resolves a path for writing: it may not exist yet, but it must stay below the root.
    /// The parent is created by the caller (the queue does that right before a job).
    public Path resolveForWrite(Path root, String relative) throws IOException {
        Path base = realRoot(root);
        if (relative == null || relative.isBlank() || ".".equals(relative)) {
            return base;
        }
        Path target;
        try {
            target = base.resolve(relative).normalize();
        } catch (RuntimeException e) {
            throw new IOException("invalid path: " + relative);
        }
        if (!target.startsWith(base)) {
            throw new IOException("path escapes the media root: " + relative);
        }
        // If the directory exists, its real path must still be inside the root (a
        // symlinked output directory must not silently move writes outside the share).
        if (Files.exists(target)) {
            Path real = target.toRealPath();
            if (!real.startsWith(base)) {
                throw new IOException("path escapes the media root through a link: " + relative);
            }
            return real;
        }
        return target;
    }

    private static Path realRoot(Path root) throws IOException {
        if (!Files.isDirectory(root)) {
            throw new IOException("the media root does not exist: " + root);
        }
        return root.toRealPath();
    }

    private static String relativeOf(Path base, Path path) {
        Path relative = base.relativize(path);
        return relative.toString().replace('\\', '/');
    }

    private static long safeSize(Path file) {
        try {
            return Files.size(file);
        } catch (IOException e) {
            return -1;
        }
    }

    private static FileTime safeModified(Path file) {
        try {
            return Files.getLastModifiedTime(file);
        } catch (IOException e) {
            return FileTime.from(Instant.EPOCH);
        }
    }

    private static String kind(String name) {
        int dot = name.lastIndexOf('.');
        String extension = dot < 0 ? "" : name.substring(dot + 1).toLowerCase(Locale.ROOT);
        if (VIDEO.contains(extension)) return "video";
        if (AUDIO.contains(extension)) return "audio";
        if (SUBTITLE.contains(extension)) return "subtitle";
        if (IMAGE.contains(extension)) return "image";
        return "other";
    }

    /// One directory entry.
    public record Entry(String name, String path, boolean directory, long size,
                        String modified, String kind) {
    }

    /// One page plus the directory it describes.
    public record Listing(String path, int offset, int total, List<Entry> entries) {
    }

    /// Flattened media files under one directory (the "add folder" result).
    public record Scan(String path, int total, boolean truncated, List<Entry> entries) {
    }
}