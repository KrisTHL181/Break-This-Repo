package net.minecraft.world.level.redstone;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public abstract class RedstoneWireEvaluator {
   protected final RedStoneWireBlock wireBlock;

   protected RedstoneWireEvaluator(final RedStoneWireBlock wireBlock) {
      this.wireBlock = wireBlock;
   }

   public abstract void updatePowerStrength(
      final Level level, final BlockPos pos, final BlockState state, final @Nullable Orientation orientation, final boolean skipShapeUpdates
   );

   protected int getBlockSignal(final Level level, final BlockPos pos) {
      return this.wireBlock.getBlockSignal(level, pos);
   }

   protected int getWireSignal(final BlockPos pos, final BlockState state) {
      return state.is(this.wireBlock) ? state.getValue(RedStoneWireBlock.POWER) : 0;
   }

   protected int getIncomingWireSignal(final Level level, final BlockPos pos) {
      int wireSignal = 0;

      for (Direction direction : Direction.Plane.HORIZONTAL) {
         BlockPos neighborPos = pos.relative(direction);
         BlockState neighborState = level.getBlockState(neighborPos);
         wireSignal = Math.max(wireSignal, this.getWireSignal(neighborPos, neighborState));
         BlockPos abovePos = pos.above();
         if (neighborState.isRedstoneConductor(level, neighborPos) && !level.getBlockState(abovePos).isRedstoneConductor(level, abovePos)) {
            BlockPos aboveNeighborPos = neighborPos.above();
            wireSignal = Math.max(wireSignal, this.getWireSignal(aboveNeighborPos, level.getBlockState(aboveNeighborPos)));
         } else if (!neighborState.isRedstoneConductor(level, neighborPos)) {
            BlockPos belowNeighborPos = neighborPos.below();
            wireSignal = Math.max(wireSignal, this.getWireSignal(belowNeighborPos, level.getBlockState(belowNeighborPos)));
         }
      }

      return Math.max(0, wireSignal - 1);
   }
}
