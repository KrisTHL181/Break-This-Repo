package net.minecraft.client;

import com.mojang.serialization.Codec;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public enum CloudStatus implements StringRepresentable {
   OFF("false", "options.off"),
   FAST("fast", "options.clouds.fast"),
   FANCY("true", "options.clouds.fancy");

   public static final Codec<CloudStatus> CODEC = StringRepresentable.fromEnum(CloudStatus::values);
   private final String legacyName;
   private final Component caption;

   CloudStatus(final String p_231335_, final String p_298796_) {
      this.legacyName = p_231335_;
      this.caption = Component.translatable(p_298796_);
   }

   public Component caption() {
      return this.caption;
   }

   @Override
   public String getSerializedName() {
      return this.legacyName;
   }
}
