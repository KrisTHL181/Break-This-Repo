import type { TranscodePreset, TranscodeSchema } from "../../api";

/**
 * 预设里保存的是 ffmpeg 的原始取值（libx264 / crf / copy …）。
 * 面板里给它们配中文短名：下拉框收起时、参数总览、预设预览都显示中文，
 * 原始取值放在下拉项右侧或悬停提示里，方便对照命令行。
 */
const VALUE_LABELS: Record<string, Record<string, string>> = {
  container: {
    mkv: "MKV",
    mp4: "MP4",
    mov: "MOV",
    webm: "WebM",
    avi: "AVI",
    ts: "MPEG-TS",
    m4a: "M4A（纯音频）",
  },
  outputNaming: {
    suffix: "追加后缀",
    timestamp: "时间戳",
    none: "不追加",
  },
  videoEncoder: {
    copy: "直接复制",
    none: "丢弃视频",
    libx264: "H.264（x264）",
    libx265: "H.265（x265）",
    "libvpx-vp9": "VP9（libvpx）",
    "libaom-av1": "AV1（aom）",
    libsvtav1: "AV1（SVT-AV1）",
    librav1e: "AV1（rav1e）",
    h264_nvenc: "H.264（NVENC）",
    hevc_nvenc: "H.265（NVENC）",
    av1_nvenc: "AV1（NVENC）",
    h264_vaapi: "H.264（VAAPI）",
    hevc_vaapi: "H.265（VAAPI）",
    vp9_vaapi: "VP9（VAAPI）",
    av1_vaapi: "AV1（VAAPI）",
    h264_qsv: "H.264（QSV）",
    hevc_qsv: "H.265（QSV）",
    vp9_qsv: "VP9（QSV）",
    av1_qsv: "AV1（QSV）",
    h264_amf: "H.264（AMF）",
    hevc_amf: "H.265（AMF）",
    av1_amf: "AV1（AMF）",
    h264_d3d12va: "H.264（D3D12VA）",
    hevc_d3d12va: "H.265（D3D12VA）",
    av1_d3d12va: "AV1（D3D12VA）",
    h264_vulkan: "H.264（Vulkan）",
    hevc_vulkan: "H.265（Vulkan）",
    av1_vulkan: "AV1（Vulkan）",
  },
  qualityMode: {
    crf: "CRF 恒定质量",
    cqp: "CQP 固定质量",
    bitrate: "固定码率（旧）",
    vbr: "VBR 可变码率",
    cbr: "CBR 固定码率",
    none: "不指定质量",
  },
  subtitleMode: {
    copy: "保留字幕轨",
    convert: "转换字幕格式",
    burn: "烧录到画面",
    none: "丢弃字幕",
  },
  subtitleCodec: {
    mov_text: "mov_text（MP4）",
    ass: "ASS",
    srt: "SRT",
    webvtt: "WebVTT",
  },
  preset: {
    "": "自动",
    ultrafast: "极快",
    superfast: "超快",
    veryfast: "很快",
    faster: "较快",
    fast: "快",
    medium: "平衡",
    slow: "慢",
    slower: "更慢",
    veryslow: "最慢",
    p1: "P1（最快）",
    p2: "P2（更快）",
    p3: "P3（快）",
    p4: "P4（平衡）",
    p5: "P5（慢）",
    p6: "P6（更慢）",
    p7: "P7（最慢）",
    high_quality: "高质量优先",
    quality: "质量优先",
    balanced: "平衡",
    speed: "速度优先",
    "0": "0（最慢，最好）",
    "1": "1（更慢）",
    "2": "2（慢）",
    "3": "3（略慢）",
    "4": "4（平衡）",
    "5": "5（略快）",
    "6": "6（快）",
    "7": "7（更快）",
    "8": "8（最快）",
  },
  pixelFormat: {
    "": "跟随源",
    yuv420p: "8-bit 4:2:0",
    yuv422p: "8-bit 4:2:2",
    yuv444p: "8-bit 4:4:4",
    yuv420p10le: "10-bit 4:2:0",
    yuv422p10le: "10-bit 4:2:2",
    yuv444p10le: "10-bit 4:4:4",
    yuv420p12le: "12-bit 4:2:0",
    yuv444p12le: "12-bit 4:4:4",
    p010le: "10-bit P010（HDR）",
    p016le: "16-bit P016",
    nv12: "8-bit NV12 半平面",
    nv16: "8-bit NV16 半平面",
    gbrp: "平面 RGB",
    gbrp10le: "10-bit 平面 RGB",
    gray: "灰度 8-bit",
    gray10le: "灰度 10-bit",
    qsv: "QSV 内部格式",
  },
  rotate: {
    "": "不旋转",
    "90": "顺时针 90°",
    "180": "旋转 180°",
    "270": "逆时针 90°",
  },
  deinterlace: {
    "": "不去隔行",
    yadif: "yadif 去隔行",
  },
  colorPrimaries: {
    "": "跟随源",
    bt709: "BT.709（SDR）",
    bt2020: "BT.2020（HDR 广色域）",
    smpte170m: "SMPTE 170M（NTSC/SD）",
    smpte240m: "SMPTE 240M（老 HD）",
  },
  colorTransfer: {
    "": "跟随源",
    bt709: "BT.709（SDR）",
    smpte2084: "PQ（HDR10）",
    "arib-std-b67": "HLG（广播 HDR）",
    "bt2020-10": "BT.2020 10-bit",
    "bt2020-12": "BT.2020 12-bit",
  },
  colorMatrix: {
    "": "跟随源",
    bt709: "BT.709（SDR）",
    bt2020nc: "BT.2020 非恒定（HDR）",
    smpte170m: "SMPTE 170M（NTSC/SD）",
    smpte240m: "SMPTE 240M（老 HD）",
  },
  colorRange: {
    "": "跟随源",
    tv: "有限范围 TV（16–235）",
    pc: "全范围 PC（0–255）",
  },
  audioMode: {
    copy: "保留原音轨",
    encode: "重新编码",
    none: "丢弃音频",
  },
  audioEncoder: {
    aac: "AAC",
    libopus: "Opus",
    libmp3lame: "MP3",
    flac: "FLAC（无损）",
    ac3: "AC-3",
    eac3: "E-AC-3",
  },
  denoise: {
    "": "不降噪",
    hqdn3d: "hqdn3d 时空降噪",
    nlmeans: "nlmeans 高级降噪",
    atadenoise: "atadenoise 轻量降噪",
    bm3d: "bm3d 高质量降噪",
  },
  sharpen: {
    "": "不锐化",
    cas: "cas 自适应锐化",
    unsharp: "unsharp 传统锐化",
  },
};

