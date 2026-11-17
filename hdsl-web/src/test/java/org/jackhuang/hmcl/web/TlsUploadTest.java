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

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.ExtendedKeyUsage;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.openssl.jcajce.JcaMiscPEMGenerator;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.jackhuang.hmcl.web.tls.CertificateManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.math.BigInteger;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.Date;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The TLS settings endpoint: uploading a PEM pair on a plain-HTTP server
/// validates it, stores it (0600), records it in `server.yaml`, and switches
/// the very same port to TLS without a restart — after which the settings
/// endpoint describes the uploaded certificate, and a second upload hot-reloads.
/// A PKCS#12 upload takes the same path. Bad material is a 400 that changes nothing.
class TlsUploadTest {

    @TempDir
    Path dataDir;

    private TestSupport.RunningServer startServer() throws Exception {
        return TestSupport.start(dataDir, Map.of(), config -> {
            config.bindHost = "127.0.0.1";
            config.auth.disabled = true;
        });
    }

    @Test
    void pemUploadHotEnablesTlsOnTheSamePort() throws Exception {
        try (TestSupport.RunningServer running = startServer()) {
            HttpClient plain = TestSupport.client();
            String httpBase = running.baseUrl();
            int port = running.port();

            // Plain HTTP answers before the upload; the settings say so.
            assertEquals(200, get(plain, httpBase + "/api/health").statusCode());
            HttpResponse<String> before = get(plain, httpBase + "/api/settings/tls");
            assertEquals(200, before.statusCode(), before.body());
            JsonObject beforeJson = json(before);
            assertFalse(beforeJson.get("https").getAsBoolean());
            assertEquals("none", beforeJson.get("source").getAsString());

            // Upload a real (self-signed) PEM pair.
            PemPair pair = generateSelfSigned("TlsUploadTest One");
            HttpResponse<String> upload = multipart(plain, httpBase + "/api/settings/tls",
                    Map.of(), Map.of("certFile", pair.certPem(), "keyFile", pair.keyPem()));
            assertEquals(200, upload.statusCode(), upload.body());
            JsonObject uploadJson = json(upload);
            assertTrue(uploadJson.get("ok").getAsBoolean(), upload.body());
            assertFalse(uploadJson.get("restartRequired").getAsBoolean());
            assertTrue(uploadJson.get("https").getAsBoolean());

            // The material is stored owner-only under <data>/certs/.
            Path certFile = dataDir.resolve("certs").resolve(CertificateManager.UPLOADED_CERT_FILE);
            Path keyFile = dataDir.resolve("certs").resolve(CertificateManager.UPLOADED_KEY_FILE);
            assertTrue(Files.isRegularFile(certFile));
            assertTrue(Files.isRegularFile(keyFile));
            org.jackhuang.hmcl.util.PosixPermissions.assertOwnerOnly(keyFile,
                    "the uploaded key must be 0600");

            // server.yaml points at the upload with TLS on.
            String yaml = Files.readString(dataDir.resolve("server.yaml"));
            assertTrue(yaml.contains("enabled: true"), yaml);
            assertTrue(yaml.contains("cert_file"), yaml);

            // The very same port now completes a TLS handshake.
            HttpClient tls = TestSupport.trustAllClient();
            String httpsBase = "https://127.0.0.1:" + port;
            HttpResponse<String> health = get(tls, httpsBase + "/api/health");
            assertEquals(200, health.statusCode(), "TLS must be live on the same port: " + health.body());

            // The settings endpoint describes the uploaded certificate.
            HttpResponse<String> settings = get(tls, httpsBase + "/api/settings/tls");
            assertEquals(200, settings.statusCode(), settings.body());
            JsonObject settingsJson = json(settings);
            assertTrue(settingsJson.get("https").getAsBoolean());
            assertEquals("uploaded", settingsJson.get("source").getAsString());
            assertTrue(settingsJson.get("subject").getAsString().contains("TlsUploadTest One"),
                    settingsJson.toString());
            assertNotNull(settingsJson.get("issuer"));
            assertNotNull(settingsJson.get("expires"));
            assertEquals(64, settingsJson.get("fingerprintSha256").getAsString().length());
            assertTrue(settingsJson.getAsJsonArray("sans").toString().contains("127.0.0.1"));

            // A second upload on a live TLS server hot-reloads.
            PemPair second = generateSelfSigned("TlsUploadTest Two");
            HttpResponse<String> reupload = multipart(tls, httpsBase + "/api/settings/tls",
                    Map.of(), Map.of("certFile", second.certPem(), "keyFile", second.keyPem()));
            assertEquals(200, reupload.statusCode(), reupload.body());
            JsonObject reuploadJson = json(reupload);
            assertTrue(reuploadJson.get("ok").getAsBoolean(), reupload.body());
            assertFalse(reuploadJson.get("restartRequired").getAsBoolean());

            HttpResponse<String> reloaded = get(tls, httpsBase + "/api/settings/tls");
            assertEquals(200, reloaded.statusCode(), "TLS keeps working after the reload: " + reloaded.body());
            assertTrue(json(reloaded).get("subject").getAsString().contains("TlsUploadTest Two"),
                    reloaded.body());
        }
    }

