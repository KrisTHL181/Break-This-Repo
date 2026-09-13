package net.minecraft.client.multiplayer.p2p.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.net.http.WebSocket;
import java.net.http.WebSocket.Listener;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ScheduledExecutorService;
import net.minecraft.server.jsonrpc.JsonRPCErrors;
import net.minecraft.server.jsonrpc.JsonRPCUtils;
import net.minecraft.util.StrictJsonParser;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public final class JsonRpcClient implements Listener {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final int MAX_MESSAGE_BYTES = 65536;
   private final ScheduledExecutorService executor;
   private final JsonRpcClient.MethodHandler methodHandler;
   private final Runnable onDisconnect;
   private final Map<Integer, CompletableFuture<JsonElement>> pendingRequests = new HashMap<>();
   private final StringBuilder messageBuffer = new StringBuilder();
   private CompletableFuture<Void> sendChain = CompletableFuture.completedFuture(null);
   private @Nullable WebSocket webSocket;
   private int transactionId;

   public JsonRpcClient(final ScheduledExecutorService executor, final JsonRpcClient.MethodHandler methodHandler, final Runnable onDisconnect) {
      this.executor = executor;
      this.methodHandler = methodHandler;
      this.onDisconnect = onDisconnect;
   }

   @Override
   public void onOpen(final WebSocket webSocket) {
      LOGGER.debug("WebSocket connection established");
      this.executor.execute(() -> {
         this.webSocket = webSocket;
         webSocket.request(1L);
      });
   }

   @Override
   public CompletionStage<?> onClose(final WebSocket webSocket, final int statusCode, final String reason) {
      this.executor.execute(() -> this.teardown(new IOException("Signaling WebSocket closed (code=" + statusCode + ", reason=" + reason + ")"), true));
      return CompletableFuture.completedFuture(null);
   }

   @Override
   public CompletionStage<?> onText(final WebSocket webSocket, final CharSequence chars, final boolean last) {
      String slice = chars.toString();
      this.executor.execute(() -> {
         this.appendAndDispatch(slice, last);
         webSocket.request(1L);
      });
      return CompletableFuture.completedFuture(null);
   }

   @Override
   public void onError(final WebSocket webSocket, final Throwable error) {
      this.executor.execute(() -> this.teardown(new IOException("Signaling WebSocket errored", error), true));
   }

   private static boolean isValidResponseId(final JsonElement id) {
      return id instanceof JsonPrimitive p && p.isNumber();
   }

   public void sendNotification(final String method) {
      this.executor.execute(() -> this.send(JsonRPCUtils.createRequest(null, method, List.of()).toString()));
   }

   public void sendResponse(final JsonElement id, final JsonElement result) {
      this.executor.execute(() -> this.send(JsonRPCUtils.createSuccessResult(id, result).toString()));
   }

   public void sendError(final JsonElement id, final JsonRPCErrors error) {
      this.executor.execute(() -> this.send(error.createWithoutData(id).toString()));
   }

   public void sendError(final JsonElement id, final JsonRPCErrors error, final String data) {
      this.executor.execute(() -> this.send(error.create(id, data).toString()));
   }

   private void send(final String payload) {
      WebSocket ws = this.webSocket;
      if (ws != null) {
         this.sendChain = this.sendChain.<Void>thenCompose(var2x -> ws.sendText(payload, true).thenApply(var0x -> (Void)null)).exceptionally(err -> {
            LOGGER.debug("WebSocket send failed", err);
            return null;
         });
      }
   }

   public CompletableFuture<?> close() {
      CompletableFuture<?> done = new CompletableFuture();
      this.executor.execute(() -> {
         WebSocket ws = this.webSocket;
         this.teardown(new IOException("JSON-RPC client closed"), false);
         if (ws != null && !ws.isOutputClosed()) {
            ws.sendClose(1000, "shutdown").whenComplete((var1x, var2x) -> done.complete(null));
         } else {
            done.complete(null);
         }
      });
      return done;
   }

   public CompletableFuture<JsonElement> sendRequest(final String method, final List<JsonElement> params) {
      CompletableFuture<JsonElement> future = new CompletableFuture<>();
      this.executor.execute(() -> {
         WebSocket ws = this.webSocket;
         if (ws == null) {
            future.completeExceptionally(new IOException("WebSocket is not connected"));
         } else {
            int id = ++this.transactionId;
            String payload = JsonRPCUtils.createRequest(id, method, params).toString();
            this.pendingRequests.put(id, future);
            this.sendChain = this.sendChain.<Void>thenCompose(var2x -> ws.sendText(payload, true).thenApply(var0x -> (Void)null)).exceptionally(err -> {
               LOGGER.debug("WebSocket send failed", err);
               this.executor.execute(() -> {
                  CompletableFuture<JsonElement> pending = this.pendingRequests.remove(id);
                  if (pending != null) {
                     pending.completeExceptionally(new IOException("WebSocket send failed", err));
                  }
               });
               return null;
            });
         }
      });
      return future;
   }

   private void appendAndDispatch(final String slice, final boolean last) {
      if (this.messageBuffer.length() + slice.length() > 65536) {
         LOGGER.warn("JSON-RPC message exceeded {} bytes, dropping", 65536);
         this.messageBuffer.setLength(0);
      } else {
         this.messageBuffer.append(slice);
         if (last) {
            String full = this.messageBuffer.toString();
            this.messageBuffer.setLength(0);

            try {
               this.dispatch(full);
            } catch (RuntimeException e) {
               LOGGER.error("Failed to handle JSON-RPC message ({} bytes)", full.length(), e);
               LOGGER.trace("Offending JSON-RPC payload: {}", full);
            }
         }
      }
   }

   private void dispatch(final String text) {
      JsonObject obj;
      try {
         JsonElement root = StrictJsonParser.parse(text);
         if (!root.isJsonObject()) {
            LOGGER.warn("Dropping non-object JSON-RPC message");
            return;
         }

         obj = root.getAsJsonObject();
      } catch (JsonSyntaxException e) {
         LOGGER.warn("Dropping unparseable JSON-RPC message: {}", e.getMessage());
         return;
      }

      JsonElement id = JsonRPCUtils.getRequestId(obj);
      boolean hasId = id != null && !id.isJsonNull();
      String method = JsonRPCUtils.getMethodName(obj);
      JsonElement result = JsonRPCUtils.getResult(obj);
      JsonObject error = JsonRPCUtils.getError(obj);
      if (method != null && result == null && error == null) {
         this.invokeHandler(hasId ? id : null, method, JsonRPCUtils.getParams(obj));
      } else if (method == null && error == null && result != null && hasId) {
         if (isValidResponseId(id)) {
            this.completePending(id.getAsInt(), result);
         } else {
            LOGGER.warn("Ignoring JSON-RPC response with non-numeric id: {}", id);
         }
      } else if (method == null && result == null && error != null) {
         this.handleErrorResponse(hasId ? id : null, error);
      } else {
         LOGGER.warn("Dropping invalid JSON-RPC envelope");
      }
   }

   private void completePending(final int id, final JsonElement result) {
      CompletableFuture<JsonElement> pending = this.pendingRequests.remove(id);
      if (pending != null) {
         pending.complete(result);
      } else {
         LOGGER.warn("Received JSON-RPC result for unknown request id={}", id);
      }
   }

   private void handleErrorResponse(final @Nullable JsonElement id, final JsonObject error) {
      int code = error.has("code") ? error.get("code").getAsInt() : 0;
      String message = error.has("message") ? error.get("message").getAsString() : "";
      JsonElement data = error.get("data");
      if (id == null) {
         LOGGER.warn("JSON-RPC error (no id): code={} message={}", code, message);
      } else if (!isValidResponseId(id)) {
         LOGGER.warn("Ignoring JSON-RPC error with non-numeric id: {}", id);
      } else {
         CompletableFuture<JsonElement> pending = this.pendingRequests.remove(id.getAsInt());
         if (pending != null) {
            pending.completeExceptionally(new JsonRpcException(code, message, data));
         } else {
            LOGGER.warn("Received JSON-RPC error for unknown request id={}: code={} message={}", new Object[]{id, code, message});
         }
      }
   }

   private void invokeHandler(final @Nullable JsonElement id, final String method, final @Nullable JsonElement params) {
      try {
         this.methodHandler.onMethod(this, id, method, params);
      } catch (RuntimeException e) {
         LOGGER.error("JSON-RPC handler threw for method {}", method, e);
         if (id != null) {
            this.sendError(id, JsonRPCErrors.INTERNAL_ERROR);
         }
      }
   }

   private void teardown(final Throwable cause, final boolean fireDisconnect) {
      this.webSocket = null;
      this.pendingRequests.values().forEach(p -> p.completeExceptionally(cause));
      this.pendingRequests.clear();
      if (fireDisconnect) {
         try {
            this.onDisconnect.run();
         } catch (RuntimeException e) {
            LOGGER.error("onDisconnect callback threw", e);
         }
      }
   }

   @FunctionalInterface
   public interface MethodHandler {
      void onMethod(JsonRpcClient rpc, @Nullable JsonElement id, String method, @Nullable JsonElement params);
   }
}
