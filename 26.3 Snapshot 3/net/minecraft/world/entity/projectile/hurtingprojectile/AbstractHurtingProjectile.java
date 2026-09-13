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

   protected AbstractHurtingProjectile(final EntityType<? extends AbstractHurtingProjectile> type, final Level level) {
      super(type, level);
   }

   protected AbstractHurtingProjectile(
      final EntityType<? extends AbstractHurtingProjectile> type, final double x, final double y, final double z, final Level level
   ) {
      this(type, level);
      this.setPos(x, y, z);
   }

   public AbstractHurtingProjectile(
      final EntityType<? extends AbstractHurtingProjectile> type, final double x, final double y, final double z, final Vec3 direction, final Level level
   ) {
      this(type, level);
      this.snapTo(x, y, z, this.getYRot(), this.getXRot());
      this.reapplyPosition();
      this.assignDirectionalMovement(direction, this.accelerationPower);
   }

   public AbstractHurtingProjectile(final EntityType<? extends AbstractHurtingProjectile> type, final LivingEntity mob, final Vec3 direction, final Level level) {
      this(type, mob.getX(), mob.getY(), mob.getZ(), direction, level);
      this.setOwner(mob);
      this.setRot(mob.getYRot(), mob.getXRot());
   }

   @Override
   protected void defineSynchedData(final SynchedEntityData.Builder entityData) {
   }

   @Override
   public boolean shouldRenderAtSqrDistance(final double distance) {
      double size = this.getBoundingBox().getSize() * 4.0;
      if (Double.isNaN(size)) {
         size = 4.0;
      }

      size *= 64.0;
      return distance < size * size;
   }

   protected ClipContext.Block getClipType() {
      return ClipContext.Block.COLLIDER;
   }

   @Override
   public void tick() {
      Entity owner = this.getOwner();
      this.applyInertia();
      if (this.level().isClientSide() || (owner == null || !owner.isRemoved()) && this.level().hasChunkAt(this.blockPosition())) {
         HitResult hitResult = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity, this.getClipType());
         Vec3 newPosition;
         if (hitResult.getType() != HitResult.Type.MISS) {
            newPosition = hitResult.getLocation();
         } else {
            newPosition = this.position().add(this.getDeltaMovement());
         }

         ProjectileUtil.rotateTowardsMovement(this, 0.2F);
         this.setPos(newPosition);
         this.applyEffectsFromBlocks();
         super.tick();
         if (this.shouldBurn()) {
            this.igniteForSeconds(1.0F);
         }

         if (hitResult.getType() != HitResult.Type.MISS && this.isAlive()) {
            this.hitTargetOrDeflectSelf(hitResult);
         }

         this.createParticleTrail();
      } else {
         this.discard();
      }
   }

   private void applyInertia() {
      Vec3 movement = this.getDeltaMovement();
      Vec3 position = this.position();
      float inertia;
      if (this.isInWater()) {
         for (int i = 0; i < 4; i++) {
            float s = 0.25F;
            this.level()
               .addParticle(
                  ParticleTypes.BUBBLE,
                  position.x - movement.x * 0.25,
                  position.y - movement.y * 0.25,
                  position.z - movement.z * 0.25,
                  movement.x,
                  movement.y,
                  movement.z
               );
         }

         inertia = this.getLiquidInertia();
      } else {
         inertia = this.getInertia();
      }

      this.setDeltaMovement(movement.add(movement.normalize().scale(this.accelerationPower)).scale(inertia));
   }

   private void createParticleTrail() {
      ParticleOptions trailParticle = this.getTrailParticle();
      Vec3 position = this.position();
      if (trailParticle != null) {
         this.level().addParticle(trailParticle, position.x, position.y + 0.5, position.z, 0.0, 0.0, 0.0);
      }
   }

   @Override
   public boolean hurtServer(final ServerLevel level, final DamageSource source, final float damage) {
      return false;
   }

   @Override
   protected boolean canHitEntity(final Entity entity) {
      return super.canHitEntity(entity) && !entity.noPhysics;
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
   protected void addAdditionalSaveData(final ValueOutput output) {
      super.addAdditionalSaveData(output);
      output.putDouble("acceleration_power", this.accelerationPower);
   }

   @Override
   protected void readAdditionalSaveData(final ValueInput input) {
      super.readAdditionalSaveData(input);
      this.accelerationPower = input.getDoubleOr("acceleration_power", 0.1);
   }

   @Override
   public float getLightLevelDependentMagicValue() {
      return 1.0F;
   }

   private void assignDirectionalMovement(final Vec3 direction, final double speed) {
      this.setDeltaMovement(direction.normalize().scale(speed));
      this.needsSync = true;
   }

   @Override
   protected void onDeflection(final boolean byAttack) {
      super.onDeflection(byAttack);
      if (byAttack) {
         this.accelerationPower = 0.1;
      } else {
         this.accelerationPower *= 0.5;
      }
   }
}
