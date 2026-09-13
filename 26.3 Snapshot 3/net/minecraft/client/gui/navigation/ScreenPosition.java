package net.minecraft.client.gui.navigation;

public record ScreenPosition(int x, int y) {
   public static ScreenPosition of(final ScreenAxis axis, final int primaryValue, final int secondaryValue) {
      return switch (axis) {
         case HORIZONTAL -> new ScreenPosition(primaryValue, secondaryValue);
         case VERTICAL -> new ScreenPosition(secondaryValue, primaryValue);
      };
   }

   public ScreenPosition step(final ScreenDirection direction) {
      return switch (direction) {
         case DOWN -> new ScreenPosition(this.x, this.y + 1);
         case UP -> new ScreenPosition(this.x, this.y - 1);
         case LEFT -> new ScreenPosition(this.x - 1, this.y);
         case RIGHT -> new ScreenPosition(this.x + 1, this.y);
      };
   }

   public int getCoordinate(final ScreenAxis axis) {
      return switch (axis) {
         case HORIZONTAL -> this.x;
         case VERTICAL -> this.y;
      };
   }
}
