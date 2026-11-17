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

import com.sun.management.OperatingSystemMXBean;
import org.jackhuang.hmcl.util.logging.Logger;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/// The panel's own view of the machine, sampled on a timer and kept in memory.
///
/// **Two gears, never faster than a second.** While someone is using the panel
/// ([SystemActivity]) a sample is taken every second; otherwise every thirty
/// seconds. The browser cannot make it faster: the endpoint serves the latest
/// snapshot and takes no interval parameter. That is the polite reading of
/// "don't hammer `/proc`" — one read per second at most, no matter how many
/// tabs or agents poll.
///
/// **One sampler for every client.** The history ring (240 snapshots — four
/// minutes fast, two hours slow) is shared, so a freshly opened page can draw
/// the last few minutes immediately and every viewer sees the same numbers.
///
/// **Nothing is persisted.** A restart empties the ring, exactly like sessions
/// and elevations. Only anomalies go to the log ([AnomalyDetector]).
///
/// The class is a plain object rather than a static singleton so a test can
/// point it at a fake `/proc`; the composition root owns the one instance.
@NotNullByDefault
public final class SystemMetrics {

    /// Sampling period while the panel is in use.
    public static final long FAST_INTERVAL_MS = 1_000;
    /// Sampling period while nobody is using it.
    public static final long SLOW_INTERVAL_MS = 30_000;
    /// How many snapshots the ring keeps: 4 minutes fast, 2 hours slow.
    public static final int HISTORY_SIZE = 240;

    private final Path dataDir;
    private final HostInfo hostInfo;
    /// The Windows readers, or `null` everywhere else (and when JNA cannot
    /// reach the native libraries — the page then hides those cards).
    private final @Nullable WindowsMetrics windows;
    private final GpuReader gpuReader;
    private final Path procRoot;
    private final Path sysBlockRoot;
    private final OperatingSystemMXBean os;
    private final AnomalyDetector anomalies = new AnomalyDetector();

    private final ScheduledExecutorService executor =
            Executors.newSingleThreadScheduledExecutor(runnable -> {
                Thread thread = new Thread(runnable, "system-metrics");
                thread.setDaemon(true);
                return thread;
            });
    private final Deque<SystemSnapshot> history = new ArrayDeque<>(HISTORY_SIZE);

    private @Nullable SystemSnapshot latest;
    private @Nullable ScheduledFuture<?> pending;
    private boolean started;
    private long intervalMs = SLOW_INTERVAL_MS;

    /// How many samples have been appended since this process started. The API
    /// serialises the ring once per value and reuses it until the next one, so
    /// this is the cache key: 240 snapshots per request was 811 rps against
    /// 9k for every other endpoint on the same box.
    private long sequence;

    // Raw counters from the previous sample, for the rates.
    private Map<String, ProcStat.Times> previousCpu = Map.of();
    private Map<String, ProcDiskStats.Counters> previousDisk = Map.of();
    private Map<String, ProcNetDev.Counters> previousNet = Map.of();
    private long previousNanos;
    /// Raw counters read for the sample in flight, moved into the `previous*`
    /// fields when it is finished.
    private Map<String, ProcDiskStats.Counters> lastDiskRaw = Map.of();
    private Map<String, ProcNetDev.Counters> lastNetRaw = Map.of();

    public SystemMetrics(Path dataDir) {
        this(dataDir, Path.of("/proc"), Path.of("/sys/block"));
    }

    /// A sampler against explicit roots, for tests.
    SystemMetrics(Path dataDir, Path procRoot, Path sysBlockRoot) {
        this(dataDir, procRoot, sysBlockRoot, HostInfo.detect());
    }

    /// A sampler with an explicit host description, for tests.
    SystemMetrics(Path dataDir, Path procRoot, Path sysBlockRoot, HostInfo hostInfo) {
        this.dataDir = dataDir;
        this.procRoot = procRoot;
        this.sysBlockRoot = sysBlockRoot;
        this.hostInfo = hostInfo;
        this.windows = "windows".equals(hostInfo.os()) ? WindowsMetrics.create() : null;
        this.gpuReader = GpuReaders.create(hostInfo);
        this.os = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
    }

    /// Starts sampling. Idempotent; the first sample runs immediately.
    public synchronized void start() {
        if (started) {
            return;
        }
        started = true;
        schedule(0);
    }

