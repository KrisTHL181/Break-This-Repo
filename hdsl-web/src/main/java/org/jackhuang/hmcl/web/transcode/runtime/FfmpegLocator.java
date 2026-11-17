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

import org.jackhuang.hmcl.web.config.ServerConfig;
import org.jackhuang.hmcl.web.transcode.TranscodePaths;
import org.jackhuang.hmcl.util.platform.Executables;
import org.jackhuang.hmcl.util.platform.OperatingSystem;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/// Finds the `ffmpeg` executable the transcode module should use.
///
/// Order: explicit `transcode.ffmpeg_path` / `HDSL_FFMPEG_PATH`, then the panel-managed
/// copy under `<data>/transcode/tools/ffmpeg/current/bin`, then `PATH`. A sibling
/// `ffprobe` is required for progress duration and output validation; a build without
/// one is reported as unavailable rather than half-working.
@NotNullByDefault
public final class FfmpegLocator {

    private FfmpegLocator() {
    }

    /// The pair of programs, or `null` when either is missing.
    public static @Nullable FfmpegPaths locate(ServerConfig config) {
        Path ffmpeg = locate(config, "ffmpeg");
        if (ffmpeg == null) {
            return null;
        }
        Path ffprobe = sibling(ffmpeg, "ffprobe");
        if (ffprobe == null) {
            return null;
        }
        return new FfmpegPaths(ffmpeg, ffprobe);
    }

    private static @Nullable Path locate(ServerConfig config, String name) {
        String configured = config.transcode.ffmpegPath;
        if (configured != null && !configured.isBlank()) {
            String trimmed = configured.trim();
            Path path;
            try {
                path = Path.of(trimmed);
            } catch (RuntimeException e) {
                return null;
            }
            if (Files.isDirectory(path)) {
                path = path.resolve(executableName(name));
            }
            if (Files.isRegularFile(path)) {
                return path.toAbsolutePath().normalize();
            }
            // A bare name is a PATH lookup, not a relative path.
            if (!trimmed.contains("/") && !trimmed.contains("\\")) {
                return Executables.resolve(name).orElse(null);
            }
            return null;
        }

        Path managed = TranscodePaths.dataDir(config)
                .resolve("tools").resolve("ffmpeg").resolve("current").resolve("bin")
                .resolve(executableName(name));
        if (Files.isRegularFile(managed)) {
            return managed.toAbsolutePath().normalize();
        }
        return Executables.resolve(name).orElse(null);
    }

    private static @Nullable Path sibling(Path ffmpeg, String name) {
        Path parent = ffmpeg.getParent();
        if (parent == null) {
            return Executables.resolve(name).orElse(null);
        }
        Path candidate = parent.resolve(executableName(name));
        if (Files.isRegularFile(candidate)) {
            return candidate;
        }
        return Executables.resolve(name).orElse(null);
    }

    private static String executableName(String name) {
        return OperatingSystem.CURRENT_OS == OperatingSystem.WINDOWS && !name.endsWith(".exe")
                ? name + ".exe"
                : name;
    }

    /// The resolved pair.
    public record FfmpegPaths(Path ffmpeg, Path ffprobe) {
        public FfmpegPaths {
            ffmpeg = ffmpeg.toAbsolutePath().normalize();
            ffprobe = ffprobe.toAbsolutePath().normalize();
        }
    }
}