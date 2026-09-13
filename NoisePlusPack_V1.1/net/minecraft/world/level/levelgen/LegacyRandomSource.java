package net.minecraft.world.level.levelgen;

import com.google.common.annotations.VisibleForTesting;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.ThreadingDetector;

public class LegacyRandomSource implements BitRandomSource {
   private static final int MODULUS_BITS = 48;
   private static final long MODULUS_MASK = 281474976710655L;
   private static final long MULTIPLIER = 25214903917L;
   private static final long INCREMENT = 11L;
   private final AtomicLong seed = new AtomicLong();
   private final MarsagliaPolarGaussian gaussianSource = new MarsagliaPolarGaussian(this);

   public LegacyRandomSource(long p_188578_) {
      this.setSeed(p_188578_);
   }

   @Override
   public RandomSource fork() {
      return new LegacyRandomSource(this.nextLong());
   }

   @Override
   public PositionalRandomFactory forkPositional() {
      return new LegacyRandomSource.LegacyPositionalRandomFactory(this.nextLong());
   }

   @Override
   public void setSeed(long p_188585_) {
      if (!this.seed.compareAndSet(this.seed.get(), (p_188585_ ^ 25214903917L) & 281474976710655L)) {
         throw ThreadingDetector.makeThreadingException("LegacyRandomSource", null);
      }

      this.gaussianSource.reset();
   }

   @Override
   public int next(int p_188581_) {
      long i = this.seed.get();
      long j = i * 25214903917L + 11L & 281474976710655L;
      if (!this.seed.compareAndSet(i, j)) {
         throw ThreadingDetector.makeThreadingException("LegacyRandomSource", null);
      } else {
         return (int)(j >> 48 - p_188581_);
      }
   }

   @Override
   public double nextGaussian() {
      return this.gaussianSource.nextGaussian();
   }

   public static class LegacyPositionalRandomFactory implements PositionalRandomFactory {
      private final long seed;

      public LegacyPositionalRandomFactory(long p_188588_) {
         this.seed = p_188588_;
      }

      @Override
      public RandomSource at(int p_224198_, int p_224199_, int p_224200_) {
         long i = Mth.getSeed(p_224198_, p_224199_, p_224200_);
         long j = i ^ this.seed;
         return new LegacyRandomSource(j);
      }

      @Override
      public RandomSource fromHashOf(String p_224202_) {
         int i = p_224202_.hashCode();
         return new LegacyRandomSource(i ^ this.seed);
      }

      @Override
      public RandomSource fromSeed(long p_344211_) {
         return new LegacyRandomSource(p_344211_);
      }

      @VisibleForTesting
      @Override
      public void parityConfigString(StringBuilder p_188596_) {
         p_188596_.append("LegacyPositionalRandomFactory{").append(this.seed).append("}");
      }
   }
}
