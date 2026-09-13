package net.minecraft.util.filefix.fixes;

import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.OptionalDynamic;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.util.Util;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.util.datafix.fixes.References;
import net.minecraft.util.filefix.FileFix;
import net.minecraft.util.filefix.access.CompressedNbt;
import net.minecraft.util.filefix.access.FileAccess;
import net.minecraft.util.filefix.access.FileRelation;
import net.minecraft.util.filefix.access.FileResourceTypes;
import net.minecraft.util.filefix.access.LevelDat;
import net.minecraft.util.filefix.access.PlayerData;
import net.minecraft.util.filefix.access.SavedDataNbt;
import net.minecraft.util.worldupdate.UpgradeProgress;

public class LevelDatToSavedDataFileFix extends FileFix {
   private static final UUID FALLBACK_SINGLE_PLAYER_UUID = Util.NIL_UUID;
   private static final String OVERWORLD = "overworld";
   private static final String THE_NETHER = "the_nether";
   private static final String THE_END = "the_end";
   private static final String WORLD_BORDER_KEY = "world_border";
   private static final String WORLD_BORDER_FILE_NAME = "minecraft/world_border.dat";

   public LevelDatToSavedDataFileFix(final Schema schema) {
      super(schema);
   }

   @Override
   public void makeFixer() {
      this.addFileContentFix(
         files -> {
            FileAccess<LevelDat> levelDat = files.getFileAccess(FileResourceTypes.LEVEL_DAT, FileRelation.ORIGIN.forFile("level.dat"));
            FileAccess<SavedDataNbt> dragonFight = files.getFileAccess(
               FileResourceTypes.savedData(References.SAVED_DATA_ENDER_DRAGON_FIGHT),
               FileRelation.forDataFileInDimension("the_end", "minecraft/ender_dragon_fight.dat")
            );
            FileAccess<PlayerData> fallbackPlayerData = files.getFileAccess(
               FileResourceTypes.PLAYER_DATA, FileRelation.PLAYER_DATA.forFile(FALLBACK_SINGLE_PLAYER_UUID + ".dat")
            );
            FileAccess<SavedDataNbt> wanderingTrader = files.getFileAccess(
               FileResourceTypes.savedData(References.SAVED_DATA_WANDERING_TRADER), FileRelation.DATA.forFile("minecraft/wandering_trader.dat")
            );
            FileAccess<SavedDataNbt> customBossEvents = files.getFileAccess(
               FileResourceTypes.savedData(References.SAVED_DATA_CUSTOM_BOSS_EVENTS), FileRelation.DATA.forFile("minecraft/custom_boss_events.dat")
            );
            FileAccess<SavedDataNbt> weatherData = files.getFileAccess(
               FileResourceTypes.savedData(References.SAVED_DATA_WEATHER), FileRelation.DATA.forFile("minecraft/weather.dat")
            );
            FileAccess<SavedDataNbt> scheduledEvents = files.getFileAccess(
               FileResourceTypes.savedData(References.SAVED_DATA_SCHEDULED_EVENTS), FileRelation.DATA.forFile("minecraft/scheduled_events.dat")
            );
            FileAccess<SavedDataNbt> worldBorderOverworld = files.getFileAccess(
               FileResourceTypes.savedData(References.SAVED_DATA_WORLD_BORDER), FileRelation.forDataFileInDimension("overworld", "minecraft/world_border.dat")
            );
            FileAccess<SavedDataNbt> worldBorderNether = files.getFileAccess(
               FileResourceTypes.savedData(References.SAVED_DATA_WORLD_BORDER), FileRelation.forDataFileInDimension("the_nether", "minecraft/world_border.dat")
            );
            FileAccess<SavedDataNbt> worldBorderEnd = files.getFileAccess(
               FileResourceTypes.savedData(References.SAVED_DATA_WORLD_BORDER), FileRelation.forDataFileInDimension("the_end", "minecraft/world_border.dat")
            );
            FileAccess<SavedDataNbt> gameRules = files.getFileAccess(
               FileResourceTypes.savedData(References.SAVED_DATA_GAME_RULES), FileRelation.DATA.forFile("minecraft/game_rules.dat")
            );
            FileAccess<SavedDataNbt> worldGenSettings = files.getFileAccess(
               FileResourceTypes.savedData(References.SAVED_DATA_WORLD_GEN_SETTINGS), FileRelation.DATA.forFile("minecraft/world_gen_settings.dat")
            );
            FileAccess<SavedDataNbt> worldClocks = files.getFileAccess(
               FileResourceTypes.savedData(References.SAVED_DATA_WORLD_CLOCKS), FileRelation.DATA.forFile("minecraft/world_clocks.dat")
            );
            return upgradeProgress -> {
               upgradeProgress.setType(UpgradeProgress.Type.FILES);
               LevelDat levelDatFile = levelDat.getOnlyFile();
               Optional<Dynamic<Tag>> readData = levelDatFile.read();
               if (!readData.isEmpty()) {
                  Dynamic<?> content = readData.get();
                  content = extractToFile(dragonFight, content, "dragon_fight");
                  content = this.extractPlayerDataToFile(fallbackPlayerData, content);
                  content = extractToFile(wanderingTrader, content, "wandering_trader_migration_data");
                  content = extractToFile(customBossEvents, content, "CustomBossEvents");
                  content = extractToFile(weatherData, content, "weather_data");
                  content = extractToFile(scheduledEvents, content, "scheduled_events");
                  content = extractWorldBorderToFiles(worldBorderOverworld, worldBorderNether, worldBorderEnd, content);
                  content = extractToFile(gameRules, content, "game_rules");
                  content = this.extractWorldGenSettingsToFile(worldGenSettings, content);
                  content = extractToFile(worldClocks, content, "world_clocks");
                  levelDatFile.write(content);
               }
            };
         }
      );
   }

