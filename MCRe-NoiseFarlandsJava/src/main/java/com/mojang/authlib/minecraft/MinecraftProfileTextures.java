package com.mojang.authlib.minecraft;

import com.mojang.authlib.SignatureState;

import javax.annotation.Nullable;

public record MinecraftProfileTextures(
    @Nullable MinecraftProfileTexture skin,
    @Nullable MinecraftProfileTexture cape,
    @Nullable MinecraftProfileTexture elytra,
    SignatureState signatureState
) {
    public static final MinecraftProfileTextures EMPTY = new MinecraftProfileTextures(null, null, null, SignatureState.SIGNED);
}
