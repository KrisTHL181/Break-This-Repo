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
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class McJavaRuntimesTest {

    private static String sha256(byte[] bytes) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }

    private static void respond(com.sun.net.httpserver.HttpExchange exchange, byte[] body)
            throws java.io.IOException {
        exchange.sendResponseHeaders(200, body.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(body);
        }
    }

    /// A zip shaped like a JRE distribution: one top-level directory, `bin/java`
    /// inside it.
    private static byte[] zipJre(String javaBody) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
            zip.putNextEntry(new ZipEntry("jdk-21.0.5+11-jre/"));
            zip.closeEntry();
            zip.putNextEntry(new ZipEntry("jdk-21.0.5+11-jre/bin/"));
            zip.closeEntry();
            zip.putNextEntry(new ZipEntry("jdk-21.0.5+11-jre/bin/java"));
            zip.write(javaBody.getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }
        return bytes.toByteArray();
    }

    /// A `.tar.gz` with the same shape, written by hand: tar's header is a fixed
    /// 512-byte block, and this is the smallest legal subset of it.
    private static byte[] tarGzJre(String javaBody) throws Exception {
        ByteArrayOutputStream tar = new ByteArrayOutputStream();
        tarEntry(tar, "jdk-21.0.5+11-jre/bin/java", javaBody.getBytes(StandardCharsets.UTF_8));
        tar.write(new byte[1024]);
        ByteArrayOutputStream gz = new ByteArrayOutputStream();
        try (GZIPOutputStream out = new GZIPOutputStream(gz)) {
            out.write(tar.toByteArray());
        }
        return gz.toByteArray();
    }

    private static void tarEntry(ByteArrayOutputStream out, String name, byte[] content) throws Exception {
        byte[] header = new byte[512];
        byte[] nameBytes = name.getBytes(StandardCharsets.UTF_8);
        System.arraycopy(nameBytes, 0, header, 0, Math.min(nameBytes.length, 100));
        writeOctal(header, 100, 8, 420);
        writeOctal(header, 108, 8, 0);
        writeOctal(header, 116, 8, 0);
        writeOctal(header, 124, 12, content.length);
        writeOctal(header, 136, 12, 0);
        header[156] = '0';
        byte[] magic = "ustar\0".getBytes(StandardCharsets.US_ASCII);
        System.arraycopy(magic, 0, header, 257, magic.length);
        header[263] = '0';
        header[264] = '0';
        for (int i = 148; i < 156; i++) {
            header[i] = ' ';
        }
        long sum = 0;
        for (byte value : header) {
            sum += value & 0xff;
        }
        writeOctal(header, 148, 8, sum);
        out.write(header);
        out.write(content);
        int padding = (512 - content.length % 512) % 512;
        out.write(new byte[padding]);
    }

    private static void writeOctal(byte[] header, int offset, int length, long value) {
        String text = Long.toOctalString(value);
        while (text.length() < length - 1) {
            text = "0" + text;
        }
        byte[] bytes = text.getBytes(StandardCharsets.US_ASCII);
        System.arraycopy(bytes, 0, header, offset, Math.min(bytes.length, length - 1));
        header[offset + length - 1] = 0;
    }

    private static HttpServer fakeAdoptium(byte[] pkg, String checksum, String fileName) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        int port = server.getAddress().getPort();
        String base = "http://127.0.0.1:" + port;
        server.createContext("/v3/info/available_releases", exchange -> respond(exchange,
                "{\"available_releases\":[8,17,21]}".getBytes(StandardCharsets.UTF_8)));
        server.createContext("/v3/assets/latest/21/hotspot", exchange -> respond(exchange,
                ("""
                        [{"version":{"semver":"21.0.5+11","major":21},
                          "binary":{"package":{"link":"%s/jre.zip","checksum":"%s","size":%d}}}]
                        """.formatted(base, checksum, pkg.length)).getBytes(StandardCharsets.UTF_8)));
        server.createContext("/v3/assets/latest/17/hotspot", exchange -> respond(exchange,
                "[]".getBytes(StandardCharsets.UTF_8)));
        server.createContext("/v3/assets/latest/8/hotspot", exchange -> respond(exchange,
                "[]".getBytes(StandardCharsets.UTF_8)));
        server.createContext("/" + fileName, exchange -> respond(exchange, pkg));
        server.start();
        return server;
    }

    @Test
    void installsAVerifiedJreZipAndFindsItsJava(@TempDir Path dir) throws Exception {
        byte[] pkg = zipJre("#!/bin/sh\nexit 0\n");
        HttpServer adoptium = fakeAdoptium(pkg, sha256(pkg), "jre.zip");
        try {
            String base = "http://127.0.0.1:" + adoptium.getAddress().getPort();
            McJavaRuntimes runtimes = new McJavaRuntimes(dir.resolve("java"), URI.create(base));
            McJavaRuntimes.Installed installed = runtimes.install(21, null);
            assertEquals(21, installed.feature());
            assertTrue(Files.isRegularFile(installed.path()));
            assertTrue(installed.path().getFileName().toString().equals("java")
                            || installed.path().getFileName().toString().equals("java.exe"),
                    installed.path().toString());
            assertEquals(1, runtimes.listInstalled().size());
            assertNotNull(runtimes.findInstalled(21));
            String record = Files.readString(dir.resolve("java").resolve("21").resolve("runtime.json"));
            assertTrue(record.contains("21.0.5+11"), record);
            assertTrue(runtimes.remove(21));
            assertFalse(Files.exists(dir.resolve("java").resolve("21")));
            assertFalse(runtimes.remove(21));
        } finally {
            adoptium.stop(0);
        }
    }

    @Test
    void unpacksATarGzJre(@TempDir Path dir) throws Exception {
        byte[] pkg = tarGzJre("#!/bin/sh\nexit 0\n");
        String checksum = sha256(pkg);
        HttpServer tarHost = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        int port = tarHost.getAddress().getPort();
        String tarBase = "http://127.0.0.1:" + port;
        tarHost.createContext("/v3/info/available_releases", exchange -> respond(exchange,
                "{\"available_releases\":[21]}".getBytes(StandardCharsets.UTF_8)));
        tarHost.createContext("/v3/assets/latest/21/hotspot", exchange -> respond(exchange,
                ("""
                        [{"version":{"semver":"21.0.5+11","major":21},
                          "binary":{"package":{"link":"%s/jre.tar.gz","checksum":"%s","size":%d}}}]
                        """.formatted(tarBase, checksum, pkg.length))
                        .getBytes(StandardCharsets.UTF_8)));
        tarHost.createContext("/jre.tar.gz", exchange -> respond(exchange, pkg));
        tarHost.start();
        try {
            McJavaRuntimes runtimes = new McJavaRuntimes(dir.resolve("java"), URI.create(tarBase));
            McJavaRuntimes.Installed installed = runtimes.install(21, null);
            assertTrue(Files.isRegularFile(installed.path()));
            assertEquals("#!/bin/sh\nexit 0\n", Files.readString(installed.path()));
        } finally {
            tarHost.stop(0);
        }
    }

    @Test
    void readsMicrosoftBuildsThroughFoojayAndInstallsOne(@TempDir Path dir) throws Exception {
        byte[] pkg = tarGzJre("#!/bin/sh\nexit 0\n");
        String checksum = sha256(pkg);
        HttpServer host = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        String base = "http://127.0.0.1:" + host.getAddress().getPort();
        host.createContext("/packages", exchange -> respond(exchange,
                ("""
                        {"result":[{"id":"p21","jdk_version":21,"java_version":"21.0.13",
                          "size":%d,"archive_type":"tar.gz"}]}
                        """.formatted(pkg.length)).getBytes(StandardCharsets.UTF_8)));
        host.createContext("/ids/p21", exchange -> respond(exchange,
                ("""
                        {"result":[{"filename":"jdk.tar.gz",
                          "direct_download_uri":"%s/pkg.tar.gz",
                          "checksum":"","checksum_type":"sha256",
                          "checksum_uri":"%s/pkg.sha256"}]}
                        """.formatted(base, base)).getBytes(StandardCharsets.UTF_8)));
        host.createContext("/pkg.sha256", exchange -> respond(exchange,
                (checksum + "  jdk.tar.gz\n").getBytes(StandardCharsets.UTF_8)));
        host.createContext("/pkg.tar.gz", exchange -> respond(exchange, pkg));
        host.start();
        try {
            McJavaRuntimes runtimes = new McJavaRuntimes(dir.resolve("java"),
                    URI.create("http://127.0.0.1:1"), URI.create(base));
            var releases = runtimes.available(McJavaRuntimes.Vendor.MICROSOFT);
            assertEquals(1, releases.size());
            assertEquals(checksum, releases.get(0).sha256());
            assertEquals(McJavaRuntimes.Vendor.MICROSOFT, releases.get(0).vendor());
            McJavaRuntimes.Installed installed = runtimes.install(21,
                    McJavaRuntimes.Vendor.MICROSOFT, null);
            assertTrue(Files.isRegularFile(installed.path()));
            assertEquals(McJavaRuntimes.Vendor.MICROSOFT,
                    runtimes.listInstalled().get(0).vendor());
        } finally {
            host.stop(0);
        }
    }

    @Test
    void refusesOracleDownloadHosts() {
        assertFalse(McJavaRuntimes.isRedistributable(URI.create(
                "https://download.oracle.com/java/21/latest/jdk-21_linux-x64_bin.tar.gz")));
        assertFalse(McJavaRuntimes.isRedistributable(URI.create(
                "https://javadl.oracle.com/webapps/download/GetFile/x")));
        assertFalse(McJavaRuntimes.isRedistributable(URI.create(
                "https://java.com/download/jdk.zip")));
        assertTrue(McJavaRuntimes.isRedistributable(URI.create(
                "https://github.com/adoptium/temurin21-binaries/releases/download/x/jdk.tar.gz")));
        assertTrue(McJavaRuntimes.isRedistributable(URI.create(
                "https://mirrors.example.org/jdk.tar.gz")));
    }

    @Test
    void refusesToInstallADownloadFromOracle(@TempDir Path dir) throws Exception {
        HttpServer host = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        host.createContext("/v3/info/available_releases", exchange -> respond(exchange,
                "{\"available_releases\":[21]}".getBytes(StandardCharsets.UTF_8)));
        host.createContext("/v3/assets/latest/21/hotspot", exchange -> respond(exchange,
                ("""
                        [{"version":{"semver":"21.0.1+1","major":21},
                          "binary":{"package":{"link":"https://download.oracle.com/java/21/jdk.tar.gz",
                            "checksum":"%s","size":10}}}]
                        """.formatted("00".repeat(32))).getBytes(StandardCharsets.UTF_8)));
        host.start();
        try {
            McJavaRuntimes runtimes = new McJavaRuntimes(dir.resolve("java"),
                    URI.create("http://127.0.0.1:" + host.getAddress().getPort()));
            McException refused = assertThrows(McException.class, () -> runtimes.install(21, null));
            assertTrue(refused.getMessage().contains("Oracle"), refused.getMessage());
            assertFalse(Files.exists(dir.resolve("java").resolve("21")));
        } finally {
            host.stop(0);
        }
    }

    @Test
    void followsTheRedirectAdoptiumAnswersWith(@TempDir Path dir) throws Exception {
        byte[] pkg = zipJre("#!/bin/sh\nexit 0\n");
        String checksum = sha256(pkg);
        HttpServer host = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        String base = "http://127.0.0.1:" + host.getAddress().getPort();
        host.createContext("/v3/info/available_releases", exchange -> respond(exchange,
                "{\"available_releases\":[21]}".getBytes(StandardCharsets.UTF_8)));
        host.createContext("/v3/assets/latest/21/hotspot", exchange -> respond(exchange,
                ("""
                        [{"version":{"semver":"21.0.5+11","major":21},
                          "binary":{"package":{"link":"%s/redirect.zip","checksum":"%s","size":%d}}}]
                        """.formatted(base, checksum, pkg.length))
                        .getBytes(StandardCharsets.UTF_8)));
        // Adoptium's real link is a 302 to another host; a client that does not
        // follow it fails the install with "answered HTTP 302".
        host.createContext("/redirect.zip", exchange -> {
            exchange.getResponseHeaders().add("Location", base + "/jdk.zip");
            exchange.sendResponseHeaders(302, -1);
            exchange.close();
        });
        host.createContext("/jdk.zip", exchange -> respond(exchange, pkg));
        host.start();
        try {
            McJavaRuntimes runtimes = new McJavaRuntimes(dir.resolve("java"), URI.create(base));
            McJavaRuntimes.Installed installed = runtimes.install(21, null);
            assertTrue(Files.isRegularFile(installed.path()));
        } finally {
            host.stop(0);
        }
    }

    @Test
    void theUnpackedJavaIsExecutableAndMeasured(@TempDir Path dir) throws Exception {
        org.junit.jupiter.api.Assumptions.assumeTrue(
                java.io.File.separatorChar == '/', "POSIX permissions only");
        byte[] pkg = tarGzJre("#!/bin/sh\nexit 0\n");
        String checksum = sha256(pkg);
        HttpServer host = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        String base = "http://127.0.0.1:" + host.getAddress().getPort();
        host.createContext("/v3/info/available_releases", exchange -> respond(exchange,
                "{\"available_releases\":[21]}".getBytes(StandardCharsets.UTF_8)));
        host.createContext("/v3/assets/latest/21/hotspot", exchange -> respond(exchange,
                ("""
                        [{"version":{"semver":"21.0.5+11","major":21},
                          "binary":{"package":{"link":"%s/jdk.tar.gz","checksum":"%s","size":%d}}}]
                        """.formatted(base, checksum, pkg.length)).getBytes(StandardCharsets.UTF_8)));
        host.createContext("/jdk.tar.gz", exchange -> respond(exchange, pkg));
        host.start();
        try {
            McJavaRuntimes runtimes = new McJavaRuntimes(dir.resolve("java"), URI.create(base));
            McJavaRuntimes.Installed installed = runtimes.install(21, null);
            // The archive is unpacked with the panel's umask, so `bin/java` comes
            // out 0644 unless the panel sets the mode: not executable by anyone,
            // including root. This is the regression that test exists for.
            assertTrue(Files.isExecutable(installed.path()), installed.path().toString());
            assertTrue(installed.bytes() > 0);
            McJavaRuntimes.Installed listed = runtimes.listInstalled().get(0);
            assertEquals("21.0.5+11", listed.version());
            assertEquals(installed.bytes(), listed.bytes());
        } finally {
            host.stop(0);
        }
    }

    @Test
    void refusesAPackageThatDoesNotMatchItsChecksum(@TempDir Path dir) throws Exception {
        byte[] pkg = zipJre("payload");
        HttpServer adoptium = fakeAdoptium(pkg, "00".repeat(32), "jre.zip");
        try {
            String base = "http://127.0.0.1:" + adoptium.getAddress().getPort();
            McJavaRuntimes runtimes = new McJavaRuntimes(dir.resolve("java"), URI.create(base));
            McException failure = assertThrows(McException.class, () -> runtimes.install(21, null));
            assertTrue(failure.getMessage().contains("SHA-256"), failure.getMessage());
            assertFalse(Files.exists(dir.resolve("java").resolve("21")));
            assertEquals(0, runtimes.listInstalled().size());
        } finally {
            adoptium.stop(0);
        }
    }

    @Test
    void recommendsTheDocumentedJavaPerMinecraftVersion() {
        assertEquals(21, McJavaRuntimes.recommendFor("1.21.4"));
        assertEquals(21, McJavaRuntimes.recommendFor("1.20.5"));
        assertEquals(21, McJavaRuntimes.recommendFor("1.20.6"));
        assertEquals(17, McJavaRuntimes.recommendFor("1.20.4"));
        assertEquals(17, McJavaRuntimes.recommendFor("1.18"));
        assertEquals(16, McJavaRuntimes.recommendFor("1.17.1"));
        assertEquals(8, McJavaRuntimes.recommendFor("1.16.5"));
        assertEquals(8, McJavaRuntimes.recommendFor("1.8.9"));
        assertEquals(25, McJavaRuntimes.recommendFor("26.1"));
        assertEquals(21, McJavaRuntimes.recommendFor("not-a-version"));
    }

    @Test
    void ignoresDirectoriesThatAreNotFeatureReleases(@TempDir Path dir) throws Exception {
        Path root = dir.resolve("java");
        Files.createDirectories(root.resolve("junk").resolve("bin"));
        Files.writeString(root.resolve("junk").resolve("bin").resolve("java"), "not a jre");
        Files.createDirectories(root.resolve("21").resolve("bin"));
        Files.writeString(root.resolve("21").resolve("bin").resolve("java"), "#!/bin/sh\n");
        McJavaRuntimes runtimes = new McJavaRuntimes(root);
        assertEquals(1, runtimes.listInstalled().size());
        assertEquals(21, runtimes.listInstalled().get(0).feature());
    }
}
