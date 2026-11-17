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

/// `/proc/diskstats`: cumulative sectors read and written per block device.
///
/// The file lists whole disks **and** their partitions (`nvme0n1` and
/// `nvme0n1p1`), plus loop and device-mapper nodes. Rates are differences; the
/// sampler filters to whole devices by asking `/sys/block`, so a partition is
/// never counted twice.
///
/// Field positions (kernel 5.x): `major minor name reads reads_merged
/// sectors_read ms_reading writes writes_merged sectors_written …` — sectors
/// are 512 bytes by definition, whatever the device's block size is.
@NotNullByDefault
public final class ProcDiskStats {

    /// One device's cumulative counters.
    ///
    /// @param readSectors   sectors read since boot
    /// @param writeSectors  sectors written since boot
    public record Counters(long readSectors, long writeSectors) {
    }

    /// The size of a "sector" in this file, fixed by the kernel interface.
    public static final long SECTOR_BYTES = 512;

    private ProcDiskStats() {
    }

    /// Parses every well-formed line.
    ///
    /// @param text the file's contents
    /// @return device name → counters, in file order
    public static Map<String, Counters> parse(String text) {
        Map<String, Counters> devices = new LinkedHashMap<>();
        for (String line : text.split("\n")) {
            String[] fields = line.trim().split("\\s+");
            if (fields.length < 10) {
                continue;
            }
            try {
                // 0 major, 1 minor, 2 name, 3 reads, 4 merged, 5 sectors read,
                // 6 ms reading, 7 writes, 8 merged, 9 sectors written.
                devices.put(fields[2], new Counters(Long.parseLong(fields[5]), Long.parseLong(fields[9])));
            } catch (NumberFormatException ignored) {
                // A header or a short line; not a device.
            }
        }
        return devices;
    }

    /// Bytes read per second between two samples, or `-1` when it cannot be computed.
    ///
    /// @param previous earlier counters, or `null`
    /// @param current  later counters
    /// @param seconds  wall time between the samples
    /// @return bytes per second, or `-1`
    public static double readBytesPerSecond(Counters previous, Counters current, double seconds) {
        return bytesPerSecond(previous, current, seconds, false);
    }

    /// Bytes written per second between two samples, or `-1` when it cannot be computed.
    ///
    /// @param previous earlier counters, or `null`
    /// @param current  later counters
    /// @param seconds  wall time between the samples
    /// @return bytes per second, or `-1`
    public static double writeBytesPerSecond(Counters previous, Counters current, double seconds) {
        return bytesPerSecond(previous, current, seconds, true);
    }

    private static double bytesPerSecond(Counters previous, Counters current, double seconds, boolean write) {
        if (previous == null || current == null || seconds <= 0) {
            return -1;
        }
        long sectors = write
                ? current.writeSectors() - previous.writeSectors()
                : current.readSectors() - previous.readSectors();
        if (sectors < 0) {
            return -1;
        }
        return sectors * (double) SECTOR_BYTES / seconds;
    }
}
