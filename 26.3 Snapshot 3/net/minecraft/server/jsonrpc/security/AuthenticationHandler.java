package net.minecraft.server.jsonrpc.security;

import com.google.common.collect.Sets;
import com.mojang.logging.LogUtils;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.AttributeKey;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@Sharable
public class AuthenticationHandler extends ChannelDuplexHandler {
   private final Logger LOGGER = LogUtils.getLogger();
   private static final AttributeKey<Boolean> AUTHENTICATED_KEY = AttributeKey.valueOf("authenticated");
   private static final AttributeKey<Boolean> ATTR_WEBSOCKET_ALLOWED = AttributeKey.valueOf("websocket_auth_allowed");
   private static final String SUBPROTOCOL_VALUE = "minecraft-v1";
   private static final String SUBPROTOCOL_HEADER_PREFIX = "minecraft-v1,";
   public static final String BEARER_PREFIX = "Bearer ";
   private final SecurityConfig securityConfig;
   private final Set<String> allowedOrigins;

   public AuthenticationHandler(final SecurityConfig securityConfig, final String allowedOrigins) {
      this.securityConfig = securityConfig;
      this.allowedOrigins = Sets.newHashSet(allowedOrigins.split(","));
   }

   public void channelRead(final ChannelHandlerContext context, final Object msg) throws Exception {
      String clientIp = this.getClientIp(context);
      if (msg instanceof HttpRequest request) {
         AuthenticationHandler.SecurityCheckResult result = this.performSecurityChecks(request);
         if (!result.isAllowed()) {
            this.LOGGER.debug("Authentication rejected for connection with ip {}: {}", clientIp, result.getReason());
            context.channel().attr(AUTHENTICATED_KEY).set(false);
            this.sendUnauthorizedResponse(context, result.getReason());
            return;
         }

         context.channel().attr(AUTHENTICATED_KEY).set(true);
         if (result.isTokenSentInSecWebsocketProtocol()) {
            context.channel().attr(ATTR_WEBSOCKET_ALLOWED).set(Boolean.TRUE);
         }
      }

      Boolean isAuthenticated = (Boolean)context.channel().attr(AUTHENTICATED_KEY).get();
      if (Boolean.TRUE.equals(isAuthenticated)) {
         super.channelRead(context, msg);
      } else {
         this.LOGGER.debug("Dropping unauthenticated connection with ip {}", clientIp);
         context.close();
      }
   }

   public void write(final ChannelHandlerContext ctx, final Object msg, final ChannelPromise promise) throws Exception {
      if (msg instanceof HttpResponse response
         && response.status().code() == HttpResponseStatus.SWITCHING_PROTOCOLS.code()
         && ctx.channel().attr(ATTR_WEBSOCKET_ALLOWED).get() != null
         && ((Boolean)ctx.channel().attr(ATTR_WEBSOCKET_ALLOWED).get()).equals(Boolean.TRUE)) {
         response.headers().set(HttpHeaderNames.SEC_WEBSOCKET_PROTOCOL, "minecraft-v1");
      }

      super.write(ctx, msg, promise);
   }

   private AuthenticationHandler.SecurityCheckResult performSecurityChecks(final HttpRequest request) {
      String tokenInAuthorizationHeader = this.parseTokenInAuthorizationHeader(request);
      if (tokenInAuthorizationHeader != null) {
         return this.isValidApiKey(tokenInAuthorizationHeader)
            ? AuthenticationHandler.SecurityCheckResult.allowed()
            : AuthenticationHandler.SecurityCheckResult.denied("Invalid API key");
      }

      String tokenInSecWebsocketProtocolHeader = this.parseTokenInSecWebsocketProtocolHeader(request);
      if (tokenInSecWebsocketProtocolHeader != null) {
         if (!this.isAllowedOriginHeader(request)) {
            return AuthenticationHandler.SecurityCheckResult.denied("Origin Not Allowed");
         } else {
            return this.isValidApiKey(tokenInSecWebsocketProtocolHeader)
               ? AuthenticationHandler.SecurityCheckResult.allowed(true)
               : AuthenticationHandler.SecurityCheckResult.denied("Invalid API key");
         }
      } else {
         return AuthenticationHandler.SecurityCheckResult.denied("Missing API key");
      }
   }

