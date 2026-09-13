package net.minecraft.client;

import com.mojang.serialization.Codec;
import java.util.function.IntFunction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ByIdMap;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public enum PrioritizeChunkUpdates {
   NONE(0, "options.prioritizeChunkUpdates.none"),
   PLAYER_AFFECTED(1, "options.prioritizeChunkUpdates.byPlayer"),
   NEARBY(2, "options.prioritizeChunkUpdates.nearby");

   private static final IntFunction<PrioritizeChunkUpdates> BY_ID = ByIdMap.continuous(p_454102_ -> p_454102_.id, values(), ByIdMap.OutOfBoundsStrategy.WRAP);
   public static final Codec<PrioritizeChunkUpdates> LEGACY_CODEC = Codec.INT.xmap(BY_ID::apply, p_455923_ -> p_455923_.id);
   private final int id;
   private final Component caption;

   PrioritizeChunkUpdates(final int p_193784_, final String p_193785_) {
      this.id = p_193784_;
      this.caption = Component.translatable(p_193785_);
   }

   public Component caption() {
      return this.caption;
   }
}
