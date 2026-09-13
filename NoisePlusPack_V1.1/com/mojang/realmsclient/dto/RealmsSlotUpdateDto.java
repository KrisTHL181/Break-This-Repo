package com.mojang.realmsclient.dto;

import com.google.gson.annotations.SerializedName;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public record RealmsSlotUpdateDto(
   @SerializedName("slotId") int slotId,
   @SerializedName("spawnProtection") int spawnProtection,
   @SerializedName("forceGameMode") boolean forceGameMode,
   @SerializedName("difficulty") int difficulty,
   @SerializedName("gameMode") int gameMode,
   @SerializedName("slotName") String slotName,
   @SerializedName("version") String version,
   @SerializedName("compatibility") RealmsServer.Compatibility compatibility,
   @SerializedName("worldTemplateId") long templateId,
   @SerializedName("worldTemplateImage") @Nullable String templateImage,
   @SerializedName("hardcore") boolean hardcore
) implements ReflectionBasedSerialization {
   public RealmsSlotUpdateDto(int p_407504_, RealmsWorldOptions p_409419_, boolean p_406504_) {
      this(
         p_407504_,
         p_409419_.spawnProtection,
         p_409419_.forceGameMode,
         p_409419_.difficulty,
         p_409419_.gameMode,
         p_409419_.getSlotName(p_407504_),
         p_409419_.version,
         p_409419_.compatibility,
         p_409419_.templateId,
         p_409419_.templateImage,
         p_406504_
      );
   }
}
