package net.minecraft.util.valueproviders;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

public record UniformInt(int minInclusive, int maxInclusive) implements IntProvider {
   public static final MapCodec<UniformInt> MAP_CODEC = RecordCodecBuilder.mapCodec(
         i -> i.group(
               Codec.INT.fieldOf("min_inclusive").forGetter(UniformInt::minInclusive), Codec.INT.fieldOf("max_inclusive").forGetter(UniformInt::maxInclusive)
            )
            .apply(i, UniformInt::new)
      )
      .validate(
         u -> u.maxInclusive < u.minInclusive
            ? DataResult.error(() -> "Max must be at least min, min_inclusive: " + u.minInclusive + ", max_inclusive: " + u.maxInclusive)
            : DataResult.success(u)
      );

   public static UniformInt of(final int minInclusive, final int maxInclusive) {
      return new UniformInt(minInclusive, maxInclusive);
   }

   @Override
   public int sample(final RandomSource random) {
      return Mth.randomBetweenInclusive(random, this.minInclusive, this.maxInclusive);
   }

   @Override
   public MapCodec<UniformInt> codec() {
      return MAP_CODEC;
   }

   @Override
   public String toString() {
      return "[" + this.minInclusive + "-" + this.maxInclusive + "]";
   }
}
