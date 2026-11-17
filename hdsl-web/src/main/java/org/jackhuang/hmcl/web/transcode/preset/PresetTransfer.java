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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.NotNullByDefault;

/// A best-effort bridge to FFmpegFreeUI's `.3fui` JSON.
///
/// The two models are not the same: 3FUI's file is a serialized WinForms preset with
/// hundreds of Windows-only fields. This class maps the common fields (container,
/// encoder/quality/audio, resolution/fps, metadata, chapters/attachments) in both
/// directions; everything else keeps the panel's default. The point is "a preset made
/// on Windows opens here and back", not byte-identical round-tripping.
@NotNullByDefault
public final class PresetTransfer {

    private PresetTransfer() {
    }

    /// `true` when the object looks like a 3FUI preset rather than our own JSON.
    public static boolean looksLike3fui(JsonObject object) {
        return object.has("预设文件版本") || object.has("输出容器")
                || object.has("视频参数_编码器_具体编码");
    }

    public static TranscodePreset from3fui(JsonObject source) {
        TranscodePreset preset = new TranscodePreset();
        String note = text(source, "预设备注");
        preset.name = note.isBlank() ? "从 .3fui 导入" : note;
        preset.description = note;
        preset.container = nonBlank(text(source, "输出容器"), preset.container);
        preset.outputDir = text(source, "输出目录");
        String naming = text(source, "输出_自动命名选项");
        if (naming.contains("时间戳")) {
            preset.outputNaming = "timestamp";
        } else if (naming.contains("不使用")) {
            preset.outputNaming = "none";
        }
        preset.videoEncoder = nonBlank(text(source, "视频参数_编码器_具体编码"), preset.videoEncoder);
        preset.preset = nonBlank(text(source, "视频参数_编码器_编码预设"), preset.preset);
        preset.profile = text(source, "视频参数_编码器_配置文件");
        preset.tune = text(source, "视频参数_编码器_场景优化");
        preset.pixelFormat = text(source, "视频参数_色彩管理_像素格式");
        preset.threads = text(source, "视频参数_编码器_threads");
        preset.width = text(source, "视频参数_分辨率自动计算_宽度");
        preset.height = text(source, "视频参数_分辨率自动计算_高度");
        preset.fps = text(source, "视频参数_帧速率");
        preset.crop = text(source, "视频参数_分辨率_裁剪滤镜参数");

        String control = text(source, "视频参数_比特率_控制方式").trim();
        String qualityValue = text(source, "视频参数_质量控制_值").trim();
        String bitrate = text(source, "视频参数_比特率_基础").trim();
        if (!qualityValue.isBlank()) {
            preset.qualityValue = qualityValue;
        } else if (!bitrate.isBlank()) {
            preset.qualityValue = bitrate;
        }
        switch (control.toUpperCase(java.util.Locale.ROOT)) {
            case "CRF" -> preset.qualityMode = "crf";
            case "CQP" -> preset.qualityMode = "cqp";
            case "VBR", "VBR HQ" -> preset.qualityMode = "vbr";
            case "CBR" -> preset.qualityMode = "cbr";
            default -> {
                // 3FUI 没写控制方式时，按编码器是否硬件选择合法的默认值。
                preset.qualityMode = hardwareEncoder(preset.videoEncoder) ? "cqp" : "crf";
            }
        }
        preset.bitrateMin = text(source, "视频参数_比特率_最低值");
        preset.bitrateMax = text(source, "视频参数_比特率_最高值");
        preset.bitrateBuf = text(source, "视频参数_比特率_缓冲区");

        preset.audioEncoder = nonBlank(text(source, "音频参数_编码器_代号"), preset.audioEncoder);
        preset.audioBitrate = nonBlank(text(source, "音频参数_比特率"), preset.audioBitrate);
        preset.audioChannels = text(source, "音频参数_声道数");
        preset.audioSampleRate = text(source, "音频参数_采样率");
        if (!preset.audioEncoder.isBlank() && !"copy".equalsIgnoreCase(preset.audioEncoder)) {
            preset.audioMode = "encode";
        }

        String chapters = text(source, "流控制_章节选项");
        if (chapters.contains("清除")) {
            preset.copyChapters = false;
        }
        String attachments = text(source, "流控制_附件选项");
        if (attachments.contains("清除")) {
            preset.copyAttachments = false;
        }
        preset.chaptersFile = text(source, "章节_文件路径");

        if (source.has("元数据_要写入的信息") && source.get("元数据_要写入的信息").isJsonArray()) {
            JsonArray items = source.getAsJsonArray("元数据_要写入的信息");
            for (JsonElement element : items) {
                if (!element.isJsonObject()) {
                    continue;
                }
                JsonObject item = element.getAsJsonObject();
                String key = text(item, "字段");
                String value = text(item, "值");
                switch (key.toLowerCase(java.util.Locale.ROOT)) {
                    case "title" -> preset.metadataTitle = value;
                    case "artist" -> preset.metadataArtist = value;
                    case "album" -> preset.metadataAlbum = value;
                    case "comment" -> preset.metadataComment = value;
                    case "date" -> preset.metadataDate = value;
                    case "genre" -> preset.metadataGenre = value;
                    default -> {
                    }
                }
            }
        }
        preset.validate();
        return preset;
    }

