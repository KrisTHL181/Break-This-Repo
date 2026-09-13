package com.mojang.realmsclient.dto;

import com.google.gson.annotations.SerializedName;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.util.StringUtil;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import org.jspecify.annotations.Nullable;

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
      final int spawnProtection,
      final int difficulty,
      final int gameMode,
      final boolean forceGameMode,
      final String slotName,
      final String version,
      final RealmsServer.Compatibility compatibility
   ) {
      this.spawnProtection = spawnProtection;
      this.difficulty = difficulty;
      this.gameMode = gameMode;
      this.forceGameMode = forceGameMode;
      this.slotName = slotName;
      this.version = version;
      this.compatibility = compatibility;
   }

   public static RealmsWorldOptions createDefaults() {
      return new RealmsWorldOptions();
   }

   public static RealmsWorldOptions createDefaultsWith(
      final GameType gameMode, final Difficulty difficulty, final boolean hardcore, final String version, final String worldName
   ) {
      RealmsWorldOptions options = createDefaults();
      options.difficulty = difficulty.getId();
      options.gameMode = gameMode.getId();
      options.slotName = worldName;
      options.version = version;
      return options;
   }

   public static RealmsWorldOptions createFromSettings(final LevelSettings settings, final String worldVersion) {
      return createDefaultsWith(
         settings.gameType(), settings.difficultySettings().difficulty(), settings.difficultySettings().hardcore(), worldVersion, settings.levelName()
      );
   }

   public static RealmsWorldOptions createEmptyDefaults() {
      RealmsWorldOptions options = createDefaults();
      options.setEmpty(true);
      return options;
   }

   public void setEmpty(final boolean empty) {
      this.empty = empty;
   }

   public static RealmsWorldOptions parse(final GuardedSerializer gson, final String json) {
      RealmsWorldOptions options = gson.fromJson(json, RealmsWorldOptions.class);
      if (options == null) {
         return createDefaults();
      }

      finalize(options);
      return options;
   }

   private static void finalize(final RealmsWorldOptions options) {
      if (options.slotName == null) {
         options.slotName = "";
      }

      if (options.version == null) {
         options.version = "";
      }

      if (options.compatibility == null) {
         options.compatibility = RealmsServer.Compatibility.UNVERIFIABLE;
      }
   }

   public String getSlotName(final int i) {
      if (StringUtil.isBlank(this.slotName)) {
         return this.empty ? I18n.get("mco.configure.world.slot.empty") : this.getDefaultSlotName(i);
      } else {
         return this.slotName;
      }
   }

   public String getDefaultSlotName(final int i) {
      return I18n.get("mco.configure.world.slot", i);
   }

   public RealmsWorldOptions copy() {
      return new RealmsWorldOptions(this.spawnProtection, this.difficulty, this.gameMode, this.forceGameMode, this.slotName, this.version, this.compatibility);
   }
}
