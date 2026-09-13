package net.minecraft.world.level;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.jspecify.annotations.Nullable;

public enum EmptyBlockGetter implements BlockGetter {
   INSTANCE;

   @Override
   public @Nullable BlockEntity getBlockEntity(final BlockPos pos) {
      return null;
   }

   @Override
   public BlockState getBlockState(final BlockPos pos) {
      return Blocks.AIR.defaultBlockState();
   }

   @Override
   public FluidState getFluidState(final BlockPos pos) {
      return Fluids.EMPTY.defaultFluidState();
   }

   @Override
   public int getMinY() {
      return 0;
   }

   @Override
   public int getHeight() {
      return 0;
   }
}
