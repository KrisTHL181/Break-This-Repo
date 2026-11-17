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

import org.jackhuang.hmcl.util.platform.Privileges;
import org.jackhuang.hmcl.web.transcode.TranscodeModule;
import org.jetbrains.annotations.NotNullByDefault;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/// One-frame capability probe: does this encoder actually produce a frame on this host?
///
/// `ffmpeg -encoders` lists what the build knows, not what the GPU/driver can do. The
/// known Intel failure (QSV returns an empty bitstream on some iGPUs) is exactly a case
/// where the name is present and the encode still produces nothing, so the panel offers
/// this 1-frame test rather than pretending the matrix is truth.
@NotNullByDefault
public final class HardwareProbe {

    private HardwareProbe() {
    }

    public record Result(boolean ok, String encoder, int exitCode, String output) {
    }

    public static Result probe(TranscodeModule module, String encoder) {
        FfmpegProbe.FfmpegInfo info = module.runtime().info();
        if (!info.available() || info.ffmpeg() == null) {
            return new Result(false, encoder, -1, info.error() == null ? "ffmpeg unavailable" : info.error());
        }
        List<String> args = new ArrayList<>();
        args.add("-hide_banner");
        args.add("-loglevel");
        args.add("error");
        if (encoder.endsWith("_vaapi")) {
            args.add("-vaapi_device");
            args.add("/dev/dri/renderD128");
        } else if (encoder.endsWith("_qsv")) {
            args.add("-init_hw_device");
            args.add("qsv=hw");
            args.add("-filter_hw_device");
            args.add("hw");
        }
        args.add("-f");
        args.add("lavfi");
        args.add("-i");
        args.add("testsrc=size=128x72:rate=1");
        args.add("-frames:v");
        args.add("1");
        args.add("-c:v");
        args.add(encoder);
        if (encoder.endsWith("_qsv")) {
            args.add("-low_power");
            args.add("1");
        }
        if (encoder.endsWith("_vaapi") || encoder.endsWith("_qsv")) {
            args.add("-vf");
            args.add("format=nv12,hwupload");
        }
        args.add("-f");
        args.add("null");
        args.add("-");

        List<String> command = new ArrayList<>(args.size() + 1);
        command.add(info.ffmpeg().toString());
        command.addAll(args);
        try {
            Process process = new ProcessBuilder(Privileges.asAgent(command))
                    .redirectErrorStream(true)
                    .start();
            String output;
            try (InputStream in = process.getInputStream()) {
                output = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
            if (!process.waitFor(20, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                return new Result(false, encoder, -1, "probe timed out after 20s\n" + tail(output));
            }
            return new Result(process.exitValue() == 0, encoder, process.exitValue(), tail(output));
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return new Result(false, encoder, -1, e.getMessage() == null ? "probe failed" : e.getMessage());
        }
    }

    private static String tail(String output) {
        String[] lines = output.split("\\R");
        int from = Math.max(0, lines.length - 20);
        return String.join("\n", java.util.Arrays.copyOfRange(lines, from, lines.length));
    }
}