package net.minecraft.world.effect;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

class RaidOmenMobEffect extends MobEffect {
   protected RaidOmenMobEffect(final MobEffectCategory category, final int color, final ParticleOptions particleOptions) {
      super(category, color, particleOptions);
   }

   @Override
   public boolean shouldApplyEffectTickThisTick(final int remainingDuration, final int amplification) {
      return remainingDuration == 1;
   }

   @Override
   public boolean applyEffectTick(final ServerLevel level, final LivingEntity mob, final int amplification) {
      if (mob instanceof ServerPlayer player && !mob.isSpectator()) {
         BlockPos raidOmenPosition = player.getRaidOmenPosition();
         if (raidOmenPosition != null) {
            level.getRaids().createOrExtendRaid(player, raidOmenPosition);
            player.clearRaidOmenPosition();
            return false;
         }
      }

      return true;
   }
}
