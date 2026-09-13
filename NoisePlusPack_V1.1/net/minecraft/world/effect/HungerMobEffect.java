package net.minecraft.world.effect;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

class HungerMobEffect extends MobEffect {
   protected HungerMobEffect(MobEffectCategory p_299451_, int p_297803_) {
      super(p_299451_, p_297803_);
   }

   @Override
   public boolean applyEffectTick(ServerLevel p_365905_, LivingEntity p_301304_, int p_301079_) {
      if (p_301304_ instanceof Player player) {
         player.causeFoodExhaustion(0.005F * (p_301079_ + 1));
      }

      return true;
   }

   @Override
   public boolean shouldApplyEffectTickThisTick(int p_301244_, int p_298950_) {
      return true;
   }
}
