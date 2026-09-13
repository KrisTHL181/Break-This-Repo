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
   protected WindChargedMobEffect(MobEffectCategory p_332863_, int p_333215_) {
      super(p_332863_, p_333215_, ParticleTypes.SMALL_GUST);
   }

   @Override
   public void onMobRemoved(ServerLevel p_365553_, LivingEntity p_333151_, int p_331087_, Entity.RemovalReason p_335248_) {
      if (p_335248_ == Entity.RemovalReason.KILLED) {
         double d0 = p_333151_.getX();
         double d1 = p_333151_.getY() + p_333151_.getBbHeight() / 2.0F;
         double d2 = p_333151_.getZ();
         float f = 3.0F + p_333151_.getRandom().nextFloat() * 2.0F;
         p_365553_.explode(
            p_333151_,
            null,
            AbstractWindCharge.EXPLOSION_DAMAGE_CALCULATOR,
            d0,
            d1,
            d2,
            f,
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
