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

/// The field metadata the preset form renders.
///
/// This is the contract between the generic React form and the Java preset model. Adding
/// a field means adding it to [TranscodePreset], the [CommandBuilder] and one entry here;
/// the frontend does not need a bespoke component for it.
///
/// `hint` is the one-line explanation under a control (the web port of 3FUI's per-field
/// description). `important` marks the fields the overview and the beginner flow read.
/// `optionHints` explains individual select values without changing the wire format; the
/// frontend shows it after the Chinese option name and in the hover tooltip.
@NotNullByDefault
public final class PresetSchema {

    private PresetSchema() {
    }

    public static JsonObject json() {
        return json(null);
    }

    /// `availableEncoders` comes from `ffmpeg -encoders`; the schema marks the rest as
    /// unavailable so the form can gray them out instead of failing after submit.
    public static JsonObject json(java.util.Collection<String> availableEncoders) {
        JsonObject root = new JsonObject();
        root.addProperty("version", 2);
        JsonArray sections = new JsonArray();

        sections.add(section("输出", "决定输出文件的名字、位置与容器。MKV 能装下全部轨道；MP4 兼容性最好但字幕/附件受限。", new JsonArray[]{
                fields(
                        hint(select("container", "输出容器", "mkv", "mkv", "mp4", "mov", "webm", "avi", "ts", "m4a"),
                                "mkv 能装下全部音轨/字幕/附件；mp4 兼容性最好但字幕与附件受限",
                                "mkv", "能装下全部轨道（推荐）",
                                "mp4", "兼容性最好；字幕只能 mov_text，附件装不下",
                                "mov", "与 mp4 类似，剪辑生态更常见",
                                "webm", "只支持 VP9/AV1 + Opus/Vorbis；别装 AAC",
                                "avi", "老设备兼容；字幕/章节支持很差",
                                "ts", "电视/直播流；容错好但体积大",
                                "m4a", "纯音频容器"),
                        important(),
                        hint(select("outputNaming", "自动命名", "suffix", "suffix", "timestamp", "none"),
                                "suffix 追加后缀；timestamp 带日期时间；none 直接同名（会撞已有文件）",
                                "suffix", "追加固定后缀（推荐）",
                                "timestamp", "追加 yyyyMMdd-HHmmss",
                                "none", "不加后缀，可能覆盖同名输出"),
                        hint(text("outputSuffix", "文件名后缀", "_transcoded"),
                                "自动命名选 suffix 时使用"),
                        hint(text("outputDir", "输出目录（相对媒体根；空 = 源目录）", ""),
                                "留空输出到输入文件旁边；填相对路径则必须有写权限")
                )
        }));

        sections.add(section("剪辑", "剪掉片头片尾。输入侧定位：重新编码时精确到帧，copy 时落在最近的关键帧。", new JsonArray[]{
                fields(
                        hint(text("trimStart", "开始时间", ""), "留空从头开始；例 00:01:30、90 或 1:30"),
                        hint(text("trimEnd", "结束时间", ""), "留空到结尾；与开始时间格式相同，是绝对时间")
                )
        }));

        sections.add(section("编码器与质量", "先选编码器，再定质量。软件编码用 CRF（越小越清晰），硬件编码用 CQP；preset 只对软件编码有效。", new JsonArray[]{
                fields(
                        videoEncoderGroups(hint(select("videoEncoder", "视频编码器", "libx264",
                                "copy", "none",
                                "libx264", "h264_nvenc", "h264_qsv", "h264_vaapi",
                                "h264_amf", "h264_d3d12va", "h264_vulkan",
                                "libx265", "hevc_nvenc", "hevc_qsv", "hevc_vaapi",
                                "hevc_amf", "hevc_d3d12va", "hevc_vulkan",
                                "libvpx-vp9", "vp9_qsv", "vp9_vaapi",
                                "libaom-av1", "libsvtav1", "librav1e", "av1_nvenc", "av1_qsv", "av1_vaapi",
                                "av1_amf", "av1_d3d12va", "av1_vulkan"),
                                "copy 不重编码；x264 兼容性最好；x265 同画质更小；VAAPI/QSV 走核显",
                                "copy", "直接复制视频流，不重编码",
                                "none", "丢弃视频（纯音频输出）",
                                "libx264", "H.264 软件编码，兼容性最好",
                                "libx265", "H.265 软件编码，同画质约小 30–50%",
                                "libvpx-vp9", "VP9 软件编码，WebM 生态",
                                "libaom-av1", "AV1 软件编码（aom），压缩强、速度慢",
                                "libsvtav1", "AV1 软件编码（SVT-AV1），比 aom 快很多",
                                "librav1e", "AV1 软件编码（rav1e），速度档数值越大越快",
                                "h264_nvenc", "NVIDIA 硬件 H.264",
                                "hevc_nvenc", "NVIDIA 硬件 H.265",
                                "av1_nvenc", "NVIDIA 硬件 AV1（RTX 40 系起）",
                                "h264_vaapi", "Intel/AMD 核显 H.264",
                                "hevc_vaapi", "Intel/AMD 核显 H.265（推荐硬编）",
                                "vp9_vaapi", "Intel/AMD 核显 VP9",
                                "av1_vaapi", "Intel/AMD 核显 AV1（较新硬件）",
                                "h264_qsv", "Intel QSV H.264",
                                "hevc_qsv", "Intel QSV H.265（部分平台不可用）",
                                "vp9_qsv", "Intel QSV VP9",
                                "av1_qsv", "Intel QSV AV1（Arc 起）",
                                "h264_amf", "AMD AMF H.264（Windows）",
                                "hevc_amf", "AMD AMF H.265（Windows）",
                                "av1_amf", "AMD AMF AV1（Windows，RDNA3 起）",
                                "h264_d3d12va", "Windows D3D12VA H.264（需较新 ffmpeg）",
                                "hevc_d3d12va", "Windows D3D12VA H.265（需较新 ffmpeg）",
                                "av1_d3d12va", "Windows D3D12VA AV1（多数构建尚未支持）",
                                "h264_vulkan", "Vulkan H.264（实验性，需可用 Vulkan 设备）",
                                "hevc_vulkan", "Vulkan H.265（实验性，需可用 Vulkan 设备）",
                                "av1_vulkan", "Vulkan AV1（实验性，需可用 Vulkan 设备）")),
                        important(),
                        qualityModeGroups(hint(select("qualityMode", "质量模式", "crf",
                                "crf", "cqp", "vbr", "cbr", "bitrate", "none"),
                                "crf 恒定质量；cqp 固定 QP；vbr/cbr 固定目标码率；none 不加质量参数",
                                "crf", "恒定质量，越小越清晰（软件编码推荐）",
                                "cqp", "固定质量/QP，硬件编码常用",
                                "vbr", "可变码率：按目标码率浮动，画质优先",
                                "cbr", "固定码率：码率最稳，同体积画质更差",
                                "bitrate", "兼容旧预设：只写 -b:v",
                                "none", "不输出质量参数，交给编码器默认")),
                        hint(text("qualityValue", "质量值 / 码率", "20"),
                                "CRF/CQP 写数字（x264/x265 常用 18–28）；VBR/CBR 写 2500k 或 5M"),
                        hint(text("bitrateMin", "最低码率 minrate", ""),
                                "仅 VBR/CBR 生效，可留空；例 1500k"),
                        hint(text("bitrateMax", "最高码率 maxrate", ""),
                                "仅 VBR/CBR 生效，可留空；例 4000k"),
                        hint(text("bitrateBuf", "码率缓冲 bufsize", ""),
                                "仅 VBR/CBR 生效，可留空；常用 2 倍最高码率"),
                        hint(booleanField("twoPass", "两遍编码", false),
                                "只支持 x264/x265 + 固定码率；第一遍分析、第二遍出片，更接近目标体积"),
                        important(),
                        speedPresets(hint(select("preset", "编码速度档 preset", "medium",
                                "", "ultrafast", "superfast", "veryfast", "faster", "fast",
                                "medium", "slow", "slower", "veryslow"),
                                "软件编码用 ultrafast…veryslow；VP9/AV1 用 0–8；NVENC 用 p1–p7；"
                                        + "QSV 用 veryfast…veryslow；VAAPI 用 0–8（越大越快）；"
                                        + "AMF 用 quality/balanced/speed；Vulkan 用 5（最慢）~ 0（最快）",
                                "", "自动（由编码器决定）",
                                "ultrafast", "最快，体积最大",
                                "superfast", "很快，体积偏大",
                                "veryfast", "快，体积略大",
                                "faster", "偏快，压缩率略低",
                                "fast", "稍快于默认",
                                "medium", "默认平衡",
                                "slow", "更小体积，更慢",
                                "slower", "更小体积，明显更慢",
                                "veryslow", "最慢，压缩最好",
                                "p1", "NVENC 最快，质量最低",
                                "p2", "NVENC 更快，质量更低",
                                "p3", "NVENC 快，质量偏低",
                                "p4", "NVENC 平衡（默认）",
                                "p5", "NVENC 慢，质量好",
                                "p6", "NVENC 更慢，质量更好",
                                "p7", "NVENC 最慢，质量最好",
                                "0", "最慢，质量最好",
                                "1", "更慢，质量更好",
                                "2", "慢，质量偏上",
                                "3", "略慢于平衡",
                                "4", "平衡",
                                "5", "略快于平衡",
                                "6", "快，质量偏低",
                                "7", "更快，质量更低",
                                "8", "最快，质量最低")),
                        profileGroups(hint(select("profile", "编码 profile（档次）", "",
                                "", "baseline", "main", "high", "high10", "high422", "high444",
                                "main10", "main12", "mainstillpicture", "high444p", "rext", "mainsp", "scc",
                                "0", "1", "2"),
                                "随编码器变化；留空由编码器决定",
                                "", "自动",
                                "baseline", "最低兼容（H.264）",
                                "main", "主流档次（H.265 常用）",
                                "high", "高质量（H.264 常用）",
                                "high10", "10-bit（H.264）",
                                "high422", "4:2:2（H.264）",
                                "high444", "4:4:4（H.264）",
                                "main10", "10-bit（H.265）",
                                "main12", "12-bit（H.265）",
                                "mainstillpicture", "静态图像（H.265）",
                                "high444p", "4:4:4（NVENC H.264）",
                                "rext", "范围扩展（NVENC/QSV H.265）",
                                "mainsp", "可分级（QSV H.265）",
                                "scc", "屏幕内容（QSV H.265）",
                                "0", "AV1 profile 0（main）",
                                "1", "AV1 profile 1（high）",
                                "2", "AV1 profile 2（professional）")),
                        hint(text("level", "编码 level（级别）", ""), "留空自动；设备兼容问题时可指定 4.1/5.1"),
                        tuneGroups(hint(select("tune", "画面调优 tune", "",
                                "", "film", "animation", "grain", "stillimage", "psnr", "ssim",
                                "fastdecode", "zerolatency", "hq", "ll", "ull", "lossless"),
                                "随编码器变化；留空自动",
                                "", "自动",
                                "film", "实拍电影素材（x264/x265）",
                                "animation", "动画（x264/x265）",
                                "grain", "保留胶片颗粒（x264/x265）",
                                "stillimage", "静态图像（x264/x265）",
                                "psnr", "按 PSNR 优化",
                                "ssim", "按 SSIM 优化",
                                "fastdecode", "优先解码速度",
                                "zerolatency", "低延迟（直播）",
                                "hq", "NVENC 高质量",
                                "ll", "NVENC 低延迟",
                                "ull", "NVENC 超低延迟",
                                "lossless", "NVENC 无损")),
                        pixelFormatGroups(hint(select("pixelFormat", "像素格式", "",
                                "", "yuv420p", "yuv422p", "yuv444p", "yuv420p10le", "yuv422p10le", "yuv444p10le",
                                "p010le", "nv12", "p016le", "nv16", "gbrp", "gbrp10le", "gray", "gray10le", "qsv"),
                                "随编码器变化；留空跟随源。8-bit 用 yuv420p，10-bit 用 yuv420p10le / p010le",
                                "", "跟随源视频，不改像素格式",
                                "yuv420p", "8-bit 4:2:0，兼容性最好",
                                "yuv422p", "8-bit 4:2:2，采集/中间格式",
                                "yuv444p", "8-bit 4:4:4，无色度抽样",
                                "yuv420p10le", "10-bit 4:2:0，HDR/高精度",
                                "yuv422p10le", "10-bit 4:2:2，专业中间格式",
                                "yuv444p10le", "10-bit 4:4:4，无色度抽样",
                                "p010le", "10-bit P010，硬件/HDR 常用",
                                "nv12", "8-bit 4:2:0 半平面，硬件常用",
                                "p016le", "16-bit P016",
                                "nv16", "8-bit 4:2:2 半平面",
                                "gbrp", "平面 RGB",
                                "gbrp10le", "10-bit 平面 RGB",
                                "gray", "灰度 8-bit",
                                "gray10le", "灰度 10-bit",
                                "qsv", "QSV 内部格式（仅 QSV）")),
                        hint(booleanField("tenBit", "10-bit（未指定像素格式时）", false),
                                "只在像素格式留空时生效；会转 yuv420p10le"),
                        hint(text("gop", "关键帧间隔 GOP", ""), "留空由编码器决定；按秒算可填 250（约 10 秒 @25fps）"),
                        hint(text("threads", "线程数", ""), "留空自动；限制 CPU 占用时可填 4/6"),
                        hint(text("videoMap", "视频流选择 map", "0:v:0?"),
                                "0:v:0? 第一条视频；0:v? 保留全部视频流（copy 时常用）"),
                        hint(booleanField("videoKeepOthers", "保留其它视频流", false),
                                "map 全部视频流，只重编码索引指定的那条，其余直接 copy"),
                        hint(text("videoStreamIndex", "要重编码的视频流索引", "0"),
                                "从 0 开始；仅在「保留其它视频流」开启时生效")
                )
        }));

        sections.add(section("画面帧", "留空 = 跟随源视频。只填宽或高会按比例缩放；裁剪/旋转/去隔行按需开。", new JsonArray[]{
                fields(
                        hint(text("width", "宽", ""), "留空跟随源；只填一边另一边按比例"),
                        hint(text("height", "高", ""), "留空跟随源；常用 1080 / 720"),
                        hint(text("fps", "帧率", ""), "留空跟随源；可写 30 或 30000/1001"),
                        hint(text("crop", "裁剪（宽:高:x:y）", ""), "例 1920:800:0:140（宽:高:左上角 x:y）"),
                        hint(select("rotate", "旋转", "", "", "90", "180", "270"),
                                "顺时针旋转画面；手机竖拍视频常用",
                                "", "不旋转，保持源方向",
                                "90", "顺时针旋转 90°",
                                "180", "旋转 180°（上下颠倒）",
                                "270", "逆时针旋转 90°"),
                        hint(select("deinterlace", "去隔行", "", "", "yadif"),
                                "DVD/电视源用 yadif",
                                "", "不去隔行",
                                "yadif", "按帧去隔行，DVD/电视源常用")
                )
        }));

        sections.add(section("画面增强", "默认关闭。降噪/锐化/去色带会明显增加编码时间；调色留空表示不改。", new JsonArray[]{
                fields(
                        hint(select("denoise", "降噪", "", "", "hqdn3d", "nlmeans", "atadenoise", "bm3d"),
                                "hqdn3d 适合普通噪声；nlmeans/bm3d 更慢；atadenoise 轻量",
                                "", "不降噪",
                                "hqdn3d", "时空域降噪，速度与效果均衡",
                                "nlmeans", "高级降噪，效果更好但很慢",
                                "atadenoise", "轻量时间域降噪",
                                "bm3d", "高质量降噪，适合严重噪声"),
                        hint(text("denoiseParams", "降噪参数", ""),
                                "留空用 ffmpeg 默认；hqdn3d 可写 4 个参数，如 4:3:6:4.5"),
                        hint(select("sharpen", "锐化", "", "", "cas", "unsharp"),
                                "cas 轻量自适应；unsharp 传统锐化",
                                "", "不锐化",
                                "cas", "自适应对比度锐化，轻量",
                                "unsharp", "传统反遮罩锐化，参数更细"),
                        hint(text("sharpenParams", "锐化参数", ""),
                                "留空用默认；unsharp 可写 5:5:1.0:5:5:0.0"),
                        hint(booleanField("deband", "去色带", false),
                                "去除渐变色带，动画/低码率素材常用"),
                        hint(text("debandParams", "去色带参数", ""),
                                "留空用默认；例 1thr=0.02:2thr=0.02"),
                        hint(text("eqBrightness", "亮度 brightness", ""), "默认 0，范围 -1~1；例 0.03"),
                        hint(text("eqContrast", "对比度 contrast", ""), "默认 1；例 1.1 稍强"),
                        hint(text("eqSaturation", "饱和度 saturation", ""), "默认 1；例 1.2 更艳"),
                        hint(text("eqGamma", "伽马 gamma", ""), "默认 1；例 0.95 稍暗")
                )
        }));

        sections.add(section("色彩", "普通转码留空。只有 HDR↔SDR 转换或需要强制标记色彩时才填。", new JsonArray[]{
                fields(
                        hint(select("colorPrimaries", "色彩原色 primaries", "",
                                "", "bt709", "bt2020", "smpte170m", "smpte240m"),
                                "SDR 用 bt709；HDR 用 bt2020",
                                "", "跟随源，不强制标记",
                                "bt709", "BT.709，SDR 高清标准",
                                "bt2020", "BT.2020，HDR 广色域",
                                "smpte170m", "SMPTE 170M，NTSC/SD",
                                "smpte240m", "SMPTE 240M，老 HD 标准"),
                        hint(select("colorTransfer", "传输特性 transfer", "",
                                "", "bt709", "smpte2084", "arib-std-b67", "bt2020-10", "bt2020-12"),
                                "HDR10 用 smpte2084；HLG 用 arib-std-b67",
                                "", "跟随源，不强制标记",
                                "bt709", "BT.709，SDR 标准曲线",
                                "smpte2084", "PQ，HDR10 使用",
                                "arib-std-b67", "HLG，广播电视 HDR",
                                "bt2020-10", "BT.2020 10-bit 曲线",
                                "bt2020-12", "BT.2020 12-bit 曲线"),
                        hint(select("colorMatrix", "色彩矩阵 matrix", "",
                                "", "bt709", "bt2020nc", "smpte170m", "smpte240m"),
                                "HDR 用 bt2020nc",
                                "", "跟随源，不强制标记",
                                "bt709", "BT.709 矩阵，SDR 常用",
                                "bt2020nc", "BT.2020 非恒定矩阵，HDR 用",
                                "smpte170m", "SMPTE 170M，NTSC/SD",
                                "smpte240m", "SMPTE 240M，老 HD 标准"),
                        hint(select("colorRange", "取值范围 range", "", "", "tv", "pc"),
                                "视频通常 tv（有限范围）",
                                "", "跟随源，不强制标记",
                                "tv", "有限范围（16–235），视频标准",
                                "pc", "全范围（0–255），屏幕截图/游戏录制")
                )
        }));

        sections.add(section("音频", "默认 copy 原样保留全部音轨，不重编码；要统一格式或减小体积时改 encode。", new JsonArray[]{
                fields(
                        hint(select("audioMode", "音频模式", "copy", "copy", "encode", "none"),
                                "copy 原样保留全部音轨（不重编码）；encode 重编码；none 丢弃音频",
                                "copy", "保留全部音轨，不重编码（推荐）",
                                "encode", "按下面的编码器/码率重编码",
                                "none", "丢弃音频"),
                        important(),
                        audioEncoderGroups(hint(select("audioEncoder", "音频编码器", "aac", "aac", "libopus", "libmp3lame", "flac", "ac3", "eac3"),
                                "encode 时生效；选项随输出容器变化（webm 只能 Opus，mp4 系不能 FLAC/Opus）",
                                "aac", "AAC，MP4 最通用",
                                "libopus", "Opus，MKV/WebM 推荐，同码率更好",
                                "libmp3lame", "MP3，老设备兼容",
                                "flac", "FLAC，无损，体积大",
                                "ac3", "AC-3，老家庭影院兼容",
                                "eac3", "E-AC-3，杜比数字+")),
                        hint(text("audioBitrate", "音频码率", "192k"), "encode 时生效，例 192k / 320k"),
                        hint(text("audioChannels", "声道数", ""), "留空跟随源；2=立体声，6=5.1"),
                        hint(text("audioSampleRate", "采样率", ""), "留空跟随源；常用 48000"),
                        hint(text("audioMap", "音频流选择 map", "0:a?"),
                                "0:a? 全部音轨；只要第一条写 0:a:0?"),
                        hint(booleanField("audioKeepOthers", "保留其它音轨", false),
                                "map 全部音轨，只重编码索引指定的那条，其余直接 copy"),
                        hint(text("audioStreamIndex", "要重编码的音轨索引", "0"),
                                "从 0 开始；仅在「保留其它音轨」开启时生效"),
                        hint(text("audioVolume", "音量（倍数或 dB）", ""),
                                "只在重新编码时生效；例 1.0 原样、0.8 减小、-3dB 降低 3 dB"),
                        hint(booleanField("loudnormEnabled", "响度标准化", false),
                                "按 EBU R128 统一响度；只在重新编码时生效"),
                        hint(text("loudnormI", "目标响度 I", "-16"),
                                "单位 LUFS；常用 -16（播客/网页）、-23（广播）"),
                        hint(text("loudnormTP", "峰值 TP", "-1.5"),
                                "单位 dBTP；常用 -1.5 或 -2"),
                        hint(text("loudnormLRA", "动态范围 LRA", "11"),
                                "单位 LU；常用 11，越大保留越多动态")
                )
        }));

        sections.add(section("字幕", "copy 保留字幕轨；convert 转字幕格式；burn 烧进画面（需要字体）；none 丢弃。",
                new JsonArray[]{
                fields(
                        hint(select("subtitleMode", "字幕模式", "copy", "copy", "convert", "burn", "none"),
                                "copy 原样保留；convert 转格式（MP4 常用 mov_text）；burn 烧进画面；none 丢弃",
                                "copy", "保留字幕轨，不烧录（推荐）",
                                "convert", "转换字幕编码，MP4 里 ASS/PGS 先转 mov_text",
                                "burn", "烧录到画面，兼容一切播放器",
                                "none", "丢弃字幕"),
                        hint(select("subtitleCodec", "转换目标格式", "mov_text",
                                "mov_text", "ass", "srt", "webvtt"),
                                "仅在 convert 时生效；MP4/MOV 只能 mov_text",
                                "mov_text", "MP4/MOV 字幕，兼容性最好",
                                "ass", "保留样式，MKV 常用",
                                "srt", "纯文本字幕",
                                "webvtt", "WebVTT，Web 播放常用"),
                        important(),
                        hint(text("subtitleFile", "烧录字幕文件（相对媒体根）", ""),
                                "只在 burn 时使用；ASS/SRT 都行，中文字体缺失会显示方块"),
                        hint(text("subtitleFont", "字体", ""), "burn 时生效，例 Noto Sans CJK SC"),
                        hint(text("subtitleFontSize", "字号", ""), "burn 时生效，例 24"),
                        hint(text("subtitleStyle", "额外字幕样式", ""),
                                "force_style 片段，例 Bold=1,Outline=1；留空只改字体/字号"),
                        hint(text("subtitleMap", "字幕流选择 map", "0:s?"),
                                "0:s? 全部字幕；0:s:0? 第一条"),
                        hint(booleanField("subtitleKeepOthers", "保留其它字幕轨", false),
                                "map 全部字幕，只转换索引指定的那条，其余直接 copy"),
                        hint(text("subtitleStreamIndex", "要转换的字幕流索引", "0"),
                                "从 0 开始；仅在「保留其它字幕轨」开启时生效")
                )
        }));

        sections.add(section("元数据 / 章节 / 附件", "默认全保留，刮削信息不会丢；MP4 等受限容器会丢弃部分项。", new JsonArray[]{
                fields(
                        hint(text("metadataTitle", "标题", ""), "留空不写入"),
                        hint(text("metadataArtist", "艺术家", ""), "留空不写入"),
                        hint(text("metadataAlbum", "专辑", ""), "留空不写入"),
                        hint(text("metadataComment", "备注", ""), "留空不写入"),
                        hint(text("metadataDate", "日期", ""), "留空不写入"),
                        hint(text("metadataGenre", "流派", ""), "留空不写入"),
                        hint(textarea("metadataExtra", "额外元数据（每行 key=value）", ""),
                                "例 encoder=HDSL-web；每行一条，没有等号的行会被忽略"),
                        hint(text("chaptersFile", "章节文件（ffmetadata，相对媒体根）", ""),
                                "留空沿用源章节；填写则用这个 ffmetadata 文件"),
                        hint(booleanField("copyMetadata", "复制源元数据", true), "保留标题/演员等刮削信息"),
                        hint(booleanField("copyChapters", "复制源章节", true), "保留章节标记"),
                        hint(booleanField("copyAttachments", "复制附件（字体等）", true),
                                "MKV 里的字体/封面；mp4 装不下，入队前会被拦下")
                )
        }));

        sections.add(section("滤镜 / 高级", "普通转码不用改。滤镜按 ffmpeg 语法写，逗号分隔，例如 scale=-2:1080,yadif。", new JsonArray[]{
                fields(
                        hint(textarea("filterGraph", "附加滤镜链", ""),
                                "逗号分隔的 ffmpeg 滤镜，例 scale=-2:1080,yadif；留空不加滤镜")
                )
        }));

        root.add("sections", sections);
        if (availableEncoders != null) {
            markUnavailableEncoders(sections, availableEncoders);
        }
        return root;
    }

