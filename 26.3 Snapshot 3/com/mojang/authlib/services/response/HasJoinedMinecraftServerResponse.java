package com.mojang.authlib.services.response;

import com.google.gson.annotations.SerializedName;
import com.mojang.authlib.properties.PropertyMap;
import java.util.Set;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

public record HasJoinedMinecraftServerResponse(
   @Nullable @SerializedName("id") UUID id, @Nullable @SerializedName("properties") PropertyMap properties, @Nullable Set<ProfileAction> profileActions
) {
   public Set<ProfileAction> profileActions() {
      return this.profileActions != null ? this.profileActions : Set.of();
   }
}
