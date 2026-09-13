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
      public boolean isSupporting(BlockState p_57220_, BlockGetter p_57221_, BlockPos p_57222_, Direction p_57223_) {
         return Block.isFaceFull(p_57220_.getBlockSupportShape(p_57221_, p_57222_), p_57223_);
      }
   },
   CENTER {
      private final VoxelShape CENTER_SUPPORT_SHAPE = Block.column(2.0, 0.0, 10.0);

      @Override
      public boolean isSupporting(BlockState p_57230_, BlockGetter p_57231_, BlockPos p_57232_, Direction p_57233_) {
         return !Shapes.joinIsNotEmpty(
            p_57230_.getBlockSupportShape(p_57231_, p_57232_).getFaceShape(p_57233_), this.CENTER_SUPPORT_SHAPE, BooleanOp.ONLY_SECOND
         );
      }
   },
   RIGID {
      private final VoxelShape RIGID_SUPPORT_SHAPE = Shapes.join(Shapes.block(), Block.column(12.0, 0.0, 16.0), BooleanOp.ONLY_FIRST);

      @Override
      public boolean isSupporting(BlockState p_57240_, BlockGetter p_57241_, BlockPos p_57242_, Direction p_57243_) {
         return !Shapes.joinIsNotEmpty(
            p_57240_.getBlockSupportShape(p_57241_, p_57242_).getFaceShape(p_57243_), this.RIGID_SUPPORT_SHAPE, BooleanOp.ONLY_SECOND
         );
      }
   };

   public abstract boolean isSupporting(BlockState var1, BlockGetter var2, BlockPos var3, Direction var4);
}
