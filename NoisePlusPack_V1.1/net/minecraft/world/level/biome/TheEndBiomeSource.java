package net.minecraft.world.level.biome;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.QuartPos;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.level.levelgen.DensityFunction;

public class TheEndBiomeSource extends BiomeSource {
   public static final MapCodec<TheEndBiomeSource> CODEC = RecordCodecBuilder.mapCodec(
      p_255555_ -> p_255555_.group(
            RegistryOps.retrieveElement(Biomes.THE_END),
            RegistryOps.retrieveElement(Biomes.END_HIGHLANDS),
            RegistryOps.retrieveElement(Biomes.END_MIDLANDS),
            RegistryOps.retrieveElement(Biomes.SMALL_END_ISLANDS),
            RegistryOps.retrieveElement(Biomes.END_BARRENS)
         )
         .apply(p_255555_, p_255555_.stable(TheEndBiomeSource::new))
   );
   private final Holder<Biome> end;
   private final Holder<Biome> highlands;
   private final Holder<Biome> midlands;
   private final Holder<Biome> islands;
   private final Holder<Biome> barrens;

   public static TheEndBiomeSource create(HolderGetter<Biome> p_256561_) {
      return new TheEndBiomeSource(
         p_256561_.getOrThrow(Biomes.THE_END),
         p_256561_.getOrThrow(Biomes.END_HIGHLANDS),
         p_256561_.getOrThrow(Biomes.END_MIDLANDS),
         p_256561_.getOrThrow(Biomes.SMALL_END_ISLANDS),
         p_256561_.getOrThrow(Biomes.END_BARRENS)
      );
   }

   private TheEndBiomeSource(Holder<Biome> p_220678_, Holder<Biome> p_220679_, Holder<Biome> p_220680_, Holder<Biome> p_220681_, Holder<Biome> p_220682_) {
      this.end = p_220678_;
      this.highlands = p_220679_;
      this.midlands = p_220680_;
      this.islands = p_220681_;
      this.barrens = p_220682_;
   }

   @Override
   protected Stream<Holder<Biome>> collectPossibleBiomes() {
      return Stream.of(this.end, this.highlands, this.midlands, this.islands, this.barrens);
   }

   @Override
   protected MapCodec<? extends BiomeSource> codec() {
      return CODEC;
   }

   @Override
   public Holder<Biome> getNoiseBiome(int p_204292_, int p_204293_, int p_204294_, Climate.Sampler p_204295_) {
      int i = QuartPos.toBlock(p_204292_);
      int j = QuartPos.toBlock(p_204293_);
      int k = QuartPos.toBlock(p_204294_);
      int l = SectionPos.blockToSectionCoord(i);
      int i1 = SectionPos.blockToSectionCoord(k);
      if ((long)l * l + (long)i1 * i1 <= 4096L) {
         return this.end;
      } else {
         int j1 = (SectionPos.blockToSectionCoord(i) * 2 + 1) * 8;
         int k1 = (SectionPos.blockToSectionCoord(k) * 2 + 1) * 8;
         double d0 = p_204295_.erosion().compute(new DensityFunction.SinglePointContext(j1, j, k1));
         if (d0 > 0.25) {
            return this.highlands;
         } else if (d0 >= -0.0625) {
            return this.midlands;
         } else {
            return d0 < -0.21875 ? this.islands : this.barrens;
         }
      }
   }
}
