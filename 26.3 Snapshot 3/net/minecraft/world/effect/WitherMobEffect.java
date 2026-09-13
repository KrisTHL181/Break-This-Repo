package net.minecraft.world.effect;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;

public class WitherMobEffect extends MobEffect {
   public static final int DAMAGE_INTERVAL = 40;

   protected WitherMobEffect(final MobEffectCategory category, final int color) {
      super(category, color);
   }

   @Override
   public boolean applyEffectTick(final ServerLevel level, final LivingEntity mob, final int amplification) {
      mob.hurtServer(level, mob.damageSources().wither(), 1.0F);
      return true;
   }

   @Override
   public boolean shouldApplyEffectTickThisTick(final int tickCount, final int amplification) {
      int interval = 40 >> amplification;
      return interval > 0 ? tickCount % interval == 0 : true;
   }
}
