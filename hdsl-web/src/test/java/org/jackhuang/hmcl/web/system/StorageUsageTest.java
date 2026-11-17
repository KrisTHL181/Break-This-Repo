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
package org.jackhuang.hmcl.web.system;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The data directory walk: buckets by top-level name, known labels, and a
/// filesystem line that does not pretend to be zero when it cannot be read.
class StorageUsageTest {

    @TempDir
    Path temp;

    private void write(Path root, String relative, int bytes) throws Exception {
        Path file = root.resolve(relative);
        Files.createDirectories(file.getParent());
        Files.write(file, new byte[bytes]);
    }

    @Test
    void bucketsAreTheTopLevelDirectoriesWithFriendlyLabels() throws Exception {
        Path data = temp.resolve("data");
        write(data, "brands/releases/0.4.6/app.bin", 100);
        write(data, "brands/instances/abc/home/config.json", 20);
        write(data, "logs/hdsl-web.log", 7);
        write(data, "staging/unknown.bin", 3);
        // A file at the top level is not a bucket, and must not be counted.
        Files.write(data.resolve("server.yaml"), new byte[500]);

        StorageUsage.Report report = StorageUsage.compute(data);

        assertEquals(data.toAbsolutePath().normalize().toString(), report.dataDir());
        assertEquals(130, report.totalBytes(), "120 brands + 7 logs + 3 staging");
        assertEquals(3, report.buckets().size());
        assertEquals("brands", report.buckets().get(0).name(), "sorted by size, largest first");
        assertEquals(120, report.buckets().get(0).bytes());
        assertEquals("品牌实例与版本", report.buckets().get(0).label());
        assertEquals("日志", report.buckets().get(1).label());
        assertEquals("staging", report.buckets().get(2).label(), "an unknown directory keeps its own name");
        assertTrue(report.mount().totalBytes() > 0);
        assertTrue(Instant.parse(report.computedAt()).isBefore(Instant.now().plusSeconds(5)));
    }

    @Test
    void aMissingDirectoryReportsZeroInsteadOfFailing() {
        StorageUsage.Report report = StorageUsage.compute(temp.resolve("nope"));
        assertEquals(0, report.totalBytes());
        assertTrue(report.buckets().isEmpty());
    }
}
