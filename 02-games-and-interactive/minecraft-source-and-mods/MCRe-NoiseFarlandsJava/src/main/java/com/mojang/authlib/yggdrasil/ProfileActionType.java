package com.mojang.authlib.yggdrasil;

import com.google.gson.annotations.SerializedName;

public enum ProfileActionType {
    @SerializedName("FORCED_NAME_CHANGE")
    FORCED_NAME_CHANGE,
    @SerializedName("USING_BANNED_SKIN")
    USING_BANNED_SKIN,
}
