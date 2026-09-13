package net.minecraft.world.level.levelgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.function.Function;

public record NoiseRouter(
   DensityFunction barrierNoise,
   DensityFunction fluidLevelFloodednessNoise,
   DensityFunction fluidLevelSpreadNoise,
   DensityFunction lavaNoise,
   DensityFunction temperature,
   DensityFunction vegetation,
   DensityFunction continents,
   DensityFunction erosion,
   DensityFunction depth,
   DensityFunction ridges,
   DensityFunction preliminarySurfaceLevel,
   DensityFunction finalDensity,
   DensityFunction veinToggle,
   DensityFunction veinRidged,
   DensityFunction veinGap
) {
   public static final Codec<NoiseRouter> CODEC = RecordCodecBuilder.create(
      p_224411_ -> p_224411_.group(
            field("barrier", NoiseRouter::barrierNoise),
            field("fluid_level_floodedness", NoiseRouter::fluidLevelFloodednessNoise),
            field("fluid_level_spread", NoiseRouter::fluidLevelSpreadNoise),
            field("lava", NoiseRouter::lavaNoise),
            field("temperature", NoiseRouter::temperature),
            field("vegetation", NoiseRouter::vegetation),
            field("continents", NoiseRouter::continents),
            field("erosion", NoiseRouter::erosion),
            field("depth", NoiseRouter::depth),
            field("ridges", NoiseRouter::ridges),
            field("preliminary_surface_level", NoiseRouter::preliminarySurfaceLevel),
            field("final_density", NoiseRouter::finalDensity),
            field("vein_toggle", NoiseRouter::veinToggle),
            field("vein_ridged", NoiseRouter::veinRidged),
            field("vein_gap", NoiseRouter::veinGap)
         )
         .apply(p_224411_, NoiseRouter::new)
   );

   private static RecordCodecBuilder<NoiseRouter, DensityFunction> field(String p_224415_, Function<NoiseRouter, DensityFunction> p_224416_) {
      return DensityFunction.HOLDER_HELPER_CODEC.fieldOf(p_224415_).forGetter(p_224416_);
   }

   public NoiseRouter mapAll(DensityFunction.Visitor p_224413_) {
      return new NoiseRouter(
         this.barrierNoise.mapAll(p_224413_),
         this.fluidLevelFloodednessNoise.mapAll(p_224413_),
         this.fluidLevelSpreadNoise.mapAll(p_224413_),
         this.lavaNoise.mapAll(p_224413_),
         this.temperature.mapAll(p_224413_),
         this.vegetation.mapAll(p_224413_),
         this.continents.mapAll(p_224413_),
         this.erosion.mapAll(p_224413_),
         this.depth.mapAll(p_224413_),
         this.ridges.mapAll(p_224413_),
         this.preliminarySurfaceLevel.mapAll(p_224413_),
         this.finalDensity.mapAll(p_224413_),
         this.veinToggle.mapAll(p_224413_),
         this.veinRidged.mapAll(p_224413_),
         this.veinGap.mapAll(p_224413_)
      );
   }
}
