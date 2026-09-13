package net.minecraft.world.level.biome;

import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Lifecycle;
import com.mojang.serialization.MapCodec;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.VisibleForDebug;
import net.minecraft.world.level.levelgen.NoiseRouterData;

public class MultiNoiseBiomeSource extends BiomeSource {
   private static final MapCodec<Holder<Biome>> ENTRY_CODEC = Biome.CODEC.fieldOf("biome");
   public static final MapCodec<Climate.ParameterList<Holder<Biome>>> DIRECT_CODEC = Climate.ParameterList.codec(ENTRY_CODEC).fieldOf("biomes");
   private static final MapCodec<Holder<MultiNoiseBiomeSourceParameterList>> PRESET_CODEC = MultiNoiseBiomeSourceParameterList.CODEC
      .fieldOf("preset")
      .withLifecycle(Lifecycle.stable());
   public static final MapCodec<MultiNoiseBiomeSource> CODEC = Codec.mapEither(DIRECT_CODEC, PRESET_CODEC)
      .xmap(MultiNoiseBiomeSource::new, p_275170_ -> p_275170_.parameters);
   private final Either<Climate.ParameterList<Holder<Biome>>, Holder<MultiNoiseBiomeSourceParameterList>> parameters;

   private MultiNoiseBiomeSource(Either<Climate.ParameterList<Holder<Biome>>, Holder<MultiNoiseBiomeSourceParameterList>> p_275370_) {
      this.parameters = p_275370_;
   }

   public static MultiNoiseBiomeSource createFromList(Climate.ParameterList<Holder<Biome>> p_275223_) {
      return new MultiNoiseBiomeSource(Either.left(p_275223_));
   }

   public static MultiNoiseBiomeSource createFromPreset(Holder<MultiNoiseBiomeSourceParameterList> p_275250_) {
      return new MultiNoiseBiomeSource(Either.right(p_275250_));
   }

   private Climate.ParameterList<Holder<Biome>> parameters() {
      return (Climate.ParameterList<Holder<Biome>>)this.parameters
         .map(p_275171_ -> p_275171_, p_275172_ -> ((MultiNoiseBiomeSourceParameterList)p_275172_.value()).parameters());
   }

   @Override
   protected Stream<Holder<Biome>> collectPossibleBiomes() {
      return this.parameters().values().stream().map(Pair::getSecond);
   }

   @Override
   protected MapCodec<? extends BiomeSource> codec() {
      return CODEC;
   }

   public boolean stable(ResourceKey<MultiNoiseBiomeSourceParameterList> p_275637_) {
      Optional<Holder<MultiNoiseBiomeSourceParameterList>> optional = this.parameters.right();
      return optional.isPresent() && optional.get().is(p_275637_);
   }

   @Override
   public Holder<Biome> getNoiseBiome(int p_204272_, int p_204273_, int p_204274_, Climate.Sampler p_204275_) {
      return this.getNoiseBiome(p_204275_.sample(p_204272_, p_204273_, p_204274_));
   }

   @VisibleForDebug
   public Holder<Biome> getNoiseBiome(Climate.TargetPoint p_204270_) {
      return this.parameters().findValue(p_204270_);
   }

   @Override
   public void addDebugInfo(List<String> p_207895_, BlockPos p_207896_, Climate.Sampler p_207897_) {
      int i = QuartPos.fromBlock(p_207896_.getX());
      int j = QuartPos.fromBlock(p_207896_.getY());
      int k = QuartPos.fromBlock(p_207896_.getZ());
      Climate.TargetPoint climate$targetpoint = p_207897_.sample(i, j, k);
      float f = Climate.unquantizeCoord(climate$targetpoint.continentalness());
      float f1 = Climate.unquantizeCoord(climate$targetpoint.erosion());
      float f2 = Climate.unquantizeCoord(climate$targetpoint.temperature());
      float f3 = Climate.unquantizeCoord(climate$targetpoint.humidity());
      float f4 = Climate.unquantizeCoord(climate$targetpoint.weirdness());
      double d0 = NoiseRouterData.peaksAndValleys(f4);
      OverworldBiomeBuilder overworldbiomebuilder = new OverworldBiomeBuilder();
      p_207895_.add(
         "Biome builder PV: "
            + OverworldBiomeBuilder.getDebugStringForPeaksAndValleys(d0)
            + " C: "
            + overworldbiomebuilder.getDebugStringForContinentalness(f)
            + " E: "
            + overworldbiomebuilder.getDebugStringForErosion(f1)
            + " T: "
            + overworldbiomebuilder.getDebugStringForTemperature(f2)
            + " H: "
            + overworldbiomebuilder.getDebugStringForHumidity(f3)
      );
   }
}
