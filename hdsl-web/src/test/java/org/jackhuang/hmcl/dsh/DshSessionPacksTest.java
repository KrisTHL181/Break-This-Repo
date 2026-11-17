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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Verifies that a pack of sessions arrives intact, and that a pack which would
/// stop the harness from starting is refused.
///
/// Three rules decide whether the harness can read a session where it is put, and
/// all three fail by refusing to start rather than by degrading: the directory a
/// log sits in has to be the one its own working directory identifies, the log's
/// compression has to match what the home is configured for, and a log's first
/// compressed frame has to be the header line by itself. This launcher has no
/// decompressor, so the only way to satisfy all three is to copy the bytes — which
/// is what these tests are about.
class DshSessionPacksTest {
    /// The instance sessions are exported from.
    private static final String SOURCE_ID = "pack-source";

    /// The instance a pack is read into.
    private static final String TARGET_ID = "pack-target";

    /// Removes the instances the tests made.
    @AfterEach
    void removeInstances() {
        for (String id : List.of(SOURCE_ID, TARGET_ID)) {
            try {
                if (DshInstanceManager.find(id) != null) {
                    DshInstanceManager.delete(id);
                }
            } catch (Exception ignored) {
                // The test's own cleanup: a failure here would hide the real one.
            }
        }
    }

    @Test
    void aPackCarriesTheLogsByteForByte() throws Exception {
        DshInstance source = makeInstance(SOURCE_ID);
        byte[] log = logBytes();
        Path session = sessionDirectory(source, "--tmp-project--", "11111111-2222-3333-4444-555555555555");
        Files.write(session.resolve("session.v3.jsonl.zstd"), log);
        // A plugin's own copy of a log lives in the same directory and is not the
        // session; a pack must not carry it, and must not mistake it for the log.
        Files.writeString(session.resolve("session.v3.jsonl.zstd.cost-meter-backup-1-2"), "backup");
        Files.createDirectories(source.homeDirectory().resolve("storages")
                .resolve("session_projcache").resolve("sessions"));
        Files.writeString(source.homeDirectory().resolve("storages").resolve("session_projcache")
                .resolve("sessions").resolve("11111111-2222-3333-4444-555555555555.json"), "{\"version\":7}");

        Path pack = Files.createTempFile("sessions", ".zip");
        List<DshSession> sessions = DshSessions.list(source.homeDirectory());
        DshSessionPacks.ExportResult result = DshSessionPacks.export(source, sessions, pack, null);

        assertEquals(1, result.sessions());
        try (ZipFile zip = new ZipFile(pack.toFile())) {
            ZipEntry entry = zip.getEntry("sessions/--tmp-project--/11111111-2222-3333-4444-555555555555/session.v3.jsonl.zstd");
            assertTrue(entry != null, "the log is in the pack under the directory it came from");
            org.junit.jupiter.api.Assertions.assertArrayEquals(log, zip.getInputStream(entry).readAllBytes());
            assertTrue(zip.getEntry("sessions/--tmp-project--/11111111-2222-3333-4444-555555555/session.v3.jsonl.zstd.cost-meter-backup-1-2") == null,
                    "a plugin's copy is not part of the session");
            assertTrue(zip.getEntry("projcache/sessions/11111111-2222-3333-4444-555555555555.json") != null,
                    "the title the conversation is listed under travels with it");

            DshSessionPacks.Manifest manifest = DshSessionPacks.readManifest(pack);
            assertEquals(DshSessionPacks.FORMAT, manifest.format());
            assertEquals(SOURCE_ID, manifest.instanceId());
            assertEquals("--tmp-project--", manifest.sessions().get(0).slug());
        }

        DshInstance target = makeInstance(TARGET_ID);
        DshSessionPacks.ImportResult imported = DshSessionPacks.importFrom(target.homeDirectory(), pack, null);

        assertEquals(1, imported.imported());
        assertEquals(0, imported.skipped());
        Path arrived = target.homeDirectory().resolve("sessions").resolve("--tmp-project--")
                .resolve("11111111-2222-3333-4444-555555555555").resolve("session.v3.jsonl.zstd");
        org.junit.jupiter.api.Assertions.assertArrayEquals(log, Files.readAllBytes(arrived),
                "the log arrives exactly as it left");
        assertTrue(Files.exists(target.homeDirectory().resolve("storages").resolve("session_projcache")
                .resolve("sessions").resolve("11111111-2222-3333-4444-555555555555.json")));
        Files.deleteIfExists(pack);
    }

