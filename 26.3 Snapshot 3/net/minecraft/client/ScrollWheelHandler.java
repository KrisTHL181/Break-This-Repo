package net.minecraft.client;

import org.joml.Vector2i;

public class ScrollWheelHandler {
   private double accumulatedScrollX;
   private double accumulatedScrollY;

   public Vector2i onMouseScroll(final double scaledXScrollOffset, final double scaledYScrollOffset) {
      if (this.accumulatedScrollX != 0.0 && Math.signum(scaledXScrollOffset) != Math.signum(this.accumulatedScrollX)) {
         this.accumulatedScrollX = 0.0;
      }

      if (this.accumulatedScrollY != 0.0 && Math.signum(scaledYScrollOffset) != Math.signum(this.accumulatedScrollY)) {
         this.accumulatedScrollY = 0.0;
      }

      this.accumulatedScrollX += scaledXScrollOffset;
      this.accumulatedScrollY += scaledYScrollOffset;
      int wheelX = (int)this.accumulatedScrollX;
      int wheelY = (int)this.accumulatedScrollY;
      if (wheelX == 0 && wheelY == 0) {
         return new Vector2i(0, 0);
      }

      this.accumulatedScrollX -= wheelX;
      this.accumulatedScrollY -= wheelY;
      return new Vector2i(wheelX, wheelY);
   }

   public static int getNextScrollWheelSelection(final double wheel, int currentSelected, final int limit) {
      int step = (int)Math.signum(wheel);
      currentSelected -= step;
      currentSelected = Math.max(-1, currentSelected);

      while (currentSelected < 0) {
         currentSelected += limit;
      }

      while (currentSelected >= limit) {
         currentSelected -= limit;
      }

      return currentSelected;
   }
}
