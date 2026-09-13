package net.minecraft.world.ticks;

import net.minecraft.core.BlockPos;

public interface TickAccess<T> {
   void schedule(ScheduledTick<T> var1);

   boolean hasScheduledTick(BlockPos var1, T var2);

   int count();
}
