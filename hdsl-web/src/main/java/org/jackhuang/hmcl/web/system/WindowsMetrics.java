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

import com.sun.jna.Library;
import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.WString;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;
import org.jackhuang.hmcl.util.logging.Logger;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// The Windows readers behind the same snapshot the Linux `/proc` code fills.
///
/// Windows has no procfs, so each number comes from a different native surface:
///
/// - **per-core CPU**: `NtQuerySystemInformation(SystemProcessorPerformanceInformation)`,
///   which returns cumulative idle/kernel/user times per logical processor —
///   the same "diff two samples" shape as `/proc/stat`, so the arithmetic is
///   shared;
/// - **memory**: `GetPerformanceInfo` (physical total, available and the system
///   cache, i.e. Windows' standby cache);
/// - **disk and network rates**: PDH counters (`\PhysicalDisk(_Total)\…` and
///   `\Network Interface(*)\…`), which are already per-second values.
///
/// Every reader fails soft. A machine whose performance counters are broken
/// (a real Windows condition), or where JNA cannot find the library, answers
/// "not available" and the page hides that card — it never shows a fabricated
/// zero. Nothing here runs unless the panel itself is on Windows.
///
/// This class cannot be exercised on a Linux test host; the parsing helpers are
/// package-private and tested with synthetic buffers, and the native calls are
/// guarded so a failure degrades the page instead of the process.
@NotNullByDefault
final class WindowsMetrics {

    /// Cumulative per-processor times, in 100 ns units.
    ///
    /// @param idle   time spent idle
    /// @param kernel kernel time (includes idle on Windows)
    /// @param user   user time
    record CpuTimes(long idle, long kernel, long user) {
        long total() {
            return kernel + user;
        }
    }

    /// @param totalBytes     physical memory
    /// @param availableBytes `PhysicalAvailable` (free + standby)
    /// @param cachedBytes    `SystemCache` (standby cache)
    record MemoryInfo(long totalBytes, long availableBytes, long cachedBytes) {
    }

    /// A PDH-formatted rate.
    record DiskRates(double readBytesPerSecond, double writeBytesPerSecond) {
    }

    /// One interface's receive/transmit rates.
    record NetworkRate(String name, double rxBytesPerSecond, double txBytesPerSecond) {
    }

    private static final int SystemProcessorPerformanceInformation = 8;
    private static final int STATUS_INFO_LENGTH_MISMATCH = 0xC0000004;
    private static final int PER_PROCESSOR_BYTES = 48; // six LARGE_INTEGERs

    private static final int PDH_FMT_DOUBLE = 0x00000200;
    private static final int PDH_MORE_DATA = 0x800007D2;
    private static final int PDH_ITEM_BYTES = 24; // LPWSTR + PDH_FMT_COUNTERVALUE on x64

    private final Ntdll ntdll;
    private final Kernel32 kernel32;
    private final Pdh pdh;
    private final @Nullable Pointer query;
    private final @Nullable Pointer diskRead;
    private final @Nullable Pointer diskWrite;
    private final @Nullable Pointer netReceived;
    private final @Nullable Pointer netSent;

    private WindowsMetrics(Ntdll ntdll, Kernel32 kernel32, Pdh pdh) {
        this.ntdll = ntdll;
        this.kernel32 = kernel32;
        this.pdh = pdh;
        PointerByReference handle = new PointerByReference();
        Pointer opened = null;
        Pointer read = null;
        Pointer write = null;
        Pointer received = null;
        Pointer sent = null;
        if (pdh.PdhOpenQueryW(null, Pointer.NULL, handle) == 0) {
            opened = handle.getValue();
            read = addCounter(pdh, opened, "\\PhysicalDisk(_Total)\\Disk Read Bytes/sec");
            write = addCounter(pdh, opened, "\\PhysicalDisk(_Total)\\Disk Write Bytes/sec");
            received = addCounter(pdh, opened, "\\Network Interface(*)\\Bytes Received/sec");
            sent = addCounter(pdh, opened, "\\Network Interface(*)\\Bytes Sent/sec");
        }
        this.query = opened;
        this.diskRead = read;
        this.diskWrite = write;
        this.netReceived = received;
        this.netSent = sent;
    }

