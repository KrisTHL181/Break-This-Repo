package com.mojang.realmsclient.dto;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class GuardedSerializer {
    private static final ExclusionStrategy STRATEGY = new ExclusionStrategy() {
        @Override
        public boolean shouldSkipClass(final Class<?> clazz) {
            return false;
        }

        @Override
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
        return this.gson.fromJson(contents, cls);
    }
}