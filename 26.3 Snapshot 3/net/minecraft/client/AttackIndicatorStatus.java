package net.minecraft.client;

import com.mojang.serialization.Codec;
import java.util.function.IntFunction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ByIdMap;

public enum AttackIndicatorStatus {
   OFF(0, "options.off"),
   CROSSHAIR(1, "options.attack.crosshair"),
   HOTBAR(2, "options.attack.hotbar");

   private static final IntFunction<AttackIndicatorStatus> BY_ID = ByIdMap.continuous(s -> s.id, values(), ByIdMap.OutOfBoundsStrategy.WRAP);
   public static final Codec<AttackIndicatorStatus> LEGACY_CODEC = Codec.INT.xmap(BY_ID::apply, s -> s.id);
   private final int id;
   private final Component caption;

   AttackIndicatorStatus(final int id, final String key) {
      this.id = id;
      this.caption = Component.translatable(key);
   }

   public Component caption() {
      return this.caption;
   }
}
