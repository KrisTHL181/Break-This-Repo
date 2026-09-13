package net.minecraft.util;

public class SmoothDouble {
   private double targetValue;
   private double remainingValue;
   private double lastAmount;

   public double getNewDeltaValue(final double targetDelta, final double time) {
      this.targetValue += targetDelta;
      double delta = this.targetValue - this.remainingValue;
      double newLastAmount = Mth.lerp(0.5, this.lastAmount, delta);
      double deltaSign = Math.signum(delta);
      if (deltaSign * delta > deltaSign * this.lastAmount) {
         delta = newLastAmount;
      }

      this.lastAmount = newLastAmount;
      this.remainingValue += delta * time;
      return delta * time;
   }

   public void reset() {
      this.targetValue = 0.0;
      this.remainingValue = 0.0;
      this.lastAmount = 0.0;
   }
}
