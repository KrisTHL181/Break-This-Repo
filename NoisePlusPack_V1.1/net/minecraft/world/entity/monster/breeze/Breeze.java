package net.minecraft.world.entity.monster.breeze;

import com.mojang.serialization.Dynamic;
import java.util.Optional;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.util.debug.DebugBreezeInfo;
import net.minecraft.util.debug.DebugSubscriptions;
import net.minecraft.util.debug.DebugValueSource;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileDeflection;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class Breeze extends Monster {
   private static final int SLIDE_PARTICLES_AMOUNT = 20;
   private static final int IDLE_PARTICLES_AMOUNT = 1;
   private static final int JUMP_DUST_PARTICLES_AMOUNT = 20;
   private static final int JUMP_TRAIL_PARTICLES_AMOUNT = 3;
   private static final int JUMP_TRAIL_DURATION_TICKS = 5;
   private static final int JUMP_CIRCLE_DISTANCE_Y = 10;
   private static final float FALL_DISTANCE_SOUND_TRIGGER_THRESHOLD = 3.0F;
   private static final int WHIRL_SOUND_FREQUENCY_MIN = 1;
   private static final int WHIRL_SOUND_FREQUENCY_MAX = 80;
   public AnimationState idle = new AnimationState();
   public AnimationState slide = new AnimationState();
   public AnimationState slideBack = new AnimationState();
   public AnimationState longJump = new AnimationState();
   public AnimationState shoot = new AnimationState();
   public AnimationState inhale = new AnimationState();
   private int jumpTrailStartedTick = 0;
   private int soundTick = 0;
   private static final ProjectileDeflection PROJECTILE_DEFLECTION = (p_390689_, p_390690_, p_390691_) -> {
      p_390690_.level().playSound(null, p_390690_, SoundEvents.BREEZE_DEFLECT, p_390690_.getSoundSource(), 1.0F, 1.0F);
      ProjectileDeflection.REVERSE.deflect(p_390689_, p_390690_, p_390691_);
   };

   public static AttributeSupplier.Builder createAttributes() {
      return Mob.createMobAttributes()
         .add(Attributes.MOVEMENT_SPEED, 0.63F)
         .add(Attributes.MAX_HEALTH, 30.0)
         .add(Attributes.FOLLOW_RANGE, 24.0)
         .add(Attributes.ATTACK_DAMAGE, 3.0);
   }

   public Breeze(EntityType<? extends Monster> p_310338_, Level p_309512_) {
      super(p_310338_, p_309512_);
      this.setPathfindingMalus(PathType.DANGER_TRAPDOOR, -1.0F);
      this.setPathfindingMalus(PathType.DAMAGE_FIRE, -1.0F);
      this.xpReward = 10;
   }

   @Override
   protected Brain<?> makeBrain(Dynamic<?> p_311857_) {
      return BreezeAi.makeBrain(this, this.brainProvider().makeBrain(p_311857_));
   }

   @Override
   public Brain<Breeze> getBrain() {
      return (Brain<Breeze>)super.getBrain();
   }

   @Override
   protected Brain.Provider<Breeze> brainProvider() {
      return Brain.provider(BreezeAi.MEMORY_TYPES, BreezeAi.SENSOR_TYPES);
   }

   @Override
   public void onSyncedDataUpdated(EntityDataAccessor<?> p_309800_) {
      if (this.level().isClientSide() && DATA_POSE.equals(p_309800_)) {
         this.resetAnimations();
         Pose pose = this.getPose();
         switch (pose) {
            case SHOOTING:
               this.shoot.startIfStopped(this.tickCount);
               break;
            case INHALING:
               this.inhale.startIfStopped(this.tickCount);
               break;
            case SLIDING:
               this.slide.startIfStopped(this.tickCount);
         }
      }

      super.onSyncedDataUpdated(p_309800_);
   }

   private void resetAnimations() {
      this.shoot.stop();
      this.idle.stop();
      this.inhale.stop();
      this.longJump.stop();
   }

   @Override
   public void tick() {
      Pose pose = this.getPose();
      switch (pose) {
         case SHOOTING:
         case INHALING:
         case STANDING:
            this.resetJumpTrail().emitGroundParticles(1 + this.getRandom().nextInt(1));
            break;
         case SLIDING:
            this.emitGroundParticles(20);
            break;
         case LONG_JUMPING:
            this.longJump.startIfStopped(this.tickCount);
            this.emitJumpTrailParticles();
      }

      this.idle.startIfStopped(this.tickCount);
      if (pose != Pose.SLIDING && this.slide.isStarted()) {
         this.slideBack.start(this.tickCount);
         this.slide.stop();
      }

      this.soundTick = this.soundTick == 0 ? this.random.nextIntBetweenInclusive(1, 80) : this.soundTick - 1;
      if (this.soundTick == 0) {
         this.playWhirlSound();
      }

      super.tick();
   }

   public Breeze resetJumpTrail() {
      this.jumpTrailStartedTick = 0;
      return this;
   }

   public void emitJumpTrailParticles() {
      if (++this.jumpTrailStartedTick <= 5) {
         BlockState blockstate = !this.getInBlockState().isAir() ? this.getInBlockState() : this.getBlockStateOn();
         Vec3 vec3 = this.getDeltaMovement();
         Vec3 vec31 = this.position().add(vec3).add(0.0, 0.1F, 0.0);

         for (int i = 0; i < 3; i++) {
            this.level().addParticle(new BlockParticleOption(ParticleTypes.BLOCK, blockstate), vec31.x, vec31.y, vec31.z, 0.0, 0.0, 0.0);
         }
      }
   }

   public void emitGroundParticles(int p_310885_) {
      if (!this.isPassenger()) {
         Vec3 vec3 = this.getBoundingBox().getCenter();
         Vec3 vec31 = new Vec3(vec3.x, this.position().y, vec3.z);
         BlockState blockstate = !this.getInBlockState().isAir() ? this.getInBlockState() : this.getBlockStateOn();
         if (blockstate.getRenderShape() != RenderShape.INVISIBLE) {
            for (int i = 0; i < p_310885_; i++) {
               this.level().addParticle(new BlockParticleOption(ParticleTypes.BLOCK, blockstate), vec31.x, vec31.y, vec31.z, 0.0, 0.0, 0.0);
            }
         }
      }
   }

   @Override
   public void playAmbientSound() {
      if (this.getTarget() == null || !this.onGround()) {
         this.level().playLocalSound(this, this.getAmbientSound(), this.getSoundSource(), 1.0F, 1.0F);
      }
   }

   public void playWhirlSound() {
      float f = 0.7F + 0.4F * this.random.nextFloat();
      float f1 = 0.8F + 0.2F * this.random.nextFloat();
      this.level().playLocalSound(this, SoundEvents.BREEZE_WHIRL, this.getSoundSource(), f1, f);
   }

   @Override
   public ProjectileDeflection deflection(Projectile p_335920_) {
      if (p_335920_.getType() != EntityType.BREEZE_WIND_CHARGE && p_335920_.getType() != EntityType.WIND_CHARGE) {
         return this.getType().is(EntityTypeTags.DEFLECTS_PROJECTILES) ? PROJECTILE_DEFLECTION : ProjectileDeflection.NONE;
      } else {
         return ProjectileDeflection.NONE;
      }
   }

   @Override
   public SoundSource getSoundSource() {
      return SoundSource.HOSTILE;
   }

   @Override
   protected SoundEvent getDeathSound() {
      return SoundEvents.BREEZE_DEATH;
   }

   @Override
   protected SoundEvent getHurtSound(DamageSource p_311322_) {
      return SoundEvents.BREEZE_HURT;
   }

   @Override
   protected SoundEvent getAmbientSound() {
      return this.onGround() ? SoundEvents.BREEZE_IDLE_GROUND : SoundEvents.BREEZE_IDLE_AIR;
   }

   public Optional<LivingEntity> getHurtBy() {
      return this.getBrain()
         .getMemory(MemoryModuleType.HURT_BY)
         .map(DamageSource::getEntity)
         .filter(p_333499_ -> p_333499_ instanceof LivingEntity)
         .map(p_332795_ -> (LivingEntity)p_332795_);
   }

   public boolean withinInnerCircleRange(Vec3 p_311473_) {
      Vec3 vec3 = this.blockPosition().getCenter();
      return p_311473_.closerThan(vec3, 4.0, 10.0);
   }

   @Override
   protected void customServerAiStep(ServerLevel p_364535_) {
      ProfilerFiller profilerfiller = Profiler.get();
      profilerfiller.push("breezeBrain");
      this.getBrain().tick(p_364535_, this);
      profilerfiller.popPush("breezeActivityUpdate");
      BreezeAi.updateActivity(this);
      profilerfiller.pop();
      super.customServerAiStep(p_364535_);
   }

   @Override
   public boolean canAttackType(EntityType<?> p_310232_) {
      return p_310232_ == EntityType.PLAYER || p_310232_ == EntityType.IRON_GOLEM;
   }

   @Override
   public int getMaxHeadYRot() {
      return 30;
   }

   @Override
   public int getHeadRotSpeed() {
      return 25;
   }

   public double getFiringYPosition() {
      return this.getY() + this.getBbHeight() / 2.0F + 0.3F;
   }

   @Override
   public boolean isInvulnerableTo(ServerLevel p_364404_, DamageSource p_309859_) {
      return p_309859_.getEntity() instanceof Breeze || super.isInvulnerableTo(p_364404_, p_309859_);
   }

   @Override
   public double getFluidJumpThreshold() {
      return this.getEyeHeight();
   }

   @Override
   public boolean causeFallDamage(double p_395307_, float p_310250_, DamageSource p_311921_) {
      if (p_395307_ > 3.0) {
         this.playSound(SoundEvents.BREEZE_LAND, 1.0F, 1.0F);
      }

      return super.causeFallDamage(p_395307_, p_310250_, p_311921_);
   }

   @Override
   protected Entity.MovementEmission getMovementEmission() {
      return Entity.MovementEmission.EVENTS;
   }

   @Override
   public @Nullable LivingEntity getTarget() {
      return this.getTargetFromBrain();
   }

   @Override
   public void registerDebugValues(ServerLevel p_423698_, DebugValueSource.Registration p_426311_) {
      super.registerDebugValues(p_423698_, p_426311_);
      p_426311_.register(
         DebugSubscriptions.BREEZES,
         () -> new DebugBreezeInfo(
            this.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).map(Entity::getId), this.getBrain().getMemory(MemoryModuleType.BREEZE_JUMP_TARGET)
         )
      );
   }
}