/// 中文短名；没有映射时退回原始取值（空值显示“（默认）”）。
export function valueLabel(fieldId: string, value: unknown): string {
  const raw = String(value ?? "");
  const labellings = VALUE_LABELS[fieldId];
  const label = labellings?.[raw];
  if (label) return label;
  return raw === "" ? "（默认）" : raw;
}

/// 同一个字段里“原始取值 + 中文说明”，用于悬停提示。
export function valueTip(
  schema: TranscodeSchema | null,
  fieldId: string,
  value: unknown,
  extraHint?: string,
): string {
  const raw = String(value ?? "");
  const field = schema?.sections.flatMap((section) => section.fields).find((item) => item.id === fieldId);
  const description = field?.optionHints?.[raw] ?? extraHint ?? field?.hint ?? "";
  const head = raw === "" ? "（未设置，使用默认值）" : `原始取值 ${raw}`;
  return description ? `${head}\n${description}` : head;
}

// 依赖别的选项才有意义的字段：视频不重新编码时，画面/质量/编码档位全部不生效。
const VIDEO_ENCODER_FIELDS = new Set([
  "qualityMode", "qualityValue", "bitrateMin", "bitrateMax", "bitrateBuf", "preset", "profile", "level", "tune",
  "pixelFormat", "tenBit", "gop", "threads",
  "width", "height", "fps", "crop", "rotate", "deinterlace", "filterGraph",
  "colorPrimaries", "colorTransfer", "colorMatrix", "colorRange",
  "denoise", "denoiseParams", "sharpen", "sharpenParams", "deband", "debandParams",
  "eqBrightness", "eqContrast", "eqSaturation", "eqGamma",
]);
const AUDIO_ENCODE_FIELDS = new Set([
  "audioEncoder", "audioBitrate", "audioChannels", "audioSampleRate",
  "audioVolume", "loudnormEnabled", "loudnormI", "loudnormTP", "loudnormLRA",
  "audioStreamIndex",
]);
const SUBTITLE_BURN_FIELDS = new Set(["subtitleFile", "subtitleFont", "subtitleFontSize", "subtitleStyle"]);
const NO_TUNE_FAMILIES = new Set([
  "qsvH264", "qsvHevc", "vaapiH264", "vaapiHevc",
  "qsvVp9", "qsvAv1", "vaapiVp9", "vaapiAv1", "av1svt", "av1rav1e",
  "d3d12vaH264", "d3d12vaHevc", "d3d12vaAv1",
]);
const PROFILELESS_FAMILIES = new Set([
  "vp9", "nvencAv1", "qsvVp9", "qsvAv1", "vaapiVp9", "vaapiAv1", "av1svt", "av1rav1e",
]);
const NO_SPEED_FAMILIES = new Set([
  "vaapiVp9", "vaapiAv1", "d3d12vaH264", "d3d12vaHevc", "d3d12vaAv1",
]);
const RESTRICTIVE_ATTACHMENT_CONTAINERS = new Set(["mp4", "mov", "avi", "ts", "m4a"]);

