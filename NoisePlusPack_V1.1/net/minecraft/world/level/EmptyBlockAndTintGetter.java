package net.minecraft.world.level;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.jspecify.annotations.Nullable;

public enum EmptyBlockAndTintGetter implements BlockAndTintGetter {
   INSTANCE;

   @Override
   public float getShade(Direction p_363986_, boolean p_363413_) {
      return 1.0F;
   }

   @Override
   public LevelLightEngine getLightEngine() {
      return LevelLightEngine.EMPTY;
   }

   @Override
   public int getBlockTint(BlockPos p_366773_, ColorResolver p_364664_) {
      return -1;
   }

   @Override
   public @Nullable BlockEntity getBlockEntity(BlockPos p_363724_) {
      return null;
   }

   @Override
   public BlockState getBlockState(BlockPos p_366523_) {
      return Blocks.AIR.defaultBlockState();
   }

   @Override
   public FluidState getFluidState(BlockPos p_361173_) {
      return Fluids.EMPTY.defaultFluidState();
   }

   @Override
   public int getHeight() {
      return 0;
   }

   @Override
   public int getMinY() {
      return 0;
   }
}
