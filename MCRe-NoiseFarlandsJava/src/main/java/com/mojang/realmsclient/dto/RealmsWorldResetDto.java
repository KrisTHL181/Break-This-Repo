package com.mojang.realmsclient.dto;

import com.google.gson.annotations.SerializedName;
import java.util.Set;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public record RealmsWorldResetDto(
    @SerializedName("seed") String seed,
    @SerializedName("worldTemplateId") long worldTemplateId,
    @SerializedName("levelType") int levelType,
    @SerializedName("generateStructures") boolean generateStructures,
    @SerializedName("experiments") Set<String> experiments
) implements ReflectionBasedSerialization {
}