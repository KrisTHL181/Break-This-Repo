package net.minecraft.world.level.block.state.properties;

import net.minecraft.util.StringRepresentable;

public enum PotentSulfurState implements StringRepresentable {
   DRY("dry"),
   WET("wet"),
   DORMANT("dormant"),
   ERUPTING("erupting"),
   CONTINUOUS("continuous");

   private final String name;

   PotentSulfurState(final String name) {
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
