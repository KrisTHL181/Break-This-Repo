package net.minecraft.server.jsonrpc;

import com.google.common.net.HostAndPort;
import com.mojang.logging.LogUtils;
import io.netty.handler.ssl.SslContext;
import net.minecraft.server.dedicated.DedicatedServerProperties;
import net.minecraft.server.dedicated.DedicatedServerSettings;
import net.minecraft.server.jsonrpc.internalapi.MinecraftApi;
import net.minecraft.server.jsonrpc.security.AuthenticationHandler;
import net.minecraft.server.jsonrpc.security.JsonRpcSslContextProvider;
import net.minecraft.server.jsonrpc.security.SecurityConfig;
import net.minecraft.server.notifications.NotificationManager;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class JsonRpc {
   private static final Logger LOGGER = LogUtils.getLogger();

   public static @Nullable ManagementServer create(final DedicatedServerSettings settings, final NotificationManager notificationManager) {
      DedicatedServerProperties properties = settings.getProperties();
      if (properties.managementServerEnabled) {
         String managementServerSecret = properties.managementServerSecret;
         if (!SecurityConfig.isValid(managementServerSecret)) {
            throw new IllegalStateException("Invalid management server secret, must be 40 alphanumeric characters");
         }

         String managementHost = properties.managementServerHost;
         int managementPort = properties.managementServerPort;
         HostAndPort hostAndPort = HostAndPort.fromParts(managementHost, managementPort);
         SecurityConfig securityConfig = new SecurityConfig(managementServerSecret);
         String allowedOrigins = properties.managementServerAllowedOrigins;
         AuthenticationHandler authenticationHandler = new AuthenticationHandler(securityConfig, allowedOrigins);
         LOGGER.info("Starting json RPC server on {}", hostAndPort);
         ManagementServer jsonRpcServer = new ManagementServer(hostAndPort, authenticationHandler);
         MinecraftApi minecraftApi = MinecraftApi.of(notificationManager);
         minecraftApi.notificationManager().registerService(new JsonRpcNotificationService(minecraftApi, jsonRpcServer));
         if (properties.managementServerTlsEnabled) {
            SslContext sslContext = createSslContext(properties);
            jsonRpcServer.startWithTls(minecraftApi, sslContext);
         } else {
            jsonRpcServer.startWithoutTls(minecraftApi);
         }

         jsonRpcServer.scheduleHeartbeat(notificationManager, properties.statusHeartbeatInterval.get().intValue());
         return jsonRpcServer;
      } else {
         return null;
      }
   }

   private static SslContext createSslContext(final DedicatedServerProperties properties) {
      try {
         return JsonRpcSslContextProvider.createFrom(properties.managementServerTlsKeystore, properties.managementServerTlsKeystorePassword);
      } catch (Exception e) {
         JsonRpcSslContextProvider.printInstructions();
         throw new IllegalStateException("Failed to configure TLS for the server management protocol", e);
      }
   }
}
