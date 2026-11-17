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

import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.ExtendedKeyUsage;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.openssl.PEMEncryptedKeyPair;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaMiscPEMGenerator;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS8EncryptedPrivateKeyInfo;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.jackhuang.hmcl.util.logging.Logger;
import org.jackhuang.hmcl.web.config.ServerConfig;
import org.jackhuang.hmcl.web.config.ServerConfigLoader;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/// Everything TLS: where the server's identity comes from and how it reaches
/// the Jetty [SslContextFactory].
///
/// Load order, first match wins:
/// 1. `https.pkcs12_file` — a PKCS#12 keystore (JDK `KeyStore`, no BC needed)
/// 2. `https.cert_file` + `https.key_file` — PEM chain + unencrypted PEM key
/// 3. a previously generated self-signed pair in `<data>/certs/` (reused so
///    restarts do not churn the certificate)
/// 4. with HTTPS enabled and none of the above: generate a self-signed
///    certificate (BC, RSA-2048, CN=HDSL-web, SAN=localhost, 127.0.0.1 plus
///    the `public_base_url` host, valid 825 days) and write it to
///    `<data>/certs/self-signed.pem` / `self-signed-key.pem` (key 0600).
///
/// The material is always normalized into one in-memory JKS [KeyStore]
/// holding the private key and its chain, which [applyTo] hands to Jetty.
/// Certificate replacement without a restart goes through
/// [reloadFromUpload] plus `SslContextFactory.reload(...)` (Phase 2 wires
/// this to the upload UI).
@NotNullByDefault
public final class CertificateManager {

    /// Where the server's identity currently comes from.
    public enum Source {
        /// HTTPS is off; nothing is loaded.
        NONE,
        /// A PKCS#12 keystore from `https.pkcs12_file`.
        PKCS12,
        /// A PEM certificate chain + private key from the config or an upload.
        PEM,
        /// A self-signed certificate generated (or reused) by this manager.
        SELF_SIGNED,
    }

    /// The names an uploaded identity is stored under in `<data>/certs/`.
    public static final String UPLOADED_CERT_FILE = "uploaded.pem";
    public static final String UPLOADED_KEY_FILE = "uploaded.key";
    public static final String UPLOADED_PKCS12_FILE = "uploaded.p12";

    /// What the settings endpoint reports about the current identity.
    ///
    /// @param subject           the certificate's subject
    /// @param issuer            its issuer
    /// @param sans              its subject alternative names (`DNS:…`, `IP:…`)
    /// @param expires           when the certificate stops being valid
    /// @param fingerprintSha256 the SHA-256 fingerprint of the certificate, hex
    public record Info(String subject, String issuer, List<String> sans,
                       java.time.Instant expires, String fingerprintSha256) {
    }

    /// Days a generated self-signed certificate stays valid.
    private static final long SELF_SIGNED_VALIDITY_DAYS = 825;
    private static final String SELF_SIGNED_CN = "HDSL-web";

    public static final String SELF_SIGNED_CERT_FILE = "self-signed.pem";
    public static final String SELF_SIGNED_KEY_FILE = "self-signed-key.pem";

    private final Logger logger;
    private final Path dataDir;
    private final ServerConfig.HttpsConfig https;
    private final String publicBaseUrl;
    private static final SecureRandom RANDOM = new SecureRandom();

    private KeyStore keyStore;
    private String keyStorePassword;
    private Source source = Source.NONE;

    private CertificateManager(Path dataDir, ServerConfig.HttpsConfig https, String publicBaseUrl, Logger logger) {
        this.dataDir = dataDir;
        this.https = https;
        this.publicBaseUrl = publicBaseUrl;
        this.logger = logger;
    }

    /// Loads (or generates) the certificate material described by `config`.
    public static CertificateManager create(ServerConfig config, Logger logger) throws IOException {
        CertificateManager manager = new CertificateManager(
                config.dataDir, config.https, config.publicBaseUrl, logger);
        if (!config.https.enabled) {
            manager.source = Source.NONE;
            return manager;
        }
        manager.load();
        return manager;
    }

