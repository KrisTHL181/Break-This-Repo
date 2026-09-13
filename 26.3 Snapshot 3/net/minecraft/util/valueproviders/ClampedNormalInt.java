package net.minecraft.util.valueproviders;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

public record ClampedNormalInt(float mean, float deviation, int minInclusive, int maxInclusive) implements IntProvider {
   public static final MapCodec<ClampedNormalInt> MAP_CODEC = RecordCodecBuilder.mapCodec(
         i -> i.group(
               Codec.FLOAT.fieldOf("mean").forGetter(ClampedNormalInt::mean),
               Codec.FLOAT.fieldOf("deviation").forGetter(ClampedNormalInt::deviation),
               Codec.INT.fieldOf("min_inclusive").forGetter(ClampedNormalInt::minInclusive),
               Codec.INT.fieldOf("max_inclusive").forGetter(ClampedNormalInt::maxInclusive)
            )
            .apply(i, ClampedNormalInt::new)
      )
      .validate(
         c -> c.maxInclusive < c.minInclusive
            ? DataResult.error(() -> "Max must be larger than min: [" + c.minInclusive + ", " + c.maxInclusive + "]")
            : DataResult.success(c)
      );

   public static ClampedNormalInt of(final float mean, final float deviation, final int minInclusive, final int maxInclusive) {
      return new ClampedNormalInt(mean, deviation, minInclusive, maxInclusive);
   }

   @Override
   public int sample(final RandomSource random) {
      return sample(random, this.mean, this.deviation, this.minInclusive, this.maxInclusive);
   }

   public static int sample(final RandomSource random, final float mean, final float deviation, final float minInclusive, final float maxInclusive) {
      return (int)Mth.clamp(Mth.normal(random, mean, deviation), minInclusive, maxInclusive);
   }

   @Override
   public MapCodec<ClampedNormalInt> codec() {
      return MAP_CODEC;
   }

   @Override
   public String toString() {
      return "normal(" + this.mean + ", " + this.deviation + ") in [" + this.minInclusive + "-" + this.maxInclusive + "]";
   }
}