    /// Records panel activity and wakes a sleeping slow-gear sampler.
    ///
    /// Called by [SystemActivity] on every authenticated request; while the
    /// fast gear is already running the next sample is at most a second away,
    /// so this is a no-op there.
    public void touch() {
        synchronized (this) {
            if (!started) {
                start();
            }
            if (intervalMs > FAST_INTERVAL_MS) {
                intervalMs = FAST_INTERVAL_MS;
                schedule(0);
            }
        }
    }

    /// The newest snapshot, or `null` before the first sample.
    public synchronized @Nullable SystemSnapshot latest() {
        return latest;
    }

    /// Up to `max` newest snapshots, oldest first.
    ///
    /// @param max how many to keep
    /// @return an immutable list
    public synchronized List<SystemSnapshot> history(int max) {
        if (max <= 0) {
            return List.of();
        }
        int skip = Math.max(0, history.size() - max);
        List<SystemSnapshot> out = new ArrayList<>(Math.min(max, history.size()));
        int index = 0;
        for (SystemSnapshot snapshot : history) {
            if (index++ >= skip) {
                out.add(snapshot);
            }
        }
        return List.copyOf(out);
    }

    /// The history together with the counter of the newest sample in it.
    ///
    /// A caller that caches its serialised form keys on [#Frame#sequence] and
    /// must not read the samples separately: two locks means a list that ends on
    /// a different sample than the number it was filed under.
    ///
    /// @param max how many to keep
    /// @return the frame
    public synchronized Frame historyFrame(int max) {
        return new Frame(sequence, history(max));
    }

    /// @param sequence samples appended since start; strictly increasing
    /// @param samples the newest `max` snapshots, oldest first
    public record Frame(long sequence, List<SystemSnapshot> samples) {
    }

    /// Stops the sampler; used by tests and shutdown.
    public synchronized void close() {
        started = false;
        if (pending != null) {
            pending.cancel(false);
            pending = null;
        }
        executor.shutdownNow();
        if (windows != null) {
            windows.close();
        }
    }

    /// Samples once and appends to the ring, without touching the schedule.
    ///
    /// @return the snapshot
    SystemSnapshot sampleNow() {
        SystemSnapshot snapshot = sample(Instant.now(), intervalMs, System.nanoTime());
        synchronized (this) {
            record(snapshot);
        }
        return snapshot;
    }

    /// Appends a sample to the ring and bumps the sequence the API's cache
    /// watches. The caller holds the monitor.
    private void record(SystemSnapshot snapshot) {
        latest = snapshot;
        history.addLast(snapshot);
        while (history.size() > HISTORY_SIZE) {
            history.removeFirst();
        }
        sequence++;
    }

    private synchronized void schedule(long delayMs) {
        if (pending != null) {
            pending.cancel(false);
        }
        pending = executor.schedule(this::tick, delayMs, TimeUnit.MILLISECONDS);
    }

    private void tick() {
        boolean fast = SystemActivity.active();
        long delay = fast ? FAST_INTERVAL_MS : SLOW_INTERVAL_MS;
        try {
            SystemSnapshot snapshot = sample(Instant.now(), delay, System.nanoTime());
            synchronized (this) {
                intervalMs = delay;
                record(snapshot);
            }
            for (AnomalyDetector.Alert alert : anomalies.check(snapshot, Instant.now())) {
                Logger.LOG.warning("[monitor] " + alert.message());
            }
        } catch (RuntimeException e) {
            // A single unreadable file must not kill the sampler.
            Logger.LOG.warning("[monitor] sampling failed", e);
        }
        synchronized (this) {
            if (started) {
                schedule(delay);
            }
        }
    }

