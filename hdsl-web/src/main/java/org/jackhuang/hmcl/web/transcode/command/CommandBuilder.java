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
package org.jackhuang.hmcl.web.transcode.command;

import org.jackhuang.hmcl.web.transcode.preset.TranscodePreset;
import org.jetbrains.annotations.NotNullByDefault;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/// Turns a [TranscodePreset] into an ffmpeg argument vector.
///
/// The output is **arguments**, never a shell string: a path with a space or a quote is
/// one argv element, and no preset field can inject a second command. The caller
/// prepends the resolved ffmpeg executable and decides where the output lives; this
/// class only decides what ffmpeg is asked to do.
@NotNullByDefault
public final class CommandBuilder {

    private CommandBuilder() {
    }

    /// Builds the arguments for one job. `input` and `output` are absolute paths.
    public static List<String> build(TranscodePreset preset, Path input, Path output) {
        return build(preset, input, output, null, null);
    }

    /// The same, with an optional ffmetadata chapters file (already resolved under the
    /// media root by the caller).
    public static List<String> build(TranscodePreset preset, Path input, Path output, Path chapters) {
        return build(preset, input, output, chapters, null);
    }

    /// The same, with an optional subtitle file resolved under the media root. The panel
    /// otherwise has no cwd that would make a relative subtitle path work.
    public static List<String> build(TranscodePreset preset, Path input, Path output, Path chapters, Path subtitle) {
        return build(preset, input, output, chapters, subtitle, 0, null);
    }

