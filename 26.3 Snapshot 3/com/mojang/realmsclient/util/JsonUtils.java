package com.mojang.realmsclient.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.util.UndashedUuid;
import java.time.Instant;
import java.util.UUID;
import java.util.function.Function;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.Nullable;

public class JsonUtils {
   public static <T> T getRequired(final String key, final JsonObject node, final Function<JsonObject, T> parser) {
      JsonElement property = node.get(key);
      if (property == null || property.isJsonNull()) {
         throw new IllegalStateException("Missing required property: " + key);
      } else if (!property.isJsonObject()) {
         throw new IllegalStateException("Required property " + key + " was not a JsonObject as espected");
      } else {
         return parser.apply(property.getAsJsonObject());
      }
   }

   public static <T> @Nullable T getOptional(final String key, final JsonObject node, final Function<JsonObject, T> parser) {
      JsonElement property = node.get(key);
      if (property == null || property.isJsonNull()) {
         return null;
      } else if (!property.isJsonObject()) {
         throw new IllegalStateException("Required property " + key + " was not a JsonObject as espected");
      } else {
         return parser.apply(property.getAsJsonObject());
      }
   }

   public static String getRequiredString(final String key, final JsonObject node) {
      String result = getStringOr(key, node, null);
      if (result == null) {
         throw new IllegalStateException("Missing required property: " + key);
      } else {
         return result;
      }
   }

   @Contract("_,_,!null->!null;_,_,null->_")
   public static @Nullable String getStringOr(final String key, final JsonObject node, final @Nullable String defaultValue) {
      JsonElement element = node.get(key);
      if (element != null) {
         return element.isJsonNull() ? defaultValue : element.getAsString();
      } else {
         return defaultValue;
      }
   }

   @Contract("_,_,!null->!null;_,_,null->_")
   public static @Nullable UUID getUuidOr(final String key, final JsonObject node, final @Nullable UUID defaultValue) {
      String uuidAsString = getStringOr(key, node, null);
      return uuidAsString == null ? defaultValue : UndashedUuid.fromStringLenient(uuidAsString);
   }

   public static int getIntOr(final String key, final JsonObject node, final int defaultValue) {
      JsonElement element = node.get(key);
      if (element != null) {
         return element.isJsonNull() ? defaultValue : element.getAsInt();
      } else {
         return defaultValue;
      }
   }

   public static long getLongOr(final String key, final JsonObject node, final long defaultValue) {
      JsonElement element = node.get(key);
      if (element != null) {
         return element.isJsonNull() ? defaultValue : element.getAsLong();
      } else {
         return defaultValue;
      }
   }

   public static boolean getBooleanOr(final String key, final JsonObject node, final boolean defaultValue) {
      JsonElement element = node.get(key);
      if (element != null) {
         return element.isJsonNull() ? defaultValue : element.getAsBoolean();
      } else {
         return defaultValue;
      }
   }

   public static Instant getDateOr(final String key, final JsonObject node) {
      JsonElement element = node.get(key);
      return element != null ? Instant.ofEpochMilli(Long.parseLong(element.getAsString())) : Instant.EPOCH;
   }
}
