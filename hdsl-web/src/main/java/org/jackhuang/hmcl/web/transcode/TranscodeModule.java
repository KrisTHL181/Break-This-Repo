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
import org.jackhuang.hmcl.web.event.EventBus;
import org.jackhuang.hmcl.web.transcode.media.MediaBrowser;
import org.jackhuang.hmcl.web.transcode.preset.PresetStore;
import org.jackhuang.hmcl.web.transcode.quality.QualityService;
import org.jackhuang.hmcl.web.transcode.queue.TranscodeQueue;
import org.jackhuang.hmcl.web.transcode.runtime.FfmpegInstaller;
import org.jackhuang.hmcl.web.transcode.runtime.TranscodeRuntime;
import org.jackhuang.hmcl.web.transcode.tools.TranscodeTools;
import org.jetbrains.annotations.NotNullByDefault;

import java.nio.file.Path;

/// The transcode module's collaborators, assembled once with the server.
///
/// Keeping them in one object is what lets later commits add the preset store and
/// the queue without threading four more constructor arguments through the servlet.
/// Nothing here touches the disk or starts a process until a request or an explicit
/// enable asks for it.
@NotNullByDefault
public final class TranscodeModule {

    private final ServerConfig config;
    private final TranscodeFeatureState feature;
    private final TranscodeRuntime runtime;
    private final MediaBrowser media;
    private final PresetStore presets;
    private final TranscodeQueue queue;
    private final TranscodeTools tools;
    private final QualityService quality;
    private final FfmpegInstaller installer;

    public TranscodeModule(ServerConfig config, EventBus eventBus) {
        this.config = config;
        this.feature = new TranscodeFeatureState(TranscodePaths.featureFile(config));
        this.runtime = new TranscodeRuntime(config);
        this.media = new MediaBrowser(runtime);
        this.presets = new PresetStore(TranscodePaths.dataDir(config).resolve("presets"));
        this.queue = new TranscodeQueue(this, eventBus);
        this.tools = new TranscodeTools(this);
        this.quality = new QualityService(this);
        this.installer = new FfmpegInstaller(this);
        if (feature.enabled()) {
            queue.start();
        }
    }

    public ServerConfig config() {
        return config;
    }

    public TranscodeFeatureState feature() {
        return feature;
    }

    public TranscodeRuntime runtime() {
        return runtime;
    }

    public MediaBrowser media() {
        return media;
    }

    public PresetStore presets() {
        return presets;
    }

    public TranscodeQueue queue() {
        return queue;
    }

    public TranscodeTools tools() {
        return tools;
    }

    public QualityService quality() {
        return quality;
    }

    public FfmpegInstaller installer() {
        return installer;
    }

    public Path dataDir() {
        return TranscodePaths.dataDir(config);
    }

    public Path mediaRoot() {
        return TranscodePaths.mediaRoot(config);
    }
}