    /// `pass` is 1 or 2 for two-pass encoding, 0 for a single-pass job.
    private static List<String> build(TranscodePreset preset, Path input, Path output, Path chapters,
                                      Path subtitle, int pass, Path passLog) {
        preset.validate();
        List<String> args = new ArrayList<>();
        args.add("-hide_banner");
        args.add("-nostdin");
        args.add("-y");
        args.add("-nostats");
        args.add("-progress");
        args.add("pipe:1");

        boolean vaapi = preset.videoEncoder != null && preset.videoEncoder.endsWith("_vaapi");
        if (vaapi) {
            args.add("-vaapi_device");
            args.add("/dev/dri/renderD128");
        }
        // 输入侧定位：重新编码时精确到帧，copy 时落在关键帧。
        if (!blank(preset.trimStart)) {
            args.add("-ss");
            args.add(preset.trimStart);
        }
        if (!blank(preset.trimEnd)) {
            args.add("-to");
            args.add(preset.trimEnd);
        }
        args.addAll(preset.inputArgs);
        args.add("-i");
        args.add(input.toString());
        if (chapters != null) {
            args.add("-i");
            args.add(chapters.toString());
        }

        String video = preset.videoEncoder == null ? "" : preset.videoEncoder;
        boolean copyVideo = "copy".equals(video);
        boolean noVideo = "none".equals(video);
        boolean image = isImageEncoder(video);

        if (noVideo) {
            args.add("-vn");
        } else {
            args.add("-map");
            String videoMap = preset.videoKeepOthers ? "0:v?"
                    : preset.videoMap == null || preset.videoMap.isBlank() ? "0:v:0?" : preset.videoMap;
            args.add(videoMap);
        }
        if (!image && !"none".equals(preset.audioMode)) {
            args.add("-map");
            String audioMap = preset.audioKeepOthers ? "0:a?"
                    : preset.audioMap == null || preset.audioMap.isBlank() ? "0:a?" : preset.audioMap;
            args.add(audioMap);
        }
        if (!image && ("copy".equals(preset.subtitleMode) || "convert".equals(preset.subtitleMode))) {
            args.add("-map");
            String subtitleMap = preset.subtitleKeepOthers ? "0:s?"
                    : preset.subtitleMap == null || preset.subtitleMap.isBlank() ? "0:s?" : preset.subtitleMap;
            args.add(subtitleMap);
        }
        boolean keepAttachments = !image && preset.copyAttachments
                && supportsAttachments(preset.container) && pass != 1;
        if (keepAttachments) {
            args.add("-map");
            args.add("0:t?");
        }

        List<String> filters = filters(preset, vaapi, subtitle);
        if ("burn".equals(preset.subtitleMode) && copyVideo) {
            throw new IllegalArgumentException("burning subtitles requires video re-encoding; choose an encoder instead of copy");
        }
        if (!noVideo) {
            if (copyVideo) {
                args.add("-c:v");
                args.add("copy");
            } else {
                if (preset.videoKeepOthers) {
                    args.add("-c:v");
                    args.add("copy");
                    args.add("-c:v:" + preset.videoStreamIndex);
                    args.add(video);
                } else {
                    args.add("-c:v");
                    args.add(video);
                }
                addQuality(args, preset, video);
                if (video.endsWith("_qsv") && !preset.videoArgs.contains("-low_power")) {
                    args.add("-low_power");
                    args.add("1");
                }
                if (!blank(preset.preset)) {
                    if ("libvpx-vp9".equals(video) || "libaom-av1".equals(video)) {
                        // VP9/AV1 软件编码的速度档是 -cpu-used（0 最慢 ~ 8 最快）。
                        args.add("-cpu-used");
                        args.add(preset.preset);
                    } else if ("librav1e".equals(video)) {
                        // rav1e 的速度档是 `-speed`（0 最慢 ~ 10 最快），没有 -preset。
                        args.add("-speed");
                        args.add(preset.preset);
                    } else if (video.endsWith("_amf") || video.endsWith("_vulkan")) {
                        // AMF 与 Vulkan 也用 `-quality` 做速度档，取值分别见各自的 schema。
                        args.add("-quality");
                        args.add(preset.preset);
                    } else if (video.endsWith("_vaapi")) {
                        // VAAPI 的 H.264/H.265 有 `-quality`；VP9/AV1 VAAPI 没有这个旋钮。
                        if ("h264_vaapi".equals(video) || "hevc_vaapi".equals(video)) {
                            args.add("-quality");
                            args.add(preset.preset);
                        }
                    } else if (softwareEncoder(video) || video.endsWith("_nvenc") || video.endsWith("_qsv")) {
                        args.add("-preset");
                        args.add(preset.preset);
                    }
                }
                if (!blank(preset.profile)) {
                    args.add("-profile:v");
                    args.add(preset.profile);
                }
                if (!blank(preset.level)) {
                    args.add("-level");
                    args.add(preset.level);
                }
                if (!blank(preset.tune) && !video.endsWith("_d3d12va")) {
                    // AMF 没有 -tune，场景优化是 -usage；D3D12VA 两者都没有。
                    args.add(video.endsWith("_amf") ? "-usage" : "-tune");
                    args.add(preset.tune);
                }
                if (!blank(preset.pixelFormat)) {
                    args.add("-pix_fmt");
                    args.add(preset.pixelFormat);
                } else if (preset.tenBit) {
                    args.add("-pix_fmt");
                    args.add("yuv420p10le");
                }
                if (!blank(preset.gop)) {
                    args.add("-g");
                    args.add(preset.gop);
                }
                if (!blank(preset.threads)) {
                    args.add("-threads");
                    args.add(preset.threads);
                }
            }
            if (!copyVideo && !filters.isEmpty()) {
                if (preset.videoKeepOthers) {
                    args.add("-filter:v:" + preset.videoStreamIndex);
                } else {
                    args.add("-vf");
                }
                args.add(String.join(",", filters));
            }
            if (!copyVideo) {
                if (!blank(preset.colorPrimaries)) {
                    args.add("-color_primaries");
                    args.add(preset.colorPrimaries);
                }
                if (!blank(preset.colorTransfer)) {
                    args.add("-color_trc");
                    args.add(preset.colorTransfer);
                }
                if (!blank(preset.colorMatrix)) {
                    args.add("-colorspace");
                    args.add(preset.colorMatrix);
                }
                if (!blank(preset.colorRange)) {
                    args.add("-color_range");
                    args.add(preset.colorRange);
                }
            }
            args.addAll(preset.videoArgs);
        }

        if (image) {
            args.add("-an");
            args.add("-sn");
        } else switch (preset.audioMode) {
            case "none" -> args.add("-an");
            case "copy" -> {
                args.add("-c:a");
                args.add("copy");
            }
            default -> {
                if (preset.audioKeepOthers) {
                    args.add("-c:a");
                    args.add("copy");
                    args.add("-c:a:" + preset.audioStreamIndex);
                    args.add(preset.audioEncoder);
                } else {
                    args.add("-c:a");
                    args.add(preset.audioEncoder);
                }
                if (!blank(preset.audioBitrate)) {
                    args.add("-b:a");
                    args.add(preset.audioBitrate);
                }
                if (!blank(preset.audioChannels)) {
                    args.add("-ac");
                    args.add(preset.audioChannels);
                }
                if (!blank(preset.audioSampleRate)) {
                    args.add("-ar");
                    args.add(preset.audioSampleRate);
                }
                args.addAll(preset.audioArgs);
            }
        }

        if (!image && "encode".equals(preset.audioMode)) {
            List<String> audioFilters = audioFilters(preset);
            if (!audioFilters.isEmpty()) {
                if (preset.audioKeepOthers) {
                    args.add("-filter:a:" + preset.audioStreamIndex);
                } else {
                    args.add("-af");
                }
                args.add(String.join(",", audioFilters));
            }
        }

        if (!image) {
            switch (preset.subtitleMode) {
                case "none" -> args.add("-sn");
                case "copy" -> {
                    args.add("-c:s");
                    args.add("copy");
                }
                case "convert" -> {
                    if (preset.subtitleKeepOthers) {
                        args.add("-c:s");
                        args.add("copy");
                        args.add("-c:s:" + preset.subtitleStreamIndex);
                        args.add(preset.subtitleCodec);
                    } else {
                        args.add("-c:s");
                        args.add(preset.subtitleCodec);
                    }
                }
                default -> {
                    // `burn` is handled in filters(); no subtitle stream is copied.
                }
            }
            args.addAll(preset.subtitleArgs);
            if (keepAttachments) {
                args.add("-c:t");
                args.add("copy");
            }
        }

        if (pass == 1) {
            // 第一遍只分析：音频/字幕/数据流不写入，也不带 -movflags 等输出格式参数。
            args.add("-an");
            args.add("-sn");
            args.add("-dn");
            args.add("-pass");
            args.add("1");
            args.add("-passlogfile");
            args.add(passLog.toString());
            args.add("-f");
            args.add("null");
            args.add("-");
            return List.copyOf(args);
        }
        if (pass == 2) {
            args.add("-pass");
            args.add("2");
            args.add("-passlogfile");
            args.add(passLog.toString());
        }
        if (preset.copyMetadata) {
            args.add("-map_metadata");
            args.add("0");
        } else {
            args.add("-map_metadata");
            args.add("-1");
        }
        metadata(args, preset);
        args.add("-map_chapters");
        if (image || !supportsChapters(preset.container)) {
            args.add("-1");
        } else if (chapters != null) {
            args.add("1");
        } else {
            args.add(preset.copyChapters ? "0" : "-1");
        }
        if (image) {
            args.add("-frames:v");
            args.add("1");
        }
        if ("mp4".equalsIgnoreCase(preset.container)) {
            args.add("-movflags");
            args.add("+faststart");
        }
        args.addAll(preset.outputArgs);
        args.add(output.toString());
        return List.copyOf(args);
    }

