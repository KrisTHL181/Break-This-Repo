package com.mojang.authlib.minecraft.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.mojang.authlib.exceptions.MinecraftClientException;
import com.mojang.authlib.exceptions.MinecraftClientException.ErrorType;
import com.mojang.authlib.properties.PropertyMap;
import com.mojang.authlib.yggdrasil.response.ProfileSearchResultsResponse;
import com.mojang.util.ByteBufferTypeAdapter;
import com.mojang.util.InstantTypeAdapter;
import com.mojang.util.UUIDTypeAdapter;

import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class ObjectMapper {

    private final Gson gson;

    public ObjectMapper(final Gson gson) {
        this.gson = Objects.requireNonNull(gson);
    }

    public <T> T readValue(final String value, final Class<T> type) {
        try {
            return gson.fromJson(value, type);
        } catch (final JsonParseException e) {
            throw new MinecraftClientException(ErrorType.JSON_ERROR, "Failed to read value " + value, e);
        }
    }

    public String writeValueAsString(final Object entity) {
        try {
            return gson.toJson(entity);
        } catch (final RuntimeException e) {
            throw new MinecraftClientException(ErrorType.JSON_ERROR, "Failed to write value", e);
        }
    }

    public static ObjectMapper create() {
        return new ObjectMapper(new GsonBuilder()
                .registerTypeAdapter(UUID.class, new UUIDTypeAdapter())
                .registerTypeAdapter(Instant.class, new InstantTypeAdapter())
                .registerTypeHierarchyAdapter(ByteBuffer.class, new ByteBufferTypeAdapter().nullSafe())
                .registerTypeAdapter(PropertyMap.class, new PropertyMap.Serializer())
                .registerTypeAdapter(ProfileSearchResultsResponse.class, new ProfileSearchResultsResponse.Serializer())
                .create());
    }
}
