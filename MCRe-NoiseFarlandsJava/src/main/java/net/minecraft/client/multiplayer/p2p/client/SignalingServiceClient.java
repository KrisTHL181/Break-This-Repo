package net.minecraft.client.multiplayer.p2p.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.onvoid.webrtc.RTCIceServer;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import net.minecraft.client.User;
import net.minecraft.client.multiplayer.p2p.SignalingErrorMapper;
import net.minecraft.client.multiplayer.p2p.SignalingException;
import net.minecraft.client.multiplayer.p2p.SignalingMessage;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.jsonrpc.JsonRPCErrors;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public final class SignalingServiceClient {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final String WS_CONNECTION_ENDPOINT = "/ws/v1.0/messaging/connect/java";
   private static final Codec<String> SIGNALING_URI_CODEC = Codec.STRING.fieldOf("signalingUri").codec().fieldOf("result").codec();
   private static final Duration PING_INTERVAL = Duration.ofSeconds(50L);
   private static final String HEADER_AUTH = "x-mojangauth";
   private static final String HEADER_SESSION_ID = "Session-Id";
   private static final String HEADER_REQUEST_ID = "Request-Id";
   private static final SignalingServiceClient.Environment ENVIRONMENT = Optional.ofNullable(System.getenv("signaling.environment"))
      .or(() -> Optional.ofNullable(System.getProperty("signaling.environment")))
      .flatMap(SignalingServiceClient.Environment::byName)
      .orElse(SignalingServiceClient.Environment.PRODUCTION);
   private final User user;
   private final String sessionId = UUID.randomUUID().toString();
   private final List<SignalingServiceClient.ConnectionListener> connectionListeners = new CopyOnWriteArrayList<>();
   private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(r -> {
      Thread t = new Thread(r, "P2P-Signaling");
      t.setDaemon(true);
      t.setUncaughtExceptionHandler((th, e) -> LOGGER.error("Uncaught in {}", th, e));
      return t;
   });
   private @Nullable HttpClient httpClient;
   private @Nullable CompletableFuture<JsonRpcClient> websocketConnect;
   private @Nullable ScheduledFuture<?> pingTask;
   private SignalingServiceClient.@Nullable FriendJoinHandler friendJoinHandler;
   private SignalingServiceClient.@Nullable WebRtcSignalingHandler webRtcSignalingHandler;
   private SignalingServiceClient.@Nullable CachedTurn cachedTurn;
   private SignalingServiceClient.@Nullable CachedSignalingUri cachedSignalingUri;
   private @Nullable CompletableFuture<RTCIceServer> pendingTurnRefresh;

   public SignalingServiceClient(final User user) {
      this.user = user;
   }

   public void setFriendJoinHandler(final SignalingServiceClient.@Nullable FriendJoinHandler handler) {
      this.executor.execute(() -> this.friendJoinHandler = handler);
   }

   public void setWebRtcSignalingHandler(final SignalingServiceClient.@Nullable WebRtcSignalingHandler handler) {
      this.executor.execute(() -> this.webRtcSignalingHandler = handler);
   }

   public void addConnectionListener(final SignalingServiceClient.ConnectionListener listener) {
      this.connectionListeners.add(listener);
   }

   public void removeConnectionListener(final SignalingServiceClient.ConnectionListener listener) {
      this.connectionListeners.remove(listener);
   }

   public void clearHandlers() {
      this.executor.execute(() -> {
         this.friendJoinHandler = null;
         this.webRtcSignalingHandler = null;
      });
   }

   public void connect() {
      this.executor.execute(this::connectWebSocket);
   }

   public void disconnect() {
      this.executor.execute(() -> this.teardown("explicit disconnect"));
   }

   public CompletableFuture<RTCIceServer> requestTurnAuth() {
      return CompletableFuture.completedFuture(null).thenComposeAsync(var1 -> {
         SignalingServiceClient.CachedTurn cached = this.cachedTurn;
         if (cached != null && cached.isUsable()) {
            return CompletableFuture.completedFuture(cached.turnAuth().toRtcIceServer());
         }

         if (this.pendingTurnRefresh != null) {
            return this.pendingTurnRefresh;
         }

         CompletableFuture<RTCIceServer> refresh = this.refreshTurnAuth();
         this.pendingTurnRefresh = refresh;
         refresh.whenCompleteAsync((var1x, var2x) -> this.pendingTurnRefresh = null, this.executor);
         return refresh;
      }, this.executor);
   }

   public CompletableFuture<Void> sendClientMessage(final UUID toPlayerId, final SignalingMessage message) {
      String encoded = ((JsonElement)SignalingMessage.CODEC.encodeStart(JsonOps.INSTANCE, message).getOrThrow(IllegalStateException::new)).toString();
      return CompletableFuture.completedFuture(null)
         .thenComposeAsync(
            var3x -> this.sendRequest(
               "Signaling_SendClientMessage_v1_0", List.of(JsonNull.INSTANCE, new JsonPrimitive(toPlayerId.toString()), new JsonPrimitive(encoded))
            ),
            this.executor
         )
         .thenApply(var0 -> null)
         .handle((result, err) -> {
            if (err != null) {
               if (err.getCause() instanceof JsonRpcException rpcErr) {
                  SignalingException mapped = SignalingErrorMapper.fromJsonRpc(toPlayerId, rpcErr);
                  LOGGER.warn("Signaling rejected send: {}", mapped.getMessage());
                  this.fireListeners(l -> l.onSignalingError(toPlayerId, mapped));
               }
               throw new CompletionException(err);
            }
            return null;
         })
         .thenCompose(v -> CompletableFuture.completedFuture(null));
   }

   private void fireListeners(final Consumer<SignalingServiceClient.ConnectionListener> action) {
      for (SignalingServiceClient.ConnectionListener listener : this.connectionListeners) {
         try {
            action.accept(listener);
         } catch (RuntimeException e) {
            LOGGER.error("ConnectionListener {} threw", listener.getClass().getSimpleName(), e);
         }
      }
   }

   private void connectWebSocket() {
      if (this.websocketConnect == null) {
         HttpClient client = HttpClient.newBuilder().executor(Util.backgroundExecutor()).build();
         this.httpClient = client;
         JsonRpcClient rpc = new JsonRpcClient(this.executor, this::onRpcMethod, this::onWebsocketDown);
         String requestId = UUID.randomUUID().toString();
         this.websocketConnect = this.getSignalingUri(client, requestId)
            .thenComposeAsync(wsUrl -> this.openWebSocket(client, rpc, wsUrl, requestId), this.executor);
         this.websocketConnect.whenCompleteAsync((var2x, err) -> {
            if (err != null) {
               Throwable cause = err instanceof CompletionException && err.getCause() != null ? err.getCause() : err;
               if (!this.isSameHttpClientSession(client)) {
                  LOGGER.debug("Stale signaling connect attempt failed", cause);
               } else {
                  LOGGER.warn("Signaling WebSocket connect failed", cause);
                  this.cachedSignalingUri = null;
                  if (this.teardown("websocket connect failed: " + cause.getMessage())) {
                     this.fireListeners(SignalingServiceClient.ConnectionListener::onSignalingConnectFailed);
                  }
               }
            }
         }, this.executor);
      }
   }

   private void onWebsocketDown() {
      if (this.teardown("websocket closed")) {
         this.fireListeners(SignalingServiceClient.ConnectionListener::onSignalingDisconnected);
      }
   }

   private boolean teardown(final String reason) {
      HttpClient client = this.httpClient;
      CompletableFuture<JsonRpcClient> connectFuture = this.websocketConnect;
      if (client != null && connectFuture != null) {
         LOGGER.debug("Signaling session disconnecting ({})", reason);
         if (this.pingTask != null) {
            this.pingTask.cancel(false);
            this.pingTask = null;
         }

         this.pendingTurnRefresh = null;
         connectFuture.whenComplete((rpc, var2x) -> {
            CompletableFuture<?> rpcClosed = rpc != null ? rpc.close() : CompletableFuture.completedFuture(null);
            rpcClosed.whenComplete((var1x, var2xx) -> CompletableFuture.runAsync(client::close, Util.backgroundExecutor()));
         });
         connectFuture.completeExceptionally(new IllegalStateException("Signaling torn down: " + reason));
         this.httpClient = null;
         this.websocketConnect = null;
         return true;
      } else {
         return false;
      }
   }

   private CompletableFuture<String> getSignalingUri(final HttpClient client, final String requestId) {
      SignalingServiceClient.CachedSignalingUri cached = this.cachedSignalingUri;
      if (cached != null && cached.isUsable()) {
         return CompletableFuture.completedFuture(cached.wsUrl);
      }

      HttpRequest request = HttpRequest.newBuilder()
         .uri(URI.create(ENVIRONMENT.getConfigurationUri()))
         .header("x-mojangauth", this.user.getAccessToken())
         .header("Session-Id", this.sessionId)
         .header("Request-Id", requestId)
         .GET()
         .build();
      return client.sendAsync(request, BodyHandlers.ofString())
         .thenApplyAsync(
            response -> {
               if (response.statusCode() != 200) {
                  throw new IllegalStateException("Unexpected config response status: " + response.statusCode());
               }

               String baseUri = (String)SIGNALING_URI_CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(response.body()))
                  .getOrThrow(s -> new IllegalStateException("Malformed config response: " + s));
               String wsUrl = baseUri + "/ws/v1.0/messaging/connect/java";
               this.cachedSignalingUri = new SignalingServiceClient.CachedSignalingUri(wsUrl);
               return wsUrl;
            },
            this.executor
         );
   }

   private CompletableFuture<JsonRpcClient> openWebSocket(final HttpClient client, final JsonRpcClient rpc, final String wsUrl, final String requestId) {
      return !this.isSameHttpClientSession(client)
         ? CompletableFuture.failedFuture(new IllegalStateException("Signaling torn down before WebSocket open"))
         : client.newWebSocketBuilder()
            .header("x-mojangauth", this.user.getAccessToken())
            .header("Session-Id", this.sessionId)
            .header("Request-Id", requestId)
            .buildAsync(URI.create(wsUrl), rpc)
            .thenComposeAsync(webSocket -> {
               if (this.isSameHttpClientSession(client)) {
                  this.schedulePing(rpc);
                  this.fireListeners(SignalingServiceClient.ConnectionListener::onSignalingConnected);
                  return CompletableFuture.completedFuture(rpc);
               } else {
                  webSocket.abort();
                  return CompletableFuture.failedFuture(new IllegalStateException("Stale signaling WebSocket connection"));
               }
            }, this.executor);
   }

   private void schedulePing(final JsonRpcClient rpc) {
      this.pingTask = this.executor.scheduleAtFixedRate(() -> {
         try {
            rpc.sendNotification("System_Ping_v1_0");
         } catch (RuntimeException e) {
            LOGGER.debug("Signaling ping failed", e);
         }
      }, PING_INTERVAL.toMillis(), PING_INTERVAL.toMillis(), TimeUnit.MILLISECONDS);
   }

   private boolean isSameHttpClientSession(final HttpClient client) {
      return this.httpClient == client;
   }

   private CompletableFuture<RTCIceServer> refreshTurnAuth() {
      return this.sendRequest("Signaling_TurnAuth_v1_0", List.of())
         .handle((result, error) -> {
            if (error != null) {
               if (error.getCause() instanceof JsonRpcException jre) {
                  throw new CompletionException(new SignalingException.TurnAuthFailedException(jre.serverMessage()));
               }
               throw new CompletionException(error);
            }
            return result;
         })
         .thenCompose(result -> CompletableFuture.completedFuture(result))
         .thenApplyAsync(
            result -> {
               SignalingServiceClient.TurnAuthResult turnAuth = (SignalingServiceClient.TurnAuthResult)SignalingServiceClient.TurnAuthResult.CODEC
                  .parse(JsonOps.INSTANCE, result)
                  .getOrThrow(s -> new IllegalStateException("Malformed TurnAuth response: " + s));
               RTCIceServer ice = turnAuth.toRtcIceServer();
               this.cachedTurn = new SignalingServiceClient.CachedTurn(turnAuth);
               return ice;
            },
            this.executor
         );
   }

   private CompletableFuture<JsonElement> sendRequest(final String method, final List<JsonElement> params) {
      return this.websocketConnect == null
         ? CompletableFuture.failedFuture(new IllegalStateException("Signaling is not connected; call connect() first"))
         : this.websocketConnect.thenCompose(r -> r.sendRequest(method, params));
   }

   private void onRpcMethod(final JsonRpcClient rpc, final @Nullable JsonElement id, final String method, final @Nullable JsonElement params) {
      switch (method) {
         case "System_Pong_v1_0":
            break;
         case "Signaling_ReceiveMessage_v1_0":
            this.handleReceiveMessage(rpc, id, params);
            break;
         default:
            if (id != null) {
               rpc.sendError(id, JsonRPCErrors.METHOD_NOT_FOUND, method);
            }
      }
   }

   private void handleReceiveMessage(final JsonRpcClient rpc, final @Nullable JsonElement id, final @Nullable JsonElement params) {
      JsonArray arr = params != null && params.isJsonArray() ? params.getAsJsonArray() : null;
      JsonElement first = arr != null && !arr.isEmpty() ? arr.get(0) : null;
      if (first != null && first.isJsonObject()) {
         if (id != null) {
            rpc.sendResponse(id, new JsonObject());
         }

         SignalingServiceClient.SignalingServiceMessage msg;
         try {
            msg = (SignalingServiceClient.SignalingServiceMessage)SignalingServiceClient.SignalingServiceMessage.CODEC
               .parse(JsonOps.INSTANCE, first)
               .getOrThrow(IllegalStateException::new);
         } catch (RuntimeException e) {
            LOGGER.warn("Malformed ReceiveMessage envelope: {}", e.getMessage());
            return;
         }

         JsonElement inner;
         try {
            inner = JsonParser.parseString(msg.message());
         } catch (JsonSyntaxException e) {
            LOGGER.warn("Dropping non-JSON signaling payload: {}", e.getMessage());
            return;
         }

         SignalingException serviceError = SignalingErrorMapper.fromServiceEnvelope(inner);
         if (serviceError != null) {
            LOGGER.debug("Signaling service reported error from {}: {}", msg.from(), serviceError.getMessage());
            this.fireListeners(l -> l.onSignalingError(serviceError.peerPmid(), serviceError));
         } else {
            SignalingMessage parsed;
            try {
               parsed = (SignalingMessage)SignalingMessage.CODEC.parse(JsonOps.INSTANCE, inner).getOrThrow(IllegalStateException::new);
            } catch (RuntimeException e) {
               LOGGER.warn("Malformed signaling payload: {}", e.getMessage());
               return;
            }

            UUID fromPmid = parsePmid(msg.from());
            if (fromPmid != null) {
               switch (parsed) {
                  case SignalingMessage.FriendJoin friendJoin:
                     this.dispatchFriendJoinMessage(fromPmid, friendJoin);
                     break;
                  case SignalingMessage.WebRtc webRtc:
                     this.dispatchWebRtcMessage(fromPmid, webRtc);
                     break;
                  default:
                     throw new MatchException(null, null);
               }
            }
         }
      } else {
         LOGGER.warn(
            "Malformed ReceiveMessage params (type={}, size={})",
            params == null ? "null" : params.getClass().getSimpleName(),
            params == null ? 0 : params.toString().length()
         );
         if (id != null) {
            rpc.sendError(id, JsonRPCErrors.INVALID_PARAMS, "Expected [object] params");
         }
      }
   }

   private static @Nullable UUID parsePmid(final String raw) {
      try {
         return UUID.fromString(raw);
      } catch (IllegalArgumentException e) {
         LOGGER.warn("Dropping peer signaling message with non-PMID sender: {}", raw);
         return null;
      }
   }

   private void dispatchFriendJoinMessage(final UUID fromPmid, final SignalingMessage.FriendJoin message) {
      SignalingServiceClient.FriendJoinHandler handler = this.friendJoinHandler;
      if (handler != null) {
         try {
            handler.handle(fromPmid, message);
         } catch (RuntimeException e) {
            LOGGER.error("Failed to dispatch FriendJoin message", e);
         }
      }
   }

   private void dispatchWebRtcMessage(final UUID fromPmid, final SignalingMessage.WebRtc message) {
      SignalingServiceClient.WebRtcSignalingHandler handler = this.webRtcSignalingHandler;
      if (handler != null) {
         try {
            handler.handle(fromPmid, message);
         } catch (RuntimeException e) {
            LOGGER.error("Failed to dispatch WebRTC signaling message", e);
         }
      }
   }

   private record CachedSignalingUri(String wsUrl, Instant expiresAt) {
      private static final Duration TTL = Duration.ofMinutes(5L);

      private CachedSignalingUri(final String wsUrl) {
         this(wsUrl, Instant.now().plus(TTL));
      }

      private boolean isUsable() {
         return Instant.now().isBefore(this.expiresAt);
      }
   }

   private record CachedTurn(SignalingServiceClient.TurnAuthResult turnAuth, Instant expiresAt) {
      private static final Duration EXPIRY_MARGIN = Duration.ofSeconds(60L);

      private CachedTurn(final SignalingServiceClient.TurnAuthResult turnAuth) {
         this(turnAuth, Instant.now().plusSeconds(turnAuth.expirationInSeconds()));
      }

      private boolean isUsable() {
         return Instant.now().isBefore(this.expiresAt.minus(EXPIRY_MARGIN));
      }
   }

   public interface ConnectionListener {
      default void onSignalingError(final @Nullable UUID peerPmid, final SignalingException cause) {
      }

      default void onSignalingConnected() {
      }

      default void onSignalingDisconnected() {
      }

      default void onSignalingConnectFailed() {
      }
   }

   private enum Environment {
      STAGE("https://signaling-afd.stage-6fd5f759.franchise.minecraft-services.net"),
      PRODUCTION("https://signaling-afd.franchise.minecraft-services.net");

      private static final String CONFIGURATION_ENDPOINT = "/api/v1.0/configuration/java";
      private final String baseUrl;

      Environment(final String baseUrl) {
         this.baseUrl = baseUrl;
      }

      private static Optional<SignalingServiceClient.Environment> byName(final String name) {
         return switch (name.toLowerCase(Locale.ROOT)) {
            case "stage", "staging" -> Optional.of(STAGE);
            case "prod", "production" -> Optional.of(PRODUCTION);
            default -> Optional.empty();
         };
      }

      private String getConfigurationUri() {
         return this.baseUrl + "/api/v1.0/configuration/java";
      }
   }

   @FunctionalInterface
   public interface FriendJoinHandler {
      void handle(UUID fromPmid, SignalingMessage.FriendJoin message);
   }

   private static final class RpcMethods {
      private static final String PING = "System_Ping_v1_0";
      private static final String PONG = "System_Pong_v1_0";
      private static final String TURN_AUTH = "Signaling_TurnAuth_v1_0";
      private static final String SEND_CLIENT_MSG = "Signaling_SendClientMessage_v1_0";
      private static final String RECEIVE_MESSAGE = "Signaling_ReceiveMessage_v1_0";
   }

   private record SignalingServiceMessage(String from, String message, @Nullable UUID id) {
      private static final Codec<SignalingServiceClient.SignalingServiceMessage> CODEC = RecordCodecBuilder.create(
         i -> i.group(
               Codec.STRING.fieldOf("From").forGetter(SignalingServiceClient.SignalingServiceMessage::from),
               Codec.STRING.fieldOf("Message").forGetter(SignalingServiceClient.SignalingServiceMessage::message),
               UUIDUtil.STRING_CODEC.optionalFieldOf("Id").forGetter(c -> Optional.ofNullable(c.id()))
            )
            .apply(i, (from, msg, id) -> new SignalingServiceClient.SignalingServiceMessage(from, msg, (UUID)id.orElse(null)))
      );
   }

   private record TurnAuthResult(long expirationInSeconds, List<SignalingServiceClient.TurnAuthServer> turnAuthServers) {
      private static final Codec<SignalingServiceClient.TurnAuthResult> CODEC = RecordCodecBuilder.create(
         i -> i.group(
               Codec.LONG.fieldOf("ExpirationInSeconds").forGetter(SignalingServiceClient.TurnAuthResult::expirationInSeconds),
               SignalingServiceClient.TurnAuthServer.CODEC
                  .listOf()
                  .fieldOf("TurnAuthServers")
                  .forGetter(SignalingServiceClient.TurnAuthResult::turnAuthServers)
            )
            .apply(i, SignalingServiceClient.TurnAuthResult::new)
      );

      private RTCIceServer toRtcIceServer() {
         SignalingServiceClient.TurnAuthServer first = this.turnAuthServers.getFirst();
         RTCIceServer ice = new RTCIceServer();
         ice.username = first.username();
         ice.password = first.password();

         for (SignalingServiceClient.TurnAuthServer s : this.turnAuthServers) {
            ice.urls.addAll(s.urls());
         }

         return ice;
      }
   }

   private record TurnAuthServer(String username, String password, List<String> urls) {
      private static final Codec<SignalingServiceClient.TurnAuthServer> CODEC = RecordCodecBuilder.create(
         i -> i.group(
               Codec.STRING.fieldOf("Username").forGetter(SignalingServiceClient.TurnAuthServer::username),
               Codec.STRING.fieldOf("Password").forGetter(SignalingServiceClient.TurnAuthServer::password),
               Codec.STRING.listOf().fieldOf("Urls").forGetter(SignalingServiceClient.TurnAuthServer::urls)
            )
            .apply(i, SignalingServiceClient.TurnAuthServer::new)
      );

      @Override
      public String toString() {
         return "TurnAuthServer[username=" + this.username + ", password=<hidden>, urls=" + this.urls + "]";
      }
   }

   @FunctionalInterface
   public interface WebRtcSignalingHandler {
      void handle(UUID fromPmid, SignalingMessage.WebRtc message);
   }
}
