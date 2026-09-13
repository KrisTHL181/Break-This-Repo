package net.minecraft.world.level.block.state.properties;

import net.minecraft.util.StringRepresentable;

public enum RedstoneSide implements StringRepresentable {
   UP("up"),
   SIDE("side"),
   NONE("none");

   private final String name;

   RedstoneSide(final String p_61759_) {
      this.name = p_61759_;
   }

   @Override
   public String toString() {
      return this.getSerializedName();
   }

   @Override
   public String getSerializedName() {
      return this.name;
   }

   public boolean isConnected() {
      return this != NONE;
   }
}
