package net.minecraft.world.level.block.state.properties;

import net.minecraft.util.StringRepresentable;

public enum BellAttachType implements StringRepresentable {
   FLOOR("floor"),
   CEILING("ceiling"),
   SINGLE_WALL("single_wall"),
   DOUBLE_WALL("double_wall");

   private final String name;

   BellAttachType(final String p_61355_) {
      this.name = p_61355_;
   }

   @Override
   public String getSerializedName() {
      return this.name;
   }
}
