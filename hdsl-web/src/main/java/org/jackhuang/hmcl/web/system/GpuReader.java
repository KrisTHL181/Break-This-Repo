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

import org.jackhuang.hmcl.util.platform.Executables;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/// A source of GPU readings for the system monitor.
///
/// The model is "best effort, never fabricate": a machine without `nvidia-smi`, without
/// `/sys/class/drm`, or without permission to read it answers an empty list, and the
/// page hides the card. Encoder/decoder utilization is offered where the driver
/// exposes it (NVIDIA), and left `-1` where it does not (Intel sysfs).
@NotNullByDefault
public interface GpuReader {

    /// One sample. Never `null`; an unavailable backend returns an empty list.
    List<SystemSnapshot.Gpu> read();

    /// Whether this host has a reader worth showing at all.
    boolean available();
}

/// Builds the reader for the current host. Kept as a factory so a test can substitute a
/// fake without touching [SystemMetrics].
@NotNullByDefault
final class GpuReaders {

    private GpuReaders() {
    }

    static GpuReader create(HostInfo hostInfo) {
        String os = hostInfo.os();
        GpuReader reader;
        if ("linux".equals(os)) {
            reader = new LinuxGpuReader(Path.of("/sys/class/drm"));
        } else if ("windows".equals(os)) {
            reader = new NvidiaGpuReader();
        } else {
            reader = new EmptyGpuReader();
        }
        return new CachedGpuReader(reader);
    }

    /// Samples at most once every two seconds: `nvidia-smi` is a process spawn, and the
    /// 1s monitor tick must not turn into a fork bomb.
    static final class CachedGpuReader implements GpuReader {
        private final GpuReader delegate;
        private volatile List<SystemSnapshot.Gpu> cache = List.of();
        private volatile long readAt;

        CachedGpuReader(GpuReader delegate) {
            this.delegate = delegate;
        }

        @Override
        public List<SystemSnapshot.Gpu> read() {
            long now = System.currentTimeMillis();
            if (now - readAt < 2_000) {
                return cache;
            }
            synchronized (this) {
                if (now - readAt < 2_000) {
                    return cache;
                }
                cache = delegate.read();
                readAt = now;
                return cache;
            }
        }

        @Override
        public boolean available() {
            return delegate.available();
        }
    }

    static final class EmptyGpuReader implements GpuReader {
        @Override
        public List<SystemSnapshot.Gpu> read() {
            return List.of();
        }

        @Override
        public boolean available() {
            return false;
        }
    }

    static final class NvidiaGpuReader implements GpuReader {
        private volatile boolean probed;
        private volatile boolean available;

        @Override
        public List<SystemSnapshot.Gpu> read() {
            if (Executables.resolve("nvidia-smi").isEmpty()) {
                available = false;
                return List.of();
            }
            try {
                List<String> lines = run(List.of(
                        "--query-gpu=index,name,utilization.gpu,utilization.encoder,utilization.decoder,"
                                + "memory.used,memory.total,temperature.gpu,power.draw",
                        "--format=csv,noheader,nounits"));
                available = true;
                return parse(lines);
            } catch (Exception e) {
                try {
                    List<String> lines = run(List.of(
                            "--query-gpu=index,name,utilization.gpu,memory.used,memory.total,temperature.gpu,power.draw",
                            "--format=csv,noheader,nounits"));
                    available = true;
                    return parseShort(lines);
                } catch (Exception ignored) {
                    available = false;
                    return List.of();
                }
            } finally {
                probed = true;
            }
        }

        @Override
        public boolean available() {
            return probed ? available : Executables.resolve("nvidia-smi").isPresent();
        }

