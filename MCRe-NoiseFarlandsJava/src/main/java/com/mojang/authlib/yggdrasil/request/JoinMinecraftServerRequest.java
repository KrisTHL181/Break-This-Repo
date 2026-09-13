package com.mojang.authlib.yggdrasil.request;

import com.google.gson.annotations.SerializedName;

import java.util.UUID;

public record JoinMinecraftServerRequest(
    @SerializedName("accessToken")
    String accessToken,
    @SerializedName("selectedProfile")
    UUID selectedProfile,
    @SerializedName("serverId")
    String serverId
) {
}
