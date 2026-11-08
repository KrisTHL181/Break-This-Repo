package com.mojang.authlib.minecraft.report;

public record AbuseReportLimits(
   int maxOpinionCommentsLength, int maxReportedMessageCount, int maxEvidenceMessageCount, int leadingContextMessageCount, int trailingContextMessageCount
) {
   public static final AbuseReportLimits DEFAULTS = new AbuseReportLimits(1000, 4, 40, 9, 0);
}