    @Test
    void aSessionTheTargetAlreadyHasIsLeftAlone() throws Exception {
        DshInstance source = makeInstance(SOURCE_ID);
        Path session = sessionDirectory(source, "--tmp-project--", "11111111-2222-3333-4444-555555555555");
        Files.write(session.resolve("session.v3.jsonl.zstd"), logBytes());

        Path pack = Files.createTempFile("sessions", ".zip");
        DshSessionPacks.export(source, DshSessions.list(source.homeDirectory()), pack, null);

        DshInstance target = makeInstance(TARGET_ID);
        assertTrue(DshSessionPacks.importFrom(target.homeDirectory(), pack, null).imported() == 1);
        DshSessionPacks.ImportResult again = DshSessionPacks.importFrom(target.homeDirectory(), pack, null);

        assertEquals(0, again.imported(), "a session that is already there is not written again");
        assertEquals(1, again.skipped());
        Files.deleteIfExists(pack);
    }

    @Test
    void aPackThatWouldMoveALogOutOfItsDirectoryIsRefused() throws Exception {
        // A pack whose session sits in a directory the manifest does not name is
        // the one thing that stops a home from being listed at all: the harness
        // matches a log's header against the directory it is in, and refuses the
        // whole home when they disagree. Reading such a pack would be writing that
        // failure into a working installation.
        Path pack = Files.createTempFile("sessions", ".zip");
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(pack))) {
            zip.putNextEntry(new ZipEntry(DshSessionPacks.MANIFEST));
            zip.write(("""
                    {"format":"hdsl-session-pack","version":1,"createdAt":"now","instanceId":"x",
                     "dshVersion":"0.1.6","sessions":[
                       {"id":"11111111-2222-3333-4444-555555555555","slug":"--tmp-a--","formatVersion":3}]}
                    """).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            zip.closeEntry();
            zip.putNextEntry(new ZipEntry("sessions/--tmp-b--/11111111-2222-3333-4444-555555555555/session.v3.jsonl.zstd"));
            zip.write(logBytes());
            zip.closeEntry();
        }

        DshInstance target = makeInstance(TARGET_ID);
        DshSessionPacks.ImportResult result = DshSessionPacks.importFrom(target.homeDirectory(), pack, null);

        assertEquals(0, result.imported(), "a log is never written into a directory the pack did not name");
        assertFalse(Files.exists(target.homeDirectory().resolve("sessions").resolve("--tmp-b--")),
                "nothing was created for the session the manifest did not describe");
        Files.deleteIfExists(pack);
    }

    @Test
    void aPackHoldingSomethingOtherThanALogIsRefused() throws Exception {
        Path pack = Files.createTempFile("sessions", ".zip");
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(pack))) {
            zip.putNextEntry(new ZipEntry(DshSessionPacks.MANIFEST));
            zip.write("""
                    {"format":"hdsl-session-pack","version":1,"createdAt":"now","instanceId":"x",
                     "dshVersion":"0.1.6","sessions":[
                       {"id":"11111111-2222-3333-4444-555555555555","slug":"--tmp-a--","formatVersion":3}]}
                    """.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            zip.closeEntry();
            // An uncompressed log is enough to stop a home that compresses from
            // starting, and a name that is not a log name at all is ignored by the
            // harness but has no business in a session directory.
            zip.putNextEntry(new ZipEntry("sessions/--tmp-a--/11111111-2222-3333-4444-555555555555/session.v3.jsonl"));
            zip.write(logBytes());
            zip.closeEntry();
        }

        DshInstance target = makeInstance(TARGET_ID);
        DshSessionPacks.ImportResult result = DshSessionPacks.importFrom(target.homeDirectory(), pack, null);

        assertEquals(0, result.imported());
        assertFalse(Files.exists(target.homeDirectory().resolve("sessions").resolve("--tmp-a--")),
                "an uncompressed log is not written into a compressing home");
        Files.deleteIfExists(pack);
    }

    @Test
    void anArchiveThatClimbsOutOfTheSessionsDirectoryIsRefused() throws Exception {
        // `restoreInto` is the path a modpack takes: it reads every session entry an archive holds
        // rather than a list a manifest names, so nothing else stands between a member's name and
        // the `resolve` that writes it. `sessions/../planted/session.v3.jsonl` passes the prefix
        // test and is a legal path, so without a check the log lands in the home itself —
        // anywhere the archive says, including outside the sessions directory.
        DshInstance target = makeInstance(TARGET_ID);
        // A home that already holds an uncompressed log reads uncompressed logs, so the member's
        // name is not what refuses it: the path is.
        Path existing = sessionDirectory(target, "--tmp-existing--",
                "99999999-8888-7777-6666-555555555555");
        Files.write(existing.resolve("session.v3.jsonl"), logBytes());

        Path archive = Files.createTempFile("archive", ".zip");
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(archive))) {
            zip.putNextEntry(new ZipEntry("sessions/../planted/session.v3.jsonl"));
            zip.write(logBytes());
            zip.closeEntry();
        }

        DshSessionPacks.restoreInto(archive, target.homeDirectory(), null);

        assertFalse(Files.exists(target.homeDirectory().resolve("planted")),
                "a member that climbs out of the sessions directory is not written");
        Files.deleteIfExists(archive);
    }

    @Test
    void somethingThatIsNotAPackIsRefused() throws Exception {
        Path notAPack = Files.createTempFile("sessions", ".zip");
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(notAPack))) {
            zip.putNextEntry(new ZipEntry("readme.txt"));
            zip.write("not a pack".getBytes(java.nio.charset.StandardCharsets.UTF_8));
            zip.closeEntry();
        }

        DshInstance target = makeInstance(TARGET_ID);
        assertThrows(DshException.class,
                () -> DshSessionPacks.importFrom(target.homeDirectory(), notAPack, null));
        Files.deleteIfExists(notAPack);
    }

    /// Returns bytes standing in for a compressed log.
    ///
    /// The content is never interpreted: the point of a pack is that it copies
    /// what is there, and the launcher has no decompressor to interpret it with.
    ///
    /// @return the bytes
    private static byte[] logBytes() {
        byte[] bytes = new byte[512];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) (i * 31);
        }
        return bytes;
    }

    /// Creates an instance whose home the tests can fill.
    ///
    /// @param id the instance id
    /// @return the instance
    private DshInstance makeInstance(String id) throws Exception {
        Path source = Files.createTempDirectory("pack-home");
        return DshInstanceManager.create(id, "0.1.6-alpha.2", DshInstance.DEFAULT_PROFILE,
                source, DshHomeMode.ISOLATED, null, List.of(), Map.of());
    }

    /// Creates a session directory inside an instance's home.
    ///
    /// @param instance  the instance
    /// @param slug      the project directory
    /// @param sessionId the session id
    /// @return the session directory
    private static Path sessionDirectory(DshInstance instance, String slug, String sessionId) throws Exception {
        Path directory = instance.homeDirectory().resolve("sessions").resolve(slug).resolve(sessionId);
        Files.createDirectories(directory);
        return directory;
    }

}
