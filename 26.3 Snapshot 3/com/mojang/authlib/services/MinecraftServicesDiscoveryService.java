package com.mojang.authlib.services;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.mojang.authlib.Environment;
import com.mojang.authlib.EnvironmentParser;
import com.mojang.authlib.GameProfileRepository;
import com.mojang.authlib.HttpDiscoveryService;
import com.mojang.authlib.exceptions.MinecraftClientException;
import com.mojang.authlib.minecraft.SessionService;
import com.mojang.authlib.minecraft.UserApiService;
import com.mojang.authlib.minecraft.client.MinecraftClient;
import com.mojang.authlib.services.response.discovery.DiscoveryResponse;
import com.mojang.authlib.services.response.discovery.Endpoint;
import com.mojang.authlib.services.response.discovery.Endpoints;
import com.mojang.authlib.services.response.discovery.Service;
import java.net.Proxy;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MinecraftServicesDiscoveryService extends HttpDiscoveryService {
   private static final Logger LOGGER = LoggerFactory.getLogger(MinecraftServicesDiscoveryService.class);
   public static final ScheduledExecutorService DISCOVERY_EXECUTOR = Executors.newScheduledThreadPool(
      1, new ThreadFactoryBuilder().setNameFormat("Minecraft Services Discovery").setDaemon(true).build()
   );
   private final boolean servicesKeySetEnabled;
   private final Supplier<DiscoveryResponse> discoverySupplier;

   private MinecraftServicesDiscoveryService(Proxy proxy, boolean servicesKeySetEnabled, Supplier<DiscoveryResponse> discoverySupplier) {
      super(proxy);
      this.servicesKeySetEnabled = servicesKeySetEnabled;
      this.discoverySupplier = discoverySupplier;
   }

   public static MinecraftServicesDiscoveryService create(Proxy proxy) {
      return create(proxy, true, determineEnvironment());
   }

   public static MinecraftServicesDiscoveryService create(Proxy proxy, boolean servicesKeySetEnabled) {
      return create(proxy, servicesKeySetEnabled, determineEnvironment());
   }

   public static MinecraftServicesDiscoveryService create(Proxy proxy, boolean servicesKeySetEnabled, Environment environment) {
      LOGGER.info("Environment: {}", environment);
      return new MinecraftServicesDiscoveryService(proxy, servicesKeySetEnabled, createDiscoverySupplier(proxy, environment));
   }

   private static Supplier<DiscoveryResponse> createDiscoverySupplier(Proxy proxy, Environment environment) {
      MinecraftClient client = MinecraftClient.unauthenticated(proxy);
      return RetryableFetch.fetch(
         () -> HttpDiscoveryService.constantURL(environment.discoveryUrl()),
         DISCOVERY_EXECUTOR,
         url -> Optional.ofNullable(client.get(url, DiscoveryResponse.class)),
         DiscoveryResponse.offline(),
         24,
         1
      );
   }

   public static MinecraftServicesDiscoveryService createOffline(Proxy proxy) {
      return new MinecraftServicesDiscoveryService(proxy, false, DiscoveryResponse::offline);
   }

   private static Environment determineEnvironment() {
      return EnvironmentParser.getEnvironmentFromProperties().orElse(MinecraftServicesEnvironment.PROD.getEnvironment());
   }

   @Override
   public SessionService createMinecraftSessionService() {
      return new MinecraftServicesSessionService(this.getServicesKeySet(), this.getProxy(), this);
   }

   @Override
   public GameProfileRepository createProfileRepository() {
      return new MinecraftServicesProfileRepository(this.getProxy(), this);
   }

   public UserApiService createUserApiService(String accessToken) {
      return new MinecraftServicesUserApiService(accessToken, this, this.getProxy());
   }

   public FriendsService createFriendsService(String accessToken) {
      return new MinecraftServicesFriendsService(accessToken, this.getProxy(), this);
   }

   public ServicesKeySet getServicesKeySet() {
      if (!this.servicesKeySetEnabled) {
         return ServicesKeySet.EMPTY;
      }

      MinecraftClient client = MinecraftClient.unauthenticated(this.getProxy());
      return MinecraftServicesKeyInfo.get(() -> HttpDiscoveryService.constantURL(this.getUrl(Service.AUTHENTICATION, "getPublicKeys")), client);
   }

   public String getUrl(Service service, String endpointKey) {
      Endpoint endpoint = this.getEndpoint(service, endpointKey);
      return endpoint.uri();
   }

   public List<String> getValidUris(Service service, String endpointKey) {
      Endpoint endpoint = this.getEndpoint(service, endpointKey);
      List<String> validUris = endpoint.validUris();
      return validUris != null ? validUris : List.of();
   }

   private Endpoint getEndpoint(Service service, String endpointKey) {
      DiscoveryResponse discovery = this.discoverySupplier.get();
      if (discovery.isOffline()) {
         LOGGER.warn("Services are unavailable, unable to fetch uri for {}/{}", service, endpointKey);
         throw new MinecraftClientException(MinecraftClientException.ErrorType.SERVICE_UNAVAILABLE, "Services are unavailable");
      } else {
         Endpoints endpoints = discovery.discovery().mapEndpoints(service);
         Endpoint endpoint = endpoints.endpoints().get(endpointKey);
         if (endpoint == null) {
            LOGGER.warn("Services are available, but unable to find uri for {}/{}", service, endpointKey);
            throw new MinecraftClientException(MinecraftClientException.ErrorType.SERVICE_UNAVAILABLE, "Services are unavailable");
         } else {
            return endpoint;
         }
      }
   }

   public boolean isAllowedTextureDomain(String url) {
      for (String validUri : this.getValidUris(Service.PROFILES, "getTexture")) {
         if (url.startsWith(validUri.replace("{textureId}", ""))) {
            return true;
         }
      }

      return false;
   }
}
