package net.minecraft.world.entity.animal.squid;

import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.AgeableWaterCreature;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class Squid extends AgeableWaterCreature {
   public float xBodyRot;
   public float xBodyRotO;
   public float zBodyRot;
   public float zBodyRotO;
   public float tentacleMovement;
   public float oldTentacleMovement;
   public float tentacleAngle;
   public float oldTentacleAngle;
   private float speed;
   private float tentacleSpeed;
   private float rotateSpeed;
   Vec3 movementVector = Vec3.ZERO;

   public Squid(EntityType<? extends Squid> p_460116_, Level p_450800_) {
      super(p_460116_, p_450800_);
      this.random.setSeed(this.getId());
      this.tentacleSpeed = 1.0F / (this.random.nextFloat() + 1.0F) * 0.2F;
   }

   @Override
   protected void registerGoals() {
      this.goalSelector.addGoal(0, new Squid.SquidRandomMovementGoal(this));
      this.goalSelector.addGoal(1, new Squid.SquidFleeGoal());
   }

   public static AttributeSupplier.Builder createAttributes() {
      return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 10.0);
   }

   @Override
   protected SoundEvent getAmbientSound() {
      return SoundEvents.SQUID_AMBIENT;
   }

   @Override
   protected SoundEvent getHurtSound(DamageSource p_453146_) {
      return SoundEvents.SQUID_HURT;
   }

   @Override
   protected SoundEvent getDeathSound() {
      return SoundEvents.SQUID_DEATH;
   }

   protected SoundEvent getSquirtSound() {
      return SoundEvents.SQUID_SQUIRT;
   }

   @Override
   public boolean canBeLeashed() {
      return true;
   }

   @Override
   protected float getSoundVolume() {
      return 0.4F;
   }

   @Override
   protected Entity.MovementEmission getMovementEmission() {
      return Entity.MovementEmission.EVENTS;
   }

   @Override
   public @Nullable AgeableMob getBreedOffspring(ServerLevel p_450416_, AgeableMob p_458438_) {
      return EntityType.SQUID.create(p_450416_, EntitySpawnReason.BREEDING);
   }

   @Override
   protected double getDefaultGravity() {
      return 0.08;
   }

   @Override
   public void aiStep() {
      super.aiStep();
      this.xBodyRotO = this.xBodyRot;
      this.zBodyRotO = this.zBodyRot;
      this.oldTentacleMovement = this.tentacleMovement;
      this.oldTentacleAngle = this.tentacleAngle;
      this.tentacleMovement = this.tentacleMovement + this.tentacleSpeed;
      if (this.tentacleMovement > Math.PI * 2) {
         if (this.level().isClientSide()) {
            this.tentacleMovement = (float) (Math.PI * 2);
         } else {
            this.tentacleMovement -= (float) (Math.PI * 2);
            if (this.random.nextInt(10) == 0) {
               this.tentacleSpeed = 1.0F / (this.random.nextFloat() + 1.0F) * 0.2F;
            }

            this.level().broadcastEntityEvent(this, (byte)19);
         }
      }

      if (this.isInWater()) {
         if (this.tentacleMovement < (float) Math.PI) {
            float f = this.tentacleMovement / (float) Math.PI;
            this.tentacleAngle = Mth.sin(f * f * (float) Math.PI) * (float) Math.PI * 0.25F;
            if (f > 0.75) {
               if (this.isLocalInstanceAuthoritative()) {
                  this.setDeltaMovement(this.movementVector);
               }

               this.rotateSpeed = 1.0F;
            } else {
               this.rotateSpeed *= 0.8F;
            }
         } else {
            this.tentacleAngle = 0.0F;
            if (this.isLocalInstanceAuthoritative()) {
               this.setDeltaMovement(this.getDeltaMovement().scale(0.9));
            }

            this.rotateSpeed *= 0.99F;
         }

         Vec3 vec3 = this.getDeltaMovement();
         double d0 = vec3.horizontalDistance();
         this.yBodyRot = this.yBodyRot + (-((float)Mth.atan2(vec3.x, vec3.z)) * (180.0F / (float)Math.PI) - this.yBodyRot) * 0.1F;
         this.setYRot(this.yBodyRot);
         this.zBodyRot = this.zBodyRot + (float) Math.PI * this.rotateSpeed * 1.5F;
         this.xBodyRot = this.xBodyRot + (-((float)Mth.atan2(d0, vec3.y)) * (180.0F / (float)Math.PI) - this.xBodyRot) * 0.1F;
      } else {
         this.tentacleAngle = Mth.abs(Mth.sin(this.tentacleMovement)) * (float) Math.PI * 0.25F;
         if (!this.level().isClientSide()) {
            double d1 = this.getDeltaMovement().y;
            if (this.hasEffect(MobEffects.LEVITATION)) {
               d1 = 0.05 * (this.getEffect(MobEffects.LEVITATION).getAmplifier() + 1);
            } else {
               d1 -= this.getGravity();
            }

            this.setDeltaMovement(0.0, d1 * 0.98F, 0.0);
         }

         this.xBodyRot = this.xBodyRot + (-90.0F - this.xBodyRot) * 0.02F;
      }
   }

   @Override
   public boolean hurtServer(ServerLevel p_455515_, DamageSource p_456534_, float p_452348_) {
      if (super.hurtServer(p_455515_, p_456534_, p_452348_) && this.getLastHurtByMob() != null) {
         this.spawnInk();
         return true;
      } else {
         return false;
      }
   }

   private Vec3 rotateVector(Vec3 p_452860_) {
      Vec3 vec3 = p_452860_.xRot(this.xBodyRotO * (float) (Math.PI / 180.0));
      return vec3.yRot(-this.yBodyRotO * (float) (Math.PI / 180.0));
   }

   private void spawnInk() {
      this.makeSound(this.getSquirtSound());
      Vec3 vec3 = this.rotateVector(new Vec3(0.0, -1.0, 0.0)).add(this.getX(), this.getY(), this.getZ());

      for (int i = 0; i < 30; i++) {
         Vec3 vec31 = this.rotateVector(new Vec3(this.random.nextFloat() * 0.6 - 0.3, -1.0, this.random.nextFloat() * 0.6 - 0.3));
         float f = this.isBaby() ? 0.1F : 0.3F;
         Vec3 vec32 = vec31.scale(f + this.random.nextFloat() * 2.0F);
         ((ServerLevel)this.level()).sendParticles(this.getInkParticle(), vec3.x, vec3.y + 0.5, vec3.z, 0, vec32.x, vec32.y, vec32.z, 0.1F);
      }
   }

   protected ParticleOptions getInkParticle() {
      return ParticleTypes.SQUID_INK;
   }

   @Override
   public void travel(Vec3 p_452122_) {
      this.move(MoverType.SELF, this.getDeltaMovement());
   }

   @Override
   public void handleEntityEvent(byte p_458014_) {
      if (p_458014_ == 19) {
         this.tentacleMovement = 0.0F;
      } else {
         super.handleEntityEvent(p_458014_);
      }
   }

   public boolean hasMovementVector() {
      return this.movementVector.lengthSqr() > 1.0E-5F;
   }

   @Override
   public @Nullable SpawnGroupData finalizeSpawn(
      ServerLevelAccessor p_457368_, DifficultyInstance p_459752_, EntitySpawnReason p_453936_, @Nullable SpawnGroupData p_460753_
   ) {
      SpawnGroupData spawngroupdata = Objects.requireNonNullElseGet(p_460753_, () -> new AgeableMob.AgeableMobGroupData(0.05F));
      return super.finalizeSpawn(p_457368_, p_459752_, p_453936_, spawngroupdata);
   }

   class SquidFleeGoal extends Goal {
      private static final float SQUID_FLEE_SPEED = 3.0F;
      private static final float SQUID_FLEE_MIN_DISTANCE = 5.0F;
      private static final float SQUID_FLEE_MAX_DISTANCE = 10.0F;
      private int fleeTicks;

      @Override
      public boolean canUse() {
         LivingEntity livingentity = Squid.this.getLastHurtByMob();
         return Squid.this.isInWater() && livingentity != null ? Squid.this.distanceToSqr(livingentity) < 100.0 : false;
      }

      @Override
      public void start() {
         this.fleeTicks = 0;
      }

      @Override
      public boolean requiresUpdateEveryTick() {
         return true;
      }

      @Override
      public void tick() {
         this.fleeTicks++;
         LivingEntity livingentity = Squid.this.getLastHurtByMob();
         if (livingentity != null) {
            Vec3 vec3 = new Vec3(Squid.this.getX() - livingentity.getX(), Squid.this.getY() - livingentity.getY(), Squid.this.getZ() - livingentity.getZ());
            BlockState blockstate = Squid.this.level()
               .getBlockState(BlockPos.containing(Squid.this.getX() + vec3.x, Squid.this.getY() + vec3.y, Squid.this.getZ() + vec3.z));
            FluidState fluidstate = Squid.this.level()
               .getFluidState(BlockPos.containing(Squid.this.getX() + vec3.x, Squid.this.getY() + vec3.y, Squid.this.getZ() + vec3.z));
            if (fluidstate.is(FluidTags.WATER) || blockstate.isAir()) {
               double d0 = vec3.length();
               if (d0 > 0.0) {
                  vec3.normalize();
                  double d1 = 3.0;
                  if (d0 > 5.0) {
                     d1 -= (d0 - 5.0) / 5.0;
                  }

                  if (d1 > 0.0) {
                     vec3 = vec3.scale(d1);
                  }
               }

               if (blockstate.isAir()) {
                  vec3 = vec3.subtract(0.0, vec3.y, 0.0);
               }

               Squid.this.movementVector = new Vec3(vec3.x / 20.0, vec3.y / 20.0, vec3.z / 20.0);
            }

            if (this.fleeTicks % 10 == 5) {
               Squid.this.level().addParticle(ParticleTypes.BUBBLE, Squid.this.getX(), Squid.this.getY(), Squid.this.getZ(), 0.0, 0.0, 0.0);
            }
         }
      }
   }

   static class SquidRandomMovementGoal extends Goal {
      private final Squid squid;

      public SquidRandomMovementGoal(Squid p_457834_) {
         this.squid = p_457834_;
      }

      @Override
      public boolean canUse() {
         return true;
      }

      @Override
      public void tick() {
         int i = this.squid.getNoActionTime();
         if (i > 100) {
            this.squid.movementVector = Vec3.ZERO;
         } else if (this.squid.getRandom().nextInt(reducedTickDelay(50)) == 0 || !this.squid.wasTouchingWater || !this.squid.hasMovementVector()) {
            float f = this.squid.getRandom().nextFloat() * (float) (Math.PI * 2);
            this.squid.movementVector = new Vec3(Mth.cos(f) * 0.2F, -0.1F + this.squid.getRandom().nextFloat() * 0.2F, Mth.sin(f) * 0.2F);
         }
      }
   }
}
