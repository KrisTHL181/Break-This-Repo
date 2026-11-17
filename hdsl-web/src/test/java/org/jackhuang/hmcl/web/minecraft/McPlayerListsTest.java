/*
 * HDSL-web
 * Copyright (C) 2026  HDSL-web Contributors
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class McPlayerListsTest {

    private static McInstance instance(Path dir, McInstanceManager manager) throws Exception {
        return manager.create("survival", "1.21.4", 25565, 25575, 1024, "world", null);
    }

    private static byte[] zip(Map<String, String> files) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(out)) {
            for (Map.Entry<String, String> entry : files.entrySet()) {
                zip.putNextEntry(new ZipEntry(entry.getKey()));
                zip.write(entry.getValue().getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
            }
        }
        return out.toByteArray();
    }

    @Test
    void importsTheListsTheZipCarriesAndCountsThem(@TempDir Path dir) throws Exception {
        McInstanceManager manager = new McInstanceManager(dir.resolve("instances"));
        McInstance instance = instance(dir, manager);

        byte[] archive = zip(Map.of(
                "whitelist.json", "[{\"name\":\"alice\",\"uuid\":\"a\"},{\"name\":\"bob\",\"uuid\":\"b\"}]",
                "ops.json", "[{\"name\":\"alice\",\"uuid\":\"a\",\"level\":4}]"));

        Map<String, Integer> counts = McPlayerLists.importZip(instance, manager,
                new ByteArrayInputStream(archive));

        assertEquals(2, counts.get("whitelist.json"));
        assertEquals(1, counts.get("ops.json"));
        Path instanceDir = manager.instanceDirectory(instance.id());
        assertTrue(Files.readString(instanceDir.resolve("whitelist.json")).contains("alice"));
        assertTrue(Files.readString(instanceDir.resolve("ops.json")).contains("level"));
    }

    @Test
    void entriesThatAreNotPlayerListsAreIgnored(@TempDir Path dir) throws Exception {
        McInstanceManager manager = new McInstanceManager(dir.resolve("instances"));
        McInstance instance = instance(dir, manager);

        byte[] archive = zip(Map.of(
                "whitelist.json", "[]",
                "server.jar", "pretend this is a jar"));

        McPlayerLists.importZip(instance, manager, new ByteArrayInputStream(archive));

        Path instanceDir = manager.instanceDirectory(instance.id());
        assertTrue(Files.isRegularFile(instanceDir.resolve("whitelist.json")));
        assertFalse(Files.exists(instanceDir.resolve("server.jar")),
                "a zip of a server root must not smuggle a jar in");
    }

    @Test
    void aNestedEntryIsRefused(@TempDir Path dir) throws Exception {
        McInstanceManager manager = new McInstanceManager(dir.resolve("instances"));
        McInstance instance = instance(dir, manager);

        byte[] archive = zip(Map.of("some-dir/whitelist.json", "[]"));
        assertThrows(McException.class, () -> McPlayerLists.importZip(instance, manager,
                new ByteArrayInputStream(archive)));
    }

    @Test
    void aListThatIsNotAJsonArrayIsRefusedAndTheOldOneSurvives(@TempDir Path dir) throws Exception {
        McInstanceManager manager = new McInstanceManager(dir.resolve("instances"));
        McInstance instance = instance(dir, manager);
        Path whitelist = manager.instanceDirectory(instance.id()).resolve("whitelist.json");
        Files.writeString(whitelist, "[]");

        byte[] archive = zip(Map.of("whitelist.json", "{\"not\":\"an array\"}"));
        assertThrows(McException.class, () -> McPlayerLists.importZip(instance, manager,
                new ByteArrayInputStream(archive)));
        assertEquals("[]", Files.readString(whitelist));
    }

    @Test
    void anEmptyZipIsRefused(@TempDir Path dir) throws Exception {
        McInstanceManager manager = new McInstanceManager(dir.resolve("instances"));
        McInstance instance = instance(dir, manager);
        assertThrows(McException.class, () -> McPlayerLists.importZip(instance, manager,
                new ByteArrayInputStream(zip(Map.of()))));
    }
}