    private static void markUnavailableEncoders(JsonArray sections, java.util.Collection<String> available) {
        for (JsonElement sectionElement : sections) {
            JsonObject section = sectionElement.getAsJsonObject();
            if (!section.has("fields")) {
                continue;
            }
            for (JsonElement fieldElement : section.getAsJsonArray("fields")) {
                JsonObject field = fieldElement.getAsJsonObject();
                if (!"videoEncoder".equals(field.get("id").getAsString())) {
                    continue;
                }
                JsonArray unavailable = new JsonArray();
                for (JsonElement option : field.getAsJsonArray("options")) {
                    String name = option.getAsString();
                    if (!"copy".equals(name) && !"none".equals(name) && !available.contains(name)) {
                        unavailable.add(name);
                    }
                }
                field.add("unavailable", unavailable);
            }
        }
    }

    private static JsonObject section(String title, String hint, JsonArray[] groups) {
        JsonObject section = new JsonObject();
        section.addProperty("title", title);
        section.addProperty("hint", hint);
        JsonArray fields = new JsonArray();
        for (JsonArray group : groups) {
            for (int i = 0; i < group.size(); i++) {
                JsonObject field = group.get(i).getAsJsonObject();
                // `important()` is a marker: it raises the flag on the field declared
                // immediately before it, so the schema literals stay readable.
                if (field.has("__important")) {
                    if (fields.size() > 0) {
                        fields.get(fields.size() - 1).getAsJsonObject().addProperty("important", true);
                    }
                    continue;
                }
                fields.add(field);
            }
        }
        section.add("fields", fields);
        return section;
    }

