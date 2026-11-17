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
package org.jackhuang.hmcl.web.transcode.queue;

import org.jackhuang.hmcl.util.platform.OperatingSystem;
import org.jackhuang.hmcl.web.config.ServerConfig;
import org.jackhuang.hmcl.web.event.EventBus;
import org.jackhuang.hmcl.web.transcode.TranscodeConsent;
import org.jackhuang.hmcl.web.transcode.TranscodeModule;
import org.jackhuang.hmcl.web.transcode.TranscodeTerms;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TranscodeQueueTest {

    private static void script(Path file, String body) throws Exception {
        Files.writeString(file, body);
        Files.setPosixFilePermissions(file, PosixFilePermissions.fromString("rwxr-xr-x"));
    }

    private static TranscodeConsent consent() {
        return new TranscodeConsent(TranscodeTerms.VERSION, TranscodeTerms.hash(),
                "2026-11-10T00:00:00Z", "tester", "127.0.0.1",
                "checkbox", "0.6.82", "UA", "abc1234", true);
    }

    @Test
    void runsAFakeEncodeToDone(@TempDir Path dir) throws Exception {
        Assumptions.assumeFalse(OperatingSystem.CURRENT_OS == OperatingSystem.WINDOWS,
                "the fake ffmpeg uses a POSIX shell script");

        Path media = Files.createDirectory(dir.resolve("media"));
        Path input = media.resolve("input.mp4");
        Files.writeString(input, "source");
        Path bin = Files.createDirectory(dir.resolve("bin"));
        Path ffmpeg = bin.resolve("ffmpeg");
        Path ffprobe = bin.resolve("ffprobe");
        script(ffmpeg, """
                #!/bin/sh
                case "$*" in
                  *-version*) echo "ffmpeg version 7.1.5"; exit 0 ;;
                  *-encoders*) echo " V....D libx264 H.264"; exit 0 ;;
                  *-hwaccels*) echo "Hardware acceleration methods:"; echo "vaapi"; exit 0 ;;
                  *-filters*) echo " TSC scale V->V Scale"; exit 0 ;;
                esac
                for last; do :; done
                echo "out_time_us=1000000"
                echo "speed=2.0x"
                echo "progress=end"
                printf x > "$last"
                exit 0
                """);
        script(ffprobe, """
                #!/bin/sh
                echo '{"streams":[{"codec_type":"video"},{"codec_type":"audio"}],"format":{"duration":"2.0"}}'
                """);

        ServerConfig config = new ServerConfig();
        config.dataDir = dir.resolve("data");
        config.transcode.ffmpegPath = ffmpeg.toString();
        config.transcode.mediaRoot = media.toString();
        config.transcode.maxConcurrent = 1;

        TranscodeModule module = new TranscodeModule(config, new EventBus());
        module.feature().enable(consent());
        module.queue().start();
        try {
            var preset = module.presets().get("h264-quality").orElseThrow().preset();
            var job = module.queue().submit("input.mp4", "", "h264-quality", preset);
            module.queue().startQueue();

            String state = "";
            for (int i = 0; i < 100; i++) {
                Thread.sleep(50);
                var current = module.queue().jobJson(job.get("id").getAsString()).orElseThrow();
                state = current.get("state").getAsString();
                if ("done".equals(state) || "failed".equals(state)) {
                    break;
                }
            }
            assertEquals("done", state, module.queue().snapshot().toString());
            var jobJson = module.queue().jobJson(job.get("id").getAsString()).orElseThrow();
            Path output = media.resolve(jobJson.get("output").getAsString());
            assertTrue(Files.isRegularFile(output));
            assertTrue(Files.size(output) > 0);
            String log = module.queue().log(job.get("id").getAsString(), 100);
            assertTrue(log.contains("ffmpeg"), log);
            assertEquals(1.0, jobJson.get("fraction").getAsDouble(), 0.0001);
        } finally {
            module.queue().stop();
        }
    }
    @Test
    void fullAutoFallsBackFromVaapiToSoftware(@TempDir Path dir) throws Exception {
        Assumptions.assumeFalse(OperatingSystem.CURRENT_OS == OperatingSystem.WINDOWS,
                "the fake ffmpeg uses a POSIX shell script");

        Path media = Files.createDirectory(dir.resolve("media"));
        Files.writeString(media.resolve("input.mp4"), "source");
        Path bin = Files.createDirectory(dir.resolve("bin"));
        Path ffmpeg = bin.resolve("ffmpeg");
        Path ffprobe = bin.resolve("ffprobe");
        script(ffmpeg, """
                #!/bin/sh
                case "$*" in
                  *-version*) echo "ffmpeg version 7.1.5"; exit 0 ;;
                  *-encoders*) echo " V....D libx264 H.264"; exit 0 ;;
                  *-hwaccels*) echo "Hardware acceleration methods:"; echo "vaapi"; exit 0 ;;
                  *-filters*) echo " TSC scale V->V Scale"; exit 0 ;;
                  *hevc_vaapi*) echo "Invalid FrameType:0"; exit 1 ;;
                esac
                for last; do :; done
                echo "out_time_us=1000000"
                echo "speed=2.0x"
                echo "progress=end"
                printf x > "$last"
                exit 0
                """);
        script(ffprobe, """
                #!/bin/sh
                echo '{"streams":[{"codec_type":"video"},{"codec_type":"audio"}],"format":{"duration":"2.0"}}'
                """);

        ServerConfig config = new ServerConfig();
        config.dataDir = dir.resolve("data");
        config.transcode.ffmpegPath = ffmpeg.toString();
        config.transcode.mediaRoot = media.toString();
        config.transcode.failurePolicy = "full-auto";

        TranscodeModule module = new TranscodeModule(config, new EventBus());
        module.feature().enable(consent());
        module.queue().start();
        try {
            var preset = module.presets().get("hevc-vaapi").orElseThrow().preset();
            var job = module.queue().submit("input.mp4", "", "hevc-vaapi", preset);
            module.queue().startQueue();
            String state = "";
            for (int i = 0; i < 100; i++) {
                Thread.sleep(50);
                var current = module.queue().jobJson(job.get("id").getAsString()).orElseThrow();
                state = current.get("state").getAsString();
                if ("done".equals(state) || "failed".equals(state)) {
                    break;
                }
            }
            var finalJob = module.queue().jobJson(job.get("id").getAsString()).orElseThrow();
            assertEquals("done", state, module.queue().snapshot().toString());
            assertEquals("hevc_vaapi", finalJob.get("fallbackFrom").getAsString());
            assertEquals("libx265", finalJob.get("fallbackTo").getAsString());
            assertTrue(Files.isRegularFile(media.resolve(finalJob.get("output").getAsString())));
        } finally {
            module.queue().stop();
        }
    }

    @Test
    void timeoutAutoFallsBackAfterTheDecisionWindow(@TempDir Path dir) throws Exception {
        Assumptions.assumeFalse(OperatingSystem.CURRENT_OS == OperatingSystem.WINDOWS,
                "the fake ffmpeg uses a POSIX shell script");

        Path media = Files.createDirectory(dir.resolve("media"));
        Files.writeString(media.resolve("input.mp4"), "source");
        Path bin = Files.createDirectory(dir.resolve("bin"));
        Path ffmpeg = bin.resolve("ffmpeg");
        Path ffprobe = bin.resolve("ffprobe");
        script(ffmpeg, """
                #!/bin/sh
                case "$*" in
                  *-version*) echo "ffmpeg version 7.1.5"; exit 0 ;;
                  *-encoders*) echo " V....D libx264 H.264"; exit 0 ;;
                  *-hwaccels*) echo "Hardware acceleration methods:"; echo "vaapi"; exit 0 ;;
                  *-filters*) echo " TSC scale V->V Scale"; exit 0 ;;
                  *hevc_vaapi*) echo "Invalid FrameType:0"; exit 1 ;;
                esac
                for last; do :; done
                echo "out_time_us=1000000"
                echo "speed=2.0x"
                echo "progress=end"
                printf x > "$last"
                exit 0
                """);
        script(ffprobe, """
                #!/bin/sh
                echo '{"streams":[{"codec_type":"video"},{"codec_type":"audio"}],"format":{"duration":"2.0"}}'
                """);

        ServerConfig config = new ServerConfig();
        config.dataDir = dir.resolve("data");
        config.transcode.ffmpegPath = ffmpeg.toString();
        config.transcode.mediaRoot = media.toString();
        config.transcode.failurePolicy = "timeout-auto";
        config.transcode.failureTimeoutMinutes = 1;

        TranscodeModule module = new TranscodeModule(config, new EventBus());
        module.feature().enable(consent());
        module.queue().start();
        try {
            var preset = module.presets().get("hevc-vaapi").orElseThrow().preset();
            var job = module.queue().submit("input.mp4", "", "hevc-vaapi", preset);
            String id = job.get("id").getAsString();
            module.queue().startQueue();

            String state = "";
            for (int i = 0; i < 100; i++) {
                Thread.sleep(50);
                state = module.queue().jobJson(id).orElseThrow().get("state").getAsString();
                if ("waiting_decision".equals(state) || "failed".equals(state)) {
                    break;
                }
            }
            assertEquals("waiting_decision", state, module.queue().snapshot().toString());

            module.queue().onDecisionTimeout(id);
            for (int i = 0; i < 100; i++) {
                Thread.sleep(50);
                state = module.queue().jobJson(id).orElseThrow().get("state").getAsString();
                if ("done".equals(state) || "failed".equals(state)) {
                    break;
                }
            }
            var finalJob = module.queue().jobJson(id).orElseThrow();
            assertEquals("done", state, module.queue().snapshot().toString());
            assertEquals("libx265", finalJob.get("fallbackTo").getAsString());
        } finally {
            module.queue().stop();
        }
    }

    @Test
    void repeatedSubmissionsReserveDistinctOutputs(@TempDir Path dir) throws Exception {
        Path media = Files.createDirectory(dir.resolve("media"));
        Files.writeString(media.resolve("input.mp4"), "source");

        ServerConfig config = new ServerConfig();
        config.dataDir = dir.resolve("data");
        config.transcode.mediaRoot = media.toString();

        TranscodeModule module = new TranscodeModule(config, new EventBus());
        module.feature().enable(consent());
        module.queue().start();
        try {
            var preset = module.presets().get("h264-quality").orElseThrow().preset();
            String first = module.queue().submit("input.mp4", "", "h264-quality", preset)
                    .get("output").getAsString();
            String second = module.queue().submit("input.mp4", "", "h264-quality", preset)
                    .get("output").getAsString();
            String third = module.queue().submit("input.mp4", "", "h264-quality", preset)
                    .get("output").getAsString();
            assertEquals(3, java.util.Set.of(first, second, third).size(),
                    "queued jobs must not share an output path");
            assertEquals("input_transcoded.mkv", first);
            assertEquals("input_transcoded_1.mkv", second);
            assertEquals("input_transcoded_2.mkv", third);
        } finally {
            module.queue().stop();
        }
    }

    @Test
    void aVideoOnlySourceDoesNotFailAnAudioCopyPreset(@TempDir Path dir) throws Exception {
        Assumptions.assumeFalse(OperatingSystem.CURRENT_OS == OperatingSystem.WINDOWS,
                "the fake ffmpeg uses a POSIX shell script");

        Path media = Files.createDirectory(dir.resolve("media"));
        Files.writeString(media.resolve("input.mp4"), "source");
        Path bin = Files.createDirectory(dir.resolve("bin"));
        Path ffmpeg = bin.resolve("ffmpeg");
        Path ffprobe = bin.resolve("ffprobe");
        script(ffmpeg, """
                #!/bin/sh
                case "$*" in
                  *-version*) echo "ffmpeg version 7.1.5"; exit 0 ;;
                  *-encoders*) echo " V....D libx264 H.264"; exit 0 ;;
                  *-hwaccels*) echo "Hardware acceleration methods:"; echo "vaapi"; exit 0 ;;
                  *-filters*) echo " TSC scale V->V Scale"; exit 0 ;;
                esac
                for last; do :; done
                echo "out_time_us=1000000"
                echo "progress=end"
                printf x > "$last"
                exit 0
                """);
        // Video only: no audio stream in the source or the output.
        script(ffprobe, """
                #!/bin/sh
                echo '{"streams":[{"codec_type":"video"}],"format":{"duration":"2.0"}}'
                """);

        ServerConfig config = new ServerConfig();
        config.dataDir = dir.resolve("data");
        config.transcode.ffmpegPath = ffmpeg.toString();
        config.transcode.mediaRoot = media.toString();

        TranscodeModule module = new TranscodeModule(config, new EventBus());
        module.feature().enable(consent());
        module.queue().start();
        try {
            var preset = module.presets().get("h264-quality").orElseThrow().preset();
            var job = module.queue().submit("input.mp4", "", "h264-quality", preset);
            String id = job.get("id").getAsString();
            module.queue().startQueue();
            String state = "";
            for (int i = 0; i < 100; i++) {
                Thread.sleep(50);
                state = module.queue().jobJson(id).orElseThrow().get("state").getAsString();
                if ("done".equals(state) || "failed".equals(state)) {
                    break;
                }
            }
            assertEquals("done", state, module.queue().snapshot().toString());
        } finally {
            module.queue().stop();
        }
    }

    /// Queue housekeeping is pure state: no ffmpeg child is needed, so this runs on
    /// Windows too (unlike the fake-shell encode tests above).
    @Test
    void removesClearsAndRePresetsJobs(@TempDir Path dir) throws Exception {
        Path media = Files.createDirectory(dir.resolve("media"));
        Files.writeString(media.resolve("input.mp4"), "source");

        ServerConfig config = new ServerConfig();
        config.dataDir = dir.resolve("data");
        config.transcode.mediaRoot = media.toString();
        TranscodeModule module = new TranscodeModule(config, new EventBus());
        module.feature().enable(consent());
        module.queue().start();
        try {
            var preset = module.presets().get("h264-quality").orElseThrow().preset();
            var job = module.queue().submit("input.mp4", "", "h264-quality", preset);
            String id = job.get("id").getAsString();
            String originalOutput = job.get("output").getAsString();

            // A job keeps its submitted snapshot until it is explicitly reset.
            var replacement = preset.copy();
            replacement.name = "custom mp4";
            replacement.container = "mp4";
            replacement.subtitleMode = "none";
            assertTrue(module.queue().resetPreset(id, "custom-mp4", replacement));
            var reset = module.queue().jobJson(id).orElseThrow();
            assertEquals("queued", reset.get("state").getAsString());
            assertEquals("custom-mp4", reset.get("presetId").getAsString());
            assertEquals("mp4", reset.getAsJsonObject("preset").get("container").getAsString());
            assertTrue(!originalOutput.equals(reset.get("output").getAsString()),
                    "reset must re-plan the output for the new container");

            // Removing releases the reservation, so a fresh submit can reuse the name.
            assertTrue(module.queue().remove(id));
            assertTrue(module.queue().jobJson(id).isEmpty());
            var again = module.queue().submit("input.mp4", "", "h264-quality", preset);
            assertEquals(originalOutput, again.get("output").getAsString());

            // clearTerminal drops finished records but leaves the queue usable.
            assertTrue(module.queue().cancel(again.get("id").getAsString()));
            assertEquals(1, module.queue().clearTerminal());
            assertEquals(0, module.queue().snapshot().getAsJsonArray("jobs").size());
        } finally {
            module.queue().stop();
        }
    }

    /// The two promises the reset dialog makes: an already produced file is never
    /// overwritten, and tool jobs have no preset to replace. Pure state, no ffmpeg.
    @Test
    void resetPresetKeepsTheOldOutputAndRefusesToolJobs(@TempDir Path dir) throws Exception {
        Path media = Files.createDirectory(dir.resolve("media"));
        Files.writeString(media.resolve("input.mp4"), "source");

        ServerConfig config = new ServerConfig();
        config.dataDir = dir.resolve("data");
        config.transcode.mediaRoot = media.toString();
        TranscodeModule module = new TranscodeModule(config, new EventBus());
        module.feature().enable(consent());
        module.queue().start();
        try {
            var preset = module.presets().get("h264-quality").orElseThrow().preset();
            var job = module.queue().submit("input.mp4", "", "h264-quality", preset);
            String id = job.get("id").getAsString();
            String firstOutput = job.get("output").getAsString();

            // Stop it (the operator's "效果不对" path) and leave the old file behind.
            assertTrue(module.queue().cancel(id));
            Path produced = media.resolve(firstOutput);
            Files.writeString(produced, "already encoded");

            var replacement = preset.copy();
            replacement.name = "same again";
            assertTrue(module.queue().resetPreset(id, "h264-quality", replacement));
            var after = module.queue().jobJson(id).orElseThrow();
            assertEquals("queued", after.get("state").getAsString());
            assertTrue(!firstOutput.equals(after.get("output").getAsString()),
                    "reset must not reuse a name that already exists on disk");
            assertEquals("already encoded", Files.readString(produced),
                    "reset must not touch the file the previous encode produced");

            // A tool job has no preset of its own: reset refuses, remove still works.
            var tool = module.queue().submitCommand("提取", "extract.mkv",
                    java.util.List.of("ffmpeg", "-i", "input.mp4", "extract.mkv"), "extract");
            String toolId = tool.get("id").getAsString();
            assertTrue(!module.queue().resetPreset(toolId, "h264-quality", preset),
                    "tool jobs must not be re-presetted");
            assertTrue(!module.queue().remove("no-such-job"));
            assertTrue(module.queue().remove(toolId));

            // Nothing terminal is left, so clearTerminal is a no-op and the queued job survives.
            assertEquals(0, module.queue().clearTerminal());
            assertEquals(1, module.queue().snapshot().getAsJsonArray("jobs").size());
        } finally {
            module.queue().stop();
        }
    }

    /// Retry must not turn a tool job into a broken preset job, and must not claim an
    /// output name that another job has taken since the cancel released it.
    @Test
    void retryKeepsToolJobsAndAvoidsTakenOutputNames(@TempDir Path dir) throws Exception {
        Path media = Files.createDirectory(dir.resolve("media"));
        Files.writeString(media.resolve("input.mp4"), "source");

        ServerConfig config = new ServerConfig();
        config.dataDir = dir.resolve("data");
        config.transcode.mediaRoot = media.toString();
        TranscodeModule module = new TranscodeModule(config, new EventBus());
        module.feature().enable(consent());
        module.queue().start();
        try {
            var args = java.util.List.of("ffmpeg", "-i", "input.mp4", "extract.mkv");
            var tool = module.queue().submitCommand("提取 0:v:0 · input.mp4", "extract.mkv", args, "extract");
            String toolId = tool.get("id").getAsString();

            // The label is not a path: after this cancel the job must stay a tool job.
            assertTrue(module.queue().cancel(toolId));

            // Someone else takes the name the cancelled job just released.
            var second = module.queue().submitCommand("提取 0:a:0 · input.mp4", "extract.mkv", args, "extract");
            assertEquals("extract.mkv", second.get("output").getAsString());

            assertTrue(module.queue().retry(toolId));
            var back = module.queue().jobJson(toolId).orElseThrow();
            assertEquals("queued", back.get("state").getAsString());
            assertEquals("extract", back.get("tool").getAsString(),
                    "retry must keep a tool job a tool job");
            assertEquals("ffmpeg", back.getAsJsonArray("command").get(0).getAsString(),
                    "retry must keep the tool argv");
            assertTrue(!second.get("output").getAsString().equals(back.get("output").getAsString()),
                    "retry must not claim a name another job already reserved");
        } finally {
            module.queue().stop();
        }
    }

    /// `caps` is what the UI trusts instead of re-deriving the state machine, and
    /// `syncPreset` is the batch form of reset-preset. Both are pure state.
    @Test
    void capsAndBatchPresetSync(@TempDir Path dir) throws Exception {
        Path media = Files.createDirectory(dir.resolve("media"));
        Files.writeString(media.resolve("a.mp4"), "source");
        Files.writeString(media.resolve("b.mp4"), "source");

        ServerConfig config = new ServerConfig();
        config.dataDir = dir.resolve("data");
        config.transcode.mediaRoot = media.toString();
        TranscodeModule module = new TranscodeModule(config, new EventBus());
        module.feature().enable(consent());
        module.queue().start();
        try {
            var preset = module.presets().get("h264-quality").orElseThrow().preset();
            var queued = module.queue().submit("a.mp4", "", "h264-quality", preset);
            var stopped = module.queue().submit("b.mp4", "", "h264-quality", preset);
            String queuedId = queued.get("id").getAsString();
            String stoppedId = stopped.get("id").getAsString();
            assertTrue(module.queue().cancel(stoppedId));

            var queuedCaps = module.queue().jobJson(queuedId).orElseThrow().getAsJsonObject("caps");
            assertTrue(queuedCaps.get("start").getAsBoolean(), "a queued job can be started");
            assertTrue(queuedCaps.get("stop").getAsBoolean(), "a queued job can be cancelled");
            assertTrue(queuedCaps.get("remove").getAsBoolean());
            assertTrue(queuedCaps.get("reset").getAsBoolean());
            assertTrue(queuedCaps.get("reorder").getAsBoolean());
            assertTrue(!queuedCaps.get("pause").getAsBoolean(), "nothing to pause before it runs");
            assertTrue(!queuedCaps.get("resume").getAsBoolean());

            var stoppedCaps = module.queue().jobJson(stoppedId).orElseThrow().getAsJsonObject("caps");
            assertTrue(!stoppedCaps.get("start").getAsBoolean(), "only queued jobs can start");
            assertTrue(!stoppedCaps.get("stop").getAsBoolean(), "a cancelled job is already stopped");
            assertTrue(stoppedCaps.get("remove").getAsBoolean());
            assertTrue(stoppedCaps.get("reset").getAsBoolean());

            // Batch preset sync touches the queued job only and reports what it skipped.
            var replacement = preset.copy();
            replacement.name = "batch h265";
            var counts = module.queue().syncPreset("h265-quality", replacement, java.util.List.of());
            assertEquals(1, counts.get("updated").getAsInt());
            assertEquals(1, counts.get("skippedTerminal").getAsInt());
            assertEquals(0, counts.get("skippedTool").getAsInt());
            var after = module.queue().jobJson(queuedId).orElseThrow();
            assertEquals("h265-quality", after.get("presetId").getAsString());
            assertEquals("queued", after.get("state").getAsString());

            // A tool job has no preset to swap and is counted separately; the preset job
            // is still queued (the queue never started), so it is updated again.
            var tool = module.queue().submitCommand("提取", "extract.mkv",
                    java.util.List.of("ffmpeg", "-i", "a.mp4", "extract.mkv"), "extract");
            var withTool = module.queue().syncPreset("h264-quality", preset, java.util.List.of());
            assertEquals(1, withTool.get("updated").getAsInt());
            assertEquals(1, withTool.get("skippedTool").getAsInt());
            assertTrue(module.queue().jobJson(tool.get("id").getAsString()).isPresent());
        } finally {
            module.queue().stop();
        }
    }

    /// Reordering must refuse a partial list, duplicates and unknown ids instead of
    /// applying half of it, and must keep the accepted order in the snapshot.
    @Test
    void reorderRequiresTheWholeOrder(@TempDir Path dir) throws Exception {
        Path media = Files.createDirectory(dir.resolve("media"));
        Files.writeString(media.resolve("a.mp4"), "source");
        Files.writeString(media.resolve("b.mp4"), "source");
        Files.writeString(media.resolve("c.mp4"), "source");

        ServerConfig config = new ServerConfig();
        config.dataDir = dir.resolve("data");
        config.transcode.mediaRoot = media.toString();
        TranscodeModule module = new TranscodeModule(config, new EventBus());
        module.feature().enable(consent());
        module.queue().start();
        try {
            var preset = module.presets().get("h264-quality").orElseThrow().preset();
            String a = module.queue().submit("a.mp4", "", "h264-quality", preset).get("id").getAsString();
            String b = module.queue().submit("b.mp4", "", "h264-quality", preset).get("id").getAsString();
            String c = module.queue().submit("c.mp4", "", "h264-quality", preset).get("id").getAsString();

            assertTrue(!module.queue().reorder(java.util.List.of(a)), "a partial order is refused");
            assertTrue(!module.queue().reorder(java.util.List.of(a, a, b, c)), "duplicates are refused");
            assertTrue(!module.queue().reorder(java.util.List.of(a, b, "no-such-job")),
                    "an unknown id is refused");

            assertTrue(module.queue().reorder(java.util.List.of(c, b, a)));
            var jobs = module.queue().snapshot().getAsJsonArray("jobs");
            assertEquals(c, jobs.get(0).getAsJsonObject().get("id").getAsString());
            assertEquals(a, jobs.get(2).getAsJsonObject().get("id").getAsString());
        } finally {
            module.queue().stop();
        }
    }

    /// A job parked for a decision pauses the queue so nothing starts behind it. Removing
    /// that job must lift the pause: the operator did not press 暂停, and the queued work
    /// would otherwise sit there until someone noticed.
    @Test
    void removingAParkedJobResumesTheQueue(@TempDir Path dir) throws Exception {
        Assumptions.assumeFalse(OperatingSystem.CURRENT_OS == OperatingSystem.WINDOWS,
                "the fake ffmpeg uses a POSIX shell script");

        Path media = Files.createDirectory(dir.resolve("media"));
        Files.writeString(media.resolve("input.mp4"), "source");
        Path bin = Files.createDirectory(dir.resolve("bin"));
        Path ffmpeg = bin.resolve("ffmpeg");
        Path ffprobe = bin.resolve("ffprobe");
        script(ffmpeg, """
                #!/bin/sh
                case "$*" in
                  *-version*) echo "ffmpeg version 7.1.5"; exit 0 ;;
                  *-encoders*) echo " V....D libx264 H.264"; exit 0 ;;
                  *-hwaccels*) echo "Hardware acceleration methods:"; echo "vaapi"; exit 0 ;;
                  *-filters*) echo " TSC scale V->V Scale"; exit 0 ;;
                  *hevc_vaapi*) echo "Invalid FrameType:0"; exit 1 ;;
                esac
                for last; do :; done
                echo "out_time_us=1000000"
                echo "speed=2.0x"
                echo "progress=end"
                printf x > "$last"
                exit 0
                """);
        script(ffprobe, """
                #!/bin/sh
                echo '{"streams":[{"codec_type":"video"},{"codec_type":"audio"}],"format":{"duration":"2.0"}}'
                """);

        ServerConfig config = new ServerConfig();
        config.dataDir = dir.resolve("data");
        config.transcode.ffmpegPath = ffmpeg.toString();
        config.transcode.mediaRoot = media.toString();
        config.transcode.failurePolicy = "ask";

        TranscodeModule module = new TranscodeModule(config, new EventBus());
        module.feature().enable(consent());
        module.queue().start();
        try {
            var hardware = module.presets().get("hevc-vaapi").orElseThrow().preset();
            var software = module.presets().get("h264-quality").orElseThrow().preset();
            String parked = module.queue().submit("input.mp4", "", "hevc-vaapi", hardware)
                    .get("id").getAsString();
            String behind = module.queue().submit("input.mp4", "", "h264-quality", software)
                    .get("id").getAsString();
            module.queue().startQueue();

            String state = "";
            for (int i = 0; i < 100; i++) {
                Thread.sleep(50);
                state = module.queue().jobJson(parked).orElseThrow().get("state").getAsString();
                if ("waiting_decision".equals(state) || "failed".equals(state)) {
                    break;
                }
            }
            assertEquals("waiting_decision", state, module.queue().snapshot().toString());
            assertEquals("paused", module.queue().snapshot().get("runState").getAsString());

            assertTrue(module.queue().remove(parked), module.queue().snapshot().toString());
            // The park is gone, so the pause it caused must be gone with it.
            assertEquals("running", module.queue().snapshot().get("runState").getAsString());

            for (int i = 0; i < 100; i++) {
                Thread.sleep(50);
                state = module.queue().jobJson(behind).orElseThrow().get("state").getAsString();
                if ("done".equals(state) || "failed".equals(state)) {
                    break;
                }
            }
            assertEquals("done", state, module.queue().snapshot().toString());
        } finally {
            module.queue().stop();
        }
    }

}
