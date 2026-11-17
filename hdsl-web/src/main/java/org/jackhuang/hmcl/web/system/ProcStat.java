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

import java.util.LinkedHashMap;
import java.util.Map;

/// `/proc/stat`: the cumulative CPU times the kernel exposes per line.
///
/// Each line is `cpu user nice system idle iowait irq softirq steal guest
/// guest_nice`. `guest` is already included in `user`, and `guest_nice` in
/// `nice`, so summing all ten columns double-counts; the first eight are the
/// total. Usage is a **difference** between two samples — the absolute values
/// are jiffies since boot and mean nothing on their own.
///
/// The parser is pure so the arithmetic can be tested without a Linux host.
@NotNullByDefault
public final class ProcStat {

    /// One line: busy and idle jiffies, ready for a difference.
    ///
    /// @param total user+nice+system+idle+iowait+irq+softirq+steal
    /// @param idle  idle+iowait, the part that was not doing work
    public record Times(long total, long idle) {
    }

    private ProcStat() {
    }

    /// Parses every `cpu*` line, keeping the kernel's order.
    ///
    /// @param text the file's contents
    /// @return `cpu` (aggregate) first, then `cpu0`, `cpu1`, … — empty when the
    ///         text is not a stat file
    public static Map<String, Times> parse(String text) {
        Map<String, Times> lines = new LinkedHashMap<>();
        for (String line : text.split("\n")) {
            if (!line.startsWith("cpu")) {
                continue;
            }
            String[] fields = line.trim().split("\\s+");
            if (fields.length < 5) {
                continue;
            }
            // Every key after "cpu" must be a number: "cpu0" is a core,
            // "cpufreq" (not present here, but possible in other files) is not.
            if (fields[0].length() > 3 && !fields[0].substring(3).chars().allMatch(Character::isDigit)) {
                continue;
            }
            long total = 0;
            long idle = 0;
            int limit = Math.min(fields.length, 9); // user … steal
            for (int i = 1; i < limit; i++) {
                long value;
                try {
                    value = Long.parseLong(fields[i]);
                } catch (NumberFormatException e) {
                    break;
                }
                total += value;
                if (i == 4 || i == 5) { // idle, iowait
                    idle += value;
                }
            }
            if (total > 0) {
                lines.put(fields[0], new Times(total, idle));
            }
        }
        return lines;
    }

    /// How much of the period was busy, as 0..1.
    ///
    /// A counter that went backwards (a reboot between samples, a counter
    /// wrap) answers `-1` so the caller can show "collecting" instead of a
    /// fabricated percentage.
    ///
    /// @param previous the earlier sample, or `null` for the first one
    /// @param current  the later sample
    /// @return the busy fraction, or `-1` when it cannot be computed
    public static double usage(Times previous, Times current) {
        if (previous == null || current == null) {
            return -1;
        }
        long total = current.total() - previous.total();
        long idle = current.idle() - previous.idle();
        if (total <= 0 || idle < 0 || idle > total) {
            return -1;
        }
        return 1.0 - (double) idle / total;
    }
}
