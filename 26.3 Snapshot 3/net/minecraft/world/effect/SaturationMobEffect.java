package net.minecraft.world.effect;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

class SaturationMobEffect extends InstantaneousMobEffect {
   protected SaturationMobEffect(final MobEffectCategory category, final int color) {
      super(category, color);
   }

   @Override
   public boolean applyEffectTick(final ServerLevel level, final LivingEntity mob, final int amplification) {
      if (mob instanceof Player player) {
         player.getFoodData().eat(amplification + 1, 1.0F);
      }

      return true;
   }
}
