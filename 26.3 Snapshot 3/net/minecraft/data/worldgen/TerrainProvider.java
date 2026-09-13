package net.minecraft.data.worldgen;

import it.unimi.dsi.fastutil.floats.Float2FloatFunction;
import net.minecraft.util.BoundedFloatFunction;
import net.minecraft.util.CubicSpline;
import net.minecraft.util.Mth;

public class TerrainProvider {
   private static final float DEEP_OCEAN_CONTINENTALNESS = -0.51F;
   private static final float OCEAN_CONTINENTALNESS = -0.4F;
   private static final float PLAINS_CONTINENTALNESS = 0.1F;
   private static final float BEACH_CONTINENTALNESS = -0.15F;
   private static final Float2FloatFunction NO_TRANSFORM = Float2FloatFunction.identity();
   private static final Float2FloatFunction AMPLIFIED_OFFSET = offset -> offset < 0.0F ? offset : offset * 2.0F;
   private static final Float2FloatFunction AMPLIFIED_FACTOR = factor -> 1.25F - 6.25F / (factor + 5.0F);
   private static final Float2FloatFunction AMPLIFIED_JAGGEDNESS = jaggedness -> jaggedness * 2.0F;

   public static <I extends BoundedFloatFunction<?>> CubicSpline<I> overworldOffset(
      final I continents, final I erosion, final I ridges, final boolean amplified
   ) {
      Float2FloatFunction offsetTransformer = amplified ? AMPLIFIED_OFFSET : NO_TRANSFORM;
      CubicSpline<I> beachSpline = buildErosionOffsetSpline(erosion, ridges, -0.15F, 0.0F, 0.0F, 0.1F, 0.0F, -0.03F, false, false, offsetTransformer);
      CubicSpline<I> lowSpline = buildErosionOffsetSpline(erosion, ridges, -0.1F, 0.03F, 0.1F, 0.1F, 0.01F, -0.03F, false, false, offsetTransformer);
      CubicSpline<I> midSpline = buildErosionOffsetSpline(erosion, ridges, -0.1F, 0.03F, 0.1F, 0.7F, 0.01F, -0.03F, true, true, offsetTransformer);
      CubicSpline<I> highSpline = buildErosionOffsetSpline(erosion, ridges, -0.05F, 0.03F, 0.1F, 1.0F, 0.01F, 0.01F, true, true, offsetTransformer);
      return CubicSpline.<I>builder(continents, offsetTransformer)
         .addPoint(-1.1F, 0.044F)
         .addPoint(-1.02F, -0.2222F)
         .addPoint(-0.51F, -0.2222F)
         .addPoint(-0.44F, -0.12F)
         .addPoint(-0.18F, -0.12F)
         .addPoint(-0.16F, beachSpline)
         .addPoint(-0.15F, beachSpline)
         .addPoint(-0.1F, lowSpline)
         .addPoint(0.25F, midSpline)
         .addPoint(1.0F, highSpline)
         .build();
   }

   public static <I extends BoundedFloatFunction<?>> CubicSpline<I> overworldFactor(
      final I continents, final I erosion, final I weirdness, final I ridges, final boolean amplified
   ) {
      Float2FloatFunction factorTransformer = amplified ? AMPLIFIED_FACTOR : NO_TRANSFORM;
      return CubicSpline.<I>builder(continents, NO_TRANSFORM)
         .addPoint(-0.19F, 3.95F)
         .addPoint(-0.15F, getErosionFactor(erosion, weirdness, ridges, 6.25F, true, NO_TRANSFORM))
         .addPoint(-0.1F, getErosionFactor(erosion, weirdness, ridges, 5.47F, true, factorTransformer))
         .addPoint(0.03F, getErosionFactor(erosion, weirdness, ridges, 5.08F, true, factorTransformer))
         .addPoint(0.06F, getErosionFactor(erosion, weirdness, ridges, 4.69F, false, factorTransformer))
         .build();
   }