   private boolean isAllowedOriginHeader(final HttpRequest request) {
      String originHeader = request.headers().get(HttpHeaderNames.ORIGIN);
      return originHeader != null && !originHeader.isEmpty() ? this.allowedOrigins.contains(originHeader) : false;
   }

   private @Nullable String parseTokenInAuthorizationHeader(final HttpRequest request) {
      String authHeader = request.headers().get(HttpHeaderNames.AUTHORIZATION);
      return authHeader != null && authHeader.startsWith("Bearer ") ? authHeader.substring("Bearer ".length()).trim() : null;
   }

   private @Nullable String parseTokenInSecWebsocketProtocolHeader(final HttpRequest request) {
      String authHeader = request.headers().get(HttpHeaderNames.SEC_WEBSOCKET_PROTOCOL);
      return authHeader != null && authHeader.startsWith("minecraft-v1,") ? authHeader.substring("minecraft-v1,".length()).trim() : null;
   }

   public boolean isValidApiKey(final String suppliedKey) {
      if (suppliedKey.isEmpty()) {
         return false;
      }

      byte[] suppliedKeyBytes = suppliedKey.getBytes(StandardCharsets.UTF_8);
      byte[] configuredKeyBytes = this.securityConfig.secretKey().getBytes(StandardCharsets.UTF_8);
      return MessageDigest.isEqual(suppliedKeyBytes, configuredKeyBytes);
   }

   private String getClientIp(final ChannelHandlerContext context) {
      InetSocketAddress remoteAddress = (InetSocketAddress)context.channel().remoteAddress();
      return remoteAddress.getAddress().getHostAddress();
   }

   private void sendUnauthorizedResponse(final ChannelHandlerContext context, final String reason) {
      String responseBody = "{\"error\":\"Unauthorized\",\"message\":\"" + reason + "\"}";
      byte[] content = responseBody.getBytes(StandardCharsets.UTF_8);
      DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.UNAUTHORIZED, Unpooled.wrappedBuffer(content));
      response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json");
      response.headers().set(HttpHeaderNames.CONTENT_LENGTH, content.length);
      response.headers().set(HttpHeaderNames.CONNECTION, "close");
      context.writeAndFlush(response).addListener(future -> context.close());
   }

   private static class SecurityCheckResult {
      private final boolean allowed;
      private final String reason;
      private final boolean tokenSentInSecWebsocketProtocol;

      private SecurityCheckResult(final boolean allowed, final String reason, final boolean tokenSentInSecWebsocketProtocol) {
         this.allowed = allowed;
         this.reason = reason;
         this.tokenSentInSecWebsocketProtocol = tokenSentInSecWebsocketProtocol;
      }

      public static AuthenticationHandler.SecurityCheckResult allowed() {
         return new AuthenticationHandler.SecurityCheckResult(true, null, false);
      }

      public static AuthenticationHandler.SecurityCheckResult allowed(final boolean tokenSentInSecWebsocketProtocol) {
         return new AuthenticationHandler.SecurityCheckResult(true, null, tokenSentInSecWebsocketProtocol);
      }

      public static AuthenticationHandler.SecurityCheckResult denied(final String reason) {
         return new AuthenticationHandler.SecurityCheckResult(false, reason, false);
      }

      public boolean isAllowed() {
         return this.allowed;
      }

      public String getReason() {
         return this.reason;
      }

      public boolean isTokenSentInSecWebsocketProtocol() {
         return this.tokenSentInSecWebsocketProtocol;
      }
   }
}
