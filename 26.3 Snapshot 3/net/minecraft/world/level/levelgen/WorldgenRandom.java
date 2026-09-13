package net.minecraft.world.level.levelgen;

import java.util.function.LongFunction;
import net.minecraft.util.RandomSource;

public class WorldgenRandom extends LegacyRandomSource {
   private final RandomSource randomSource;
   private int count;

   public WorldgenRandom(final RandomSource randomSource) {
      super(0L);
      this.randomSource = randomSource;
   }

   public int getCount() {
      return this.count;
   }

   @Override
   public RandomSource fork() {
      return this.randomSource.fork();
   }

   @Override
   public PositionalRandomFactory forkPositional() {
      return this.randomSource.forkPositional();
   }

   @Override
   public int next(final int bits) {
      this.count++;
      return this.randomSource instanceof LegacyRandomSource legacyRandomSource
         ? legacyRandomSource.next(bits)
         : (int)(this.randomSource.nextLong() >>> 64 - bits);
   }

   @Override
   public synchronized void setSeed(final long seed) {
      if (this.randomSource != null) {
         this.randomSource.setSeed(seed);
      }
   }

   public long setDecorationSeed(final long seed, final int chunkX, final int chunkZ) {
      this.setSeed(seed);
      long xScale = this.nextLong() | 1L;
      long zScale = this.nextLong() | 1L;
      long result = chunkX * xScale + chunkZ * zScale ^ seed;
      this.setSeed(result);
      return result;
   }

   public void setFeatureSeed(final long seed, final int index, final int step) {
      long result = seed + index + 10000 * step;
      this.setSeed(result);
   }

   public void setLargeFeatureSeed(final long seed, final int chunkX, final int chunkZ) {
      this.setSeed(seed);
      long xScale = this.nextLong();
      long zScale = this.nextLong();
      long result = chunkX * xScale ^ chunkZ * zScale ^ seed;
      this.setSeed(result);
   }

   public void setLargeFeatureWithSalt(final long seed, final int x, final int z, final int blend) {
      long result = x * 341873128712L + z * 132897987541L + seed + blend;
      this.setSeed(result);
   }

   public static RandomSource seedSlimeChunk(final int x, final int z, final long seed, final long salt) {
      return RandomSource.createThreadLocalInstance(seed + x * x * 4987142 + x * 5947611 + z * z * 4392871L + z * 389711 ^ salt);
   }

   public enum Algorithm {
      LEGACY(LegacyRandomSource::new),
      XOROSHIRO(XoroshiroRandomSource::new);

      private final LongFunction<RandomSource> constructor;

      Algorithm(final LongFunction<RandomSource> constructor) {
         this.constructor = constructor;
      }

      public RandomSource newInstance(final long seed) {
         return this.constructor.apply(seed);
      }
   }
}
