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
import java.nio.file.Files;
import java.nio.file.Path;
import org.jetbrains.annotations.Nullable;

/// What the container's cgroup says about memory and CPU, when it says
/// anything at all.
///
/// Two facts matter here:
///
/// - **`max` is a legal value**, not a number: it means "no limit". Parsing it
///   as a long throws, and treating the throw as "no data" would make an
///   unlimited container look like one whose limits cannot be read.
/// - `/proc/stat` and `/proc/meminfo` are **not namespaced**: a container with
///   `--cpus=2` still sees every host core and the host's memory. The cgroup
///   files are the only place the limit appears, which is why the panel shows
///   both numbers when they differ.
///
/// cgroup v2 is read first; the v1 paths are a fallback so an older host still
/// answers instead of silently reporting the host's numbers.
@NotNullByDefault
public final class CgroupLimits {

    /// What was found; `0` / `-1` mean "not limited here".
    ///
    /// @param memoryLimitBytes the cgroup memory ceiling, or `-1` when unlimited
    /// @param memoryUsedBytes  the cgroup's current usage, or `-1` when unknown
    /// @param cpuQuotaCores    the CPU quota in cores, or `-1` when unlimited
    /// @param fileCacheBytes   page cache inside the cgroup, or `-1` when unknown.
    ///                         `memory.current` includes it, so "in use" has to
    ///                         subtract it or Linux looks permanently full.
    public record Limits(long memoryLimitBytes, long memoryUsedBytes, double cpuQuotaCores,
                         long fileCacheBytes) {
        public static final Limits NONE = new Limits(-1, -1, -1, -1);
    }

    private static final Path V2 = Path.of("/sys/fs/cgroup");
    private static final Path V1_MEMORY = Path.of("/sys/fs/cgroup/memory");
    private static final Path V1_CPU = Path.of("/sys/fs/cgroup/cpu");

    private CgroupLimits() {
    }

    /// Reads the limits from the root cgroup.
    ///
    /// @return the limits, with `-1` for every value the host does not expose
    public static Limits read() {
        return read(V2);
    }

    /// The same read against an explicit root, for tests.
    static Limits read(Path root) {
        long limit = parseLong(text(root.resolve("memory.max")), -1);
        long used = parseLong(text(root.resolve("memory.current")), -1);
        double quota = parseCpuQuota(text(root.resolve("cpu.max")));
        long cache = statValue(text(root.resolve("memory.stat")), "file");
        if (limit < 0 && used < 0 && quota < 0) {
            // cgroup v1: the same numbers under different names.
            limit = parseLong(text(V1_MEMORY.resolve("memory.limit_in_bytes")), -1);
            used = parseLong(text(V1_MEMORY.resolve("memory.usage_in_bytes")), -1);
            long quotaMicros = parseLong(text(V1_CPU.resolve("cpu.cfs_quota_us")), -1);
            long periodMicros = parseLong(text(V1_CPU.resolve("cpu.cfs_period_us")), -1);
            if (quotaMicros > 0 && periodMicros > 0) {
                quota = (double) quotaMicros / periodMicros;
            }
            cache = statValue(text(V1_MEMORY.resolve("memory.stat")), "total_cache");
        }
        // A v1 host reports a huge sentinel (PAGE_COUNTER_MAX) for "unlimited".
        if (limit > 1L << 50) {
            limit = -1;
        }
        return new Limits(limit, used, quota, cache);
    }

    /// One numeric line of `memory.stat` (`file 12345`, `total_cache 12345`).
    static long statValue(@Nullable String text, String key) {
        if (text == null) {
            return -1;
        }
        for (String line : text.split("\n")) {
            String[] fields = line.trim().split("\\s+");
            if (fields.length == 2 && fields[0].equals(key)) {
                return parseLong(fields[1], -1);
            }
        }
        return -1;
    }

    /// `200000 100000` means two cores; `max 100000` means no quota.
    static double parseCpuQuota(String text) {
        if (text == null || text.isBlank()) {
            return -1;
        }
        String[] fields = text.trim().split("\\s+");
        if (fields.length < 2 || "max".equals(fields[0])) {
            return -1;
        }
        try {
            long quota = Long.parseLong(fields[0]);
            long period = Long.parseLong(fields[1]);
            return quota > 0 && period > 0 ? (double) quota / period : -1;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static String text(Path file) {
        try {
            return Files.isRegularFile(file) ? Files.readString(file).trim() : null;
        } catch (IOException | RuntimeException e) {
            return null;
        }
    }

    private static long parseLong(String text, long fallback) {
        if (text == null || text.isBlank() || "max".equals(text)) {
            return fallback;
        }
        try {
            return Long.parseLong(text);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
