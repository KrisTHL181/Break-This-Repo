package net.minecraft.world.level.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class ConcretePowderBlock extends FallingBlock {
   private final Block concrete;

   public ConcretePowderBlock(final Block concrete, final BlockBehaviour.Properties properties) {
      super(properties);
      this.concrete = concrete;
   }

   @Override
   public void onLand(final Level level, final BlockPos pos, final BlockState state, final BlockState replacedBlock, final FallingBlockEntity entity) {
      if (shouldSolidify(level, pos, replacedBlock)) {
         level.setBlockAndUpdate(pos, this.concrete.defaultBlockState());
      }
   }

   @Override
   public BlockState getStateForPlacement(final BlockPlaceContext context) {
      BlockGetter level = context.getLevel();
      BlockPos pos = context.getClickedPos();
      BlockState replacedBlock = level.getBlockState(pos);
      return shouldSolidify(level, pos, replacedBlock) ? this.concrete.defaultBlockState() : super.getStateForPlacement(context);
   }

   private static boolean shouldSolidify(final BlockGetter level, final BlockPos pos, final BlockState replacedBlock) {
      return canSolidify(replacedBlock) || touchesLiquid(level, pos);
   }

   private static boolean touchesLiquid(final BlockGetter level, final BlockPos pos) {
      boolean touchesLiquid = false;
      BlockPos.MutableBlockPos testPos = pos.mutable();

      for (Direction direction : Direction.values()) {
         BlockState blockState = level.getBlockState(testPos);
         if (direction != Direction.DOWN || canSolidify(blockState)) {
            testPos.setWithOffset(pos, direction);
            blockState = level.getBlockState(testPos);
            if (canSolidify(blockState) && !blockState.isFaceSturdy(level, pos, direction.getOpposite())) {
               touchesLiquid = true;
               break;
            }
         }
      }

      return touchesLiquid;
   }

   private static boolean canSolidify(final BlockState state) {
      return state.getFluidState().is(FluidTags.WATER);
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
      return touchesLiquid(level, pos)
         ? this.concrete.defaultBlockState()
         : super.updateShape(state, level, ticks, pos, directionToNeighbour, neighbourPos, neighbourState, random);
   }

   @Override
   public int getDustColor(final BlockState blockState, final BlockGetter level, final BlockPos pos) {
      return blockState.getMapColor(level, pos).col;
   }
}
