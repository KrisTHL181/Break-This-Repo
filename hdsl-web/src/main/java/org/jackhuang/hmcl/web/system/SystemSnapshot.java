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

import java.util.List;

/// One moment of the machine, as the panel describes it to the browser.
///
/// The shape is the contract: the front end never asks the operating system
/// anything, it only draws what a snapshot says, and `capabilities` tells it
/// which cards are worth drawing on this host. Values that could not be
/// measured are `-1` (a rate that needs a second sample), never `0`, because
/// zero reads as "idle" and that would be a lie.
///
/// `at` is an ISO-8601 string rather than a date object: it goes straight to
/// JSON, and the browser parses it with `Date.parse`.
@NotNullByDefault
public record SystemSnapshot(
        String at,
        long intervalMs,
        Host host,
        Capabilities capabilities,
        Cpu cpu,
        Memory memory,
        List<DiskRate> disks,
        List<MountUsage> mounts,
        List<NetworkRate> networks,
        List<Gpu> gpus,
        Panel panel) {

    /// Which operating system, and whether it is a sandbox. The page's notes
    /// switch on this: "容器里显示的是宿主机核心" is only true in a container.
    ///
    /// @param os          `linux`, `windows`, `macos` or `other`
    /// @param environment `container`, `wsl` or `native`
    public record Host(String os, String environment) {
    }

    /// What this host can actually show.
    ///
    /// @param perCore per-core CPU percentages, not just the aggregate
    /// @param diskIo  per-device read/write rates
    /// @param network per-interface receive/transmit rates
    /// @param gpu     a GPU reader is installed (reserved; `false` in this build)
    public record Capabilities(boolean perCore, boolean diskIo, boolean network, boolean gpu) {
    }

    /// @param load        aggregate busy fraction 0..1, or `-1` while collecting
    /// @param count       logical processors the kernel reports
    /// @param quotaCores  cgroup CPU quota in cores, or `-1` when unlimited
    /// @param cores       per-core busy fractions, empty when unsupported
    /// @param loadAverage 1/5/15-minute load, or empty on hosts without it
    public record Cpu(double load, int count, double quotaCores, double[] cores, double[] loadAverage) {
    }

    /// @param totalBytes     limit the panel should show (cgroup when it applies)
    /// @param usedBytes      memory holding something that is not reclaimable
    /// @param availableBytes what a new workload could still allocate
    ///                       (`cachedBytes + freeBytes`, the way the kernel
    ///                       counts reclaimable cache)
    /// @param freeBytes      memory holding nothing at all
    /// @param cachedBytes    page cache and buffers, reclaimable on demand
    /// @param source         `cgroup` or `host` — what `totalBytes` describes
    public record Memory(long totalBytes, long usedBytes, long availableBytes,
                         long freeBytes, long cachedBytes,
                         long swapTotalBytes, long swapUsedBytes, String source) {
    }

    /// @param readBytesPerSec  `-1` while collecting
    /// @param writeBytesPerSec `-1` while collecting
    public record DiskRate(String name, double readBytesPerSec, double writeBytesPerSec) {
    }

    /// @param path       the mount (or directory) the numbers describe
    /// @param totalBytes filesystem size
    /// @param usedBytes  size minus what is still usable
    public record MountUsage(String path, long totalBytes, long usedBytes) {
    }

    /// @param rxBytesPerSec `-1` while collecting
    /// @param txBytesPerSec `-1` while collecting
    public record NetworkRate(String name, double rxBytesPerSec, double txBytesPerSec) {
    }

    /// One GPU as the monitor tab draws it. Unknown values are `-1`, never `0`:
    /// `0%` reads as "idle", while `-1` tells the page to print a dash.
    public record Gpu(int index, String name, String vendor, String backend,
                      double utilization, double encoderUtilization, double decoderUtilization,
                      long memoryUsedBytes, long memoryTotalBytes,
                      double temperatureC, double powerW) {
    }

    /// The panel's own footprint.
    ///
    /// @param cpuLoad  process CPU load, or `-1`
    /// @param rssBytes resident set size, or `-1`
    public record Panel(double cpuLoad, long rssBytes, int threads, long uptimeSec) {
    }
}
