package net.minecraft.world.level;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DiodeBlock;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.state.BlockState;

public interface SignalGetter extends BlockGetter {
   Direction[] DIRECTIONS = Direction.values();

   default int getDirectSignal(BlockPos p_277954_, Direction p_277342_) {
      return this.getBlockState(p_277954_).getDirectSignal(this, p_277954_, p_277342_);
   }

   default int getDirectSignalTo(BlockPos p_277959_) {
      int i = 0;
      i = Math.max(i, this.getDirectSignal(p_277959_.below(), Direction.DOWN));
      if (i >= 15) {
         return i;
      }

      i = Math.max(i, this.getDirectSignal(p_277959_.above(), Direction.UP));
      if (i >= 15) {
         return i;
      }

      i = Math.max(i, this.getDirectSignal(p_277959_.north(), Direction.NORTH));
      if (i >= 15) {
         return i;
      }

      i = Math.max(i, this.getDirectSignal(p_277959_.south(), Direction.SOUTH));
      if (i >= 15) {
         return i;
      }

      i = Math.max(i, this.getDirectSignal(p_277959_.west(), Direction.WEST));
      if (i >= 15) {
         return i;
      }

      i = Math.max(i, this.getDirectSignal(p_277959_.east(), Direction.EAST));
      return i >= 15 ? i : i;
   }

   default int getControlInputSignal(BlockPos p_277757_, Direction p_278104_, boolean p_277707_) {
      BlockState blockstate = this.getBlockState(p_277757_);
      if (p_277707_) {
         return DiodeBlock.isDiode(blockstate) ? this.getDirectSignal(p_277757_, p_278104_) : 0;
      } else if (blockstate.is(Blocks.REDSTONE_BLOCK)) {
         return 15;
      } else if (blockstate.is(Blocks.REDSTONE_WIRE)) {
         return blockstate.getValue(RedStoneWireBlock.POWER);
      } else {
         return blockstate.isSignalSource() ? this.getDirectSignal(p_277757_, p_278104_) : 0;
      }
   }

   default boolean hasSignal(BlockPos p_277371_, Direction p_277391_) {
      return this.getSignal(p_277371_, p_277391_) > 0;
   }

   default int getSignal(BlockPos p_277961_, Direction p_277351_) {
      BlockState blockstate = this.getBlockState(p_277961_);
      int i = blockstate.getSignal(this, p_277961_, p_277351_);
      return blockstate.isRedstoneConductor(this, p_277961_) ? Math.max(i, this.getDirectSignalTo(p_277961_)) : i;
   }

   default boolean hasNeighborSignal(BlockPos p_277626_) {
      if (this.getSignal(p_277626_.below(), Direction.DOWN) > 0) {
         return true;
      } else if (this.getSignal(p_277626_.above(), Direction.UP) > 0) {
         return true;
      } else if (this.getSignal(p_277626_.north(), Direction.NORTH) > 0) {
         return true;
      } else if (this.getSignal(p_277626_.south(), Direction.SOUTH) > 0) {
         return true;
      } else {
         return this.getSignal(p_277626_.west(), Direction.WEST) > 0 ? true : this.getSignal(p_277626_.east(), Direction.EAST) > 0;
      }
   }

   default int getBestNeighborSignal(BlockPos p_277977_) {
      int i = 0;

      for (Direction direction : DIRECTIONS) {
         int j = this.getSignal(p_277977_.relative(direction), direction);
         if (j >= 15) {
            return 15;
         }

         if (j > i) {
            i = j;
         }
      }

      return i;
   }
}
