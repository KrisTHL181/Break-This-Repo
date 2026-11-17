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
package org.jackhuang.hmcl.web.brand;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

/// The pid the terminal's detached gateway leaves behind.
///
/// Switching from the terminal back to the web mode has to stop that gateway
/// **before** the wrapper chooses its ports: it still holds the API port, so a
/// launch that skipped this would reallocate the pair on every switch — and a
/// saved "API 端点" account points at the old one.
///
/// The file is Hermes' own (`<HERMES_HOME>/gateway_state.json`), written by
/// whichever gateway serves the home, so all this reads is the `pid` field.
class HermesGatewayPidTest {

    @Test
    void readsThePidTheGatewayWrote(@TempDir Path home) throws IOException {
        Files.writeString(home.resolve("gateway_state.json"),
                "{\"pid\": 4242, \"kind\": \"hermes-gateway\", \"gateway_state\": \"running\"}",
                StandardCharsets.UTF_8);

        assertEquals(4242, BrandRuntime.gatewayPidOf(home));
    }

    @Test
    void aMissingStateFileIsZero(@TempDir Path home) {
        assertEquals(0, BrandRuntime.gatewayPidOf(home));
    }

    @Test
    void aStateFileWithoutAPidIsZero(@TempDir Path home) throws IOException {
        Files.writeString(home.resolve("gateway_state.json"), "{\"gateway_state\": \"running\"}",
                StandardCharsets.UTF_8);

        assertEquals(0, BrandRuntime.gatewayPidOf(home));
    }
}
