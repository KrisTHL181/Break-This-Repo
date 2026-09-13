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
      i -> i.group(
            RegistryOps.retrieveElement(Biomes.THE_END),
            RegistryOps.retrieveElement(Biomes.END_HIGHLANDS),
            RegistryOps.retrieveElement(Biomes.END_MIDLANDS),
            RegistryOps.retrieveElement(Biomes.SMALL_END_ISLANDS),
            RegistryOps.retrieveElement(Biomes.END_BARRENS)
         )
         .apply(i, i.stable(TheEndBiomeSource::new))
   );
   private final Holder<Biome> end;
   private final Holder<Biome> highlands;
   private final Holder<Biome> midlands;
   private final Holder<Biome> islands;
   private final Holder<Biome> barrens;

   public static TheEndBiomeSource create(final HolderGetter<Biome> biomes) {
      return new TheEndBiomeSource(
         biomes.getOrThrow(Biomes.THE_END),
         biomes.getOrThrow(Biomes.END_HIGHLANDS),
         biomes.getOrThrow(Biomes.END_MIDLANDS),
         biomes.getOrThrow(Biomes.SMALL_END_ISLANDS),
         biomes.getOrThrow(Biomes.END_BARRENS)
      );
   }

   private TheEndBiomeSource(
      final Holder<Biome> end, final Holder<Biome> highlands, final Holder<Biome> midlands, final Holder<Biome> islands, final Holder<Biome> barrens
   ) {
      this.end = end;
      this.highlands = highlands;
      this.midlands = midlands;
      this.islands = islands;
      this.barrens = barrens;
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
   public Holder<Biome> getNoiseBiome(final int quartX, final int quartY, final int quartZ, final Climate.Sampler sampler) {
      int blockX = QuartPos.toBlock(quartX);
      int blockY = QuartPos.toBlock(quartY);
      int blockZ = QuartPos.toBlock(quartZ);
      int chunkX = SectionPos.blockToSectionCoord(blockX);
      int chunkZ = SectionPos.blockToSectionCoord(blockZ);
      if ((long)chunkX * chunkX + (long)chunkZ * chunkZ <= 4096L) {
         return this.end;
      } else {
         int weirdBlockX = (SectionPos.blockToSectionCoord(blockX) * 2 + 1) * 8;
         int weirdBlockZ = (SectionPos.blockToSectionCoord(blockZ) * 2 + 1) * 8;
         double heightValue = sampler.erosion().compute(new DensityFunction.SinglePointContext(weirdBlockX, blockY, weirdBlockZ));
         if (heightValue > 0.25) {
            return this.highlands;
         } else if (heightValue >= -0.0625) {
            return this.midlands;
         } else {
            return heightValue < -0.21875 ? this.islands : this.barrens;
         }
      }
   }
}