   public static <I extends BoundedFloatFunction<?>> CubicSpline<I> overworldJaggedness(
      final I continents, final I erosion, final I weirdness, final I ridges, final boolean amplified
   ) {
      Float2FloatFunction jaggednessTransformer = amplified ? AMPLIFIED_JAGGEDNESS : NO_TRANSFORM;
      float farInlandMiddle = 0.65F;
      return CubicSpline.<I>builder(continents, jaggednessTransformer)
         .addPoint(-0.11F, 0.0F)
         .addPoint(0.03F, buildErosionJaggednessSpline(erosion, weirdness, ridges, 1.0F, 0.5F, 0.0F, 0.0F, jaggednessTransformer))
         .addPoint(0.65F, buildErosionJaggednessSpline(erosion, weirdness, ridges, 1.0F, 1.0F, 1.0F, 0.0F, jaggednessTransformer))
         .build();
   }

   private static <I extends BoundedFloatFunction<?>> CubicSpline<I> buildErosionJaggednessSpline(
      final I erosion,
      final I weirdness,
      final I ridges,
      final float jaggednessFactorAtPeakRidgeAndErosionIndex0,
      final float jaggednessFactorAtPeakRidgeAndErosionIndex1,
      final float jaggednessFactorAtHighRidgeAndErosionIndex0,
      final float jaggednessFactorAtHighRidgeAndErosionIndex1,
      final Float2FloatFunction jaggednessTransformer
   ) {
      float erosionIndex1Middle = -0.5775F;
      CubicSpline<I> ridgeJaggednessSplineAtErosion0 = buildRidgeJaggednessSpline(
         weirdness, ridges, jaggednessFactorAtPeakRidgeAndErosionIndex0, jaggednessFactorAtHighRidgeAndErosionIndex0, jaggednessTransformer
      );
      CubicSpline<I> ridgeJaggednessSplineAtErosion1 = buildRidgeJaggednessSpline(
         weirdness, ridges, jaggednessFactorAtPeakRidgeAndErosionIndex1, jaggednessFactorAtHighRidgeAndErosionIndex1, jaggednessTransformer
      );
      return CubicSpline.<I>builder(erosion, jaggednessTransformer)
         .addPoint(-1.0F, ridgeJaggednessSplineAtErosion0)
         .addPoint(-0.78F, ridgeJaggednessSplineAtErosion1)
         .addPoint(-0.5775F, ridgeJaggednessSplineAtErosion1)
         .addPoint(-0.375F, 0.0F)
         .build();
   }

   public static float peaksAndValleys(final float weirdness) {
      return -(Math.abs(Math.abs(weirdness) - 0.6666667F) - 0.33333334F) * 3.0F;
   }

   private static <I extends BoundedFloatFunction<?>> CubicSpline<I> buildRidgeJaggednessSpline(
      final I weirdness,
      final I ridges,
      final float jaggednessFactorAtPeakRidge,
      final float jaggednessFactorAtHighRidge,
      final Float2FloatFunction jaggednessTransformer
   ) {
      float highSliceStart = peaksAndValleys(0.4F);
      float highSliceEnd = peaksAndValleys(0.56666666F);
      float highSliceMiddle = (highSliceStart + highSliceEnd) / 2.0F;
      CubicSpline.Builder<I> ridgeSpline = CubicSpline.builder(ridges, jaggednessTransformer);
      ridgeSpline.addPoint(highSliceStart, 0.0F);
      if (jaggednessFactorAtHighRidge > 0.0F) {
         ridgeSpline.addPoint(highSliceMiddle, buildWeirdnessJaggednessSpline(weirdness, jaggednessFactorAtHighRidge, jaggednessTransformer));
      } else {
         ridgeSpline.addPoint(highSliceMiddle, 0.0F);
      }

      if (jaggednessFactorAtPeakRidge > 0.0F) {
         ridgeSpline.addPoint(1.0F, buildWeirdnessJaggednessSpline(weirdness, jaggednessFactorAtPeakRidge, jaggednessTransformer));
      } else {
         ridgeSpline.addPoint(1.0F, 0.0F);
      }

      return ridgeSpline.build();
   }

