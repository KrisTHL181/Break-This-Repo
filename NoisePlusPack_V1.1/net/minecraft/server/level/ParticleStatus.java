package net.minecraft.server.level;

import com.mojang.serialization.Codec;
import java.util.function.IntFunction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ByIdMap;

public enum ParticleStatus {
   ALL(0, "options.particles.all"),
   DECREASED(1, "options.particles.decreased"),
   MINIMAL(2, "options.particles.minimal");

   private static final IntFunction<ParticleStatus> BY_ID = ByIdMap.continuous(p_450286_ -> p_450286_.id, values(), ByIdMap.OutOfBoundsStrategy.WRAP);
   public static final Codec<ParticleStatus> LEGACY_CODEC = Codec.INT.xmap(BY_ID::apply, p_459768_ -> p_459768_.id);
   private final int id;
   private final Component caption;

   ParticleStatus(final int p_363607_, final String p_370037_) {
      this.id = p_363607_;
      this.caption = Component.translatable(p_370037_);
   }

   public Component caption() {
      return this.caption;
   }
}
