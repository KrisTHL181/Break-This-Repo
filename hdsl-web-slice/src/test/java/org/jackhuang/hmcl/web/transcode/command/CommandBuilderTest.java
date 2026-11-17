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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommandBuilderTest {

    private static TranscodePreset preset() {
        TranscodePreset preset = new TranscodePreset();
        preset.name = "test";
        preset.container = "mkv";
        preset.videoEncoder = "libx264";
        preset.qualityMode = "crf";
        preset.qualityValue = "20";
        preset.preset = "medium";
        preset.audioMode = "copy";
        preset.subtitleMode = "copy";
        return preset;
    }

    private static String joined(List<String> args) {
        return String.join(" ", args);
    }

    @Test
    void buildsAnH264JobWithStreamMapping(@TempDir Path dir) {
        var args = CommandBuilder.build(preset(), dir.resolve("in.mkv"), dir.resolve("out.mkv"));
        String command = joined(args);
        assertTrue(command.contains("-c:v libx264"));
        assertTrue(command.contains("-crf 20"));
        assertTrue(command.contains("-preset medium"));
        assertTrue(command.contains("-map 0:v:0?"));
        assertTrue(command.contains("-map 0:a?"));
        assertTrue(command.contains("-c:a copy"));
        assertTrue(command.contains("-c:s copy"));
        assertTrue(command.contains("-map_metadata 0"));
        assertTrue(args.get(args.size() - 1).endsWith("out.mkv"));
    }

    @Test
    void copyIsARemuxNotAnEncoder(@TempDir Path dir) {
        TranscodePreset preset = preset();
        preset.videoEncoder = "copy";
        var args = CommandBuilder.build(preset, dir.resolve("in.mkv"), dir.resolve("out.mkv"));
        String command = joined(args);
        assertTrue(command.contains("-c:v copy"));
        assertTrue(command.contains("-c:a copy"));
        assertTrue(command.contains("-c:s copy"));
        assertTrue(!command.contains("-crf"));
    }

    @Test
    void audioOnlyDropsVideo(@TempDir Path dir) {
        TranscodePreset preset = preset();
        preset.container = "m4a";
        preset.videoEncoder = "none";
        preset.audioMode = "encode";
        preset.audioEncoder = "aac";
        preset.audioBitrate = "192k";
        preset.subtitleMode = "none";
        var args = CommandBuilder.build(preset, dir.resolve("in.mkv"), dir.resolve("out.m4a"));
        String command = joined(args);
        assertTrue(command.contains("-vn"));
        assertTrue(command.contains("-c:a aac"));
        assertTrue(command.contains("-b:a 192k"));
        assertTrue(command.contains("-sn"));
    }

    @Test
    void aPathWithSpacesStaysOneArgument(@TempDir Path dir) {
        Path input = dir.resolve("my movie.mkv");
        var args = CommandBuilder.build(preset(), input, dir.resolve("out.mkv"));
        assertTrue(args.contains(input.toString()));
        assertTrue(CommandBuilder.display(args).contains("my movie.mkv"));
        assertTrue(CommandBuilder.display(args).contains("\""));
    }

    @Test
    void outputNamesAvoidCollisions(@TempDir Path dir) throws Exception {
        TranscodePreset preset = preset();
        Path input = dir.resolve("clip.mp4");
        Files.writeString(input, "x");
        Path first = CommandBuilder.outputPath(preset, input, dir);
        assertEquals("clip_transcoded.mkv", first.getFileName().toString());
        Files.writeString(first, "x");
        Path second = CommandBuilder.outputPath(preset, input, dir);
        assertEquals("clip_transcoded_1.mkv", second.getFileName().toString());
    }

    @Test
    void burningSubtitlesRefusesACopyVideo(@TempDir Path dir) {
        TranscodePreset preset = preset();
        preset.videoEncoder = "copy";
        preset.subtitleMode = "burn";
        preset.subtitleFile = "subs.srt";
        assertThrows(IllegalArgumentException.class,
                () -> CommandBuilder.build(preset, dir.resolve("in.mkv"), dir.resolve("out.mkv")));
    }
    @Test
    void writesMetadataAttachmentsAndChapters(@TempDir Path dir) {
        TranscodePreset preset = preset();
        preset.metadataTitle = "My film";
        preset.metadataArtist = "Someone";
        preset.copyAttachments = true;
        Path chapters = dir.resolve("chapters.txt");
        var args = CommandBuilder.build(preset, dir.resolve("in.mkv"), dir.resolve("out.mkv"), chapters);
        String command = joined(args);
        assertTrue(command.contains("-metadata title=My film"));
        assertTrue(command.contains("-metadata artist=Someone"));
        assertTrue(command.contains("-map 0:t?"));
        assertTrue(command.contains("-c:t copy"));
        assertTrue(command.contains(chapters.toString()));
        assertTrue(command.contains("-map_chapters 1"));
    }

    @Test
    void qsvGetsLowPowerAndImageEncodersDropAudioSubtitles(@TempDir Path dir) {
        TranscodePreset qsv = preset();
        qsv.videoEncoder = "hevc_qsv";
        qsv.qualityMode = "cqp";
        qsv.qualityValue = "24";
        var qsvArgs = CommandBuilder.build(qsv, dir.resolve("in.mkv"), dir.resolve("out.mkv"));
        assertTrue(joined(qsvArgs).contains("-low_power 1"));

        TranscodePreset image = preset();
        image.videoEncoder = "mjpeg";
        image.container = "jpg";
        image.audioMode = "copy";
        image.subtitleMode = "copy";
        image.copyAttachments = true;
        var imageArgs = CommandBuilder.build(image, dir.resolve("in.mkv"), dir.resolve("out.jpg"));
        String command = joined(imageArgs);
        assertTrue(command.contains("-an"));
        assertTrue(command.contains("-sn"));
        assertTrue(command.contains("-frames:v 1"));
        assertTrue(!command.contains("-map 0:a?"));
        assertTrue(!command.contains("-map 0:s?"));
        assertTrue(!command.contains("-map 0:t?"));
    }

    @Test
    void twoPassBuildsAnalysisAndEncodePasses(@TempDir Path dir) {
        TranscodePreset preset = preset();
        preset.qualityMode = "bitrate";
        preset.qualityValue = "2500k";
        preset.twoPass = true;
        Path output = dir.resolve("out.mkv");
        List<List<String>> passes = CommandBuilder.buildPasses(preset, dir.resolve("in.mkv"), output);
        assertEquals(2, passes.size());
        String first = joined(passes.get(0));
        String second = joined(passes.get(1));
        assertTrue(first.contains("-pass 1"));
        assertTrue(first.contains("-passlogfile " + output + ".pass"));
        assertTrue(first.contains("-f null -"));
        assertTrue(!first.contains("-movflags"));
        assertTrue(!first.endsWith(output.toString()));
        assertTrue(second.contains("-pass 2"));
        assertTrue(second.endsWith(output.toString()));
        assertTrue(!second.contains("-f null"));
    }

    @Test
    void twoPassRejectsUnsupportedEncoderOrQualityMode(@TempDir Path dir) {
        TranscodePreset crf = preset();
        crf.twoPass = true;
        assertThrows(IllegalArgumentException.class,
                () -> CommandBuilder.buildPasses(crf, dir.resolve("in.mkv"), dir.resolve("out.mkv")));

        TranscodePreset hardware = preset();
        hardware.twoPass = true;
        hardware.qualityMode = "bitrate";
        hardware.qualityValue = "2500k";
        hardware.videoEncoder = "hevc_nvenc";
        assertThrows(IllegalArgumentException.class,
                () -> CommandBuilder.buildPasses(hardware, dir.resolve("in.mkv"), dir.resolve("out.mkv")));
    }

    @Test
    void newAv1AndVp9EncodersGetTheirOwnFlags(@TempDir Path dir) {
        TranscodePreset svt = preset();
        svt.videoEncoder = "libsvtav1";
        svt.qualityValue = "35";
        svt.preset = "8";
        String svtCommand = joined(CommandBuilder.build(svt, dir.resolve("in.mkv"), dir.resolve("out.mkv")));
        assertTrue(svtCommand.contains("-c:v libsvtav1"));
        assertTrue(svtCommand.contains("-preset 8"));
        assertTrue(svtCommand.contains("-crf 35"));

        TranscodePreset rav1e = preset();
        rav1e.videoEncoder = "librav1e";
        rav1e.qualityValue = "100";
        rav1e.preset = "6";
        String rav1eCommand = joined(CommandBuilder.build(rav1e, dir.resolve("in.mkv"), dir.resolve("out.mkv")));
        assertTrue(rav1eCommand.contains("-speed 6"));
        assertTrue(rav1eCommand.contains("-qp 100"));
        assertTrue(!rav1eCommand.contains("-crf"));

        TranscodePreset vp9Vaapi = preset();
        vp9Vaapi.videoEncoder = "vp9_vaapi";
        vp9Vaapi.qualityMode = "cqp";
        vp9Vaapi.qualityValue = "30";
        vp9Vaapi.preset = "4";
        String vaapiCommand = joined(CommandBuilder.build(vp9Vaapi, dir.resolve("in.mkv"), dir.resolve("out.mkv")));
        assertTrue(vaapiCommand.contains("-vaapi_device"));
        assertTrue(vaapiCommand.contains("-qp 30"));
        assertTrue(vaapiCommand.contains("hwupload"));
        assertTrue(!vaapiCommand.contains("-quality"));

        TranscodePreset av1Qsv = preset();
        av1Qsv.videoEncoder = "av1_qsv";
        av1Qsv.qualityMode = "cqp";
        av1Qsv.qualityValue = "30";
        av1Qsv.preset = "medium";
        String qsvCommand = joined(CommandBuilder.build(av1Qsv, dir.resolve("in.mkv"), dir.resolve("out.mkv")));
        assertTrue(qsvCommand.contains("-preset medium"));
        assertTrue(qsvCommand.contains("-global_quality 30"));

        TranscodePreset av1Nvenc = preset();
        av1Nvenc.videoEncoder = "av1_nvenc";
        av1Nvenc.qualityMode = "cqp";
        av1Nvenc.qualityValue = "28";
        av1Nvenc.preset = "p4";
        String nvencCommand = joined(CommandBuilder.build(av1Nvenc, dir.resolve("in.mkv"), dir.resolve("out.mkv")));
        assertTrue(nvencCommand.contains("-preset p4"));
        assertTrue(nvencCommand.contains("-qp 28"));

        TranscodePreset aom = preset();
        aom.videoEncoder = "libaom-av1";
        aom.preset = "8";
        String aomCommand = joined(CommandBuilder.build(aom, dir.resolve("in.mkv"), dir.resolve("out.mkv")));
        assertTrue(aomCommand.contains("-map 0:a?"));
        assertTrue(!aomCommand.contains("-frames:v"));
    }

    @Test
    void windowsEncodersUseTheirOwnQualityAndUsageFlags(@TempDir Path dir) {
        TranscodePreset amf = preset();
        amf.videoEncoder = "h264_amf";
        amf.qualityMode = "cqp";
        amf.qualityValue = "24";
        amf.preset = "quality";
        amf.profile = "main";
        amf.tune = "transcoding";
        amf.pixelFormat = "nv12";
        String amfCommand = joined(CommandBuilder.build(amf, dir.resolve("in.mkv"), dir.resolve("out.mkv")));
        assertTrue(amfCommand.contains("-c:v h264_amf"));
        assertTrue(amfCommand.contains("-quality quality"));
        assertTrue(amfCommand.contains("-profile:v main"));
        assertTrue(amfCommand.contains("-usage transcoding"));
        assertTrue(amfCommand.contains("-rc cqp"));
        assertTrue(amfCommand.contains("-qp 24"));
        assertTrue(amfCommand.contains("-pix_fmt nv12"));
        assertTrue(!amfCommand.contains("-preset quality"));
        assertTrue(!amfCommand.contains("-tune"));

        TranscodePreset amfBitrate = preset();
        amfBitrate.videoEncoder = "hevc_amf";
        amfBitrate.qualityMode = "bitrate";
        amfBitrate.qualityValue = "2500k";
        String amfBitrateCommand = joined(CommandBuilder.build(amfBitrate, dir.resolve("in.mkv"), dir.resolve("out.mkv")));
        assertTrue(amfBitrateCommand.contains("-rc cbr"));
        assertTrue(amfBitrateCommand.contains("-b:v 2500k"));

        TranscodePreset d3d12 = preset();
        d3d12.videoEncoder = "av1_d3d12va";
        d3d12.qualityMode = "cqp";
        d3d12.qualityValue = "28";
        d3d12.preset = "whatever";
        d3d12.tune = "hq";
        d3d12.pixelFormat = "d3d12";
        String d3d12Command = joined(CommandBuilder.build(d3d12, dir.resolve("in.mkv"), dir.resolve("out.mkv")));
        assertTrue(d3d12Command.contains("-rc_mode CQP"));
        assertTrue(d3d12Command.contains("-qp 28"));
        assertTrue(d3d12Command.contains("-pix_fmt d3d12"));
        assertTrue(!d3d12Command.contains("-preset"));
        assertTrue(!d3d12Command.contains("-tune"));
        assertTrue(!d3d12Command.contains("-usage"));

        TranscodePreset vulkan = preset();
        vulkan.videoEncoder = "hevc_vulkan";
        vulkan.qualityMode = "cqp";
        vulkan.qualityValue = "30";
        vulkan.preset = "5";
        vulkan.tune = "hq";
        vulkan.pixelFormat = "vulkan";
        String vulkanCommand = joined(CommandBuilder.build(vulkan, dir.resolve("in.mkv"), dir.resolve("out.mkv")));
        assertTrue(vulkanCommand.contains("-quality 5"));
        assertTrue(vulkanCommand.contains("-tune hq"));
        assertTrue(vulkanCommand.contains("-qp 30"));
        assertTrue(vulkanCommand.contains("-pix_fmt vulkan"));
    }

    @Test
    void filterOrderPutsCropAndDeinterlaceBeforeScale(@TempDir Path dir) {
        TranscodePreset preset = preset();
        preset.crop = "640:360:0:0";
        preset.deinterlace = "yadif";
        preset.width = "1280";
        String command = joined(CommandBuilder.build(preset, dir.resolve("in.mkv"), dir.resolve("out.mkv")));
        int crop = command.indexOf("crop=640:360:0:0");
        int deinterlace = command.indexOf("yadif");
        int scale = command.indexOf("scale=1280:-2");
        assertTrue(crop >= 0 && deinterlace >= 0 && scale >= 0);
        assertTrue(crop < scale);
        assertTrue(deinterlace < scale);
    }

    @Test
    void vbrAndCbrEmitBitrateBoundsAndNoneEmitsNoQualityFlag(@TempDir Path dir) {
        TranscodePreset vbr = preset();
        vbr.qualityMode = "vbr";
        vbr.qualityValue = "2500k";
        vbr.bitrateMax = "3500k";
        vbr.bitrateBuf = "7000k";
        String vbrCommand = joined(CommandBuilder.build(vbr, dir.resolve("in.mkv"), dir.resolve("out.mkv")));
        assertTrue(vbrCommand.contains("-b:v 2500k"));
        assertTrue(vbrCommand.contains("-maxrate 3500k"));
        assertTrue(vbrCommand.contains("-bufsize 7000k"));
        assertTrue(!vbrCommand.contains("-crf"));

        TranscodePreset cbr = preset();
        cbr.videoEncoder = "libx264";
        cbr.qualityMode = "cbr";
        cbr.qualityValue = "3000k";
        cbr.bitrateMin = "3000k";
        cbr.bitrateMax = "3000k";
        String cbrCommand = joined(CommandBuilder.build(cbr, dir.resolve("in.mkv"), dir.resolve("out.mkv")));
        assertTrue(cbrCommand.contains("-minrate 3000k"));
        assertTrue(cbrCommand.contains("-maxrate 3000k"));

        TranscodePreset none = preset();
        none.qualityMode = "none";
        String noneCommand = joined(CommandBuilder.build(none, dir.resolve("in.mkv"), dir.resolve("out.mkv")));
        assertTrue(!noneCommand.contains("-crf"));
        assertTrue(!noneCommand.contains("-b:v"));
        assertTrue(!noneCommand.contains("-qp"));
    }

    @Test
    void subtitleConversionAndKeepOthersUsePerStreamCodecs(@TempDir Path dir) {
        TranscodePreset convert = preset();
        convert.container = "mp4";
        convert.subtitleMode = "convert";
        convert.subtitleCodec = "mov_text";
        String convertCommand = joined(CommandBuilder.build(convert, dir.resolve("in.mkv"), dir.resolve("out.mp4")));
        assertTrue(convertCommand.contains("-map 0:s?"));
        assertTrue(convertCommand.contains("-c:s mov_text"));
        assertTrue(!convertCommand.contains("-c:s copy"));

        TranscodePreset keep = preset();
        keep.subtitleMode = "convert";
        keep.subtitleCodec = "ass";
        keep.subtitleKeepOthers = true;
        keep.subtitleStreamIndex = 2;
        String keepCommand = joined(CommandBuilder.build(keep, dir.resolve("in.mkv"), dir.resolve("out.mkv")));
        assertTrue(keepCommand.contains("-c:s copy"));
        assertTrue(keepCommand.contains("-c:s:2 ass"));
    }

    @Test
    void streamControlKeepsOtherTracksAndEncodesOnlyTheSelectedOnes(@TempDir Path dir) {
        TranscodePreset preset = preset();
        preset.videoKeepOthers = true;
        preset.videoStreamIndex = 1;
        preset.width = "1280";
        preset.audioMode = "encode";
        preset.audioEncoder = "aac";
        preset.audioBitrate = "192k";
        preset.audioKeepOthers = true;
        preset.audioStreamIndex = 0;
        preset.audioVolume = "-3dB";
        preset.subtitleMode = "convert";
        preset.subtitleCodec = "ass";
        preset.subtitleKeepOthers = true;
        preset.subtitleStreamIndex = 2;
        String command = joined(CommandBuilder.build(preset, dir.resolve("in.mkv"), dir.resolve("out.mkv")));
        assertTrue(command.contains("-map 0:v?"));
        assertTrue(command.contains("-map 0:a?"));
        assertTrue(command.contains("-map 0:s?"));
        assertTrue(command.contains("-c:v copy"));
        assertTrue(command.contains("-c:v:1 libx264"));
        assertTrue(command.contains("-filter:v:1 scale=1280:-2"));
        assertTrue(command.contains("-c:a copy"));
        assertTrue(command.contains("-c:a:0 aac"));
        assertTrue(command.contains("-b:a 192k"));
        assertTrue(command.contains("-filter:a:0 volume=-3dB"));
        assertTrue(command.contains("-c:s copy"));
        assertTrue(command.contains("-c:s:2 ass"));
    }

    @Test
    void validationRejectsIncompatibleCombinations(@TempDir Path dir) {
        TranscodePreset hardwareCrf = preset();
        hardwareCrf.videoEncoder = "hevc_nvenc";
        hardwareCrf.qualityMode = "crf";
        assertThrows(IllegalArgumentException.class,
                () -> CommandBuilder.build(hardwareCrf, dir.resolve("in.mkv"), dir.resolve("out.mkv")));

        TranscodePreset webmH264 = preset();
        webmH264.container = "webm";
        assertThrows(IllegalArgumentException.class,
                () -> CommandBuilder.build(webmH264, dir.resolve("in.mkv"), dir.resolve("out.webm")));

        TranscodePreset m4aVideo = preset();
        m4aVideo.container = "m4a";
        assertThrows(IllegalArgumentException.class,
                () -> CommandBuilder.build(m4aVideo, dir.resolve("in.mkv"), dir.resolve("out.m4a")));

        TranscodePreset badSubtitle = preset();
        badSubtitle.container = "mp4";
        badSubtitle.subtitleMode = "convert";
        badSubtitle.subtitleCodec = "ass";
        assertThrows(IllegalArgumentException.class,
                () -> CommandBuilder.build(badSubtitle, dir.resolve("in.mkv"), dir.resolve("out.mp4")));

        TranscodePreset copyKeep = preset();
        copyKeep.videoEncoder = "copy";
        copyKeep.videoKeepOthers = true;
        assertThrows(IllegalArgumentException.class,
                () -> CommandBuilder.build(copyKeep, dir.resolve("in.mkv"), dir.resolve("out.mkv")));

        TranscodePreset badBitrate = preset();
        badBitrate.qualityMode = "vbr";
        badBitrate.qualityValue = "not-a-bitrate";
        assertThrows(IllegalArgumentException.class,
                () -> CommandBuilder.build(badBitrate, dir.resolve("in.mkv"), dir.resolve("out.mkv")));
    }

}