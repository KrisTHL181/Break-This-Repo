/*
 * HDSL-web
 * Copyright (C) 2026  HDSL-web contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package org.jackhuang.hmcl.web.transcode.quality;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class QualityParseTest {

    @Test
    void parsesPsnrStats(@TempDir Path dir) throws Exception {
        Path stats = dir.resolve("psnr.log");
        Files.writeString(stats, """
                n:1 mse_avg:31.30 psnr_avg:33.18 psnr_y:32.99 psnr_u:33.29 psnr_v:33.86
                n:2 mse_avg:31.03 psnr_avg:33.21 psnr_y:32.97 psnr_u:33.52 psnr_v:33.97
                """);
        var result = QualityService.parseFrames("psnr", stats);
        assertEquals(2, result.frames().size());
        assertEquals(33.195, result.average(), 0.01);
    }

    @Test
    void parsesSsimStats(@TempDir Path dir) throws Exception {
        Path stats = dir.resolve("ssim.log");
        Files.writeString(stats, """
                n:1 Y:0.984209 U:0.985331 V:0.997137 All:0.986551 (18.713010)
                n:2 Y:0.983928 U:0.985491 V:0.997145 All:0.986391 (18.661778)
                """);
        var result = QualityService.parseFrames("ssim", stats);
        assertEquals(0.986471, result.average(), 0.0001);
    }

    @Test
    void parsesXpsnrStats(@TempDir Path dir) throws Exception {
        Path stats = dir.resolve("xpsnr.log");
        Files.writeString(stats, """
                n:    1  XPSNR y: 33.6187  XPSNR u: 33.1776  XPSNR v: 33.7859
                n:    2  XPSNR y: 28.6322  XPSNR u: 27.6264  XPSNR v: 28.2190
                """);
        var result = QualityService.parseFrames("xpsnr", stats);
        assertEquals(2, result.frames().size());
        assertEquals(30.8433, result.average(), 0.001);
    }

    @Test
    void parsesVmafJson(@TempDir Path dir) throws Exception {
        Path stats = dir.resolve("vmaf.json");
        Files.writeString(stats, """
                {"frames":[{"frameNum":1,"vmaf":90.0},{"frameNum":2,"vmaf":92.0}]}
                """);
        var result = QualityService.parseVmaf(stats);
        assertEquals(2, result.frames().size());
        assertEquals(91.0, result.average(), 0.001);
    }
}