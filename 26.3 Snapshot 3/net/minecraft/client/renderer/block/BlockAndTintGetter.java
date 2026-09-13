package net.minecraft.client.renderer.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndLightGetter;
import net.minecraft.world.level.CardinalLighting;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.jspecify.annotations.Nullable;

public interface BlockAndTintGetter extends BlockAndLightGetter {
   BlockAndTintGetter EMPTY = new BlockAndTintGetter() {
      @Override
      public CardinalLighting cardinalLighting() {
         return CardinalLighting.DEFAULT;
      }

      @Override
      public LevelLightEngine getLightEngine() {
         return LevelLightEngine.EMPTY;
      }

      @Override
      public int getBlockTint(final BlockPos pos, final ColorResolver color) {
         return -1;
      }

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
      public int getHeight() {
         return 0;
      }

      @Override
      public int getMinY() {
         return 0;
      }
   };

   CardinalLighting cardinalLighting();

   int getBlockTint(BlockPos pos, ColorResolver color);
}