/// 编码器家族：决定速度档、profile、tune、像素格式，键与后端 PresetSchema 的 optionGroups 一致。
export function encoderFamily(encoder: string): string {
  switch (encoder) {
    case "libx264": return "x264";
    case "libx265": return "x265";
    case "libvpx-vp9": return "vp9";
    case "libaom-av1": return "av1";
    case "libsvtav1": return "av1svt";
    case "librav1e": return "av1rav1e";
    case "h264_nvenc": return "nvencH264";
    case "hevc_nvenc": return "nvencHevc";
    case "av1_nvenc": return "nvencAv1";
    case "h264_vaapi": return "vaapiH264";
    case "hevc_vaapi": return "vaapiHevc";
    case "vp9_vaapi": return "vaapiVp9";
    case "av1_vaapi": return "vaapiAv1";
    case "h264_qsv": return "qsvH264";
    case "hevc_qsv": return "qsvHevc";
    case "vp9_qsv": return "qsvVp9";
    case "av1_qsv": return "qsvAv1";
    case "h264_amf": return "amfH264";
    case "hevc_amf": return "amfHevc";
    case "av1_amf": return "amfAv1";
    case "h264_d3d12va": return "d3d12vaH264";
    case "hevc_d3d12va": return "d3d12vaHevc";
    case "av1_d3d12va": return "d3d12vaAv1";
    case "h264_vulkan": return "vulkanH264";
    case "hevc_vulkan": return "vulkanHevc";
    case "av1_vulkan": return "vulkanAv1";
    default: return "x264";
  }
}

