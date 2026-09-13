package com.mojang.realmsclient.dto;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import org.jspecify.annotations.Nullable;

public class GuardedSerializer {
   private static final ExclusionStrategy STRATEGY = new ExclusionStrategy() {
      public boolean shouldSkipClass(final Class<?> clazz) {
         return false;
      }

      public boolean shouldSkipField(final FieldAttributes field) {
         return field.getAnnotation(Exclude.class) != null;
      }
   };
   private final Gson gson = new GsonBuilder().addSerializationExclusionStrategy(STRATEGY).addDeserializationExclusionStrategy(STRATEGY).create();

   public String toJson(final ReflectionBasedSerialization object) {
      return this.gson.toJson(object);
   }

   public String toJson(final JsonElement jsonElement) {
      return this.gson.toJson(jsonElement);
   }

   public <T extends ReflectionBasedSerialization> @Nullable T fromJson(final String contents, final Class<T> cls) {
      return (T)this.gson.fromJson(contents, cls);
   }
}
