package net.minecraft.world.effect;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;

class AbsorptionMobEffect extends MobEffect {
   protected AbsorptionMobEffect(final MobEffectCategory category, final int color) {
      super(category, color);
   }

   @Override
   public boolean applyEffectTick(final ServerLevel level, final LivingEntity mob, final int amplification) {
      return mob.getAbsorptionAmount() > 0.0F;
   }

   @Override
   public boolean shouldApplyEffectTickThisTick(final int tickCount, final int amplification) {
      return true;
   }

   @Override
   public void onEffectStarted(final LivingEntity mob, final int amplifier) {
      super.onEffectStarted(mob, amplifier);
      mob.setAbsorptionAmount(Math.max(mob.getAbsorptionAmount(), 4 * (1 + amplifier)));
   }
}
