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

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class McModsTest {

    private static void respond(com.sun.net.httpserver.HttpExchange exchange, byte[] body)
            throws java.io.IOException {
        exchange.sendResponseHeaders(200, body.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(body);
        }
    }

    private static byte[] jarBytes(String descriptor, String content) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
            if (descriptor != null && !descriptor.equals("plugin.yml")) {
                zip.putNextEntry(new ZipEntry(descriptor));
                zip.write(content.getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
            }
            zip.putNextEntry(new ZipEntry("plugin.yml"));
            zip.write("name: test".getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }
        return bytes.toByteArray();
    }

    @Test
    void listsTogglesAndDeletesFiles(@TempDir Path dir) throws Exception {
        Path instance = dir.resolve("instance");
        Path plugins = instance.resolve("plugins");
        Files.createDirectories(plugins);
        Files.write(plugins.resolve("a.jar"), jarBytes("plugin.yml", "name: a"));
        Files.write(plugins.resolve("b.jar.disabled"), jarBytes("plugin.yml", "name: b"));

        McMods mods = new McMods(URI.create("http://127.0.0.1:1"));
        List<McMods.Entry> entries = mods.list(instance, "plugins");
        assertEquals(2, entries.size());
        assertEquals("a.jar", entries.get(0).name());
        assertTrue(entries.get(0).enabled());
        assertFalse(entries.get(1).enabled());

        assertEquals("a.jar.disabled", mods.toggle(instance, "plugins", "a.jar", false));
        assertTrue(Files.isRegularFile(plugins.resolve("a.jar.disabled")));
        assertEquals("a.jar", mods.toggle(instance, "plugins", "a.jar.disabled", true));
        assertTrue(Files.isRegularFile(plugins.resolve("a.jar")));

        mods.delete(instance, "plugins", "b.jar.disabled");
        assertEquals(1, mods.list(instance, "plugins").size());

        // Traversal and wrong extensions are refused before touching the disk.
        assertThrows(McException.class, () -> mods.delete(instance, "plugins", "../server.jar"));
        assertThrows(McException.class, () -> mods.list(instance, ".."));
    }

    @Test
    void uploadsAJarAndRefusesToOverwrite(@TempDir Path dir) throws Exception {
        Path instance = dir.resolve("instance");
        Files.createDirectories(instance);
        McMods mods = new McMods(URI.create("http://127.0.0.1:1"));
        byte[] jar = jarBytes(null, "");
        assertEquals("x.jar", mods.upload(instance, "mods", "x.jar", new ByteArrayInputStream(jar)));
        assertTrue(Files.isRegularFile(instance.resolve("mods").resolve("x.jar")));
        assertThrows(McException.class,
                () -> mods.upload(instance, "mods", "x.jar", new ByteArrayInputStream(jar)));
        assertThrows(McException.class,
                () -> mods.upload(instance, "mods", "notajar.txt", new ByteArrayInputStream(jar)));
        assertThrows(McException.class,
                () -> mods.upload(instance, "mods", "x.zip", new ByteArrayInputStream(jar)));
    }

    @Test
    void aFabricJarThatSaysClientIsReportedAsClientOnly(@TempDir Path dir) throws Exception {
        Path instance = dir.resolve("instance");
        Path mods = instance.resolve("mods");
        Files.createDirectories(mods);
        Files.write(mods.resolve("client.jar"), jarBytes("fabric.mod.json",
                "{\"environment\":{\"client\":\"required\",\"server\":\"unsupported\"}}"));
        Files.write(mods.resolve("both.jar"), jarBytes("fabric.mod.json",
                "{\"environment\":{\"client\":\"optional\",\"server\":\"optional\"}}"));
        McMods catalog = new McMods(URI.create("http://127.0.0.1:1"));
        List<McMods.Entry> entries = catalog.list(instance, "mods");
        boolean clientOnly = entries.stream()
                .filter(entry -> entry.name().equals("client.jar"))
                .findFirst().orElseThrow().clientOnly();
        boolean bothSides = entries.stream()
                .filter(entry -> entry.name().equals("both.jar"))
                .findFirst().orElseThrow().clientOnly();
        assertTrue(clientOnly);
        assertFalse(bothSides);
    }

    @Test
    void searchesAndInstallsFromModrinthWithItsHash(@TempDir Path dir) throws Exception {
        byte[] jar = jarBytes("fabric.mod.json", "{}");
        String sha512 = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-512").digest(jar));
        HttpServer api = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        String base = "http://127.0.0.1:" + api.getAddress().getPort();
        api.createContext("/search", exchange -> respond(exchange, """
                {"hits":[{"project_id":"P1","slug":"sodium","title":"Sodium",
                  "author":"jellysquid3","downloads":123,"description":"fast"}]}
                """.getBytes(StandardCharsets.UTF_8)));
        api.createContext("/project/P1/version", exchange -> respond(exchange, ("""
                [{"files":[{"filename":"sodium.jar","url":"%s/files/sodium.jar",
                  "hashes":{"sha512":"%s"}}]}]
                """.formatted(base, sha512)).getBytes(StandardCharsets.UTF_8)));
        api.createContext("/files/sodium.jar", exchange -> respond(exchange, jar));
        api.start();
        try {
            McMods mods = new McMods(URI.create(base));
            McMods.Folder folder = McMods.folderOf("fabric");
            assertEquals("mods", folder.kind());
            List<McMods.Hit> hits = mods.search("sodium", folder, "1.21.4").hits();
            assertEquals(1, hits.size());
            assertEquals("Sodium", hits.get(0).title());

            Path instance = dir.resolve("instance");
            String stored = mods.install(instance, folder, "P1", "1.21.4");
            assertEquals("sodium.jar", stored);
            assertTrue(Files.isRegularFile(instance.resolve("mods").resolve("sodium.jar")));
        } finally {
            api.stop(0);
        }
    }

    @Test
    void aTamperedModDownloadIsRejected(@TempDir Path dir) throws Exception {
        byte[] jar = jarBytes(null, "");
        HttpServer api = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        String base = "http://127.0.0.1:" + api.getAddress().getPort();
        api.createContext("/project/P1/version", exchange -> respond(exchange, ("""
                [{"files":[{"filename":"bad.jar","url":"%s/files/bad.jar",
                  "hashes":{"sha512":"%s"}}]}]
                """.formatted(base, "00".repeat(64))).getBytes(StandardCharsets.UTF_8)));
        api.createContext("/files/bad.jar", exchange -> respond(exchange, jar));
        api.start();
        try {
            McMods mods = new McMods(URI.create(base));
            Path instance = dir.resolve("instance");
            McException failure = assertThrows(McException.class,
                    () -> mods.install(instance, McMods.folderOf("fabric"), "P1", "1.21.4"));
            assertTrue(failure.getMessage().contains("SHA-512"), failure.getMessage());
            assertFalse(Files.exists(instance.resolve("mods").resolve("bad.jar")));
        } finally {
            api.stop(0);
        }
    }

    @Test
    void folderFollowsTheCore() {
        assertEquals("plugins", McMods.folderOf("paper").kind());
        assertEquals("plugins", McMods.folderOf("purpur").kind());
        assertEquals("mods", McMods.folderOf("fabric").kind());
        assertEquals("mods", McMods.folderOf("forge").kind());
        assertEquals("mods", McMods.folderOf("neoforge").kind());
        assertEquals(null, McMods.folderOf("vanilla"));
    }
}
