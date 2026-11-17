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

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/// `/proc/meminfo`: `MemTotal`, `MemAvailable` and the swap pair.
///
/// Values are in kB. `MemAvailable` is the kernel's own estimate of what a new
/// workload could allocate without swapping, which is the number a task
/// manager shows as "available" — `MemFree` alone would make a healthy machine
/// look full because the page cache is counted as used.
@NotNullByDefault
public final class ProcMeminfo {

    /// The four numbers the panel needs, in bytes.
    ///
    /// @param totalBytes     memory installed (or the cgroup limit, when one applies)
    /// @param availableBytes what the kernel says can still be allocated
    /// @param freeBytes      memory that is not holding anything at all
    /// @param cachedBytes    page cache + buffers: reclaimable, which is why
    ///                       Linux looks "full" while `availableBytes` stays high
    /// @param swapTotalBytes swap size
    /// @param swapFreeBytes  swap still free
    public record Memory(long totalBytes, long availableBytes, long freeBytes, long cachedBytes,
                         long swapTotalBytes, long swapFreeBytes) {
    }

    private ProcMeminfo() {
    }

    /// Parses the file; a missing key is `0`.
    ///
    /// @param text the file's contents
    /// @return the four numbers in bytes
    public static Memory parse(String text) {
        Map<String, Long> fields = new HashMap<>();
        for (String line : text.split("\n")) {
            int colon = line.indexOf(':');
            if (colon <= 0) {
                continue;
            }
            String key = line.substring(0, colon).trim();
            String rest = line.substring(colon + 1).trim().toLowerCase(Locale.ROOT);
            int space = rest.indexOf(' ');
            String number = space < 0 ? rest : rest.substring(0, space);
            try {
                fields.put(key, Long.parseLong(number) * 1024);
            } catch (NumberFormatException ignored) {
                // A line this parser does not understand is not worth failing over.
            }
        }
        long free = fields.getOrDefault("MemFree", 0L);
        return new Memory(
                fields.getOrDefault("MemTotal", 0L),
                // Kernels before 3.14 have no MemAvailable; MemFree is the best
                // available substitute there.
                fields.getOrDefault("MemAvailable", free),
                free,
                fields.getOrDefault("Buffers", 0L) + fields.getOrDefault("Cached", 0L),
                fields.getOrDefault("SwapTotal", 0L),
                fields.getOrDefault("SwapFree", 0L));
    }
}
