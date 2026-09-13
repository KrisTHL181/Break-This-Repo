package net.minecraft.world.entity;

import java.util.function.Consumer;

public class AnimationState {
   private static final int STOPPED = Integer.MIN_VALUE;
   private int startTick = Integer.MIN_VALUE;

   public void start(final int tickCount) {
      this.startTick = tickCount;
   }

   public void startIfStopped(final int tickCount) {
      if (!this.isStarted()) {
         this.start(tickCount);
      }
   }

   public void animateWhen(final boolean condition, final int tickCount) {
      if (condition) {
         this.startIfStopped(tickCount);
      } else {
         this.stop();
      }
   }

   public void stop() {
      this.startTick = Integer.MIN_VALUE;
   }

   public void ifStarted(final Consumer<AnimationState> timer) {
      if (this.isStarted()) {
         timer.accept(this);
      }
   }

   public void fastForward(final int ticks, final float timeScale) {
      if (this.isStarted()) {
         this.startTick -= (int)(ticks * timeScale);
      }
   }

   public long getTimeInMillis(final float ageInTicks) {
      float timeInTicks = ageInTicks - this.startTick;
      return (long)(timeInTicks * 50.0F);
   }

   public boolean isStarted() {
      return this.startTick != Integer.MIN_VALUE;
   }

   public void copyFrom(final AnimationState state) {
      this.startTick = state.startTick;
   }
}
