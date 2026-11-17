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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Verifies session discovery and migration.
///
/// The fixtures are written in a temporary directory rather than against a real
/// home: migration moves files, and a test must never reach the user's data.
class DshSessionsTest {
    /// The workspace slug the fixture sessions live under.
    private static final String SLUG = "--home-user-project--";

    /// The fixture session id.
    private static final String SESSION_ID = "11111111-2222-3333-4444-555555555555";

    /// Builds an instance whose home is a directory.
    ///
    /// @param id      the instance id
    /// @param home    the directory standing in for `DSH_HOME`
    /// @param version the pinned version
    /// @return the instance
    private static DshInstance instance(String id, Path home, String version) {
        return new DshInstance(id, version, "web", "/tmp",
                DshNodeRuntime.SYSTEM, DshHomeMode.CUSTOM, home.toString(),
                List.of(), Map.of(), DshInstanceIcon.DEFAULT.id(), null, DshPortMode.AUTO, 0, 0L);
    }

    /// Writes a session directory and its projection-cache row.
    ///
    /// @param home          the `DSH_HOME`
    /// @param formatVersion the log format version
    /// @param title         the title to record in the cache, or `null` for none
    /// @throws IOException when the fixture cannot be written
    private static void writeSession(Path home, int formatVersion, String title) throws IOException {
        writeSession(home, SESSION_ID, formatVersion, title);
    }

    /// Writes a session directory and its projection-cache row under a given id.
    ///
    /// @param home          the `DSH_HOME`
    /// @param sessionId     the session id
    /// @param formatVersion the log format version
    /// @param title         the title to record in the cache, or `null` for none
    /// @throws IOException when the fixture cannot be written
    private static void writeSession(Path home, String sessionId, int formatVersion, String title)
            throws IOException {
        Path directory = home.resolve("sessions").resolve(SLUG).resolve(sessionId);
        Files.createDirectories(directory);
        Files.writeString(directory.resolve("session.v" + formatVersion + ".jsonl.zstd"), "fixture-log");
        Files.writeString(directory.resolve("session.lock"), "");

        if (title != null) {
            Path cache = home.resolve("storages").resolve("session_projcache").resolve("sessions")
                    .resolve(sessionId + ".json");
            Files.createDirectories(cache.getParent());
            Files.writeString(cache, """
                    {"version":7,"record":{"identity":{"formatVersion":%d,"createdAt":1700000000000,
                    "cwd":"/home/user/project"},"rows":{"title":{"ver":1,"seq":1,"val":"%s"}}}}
                    """.formatted(formatVersion, title));
        }
    }

    @Test
    void listsASessionWithItsTitleAndFormat(@TempDir Path home) throws Exception {
        writeSession(home, 3, "Refactor the parser");

        List<DshSession> sessions = DshSessions.list(home);

        assertEquals(1, sessions.size());
        DshSession session = sessions.get(0);
        assertEquals(SESSION_ID, session.id());
        assertEquals(3, session.formatVersion());
        assertEquals("Refactor the parser", session.label());
        assertEquals("/home/user/project", session.workingDirectory());
        assertEquals(SLUG, session.workspaceSlug());
    }

    @Test
    void aSessionWithoutACacheRowStillLists(@TempDir Path home) throws Exception {
        writeSession(home, 3, null);

        List<DshSession> sessions = DshSessions.list(home);
        assertEquals(1, sessions.size());
        assertEquals("11111111", sessions.get(0).label(),
                "an unprojected session falls back to a shortened id");
    }