    private static JsonArray fields(JsonObject... fields) {
        JsonArray array = new JsonArray();
        for (JsonObject field : fields) {
            array.add(field);
        }
        return array;
    }

    private static JsonObject text(String id, String label, String value) {
        JsonObject field = base(id, label, "text");
        field.addProperty("default", value);
        return field;
    }

    private static JsonObject select(String id, String label, String value, String... options) {
        JsonObject field = base(id, label, "select");
        field.addProperty("default", value);
        JsonArray array = new JsonArray();
        for (String option : options) {
            array.add(option);
        }
        field.add("options", array);
        return field;
    }

    private static JsonObject textarea(String id, String label, String value) {
        JsonObject field = base(id, label, "textarea");
        field.addProperty("default", value);
        return field;
    }

    private static JsonObject booleanField(String id, String label, boolean value) {
        JsonObject field = base(id, label, "switch");
        field.addProperty("default", value);
        return field;
    }

    // 编码器家族：决定速度档、profile、tune、像素格式的可用值。
    // 键与前端 labels.ts 的 encoderFamily() 保持一致。
    private static final String[] SOFTWARE_SPEED = {"", "ultrafast", "superfast", "veryfast", "faster", "fast",
            "medium", "slow", "slower", "veryslow"};
    private static final String[] NVENC_SPEED = {"", "p1", "p2", "p3", "p4", "p5", "p6", "p7"};
    private static final String[] QSV_SPEED = {"", "veryfast", "faster", "fast", "medium", "slow", "slower", "veryslow"};
    private static final String[] NUMERIC_SPEED = {"", "0", "1", "2", "3", "4", "5", "6", "7", "8"};
    private static final String[] SVT_SPEED = {"", "0", "1", "2", "3", "4", "5", "6", "7",
            "8", "9", "10", "11", "12", "13"};
    private static final String[] RAV1E_SPEED = {"", "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10"};
    private static final String[] AMF_QUALITY = {"", "quality", "high_quality", "balanced", "speed"};
    private static final String[] VULKAN_QUALITY = {"", "5", "4", "3", "2", "1", "0"};

