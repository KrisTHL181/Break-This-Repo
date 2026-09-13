package net.minecraft.world.effect;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

class SaturationMobEffect extends InstantenousMobEffect {
   protected SaturationMobEffect(MobEffectCategory p_297998_, int p_300022_) {
      super(p_297998_, p_300022_);
   }

   @Override
   public boolean applyEffectTick(ServerLevel p_368977_, LivingEntity p_300503_, int p_301046_) {
      if (p_300503_ instanceof Player player) {
         player.getFoodData().eat(p_301046_ + 1, 1.0F);
      }

      return true;
   }
}
