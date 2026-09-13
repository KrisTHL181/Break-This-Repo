package net.minecraft.world.level.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public enum SupportType {
   FULL {
      @Override
      public boolean isSupporting(final BlockState state, final BlockGetter level, final BlockPos pos, final Direction direction) {
         return Block.isFaceFull(state.getBlockSupportShape(level, pos), direction);
      }
   },
   CENTER {
      private final VoxelShape CENTER_SUPPORT_SHAPE = Block.column(2.0, 0.0, 10.0);

      @Override
      public boolean isSupporting(final BlockState state, final BlockGetter level, final BlockPos pos, final Direction direction) {
         return !Shapes.joinIsNotEmpty(state.getBlockSupportShape(level, pos).getFaceShape(direction), this.CENTER_SUPPORT_SHAPE, BooleanOp.ONLY_SECOND);
      }
   },
   RIGID {
      private final VoxelShape RIGID_SUPPORT_SHAPE = Shapes.join(Shapes.block(), Block.column(12.0, 0.0, 16.0), BooleanOp.ONLY_FIRST);

      @Override
      public boolean isSupporting(final BlockState state, final BlockGetter level, final BlockPos pos, final Direction direction) {
         return !Shapes.joinIsNotEmpty(state.getBlockSupportShape(level, pos).getFaceShape(direction), this.RIGID_SUPPORT_SHAPE, BooleanOp.ONLY_SECOND);
      }
   };

   public abstract boolean isSupporting(final BlockState state, final BlockGetter level, final BlockPos pos, final Direction direction);
}
