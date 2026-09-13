package net.minecraft.server.jsonrpc;

import com.google.common.annotations.VisibleForTesting;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.logging.LogUtils;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.ReadTimeoutException;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.jsonrpc.internalapi.MinecraftApi;
import net.minecraft.server.jsonrpc.methods.ClientInfo;
import net.minecraft.server.jsonrpc.methods.EncodeJsonRpcException;
import net.minecraft.server.jsonrpc.methods.InvalidParameterJsonRpcException;
import net.minecraft.server.jsonrpc.methods.InvalidRequestJsonRpcException;
import net.minecraft.server.jsonrpc.methods.MethodNotFoundJsonRpcException;
import net.minecraft.server.jsonrpc.methods.RemoteRpcErrorException;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.Util;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class Connection extends SimpleChannelInboundHandler<JsonElement> {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final AtomicInteger CONNECTION_ID_COUNTER = new AtomicInteger(0);
   private final JsonRpcLogger jsonRpcLogger;
   private final ClientInfo clientInfo;
   private final ManagementServer managementServer;
   private final Channel channel;
   private final MinecraftApi minecraftApi;
   private final AtomicInteger transactionId = new AtomicInteger();
   private final Int2ObjectMap<PendingRpcRequest<?>> pendingRequests = Int2ObjectMaps.synchronize(new Int2ObjectOpenHashMap());

   public Connection(Channel p_426477_, ManagementServer p_430885_, MinecraftApi p_423684_, JsonRpcLogger p_422992_) {
      this.clientInfo = ClientInfo.of(CONNECTION_ID_COUNTER.incrementAndGet());
      this.managementServer = p_430885_;
      this.minecraftApi = p_423684_;
      this.channel = p_426477_;
      this.jsonRpcLogger = p_422992_;
   }

   public void tick() {
      long i = Util.getMillis();
      this.pendingRequests
         .int2ObjectEntrySet()
         .removeIf(
            p_449115_ -> {
               boolean flag = ((PendingRpcRequest)p_449115_.getValue()).timedOut(i);
               if (flag) {
                  ((PendingRpcRequest)p_449115_.getValue())
                     .resultFuture()
                     .completeExceptionally(
                        new ReadTimeoutException(
                           "RPC method " + ((PendingRpcRequest)p_449115_.getValue()).method().key().identifier() + " timed out waiting for response"
                        )
                     );
               }

               return flag;
            }
         );
   }

   public void channelActive(ChannelHandlerContext p_425017_) throws Exception {
      this.jsonRpcLogger.log(this.clientInfo, "Management connection opened for {}", this.channel.remoteAddress());
      super.channelActive(p_425017_);
      this.managementServer.onConnected(this);
   }

   public void channelInactive(ChannelHandlerContext p_422732_) throws Exception {
      this.jsonRpcLogger.log(this.clientInfo, "Management connection closed for {}", this.channel.remoteAddress());
      super.channelInactive(p_422732_);
      this.managementServer.onDisconnected(this);
   }

   public void exceptionCaught(ChannelHandlerContext p_422364_, Throwable p_423460_) throws Exception {
      if (p_423460_.getCause() instanceof JsonParseException) {
         this.channel.writeAndFlush(JsonRPCErrors.PARSE_ERROR.createWithUnknownId(p_423460_.getMessage()));
      } else {
         super.exceptionCaught(p_422364_, p_423460_);
         this.channel.close().awaitUninterruptibly();
      }
   }

   protected void channelRead0(ChannelHandlerContext p_425031_, JsonElement p_423367_) {
      if (p_423367_.isJsonObject()) {
         JsonObject jsonobject = this.handleJsonObject(p_423367_.getAsJsonObject());
         if (jsonobject != null) {
            this.channel.writeAndFlush(jsonobject);
         }
      } else if (p_423367_.isJsonArray()) {
         this.channel.writeAndFlush(this.handleBatchRequest(p_423367_.getAsJsonArray().asList()));
      } else {
         this.channel.writeAndFlush(JsonRPCErrors.INVALID_REQUEST.createWithUnknownId(null));
      }
   }

   private JsonArray handleBatchRequest(List<JsonElement> p_431423_) {
      JsonArray jsonarray = new JsonArray();
      p_431423_.stream().map(p_428168_ -> this.handleJsonObject(p_428168_.getAsJsonObject())).filter(Objects::nonNull).forEach(jsonarray::add);
      return jsonarray;
   }

   public void sendNotification(Holder.Reference<? extends OutgoingRpcMethod<Void, ?>> p_423389_) {
      this.sendRequest(p_423389_, null, false);
   }

   public <Params> void sendNotification(Holder.Reference<? extends OutgoingRpcMethod<Params, ?>> p_429716_, Params p_422987_) {
      this.sendRequest(p_429716_, p_422987_, false);
   }

   public <Result> CompletableFuture<Result> sendRequest(Holder.Reference<? extends OutgoingRpcMethod<Void, Result>> p_430250_) {
      return this.sendRequest(p_430250_, null, true);
   }

   public <Params, Result> CompletableFuture<Result> sendRequest(Holder.Reference<? extends OutgoingRpcMethod<Params, Result>> p_426489_, Params p_424704_) {
      return this.sendRequest(p_426489_, p_424704_, true);
   }

   @Contract("_,_,false->null;_,_,true->!null")
   private <Params, Result> @Nullable CompletableFuture<Result> sendRequest(
      Holder.Reference<? extends OutgoingRpcMethod<Params, ? extends Result>> p_424976_, @Nullable Params p_431689_, boolean p_428466_
   ) {
      List<JsonElement> list = p_431689_ != null ? List.of(Objects.requireNonNull(p_424976_.value().encodeParams(p_431689_))) : List.of();
      if (p_428466_) {
         CompletableFuture<Result> completablefuture = new CompletableFuture<>();
         int i = this.transactionId.incrementAndGet();
         long j = Util.timeSource.get(TimeUnit.MILLISECONDS);
         this.pendingRequests.put(i, new PendingRpcRequest<>(p_424976_, completablefuture, j + 5000L));
         this.channel.writeAndFlush(JsonRPCUtils.createRequest(i, p_424976_.key().identifier(), list));
         return completablefuture;
      } else {
         this.channel.writeAndFlush(JsonRPCUtils.createRequest(null, p_424976_.key().identifier(), list));
         return null;
      }
   }

   @VisibleForTesting
   @Nullable JsonObject handleJsonObject(JsonObject p_428341_) {
      try {
         JsonElement jsonelement = JsonRPCUtils.getRequestId(p_428341_);
         String s = JsonRPCUtils.getMethodName(p_428341_);
         JsonElement jsonelement1 = JsonRPCUtils.getResult(p_428341_);
         JsonElement jsonelement2 = JsonRPCUtils.getParams(p_428341_);
         JsonObject jsonobject = JsonRPCUtils.getError(p_428341_);
         if (s != null && jsonelement1 == null && jsonobject == null) {
            return jsonelement != null && !isValidRequestId(jsonelement)
               ? JsonRPCErrors.INVALID_REQUEST.createWithUnknownId("Invalid request id - only String, Number and NULL supported")
               : this.handleIncomingRequest(jsonelement, s, jsonelement2);
         }

         if (s == null && jsonelement1 != null && jsonobject == null && jsonelement != null) {
            if (isValidResponseId(jsonelement)) {
               this.handleRequestResponse(jsonelement.getAsInt(), jsonelement1);
            } else {
               LOGGER.warn("Received respose {} with id {} we did not request", jsonelement1, jsonelement);
            }

            return null;
         } else {
            return s == null && jsonelement1 == null && jsonobject != null
               ? this.handleError(jsonelement, jsonobject)
               : JsonRPCErrors.INVALID_REQUEST.createWithoutData(Objects.requireNonNullElse(jsonelement, JsonNull.INSTANCE));
         }
      } catch (Exception exception) {
         LOGGER.error("Error while handling rpc request", exception);
         return JsonRPCErrors.INTERNAL_ERROR.createWithUnknownId("Unknown error handling request - check server logs for stack trace");
      }
   }

   private static boolean isValidRequestId(JsonElement p_429929_) {
      return p_429929_.isJsonNull() || GsonHelper.isNumberValue(p_429929_) || GsonHelper.isStringValue(p_429929_);
   }

   private static boolean isValidResponseId(JsonElement p_422655_) {
      return GsonHelper.isNumberValue(p_422655_);
   }

   private @Nullable JsonObject handleIncomingRequest(@Nullable JsonElement p_423022_, String p_425180_, @Nullable JsonElement p_423468_) {
      boolean flag = p_423022_ != null;

      try {
         JsonElement jsonelement = this.dispatchIncomingRequest(p_425180_, p_423468_);
         return jsonelement != null && flag ? JsonRPCUtils.createSuccessResult(p_423022_, jsonelement) : null;
      } catch (InvalidParameterJsonRpcException invalidparameterjsonrpcexception) {
         LOGGER.debug("Invalid parameter invocation {}: {}, {}", new Object[]{p_425180_, p_423468_, invalidparameterjsonrpcexception.getMessage()});
         return flag ? JsonRPCErrors.INVALID_PARAMS.create(p_423022_, invalidparameterjsonrpcexception.getMessage()) : null;
      } catch (EncodeJsonRpcException encodejsonrpcexception) {
         LOGGER.error("Failed to encode json rpc response {}: {}", p_425180_, encodejsonrpcexception.getMessage());
         return flag ? JsonRPCErrors.INTERNAL_ERROR.create(p_423022_, encodejsonrpcexception.getMessage()) : null;
      } catch (InvalidRequestJsonRpcException invalidrequestjsonrpcexception) {
         return flag ? JsonRPCErrors.INVALID_REQUEST.create(p_423022_, invalidrequestjsonrpcexception.getMessage()) : null;
      } catch (MethodNotFoundJsonRpcException methodnotfoundjsonrpcexception) {
         return flag ? JsonRPCErrors.METHOD_NOT_FOUND.create(p_423022_, methodnotfoundjsonrpcexception.getMessage()) : null;
      } catch (Exception exception) {
         LOGGER.error("Error while dispatching rpc method {}", p_425180_, exception);
         return flag ? JsonRPCErrors.INTERNAL_ERROR.createWithoutData(p_423022_) : null;
      }
   }

   public @Nullable JsonElement dispatchIncomingRequest(String p_426702_, @Nullable JsonElement p_429101_) {
      Identifier identifier = Identifier.tryParse(p_426702_);
      if (identifier == null) {
         throw new InvalidRequestJsonRpcException("Failed to parse method value: " + p_426702_);
      }

      Optional<IncomingRpcMethod<?, ?>> optional = BuiltInRegistries.INCOMING_RPC_METHOD.getOptional(identifier);
      if (optional.isEmpty()) {
         throw new MethodNotFoundJsonRpcException("Method not found: " + p_426702_);
      }

      if (optional.get().attributes().runOnMainThread()) {
         try {
            return this.minecraftApi.<JsonElement>submit(() -> optional.get().apply(this.minecraftApi, p_429101_, this.clientInfo)).join();
         } catch (CompletionException completionexception) {
            if (completionexception.getCause() instanceof RuntimeException runtimeexception) {
               throw runtimeexception;
            } else {
               throw completionexception;
            }
         }
      } else {
         return optional.get().apply(this.minecraftApi, p_429101_, this.clientInfo);
      }
   }

   private void handleRequestResponse(int p_425412_, JsonElement p_424729_) {
      PendingRpcRequest<?> pendingrpcrequest = (PendingRpcRequest<?>)this.pendingRequests.remove(p_425412_);
      if (pendingrpcrequest == null) {
         LOGGER.warn("Received unknown response (id: {}): {}", p_425412_, p_424729_);
      } else {
         pendingrpcrequest.accept(p_424729_);
      }
   }

   private @Nullable JsonObject handleError(@Nullable JsonElement p_428115_, JsonObject p_424320_) {
      if (p_428115_ != null && isValidResponseId(p_428115_)) {
         PendingRpcRequest<?> pendingrpcrequest = (PendingRpcRequest<?>)this.pendingRequests.remove(p_428115_.getAsInt());
         if (pendingrpcrequest != null) {
            pendingrpcrequest.resultFuture().completeExceptionally(new RemoteRpcErrorException(p_428115_, p_424320_));
         }
      }

      LOGGER.error("Received error (id: {}): {}", p_428115_, p_424320_);
      return null;
   }
}
