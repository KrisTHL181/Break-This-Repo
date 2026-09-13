package net.minecraft.world.level.block.state.properties;

import net.minecraft.util.StringRepresentable;

public enum Tilt implements StringRepresentable {
   NONE("none", true),
   UNSTABLE("unstable", false),
   PARTIAL("partial", true),
   FULL("full", true);

   private final String name;
   private final boolean causesVibration;

   Tilt(final String p_156082_, final boolean p_156083_) {
      this.name = p_156082_;
      this.causesVibration = p_156083_;
   }

   @Override
   public String getSerializedName() {
      return this.name;
   }

   public boolean causesVibration() {
      return this.causesVibration;
   }
}
