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

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jackhuang.hmcl.web.config.ServerConfig;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

/// The lazily probed ffmpeg installation and the two operations the panel needs from it.
///
/// Probing is cached for the lifetime of the module: `-version`/`-encoders`/`-hwaccels`
/// are cheap but not free, and the answer cannot change unless the operator installs a
/// different binary. [refresh] is what the settings page calls after a download.
@NotNullByDefault
public final class TranscodeRuntime {

    private final ServerConfig config;
    private volatile @Nullable FfmpegProbe.FfmpegInfo info;

    public TranscodeRuntime(ServerConfig config) {
        this.config = config;
    }

    /// The cached probe, or a fresh one on first use.
    public FfmpegProbe.FfmpegInfo info() {
        FfmpegProbe.FfmpegInfo current = info;
        if (current != null) {
            return current;
        }
        synchronized (this) {
            current = info;
            if (current == null) {
                current = FfmpegProbe.probe(FfmpegLocator.locate(config));
                info = current;
            }
            return current;
        }
    }

    /// Re-probes after the operator installed or pointed at a different binary.
    public synchronized FfmpegProbe.FfmpegInfo refresh() {
        FfmpegProbe.FfmpegInfo next = FfmpegProbe.probe(FfmpegLocator.locate(config));
        info = next;
        return next;
    }

    /// Runs `ffprobe -print_format json` on a file and returns the parsed object.
    public JsonObject probeMedia(Path file) throws IOException, InterruptedException {
        FfmpegProbe.FfmpegInfo current = info();
        if (!current.available() || current.ffprobe() == null) {
            throw new IOException(current.error() == null ? "ffprobe is unavailable" : current.error());
        }
        String output = FfmpegProbe.run(current.ffprobe(), List.of(
                "-v", "error",
                "-print_format", "json",
                "-show_format",
                "-show_streams",
                file.toString()));
        if (output.isBlank()) {
            throw new IOException("ffprobe returned nothing");
        }
        return JsonParser.parseString(output).getAsJsonObject();
    }

    /// A one-line description for the overview page.
    public String summary() {
        FfmpegProbe.FfmpegInfo current = info();
        return current.available() ? current.version() : "不可用";
    }

    /// UTF-8 helper for tests and callers that want the raw text.
    public static String text(byte[] bytes) {
        return new String(bytes, StandardCharsets.UTF_8);
    }
}