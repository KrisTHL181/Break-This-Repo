package com.mojang.authlib.yggdrasil.response;

import com.google.gson.annotations.SerializedName;
import com.mojang.authlib.yggdrasil.ProfileActionType;

public record ProfileAction(
    @SerializedName("updateType")
    ProfileActionType type
) {
}
