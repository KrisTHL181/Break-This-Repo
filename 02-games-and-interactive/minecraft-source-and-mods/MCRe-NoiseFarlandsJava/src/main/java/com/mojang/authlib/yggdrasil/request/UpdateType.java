package com.mojang.authlib.yggdrasil.request;

import com.google.gson.annotations.SerializedName;

public enum UpdateType {
    @SerializedName("ADD")
    ADD,
    @SerializedName("REMOVE")
    REMOVE
}

