package com.mojang.authlib.yggdrasil.response;

import com.google.gson.annotations.SerializedName;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;

import java.util.Map;
import java.util.UUID;

public record MinecraftTexturesPayload(
    @SerializedName("timestamp")
    long timestamp,
    @SerializedName("profileId")
    UUID profileId,
    @SerializedName("profileName")
    String profileName,
    @SerializedName("isPublic")
    boolean isPublic,
    @SerializedName("textures")
    Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> textures
) {
}