    private SystemSnapshot sample(Instant at, long interval, long nowNanos) {
        double seconds = previousNanos == 0 ? 0 : (nowNanos - previousNanos) / 1_000_000_000.0;
        boolean ratesUsable = seconds >= 0.2;

        Map<String, ProcStat.Times> cpuTimes;
        if (windows != null) {
            cpuTimes = windowsCpuTimes();
        } else {
            String statText = read(procRoot.resolve("stat"));
            cpuTimes = statText == null ? Map.of() : ProcStat.parse(statText);
        }

        double[] loadAverage = loadAverage(read(procRoot.resolve("loadavg")));
        double cpuLoad = -1;
        double[] cores = new double[0];
        int coreCount = Runtime.getRuntime().availableProcessors();
        if (!cpuTimes.isEmpty()) {
            cpuLoad = ratesUsable ? ProcStat.usage(previousCpu.get("cpu"), cpuTimes.get("cpu")) : -1;
            List<String> coreKeys = cpuTimes.keySet().stream()
                    .filter(key -> key.length() > 3 && key.substring(3).chars().allMatch(Character::isDigit))
                    .sorted(Comparator.comparingInt(key -> Integer.parseInt(key.substring(3))))
                    .toList();
            cores = new double[coreKeys.size()];
            for (int i = 0; i < coreKeys.size(); i++) {
                cores[i] = ratesUsable ? ProcStat.usage(previousCpu.get(coreKeys.get(i)), cpuTimes.get(coreKeys.get(i))) : -1;
            }
            coreCount = coreKeys.size();
        } else {
            double load = os.getCpuLoad();
            cpuLoad = load >= 0 ? load : -1;
            if (loadAverage.length == 0) {
                double oneMinute = os.getSystemLoadAverage();
                if (oneMinute >= 0) {
                    loadAverage = new double[]{oneMinute};
                }
            }
        }

        CgroupLimits.Limits limits = CgroupLimits.read();
        SystemSnapshot.Memory memory = memory(limits);

        List<SystemSnapshot.DiskRate> disks = windows != null
                ? windowsDisks()
                : disks(previousDisk, ratesUsable, seconds);
        List<SystemSnapshot.MountUsage> mounts = mounts();
        List<SystemSnapshot.NetworkRate> networks = windows != null
                ? windowsNetworks()
                : networks(previousNet, ratesUsable, seconds);

        SystemSnapshot.Panel panel = panel();

        List<SystemSnapshot.Gpu> gpus;
        try {
            gpus = gpuReader.read();
        } catch (RuntimeException e) {
            gpus = List.of();
        }
        SystemSnapshot snapshot = new SystemSnapshot(
                at.toString(),
                interval,
                new SystemSnapshot.Host(hostInfo.os(), hostInfo.environment()),
                new SystemSnapshot.Capabilities(cores.length > 0, !disks.isEmpty(), !networks.isEmpty(),
                        gpuReader.available()),
                new SystemSnapshot.Cpu(cpuLoad, coreCount, limits.cpuQuotaCores(), cores, loadAverage),
                memory,
                disks,
                mounts,
                networks,
                gpus,
                panel);

        previousCpu = cpuTimes;
        if (windows == null) {
            previousDisk = lastDiskRaw;
            previousNet = lastNetRaw;
        }
        previousNanos = nowNanos;
        return snapshot;
    }

    private List<SystemSnapshot.DiskRate> disks(Map<String, ProcDiskStats.Counters> previous,
                                                boolean ratesUsable, double seconds) {
        lastDiskRaw = procDiskCounters();
        List<SystemSnapshot.DiskRate> out = new ArrayList<>();
        for (String name : wholeDevices()) {
            ProcDiskStats.Counters current = lastDiskRaw.get(name);
            if (current == null) {
                continue;
            }
            ProcDiskStats.Counters before = previous.get(name);
            double read = ratesUsable ? ProcDiskStats.readBytesPerSecond(before, current, seconds) : -1;
            double write = ratesUsable ? ProcDiskStats.writeBytesPerSecond(before, current, seconds) : -1;
            out.add(new SystemSnapshot.DiskRate(name, read, write));
        }
        return out;
    }

    private List<SystemSnapshot.NetworkRate> networks(Map<String, ProcNetDev.Counters> previous,
                                                       boolean ratesUsable, double seconds) {
        lastNetRaw = procNetCounters();
        List<SystemSnapshot.NetworkRate> out = new ArrayList<>();
        for (Map.Entry<String, ProcNetDev.Counters> entry : lastNetRaw.entrySet()) {
            ProcNetDev.Counters before = previous.get(entry.getKey());
            double rx = ratesUsable ? ProcNetDev.bytesPerSecond(before, entry.getValue(), seconds, false) : -1;
            double tx = ratesUsable ? ProcNetDev.bytesPerSecond(before, entry.getValue(), seconds, true) : -1;
            out.add(new SystemSnapshot.NetworkRate(entry.getKey(), rx, tx));
        }
        return out;
    }

