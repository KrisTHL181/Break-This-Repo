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
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class McForgeTest {

    private static void respond(com.sun.net.httpserver.HttpExchange exchange, String body)
            throws java.io.IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }

    private static void respondBytes(com.sun.net.httpserver.HttpExchange exchange, byte[] bytes)
            throws java.io.IOException {
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }

    private static byte[] zipBytes() throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
            zip.putNextEntry(new ZipEntry("install_profile.json"));
            zip.write("{\"minecraft\":\"1.21.4\"}".getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }
        return bytes.toByteArray();
    }

    /// Serves the Forge promotions, the NeoForge Maven metadata and both
    /// installers; `sha1` may be null to model a mirror without checksums.
    private static HttpServer fakeUpstream(byte[] installer, String sha1Forge, String sha1Neo)
            throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/promotions_slim.json", exchange -> respond(exchange, """
                {"promos":{
                  "1.21.4-latest":"54.0.16",
                  "1.21.4-recommended":"54.0.15",
                  "1.20.1-latest":"47.3.0",
                  "1.19.2-recommended":"43.2.0"
                }}
                """));
        server.createContext("/releases/net/neoforged/neoforge/maven-metadata.xml",
                exchange -> respond(exchange, """
                        <metadata><versioning><versions>
                          <version>21.4.123</version>
                          <version>21.4.120-beta</version>
                          <version>21.1.95</version>
                          <version>20.2.88</version>
                        </versions></versioning></metadata>
                        """));
        String forgePath = "/net/minecraftforge/forge/1.21.4-54.0.15/forge-1.21.4-54.0.15-installer.jar";
        server.createContext(forgePath, exchange -> respondBytes(exchange, installer));
        // HttpServer matches contexts by prefix and the longest one wins, so the
        // `.sha1` context has to exist even when there is no checksum — otherwise
        // the jar handler answers that request with the jar.
        server.createContext(forgePath + ".sha1", exchange -> {
            if (sha1Forge == null) {
                exchange.sendResponseHeaders(404, -1);
                exchange.close();
                return;
            }
            respond(exchange, sha1Forge + "  forge.jar\n");
        });
        String neoPath = "/releases/net/neoforged/neoforge/21.4.123/neoforge-21.4.123-installer.jar";
        server.createContext(neoPath, exchange -> respondBytes(exchange, installer));
        server.createContext(neoPath + ".sha1", exchange -> {
            if (sha1Neo == null) {
                exchange.sendResponseHeaders(404, -1);
                exchange.close();
                return;
            }
            respond(exchange, sha1Neo + "\n");
        });
        server.start();
        return server;
    }

    private static String base(HttpServer server) {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @Test
    void listsGameVersionsFromUpstreamMetadata(@TempDir Path dir) throws Exception {
        HttpServer upstream = fakeUpstream(zipBytes(), null, null);
        try {
            McForge forge = new McForge(URI.create(base(upstream)),
                    URI.create(base(upstream) + "/promotions_slim.json"),
                    URI.create(base(upstream)), (java, installer, directory, mirror, log) -> 0);
            List<String> versions = forge.gameVersions("forge");
            assertEquals(List.of("1.21.4", "1.20.1", "1.19.2"), versions);
            // The recommended promotion wins over the newer "latest" one.
            McForge.Build build = forge.resolve("forge", "1.21.4");
            assertEquals("54.0.15", build.loaderVersion());
            assertTrue(build.installerUrl().endsWith(
                    "/forge-1.21.4-54.0.15-installer.jar"), build.installerUrl());

            List<String> neo = forge.gameVersions("neoforge");
            assertEquals(List.of("1.21.4", "1.21.1", "1.20.2"), neo);
            McForge.Build neoBuild = forge.resolve("neoforge", "1.21.4");
            assertEquals("21.4.123", neoBuild.loaderVersion());
            assertThrows(McException.class, () -> forge.resolve("forge", "1.7.10"));
        } finally {
            upstream.stop(0);
        }
    }

    @Test
    void runsTheInstallerAndRecordsTheArgumentFile(@TempDir Path dir) throws Exception {
        byte[] installer = zipBytes();
        HttpServer upstream = fakeUpstream(installer, null, null);
        try {
            Path instance = dir.resolve("instance");
            Files.createDirectories(instance);
            // A killed attempt leaves `.part` files behind; the next install has to
            // start from a clean directory instead of stacking them up.
            Path stale = instance.resolve(".installer").resolve("installer123.part");
            Files.createDirectories(stale.getParent());
            Files.writeString(stale, "half");
            boolean[] ran = {false};
            McForge forge = new McForge(URI.create(base(upstream)),
                    URI.create(base(upstream) + "/promotions_slim.json"),
                    URI.create(base(upstream)), (java, file, directory, mirror, log) -> {
                        ran[0] = true;
                        log.accept("installer started");
                        Path args = directory.resolve(
                                "libraries/net/minecraftforge/forge/1.21.4-54.0.15/unix_args.txt");
                        Files.createDirectories(args.getParent());
                        Files.writeString(args, "-cp libs/* net.minecraftforge.MainKt");
                        return 0;
                    });
            McForge.Launch launch = forge.install(instance, "forge", "1.21.4", "java", null);
            assertTrue(ran[0]);
            assertNotNull(launch.argsFile());
            List<String> argv = McForge.launchArguments(instance);
            assertNotNull(argv);
            assertEquals(2, argv.size(), argv.toString());
            assertTrue(argv.get(0).startsWith("@"), argv.get(0));
            assertTrue(argv.get(0).endsWith("unix_args.txt"), argv.get(0));
            assertEquals("nogui", argv.get(1));
            String record = Files.readString(instance.resolve(McForge.LAUNCH_FILE));
            assertTrue(record.contains("54.0.15"), record);
            // The installer jar is not left behind in the instance.
            assertFalse(Files.exists(instance.resolve(".installer")
                    .resolve("forge-54.0.15-installer.jar")));
            assertFalse(Files.exists(stale), "a stale .part survived the install");
        } finally {
            upstream.stop(0);
        }
    }

    @Test
    void aFailedInstallerReportsItsOutput(@TempDir Path dir) throws Exception {
        HttpServer upstream = fakeUpstream(zipBytes(), null, null);
        try {
            McForge forge = new McForge(URI.create(base(upstream)),
                    URI.create(base(upstream) + "/promotions_slim.json"),
                    URI.create(base(upstream)), (java, file, directory, mirror, log) -> {
                        log.accept("cannot reach maven");
                        return 3;
                    });
            Path instance = dir.resolve("instance");
            Files.createDirectories(instance);
            McException failure = assertThrows(McException.class,
                    () -> forge.install(instance, "forge", "1.21.4", "java", null));
            assertTrue(failure.getMessage().contains("code 3"), failure.getMessage());
            assertTrue(failure.getMessage().contains("cannot reach maven"), failure.getMessage());
            assertFalse(Files.exists(instance.resolve(McForge.LAUNCH_FILE)));
        } finally {
            upstream.stop(0);
        }
    }

    @Test
    void aTamperedInstallerIsRefusedBeforeItRuns(@TempDir Path dir) throws Exception {
        HttpServer upstream = fakeUpstream(zipBytes(), "00".repeat(20), null);
        try {
            McForge forge = new McForge(URI.create(base(upstream)),
                    URI.create(base(upstream) + "/promotions_slim.json"),
                    URI.create(base(upstream)), (java, file, directory, mirror, log) -> 0);
            Path instance = dir.resolve("instance");
            Files.createDirectories(instance);
            McException failure = assertThrows(McException.class,
                    () -> forge.install(instance, "forge", "1.21.4", "java", null));
            assertTrue(failure.getMessage().contains("SHA-1"), failure.getMessage());
        } finally {
            upstream.stop(0);
        }
    }

    @Test
    void fallsBackToTheOldLauncherJar(@TempDir Path dir) throws Exception {
        HttpServer upstream = fakeUpstream(zipBytes(), null, null);
        try {
            Path instance = dir.resolve("instance");
            Files.createDirectories(instance);
            McForge forge = new McForge(URI.create(base(upstream)),
                    URI.create(base(upstream) + "/promotions_slim.json"),
                    URI.create(base(upstream)), (java, file, directory, mirror, log) -> {
                        try {
                            Files.write(directory.resolve("forge-1.12.2-14.23.5.2860.jar"), zipBytes());
                        } catch (Exception e) {
                            throw new java.io.IOException(e);
                        }
                        return 0;
                    });
            McForge.Launch launch = forge.install(instance, "forge", "1.21.4", "java", null);
            assertNull(launch.argsFile());
            assertEquals("forge-1.12.2-14.23.5.2860.jar", launch.jar());
            List<String> argv = McForge.launchArguments(instance);
            assertNotNull(argv);
            assertEquals("-jar", argv.get(0));
            assertEquals("nogui", argv.get(2));
        } finally {
            upstream.stop(0);
        }
    }

    @Test
    void anInstanceWithoutALaunchProfileHasNoArguments(@TempDir Path dir) {
        assertNull(McForge.launchArguments(dir));
        try {
            Files.writeString(dir.resolve(McForge.LAUNCH_FILE),
                    "{\"argsFile\":\"../../etc/passwd\"}");
        } catch (Exception e) {
            throw new AssertionError(e);
        }
        // A profile that points outside the instance is not a launch profile.
        assertNull(McForge.launchArguments(dir));
    }

    @Test
    void installerArgvAddsTheMirrorWithATrailingSlash(@TempDir Path dir) {
        List<String> with = McForge.installerArgv("java", dir.resolve("i.jar"),
                "https://bmclapi2.bangbang93.com/maven");
        assertEquals("--mirror", with.get(with.size() - 2), with.toString());
        assertEquals("https://bmclapi2.bangbang93.com/maven/", with.get(with.size() - 1),
                with.toString());
        List<String> without = McForge.installerArgv("java", dir.resolve("i.jar"), null);
        assertFalse(without.contains("--mirror"), without.toString());
        assertTrue(without.contains("--installServer"), without.toString());
    }

    @Test
    void theInstallerMirrorIsUsedOnlyWhenBmclapiIsListed() {
        McSources sources = new McSources(null);
        sources.setOrder("forge", List.of("official"));
        McForge plain = new McForge(sources);
        assertNull(plain.mirrorFor("forge"));

        sources.setOrder("forge", List.of("bmclapi", "official"));
        McForge mirrored = new McForge(sources);
        String mirror = mirrored.mirrorFor("forge");
        assertNotNull(mirror);
        assertTrue(mirror.endsWith("/"), mirror);
        assertTrue(mirror.contains("bmclapi"), mirror);
    }

    @Test
    void followsARedirectOnThePromotionsFile(@TempDir Path dir) throws Exception {
        HttpServer upstream = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        String base = base(upstream);
        upstream.createContext("/promotions_slim.json", exchange -> {
            exchange.getResponseHeaders().add("Location", base + "/promotions-real.json");
            exchange.sendResponseHeaders(302, -1);
            exchange.close();
        });
        upstream.createContext("/promotions-real.json", exchange -> respond(exchange, """
                {"promos":{"1.21.4-recommended":"54.0.15"}}
                """));
        upstream.createContext("/releases/net/neoforged/neoforge/maven-metadata.xml",
                exchange -> respond(exchange, "<metadata/>"));
        upstream.start();
        try {
            // BMCLAPI answers its Maven tree with a 302; without follow-redirects
            // the source looked dead and every install fell back to the slow host.
            McForge forge = new McForge(URI.create(base),
                    URI.create(base + "/promotions_slim.json"), URI.create(base),
                    (java, installer, directory, mirror, log) -> 0);
            assertEquals(List.of("1.21.4"), forge.gameVersions("forge"));
        } finally {
            upstream.stop(0);
        }
    }
}
