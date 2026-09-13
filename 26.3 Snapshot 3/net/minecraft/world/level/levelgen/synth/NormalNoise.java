package net.minecraft.world.level.levelgen.synth;

import com.google.common.annotations.VisibleForTesting;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import it.unimi.dsi.fastutil.doubles.DoubleListIterator;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryFileCodec;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;

public class NormalNoise {
   private static final double INPUT_FACTOR = 1.0181268882175227;
   private static final double TARGET_DEVIATION = 0.3333333333333333;
   private final double valueFactor;
   private final PerlinNoise first;
   private final PerlinNoise second;
   private final double maxValue;
   private final NormalNoise.NoiseParameters parameters;

   @Deprecated
   public static NormalNoise createLegacyNetherBiome(final RandomSource random, final NormalNoise.NoiseParameters parameters) {
      return new NormalNoise(random, parameters, false);
   }

   public static NormalNoise create(final RandomSource random, final int firstOctave, final double... amplitudes) {
      return create(random, new NormalNoise.NoiseParameters(firstOctave, new DoubleArrayList(amplitudes)));
   }

   public static NormalNoise create(final RandomSource random, final NormalNoise.NoiseParameters parameters) {
      return new NormalNoise(random, parameters, true);
   }

   private NormalNoise(final RandomSource random, final NormalNoise.NoiseParameters parameters, final boolean useNewInitialization) {
      int firstOctave = parameters.firstOctave;
      DoubleList amplitudes = parameters.amplitudes;
      this.parameters = parameters;
      if (useNewInitialization) {
         this.first = PerlinNoise.create(random, firstOctave, amplitudes);
         this.second = PerlinNoise.create(random, firstOctave, amplitudes);
      } else {
         this.first = PerlinNoise.createLegacyForLegacyNetherBiome(random, firstOctave, amplitudes);
         this.second = PerlinNoise.createLegacyForLegacyNetherBiome(random, firstOctave, amplitudes);
      }

      int minOctave = Integer.MAX_VALUE;
      int maxOctave = Integer.MIN_VALUE;
      DoubleListIterator iterator = amplitudes.iterator();

      while (iterator.hasNext()) {
         int i = iterator.nextIndex();
         double amplitude = iterator.nextDouble();
         if (amplitude != 0.0) {
            minOctave = Math.min(minOctave, i);
            maxOctave = Math.max(maxOctave, i);
         }
      }

      this.valueFactor = 0.16666666666666666 / expectedDeviation(maxOctave - minOctave);
      this.maxValue = (this.first.maxValue() + this.second.maxValue()) * this.valueFactor;
   }

   public double maxValue() {
      return this.maxValue;
   }

   private static double expectedDeviation(final int octaveSpan) {
      return 0.1 * (1.0 + 1.0 / (octaveSpan + 1));
   }

   public double getValue(final double x, final double y, final double z) {
      double x2 = x * 1.0181268882175227;
      double y2 = y * 1.0181268882175227;
      double z2 = z * 1.0181268882175227;
      return (this.first.getValue(x, y, z) + this.second.getValue(x2, y2, z2)) * this.valueFactor;
   }

   public NormalNoise.NoiseParameters parameters() {
      return this.parameters;
   }

   @VisibleForTesting
   public void parityConfigString(final StringBuilder sb) {
      sb.append("NormalNoise {");
      sb.append("first: ");
      this.first.parityConfigString(sb);
      sb.append(", second: ");
      this.second.parityConfigString(sb);
      sb.append("}");
   }

   public record NoiseParameters(int firstOctave, DoubleList amplitudes) {
      public static final Codec<NormalNoise.NoiseParameters> DIRECT_CODEC = RecordCodecBuilder.create(
         i -> i.group(
               Codec.INT.fieldOf("firstOctave").forGetter(NormalNoise.NoiseParameters::firstOctave),
               Codec.DOUBLE.listOf().fieldOf("amplitudes").forGetter(NormalNoise.NoiseParameters::amplitudes)
            )
            .apply(i, NormalNoise.NoiseParameters::new)
      );
      public static final Codec<Holder<NormalNoise.NoiseParameters>> CODEC = RegistryFileCodec.create(Registries.NOISE, DIRECT_CODEC);

      public NoiseParameters(final int firstOctave, final List<Double> amplitudes) {
         this(firstOctave, new DoubleArrayList(amplitudes));
      }

      public NoiseParameters(final int firstOctave, final double firstAmplitude, final double... amplitudes) {
         this(firstOctave, Util.make(new DoubleArrayList(amplitudes), list -> list.add(0, firstAmplitude)));
      }
   }
}
