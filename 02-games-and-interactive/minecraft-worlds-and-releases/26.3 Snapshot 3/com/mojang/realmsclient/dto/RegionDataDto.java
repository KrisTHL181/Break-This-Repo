package com.mojang.realmsclient.dto;

import com.google.gson.annotations.SerializedName;

public record RegionDataDto(@SerializedName("regionName") RealmsRegion region, @SerializedName("serviceQuality") ServiceQuality serviceQuality)
   implements ReflectionBasedSerialization {
}
