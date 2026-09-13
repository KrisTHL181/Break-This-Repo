package net.minecraft.data.worldgen;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.Noises;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

public class NoiseData {
   @Deprecated
   public static final NormalNoise.NoiseParameters DEFAULT_SHIFT = new NormalNoise.NoiseParameters(-3, 1.0, 1.0, 1.0, 0.0);

   public static void bootstrap(BootstrapContext<NormalNoise.NoiseParameters> p_330944_) {
      registerBiomeNoises(p_330944_, 0, Noises.TEMPERATURE, Noises.VEGETATION, Noises.CONTINENTALNESS, Noises.EROSION);
      registerBiomeNoises(p_330944_, -2, Noises.TEMPERATURE_LARGE, Noises.VEGETATION_LARGE, Noises.CONTINENTALNESS_LARGE, Noises.EROSION_LARGE);
      register(p_330944_, Noises.RIDGE, -7, 1.0, 2.0, 1.0, 0.0, 0.0, 0.0);
      p_330944_.register(Noises.SHIFT, DEFAULT_SHIFT);
      register(p_330944_, Noises.AQUIFER_BARRIER, -3, 1.0);
      register(p_330944_, Noises.AQUIFER_FLUID_LEVEL_FLOODEDNESS, -7, 1.0);
      register(p_330944_, Noises.AQUIFER_LAVA, -1, 1.0);
      register(p_330944_, Noises.AQUIFER_FLUID_LEVEL_SPREAD, -5, 1.0);
      register(p_330944_, Noises.PILLAR, -7, 1.0, 1.0);
      register(p_330944_, Noises.PILLAR_RARENESS, -8, 1.0);
      register(p_330944_, Noises.PILLAR_THICKNESS, -8, 1.0);
      register(p_330944_, Noises.SPAGHETTI_2D, -7, 1.0);
      register(p_330944_, Noises.SPAGHETTI_2D_ELEVATION, -8, 1.0);
      register(p_330944_, Noises.SPAGHETTI_2D_MODULATOR, -11, 1.0);
      register(p_330944_, Noises.SPAGHETTI_2D_THICKNESS, -11, 1.0);
      register(p_330944_, Noises.SPAGHETTI_3D_1, -7, 1.0);
      register(p_330944_, Noises.SPAGHETTI_3D_2, -7, 1.0);
      register(p_330944_, Noises.SPAGHETTI_3D_RARITY, -11, 1.0);
      register(p_330944_, Noises.SPAGHETTI_3D_THICKNESS, -8, 1.0);
      register(p_330944_, Noises.SPAGHETTI_ROUGHNESS, -5, 1.0);
      register(p_330944_, Noises.SPAGHETTI_ROUGHNESS_MODULATOR, -8, 1.0);
      register(p_330944_, Noises.CAVE_ENTRANCE, -7, 0.4, 0.5, 1.0);
      register(p_330944_, Noises.CAVE_LAYER, -8, 1.0);
      register(p_330944_, Noises.CAVE_CHEESE, -8, 0.5, 1.0, 2.0, 1.0, 2.0, 1.0, 0.0, 2.0, 0.0);
      register(p_330944_, Noises.ORE_VEININESS, -8, 1.0);
      register(p_330944_, Noises.ORE_VEIN_A, -7, 1.0);
      register(p_330944_, Noises.ORE_VEIN_B, -7, 1.0);
      register(p_330944_, Noises.ORE_GAP, -5, 1.0);
      register(p_330944_, Noises.NOODLE, -8, 1.0);
      register(p_330944_, Noises.NOODLE_THICKNESS, -8, 1.0);
      register(p_330944_, Noises.NOODLE_RIDGE_A, -7, 1.0);
      register(p_330944_, Noises.NOODLE_RIDGE_B, -7, 1.0);
      register(p_330944_, Noises.JAGGED, -16, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0);
      register(p_330944_, Noises.SURFACE, -6, 1.0, 1.0, 1.0);
      register(p_330944_, Noises.SURFACE_SECONDARY, -6, 1.0, 1.0, 0.0, 1.0);
      register(p_330944_, Noises.CLAY_BANDS_OFFSET, -8, 1.0);
      register(p_330944_, Noises.BADLANDS_PILLAR, -2, 1.0, 1.0, 1.0, 1.0);
      register(p_330944_, Noises.BADLANDS_PILLAR_ROOF, -8, 1.0);
      register(p_330944_, Noises.BADLANDS_SURFACE, -6, 1.0, 1.0, 1.0);
      register(p_330944_, Noises.ICEBERG_PILLAR, -6, 1.0, 1.0, 1.0, 1.0);
      register(p_330944_, Noises.ICEBERG_PILLAR_ROOF, -3, 1.0);
      register(p_330944_, Noises.ICEBERG_SURFACE, -6, 1.0, 1.0, 1.0);
      register(p_330944_, Noises.SWAMP, -2, 1.0);
      register(p_330944_, Noises.CALCITE, -9, 1.0, 1.0, 1.0, 1.0);
      register(p_330944_, Noises.GRAVEL, -8, 1.0, 1.0, 1.0, 1.0);
      register(p_330944_, Noises.POWDER_SNOW, -6, 1.0, 1.0, 1.0, 1.0);
      register(p_330944_, Noises.PACKED_ICE, -7, 1.0, 1.0, 1.0, 1.0);
      register(p_330944_, Noises.ICE, -4, 1.0, 1.0, 1.0, 1.0);
      register(p_330944_, Noises.SOUL_SAND_LAYER, -8, 1.0, 1.0, 1.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.013333333333333334);
      register(p_330944_, Noises.GRAVEL_LAYER, -8, 1.0, 1.0, 1.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.013333333333333334);
      register(p_330944_, Noises.PATCH, -5, 1.0, 0.0, 0.0, 0.0, 0.0, 0.013333333333333334);
      register(p_330944_, Noises.NETHERRACK, -3, 1.0, 0.0, 0.0, 0.35);
      register(p_330944_, Noises.NETHER_WART, -3, 1.0, 0.0, 0.0, 0.9);
      register(p_330944_, Noises.NETHER_STATE_SELECTOR, -4, 1.0);
   }

   private static void registerBiomeNoises(
      BootstrapContext<NormalNoise.NoiseParameters> p_331173_,
      int p_236479_,
      ResourceKey<NormalNoise.NoiseParameters> p_236480_,
      ResourceKey<NormalNoise.NoiseParameters> p_236481_,
      ResourceKey<NormalNoise.NoiseParameters> p_236482_,
      ResourceKey<NormalNoise.NoiseParameters> p_236483_
   ) {
      register(p_331173_, p_236480_, -10 + p_236479_, 1.5, 0.0, 1.0, 0.0, 0.0, 0.0);
      register(p_331173_, p_236481_, -8 + p_236479_, 1.0, 1.0, 0.0, 0.0, 0.0, 0.0);
      register(p_331173_, p_236482_, -9 + p_236479_, 1.0, 1.0, 2.0, 2.0, 2.0, 1.0, 1.0, 1.0, 1.0);
      register(p_331173_, p_236483_, -9 + p_236479_, 1.0, 1.0, 0.0, 1.0, 1.0);
   }

   private static void register(
      BootstrapContext<NormalNoise.NoiseParameters> p_333944_,
      ResourceKey<NormalNoise.NoiseParameters> p_255970_,
      int p_256539_,
      double p_256566_,
      double... p_255998_
   ) {
      p_333944_.register(p_255970_, new NormalNoise.NoiseParameters(p_256539_, p_256566_, p_255998_));
   }
}
