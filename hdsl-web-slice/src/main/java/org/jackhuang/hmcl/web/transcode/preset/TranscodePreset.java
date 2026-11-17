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

import java.util.ArrayList;
import java.util.List;

/// One transcoding preset, as stored in `<data>/transcode/presets/<id>.json`.
///
/// The field names are the public JSON contract; the frontend's generic form and the
/// command builder read the same object. New fields must keep a sane default so an old
/// preset file still loads (Gson leaves missing fields at the initializer).
///
/// This is deliberately a small, typed core rather than a copy of 3FUI's WinForms
/// fields: the panel owns its own schema, and the broad parameter panel is built on
/// top of [PresetSchema].
@NotNullByDefault
public final class TranscodePreset {

    public String name = "";
    public String description = "";

    // ---- output -----------------------------------------------------------
    public String container = "mkv";
    /// `suffix`, `timestamp` or `none`.
    public String outputNaming = "suffix";
    public String outputSuffix = "_transcoded";
    /// Empty means "beside the input"; otherwise a directory under the media root.
    public String outputDir = "";

    // ---- video ------------------------------------------------------------
    /// `copy`, `none` or an ffmpeg encoder name.
    public String videoEncoder = "libx264";
    /// `crf`, `cqp`, `bitrate` (legacy CBR), `vbr`, `cbr` or `none`.
    public String qualityMode = "crf";
    public String qualityValue = "20";
    /// VBR/CBR extras; empty means "not emitted".
    public String bitrateMin = "";
    public String bitrateMax = "";
    public String bitrateBuf = "";
    /// Two-pass encode (x264/x265 + bitrate mode): analyze, then encode.
    public boolean twoPass = false;
    public String preset = "medium";
    public String profile = "";
    public String level = "";
    public String tune = "";
    public String pixelFormat = "";
    public boolean tenBit = false;
    public String gop = "";
    public String threads = "";
    public String videoMap = "0:v:0?";
    /// Map every video stream and re-encode only [videoStreamIndex], copying the rest.
    public boolean videoKeepOthers = false;
    public int videoStreamIndex = 0;

    // ---- frame / colour ---------------------------------------------------
    public String width = "";
    public String height = "";
    public String fps = "";
    public String crop = "";
    /// `90`, `180`, `270`; empty means no rotation.
    public String rotate = "";
    /// `yadif` or empty.
    public String deinterlace = "";
    public String colorPrimaries = "";
    public String colorTransfer = "";
    public String colorMatrix = "";
    public String colorRange = "";

    // ---- audio ------------------------------------------------------------
    /// `copy`, `encode` or `none`.
    public String audioMode = "copy";
    public String audioEncoder = "aac";
    public String audioBitrate = "192k";
    public String audioChannels = "";
    public String audioSampleRate = "";
    public String audioMap = "0:a?";
    /// Map every audio stream and re-encode only [audioStreamIndex], copying the rest.
    public boolean audioKeepOthers = false;
    public int audioStreamIndex = 0;

    // ---- subtitle ---------------------------------------------------------
    /// `copy`, `burn`, `convert` or `none`.
    public String subtitleMode = "copy";
    /// Target codec for `convert`: `mov_text`, `ass`, `srt` or `webvtt`.
    public String subtitleCodec = "mov_text";
    public String subtitleFile = "";
    public String subtitleFont = "";
    public String subtitleFontSize = "";
    /// Extra `force_style` entries, e.g. `Bold=1,Outline=1`.
    public String subtitleStyle = "";
    public String subtitleMap = "0:s?";
    /// Map every subtitle stream and convert only [subtitleStreamIndex], copying the rest.
    public boolean subtitleKeepOthers = false;
    public int subtitleStreamIndex = 0;

    // ---- metadata / chapters / attachments -------------------------------
    public String metadataTitle = "";
    public String metadataArtist = "";
    public String metadataAlbum = "";
    public String metadataComment = "";
    public String metadataDate = "";
    public String metadataGenre = "";
    public boolean copyAttachments = true;
    /// Relative to the media root; empty means "use the input's chapters".
    public String chaptersFile = "";

    // ---- trim / video enhancement / audio filters -------------------------
    public String trimStart = "";
    public String trimEnd = "";
    public String denoise = "";
    public String denoiseParams = "";
    public String sharpen = "";
    public String sharpenParams = "";
    public boolean deband = false;
    public String debandParams = "";
    public String eqBrightness = "";
    public String eqContrast = "";
    public String eqSaturation = "";
    public String eqGamma = "";
    public String audioVolume = "";
    public boolean loudnormEnabled = false;
    public String loudnormI = "-16";
    public String loudnormTP = "-1.5";
    public String loudnormLRA = "11";
    public String metadataExtra = "";

    // ---- filters / custom args -------------------------------------------
    public String filterGraph = "";
    public boolean copyMetadata = true;
    public boolean copyChapters = true;
    public List<String> inputArgs = new ArrayList<>();
    public List<String> videoArgs = new ArrayList<>();
    public List<String> audioArgs = new ArrayList<>();
    public List<String> subtitleArgs = new ArrayList<>();
    public List<String> outputArgs = new ArrayList<>();

    /// A deep copy, so editing a preset never mutates the built-in instance.
    public TranscodePreset copy() {
        String json = org.jackhuang.hmcl.web.http.Json.GSON.toJson(this);
        return org.jackhuang.hmcl.web.http.Json.GSON.fromJson(json, TranscodePreset.class);
    }