/// 各编码器家族的速度档：可选值与切换时的默认档。
const SPEED_PRESETS: Record<string, { values: string[]; fallback: string }> = {
  x264: {
    values: ["", "ultrafast", "superfast", "veryfast", "faster", "fast", "medium", "slow", "slower", "veryslow"],
    fallback: "medium",
  },
  x265: {
    values: ["", "ultrafast", "superfast", "veryfast", "faster", "fast", "medium", "slow", "slower", "veryslow"],
    fallback: "medium",
  },
  vp9: { values: ["", "0", "1", "2", "3", "4", "5", "6", "7", "8"], fallback: "" },
  av1: { values: ["", "0", "1", "2", "3", "4", "5", "6", "7", "8"], fallback: "" },
  nvencH264: { values: ["", "p1", "p2", "p3", "p4", "p5", "p6", "p7"], fallback: "p4" },
  nvencHevc: { values: ["", "p1", "p2", "p3", "p4", "p5", "p6", "p7"], fallback: "p4" },
  nvencAv1: { values: ["", "p1", "p2", "p3", "p4", "p5", "p6", "p7"], fallback: "p4" },
  qsvH264: { values: ["", "veryfast", "faster", "fast", "medium", "slow", "slower", "veryslow"], fallback: "medium" },
  qsvHevc: { values: ["", "veryfast", "faster", "fast", "medium", "slow", "slower", "veryslow"], fallback: "medium" },
  qsvVp9: { values: ["", "veryfast", "faster", "fast", "medium", "slow", "slower", "veryslow"], fallback: "medium" },
  qsvAv1: { values: ["", "veryfast", "faster", "fast", "medium", "slow", "slower", "veryslow"], fallback: "medium" },
  vaapiH264: { values: ["", "0", "1", "2", "3", "4", "5", "6", "7", "8"], fallback: "" },
  vaapiHevc: { values: ["", "0", "1", "2", "3", "4", "5", "6", "7", "8"], fallback: "" },
  vaapiVp9: { values: [], fallback: "" },
  vaapiAv1: { values: [], fallback: "" },
  av1svt: {
    values: ["", "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13"],
    fallback: "8",
  },
  av1rav1e: { values: ["", "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10"], fallback: "6" },
  amfH264: { values: ["", "quality", "high_quality", "balanced", "speed"], fallback: "quality" },
  amfHevc: { values: ["", "quality", "high_quality", "balanced", "speed"], fallback: "quality" },
  amfAv1: { values: ["", "quality", "high_quality", "balanced", "speed"], fallback: "high_quality" },
  d3d12vaH264: { values: [], fallback: "" },
  d3d12vaHevc: { values: [], fallback: "" },
  d3d12vaAv1: { values: [], fallback: "" },
  vulkanH264: { values: ["", "5", "4", "3", "2", "1", "0"], fallback: "" },
  vulkanHevc: { values: ["", "5", "4", "3", "2", "1", "0"], fallback: "" },
  vulkanAv1: { values: ["", "5", "4", "3", "2", "1", "0"], fallback: "" },
};

/// 当前编码器可用的速度档；schema 的 optionGroups 是权威列表，拿不到时退回本地映射。
export function speedPresetOptions(fieldOptions: string[] | undefined, encoder: string): string[] {
  return SPEED_PRESETS[encoderFamily(encoder)]?.values ?? fieldOptions ?? [];
}

/// 切换编码器后把旧速度档换成新编码器能接受的值；不认识就落到该家族的默认档。
export function remapSpeedPreset(encoder: string, current: unknown): string {
  const group = SPEED_PRESETS[encoderFamily(encoder)];
  if (!group) return String(current ?? "");
  const value = String(current ?? "");
  return group.values.includes(value) ? value : group.fallback;
}

