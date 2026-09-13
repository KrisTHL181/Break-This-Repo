package net.minecraft.client;

import com.mojang.serialization.Codec;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public enum InactivityFpsLimit implements StringRepresentable {
   MINIMIZED("minimized", "options.inactivityFpsLimit.minimized"),
   AFK("afk", "options.inactivityFpsLimit.afk");

   public static final Codec<InactivityFpsLimit> CODEC = StringRepresentable.fromEnum(InactivityFpsLimit::values);
   private final String serializedName;
   private final Component caption;

   InactivityFpsLimit(final String p_363713_, final String p_363219_) {
      this.serializedName = p_363713_;
      this.caption = Component.translatable(p_363219_);
   }

   public Component caption() {
      return this.caption;
   }

   @Override
   public String getSerializedName() {
      return this.serializedName;
   }
}
