package net.minecraft.client;

import it.unimi.dsi.fastutil.floats.FloatUnaryOperator;

public interface DeltaTracker {
   DeltaTracker ZERO = new DeltaTracker.DefaultValue(0.0F);
   DeltaTracker ONE = new DeltaTracker.DefaultValue(1.0F);

   float getGameTimeDeltaTicks();

   float getGameTimeDeltaPartialTick(boolean ignoreFrozenGame);

   float getRealtimeDeltaTicks();

   class DefaultValue implements DeltaTracker {
      private final float value;

      private DefaultValue(final float value) {
         this.value = value;
      }

      @Override
      public float getGameTimeDeltaTicks() {
         return this.value;
      }

      @Override
      public float getGameTimeDeltaPartialTick(final boolean ignored) {
         return this.value;
      }

      @Override
      public float getRealtimeDeltaTicks() {
         return this.value;
      }
   }

   class Timer implements DeltaTracker {
      private float deltaTicks;
      private float deltaTickResidual;
      private float realtimeDeltaTicks;
      private float pausedDeltaTickResidual;
      private long lastMs;
      private long lastUiMs;
      private final float msPerTick;
      private final FloatUnaryOperator targetMsptProvider;
      private boolean paused;
      private boolean frozen;

      public Timer(final float ticksPerSecond, final long currentMs, final FloatUnaryOperator targetMsptProvider) {
         this.msPerTick = 1000.0F / ticksPerSecond;
         this.lastUiMs = this.lastMs = currentMs;
         this.targetMsptProvider = targetMsptProvider;
      }

      public int advanceGameTime(final long currentMs) {
         this.deltaTicks = (float)(currentMs - this.lastMs) / this.targetMsptProvider.apply(this.msPerTick);
         this.lastMs = currentMs;
         this.deltaTickResidual = this.deltaTickResidual + this.deltaTicks;
         int ticks = (int)this.deltaTickResidual;
         this.deltaTickResidual -= ticks;
         return ticks;
      }

      public void advanceRealTime(final long currentMs) {
         this.realtimeDeltaTicks = (float)(currentMs - this.lastUiMs) / this.msPerTick;
         this.lastUiMs = currentMs;
      }

      public void updatePauseState(final boolean pauseState) {
         if (pauseState) {
            this.pause();
         } else {
            this.unPause();
         }
      }

      private void pause() {
         if (!this.paused) {
            this.pausedDeltaTickResidual = this.deltaTickResidual;
         }

         this.paused = true;
      }

      private void unPause() {
         if (this.paused) {
            this.deltaTickResidual = this.pausedDeltaTickResidual;
         }

         this.paused = false;
      }

      public void updateFrozenState(final boolean frozen) {
         this.frozen = frozen;
      }

      @Override
      public float getGameTimeDeltaTicks() {
         return this.deltaTicks;
      }

      @Override
      public float getGameTimeDeltaPartialTick(final boolean ignoreFrozenGame) {
         if (!ignoreFrozenGame && this.frozen) {
            return 1.0F;
         } else {
            return this.paused ? this.pausedDeltaTickResidual : this.deltaTickResidual;
         }
      }

      @Override
      public float getRealtimeDeltaTicks() {
         return this.realtimeDeltaTicks > 7.0F ? 0.5F : this.realtimeDeltaTicks;
      }
   }
}
