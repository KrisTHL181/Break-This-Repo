package net.minecraft.client.gui.components.debug;

import net.minecraft.util.StringRepresentable;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public enum DebugScreenProfile implements StringRepresentable {
   DEFAULT("default", "debug.options.profile.default"),
   PERFORMANCE("performance", "debug.options.profile.performance");

   public static final StringRepresentable.EnumCodec<DebugScreenProfile> CODEC = StringRepresentable.fromEnum(DebugScreenProfile::values);
   private final String name;
   private final String translationKey;

   DebugScreenProfile(final String p_422723_, final String p_426635_) {
      this.name = p_422723_;
      this.translationKey = p_426635_;
   }

   public String translationKey() {
      return this.translationKey;
   }

   @Override
   public String getSerializedName() {
      return this.name;
   }
}
