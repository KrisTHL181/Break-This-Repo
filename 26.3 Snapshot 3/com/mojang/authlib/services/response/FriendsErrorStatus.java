package com.mojang.authlib.services.response;

import com.google.gson.annotations.SerializedName;

public enum FriendsErrorStatus {
   @SerializedName("UNKNOWN_PROFILE")
   UNKNOWN_PROFILE,
   @SerializedName("CANNOT_ADD_SELF")
   CANNOT_ADD_SELF,
   @SerializedName("DUPLICATED_PROFILES")
   DUPLICATED_PROFILES;
}
