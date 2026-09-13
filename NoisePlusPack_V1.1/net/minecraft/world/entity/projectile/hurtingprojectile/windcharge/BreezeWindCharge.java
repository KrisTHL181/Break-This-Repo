package net.minecraft.world.entity.projectile.hurtingprojectile.windcharge;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.breeze.Breeze;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class BreezeWindCharge extends AbstractWindCharge {
   private static final float RADIUS = 3.0F;

   public BreezeWindCharge(EntityType<? extends AbstractWindCharge> p_453894_, Level p_457761_) {
      super(p_453894_, p_457761_);
   }

   public BreezeWindCharge(Breeze p_454116_, Level p_454637_) {
      super(EntityType.BREEZE_WIND_CHARGE, p_454637_, p_454116_, p_454116_.getX(), p_454116_.getFiringYPosition(), p_454116_.getZ());
   }

   @Override
   protected void explode(Vec3 p_460556_) {
      this.level()
         .explode(
            this,
            null,
            EXPLOSION_DAMAGE_CALCULATOR,
            p_460556_.x(),
            p_460556_.y(),
            p_460556_.z(),
            3.0F,
            false,
            Level.ExplosionInteraction.TRIGGER,
            ParticleTypes.GUST_EMITTER_SMALL,
            ParticleTypes.GUST_EMITTER_LARGE,
            WeightedList.of(),
            SoundEvents.BREEZE_WIND_CHARGE_BURST
         );
   }
}
