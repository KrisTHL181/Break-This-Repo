package net.minecraft.world.level.storage;

import net.minecraft.world.level.levelgen.WorldDimensions;
import net.minecraft.world.level.levelgen.WorldGenSettings;

public record LevelDataAndDimensions(LevelDataAndDimensions.WorldDataAndGenSettings worldDataAndGenSettings, WorldDimensions.Complete dimensions) {
   public static LevelDataAndDimensions create(final WorldData data, final WorldGenSettings genSettings, final WorldDimensions.Complete dimensions) {
      return new LevelDataAndDimensions(new LevelDataAndDimensions.WorldDataAndGenSettings(data, genSettings), dimensions);
   }

   public record WorldDataAndGenSettings(WorldData data, WorldGenSettings genSettings) {
   }
}
