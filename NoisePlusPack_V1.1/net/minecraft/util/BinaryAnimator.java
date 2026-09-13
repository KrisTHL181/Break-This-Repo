package net.minecraft.util;

public class BinaryAnimator {
   private final int animationLength;
   private final EasingType easing;
   private int ticks;
   private int ticksOld;

   public BinaryAnimator(int p_368328_, EasingType p_459638_) {
      this.animationLength = p_368328_;
      this.easing = p_459638_;
   }

   public BinaryAnimator(int p_365144_) {
      this(p_365144_, EasingType.LINEAR);
   }

   public void tick(boolean p_364056_) {
      this.ticksOld = this.ticks;
      if (p_364056_) {
         if (this.ticks < this.animationLength) {
            this.ticks++;
         }
      } else if (this.ticks > 0) {
         this.ticks--;
      }
   }

   public float getFactor(float p_364595_) {
      float f = Mth.lerp(p_364595_, this.ticksOld, this.ticks) / this.animationLength;
      return this.easing.apply(f);
   }
}