    @Test
    void migrateMovesTheLogAndTheCache(@TempDir Path root) throws Exception {
        Path sourceHome = Files.createDirectories(root.resolve("source"));
        Path targetHome = Files.createDirectories(root.resolve("target"));
        writeSession(sourceHome, 3, "Keep me");

        DshInstance source = instance("source", sourceHome, "0.1.5");
        DshInstance target = instance("target", targetHome, "0.1.5");
        DshSession session = DshSessions.list(sourceHome).get(0);

        DshSessions.migrate(source, session, target, true);

        assertFalse(Files.exists(session.directory()), "a move must clear the source");
        Path migrated = targetHome.resolve("sessions").resolve(SLUG).resolve(SESSION_ID);
        assertTrue(Files.isRegularFile(migrated.resolve("session.v3.jsonl.zstd")));
        assertFalse(Files.exists(migrated.resolve("session.lock")),
                "the source process's lock must not travel with the session");

        List<DshSession> listed = DshSessions.list(targetHome);
        assertEquals(1, listed.size());
        assertEquals("Keep me", listed.get(0).label(), "the cache row must travel too");
    }

    @Test
    void copyingLeavesTheOriginalInPlace(@TempDir Path root) throws Exception {
        Path sourceHome = Files.createDirectories(root.resolve("source"));
        Path targetHome = Files.createDirectories(root.resolve("target"));
        writeSession(sourceHome, 3, "Both sides");

        DshInstance source = instance("source", sourceHome, "0.1.5");
        DshInstance target = instance("target", targetHome, "0.1.5");
        DshSession session = DshSessions.list(sourceHome).get(0);

        DshSessions.migrate(source, session, target, false);

        assertTrue(Files.exists(session.directory()));
        assertEquals(1, DshSessions.list(targetHome).size());
    }

    @Test
    void aTargetThatCannotReadTheFormatIsRefused(@TempDir Path root) throws Exception {
        Path sourceHome = Files.createDirectories(root.resolve("source"));
        Path targetHome = Files.createDirectories(root.resolve("target"));
        writeSession(sourceHome, 4, "Newer format");
        // A different id, so the format check is what is under test rather than
        // the separate same-id collision check.
        writeSession(targetHome, "99999999-8888-7777-6666-555555555555", 3, "Older");

        DshInstance source = instance("source", sourceHome, "0.2.0");
        DshInstance target = instance("target", targetHome, "0.1.5");
        DshSession session = DshSessions.list(sourceHome).get(0);

        String refusal = DshSessions.refusalReason(source, session, target);
        assertNotNull(refusal);
        assertTrue(refusal.contains("v4"), refusal);
    }

    @Test
    void anUnknownTargetFormatIsRefusedAcrossVersions(@TempDir Path root) throws Exception {
        Path sourceHome = Files.createDirectories(root.resolve("source"));
        Path targetHome = Files.createDirectories(root.resolve("target"));
        writeSession(sourceHome, 3, "Mine");

        DshInstance source = instance("source", sourceHome, "0.1.5");
        DshInstance target = instance("target", targetHome, "0.2.0");
        DshSession session = DshSessions.list(sourceHome).get(0);

        assertNotNull(DshSessions.refusalReason(source, session, target),
                "an empty target cannot prove it reads this format, so differing versions are refused");
    }

    @Test
    void anEmptyTargetOnTheSameVersionIsAllowed(@TempDir Path root) throws Exception {
        Path sourceHome = Files.createDirectories(root.resolve("source"));
        Path targetHome = Files.createDirectories(root.resolve("target"));
        writeSession(sourceHome, 3, "Mine");

        DshInstance source = instance("source", sourceHome, "0.1.5");
        DshInstance target = instance("target", targetHome, "0.1.5");
        DshSession session = DshSessions.list(sourceHome).get(0);

        assertNull(DshSessions.refusalReason(source, session, target));
    }

    @Test
    void anExistingSessionWithTheSameIdIsRefused(@TempDir Path root) throws Exception {
        Path sourceHome = Files.createDirectories(root.resolve("source"));
        Path targetHome = Files.createDirectories(root.resolve("target"));
        writeSession(sourceHome, 3, "Mine");
        writeSession(targetHome, SESSION_ID, 3, "Theirs");

        DshInstance source = instance("source", sourceHome, "0.1.5");
        DshInstance target = instance("target", targetHome, "0.1.5");
        DshSession session = DshSessions.list(sourceHome).get(0);

        assertNotNull(DshSessions.refusalReason(source, session, target));
    }
}
