package com.mojang.realmsclient.dto;

import com.google.gson.annotations.SerializedName;
import org.jspecify.annotations.Nullable;

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
   public RealmsSlotUpdateDto(final int slotId, final RealmsWorldOptions options, final boolean hardcore) {
      this(
         slotId,
         options.spawnProtection,
         options.forceGameMode,
         options.difficulty,
         options.gameMode,
         options.getSlotName(slotId),
         options.version,
         options.compatibility,
         options.templateId,
         options.templateImage,
         hardcore
      );
   }
}
