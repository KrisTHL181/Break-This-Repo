package net.minecraft.client.gui.navigation;

import it.unimi.dsi.fastutil.ints.IntComparator;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public enum ScreenDirection {
   UP,
   DOWN,
   LEFT,
   RIGHT;

   private final IntComparator coordinateValueComparator = (p_265081_, p_265641_) -> p_265081_ == p_265641_
      ? 0
      : (this.isBefore(p_265081_, p_265641_) ? -1 : 1);

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

   public boolean isAfter(int p_265461_, int p_265553_) {
      return this.isPositive() ? p_265461_ > p_265553_ : p_265553_ > p_265461_;
   }

   public boolean isBefore(int p_265215_, int p_265040_) {
      return this.isPositive() ? p_265215_ < p_265040_ : p_265040_ < p_265215_;
   }

   public IntComparator coordinateValueComparator() {
      return this.coordinateValueComparator;
   }
}
