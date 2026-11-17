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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class McBackupsTest {

    private static McInstanceManager manager(Path dir) {
        return new McInstanceManager(dir.resolve("minecraft").resolve("instances"));
    }

    private static McInstance instance(Path dir, McInstanceManager manager) throws Exception {
        McInstance instance = manager.create("survival", "1.21.4", 25565, 25575, 1024, "world", null);
        Path world = manager.worldDirectory(instance);
        Files.createDirectories(world);
        Files.writeString(world.resolve("level.dat"), "original");
        Files.writeString(world.resolve("region.mca"), "region");
        return instance;
    }

    @Test
    void createListAndRestore(@TempDir Path dir) throws Exception {
        McInstanceManager manager = manager(dir);
        McInstance instance = instance(dir, manager);
        McBackups backups = new McBackups(dir.resolve("minecraft").resolve("backups"));

        McBackups.Backup backup = backups.create(instance, manager);
        assertTrue(backup.name().endsWith(".zip"));
        assertEquals(1, backups.list(instance.id()).size());

        Files.writeString(manager.worldDirectory(instance).resolve("level.dat"), "changed");
        McBackups.Backup safety = backups.restore(instance, manager, backup.name());

        assertEquals("original", Files.readString(manager.worldDirectory(instance).resolve("level.dat")));
        assertNotNull(safety);
        assertEquals(2, backups.list(instance.id()).size(), "restore takes a safety snapshot first");
    }

    @Test
    void aZipThatTriesToLeaveTheInstanceIsIgnored(@TempDir Path dir) throws Exception {
        McInstanceManager manager = manager(dir);
        McInstance instance = instance(dir, manager);
        McBackups backups = new McBackups(dir.resolve("minecraft").resolve("backups"));
        Path instanceBackups = dir.resolve("minecraft").resolve("backups").resolve(instance.id());
        Files.createDirectories(instanceBackups);
        Path evilZip = instanceBackups.resolve("20261030-120000.zip");
        try (OutputStream out = Files.newOutputStream(evilZip);
             ZipOutputStream zip = new ZipOutputStream(out)) {
            zip.putNextEntry(new ZipEntry("../escaped.txt"));
            zip.write("nope".getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }

        backups.restore(instance, manager, evilZip.getFileName().toString());
        assertFalse(Files.exists(dir.resolve("minecraft").resolve("escaped.txt")));
        assertFalse(Files.exists(dir.resolve("escaped.txt")));
    }

    @Test
    void deleteRemovesOneSnapshot(@TempDir Path dir) throws Exception {
        McInstanceManager manager = manager(dir);
        McInstance instance = instance(dir, manager);
        McBackups backups = new McBackups(dir.resolve("minecraft").resolve("backups"));
        McBackups.Backup backup = backups.create(instance, manager);

        backups.delete(instance.id(), backup.name());
        assertEquals(0, backups.list(instance.id()).size());
    }
}