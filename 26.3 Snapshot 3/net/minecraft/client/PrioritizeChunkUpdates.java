package net.minecraft.client;

import com.mojang.serialization.Codec;
import java.util.function.IntFunction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ByIdMap;

public enum PrioritizeChunkUpdates {
   NONE(0, "options.prioritizeChunkUpdates.none"),
   PLAYER_AFFECTED(1, "options.prioritizeChunkUpdates.byPlayer"),
   NEARBY(2, "options.prioritizeChunkUpdates.nearby");

   private static final IntFunction<PrioritizeChunkUpdates> BY_ID = ByIdMap.continuous(p -> p.id, values(), ByIdMap.OutOfBoundsStrategy.WRAP);
   public static final Codec<PrioritizeChunkUpdates> LEGACY_CODEC = Codec.INT.xmap(BY_ID::apply, p -> p.id);
   private final int id;
   private final Component caption;

   PrioritizeChunkUpdates(final int id, final String key) {
      this.id = id;
      this.caption = Component.translatable(key);
   }

   public Component caption() {
      return this.caption;
   }
}
