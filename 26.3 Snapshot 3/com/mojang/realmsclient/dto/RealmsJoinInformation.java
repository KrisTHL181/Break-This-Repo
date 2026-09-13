package com.mojang.realmsclient.dto;

import com.google.gson.annotations.SerializedName;
import com.mojang.logging.LogUtils;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public record RealmsJoinInformation(
   @SerializedName("address") @Nullable String address,
   @SerializedName("resourcePackUrl") @Nullable String resourcePackUrl,
   @SerializedName("resourcePackHash") @Nullable String resourcePackHash,
   @SerializedName("sessionRegionData") RealmsJoinInformation.@Nullable RegionData regionData
) implements ReflectionBasedSerialization {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final RealmsJoinInformation EMPTY = new RealmsJoinInformation(null, null, null, null);

   public static RealmsJoinInformation parse(final GuardedSerializer gson, final String json) {
      try {
         RealmsJoinInformation server = gson.fromJson(json, RealmsJoinInformation.class);
         if (server == null) {
            LOGGER.error("Could not parse RealmsServerAddress: {}", json);
            return EMPTY;
         } else {
            return server;
         }
      } catch (Exception e) {
         LOGGER.error("Could not parse RealmsServerAddress", e);
         return EMPTY;
      }
   }

   public record RegionData(
      @SerializedName("regionName") @Nullable RealmsRegion region, @SerializedName("serviceQuality") @Nullable ServiceQuality serviceQuality
   ) implements ReflectionBasedSerialization {
   }
}
