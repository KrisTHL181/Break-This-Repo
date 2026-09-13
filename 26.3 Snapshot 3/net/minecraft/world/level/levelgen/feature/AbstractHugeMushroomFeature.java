package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

public interface AbstractHugeMushroomFeature extends Feature {
   int MIN_MUSHROOM_HEIGHT = 4;

   BlockStateProvider capProvider();

   BlockStateProvider stemProvider();

   int foliageRadius();

   BlockPredicate canPlaceOn();

   @Override
   MapCodec<? extends AbstractHugeMushroomFeature> codec();

   default void placeTrunk(
      final WorldGenLevel level, final RandomSource random, final BlockPos origin, final int treeHeight, final BlockPos.MutableBlockPos blockPos
   ) {
      for (int dy = 0; dy < treeHeight; dy++) {
         blockPos.set(origin).move(Direction.UP, dy);
         this.placeMushroomBlock(level, blockPos, this.stemProvider().getState(level, random, origin));
      }
   }

   default void placeMushroomBlock(final LevelAccessor level, final BlockPos.MutableBlockPos blockPos, final BlockState newState) {
      BlockState currentState = level.getBlockState(blockPos);
      if (currentState.isAir() || currentState.is(BlockTags.REPLACEABLE_BY_MUSHROOMS)) {
         this.setBlock(level, blockPos, newState);
      }
   }

   default int getTreeHeight(final RandomSource random) {
      int treeHeight = random.nextInt(3) + 4;
      if (random.nextInt(12) == 0) {
         treeHeight *= 2;
      }

      return treeHeight;
   }

   default boolean isValidPosition(final WorldGenLevel level, final BlockPos origin, final int treeHeight, final BlockPos.MutableBlockPos blockPos) {
      int y = origin.getY();
      if (y >= level.getMinY() + 1 && y + treeHeight + 1 <= level.getMaxY()) {
         if (!this.canPlaceOn().test(level, origin.below())) {
            return false;
         }

         for (int dy = 0; dy <= treeHeight; dy++) {
            int radius = this.getTreeRadiusForHeight(-1, -1, this.foliageRadius(), dy);

            for (int dx = -radius; dx <= radius; dx++) {
               for (int dz = -radius; dz <= radius; dz++) {
                  BlockState state = level.getBlockState(blockPos.setWithOffset(origin, dx, dy, dz));
                  if (!state.isAir() && !state.is(BlockTags.LEAVES)) {
                     return false;
                  }
               }
            }
         }

         return true;
      } else {
         return false;
      }
   }

   @Override
   default boolean place(final WorldGenLevel level, final ChunkGenerator chunkGenerator, final RandomSource random, final BlockPos origin) {
      int treeHeight = this.getTreeHeight(random);
      BlockPos.MutableBlockPos blockPos = new BlockPos.MutableBlockPos();
      if (!this.isValidPosition(level, origin, treeHeight, blockPos)) {
         return false;
      }

      this.makeCap(level, random, origin, treeHeight, blockPos);
      this.placeTrunk(level, random, origin, treeHeight, blockPos);
      return true;
   }

   int getTreeRadiusForHeight(final int trunkHeight, final int treeHeight, final int leafRadius, final int yo);

   void makeCap(final WorldGenLevel level, final RandomSource random, final BlockPos origin, final int treeHeight, final BlockPos.MutableBlockPos blockPos);
}
