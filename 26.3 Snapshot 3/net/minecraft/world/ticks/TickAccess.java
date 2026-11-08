package net.minecraft.world.ticks;

import net.minecraft.core.BlockPos;

public interface TickAccess<T> {
   void schedule(ScheduledTick<T> tick);

   boolean hasScheduledTick(BlockPos pos, T type);

   int count();
}
