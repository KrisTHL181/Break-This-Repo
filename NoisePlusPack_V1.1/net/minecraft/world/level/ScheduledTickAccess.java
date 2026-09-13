package net.minecraft.world.level;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.ticks.LevelTickAccess;
import net.minecraft.world.ticks.ScheduledTick;
import net.minecraft.world.ticks.TickPriority;

public interface ScheduledTickAccess {
   <T> ScheduledTick<T> createTick(BlockPos var1, T var2, int var3, TickPriority var4);

   <T> ScheduledTick<T> createTick(BlockPos var1, T var2, int var3);

   LevelTickAccess<Block> getBlockTicks();

   default void scheduleTick(BlockPos p_361445_, Block p_367041_, int p_364859_, TickPriority p_363121_) {
      this.getBlockTicks().schedule(this.createTick(p_361445_, p_367041_, p_364859_, p_363121_));
   }

   default void scheduleTick(BlockPos p_362063_, Block p_364161_, int p_361870_) {
      this.getBlockTicks().schedule(this.createTick(p_362063_, p_364161_, p_361870_));
   }

   LevelTickAccess<Fluid> getFluidTicks();

   default void scheduleTick(BlockPos p_367841_, Fluid p_370049_, int p_367971_, TickPriority p_367815_) {
      this.getFluidTicks().schedule(this.createTick(p_367841_, p_370049_, p_367971_, p_367815_));
   }

   default void scheduleTick(BlockPos p_367769_, Fluid p_368705_, int p_368160_) {
      this.getFluidTicks().schedule(this.createTick(p_367769_, p_368705_, p_368160_));
   }
}
