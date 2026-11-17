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
package org.jackhuang.hmcl.util.platform;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.jackhuang.hmcl.util.logging.Logger.LOG;

/// Whether outbound HTTPS has to prove who it is talking to, and what to say when
/// it cannot.
///
/// Two facts make this a policy rather than a setting:
///
/// - A TLS-inspecting proxy — an antivirus, a corporate gateway, a GitHub
///   accelerator such as FastGithub — puts **its own** root in the Windows store,
///   while Java only reads its bundled `cacerts`. The browser opens the page, the
///   panel reports `PKIX path building failed`, and the visible symptom is a brand
///   version list that is simply empty. Nothing said why.
/// - Widening trust is a decision with a cost, so the panel does not take it on
///   its own. It **asks**: the failure is recorded and shown with the reason, and
///   only an explicit acknowledgement opens a window (time-limited, revocable)
///   during which an unverifiable certificate is accepted.
///
/// Even inside that window verification is not simply thrown away: the chain is
/// first checked against the machine's own roots (the proxy's root usually lives
/// there), and only a chain that fails *that* is accepted — logged with the
/// certificate it belonged to, so the audit trail names what was trusted.
///
/// The window is applied through `SSLContext.setDefault`, which every client this
/// panel builds afterwards picks up. Clients cached across the change would keep
/// the old context, so the ones on the GitHub path build per call.
@NotNullByDefault
public final class OutboundTls {

    /// The longest window an acknowledgement may open. A day is enough to install
    /// what needed installing, and short enough that nobody forgets it is open.
    public static final Duration MAX_WINDOW = Duration.ofHours(24);

    /// What "the certificate could not be verified" looked like, for the panel to
    /// show and for a restart to explain.
    ///
    /// @param host   the host the panel was talking to
    /// @param reason the platform's own words, kept verbatim: they are what a
    ///               person forwards to whoever runs the network
    /// @param at     when it happened
    public record Trouble(String host, String reason, Instant at) {
    }

    private static volatile @Nullable Instant acceptedUntil;
    private static volatile @Nullable Trouble lastTrouble;
    private static volatile long generation;

    private OutboundTls() {
    }

    /// Whether an unverifiable certificate is accepted right now.
    public static boolean acceptingUnverified() {
        Instant until = acceptedUntil;
        return until != null && Instant.now().isBefore(until);
    }

    /// The acknowledgement's expiry, or `null` when trust is strict.
    public static @Nullable Instant acceptedUntil() {
        return acceptingUnverified() ? acceptedUntil : null;
    }

    /// The last certificate failure the panel hit, if any.
    public static @Nullable Trouble lastTrouble() {
        return lastTrouble;
    }

    /// Records one failure, for the message that explains it.
    ///
    /// @param host  the host that could not be verified
    /// @param error the exception it failed with
    public static void recordTrouble(String host, Throwable error) {
        if (!isTrustFailure(error)) {
            return;
        }
        lastTrouble = new Trouble(host, reasonOf(error), Instant.now());
    }

    /// Opens (or closes) the acknowledgement window and applies it.
    ///
    /// @param until when unverifiable certificates stop being accepted, or `null`
    ///              for strict verification
    /// @return whether the window is open
    public static boolean accept(@Nullable Instant until) {
        Instant capped = until == null || until.isAfter(Instant.now().plus(MAX_WINDOW))
                ? Instant.now().plus(MAX_WINDOW)
                : until;
        boolean open = until != null && capped.isAfter(Instant.now());
        if (!open && acceptedUntil == null) {
            // Strict is the default state: nothing to install, nothing to log. This
            // is the ordinary startup path.
            return false;
        }
        acceptedUntil = open ? capped : null;
        install(open);
        generation++;
        if (open) {
            LOG.warning("Outbound TLS: accepting unverifiable certificates until " + capped
                    + " (acknowledged by an operator); this window is not a default");
        } else {
            LOG.info("Outbound TLS: certificate verification is strict again");
        }
        return open;
    }

    /// Whether an exception is a certificate failure rather than a network one.
    ///
    /// Kept narrow on purpose: an unreachable host is a different message, and
    /// telling somebody their proxy is the problem when the cable is out would be
    /// worse than saying nothing.
    ///
    /// @param error the failure
    /// @return whether it is one of the trust failures this class can explain
    public static boolean isTrustFailure(@Nullable Throwable error) {
        for (Throwable cause : causesOf(error)) {
            if (cause instanceof SSLHandshakeException) {
                String message = String.valueOf(cause.getMessage());
                if (message.contains("PKIX")
                        || message.contains("certification path")
                        || message.contains("unable to find valid")
                        || message.contains("No subject alternative")
                        || message.contains("certificate")) {
                    return true;
                }
            }
        }
        return false;
    }

