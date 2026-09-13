package com.mojang.authlib.minecraft.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.mojang.authlib.exceptions.MinecraftClientException;
import com.mojang.authlib.properties.PropertyMap;
import com.mojang.authlib.services.response.ProfileSearchResultsResponse;
import com.mojang.util.ByteBufferTypeAdapter;
import com.mojang.util.InstantTypeAdapter;
import com.mojang.util.UUIDTypeAdapter;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class ObjectMapper {
   private final Gson gson;

   public ObjectMapper(Gson gson) {
      this.gson = Objects.requireNonNull(gson);
   }

   public <T> T readValue(String value, Class<T> type) {
      try {
         return (T)this.gson.fromJson(value, type);
      } catch (JsonParseException e) {
         throw new MinecraftClientException(MinecraftClientException.ErrorType.JSON_ERROR, "Failed to read value " + value, e);
      }
   }

   public String writeValueAsString(Object entity) {
      try {
         return this.gson.toJson(entity);
      } catch (RuntimeException e) {
         throw new MinecraftClientException(MinecraftClientException.ErrorType.JSON_ERROR, "Failed to write value", e);
      }
   }

   public static ObjectMapper create() {
      return new ObjectMapper(
         new GsonBuilder()
            .registerTypeAdapter(UUID.class, new UUIDTypeAdapter())
            .registerTypeAdapter(Instant.class, new InstantTypeAdapter())
            .registerTypeHierarchyAdapter(ByteBuffer.class, new ByteBufferTypeAdapter().nullSafe())
            .registerTypeAdapter(PropertyMap.class, new PropertyMap.Serializer())
            .registerTypeAdapter(ProfileSearchResultsResponse.class, new ProfileSearchResultsResponse.Serializer())
            .create()
      );
   }
}
