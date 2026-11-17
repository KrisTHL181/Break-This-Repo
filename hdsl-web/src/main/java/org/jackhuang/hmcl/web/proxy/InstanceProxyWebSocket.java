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
package org.jackhuang.hmcl.web.proxy;

import org.eclipse.jetty.http.HttpField;
import org.eclipse.jetty.websocket.api.Callback;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.StatusCode;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.jackhuang.hmcl.dsh.DshProcess;
import org.jackhuang.hmcl.dsh.DshProcessManager;
import org.jetbrains.annotations.NotNullByDefault;

import org.jackhuang.hmcl.web.brand.Brand;
import org.jackhuang.hmcl.web.brand.BrandRuntime;

import java.net.URI;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.jackhuang.hmcl.util.logging.Logger.LOG;

/// One relayed WebSocket: the browser's upgrade under `/i/<id>/...` on one
/// side, a fresh client connection to the dsh instance's loopback port on the
/// other, and every frame passed between the two.
///
/// The handshake is replayed, not renegotiated: every header that is not
/// hop-by-hop — Host, Origin, Cookie and the `Sec-WebSocket-*` names among
/// them — is set on the upstream request verbatim. dsh speaks plain `ws` with
/// JSON text frames and a 2s ping heartbeat, no subprotocol negotiation, so
/// nothing else is needed.
///
/// Ping handling: each side pongs its own peer locally (a browser or dsh that
/// stops hearing pongs closes the connection) *and* forwards the ping, so the
/// heartbeat is observable end to end.
@NotNullByDefault
public final class InstanceProxyWebSocket implements Session.Listener.AutoDemanding {

    /// The largest single frame and message the relay moves in either
    /// direction.
    ///
    /// Jetty's default is 64 KiB for both — a limit written for chat-sized
    /// traffic, and far below what dsh sends: one session history, one tool
    /// result, one pasted file arrives as a single WebSocket message, and the
    /// relay would fail the connection with 1009 the moment one exceeded it.
    /// dsh's own server (node's `ws`) allows 100 MiB per message, so the cap
    /// here is a bound of the same order rather than a lower one: 32 MiB, which
    /// covers the payloads a harness produces while still refusing a peer that
    /// tries to make the panel allocate without limit.
    public static final long MAX_MESSAGE_BYTES = 32L * 1024 * 1024;

    private final WebSocketClient client;
    private final String instanceId;
    private final String targetPath;
    private final List<HttpField> handshakeHeaders;
    /// The panel's own mount, so the prefix handed to a prefix-aware server is
    /// the one the browser is actually on (`/panel/i/<id>` when deployed under
    /// a base path).
    private final String basePath;

    private volatile Session browser;
    private volatile Session upstream;
    private final AtomicBoolean closed = new AtomicBoolean();
    /// Text frames that arrived before the upstream connection was up. The
    /// browser's `onopen` fires the moment our upgrade completes, which can be
    /// a few milliseconds ahead of the upstream handshake — dropping those
    /// first frames would lose dsh's opening JSON-RPC.
    private final java.util.ArrayDeque<String> pendingText = new java.util.ArrayDeque<>();

    public InstanceProxyWebSocket(WebSocketClient client, String instanceId, String targetPath,
                                  List<HttpField> handshakeHeaders, String basePath) {
        this.client = client;
        this.instanceId = instanceId;
        this.targetPath = targetPath;
        this.handshakeHeaders = handshakeHeaders;
        this.basePath = basePath == null ? "" : basePath;
    }

    // ----------------------------------------------------------- browser side --

    @Override
    public void onWebSocketOpen(Session session) {
        this.browser = session;
        // The same resolution the HTTP proxy uses, so a relay can never point
        // somewhere the page it belongs to does not.
        int port = InstanceProxyTargets.resolveForRelay(instanceId);
        if (port <= 0) {
            close(StatusCode.SERVER_ERROR, "instance not running");
            return;
        }
        URI target = URI.create("ws://127.0.0.1:" + port + targetPath);

        ClientUpgradeRequest upgradeRequest = new ClientUpgradeRequest();
        // A prefix-aware server puts its own boundary in front of the WebSocket
        // as well: it checks Host and, when present, Origin against the loopback
        // host it serves. The browser's Origin is the panel's, so replaying it
        // would be an instant 403 — both headers are dropped here and the client
        // derives Host from the loopback URI.
        Brand brand = BrandRuntime.brandForMount(instanceId);
        boolean prefixLocked = brand != null && brand.serverSidePrefix();
        for (HttpField field : handshakeHeaders) {
            String name = field.getName();
            String lower = name.toLowerCase(Locale.ROOT);
            if (InstanceProxyServlet.HOP_BY_HOP.contains(lower)) {
                continue;
            }
            if (prefixLocked && (lower.equals("host") || lower.equals("origin"))) {
                continue;
            }
            upgradeRequest.setHeader(name, field.getValue());
        }
        if (prefixLocked) {
            upgradeRequest.setHeader("X-Forwarded-Prefix", basePath + "/i/" + instanceId);
        }
        try {
            client.connect(new UpstreamListener(this), target, upgradeRequest).whenComplete((upstreamSession, error) -> {
if (error != null || upstreamSession == null) {
                    LOG.warning("Could not connect the relay of instance " + instanceId
                            + " to " + target + ": " + error);
                    close(StatusCode.SERVER_ERROR, "upstream connect failed");
                    return;
                }
                synchronized (pendingText) {
                    this.upstream = upstreamSession;
                    while (!pendingText.isEmpty()) {
                        upstreamSession.sendText(pendingText.poll(), Callback.NOOP);
                    }
                }
            });
        } catch (Exception e) {
            LOG.warning("Could not start the relay of instance " + instanceId + " to " + target, e);
            close(StatusCode.SERVER_ERROR, "upstream connect failed");
        }
    }