    /// 速度档随编码器家族变化：x264/x265 命名档，VP9/AV1 与 VAAPI 数值档，NVENC p1–p7，QSV 命名档。
    private static JsonObject speedPresets(JsonObject field) {
        putGroup(field, "x264", SOFTWARE_SPEED);
        putGroup(field, "x265", SOFTWARE_SPEED);
        putGroup(field, "vp9", NUMERIC_SPEED);
        putGroup(field, "av1", NUMERIC_SPEED);
        putGroup(field, "nvencH264", NVENC_SPEED);
        putGroup(field, "nvencHevc", NVENC_SPEED);
        putGroup(field, "qsvH264", QSV_SPEED);
        putGroup(field, "qsvHevc", QSV_SPEED);
        putGroup(field, "vaapiH264", NUMERIC_SPEED);
        putGroup(field, "vaapiHevc", NUMERIC_SPEED);
        putGroup(field, "nvencAv1", NVENC_SPEED);
        putGroup(field, "qsvVp9", QSV_SPEED);
        putGroup(field, "qsvAv1", QSV_SPEED);
        putGroup(field, "vaapiVp9");
        putGroup(field, "vaapiAv1");
        putGroup(field, "av1svt", SVT_SPEED);
        putGroup(field, "av1rav1e", RAV1E_SPEED);
        putGroup(field, "amfH264", AMF_QUALITY);
        putGroup(field, "amfHevc", AMF_QUALITY);
        putGroup(field, "amfAv1", AMF_QUALITY);
        putGroup(field, "d3d12vaH264");
        putGroup(field, "d3d12vaHevc");
        putGroup(field, "d3d12vaAv1");
        putGroup(field, "vulkanH264", VULKAN_QUALITY);
        putGroup(field, "vulkanHevc", VULKAN_QUALITY);
        putGroup(field, "vulkanAv1", VULKAN_QUALITY);
        return field;
    }

