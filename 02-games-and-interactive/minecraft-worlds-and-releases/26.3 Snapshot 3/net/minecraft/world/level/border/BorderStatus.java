package net.minecraft.world.level.border;

public enum BorderStatus {
   GROWING(4259712),
   SHRINKING(16724016),
   STATIONARY(2138367);

   private final int color;

   BorderStatus(final int color) {
      this.color = color;
   }

   public int getColor() {
      return this.color;
   }
}
