package net.minecraft.world.level.levelgen.blockpredicates;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.biome.Biome;

public class MatchingBiomesPredicate implements BlockPredicate {
   public static final MapCodec<MatchingBiomesPredicate> CODEC = RecordCodecBuilder.mapCodec(
      i -> i.group(RegistryCodecs.homogeneousList(Registries.BIOME).fieldOf("biomes").forGetter(c -> c.biomes)).apply(i, MatchingBiomesPredicate::new)
   );
   private final HolderSet<Biome> biomes;

   public MatchingBiomesPredicate(final HolderSet<Biome> biomes) {
      this.biomes = biomes;
   }

   public boolean test(final LevelAccessor worldGenLevel, final BlockPos blockPos) {
      return this.biomes.contains(worldGenLevel.getBiome(blockPos));
   }

   @Override
   public BlockPredicateType<?> type() {
      return BlockPredicateType.MATCHING_BIOMES;
   }
}
