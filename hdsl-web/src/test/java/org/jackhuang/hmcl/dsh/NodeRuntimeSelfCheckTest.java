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
package org.jackhuang.hmcl.dsh;

import org.jackhuang.hmcl.util.platform.OperatingSystem;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/// Whether a managed runtime that was unpacked can actually be run.
///
/// This is the shape a slim base image produces: the archive contains
/// `bin/node`, it is executable, the version in the directory name is right — and
/// the binary dies on a shared library the image does not ship (`libatomic.so.1`
/// for Node's official builds since 26). The panel used to call that a finished
/// install: the task said done, `/api/node` said `suitable: true`, and the only
/// trace was `exit 127` from the pnpm step further down a log.
///
/// The fixtures are shell scripts, so the process-spawning cases only run where
/// a shell can be executed; the "no binary at all" case runs everywhere.
class NodeRuntimeSelfCheckTest {

    /// A runtime whose binary exists and cannot load, in the words the loader
    /// itself uses.
    private static void brokenRuntime(Path directory) throws IOException {
        Path node = RuntimeLayout.CURRENT.node(directory);
        Files.createDirectories(node.getParent());
        Files.writeString(node, "#!/bin/sh\n"
                + "echo 'node: error while loading shared libraries: libatomic.so.1: "
                + "cannot open shared object file: No such file or directory' >&2\n"
                + "exit 127\n");
        assertTrue(node.toFile().setExecutable(true), "the fixture has to be executable");
    }

    /// A runtime that answers the way a working one does.
    private static void workingRuntime(Path directory, String reported) throws IOException {
        Path node = RuntimeLayout.CURRENT.node(directory);
        Files.createDirectories(node.getParent());
        Files.writeString(node, "#!/bin/sh\necho v" + reported + "\n");
        assertTrue(node.toFile().setExecutable(true), "the fixture has to be executable");
    }

    @Test
    void aRuntimeWithoutABinaryIsNotRunnable(@TempDir Path directory) {
        NodeRuntimeManager.NodeProbe probe = NodeRuntimeManager.probe(directory);

        assertFalse(probe.runs(), probe.detail());
        assertTrue(probe.detail().contains("node"), probe.detail());
        assertNull(NodeRuntimeManager.versionOfDirectory(directory));
    }

    @Test
    void aBinaryThatCannotLoadIsReportedInItsOwnWords(@TempDir Path directory) throws Exception {
        assumeTrue(OperatingSystem.CURRENT_OS != OperatingSystem.WINDOWS,
                "the fixture is a POSIX shell script");
        brokenRuntime(directory);

        NodeRuntimeManager.NodeProbe probe = NodeRuntimeManager.probe(directory);

        assertFalse(probe.runs());
        assertTrue(probe.detail().contains("libatomic.so.1"), probe.detail());
        assertFalse(NodeRuntimeManager.runs(new NodeRuntime("26.13.0", directory)),
                "a binary that cannot load must not be reported as runnable");
        assertNull(NodeRuntimeManager.versionOfDirectory(directory));
    }

    @Test
    void aBinaryThatAnswersReportsTheVersionItSays(@TempDir Path directory) throws Exception {
        assumeTrue(OperatingSystem.CURRENT_OS != OperatingSystem.WINDOWS,
                "the fixture is a POSIX shell script");
        workingRuntime(directory, "22.23.1");

        NodeRuntimeManager.NodeProbe probe = NodeRuntimeManager.probe(directory);

        assertTrue(probe.runs(), probe.detail());
        assertTrue("22.23.1".equals(probe.version()), probe.version());
        assertTrue(NodeRuntimeManager.runs(new NodeRuntime("22.23.1", directory)));
        assertTrue("22.23.1".equals(NodeRuntimeManager.versionOfDirectory(directory)));
    }

    @Test
    void verifyingARuntimeThatDoesNotRunRemovesItAndSaysWhy(@TempDir Path directory) throws Exception {
        assumeTrue(OperatingSystem.CURRENT_OS != OperatingSystem.WINDOWS,
                "the fixture is a POSIX shell script");
        brokenRuntime(directory);

        DshException refused = assertThrows(DshException.class,
                () -> NodeRuntimeManager.verifyRunnable(new NodeRuntime("26.13.0", directory)));

        assertTrue(refused.getMessage().contains("26.13.0"), refused.getMessage());
        assertTrue(refused.getMessage().contains("libatomic.so.1"), refused.getMessage());
        assertTrue(refused.getMessage().contains("libatomic1"), refused.getMessage());
        assertFalse(Files.exists(directory),
                "a runtime that cannot run must not be left behind as an installed one");
    }

    @Test
    void verifyingARuntimeThatRunsKeepsIt(@TempDir Path directory) throws Exception {
        assumeTrue(OperatingSystem.CURRENT_OS != OperatingSystem.WINDOWS,
                "the fixture is a POSIX shell script");
        workingRuntime(directory, "22.23.1");

        NodeRuntimeManager.verifyRunnable(new NodeRuntime("22.23.1", directory));

        assertTrue(Files.isDirectory(directory), "a working runtime is not touched");
    }
}
