package com.mojang.authlib.services.request;

import com.google.gson.annotations.SerializedName;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

public record FriendActionRequest(
   @Nullable @SerializedName("name") String name, @Nullable @SerializedName("profileId") UUID profileId, @SerializedName("updateType") UpdateType updateType
) {
   public static FriendActionRequest byId(UUID id, UpdateType action) {
      return new FriendActionRequest(null, id, action);
   }

   public static FriendActionRequest byName(String name, UpdateType action) {
      return new FriendActionRequest(name, null, action);
   }
}
