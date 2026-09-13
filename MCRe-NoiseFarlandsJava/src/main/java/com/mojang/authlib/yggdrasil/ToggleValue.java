package com.mojang.authlib.yggdrasil;

import com.google.gson.annotations.SerializedName;

public enum ToggleValue {
    @SerializedName("DISABLED")
    DISABLED,
    @SerializedName("ENABLED")
    ENABLED;

    public boolean isEnabled() {
        return this == ENABLED;
    }

    public boolean isDisabled() {
        return this == DISABLED;
    }
}
