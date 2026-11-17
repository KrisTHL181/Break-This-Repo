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
package org.jackhuang.hmcl.web.tls;

import com.google.gson.JsonParser;
import org.jackhuang.hmcl.util.logging.Logger;
import org.jackhuang.hmcl.web.TestSupport;
import org.jackhuang.hmcl.web.config.ServerConfig;
import org.jackhuang.hmcl.web.config.ServerConfigLoader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// HTTPS with no configured certificate: the manager must generate (and then
/// reuse) a self-signed pair, the server must answer TLS with it, and Jetty's
/// SslContextFactory.reload must accept a replacement without a restart.
class SelfSignedTlsTest {

    @TempDir
    Path dataDir;

    @Test
    void selfSignedGenerationServesTlsAndReloads() throws Exception {
        try (TestSupport.RunningServer running = TestSupport.start(dataDir, Map.of(),
                config -> {
                    config.bindHost = "127.0.0.1";
                    config.https.enabled = true;
                })) {

            // The generated pair lands in <data>/certs/, key owner-only.
            Path certFile = dataDir.resolve("certs").resolve(CertificateManager.SELF_SIGNED_CERT_FILE);
            Path keyFile = dataDir.resolve("certs").resolve(CertificateManager.SELF_SIGNED_KEY_FILE);
            assertTrue(Files.exists(certFile), "self-signed cert must be written for reuse");
            assertTrue(Files.exists(keyFile), "self-signed key must be written for reuse");
            org.jackhuang.hmcl.util.PosixPermissions.assertOwnerOnly(keyFile,
                    "the private key must be 0600");
            assertEquals(CertificateManager.Source.SELF_SIGNED, running.certificates().getSource());

            // A trust-all client can complete the handshake and reach /api/health.
            HttpClient client = TestSupport.trustAllClient();
            String base = "https://127.0.0.1:" + running.port();
            HttpResponse<String> health = client.send(
                    HttpRequest.newBuilder(URI.create(base + "/api/health")).GET().build(),
                    HttpResponse.BodyHandlers.ofString());
            assertEquals(200, health.statusCode(), health.body());
            assertEquals("ok", JsonParser.parseString(health.body()).getAsJsonObject().get("status").getAsString());

            // Restarting must reuse the files rather than churn a new cert: a
            // fresh manager over the same data dir reports the same source
            // without regenerating.
            String certBefore = Files.readString(certFile);
            ServerConfig config2 = ServerConfigLoader.load(dataDir, Map.of(), Logger.LOG);
            config2.https.enabled = true;
            CertificateManager reused = CertificateManager.create(config2, Logger.LOG);
            assertEquals(CertificateManager.Source.SELF_SIGNED, reused.getSource());
            assertEquals(certBefore, Files.readString(certFile), "restart must reuse the stored self-signed cert");

            // Hot reload: swap in a "newly uploaded" pair (a copy, for Phase 1)
            // and let Jetty re-initialize the SSL context without a restart.
            Path uploadedCert = dataDir.resolve("certs/uploaded.pem");
            Path uploadedKey = dataDir.resolve("certs/uploaded-key.pem");
            Files.copy(certFile, uploadedCert);
            Files.copy(keyFile, uploadedKey);
            running.certificates().reloadFromUpload(uploadedCert, uploadedKey);
            var sslFactory = running.server().getSslContextFactory().orElseThrow();
            sslFactory.reload(running.certificates()::applyTo);

            HttpResponse<String> after = client.send(
                    HttpRequest.newBuilder(URI.create(base + "/api/health")).GET().build(),
                    HttpResponse.BodyHandlers.ofString());
            assertEquals(200, after.statusCode(), "TLS must keep working after reload: " + after.body());
        }
    }

    /// The generated pair must carry this machine's own addresses: a deployment
    /// without a domain reaches the panel at the plain IP, and "HTTPS is on"
    /// must not turn that address into a hostname mismatch instead of a page.
    @Test
    void selfSignedCertificateCoversTheMachinesOwnAddresses() throws Exception {
        try (TestSupport.RunningServer running = TestSupport.start(dataDir, Map.of(),
                config -> {
                    config.bindHost = "127.0.0.1";
                    config.https.enabled = true;
                })) {
            assertEquals(CertificateManager.Source.SELF_SIGNED, running.certificates().getSource(),
                    "the certificate is generated on first use");
            Path certFile = dataDir.resolve("certs").resolve(CertificateManager.SELF_SIGNED_CERT_FILE);
            var certificate = (java.security.cert.X509Certificate) java.security.cert.CertificateFactory
                    .getInstance("X.509")
                    .generateCertificate(Files.newInputStream(certFile));

            List<String> dns = new ArrayList<>();
            List<String> addresses = new ArrayList<>();
            for (var entry : certificate.getSubjectAlternativeNames()) {
                int type = (Integer) entry.get(0);
                String value = String.valueOf(entry.get(1));
                if (type == 2) {
                    dns.add(value);
                } else if (type == 7) {
                    addresses.add(value);
                }
            }
            assertTrue(dns.contains("localhost"), dns.toString());
            assertTrue(addresses.contains("127.0.0.1"), addresses.toString());
            String lan = firstLanAddress();
            if (lan != null) {
                assertTrue(addresses.contains(lan),
                        "the SAN list must carry this machine's own address " + lan + ": " + addresses);
            }
        }
    }

    /// The machine's first non-loopback IPv4 address, or `null` when it has none.
    private static String firstLanAddress() throws java.net.SocketException {
        for (java.net.NetworkInterface nic : java.util.Collections.list(
                java.net.NetworkInterface.getNetworkInterfaces())) {
            for (java.net.InetAddress address : java.util.Collections.list(nic.getInetAddresses())) {
                String literal = address.getHostAddress();
                if (!address.isLoopbackAddress() && literal != null && !literal.contains(":")) {
                    return literal;
                }
            }
        }
        return null;
    }

    @Test
    void httpsOffNeedsNoCertificateMaterial() throws Exception {
        try (TestSupport.RunningServer running = TestSupport.start(dataDir, Map.of(),
                config -> config.bindHost = "127.0.0.1")) {
            assertEquals(CertificateManager.Source.NONE, running.certificates().getSource());
            assertTrue(running.server().getSslContextFactory().isEmpty());
            // Plain HTTP still works.
            HttpResponse<String> health = TestSupport.client().send(
                    HttpRequest.newBuilder(URI.create(running.baseUrl() + "/api/health")).GET().build(),
                    HttpResponse.BodyHandlers.ofString());
            assertEquals(200, health.statusCode());
        }
    }
}
