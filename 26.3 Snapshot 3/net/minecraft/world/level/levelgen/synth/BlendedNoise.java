package net.minecraft.world.level.levelgen.synth;

import com.google.common.annotations.VisibleForTesting;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Locale;
import java.util.stream.IntStream;
import net.minecraft.util.Interval;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;

public class BlendedNoise implements DensityFunction.SimpleFunction {
   private static final Codec<Double> SCALE_RANGE = Codec.doubleRange(0.001, 1000.0);
   private static final MapCodec<BlendedNoise> DATA_CODEC = RecordCodecBuilder.mapCodec(
      i -> i.group(
            SCALE_RANGE.fieldOf("xz_scale").forGetter(n -> n.xzScale),
            SCALE_RANGE.fieldOf("y_scale").forGetter(n -> n.yScale),
            SCALE_RANGE.fieldOf("xz_factor").forGetter(n -> n.xzFactor),
            SCALE_RANGE.fieldOf("y_factor").forGetter(n -> n.yFactor),
            Codec.doubleRange(1.0, 8.0).fieldOf("smear_scale_multiplier").forGetter(n -> n.smearScaleMultiplier)
         )
         .apply(i, BlendedNoise::createUnseeded)
   );
   public static final KeyDispatchDataCodec<BlendedNoise> CODEC = KeyDispatchDataCodec.of(DATA_CODEC);
   private final PerlinNoise minLimitNoise;
   private final PerlinNoise maxLimitNoise;
   private final PerlinNoise mainNoise;
   private final double xzMultiplier;
   private final double yMultiplier;
   private final double xzFactor;
   private final double yFactor;
   private final double smearScaleMultiplier;
   private final double maxValue;
   private final double xzScale;
   private final double yScale;

   public static BlendedNoise createUnseeded(
      final double xzScale, final double yScale, final double xzFactor, final double yFactor, final double smearScaleMultiplier
   ) {
      return new BlendedNoise(new XoroshiroRandomSource(0L), xzScale, yScale, xzFactor, yFactor, smearScaleMultiplier);
   }

   private BlendedNoise(
      final PerlinNoise minLimitNoise,
      final PerlinNoise maxLimitNoise,
      final PerlinNoise mainNoise,
      final double xzScale,
      final double yScale,
      final double xzFactor,
      final double yFactor,
      final double smearScaleMultiplier
   ) {
      this.minLimitNoise = minLimitNoise;
      this.maxLimitNoise = maxLimitNoise;
      this.mainNoise = mainNoise;
      this.xzScale = xzScale;
      this.yScale = yScale;
      this.xzFactor = xzFactor;
      this.yFactor = yFactor;
      this.smearScaleMultiplier = smearScaleMultiplier;
      this.xzMultiplier = 684.412 * this.xzScale;
      this.yMultiplier = 684.412 * this.yScale;
      this.maxValue = minLimitNoise.maxBrokenValue(this.yMultiplier);
   }

   @VisibleForTesting
   public BlendedNoise(
      final RandomSource random, final double xzScale, final double yScale, final double xzFactor, final double yFactor, final double smearScaleMultiplier
   ) {
      this(
         PerlinNoise.createLegacyForBlendedNoise(random, IntStream.rangeClosed(-15, 0)),
         PerlinNoise.createLegacyForBlendedNoise(random, IntStream.rangeClosed(-15, 0)),
         PerlinNoise.createLegacyForBlendedNoise(random, IntStream.rangeClosed(-7, 0)),
         xzScale,
         yScale,
         xzFactor,
         yFactor,
         smearScaleMultiplier
      );
   }

   public BlendedNoise withNewRandom(final RandomSource terrainRandom) {
      return new BlendedNoise(terrainRandom, this.xzScale, this.yScale, this.xzFactor, this.yFactor, this.smearScaleMultiplier);
   }

   @Override
   public double compute(final DensityFunction.FunctionContext context) {
      double limitX = context.blockX() * this.xzMultiplier;
      double limitY = context.blockY() * this.yMultiplier;
      double limitZ = context.blockZ() * this.xzMultiplier;
      double mainX = limitX / this.xzFactor;
      double mainY = limitY / this.yFactor;
      double mainZ = limitZ / this.xzFactor;
      double limitSmear = this.yMultiplier * this.smearScaleMultiplier;
      double mainSmear = limitSmear / this.yFactor;
      double blendMin = 0.0;
      double blendMax = 0.0;
      double mainNoiseValue = 0.0;
      boolean optimizeLoop = true;
      double pow = 1.0;

      for (int i = 0; i < 8; i++) {
         ImprovedNoise noise = this.mainNoise.getOctaveNoise(i);
         if (noise != null) {
            mainNoiseValue += noise.noise(
                  PerlinNoise.wrap(mainX * pow), PerlinNoise.wrap(mainY * pow), PerlinNoise.wrap(mainZ * pow), mainSmear * pow, mainY * pow
               )
               / pow;
         }

         pow /= 2.0;
      }

      double factor = (mainNoiseValue / 10.0 + 1.0) / 2.0;
      boolean isMax = factor >= 1.0;
      boolean isMin = factor <= 0.0;
      pow = 1.0;

      for (int i = 0; i < 16; i++) {
         double wx = PerlinNoise.wrap(limitX * pow);
         double wy = PerlinNoise.wrap(limitY * pow);
         double wz = PerlinNoise.wrap(limitZ * pow);
         double yScalePow = limitSmear * pow;
         if (!isMax) {
            ImprovedNoise minNoise = this.minLimitNoise.getOctaveNoise(i);
            if (minNoise != null) {
               blendMin += minNoise.noise(wx, wy, wz, yScalePow, limitY * pow) / pow;
            }
         }

         if (!isMin) {
            ImprovedNoise maxNoise = this.maxLimitNoise.getOctaveNoise(i);
            if (maxNoise != null) {
               blendMax += maxNoise.noise(wx, wy, wz, yScalePow, limitY * pow) / pow;
            }
         }

         pow /= 2.0;
      }

      return Mth.clampedLerp(factor, blendMin / 512.0, blendMax / 512.0) / 128.0;
   }

   @Override
   public Interval range() {
      return Interval.ofSymmetric(this.maxValue);
   }

   @VisibleForTesting
   public void parityConfigString(final StringBuilder sb) {
      sb.append("BlendedNoise{minLimitNoise=");
      this.minLimitNoise.parityConfigString(sb);
      sb.append(", maxLimitNoise=");
      this.maxLimitNoise.parityConfigString(sb);
      sb.append(", mainNoise=");
      this.mainNoise.parityConfigString(sb);
      sb.append(
            String.format(
               Locale.ROOT,
               ", xzScale=%.3f, yScale=%.3f, xzMainScale=%.3f, yMainScale=%.3f, cellWidth=4, cellHeight=8",
               684.412,
               684.412,
               8.555150000000001,
               4.277575000000001
            )
         )
         .append('}');
   }

   @Override
   public KeyDispatchDataCodec<? extends DensityFunction> codec() {
      return CODEC;
   }
}
