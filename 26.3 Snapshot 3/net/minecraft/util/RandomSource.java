package net.minecraft.util;

import io.netty.util.internal.ThreadLocalRandom;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.PositionalRandomFactory;
import net.minecraft.world.level.levelgen.RandomSupport;
import net.minecraft.world.level.levelgen.SingleThreadedRandomSource;
import net.minecraft.world.level.levelgen.ThreadSafeLegacyRandomSource;

public interface RandomSource {
   @Deprecated
   double GAUSSIAN_SPREAD_FACTOR = 2.297;

   static RandomSource create() {
      return create(RandomSupport.generateUniqueSeed());
   }

   @Deprecated
   static RandomSource createThreadSafe() {
      return new ThreadSafeLegacyRandomSource(RandomSupport.generateUniqueSeed());
   }

   static RandomSource create(final long seed) {
      return new LegacyRandomSource(seed);
   }

   static RandomSource createThreadLocalInstance() {
      return new SingleThreadedRandomSource(ThreadLocalRandom.current().nextLong());
   }

   static RandomSource createThreadLocalInstance(final long seed) {
      return new SingleThreadedRandomSource(seed);
   }

   RandomSource fork();

   PositionalRandomFactory forkPositional();

   void setSeed(long seed);

   int nextInt();

   int nextInt(int bound);

   default int nextIntBetweenInclusive(final int min, final int maxInclusive) {
      return this.nextInt(maxInclusive - min + 1) + min;
   }

   long nextLong();

   boolean nextBoolean();

   float nextFloat();

   double nextDouble();

   double nextGaussian();

   default double triangle(final double mean, final double spread) {
      return mean + spread * (this.nextDouble() - this.nextDouble());
   }

   default float triangle(final float mean, final float spread) {
      return mean + spread * (this.nextFloat() - this.nextFloat());
   }

   default void consumeCount(final int rounds) {
      for (int i = 0; i < rounds; i++) {
         this.nextInt();
      }
   }

   default int nextInt(final int origin, final int bound) {
      if (origin >= bound) {
         throw new IllegalArgumentException("bound - origin is non positive");
      } else {
         return origin + this.nextInt(bound - origin);
      }
   }
}
