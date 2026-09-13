package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.LightEngine;

public abstract class SpreadingSnowyDirtBlock extends SnowyDirtBlock {
   protected SpreadingSnowyDirtBlock(BlockBehaviour.Properties p_56817_) {
      super(p_56817_);
   }

   private static boolean canBeGrass(BlockState p_56824_, LevelReader p_56825_, BlockPos p_56826_) {
      BlockPos blockpos = p_56826_.above();
      BlockState blockstate = p_56825_.getBlockState(blockpos);
      if (blockstate.is(Blocks.SNOW) && blockstate.getValue(SnowLayerBlock.LAYERS) == 1) {
         return true;
      }

      if (blockstate.getFluidState().getAmount() == 8) {
         return false;
      }

      int i = LightEngine.getLightBlockInto(p_56824_, blockstate, Direction.UP, blockstate.getLightBlock());
      return i < 15;
   }

   @Override
   protected abstract MapCodec<? extends SpreadingSnowyDirtBlock> codec();

   private static boolean canPropagate(BlockState p_56828_, LevelReader p_56829_, BlockPos p_56830_) {
      BlockPos blockpos = p_56830_.above();
      return canBeGrass(p_56828_, p_56829_, p_56830_) && !p_56829_.getFluidState(blockpos).is(FluidTags.WATER);
   }

   @Override
   protected void randomTick(BlockState p_222508_, ServerLevel p_222509_, BlockPos p_222510_, RandomSource p_222511_) {
      if (!canBeGrass(p_222508_, p_222509_, p_222510_)) {
         p_222509_.setBlockAndUpdate(p_222510_, Blocks.DIRT.defaultBlockState());
      } else if (p_222509_.getMaxLocalRawBrightness(p_222510_.above()) >= 9) {
         BlockState blockstate = this.defaultBlockState();

         for (int i = 0; i < 4; i++) {
            BlockPos blockpos = p_222510_.offset(p_222511_.nextInt(3) - 1, p_222511_.nextInt(5) - 3, p_222511_.nextInt(3) - 1);
            if (p_222509_.getBlockState(blockpos).is(Blocks.DIRT) && canPropagate(blockstate, p_222509_, blockpos)) {
               p_222509_.setBlockAndUpdate(blockpos, blockstate.setValue(SNOWY, isSnowySetting(p_222509_.getBlockState(blockpos.above()))));
            }
         }
      }
   }
}
