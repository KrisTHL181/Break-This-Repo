package com.mojang.authlib.services.request;

import com.google.gson.annotations.SerializedName;
import com.mojang.authlib.minecraft.report.AbuseReport;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

public record AbuseReportRequest(
   @SerializedName("version") int version,
   @SerializedName("id") UUID id,
   @SerializedName("report") AbuseReport report,
   @SerializedName("clientInfo") AbuseReportRequest.ClientInfo clientInfo,
   @Nullable @SerializedName("thirdPartyServerInfo") AbuseReportRequest.ThirdPartyServerInfo thirdPartyServerInfo,
   @Nullable @SerializedName("realmInfo") AbuseReportRequest.RealmInfo realmInfo,
   @SerializedName("reportType") String reportType
) {
   public record ClientInfo(@SerializedName("clientVersion") String clientVersion, @SerializedName("locale") String locale) {
   }

   public record RealmInfo(@SerializedName("realmId") String realmId, @SerializedName("slotId") int slotId) {
   }

   public record ThirdPartyServerInfo(@SerializedName("address") String address) {
   }
}
