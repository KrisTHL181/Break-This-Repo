package com.mojang.authlib.services.response;

import com.google.gson.annotations.SerializedName;

public enum ChatToggleValue {
   @SerializedName("DISABLED")
   DISABLED,
   @SerializedName("FRIENDS_ONLY")
   FRIENDS_ONLY,
   @SerializedName("ENABLED")
   ENABLED;

   public boolean isEnabled() {
      return this == ENABLED;
   }

   public boolean isFriendsOnly() {
      return this == FRIENDS_ONLY;
   }

   public boolean isDisabled() {
      return this == DISABLED;
   }
}
