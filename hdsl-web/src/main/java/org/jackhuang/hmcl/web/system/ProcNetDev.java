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

/// `/proc/net/dev`: cumulative bytes received and transmitted per interface.
///
/// The first two lines are headers. Each data line is `name: rx_bytes rx_packets
/// … multicast tx_bytes tx_packets …` — 16 columns, the same shape since 2.6.
/// Inside a container this reflects the container's own network namespace,
/// unlike `/proc/diskstats`, which is host-wide.
@NotNullByDefault
public final class ProcNetDev {

    /// One interface's cumulative counters.
    ///
    /// @param rxBytes bytes received since boot
    /// @param txBytes bytes transmitted since boot
    public record Counters(long rxBytes, long txBytes) {
    }

    private ProcNetDev() {
    }

    /// Parses every interface line.
    ///
    /// @param text the file's contents
    /// @return interface name → counters, in file order
    public static Map<String, Counters> parse(String text) {
        Map<String, Counters> interfaces = new LinkedHashMap<>();
        for (String line : text.split("\n")) {
            int colon = line.indexOf(':');
            if (colon <= 0) {
                continue;
            }
            String name = line.substring(0, colon).trim();
            String[] fields = line.substring(colon + 1).trim().split("\\s+");
            if (name.isEmpty() || fields.length < 9) {
                continue;
            }
            try {
                interfaces.put(name, new Counters(Long.parseLong(fields[0]), Long.parseLong(fields[8])));
            } catch (NumberFormatException ignored) {
                // Not a data line.
            }
        }
        return interfaces;
    }

    /// Bytes per second between two samples, or `-1` when it cannot be computed.
    ///
    /// @param previous earlier counters, or `null`
    /// @param current  later counters
    /// @param seconds  wall time between the samples
    /// @param transmit whether to report the transmit side
    /// @return bytes per second, or `-1`
    public static double bytesPerSecond(Counters previous, Counters current, double seconds, boolean transmit) {
        if (previous == null || current == null || seconds <= 0) {
            return -1;
        }
        long bytes = transmit
                ? current.txBytes() - previous.txBytes()
                : current.rxBytes() - previous.rxBytes();
        if (bytes < 0) {
            return -1;
        }
        return bytes / seconds;
    }
}
