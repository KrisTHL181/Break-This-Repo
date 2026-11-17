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
package org.jackhuang.hmcl.web.http;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import org.jackhuang.hmcl.util.logging.Logger;
import org.jackhuang.hmcl.web.audit.AuditLog;
import org.jackhuang.hmcl.web.auth.AuthFilter;
import org.jackhuang.hmcl.web.auth.AuthService;
import org.jackhuang.hmcl.web.config.ServerConfig;
import org.jackhuang.hmcl.web.config.ServerConfigLoader;
import org.jackhuang.hmcl.web.config.ServerConfigWriter;
import org.jackhuang.hmcl.web.server.HdslServer;
import org.jackhuang.hmcl.web.tls.CertificateManager;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermission;
import java.util.EnumSet;
import java.util.Set;
import java.util.function.Supplier;

/// The TLS settings endpoint, mapped at `/api/settings/*`:
///
/// - `GET  /api/settings/tls` — what the main connector currently serves:
///   `{https, source, subject?, issuer?, sans?, expires?, fingerprintSha256?}`
/// - `POST /api/settings/tls` — upload an identity, multipart: `certFile` +
///   `keyFile` (a PEM pair) or `p12File` + `password` (PKCS#12). The material
///   is validated — parseable, key matching the certificate — stored under
///   `<data>/certs/uploaded.{pem,key,p12}` (0600), recorded in `server.yaml`,
///   and applied **without a restart**: a TLS server hot-reloads its
///   [org.eclipse.jetty.util.ssl.SslContextFactory], a plain-HTTP server swaps
///   its connector for a TLS one on the same port. A failed swap rolls the
///   connector back and answers `{ok:false, restartRequired:true, error}` —
///   the config on disk already points at the upload, so a restart applies it.
@NotNullByDefault
public final class TlsApiServlet extends HttpServlet {

    private final ServerConfig config;
    private final @Nullable CertificateManager certificates;
    private final Supplier<@Nullable HdslServer> server;