    private void load() throws IOException {
        if (!https.pkcs12File.isBlank()) {
            loadPkcs12(resolve(https.pkcs12File), https.pkcs12Password);
            return;
        }
        if (!https.certFile.isBlank()) {
            loadPem(resolve(https.certFile), resolve(https.keyFile));
            return;
        }
        Path certPath = selfSignedCertPath();
        Path keyPath = selfSignedKeyPath();
        if (Files.exists(certPath) && Files.exists(keyPath)) {
            loadPem(certPath, keyPath);
            source = Source.SELF_SIGNED;
            logger.info("Reusing the self-signed certificate from " + certPath
                    + "; replace it by configuring https.cert_file or https.pkcs12_file.");
            return;
        }
        generateSelfSigned();
    }

    // ---------------------------------------------------------------- loading --

    private void loadPkcs12(Path path, String password) throws IOException {
        KeyStore p12;
        try {
            p12 = KeyStore.getInstance("PKCS12");
            try (InputStream in = Files.newInputStream(path)) {
                p12.load(in, password.toCharArray());
            }
        } catch (Exception e) {
            throw new ServerConfigLoader.ConfigException("Failed to load PKCS#12 keystore " + path
                    + " (wrong password or not a PKCS#12 file): " + e.getMessage(), e);
        }

        String jksPassword = newPassword();
        KeyStore jks;
        try {
            jks = KeyStore.getInstance("JKS");
            jks.load(null, jksPassword.toCharArray());
        } catch (Exception e) {
            throw new IllegalStateException("This JRE has no JKS keystore", e);
        }
        int keys = 0;
        try {
            Enumeration<String> aliases = p12.aliases();
            while (aliases.hasMoreElements()) {
                String alias = aliases.nextElement();
                if (p12.isKeyEntry(alias)) {
                    java.security.Key key = p12.getKey(alias, password.toCharArray());
                    java.security.cert.Certificate[] chain = p12.getCertificateChain(alias);
                    if (chain == null || chain.length == 0) {
                        throw new ServerConfigLoader.ConfigException("PKCS#12 keystore " + path
                                + ": entry `" + alias + "` has no certificate chain");
                    }
                    if (key instanceof PrivateKey privateKey) {
                        requireMatchingKey(privateKey, chain[0], "PKCS#12 keystore " + path);
                    }
                    jks.setKeyEntry(alias, key, jksPassword.toCharArray(), chain);
                    keys++;
                }
            }
        } catch (ServerConfigLoader.ConfigException e) {
            throw e;
        } catch (Exception e) {
            throw new ServerConfigLoader.ConfigException("Failed to read PKCS#12 keystore " + path + ": " + e.getMessage(), e);
        }
        if (keys == 0) {
            throw new ServerConfigLoader.ConfigException("PKCS#12 keystore " + path + " contains no private key");
        }
        install(new KeystoreMaterial(jks, jksPassword), Source.PKCS12);
        logger.info("TLS identity loaded from PKCS#12 keystore " + path);
    }

    private void loadPem(Path certPath, Path keyPath) throws IOException {
        install(buildKeyStoreFromPem(certPath, keyPath), Source.PEM);
        logger.info("TLS identity loaded from " + certPath + " and " + keyPath);
    }

    /// Builds the in-memory keystore from an unencrypted PEM chain and key.
    private KeystoreMaterial buildKeyStoreFromPem(Path certPath, Path keyPath) throws IOException {
        List<X509Certificate> chain = new ArrayList<>();
        try (PEMParser parser = new PEMParser(Files.newBufferedReader(certPath, StandardCharsets.UTF_8))) {
            Object object;
            while ((object = parser.readObject()) != null) {
                if (object instanceof X509CertificateHolder holder) {
                    chain.add(new JcaX509CertificateConverter().getCertificate(holder));
                }
            }
        } catch (Exception e) {
            throw new ServerConfigLoader.ConfigException("Failed to parse PEM certificates in " + certPath + ": " + e.getMessage(), e);
        }
        if (chain.isEmpty()) {
            throw new ServerConfigLoader.ConfigException("No certificates found in " + certPath);
        }

        PrivateKey key;
        try (PEMParser parser = new PEMParser(Files.newBufferedReader(keyPath, StandardCharsets.UTF_8))) {
            Object object = parser.readObject();
            JcaPEMKeyConverter converter = new JcaPEMKeyConverter();
            if (object instanceof PEMKeyPair pair) {
                key = converter.getKeyPair(pair).getPrivate();
            } else if (object instanceof PrivateKeyInfo info) {
                key = converter.getPrivateKey(info);
            } else if (object instanceof PEMEncryptedKeyPair || object instanceof PKCS8EncryptedPrivateKeyInfo) {
                throw new ServerConfigLoader.ConfigException(keyPath + ": encrypted private keys are not supported; "
                        + "store the key unencrypted (or use https.pkcs12_file with a password)");
            } else {
                throw new ServerConfigLoader.ConfigException(keyPath + ": no private key found (expected a PEM key)");
            }
        } catch (ServerConfigLoader.ConfigException e) {
            throw e;
        } catch (Exception e) {
            throw new ServerConfigLoader.ConfigException("Failed to parse PEM private key in " + keyPath + ": " + e.getMessage(), e);
        }
        return buildKeyStore(key, chain, keyPath + " does not match " + certPath);
    }

