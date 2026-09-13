package com.mojang.realmsclient.dto;

import com.google.gson.annotations.SerializedName;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.util.StringUtil;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class RealmsWorldOptions extends ValueObject implements ReflectionBasedSerialization {
   @SerializedName("spawnProtection")
   public int spawnProtection = 0;
   @SerializedName("forceGameMode")
   public boolean forceGameMode = false;
   @SerializedName("difficulty")
   public int difficulty = 2;
   @SerializedName("gameMode")
   public int gameMode = 0;
   @SerializedName("slotName")
   private String slotName = "";
   @SerializedName("version")
   public String version = "";
   @SerializedName("compatibility")
   public RealmsServer.Compatibility compatibility = RealmsServer.Compatibility.UNVERIFIABLE;
   @SerializedName("worldTemplateId")
   public long templateId = -1L;
   @SerializedName("worldTemplateImage")
   public @Nullable String templateImage = null;
   @Exclude
   public boolean empty;

   private RealmsWorldOptions() {
   }

   public RealmsWorldOptions(
      int p_407314_, int p_410745_, int p_409784_, boolean p_409224_, String p_409662_, String p_407883_, RealmsServer.Compatibility p_409798_
   ) {
      this.spawnProtection = p_407314_;
      this.difficulty = p_410745_;
      this.gameMode = p_409784_;
      this.forceGameMode = p_409224_;
      this.slotName = p_409662_;
      this.version = p_407883_;
      this.compatibility = p_409798_;
   }

   public static RealmsWorldOptions createDefaults() {
      return new RealmsWorldOptions();
   }

   public static RealmsWorldOptions createDefaultsWith(GameType p_364043_, Difficulty p_366299_, boolean p_368672_, String p_361621_, String p_365919_) {
      RealmsWorldOptions realmsworldoptions = createDefaults();
      realmsworldoptions.difficulty = p_366299_.getId();
      realmsworldoptions.gameMode = p_364043_.getId();
      realmsworldoptions.slotName = p_365919_;
      realmsworldoptions.version = p_361621_;
      return realmsworldoptions;
   }

   public static RealmsWorldOptions createFromSettings(LevelSettings p_361674_, String p_370223_) {
      return createDefaultsWith(p_361674_.gameType(), p_361674_.difficulty(), p_361674_.hardcore(), p_370223_, p_361674_.levelName());
   }

   public static RealmsWorldOptions createEmptyDefaults() {
      RealmsWorldOptions realmsworldoptions = createDefaults();
      realmsworldoptions.setEmpty(true);
      return realmsworldoptions;
   }

   public void setEmpty(boolean p_87631_) {
      this.empty = p_87631_;
   }

   public static RealmsWorldOptions parse(GuardedSerializer p_410511_, String p_408086_) {
      RealmsWorldOptions realmsworldoptions = p_410511_.fromJson(p_408086_, RealmsWorldOptions.class);
      if (realmsworldoptions == null) {
         return createDefaults();
      }

      finalize(realmsworldoptions);
      return realmsworldoptions;
   }

   private static void finalize(RealmsWorldOptions p_406148_) {
      if (p_406148_.slotName == null) {
         p_406148_.slotName = "";
      }

      if (p_406148_.version == null) {
         p_406148_.version = "";
      }

      if (p_406148_.compatibility == null) {
         p_406148_.compatibility = RealmsServer.Compatibility.UNVERIFIABLE;
      }
   }

   public String getSlotName(int p_87627_) {
      if (StringUtil.isBlank(this.slotName)) {
         return this.empty ? I18n.get("mco.configure.world.slot.empty") : this.getDefaultSlotName(p_87627_);
      } else {
         return this.slotName;
      }
   }

   public String getDefaultSlotName(int p_87634_) {
      return I18n.get("mco.configure.world.slot", p_87634_);
   }

   public RealmsWorldOptions copy() {
      return new RealmsWorldOptions(this.spawnProtection, this.difficulty, this.gameMode, this.forceGameMode, this.slotName, this.version, this.compatibility);
   }
}