    /// The exception and its causes, bounded: a chain that loops (or is absurdly
    /// deep) must not turn an error message into a hang.
    private static List<Throwable> causesOf(@Nullable Throwable error) {
        List<Throwable> causes = new ArrayList<>();
        for (Throwable cause = error; cause != null && causes.size() < 12; cause = cause.getCause()) {
            causes.add(cause);
            if (causes.contains(cause.getCause())) {
                break;
            }
        }
        return causes;
    }

    /// The sentence a person can act on: what was not verified, whose words say
    /// so, and the two ways out.
    ///
    /// @param trouble the recorded failure
    /// @return the explanation, in the panel's own language
    public static String explain(Trouble trouble) {
        return "TLS 证书没通过校验（" + trouble.host() + "：" + trouble.reason() + "）。\n\n"
                + "常见原因：这台机器上有反向代理、抓包工具或 GitHub 加速器（例如 FastGithub）在转发 HTTPS，"
                + "它用自己的根证书签了 " + trouble.host() + "；浏览器的信任库里有那张根证书，"
                + "Java 只认自带的 cacerts，所以浏览器打得开、面板打不开。\n\n"
                + "两条路：① 如果你知道并信任这个代理，点下面那个『我确认』按钮"
                + "（或「设置 → 下载源 → 出站证书」；一次性授权，最长 24 小时后自动收紧）；"
                + "② 否则请检查网络或代理配置——面板不会替你跳过证书校验。";
    }

    private static String reasonOf(Throwable error) {
        for (Throwable cause : causesOf(error)) {
            if (cause instanceof SSLHandshakeException) {
                return String.valueOf(cause.getMessage());
            }
        }
        return String.valueOf(error.getMessage());
    }

    /// The number that changes when the policy does. A client that caches itself
    /// compares it, so a consent given after startup is picked up without one.
    public static long generation() {
        return generation;
    }

    private static void install(boolean permissive) {
        try {
            SSLContext context;
            if (permissive) {
                context = SSLContext.getInstance("TLS");
                context.init(null, new TrustManager[]{new Acknowledged()}, null);
            } else {
                context = SSLContext.getInstance("TLS");
                context.init(null, null, null);
            }
            SSLContext.setDefault(context);
        } catch (GeneralSecurityException e) {
            // A JVM without the algorithms this needs keeps whatever it had; the
            // panel's next request still says what it could not verify.
            LOG.warning("Could not switch outbound TLS to "
                    + (permissive ? "the acknowledged mode" : "strict verification"), e);
        }
    }

    /// Verification that is only widened for a chain the machine itself does not
    /// vouch for — and that says so when it happens.
    ///
    /// The order matters: the bundled roots first, then the machine's own (a
    /// proxy's root is installed there), then — and only then, and only inside an
    /// acknowledged window — the certificate is accepted as-is.
    private static final class Acknowledged implements X509TrustManager {

        private final @Nullable X509TrustManager bundled = manager(null);
        /// `Windows-ROOT`: the machine's own roots, which is where a TLS-inspecting
        /// proxy installs its certificate.
        private final @Nullable X509TrustManager machine = manager("Windows-ROOT");

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            throw new CertificateException("this panel is a TLS client, not a server");
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            for (X509TrustManager manager : List.of(bundled, machine)) {
                if (manager == null) {
                    continue;
                }
                try {
                    manager.checkServerTrusted(chain, authType);
                    return;
                } catch (CertificateException ignored) {
                    // try the next one
                }
            }
            X509Certificate peer = chain.length == 0 ? null : chain[0];
            LOG.warning("Outbound TLS: accepting a certificate this machine cannot verify"
                    + (peer == null ? "" : " (subject " + peer.getSubjectX500Principal()
                            + ", issuer " + peer.getIssuerX500Principal() + ")")
                    + "; an operator acknowledged this within a time-limited window");
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            List<X509Certificate> issuers = new ArrayList<>();
            for (X509TrustManager manager : List.of(bundled, machine)) {
                if (manager != null) {
                    issuers.addAll(List.of(manager.getAcceptedIssuers()));
                }
            }
            return issuers.toArray(new X509Certificate[0]);
        }

        private static @Nullable X509TrustManager manager(@Nullable String keyStoreType) {
            try {
                TrustManagerFactory factory = TrustManagerFactory.getInstance(
                        TrustManagerFactory.getDefaultAlgorithm());
                if (keyStoreType == null) {
                    factory.init((KeyStore) null);
                } else {
                    KeyStore store = KeyStore.getInstance(keyStoreType);
                    store.load(null, null);
                    factory.init(store);
                }
                for (TrustManager candidate : factory.getTrustManagers()) {
                    if (candidate instanceof X509TrustManager manager) {
                        return manager;
                    }
                }
                return null;
            } catch (Exception | LinkageError e) {
                // A JDK without SunMSCAPI cannot read the machine's store; the
                // bundled roots still work, and the window still covers the rest.
                return null;
            }
        }
    }
}
