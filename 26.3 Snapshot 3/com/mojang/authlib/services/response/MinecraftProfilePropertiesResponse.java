package com.mojang.authlib.services.response;

import com.google.gson.annotations.SerializedName;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.PropertyMap;
import java.util.Set;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

public record MinecraftProfilePropertiesResponse(
   @SerializedName("id") UUID id,
   @SerializedName("name") String name,
   @SerializedName("properties") PropertyMap properties,
   @Nullable Set<ProfileAction> profileActions
) {
   public GameProfile profile() {
      return new GameProfile(this.id, this.name, this.properties);
   }

   public Set<ProfileAction> profileActions() {
      return this.profileActions != null ? this.profileActions : Set.of();
   }
}
