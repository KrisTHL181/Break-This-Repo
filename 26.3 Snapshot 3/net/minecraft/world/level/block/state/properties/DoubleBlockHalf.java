package net.minecraft.world.level.block.state.properties;

import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;

public enum DoubleBlockHalf implements StringRepresentable {
   UPPER(Direction.DOWN),
   LOWER(Direction.UP);

   private final Direction directionToOther;

   DoubleBlockHalf(final Direction directionToOther) {
      this.directionToOther = directionToOther;
   }

   public Direction getDirectionToOther() {
      return this.directionToOther;
   }

   @Override
   public String toString() {
      return this.getSerializedName();
   }

   @Override
   public String getSerializedName() {
      return this == UPPER ? "upper" : "lower";
   }

   public DoubleBlockHalf getOtherHalf() {
      return this == UPPER ? LOWER : UPPER;
   }
}
