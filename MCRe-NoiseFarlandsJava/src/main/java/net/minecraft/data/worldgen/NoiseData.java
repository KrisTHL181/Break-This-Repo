package net.minecraft.data.worldgen;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.Noises;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

public class NoiseData {
    @Deprecated
    public static final NormalNoise.NoiseParameters DEFAULT_SHIFT = new NormalNoise.NoiseParameters(-3, 1.0, 1.0, 1.0, 0.0);

    public static void bootstrap(final BootstrapContext<NormalNoise.NoiseParameters> context) {
        registerBiomeNoises(context, 0, Noises.TEMPERATURE, Noises.VEGETATION, Noises.CONTINENTALNESS, Noises.EROSION);
        registerBiomeNoises(context, -2, Noises.TEMPERATURE_LARGE, Noises.VEGETATION_LARGE, Noises.CONTINENTALNESS_LARGE, Noises.EROSION_LARGE);
        register(context, Noises.TEMPERATURE_NETHER, -7, 1.0, 1.0);
        register(context, Noises.VEGETATION_NETHER, -7, 1.0, 1.0);
        register(context, Noises.RIDGE, -7, 1.0, 2.0, 1.0, 0.0, 0.0, 0.0);
        context.register(Noises.SHIFT, DEFAULT_SHIFT);
        register(context, Noises.AQUIFER_BARRIER, -3, 1.0);
        register(context, Noises.AQUIFER_FLUID_LEVEL_FLOODEDNESS, -7, 1.0);
        register(context, Noises.AQUIFER_LAVA, -1, 1.0);
        register(context, Noises.AQUIFER_FLUID_LEVEL_SPREAD, -5, 1.0);
        register(context, Noises.PILLAR, -7, 1.0, 1.0);
        register(context, Noises.PILLAR_RARENESS, -8, 1.0);
        register(context, Noises.PILLAR_THICKNESS, -8, 1.0);
        register(context, Noises.SPAGHETTI_2D, -7, 1.0);
        register(context, Noises.SPAGHETTI_2D_ELEVATION, -8, 1.0);
        register(context, Noises.SPAGHETTI_2D_MODULATOR, -11, 1.0);
        register(context, Noises.SPAGHETTI_2D_THICKNESS, -11, 1.0);
        register(context, Noises.SPAGHETTI_3D_1, -7, 1.0);
        register(context, Noises.SPAGHETTI_3D_2, -7, 1.0);
        register(context, Noises.SPAGHETTI_3D_RARITY, -11, 1.0);
        register(context, Noises.SPAGHETTI_3D_THICKNESS, -8, 1.0);
        register(context, Noises.SPAGHETTI_ROUGHNESS, -5, 1.0);
        register(context, Noises.SPAGHETTI_ROUGHNESS_MODULATOR, -8, 1.0);
        register(context, Noises.CAVE_ENTRANCE, -7, 0.4, 0.5, 1.0);
        register(context, Noises.CAVE_LAYER, -8, 1.0);
        register(context, Noises.CAVE_CHEESE, -8, 0.5, 1.0, 2.0, 1.0, 2.0, 1.0, 0.0, 2.0, 0.0);
        register(context, Noises.ORE_VEININESS, -8, 1.0);
        register(context, Noises.ORE_VEIN_A, -7, 1.0);
        register(context, Noises.ORE_VEIN_B, -7, 1.0);
        register(context, Noises.ORE_GAP, -5, 1.0);
        register(context, Noises.NOODLE, -8, 1.0);
        register(context, Noises.NOODLE_THICKNESS, -8, 1.0);
        register(context, Noises.NOODLE_RIDGE_A, -7, 1.0);
        register(context, Noises.NOODLE_RIDGE_B, -7, 1.0);
        register(context, Noises.JAGGED, -16, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0);
        register(context, Noises.SURFACE, -6, 1.0, 1.0, 1.0);
        register(context, Noises.SURFACE_SECONDARY, -6, 1.0, 1.0, 0.0, 1.0);
        register(context, Noises.CLAY_BANDS_OFFSET, -8, 1.0);
        register(context, Noises.BADLANDS_PILLAR, -2, 1.0, 1.0, 1.0, 1.0);
        register(context, Noises.BADLANDS_PILLAR_ROOF, -8, 1.0);
        register(context, Noises.BADLANDS_SURFACE, -6, 1.0, 1.0, 1.0);
        register(context, Noises.ICEBERG_PILLAR, -6, 1.0, 1.0, 1.0, 1.0);
        register(context, Noises.ICEBERG_PILLAR_ROOF, -3, 1.0);
        register(context, Noises.ICEBERG_SURFACE, -6, 1.0, 1.0, 1.0);
        register(context, Noises.SULFUR_CAVE_GRADIENT, -5, 1.0, 0.0, 1.0);
        register(context, Noises.SWAMP, -2, 1.0);
        register(context, Noises.CALCITE, -9, 1.0, 1.0, 1.0, 1.0);
        register(context, Noises.GRAVEL, -8, 1.0, 1.0, 1.0, 1.0);
        register(context, Noises.POWDER_SNOW, -6, 1.0, 1.0, 1.0, 1.0);
        register(context, Noises.PACKED_ICE, -7, 1.0, 1.0, 1.0, 1.0);
        register(context, Noises.ICE, -4, 1.0, 1.0, 1.0, 1.0);
        register(context, Noises.SOUL_SAND_LAYER, -8, 1.0, 1.0, 1.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.013333333333333334);
        register(context, Noises.GRAVEL_LAYER, -8, 1.0, 1.0, 1.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.013333333333333334);
        register(context, Noises.PATCH, -5, 1.0, 0.0, 0.0, 0.0, 0.0, 0.013333333333333334);
        register(context, Noises.NETHERRACK, -3, 1.0, 0.0, 0.0, 0.35);
        register(context, Noises.NETHER_WART, -3, 1.0, 0.0, 0.0, 0.9);
        register(context, Noises.NETHER_STATE_SELECTOR, -4, 1.0);
    }

    private static void registerBiomeNoises(
        final BootstrapContext<NormalNoise.NoiseParameters> context,
        final int octaveOffset,
        final ResourceKey<NormalNoise.NoiseParameters> temperature,
        final ResourceKey<NormalNoise.NoiseParameters> vegetation,
        final ResourceKey<NormalNoise.NoiseParameters> continentalness,
        final ResourceKey<NormalNoise.NoiseParameters> erosion
    ) {
        register(context, temperature, -10 + octaveOffset, 1.5, 0.0, 1.0, 0.0, 0.0, 0.0);
        register(context, vegetation, -8 + octaveOffset, 1.0, 1.0, 0.0, 0.0, 0.0, 0.0);
        register(context, continentalness, -9 + octaveOffset, 1.0, 1.0, 2.0, 2.0, 2.0, 1.0, 1.0, 1.0, 1.0);
        register(context, erosion, -9 + octaveOffset, 1.0, 1.0, 0.0, 1.0, 1.0);
    }

    private static void register(
        final BootstrapContext<NormalNoise.NoiseParameters> context,
        final ResourceKey<NormalNoise.NoiseParameters> key,
        final int firstOctave,
        final double firstAmplitude,
        final double... amplitudes
    ) {
        context.register(key, new NormalNoise.NoiseParameters(firstOctave, firstAmplitude, amplitudes));
    }
}