        private static List<String> run(List<String> query) throws IOException, InterruptedException {
            List<String> command = new ArrayList<>();
            command.add(Executables.resolve("nvidia-smi").map(Path::toString).orElse("nvidia-smi"));
            command.addAll(query);
            Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
            java.util.concurrent.ExecutorService reader =
                    java.util.concurrent.Executors.newSingleThreadExecutor(runnable -> {
                        Thread thread = new Thread(runnable, "gpu-nvidia-reader");
                        thread.setDaemon(true);
                        return thread;
                    });
            try {
                java.util.concurrent.Future<List<String>> future = reader.submit(
                        () -> process.inputReader(StandardCharsets.UTF_8).lines().toList());
                if (!process.waitFor(3, TimeUnit.SECONDS)) {
                    process.destroyForcibly();
                    future.cancel(true);
                    throw new IOException("nvidia-smi timed out");
                }
                return future.get(1, TimeUnit.SECONDS);
            } catch (java.util.concurrent.ExecutionException | java.util.concurrent.TimeoutException e) {
                throw new IOException("could not read nvidia-smi output", e);
            } finally {
                process.destroyForcibly();
                reader.shutdownNow();
            }
        }

        static List<SystemSnapshot.Gpu> parse(List<String> lines) {
            List<SystemSnapshot.Gpu> result = new ArrayList<>();
            for (String line : lines) {
                String[] parts = line.split(",");
                if (parts.length < 9) {
                    continue;
                }
                result.add(new SystemSnapshot.Gpu(
                        integer(parts[0], result.size()),
                        parts[1].trim(),
                        "NVIDIA",
                        "nvidia-smi",
                        percent(parts[2]),
                        percent(parts[3]),
                        percent(parts[4]),
                        mib(parts[5]),
                        mib(parts[6]),
                        number(parts[7]),
                        number(parts[8])));
            }
            return result;
        }

        static List<SystemSnapshot.Gpu> parseShort(List<String> lines) {
            List<SystemSnapshot.Gpu> result = new ArrayList<>();
            for (String line : lines) {
                String[] parts = line.split(",");
                if (parts.length < 7) {
                    continue;
                }
                result.add(new SystemSnapshot.Gpu(
                        integer(parts[0], result.size()),
                        parts[1].trim(),
                        "NVIDIA",
                        "nvidia-smi",
                        percent(parts[2]),
                        -1,
                        -1,
                        mib(parts[3]),
                        mib(parts[4]),
                        number(parts[5]),
                        number(parts[6])));
            }
            return result;
        }

        private static double percent(String value) {
            double parsed = number(value);
            return parsed < 0 ? -1 : parsed / 100.0;
        }

        private static long mib(String value) {
            double number = number(value);
            return number < 0 ? -1 : Math.round(number * 1024 * 1024);
        }

        private static double number(String value) {
            String trimmed = value.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("[")) {
                return -1;
            }
            try {
                return Double.parseDouble(trimmed);
            } catch (NumberFormatException e) {
                return -1;
            }
        }

