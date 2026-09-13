package net.minecraft.world.level;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.ticks.LevelTickAccess;
import net.minecraft.world.ticks.ScheduledTick;
import net.minecraft.world.ticks.TickPriority;

public interface ScheduledTickAccess {
   <T> ScheduledTick<T> createTick(BlockPos pos, T type, int tickDelay, TickPriority priority);

   <T> ScheduledTick<T> createTick(BlockPos pos, T type, int tickDelay);

   LevelTickAccess<Block> getBlockTicks();

   default void scheduleTick(final BlockPos pos, final Block type, final int tickDelay, final TickPriority priority) {
      this.getBlockTicks().schedule(this.createTick(pos, type, tickDelay, priority));
   }

   default void scheduleTick(final BlockPos pos, final Block type, final int tickDelay) {
      this.getBlockTicks().schedule(this.createTick(pos, type, tickDelay));
   }

   LevelTickAccess<Fluid> getFluidTicks();

   default void scheduleTick(final BlockPos pos, final Fluid type, final int tickDelay, final TickPriority priority) {
      this.getFluidTicks().schedule(this.createTick(pos, type, tickDelay, priority));
   }

   default void scheduleTick(final BlockPos pos, final Fluid type, final int tickDelay) {
      this.getFluidTicks().schedule(this.createTick(pos, type, tickDelay));
   }
}
