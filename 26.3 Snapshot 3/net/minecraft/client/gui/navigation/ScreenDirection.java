package net.minecraft.client.gui.navigation;

import it.unimi.dsi.fastutil.ints.IntComparator;

public enum ScreenDirection {
   UP,
   DOWN,
   LEFT,
   RIGHT;

   private final IntComparator coordinateValueComparator = (k1, k2) -> k1 == k2 ? 0 : (this.isBefore(k1, k2) ? -1 : 1);

   public ScreenAxis getAxis() {
      return switch (this) {
         case UP, DOWN -> ScreenAxis.VERTICAL;
         case LEFT, RIGHT -> ScreenAxis.HORIZONTAL;
      };
   }

   public ScreenDirection getOpposite() {
      return switch (this) {
         case UP -> DOWN;
         case DOWN -> UP;
         case LEFT -> RIGHT;
         case RIGHT -> LEFT;
      };
   }

   public boolean isPositive() {
      return switch (this) {
         case UP, LEFT -> false;
         case DOWN, RIGHT -> true;
      };
   }

   public boolean isAfter(final int a, final int b) {
      return this.isPositive() ? a > b : b > a;
   }

   public boolean isBefore(final int a, final int b) {
      return this.isPositive() ? a < b : b < a;
   }

   public IntComparator coordinateValueComparator() {
      return this.coordinateValueComparator;
   }
}
