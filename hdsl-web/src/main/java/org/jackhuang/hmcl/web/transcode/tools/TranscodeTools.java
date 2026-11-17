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
package org.jackhuang.hmcl.web.transcode.tools;

import com.google.gson.JsonObject;
import org.jackhuang.hmcl.web.transcode.TranscodeModule;
import org.jetbrains.annotations.NotNullByDefault;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/// Queue-backed media tools. The command is built here; the queue owns the process,
/// log, progress and cancellation, so the Tools page and the Queue page tell the same
/// story about a running merge or extract.
@NotNullByDefault
public final class TranscodeTools {

    private final TranscodeModule module;

    public TranscodeTools(TranscodeModule module) {
        this.module = module;
    }

    /// `concat` demuxer with `-c copy`: the fast path when every input has the same
    /// codecs. A mismatch fails with ffmpeg's reason in the job log rather than being
    /// silently re-encoded.
    public JsonObject merge(List<String> inputs, String output) throws IOException {
        if (inputs == null || inputs.size() < 2) {
            throw new IOException("merge needs at least two inputs");
        }
        if (output == null || output.isBlank()) {
            throw new IOException("output is required");
        }
        List<Path> resolved = new ArrayList<>();
        for (String input : inputs) {
            resolved.add(module.media().resolve(module.mediaRoot(), input));
        }
        Path listFile = module.dataDir().resolve("tools").resolve("concat")
                .resolve(UUID.randomUUID().toString().replace("-", "") + ".txt");
        Files.createDirectories(listFile.getParent());
        StringBuilder list = new StringBuilder();
        for (Path file : resolved) {
            list.append("file '").append(file.toString().replace("'", "'\\''")).append("'\n");
        }
        Files.writeString(listFile, list.toString(), StandardCharsets.UTF_8);

        List<String> args = new ArrayList<>(List.of(
                "-hide_banner", "-nostdin", "-y", "-nostats", "-progress", "pipe:1",
                "-f", "concat", "-safe", "0", "-i", listFile.toString(), "-c", "copy"));
        args.add(output);
        return module.queue().submitCommand("合并 " + inputs.size() + " 个文件", output, args, "merge");
    }

    /// Copies one stream out. The output extension decides the container; `-c copy`
    /// means a video extract is usually `.mkv`, audio `.mka`/`.m4a`, subtitles `.mkv`.
    public JsonObject extract(String input, String type, int index, String output) throws IOException {
        if (input == null || input.isBlank()) {
            throw new IOException("input is required");
        }
        if (output == null || output.isBlank()) {
            throw new IOException("output is required");
        }
        String selector = switch (type == null ? "" : type) {
            case "video" -> "0:v:" + Math.max(0, index);
            case "audio" -> "0:a:" + Math.max(0, index);
            case "subtitle" -> "0:s:" + Math.max(0, index);
            default -> throw new IOException("type must be video, audio or subtitle");
        };
        Path source = module.media().resolve(module.mediaRoot(), input);
        List<String> args = new ArrayList<>(List.of(
                "-hide_banner", "-nostdin", "-y", "-nostats", "-progress", "pipe:1",
                "-i", source.toString(), "-map", selector, "-c", "copy"));
        args.add(output);
        return module.queue().submitCommand("提取 " + selector + " · " + input, output, args, "extract");
    }

    /// Muxes tracks from several files with explicit `-map` selectors.
    public JsonObject mux(List<String> inputs, List<String> maps, String output) throws IOException {
        if (inputs == null || inputs.isEmpty()) {
            throw new IOException("mux needs at least one input");
        }
        if (maps == null || maps.isEmpty()) {
            throw new IOException("mux needs at least one stream map");
        }
        if (output == null || output.isBlank()) {
            throw new IOException("output is required");
        }
        List<String> args = new ArrayList<>(List.of(
                "-hide_banner", "-nostdin", "-y", "-nostats", "-progress", "pipe:1"));
        for (String input : inputs) {
            Path resolved = module.media().resolve(module.mediaRoot(), input);
            args.add("-i");
            args.add(resolved.toString());
        }
        for (String map : maps) {
            args.add("-map");
            args.add(map);
        }
        args.add("-c");
        args.add("copy");
        args.add(output);
        return module.queue().submitCommand("混流 " + inputs.size() + " 个输入", output, args, "mux");
    }
}