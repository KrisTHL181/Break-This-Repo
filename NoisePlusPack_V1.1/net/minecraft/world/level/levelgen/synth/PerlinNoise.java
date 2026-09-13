package net.minecraft.world.level.levelgen.synth;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import dev.miru.main.ModMain;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import it.unimi.dsi.fastutil.ints.IntBidirectionalIterator;
import it.unimi.dsi.fastutil.ints.IntRBTreeSet;
import it.unimi.dsi.fastutil.ints.IntSortedSet;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.IntStream;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.PositionalRandomFactory;
import org.jspecify.annotations.Nullable;

public class PerlinNoise {
   private static final int ROUND_OFF = 33554432;
   private final @Nullable ImprovedNoise[] noiseLevels;
   private final int firstOctave;
   private final DoubleList amplitudes;
   private final double lowestFreqValueFactor;
   private final double lowestFreqInputFactor;
   private final double maxValue;

   @Deprecated
   public static PerlinNoise createLegacyForBlendedNoise(RandomSource p_230533_, IntStream p_230534_) {
      return new PerlinNoise(p_230533_, makeAmplitudes(new IntRBTreeSet(p_230534_.boxed().collect(ImmutableList.toImmutableList()))), false);
   }

   @Deprecated
   public static PerlinNoise createLegacyForLegacyNetherBiome(RandomSource p_230526_, int p_230527_, DoubleList p_230528_) {
      return new PerlinNoise(p_230526_, Pair.of(p_230527_, p_230528_), false);
   }

   public static PerlinNoise create(RandomSource p_230540_, IntStream p_230541_) {
      return create(p_230540_, p_230541_.boxed().collect(ImmutableList.toImmutableList()));
   }

   public static PerlinNoise create(RandomSource p_230530_, List<Integer> p_230531_) {
      return new PerlinNoise(p_230530_, makeAmplitudes(new IntRBTreeSet(p_230531_)), true);
   }

   public static PerlinNoise create(RandomSource p_230521_, int p_230522_, double p_230523_, double... p_230524_) {
      DoubleArrayList doublearraylist = new DoubleArrayList(p_230524_);
      doublearraylist.add(0, p_230523_);
      return new PerlinNoise(p_230521_, Pair.of(p_230522_, doublearraylist), true);
   }

   public static PerlinNoise create(RandomSource p_230536_, int p_230537_, DoubleList p_230538_) {
      return new PerlinNoise(p_230536_, Pair.of(p_230537_, p_230538_), true);
   }

   private static Pair<Integer, DoubleList> makeAmplitudes(IntSortedSet p_75431_) {
      if (p_75431_.isEmpty()) {
         throw new IllegalArgumentException("Need some octaves!");
      }

      int i = -p_75431_.firstInt();
      int j = p_75431_.lastInt();
      int k = i + j + 1;
      if (k < 1) {
         throw new IllegalArgumentException("Total number of octaves needs to be >= 1");
      }

      DoubleList doublelist = new DoubleArrayList(new double[k]);
      IntBidirectionalIterator intbidirectionaliterator = p_75431_.iterator();

      while (intbidirectionaliterator.hasNext()) {
         int l = intbidirectionaliterator.nextInt();
         doublelist.set(l + i, 1.0);
      }

      return Pair.of(-i, doublelist);
   }

