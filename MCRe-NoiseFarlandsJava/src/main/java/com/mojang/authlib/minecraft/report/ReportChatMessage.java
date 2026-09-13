package com.mojang.authlib.minecraft.report;

import com.google.gson.annotations.SerializedName;

import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ReportChatMessage(
    @SerializedName("index")
    int index,
    @SerializedName("profileId")
    UUID profileId,
    @SerializedName("sessionId")
    UUID sessionId,
    @SerializedName("timestamp")
    Instant timestamp,
    @SerializedName("salt")
    long salt,
    @SerializedName("lastSeen")
    List<ByteBuffer> lastSeen,
    @SerializedName("message")
    String message,
    @SerializedName("signature")
    ByteBuffer signature,
    @SerializedName("messageReported")
    boolean messageReported
) {
}
