package net.minecraft.world.level.levelgen;

import com.google.common.annotations.VisibleForTesting;
import com.mojang.serialization.Codec;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

public class XoroshiroRandomSource implements RandomSource {
   private static final float FLOAT_UNIT = 5.9604645E-8F;
   private static final double DOUBLE_UNIT = 1.110223E-16F;
   public static final Codec<XoroshiroRandomSource> CODEC = Xoroshiro128PlusPlus.CODEC
      .xmap(p_287645_ -> new XoroshiroRandomSource(p_287645_), p_287690_ -> p_287690_.randomNumberGenerator);
   private Xoroshiro128PlusPlus randomNumberGenerator;
   private final MarsagliaPolarGaussian gaussianSource = new MarsagliaPolarGaussian(this);

   public XoroshiroRandomSource(long p_190102_) {
      this.randomNumberGenerator = new Xoroshiro128PlusPlus(RandomSupport.upgradeSeedTo128bit(p_190102_));
   }

   public XoroshiroRandomSource(RandomSupport.Seed128bit p_289014_) {
      this.randomNumberGenerator = new Xoroshiro128PlusPlus(p_289014_);
   }

   public XoroshiroRandomSource(long p_190104_, long p_190105_) {
      this.randomNumberGenerator = new Xoroshiro128PlusPlus(p_190104_, p_190105_);
   }

   private XoroshiroRandomSource(Xoroshiro128PlusPlus p_287656_) {
      this.randomNumberGenerator = p_287656_;
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
   public void setSeed(long p_190121_) {
      this.randomNumberGenerator = new Xoroshiro128PlusPlus(RandomSupport.upgradeSeedTo128bit(p_190121_));
      this.gaussianSource.reset();
   }

   @Override
   public int nextInt() {
      return (int)this.randomNumberGenerator.nextLong();
   }

   @Override
   public int nextInt(int p_190118_) {
      if (p_190118_ <= 0) {
         throw new IllegalArgumentException("Bound must be positive");
      }

      long i = Integer.toUnsignedLong(this.nextInt());
      long j = i * p_190118_;
      long k = j & 4294967295L;
      if (k < p_190118_) {
         for (int l = Integer.remainderUnsigned(~p_190118_ + 1, p_190118_); k < l; k = j & 4294967295L) {
            i = Integer.toUnsignedLong(this.nextInt());
            j = i * p_190118_;
         }
      }

      long i1 = j >> 32;
      return (int)i1;
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
      return (float)this.nextBits(53) * 1.110223E-16F;
   }

   @Override
   public double nextGaussian() {
      return this.gaussianSource.nextGaussian();
   }

   @Override
   public void consumeCount(int p_190111_) {
      for (int i = 0; i < p_190111_; i++) {
         this.randomNumberGenerator.nextLong();
      }
   }

   private long nextBits(int p_190108_) {
      return this.randomNumberGenerator.nextLong() >>> 64 - p_190108_;
   }

   public static class XoroshiroPositionalRandomFactory implements PositionalRandomFactory {
      private final long seedLo;
      private final long seedHi;

      public XoroshiroPositionalRandomFactory(long p_190127_, long p_190128_) {
         this.seedLo = p_190127_;
         this.seedHi = p_190128_;
      }

      @Override
      public RandomSource at(int p_224691_, int p_224692_, int p_224693_) {
         long i = Mth.getSeed(p_224691_, p_224692_, p_224693_);
         long j = i ^ this.seedLo;
         return new XoroshiroRandomSource(j, this.seedHi);
      }

      @Override
      public RandomSource fromHashOf(String p_224695_) {
         RandomSupport.Seed128bit randomsupport$seed128bit = RandomSupport.seedFromHashOf(p_224695_);
         return new XoroshiroRandomSource(randomsupport$seed128bit.xor(this.seedLo, this.seedHi));
      }

      @Override
      public RandomSource fromSeed(long p_343733_) {
         return new XoroshiroRandomSource(p_343733_ ^ this.seedLo, p_343733_ ^ this.seedHi);
      }

      @VisibleForTesting
      @Override
      public void parityConfigString(StringBuilder p_190136_) {
         p_190136_.append("seedLo: ").append(this.seedLo).append(", seedHi: ").append(this.seedHi);
      }
   }
}
