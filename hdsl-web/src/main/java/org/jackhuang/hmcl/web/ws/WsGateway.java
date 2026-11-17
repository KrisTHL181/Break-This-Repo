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
package org.jackhuang.hmcl.web.ws;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import jakarta.websocket.CloseReason;
import jakarta.websocket.EndpointConfig;
import jakarta.websocket.HandshakeResponse;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.HandshakeRequest;
import jakarta.websocket.server.ServerEndpoint;
import jakarta.websocket.server.ServerEndpointConfig;
import org.jackhuang.hmcl.web.acp.AcpSessionManager;
import org.jackhuang.hmcl.web.auth.AuthService;
import org.jackhuang.hmcl.web.event.EventBus;
import org.jackhuang.hmcl.web.http.Json;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/// The WebSocket gateway at `/ws` — the panel's live event stream.
///
/// The protocol is JSON text frames both ways. A client manages its
/// subscriptions with
///
/// ```json
/// {"type": "subscribe", "topics": ["instances", "tasks", "instance:<id>"]}
/// {"type": "unsubscribe", "topics": ["instance:<id>"]}
/// ```
///
/// A subscribe without topics (or none at all, as after connect) means
/// "everything"; one with topics replaces the current set. An unsubscribe
/// without topics drops every subscription; one with topics removes just
/// those — while subscribed to everything it adds them to an exclusion set,
/// so "all except the instance I just left" is expressible. Events are the
/// payloads published on the bus: `instance-state`, `log`, `task` and the
/// `instance-created`/`instance-deleted`/`instance-updated` CRUD notices.
///
/// Authentication happens twice: the [AuthFilter] on `/ws` is the gate that
/// answers 401 before an upgrade is ever negotiated, and the configurator
/// below re-checks the session cookie at handshake time as a second line that
/// cannot be bypassed by filter-mapping mistakes.
@NotNullByDefault
@ServerEndpoint(value = "/ws", configurator = WsGateway.SessionConfigurator.class)
public final class WsGateway {

    private static volatile EventBus bus = new EventBus();
    private static volatile AuthService authService;
    private static volatile boolean authDisabled = true;
    /// The ACP console bridge, or `null` before assembly. A console message
    /// arriving without it is answered with `acp-error`.
    private static volatile @Nullable AcpSessionManager acpSessions;

    /// Guards every mutation of the three subscription fields; the send
    /// callbacks themselves run on publisher threads and only read.
    private final Object lock = new Object();
    /// Explicit per-topic subscriptions; empty while subscribed to all.
    private final Map<String, EventBus.Subscription> byTopic = new HashMap<>();
    /// Topics muted while subscribed to all, by an unsubscribe naming them.
    private final Set<String> excluded = ConcurrentHashMap.newKeySet();
    private volatile @Nullable EventBus.Subscription allSubscription;

    /// Installs the gateway's collaborators. Called once at assembly, before
    /// any connection exists.
    ///
    /// @param eventBus     the bus to forward
    /// @param auth         the session verifier, or `null` with auth off
    /// @param disabledAuth whether authentication is switched off
    /// @param acp          the ACP console bridge, or `null` when there is none
    public static void install(EventBus eventBus, @Nullable AuthService auth, boolean disabledAuth,
                               @Nullable AcpSessionManager acp) {
        bus = eventBus;
        authService = auth;
        authDisabled = disabledAuth;
        acpSessions = acp;
    }

    @OnOpen
    public void onOpen(Session session, EndpointConfig config) {
        // A connection with no subscribe message gets everything, so the
        // launch view opened in a fresh tab misses nothing while its first
        // frame is in flight.
        synchronized (lock) {
            allSubscription = subscribeAll(session);
        }
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        JsonObject object;
        try {
            object = JsonParser.parseString(message).getAsJsonObject();
        } catch (JsonParseException | IllegalStateException e) {
            send(session, error("a subscription message must be a JSON object"));
            return;
        }
        String type = stringField(object, "type");
        Set<String> topics = new HashSet<>();
        boolean topicsGiven = object.has("topics") && object.get("topics").isJsonArray();
        if (topicsGiven) {
            JsonArray array = object.getAsJsonArray("topics");
            for (var element : array) {
                if (element.isJsonPrimitive()) {
                    topics.add(element.getAsString());
                }
            }
        }
        switch (type == null ? "" : type) {
            case "subscribe" -> subscribe(session, topicsGiven ? topics : null);
            case "unsubscribe" -> unsubscribe(topicsGiven ? topics : null);
            case "acp-start", "acp-prompt", "acp-cancel", "acp-stop" ->
                    handleAcp(session, object, type);
            default -> send(session, error("unknown message type"));
        }
    }

    /// Routes an `acp-*` message to the console bridge.
    ///
    /// Every message names its instance; the bridge answers on the
    /// `instance:<id>` topic, which is where the instance page's subscriptions
    /// already are. A malformed message, or one arriving before the bridge was
    /// installed, is answered with `acp-error` directly.
    private static void handleAcp(Session session, JsonObject object, String type) {
        String instance = stringField(object, "instance");
        if (instance == null || instance.isBlank()) {
            send(session, acpError(null, "an ACP message must name an instance"));
            return;
        }
        AcpSessionManager manager = acpSessions;
        if (manager == null) {
            send(session, acpError(instance, "the ACP console is not available"));
            return;
        }
        switch (type) {
            case "acp-start" -> manager.start(instance, stringField(object, "cwd"));
            case "acp-prompt" -> manager.prompt(instance, stringField(object, "session"),
                    stringField(object, "text"));
            case "acp-cancel" -> manager.cancel(instance, stringField(object, "session"));
            case "acp-stop" -> manager.stop(instance);
            default -> send(session, error("unknown message type"));
        }
    }

