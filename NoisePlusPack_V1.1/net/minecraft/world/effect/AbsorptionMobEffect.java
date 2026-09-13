package net.minecraft.world.effect;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;

class AbsorptionMobEffect extends MobEffect {
   protected AbsorptionMobEffect(MobEffectCategory p_300567_, int p_300827_) {
      super(p_300567_, p_300827_);
   }

   @Override
   public boolean applyEffectTick(ServerLevel p_364113_, LivingEntity p_298017_, int p_299434_) {
      return p_298017_.getAbsorptionAmount() > 0.0F;
   }

   @Override
   public boolean shouldApplyEffectTickThisTick(int p_299365_, int p_298390_) {
      return true;
   }

   @Override
   public void onEffectStarted(LivingEntity p_298184_, int p_297925_) {
      super.onEffectStarted(p_298184_, p_297925_);
      p_298184_.setAbsorptionAmount(Math.max(p_298184_.getAbsorptionAmount(), 4 * (1 + p_297925_)));
   }
}