    /// Windows per-processor times in the same shape the Linux parser returns:
    /// cumulative busy and idle, ready for a difference.
    private Map<String, ProcStat.Times> windowsCpuTimes() {
        List<WindowsMetrics.CpuTimes> times = windows == null ? List.of() : windows.cpuTimes();
        if (times.isEmpty()) {
            return Map.of();
        }
        Map<String, ProcStat.Times> out = new LinkedHashMap<>();
        long total = 0;
        long idle = 0;
        for (int index = 0; index < times.size(); index++) {
            WindowsMetrics.CpuTimes core = times.get(index);
            out.put("cpu" + index, new ProcStat.Times(core.total(), core.idle()));
            total += core.total();
            idle += core.idle();
        }
        out.put("cpu", new ProcStat.Times(total, idle));
        return out;
    }

    /// PDH reports rates, not counters: one entry for the whole disk set.
    private List<SystemSnapshot.DiskRate> windowsDisks() {
        WindowsMetrics.DiskRates rates = windows == null ? null : windows.diskRates();
        if (rates == null) {
            return List.of();
        }
        return List.of(new SystemSnapshot.DiskRate("total",
                rates.readBytesPerSecond(), rates.writeBytesPerSecond()));
    }

    private List<SystemSnapshot.NetworkRate> windowsNetworks() {
        List<WindowsMetrics.NetworkRate> rates = windows == null ? List.of() : windows.networkRates();
        List<SystemSnapshot.NetworkRate> out = new ArrayList<>(rates.size());
        for (WindowsMetrics.NetworkRate rate : rates) {
            out.add(new SystemSnapshot.NetworkRate(rate.name(),
                    rate.rxBytesPerSecond(), rate.txBytesPerSecond()));
        }
        return out;
    }

    private Map<String, ProcDiskStats.Counters> procDiskCounters() {
        String text = read(procRoot.resolve("diskstats"));
        return text == null ? Map.of() : ProcDiskStats.parse(text);
    }

    private Map<String, ProcNetDev.Counters> procNetCounters() {
        String text = read(procRoot.resolve("net").resolve("dev"));
        return text == null ? Map.of() : ProcNetDev.parse(text);
    }

    /// Whole block devices only: `/sys/block` already excludes partitions, and
    /// loop/device-mapper nodes are not disks a person recognises.
    private List<String> wholeDevices() {
        if (!Files.isDirectory(sysBlockRoot)) {
            return List.of();
        }
        try (var entries = Files.list(sysBlockRoot)) {
            return entries
                    .map(path -> path.getFileName().toString())
                    .filter(name -> !name.startsWith("loop") && !name.startsWith("dm-")
                            && !name.startsWith("ram") && !name.startsWith("zram")
                            // An optical drive is a block device but not a disk
                            // whose throughput anybody watches; the same for
                            // network block devices.
                            && !name.startsWith("sr") && !name.startsWith("nbd"))
                    .sorted()
                    .toList();
        } catch (IOException | RuntimeException e) {
            return List.of();
        }
    }