   private static <I extends BoundedFloatFunction<?>> CubicSpline<I> buildWeirdnessJaggednessSpline(
      final I weirdness, final float jaggednessFactor, final Float2FloatFunction jaggednessTransformer
   ) {
      float maxJaggednessAtNegativeWeirdness = 0.63F * jaggednessFactor;
      float maxJaggednessAtPositiveWeirdness = 0.3F * jaggednessFactor;
      return CubicSpline.<I>builder(weirdness, jaggednessTransformer)
         .addPoint(-0.01F, maxJaggednessAtNegativeWeirdness)
         .addPoint(0.01F, maxJaggednessAtPositiveWeirdness)
         .build();
   }

   private static <I extends BoundedFloatFunction<?>> CubicSpline<I> getErosionFactor(
      final I erosion, final I weirdness, final I ridges, final float baseValue, final boolean shatteredTerrain, final Float2FloatFunction factorTransformer
   ) {
      CubicSpline<I> baseSpline = CubicSpline.<I>builder(weirdness, factorTransformer).addPoint(-0.2F, 6.3F).addPoint(0.2F, baseValue).build();
      CubicSpline.Builder<I> erosionPoints = CubicSpline.<I>builder(erosion, factorTransformer)
         .addPoint(-0.6F, baseSpline)
         .addPoint(-0.5F, CubicSpline.<I>builder(weirdness, factorTransformer).addPoint(-0.05F, 6.3F).addPoint(0.05F, 2.67F).build())
         .addPoint(-0.35F, baseSpline)
         .addPoint(-0.25F, baseSpline)
         .addPoint(-0.1F, CubicSpline.<I>builder(weirdness, factorTransformer).addPoint(-0.05F, 2.67F).addPoint(0.05F, 6.3F).build())
         .addPoint(0.03F, baseSpline);
      if (shatteredTerrain) {
         CubicSpline<I> weirdnessShattered = CubicSpline.<I>builder(weirdness, factorTransformer).addPoint(0.0F, baseValue).addPoint(0.1F, 0.625F).build();
         CubicSpline<I> ridgesShattered = CubicSpline.<I>builder(ridges, factorTransformer)
            .addPoint(-0.9F, baseValue)
            .addPoint(-0.69F, weirdnessShattered)
            .build();
         erosionPoints.addPoint(0.35F, baseValue).addPoint(0.45F, ridgesShattered).addPoint(0.55F, ridgesShattered).addPoint(0.62F, baseValue);
      } else {
         CubicSpline<I> extremeHillsTerrainFromMidSliceAndUp = CubicSpline.<I>builder(ridges, factorTransformer)
            .addPoint(-0.7F, baseSpline)
            .addPoint(-0.15F, 1.37F)
            .build();
         CubicSpline<I> extra3dNoiseOnPeaksOnly = CubicSpline.<I>builder(ridges, factorTransformer).addPoint(0.45F, baseSpline).addPoint(0.7F, 1.56F).build();
         erosionPoints.addPoint(0.05F, extra3dNoiseOnPeaksOnly)
            .addPoint(0.4F, extra3dNoiseOnPeaksOnly)
            .addPoint(0.45F, extremeHillsTerrainFromMidSliceAndUp)
            .addPoint(0.55F, extremeHillsTerrainFromMidSliceAndUp)
            .addPoint(0.58F, baseValue);
      }

      return erosionPoints.build();
   }

   private static float calculateSlope(final float y1, final float y2, final float x1, final float x2) {
      return (y2 - y1) / (x2 - x1);
   }

