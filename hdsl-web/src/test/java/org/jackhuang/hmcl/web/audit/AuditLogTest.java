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
package org.jackhuang.hmcl.web.audit;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/// [AuditLog] is append-only JSONL under `logs/`, owner-only where POSIX
/// exists, a no-op until something initialises it, and survives re-init —
/// every [org.jackhuang.hmcl.web.server.HdslServer] build re-initialises it
/// in the same JVM and must not drop what was already written.
class AuditLogTest {

    @TempDir
    Path dataDir;

    @Test
    void logBeforeInitIsANoOp() {
        assumeTrue(!AuditLog.initialized(),
                "another test in this JVM already initialised the audit log");
        AuditLog.log(AuditLog.Channel.WEB, "login", null, "success", "alice", "127.0.0.1");
        assertTrue(Files.notExists(todayFile()));
    }

    @Test
    void writesOneParseableJsonLinePerEvent() throws Exception {
        AuditLog.init(dataDir.resolve("logs"));
        AuditLog.log(AuditLog.Channel.WEB, "login", null, "success", "alice", "127.0.0.1");
        AuditLog.log(AuditLog.Channel.WEB, "instance.create", "srv", "success", "alice", null);
        AuditLog.log(AuditLog.Channel.TERMINAL, "session.open", null, "blocked", null, "10.0.0.9");

        assertTrue(Files.isRegularFile(todayFile()));
        List<String> lines = Files.readAllLines(todayFile());
        assertEquals(3, lines.size());
        for (String line : lines) {
            JsonObject event = JsonParser.parseString(line).getAsJsonObject();
            assertTrue(event.has("ts"), "every line carries ts: " + line);
            assertTrue(event.has("channel"), "every line carries channel: " + line);
            assertTrue(event.has("action"), "every line carries action: " + line);
            assertTrue(event.has("outcome"), "every line carries outcome: " + line);
        }
    }

    @Test
    void fileIsOwnerOnlyWherePosixExists() throws Exception {
        assumeTrue(FileSystems.getDefault().supportedFileAttributeViews().contains("posix"),
                "POSIX permission views unavailable on this filesystem");

        AuditLog.init(dataDir.resolve("logs"));
        AuditLog.log(AuditLog.Channel.WEB, "login", null, "success", "alice", "127.0.0.1");

        PosixFileAttributes attrs = Files.readAttributes(todayFile(), PosixFileAttributes.class);
        assertEquals(Set.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE),
                attrs.permissions());
    }

    @Test
    void reInitKeepsPreviouslyWrittenLines() throws Exception {
        Path logsDir = dataDir.resolve("logs");
        AuditLog.init(logsDir);
        AuditLog.log(AuditLog.Channel.WEB, "login", null, "success", "alice", "127.0.0.1");
        AuditLog.init(logsDir); // a second HdslServer build in the same JVM
        AuditLog.log(AuditLog.Channel.WEB, "auth.setup", null, "success", "bob", "127.0.0.1");

        List<String> lines = Files.readAllLines(todayFile());
        assertEquals(2, lines.size());
    }

    private Path todayFile() {
        return dataDir.resolve("logs").resolve("audit-" + LocalDate.now() + ".jsonl");
    }
}