    /// CRF 只有软件编码器支持；硬件编码器用 CQP + 固定码率。
    private static JsonObject qualityModeGroups(JsonObject field) {
        String[] software = {"crf", "cqp", "bitrate", "vbr", "cbr", "none"};
        String[] softwareAv1Vp9 = {"crf", "bitrate", "vbr", "cbr", "none"};
        String[] hardware = {"cqp", "bitrate", "vbr", "cbr", "none"};
        putGroup(field, "x264", software);
        putGroup(field, "x265", software);
        putGroup(field, "vp9", softwareAv1Vp9);
        putGroup(field, "av1", softwareAv1Vp9);
        putGroup(field, "nvencH264", hardware);
        putGroup(field, "nvencHevc", hardware);
        putGroup(field, "qsvH264", hardware);
        putGroup(field, "qsvHevc", hardware);
        putGroup(field, "vaapiH264", hardware);
        putGroup(field, "vaapiHevc", hardware);
        putGroup(field, "nvencAv1", hardware);
        putGroup(field, "qsvVp9", hardware);
        putGroup(field, "qsvAv1", hardware);
        putGroup(field, "vaapiVp9", hardware);
        putGroup(field, "vaapiAv1", hardware);
        putGroup(field, "av1svt", softwareAv1Vp9);
        putGroup(field, "av1rav1e", softwareAv1Vp9);
        putGroup(field, "amfH264", hardware);
        putGroup(field, "amfHevc", hardware);
        putGroup(field, "amfAv1", hardware);
        putGroup(field, "d3d12vaH264", hardware);
        putGroup(field, "d3d12vaHevc", hardware);
        putGroup(field, "d3d12vaAv1", hardware);
        putGroup(field, "vulkanH264", hardware);
        putGroup(field, "vulkanHevc", hardware);
        putGroup(field, "vulkanAv1", hardware);
        return field;
    }

