package net.minecraft.util.valueproviders;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.RandomSource;

public record BiasedToBottomInt(int minInclusive, int maxInclusive) implements IntProvider {
   public static final MapCodec<BiasedToBottomInt> MAP_CODEC = RecordCodecBuilder.mapCodec(
         i -> i.group(
               Codec.INT.fieldOf("min_inclusive").forGetter(BiasedToBottomInt::minInclusive),
               Codec.INT.fieldOf("max_inclusive").forGetter(BiasedToBottomInt::maxInclusive)
            )
            .apply(i, BiasedToBottomInt::new)
      )
      .validate(
         u -> u.maxInclusive < u.minInclusive
            ? DataResult.error(() -> "Max must be at least min, min_inclusive: " + u.minInclusive + ", max_inclusive: " + u.maxInclusive)
            : DataResult.success(u)
      );

   public static BiasedToBottomInt of(final int minInclusive, final int maxInclusive) {
      return new BiasedToBottomInt(minInclusive, maxInclusive);
   }

   @Override
   public int sample(final RandomSource random) {
      return this.minInclusive + random.nextInt(random.nextInt(this.maxInclusive - this.minInclusive + 1) + 1);
   }

   @Override
   public MapCodec<BiasedToBottomInt> codec() {
      return MAP_CODEC;
   }

   @Override
   public String toString() {
      return "[" + this.minInclusive + "-" + this.maxInclusive + "]";
   }
}
