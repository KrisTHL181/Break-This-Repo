package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.BlockPileConfiguration;

public class BlockPileFeature extends Feature<BlockPileConfiguration> {
   public BlockPileFeature(Codec<BlockPileConfiguration> p_65262_) {
      super(p_65262_);
   }

   @Override
   public boolean place(FeaturePlaceContext<BlockPileConfiguration> p_159473_) {
      BlockPos blockpos = p_159473_.origin();
      WorldGenLevel worldgenlevel = p_159473_.level();
      RandomSource randomsource = p_159473_.random();
      BlockPileConfiguration blockpileconfiguration = p_159473_.config();
      if (blockpos.getY() < worldgenlevel.getMinY() + 5) {
         return false;
      }

      int i = 2 + randomsource.nextInt(2);
      int j = 2 + randomsource.nextInt(2);

      for (BlockPos blockpos1 : BlockPos.betweenClosed(blockpos.offset(-i, 0, -j), blockpos.offset(i, 1, j))) {
         int k = blockpos.getX() - blockpos1.getX();
         int l = blockpos.getZ() - blockpos1.getZ();
         if (k * k + l * l <= randomsource.nextFloat() * 10.0F - randomsource.nextFloat() * 6.0F) {
            this.tryPlaceBlock(worldgenlevel, blockpos1, randomsource, blockpileconfiguration);
         } else if (randomsource.nextFloat() < 0.031) {
            this.tryPlaceBlock(worldgenlevel, blockpos1, randomsource, blockpileconfiguration);
         }
      }

      return true;
   }

   private boolean mayPlaceOn(LevelAccessor p_224945_, BlockPos p_224946_, RandomSource p_224947_) {
      BlockPos blockpos = p_224946_.below();
      BlockState blockstate = p_224945_.getBlockState(blockpos);
      return blockstate.is(Blocks.DIRT_PATH) ? p_224947_.nextBoolean() : blockstate.isFaceSturdy(p_224945_, blockpos, Direction.UP);
   }

   private void tryPlaceBlock(LevelAccessor p_224949_, BlockPos p_224950_, RandomSource p_224951_, BlockPileConfiguration p_224952_) {
      if (p_224949_.isEmptyBlock(p_224950_) && this.mayPlaceOn(p_224949_, p_224950_, p_224951_)) {
         p_224949_.setBlock(p_224950_, p_224952_.stateProvider.getState(p_224951_, p_224950_), 260);
      }
   }
}
