package com.mojang.authlib.minecraft.report;

import com.google.gson.annotations.SerializedName;

import java.util.UUID;

public record ReportedEntity(
    @SerializedName("profileId")
    UUID profileId
) {
}
