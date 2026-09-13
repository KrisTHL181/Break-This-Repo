package net.minecraft.client.gui.navigation;

public enum ScreenAxis {
   HORIZONTAL,
   VERTICAL;

   public ScreenAxis orthogonal() {
      return switch (this) {
         case HORIZONTAL -> VERTICAL;
         case VERTICAL -> HORIZONTAL;
      };
   }

   public ScreenDirection getPositive() {
      return switch (this) {
         case HORIZONTAL -> ScreenDirection.RIGHT;
         case VERTICAL -> ScreenDirection.DOWN;
      };
   }

   public ScreenDirection getNegative() {
      return switch (this) {
         case HORIZONTAL -> ScreenDirection.LEFT;
         case VERTICAL -> ScreenDirection.UP;
      };
   }

   public ScreenDirection getDirection(final boolean positive) {
      return positive ? this.getPositive() : this.getNegative();
   }
}
