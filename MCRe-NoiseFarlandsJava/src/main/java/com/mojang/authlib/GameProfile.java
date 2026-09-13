package com.mojang.authlib;

import com.mojang.authlib.properties.PropertyMap;

import java.util.Objects;
import java.util.UUID;

public record GameProfile(
    UUID id,
    String name,
    PropertyMap properties
) {

    /**
     * Constructs a new Game Profile with the specified ID and name.
     *
     * @param id Unique ID of the profile
     * @param name Display name of the profile
     * @param properties Properties of the profile
     * @throws java.lang.NullPointerException if any parameter is {@code null}
     */
    public GameProfile {
        Objects.requireNonNull(id, "Profile ID must not be null");
        Objects.requireNonNull(name, "Profile name must not be null");
        Objects.requireNonNull(properties, "Profile properties must not be null");
    }

    public GameProfile(final UUID id, final String name) {
        this(id, name, PropertyMap.EMPTY);
    }
}
