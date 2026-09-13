package com.mojang.authlib;

import com.mojang.authlib.properties.PropertyMap;
import java.util.Objects;
import java.util.UUID;

public record GameProfile(UUID id, String name, PropertyMap properties) {
   public GameProfile {
      Objects.requireNonNull(id, "Profile ID must not be null");
      Objects.requireNonNull(name, "Profile name must not be null");
      Objects.requireNonNull(properties, "Profile properties must not be null");
   }

   public GameProfile(UUID id, String name) {
      this(id, name, PropertyMap.EMPTY);
   }
}