    /// A shell-quoted display form for the preview card. It is **not** executed.
    public static List<List<String>> buildPasses(TranscodePreset preset, Path input, Path output) {
        return buildPasses(preset, input, output, null, null);
    }

    /// 两遍编码：第一遍只分析（`-f null`），第二遍才写文件。返回 1 或 2 条命令。
    /// 仅支持 x264/x265 + 固定码率；其它组合在 `TranscodePreset.validate()` 里就拒绝，
    /// 因此提交任务时不会接受一个跑到一半才失败的预设。
    public static List<List<String>> buildPasses(TranscodePreset preset, Path input, Path output,
                                                 Path chapters, Path subtitle) {
        if (!preset.twoPass) {
            return List.of(build(preset, input, output, chapters, subtitle, 0, null));
        }
        Path passLog = output.resolveSibling(output.getFileName() + ".pass");
        return List.of(
                build(preset, input, output, chapters, subtitle, 1, passLog),
                build(preset, input, output, chapters, subtitle, 2, passLog));
    }

    public static String display(List<String> args) {
        StringBuilder result = new StringBuilder();
        for (String arg : args) {
            if (result.length() > 0) {
                result.append(' ');
            }
            if (arg.isEmpty() || arg.chars().anyMatch(Character::isWhitespace)) {
                result.append('"').append(arg.replace("\"", "\\\"")).append('"');
            } else {
                result.append(arg);
            }
        }
        return result.toString();
    }

    private static void metadata(List<String> args, TranscodePreset preset) {
        putMetadata(args, "title", preset.metadataTitle);
        putMetadata(args, "artist", preset.metadataArtist);
        putMetadata(args, "album", preset.metadataAlbum);
        putMetadata(args, "comment", preset.metadataComment);
        putMetadata(args, "date", preset.metadataDate);
        putMetadata(args, "genre", preset.metadataGenre);
        if (!blank(preset.metadataExtra)) {
            for (String line : preset.metadataExtra.split("\\R")) {
                int split = line.indexOf('=');
                if (split > 0) {
                    putMetadata(args, line.substring(0, split).trim(), line.substring(split + 1).trim());
                }
            }
        }
    }