    /// Loads the three libraries, or answers `null` when this is not Windows or
    /// JNA cannot reach them.
    ///
    /// @return the reader, or `null`
    static @Nullable WindowsMetrics create() {
        try {
            Ntdll ntdll = Native.load("ntdll", Ntdll.class);
            Kernel32 kernel32 = Native.load("kernel32", Kernel32.class);
            Pdh pdh = Native.load("pdh", Pdh.class);
            return new WindowsMetrics(ntdll, kernel32, pdh);
        } catch (Throwable e) {
            // UnsatisfiedLinkError, a missing DLL, a stubbed JNA on a test host:
            // all of them mean "no Windows readings", not "crash the panel".
            Logger.LOG.warning("[monitor] Windows performance APIs unavailable: " + e);
            return null;
        }
    }

    /// Per-logical-processor cumulative times, in processor index order.
    ///
    /// @return the times, empty when the query fails
    List<CpuTimes> cpuTimes() {
        int cores = Math.max(1, Runtime.getRuntime().availableProcessors());
        int size = cores * PER_PROCESSOR_BYTES;
        Memory buffer = new Memory(size);
        IntByReference returned = new IntByReference();
        int status = ntdll.NtQuerySystemInformation(SystemProcessorPerformanceInformation,
                buffer, size, returned);
        if (status == STATUS_INFO_LENGTH_MISMATCH) {
            int needed = Math.max(size, returned.getValue());
            buffer = new Memory(needed);
            status = ntdll.NtQuerySystemInformation(SystemProcessorPerformanceInformation,
                    buffer, needed, returned);
            size = needed;
        }
        if (status != 0) {
            return List.of();
        }
        int count = Math.min(cores, size / PER_PROCESSOR_BYTES);
        if (returned.getValue() > 0) {
            count = Math.min(count, returned.getValue() / PER_PROCESSOR_BYTES);
        }
        return parseCpuTimes(buffer, count);
    }

