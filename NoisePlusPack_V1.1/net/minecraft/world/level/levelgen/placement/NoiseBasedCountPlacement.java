package net.minecraft.world.level.levelgen.placement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;

public class NoiseBasedCountPlacement extends RepeatingPlacement {
   public static final MapCodec<NoiseBasedCountPlacement> CODEC = RecordCodecBuilder.mapCodec(
      p_191736_ -> p_191736_.group(
            Codec.INT.fieldOf("noise_to_count_ratio").forGetter(p_191746_ -> p_191746_.noiseToCountRatio),
            Codec.DOUBLE.fieldOf("noise_factor").forGetter(p_191744_ -> p_191744_.noiseFactor),
            Codec.DOUBLE.fieldOf("noise_offset").orElse(0.0).forGetter(p_191738_ -> p_191738_.noiseOffset)
         )
         .apply(p_191736_, NoiseBasedCountPlacement::new)
   );
   private final int noiseToCountRatio;
   private final double noiseFactor;
   private final double noiseOffset;

   private NoiseBasedCountPlacement(int p_191728_, double p_191729_, double p_191730_) {
      this.noiseToCountRatio = p_191728_;
      this.noiseFactor = p_191729_;
      this.noiseOffset = p_191730_;
   }

   public static NoiseBasedCountPlacement of(int p_191732_, double p_191733_, double p_191734_) {
      return new NoiseBasedCountPlacement(p_191732_, p_191733_, p_191734_);
   }

   @Override
   protected int count(RandomSource p_226352_, BlockPos p_226353_) {
      double d0 = Biome.BIOME_INFO_NOISE.getValue(p_226353_.getX() / this.noiseFactor, p_226353_.getZ() / this.noiseFactor, false);
      return (int)Math.ceil((d0 + this.noiseOffset) * this.noiseToCountRatio);
   }

   @Override
   public PlacementModifierType<?> type() {
      return PlacementModifierType.NOISE_BASED_COUNT;
   }
}
