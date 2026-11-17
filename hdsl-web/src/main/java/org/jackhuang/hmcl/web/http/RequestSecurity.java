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

import jakarta.servlet.http.HttpServletRequest;
import org.jackhuang.hmcl.web.config.ServerConfig;
import org.jetbrains.annotations.NotNullByDefault;

/// One question, asked by every HTTPS-only feature: did this request reach the
/// **browser** over TLS?
///
/// Two shapes count:
/// 1. the panel's own connector speaks TLS ([HttpServletRequest#isSecure], or
///    the live connector state passed in);
/// 2. a reverse proxy terminated TLS and forwarded the request over plain
///    HTTP, saying so in `X-Forwarded-Proto: https`.
///
/// The second is trusted only when `https.trust_forwarded_proto` is on: any
/// client can write that header, so an operator has to assert that the panel is
/// reachable solely through the proxy. Without the flag the header is ignored,
/// which is what keeps a directly exposed HTTP panel from being talked into
/// "secure" by a forged header.
@NotNullByDefault
public final class RequestSecurity {

    private RequestSecurity() {
    }

    /// @param request      the request being judged
    /// @param connectorTls whether the panel's own main connector speaks TLS
    /// @param config       for the forwarded-proto opt-in
    /// @return whether the browser leg of this request is HTTPS
    public static boolean isSecure(HttpServletRequest request, boolean connectorTls, ServerConfig config) {
        return connectorTls || request.isSecure() || forwardedHttps(request, config);
    }

    /// Whether a **trusted** reverse proxy reported an HTTPS browser leg.
    /// `X-Forwarded-Proto` is a comma-separated list when several proxies are
    /// chained; the first entry is the protocol the browser used.
    public static boolean forwardedHttps(HttpServletRequest request, ServerConfig config) {
        if (!config.https.trustForwardedProto) {
            return false;
        }
        String proto = request.getHeader("X-Forwarded-Proto");
        if (proto == null) {
            return false;
        }
        int comma = proto.indexOf(',');
        String first = (comma >= 0 ? proto.substring(0, comma) : proto).trim();
        return first.equalsIgnoreCase("https");
    }
}
