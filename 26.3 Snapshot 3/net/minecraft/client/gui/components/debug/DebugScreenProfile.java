package net.minecraft.client.gui.components.debug;

import net.minecraft.util.StringRepresentable;

public enum DebugScreenProfile implements StringRepresentable {
   DEFAULT("default", "debug.options.profile.default"),
   PERFORMANCE("performance", "debug.options.profile.performance");

   public static final StringRepresentable.EnumCodec<DebugScreenProfile> CODEC = StringRepresentable.fromEnum(DebugScreenProfile::values);
   private final String name;
   private final String translationKey;

   DebugScreenProfile(final String name, final String translationKey) {
      this.name = name;
      this.translationKey = translationKey;
   }

   public String translationKey() {
      return this.translationKey;
   }

   @Override
   public String getSerializedName() {
      return this.name;
   }
}