   private static <I extends BoundedFloatFunction<?>> CubicSpline<I> buildMountainRidgeSplineWithPoints(
      final I ridges, final float modulation, final boolean saddle, final Float2FloatFunction offsetTransformer
   ) {
      CubicSpline.Builder<I> build = CubicSpline.builder(ridges, offsetTransformer);
      float allowRiversBelow = -0.7F;
      float minPoint = -1.0F;
      float minPointContinentalness = mountainContinentalness(-1.0F, modulation, -0.7F);
      float maxPoint = 1.0F;
      float maxPointContinentalness = mountainContinentalness(1.0F, modulation, -0.7F);
      float ridgeZeroPoint = calculateMountainRidgeZeroContinentalnessPoint(modulation);
      float afterRiverPoint = -0.65F;
      if (-0.65F < ridgeZeroPoint && ridgeZeroPoint < 1.0F) {
         float afterRiverThresholdContinentalness = mountainContinentalness(-0.65F, modulation, -0.7F);
         float beforeRiverPoint = -0.75F;
         float beforeRiverThresholdContinentalness = mountainContinentalness(-0.75F, modulation, -0.7F);
         float minPointDerivative = calculateSlope(minPointContinentalness, beforeRiverThresholdContinentalness, -1.0F, -0.75F);
         build.addPoint(-1.0F, minPointContinentalness, minPointDerivative);
         build.addPoint(-0.75F, beforeRiverThresholdContinentalness);
         build.addPoint(-0.65F, afterRiverThresholdContinentalness);
         float ridgeZeroPointContinentalness = mountainContinentalness(ridgeZeroPoint, modulation, -0.7F);
         float maxPointDerivative = calculateSlope(ridgeZeroPointContinentalness, maxPointContinentalness, ridgeZeroPoint, 1.0F);
         float smallOffset = 0.01F;
         build.addPoint(ridgeZeroPoint - 0.01F, ridgeZeroPointContinentalness);
         build.addPoint(ridgeZeroPoint, ridgeZeroPointContinentalness, maxPointDerivative);
         build.addPoint(1.0F, maxPointContinentalness, maxPointDerivative);
      } else {
         float simpleDerivative = calculateSlope(minPointContinentalness, maxPointContinentalness, -1.0F, 1.0F);
         if (saddle) {
            build.addPoint(-1.0F, Math.max(0.2F, minPointContinentalness));
            build.addPoint(0.0F, Mth.lerp(0.5F, minPointContinentalness, maxPointContinentalness), simpleDerivative);
         } else {
            build.addPoint(-1.0F, minPointContinentalness, simpleDerivative);
         }

         build.addPoint(1.0F, maxPointContinentalness, simpleDerivative);
      }

      return build.build();
   }

   private static float mountainContinentalness(final float ridge, final float modulation, final float allowRiversBelow) {
      float ridgeOffset = 1.17F;
      float ridgeAmplitude = 0.46082947F;
      float ridgeSlope = 1.0F - (1.0F - modulation) * 0.5F;
      float ridgeIntersect = 0.5F * (1.0F - modulation);
      float adjustedRidgeHeight = (ridge + 1.17F) * 0.46082947F;
      float continentalness = adjustedRidgeHeight * ridgeSlope - ridgeIntersect;
      return ridge < allowRiversBelow ? Math.max(continentalness, -0.2222F) : Math.max(continentalness, 0.0F);
   }

   private static float calculateMountainRidgeZeroContinentalnessPoint(final float modulation) {
      float ridgeOffset = 1.17F;
      float ridgeAmplitude = 0.46082947F;
      float ridgeSlope = 1.0F - (1.0F - modulation) * 0.5F;
      float ridgeIntersect = 0.5F * (1.0F - modulation);
      return ridgeIntersect / (0.46082947F * ridgeSlope) - 1.17F;
   }