    @OnClose
    public void onClose(Session session, CloseReason reason) {
        detach();
    }

    @OnError
    public void onError(Session session, Throwable error) {
        detach();
    }

    /// Replaces the connection's subscriptions: `null` or an empty set means
    /// "everything" (and clears the exclusions), anything else is the exact
    /// topic set from now on.
    private void subscribe(Session session, @Nullable Set<String> topics) {
        synchronized (lock) {
            detachLocked();
            if (topics == null || topics.isEmpty()) {
                allSubscription = subscribeAll(session);
                return;
            }
            for (String topic : topics) {
                byTopic.put(topic, bus.subscribe(topic, (ignored, payload) -> send(session, payload)));
            }
        }
    }

    /// The "everything" subscription, with the exclusion filter applied.
    private EventBus.Subscription subscribeAll(Session session) {
        return bus.subscribe(EventBus.ALL_TOPIC, (topic, payload) -> {
            if (!excluded.contains(topic)) {
                send(session, payload);
            }
        });
    }

    /// Removes subscriptions: `null` or an empty set drops everything; named
    /// topics leave the explicit set, or — while subscribed to everything —
    /// join the exclusion set.
    private void unsubscribe(@Nullable Set<String> topics) {
        synchronized (lock) {
            if (topics == null || topics.isEmpty()) {
                detachLocked();
                return;
            }
            if (allSubscription != null) {
                excluded.addAll(topics);
                return;
            }
            for (String topic : topics) {
                EventBus.Subscription subscription = byTopic.remove(topic);
                if (subscription != null) {
                    subscription.close();
                }
            }
        }
    }

    private void detach() {
        synchronized (lock) {
            detachLocked();
        }
    }

    private void detachLocked() {
        if (allSubscription != null) {
            allSubscription.close();
            allSubscription = null;
        }
        for (EventBus.Subscription subscription : byTopic.values()) {
            subscription.close();
        }
        byTopic.clear();
        excluded.clear();
    }

    private static void send(Session session, JsonObject payload) {
        if (!session.isOpen()) {
            return;
        }
        try {
            session.getBasicRemote().sendText(Json.GSON.toJson(payload));
        } catch (IOException | RuntimeException e) {
            // A browser that left mid-frame; the close handler cleans up.
            try {
                session.close();
            } catch (IOException ignored) {
                // Already gone.
            }
        }
    }

    private static JsonObject error(String message) {
        JsonObject payload = new JsonObject();
        payload.addProperty("type", "error");
        payload.addProperty("message", message);
        return payload;
    }

    /// An `acp-error` frame sent straight to one connection — the answer when
    /// the message itself cannot be routed, so there is no instance topic to
    /// publish on.
    private static JsonObject acpError(@Nullable String instance, String message) {
        JsonObject payload = new JsonObject();
        payload.addProperty("type", "acp-error");
        if (instance == null) {
            payload.add("instance", com.google.gson.JsonNull.INSTANCE);
        } else {
            payload.addProperty("instance", instance);
        }
        payload.addProperty("error", message);
        return payload;
    }

    private static @org.jetbrains.annotations.Nullable String stringField(JsonObject object, String name) {
        return object.has(name) && object.get(name).isJsonPrimitive() ? object.get(name).getAsString() : null;
    }

    /// The handshake-time session check. The primary gate is the AuthFilter in
    /// front of `/ws`; this exists so the endpoint itself refuses a handshake
    /// that somehow reached it without a session.
    public static final class SessionConfigurator extends ServerEndpointConfig.Configurator {
        @Override
        public void modifyHandshake(ServerEndpointConfig config, HandshakeRequest request, HandshakeResponse response) {
            if (authDisabled) {
                return;
            }
            AuthService auth = authService;
            if (auth == null) {
                throw new IllegalStateException("no AuthService installed");
            }
            String token = sessionCookie(request);
            Optional<AuthService.Session> session = auth.verify(token);
            if (session.isEmpty()) {
                throw new SecurityException("unauthorized");
            }
            config.getUserProperties().put("username", session.get().username());
        }

        /// Reads the `hdsl_session` cookie out of the handshake's Cookie header.
        private static @org.jetbrains.annotations.Nullable String sessionCookie(HandshakeRequest request) {
            for (Map.Entry<String, List<String>> header : request.getHeaders().entrySet()) {
                if (!"cookie".equalsIgnoreCase(header.getKey())) {
                    continue;
                }
                for (String value : header.getValue()) {
                    for (String pair : value.split(";")) {
                        String trimmed = pair.trim();
                        if (trimmed.startsWith(org.jackhuang.hmcl.web.auth.AuthFilter.SESSION_COOKIE + "=")) {
                            return trimmed.substring(org.jackhuang.hmcl.web.auth.AuthFilter.SESSION_COOKIE.length() + 1);
                        }
                    }
                }
            }
            return null;
        }
    }
}