   private static Dynamic<?> extractToFile(final FileAccess<? extends CompressedNbt> targetFile, final Dynamic<?> content, final String key) {
      OptionalDynamic<?> tagOpt = content.get(key);
      if (tagOpt.result().isEmpty()) {
         return content;
      }

      Dynamic<?> tag = (Dynamic<?>)tagOpt.result().get();
      targetFile.getOnlyFile().write(tag);
      return content.remove(key);
   }

   private Dynamic<?> extractPlayerDataToFile(final FileAccess<PlayerData> fallbackFile, final Dynamic<?> content) {
      OptionalDynamic<?> playerTagOpt = content.get("Player");
      if (playerTagOpt.result().isEmpty()) {
         return content;
      }

      Dynamic<?> playerTag = (Dynamic<?>)playerTagOpt.result().get();
      int dataVersion = NbtUtils.getDataVersion(playerTag);
      Dynamic<?> playerTagFixed = DataFixTypes.PLAYER.update(DataFixers.getDataFixer(), playerTag, dataVersion, this.getVersion());
      Optional<? extends Dynamic<?>> playerUuid = playerTagFixed.get("UUID").result();
      Dynamic<?> usedUuid;
      if (playerUuid.isPresent()) {
         usedUuid = (Dynamic<?>)playerUuid.get();
      } else {
         fallbackFile.getOnlyFile().write(playerTagFixed);
         usedUuid = content.createIntList(Arrays.stream(UUIDUtil.uuidToIntArray(FALLBACK_SINGLE_PLAYER_UUID)));
      }

      return content.remove("Player").set("singleplayer_uuid", usedUuid);
   }

   private static Dynamic<?> extractWorldBorderToFiles(
      final FileAccess<? extends CompressedNbt> worldBorderOverworld,
      final FileAccess<? extends CompressedNbt> worldBorderNether,
      final FileAccess<? extends CompressedNbt> worldBorderEnd,
      final Dynamic<?> content
   ) {
      extractWorldBorderToFile(worldBorderOverworld, content, 1.0);
      extractWorldBorderToFile(worldBorderNether, content, 8.0);
      extractWorldBorderToFile(worldBorderEnd, content, 1.0);
      return content.remove("world_border");
   }

   private static void extractWorldBorderToFile(final FileAccess<? extends CompressedNbt> targetFile, final Dynamic<?> content, final double divider) {
      OptionalDynamic<?> worldBorderTagOpt = content.get("world_border");
      if (!worldBorderTagOpt.result().isEmpty()) {
         Dynamic<?> worldBorderTag = ((Dynamic)worldBorderTagOpt.result().get())
            .update("center_x", x -> x.createDouble(x.asDouble(0.0) / divider))
            .update("center_z", z -> z.createDouble(z.asDouble(0.0) / divider));
         targetFile.getOnlyFile().write(worldBorderTag);
      }
   }

   private Dynamic<?> extractWorldGenSettingsToFile(final FileAccess<? extends CompressedNbt> targetFile, final Dynamic<?> content) {
      OptionalDynamic<?> worldGenSettingsTagOpt = content.get("world_gen_settings");
      if (worldGenSettingsTagOpt.result().isEmpty()) {
         return content;
      }

      Dynamic<?> worldGenSettingsTag = (Dynamic<?>)worldGenSettingsTagOpt.result().get();
      int dataVersion = NbtUtils.getDataVersion(content);
      Dynamic<?> worldGenSettingsTagFixed = DataFixTypes.WORLD_GEN_SETTINGS
         .update(DataFixers.getDataFixer(), worldGenSettingsTag, dataVersion, this.getVersion());
      targetFile.getOnlyFile().write(worldGenSettingsTagFixed);
      return content.remove("world_gen_settings");
   }
}
