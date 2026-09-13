package net.minecraft.world.level.levelgen.placement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;

public record NoiseThresholdCountPlacement(double noiseLevel, int belowNoise, int aboveNoise) implements RepeatingPlacement {
   public static final MapCodec<NoiseThresholdCountPlacement> CODEC = RecordCodecBuilder.mapCodec(
      i -> i.group(
            Codec.DOUBLE.fieldOf("noise_level").forGetter(NoiseThresholdCountPlacement::noiseLevel),
            Codec.INT.fieldOf("below_noise").forGetter(NoiseThresholdCountPlacement::belowNoise),
            Codec.INT.fieldOf("above_noise").forGetter(NoiseThresholdCountPlacement::aboveNoise)
         )
         .apply(i, NoiseThresholdCountPlacement::new)
   );

   public static NoiseThresholdCountPlacement of(final double noiseLevel, final int belowNoise, final int aboveNoise) {
      return new NoiseThresholdCountPlacement(noiseLevel, belowNoise, aboveNoise);
   }

   @Override
   public int count(final RandomSource random, final BlockPos origin) {
      double flowerNoise = Biome.BIOME_INFO_NOISE.getValue(origin.getX() / 200.0, origin.getZ() / 200.0, false);
      return flowerNoise < this.noiseLevel ? this.belowNoise : this.aboveNoise;
   }

   @Override
   public MapCodec<NoiseThresholdCountPlacement> codec() {
      return CODEC;
   }
}
