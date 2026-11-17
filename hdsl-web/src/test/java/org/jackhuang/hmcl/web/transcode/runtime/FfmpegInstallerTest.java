/*
 * HDSL-web
 * Copyright (C) 2026  HDSL-web contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package org.jackhuang.hmcl.web.transcode.runtime;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FfmpegInstallerTest {

    @Test
    void onlyPublicHttpsSourcesAreAccepted() {
        assertDoesNotThrow(() -> FfmpegInstaller.validateUrl("https://example.com/ffmpeg.zip"));
        assertThrows(IOException.class, () -> FfmpegInstaller.validateUrl("http://example.com/ffmpeg.zip"));
        assertThrows(IOException.class, () -> FfmpegInstaller.validateUrl("https://127.0.0.1/ffmpeg.zip"));
        assertThrows(IOException.class, () -> FfmpegInstaller.validateUrl("https://192.168.1.2/ffmpeg.zip"));
        assertThrows(IOException.class, () -> FfmpegInstaller.validateUrl("file:///tmp/ffmpeg.zip"));
    }
}
