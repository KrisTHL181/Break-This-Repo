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

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class McWorldsTest {

    private static McInstance setup(Path dir, McInstanceManager manager) throws Exception {
        McInstance instance = manager.create("survival", "1.21.4", 25565, 25575, 1024, "world", null);
        Path world = manager.worldDirectory(instance);
        Files.createDirectories(world);
        Files.writeString(world.resolve("level.dat"), "original");
        Files.writeString(world.resolve("region.mca"), "region");
        return instance;
    }

    @Test
    void exportThenImportRestoresTheWorldAndKeepsASafetyBackup(@TempDir Path dir) throws Exception {
        McInstanceManager manager = new McInstanceManager(dir.resolve("instances"));
        McInstance instance = setup(dir, manager);
        McBackups backups = new McBackups(dir.resolve("backups"));

        ByteArrayOutputStream exported = new ByteArrayOutputStream();
        McWorlds.export(instance, manager, exported);
        assertTrue(exported.size() > 0);

        Files.writeString(manager.worldDirectory(instance).resolve("level.dat"), "changed");
        String safety = McWorlds.importZip(instance, manager, backups,
                new ByteArrayInputStream(exported.toByteArray()));

        assertNotNull(safety);
        assertEquals("original", Files.readString(manager.worldDirectory(instance).resolve("level.dat")));
        assertEquals(1, backups.list(instance.id()).size(), "the safety snapshot is kept");
    }

    @Test
    void aFlatZipIsAccepted(@TempDir Path dir) throws Exception {
        McInstanceManager manager = new McInstanceManager(dir.resolve("instances"));
        McInstance instance = setup(dir, manager);
        McBackups backups = new McBackups(dir.resolve("backups"));

        ByteArrayOutputStream flat = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(flat)) {
            zip.putNextEntry(new ZipEntry("level.dat"));
            zip.write("flat".getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }
        McWorlds.importZip(instance, manager, backups, new ByteArrayInputStream(flat.toByteArray()));
        assertEquals("flat", Files.readString(manager.worldDirectory(instance).resolve("level.dat")));
    }

    @Test
    void aWorldZippedFromInsideItsDirectoryIsAccepted(@TempDir Path dir) throws Exception {
        McInstanceManager manager = new McInstanceManager(dir.resolve("instances"));
        McInstance instance = setup(dir, manager);
        McBackups backups = new McBackups(dir.resolve("backups"));

        ByteArrayOutputStream contents = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(contents)) {
            zip.putNextEntry(new ZipEntry("level.dat"));
            zip.write("inside".getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
            zip.putNextEntry(new ZipEntry("region/r.0.0.mca"));
            zip.write(new byte[] {1, 2, 3});
            zip.closeEntry();
        }
        McWorlds.importZip(instance, manager, backups, new ByteArrayInputStream(contents.toByteArray()));
        Path world = manager.worldDirectory(instance);
        assertEquals("inside", Files.readString(world.resolve("level.dat")));
        assertTrue(Files.isRegularFile(world.resolve("region/r.0.0.mca")));
    }

    @Test
    void anUnsafeEntryIsRefusedAndTheWorldIsUntouched(@TempDir Path dir) throws Exception {
        McInstanceManager manager = new McInstanceManager(dir.resolve("instances"));
        McInstance instance = setup(dir, manager);
        McBackups backups = new McBackups(dir.resolve("backups"));

        ByteArrayOutputStream evil = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(evil)) {
            zip.putNextEntry(new ZipEntry("../escaped.txt"));
            zip.write("nope".getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }

        assertThrows(McException.class,
                () -> McWorlds.importZip(instance, manager, backups,
                        new ByteArrayInputStream(evil.toByteArray())));
        assertEquals("original", Files.readString(manager.worldDirectory(instance).resolve("level.dat")));
        assertFalse(Files.exists(dir.resolve("escaped.txt")));
    }

    @Test
    void anEmptyZipIsRefused(@TempDir Path dir) throws Exception {
        McInstanceManager manager = new McInstanceManager(dir.resolve("instances"));
        McInstance instance = setup(dir, manager);
        McBackups backups = new McBackups(dir.resolve("backups"));
        assertThrows(McException.class, () -> McWorlds.importZip(instance, manager, backups,
                new ByteArrayInputStream(new byte[0])));
    }

    /// A zip packed under a *different* top-level directory used to be silently
    /// nested (`world/oldworld/…`), and the server then generated a fresh world
    /// because `world/level.dat` was missing. Refusing with the name is the only
    /// outcome that does not lose the import without saying so.
    @Test
    void aZipPackedUnderAnotherDirectoryNameIsRefused(@TempDir Path dir) throws Exception {
        McInstanceManager manager = new McInstanceManager(dir.resolve("instances"));
        McInstance instance = setup(dir, manager);
        McBackups backups = new McBackups(dir.resolve("backups"));

        ByteArrayOutputStream other = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(other)) {
            zip.putNextEntry(new ZipEntry("oldworld/level.dat"));
            zip.write("data".getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }
        McException error = assertThrows(McException.class, () -> McWorlds.importZip(instance, manager, backups,
                new ByteArrayInputStream(other.toByteArray())));
        assertTrue(error.getMessage().contains("oldworld"));
        assertTrue(error.getMessage().contains("world"));
    }

    @Test
    void theExportedZipNamesEntriesUnderTheWorldDirectory(@TempDir Path dir) throws Exception {
        McInstanceManager manager = new McInstanceManager(dir.resolve("instances"));
        McInstance instance = setup(dir, manager);
        ByteArrayOutputStream exported = new ByteArrayOutputStream();
        McWorlds.export(instance, manager, exported);
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(exported.toByteArray()))) {
            ZipEntry entry = zip.getNextEntry();
            assertNotNull(entry);
            assertTrue(entry.getName().startsWith("world/"), entry.getName());
        }
    }

    /// A zip that expands past the limit is refused while unpacking, before it fills the disk.
    /// The limit is named here so the test does not need a 32 GiB archive.
    @Test
    void anArchiveThatUnpacksTooFarIsRefused(@TempDir Path dir) throws Exception {
        McInstanceManager manager = new McInstanceManager(dir.resolve("instances"));
        McInstance instance = setup(dir, manager);
        McBackups backups = new McBackups(dir.resolve("backups"));

        ByteArrayOutputStream packed = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(packed)) {
            zip.putNextEntry(new ZipEntry(instance.levelName() + "/level.dat"));
            zip.write(new byte[4096]);
            zip.closeEntry();
        }
        Path archive = dir.resolve("world.zip");
        Files.write(archive, packed.toByteArray());

        McException error = assertThrows(McException.class,
                () -> McWorlds.importArchive(instance, manager, backups, archive, null, 1024));
        assertTrue(error.getMessage().contains("unpacks to more than"), error.getMessage());
        assertEquals("original",
                Files.readString(manager.worldDirectory(instance).resolve("level.dat")),
                "a refused import must leave the old world alone");
    }

    /// Real-world sizes. A world bigger than 4 GiB is what the streaming paths and zip64 were
    /// written for; CI cannot afford it, so it runs only when asked:
    ///
    /// ```sh
    /// HDSL_TEST_LARGE_WORLD_MB=5120 ./gradlew test \
    ///   --tests '*McWorldsTest.aWorldBiggerThanFourGigabytesRoundTrips*'
    /// ```
    ///
    /// By default the region file is sparse: the test measures the zip pipeline, zip64 and
    /// progress, not the disk controller. Set `HDSL_TEST_LARGE_WORLD_RANDOM=1` for
    /// incompressible bytes, which is what a real region file is — the archive then matches the
    /// world's size, and the run takes minutes rather than seconds.
    @Test
    @Timeout(value = 120, unit = TimeUnit.MINUTES)
    void aWorldBiggerThanFourGigabytesRoundTrips(@TempDir Path dir) throws Exception {
        String requested = System.getenv("HDSL_TEST_LARGE_WORLD_MB");
        Assumptions.assumeTrue(requested != null && !requested.isBlank(),
                "set HDSL_TEST_LARGE_WORLD_MB to run the large-world test");
        long megabytes = Long.parseLong(requested.trim());
        long bytes = megabytes * 1024 * 1024;

        McInstanceManager manager = new McInstanceManager(dir.resolve("instances"));
        McInstance instance = setup(dir, manager);
        Path world = manager.worldDirectory(instance);
        Path region = world.resolve("region").resolve("r.0.0.mca");
        Files.createDirectories(region.getParent());
        boolean random = "1".equals(System.getenv("HDSL_TEST_LARGE_WORLD_RANDOM"));
        if (random) {
            // Incompressible data is what a region file looks like: the archive is as big as
            // the world, which is the case the upload and space checks were written for.
            try (var out = Files.newOutputStream(region)) {
                byte[] buffer = new byte[8 * 1024 * 1024];
                var rnd = new java.util.Random(42);
                long left = bytes;
                while (left > 0) {
                    rnd.nextBytes(buffer);
                    int chunk = (int) Math.min(buffer.length, left);
                    out.write(buffer, 0, chunk);
                    left -= chunk;
                }
            }
        } else {
            try (var channel = java.nio.channels.FileChannel.open(region,
                    java.nio.file.StandardOpenOption.CREATE_NEW,
                    java.nio.file.StandardOpenOption.WRITE)) {
                channel.position(bytes - 1);
                channel.write(java.nio.ByteBuffer.wrap(new byte[] {0}));
            }
        }
        assertEquals(bytes, Files.size(region));

        Path archive = dir.resolve("big-world.zip");
        long exportStart = System.nanoTime();
        try (var out = Files.newOutputStream(archive)) {
            McWorlds.export(instance, manager, out);
        }
        long exportMillis = (System.nanoTime() - exportStart) / 1_000_000;

        try (var zip = new java.util.zip.ZipFile(archive.toFile())) {
            boolean big = zip.stream().anyMatch(entry -> entry.getSize() >= bytes);
            assertTrue(big, "zip64 must record the entry's real size: " + archive);
        }

        McBackups backups = new McBackups(dir.resolve("backups"));
        long archiveBytes = Files.size(archive);
        long[] seen = {0};
        long[] declared = {0};
        long importStart = System.nanoTime();
        String safety = McWorlds.importArchive(instance, manager, backups, archive,
                (files, total, copied, totalBytes) -> {
                    seen[0] = copied;
                    declared[0] = totalBytes;
                });
        long importMillis = (System.nanoTime() - importStart) / 1_000_000;

        assertNotNull(safety);
        assertEquals(bytes, Files.size(manager.worldDirectory(instance).resolve("region/r.0.0.mca")));
        assertTrue(declared[0] >= bytes, "the directory's sizes drive progress: " + declared[0]);
        assertTrue(seen[0] >= bytes, "the last callback sees every byte: " + seen[0]);
        System.out.printf("large world: %d MiB, export %d ms, import %d ms, archive %d MiB%n",
                megabytes, exportMillis, importMillis, archiveBytes / (1024 * 1024));
    }
}
