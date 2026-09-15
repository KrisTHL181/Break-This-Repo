package net.minecraft.world.clock;

public interface ClockInstance {
   long totalTicks();

   float partialTick();

   float rate();

   boolean isPaused();
}
