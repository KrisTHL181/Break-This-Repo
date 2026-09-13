package net.minecraft.world.level.block.state.properties;

import net.minecraft.util.StringRepresentable;

public enum CreakingHeartState implements StringRepresentable {
   UPROOTED("uprooted"),
   DORMANT("dormant"),
   AWAKE("awake");

   private final String name;

   CreakingHeartState(final String name) {
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
