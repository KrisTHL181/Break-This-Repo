package net.minecraft.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Vector2i;

@OnlyIn(Dist.CLIENT)
public class ScrollWheelHandler {
   private double accumulatedScrollX;
   private double accumulatedScrollY;

   public Vector2i onMouseScroll(double p_360932_, double p_361863_) {
      if (this.accumulatedScrollX != 0.0 && Math.signum(p_360932_) != Math.signum(this.accumulatedScrollX)) {
         this.accumulatedScrollX = 0.0;
      }

      if (this.accumulatedScrollY != 0.0 && Math.signum(p_361863_) != Math.signum(this.accumulatedScrollY)) {
         this.accumulatedScrollY = 0.0;
      }

      this.accumulatedScrollX += p_360932_;
      this.accumulatedScrollY += p_361863_;
      int i = (int)this.accumulatedScrollX;
      int j = (int)this.accumulatedScrollY;
      if (i == 0 && j == 0) {
         return new Vector2i(0, 0);
      }

      this.accumulatedScrollX -= i;
      this.accumulatedScrollY -= j;
      return new Vector2i(i, j);
   }

   public static int getNextScrollWheelSelection(double p_363884_, int p_366040_, int p_361773_) {
      int i = (int)Math.signum(p_363884_);
      p_366040_ -= i;
      p_366040_ = Math.max(-1, p_366040_);

      while (p_366040_ < 0) {
         p_366040_ += p_361773_;
      }

      while (p_366040_ >= p_361773_) {
         p_366040_ -= p_361773_;
      }

      return p_366040_;
   }
}
