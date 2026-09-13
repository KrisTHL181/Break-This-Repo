package net.minecraft.data.worldgen.biome;

import net.minecraft.core.HolderGetter;
import net.minecraft.data.worldgen.BiomeDefaultFeatures;
import net.minecraft.data.worldgen.placement.EndPlacements;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

public class EndBiomes {
   private static Biome baseEndBiome(BiomeGenerationSettings.Builder p_194825_) {
      MobSpawnSettings.Builder mobspawnsettings$builder = new MobSpawnSettings.Builder();
      BiomeDefaultFeatures.endSpawns(mobspawnsettings$builder);
      return new Biome.BiomeBuilder()
         .hasPrecipitation(false)
         .temperature(0.5F)
         .downfall(0.5F)
         .specialEffects(new BiomeSpecialEffects.Builder().waterColor(4159204).build())
         .mobSpawnSettings(mobspawnsettings$builder.build())
         .generationSettings(p_194825_.build())
         .build();
   }

   public static Biome endBarrens(HolderGetter<PlacedFeature> p_256510_, HolderGetter<ConfiguredWorldCarver<?>> p_256130_) {
      BiomeGenerationSettings.Builder biomegenerationsettings$builder = new BiomeGenerationSettings.Builder(p_256510_, p_256130_);
      return baseEndBiome(biomegenerationsettings$builder);
   }

   public static Biome theEnd(HolderGetter<PlacedFeature> p_255623_, HolderGetter<ConfiguredWorldCarver<?>> p_255991_) {
      BiomeGenerationSettings.Builder biomegenerationsettings$builder = new BiomeGenerationSettings.Builder(p_255623_, p_255991_)
         .addFeature(GenerationStep.Decoration.SURFACE_STRUCTURES, EndPlacements.END_SPIKE)
         .addFeature(GenerationStep.Decoration.TOP_LAYER_MODIFICATION, EndPlacements.END_PLATFORM);
      return baseEndBiome(biomegenerationsettings$builder);
   }

   public static Biome endMidlands(HolderGetter<PlacedFeature> p_255719_, HolderGetter<ConfiguredWorldCarver<?>> p_255751_) {
      BiomeGenerationSettings.Builder biomegenerationsettings$builder = new BiomeGenerationSettings.Builder(p_255719_, p_255751_);
      return baseEndBiome(biomegenerationsettings$builder);
   }

   public static Biome endHighlands(HolderGetter<PlacedFeature> p_256650_, HolderGetter<ConfiguredWorldCarver<?>> p_256540_) {
      BiomeGenerationSettings.Builder biomegenerationsettings$builder = new BiomeGenerationSettings.Builder(p_256650_, p_256540_)
         .addFeature(GenerationStep.Decoration.SURFACE_STRUCTURES, EndPlacements.END_GATEWAY_RETURN)
         .addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, EndPlacements.CHORUS_PLANT);
      return baseEndBiome(biomegenerationsettings$builder);
   }

   public static Biome smallEndIslands(HolderGetter<PlacedFeature> p_255848_, HolderGetter<ConfiguredWorldCarver<?>> p_256605_) {
      BiomeGenerationSettings.Builder biomegenerationsettings$builder = new BiomeGenerationSettings.Builder(p_255848_, p_256605_)
         .addFeature(GenerationStep.Decoration.RAW_GENERATION, EndPlacements.END_ISLAND_DECORATED);
      return baseEndBiome(biomegenerationsettings$builder);
   }
}
