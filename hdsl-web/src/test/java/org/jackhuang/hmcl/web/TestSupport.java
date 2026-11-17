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
package org.jackhuang.hmcl.web;

import org.jackhuang.hmcl.util.logging.Logger;
import org.jackhuang.hmcl.web.auth.AuthService;
import org.jackhuang.hmcl.web.auth.UserStore;
import org.jackhuang.hmcl.web.config.ServerConfig;
import org.jackhuang.hmcl.web.config.ServerConfigLoader;
import org.jackhuang.hmcl.web.server.HdslServer;
import org.jackhuang.hmcl.web.tls.CertificateManager;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.net.http.HttpClient;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.Map;
import java.util.function.Consumer;

/// Shared scaffolding for the Phase 1 server tests: boots a full server on
/// an ephemeral port (port 0) with an isolated data directory and environment.
public final class TestSupport {

    static {
        // java.net.http refuses to set the Host header unless this property
        // names it. The proxy must pass Host through verbatim (dsh compares
        // Origin to Host), so the property is enabled for the test JVM before
        // any HttpClient exists.
        System.setProperty("jdk.httpclient.allowRestrictedHeaders", "host");
    }

    private TestSupport() {
    }

    /// Wipes the mutable state of the shared per-JVM launcher home (the
    /// gradle `test` task points `hdsl.home` at `build/test-home`, which
    /// every test class in the fork shares): the instance directories, the
    /// shared version homes and the launcher settings file.
    ///
    /// `start()` calls this before every boot, so each test's server stands
    /// on a home that no earlier test — or earlier interrupted run — could
    /// have polluted. The build also deletes the whole directory before the
    /// task; this is the in-run half of the same promise.
    private static void wipeSharedHome() {
        Path home = org.jackhuang.hmcl.Metadata.HMCL_USER_HOME;
        deleteRecursively(home.resolve("instances"));
        deleteRecursively(home.resolve("homes"));
        try {
            Files.deleteIfExists(home.resolve("launcher-settings.json"));
        } catch (IOException e) {
            throw new IllegalStateException("Could not wipe the shared launcher settings", e);
        }
    }

    private static void deleteRecursively(Path root) {
        if (!Files.exists(root)) {
            return;
        }
        try (var walk = Files.walk(root)) {
            for (Path path : walk.sorted(java.util.Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Could not wipe " + root, e);
        }
    }

    /// A running server plus the bits tests need to talk to it.
    public record RunningServer(HdslServer server, CertificateManager certificates, Path dataDir, int port)
            implements AutoCloseable {
        /// HTTP base URL of the main connector, e.g. `http://127.0.0.1:41234`.
        public String baseUrl() {
            return "http://127.0.0.1:" + port;
        }

        @Override
        public void close() throws Exception {
            server.stop();
        }
    }

    /// Loads the config from `dataDir` + `env`, applies `tweak` (for fields
    /// tests cannot express in yaml/env, like ephemeral redirect ports), and
    /// starts the server on port 0.
    public static RunningServer start(Path dataDir, Map<String, String> env, Consumer<ServerConfig> tweak)
            throws Exception {
        wipeSharedHome();
        ServerConfig config = ServerConfigLoader.load(dataDir, env, Logger.LOG);
        tweak.accept(config);
        config.port = 0;

        UserStore users = UserStore.open(dataDir, env, Logger.LOG);
        AuthService auth = new AuthService(users, Duration.ofHours(config.auth.sessionTtlHours));
        CertificateManager certificates = CertificateManager.create(config, Logger.LOG);
        HdslServer server = HdslServer.build(config, auth, certificates, Logger.LOG);
        server.start();
        return new RunningServer(server, certificates, dataDir, server.getPort());
    }

    /// A plain client, no redirects, no cookie jar.
    public static HttpClient client() {
        return HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    /// A client that trusts every certificate — for talking to self-signed
    /// TLS endpoints. Hostname verification stays on (it is not a
    /// TrustManager concern), which is exactly what the self-signed SAN list
    /// is tested against.
    public static HttpClient trustAllClient() {
        TrustManager[] trustAll = {new X509TrustManager() {
            @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType) {
            }

            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType) {
            }

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[0];
            }
        }};
        try {
            SSLContext context = SSLContext.getInstance("TLS");
            context.init(null, trustAll, new SecureRandom());
            return HttpClient.newBuilder()
                    .sslContext(context)
                    .followRedirects(HttpClient.Redirect.NEVER)
                    .build();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to build the trust-all client", e);
        }
    }

    /// A free port, by the bind-then-close dance.
    public static int freePort() throws Exception {
        try (var socket = new java.net.ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    /// Turns on the fake `pnpm` (see `src/test/resources/fake-pnpm/pnpm`):
    /// harness installs lay down a stub runtime instead of downloading one.
    /// The marker sits beside the shared test home, so it survives the
    /// per-boot wipe — every test that enables it must disable it again.
    public static void enableFakePnpm() throws IOException {
        Files.write(buildDirectory().resolve("fake-pnpm.enabled"), new byte[0]);
    }

    /// Turns the fake `pnpm` back off.
    public static void disableFakePnpm() throws IOException {
        Files.deleteIfExists(buildDirectory().resolve("fake-pnpm.enabled"));
    }

    /// The gradle build directory: the test task points `hdsl.home` at
    /// `<build>/test-home`, so the build directory is its parent.
    private static Path buildDirectory() {
        return org.jackhuang.hmcl.Metadata.HMCL_USER_HOME.getParent();
    }
}