    /// Reads the cumulative array `NtQuerySystemInformation` filled.
    ///
    /// @param buffer the structure array
    /// @param count  how many entries it holds
    /// @return one record per processor
    static List<CpuTimes> parseCpuTimes(Pointer buffer, int count) {
        List<CpuTimes> times = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            long offset = (long) index * PER_PROCESSOR_BYTES;
            times.add(new CpuTimes(
                    buffer.getLong(offset),
                    buffer.getLong(offset + 8),
                    buffer.getLong(offset + 16)));
        }
        return times;
    }

    /// Physical memory, or `null` when the call fails.
    ///
    /// `GlobalMemoryStatusEx` rather than `GetPerformanceInfo`: the latter's
    /// `SIZE_T`-heavy struct came back misaligned on the first Windows test
    /// (a page count multiplied by a garbage page size — an absurd total),
    /// while this structure is two DWORDs and seven DWORDLONGs with no padding
    /// surprises. Windows' standby cache is not collected yet, so `cached` is
    /// zero and the page simply does not draw that segment.
    MemoryInfo memory() {
        MemoryStatusEx status = new MemoryStatusEx();
        if (!kernel32.GlobalMemoryStatusEx(status)) {
            return null;
        }
        return new MemoryInfo(status.ullTotalPhys, status.ullAvailPhys, 0);
    }

    /// Disk rates over the PDH collection interval, or `null` when the counters
    /// could not be opened.
    @Nullable DiskRates diskRates() {
        if (query == null || diskRead == null || diskWrite == null) {
            return null;
        }
        if (pdh.PdhCollectQueryData(query) != 0) {
            return null;
        }
        Double read = format(diskRead);
        Double write = format(diskWrite);
        if (read == null || write == null) {
            return null;
        }
        return new DiskRates(read, write);
    }

    /// Per-interface rates, empty when the wildcard counters are unavailable.
    List<NetworkRate> networkRates() {
        if (query == null || netReceived == null || netSent == null) {
            return List.of();
        }
        if (pdh.PdhCollectQueryData(query) != 0) {
            return List.of();
        }
        Map<String, double[]> rates = new LinkedHashMap<>();
        collectArray(netReceived, 0, rates);
        collectArray(netSent, 1, rates);
        List<NetworkRate> out = new ArrayList<>(rates.size());
        for (Map.Entry<String, double[]> entry : rates.entrySet()) {
            out.add(new NetworkRate(entry.getKey(), entry.getValue()[0], entry.getValue()[1]));
        }
        return out;
    }

    void close() {
        if (query != null) {
            try {
                pdh.PdhCloseQuery(query);
            } catch (Throwable ignored) {
                // Closing a query the process is about to lose anyway.
            }
        }
    }

    /// One formatted double value.
    private @Nullable Double format(Pointer counter) {
        PdhCounterValue value = new PdhCounterValue();
        if (pdh.PdhGetFormattedCounterValue(counter, PDH_FMT_DOUBLE, null, value) != 0) {
            return null;
        }
        return value.doubleValue;
    }

    /// Reads one wildcard counter into `rates[slot]`, keyed by instance name.
    private void collectArray(Pointer counter, int slot, Map<String, double[]> rates) {
        IntByReference size = new IntByReference();
        IntByReference count = new IntByReference();
        int status = pdh.PdhGetFormattedCounterArrayW(counter, PDH_FMT_DOUBLE, size, count, null);
        if (status != PDH_MORE_DATA || size.getValue() <= 0) {
            return;
        }
        Memory buffer = new Memory(size.getValue());
        status = pdh.PdhGetFormattedCounterArrayW(counter, PDH_FMT_DOUBLE, size, count, buffer);
        if (status != 0) {
            return;
        }
        readCounterArray(buffer, count.getValue(), slot, rates);
    }

    /// Reads the `PDH_FMT_COUNTERVALUE_ITEM_W` array into `rates[slot]`, keyed
    /// by instance name. Split out so the layout can be tested with a synthetic
    /// buffer instead of a Windows machine.
    static void readCounterArray(Pointer buffer, int count, int slot, Map<String, double[]> rates) {
        for (int index = 0; index < count; index++) {
            Pointer item = buffer.share((long) index * PDH_ITEM_BYTES);
            Pointer namePointer = item.getPointer(0);
            if (namePointer == null) {
                continue;
            }
            String name = namePointer.getWideString(0);
            double value = item.getDouble(16);
            rates.computeIfAbsent(name, key -> new double[]{0, 0})[slot] = value;
        }
    }

    private static @Nullable Pointer addCounter(Pdh pdh, Pointer query, String path) {
        PointerByReference counter = new PointerByReference();
        if (pdh.PdhAddEnglishCounterW(query, new WString(path), Pointer.NULL, counter) != 0) {
            return null;
        }
        return counter.getValue();
    }

    interface Ntdll extends Library {
        int NtQuerySystemInformation(int informationClass, Pointer information,
                                     int informationLength, IntByReference returnLength);
    }

    interface Kernel32 extends Library {
        boolean GlobalMemoryStatusEx(MemoryStatusEx status);
    }

    interface Pdh extends Library {
        int PdhOpenQueryW(@Nullable WString dataSource, Pointer userData, PointerByReference query);

        int PdhAddEnglishCounterW(Pointer query, WString path, Pointer userData, PointerByReference counter);

        int PdhCollectQueryData(Pointer query);

        int PdhGetFormattedCounterValue(Pointer counter, int format, IntByReference type,
                                        PdhCounterValue value);

        int PdhGetFormattedCounterArrayW(Pointer counter, int format, IntByReference bufferSize,
                                         IntByReference itemCount, @Nullable Pointer buffer);

        int PdhCloseQuery(Pointer query);
    }

    /// `MEMORYSTATUSEX` from sysinfoapi.h: two DWORDs followed by seven
    /// DWORDLONGs, 64 bytes on a 64-bit process with no padding surprises.
    @Structure.FieldOrder({"dwLength", "dwMemoryLoad", "ullTotalPhys", "ullAvailPhys",
            "ullTotalPageFile", "ullAvailPageFile", "ullTotalVirtual", "ullAvailVirtual",
            "ullAvailExtendedVirtual"})
    public static final class MemoryStatusEx extends Structure {
        public int dwLength;
        public int dwMemoryLoad;
        public long ullTotalPhys;
        public long ullAvailPhys;
        public long ullTotalPageFile;
        public long ullAvailPageFile;
        public long ullTotalVirtual;
        public long ullAvailVirtual;
        public long ullAvailExtendedVirtual;

        public MemoryStatusEx() {
            dwLength = size();
        }
    }

    /// `PDH_FMT_COUNTERVALUE` for the double format.
    @Structure.FieldOrder({"CStatus", "doubleValue"})
    public static final class PdhCounterValue extends Structure {
        public int CStatus;
        public double doubleValue;
    }
}
