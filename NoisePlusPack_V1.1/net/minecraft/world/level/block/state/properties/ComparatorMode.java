package net.minecraft.world.level.block.state.properties;

import net.minecraft.util.StringRepresentable;

public enum ComparatorMode implements StringRepresentable {
   COMPARE("compare"),
   SUBTRACT("subtract");

   private final String name;

   ComparatorMode(final String p_61534_) {
      this.name = p_61534_;
   }

   @Override
   public String toString() {
      return this.name;
   }

   @Override
   public String getSerializedName() {
      return this.name;
   }
}
