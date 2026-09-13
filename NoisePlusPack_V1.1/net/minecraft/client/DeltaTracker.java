package net.minecraft.client;

import it.unimi.dsi.fastutil.floats.FloatUnaryOperator;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface DeltaTracker {
   DeltaTracker ZERO = new DeltaTracker.DefaultValue(0.0F);
   DeltaTracker ONE = new DeltaTracker.DefaultValue(1.0F);

   float getGameTimeDeltaTicks();

   float getGameTimeDeltaPartialTick(boolean var1);

   float getRealtimeDeltaTicks();

   @OnlyIn(Dist.CLIENT)
   class DefaultValue implements DeltaTracker {
      private final float value;

      DefaultValue(float p_343701_) {
         this.value = p_343701_;
      }

      @Override
      public float getGameTimeDeltaTicks() {
         return this.value;
      }

      @Override
      public float getGameTimeDeltaPartialTick(boolean p_344036_) {
         return this.value;
      }

      @Override
      public float getRealtimeDeltaTicks() {
         return this.value;
      }
   }

   @OnlyIn(Dist.CLIENT)
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

      public Timer(float p_343882_, long p_344080_, FloatUnaryOperator p_343677_) {
         this.msPerTick = 1000.0F / p_343882_;
         this.lastUiMs = this.lastMs = p_344080_;
         this.targetMsptProvider = p_343677_;
      }

      public int advanceTime(long p_343106_, boolean p_342855_) {
         this.advanceRealTime(p_343106_);
         return p_342855_ ? this.advanceGameTime(p_343106_) : 0;
      }

      private int advanceGameTime(long p_342679_) {
         this.deltaTicks = (float)(p_342679_ - this.lastMs) / this.targetMsptProvider.apply(this.msPerTick);
         this.lastMs = p_342679_;
         this.deltaTickResidual = this.deltaTickResidual + this.deltaTicks;
         int i = (int)this.deltaTickResidual;
         this.deltaTickResidual -= i;
         return i;
      }

      private void advanceRealTime(long p_342368_) {
         this.realtimeDeltaTicks = (float)(p_342368_ - this.lastUiMs) / this.msPerTick;
         this.lastUiMs = p_342368_;
      }

      public void updatePauseState(boolean p_342098_) {
         if (p_342098_) {
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

      public void updateFrozenState(boolean p_344005_) {
         this.frozen = p_344005_;
      }

      @Override
      public float getGameTimeDeltaTicks() {
         return this.deltaTicks;
      }

      @Override
      public float getGameTimeDeltaPartialTick(boolean p_344876_) {
         if (!p_344876_ && this.frozen) {
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
