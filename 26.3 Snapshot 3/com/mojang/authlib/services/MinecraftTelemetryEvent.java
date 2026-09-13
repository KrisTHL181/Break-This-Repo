package com.mojang.authlib.services;

import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.mojang.authlib.minecraft.TelemetryEvent;
import org.jspecify.annotations.Nullable;

public class MinecraftTelemetryEvent implements TelemetryEvent {
   private final MinecraftTelemetrySession service;
   private final String type;
   @Nullable
   private JsonObject data = new JsonObject();

   MinecraftTelemetryEvent(MinecraftTelemetrySession service, String type) {
      this.service = service;
      this.type = type;
   }

   private JsonObject data() {
      if (this.data == null) {
         throw new IllegalStateException("Event already sent");
      } else {
         return this.data;
      }
   }

   @Override
   public void addProperty(String id, String value) {
      this.data().addProperty(id, value);
   }

   @Override
   public void addProperty(String id, int value) {
      this.data().addProperty(id, value);
   }

   @Override
   public void addProperty(String id, long value) {
      this.data().addProperty(id, value);
   }

   @Override
   public void addProperty(String id, boolean value) {
      this.data().addProperty(id, value);
   }

   @Override
   public void addNullProperty(String id) {
      this.data().add(id, JsonNull.INSTANCE);
   }

   @Override
   public void send() {
      if (this.data != null) {
         this.service.sendEvent(this.type, this.data);
      }

      this.data = null;
   }
}