    private static JsonObject profileGroups(JsonObject field) {
        putGroup(field, "x264", "", "baseline", "main", "high", "high10", "high422", "high444");
        putGroup(field, "x265", "", "main", "main10", "main12", "mainstillpicture");
        putGroup(field, "vp9");
        putGroup(field, "av1", "", "0", "1", "2");
        putGroup(field, "nvencH264", "", "baseline", "main", "high", "high10", "high422", "high444p");
        putGroup(field, "nvencHevc", "", "main", "main10", "rext");
        putGroup(field, "qsvH264", "", "baseline", "main", "high");
        putGroup(field, "qsvHevc", "", "main", "main10", "mainsp", "rext", "scc");
        putGroup(field, "vaapiH264", "", "baseline", "main", "high");
        putGroup(field, "vaapiHevc", "", "main", "main10");
        putGroup(field, "nvencAv1", "");
        putGroup(field, "qsvVp9", "");
        putGroup(field, "qsvAv1", "");
        putGroup(field, "vaapiVp9", "");
        putGroup(field, "vaapiAv1", "");
        putGroup(field, "av1svt", "");
        putGroup(field, "av1rav1e", "");
        putGroup(field, "amfH264", "", "main", "high", "constrained_baseline", "constrained_high");
        putGroup(field, "amfHevc", "", "main", "main10");
        putGroup(field, "amfAv1", "", "main");
        putGroup(field, "d3d12vaH264", "", "main", "high", "high10");
        putGroup(field, "d3d12vaHevc", "", "main", "main10");
        putGroup(field, "d3d12vaAv1", "", "main", "high", "professional");
        putGroup(field, "vulkanH264", "", "constrained_baseline", "main", "high", "high444p");
        putGroup(field, "vulkanHevc", "", "main", "main10", "rext");
        putGroup(field, "vulkanAv1", "", "main", "high", "professional");
        return field;
    }

