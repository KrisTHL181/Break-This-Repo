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
import net.minecraft.server.notifications.NotificationManager;
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
    private final Int2ObjectMap<PendingRpcRequest<?>> pendingRequests = Int2ObjectMaps.synchronize(new Int2ObjectOpenHashMap<>());

    public Connection(final Channel channel, final ManagementServer managementServer, final MinecraftApi minecraftApi, final JsonRpcLogger jsonrpcLogger) {
        this.clientInfo = ClientInfo.of(CONNECTION_ID_COUNTER.incrementAndGet());
        this.managementServer = managementServer;
        this.minecraftApi = minecraftApi;
        this.channel = channel;
        this.jsonRpcLogger = jsonrpcLogger;
    }

    public void tick() {
        long time = Util.getMillis();
        this.pendingRequests
            .int2ObjectEntrySet()
            .removeIf(
                entry -> {
                    boolean timedOut = entry.getValue().timedOut(time);
                    if (timedOut) {
                        entry.getValue()
                            .resultFuture()
                            .completeExceptionally(
                                new ReadTimeoutException("RPC method " + entry.getValue().method().key().identifier() + " timed out waiting for response")
                            );
                    }

                    return timedOut;
                }
            );
    }

    @Override
    public void channelActive(final ChannelHandlerContext ctx) throws Exception {
        this.jsonRpcLogger.log(this.clientInfo, "Management connection opened for {}", this.channel.remoteAddress());
        super.channelActive(ctx);
        this.managementServer.onConnected(this);
    }

    @Override
    public void channelInactive(final ChannelHandlerContext ctx) throws Exception {
        this.jsonRpcLogger.log(this.clientInfo, "Management connection closed for {}", this.channel.remoteAddress());
        super.channelInactive(ctx);
        this.managementServer.onDisconnected(this);
    }

    @Override
    public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) throws Exception {
        if (cause.getCause() instanceof JsonParseException) {
            this.channel.writeAndFlush(JsonRPCErrors.PARSE_ERROR.createWithUnknownId(cause.getMessage()));
        } else {
            super.exceptionCaught(ctx, cause);
            this.channel.close().awaitUninterruptibly();
        }
    }

    protected void channelRead0(final ChannelHandlerContext channelHandlerContext, final JsonElement jsonElement) {
        if (jsonElement.isJsonObject()) {
            JsonObject response = this.handleJsonObject(jsonElement.getAsJsonObject());
            if (response != null) {
                this.channel.writeAndFlush(response);
            }
        } else if (jsonElement.isJsonArray()) {
            this.channel.writeAndFlush(this.handleBatchRequest(jsonElement.getAsJsonArray().asList()));
        } else {
            this.channel.writeAndFlush(JsonRPCErrors.INVALID_REQUEST.createWithUnknownId(null));
        }
    }

    private JsonArray handleBatchRequest(final List<JsonElement> batchRequests) {
        JsonArray batchResponses = new JsonArray();
        batchRequests.stream().map(batchEntry -> this.handleJsonObject(batchEntry.getAsJsonObject())).filter(Objects::nonNull).forEach(batchResponses::add);
        return batchResponses;
    }

    public void sendNotification(final Holder.Reference<? extends OutgoingRpcMethod<Void, ?>> method) {
        this.sendRequest(method, null, false);
    }

    public <Params> void sendNotification(final Holder.Reference<? extends OutgoingRpcMethod<Params, ?>> method, final Params params) {
        this.sendRequest(method, params, false);
    }

    public <Result> CompletableFuture<Result> sendRequest(final Holder.Reference<? extends OutgoingRpcMethod<Void, Result>> method) {
        return this.sendRequest(method, null, true);
    }

    public <Params, Result> CompletableFuture<Result> sendRequest(
        final Holder.Reference<? extends OutgoingRpcMethod<Params, Result>> method, final Params params
    ) {
        return this.sendRequest(method, params, true);
    }

    @Contract("_,_,false->null;_,_,true->!null")
    private <Params, Result> @Nullable CompletableFuture<Result> sendRequest(
        final Holder.Reference<? extends OutgoingRpcMethod<Params, ? extends Result>> methodHolder, final @Nullable Params params, final boolean expectReply
    ) {
        OutgoingRpcMethod<Params, ? extends Result> method = (OutgoingRpcMethod<Params, ? extends Result>)methodHolder.value();
        if (this.minecraftApi.notificationManager().server() == null && !method.attributes().allowPreServerInit()) {
            return CompletableFuture.failedFuture(new InvalidRequestJsonRpcException("Method cannot be dispatched pre server initialization: " + method));
        } else {
            List<JsonElement> jsonParams = params != null ? List.of(Objects.requireNonNull(method.encodeParams(params))) : List.of();
            if (expectReply) {
                CompletableFuture<Result> future = new CompletableFuture<>();
                int id = this.transactionId.incrementAndGet();
                long time = Util.getMillis();
                this.pendingRequests.put(id, new PendingRpcRequest<>(methodHolder, future, time + 5000L));
                this.channel.writeAndFlush(JsonRPCUtils.createRequest(id, methodHolder.key().identifier(), jsonParams));
                return future;
            } else {
                this.channel.writeAndFlush(JsonRPCUtils.createRequest(null, methodHolder.key().identifier(), jsonParams));
                return null;
            }
        }
    }

    @VisibleForTesting
    @Nullable JsonObject handleJsonObject(final JsonObject jsonObject) {
        try {
            JsonElement id = JsonRPCUtils.getRequestId(jsonObject);
            String method = JsonRPCUtils.getMethodName(jsonObject);
            JsonElement result = JsonRPCUtils.getResult(jsonObject);
            JsonElement params = JsonRPCUtils.getParams(jsonObject);
            JsonObject error = JsonRPCUtils.getError(jsonObject);
            if (method != null && result == null && error == null) {
                return id != null && !isValidRequestId(id)
                    ? JsonRPCErrors.INVALID_REQUEST.createWithUnknownId("Invalid request id - only String, Number and NULL supported")
                    : this.handleIncomingRequest(id, method, params);
            }

            if (method == null && result != null && error == null && id != null) {
                if (isValidResponseId(id)) {
                    this.handleRequestResponse(id.getAsInt(), result);
                } else {
                    LOGGER.warn("Received respose {} with id {} we did not request", result, id);
                }

                return null;
            } else {
                return method == null && result == null && error != null
                    ? this.handleError(id, error)
                    : JsonRPCErrors.INVALID_REQUEST.createWithoutData(Objects.requireNonNullElse(id, JsonNull.INSTANCE));
            }
        } catch (Exception e) {
            LOGGER.error("Error while handling rpc request", e);
            return JsonRPCErrors.INTERNAL_ERROR.createWithUnknownId("Unknown error handling request - check server logs for stack trace");
        }
    }

    private static boolean isValidRequestId(final JsonElement id) {
        return id.isJsonNull() || GsonHelper.isNumberValue(id) || GsonHelper.isStringValue(id);
    }

    private static boolean isValidResponseId(final JsonElement id) {
        return GsonHelper.isNumberValue(id);
    }

    private @Nullable JsonObject handleIncomingRequest(final @Nullable JsonElement id, final String method, final @Nullable JsonElement params) {
        boolean sendResponse = id != null;

        try {
            JsonElement result = this.dispatchIncomingRequest(method, params);
            return result != null && sendResponse ? JsonRPCUtils.createSuccessResult(id, result) : null;
        } catch (InvalidParameterJsonRpcException e) {
            LOGGER.debug("Invalid parameter invocation {}: {}, {}", method, params, e.getMessage());
            return sendResponse ? JsonRPCErrors.INVALID_PARAMS.create(id, e.getMessage()) : null;
        } catch (EncodeJsonRpcException e) {
            LOGGER.error("Failed to encode json rpc response {}: {}", method, e.getMessage());
            return sendResponse ? JsonRPCErrors.INTERNAL_ERROR.create(id, e.getMessage()) : null;
        } catch (InvalidRequestJsonRpcException e) {
            return sendResponse ? JsonRPCErrors.INVALID_REQUEST.create(id, e.getMessage()) : null;
        } catch (MethodNotFoundJsonRpcException e) {
            return sendResponse ? JsonRPCErrors.METHOD_NOT_FOUND.create(id, e.getMessage()) : null;
        } catch (Exception e) {
            LOGGER.error("Error while dispatching rpc method {}", method, e);
            return sendResponse ? JsonRPCErrors.INTERNAL_ERROR.createWithoutData(id) : null;
        }
    }

    public @Nullable JsonElement dispatchIncomingRequest(final String method, final @Nullable JsonElement params) {
        Identifier identifier = Identifier.tryParse(method);
        if (identifier == null) {
            throw new InvalidRequestJsonRpcException("Failed to parse method value: " + method);
        }

        Optional<IncomingRpcMethod<?, ?>> incomingRpcMethod = BuiltInRegistries.INCOMING_RPC_METHOD.getOptional(identifier);
        if (incomingRpcMethod.isEmpty()) {
            throw new MethodNotFoundJsonRpcException("Method not found: " + method);
        }

        IncomingRpcMethod.Attributes attributes = incomingRpcMethod.get().attributes();
        NotificationManager notificationManager = this.minecraftApi.notificationManager();
        if (notificationManager != null && notificationManager.server() == null && !attributes.allowPreServerInit()) {
            throw new InvalidRequestJsonRpcException("Method cannot be dispatched pre server initialization: " + method);
        }

        if (attributes.runOnMainThread()) {
            try {
                return this.minecraftApi.<JsonElement>submit(() -> incomingRpcMethod.get().apply(this.minecraftApi, params, this.clientInfo)).join();
            } catch (CompletionException e) {
                if (e.getCause() instanceof RuntimeException re) {
                    throw re;
                } else {
                    throw e;
                }
            }
        } else {
            return incomingRpcMethod.get().apply(this.minecraftApi, params, this.clientInfo);
        }
    }

    private void handleRequestResponse(final int id, final JsonElement result) {
        PendingRpcRequest<?> request = this.pendingRequests.remove(id);
        if (request == null) {
            LOGGER.warn("Received unknown response (id: {}): {}", id, result);
        } else {
            request.accept(result);
        }
    }

    private @Nullable JsonObject handleError(final @Nullable JsonElement id, final JsonObject error) {
        if (id != null && isValidResponseId(id)) {
            PendingRpcRequest<?> request = this.pendingRequests.remove(id.getAsInt());
            if (request != null) {
                request.resultFuture().completeExceptionally(new RemoteRpcErrorException(id, error));
            }
        }

        LOGGER.error("Received error (id: {}): {}", id, error);
        return null;
    }
}