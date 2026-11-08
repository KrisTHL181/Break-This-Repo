package com.mojang.realmsclient.dto;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public record PingResult(@SerializedName("pingResults") List<RegionPingResult> pingResults, @SerializedName("worldIds") List<Long> realmIds)
   implements ReflectionBasedSerialization {
}