    private static JsonObject tuneGroups(JsonObject field) {
        String[] x26x = {"", "film", "animation", "grain", "stillimage", "psnr", "ssim", "fastdecode", "zerolatency"};
        putGroup(field, "x264", x26x);
        putGroup(field, "x265", x26x);
        putGroup(field, "vp9", "", "psnr", "ssim");
        putGroup(field, "av1", "", "psnr", "ssim");
        putGroup(field, "nvencH264", "", "hq", "ll", "ull", "lossless");
        putGroup(field, "nvencHevc", "", "hq", "ll", "ull", "lossless");
        putGroup(field, "qsvH264");
        putGroup(field, "qsvHevc");
        putGroup(field, "vaapiH264");
        putGroup(field, "vaapiHevc");
        putGroup(field, "nvencAv1", "", "hq", "ll", "ull", "lossless");
        putGroup(field, "qsvVp9");
        putGroup(field, "qsvAv1");
        putGroup(field, "vaapiVp9");
        putGroup(field, "vaapiAv1");
        putGroup(field, "av1svt");
        putGroup(field, "av1rav1e");
        putGroup(field, "amfH264", "", "transcoding", "ultralowlatency", "lowlatency", "webcam",
                "high_quality", "lowlatency_high_quality");
        putGroup(field, "amfHevc", "", "transcoding", "ultralowlatency", "lowlatency", "webcam",
                "high_quality", "lowlatency_high_quality");
        putGroup(field, "amfAv1", "", "transcoding", "ultralowlatency", "lowlatency",
                "high_quality", "lowlatency_high_quality");
        putGroup(field, "d3d12vaH264");
        putGroup(field, "d3d12vaHevc");
        putGroup(field, "d3d12vaAv1");
        putGroup(field, "vulkanH264", "", "default", "hq", "ll", "ull", "lossless");
        putGroup(field, "vulkanHevc", "", "default", "hq", "ll", "ull", "lossless");
        putGroup(field, "vulkanAv1", "", "default", "hq", "ll", "ull", "lossless");
        return field;
    }