   public static <I extends BoundedFloatFunction<?>> CubicSpline<I> buildErosionOffsetSpline(
      final I erosion,
      final I ridges,
      final float lowValley,
      final float hill,
      final float tallHill,
      final float mountainFactor,
      final float plain,
      final float swamp,
      final boolean includeExtremeHills,
      final boolean saddle,
      final Float2FloatFunction offsetTransformer
   ) {
      float lowPeaks = 0.6F;
      float valleyPlateau = 0.5F;
      float plateau = 0.5F;
      CubicSpline<I> veryLowErosionMountains = buildMountainRidgeSplineWithPoints(ridges, Mth.lerp(mountainFactor, 0.6F, 1.5F), saddle, offsetTransformer);
      CubicSpline<I> lowErosionMountains = buildMountainRidgeSplineWithPoints(ridges, Mth.lerp(mountainFactor, 0.6F, 1.0F), saddle, offsetTransformer);
      CubicSpline<I> mountains = buildMountainRidgeSplineWithPoints(ridges, mountainFactor, saddle, offsetTransformer);
      CubicSpline<I> widePlateau = ridgeSpline(
         ridges,
         lowValley - 0.15F,
         0.5F * mountainFactor,
         Mth.lerp(0.5F, 0.5F, 0.5F) * mountainFactor,
         0.5F * mountainFactor,
         0.6F * mountainFactor,
         0.5F,
         offsetTransformer
      );
      CubicSpline<I> narrowPlateau = ridgeSpline(
         ridges, lowValley, plain * mountainFactor, hill * mountainFactor, 0.5F * mountainFactor, 0.6F * mountainFactor, 0.5F, offsetTransformer
      );
      CubicSpline<I> plains = ridgeSpline(ridges, lowValley, plain, plain, hill, tallHill, 0.5F, offsetTransformer);
      CubicSpline<I> plainsFarInland = ridgeSpline(ridges, lowValley, plain, plain, hill, tallHill, 0.5F, offsetTransformer);
      CubicSpline<I> extremeHills = CubicSpline.<I>builder(ridges, offsetTransformer)
         .addPoint(-1.0F, lowValley)
         .addPoint(-0.4F, plains)
         .addPoint(0.0F, tallHill + 0.07F)
         .build();
      CubicSpline<I> swamps = ridgeSpline(ridges, -0.02F, swamp, swamp, hill, tallHill, 0.0F, offsetTransformer);
      CubicSpline.Builder<I> builder = CubicSpline.<I>builder(erosion, offsetTransformer)
         .addPoint(-0.85F, veryLowErosionMountains)
         .addPoint(-0.7F, lowErosionMountains)
         .addPoint(-0.4F, mountains)
         .addPoint(-0.35F, widePlateau)
         .addPoint(-0.1F, narrowPlateau)
         .addPoint(0.2F, plains);
      if (includeExtremeHills) {
         builder.addPoint(0.4F, plainsFarInland).addPoint(0.45F, extremeHills).addPoint(0.55F, extremeHills).addPoint(0.58F, plainsFarInland);
      }

      builder.addPoint(0.7F, swamps);
      return builder.build();
   }

   private static <I extends BoundedFloatFunction<?>> CubicSpline<I> ridgeSpline(
      final I ridges,
      final float valley,
      final float low,
      final float mid,
      final float high,
      final float peaks,
      final float minValleySteepness,
      final Float2FloatFunction offsetTransformer
   ) {
      float d1 = Math.max(0.5F * (low - valley), minValleySteepness);
      float d2 = 5.0F * (mid - low);
      return CubicSpline.<I>builder(ridges, offsetTransformer)
         .addPoint(-1.0F, valley, d1)
         .addPoint(-0.4F, low, Math.min(d1, d2))
         .addPoint(0.0F, mid, d2)
         .addPoint(0.4F, high, 2.0F * (high - mid))
         .addPoint(1.0F, peaks, 0.7F * (peaks - high))
         .build();
   }
}
