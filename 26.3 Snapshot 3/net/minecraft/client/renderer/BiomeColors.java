package net.minecraft.client.renderer;

import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.biome.Biome;

public class BiomeColors {
   public static final ColorResolver GRASS_COLOR_RESOLVER = Biome::getGrassColor;
   public static final ColorResolver FOLIAGE_COLOR_RESOLVER = (biome, x, z) -> biome.getFoliageColor();
   public static final ColorResolver DRY_FOLIAGE_COLOR_RESOLVER = (biome, x, z) -> biome.getDryFoliageColor();
   public static final ColorResolver WATER_COLOR_RESOLVER = (biome, x, z) -> biome.getWaterColor();

   private static int getAverageColor(final BlockAndTintGetter level, final BlockPos pos, final ColorResolver colorResolver) {
      return level.getBlockTint(pos, colorResolver);
   }

   public static int getAverageGrassColor(final BlockAndTintGetter level, final BlockPos pos) {
      return getAverageColor(level, pos, GRASS_COLOR_RESOLVER);
   }

   public static int getAverageFoliageColor(final BlockAndTintGetter level, final BlockPos pos) {
      return getAverageColor(level, pos, FOLIAGE_COLOR_RESOLVER);
   }

   public static int getAverageDryFoliageColor(final BlockAndTintGetter level, final BlockPos pos) {
      return getAverageColor(level, pos, DRY_FOLIAGE_COLOR_RESOLVER);
   }

   public static int getAverageWaterColor(final BlockAndTintGetter level, final BlockPos pos) {
      return getAverageColor(level, pos, WATER_COLOR_RESOLVER);
   }
}
