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
package org.jackhuang.hmcl.util.logging;

import org.jackhuang.hmcl.util.PosixPermissions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Method;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/// The raw app log is created owner-only, like the audit trail: it holds user
/// names, source addresses, instance names and proxied URLs, and the process
/// umask must not decide who on the host gets to read that. Asking for a mode
/// the filesystem cannot express must not cost the process its log either.
///
/// The same holds for the `.xz` the raw log is turned into at shutdown — that is
/// the file which survives and which a log download hands out.
class LoggerPermissionsTest {

    @Test
    void theLogFileIsOwnerOnlyWherePosixExists(@TempDir Path logFolder) throws Exception {
        assumeTrue(FileSystems.getDefault().supportedFileAttributeViews().contains("posix"),
                "POSIX permission views unavailable on this filesystem");

        Logger logger = new Logger();
        try {
            logger.start(logFolder);
            Path file = logger.getLogFile();
            assertNotNull(file, "the log file must be created");
            PosixPermissions.assertOwnerOnly(file, "the app log must not be readable by other accounts");
        } finally {
            logger.shutdown();
        }
    }

    @Test
    void theArchivedLogIsOwnerOnlyToo(@TempDir Path logFolder) throws Exception {
        assumeTrue(FileSystems.getDefault().supportedFileAttributeViews().contains("posix"),
                "POSIX permission views unavailable on this filesystem");

        Logger logger = new Logger();
        logger.start(logFolder);
        Path raw = logger.getLogFile();
        assertNotNull(raw, "the log file must be created");
        logger.info("a line worth a log line");
        logger.shutdown();
        // The compression is the JVM's shutdown hook, which a test cannot
        // trigger, so the hook's own method is called directly. It is what
        // deletes the raw log, which makes the archive the file that survives.
        Method onExit = Logger.class.getDeclaredMethod("onExit");
        onExit.setAccessible(true);
        onExit.invoke(logger);

        Path archived = raw.resolveSibling(raw.getFileName() + ".xz");
        assertTrue(Files.size(archived) > 0, "the archive holds the log: " + archived);
        PosixPermissions.assertOwnerOnly(archived,
                "the archived log holds the same content as the raw one");
    }

    @Test
    void aFilesystemWithoutPosixPermissionsStillGetsItsLog() throws Exception {
        // A zip filesystem has no POSIX attribute view, so it stands in for
        // Windows here: asking for 0600 must skip the tightening rather than
        // fail the start and leave the process with no log at all — the same
        // catch AuditLog makes for the audit trail.
        //
        // The filesystem is left open on purpose (the logger holds the file
        // until the JVM exit hook compresses it), and a zip entry only becomes
        // visible once its writer is closed, which the logger deliberately does
        // not do yet. So what is asserted is that start() accepted the path and
        // opened a file inside this filesystem, not that the entry could be
        // listed.
        Path zip = Files.createTempDirectory("hdsl-log-noposix").resolve("logs.zip");
        FileSystem noPosix = FileSystems.newFileSystem(
                URI.create("jar:" + zip.toUri()), Map.of("create", "true"));
        assertFalse(noPosix.supportedFileAttributeViews().contains("posix"),
                "the stand-in filesystem must have no POSIX view to begin with");

        Logger logger = new Logger();
        try {
            logger.start(noPosix.getPath("/logs"));
            Path file = logger.getLogFile();
            assertNotNull(file, "the log file must be opened even where permissions cannot be set");
            assertEquals(noPosix, file.getFileSystem(), "the log was opened in that filesystem");
        } finally {
            logger.shutdown();
        }
    }
}