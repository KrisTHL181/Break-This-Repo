package com.mojang.authlib.minecraft.report;

import com.google.gson.annotations.SerializedName;
import java.time.Instant;
import org.jspecify.annotations.Nullable;

public record AbuseReport(
   @SerializedName("opinionComments") String opinionComments,
   @Nullable @SerializedName("reason") String reason,
   @Nullable @SerializedName("evidence") ReportEvidence evidence,
   @Nullable @SerializedName("skinUrl") String skinUrl,
   @SerializedName("reportedEntity") ReportedEntity reportedEntity,
   @SerializedName("createdTime") Instant createdTime
) {
   public static AbuseReport name(String opinionComments, ReportedEntity reportedEntity, Instant createdTime) {
      return new AbuseReport(opinionComments, null, null, null, reportedEntity, createdTime);
   }

   public static AbuseReport skin(String opinionComments, String reason, @Nullable String skinUrl, ReportedEntity reportedEntity, Instant createdTime) {
      return new AbuseReport(opinionComments, reason, null, skinUrl, reportedEntity, createdTime);
   }

   public static AbuseReport chat(String opinionComments, String reason, ReportEvidence evidence, ReportedEntity reportedEntity, Instant createdTime) {
      return new AbuseReport(opinionComments, reason, evidence, null, reportedEntity, createdTime);
   }
}
