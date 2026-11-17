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
package org.jackhuang.hmcl.web.http;

import org.jackhuang.hmcl.dsh.NodeRuntime;
import org.jackhuang.hmcl.dsh.RuntimeLayout;
import org.jackhuang.hmcl.util.platform.OperatingSystem;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/// What `/api/node` is allowed to call `suitable`.
///
/// It used to mean "the version is inside the supported range", and a runtime
/// whose binary cannot start — Node's official builds link `libatomic.so.1`,
/// which Debian's slim images do not ship — came back as `suitable: true` while
/// every install on it exited 127. The settings page draws its "ready" state
/// from this field, so the field has to mean "this can run".
class NodeApiSuitabilityTest {

    private static void runtimeBinary(Path directory, String body) throws IOException {
        Path node = RuntimeLayout.CURRENT.node(directory);
        Files.createDirectories(node.getParent());
        Files.writeString(node, body);
        node.toFile().setExecutable(true);
    }

    @Test
    void anUnsupportedVersionIsNotSuitableWhateverTheBinaryDoes(@TempDir Path directory) {
        // No process is spawned for this one, so it runs on every platform.
        assertFalse(NodeApiServlet.suitable(new NodeRuntime("20.1.0", directory)));
    }

    @Test
    void aSupportedVersionWhoseBinaryCannotLoadIsNotSuitable(@TempDir Path directory) throws Exception {
        assumeTrue(OperatingSystem.CURRENT_OS != OperatingSystem.WINDOWS,
                "the fixture is a POSIX shell script");
        runtimeBinary(directory, "#!/bin/sh\n"
                + "echo 'node: error while loading shared libraries: libatomic.so.1' >&2\n"
                + "exit 127\n");

        assertFalse(NodeApiServlet.suitable(new NodeRuntime("26.13.0", directory)),
                "the version is in range, but nothing can be installed with it");
    }

    @Test
    void aSupportedVersionThatRunsIsSuitable(@TempDir Path directory) throws Exception {
        assumeTrue(OperatingSystem.CURRENT_OS != OperatingSystem.WINDOWS,
                "the fixture is a POSIX shell script");
        runtimeBinary(directory, "#!/bin/sh\necho v22.23.1\n");

        assertTrue(NodeApiServlet.suitable(new NodeRuntime("22.23.1", directory)));
    }
}