    @Override
    public void onWebSocketText(String message) {
        Session target = upstream;
        if (target != null) {
            target.sendText(message, Callback.NOOP);
            return;
        }
        synchronized (pendingText) {
            target = upstream;
            if (target != null) {
                target.sendText(message, Callback.NOOP);
            } else if (pendingText.size() < 64) {
                pendingText.add(message);
            }
        }
    }

    @Override
    public void onWebSocketBinary(ByteBuffer payload, Callback callback) {
        Session target = upstream;
        if (target != null) {
            // The payload is recycled when the callback completes; the copy
            // outlives it on the way to the other connection.
            target.sendBinary(copy(payload), Callback.NOOP);
        }
        callback.succeed();
    }

    @Override
    public void onWebSocketPing(ByteBuffer payload) {
        Session local = browser;
        if (local != null) {
            local.sendPong(copy(payload), Callback.NOOP);
        }
        Session target = upstream;
        if (target != null) {
            target.sendPing(copy(payload), Callback.NOOP);
        }
    }

    @Override
    public void onWebSocketPong(ByteBuffer payload) {
        Session target = upstream;
        if (target != null) {
            target.sendPong(copy(payload), Callback.NOOP);
        }
    }

    @Override
    public void onWebSocketError(Throwable cause) {
        close(StatusCode.SERVER_ERROR, "browser connection failed");
    }

    @Override
    public void onWebSocketClose(int statusCode, String reason) {
        close(statusCode, reason);
    }

    // ---------------------------------------------------------- upstream side --

    /// The client-side endpoint on the connection to dsh. Public because
    /// Jetty's client binds endpoint methods through method handles, which
    /// cannot see into a private inner class.
    public static final class UpstreamListener implements Session.Listener.AutoDemanding {
        private final InstanceProxyWebSocket relay;

        public UpstreamListener(InstanceProxyWebSocket relay) {
            this.relay = relay;
        }

        @Override
        public void onWebSocketText(String message) {
            Session local = relay.browser;
            if (local != null) {
                local.sendText(message, Callback.NOOP);
            }
        }

        @Override
        public void onWebSocketBinary(ByteBuffer payload, Callback callback) {
            Session local = relay.browser;
            if (local != null) {
                local.sendBinary(copy(payload), Callback.NOOP);
            }
            callback.succeed();
        }

        @Override
        public void onWebSocketPing(ByteBuffer payload) {
            // dsh's 2s heartbeat: pong it locally or it closes the connection.
            Session local = relay.upstream;
            if (local != null) {
                local.sendPong(copy(payload), Callback.NOOP);
            }
            Session target = relay.browser;
            if (target != null) {
                target.sendPing(copy(payload), Callback.NOOP);
            }
        }

        @Override
        public void onWebSocketPong(ByteBuffer payload) {
            Session target = relay.browser;
            if (target != null) {
                target.sendPong(copy(payload), Callback.NOOP);
            }
        }

        @Override
        public void onWebSocketError(Throwable cause) {
            relay.close(StatusCode.SERVER_ERROR, "upstream connection failed");
        }

        @Override
        public void onWebSocketClose(int statusCode, String reason) {
            relay.close(statusCode, reason);
        }
    }

    // -------------------------------------------------------------- teardown --

    /// Closes both ends once: a close frame with the code and reason to the
    /// other side, then the local session itself.
    ///
    /// An ending that is not a normal one is logged: without it a relay that
    /// fails on a payload (the 1009 a message over the limit produces) looks
    /// from the panel side like a client that simply went away, and the reason
    /// only exists in the browser's console.
    private void close(int statusCode, String reason) {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        if (statusCode != StatusCode.NORMAL && statusCode != StatusCode.SHUTDOWN
                && statusCode != StatusCode.NO_CODE) {
            LOG.info("Relay for instance " + instanceId + " closed: "
                    + statusCode + (reason == null || reason.isBlank() ? "" : " " + reason));
        }
        Session browserSession = browser;
        Session upstreamSession = upstream;
        if (browserSession != null) {
            browserSession.close(statusCode, reason, Callback.NOOP);
        }
        if (upstreamSession != null) {
            upstreamSession.close(statusCode, reason, Callback.NOOP);
        }
    }

    private static ByteBuffer copy(ByteBuffer source) {
        ByteBuffer copy = ByteBuffer.allocate(source.remaining());
        copy.put(source.duplicate());
        copy.flip();
        return copy;
    }
}
