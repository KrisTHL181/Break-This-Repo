package com.mojang.realmsclient.dto;

import com.google.gson.annotations.SerializedName;
import java.util.List;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public record PreferredRegionsDto(@SerializedName("regionDataList") List<RegionDataDto> regionData) implements ReflectionBasedSerialization {
    public static PreferredRegionsDto empty() {
        return new PreferredRegionsDto(List.of());
    }
}