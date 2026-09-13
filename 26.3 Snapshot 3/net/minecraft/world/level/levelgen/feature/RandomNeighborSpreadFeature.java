package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.IntProviders;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

public record RandomNeighborSpreadFeature(
   BlockStateProvider block, HolderSet<Block> acceptedNeighbors, BlockPredicate canReplace, IntProvider attempts, IntProvider xzOffset, IntProvider yOffset
) implements Feature {
   public static final MapCodec<RandomNeighborSpreadFeature> CODEC = RecordCodecBuilder.mapCodec(
      i -> i.group(
            BlockStateProvider.CODEC.fieldOf("block").forGetter(RandomNeighborSpreadFeature::block),
            RegistryCodecs.homogeneousList(Registries.BLOCK).fieldOf("accepted_neighbors").forGetter(RandomNeighborSpreadFeature::acceptedNeighbors),
            BlockPredicate.CODEC.fieldOf("can_replace").forGetter(RandomNeighborSpreadFeature::canReplace),
            IntProviders.codec(1, 3000).fieldOf("attempts").forGetter(RandomNeighborSpreadFeature::attempts),
            IntProviders.codec(-16, 16).fieldOf("xz_offset").forGetter(RandomNeighborSpreadFeature::xzOffset),
            IntProviders.codec(-16, 16).fieldOf("y_offset").forGetter(RandomNeighborSpreadFeature::yOffset)
         )
         .apply(i, RandomNeighborSpreadFeature::new)
   );

   @Override
   public MapCodec<RandomNeighborSpreadFeature> codec() {
      return CODEC;
   }

   @Override
   public boolean place(final WorldGenLevel level, final ChunkGenerator chunkGenerator, final RandomSource random, final BlockPos origin) {
      level.setBlock(origin, this.block.getState(level, random, origin), 2);
      int attempts = this.attempts.sample(random);

      for (int i = 0; i < attempts; i++) {
         BlockPos placePos = origin.offset(this.xzOffset.sample(random), this.yOffset.sample(random), this.xzOffset.sample(random));
         if (this.canReplace.test(level, placePos)) {
            int neighbours = 0;

            for (Direction direction : Direction.values()) {
               if (level.getBlockState(placePos.relative(direction)).is(this.acceptedNeighbors)) {
                  neighbours++;
               }

               if (neighbours > 1) {
                  break;
               }
            }

            if (neighbours == 1) {
               level.setBlock(placePos, this.block.getState(level, random, placePos), 2);
            }
         }
      }

      return true;
   }
}
