package net.minecraft.util.valueproviders;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

public record ClampedInt(IntProvider source, int minInclusive, int maxInclusive) implements IntProvider {
   public static final MapCodec<ClampedInt> MAP_CODEC = RecordCodecBuilder.mapCodec(
         i -> i.group(
               IntProviders.CODEC.fieldOf("source").forGetter(ClampedInt::source),
               Codec.INT.fieldOf("min_inclusive").forGetter(ClampedInt::minInclusive),
               Codec.INT.fieldOf("max_inclusive").forGetter(ClampedInt::maxInclusive)
            )
            .apply(i, ClampedInt::new)
      )
      .validate(
         u -> u.maxInclusive < u.minInclusive
            ? DataResult.error(() -> "Max must be at least min, min_inclusive: " + u.minInclusive + ", max_inclusive: " + u.maxInclusive)
            : DataResult.success(u)
      );

   public static ClampedInt of(final IntProvider source, final int minInclusive, final int maxInclusive) {
      return new ClampedInt(source, minInclusive, maxInclusive);
   }

   @Override
   public int sample(final RandomSource random) {
      return Mth.clamp(this.source.sample(random), this.minInclusive, this.maxInclusive);
   }

   @Override
   public int minInclusive() {
      return Math.max(this.minInclusive, this.source.minInclusive());
   }

   @Override
   public int maxInclusive() {
      return Math.min(this.maxInclusive, this.source.maxInclusive());
   }

   @Override
   public MapCodec<ClampedInt> codec() {
      return MAP_CODEC;
   }
}