    // ------------------------------------------------------- self-signed bits --

    private void generateSelfSigned() throws IOException {
        Path certPath = selfSignedCertPath();
        Path keyPath = selfSignedKeyPath();
        Files.createDirectories(certPath.getParent());

        KeyPair keyPair;
        X509Certificate certificate;
        try {
            keyPair = generateRsaKeyPair();
            certificate = buildSelfSignedCertificate(keyPair);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to generate a self-signed certificate", e);
        }

        writePem(certPath, certificate);
        writePem(keyPath, keyPair.getPrivate());
        setOwnerOnly(keyPath);
        install(buildKeyStore(keyPair.getPrivate(), List.of(certificate),
                "the generated self-signed certificate"), Source.SELF_SIGNED);

        logger.warning("No TLS certificate configured: generated a self-signed certificate in " + certPath
                + " (CN=" + SELF_SIGNED_CN + ", valid " + SELF_SIGNED_VALIDITY_DAYS + " days). "
                + "Browsers will show a warning for it.");
        logger.warning("To use a real certificate, set https.cert_file/https.key_file (PEM) or "
                + "https.pkcs12_file (PKCS#12) in server.yaml, or upload one from the panel once Phase 2 ships.");
    }

    private KeyPair generateRsaKeyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return generator.generateKeyPair();
    }

    private X509Certificate buildSelfSignedCertificate(KeyPair keyPair) throws Exception {
        X500Name subject = new X500Name("CN=" + SELF_SIGNED_CN);
        long now = System.currentTimeMillis();
        Date notBefore = new Date(now - Duration.ofDays(1).toMillis());
        Date notAfter = new Date(now + Duration.ofDays(SELF_SIGNED_VALIDITY_DAYS).toMillis());

        JcaX509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(
                subject, new BigInteger(64, RANDOM), notBefore, notAfter, subject, keyPair.getPublic());
        builder.addExtension(Extension.basicConstraints, true, new BasicConstraints(false));
        builder.addExtension(Extension.keyUsage, true,
                new KeyUsage(KeyUsage.digitalSignature | KeyUsage.keyEncipherment));
        builder.addExtension(Extension.extendedKeyUsage, false,
                new ExtendedKeyUsage(KeyPurposeId.id_kp_serverAuth));
        builder.addExtension(Extension.subjectAlternativeName, false, buildSubjectAlternativeNames());

        ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA").build(keyPair.getPrivate());
        return new JcaX509CertificateConverter().getCertificate(builder.build(signer));
    }

    /// SAN = `localhost` + `127.0.0.1` + **every address this machine answers on**
    /// (plus its host name) + the host of `public_base_url` (if any), so the
    /// browser's hostname check can pass for every realistic local or proxied URL
    /// this instance is reached under.
    ///
    /// The machine's own addresses are the point of the exercise for a deployment
    /// without a domain: the address people type there **is** the IP, and a
    /// self-signed certificate without it turns "HTTPS is on" into a hostname
    /// mismatch instead of a page.
    private GeneralNames buildSubjectAlternativeNames() {
        List<GeneralName> names = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        names.add(new GeneralName(GeneralName.dNSName, "localhost"));
        seen.add("dns:localhost");
        names.add(new GeneralName(GeneralName.iPAddress, new DEROctetString(new byte[]{127, 0, 0, 1})));
        seen.add("ip:127.0.0.1");
        addLocalNames(names, seen);
        String publicHost = publicBaseHost();
        if (publicHost != null) {
            names.add(hostGeneralName(publicHost));
        }
        return new GeneralNames(names.toArray(new GeneralName[0]));
    }

    /// Adds the machine's own addresses and host name to the SAN list, skipping
    /// anything already there. Interfaces come and go (a VPN dials in, a phone
    /// hotspot drops), so this is read at generation time and re-read whenever a
    /// certificate is regenerated — a stale list is the one thing a self-signed
    /// pair cannot fix on its own.
    private void addLocalNames(List<GeneralName> names, Set<String> seen) {
        try {
            for (NetworkInterface nic : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                for (InetAddress address : Collections.list(nic.getInetAddresses())) {
                    byte[] octets = address.getAddress();
                    if (octets == null) {
                        continue;
                    }
                    String key = "ip:" + address.getHostAddress();
                    if (seen.add(key)) {
                        names.add(new GeneralName(GeneralName.iPAddress, new DEROctetString(octets)));
                    }
                }
            }
        } catch (SocketException e) {
            logger.warning("Cannot enumerate this machine's addresses; the certificate keeps the loopback names only");
        }
        try {
            String hostName = InetAddress.getLocalHost().getHostName();
            if (hostName != null && !hostName.isBlank() && !hostName.contains(":") && seen.add("dns:" + hostName)) {
                names.add(new GeneralName(GeneralName.dNSName, hostName));
            }
        } catch (Exception ignored) {
            // Host-name lookup is best-effort; the addresses above already cover the IP case.
        }
    }

    private @Nullable String publicBaseHost() {
        if (publicBaseUrl == null || publicBaseUrl.isBlank()) {
            return null;
        }
        try {
            String host = new URI(publicBaseUrl).getHost();
            return host == null || host.isBlank() ? null : host;
        } catch (URISyntaxException e) {
            logger.warning("public_base_url `" + publicBaseUrl + "` is not a valid URL; its host is skipped in the certificate SAN list");
            return null;
        }
    }

    private static GeneralName hostGeneralName(String host) {
        String clean = host;
        if (clean.startsWith("[") && clean.endsWith("]")) {
            clean = clean.substring(1, clean.length() - 1);
        }
        try {
            InetAddress address = InetAddress.getByName(clean);
            // Only treat it as an IP when the host really is an IP literal;
            // anything else goes in as a DNS name.
            if (clean.equals(address.getHostAddress()) || address instanceof java.net.Inet6Address) {
                return new GeneralName(GeneralName.iPAddress, new DEROctetString(address.getAddress()));
            }
        } catch (Exception ignored) {
        }
        return new GeneralName(GeneralName.dNSName, clean);
    }

    // ------------------------------------------------------------- plumbing --

    /// Hands the current identity to a Jetty [SslContextFactory]. The setters
    /// live on the base class, so the method also serves directly as the
    /// consumer for `factory.reload(...)` (whose real signature is
    /// `reload(Consumer<SslContextFactory>) throws Exception`), which is what
    /// makes certificate hot-reload work without a restart.
    public void applyTo(SslContextFactory ssl) {
        if (keyStore == null) {
            throw new IllegalStateException("HTTPS is not enabled: no certificate material to apply");
        }
        ssl.setKeyStore(keyStore);
        ssl.setKeyStorePassword(keyStorePassword);
    }

    /// Replaces the identity from freshly uploaded PEM files (Phase 2's
    /// upload UI calls this, then the caller triggers
    /// `SslContextFactory.reload(this::applyTo)` on the live factory).
    ///
    /// Paths are resolved against the data directory when relative, so a UI
    /// may pass either form.
    public synchronized void reloadFromUpload(Path certPem, Path keyPem) throws IOException {
        Path certPath = resolve(certPem);
        Path keyPath = resolve(keyPem);
        install(buildKeyStoreFromPem(certPath, keyPath), Source.PEM);
        https.certFile = certPem.toString();
        https.keyFile = keyPem.toString();
        logger.info("Replaced the TLS identity with " + certPath + " and " + keyPath);
    }

    /// Installs an uploaded PKCS#12 keystore as the current identity.
    ///
    /// The file is validated — readable, right password, key matching its
    /// certificate — before anything changes, so a bad upload leaves the
    /// running identity untouched. The PEM pair in the config is cleared:
    /// PKCS#12 wins the load order, and a stale `cert_file` would be a lie
    /// about which identity is live.
    ///
    /// @param p12      the keystore file
    /// @param password its password
    /// @throws IOException when the file cannot be read or does not hold a usable key
    public synchronized void installUploadedPkcs12(Path p12, String password) throws IOException {
        Path path = resolve(p12);
        loadPkcs12(path, password);
        https.pkcs12File = p12.toString();
        https.pkcs12Password = password;
        https.certFile = "";
        https.keyFile = "";
        logger.info("Replaced the TLS identity with the PKCS#12 keystore " + path);
    }

    /// Installs an uploaded PEM pair as the current identity, validated before
    /// anything changes (unparseable or mismatched material leaves the running
    /// identity untouched).
    ///
    /// @param certPem the certificate chain
    /// @param keyPem  the unencrypted private key
    /// @throws IOException when the files cannot be read or do not pair up
    public synchronized void installUploadedPem(Path certPem, Path keyPem) throws IOException {
        reloadFromUpload(certPem, keyPem);
        https.pkcs12File = "";
        https.pkcs12Password = "";
    }

    /// Makes a self-signed identity the one this manager serves: reuses the
    /// pair already under `<data>/certs/`, generates one otherwise. It does
    /// **not** touch `server.yaml` and does not switch the connector — the
    /// caller persists the choice and calls `enableTls`/`reloadTls`.
    ///
    /// @return what the certificate says about itself
    /// @throws IOException when the pair cannot be read or generated
    public synchronized Info useSelfSigned() throws IOException {
        loadSelfSigned();
        Info loaded = info();
        if (loaded == null) {
            throw new IOException("the self-signed certificate could not be loaded");
        }
        return loaded;
    }

    private void loadSelfSigned() throws IOException {
        Path certPath = selfSignedCertPath();
        Path keyPath = selfSignedKeyPath();
        if (Files.exists(certPath) && Files.exists(keyPath)) {
            install(buildKeyStoreFromPem(certPath, keyPath), Source.SELF_SIGNED);
            logger.info("Reusing the self-signed certificate from " + certPath);
        } else {
            generateSelfSigned();
        }
    }

    /// What the current identity's certificate says about itself, for the
    /// settings endpoint; `null` when no material is loaded.
    public synchronized @Nullable Info info() {
        KeyStore current = keyStore;
        if (current == null) {
            return null;
        }
        try {
            Enumeration<String> aliases = current.aliases();
            while (aliases.hasMoreElements()) {
                String alias = aliases.nextElement();
                if (!current.isKeyEntry(alias)) {
                    continue;
                }
                java.security.cert.Certificate[] chain = current.getCertificateChain(alias);
                if (chain != null && chain.length > 0
                        && chain[0] instanceof X509Certificate certificate) {
                    return infoOf(certificate);
                }
            }
            return null;
        } catch (Exception e) {
            logger.info("Could not read the certificate details: " + e.getMessage());
            return null;
        }
    }

    private static Info infoOf(X509Certificate certificate) {
        List<String> sans = new ArrayList<>();
        try {
            var names = certificate.getSubjectAlternativeNames();
            if (names != null) {
                for (List<?> name : names) {
                    if (name.size() >= 2 && name.get(1) != null) {
                        Integer type = name.get(0) instanceof Integer t ? t : null;
                        String prefix = type == null ? "" : switch (type) {
                            case 2 -> "DNS:";
                            case 7 -> "IP:";
                            default -> "";
                        };
                        sans.add(prefix + name.get(1));
                    }
                }
            }
        } catch (Exception ignored) {
            // A certificate whose SANs cannot be read still has the rest to show.
        }
        String fingerprint;
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            StringBuilder hex = new StringBuilder();
            for (byte value : digest.digest(certificate.getEncoded())) {
                hex.append(String.format("%02x", value));
            }
            fingerprint = hex.toString();
        } catch (Exception e) {
            fingerprint = "";
        }
        return new Info(certificate.getSubjectX500Principal().getName(),
                certificate.getIssuerX500Principal().getName(),
                List.copyOf(sans), certificate.getNotAfter().toInstant(), fingerprint);
    }

    /// Where the current identity came from; for the startup banner.
    public Source getSource() {
        return source;
    }

    public Path selfSignedCertPath() {
        return dataDir.resolve("certs").resolve(SELF_SIGNED_CERT_FILE);
    }

    public Path selfSignedKeyPath() {
        return dataDir.resolve("certs").resolve(SELF_SIGNED_KEY_FILE);
    }

    /// An in-memory keystore together with the password that unlocks it. The
    /// two must travel together: Jetty's [SslContextFactory] reads the key
    /// with exactly the password [applyTo] hands over.
    private record KeystoreMaterial(KeyStore keyStore, String password) {
    }

    private void install(KeystoreMaterial material, Source newSource) {
        keyStore = material.keyStore();
        keyStorePassword = material.password();
        source = newSource;
    }

    private KeystoreMaterial buildKeyStore(PrivateKey key, List<X509Certificate> chain, String source)
            throws IOException {
        requireMatchingKey(key, chain.get(0), source);
        String password = newPassword();
        try {
            KeyStore ks = KeyStore.getInstance("JKS");
            ks.load(null, password.toCharArray());
            ks.setKeyEntry("server", key, password.toCharArray(), chain.toArray(new X509Certificate[0]));
            return new KeystoreMaterial(ks, password);
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to build the in-memory keystore", e);
        }
    }

    /// Checks that a private key is the one a certificate was issued for, by
    /// signing a nonce with the key and verifying it against the certificate's
    /// public key. A keystore accepts a mismatched pair without a word, and the
    /// failure would otherwise surface as a handshake error with no useful
    /// message — the upload endpoint asks here so it can say what is wrong.
    private static void requireMatchingKey(PrivateKey key, java.security.cert.Certificate certificate,
                                           String source) {
        String keyAlgorithm = key.getAlgorithm();
        String signatureAlgorithm = switch (keyAlgorithm == null ? "" : keyAlgorithm.toUpperCase(java.util.Locale.ROOT)) {
            case "RSA" -> "SHA256withRSA";
            case "EC", "ECDSA" -> "SHA256withECDSA";
            case "ED25519", "EDDSA" -> "Ed25519";
            case "DSA" -> "SHA256withDSA";
            default -> throw new ServerConfigLoader.ConfigException(
                    source + ": keys of type " + keyAlgorithm + " are not supported");
        };
        try {
            byte[] nonce = new byte[32];
            RANDOM.nextBytes(nonce);
            java.security.Signature signer = java.security.Signature.getInstance(signatureAlgorithm);
            signer.initSign(key);
            signer.update(nonce);
            byte[] signed = signer.sign();
            java.security.Signature verifier = java.security.Signature.getInstance(signatureAlgorithm);
            verifier.initVerify(certificate.getPublicKey());
            verifier.update(nonce);
            if (!verifier.verify(signed)) {
                throw new ServerConfigLoader.ConfigException(
                        source + ": the private key does not belong to the certificate");
            }
        } catch (ServerConfigLoader.ConfigException e) {
            throw e;
        } catch (Exception e) {
            throw new ServerConfigLoader.ConfigException(source
                    + ": could not check the key against the certificate: " + e.getMessage(), e);
        }
    }

    private String newPassword() {
        byte[] bytes = new byte[24];
        RANDOM.nextBytes(bytes);
        return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private Path resolve(String path) {
        Path p = Path.of(path);
        return p.isAbsolute() ? p.normalize() : dataDir.resolve(p).normalize();
    }

    private Path resolve(Path path) {
        return path.isAbsolute() ? path.normalize() : dataDir.resolve(path).normalize();
    }

    private static void writePem(Path path, Object object) throws IOException {
        try (OutputStream out = Files.newOutputStream(path);
             JcaPEMWriter writer = new JcaPEMWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8))) {
            writer.writeObject(new JcaMiscPEMGenerator(object));
        }
    }

    private static void setOwnerOnly(Path path) {
        try {
            Set<PosixFilePermission> perms = EnumSet.of(
                    PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE);
            Files.setPosixFilePermissions(path, perms);
        } catch (IOException | UnsupportedOperationException e) {
            // Best effort; non-POSIX filesystems cannot express this.
        }
    }
}