   protected PerlinNoise(RandomSource p_230515_, Pair<Integer, DoubleList> p_230516_, boolean p_230517_) {
      this.firstOctave = (Integer)p_230516_.getFirst();
      this.amplitudes = (DoubleList)p_230516_.getSecond();
      int i = this.amplitudes.size();
      int j = -this.firstOctave;
      this.noiseLevels = new ImprovedNoise[i];
      if (p_230517_) {
         PositionalRandomFactory positionalrandomfactory = p_230515_.forkPositional();

         for (int k = 0; k < i; k++) {
            if (this.amplitudes.getDouble(k) != 0.0) {
               int l = this.firstOctave + k;
               this.noiseLevels[k] = new ImprovedNoise(positionalrandomfactory.fromHashOf("octave_" + l));
            }
         }
      } else {
         ImprovedNoise improvednoise = new ImprovedNoise(p_230515_);
         if (j >= 0 && j < i) {
            double d0 = this.amplitudes.getDouble(j);
            if (d0 != 0.0) {
               this.noiseLevels[j] = improvednoise;
            }
         }

         for (int i1 = j - 1; i1 >= 0; i1--) {
            if (i1 < i) {
               double d1 = this.amplitudes.getDouble(i1);
               if (d1 != 0.0) {
                  this.noiseLevels[i1] = new ImprovedNoise(p_230515_);
               } else {
                  skipOctave(p_230515_);
               }
            } else {
               skipOctave(p_230515_);
            }
         }

         if (Arrays.stream(this.noiseLevels).filter(Objects::nonNull).count() != this.amplitudes.stream().filter(p_192897_ -> p_192897_ != 0.0).count()) {
            throw new IllegalStateException("Failed to create correct number of noise levels for given non-zero amplitudes");
         }

         if (j < i - 1) {
            throw new IllegalArgumentException("Positive octaves are temporarily disabled");
         }
      }

      this.lowestFreqInputFactor = Math.pow(2.0, -j);
      this.lowestFreqValueFactor = Math.pow(2.0, i - 1) / (Math.pow(2.0, i) - 1.0);
      this.maxValue = this.edgeValue(2.0);
   }

   protected double maxValue() {
      return this.maxValue;
   }

   private static void skipOctave(RandomSource p_230519_) {
      p_230519_.consumeCount(262);
   }

   public double getValue(double p_75409_, double p_75410_, double p_75411_) {
      return this.getValue(p_75409_, p_75410_, p_75411_, 0.0, 0.0, false);
   }

   @Deprecated
   public double getValue(double p_75418_, double p_75419_, double p_75420_, double p_75421_, double p_75422_, boolean p_75423_) {
      double d0 = 0.0;
      double d1 = this.lowestFreqInputFactor;
      double d2 = this.lowestFreqValueFactor;

      for (int i = 0; i < this.noiseLevels.length; i++) {
         ImprovedNoise improvednoise = this.noiseLevels[i];
         if (improvednoise != null) {
            double d3 = improvednoise.noise(
               wrap(p_75418_ * d1), p_75423_ ? -improvednoise.yo : wrap(p_75419_ * d1), wrap(p_75420_ * d1), p_75421_ * d1, p_75422_ * d1
            );
            d0 += this.amplitudes.getDouble(i) * d3 * d2;
         }

         d1 *= 2.0;
         d2 /= 2.0;
      }

      return d0;
   }

   public double maxBrokenValue(double p_210644_) {
      return this.edgeValue(p_210644_ + 2.0);
   }

   private double edgeValue(double p_210650_) {
      double d0 = 0.0;
      double d1 = this.lowestFreqValueFactor;

      for (int i = 0; i < this.noiseLevels.length; i++) {
         ImprovedNoise improvednoise = this.noiseLevels[i];
         if (improvednoise != null) {
            d0 += this.amplitudes.getDouble(i) * p_210650_ * d1;
         }

         d1 /= 2.0;
      }

      return d0;
   }

   public @Nullable ImprovedNoise getOctaveNoise(int p_75425_) {
      return this.noiseLevels[this.noiseLevels.length - 1 - p_75425_];
   }

   public static double wrap(double p_75407_) {
      return ModMain.getOptions().getWgen_wrapModeValue().func.apply(p_75407_, ModMain.getOptions().getWgen_wrapDivisionValue());
   }

   protected int firstOctave() {
      return this.firstOctave;
   }

   protected DoubleList amplitudes() {
      return this.amplitudes;
   }

   @VisibleForTesting
   public void parityConfigString(StringBuilder p_192891_) {
      p_192891_.append("PerlinNoise{");
      List<String> list = this.amplitudes.stream().map(p_192889_ -> String.format(Locale.ROOT, "%.2f", p_192889_)).toList();
      p_192891_.append("first octave: ").append(this.firstOctave).append(", amplitudes: ").append(list).append(", noise levels: [");

      for (int i = 0; i < this.noiseLevels.length; i++) {
         p_192891_.append(i).append(": ");
         ImprovedNoise improvednoise = this.noiseLevels[i];
         if (improvednoise == null) {
            p_192891_.append("null");
         } else {
            improvednoise.parityConfigString(p_192891_);
         }

         p_192891_.append(", ");
      }

      p_192891_.append("]");
      p_192891_.append("}");
   }
}
