package net.minecraft.world.level.timers;

import com.mojang.serialization.MapCodec;

public interface TimerCallback<T> {
   void handle(final T context, TimerQueue<T> queue, long time);

   MapCodec<? extends TimerCallback<T>> codec();
}
