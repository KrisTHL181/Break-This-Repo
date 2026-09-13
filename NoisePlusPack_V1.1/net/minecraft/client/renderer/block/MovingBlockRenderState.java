package net.minecraft.client.renderer.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.EmptyBlockAndTintGetter;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class MovingBlockRenderState implements BlockAndTintGetter {
   public BlockPos randomSeedPos = BlockPos.ZERO;
   public BlockPos blockPos = BlockPos.ZERO;
   public BlockState blockState = Blocks.AIR.defaultBlockState();
   public @Nullable Holder<Biome> biome;
   public BlockAndTintGetter level = EmptyBlockAndTintGetter.INSTANCE;

   @Override
   public float getShade(Direction p_426830_, boolean p_425350_) {
      return this.level.getShade(p_426830_, p_425350_);
   }

   @Override
   public LevelLightEngine getLightEngine() {
      return this.level.getLightEngine();
   }

   @Override
   public int getBlockTint(BlockPos p_428654_, ColorResolver p_422389_) {
      return this.biome == null ? -1 : p_422389_.getColor(this.biome.value(), p_428654_.getX(), p_428654_.getZ());
   }

   @Override
   public @Nullable BlockEntity getBlockEntity(BlockPos p_431119_) {
      return null;
   }

   @Override
   public BlockState getBlockState(BlockPos p_429414_) {
      return p_429414_.equals(this.blockPos) ? this.blockState : Blocks.AIR.defaultBlockState();
   }

   @Override
   public FluidState getFluidState(BlockPos p_423286_) {
      return this.getBlockState(p_423286_).getFluidState();
   }

   @Override
   public int getHeight() {
      return 1;
   }

   @Override
   public int getMinY() {
      return this.blockPos.getY();
   }
}