    private static JsonObject pixelFormatGroups(JsonObject field) {
        putGroup(field, "x264", "", "yuv420p", "yuv422p", "yuv444p", "yuv420p10le", "yuv422p10le", "yuv444p10le",
                "nv12", "nv16", "gray", "gray10le");
        putGroup(field, "x265", "", "yuv420p", "yuv422p", "yuv444p", "yuv420p10le", "yuv422p10le", "yuv444p10le",
                "yuv420p12le", "yuv444p12le", "gbrp", "gbrp10le");
        putGroup(field, "vp9", "", "yuv420p", "yuv422p", "yuv444p", "yuv420p10le", "yuv422p10le", "yuv444p10le",
                "gbrp", "gbrp10le");
        putGroup(field, "av1", "", "yuv420p", "yuv422p", "yuv444p", "yuv420p10le", "yuv422p10le", "yuv444p10le",
                "gbrp", "gbrp10le");
        putGroup(field, "nvencH264", "", "yuv420p", "nv12", "p010le", "yuv444p", "p016le", "nv16", "gbrp", "gbrp10le");
        putGroup(field, "nvencHevc", "", "yuv420p", "nv12", "p010le", "yuv444p", "p016le", "nv16", "gbrp", "gbrp10le");
        putGroup(field, "qsvH264", "", "nv12", "qsv");
        putGroup(field, "qsvHevc", "", "nv12", "p010le", "qsv");
        putGroup(field, "vaapiH264", "", "nv12");
        putGroup(field, "vaapiHevc", "", "nv12", "p010le");
        putGroup(field, "nvencAv1", "", "yuv420p", "nv12", "p010le", "yuv444p", "p016le", "gbrp", "gbrp10le");
        putGroup(field, "qsvVp9", "", "nv12", "p010le");
        putGroup(field, "qsvAv1", "", "nv12", "p010le");
        putGroup(field, "vaapiVp9", "", "nv12");
        putGroup(field, "vaapiAv1", "", "nv12", "p010le");
        putGroup(field, "av1svt", "", "yuv420p", "yuv420p10le");
        putGroup(field, "av1rav1e", "", "yuv420p", "yuv420p10le", "yuv444p", "yuv444p10le");
        putGroup(field, "amfH264", "", "nv12", "yuv420p", "d3d11", "dxva2_vld", "p010le", "amf",
                "bgr0", "rgb0", "bgra", "argb", "rgba", "x2bgr10le", "rgbaf16le");
        putGroup(field, "amfHevc", "", "nv12", "yuv420p", "d3d11", "dxva2_vld", "p010le", "amf",
                "bgr0", "rgb0", "bgra", "argb", "rgba", "x2bgr10le", "rgbaf16le");
        putGroup(field, "amfAv1", "", "nv12", "yuv420p", "d3d11", "dxva2_vld", "p010le", "amf",
                "bgr0", "rgb0", "bgra", "argb", "rgba", "x2bgr10le", "rgbaf16le");
        putGroup(field, "d3d12vaH264", "", "d3d12");
        putGroup(field, "d3d12vaHevc", "", "d3d12");
        putGroup(field, "d3d12vaAv1", "", "d3d12");
        putGroup(field, "vulkanH264", "", "vulkan");
        putGroup(field, "vulkanHevc", "", "vulkan");
        putGroup(field, "vulkanAv1", "", "vulkan");
        return field;
    }

    /// 容器约束视频编码器：WebM 只装 VP9/AV1，M4A 是纯音频容器。
    private static JsonObject videoEncoderGroups(JsonObject field) {
        String[] all = {"copy", "none",
                "libx264", "h264_nvenc", "h264_qsv", "h264_vaapi",
                "h264_amf", "h264_d3d12va", "h264_vulkan",
                "libx265", "hevc_nvenc", "hevc_qsv", "hevc_vaapi",
                "hevc_amf", "hevc_d3d12va", "hevc_vulkan",
                "libvpx-vp9", "vp9_qsv", "vp9_vaapi",
                "libaom-av1", "libsvtav1", "librav1e", "av1_nvenc", "av1_qsv", "av1_vaapi",
                "av1_amf", "av1_d3d12va", "av1_vulkan"};
        putGroup(field, "mkv", all);
        putGroup(field, "mp4", all);
        putGroup(field, "mov", all);
        putGroup(field, "avi", all);
        putGroup(field, "ts", all);
        putGroup(field, "m4a", "none");
        putGroup(field, "webm", "none",
                "libvpx-vp9", "vp9_qsv", "vp9_vaapi",
                "libaom-av1", "libsvtav1", "librav1e", "av1_nvenc", "av1_qsv", "av1_vaapi",
                "av1_amf", "av1_d3d12va", "av1_vulkan");
        return field;
    }

    /// 音频编码器的可选值由容器决定：webm 只能 Opus，mp4 系装不下 FLAC/Opus。
    private static JsonObject audioEncoderGroups(JsonObject field) {
        String[] all = {"aac", "libopus", "libmp3lame", "flac", "ac3", "eac3"};
        String[] mp4 = {"aac", "libmp3lame", "ac3", "eac3"};
        putGroup(field, "mkv", all);
        putGroup(field, "mp4", mp4);
        putGroup(field, "mov", mp4);
        putGroup(field, "m4a", mp4);
        putGroup(field, "webm", "libopus");
        putGroup(field, "avi", "aac", "libmp3lame", "ac3");
        putGroup(field, "ts", "aac", "libmp3lame", "ac3", "eac3");
        return field;
    }

    private static void putGroup(JsonObject field, String key, String... values) {
        JsonObject groups = field.has("optionGroups") ? field.getAsJsonObject("optionGroups") : new JsonObject();
        JsonArray array = new JsonArray();
        for (String value : values) {
            array.add(value);
        }
        groups.add(key, array);
        field.add("optionGroups", groups);
    }

    private static JsonObject hint(JsonObject field, String hint, String... optionHints) {
        field.addProperty("hint", hint);
        if (optionHints.length >= 2) {
            JsonObject map = new JsonObject();
            for (int i = 0; i + 1 < optionHints.length; i += 2) {
                map.addProperty(optionHints[i], optionHints[i + 1]);
            }
            field.add("optionHints", map);
        }
        return field;
    }

    private static JsonObject important() {
        JsonObject marker = new JsonObject();
        marker.addProperty("__important", true);
        return marker;
    }

    private static JsonObject base(String id, String label, String type) {
        JsonObject field = new JsonObject();
        field.addProperty("id", id);
        field.addProperty("label", label);
        field.addProperty("type", type);
        return field;
    }
}
