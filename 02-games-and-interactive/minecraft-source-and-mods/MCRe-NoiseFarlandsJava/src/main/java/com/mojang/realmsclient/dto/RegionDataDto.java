package com.mojang.realmsclient.dto;

import com.google.gson.annotations.SerializedName;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public record RegionDataDto(@SerializedName("regionName") RealmsRegion region, @SerializedName("serviceQuality") ServiceQuality serviceQuality)
    implements ReflectionBasedSerialization {
}