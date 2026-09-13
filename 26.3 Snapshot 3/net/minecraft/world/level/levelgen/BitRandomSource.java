package net.minecraft.world.level.levelgen;

import net.minecraft.util.RandomSource;

public interface BitRandomSource extends RandomSource {
   float FLOAT_MULTIPLIER = 5.9604645E-8F;
   double DOUBLE_MULTIPLIER = 1.110223E-16F;

   int next(final int bits);

   @Override
   default int nextInt() {
      return this.next(32);
   }

   @Override
   default int nextInt(final int bound) {
      if (bound <= 0) {
         throw new IllegalArgumentException("Bound must be positive");
      }

      if ((bound & bound - 1) == 0) {
         return (int)((long)bound * this.next(31) >> 31);
      }

      int sample;
      int modulo;
      do {
         sample = this.next(31);
         modulo = sample % bound;
      } while (sample - modulo + (bound - 1) < 0);

      return modulo;
   }

   @Override
   default long nextLong() {
      int upper = this.next(32);
      int lower = this.next(32);
      long shifted = (long)upper << 32;
      return shifted + lower;
   }

   @Override
   default boolean nextBoolean() {
      return this.next(1) != 0;
   }

   @Override
   default float nextFloat() {
      return this.next(24) * 5.9604645E-8F;
   }

   @Override
   default double nextDouble() {
      int upper = this.next(26);
      int lower = this.next(27);
      long combined = ((long)upper << 27) + lower;
      return combined * 1.110223E-16F;
   }
}
