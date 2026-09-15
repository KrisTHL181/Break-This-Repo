package com.mojang.authlib.yggdrasil.response;

import com.google.gson.annotations.SerializedName;

import java.util.Set;
import java.util.UUID;

/*
{
  "blockedProfiles": [
    "3fa85f64-5717-4562-b3fc-2c963f66afa6"
  ]
}
*/
public record BlockListResponse(
    @SerializedName("blockedProfiles")
    Set<UUID> blockedProfiles
) {
}
