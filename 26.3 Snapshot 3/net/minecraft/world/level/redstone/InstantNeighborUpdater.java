package net.minecraft.world.level.redstone;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public class InstantNeighborUpdater implements NeighborUpdater {
   private final Level level;

   public InstantNeighborUpdater(final Level level) {
      this.level = level;
   }

   @Override
   public void shapeUpdate(
      final Direction direction,
      final BlockState neighborState,
      final BlockPos pos,
      final BlockPos neighborPos,
      final @Block.UpdateFlags int updateFlags,
      final int updateLimit
   ) {
      NeighborUpdater.executeShapeUpdate(this.level, direction, pos, neighborPos, neighborState, updateFlags, updateLimit - 1);
   }

   @Override
   public void neighborChanged(final BlockPos pos, final Block changedBlock, final @Nullable Orientation orientation) {
      BlockState state = this.level.getBlockState(pos);
      this.neighborChanged(state, pos, changedBlock, orientation, false);
   }

   @Override
   public void neighborChanged(
      final BlockState state, final BlockPos pos, final Block changedBlock, final @Nullable Orientation orientation, final boolean movedByPiston
   ) {
      NeighborUpdater.executeUpdate(this.level, state, pos, changedBlock, orientation, movedByPiston);
   }
}
