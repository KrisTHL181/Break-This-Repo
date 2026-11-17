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
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class McCoresTest {

    private static String hex(String algorithm, byte[] bytes) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance(algorithm).digest(bytes));
    }

    private static void respond(com.sun.net.httpserver.HttpExchange exchange, byte[] body)
            throws java.io.IOException {
        exchange.sendResponseHeaders(200, body.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(body);
        }
    }

    private static byte[] zipBytes(String payload) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
            zip.putNextEntry(new ZipEntry("META-INF/MANIFEST.MF"));
            zip.write(payload.getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }
        return bytes.toByteArray();
    }

    private static HttpServer fakeApi(String context, String json) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext(context, exchange -> respond(exchange,
                json.getBytes(StandardCharsets.UTF_8)));
        server.start();
        return server;
    }

    @Test
    void installsPaperAndVerifiesItsSha256(@TempDir Path dir) throws Exception {
        byte[] jar = zipBytes("paper");
        String sha256 = hex("SHA-256", jar);
        HttpServer api = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        String base = "http://127.0.0.1:" + api.getAddress().getPort();
        api.createContext("/v3/projects/paper/versions/1.21.4/builds", exchange -> respond(exchange,
                ("""
                        [{"id":13,"downloads":{"server:default":{
                          "name":"paper-1.21.4-13.jar",
                          "url":"%s/downloads/paper-1.21.4-13.jar",
                          "checksums":{"sha256":"%s"},"size":%d}}}]
                        """.formatted(base, sha256, jar.length)).getBytes(StandardCharsets.UTF_8)));
        api.createContext("/downloads/paper-1.21.4-13.jar", exchange -> respond(exchange, jar));
        api.start();
        try {
            McVersions versions = new McVersions(dir.resolve("versions"),
                    URI.create("http://127.0.0.1:1/manifest.json"));
            McCores cores = new McCores(versions, dir.resolve("cores"),
                    URI.create(base), URI.create("http://127.0.0.1:1"),
                    URI.create("http://127.0.0.1:1"));
            McInstanceManager manager = new McInstanceManager(dir.resolve("instances"));
            McInstance instance = manager.create("paper", "1.21.4", 25565, 25575, 1024, "world",
                    null, false, "paper");

            McInstance installed = cores.install(instance, manager, "1.21.4", null);
            assertEquals("paper", installed.core());
            assertArrayEquals(jar, Files.readAllBytes(manager.serverJar(installed)));
            String record = Files.readString(dir.resolve("cores").resolve("paper")
                    .resolve("1.21.4").resolve("version.json"));
            assertTrue(record.contains("sha256"), record);
            assertEquals("13", com.google.gson.JsonParser.parseString(record).getAsJsonObject()
                    .get("build").getAsString());
        } finally {
            api.stop(0);
        }
    }

    @Test
    void aCorruptedPaperDownloadIsRejected(@TempDir Path dir) throws Exception {
        byte[] jar = zipBytes("paper");
        HttpServer api = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        String base = "http://127.0.0.1:" + api.getAddress().getPort();
        api.createContext("/v3/projects/paper/versions/1.21.4/builds", exchange -> respond(exchange,
                ("""
                        [{"id":1,"downloads":{"server:default":{
                          "name":"paper.jar",
                          "url":"%s/downloads/paper.jar",
                          "checksums":{"sha256":"%s"},"size":%d}}}]
                        """.formatted(base, "00".repeat(32), jar.length))
                        .getBytes(StandardCharsets.UTF_8)));
        api.createContext("/downloads/paper.jar", exchange -> respond(exchange, jar));
        api.start();
        try {
            McVersions versions = new McVersions(dir.resolve("versions"),
                    URI.create("http://127.0.0.1:1/manifest.json"));
            McCores cores = new McCores(versions, dir.resolve("cores"),
                    URI.create(base), URI.create("http://127.0.0.1:1"),
                    URI.create("http://127.0.0.1:1"));
            McInstanceManager manager = new McInstanceManager(dir.resolve("instances"));
            McInstance instance = manager.create("paper", "1.21.4", 25565, 25575, 1024, "world",
                    null, false, "paper");
            McException failure = assertThrows(McException.class,
                    () -> cores.install(instance, manager, "1.21.4", null));
            assertTrue(failure.getMessage().contains("SHA-256"), failure.getMessage());
            assertFalse(Files.exists(dir.resolve("cores").resolve("paper")
                    .resolve("1.21.4").resolve("server.jar")));
        } finally {
            api.stop(0);
        }
    }

    @Test
    void installsPurpurAndVerifiesItsMd5(@TempDir Path dir) throws Exception {
        byte[] jar = zipBytes("purpur");
        String md5 = hex("MD5", jar);
        HttpServer api = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        String base = "http://127.0.0.1:" + api.getAddress().getPort();
        api.createContext("/v2/purpur/1.21.4", exchange -> respond(exchange,
                "{\"builds\":{\"latest\":\"2062\",\"all\":[\"2062\"]}}"
                        .getBytes(StandardCharsets.UTF_8)));
        api.createContext("/v2/purpur/1.21.4/2062", exchange -> respond(exchange,
                ("{\"md5\":\"" + md5 + "\"}").getBytes(StandardCharsets.UTF_8)));
        api.createContext("/v2/purpur/1.21.4/2062/download", exchange -> respond(exchange, jar));
        api.start();
        try {
            McVersions versions = new McVersions(dir.resolve("versions"),
                    URI.create("http://127.0.0.1:1/manifest.json"));
            McCores cores = new McCores(versions, dir.resolve("cores"),
                    URI.create("http://127.0.0.1:1"), URI.create(base),
                    URI.create("http://127.0.0.1:1"));
            McInstanceManager manager = new McInstanceManager(dir.resolve("instances"));
            McInstance instance = manager.create("purpur", "1.21.4", 25565, 25575, 1024, "world",
                    null, false, "purpur");
            cores.install(instance, manager, "1.21.4", null);
            assertArrayEquals(jar, Files.readAllBytes(manager.serverJar(instance)));
        } finally {
            api.stop(0);
        }
    }

    @Test
    void aFailedDownloadFallsBackToTheNextSource(@TempDir Path dir) throws Exception {
        byte[] jar = zipBytes("purpur");
        String md5 = hex("MD5", jar);
        String sha256 = hex("SHA-256", jar);

        HttpServer official = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        String officialBase = "http://127.0.0.1:" + official.getAddress().getPort();
        official.createContext("/v2/purpur/1.21.4", exchange -> respond(exchange,
                "{\"builds\":{\"latest\":\"2062\",\"all\":[\"2062\"]}}"
                        .getBytes(StandardCharsets.UTF_8)));
        official.createContext("/v2/purpur/1.21.4/2062", exchange -> respond(exchange,
                ("{\"md5\":\"" + md5 + "\"}").getBytes(StandardCharsets.UTF_8)));
        official.createContext("/v2/purpur/1.21.4/2062/download", exchange -> {
            exchange.sendResponseHeaders(500, -1);
            exchange.close();
        });
        official.start();

        HttpServer msl = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        String mslBase = "http://127.0.0.1:" + msl.getAddress().getPort();
        String jarUrl = mslBase + "/purpur.jar";
        msl.createContext("/download/server/purpur/1.21.4", exchange -> respond(exchange,
                ("{\"data\":{\"url\":\"" + jarUrl + "\",\"sha256\":\"" + sha256 + "\"}}")
                        .getBytes(StandardCharsets.UTF_8)));
        msl.createContext("/purpur.jar", exchange -> respond(exchange, jar));
        msl.start();

        try {
            McSources sources = new McSources(null);
            sources.setOrder("purpur", List.of("official", "mslmc"));
            sources.setCustomBase("purpur", officialBase);
            sources.setCustomBase("mslmc", mslBase);

            McCores cores = new McCores(new McVersions(dir.resolve("versions"),
                    URI.create("http://127.0.0.1:1/manifest.json")),
                    dir.resolve("cores"), sources);
            McInstanceManager manager = new McInstanceManager(dir.resolve("instances"));
            McInstance instance = manager.create("purpur", "1.21.4", 25565, 25575, 1024, "world",
                    null, false, "purpur");

            cores.install(instance, manager, "1.21.4", null);

            assertArrayEquals(jar, Files.readAllBytes(manager.serverJar(instance)));
            String artifact = Files.readString(dir.resolve("cores").resolve("purpur")
                    .resolve("1.21.4").resolve("artifact.json"));
            assertTrue(artifact.contains("\"source\": \"mslmc\"")
                    || artifact.contains("\"source\":\"mslmc\""), artifact);
        } finally {
            official.stop(0);
            msl.stop(0);
        }
    }

    @Test
    void theDownloadWindowRejectsATrickle() {
        assertTrue(McCores.tooSlow(50L * 1024, TimeUnit.SECONDS.toNanos(10)),
                "50 KiB in 10 s is the official-Purpur trickle that hung an install");
        assertFalse(McCores.tooSlow(640L * 1024, TimeUnit.SECONDS.toNanos(10)));
        assertFalse(McCores.tooSlow(0, TimeUnit.SECONDS.toNanos(9)),
                "a window shorter than the cutoff has not had time to judge");
    }

    @Test
    void installsTheFabricLauncherJar(@TempDir Path dir) throws Exception {
        byte[] jar = zipBytes("fabric");
        HttpServer api = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        String base = "http://127.0.0.1:" + api.getAddress().getPort();
        api.createContext("/v2/versions/loader", exchange -> respond(exchange,
                "[{\"version\":\"0.17.2\",\"stable\":true}]"
                        .getBytes(StandardCharsets.UTF_8)));
        api.createContext("/v2/versions/loader/1.21.4/0.17.2/server/jar",
                exchange -> respond(exchange, jar));
        api.start();
        try {
            McVersions versions = new McVersions(dir.resolve("versions"),
                    URI.create("http://127.0.0.1:1/manifest.json"));
            McCores cores = new McCores(versions, dir.resolve("cores"),
                    URI.create("http://127.0.0.1:1"), URI.create("http://127.0.0.1:1"),
                    URI.create(base));
            McInstanceManager manager = new McInstanceManager(dir.resolve("instances"));
            McInstance instance = manager.create("fabric", "1.21.4", 25565, 25575, 1024, "world",
                    null, false, "fabric");
            cores.install(instance, manager, "1.21.4", null);
            assertArrayEquals(jar, Files.readAllBytes(manager.serverJar(instance)));
            String record = Files.readString(dir.resolve("cores").resolve("fabric")
                    .resolve("1.21.4").resolve("version.json"));
            // The build column carries the loader version, which is what makes a
            // fabric install reproducible.
            assertTrue(record.contains("0.17.2"), record);
        } finally {
            api.stop(0);
        }
    }

    @Test
    void theCatalogueHidesThirdPartyCoresWhileTheGateIsOff() throws Exception {
        McVersions versions = new McVersions(Path.of("build", "test-cores-versions"),
                URI.create("http://127.0.0.1:1/manifest.json"));
        URI unreachable = URI.create("http://127.0.0.1:1");
        McForge forge = new McForge(unreachable, unreachable, unreachable,
                (java, installer, directory, mirror, log) -> 0);
        McCores cores = new McCores(versions, Path.of("build", "test-cores"),
                unreachable, unreachable, unreachable, forge);
        List<McCores.CoreInfo> off = cores.catalog(false);
        assertEquals(1, off.size());
        assertEquals(McInstance.CORE_VANILLA, off.get(0).id());
        List<McCores.CoreInfo> on = cores.catalog(true);
        assertEquals(7, on.size(), on.toString());
        assertEquals(List.of("vanilla", "paper", "folia", "purpur", "fabric", "forge", "neoforge"),
                on.stream().map(McCores.CoreInfo::id).toList());
    }

    @Test
    void coreIdsAreValidatedAndClassified() {
        assertTrue(McCores.known("paper"));
        assertTrue(McCores.known("vanilla"));
        assertTrue(McCores.known("forge"));
        assertTrue(McCores.known("neoforge"));
        assertFalse(McCores.known("mohist"));
        assertFalse(McCores.known(""));
        assertFalse(McCores.known(null));
        assertTrue(McCores.isThirdParty("fabric"));
        assertFalse(McCores.isThirdParty("vanilla"));
    }
}
