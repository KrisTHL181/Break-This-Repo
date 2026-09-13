package net.minecraft.data.worldgen.biome;

import net.minecraft.core.HolderGetter;
import net.minecraft.data.worldgen.BiomeDefaultFeatures;
import net.minecraft.data.worldgen.placement.EndPlacements;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.carver.WorldCarver;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

public class EndBiomes {
   private static Biome baseEndBiome(final BiomeGenerationSettings.Builder generation) {
      MobSpawnSettings.Builder mobs = new MobSpawnSettings.Builder();
      BiomeDefaultFeatures.endSpawns(mobs);
      return new Biome.BiomeBuilder()
         .hasPrecipitation(false)
         .temperature(0.5F)
         .downfall(0.5F)
         .specialEffects(new BiomeSpecialEffects.Builder().waterColor(4159204).build())
         .mobSpawnSettings(mobs.build())
         .generationSettings(generation.build())
         .build();
   }

   public static Biome endBarrens(final HolderGetter<PlacedFeature> placedFeatures, final HolderGetter<WorldCarver> carvers) {
      BiomeGenerationSettings.Builder generation = new BiomeGenerationSettings.Builder(placedFeatures, carvers);
      return baseEndBiome(generation);
   }

   public static Biome theEnd(final HolderGetter<PlacedFeature> placedFeatures, final HolderGetter<WorldCarver> carvers) {
      BiomeGenerationSettings.Builder generation = new BiomeGenerationSettings.Builder(placedFeatures, carvers)
         .addFeature(GenerationStep.Decoration.SURFACE_STRUCTURES, EndPlacements.END_SPIKE)
         .addFeature(GenerationStep.Decoration.TOP_LAYER_MODIFICATION, EndPlacements.END_PLATFORM);
      return baseEndBiome(generation);
   }

   public static Biome endMidlands(final HolderGetter<PlacedFeature> placedFeatures, final HolderGetter<WorldCarver> carvers) {
      BiomeGenerationSettings.Builder generation = new BiomeGenerationSettings.Builder(placedFeatures, carvers);
      return baseEndBiome(generation);
   }

   public static Biome endHighlands(final HolderGetter<PlacedFeature> placedFeatures, final HolderGetter<WorldCarver> carvers) {
      BiomeGenerationSettings.Builder generation = new BiomeGenerationSettings.Builder(placedFeatures, carvers)
         .addFeature(GenerationStep.Decoration.SURFACE_STRUCTURES, EndPlacements.END_GATEWAY_RETURN)
         .addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, EndPlacements.CHORUS_PLANT);
      return baseEndBiome(generation);
   }

   public static Biome smallEndIslands(final HolderGetter<PlacedFeature> placedFeatures, final HolderGetter<WorldCarver> carvers) {
      BiomeGenerationSettings.Builder generation = new BiomeGenerationSettings.Builder(placedFeatures, carvers)
         .addFeature(GenerationStep.Decoration.RAW_GENERATION, EndPlacements.END_ISLAND_DECORATED);
      return baseEndBiome(generation);
   }
}
