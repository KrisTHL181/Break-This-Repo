package com.mojang.authlib.yggdrasil.response;

import com.google.gson.annotations.SerializedName;
import com.mojang.authlib.properties.PropertyMap;

import javax.annotation.Nullable;
import java.util.Set;
import java.util.UUID;

public record HasJoinedMinecraftServerResponse(
    @SerializedName("id")
    @Nullable UUID id,
    @SerializedName("properties")
    @Nullable PropertyMap properties,
    @SerializedName("profileActions")
    @Nullable Set<ProfileAction> profileActions
) {
    @Override
    public Set<ProfileAction> profileActions() {
        return profileActions != null ? profileActions : Set.of();
    }
}
