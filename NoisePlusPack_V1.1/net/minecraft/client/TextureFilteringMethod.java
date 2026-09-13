package net.minecraft.client;

import com.mojang.serialization.Codec;
import java.util.function.IntFunction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ByIdMap;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public enum TextureFilteringMethod {
   NONE(0, "options.textureFiltering.none"),
   RGSS(1, "options.textureFiltering.rgss"),
   ANISOTROPIC(2, "options.textureFiltering.anisotropic");

   private static final IntFunction<TextureFilteringMethod> BY_ID = ByIdMap.continuous(p_460616_ -> p_460616_.id, values(), ByIdMap.OutOfBoundsStrategy.WRAP);
   public static final Codec<TextureFilteringMethod> LEGACY_CODEC = Codec.INT.xmap(BY_ID::apply, p_454072_ -> p_454072_.id);
   private final int id;
   private final Component caption;

   TextureFilteringMethod(final int p_452929_, final String p_450172_) {
      this.id = p_452929_;
      this.caption = Component.translatable(p_450172_);
   }

   public Component caption() {
      return this.caption;
   }
}
