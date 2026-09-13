package net.minecraft.world.effect;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;

class RegenerationMobEffect extends MobEffect {
   protected RegenerationMobEffect(final MobEffectCategory category, final int color) {
      super(category, color);
   }

   @Override
   public boolean applyEffectTick(final ServerLevel level, final LivingEntity mob, final int amplification) {
      if (mob.getHealth() < mob.getMaxHealth()) {
         mob.heal(1.0F);
      }

      return true;
   }

   @Override
   public boolean shouldApplyEffectTickThisTick(final int tickCount, final int amplification) {
      int interval = 50 >> amplification;
      return interval > 0 ? tickCount % interval == 0 : true;
   }
}
