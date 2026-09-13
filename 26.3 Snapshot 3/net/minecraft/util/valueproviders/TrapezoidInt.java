package net.minecraft.util.valueproviders;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

public record TrapezoidInt(int minInclusive, int maxInclusive, int plateau) implements IntProvider {
   public static final MapCodec<TrapezoidInt> MAP_CODEC = RecordCodecBuilder.mapCodec(
         i -> i.group(
               Codec.INT.fieldOf("min").forGetter(TrapezoidInt::minInclusive),
               Codec.INT.fieldOf("max").forGetter(TrapezoidInt::maxInclusive),
               Codec.INT.fieldOf("plateau").forGetter(TrapezoidInt::plateau)
            )
            .apply(i, TrapezoidInt::new)
      )
      .validate(
         c -> {
            if (c.maxInclusive < c.minInclusive) {
               return DataResult.error(() -> "Max must be larger than min: [" + c.minInclusive + ", " + c.maxInclusive + "]");
            } else {
               return c.plateau > c.maxInclusive - c.minInclusive
                  ? DataResult.error(() -> "Plateau can at most be the full span: [" + c.minInclusive + ", " + c.maxInclusive + "]")
                  : DataResult.success(c);
            }
         }
      );

   public static TrapezoidInt of(final int min, final int max, final int plateau) {
      return new TrapezoidInt(min, max, plateau);
   }

   public static IntProvider triangle(final int range) {
      return of(-range, range, 0);
   }

   @Override
   public int sample(final RandomSource random) {
      if (this.plateau == 0 && this.maxInclusive == -this.minInclusive) {
         return random.nextInt(this.maxInclusive + 1) - random.nextInt(this.maxInclusive + 1);
      }

      int range = this.maxInclusive - this.minInclusive;
      if (this.plateau == range) {
         return Mth.randomBetweenInclusive(random, this.minInclusive, this.maxInclusive);
      }

      int plateauStart = (range - this.plateau) / 2;
      int plateauEnd = range - plateauStart;
      return this.minInclusive + Mth.randomBetweenInclusive(random, 0, plateauEnd) + Mth.randomBetweenInclusive(random, 0, plateauStart);
   }

   @Override
   public MapCodec<TrapezoidInt> codec() {
      return MAP_CODEC;
   }

   @Override
   public String toString() {
      return "trapezoid(" + this.plateau + ") in [" + this.minInclusive + "-" + this.maxInclusive + "]";
   }
}