/// 返回空串表示可编辑；否则是「为什么变灰」的说明，同时显示在字段下方。
export function inactiveReason(fieldId: string, preset: TranscodePreset): string {
  const encoder = String(preset.videoEncoder ?? "");
  const family = encoderFamily(encoder);
  const container = String(preset.container ?? "").toLowerCase();
  if ((encoder === "copy" || encoder === "none") && VIDEO_ENCODER_FIELDS.has(fieldId)) {
    return encoder === "none" ? "视频已丢弃，此项不生效" : "不重新编码，此项不生效";
  }
  if (fieldId === "tune" && NO_TUNE_FAMILIES.has(family)) {
    return "此编码器没有 tune 选项";
  }
  if (fieldId === "preset" && NO_SPEED_FAMILIES.has(family)) {
    return "此编码器没有速度档";
  }
  if (fieldId === "profile" && PROFILELESS_FAMILIES.has(family)) {
    return "此编码器没有 profile 选项";
  }
  if (["bitrateMin", "bitrateMax", "bitrateBuf"].includes(fieldId)
      && !["vbr", "cbr"].includes(String(preset.qualityMode ?? "crf"))) {
    return "仅 VBR/CBR 生效";
  }
  if (fieldId === "qualityValue" && String(preset.qualityMode ?? "crf") === "none") {
    return "已选「不指定质量」";
  }
  if (fieldId === "subtitleCodec" && String(preset.subtitleMode ?? "copy") !== "convert") {
    return "字幕未转换，此项不生效";
  }
  if (fieldId === "videoStreamIndex" && preset.videoKeepOthers !== true) {
    return "未开启「保留其它视频流」";
  }
  if (fieldId === "audioStreamIndex" && preset.audioKeepOthers !== true) {
    return "未开启「保留其它音轨」";
  }
  if (fieldId === "audioKeepOthers" && String(preset.audioMode ?? "copy") !== "encode") {
    return "音频未重新编码，此项不生效";
  }
  if (fieldId === "subtitleStreamIndex" && preset.subtitleKeepOthers !== true) {
    return "未开启「保留其它字幕轨」";
  }
  if (fieldId === "videoKeepOthers" && (encoder === "copy" || encoder === "none")) {
    return "不重新编码视频，此项不生效";
  }
  if (fieldId === "denoiseParams" && String(preset.denoise ?? "") === "") {
    return "未选择降噪方式";
  }
  if (fieldId === "sharpenParams" && String(preset.sharpen ?? "") === "") {
    return "未选择锐化方式";
  }
  if (fieldId === "debandParams" && preset.deband !== true) {
    return "去色带未开启";
  }
  if (fieldId === "tenBit" && String(preset.pixelFormat ?? "") !== "") {
    return "已指定像素格式，10-bit 开关不生效";
  }
  if (fieldId === "outputSuffix" && String(preset.outputNaming ?? "") !== "suffix") {
    return "自动命名不是「追加后缀」";
  }
  if (fieldId === "subtitleMap" && String(preset.subtitleMode ?? "copy") !== "copy") {
    return "字幕不是「保留字幕轨」，map 不生效";
  }
  if (fieldId === "audioMap" && String(preset.audioMode ?? "copy") === "none") {
    return "已丢弃音频，map 不生效";
  }
  if (fieldId === "videoMap" && encoder === "none") {
    return "已丢弃视频，map 不生效";
  }
  if (fieldId === "copyAttachments" && RESTRICTIVE_ATTACHMENT_CONTAINERS.has(container)) {
    return "此容器不支持附件流";
  }
  if (String(preset.audioMode ?? "copy") !== "encode" && AUDIO_ENCODE_FIELDS.has(fieldId)) {
    return "音频未重新编码，此项不生效";
  }
  if (["loudnormI", "loudnormTP", "loudnormLRA"].includes(fieldId) && preset.loudnormEnabled !== true) {
    return "响度标准化未开启";
  }
  if (String(preset.subtitleMode ?? "copy") !== "burn" && SUBTITLE_BURN_FIELDS.has(fieldId)) {
    return "字幕未烧录，此项不生效";
  }
  if (fieldId === "twoPass"
      && !((encoder === "libx264" || encoder === "libx265") && String(preset.qualityMode ?? "") === "bitrate")) {
    return "仅 x264/x265 + 固定码率支持两遍编码";
  }
  return "";
}
