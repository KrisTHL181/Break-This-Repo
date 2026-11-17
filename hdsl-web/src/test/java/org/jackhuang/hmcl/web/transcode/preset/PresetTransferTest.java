/*
 * HDSL-web
 * Copyright (C) 2026  HDSL-web contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package org.jackhuang.hmcl.web.transcode.preset;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PresetTransferTest {

    @Test
    void importsTheCommonThreeFuiFields() {
        JsonObject source = com.google.gson.JsonParser.parseString("""
                {
                  "预设文件版本": 6,
                  "预设备注": "来自 Windows",
                  "输出容器": "mp4",
                  "视频参数_编码器_具体编码": "libx265",
                  "视频参数_编码器_编码预设": "slow",
                  "音频参数_编码器_代号": "aac",
                  "音频参数_比特率": "256k",
                  "流控制_章节选项": "清除章节",
                  "流控制_附件选项": "清除附件",
                  "元数据_要写入的信息": [{"字段": "title", "值": "Demo"}]
                }
                """).getAsJsonObject();
        TranscodePreset preset = PresetTransfer.from3fui(source);
        assertEquals("mp4", preset.container);
        assertEquals("libx265", preset.videoEncoder);
        assertEquals("slow", preset.preset);
        assertEquals("aac", preset.audioEncoder);
        assertEquals("256k", preset.audioBitrate);
        assertEquals("encode", preset.audioMode);
        assertEquals(false, preset.copyChapters);
        assertEquals(false, preset.copyAttachments);
        assertEquals("Demo", preset.metadataTitle);
    }

    @Test
    void exportsAThreeFuiShapedObject() {
        TranscodePreset preset = new TranscodePreset();
        preset.name = "mine";
        preset.container = "mkv";
        preset.videoEncoder = "libx264";
        preset.metadataTitle = "Title";
        JsonObject object = PresetTransfer.to3fui(preset);
        assertEquals(6, object.get("预设文件版本").getAsInt());
        assertEquals("mkv", object.get("输出容器").getAsString());
        assertEquals("libx264", object.get("视频参数_编码器_具体编码").getAsString());
        assertTrue(object.get("元数据_要写入的信息").isJsonArray());
    }
}
