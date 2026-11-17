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

import org.jetbrains.annotations.NotNullByDefault;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;

/// The `Host` header the instance proxy forwards.
///
/// `java.net.http` refuses `Host` (and `Connection`/`Content-Length`/`Expect`/
/// `Upgrade`) unless `jdk.httpclient.allowRestrictedHeaders` names it, and the
/// JDK **freezes that allow-list when the first `HttpClient` is constructed** —
/// setting the property afterwards has no effect, for the rest of the process.
///
/// That turns a harmless-looking static `HttpClient` into a trap: the panel once
/// ran a brand-installer class initialiser (its own `HttpClient` field) before
/// the proxy servlet's static block, and every `/i/*` request answered
/// `500 restricted header name: "Host"` until the JVM was restarted with the
/// property on the command line.
///
/// So [#allow] is called on the **first line of `main`**, and [#verify] runs
/// once the server is assembled: a deployment that loses the ordering (a new
/// static client, a different entry point) fails loudly at boot instead of
/// serving broken mounts.
@NotNullByDefault
public final class HostHeaderSupport {

    /// The property the JDK reads once.
    public static final String PROPERTY = "jdk.httpclient.allowRestrictedHeaders";

    private HostHeaderSupport() {
    }

    /// Names `Host` as an allowed restricted header. Idempotent and cheap; call
    /// it as early as possible, before anything constructs an `HttpClient`.
    public static void allow() {
        System.setProperty(PROPERTY, "host");
    }

    /// Whether the current process really may set `Host` on a request.
    ///
    /// Probes the JDK directly instead of reading the property back: a property
    /// that was set too late still reads correctly, which is exactly the failure
    /// this method exists to catch.
    public static boolean isHostHeaderUsable() {
        try {
            HttpRequest.newBuilder(URI.create("http://127.0.0.1/"))
                    .header("Host", "127.0.0.1")
                    .build();
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /// Fails fast when the proxy could not forward `Host`.
    ///
    /// @throws IllegalStateException with the command-line remedy, because this
    ///         is a wiring bug the operator can work around while it is fixed
    public static void verify() {
        if (isHostHeaderUsable()) {
            return;
        }
        throw new IllegalStateException("The instance proxy cannot forward the Host header: an "
                + "HttpClient was created before " + PROPERTY + " was set, and the JDK freezes that "
                + "allow-list on first use. Every /i/* mount would answer 500. Start the panel with "
                + "-D" + PROPERTY + "=host, or report this ordering bug.");
    }

    /// Kept for callers that want the class literal in a stack trace.
    static Class<?> owner() {
        return HttpClient.class;
    }
}