    @Test
    void pkcs12UploadHotEnablesTls() throws Exception {
        try (TestSupport.RunningServer running = startServer()) {
            PemPair pair = generateSelfSigned("TlsUploadTest P12");
            String password = "changeit-123";
            KeyStore p12 = KeyStore.getInstance("PKCS12");
            p12.load(null, null);
            p12.setKeyEntry("server", pair.keyPair().getPrivate(), password.toCharArray(),
                    new java.security.cert.Certificate[]{pair.certificate()});
            ByteArrayOutputStream p12Bytes = new ByteArrayOutputStream();
            p12.store(p12Bytes, password.toCharArray());

            HttpClient plain = TestSupport.client();
            String httpBase = running.baseUrl();
            HttpResponse<String> upload = multipart(plain, httpBase + "/api/settings/tls",
                    Map.of("password", password),
                    Map.of("p12File", p12Bytes.toByteArray()));
            assertEquals(200, upload.statusCode(), upload.body());
            assertTrue(json(upload).get("ok").getAsBoolean(), upload.body());

            assertTrue(Files.isRegularFile(dataDir.resolve("certs")
                    .resolve(CertificateManager.UPLOADED_PKCS12_FILE)));
            String yaml = Files.readString(dataDir.resolve("server.yaml"));
            assertTrue(yaml.contains("pkcs12_file"), yaml);

            HttpClient tls = TestSupport.trustAllClient();
            String httpsBase = "https://127.0.0.1:" + running.port();
            assertEquals(200, get(tls, httpsBase + "/api/health").statusCode());
            JsonObject settings = json(get(tls, httpsBase + "/api/settings/tls"));
            assertEquals("pkcs12", settings.get("source").getAsString());
            assertTrue(settings.get("subject").getAsString().contains("TlsUploadTest P12"));
        }
    }

    @Test
    void badUploadsAreRejectedWithoutTouchingTheLiveServer() throws Exception {
        try (TestSupport.RunningServer running = startServer()) {
            HttpClient plain = TestSupport.client();
            String httpBase = running.baseUrl();

            // A certificate with somebody else's key.
            PemPair one = generateSelfSigned("TlsUploadTest A");
            PemPair two = generateSelfSigned("TlsUploadTest B");
            HttpResponse<String> mismatched = multipart(plain, httpBase + "/api/settings/tls",
                    Map.of(), Map.of("certFile", one.certPem(), "keyFile", two.keyPem()));
            assertEquals(400, mismatched.statusCode(), mismatched.body());
            assertFalse(Files.exists(dataDir.resolve("certs").resolve(CertificateManager.UPLOADED_CERT_FILE)),
                    "a rejected upload leaves nothing behind");

            // Garbage instead of PEM.
            HttpResponse<String> garbage = multipart(plain, httpBase + "/api/settings/tls",
                    Map.of(), Map.of(
                            "certFile", "not a pem at all".getBytes(StandardCharsets.UTF_8),
                            "keyFile", "neither is this".getBytes(StandardCharsets.UTF_8)));
            assertEquals(400, garbage.statusCode(), garbage.body());

            // The server is exactly as before: plain HTTP, source none.
            assertEquals(200, get(plain, httpBase + "/api/health").statusCode());
            JsonObject settings = json(get(plain, httpBase + "/api/settings/tls"));
            assertFalse(settings.get("https").getAsBoolean());
            assertEquals("none", settings.get("source").getAsString());
        }
    }

