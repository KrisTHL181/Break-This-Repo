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
package org.jackhuang.hmcl.web.transcode;

import org.jackhuang.hmcl.web.config.ServerConfig;
import org.jetbrains.annotations.NotNullByDefault;

import java.nio.file.Path;

/// Where the transcode module keeps its own files.
///
/// The feature state and the queue are panel data (`<data>/transcode`). Media is
/// **not** panel data: it is the operator's video root, either configured
/// (`HDSL_MEDIA_ROOT` / `transcode.media_root`) or the `<data>/video` fallback
/// for a bare deployment. The container image points the configured value at
/// `/home/hdsl/video`; nothing here creates either directory.
@NotNullByDefault
public final class TranscodePaths {

    private TranscodePaths() {
    }

    /// `<data>/transcode`.
    public static Path dataDir(ServerConfig config) {
        return config.dataDir.resolve("transcode").toAbsolutePath().normalize();
    }

    /// `feature.json`, the single source of truth for whether the surface exists.
    public static Path featureFile(ServerConfig config) {
        return dataDir(config).resolve("feature.json");
    }

    /// The media root the operator configured, or `<data>/video`.
    public static Path mediaRoot(ServerConfig config) {
        String configured = config.transcode.mediaRoot;
        if (configured != null && !configured.isBlank()) {
            return Path.of(configured.trim()).toAbsolutePath().normalize();
        }
        return config.dataDir.resolve("video").toAbsolutePath().normalize();
    }
}