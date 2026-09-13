package net.minecraft.client.gui.components.debug;

import net.minecraft.util.StringRepresentable;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public enum DebugScreenEntryStatus implements StringRepresentable {
   ALWAYS_ON("alwaysOn"),
   IN_OVERLAY("inOverlay"),
   NEVER("never");

   public static final StringRepresentable.EnumCodec<DebugScreenEntryStatus> CODEC = StringRepresentable.fromEnum(DebugScreenEntryStatus::values);
   private final String name;

   DebugScreenEntryStatus(final String p_428442_) {
      this.name = p_428442_;
   }

   @Override
   public String getSerializedName() {
      return this.name;
   }
}
