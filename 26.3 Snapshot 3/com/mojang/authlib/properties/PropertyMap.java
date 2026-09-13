package com.mojang.authlib.properties;

import com.google.common.collect.ForwardingMultimap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.ImmutableMultimap.Builder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import java.lang.reflect.Type;
import java.util.Map.Entry;

public class PropertyMap extends ForwardingMultimap<String, Property> {
   public static final PropertyMap EMPTY = new PropertyMap(ImmutableMultimap.of());
   private final Multimap<String, Property> properties;

   public PropertyMap(Multimap<String, Property> properties) {
      this.properties = ImmutableMultimap.copyOf(properties);
   }

   protected Multimap<String, Property> delegate() {
      return this.properties;
   }

   public static class Serializer implements JsonSerializer<PropertyMap>, JsonDeserializer<PropertyMap> {
      public PropertyMap deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
         Builder<String, Property> builder = ImmutableMultimap.builder();
         if (json instanceof JsonObject object) {
            for (Entry<String, JsonElement> entry : object.entrySet()) {
               if (entry.getValue() instanceof JsonArray) {
                  for (JsonElement element : (JsonArray)entry.getValue()) {
                     builder.put(entry.getKey(), new Property(entry.getKey(), element.getAsString()));
                  }
               }
            }
         } else if (json instanceof JsonArray) {
            for (JsonElement element : (JsonArray)json) {
               if (element instanceof JsonObject object) {
                  String name = object.getAsJsonPrimitive("name").getAsString();
                  String value = object.getAsJsonPrimitive("value").getAsString();
                  if (object.has("signature")) {
                     builder.put(name, new Property(name, value, object.getAsJsonPrimitive("signature").getAsString()));
                  } else {
                     builder.put(name, new Property(name, value));
                  }
               }
            }
         }

         return new PropertyMap(builder.build());
      }

      public JsonElement serialize(PropertyMap src, Type typeOfSrc, JsonSerializationContext context) {
         JsonArray result = new JsonArray();

         for (Property property : src.values()) {
            JsonObject object = new JsonObject();
            object.addProperty("name", property.name());
            object.addProperty("value", property.value());
            String signature = property.signature();
            if (signature != null) {
               object.addProperty("signature", signature);
            }

            result.add(object);
         }

         return result;
      }
   }
}
