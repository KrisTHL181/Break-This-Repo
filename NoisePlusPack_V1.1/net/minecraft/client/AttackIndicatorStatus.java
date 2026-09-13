package net.minecraft.client;

import com.mojang.serialization.Codec;
import java.util.function.IntFunction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ByIdMap;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public enum AttackIndicatorStatus {
   OFF(0, "options.off"),
   CROSSHAIR(1, "options.attack.crosshair"),
   HOTBAR(2, "options.attack.hotbar");

   private static final IntFunction<AttackIndicatorStatus> BY_ID = ByIdMap.continuous(p_452886_ -> p_452886_.id, values(), ByIdMap.OutOfBoundsStrategy.WRAP);
   public static final Codec<AttackIndicatorStatus> LEGACY_CODEC = Codec.INT.xmap(BY_ID::apply, p_460505_ -> p_460505_.id);
   private final int id;
   private final Component caption;

   AttackIndicatorStatus(final int p_90506_, final String p_90507_) {
      this.id = p_90506_;
      this.caption = Component.translatable(p_90507_);
   }

   public Component caption() {
      return this.caption;
   }
}
