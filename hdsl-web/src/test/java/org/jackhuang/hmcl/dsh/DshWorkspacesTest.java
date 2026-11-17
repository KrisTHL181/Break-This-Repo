/*
 * HDSL
 * Copyright (C) 2026  HDSL contributors
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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Verifies that sessions are grouped the way the harness stores them.
///
/// The slug directory a session lives under is the harness's own grouping, and
/// it is the only one that cannot split a project in two: the readable path is a
/// convenience read from a projection-cache row, never the grouping key.
class DshWorkspacesTest {
    /// A home to build sessions in.
    @TempDir
    Path home;

    /// Writes one session, with a projection-cache row when it names a path or title.
    ///
    /// @param slug       the workspace slug
    /// @param id         the session id
    /// @param cwd        the working directory, or `null`
    /// @param title      the title, or `null`
    /// @param modifiedAt the last activity, in epoch milliseconds
    private void writeSession(String slug, String id, @Nullable String cwd, @Nullable String title,
                              long modifiedAt) throws Exception {
        Path directory = home.resolve("sessions").resolve(slug).resolve(id);
        Files.createDirectories(directory);
        Files.writeString(directory.resolve("session.v3.jsonl.zstd"), "log");

        if (cwd != null || title != null) {
            Path cache = home.resolve("storages").resolve("session_projcache")
                    .resolve("sessions").resolve(id + ".json");
            Files.createDirectories(cache.getParent());
            String identity = cwd == null ? "{}"
                    : "{\"cwd\":\"" + cwd + "\",\"createdAt\":" + modifiedAt + "}";
            String rows = title == null ? "{}" : "{\"title\":{\"val\":\"" + title + "\"}}";
            Files.writeString(cache, "{\"record\":{\"identity\":" + identity + ",\"rows\":" + rows + "}}");
        }
        Files.setLastModifiedTime(directory, FileTime.fromMillis(modifiedAt));
    }

    @Test
    void sessionsAreGroupedByTheirWorkspaceAndNewestFirst() throws Exception {
        writeSession("--tmp-alpha--", "session-1", "/tmp/alpha", "First", 1_000L);
        writeSession("--tmp-alpha--", "session-2", "/tmp/alpha", "Second", 2_000L);
        writeSession("--tmp-beta--", "session-3", "/tmp/beta", "Third", 3_000L);

        List<DshWorkspace> workspaces = DshSessions.workspaces(home);

        assertEquals(2, workspaces.size(), "one workspace per slug directory");
        assertEquals("beta", workspaces.get(0).title(), "the most recently used workspace comes first");
        assertEquals("alpha", workspaces.get(1).title());
        assertEquals(2, workspaces.get(1).sessions().size());
        assertEquals("/tmp/alpha", workspaces.get(1).path());
        assertEquals(3_000L, workspaces.get(0).modifiedAt());
    }

    @Test
    void aWorkspaceTakesThePathItsSessionsNamed() throws Exception {
        writeSession("--tmp-gamma--", "session-1", null, null, 1_000L);
        writeSession("--tmp-gamma--", "session-2", "/tmp/gamma", "Named", 2_000L);

        DshWorkspace workspace = DshSessions.workspaces(home).get(0);

        assertEquals("/tmp/gamma", workspace.path(), "the one session that named a path names the workspace");
        assertEquals("gamma", workspace.title());
    }

    @Test
    void aHomeWithNoSessionsHasNoWorkspaces() throws Exception {
        assertTrue(DshSessions.workspaces(home).isEmpty());
    }
}