    private static void putMetadata(List<String> args, String key, String value) {
        if (value != null && !value.isBlank()) {
            args.add("-metadata");
            args.add(key + "=" + value);
        }
    }

    private static void addQuality(List<String> args, TranscodePreset preset, String encoder) {
        String mode = preset.qualityMode == null ? "crf" : preset.qualityMode;
        if ("none".equals(mode)) {
            return;
        }
        if (List.of("bitrate", "vbr", "cbr").contains(mode)) {
            if (encoder.endsWith("_amf")) {
                args.add("-rc");
                args.add("cbr".equals(mode) || "bitrate".equals(mode) ? "cbr" : "vbr");
            } else if (encoder.endsWith("_d3d12va")) {
                args.add("-rc_mode");
                args.add("cbr".equals(mode) || "bitrate".equals(mode) ? "CBR" : "VBR");
            }
            args.add("-b:v");
            args.add(preset.qualityValue);
            if (!"bitrate".equals(mode)) {
                addIfPresent(args, "-minrate", preset.bitrateMin);
                addIfPresent(args, "-maxrate", preset.bitrateMax);
                addIfPresent(args, "-bufsize", preset.bitrateBuf);
            }
            return;
        }
        if ("cqp".equals(mode)) {
            if (encoder.endsWith("_amf")) {
                args.add("-rc");
                args.add("cqp");
            } else if (encoder.endsWith("_d3d12va")) {
                args.add("-rc_mode");
                args.add("CQP");
            }
            args.add(encoder.endsWith("_qsv") ? "-global_quality" : "-qp");
            args.add(preset.qualityValue);
            return;
        }
        // CRF / constant quality.
        if (encoder.endsWith("_qsv")) {
            args.add("-global_quality");
        } else if (encoder.endsWith("_vaapi")) {
            args.add("-qp");
        } else if (encoder.endsWith("_nvenc")) {
            args.add("-cq");
        } else if ("librav1e".equals(encoder)) {
            // rav1e 没有 -crf，恒定质量走 -qp。
            args.add("-qp");
        } else {
            args.add("-crf");
        }
        args.add(preset.qualityValue);
    }

    private static void addIfPresent(List<String> args, String option, String value) {
        if (value != null && !value.isBlank()) {
            args.add(option);
            args.add(value);
        }
    }

    private static boolean supportsAttachments(String container) {
        return container != null && !List.of("mp4", "mov", "avi", "ts", "m4a")
                .contains(container.toLowerCase(java.util.Locale.ROOT));
    }

    private static boolean supportsChapters(String container) {
        return container != null && List.of("mkv", "mp4", "mov", "m4a")
                .contains(container.toLowerCase(java.util.Locale.ROOT));
    }

    private static List<String> filters(TranscodePreset preset, boolean vaapi, Path subtitle) {
        List<String> filters = new ArrayList<>();
        // 裁剪与反交错都在缩放之前：裁剪坐标按源画面算，反交错在原始分辨率上效果最好。
        if (!blank(preset.crop)) {
            filters.add("crop=" + preset.crop);
        }
        if ("yadif".equals(preset.deinterlace)) {
            filters.add("yadif");
        }
        if (!blank(preset.width) || !blank(preset.height)) {
            filters.add("scale=" + (blank(preset.width) ? "-2" : preset.width)
                    + ":" + (blank(preset.height) ? "-2" : preset.height));
        }
        if (!blank(preset.fps)) {
            filters.add("fps=" + preset.fps);
        }
        switch (preset.rotate == null ? "" : preset.rotate) {
            case "90" -> filters.add("transpose=1");
            case "180" -> {
                filters.add("hflip");
                filters.add("vflip");
            }
            case "270" -> filters.add("transpose=2");
            default -> {
            }
        }
        // 画面增强：降噪 → 锐化 → 去色带 → 简易调色（与 FFmpegFreeUI 的参数顺序一致）。
        if (!blank(preset.denoise)) {
            filters.add(preset.denoise + (blank(preset.denoiseParams) ? "" : "=" + preset.denoiseParams));
        }
        if (!blank(preset.sharpen)) {
            filters.add(preset.sharpen + (blank(preset.sharpenParams) ? "" : "=" + preset.sharpenParams));
        }
        if (preset.deband) {
            filters.add("deband" + (blank(preset.debandParams) ? "" : "=" + preset.debandParams));
        }
        List<String> eq = new ArrayList<>();
        if (!blank(preset.eqBrightness)) eq.add("brightness=" + preset.eqBrightness);
        if (!blank(preset.eqContrast)) eq.add("contrast=" + preset.eqContrast);
        if (!blank(preset.eqSaturation)) eq.add("saturation=" + preset.eqSaturation);
        if (!blank(preset.eqGamma)) eq.add("gamma=" + preset.eqGamma);
        if (!eq.isEmpty()) {
            filters.add("eq=" + String.join(":", eq));
        }
        if (!blank(preset.filterGraph)) {
            filters.add(preset.filterGraph);
        }
        if ("burn".equals(preset.subtitleMode) && (subtitle != null || !blank(preset.subtitleFile))) {
            String path = subtitle != null ? subtitle.toString() : preset.subtitleFile;
            StringBuilder filter = new StringBuilder("subtitles=")
                    .append(path.replace("\\", "/").replace(":", "\\:"));
            List<String> style = new ArrayList<>();
            if (!blank(preset.subtitleFont)) {
                style.add("FontName=" + preset.subtitleFont);
            }
            if (!blank(preset.subtitleFontSize)) {
                style.add("FontSize=" + preset.subtitleFontSize);
            }
            if (!blank(preset.subtitleStyle)) {
                style.add(preset.subtitleStyle);
            }
            if (!style.isEmpty()) {
                filter.append(":force_style='").append(String.join(",", style)).append("'");
            }
            filters.add(filter.toString());
        }
        if (vaapi) {
            filters.add(preset.tenBit ? "format=p010le,hwupload" : "format=nv12,hwupload");
        }
        return filters;
    }

