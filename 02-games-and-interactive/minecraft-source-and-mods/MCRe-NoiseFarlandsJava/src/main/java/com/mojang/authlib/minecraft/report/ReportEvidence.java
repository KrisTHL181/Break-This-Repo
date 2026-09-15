package com.mojang.authlib.minecraft.report;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public record ReportEvidence(
    @SerializedName("messages")
    List<ReportChatMessage> messages
) {
}
