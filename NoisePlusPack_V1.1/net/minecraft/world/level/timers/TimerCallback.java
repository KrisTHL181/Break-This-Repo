package net.minecraft.world.level.timers;

import com.mojang.serialization.MapCodec;

public interface TimerCallback<T> {
   void handle(T var1, TimerQueue<T> var2, long var3);

   MapCodec<? extends TimerCallback<T>> codec();
}