    /// 音频滤镜：音量 → 响度标准化（只在重新编码时调用）。
    private static List<String> audioFilters(TranscodePreset preset) {
        List<String> filters = new ArrayList<>();
        if (!blank(preset.audioVolume)) {
            filters.add("volume=" + preset.audioVolume);
        }
        if (preset.loudnormEnabled) {
            String i = blank(preset.loudnormI) ? "-16" : preset.loudnormI;
            String tp = blank(preset.loudnormTP) ? "-1.5" : preset.loudnormTP;
            String lra = blank(preset.loudnormLRA) ? "11" : preset.loudnormLRA;
            filters.add("loudnorm=I=" + i + ":TP=" + tp + ":LRA=" + lra);
        }
        return filters;
    }

    private static boolean isImageEncoder(String encoder) {
        if (encoder == null || encoder.isBlank()) {
            return false;
        }
        return switch (encoder) {
            case "mjpeg", "png", "webp", "libwebp", "bmp", "tiff", "gif", "avif",
                 "jxl", "libjxl", "jpegxl" -> true;
            default -> false;
        };
    }

    private static boolean softwareEncoder(String encoder) {
        return encoder.startsWith("lib") || encoder.startsWith("aom") || encoder.startsWith("svt")
                || encoder.equals("mpeg4") || encoder.equals("libvpx") || encoder.equals("libvpx-vp9");
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    /// Resolves the output path. The caller has already made `outputDir` absolute and
    /// inside the media root.
    public static Path outputPath(TranscodePreset preset, Path input, Path outputDir) {
        return outputPath(preset, input, outputDir, java.util.Set.of());
    }

    /// The same, also avoiding paths the queue has already promised to another job.
    /// `Files.exists()` alone is not enough: queued jobs have not written their output
    /// yet, so three submissions of the same input would pick the same name.
    public static Path outputPath(TranscodePreset preset, Path input, Path outputDir,
                                  java.util.Set<Path> reserved) {
        String container = preset.container == null || preset.container.isBlank()
                ? extension(input)
                : preset.container;
        String stem = stem(input);
        String suffix = switch (preset.outputNaming == null ? "suffix" : preset.outputNaming) {
            case "none" -> "";
            case "timestamp" -> "_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
            default -> preset.outputSuffix == null ? "_transcoded" : preset.outputSuffix;
        };
        Path directory = outputDir == null ? input.getParent() : outputDir;
        Path candidate = directory.resolve(stem + suffix + "." + container).toAbsolutePath().normalize();
        int counter = 1;
        while ((java.nio.file.Files.exists(candidate) || reserved.contains(candidate)) && counter < 10_000) {
            candidate = directory.resolve(stem + suffix + "_" + counter + "." + container)
                    .toAbsolutePath().normalize();
            counter++;
        }
        return candidate;
    }

    private static String stem(Path input) {
        String name = input.getFileName().toString();
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }

    private static String extension(Path input) {
        String name = input.getFileName().toString();
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(dot + 1) : "mkv";
    }
}