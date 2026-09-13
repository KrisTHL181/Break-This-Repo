package net.minecraft.world.level.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;

public class CoralWallFanBlock extends BaseCoralWallFanBlock {
   private final Block deadBlock;

   protected CoralWallFanBlock(final Block deadBlock, final BlockBehaviour.Properties properties) {
      super(properties);
      this.deadBlock = deadBlock;
   }

   @Override
   protected void onPlace(final BlockState state, final Level level, final BlockPos pos, final BlockState oldState, final boolean movedByPiston) {
      this.tryScheduleDieTick(state, level, level, level.getRandom(), pos);
   }

   @Override
   protected void tick(final BlockState state, final ServerLevel level, final BlockPos pos, final RandomSource random) {
      if (!scanForWater(state, level, pos)) {
         level.setBlock(pos, this.deadBlock.defaultBlockState().setValue(WATERLOGGED, false).setValue(FACING, state.getValue(FACING)), 2);
      }
   }

   @Override
   protected BlockState updateShape(
      final BlockState state,
      final LevelReader level,
      final ScheduledTickAccess ticks,
      final BlockPos pos,
      final Direction directionToNeighbour,
      final BlockPos neighbourPos,
      final BlockState neighbourState,
      final RandomSource random
   ) {
      if (directionToNeighbour.getOpposite() == state.getValue(FACING) && !state.canSurvive(level, pos)) {
         return Blocks.AIR.defaultBlockState();
      }

      if (state.getValue(WATERLOGGED)) {
         ticks.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
      }

      this.tryScheduleDieTick(state, level, ticks, random, pos);
      return super.updateShape(state, level, ticks, pos, directionToNeighbour, neighbourPos, neighbourState, random);
   }
}
