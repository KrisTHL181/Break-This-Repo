package net.minecraft.world.effect;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

class HungerMobEffect extends MobEffect {
   protected HungerMobEffect(final MobEffectCategory category, final int color) {
      super(category, color);
   }

   @Override
   public boolean applyEffectTick(final ServerLevel serverLevel, final LivingEntity mob, final int amplification) {
      if (mob instanceof Player player) {
         player.causeFoodExhaustion(0.005F * (amplification + 1));
      }

      return true;
   }

   @Override
   public boolean shouldApplyEffectTickThisTick(final int tickCount, final int amplification) {
      return true;
   }
}
