package net.minecraft.util;

public class TickThrottler {
   private final int incrementStep;
   private final int threshold;
   private int count;

   public TickThrottler(final int incrementStep, final int threshold) {
      this.incrementStep = incrementStep;
      this.threshold = threshold;
   }

   public void increment() {
      this.count = this.count + this.incrementStep;
   }

   public void tick() {
      if (this.count > 0) {
         this.count--;
      }
   }

   public boolean isUnderThreshold() {
      return this.threshold <= 0 || this.count < this.threshold;
   }
}
