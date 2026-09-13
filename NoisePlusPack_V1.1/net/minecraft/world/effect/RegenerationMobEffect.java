package net.minecraft.world.effect;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;

class RegenerationMobEffect extends MobEffect {
   protected RegenerationMobEffect(MobEffectCategory p_298562_, int p_299015_) {
      super(p_298562_, p_299015_);
   }

   @Override
   public boolean applyEffectTick(ServerLevel p_365546_, LivingEntity p_301282_, int p_300945_) {
      if (p_301282_.getHealth() < p_301282_.getMaxHealth()) {
         p_301282_.heal(1.0F);
      }

      return true;
   }

   @Override
   public boolean shouldApplyEffectTickThisTick(int p_300189_, int p_298417_) {
      int i = 50 >> p_298417_;
      return i > 0 ? p_300189_ % i == 0 : true;
   }
}
