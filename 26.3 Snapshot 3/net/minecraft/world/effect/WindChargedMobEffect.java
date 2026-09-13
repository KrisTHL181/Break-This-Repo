package net.minecraft.world.effect;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.hurtingprojectile.windcharge.AbstractWindCharge;
import net.minecraft.world.level.Level;

class WindChargedMobEffect extends MobEffect {
   protected WindChargedMobEffect(final MobEffectCategory category, final int color) {
      super(category, color, ParticleTypes.SMALL_GUST);
   }

   @Override
   public void onMobRemoved(final ServerLevel level, final LivingEntity mob, final int amplifier, final Entity.RemovalReason reason) {
      if (reason == Entity.RemovalReason.KILLED) {
         double x = mob.getX();
         double y = mob.getY() + mob.getBbHeight() / 2.0F;
         double z = mob.getZ();
         float gustStrength = 3.0F + mob.getRandom().nextFloat() * 2.0F;
         level.explode(
            mob,
            null,
            AbstractWindCharge.EXPLOSION_DAMAGE_CALCULATOR,
            x,
            y,
            z,
            gustStrength,
            false,
            Level.ExplosionInteraction.TRIGGER,
            ParticleTypes.GUST_EMITTER_SMALL,
            ParticleTypes.GUST_EMITTER_LARGE,
            WeightedList.of(),
            SoundEvents.BREEZE_WIND_CHARGE_BURST
         );
      }
   }
}
