package com.mojang.authlib.services.response;

import com.google.gson.annotations.SerializedName;
import com.mojang.authlib.services.ProfileActionType;

public record ProfileAction(@SerializedName("updateType") ProfileActionType type) {
}