    /// Rejects values the command builder cannot honour. The message is shown to the
    /// user, so it names the field rather than dumping the object.
    public void validate() {
        require(container, "container");
        require(videoEncoder, "videoEncoder");
        require(qualityMode, "qualityMode");
        if (!List.of("crf", "cqp", "bitrate", "vbr", "cbr", "none").contains(qualityMode)) {
            throw new IllegalArgumentException("qualityMode must be crf, cqp, bitrate, vbr, cbr or none");
        }
        if (List.of("crf", "cqp").contains(qualityMode)) {
            require(qualityValue, "qualityValue");
            if (!qualityValue.matches("\\d+(?:\\.\\d+)?")) {
                throw new IllegalArgumentException(qualityMode + " requires a numeric quality value");
            }
        } else if (List.of("bitrate", "vbr", "cbr").contains(qualityMode)) {
            require(qualityValue, "qualityValue");
            if (!qualityValue.matches("\\d+(?:\\.\\d+)?[kKmM]?")) {
                throw new IllegalArgumentException("bitrate must look like 2500k or 5M");
            }
        }
        for (String extra : List.of(bitrateMin, bitrateMax, bitrateBuf)) {
            if (!extra.isBlank() && !extra.matches("\\d+(?:\\.\\d+)?[kKmM]?")) {
                throw new IllegalArgumentException("bitrate min/max/buffer must look like 2500k or 5M");
            }
        }
        if (!List.of("copy", "encode", "none").contains(audioMode)) {
            throw new IllegalArgumentException("audioMode must be copy, encode or none");
        }
        if ("encode".equals(audioMode)) {
            require(audioEncoder, "audioEncoder");
        }
        if (!List.of("copy", "burn", "convert", "none").contains(subtitleMode)) {
            throw new IllegalArgumentException("subtitleMode must be copy, burn, convert or none");
        }
        if ("convert".equals(subtitleMode)
                && !List.of("mov_text", "ass", "srt", "webvtt").contains(subtitleCodec)) {
            throw new IllegalArgumentException("subtitleCodec must be mov_text, ass, srt or webvtt");
        }
        if ("burn".equals(subtitleMode) && "copy".equals(videoEncoder)) {
            throw new IllegalArgumentException(
                    "burning subtitles requires video re-encoding; choose an encoder instead of copy");
        }
        if (!List.of("suffix", "timestamp", "none").contains(outputNaming)) {
            throw new IllegalArgumentException("outputNaming must be suffix, timestamp or none");
        }
        if (videoStreamIndex < 0 || audioStreamIndex < 0 || subtitleStreamIndex < 0) {
            throw new IllegalArgumentException("stream index must be >= 0");
        }
        if (videoKeepOthers && List.of("copy", "none").contains(videoEncoder)) {
            throw new IllegalArgumentException("keeping other video streams requires a video encoder");
        }
        if (subtitleKeepOthers && "burn".equals(subtitleMode)) {
            throw new IllegalArgumentException("burning subtitles cannot keep other subtitle streams");
        }
        if (twoPass && (!("libx264".equals(videoEncoder) || "libx265".equals(videoEncoder))
                || !List.of("bitrate", "vbr", "cbr").contains(qualityMode))) {
            throw new IllegalArgumentException(
                    "two-pass encoding requires libx264/libx265 with bitrate, vbr or cbr mode");
        }
        validateEncoderCombination();
        if (tenBit && pixelFormat.isBlank()) {
            pixelFormat = "yuv420p10le";
        }
    }

    private void validateEncoderCombination() {
        String containerName = container.toLowerCase(java.util.Locale.ROOT);
        if ("m4a".equals(containerName) && !"none".equals(videoEncoder)) {
            throw new IllegalArgumentException("m4a is audio-only; set videoEncoder to none");
        }
        if ("webm".equals(containerName)
                && !List.of("copy", "none", "libvpx-vp9", "libaom-av1", "libsvtav1", "librav1e",
                        "vp9_qsv", "vp9_vaapi", "av1_nvenc", "av1_qsv", "av1_vaapi",
                        "av1_amf", "av1_d3d12va", "av1_vulkan").contains(videoEncoder)) {
            throw new IllegalArgumentException("webm only supports VP9/AV1 (or copy/none)");
        }
        if (List.of("mp4", "mov").contains(containerName) && "convert".equals(subtitleMode)
                && !"mov_text".equals(subtitleCodec)) {
            throw new IllegalArgumentException("mp4/mov subtitles must use mov_text");
        }
        if ("copy".equals(videoEncoder) || "none".equals(videoEncoder)) {
            return;
        }
        List<String> allowed = allowedQualityModes(videoEncoder);
        if (!allowed.contains(qualityMode)) {
            throw new IllegalArgumentException(videoEncoder + " does not support qualityMode " + qualityMode
                    + " (allowed: " + String.join(", ", allowed) + ")");
        }
    }

    private static List<String> allowedQualityModes(String encoder) {
        boolean hardware = encoder.endsWith("_nvenc") || encoder.endsWith("_qsv")
                || encoder.endsWith("_vaapi") || encoder.endsWith("_amf")
                || encoder.endsWith("_d3d12va") || encoder.endsWith("_vulkan");
        if (hardware) {
            return List.of("cqp", "bitrate", "vbr", "cbr", "none");
        }
        if ("libvpx-vp9".equals(encoder) || "libaom-av1".equals(encoder)
                || "libsvtav1".equals(encoder) || "librav1e".equals(encoder)) {
            return List.of("crf", "bitrate", "vbr", "cbr", "none");
        }
        return List.of("crf", "cqp", "bitrate", "vbr", "cbr", "none");
    }

    private static void require(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
    }
}