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
package org.jackhuang.hmcl.web.transcode.quality;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jackhuang.hmcl.util.ProcessTree;
import org.jackhuang.hmcl.util.platform.Privileges;
import org.jackhuang.hmcl.web.transcode.TranscodeModule;
import org.jackhuang.hmcl.web.transcode.runtime.FfmpegProbe;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/// Runs PSNR / SSIM / XPSNR / VMAF comparisons in the background.
///
/// Each metric is a separate ffmpeg run (the filters do not compose into one useful
/// stats file), and each writes a per-frame stats file that is parsed after the process
/// exits. Progress is coarse — one metric at a time — but the job is cancellable and the
/// per-frame arrays are capped, so a 4-hour film does not become a 400k-point JSON.
@NotNullByDefault
public final class QualityService {

    private static final int MAX_FRAMES = 4000;

    private final TranscodeModule module;
    private final Path workDir;
    private final Map<String, QualityJob> jobs = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "transcode-quality");
        thread.setDaemon(true);
        return thread;
    });

    public QualityService(TranscodeModule module) {
        this.module = module;
        this.workDir = module.dataDir().resolve("tools").resolve("quality");
    }

    public QualityJob start(String reference, String encoded, List<String> metrics,
                            @Nullable String start, @Nullable String duration) throws IOException {
        FfmpegProbe.FfmpegInfo info = module.runtime().info();
        if (!info.available()) {
            throw new IOException(info.error() == null ? "ffmpeg is unavailable" : info.error());
        }
        Path referenceFile = module.media().resolve(module.mediaRoot(), reference);
        Path encodedFile = module.media().resolve(module.mediaRoot(), encoded);
        List<String> selected = metrics == null || metrics.isEmpty()
                ? List.of("psnr", "ssim") : metrics;
        for (String metric : selected) {
            if (!supported(info, metric)) {
                throw new IOException("metric is not available in this ffmpeg build: " + metric);
            }
        }
        Files.createDirectories(workDir);
        QualityJob job = new QualityJob();
        job.id = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        job.reference = module.mediaRoot().relativize(referenceFile).toString().replace('\\', '/');
        job.encoded = module.mediaRoot().relativize(encodedFile).toString().replace('\\', '/');
        job.metrics = List.copyOf(selected);
        job.createdAt = System.currentTimeMillis();
        jobs.put(job.id, job);
        executor.execute(() -> run(job, referenceFile, encodedFile, start, duration));
        return job;
    }

    public Optional<QualityJob> job(String id) {
        return Optional.ofNullable(jobs.get(id));
    }

    public boolean cancel(String id) {
        QualityJob job = jobs.get(id);
        if (job == null || job.state != QualityJob.State.RUNNING) {
            return false;
        }
        job.state = QualityJob.State.CANCELLED;
        job.message = "cancelled";
        job.endedAt = System.currentTimeMillis();
        Process process = job.process;
        if (process != null) {
            ProcessTree.terminate(process, 2);
        }
        return true;
    }

    private void run(QualityJob job, Path reference, Path encoded,
                     @Nullable String start, @Nullable String duration) {
        try {
            FfmpegProbe.FfmpegInfo info = module.runtime().info();
            Map<String, QualityJob.MetricResult> results = new LinkedHashMap<>();
            for (int index = 0; index < job.metrics.size(); index++) {
                if (job.state == QualityJob.State.CANCELLED) {
                    return;
                }
                String metric = job.metrics.get(index);
                job.message = metric;
                job.progress = (double) index / job.metrics.size();
                Path stats = workDir.resolve(job.id + "-" + metric
                        + ("vmaf".equals(metric) ? ".json" : ".log"));
                Files.deleteIfExists(stats);
                List<String> args = arguments(metric, encoded, reference, stats, start, duration);
                List<String> command = new ArrayList<>(args.size() + 1);
                command.add(info.ffmpeg().toString());
                command.addAll(args);
                Path log = workDir.resolve(job.id + "-" + metric + "-stderr.log");
                ProcessBuilder builder = new ProcessBuilder(Privileges.asAgent(command));
                builder.redirectError(ProcessBuilder.Redirect.to(log.toFile()));
                Process process = builder.start();
                job.process = process;
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.startsWith("progress=end")) {
                            job.progress = (index + 1.0) / job.metrics.size();
                        }
                    }
                }
                int code = process.waitFor();
                job.process = null;
                if (job.state == QualityJob.State.CANCELLED) {
                    Files.deleteIfExists(stats);
                    return;
                }
                if (code != 0) {
                    fail(job, metric + " exited with code " + code + tail(log));
                    return;
                }
                QualityJob.MetricResult result = parse(metric, stats);
                if (result == null) {
                    fail(job, metric + " produced no readable stats" + tail(log));
                    return;
                }
                results.put(metric, result);
            }
            job.results = Map.copyOf(results);
            job.state = QualityJob.State.DONE;
            job.progress = 1;
            job.message = "done";
            job.endedAt = System.currentTimeMillis();
        } catch (Exception e) {
            if (job.state != QualityJob.State.CANCELLED) {
                fail(job, e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
            }
        }
    }

    private static List<String> arguments(String metric, Path encoded, Path reference, Path stats,
                                          @Nullable String start, @Nullable String duration) {
        List<String> args = new ArrayList<>();
        args.add("-hide_banner");
        args.add("-nostdin");
        args.add("-nostats");
        args.add("-progress");
        args.add("pipe:1");
        if (start != null && !start.isBlank()) {
            args.add("-ss");
            args.add(start);
        }
        args.add("-i");
        args.add(encoded.toString());
        if (start != null && !start.isBlank()) {
            args.add("-ss");
            args.add(start);
        }
        args.add("-i");
        args.add(reference.toString());
        String filter = switch (metric) {
            case "psnr" -> "[0:v][1:v]psnr=stats_file=" + escape(stats);
            case "ssim" -> "[0:v][1:v]ssim=stats_file=" + escape(stats);
            case "xpsnr" -> "[0:v][1:v]xpsnr=stats_file=" + escape(stats);
            case "vmaf" -> "[0:v][1:v]libvmaf=log_path=" + escape(stats) + ":log_fmt=json";
            default -> throw new IllegalArgumentException("unknown metric " + metric);
        };
        args.add("-lavfi");
        args.add(filter);
        args.add("-an");
        args.add("-sn");
        args.add("-dn");
        if (duration != null && !duration.isBlank()) {
            args.add("-t");
            args.add(duration);
        }
        args.add("-f");
        args.add("null");
        args.add("-");
        return args;
    }

    private static boolean supported(FfmpegProbe.FfmpegInfo info, String metric) {
        if ("vmaf".equals(metric)) {
            return info.filters().contains("libvmaf");
        }
        return info.filters().contains(metric);
    }

    private static @Nullable QualityJob.MetricResult parse(String metric, Path stats) throws IOException {
        if (!Files.isRegularFile(stats)) {
            return null;
        }
        if ("vmaf".equals(metric)) {
            return parseVmaf(stats);
        }
        return parseFrames(metric, stats);
    }

    static @Nullable QualityJob.MetricResult parseFrames(String metric, Path stats) throws IOException {
        if ("xpsnr".equals(metric)) {
            return parseXpsnr(stats);
        }
        Pattern pattern = "ssim".equals(metric)
                ? Pattern.compile("n:(\\d+).*?All:([0-9.]+)")
                : Pattern.compile("n:(\\d+).*?psnr_avg:([0-9.]+|inf)");
        List<QualityJob.Frame> frames = new ArrayList<>();
        double sum = 0;
        for (String line : Files.readAllLines(stats, StandardCharsets.UTF_8)) {
            Matcher matcher = pattern.matcher(line);
            if (matcher.find()) {
                double value = value(matcher.group(2));
                if (!Double.isFinite(value)) {
                    continue;
                }
                frames.add(new QualityJob.Frame(Integer.parseInt(matcher.group(1)), value));
                sum += value;
            }
        }
        if (frames.isEmpty()) {
            return null;
        }
        return new QualityJob.MetricResult(sum / frames.size(), cap(frames));
    }

    static @Nullable QualityJob.MetricResult parseXpsnr(Path stats) throws IOException {
        Pattern pattern = Pattern.compile(
                "n:\\s*(\\d+).*?XPSNR\\s+y:\\s*([0-9.]+).*?XPSNR\\s+u:\\s*([0-9.]+).*?XPSNR\\s+v:\\s*([0-9.]+)");
        List<QualityJob.Frame> frames = new ArrayList<>();
        double sum = 0;
        for (String line : Files.readAllLines(stats, StandardCharsets.UTF_8)) {
            Matcher matcher = pattern.matcher(line);
            if (matcher.find()) {
                double value = (value(matcher.group(2)) + value(matcher.group(3))
                        + value(matcher.group(4))) / 3.0;
                if (!Double.isFinite(value)) {
                    continue;
                }
                frames.add(new QualityJob.Frame(Integer.parseInt(matcher.group(1)), value));
                sum += value;
            }
        }
        if (frames.isEmpty()) {
            return null;
        }
        return new QualityJob.MetricResult(sum / frames.size(), cap(frames));
    }

    static @Nullable QualityJob.MetricResult parseVmaf(Path stats) throws IOException {
        JsonObject root = JsonParser.parseString(Files.readString(stats, StandardCharsets.UTF_8)).getAsJsonObject();
        List<QualityJob.Frame> frames = new ArrayList<>();
        double sum = 0;
        if (root.has("frames") && root.get("frames").isJsonArray()) {
            JsonArray array = root.getAsJsonArray("frames");
            for (JsonElement element : array) {
                if (!element.isJsonObject()) {
                    continue;
                }
                JsonObject frame = element.getAsJsonObject();
                if (!frame.has("vmaf")) {
                    continue;
                }
                double value = frame.get("vmaf").getAsDouble();
                int n = frame.has("frameNum") ? frame.get("frameNum").getAsInt() : frames.size() + 1;
                frames.add(new QualityJob.Frame(n, value));
                sum += value;
            }
        }
        if (frames.isEmpty()) {
            return null;
        }
        return new QualityJob.MetricResult(sum / frames.size(), cap(frames));
    }

    /// `inf` is what ffmpeg prints for a lossless comparison; cap it at 100 rather
    /// than emit a non-finite number the JSON contract cannot carry.
    private static double value(String text) {
        if ("inf".equalsIgnoreCase(text.trim())) {
            return 100.0;
        }
        try {
            return Double.parseDouble(text);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static List<QualityJob.Frame> cap(List<QualityJob.Frame> frames) {
        if (frames.size() <= MAX_FRAMES) {
            return List.copyOf(frames);
        }
        int step = (int) Math.ceil((double) frames.size() / MAX_FRAMES);
        List<QualityJob.Frame> result = new ArrayList<>();
        for (int i = 0; i < frames.size(); i += step) {
            result.add(frames.get(i));
        }
        return List.copyOf(result);
    }

    private static void fail(QualityJob job, String message) {
        if (job.state == QualityJob.State.CANCELLED) {
            return;
        }
        job.state = QualityJob.State.FAILED;
        job.error = message;
        job.message = "failed";
        job.endedAt = System.currentTimeMillis();
    }

    private static String tail(Path log) {
        try {
            List<String> lines = Files.readAllLines(log, StandardCharsets.UTF_8);
            int from = Math.max(0, lines.size() - 10);
            return "\n" + String.join("\n", lines.subList(from, lines.size()));
        } catch (IOException e) {
            return "";
        }
    }

    private static String escape(Path path) {
        return path.toAbsolutePath().toString().replace("\\", "/").replace(":", "\\:");
    }
}