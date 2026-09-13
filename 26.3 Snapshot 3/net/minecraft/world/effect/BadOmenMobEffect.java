package net.minecraft.world.effect;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.raid.Raid;

class BadOmenMobEffect extends MobEffect {
   protected BadOmenMobEffect(final MobEffectCategory category, final int color) {
      super(category, color);
   }

   @Override
   public boolean shouldApplyEffectTickThisTick(final int remainingDuration, final int amplification) {
      return true;
   }

   @Override
   public boolean applyEffectTick(final ServerLevel level, final LivingEntity mob, final int amplification) {
      if (mob instanceof ServerPlayer player
         && !player.isSpectator()
         && level.getDifficulty() != Difficulty.PEACEFUL
         && level.isVillage(player.blockPosition())) {
         Raid raid = level.getRaidAt(player.blockPosition());
         if (raid == null || raid.getRaidOmenLevel() < raid.getMaxRaidOmenLevel()) {
            player.addEffect(new MobEffectInstance(MobEffects.RAID_OMEN, 600, amplification));
            player.setRaidOmenPosition(player.blockPosition());
            return false;
         }
      }

      return true;
   }
}
