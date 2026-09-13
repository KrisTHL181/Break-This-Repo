package net.minecraft.world.level.levelgen;

import com.google.common.annotations.VisibleForTesting;
import com.mojang.serialization.Codec;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

public class XoroshiroRandomSource implements RandomSource {
   private static final float FLOAT_UNIT = 5.9604645E-8F;
   private static final double DOUBLE_UNIT = 1.110223E-16F;
   public static final Codec<XoroshiroRandomSource> CODEC = Xoroshiro128PlusPlus.CODEC
      .xmap(generator -> new XoroshiroRandomSource(generator), source -> source.randomNumberGenerator);
   private Xoroshiro128PlusPlus randomNumberGenerator;
   private final MarsagliaPolarGaussian gaussianSource = new MarsagliaPolarGaussian(this);

   public XoroshiroRandomSource(final long seed) {
      this.randomNumberGenerator = new Xoroshiro128PlusPlus(RandomSupport.upgradeSeedTo128bit(seed));
   }

   public XoroshiroRandomSource(final RandomSupport.Seed128bit seed) {
      this.randomNumberGenerator = new Xoroshiro128PlusPlus(seed);
   }

   public XoroshiroRandomSource(final long seedLo, final long seedHi) {
      this.randomNumberGenerator = new Xoroshiro128PlusPlus(seedLo, seedHi);
   }

   private XoroshiroRandomSource(final Xoroshiro128PlusPlus randomNumberGenerator) {
      this.randomNumberGenerator = randomNumberGenerator;
   }

   @Override
   public RandomSource fork() {
      return new XoroshiroRandomSource(this.randomNumberGenerator.nextLong(), this.randomNumberGenerator.nextLong());
   }

   @Override
   public PositionalRandomFactory forkPositional() {
      return new XoroshiroRandomSource.XoroshiroPositionalRandomFactory(this.randomNumberGenerator.nextLong(), this.randomNumberGenerator.nextLong());
   }

   @Override
   public void setSeed(final long seed) {
      this.randomNumberGenerator = new Xoroshiro128PlusPlus(RandomSupport.upgradeSeedTo128bit(seed));
      this.gaussianSource.reset();
   }

   @Override
   public int nextInt() {
      return (int)this.randomNumberGenerator.nextLong();
   }

   @Override
   public int nextInt(final int bound) {
      if (bound <= 0) {
         throw new IllegalArgumentException("Bound must be positive");
      }

      long randomBits = Integer.toUnsignedLong(this.nextInt());
      long multipliedRandomBits = randomBits * bound;
      long fractionalPart = multipliedRandomBits & 4294967295L;
      if (fractionalPart < bound) {
         for (int unbiasedBucketsStartIndex = Integer.remainderUnsigned(~bound + 1, bound);
            fractionalPart < unbiasedBucketsStartIndex;
            fractionalPart = multipliedRandomBits & 4294967295L
         ) {
            randomBits = Integer.toUnsignedLong(this.nextInt());
            multipliedRandomBits = randomBits * bound;
         }
      }

      long integerPart = multipliedRandomBits >> 32;
      return (int)integerPart;
   }

   @Override
   public long nextLong() {
      return this.randomNumberGenerator.nextLong();
   }

   @Override
   public boolean nextBoolean() {
      return (this.randomNumberGenerator.nextLong() & 1L) != 0L;
   }

   @Override
   public float nextFloat() {
      return (float)this.nextBits(24) * 5.9604645E-8F;
   }

   @Override
   public double nextDouble() {
      return this.nextBits(53) * 1.110223E-16F;
   }

   @Override
   public double nextGaussian() {
      return this.gaussianSource.nextGaussian();
   }

   @Override
   public void consumeCount(final int rounds) {
      for (int i = 0; i < rounds; i++) {
         this.randomNumberGenerator.nextLong();
      }
   }

   private long nextBits(final int bits) {
      return this.randomNumberGenerator.nextLong() >>> 64 - bits;
   }

   public static class XoroshiroPositionalRandomFactory implements PositionalRandomFactory {
      private final long seedLo;
      private final long seedHi;

      public XoroshiroPositionalRandomFactory(final long seedLo, final long seedHi) {
         this.seedLo = seedLo;
         this.seedHi = seedHi;
      }

      @Override
      public RandomSource at(final int x, final int y, final int z) {
         long positionalSeed = Mth.getSeed(x, y, z);
         long randomSeed = positionalSeed ^ this.seedLo;
         return new XoroshiroRandomSource(randomSeed, this.seedHi);
      }

      @Override
      public RandomSource fromHashOf(final String name) {
         RandomSupport.Seed128bit seed = RandomSupport.seedFromHashOf(name);
         return new XoroshiroRandomSource(seed.xor(this.seedLo, this.seedHi));
      }

      @Override
      public RandomSource fromSeed(final long seed) {
         return new XoroshiroRandomSource(seed ^ this.seedLo, seed ^ this.seedHi);
      }

      @VisibleForTesting
      @Override
      public void parityConfigString(final StringBuilder sb) {
         sb.append("seedLo: ").append(this.seedLo).append(", seedHi: ").append(this.seedHi);
      }
   }
}