    // ----------------------------------------------------------------- helpers --

    /// A self-signed PEM pair, generated the way CertificateManager's own is.
    private record PemPair(KeyPair keyPair, X509Certificate certificate, byte[] certPem, byte[] keyPem) {
    }

    private static PemPair generateSelfSigned(String cn) throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keyPair = generator.generateKeyPair();

        X500Name subject = new X500Name("CN=" + cn);
        long now = System.currentTimeMillis();
        Date notBefore = new Date(now - Duration.ofDays(1).toMillis());
        Date notAfter = new Date(now + Duration.ofDays(90).toMillis());
        JcaX509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(
                subject, new BigInteger(64, new SecureRandom()), notBefore, notAfter, subject, keyPair.getPublic());
        builder.addExtension(Extension.basicConstraints, true, new BasicConstraints(false));
        builder.addExtension(Extension.keyUsage, true,
                new KeyUsage(KeyUsage.digitalSignature | KeyUsage.keyEncipherment));
        builder.addExtension(Extension.extendedKeyUsage, false,
                new ExtendedKeyUsage(KeyPurposeId.id_kp_serverAuth));
        builder.addExtension(Extension.subjectAlternativeName, false, new GeneralNames(new GeneralName[]{
                new GeneralName(GeneralName.dNSName, "localhost"),
                new GeneralName(GeneralName.iPAddress, new DEROctetString(new byte[]{127, 0, 0, 1})),
        }));
        ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA").build(keyPair.getPrivate());
        X509Certificate certificate = new JcaX509CertificateConverter().getCertificate(builder.build(signer));
        return new PemPair(keyPair, certificate, pem(certificate), pem(keyPair.getPrivate()));
    }

    private static byte[] pem(Object object) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (JcaPEMWriter writer = new JcaPEMWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8))) {
            writer.writeObject(new JcaMiscPEMGenerator(object));
        }
        return out.toByteArray();
    }

    /// A multipart POST with text fields and file parts (part name → bytes).
    private static HttpResponse<String> multipart(HttpClient client, String url,
                                                  Map<String, String> fields, Map<String, byte[]> files)
            throws Exception {
        String boundary = "----hdsl-tls-test-boundary";
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        for (Map.Entry<String, String> field : fields.entrySet()) {
            body.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\""
                    + field.getKey() + "\"\r\n\r\n" + field.getValue() + "\r\n")
                    .getBytes(StandardCharsets.UTF_8));
        }
        for (Map.Entry<String, byte[]> file : files.entrySet()) {
            body.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"" + file.getKey()
                    + "\"; filename=\"" + file.getKey() + ".bin\"\r\n"
                    + "Content-Type: application/octet-stream\r\n\r\n").getBytes(StandardCharsets.UTF_8));
            body.write(file.getValue());
            body.write("\r\n".getBytes(StandardCharsets.UTF_8));
        }
        body.write(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
        return client.send(HttpRequest.newBuilder(URI.create(url))
                        .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                        .POST(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray())).build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private static HttpResponse<String> get(HttpClient client, String url) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create(url)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private static JsonObject json(HttpResponse<String> response) {
        return JsonParser.parseString(response.body()).getAsJsonObject();
    }
}
