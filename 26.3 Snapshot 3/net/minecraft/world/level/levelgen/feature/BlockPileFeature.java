package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

public record BlockPileFeature(BlockStateProvider stateProvider) implements Feature {
   public static final MapCodec<BlockPileFeature> CODEC = RecordCodecBuilder.mapCodec(
      i -> i.group(BlockStateProvider.CODEC.fieldOf("state_provider").forGetter(BlockPileFeature::stateProvider)).apply(i, BlockPileFeature::new)
   );

   @Override
   public MapCodec<BlockPileFeature> codec() {
      return CODEC;
   }

   @Override
   public boolean place(final WorldGenLevel level, final ChunkGenerator chunkGenerator, final RandomSource random, final BlockPos origin) {
      if (origin.getY() < level.getMinY() + 5) {
         return false;
      }

      int xr = 2 + random.nextInt(2);
      int zr = 2 + random.nextInt(2);

      for (BlockPos blockPos : BlockPos.betweenClosed(origin.offset(-xr, 0, -zr), origin.offset(xr, 1, zr))) {
         int xd = origin.getX() - blockPos.getX();
         int zd = origin.getZ() - blockPos.getZ();
         if (xd * xd + zd * zd <= random.nextFloat() * 10.0F - random.nextFloat() * 6.0F) {
            this.tryPlaceBlock(level, blockPos, random);
         } else if (random.nextFloat() < 0.031) {
            this.tryPlaceBlock(level, blockPos, random);
         }
      }

      return true;
   }

   private boolean mayPlaceOn(final LevelAccessor level, final BlockPos blockPos, final RandomSource random) {
      BlockPos below = blockPos.below();
      BlockState belowState = level.getBlockState(below);
      return belowState.is(Blocks.DIRT_PATH) ? random.nextBoolean() : belowState.isFaceSturdy(level, below, Direction.UP);
   }

   private void tryPlaceBlock(final WorldGenLevel level, final BlockPos blockPos, final RandomSource random) {
      if (level.isEmptyBlock(blockPos) && this.mayPlaceOn(level, blockPos, random)) {
         level.setBlock(blockPos, this.stateProvider.getState(level, random, blockPos), 260);
      }
   }
}
