package net.minecraft.world.level.levelgen;

import java.util.function.LongFunction;
import net.minecraft.util.RandomSource;

public class WorldgenRandom extends LegacyRandomSource {
   private final RandomSource randomSource;
   private int count;

   public WorldgenRandom(RandomSource p_224680_) {
      super(0L);
      this.randomSource = p_224680_;
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
   public int next(int p_64708_) {
      this.count++;
      return this.randomSource instanceof LegacyRandomSource legacyrandomsource
         ? legacyrandomsource.next(p_64708_)
         : (int)(this.randomSource.nextLong() >>> 64 - p_64708_);
   }

   @Override
   public synchronized void setSeed(long p_190073_) {
      if (this.randomSource != null) {
         this.randomSource.setSeed(p_190073_);
      }
   }

   public long setDecorationSeed(long p_64691_, int p_64692_, int p_64693_) {
      this.setSeed(p_64691_);
      long i = this.nextLong() | 1L;
      long j = this.nextLong() | 1L;
      long k = p_64692_ * i + p_64693_ * j ^ p_64691_;
      this.setSeed(k);
      return k;
   }

   public void setFeatureSeed(long p_190065_, int p_190066_, int p_190067_) {
      long i = p_190065_ + p_190066_ + 10000 * p_190067_;
      this.setSeed(i);
   }

   public void setLargeFeatureSeed(long p_190069_, int p_190070_, int p_190071_) {
      this.setSeed(p_190069_);
      long i = this.nextLong();
      long j = this.nextLong();
      long k = p_190070_ * i ^ p_190071_ * j ^ p_190069_;
      this.setSeed(k);
   }

   public void setLargeFeatureWithSalt(long p_190059_, int p_190060_, int p_190061_, int p_190062_) {
      long i = p_190060_ * 341873128712L + p_190061_ * 132897987541L + p_190059_ + p_190062_;
      this.setSeed(i);
   }

   public static RandomSource seedSlimeChunk(int p_224682_, int p_224683_, long p_224684_, long p_224685_) {
      return RandomSource.create(
         p_224684_ + p_224682_ * p_224682_ * 4987142 + p_224682_ * 5947611 + p_224683_ * p_224683_ * 4392871L + p_224683_ * 389711 ^ p_224685_
      );
   }

   public enum Algorithm {
      LEGACY(LegacyRandomSource::new),
      XOROSHIRO(XoroshiroRandomSource::new);

      private final LongFunction<RandomSource> constructor;

      Algorithm(final LongFunction<RandomSource> p_190082_) {
         this.constructor = p_190082_;
      }

      public RandomSource newInstance(long p_224688_) {
         return this.constructor.apply(p_224688_);
      }
   }
}