    public TlsApiServlet(ServerConfig config, @Nullable CertificateManager certificates,
                         Supplier<@Nullable HdslServer> server) {
        this.config = config;
        this.certificates = certificates;
        this.server = server;
    }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String path = request.getPathInfo();
        if ("/tls/self-signed".equals(path)) {
            if (!"POST".equals(request.getMethod())) {
                Json.error(response, HttpServletResponse.SC_METHOD_NOT_ALLOWED, "method not allowed");
                return;
            }
            selfSigned(request, response);
            return;
        }
        if (!"/tls".equals(path)) {
            Json.error(response, HttpServletResponse.SC_NOT_FOUND, "not found");
            return;
        }
        switch (request.getMethod()) {
            case "GET" -> describe(response);
            case "POST" -> upload(request, response);
            default -> Json.error(response, HttpServletResponse.SC_METHOD_NOT_ALLOWED, "method not allowed");
        }
    }

    // --------------------------------------------------------------- describe --

    private void describe(HttpServletResponse response) throws IOException {
        JsonObject body = new JsonObject();
        HdslServer running = server.get();
        boolean https = running != null ? running.isTls() : config.https.enabled;
        body.addProperty("https", https);
        if (certificates == null) {
            body.addProperty("source", "none");
            Json.write(response, body);
            return;
        }
        body.addProperty("source", switch (certificates.getSource()) {
            case NONE -> "none";
            case SELF_SIGNED -> "self-signed";
            case PEM -> "uploaded";
            case PKCS12 -> "pkcs12";
        });
        CertificateManager.Info info = certificates.info();
        if (info != null) {
            body.addProperty("subject", info.subject());
            body.addProperty("issuer", info.issuer());
            JsonArray sans = new JsonArray();
            for (String san : info.sans()) {
                sans.add(san);
            }
            body.add("sans", sans);
            body.addProperty("expires", info.expires().toString());
            body.addProperty("fingerprintSha256", info.fingerprintSha256());
        }
        Json.write(response, body);
    }

    // ----------------------------------------------------------------- upload --

    private void upload(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HdslServer running = server.get();
        if (certificates == null || running == null) {
            Json.error(response, HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    "no certificate manager is attached to this server");
            return;
        }

        Part certPart;
        Part keyPart;
        Part p12Part;
        try {
            certPart = request.getPart("certFile");
            keyPart = request.getPart("keyFile");
            p12Part = request.getPart("p12File");
        } catch (ServletException | IllegalStateException e) {
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST,
                    "expected a multipart form with certFile+keyFile or p12File: " + e.getMessage());
            return;
        }

        boolean isP12 = p12Part != null && p12Part.getSize() > 0;
        if (!isP12 && (certPart == null || certPart.getSize() == 0 || keyPart == null || keyPart.getSize() == 0)) {
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST,
                    "certFile and keyFile are required, or p12File");
            return;
        }

        Path certsDir = config.dataDir.resolve("certs");
        Files.createDirectories(certsDir);

        // The files land first, the install validates them before the live
        // identity is touched, and a failure takes the files back out. The
        // yaml records the data-dir-relative form, so a moved volume still loads.
        if (isP12) {
            String password = request.getParameter("password");
            Path stored = certsDir.resolve(CertificateManager.UPLOADED_PKCS12_FILE);
            try {
                store(p12Part, stored);
                certificates.installUploadedPkcs12(stored, password == null ? "" : password);
            } catch (ServerConfigLoader.ConfigException e) {
                Files.deleteIfExists(stored);
                Json.error(response, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
                return;
            }
            persistYaml(response, false, null, null,
                    "certs/" + CertificateManager.UPLOADED_PKCS12_FILE, password == null ? "" : password);
        } else {
            Path certFile = certsDir.resolve(CertificateManager.UPLOADED_CERT_FILE);
            Path keyFile = certsDir.resolve(CertificateManager.UPLOADED_KEY_FILE);
            try {
                store(certPart, certFile);
                store(keyPart, keyFile);
                certificates.installUploadedPem(certFile, keyFile);
            } catch (ServerConfigLoader.ConfigException e) {
                Files.deleteIfExists(certFile);
                Files.deleteIfExists(keyFile);
                Json.error(response, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
                return;
            }
            persistYaml(response, true, "certs/" + CertificateManager.UPLOADED_CERT_FILE,
                    "certs/" + CertificateManager.UPLOADED_KEY_FILE, null, null);
        }
        if (response.isCommitted()) {
            return; // the yaml write failed and answered already
        }

        // The hot switch: reload when TLS is live, swap the connector when not.
        // A swap retires the very connection this answer rides on once it is
        // written, so the answer asks the client to let go of it.
        boolean swapping = !running.isTls();
        if (swapping) {
            response.setHeader("Connection", "close");
        }
        HdslServer.TlsSwitch result = swapping
                ? running.enableTls(certificates)
                : running.reloadTls(certificates);
        JsonObject body = new JsonObject();
        body.addProperty("ok", result.ok());
        body.addProperty("restartRequired", result.restartRequired());
        if (result.ok()) {
            config.https.enabled = true;
            body.addProperty("https", true);
            AuditLog.log(AuditLog.Channel.WEB, "tls.upload", isP12 ? "pkcs12" : "pem", "success",
                    actorOf(request), request.getRemoteAddr());
        } else if (result.error() != null) {
            body.addProperty("error", result.error());
        }
        Json.write(response, body);
    }

    /// `POST /api/settings/tls/self-signed` — generates (or reuses) the
    /// self-signed pair and switches the panel to HTTPS on the same port. The
    /// stopgap path: the developer page and the HTTPS tab both use it when the
    /// user insists on using a feature that requires TLS without owning a
    /// certificate. The answer carries `{ok, https, restartRequired}` exactly
    /// like an upload; the caller must treat `Connection: close` as "let go of
    /// this connection and come back over https".
    private void selfSigned(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HdslServer running = server.get();
        if (certificates == null || running == null) {
            Json.error(response, HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    "no certificate manager is attached to this server");
            return;
        }
        CertificateManager.Source current = certificates.getSource();
        boolean uploaded = current == CertificateManager.Source.PEM
                || current == CertificateManager.Source.PKCS12
                || config.https.hasExplicitCertificate();
        if (!running.isTls() && RequestSecurity.forwardedHttps(request, config)) {
            Json.error(response, HttpServletResponse.SC_CONFLICT,
                    "tls_terminated_upstream: the panel sits behind a TLS-terminating proxy; "
                            + "switching its own connector to HTTPS would break the proxy leg");
            return;
        }
        if (uploaded) {
            Json.error(response, HttpServletResponse.SC_CONFLICT,
                    "certificate_already_configured: a real certificate is already configured; "
                            + "remove https.cert_file / https.pkcs12_file to fall back to a self-signed pair");
            return;
        }
        CertificateManager.Info info;
        try {
            info = certificates.useSelfSigned();
        } catch (IOException | RuntimeException e) {
            Json.error(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "the self-signed certificate could not be generated: " + e.getMessage());
            return;
        }
        try {
            ServerConfigWriter.enableSelfSigned(config.dataDir);
        } catch (IOException | RuntimeException e) {
            Logger.LOG.warning("The self-signed certificate is ready but server.yaml could not be updated", e);
            Json.error(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "the certificate is ready but server.yaml could not be updated: " + e.getMessage());
            return;
        }

        boolean swapping = !running.isTls();
        if (swapping) {
            response.setHeader("Connection", "close");
        }
        HdslServer.TlsSwitch result = swapping
                ? running.enableTls(certificates)
                : running.reloadTls(certificates);
        JsonObject body = new JsonObject();
        body.addProperty("ok", result.ok());
        body.addProperty("restartRequired", result.restartRequired());
        body.addProperty("source", "self-signed");
        body.addProperty("fingerprintSha256", info.fingerprintSha256());
        body.addProperty("expires", info.expires().toString());
        if (result.ok()) {
            config.https.enabled = true;
            body.addProperty("https", true);
            AuditLog.log(AuditLog.Channel.WEB, "tls.self-signed", null, "success",
                    actorOf(request), request.getRemoteAddr());
        } else if (result.error() != null) {
            body.addProperty("error", result.error());
        }
        Json.write(response, body);
    }

    /// Records the upload in `server.yaml`; a failure answers 500 and leaves
    /// the caller to stop.
    private void persistYaml(HttpServletResponse response, boolean pem,
                             @Nullable String certFile, @Nullable String keyFile,
                             @Nullable String p12File, @Nullable String p12Password) throws IOException {
        try {
            ServerConfigWriter.enableTls(config.dataDir, pem, certFile, keyFile, p12File, p12Password);
        } catch (IOException | RuntimeException e) {
            Logger.LOG.warning("The upload is live but server.yaml could not be updated", e);
            Json.error(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "the certificate is live but server.yaml could not be updated: " + e.getMessage());
        }
    }

    /// The session's username for the audit trail, or null when the request
    /// carries no session.
    private static @Nullable String actorOf(HttpServletRequest request) {
        Object session = request.getAttribute(AuthFilter.SESSION_ATTRIBUTE);
        return session instanceof AuthService.Session s ? s.username() : null;
    }

    /// Writes a part to a file, owner-only: a private key and a PKCS#12 are
    /// credentials, and the certificate travels with them.
    private static void store(Part part, Path target) throws IOException {
        Path staging = target.resolveSibling(target.getFileName() + ".uploading");
        try (InputStream in = part.getInputStream(); OutputStream out = Files.newOutputStream(staging)) {
            in.transferTo(out);
        }
        try {
            Files.setPosixFilePermissions(staging, Set.copyOf(EnumSet.of(
                    PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE)));
        } catch (IOException | UnsupportedOperationException e) {
            // Best effort; non-POSIX filesystems cannot express this.
        }
        Files.move(staging, target, StandardCopyOption.REPLACE_EXISTING);
    }
}
