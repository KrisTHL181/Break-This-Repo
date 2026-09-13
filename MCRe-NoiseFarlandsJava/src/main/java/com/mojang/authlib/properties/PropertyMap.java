package com.mojang.authlib.properties;

import com.google.common.collect.ForwardingMultimap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;
import java.util.Map;

public class PropertyMap extends ForwardingMultimap<String, Property> {
    public static final PropertyMap EMPTY = new PropertyMap(ImmutableMultimap.of());

    private final Multimap<String, Property> properties;

    public PropertyMap(final Multimap<String, Property> properties) {
        this.properties = ImmutableMultimap.copyOf(properties);
    }

    @Override
    protected Multimap<String, Property> delegate() {
        return properties;
    }

    public static class Serializer implements JsonSerializer<PropertyMap>, JsonDeserializer<PropertyMap> {
        @Override
        public PropertyMap deserialize(final JsonElement json, final Type typeOfT, final JsonDeserializationContext context) throws JsonParseException {
            final ImmutableMultimap.Builder<String, Property> builder = ImmutableMultimap.builder();

            if (json instanceof final JsonObject object) {
                for (final Map.Entry<String, JsonElement> entry : object.entrySet()) {
                    if (entry.getValue() instanceof JsonArray) {
                        for (final JsonElement element : ((JsonArray) entry.getValue())) {
                            builder.put(entry.getKey(), new Property(entry.getKey(), element.getAsString()));
                        }
                    }
                }
            } else if (json instanceof final JsonArray array) {
                for (final JsonElement element : array) {
                    if (element instanceof final JsonObject object) {
                        final String name = object.getAsJsonPrimitive("name").getAsString();
                        final String value = object.getAsJsonPrimitive("value").getAsString();

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

        @Override
        public JsonElement serialize(final PropertyMap src, final Type typeOfSrc, final JsonSerializationContext context) {
            final JsonArray result = new JsonArray();

            for (final Property property : src.values()) {
                final JsonObject object = new JsonObject();

                object.addProperty("name", property.name());
                object.addProperty("value", property.value());

                final String signature = property.signature();
                if (signature != null) {
                    object.addProperty("signature", signature);
                }

                result.add(object);
            }

            return result;
        }
    }
}
