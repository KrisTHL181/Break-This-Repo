package net.minecraft.world.entity.projectile.hurtingprojectile;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public abstract class AbstractHurtingProjectile extends Projectile {
   public static final double INITAL_ACCELERATION_POWER = 0.1;
   public static final double DEFLECTION_SCALE = 0.5;
   public double accelerationPower = 0.1;

   protected AbstractHurtingProjectile(EntityType<? extends AbstractHurtingProjectile> p_457837_, Level p_458149_) {
      super(p_457837_, p_458149_);
   }

   protected AbstractHurtingProjectile(
      EntityType<? extends AbstractHurtingProjectile> p_458478_, double p_455096_, double p_459735_, double p_450430_, Level p_454582_
   ) {
      this(p_458478_, p_454582_);
      this.setPos(p_455096_, p_459735_, p_450430_);
   }

   public AbstractHurtingProjectile(
      EntityType<? extends AbstractHurtingProjectile> p_459206_, double p_460419_, double p_459750_, double p_459941_, Vec3 p_454350_, Level p_458608_
   ) {
      this(p_459206_, p_458608_);
      this.snapTo(p_460419_, p_459750_, p_459941_, this.getYRot(), this.getXRot());
      this.reapplyPosition();
      this.assignDirectionalMovement(p_454350_, this.accelerationPower);
   }

   public AbstractHurtingProjectile(EntityType<? extends AbstractHurtingProjectile> p_460963_, LivingEntity p_453679_, Vec3 p_452061_, Level p_451508_) {
      this(p_460963_, p_453679_.getX(), p_453679_.getY(), p_453679_.getZ(), p_452061_, p_451508_);
      this.setOwner(p_453679_);
      this.setRot(p_453679_.getYRot(), p_453679_.getXRot());
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder p_458017_) {
   }

   @Override
   public boolean shouldRenderAtSqrDistance(double p_452279_) {
      double d0 = this.getBoundingBox().getSize() * 4.0;
      if (Double.isNaN(d0)) {
         d0 = 4.0;
      }

      d0 *= 64.0;
      return p_452279_ < d0 * d0;
   }

   protected ClipContext.Block getClipType() {
      return ClipContext.Block.COLLIDER;
   }

   @Override
   public void tick() {
      Entity entity = this.getOwner();
      this.applyInertia();
      if (this.level().isClientSide() || (entity == null || !entity.isRemoved()) && this.level().hasChunkAt(this.blockPosition())) {
         HitResult hitresult = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity, this.getClipType());
         Vec3 vec3;
         if (hitresult.getType() != HitResult.Type.MISS) {
            vec3 = hitresult.getLocation();
         } else {
            vec3 = this.position().add(this.getDeltaMovement());
         }

         ProjectileUtil.rotateTowardsMovement(this, 0.2F);
         this.setPos(vec3);
         this.applyEffectsFromBlocks();
         super.tick();
         if (this.shouldBurn()) {
            this.igniteForSeconds(1.0F);
         }

         if (hitresult.getType() != HitResult.Type.MISS && this.isAlive()) {
            this.hitTargetOrDeflectSelf(hitresult);
         }

         this.createParticleTrail();
      } else {
         this.discard();
      }
   }

   private void applyInertia() {
      Vec3 vec3 = this.getDeltaMovement();
      Vec3 vec31 = this.position();
      float f;
      if (this.isInWater()) {
         for (int i = 0; i < 4; i++) {
            float f1 = 0.25F;
            this.level().addParticle(ParticleTypes.BUBBLE, vec31.x - vec3.x * 0.25, vec31.y - vec3.y * 0.25, vec31.z - vec3.z * 0.25, vec3.x, vec3.y, vec3.z);
         }

         f = this.getLiquidInertia();
      } else {
         f = this.getInertia();
      }

      this.setDeltaMovement(vec3.add(vec3.normalize().scale(this.accelerationPower)).scale(f));
   }

   private void createParticleTrail() {
      ParticleOptions particleoptions = this.getTrailParticle();
      Vec3 vec3 = this.position();
      if (particleoptions != null) {
         this.level().addParticle(particleoptions, vec3.x, vec3.y + 0.5, vec3.z, 0.0, 0.0, 0.0);
      }
   }

   @Override
   public boolean hurtServer(ServerLevel p_456232_, DamageSource p_460956_, float p_452095_) {
      return false;
   }

   @Override
   protected boolean canHitEntity(Entity p_454572_) {
      return super.canHitEntity(p_454572_) && !p_454572_.noPhysics;
   }

   protected boolean shouldBurn() {
      return true;
   }

   protected @Nullable ParticleOptions getTrailParticle() {
      return ParticleTypes.SMOKE;
   }

   protected float getInertia() {
      return 0.95F;
   }

   protected float getLiquidInertia() {
      return 0.8F;
   }

   @Override
   protected void addAdditionalSaveData(ValueOutput p_454826_) {
      super.addAdditionalSaveData(p_454826_);
      p_454826_.putDouble("acceleration_power", this.accelerationPower);
   }

   @Override
   protected void readAdditionalSaveData(ValueInput p_451525_) {
      super.readAdditionalSaveData(p_451525_);
      this.accelerationPower = p_451525_.getDoubleOr("acceleration_power", 0.1);
   }

   @Override
   public float getLightLevelDependentMagicValue() {
      return 1.0F;
   }

   private void assignDirectionalMovement(Vec3 p_456838_, double p_453614_) {
      this.setDeltaMovement(p_456838_.normalize().scale(p_453614_));
      this.needsSync = true;
   }

   @Override
   protected void onDeflection(boolean p_456708_) {
      super.onDeflection(p_456708_);
      if (p_456708_) {
         this.accelerationPower = 0.1;
      } else {
         this.accelerationPower *= 0.5;
      }
   }
}
