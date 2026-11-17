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
package org.jackhuang.hmcl.web.system;

import org.jetbrains.annotations.NotNullByDefault;

import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/// What the panel's data directory holds, split into the buckets a person
/// recognises (`brands`, `dsh`, `logs`, …).
///
/// Walking a data directory with release trees in it is not free (a few
/// hundred thousand files is seconds, not milliseconds), so the caller caches
/// the report and only recomputes on request. The walk does not follow
/// symlinks: a link out of the directory is not this directory's weight.
@NotNullByDefault
public final class StorageUsage {

    /// One top-level directory.
    ///
    /// @param name  the directory name on disk
    /// @param label what the page should call it
    /// @param bytes regular-file bytes inside it, recursively
    public record Bucket(String name, String label, long bytes) {
    }

    /// One filesystem, for the "how much room is left" line.
    public record Mount(String path, long totalBytes, long usedBytes) {
    }

    /// The whole answer.
    ///
    /// @param computedAt ISO time the walk finished — the page says how old it is
    public record Report(String dataDir, long totalBytes, List<Bucket> buckets,
                         Mount mount, String computedAt) {
    }

    /// Labels for the directories this panel is known to create. Unknown
    /// directories keep their own name, so a future feature still shows up.
    private static final Map<String, String> LABELS = Map.of(
            "brands", "品牌实例与版本",
            "dsh", "dsh 实例与版本",
            "zcode", "ZCode 实例与版本",
            "minecraft", "Minecraft 服务器",
            "hdsl", "运行时与缓存",
            "logs", "日志",
            "auth", "凭据",
            "tmp", "临时文件",
            "certificates", "证书");

    private StorageUsage() {
    }

    /// Walks `dataDir` once and reports its top-level buckets.
    ///
    /// @param dataDir the panel's data directory
    /// @return the report; a missing directory reports zero bytes
    public static Report compute(Path dataDir) {
        List<Bucket> buckets = new ArrayList<>();
        long total = 0;
        if (Files.isDirectory(dataDir)) {
            try (var children = Files.list(dataDir)) {
                for (Path child : children.filter(Files::isDirectory).toList()) {
                    String name = child.getFileName().toString();
                    long bytes = sizeOf(child);
                    buckets.add(new Bucket(name, LABELS.getOrDefault(name, name), bytes));
                    total += bytes;
                }
            } catch (IOException | RuntimeException ignored) {
                // A directory that disappears mid-walk is not worth failing over.
            }
        }
        buckets.sort(Comparator.comparingLong(Bucket::bytes).reversed()
                .thenComparing(Bucket::name));
        return new Report(pathOf(dataDir), total, List.copyOf(buckets), mountOf(dataDir),
                Instant.now().toString());
    }

    /// Recursive size of regular files, without following symlinks.
    static long sizeOf(Path root) {
        if (Files.isRegularFile(root)) {
            try {
                return Files.size(root);
            } catch (IOException e) {
                return 0;
            }
        }
        if (!Files.isDirectory(root)) {
            return 0;
        }
        long[] total = {0};
        try {
            Files.walkFileTree(root, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) {
                    if (attributes.isRegularFile()) {
                        total[0] += attributes.size();
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exception) {
                    // Permission denied inside somebody else's home: skip it.
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException | RuntimeException ignored) {
            // Partial numbers are better than a failed request.
        }
        return total[0];
    }

    private static Mount mountOf(Path dataDir) {
        try {
            FileStore store = Files.getFileStore(dataDir);
            long total = store.getTotalSpace();
            long usable = store.getUsableSpace();
            return new Mount(pathOf(dataDir), total, Math.max(0, total - usable));
        } catch (IOException | RuntimeException e) {
            return new Mount(pathOf(dataDir), 0, 0);
        }
    }

    private static String pathOf(Path path) {
        return path.toAbsolutePath().normalize().toString();
    }
}
