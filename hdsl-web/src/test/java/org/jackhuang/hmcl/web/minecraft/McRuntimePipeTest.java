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
package org.jackhuang.hmcl.web.minecraft;

import org.jackhuang.hmcl.web.terminal.TerminalManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The Windows console path (`ptySupported = false`) exercised on every host.
///
/// Keeping this separate from [McRuntimeTest] means Windows still gets the
/// pipe lifecycle covered by the Linux test suite, and Linux still runs the
/// real PTY tests.
class McRuntimePipeTest {

    @TempDir
    Path dir;

    private McInstanceManager manager;
    private McFeatureState feature;
    private TerminalManager terminals;

    @BeforeEach
    void setUp() throws Exception {
        manager = new McInstanceManager(dir.resolve("minecraft").resolve("instances"));
        feature = new McFeatureState(dir.resolve("feature.json"));
        feature.enable(consent());
        terminals = new TerminalManager();
    }

    @AfterEach
    void tearDown() {
        if (terminals != null) {
            terminals.close();
        }
    }

    private static McConsent consent() {
        return new McConsent(3, "sha256:terms", "sha256:eula",
                "2026-10-29T00:58:00Z", "2026-10-29T01:00:00Z", "2026-10-29T01:00:00Z",
                "tester", "127.0.0.1", "scroll+checkbox", true, "1.4.0", "UA", "abc1234", null);
    }

    private static String javaBin() {
        String name = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win")
                ? "java.exe" : "java";
        return Path.of(System.getProperty("java.home"), "bin", name).toString();
    }

    private McInstance installedInstance(String name) throws Exception {
        // Pick ports the host will actually let us bind: `launch` checks them
        // before spawning on **both** paths, so a fixed 25565 only works on a
        // machine where nothing else listens there — and when the panel itself
        // publishes the MC port, that machine is not this one.
        McPorts.Pair ports = McPorts.allocate(
                port -> manager.portTaken(port) || !McPorts.isFree(port), 26000, 26999);
        McInstance instance = manager.create(name, "1.21.4", ports.game(), ports.rcon(),
                1024, "world", null);
        Files.writeString(manager.serverJar(instance), "");
        manager.acceptEula(instance);
        return instance;
    }

    private McRuntime runtime() {
        List<String> command = new ArrayList<>(List.of(
                javaBin(), "-cp", System.getProperty("java.class.path"), FakeMcServer.class.getName()));
        return new McRuntime(manager, terminals, feature, instance -> command, false);
    }

    private static McRuntime.Status await(McRuntime runtime, String id, McRuntime.State expected)
            throws Exception {
        long deadline = System.nanoTime() + Duration.ofSeconds(15).toNanos();
        McRuntime.Status status = runtime.status(id);
        while (System.nanoTime() < deadline) {
            status = runtime.status(id);
            if (status.state() == expected) {
                return status;
            }
            Thread.sleep(50);
        }
        return status;
    }

    @Test
    void pipeConsoleLaunchesLogsCommandsAndStops() throws Exception {
        McInstance instance = installedInstance("pipe");
        McRuntime runtime = runtime();

        runtime.launch(instance.id());
        assertEquals(McRuntime.State.RUNNING, await(runtime, instance.id(), McRuntime.State.RUNNING).state());
        assertEquals("pipe", runtime.consoleMode(instance.id()));
        assertTrue(runtime.console(instance.id()).isEmpty(), "pipe servers have no terminal session");

        runtime.sendCommand(instance.id(), "say hello");
        long deadline = System.nanoTime() + Duration.ofSeconds(10).toNanos();
        String log = "";
        while (System.nanoTime() < deadline) {
            log = Files.readString(manager.logFile(instance));
            if (log.contains("say hello")) {
                break;
            }
            Thread.sleep(50);
        }
        assertTrue(log.contains("say hello"), log);

        runtime.stop(instance.id());
        assertEquals(McRuntime.State.STOPPED, await(runtime, instance.id(), McRuntime.State.STOPPED).state());
        assertFalse(runtime.isRunning(instance.id()));
    }

    @Test
    void stopAllStopsEveryPipeServer() throws Exception {
        McInstance first = installedInstance("first");
        McInstance second = installedInstance("second");
        McRuntime runtime = runtime();

        runtime.launch(first.id());
        runtime.launch(second.id());
        await(runtime, first.id(), McRuntime.State.RUNNING);
        await(runtime, second.id(), McRuntime.State.RUNNING);

        runtime.stopAll(Duration.ofSeconds(10));

        assertEquals(McRuntime.State.STOPPED, await(runtime, first.id(), McRuntime.State.STOPPED).state());
        assertEquals(McRuntime.State.STOPPED, await(runtime, second.id(), McRuntime.State.STOPPED).state());
        assertFalse(runtime.isRunning(first.id()));
        assertFalse(runtime.isRunning(second.id()));
    }

    @Test
    void aBusyGamePortIsRefusedBeforeSpawn() throws Exception {
        McInstance instance = installedInstance("busy");
        McRuntime runtime = runtime();

        try (java.net.ServerSocket occupied = new java.net.ServerSocket(instance.port())) {
            McException refused = org.junit.jupiter.api.Assertions.assertThrows(
                    McException.class, () -> runtime.launch(instance.id()));
            assertTrue(refused.getMessage().contains("already in use"), refused.getMessage());
        }
    }
}
