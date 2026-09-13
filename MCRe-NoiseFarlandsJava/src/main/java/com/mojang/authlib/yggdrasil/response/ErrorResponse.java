package com.mojang.authlib.yggdrasil.response;

import com.google.gson.annotations.SerializedName;

import javax.annotation.Nullable;
import java.util.Map;

/**
 * Error entity returned by all Minecraft.net services (As well as Yggdrasil services)
 */
public record ErrorResponse(
    @SerializedName("path")
    String path,
    @SerializedName("error")
    @Nullable String error,
    @SerializedName("errorMessage")
    @Nullable String errorMessage,
    @SerializedName("details")
    @Nullable Map<String, Object> details
) {
}
