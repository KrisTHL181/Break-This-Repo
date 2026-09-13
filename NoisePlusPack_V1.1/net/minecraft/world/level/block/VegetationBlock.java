package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathComputationType;

public abstract class VegetationBlock extends Block {
   protected VegetationBlock(BlockBehaviour.Properties p_395394_) {
      super(p_395394_);
   }

   @Override
   protected abstract MapCodec<? extends VegetationBlock> codec();

   protected boolean mayPlaceOn(BlockState p_395950_, BlockGetter p_394817_, BlockPos p_393668_) {
      return p_395950_.is(BlockTags.DIRT) || p_395950_.is(Blocks.FARMLAND);
   }

   @Override
   protected BlockState updateShape(
      BlockState p_394702_,
      LevelReader p_396711_,
      ScheduledTickAccess p_391200_,
      BlockPos p_395646_,
      Direction p_395539_,
      BlockPos p_396828_,
      BlockState p_397860_,
      RandomSource p_397921_
   ) {
      return !p_394702_.canSurvive(p_396711_, p_395646_)
         ? Blocks.AIR.defaultBlockState()
         : super.updateShape(p_394702_, p_396711_, p_391200_, p_395646_, p_395539_, p_396828_, p_397860_, p_397921_);
   }

   @Override
   protected boolean canSurvive(BlockState p_397664_, LevelReader p_395119_, BlockPos p_393561_) {
      BlockPos blockpos = p_393561_.below();
      return this.mayPlaceOn(p_395119_.getBlockState(blockpos), p_395119_, blockpos);
   }

   @Override
   protected boolean propagatesSkylightDown(BlockState p_394728_) {
      return p_394728_.getFluidState().isEmpty();
   }

   @Override
   protected boolean isPathfindable(BlockState p_392416_, PathComputationType p_395772_) {
      return p_395772_ == PathComputationType.AIR && !this.hasCollision ? true : super.isPathfindable(p_392416_, p_395772_);
   }
}
