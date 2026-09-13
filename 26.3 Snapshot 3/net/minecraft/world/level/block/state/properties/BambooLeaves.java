package net.minecraft.world.level.block.state.properties;

import net.minecraft.util.StringRepresentable;

public enum BambooLeaves implements StringRepresentable {
   NONE("none"),
   SMALL("small"),
   LARGE("large");

   private final String name;

   BambooLeaves(final String name) {
      this.name = name;
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
