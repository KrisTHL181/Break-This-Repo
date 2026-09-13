package com.mojang.authlib.minecraft.report;

import com.google.gson.annotations.SerializedName;

import javax.annotation.Nullable;
import java.time.Instant;

public record AbuseReport(
    @SerializedName("opinionComments")
    String opinionComments,
    @SerializedName("reason")
    @Nullable String reason,
    @SerializedName("evidence")
    @Nullable ReportEvidence evidence,
    @SerializedName("skinUrl")
    @Nullable String skinUrl,
    @SerializedName("reportedEntity")
    ReportedEntity reportedEntity,
    @SerializedName("createdTime")
    Instant createdTime
) {
    public static AbuseReport name(final String opinionComments, final ReportedEntity reportedEntity, final Instant createdTime) {
        return new AbuseReport(opinionComments, null, null, null, reportedEntity, createdTime);
    }

    public static AbuseReport skin(final String opinionComments, final String reason, @Nullable final String skinUrl, final ReportedEntity reportedEntity, final Instant createdTime) {
        return new AbuseReport(opinionComments, reason, null, skinUrl, reportedEntity, createdTime);
    }

    public static AbuseReport chat(final String opinionComments, final String reason, final ReportEvidence evidence, final ReportedEntity reportedEntity, final Instant createdTime) {
        return new AbuseReport(opinionComments, reason, evidence, null, reportedEntity, createdTime);
    }
}
