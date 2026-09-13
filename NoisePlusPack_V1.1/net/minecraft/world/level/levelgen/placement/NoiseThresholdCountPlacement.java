package net.minecraft.world.level.levelgen.placement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;

public class NoiseThresholdCountPlacement extends RepeatingPlacement {
   public static final MapCodec<NoiseThresholdCountPlacement> CODEC = RecordCodecBuilder.mapCodec(
      p_191761_ -> p_191761_.group(
            Codec.DOUBLE.fieldOf("noise_level").forGetter(p_191771_ -> p_191771_.noiseLevel),
            Codec.INT.fieldOf("below_noise").forGetter(p_191769_ -> p_191769_.belowNoise),
            Codec.INT.fieldOf("above_noise").forGetter(p_191763_ -> p_191763_.aboveNoise)
         )
         .apply(p_191761_, NoiseThresholdCountPlacement::new)
   );
   private final double noiseLevel;
   private final int belowNoise;
   private final int aboveNoise;

   private NoiseThresholdCountPlacement(double p_191753_, int p_191754_, int p_191755_) {
      this.noiseLevel = p_191753_;
      this.belowNoise = p_191754_;
      this.aboveNoise = p_191755_;
   }

   public static NoiseThresholdCountPlacement of(double p_191757_, int p_191758_, int p_191759_) {
      return new NoiseThresholdCountPlacement(p_191757_, p_191758_, p_191759_);
   }

   @Override
   protected int count(RandomSource p_226355_, BlockPos p_226356_) {
      double d0 = Biome.BIOME_INFO_NOISE.getValue(p_226356_.getX() / 200.0, p_226356_.getZ() / 200.0, false);
      return d0 < this.noiseLevel ? this.belowNoise : this.aboveNoise;
   }

   @Override
   public PlacementModifierType<?> type() {
      return PlacementModifierType.NOISE_THRESHOLD_COUNT;
   }
}
