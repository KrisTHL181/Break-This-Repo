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
package org.jackhuang.hmcl.web.zcode;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jackhuang.hmcl.web.NodeDshStub;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The experimental ZCode runtime against the `fake-zcode` node stub: the
/// readiness lines are parsed into a port, the pass-through arguments, the
/// injected `ZCODE_DATA_BASE_DIR` and the per-instance `HOME` reach the
/// process, a second launch is a no-op, the log carries the process output,
/// stop is clean, and a process that exits early leaves `error` with the log's
/// tail.
class ZcodeRuntimeTest {

    /// The stub distribution, the way `HDSL_ZCODE_PACKAGE` would point at a
    /// real one.
    private static final Path PACKAGE = Path.of("src/test/resources/fake-zcode")
            .toAbsolutePath().normalize();

    @TempDir
    Path dataDir;

    private ZcodeInstanceManager manager() {
        return new ZcodeInstanceManager(dataDir.resolve("zcode/instances"));
    }

    /// An instance can be pointed at a project the operator already has; the launch
    /// and the terminal both take the recorded path, not the one the panel would have
    /// invented. With no path given the panel invents a visible one: a directory of
    /// its own under `zcode/workspaces`, created immediately.
    @Test
    void anInstanceCanWorkInAFolderTheOperatorChose(@TempDir Path project) throws Exception {
        ZcodeInstanceManager manager = manager();
        Path chosen = project.resolve("my-project");
        ZcodeInstance instance = manager.create("lab", null, null, chosen);

        assertEquals(chosen.toAbsolutePath().normalize(), manager.workspaceDirectory(instance));
        assertTrue(java.nio.file.Files.isDirectory(chosen), "the workspace is created eagerly");
        assertEquals(chosen.toString(), instance.workspacePath());

        // No choice recorded means a default workspace of its own, next to the
        // instances rather than inside the instance directory.
        ZcodeInstance own = manager.create("own", null, null);
        Path ownWorkspace = manager.workspaceDirectory(own);
        assertEquals(manager.workspacesRoot(), ownWorkspace.getParent());
        assertEquals(ownWorkspace, Path.of(own.workspacePath()), "the recorded path is the workspace");
        assertTrue(Files.isDirectory(ownWorkspace), "the default workspace is created eagerly: " + ownWorkspace);
        assertTrue(ownWorkspace.getFileName().toString().startsWith(own.id()),
                "the directory name starts with the id: " + ownWorkspace);

        // A name with no ASCII characters leaves just the id — no dangling suffix.
        ZcodeInstance chinese = manager.create("项目", null, null);
        Path chineseWorkspace = manager.workspaceDirectory(chinese);
        assertEquals(chinese.id(), chineseWorkspace.getFileName().toString());
        assertEquals(manager.workspacesRoot(), chineseWorkspace.getParent());
        assertTrue(Files.isDirectory(chineseWorkspace));
    }

    /// A path the operator typed has to be unambiguous: a relative one would depend on
    /// the panel's own working directory, which is not theirs to know.
    @Test
    void aRelativeWorkspaceIsRefused() {
        ZcodeInstanceManager manager = manager();
        assertThrows(ZcodeException.class,
                () -> manager.create("lab", null, null, Path.of("relative-project")));
    }

    /// The session database follows `HOME` and no other variable, so every instance
    /// gets a home of its own inside its own directory — on the data volume, where a
    /// container rebuild cannot reach it — and it exists before anything is started.
    @Test
    void everyInstanceGetsItsOwnHomeOnTheDataVolume() throws Exception {
        ZcodeInstanceManager manager = manager();
        ZcodeInstance one = manager.create("one", null, null);
        ZcodeInstance two = manager.create("two", null, null);

        Path first = manager.homeDirectory(one);
        Path second = manager.homeDirectory(two);
        assertTrue(Files.isDirectory(first), "created eagerly: " + first);
        assertTrue(Files.isDirectory(second), "created eagerly: " + second);
        assertEquals(manager.instanceDirectory(one.id()), first.getParent());
        assertTrue(!first.equals(second), "instances must not share a home: " + first + " / " + second);

        // Idempotent: a launch of a home that is already there is a no-op, which is
        // what an instance created by an older panel relies on.
        Files.writeString(first.resolve("marker"), "kept");
        assertEquals(first, manager.prepareHome(one));
        assertTrue(Files.isRegularFile(first.resolve("marker")), "an existing home is left alone");
    }