    /// The four numbers a task manager puts on its memory page: in use,
    /// available, cached (reclaimable) and free.
    ///
    /// Linux fills memory with page cache on purpose, so "total minus free"
    /// would read as permanently full. The split follows the kernel's own
    /// definition instead: `available` already includes the reclaimable cache,
    /// so `used = total - available`, and the bar is
    /// `used | cached | free` where `cached + free == available`.
    private SystemSnapshot.Memory memory(CgroupLimits.Limits limits) {
        if (windows != null) {
            WindowsMetrics.MemoryInfo info = windows.memory();
            if (info != null) {
                long total = info.totalBytes();
                long available = Math.min(info.availableBytes(), Math.max(1, total));
                long cached = Math.max(0, Math.min(info.cachedBytes(), available));
                long free = Math.max(0, available - cached);
                long used = Math.max(0, total - available);
                long swapTotal = Math.max(0, os.getTotalSwapSpaceSize());
                long swapFree = Math.max(0, os.getFreeSwapSpaceSize());
                return new SystemSnapshot.Memory(total, used, available, free, cached,
                        swapTotal, Math.max(0, swapTotal - swapFree), "host");
            }
        }
        String text = read(procRoot.resolve("meminfo"));
        ProcMeminfo.Memory host = text == null ? null : ProcMeminfo.parse(text);
        long total = host == null ? os.getTotalMemorySize() : host.totalBytes();
        long available = host == null ? os.getFreeMemorySize() : host.availableBytes();
        long free = host == null ? os.getFreeMemorySize() : host.freeBytes();
        long cached = host == null ? 0 : host.cachedBytes();
        long swapTotal = host == null ? Math.max(0, os.getTotalSwapSpaceSize()) : host.swapTotalBytes();
        long swapFree = host == null ? Math.max(0, os.getFreeSwapSpaceSize()) : host.swapFreeBytes();
        String source = "host";
        if (limits.memoryLimitBytes() > 0) {
            total = limits.memoryLimitBytes();
            long current = limits.memoryUsedBytes() >= 0
                    ? limits.memoryUsedBytes()
                    : Math.min(Math.max(0, host == null
                            ? os.getTotalMemorySize() - os.getFreeMemorySize()
                            : host.totalBytes() - host.availableBytes()), total);
            cached = limits.fileCacheBytes() >= 0
                    ? Math.max(0, Math.min(limits.fileCacheBytes(), current))
                    : 0;
            long usedByCgroup = Math.max(0, current - cached);
            available = Math.max(0, total - usedByCgroup);
            free = Math.max(0, total - usedByCgroup - cached);
            source = "cgroup";
        }
        long used = Math.max(0, total - available);
        cached = Math.max(0, Math.min(cached, Math.max(0, total - used)));
        free = Math.max(0, Math.min(free, Math.max(0, total - used - cached)));
        long swapUsed = Math.max(0, swapTotal - swapFree);
        return new SystemSnapshot.Memory(total, used, available, free, cached, swapTotal, swapUsed, source);
    }

    /// The data directory's filesystem and, when it is a different one, the
    /// root filesystem. Deduplicated by store name.
    private List<SystemSnapshot.MountUsage> mounts() {
        List<SystemSnapshot.MountUsage> out = new ArrayList<>();
        List<String> seen = new ArrayList<>();
        for (Path path : List.of(dataDir, Path.of("/"))) {
            try {
                var store = Files.getFileStore(path);
                String key = store.name() + "|" + store.type();
                if (seen.contains(key)) {
                    continue;
                }
                seen.add(key);
                long total = store.getTotalSpace();
                long usable = store.getUsableSpace();
                out.add(new SystemSnapshot.MountUsage(path.toString(), total, Math.max(0, total - usable)));
            } catch (IOException | RuntimeException ignored) {
                // A path this process cannot stat is simply not reported.
            }
        }
        return out;
    }

    private SystemSnapshot.Panel panel() {
        double cpu = os.getProcessCpuLoad();
        long rss = rssBytes();
        int threads = ManagementFactory.getThreadMXBean().getThreadCount();
        long uptimeSec = ManagementFactory.getRuntimeMXBean().getUptime() / 1000;
        return new SystemSnapshot.Panel(cpu >= 0 ? cpu : -1, rss, threads, uptimeSec);
    }

    /// Resident set size from `/proc/self/status`, or the JVM's committed
    /// memory when that file is not there.
    private long rssBytes() {
        String text = read(procRoot.resolve("self").resolve("status"));
        if (text != null) {
            for (String line : text.split("\n")) {
                if (line.startsWith("VmRSS:")) {
                    String[] fields = line.trim().split("\\s+");
                    if (fields.length >= 2) {
                        try {
                            return Long.parseLong(fields[1]) * 1024;
                        } catch (NumberFormatException ignored) {
                            // Fall through to the JVM's number.
                        }
                    }
                }
            }
        }
        return os.getCommittedVirtualMemorySize();
    }

    /// `/proc/loadavg`: `0.11 0.21 0.36 2/2586 191000`.
    static double[] loadAverage(@Nullable String text) {
        if (text == null) {
            return new double[0];
        }
        String[] fields = text.trim().split("\\s+");
        if (fields.length < 3) {
            return new double[0];
        }
        try {
            return new double[]{
                    Double.parseDouble(fields[0]),
                    Double.parseDouble(fields[1]),
                    Double.parseDouble(fields[2])};
        } catch (NumberFormatException e) {
            return new double[0];
        }
    }

    private static @Nullable String read(Path file) {
        try {
            return Files.isRegularFile(file) ? Files.readString(file) : null;
        } catch (IOException | RuntimeException e) {
            return null;
        }
    }
}
