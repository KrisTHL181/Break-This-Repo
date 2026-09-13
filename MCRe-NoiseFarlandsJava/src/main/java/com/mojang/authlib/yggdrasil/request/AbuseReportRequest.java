package com.mojang.authlib.yggdrasil.request;

import com.google.gson.annotations.SerializedName;
import com.mojang.authlib.minecraft.report.AbuseReport;

import javax.annotation.Nullable;
import java.util.UUID;

public record AbuseReportRequest(
    @SerializedName("version")
    int version,
    @SerializedName("id")
    UUID id,
    @SerializedName("report")
    AbuseReport report,
    @SerializedName("clientInfo")
    ClientInfo clientInfo,
    @SerializedName("thirdPartyServerInfo")
    @Nullable ThirdPartyServerInfo thirdPartyServerInfo,
    @SerializedName("realmInfo")
    @Nullable RealmInfo realmInfo,
    @SerializedName("reportType")
    String reportType
) {
    public record ClientInfo(
        @SerializedName("clientVersion")
        String clientVersion,
        // IETF BCP 47 language tag
        @SerializedName("locale")
        String locale
    ) {
    }

    public record ThirdPartyServerInfo(
        @SerializedName("address")
        String address
    ) {
    }

    public record RealmInfo(
        @SerializedName("realmId")
        String realmId,
        @SerializedName("slotId")
        int slotId
    ) {
    }
}