    @Test
    void launchParsesThePortPassesArgumentsAndStopsCleanly() throws Exception {
        NodeDshStub.assumeNode();
        ZcodeInstanceManager manager = manager();
        ZcodeInstance instance = manager.create("lab", null, null);
        Path argsFile = dataDir.resolve("args.json");

        ZcodeRuntime.Status status = ZcodeRuntime.launch(manager, instance,
                Map.of("FAKE_ZCODE_ARGS_FILE", argsFile.toString()), "node", PACKAGE);
        assertEquals(ZcodeRuntime.State.RUNNING, status.state(), "status: " + status);

        // The announced port was parsed from the `Local:` line and persisted
        // into the manifest (the stub asks the OS for a free one for `--port 0`).
        ZcodeInstance updated = manager.find(instance.id());
        assertNotNull(updated);
        assertTrue(updated.lastPort() > 0,
                "the port must come from the stub's Local: line: " + updated.lastPort());

        // The pass-through: the loopback host, --no-token and --workspace travel
        // in argv, and the data directory lands in ZCODE_DATA_BASE_DIR. The
        // instance is reached through the panel's proxy, so it needs no token of
        // its own and must not listen beyond loopback.
        JsonObject recorded = JsonParser.parseString(Files.readString(argsFile)).getAsJsonObject();
        JsonArray argv = recorded.getAsJsonArray("argv");
        List<String> arguments = new java.util.ArrayList<>();
        argv.forEach(element -> arguments.add(element.getAsString()));
        assertTrue(arguments.contains("--web"), "argv: " + arguments);
        assertTrue(arguments.contains("--no-open"), "argv: " + arguments);
        assertTrue(arguments.contains("--host"), "argv: " + arguments);
        assertEquals("127.0.0.1", valueAfter(arguments, "--host"));
        assertEquals("0", valueAfter(arguments, "--port"), "--port 0 asks the process to pick a free port");
        assertTrue(arguments.contains("--no-token"), "argv: " + arguments);
        assertTrue(!arguments.contains("--token"), "the proxied instance must not mint a token: " + arguments);
        assertEquals(manager.workspaceDirectory(instance).toString(), valueAfter(arguments, "--workspace"));
        assertEquals(manager.dataDirectory(instance).toString(), recorded.get("dataBaseDir").getAsString());
        // Home is where the session database lives, so it has to be the
        // instance's own — the panel's home is on the container's writable layer,
        // where a rebuild erases it while `/data` keeps the index that lists it.
        assertEquals(manager.homeDirectory(instance).toString(), recorded.get("home").getAsString());
        assertTrue(Files.isDirectory(manager.homeDirectory(instance)),
                "the home must exist before the process starts: " + manager.homeDirectory(instance));

        // The log carries the readiness announcement for the tail endpoint.
        List<String> log = manager.tailLog(updated, 50);
        assertTrue(log.stream().anyMatch(line -> line.contains("ZCode Web is running")),
                "log tail: " + log);

        // A second launch while running changes nothing.
        ZcodeRuntime.Status again = ZcodeRuntime.launch(manager, updated,
                Map.of("FAKE_ZCODE_ARGS_FILE", argsFile.toString()), "node", PACKAGE);
        assertEquals(ZcodeRuntime.State.RUNNING, again.state());
        assertEquals(updated.lastPort(), manager.find(instance.id()).lastPort());

        ZcodeRuntime.stop(instance.id());
        assertEquals(ZcodeRuntime.State.STOPPED, ZcodeRuntime.status(instance.id()).state());
    }

    @Test
    void aProcessThatExitsEarlyLeavesErrorWithTheLogTail() throws Exception {
        NodeDshStub.assumeNode();
        Path broken = dataDir.resolve("broken-zcode");
        Files.createDirectories(broken.resolve("bin"));
        Files.writeString(broken.resolve("bin/zcode.mjs"),
                "console.log('boom: something is missing');\nprocess.exit(1);\n");

        ZcodeInstanceManager manager = manager();
        ZcodeInstance instance = manager.create("broken", null, null);
        ZcodeRuntime.Status status = ZcodeRuntime.launch(
                manager, instance, Map.of(), "node", broken);
        assertEquals(ZcodeRuntime.State.ERROR, status.state());
        assertNotNull(status.error());
        assertTrue(status.error().contains("boom: something is missing"),
                "the reason must carry the log tail: " + status.error());
    }

    private static @org.jetbrains.annotations.Nullable String valueAfter(List<String> arguments, String flag) {
        int index = arguments.indexOf(flag);
        return index >= 0 && index + 1 < arguments.size() ? arguments.get(index + 1) : null;
    }
}
