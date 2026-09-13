package net.minecraft.world.level.levelgen;

import net.minecraft.util.RandomSource;
import org.jspecify.annotations.Nullable;

public class SingleThreadedRandomSource implements BitRandomSource {
   private static final int MODULUS_BITS = 48;
   private static final long MODULUS_MASK = 281474976710655L;
   private static final long MULTIPLIER = 25214903917L;
   private static final long INCREMENT = 11L;
   private long seed;
   private @Nullable MarsagliaPolarGaussian gaussianSource;

   public SingleThreadedRandomSource(final long seed) {
      this.setSeed(seed);
   }

   @Override
   public RandomSource fork() {
      return new SingleThreadedRandomSource(this.nextLong());
   }

   @Override
   public PositionalRandomFactory forkPositional() {
      return new LegacyRandomSource.LegacyPositionalRandomFactory(this.nextLong());
   }

   @Override
   public void setSeed(final long seed) {
      this.seed = (seed ^ 25214903917L) & 281474976710655L;
      if (this.gaussianSource != null) {
         this.gaussianSource.reset();
      }
   }

   @Override
   public int next(final int bits) {
      long newSeed = this.seed * 25214903917L + 11L & 281474976710655L;
      this.seed = newSeed;
      return (int)(newSeed >> 48 - bits);
   }

   @Override
   public double nextGaussian() {
      if (this.gaussianSource == null) {
         this.gaussianSource = new MarsagliaPolarGaussian(this);
      }

      return this.gaussianSource.nextGaussian();
   }
}
