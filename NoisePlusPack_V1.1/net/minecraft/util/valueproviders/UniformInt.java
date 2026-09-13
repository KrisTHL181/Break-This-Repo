package net.minecraft.util.valueproviders;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

public class UniformInt extends IntProvider {
   public static final MapCodec<UniformInt> CODEC = RecordCodecBuilder.mapCodec(
         p_146628_ -> p_146628_.group(
               Codec.INT.fieldOf("min_inclusive").forGetter(p_146636_ -> p_146636_.minInclusive),
               Codec.INT.fieldOf("max_inclusive").forGetter(p_146633_ -> p_146633_.maxInclusive)
            )
            .apply(p_146628_, UniformInt::new)
      )
      .validate(
         p_274957_ -> p_274957_.maxInclusive < p_274957_.minInclusive
            ? DataResult.error(() -> "Max must be at least min, min_inclusive: " + p_274957_.minInclusive + ", max_inclusive: " + p_274957_.maxInclusive)
            : DataResult.success(p_274957_)
      );
   private final int minInclusive;
   private final int maxInclusive;

   private UniformInt(int p_146619_, int p_146620_) {
      this.minInclusive = p_146619_;
      this.maxInclusive = p_146620_;
   }

   public static UniformInt of(int p_146623_, int p_146624_) {
      return new UniformInt(p_146623_, p_146624_);
   }

   @Override
   public int sample(RandomSource p_216868_) {
      return Mth.randomBetweenInclusive(p_216868_, this.minInclusive, this.maxInclusive);
   }

   @Override
   public int getMinValue() {
      return this.minInclusive;
   }

   @Override
   public int getMaxValue() {
      return this.maxInclusive;
   }

   @Override
   public IntProviderType<?> getType() {
      return IntProviderType.UNIFORM;
   }

   @Override
   public String toString() {
      return "[" + this.minInclusive + "-" + this.maxInclusive + "]";
   }
}
