package net.minecraft.client;

import com.mojang.serialization.Codec;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;

public enum CloudStatus implements StringRepresentable {
   OFF("false", "options.off"),
   FAST("fast", "options.clouds.fast"),
   FANCY("true", "options.clouds.fancy");

   public static final Codec<CloudStatus> CODEC = StringRepresentable.fromEnum(CloudStatus::values);
   private final String legacyName;
   private final Component caption;

   CloudStatus(final String legacyName, final String key) {
      this.legacyName = legacyName;
      this.caption = Component.translatable(key);
   }

   public Component caption() {
      return this.caption;
   }

   @Override
   public String getSerializedName() {
      return this.legacyName;
   }
}
