package com.mojang.authlib.yggdrasil.response;

import com.google.gson.annotations.SerializedName;

import java.nio.ByteBuffer;

public record KeyPairResponse(
    @SerializedName("keyPair")
    KeyPair keyPair,
    @SerializedName("publicKeySignatureV2")
    ByteBuffer publicKeySignature,
    @SerializedName("expiresAt")
    String expiresAt,
    @SerializedName("refreshedAfter")
    String refreshedAfter
) {
    public record KeyPair(
        @SerializedName("privateKey")
        String privateKey,
        @SerializedName("publicKey")
        String publicKey
    ) {
    }
}
