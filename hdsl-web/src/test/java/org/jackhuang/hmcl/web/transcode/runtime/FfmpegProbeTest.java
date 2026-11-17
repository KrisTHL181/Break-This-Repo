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
package org.jackhuang.hmcl.web.transcode.runtime;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FfmpegProbeTest {

    @Test
    void parsesTheVersionLine() {
        assertEquals("ffmpeg version 7.1.5 Copyright (c) 2000-2025 the FFmpeg developers",
                FfmpegProbe.firstLine("""

                        ffmpeg version 7.1.5 Copyright (c) 2000-2025 the FFmpeg developers
                        built with gcc
                        """));
    }

    @Test
    void parsesEncodersFromTheTable() {
        var encoders = FfmpegProbe.parseEncoders("""
                Encoders:
                 V..... = Video
                 A..... = Audio
                 ------
                 V....D libx264              H.264 / AVC / MPEG-4 AVC / MPEG-4 part 10
                 V....D libx265              H.265 / HEVC
                 V....D hevc_qsv             HEVC (Intel Quick Sync Video acceleration)
                 A....D aac                  AAC (Advanced Audio Coding)
                """);
        assertTrue(encoders.contains("libx264"));
        assertTrue(encoders.contains("libx265"));
        assertTrue(encoders.contains("hevc_qsv"));
        assertTrue(encoders.contains("aac"));
        assertFalse(encoders.contains("Encoders:"));
    }

    @Test
    void parsesHardwareAccelerationMethods() {
        var methods = FfmpegProbe.parseHwaccels("""
                Hardware acceleration methods:
                vaapi
                cuda
                qsv
                """);
        assertEquals(3, methods.size());
        assertTrue(methods.contains("vaapi"));
        assertTrue(methods.contains("qsv"));
    }

    @Test
    void parsesFiltersFromTheTable() {
        var filters = FfmpegProbe.parseFilters("""
                Filters:
                 T.. = Timeline support
                 TSC scale             V->V       Scale the input video size.
                 ... hwupload          V->V       Upload a normal frame to a hardware frame.
                """);
        assertTrue(filters.contains("scale"));
        assertTrue(filters.contains("hwupload"));
    }

    @Test
    void anUnavailableProbeIsExplicit() {
        FfmpegProbe.FfmpegInfo info = FfmpegProbe.FfmpegInfo.unavailable("missing");
        assertFalse(info.available());
        assertEquals("missing", info.error());
        assertTrue(info.encoders().isEmpty());
    }
}