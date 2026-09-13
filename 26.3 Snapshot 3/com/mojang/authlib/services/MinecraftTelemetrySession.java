package com.mojang.authlib.services;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.gson.JsonObject;
import com.mojang.authlib.HttpDiscoveryService;
import com.mojang.authlib.exceptions.MinecraftClientException;
import com.mojang.authlib.minecraft.TelemetryEvent;
import com.mojang.authlib.minecraft.TelemetrySession;
import com.mojang.authlib.minecraft.client.MinecraftClient;
import com.mojang.authlib.services.request.TelemetryEventsRequest;
import com.mojang.authlib.services.response.discovery.Service;
import java.time.Instant;
import java.util.concurrent.Executor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MinecraftTelemetrySession implements TelemetrySession {
   private static final Logger LOGGER = LoggerFactory.getLogger(MinecraftTelemetrySession.class);
   private static final String SOURCE = "minecraft.java";
   private final MinecraftClient minecraftClient;
   private final MinecraftServicesDiscoveryService discoveryService;
   private final Executor ioExecutor;

   @VisibleForTesting
   MinecraftTelemetrySession(MinecraftClient minecraftClient, MinecraftServicesDiscoveryService discoveryService, Executor ioExecutor) {
      this.minecraftClient = minecraftClient;
      this.discoveryService = discoveryService;
      this.ioExecutor = ioExecutor;
   }

   @Override
   public boolean isEnabled() {
      return true;
   }

   @Override
   public TelemetryEvent createNewEvent(String type) {
      return new MinecraftTelemetryEvent(this, type);
   }

   void sendEvent(String type, JsonObject data) {
      Instant sendTime = Instant.now();
      TelemetryEventsRequest.Event request = new TelemetryEventsRequest.Event("minecraft.java", type, sendTime, data);
      this.ioExecutor.execute(() -> {
         try {
            TelemetryEventsRequest envelope = new TelemetryEventsRequest(ImmutableList.of(request));
            this.minecraftClient.post(HttpDiscoveryService.constantURL(this.discoveryService.getUrl(Service.TELEMETRY, "sendEvents")), envelope, Void.class);
         } catch (MinecraftClientException e) {
            LOGGER.debug("Failed to send telemetry event {}", request.name(), e);
         }
      });
   }
}
