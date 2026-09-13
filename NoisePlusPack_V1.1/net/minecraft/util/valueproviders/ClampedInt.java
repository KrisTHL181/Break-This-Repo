package net.minecraft.util.valueproviders;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

public class ClampedInt extends IntProvider {
   public static final MapCodec<ClampedInt> CODEC = RecordCodecBuilder.mapCodec(
         p_146400_ -> p_146400_.group(
               IntProvider.CODEC.fieldOf("source").forGetter(p_146410_ -> p_146410_.source),
               Codec.INT.fieldOf("min_inclusive").forGetter(p_146408_ -> p_146408_.minInclusive),
               Codec.INT.fieldOf("max_inclusive").forGetter(p_146405_ -> p_146405_.maxInclusive)
            )
            .apply(p_146400_, ClampedInt::new)
      )
      .validate(
         p_274932_ -> p_274932_.maxInclusive < p_274932_.minInclusive
            ? DataResult.error(() -> "Max must be at least min, min_inclusive: " + p_274932_.minInclusive + ", max_inclusive: " + p_274932_.maxInclusive)
            : DataResult.success(p_274932_)
      );
   private final IntProvider source;
   private final int minInclusive;
   private final int maxInclusive;

   public static ClampedInt of(IntProvider p_146396_, int p_146397_, int p_146398_) {
      return new ClampedInt(p_146396_, p_146397_, p_146398_);
   }

   public ClampedInt(IntProvider p_146389_, int p_146390_, int p_146391_) {
      this.source = p_146389_;
      this.minInclusive = p_146390_;
      this.maxInclusive = p_146391_;
   }

   @Override
   public int sample(RandomSource p_216834_) {
      return Mth.clamp(this.source.sample(p_216834_), this.minInclusive, this.maxInclusive);
   }

   @Override
   public int getMinValue() {
      return Math.max(this.minInclusive, this.source.getMinValue());
   }

   @Override
   public int getMaxValue() {
      return Math.min(this.maxInclusive, this.source.getMaxValue());
   }

   @Override
   public IntProviderType<?> getType() {
      return IntProviderType.CLAMPED;
   }
}
