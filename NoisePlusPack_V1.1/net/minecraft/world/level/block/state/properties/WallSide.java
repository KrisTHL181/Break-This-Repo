package net.minecraft.world.level.block.state.properties;

import net.minecraft.util.StringRepresentable;

public enum WallSide implements StringRepresentable {
   NONE("none"),
   LOW("low"),
   TALL("tall");

   private final String name;

   WallSide(final String p_61824_) {
      this.name = p_61824_;
   }

   @Override
   public String toString() {
      return this.getSerializedName();
   }

   @Override
   public String getSerializedName() {
      return this.name;
   }
}
