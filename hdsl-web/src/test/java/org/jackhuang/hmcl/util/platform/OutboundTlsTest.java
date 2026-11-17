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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import javax.net.ssl.SSLHandshakeException;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The outbound-TLS policy: strict unless an operator said otherwise, and then
/// only for a bounded window.
///
/// The switch itself changes this JVM's default `SSLContext`; what is pinned here is
/// the state machine and the classification, which is what decides whether a person
/// is told their proxy is the problem.
class OutboundTlsTest {

    @AfterEach
    void strictAgain() {
        OutboundTls.accept(null);
    }

    @Test
    void strictByDefault() {
        assertFalse(OutboundTls.acceptingUnverified());
        assertNull(OutboundTls.acceptedUntil());
    }

    @Test
    void anAcknowledgementOpensAWindowThatExpires() {
        assertTrue(OutboundTls.accept(Instant.now().plus(Duration.ofMinutes(30))));
        assertTrue(OutboundTls.acceptingUnverified());
        assertNotNull(OutboundTls.acceptedUntil());

        OutboundTls.accept(Instant.now().minus(Duration.ofMinutes(1)));
        assertFalse(OutboundTls.acceptingUnverified(),
                "an instant in the past is an expired acknowledgement, not an open window");
    }

    @Test
    void theWindowIsCappedAtADay() {
        Instant before = Instant.now();
        OutboundTls.accept(before.plus(Duration.ofDays(30)));
        Instant until = OutboundTls.acceptedUntil();
        assertNotNull(until);
        assertFalse(until.isAfter(before.plus(OutboundTls.MAX_WINDOW).plusSeconds(5)), until.toString());
    }

    @Test
    void revokingClosesItImmediately() {
        OutboundTls.accept(Instant.now().plus(Duration.ofMinutes(10)));
        OutboundTls.accept(null);
        assertFalse(OutboundTls.acceptingUnverified());
        assertNull(OutboundTls.lastTrouble() == null ? null : OutboundTls.acceptedUntil());
    }

    @Test
    void aCertificateFailureIsRecognisedThroughTheCauseChain() {
        SSLHandshakeException pkix = new SSLHandshakeException(
                "PKIX path building failed: unable to find valid certification path to requested target");
        assertTrue(OutboundTls.isTrustFailure(new IOException("send failed", pkix)));
        assertFalse(OutboundTls.isTrustFailure(new IOException("connection reset")));
        assertFalse(OutboundTls.isTrustFailure(null));
    }

    @Test
    void aRecordedFailureIsWhatThePageExplains() {
        SSLHandshakeException pkix = new SSLHandshakeException("PKIX path building failed");
        OutboundTls.recordTrouble("api.github.com", pkix);

        OutboundTls.Trouble trouble = OutboundTls.lastTrouble();
        assertNotNull(trouble);
        assertEquals("api.github.com", trouble.host());

        String explanation = OutboundTls.explain(trouble);
        assertTrue(explanation.contains("api.github.com"), explanation);
        assertTrue(explanation.contains("TLS"), explanation);
        assertTrue(explanation.contains("设置 → 下载源 → 出站证书"), explanation);
        assertTrue(explanation.contains("24 小时"), explanation);
    }

    @Test
    void aNetworkErrorIsNotBlamedOnTheProxy() {
        OutboundTls.recordTrouble("api.github.com", new SSLHandshakeException("PKIX path building failed"));
        OutboundTls.Trouble before = OutboundTls.lastTrouble();
        OutboundTls.recordTrouble("api.github.com", new IOException("connection timed out"));
        assertEquals(before, OutboundTls.lastTrouble(),
                "an unreachable host must not overwrite the certificate explanation");
    }
}
