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
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The parts of Hermes' runtime that can be decided without starting a Python
/// process: the port pair, the API key, and the wrapper it all runs through.
///
/// The wrapper is a **generated file**, so its content is a contract: an edit
/// that drops `HERMES_GATEWAY_LOCK_DIR` would let two instances share a gateway
/// lock, and one that drops the trap would leave children behind on `stop`.
class HermesRuntimeTest {

    @TempDir
    Path dir;

    @Test
    void portsArePersistedAndReadBack() throws IOException {
        Path panel = dir.resolve("panel");
        Files.createDirectories(panel);

        BrandRuntime.HermesPorts first = BrandRuntime.hermesPorts(panel);
        assertTrue(first.api() > 0 && first.dashboard() > 0);
        assertTrue(first.api() != first.dashboard(), "two services cannot share one port");

        BrandRuntime.HermesPorts second = BrandRuntime.hermesPorts(panel);
        assertEquals(first, second, "a restart must land on the same pair");

        assertTrue(Files.readString(panel.resolve("ports.json"), StandardCharsets.UTF_8)
                .contains("\"api\":" + first.api()));
    }

    @Test
    void aTakenPairIsReplacedOnce() throws IOException {
        Path panel = dir.resolve("panel");
        Files.createDirectories(panel);
        // Hold the port for real: a privileged port number is not portable
        // (a root test run can bind 1), so the test occupies what it claims.
        try (java.net.ServerSocket occupied =
                     new java.net.ServerSocket(0, 1, java.net.InetAddress.getLoopbackAddress())) {
            int taken = occupied.getLocalPort();
            Files.writeString(panel.resolve("ports.json"),
                    "{\"api\":" + taken + ",\"dashboard\":" + taken + "}\n",
                    StandardCharsets.UTF_8);

            BrandRuntime.HermesPorts replaced = BrandRuntime.hermesPorts(panel);

            assertTrue(replaced.api() != taken && replaced.dashboard() != taken);
            assertTrue(replaced.api() > 0 && replaced.dashboard() > 0);
        }
    }

    @Test
    void aGarbledPortsFileFallsBackToAFreshPair() throws IOException {
        Path panel = dir.resolve("panel");
        Files.createDirectories(panel);
        Files.writeString(panel.resolve("ports.json"), "not json at all", StandardCharsets.UTF_8);

        assertNull(BrandRuntime.readHermesPorts(panel.resolve("ports.json")));
        BrandRuntime.HermesPorts ports = BrandRuntime.hermesPorts(panel);
        assertTrue(ports.api() > 0 && ports.dashboard() > 0);
    }

    @Test
    void theApiKeyIsWrittenOnceAndReused() throws IOException {
        Path home = dir.resolve("home");
        Files.createDirectories(home);

        String generated = BrandRuntime.hermesApiKey(home);
        assertEquals(32, generated.length(), "32 hex characters: the gateway needs 16+");
        assertTrue(generated.matches("[0-9a-f]{32}"));

        BrandRuntime.writeHermesEnv(home, generated, 21001);
        String text = Files.readString(home.resolve(".env"), StandardCharsets.UTF_8);
        assertTrue(text.contains("API_SERVER_KEY=" + generated), text);
        assertTrue(text.contains("API_SERVER_HOST=127.0.0.1"), text);
        assertTrue(text.contains("API_SERVER_PORT=21001"), text);
        // Upstream enables its API on the key alone; a separate flag only adds noise.
        assertFalse(text.contains("API_SERVER_ENABLED"), text);

        assertEquals(generated, BrandRuntime.hermesApiKey(home), "a restart must not rotate the key");
    }

    @Test
    void theWrapperCarriesTheContractsItMust() throws IOException {
        Path panel = dir.resolve("panel");
        Files.createDirectories(panel);
        Path home = dir.resolve("home");
        Files.createDirectories(home);

        BrandRuntime.HermesWrapper wrapper = BrandRuntime.writeHermesWrapper(panel, home,
                new BrandRuntime.HermesPorts(21001, 21002), dir.resolve("release"));

        String script = Files.readString(wrapper.file(), StandardCharsets.UTF_8);
        for (String required : new String[]{
                "HERMES_GATEWAY_LOCK_DIR", "HERMES_RUNTIME_DIR", "XDG_RUNTIME_DIR",
                "gateway run", "dashboard --skip-build", "--no-open",
                "HERMES_WEB_DIST", "GATEWAY_HEALTH_URL", "trap ", "gateway.pid", "dashboard.pid"}) {
            assertTrue(script.contains(required), "the wrapper no longer mentions " + required);
        }
        // `HERMES_DASHBOARD=` is the "serve dashboard from a gateway" switch; this
        // design runs the dashboard as its own process.
        assertFalse(script.contains("HERMES_DASHBOARD="), script);
        // The children must inherit stdout, or the panel's log page shows only
        // stage markers and never the gateway's own errors.
        assertFalse(script.contains(">/dev/null 2>&1 &"), script);
        // The order of `$1..$5` in the script and the argv the panel builds are
        // one contract; the record that carries both is where it is pinned.
        assertEquals(List.of(home.toString(), "21001", "21002",
                        dir.resolve("release/hermes_cli/web_dist").toString(),
                        dir.resolve("release/.venv").toString()),
                wrapper.arguments());
    }

    @Test
    void theStopWindowIsPerBrand() {
        assertEquals(10, BrandRuntime.stopWaitSecondsOf("unknown-instance"));
        assertEquals(40, Brand.HERMES.stopWaitSeconds());
    }

    @Test
    void reconcileIgnoresInstancesThatAreNotHermes() throws Exception {
        Path instances = dir.resolve("instances");
        BrandInstanceManager manager = new BrandInstanceManager(Brand.HERMES, instances);
        BrandInstance created = manager.create("demo", "0.22.5");
        // The pid files live under HOME (`<home>/panel/`), written by the wrapper
        // as the agent — not in the panel-owned `<instance>/panel/`.
        Path pidDir = manager.homeDirectory(created).resolve("panel");
        Files.createDirectories(pidDir);
        // A pid file naming a process that cannot exist (pid 0 is never a user
        // process) must be cleaned up without killing anything.
        Files.writeString(pidDir.resolve("gateway.pid"), "0\n", StandardCharsets.UTF_8);

        BrandRuntime.reconcile(manager);

        assertFalse(Files.exists(pidDir.resolve("gateway.pid")), "a stale pid file is removed");
    }

    @Test
    void theEndpointIsNullBeforeAnyPortsExist() throws IOException {
        BrandInstanceManager manager = new BrandInstanceManager(Brand.HERMES, dir.resolve("instances"));
        BrandInstance instance = new BrandInstance("abc", "hermes", "demo", "0.22.5", null, 0, 1, null);
        assertNull(BrandRuntime.hermesEndpoint(manager, instance));

        Path panel = manager.instanceDirectory("abc").resolve("panel");
        Files.createDirectories(panel);
        Files.writeString(panel.resolve("ports.json"), "{\"api\":21001,\"dashboard\":21002}\n",
                StandardCharsets.UTF_8);
        Files.createDirectories(manager.homeDirectory(instance));
        BrandRuntime.writeHermesEnv(manager.homeDirectory(instance), "0123456789abcdef0123456789abcdef", 21001);

        BrandRuntime.HermesEndpoint endpoint = BrandRuntime.hermesEndpoint(manager, instance);
        assertNotNull(endpoint);
        assertEquals("http://127.0.0.1:21001/v1", endpoint.baseUrl());
        assertEquals(32, endpoint.key().length());
        assertFalse(endpoint.running(), "nothing was launched");
    }
}
