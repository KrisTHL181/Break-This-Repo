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

   public AuthenticationHandler(SecurityConfig p_426248_, String p_453911_) {
      this.securityConfig = p_426248_;
      this.allowedOrigins = Sets.newHashSet(p_453911_.split(","));
   }

   public void channelRead(ChannelHandlerContext p_430373_, Object p_423931_) throws Exception {
      String s = this.getClientIp(p_430373_);
      if (p_423931_ instanceof HttpRequest httprequest) {
         AuthenticationHandler.SecurityCheckResult authenticationhandler$securitycheckresult = this.performSecurityChecks(httprequest);
         if (!authenticationhandler$securitycheckresult.isAllowed()) {
            this.LOGGER.debug("Authentication rejected for connection with ip {}: {}", s, authenticationhandler$securitycheckresult.getReason());
            p_430373_.channel().attr(AUTHENTICATED_KEY).set(false);
            this.sendUnauthorizedResponse(p_430373_, authenticationhandler$securitycheckresult.getReason());
            return;
         }

         p_430373_.channel().attr(AUTHENTICATED_KEY).set(true);
         if (authenticationhandler$securitycheckresult.isTokenSentInSecWebsocketProtocol()) {
            p_430373_.channel().attr(ATTR_WEBSOCKET_ALLOWED).set(Boolean.TRUE);
         }
      }

      Boolean obool = (Boolean)p_430373_.channel().attr(AUTHENTICATED_KEY).get();
      if (Boolean.TRUE.equals(obool)) {
         super.channelRead(p_430373_, p_423931_);
      } else {
         this.LOGGER.debug("Dropping unauthenticated connection with ip {}", s);
         p_430373_.close();
      }
   }

   public void write(ChannelHandlerContext p_458553_, Object p_456167_, ChannelPromise p_460526_) throws Exception {
      if (p_456167_ instanceof HttpResponse httpresponse
         && httpresponse.status().code() == HttpResponseStatus.SWITCHING_PROTOCOLS.code()
         && p_458553_.channel().attr(ATTR_WEBSOCKET_ALLOWED).get() != null
         && ((Boolean)p_458553_.channel().attr(ATTR_WEBSOCKET_ALLOWED).get()).equals(Boolean.TRUE)) {
         httpresponse.headers().set(HttpHeaderNames.SEC_WEBSOCKET_PROTOCOL, "minecraft-v1");
      }

      super.write(p_458553_, p_456167_, p_460526_);
   }

   private AuthenticationHandler.SecurityCheckResult performSecurityChecks(HttpRequest p_427664_) {
      String s = this.parseTokenInAuthorizationHeader(p_427664_);
      if (s != null) {
         return this.isValidApiKey(s)
            ? AuthenticationHandler.SecurityCheckResult.allowed()
            : AuthenticationHandler.SecurityCheckResult.denied("Invalid API key");
      }

      String s1 = this.parseTokenInSecWebsocketProtocolHeader(p_427664_);
      if (s1 != null) {
         if (!this.isAllowedOriginHeader(p_427664_)) {
            return AuthenticationHandler.SecurityCheckResult.denied("Origin Not Allowed");
         } else {
            return this.isValidApiKey(s1)
               ? AuthenticationHandler.SecurityCheckResult.allowed(true)
               : AuthenticationHandler.SecurityCheckResult.denied("Invalid API key");
         }
      } else {
         return AuthenticationHandler.SecurityCheckResult.denied("Missing API key");
      }
   }

   private boolean isAllowedOriginHeader(HttpRequest p_457771_) {
      String s = p_457771_.headers().get(HttpHeaderNames.ORIGIN);
      return s != null && !s.isEmpty() ? this.allowedOrigins.contains(s) : false;
   }

   private @Nullable String parseTokenInAuthorizationHeader(HttpRequest p_455431_) {
      String s = p_455431_.headers().get(HttpHeaderNames.AUTHORIZATION);
      return s != null && s.startsWith("Bearer ") ? s.substring("Bearer ".length()).trim() : null;
   }

   private @Nullable String parseTokenInSecWebsocketProtocolHeader(HttpRequest p_459220_) {
      String s = p_459220_.headers().get(HttpHeaderNames.SEC_WEBSOCKET_PROTOCOL);
      return s != null && s.startsWith("minecraft-v1,") ? s.substring("minecraft-v1,".length()).trim() : null;
   }

   public boolean isValidApiKey(String p_425797_) {
      if (p_425797_.isEmpty()) {
         return false;
      }

      byte[] abyte = p_425797_.getBytes(StandardCharsets.UTF_8);
      byte[] abyte1 = this.securityConfig.secretKey().getBytes(StandardCharsets.UTF_8);
      return MessageDigest.isEqual(abyte, abyte1);
   }

   private String getClientIp(ChannelHandlerContext p_428847_) {
      InetSocketAddress inetsocketaddress = (InetSocketAddress)p_428847_.channel().remoteAddress();
      return inetsocketaddress.getAddress().getHostAddress();
   }

   private void sendUnauthorizedResponse(ChannelHandlerContext p_428321_, String p_427324_) {
      String s = "{\"error\":\"Unauthorized\",\"message\":\"" + p_427324_ + "\"}";
      byte[] abyte = s.getBytes(StandardCharsets.UTF_8);
      DefaultFullHttpResponse defaultfullhttpresponse = new DefaultFullHttpResponse(
         HttpVersion.HTTP_1_1, HttpResponseStatus.UNAUTHORIZED, Unpooled.wrappedBuffer(abyte)
      );
      defaultfullhttpresponse.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json");
      defaultfullhttpresponse.headers().set(HttpHeaderNames.CONTENT_LENGTH, abyte.length);
      defaultfullhttpresponse.headers().set(HttpHeaderNames.CONNECTION, "close");
      p_428321_.writeAndFlush(defaultfullhttpresponse).addListener(p_431580_ -> p_428321_.close());
   }

   static class SecurityCheckResult {
      private final boolean allowed;
      private final String reason;
      private final boolean tokenSentInSecWebsocketProtocol;

      private SecurityCheckResult(boolean p_425304_, String p_424364_, boolean p_456964_) {
         this.allowed = p_425304_;
         this.reason = p_424364_;
         this.tokenSentInSecWebsocketProtocol = p_456964_;
      }

      public static AuthenticationHandler.SecurityCheckResult allowed() {
         return new AuthenticationHandler.SecurityCheckResult(true, null, false);
      }

      public static AuthenticationHandler.SecurityCheckResult allowed(boolean p_459688_) {
         return new AuthenticationHandler.SecurityCheckResult(true, null, p_459688_);
      }

      public static AuthenticationHandler.SecurityCheckResult denied(String p_424213_) {
         return new AuthenticationHandler.SecurityCheckResult(false, p_424213_, false);
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
