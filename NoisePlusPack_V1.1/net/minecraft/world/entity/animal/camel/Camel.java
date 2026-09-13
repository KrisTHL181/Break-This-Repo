package net.minecraft.world.entity.animal.camel;

import com.google.common.annotations.VisibleForTesting;
import com.mojang.serialization.Dynamic;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.BodyRotationControl;
import net.minecraft.world.entity.ai.control.LookControl;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class Camel extends AbstractHorse {
   public static final float BABY_SCALE = 0.45F;
   public static final int DASH_COOLDOWN_TICKS = 55;
   public static final int MAX_HEAD_Y_ROT = 30;
   private static final float RUNNING_SPEED_BONUS = 0.1F;
   private static final float DASH_VERTICAL_MOMENTUM = 1.4285F;
   private static final float DASH_HORIZONTAL_MOMENTUM = 22.2222F;
   private static final int DASH_MINIMUM_DURATION_TICKS = 5;
   private static final int SITDOWN_DURATION_TICKS = 40;
   private static final int STANDUP_DURATION_TICKS = 52;
   private static final int IDLE_MINIMAL_DURATION_TICKS = 80;
   private static final float SITTING_HEIGHT_DIFFERENCE = 1.43F;
   private static final long DEFAULT_LAST_POSE_CHANGE_TICK = 0L;
   public static final EntityDataAccessor<Boolean> DASH = SynchedEntityData.defineId(Camel.class, EntityDataSerializers.BOOLEAN);
   public static final EntityDataAccessor<Long> LAST_POSE_CHANGE_TICK = SynchedEntityData.defineId(Camel.class, EntityDataSerializers.LONG);
   public final AnimationState sitAnimationState = new AnimationState();
   public final AnimationState sitPoseAnimationState = new AnimationState();
   public final AnimationState sitUpAnimationState = new AnimationState();
   public final AnimationState idleAnimationState = new AnimationState();
   public final AnimationState dashAnimationState = new AnimationState();
   private static final EntityDimensions SITTING_DIMENSIONS = EntityDimensions.scalable(EntityType.CAMEL.getWidth(), EntityType.CAMEL.getHeight() - 1.43F)
      .withEyeHeight(0.845F);
   private int dashCooldown = 0;
   private int idleAnimationTimeout = 0;

   public Camel(EntityType<? extends Camel> p_248516_, Level p_249295_) {
      super(p_248516_, p_249295_);
      this.moveControl = new Camel.CamelMoveControl();
      this.lookControl = new Camel.CamelLookControl();
      GroundPathNavigation groundpathnavigation = (GroundPathNavigation)this.getNavigation();
      groundpathnavigation.setCanFloat(true);
      groundpathnavigation.setCanWalkOverFences(true);
   }

   @Override
   protected void addAdditionalSaveData(ValueOutput p_408947_) {
      super.addAdditionalSaveData(p_408947_);
      p_408947_.putLong("LastPoseTick", this.entityData.get(LAST_POSE_CHANGE_TICK));
   }

   @Override
   protected void readAdditionalSaveData(ValueInput p_405933_) {
      super.readAdditionalSaveData(p_405933_);
      long i = p_405933_.getLongOr("LastPoseTick", 0L);
      if (i < 0L) {
         this.setPose(Pose.SITTING);
      }

      this.resetLastPoseChangeTick(i);
   }

   public static AttributeSupplier.Builder createAttributes() {
      return createBaseHorseAttributes()
         .add(Attributes.MAX_HEALTH, 32.0)
         .add(Attributes.MOVEMENT_SPEED, 0.09F)
         .add(Attributes.JUMP_STRENGTH, 0.42F)
         .add(Attributes.STEP_HEIGHT, 1.5);
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder p_333728_) {
      super.defineSynchedData(p_333728_);
      p_333728_.define(DASH, false);
      p_333728_.define(LAST_POSE_CHANGE_TICK, 0L);
   }

   @Override
   public SpawnGroupData finalizeSpawn(
      ServerLevelAccessor p_249190_, DifficultyInstance p_251264_, EntitySpawnReason p_363561_, @Nullable SpawnGroupData p_249259_
   ) {
      CamelAi.initMemories(this, p_249190_.getRandom());
      this.resetLastPoseChangeTickToFullStand(p_249190_.getLevel().getGameTime());
      return super.finalizeSpawn(p_249190_, p_251264_, p_363561_, p_249259_);
   }

   public static boolean checkCamelSpawnRules(
      EntityType<Camel> p_396073_, LevelAccessor p_393881_, EntitySpawnReason p_397323_, BlockPos p_392089_, RandomSource p_393103_
   ) {
      return p_393881_.getBlockState(p_392089_.below()).is(BlockTags.CAMELS_SPAWNABLE_ON) && isBrightEnoughToSpawn(p_393881_, p_392089_);
   }

   @Override
   protected Brain.Provider<Camel> brainProvider() {
      return CamelAi.brainProvider();
   }

   @Override
   protected void registerGoals() {
   }

   @Override
   protected Brain<?> makeBrain(Dynamic<?> p_251586_) {
      return CamelAi.makeBrain(this.brainProvider().makeBrain(p_251586_));
   }

   @Override
   public EntityDimensions getDefaultDimensions(Pose p_329548_) {
      return p_329548_ == Pose.SITTING ? SITTING_DIMENSIONS.scale(this.getAgeScale()) : super.getDefaultDimensions(p_329548_);
   }

   @Override
   protected void customServerAiStep(ServerLevel p_366084_) {
      ProfilerFiller profilerfiller = Profiler.get();
      profilerfiller.push("camelBrain");
      Brain<?> brain = this.getBrain();
      ((Brain<Camel>)brain).tick(p_366084_, this);
      profilerfiller.pop();
      profilerfiller.push("camelActivityUpdate");
      CamelAi.updateActivity(this);
      profilerfiller.pop();
      super.customServerAiStep(p_366084_);
   }

   @Override
   public void tick() {
      super.tick();
      if (this.isDashing() && this.dashCooldown < 50 && (this.onGround() || this.isInLiquid() || this.isPassenger())) {
         this.setDashing(false);
      }

      if (this.dashCooldown > 0) {
         this.dashCooldown--;
         if (this.dashCooldown == 0) {
            this.level().playSound(null, this.blockPosition(), this.getDashReadySound(), SoundSource.NEUTRAL, 1.0F, 1.0F);
         }
      }

      if (this.level().isClientSide()) {
         this.setupAnimationStates();
      }

      if (this.refuseToMove()) {
         this.clampHeadRotationToBody();
      }

      if (this.isCamelSitting() && this.isInWater()) {
         this.standUpInstantly();
      }
   }

   private void setupAnimationStates() {
      if (this.idleAnimationTimeout <= 0) {
         this.idleAnimationTimeout = this.random.nextInt(40) + 80;
         this.idleAnimationState.start(this.tickCount);
      } else {
         this.idleAnimationTimeout--;
      }

      if (this.isCamelVisuallySitting()) {
         this.sitUpAnimationState.stop();
         this.dashAnimationState.stop();
         if (this.isVisuallySittingDown()) {
            this.sitAnimationState.startIfStopped(this.tickCount);
            this.sitPoseAnimationState.stop();
         } else {
            this.sitAnimationState.stop();
            this.sitPoseAnimationState.startIfStopped(this.tickCount);
         }
      } else {
         this.sitAnimationState.stop();
         this.sitPoseAnimationState.stop();
         this.dashAnimationState.animateWhen(this.isDashing(), this.tickCount);
         this.sitUpAnimationState.animateWhen(this.isInPoseTransition() && this.getPoseTime() >= 0L, this.tickCount);
      }
   }

   @Override
   protected void updateWalkAnimation(float p_268362_) {
      float f;
      if (this.getPose() == Pose.STANDING && !this.dashAnimationState.isStarted()) {
         f = Math.min(p_268362_ * 6.0F, 1.0F);
      } else {
         f = 0.0F;
      }

      this.walkAnimation.update(f, 0.2F, this.isBaby() ? 3.0F : 1.0F);
   }

   @Override
   public void travel(Vec3 p_250068_) {
      if (this.refuseToMove() && this.onGround()) {
         this.setDeltaMovement(this.getDeltaMovement().multiply(0.0, 1.0, 0.0));
         p_250068_ = p_250068_.multiply(0.0, 1.0, 0.0);
      }

      super.travel(p_250068_);
   }

   @Override
   protected void tickRidden(Player p_278319_, Vec3 p_278301_) {
      super.tickRidden(p_278319_, p_278301_);
      if (p_278319_.zza > 0.0F && this.isCamelSitting() && !this.isInPoseTransition()) {
         this.standUp();
      }
   }

   public boolean refuseToMove() {
      return this.isCamelSitting() || this.isInPoseTransition();
   }

   @Override
   protected float getRiddenSpeed(Player p_278241_) {
      float f = p_278241_.isSprinting() && this.getJumpCooldown() == 0 ? 0.1F : 0.0F;
      return (float)this.getAttributeValue(Attributes.MOVEMENT_SPEED) + f;
   }

   @Override
   protected Vec2 getRiddenRotation(LivingEntity p_275271_) {
      return this.refuseToMove() ? new Vec2(this.getXRot(), this.getYRot()) : super.getRiddenRotation(p_275271_);
   }

   @Override
   protected Vec3 getRiddenInput(Player p_278264_, Vec3 p_278298_) {
      return this.refuseToMove() ? Vec3.ZERO : super.getRiddenInput(p_278264_, p_278298_);
   }

   @Override
   public boolean canJump() {
      return !this.refuseToMove() && super.canJump();
   }

   @Override
   public void onPlayerJump(int p_249138_) {
      if (this.isSaddled() && this.dashCooldown <= 0 && this.onGround()) {
         super.onPlayerJump(p_249138_);
      }
   }

   @Override
   public boolean canSprint() {
      return true;
   }

   @Override
   protected void executeRidersJump(float p_251967_, Vec3 p_275627_) {
      double d0 = this.getJumpPower();
      this.addDeltaMovement(
         this.getLookAngle()
            .multiply(1.0, 0.0, 1.0)
            .normalize()
            .scale(22.2222F * p_251967_ * this.getAttributeValue(Attributes.MOVEMENT_SPEED) * this.getBlockSpeedFactor())
            .add(0.0, 1.4285F * p_251967_ * d0, 0.0)
      );
      this.dashCooldown = 55;
      this.setDashing(true);
      this.needsSync = true;
   }

   public boolean isDashing() {
      return this.entityData.get(DASH);
   }

   public void setDashing(boolean p_251380_) {
      this.entityData.set(DASH, p_251380_);
   }

   @Override
   public void handleStartJump(int p_249968_) {
      this.makeSound(this.getDashingSound());
      this.gameEvent(GameEvent.ENTITY_ACTION);
      this.setDashing(true);
   }

   protected SoundEvent getDashingSound() {
      return SoundEvents.CAMEL_DASH;
   }

   protected SoundEvent getDashReadySound() {
      return SoundEvents.CAMEL_DASH_READY;
   }

   @Override
   public void handleStopJump() {
   }

   @Override
   public int getJumpCooldown() {
      return this.dashCooldown;
   }

   @Override
   protected SoundEvent getAmbientSound() {
      return SoundEvents.CAMEL_AMBIENT;
   }

   @Override
   protected SoundEvent getDeathSound() {
      return SoundEvents.CAMEL_DEATH;
   }

   @Override
   protected SoundEvent getHurtSound(DamageSource p_250052_) {
      return SoundEvents.CAMEL_HURT;
   }

   @Override
   protected void playStepSound(BlockPos p_252056_, BlockState p_251457_) {
      if (p_251457_.is(BlockTags.CAMEL_SAND_STEP_SOUND_BLOCKS)) {
         this.playSound(SoundEvents.CAMEL_STEP_SAND, 1.0F, 1.0F);
      } else {
         this.playSound(SoundEvents.CAMEL_STEP, 1.0F, 1.0F);
      }
   }

   @Override
   public boolean isFood(ItemStack p_248671_) {
      return p_248671_.is(ItemTags.CAMEL_FOOD);
   }

   @Override
   public InteractionResult mobInteract(Player p_249032_, InteractionHand p_251004_) {
      ItemStack itemstack = p_249032_.getItemInHand(p_251004_);
      if (p_249032_.isSecondaryUseActive() && !this.isBaby()) {
         this.openCustomInventoryScreen(p_249032_);
         return InteractionResult.SUCCESS;
      }

      InteractionResult interactionresult = itemstack.interactLivingEntity(p_249032_, this, p_251004_);
      if (interactionresult.consumesAction()) {
         return interactionresult;
      }

      if (this.isFood(itemstack)) {
         return this.fedFood(p_249032_, itemstack);
      }

      if (this.getPassengers().size() < 2 && !this.isBaby()) {
         this.doPlayerRide(p_249032_);
      }

      return InteractionResult.CONSUME;
   }

   @Override
   public void onElasticLeashPull() {
      super.onElasticLeashPull();
      if (this.isCamelSitting() && !this.isInPoseTransition() && this.canCamelChangePose()) {
         this.standUp();
      }
   }

   @Override
   public Vec3[] getQuadLeashOffsets() {
      return Leashable.createQuadLeashOffsets(this, 0.02, 0.48, 0.25, 0.82);
   }

   public boolean canCamelChangePose() {
      return this.wouldNotSuffocateAtTargetPose(this.isCamelSitting() ? Pose.STANDING : Pose.SITTING);
   }

   @Override
   protected boolean handleEating(Player p_249923_, ItemStack p_248995_) {
      if (!this.isFood(p_248995_)) {
         return false;
      }

      boolean flag = this.getHealth() < this.getMaxHealth();
      if (flag) {
         this.heal(2.0F);
      }

      boolean flag1 = this.isTamed() && this.getAge() == 0 && this.canFallInLove();
      if (flag1) {
         this.setInLove(p_249923_);
      }

      boolean flag2 = this.isBaby();
      if (flag2) {
         this.level().addParticle(ParticleTypes.HAPPY_VILLAGER, this.getRandomX(1.0), this.getRandomY() + 0.5, this.getRandomZ(1.0), 0.0, 0.0, 0.0);
         if (!this.level().isClientSide()) {
            this.ageUp(10);
         }
      }

      if (!flag && !flag1 && !flag2) {
         return false;
      }

      if (!this.isSilent()) {
         SoundEvent soundevent = this.getEatingSound();
         if (soundevent != null) {
            this.level()
               .playSound(
                  null,
                  this.getX(),
                  this.getY(),
                  this.getZ(),
                  soundevent,
                  this.getSoundSource(),
                  1.0F,
                  1.0F + (this.random.nextFloat() - this.random.nextFloat()) * 0.2F
               );
         }
      }

      this.gameEvent(GameEvent.EAT);
      return true;
   }

   @Override
   protected boolean canPerformRearing() {
      return false;
   }

   @Override
   public boolean canMate(Animal p_251650_) {
      return p_251650_ != this && p_251650_ instanceof Camel camel && this.canParent() && camel.canParent();
   }

   public @Nullable Camel getBreedOffspring(ServerLevel p_251227_, AgeableMob p_251047_) {
      return EntityType.CAMEL.create(p_251227_, EntitySpawnReason.BREEDING);
   }

   @Override
   protected SoundEvent getEatingSound() {
      return SoundEvents.CAMEL_EAT;
   }

   @Override
   protected void actuallyHurt(ServerLevel p_369966_, DamageSource p_250410_, float p_251451_) {
      this.standUpInstantly();
      super.actuallyHurt(p_369966_, p_250410_, p_251451_);
   }

   @Override
   protected Vec3 getPassengerAttachmentPoint(Entity p_298535_, EntityDimensions p_298993_, float p_301011_) {
      int i = Math.max(this.getPassengers().indexOf(p_298535_), 0);
      boolean flag = i == 0;
      float f = 0.5F;
      float f1 = (float)(this.isRemoved() ? 0.01F : this.getBodyAnchorAnimationYOffset(flag, 0.0F, p_298993_, p_301011_));
      if (this.getPassengers().size() > 1) {
         if (!flag) {
            f = -0.7F;
         }

         if (p_298535_ instanceof Animal) {
            f += 0.2F;
         }
      }

      return new Vec3(0.0, f1, f * p_301011_).yRot(-this.getYRot() * (float) (Math.PI / 180.0));
   }

   @Override
   public float getAgeScale() {
      return this.isBaby() ? 0.45F : 1.0F;
   }

   private double getBodyAnchorAnimationYOffset(boolean p_249228_, float p_251763_, EntityDimensions p_301064_, float p_299749_) {
      double d0 = p_301064_.height() - 0.375F * p_299749_;
      float f = p_299749_ * 1.43F;
      float f1 = f - p_299749_ * 0.2F;
      float f2 = f - f1;
      boolean flag = this.isInPoseTransition();
      boolean flag1 = this.isCamelSitting();
      if (flag) {
         int i = flag1 ? 40 : 52;
         int j;
         float f3;
         if (flag1) {
            j = 28;
            f3 = p_249228_ ? 0.5F : 0.1F;
         } else {
            j = p_249228_ ? 24 : 32;
            f3 = p_249228_ ? 0.6F : 0.35F;
         }

         float f4 = Mth.clamp((float)this.getPoseTime() + p_251763_, 0.0F, i);
         boolean flag2 = f4 < j;
         float f5 = flag2 ? f4 / j : (f4 - j) / (i - j);
         float f6 = f - f3 * f1;
         d0 += flag1 ? Mth.lerp(f5, flag2 ? f : f6, flag2 ? f6 : f2) : Mth.lerp(f5, flag2 ? f2 - f : f2 - f6, flag2 ? f2 - f6 : 0.0F);
      }

      if (flag1 && !flag) {
         d0 += f2;
      }

      return d0;
   }

   @Override
   public Vec3 getLeashOffset(float p_251477_) {
      EntityDimensions entitydimensions = this.getDimensions(this.getPose());
      float f = this.getAgeScale();
      return new Vec3(0.0, this.getBodyAnchorAnimationYOffset(true, p_251477_, entitydimensions, f) - 0.2F * f, entitydimensions.width() * 0.56F);
   }

   @Override
   public int getMaxHeadYRot() {
      return 30;
   }

   @Override
   protected boolean canAddPassenger(Entity p_248594_) {
      return this.getPassengers().size() <= 2;
   }

   public boolean isCamelSitting() {
      return this.entityData.get(LAST_POSE_CHANGE_TICK) < 0L;
   }

   public boolean isCamelVisuallySitting() {
      return this.getPoseTime() < 0L != this.isCamelSitting();
   }

   public boolean isInPoseTransition() {
      long i = this.getPoseTime();
      return i < (this.isCamelSitting() ? 40 : 52);
   }

   private boolean isVisuallySittingDown() {
      return this.isCamelSitting() && this.getPoseTime() < 40L && this.getPoseTime() >= 0L;
   }

   public void sitDown() {
      if (!this.isCamelSitting()) {
         this.makeSound(this.getSitDownSound());
         this.setPose(Pose.SITTING);
         this.gameEvent(GameEvent.ENTITY_ACTION);
         this.resetLastPoseChangeTick(-this.level().getGameTime());
      }
   }

   public void standUp() {
      if (this.isCamelSitting()) {
         this.makeSound(this.getStandUpSound());
         this.setPose(Pose.STANDING);
         this.gameEvent(GameEvent.ENTITY_ACTION);
         this.resetLastPoseChangeTick(this.level().getGameTime());
      }
   }

   protected SoundEvent getStandUpSound() {
      return SoundEvents.CAMEL_STAND;
   }

   protected SoundEvent getSitDownSound() {
      return SoundEvents.CAMEL_SIT;
   }

   public void standUpInstantly() {
      this.setPose(Pose.STANDING);
      this.gameEvent(GameEvent.ENTITY_ACTION);
      this.resetLastPoseChangeTickToFullStand(this.level().getGameTime());
   }

   @VisibleForTesting
   public void resetLastPoseChangeTick(long p_248642_) {
      this.entityData.set(LAST_POSE_CHANGE_TICK, p_248642_);
   }

   private void resetLastPoseChangeTickToFullStand(long p_265447_) {
      this.resetLastPoseChangeTick(Math.max(0L, p_265447_ - 52L - 1L));
   }

   public long getPoseTime() {
      return this.level().getGameTime() - Math.abs(this.entityData.get(LAST_POSE_CHANGE_TICK));
   }

   @Override
   protected Holder<SoundEvent> getEquipSound(EquipmentSlot p_393986_, ItemStack p_395637_, Equippable p_397313_) {
      return p_393986_ == EquipmentSlot.SADDLE ? this.getSaddleSound() : super.getEquipSound(p_393986_, p_395637_, p_397313_);
   }

   protected Holder.Reference<SoundEvent> getSaddleSound() {
      return SoundEvents.CAMEL_SADDLE;
   }

   @Override
   public void onSyncedDataUpdated(EntityDataAccessor<?> p_252215_) {
      if (!this.firstTick && DASH.equals(p_252215_)) {
         this.dashCooldown = this.dashCooldown == 0 ? 55 : this.dashCooldown;
      }

      super.onSyncedDataUpdated(p_252215_);
   }

   @Override
   public boolean isTamed() {
      return true;
   }

   @Override
   public void openCustomInventoryScreen(Player p_248613_) {
      if (!this.level().isClientSide()) {
         p_248613_.openHorseInventory(this, this.inventory);
      }
   }

   @Override
   protected BodyRotationControl createBodyControl() {
      return new Camel.CamelBodyRotationControl(this);
   }

   class CamelBodyRotationControl extends BodyRotationControl {
      public CamelBodyRotationControl(final Camel p_248635_) {
         super(p_248635_);
      }

      @Override
      public void clientTick() {
         if (!Camel.this.refuseToMove()) {
            super.clientTick();
         }
      }
   }

   class CamelLookControl extends LookControl {
      CamelLookControl() {
         super(Camel.this);
      }

      @Override
      public void tick() {
         if (!Camel.this.hasControllingPassenger()) {
            super.tick();
         }
      }
   }

   class CamelMoveControl extends MoveControl {
      public CamelMoveControl() {
         super(Camel.this);
      }

      @Override
      public void tick() {
         if (this.operation == MoveControl.Operation.MOVE_TO
            && !Camel.this.isLeashed()
            && Camel.this.isCamelSitting()
            && !Camel.this.isInPoseTransition()
            && Camel.this.canCamelChangePose()) {
            Camel.this.standUp();
         }

         super.tick();
      }
   }
}