    /// A conservative 3FUI-shaped export: only fields the panel actually understands.
    public static JsonObject to3fui(TranscodePreset preset) {
        JsonObject object = new JsonObject();
        object.addProperty("预设文件版本", 6);
        object.addProperty("预设备注", preset.name == null ? "" : preset.name);
        object.addProperty("输出容器", preset.container);
        object.addProperty("输出目录", preset.outputDir);
        object.addProperty("输出_自动命名选项",
                "timestamp".equals(preset.outputNaming) ? "附加_递增时间戳" : "附加_递增数字");
        object.addProperty("视频参数_编码器_具体编码", preset.videoEncoder);
        object.addProperty("视频参数_编码器_编码预设", preset.preset);
        object.addProperty("视频参数_编码器_配置文件", preset.profile);
        object.addProperty("视频参数_编码器_场景优化", preset.tune);
        object.addProperty("视频参数_色彩管理_像素格式", preset.pixelFormat);
        object.addProperty("视频参数_比特率_控制方式", switch (preset.qualityMode) {
            case "cqp" -> "CQP";
            case "vbr" -> "VBR";
            case "cbr" -> "CBR";
            case "bitrate" -> "CBR";
            case "none" -> "";
            default -> "CRF";
        });
        object.addProperty("视频参数_质量控制_值", preset.qualityValue);
        object.addProperty("视频参数_比特率_基础",
                java.util.List.of("bitrate", "vbr", "cbr").contains(preset.qualityMode) ? preset.qualityValue : "");
        object.addProperty("视频参数_比特率_最低值", preset.bitrateMin);
        object.addProperty("视频参数_比特率_最高值", preset.bitrateMax);
        object.addProperty("视频参数_比特率_缓冲区", preset.bitrateBuf);
        object.addProperty("视频参数_编码器_threads", preset.threads);
        object.addProperty("视频参数_分辨率自动计算_宽度", preset.width);
        object.addProperty("视频参数_分辨率自动计算_高度", preset.height);
        object.addProperty("视频参数_帧速率", preset.fps);
        object.addProperty("视频参数_分辨率_裁剪滤镜参数", preset.crop);
        object.addProperty("音频参数_编码器_代号", preset.audioEncoder);
        object.addProperty("音频参数_比特率", preset.audioBitrate);
        object.addProperty("音频参数_声道数", preset.audioChannels);
        object.addProperty("音频参数_采样率", preset.audioSampleRate);
        object.addProperty("流控制_章节选项", preset.copyChapters ? "保留章节" : "清除章节");
        object.addProperty("流控制_附件选项", preset.copyAttachments ? "保留附件" : "清除附件");
        object.addProperty("章节_文件路径", preset.chaptersFile);

        JsonArray metadata = new JsonArray();
        addMetadata(metadata, "title", preset.metadataTitle);
        addMetadata(metadata, "artist", preset.metadataArtist);
        addMetadata(metadata, "album", preset.metadataAlbum);
        addMetadata(metadata, "comment", preset.metadataComment);
        addMetadata(metadata, "date", preset.metadataDate);
        addMetadata(metadata, "genre", preset.metadataGenre);
        object.add("元数据_要写入的信息", metadata);
        return object;
    }

    private static void addMetadata(JsonArray array, String key, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        JsonObject item = new JsonObject();
        item.addProperty("字段", key);
        item.addProperty("值", value);
        array.add(item);
    }

    private static boolean hardwareEncoder(String encoder) {
        return encoder != null && (encoder.endsWith("_nvenc") || encoder.endsWith("_qsv")
                || encoder.endsWith("_vaapi") || encoder.endsWith("_amf")
                || encoder.endsWith("_d3d12va") || encoder.endsWith("_vulkan"));
    }

    private static String text(JsonObject object, String key) {
        if (!object.has(key) || !object.get(key).isJsonPrimitive()) {
            return "";
        }
        try {
            return object.get(key).getAsString();
        } catch (RuntimeException e) {
            return "";
        }
    }

    private static String nonBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}