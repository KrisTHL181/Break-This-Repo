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
package org.jackhuang.hmcl.web.transcode.preset;

import org.jetbrains.annotations.NotNullByDefault;

import java.util.LinkedHashMap;
import java.util.Map;

/// The presets the panel ships with. They are read-only; "另存为" creates a user copy.
///
/// Five broadly useful starting points, not a catalogue of every encoder: the point
/// of the feature is that a NAS user can drop a file and pick one of these, while a
/// power user opens the full parameter panel.
@NotNullByDefault
public final class BuiltInPresets {

    private static final Map<String, TranscodePreset> PRESETS = build();

    private BuiltInPresets() {
    }

    public static Map<String, TranscodePreset> all() {
        return PRESETS;
    }

    private static Map<String, TranscodePreset> build() {
        Map<String, TranscodePreset> result = new LinkedHashMap<>();
        result.put("h264-quality", preset("H.264 高质量", "x264 CRF 20，音频/字幕原样保留",
                p -> {
                    p.container = "mkv";
                    p.videoEncoder = "libx264";
                    p.qualityMode = "crf";
                    p.qualityValue = "20";
                    p.preset = "medium";
                    p.audioMode = "copy";
                    p.subtitleMode = "copy";
                }));
        result.put("h265-quality", preset("H.265 高质量", "x265 CRF 23，音频/字幕原样保留",
                p -> {
                    p.container = "mkv";
                    p.videoEncoder = "libx265";
                    p.qualityMode = "crf";
                    p.qualityValue = "23";
                    p.preset = "medium";
                    p.audioMode = "copy";
                    p.subtitleMode = "copy";
                }));
        result.put("mp4-h264", preset("MP4 · H.264", "兼容性优先：H.264 + AAC，MP4",
                p -> {
                    p.container = "mp4";
                    p.videoEncoder = "libx264";
                    p.qualityMode = "crf";
                    p.qualityValue = "20";
                    p.preset = "medium";
                    p.pixelFormat = "yuv420p";
                    p.audioMode = "encode";
                    p.audioEncoder = "aac";
                    p.audioBitrate = "192k";
                    p.subtitleMode = "none";
                }));
        result.put("remux-copy", preset("仅重封装", "视频/音频/字幕全部 copy，只换容器",
                p -> {
                    p.container = "mkv";
                    p.videoEncoder = "copy";
                    p.audioMode = "copy";
                    p.subtitleMode = "copy";
                }));
        result.put("hevc-vaapi", preset("H.265 VAAPI", "Intel/AMD 硬件编码，QP 24（需 /dev/dri）",
                p -> {
                    p.container = "mkv";
                    p.videoEncoder = "hevc_vaapi";
                    p.qualityMode = "cqp";
                    p.qualityValue = "24";
                    p.audioMode = "copy";
                    p.subtitleMode = "copy";
                }));
        result.put("audio-only", preset("仅音频", "丢掉视频，导出 AAC 音频",
                p -> {
                    p.container = "m4a";
                    p.videoEncoder = "none";
                    p.audioMode = "encode";
                    p.audioEncoder = "aac";
                    p.audioBitrate = "192k";
                    p.subtitleMode = "none";
                }));
        return java.util.Collections.unmodifiableMap(result);
    }

    private static TranscodePreset preset(String name, String description,
                                          java.util.function.Consumer<TranscodePreset> customizer) {
        TranscodePreset preset = new TranscodePreset();
        preset.name = name;
        preset.description = description;
        customizer.accept(preset);
        return preset;
    }
}