package net.minecraft.world.level.levelgen;

import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.util.RandomSource;

@Deprecated
public class ThreadSafeLegacyRandomSource implements BitRandomSource {
   private static final int MODULUS_BITS = 48;
   private static final long MODULUS_MASK = 281474976710655L;
   private static final long MULTIPLIER = 25214903917L;
   private static final long INCREMENT = 11L;
   private final AtomicLong seed = new AtomicLong();
   private final MarsagliaPolarGaussian gaussianSource = new MarsagliaPolarGaussian(this);

   public ThreadSafeLegacyRandomSource(final long seed) {
      this.setSeed(seed);
   }

   @Override
   public RandomSource fork() {
      return new ThreadSafeLegacyRandomSource(this.nextLong());
   }

   @Override
   public PositionalRandomFactory forkPositional() {
      return new LegacyRandomSource.LegacyPositionalRandomFactory(this.nextLong());
   }

   @Override
   public void setSeed(final long seed) {
      this.seed.set((seed ^ 25214903917L) & 281474976710655L);
   }

   @Override
   public int next(final int bits) {
      long oldSeed;
      long nextSeed;
      do {
         oldSeed = this.seed.get();
         nextSeed = oldSeed * 25214903917L + 11L & 281474976710655L;
      } while (!this.seed.compareAndSet(oldSeed, nextSeed));

      return (int)(nextSeed >>> 48 - bits);
   }

   @Override
   public double nextGaussian() {
      return this.gaussianSource.nextGaussian();
   }
}
