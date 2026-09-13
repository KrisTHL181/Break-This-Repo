package net.minecraft.world.entity.projectile.hurtingprojectile;

import java.util.List;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.PowerParticleOption;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class DragonFireball extends AbstractHurtingProjectile {
   public static final float SPLASH_RANGE = 4.0F;

   public DragonFireball(EntityType<? extends DragonFireball> p_451756_, Level p_450201_) {
      super(p_451756_, p_450201_);
   }

   public DragonFireball(Level p_454224_, LivingEntity p_455715_, Vec3 p_451259_) {
      super(EntityType.DRAGON_FIREBALL, p_455715_, p_451259_, p_454224_);
   }

   @Override
   protected void onHit(HitResult p_453570_) {
      super.onHit(p_453570_);
      if ((p_453570_.getType() != HitResult.Type.ENTITY || !this.ownedBy(((EntityHitResult)p_453570_).getEntity())) && !this.level().isClientSide()) {
         List<LivingEntity> list = this.level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(4.0, 2.0, 4.0));
         AreaEffectCloud areaeffectcloud = new AreaEffectCloud(this.level(), this.getX(), this.getY(), this.getZ());
         Entity entity = this.getOwner();
         if (entity instanceof LivingEntity) {
            areaeffectcloud.setOwner((LivingEntity)entity);
         }

         areaeffectcloud.setCustomParticle(PowerParticleOption.create(ParticleTypes.DRAGON_BREATH, 1.0F));
         areaeffectcloud.setRadius(3.0F);
         areaeffectcloud.setDuration(600);
         areaeffectcloud.setRadiusPerTick((7.0F - areaeffectcloud.getRadius()) / areaeffectcloud.getDuration());
         areaeffectcloud.setPotionDurationScale(0.25F);
         areaeffectcloud.addEffect(new MobEffectInstance(MobEffects.INSTANT_DAMAGE, 1, 1));
         if (!list.isEmpty()) {
            for (LivingEntity livingentity : list) {
               double d0 = this.distanceToSqr(livingentity);
               if (d0 < 16.0) {
                  areaeffectcloud.setPos(livingentity.getX(), livingentity.getY(), livingentity.getZ());
                  break;
               }
            }
         }

         this.level().levelEvent(2006, this.blockPosition(), this.isSilent() ? -1 : 1);
         this.level().addFreshEntity(areaeffectcloud);
         this.discard();
      }
   }

   @Override
   protected ParticleOptions getTrailParticle() {
      return PowerParticleOption.create(ParticleTypes.DRAGON_BREATH, 1.0F);
   }

   @Override
   protected boolean shouldBurn() {
      return false;
   }
}
