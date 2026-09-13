package net.minecraft.world.level.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.TrappedChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.ExperimentalRedstoneUtils;
import net.minecraft.world.level.redstone.Orientation;

public class TrappedChestBlockEntity extends ChestBlockEntity {
   public TrappedChestBlockEntity(final BlockPos worldPosition, final BlockState blockState) {
      super(BlockEntityTypes.TRAPPED_CHEST, worldPosition, blockState);
   }

   @Override
   protected void signalOpenCount(final Level level, final BlockPos pos, final BlockState blockState, final int previous, final int current) {
      super.signalOpenCount(level, pos, blockState, previous, current);
      if (previous != current) {
         Orientation orientation = ExperimentalRedstoneUtils.initialOrientation(
            level, blockState.getValue(TrappedChestBlock.FACING).getOpposite(), Direction.UP
         );
         Block block = blockState.getBlock();
         level.updateNeighborsAt(pos, block, orientation);
         level.updateNeighborsAt(pos.below(), block, orientation);
      }
   }
}