        private static int integer(String value, int fallback) {
            double parsed = number(value);
            return parsed < 0 ? fallback : (int) parsed;
        }
    }

    /// AMD and Intel expose different files; both are read from the DRM card nodes.
    static final class LinuxGpuReader implements GpuReader {
        private final Path drm;

        LinuxGpuReader(Path drm) {
            this.drm = drm;
        }

        @Override
        public List<SystemSnapshot.Gpu> read() {
            List<SystemSnapshot.Gpu> result = new ArrayList<>();
            if (Executables.resolve("nvidia-smi").isPresent()) {
                result.addAll(new NvidiaGpuReader().read());
            }
            if (!Files.isDirectory(drm)) {
                return result;
            }
            try (var stream = Files.list(drm)) {
                List<Path> cards = stream
                        .filter(path -> path.getFileName().toString().matches("card[0-9]+"))
                        .sorted()
                        .toList();
                for (Path card : cards) {
                    Path device = card.resolve("device");
                    if (!Files.isDirectory(device)) {
                        continue;
                    }
                    String vendor = text(device.resolve("vendor"));
                    if ("0x1002".equalsIgnoreCase(vendor)) {
                        result.add(amd(device, result.size()));
                    } else if ("0x8086".equalsIgnoreCase(vendor)) {
                        result.add(intel(device, result.size()));
                    } else if (vendor.isBlank()) {
                        // sysfs is sometimes masked/partially visible inside a container.
                        // Showing an "unknown" card with empty charts is more honest than
                        // claiming there is no GPU when /dev/dri is mounted.
                        result.add(unknown(device, result.size(), card.getFileName().toString()));
                    }
                }
            } catch (IOException ignored) {
                return result;
            }
            if (result.isEmpty()) {
                // Some container setups expose only renderD* nodes; card* may be absent.
                try (var stream = Files.list(drm)) {
                    List<Path> renders = stream
                            .filter(path -> path.getFileName().toString().matches("renderD[0-9]+"))
                            .sorted()
                            .toList();
                    for (Path render : renders) {
                        Path device = render.resolve("device");
                        if (Files.isDirectory(device)) {
                            result.add(unknown(device, result.size(), render.getFileName().toString()));
                        }
                    }
                } catch (IOException ignored) {
                }
            }
            if (result.isEmpty() && Files.isDirectory(Path.of("/dev/dri"))) {
                // The device node is mounted but sysfs is masked: still show a GPU panel
                // with empty charts instead of claiming the host has no GPU.
                result.add(new SystemSnapshot.Gpu(0, "DRM GPU", "unknown", "dev/dri",
                        -1, -1, -1, -1, -1, -1, -1));
            }
            return result;
        }

        @Override
        public boolean available() {
            return Executables.resolve("nvidia-smi").isPresent() || Files.isDirectory(drm);
        }

        private static SystemSnapshot.Gpu amd(Path device, int index) {
            double utilization = number(text(device.resolve("gpu_busy_percent")));
            long used = numberLong(text(device.resolve("mem_info_vram_used")));
            long total = numberLong(text(device.resolve("mem_info_vram_total")));
            double temp = hwmon(device, "temp1_input");
            return new SystemSnapshot.Gpu(index, "AMD GPU", "AMD", "sysfs",
                    utilization < 0 ? -1 : utilization / 100.0, -1, -1,
                    used, total, temp, -1);
        }

        private static SystemSnapshot.Gpu unknown(Path device, int index, String node) {
            double temp = hwmon(device, "temp1_input");
            return new SystemSnapshot.Gpu(index, "DRM " + node, "unknown", "sysfs",
                    -1, -1, -1, -1, -1, temp, -1);
        }

        private static SystemSnapshot.Gpu intel(Path device, int index) {
            double temp = hwmon(device, "temp1_input");
            // Intel's global utilization is not a stable sysfs file; frequency is the
            // honest reading, and the page labels it as such.
            return new SystemSnapshot.Gpu(index, "Intel GPU", "Intel", "sysfs",
                    -1, -1, -1, -1, -1, temp, -1);
        }

        private static double hwmon(Path device, String file) {
            Path hwmon = device.resolve("hwmon");
            if (!Files.isDirectory(hwmon)) {
                return -1;
            }
            try (var stream = Files.list(hwmon)) {
                for (Path dir : (Iterable<Path>) stream::iterator) {
                    double value = number(text(dir.resolve(file)));
                    if (value >= 0) {
                        return value / 1000.0;
                    }
                }
            } catch (IOException ignored) {
            }
            return -1;
        }

        private static String text(Path path) {
            try {
                return Files.isRegularFile(path) ? Files.readString(path, StandardCharsets.UTF_8).trim() : "";
            } catch (IOException e) {
                return "";
            }
        }

        private static double number(String value) {
            if (value == null || value.isBlank()) {
                return -1;
            }
            try {
                return Double.parseDouble(value.trim());
            } catch (NumberFormatException e) {
                return -1;
            }
        }

        private static long numberLong(String value) {
            double parsed = number(value);
            return parsed < 0 ? -1 : Math.round(parsed);
        }
    }
}