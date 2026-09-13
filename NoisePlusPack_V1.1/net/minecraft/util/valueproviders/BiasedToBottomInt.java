package net.minecraft.util.valueproviders;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.RandomSource;

public class BiasedToBottomInt extends IntProvider {
   public static final MapCodec<BiasedToBottomInt> CODEC = RecordCodecBuilder.mapCodec(
         p_146373_ -> p_146373_.group(
               Codec.INT.fieldOf("min_inclusive").forGetter(p_146381_ -> p_146381_.minInclusive),
               Codec.INT.fieldOf("max_inclusive").forGetter(p_146378_ -> p_146378_.maxInclusive)
            )
            .apply(p_146373_, BiasedToBottomInt::new)
      )
      .validate(
         p_274930_ -> p_274930_.maxInclusive < p_274930_.minInclusive
            ? DataResult.error(() -> "Max must be at least min, min_inclusive: " + p_274930_.minInclusive + ", max_inclusive: " + p_274930_.maxInclusive)
            : DataResult.success(p_274930_)
      );
   private final int minInclusive;
   private final int maxInclusive;

   private BiasedToBottomInt(int p_146364_, int p_146365_) {
      this.minInclusive = p_146364_;
      this.maxInclusive = p_146365_;
   }

   public static BiasedToBottomInt of(int p_146368_, int p_146369_) {
      return new BiasedToBottomInt(p_146368_, p_146369_);
   }

   @Override
   public int sample(RandomSource p_216832_) {
      return this.minInclusive + p_216832_.nextInt(p_216832_.nextInt(this.maxInclusive - this.minInclusive + 1) + 1);
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
      return IntProviderType.BIASED_TO_BOTTOM;
   }

   @Override
   public String toString() {
      return "[" + this.minInclusive + "-" + this.maxInclusive + "]";
   }
}
