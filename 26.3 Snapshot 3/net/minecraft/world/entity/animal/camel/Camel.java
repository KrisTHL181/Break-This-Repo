package net.minecraft.world.entity.animal.camel;

import com.google.common.annotations.VisibleForTesting;
import java.util.List;
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
import net.minecraft.world.entity.EntityTypes;
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
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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
   public static final float BABY_SCALE = 0.6F;
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
   private static final Brain.Provider<Camel> BRAIN_PROVIDER = Brain.provider(
      List.of(SensorType.NEAREST_LIVING_ENTITIES, SensorType.HURT_BY, SensorType.FOOD_TEMPTATIONS, SensorType.NEAREST_ADULT), var0 -> CamelAi.getActivities()
   );
   public static final EntityDataAccessor<Boolean> DASH = SynchedEntityData.defineId(Camel.class, EntityDataSerializers.BOOLEAN);
   public static final EntityDataAccessor<Long> LAST_POSE_CHANGE_TICK = SynchedEntityData.defineId(Camel.class, EntityDataSerializers.LONG);
   public final AnimationState sitAnimationState = new AnimationState();
   public final AnimationState sitPoseAnimationState = new AnimationState();
   public final AnimationState sitUpAnimationState = new AnimationState();
   public final AnimationState idleAnimationState = new AnimationState();
   public final AnimationState dashAnimationState = new AnimationState();
   private static final EntityDimensions BABY_STANDING_DIMENSIONS = EntityDimensions.scalable(0.95F, 1.4F).withEyeHeight(1.38F);
   private static final EntityDimensions BABY_SITTING_DIMENSIONS = EntityDimensions.scalable(0.95F, 0.425F).withEyeHeight(0.41F);
   private static final EntityDimensions ADULT_SITTING_DIMENSIONS = EntityDimensions.scalable(
         EntityTypes.CAMEL.getWidth(), EntityTypes.CAMEL.getHeight() - 1.43F
      )
      .withEyeHeight(0.845F);
   private int dashCooldown = 0;
   private int idleAnimationTimeout = 0;

   public Camel(final EntityType<? extends Camel> type, final Level level) {
      super(type, level);
      this.moveControl = new Camel.CamelMoveControl<>(this);
      this.lookControl = new Camel.CamelLookControl();
      GroundPathNavigation navigation = (GroundPathNavigation)this.getNavigation();
      navigation.setCanFloat(true);
      navigation.setCanWalkOverFences(true);
   }

   @Override
   protected void addAdditionalSaveData(final ValueOutput output) {
      super.addAdditionalSaveData(output);
      output.putLong("LastPoseTick", this.entityData.get(LAST_POSE_CHANGE_TICK));
   }

   @Override
   protected void readAdditionalSaveData(final ValueInput input) {
      super.readAdditionalSaveData(input);
      long poseTick = input.getLongOr("LastPoseTick", 0L);
      if (poseTick < 0L) {
         this.setPose(Pose.SITTING);
      }

      this.resetLastPoseChangeTick(poseTick);
   }

   public static AttributeSupplier.Builder createAttributes() {
      return createBaseHorseAttributes()
         .add(Attributes.MAX_HEALTH, 32.0)
         .add(Attributes.MOVEMENT_SPEED, 0.09F)
         .add(Attributes.JUMP_STRENGTH, 0.42F)
         .add(Attributes.STEP_HEIGHT, 1.5);
   }

   @Override
   protected void defineSynchedData(final SynchedEntityData.Builder entityData) {
      super.defineSynchedData(entityData);
      entityData.define(DASH, false);
      entityData.define(LAST_POSE_CHANGE_TICK, 0L);
   }

   @Override
   public SpawnGroupData finalizeSpawn(
      final ServerLevelAccessor level, final DifficultyInstance difficulty, final EntitySpawnReason spawnReason, final @Nullable SpawnGroupData groupData
   ) {
      CamelAi.initMemories(this, level.getRandom());
      this.resetLastPoseChangeTickToFullStand(level.getLevel().getGameTime());
      return super.finalizeSpawn(level, difficulty, spawnReason, groupData);
   }

   public static boolean checkCamelSpawnRules(
      final EntityType<Camel> type, final LevelAccessor level, final EntitySpawnReason spawnReason, final BlockPos pos, final RandomSource random
   ) {
      return level.getBlockState(pos.below()).is(BlockTags.CAMELS_SPAWNABLE_ON) && isBrightEnoughToSpawn(level, pos);
   }

   @Override
   protected Brain<Camel> makeBrain(final Brain.Packed packedBrain) {
      return BRAIN_PROVIDER.makeBrain(this, packedBrain);
   }

   @Override
   public Brain<Camel> getBrain() {
      return super.getBrain();
   }

   @Override
   protected void registerGoals() {
   }

   @Override
   public EntityDimensions getDefaultDimensions(final Pose pose) {
      return pose == Pose.SITTING
         ? (this.isBaby() ? BABY_SITTING_DIMENSIONS : ADULT_SITTING_DIMENSIONS)
         : (this.isBaby() ? BABY_STANDING_DIMENSIONS : super.getDefaultDimensions(pose));
   }

   @Override
   protected void customServerAiStep(final ServerLevel level) {
      ProfilerFiller profiler = Profiler.get();
      profiler.push("camelBrain");
      Brain<Camel> brain = this.getBrain();
      brain.tick(level, this);
      profiler.pop();
      profiler.push("camelActivityUpdate");
      CamelAi.updateActivity(this);
      profiler.pop();
      super.customServerAiStep(level);
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
   protected void updateWalkAnimation(final float distance) {
      float targetSpeed;
      if (this.getPose() == Pose.STANDING && !this.dashAnimationState.isStarted()) {
         targetSpeed = Math.min(distance * 6.0F, 1.0F);
      } else {
         targetSpeed = 0.0F;
      }

      this.walkAnimation.update(targetSpeed, 0.2F, this.isBaby() ? 3.0F : 1.0F);
   }

   @Override
   public void travel(Vec3 input) {
      if (this.refuseToMove() && this.onGround()) {
         this.setDeltaMovement(this.getDeltaMovement().multiply(0.0, 1.0, 0.0));
         input = input.multiply(0.0, 1.0, 0.0);
      }

      super.travel(input);
   }

   @Override
   protected void tickRidden(final Player controller, final Vec3 riddenInput) {
      super.tickRidden(controller, riddenInput);
      if (controller.zza > 0.0F && this.isCamelSitting() && !this.isInPoseTransition()) {
         this.standUp();
      }
   }

   public boolean refuseToMove() {
      return this.isCamelSitting() || this.isInPoseTransition();
   }

   @Override
   protected float getRiddenSpeed(final Player controller) {
      float movementBonus = controller.isSprinting() && this.getJumpCooldown() == 0 ? 0.1F : 0.0F;
      return (float)this.getAttributeValue(Attributes.MOVEMENT_SPEED) + movementBonus;
   }

   @Override
   protected Vec2 getRiddenRotation(final LivingEntity controller) {
      return this.refuseToMove() ? new Vec2(this.getXRot(), this.getYRot()) : super.getRiddenRotation(controller);
   }

   @Override
   protected Vec3 getRiddenInput(final Player controller, final Vec3 selfInput) {
      return this.refuseToMove() ? Vec3.ZERO : super.getRiddenInput(controller, selfInput);
   }

   @Override
   public boolean canJump() {
      return !this.refuseToMove() && super.canJump();
   }

   @Override
   public void onPlayerJump(final int jumpAmount) {
      if (this.isSaddled() && this.dashCooldown <= 0 && this.onGround()) {
         super.onPlayerJump(jumpAmount);
      }
   }

   @Override
   public boolean canSprint() {
      return true;
   }

   @Override
   protected void executeRidersJump(final float amount, final Vec3 input) {
      double jumpMomentum = this.getJumpPower();
      this.addDeltaMovement(
         this.getLookAngle()
            .multiply(1.0, 0.0, 1.0)
            .normalize()
            .scale(22.2222F * amount * this.getAttributeValue(Attributes.MOVEMENT_SPEED) * this.getBlockSpeedFactor())
            .add(0.0, 1.4285F * amount * jumpMomentum, 0.0)
      );
      this.dashCooldown = 55;
      this.setDashing(true);
      this.needsSync = true;
   }

   public boolean isDashing() {
      return this.entityData.get(DASH);
   }

   public void setDashing(final boolean isDashing) {
      this.entityData.set(DASH, isDashing);
   }

   @Override
   public void handleStartJump(final int jumpScale) {
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
   protected SoundEvent getHurtSound(final DamageSource source) {
      return SoundEvents.CAMEL_HURT;
   }

   @Override
   protected void playStepSound(final BlockPos pos, final BlockState blockState) {
      if (blockState.is(BlockTags.CAMEL_SAND_STEP_SOUND_BLOCKS)) {
         this.playSound(SoundEvents.CAMEL_STEP_SAND, 1.0F, 1.0F);
      } else {
         this.playSound(SoundEvents.CAMEL_STEP, 1.0F, 1.0F);
      }
   }

   @Override
   public boolean isFood(final ItemStack itemStack) {
      return itemStack.is(ItemTags.CAMEL_FOOD);
   }

   @Override
   public InteractionResult mobInteract(final Player player, final InteractionHand hand) {
      ItemStack itemStack = player.getItemInHand(hand);
      if (player.isSecondaryUseActive() && !this.isBaby()) {
         this.openCustomInventoryScreen(player);
         return InteractionResult.SUCCESS;
      }

      InteractionResult interactionResult = itemStack.interactLivingEntity(player, this, hand);
      if (interactionResult.consumesAction()) {
         return interactionResult;
      }

      if (this.isFood(itemStack)) {
         return this.fedFood(player, itemStack);
      }

      if (this.getPassengers().size() < 2 && !this.isBaby()) {
         this.doPlayerRide(player);
      }

      return this.isBaby() && player.isHolding(Items.GOLDEN_DANDELION) ? super.mobInteract(player, hand) : InteractionResult.CONSUME;
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
   protected boolean handleEating(final Player player, final ItemStack itemStack) {
      if (!this.isFood(itemStack)) {
         return false;
      }

      boolean couldHeal = this.getHealth() < this.getMaxHealth();
      if (couldHeal) {
         this.heal(2.0F);
      }

      boolean couldSetInLove = this.isTamed() && this.getAge() == 0 && this.canFallInLove();
      if (couldSetInLove) {
         this.setInLove(player);
      }

      boolean couldAgeUp = this.canAgeUp();
      if (couldAgeUp) {
         this.level().addParticle(ParticleTypes.HAPPY_VILLAGER, this.getRandomX(1.0), this.getRandomY() + 0.5, this.getRandomZ(1.0), 0.0, 0.0, 0.0);
         if (!this.level().isClientSide()) {
            this.ageUp(10);
         }
      }

      if (!couldHeal && !couldSetInLove && !couldAgeUp) {
         return false;
      }

      if (!this.isSilent()) {
         SoundEvent eatingSound = this.getEatingSound();
         if (eatingSound != null) {
            this.level()
               .playSound(
                  null,
                  this.getX(),
                  this.getY(),
                  this.getZ(),
                  eatingSound,
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
   public boolean canMate(final Animal partner) {
      return partner != this && partner instanceof Camel camel && this.canParent() && camel.canParent();
   }

   public @Nullable Camel getBreedOffspring(final ServerLevel level, final AgeableMob partner) {
      return EntityTypes.CAMEL.create(level, EntitySpawnReason.BREEDING);
   }

   @Override
   protected SoundEvent getEatingSound() {
      return SoundEvents.CAMEL_EAT;
   }

   @Override
   protected void actuallyHurt(final ServerLevel level, final DamageSource source, final float dmg) {
      this.standUpInstantly();
      super.actuallyHurt(level, source, dmg);
   }

   @Override
   protected Vec3 getPassengerAttachmentPoint(final Entity passenger, final EntityDimensions dimensions, final float scale) {
      int index = Math.max(this.getPassengers().indexOf(passenger), 0);
      boolean driver = index == 0;
      float offset = 0.5F;
      float height = (float)(this.isRemoved() ? 0.01F : this.getBodyAnchorAnimationYOffset(driver, 0.0F, dimensions, scale));
      if (this.getPassengers().size() > 1) {
         if (!driver) {
            offset = -0.7F;
         }

         if (passenger instanceof Animal) {
            offset += 0.2F;
         }
      }

      return new Vec3(0.0, height, offset * scale).yRot(-this.getYRot() * (float) (Math.PI / 180.0));
   }

   @Override
   public float getAgeScale() {
      return this.isBaby() ? 0.6F : 1.0F;
   }

   private double getBodyAnchorAnimationYOffset(final boolean isFront, final float partialTicks, final EntityDimensions dimensions, final float scale) {
      double ageSitYOffset = this.isBaby() ? 0.09375 : 0.375;
      double baseSitOffset = dimensions.height() - ageSitYOffset;
      float sittingHeightDifference = scale * 1.43F;
      float verticalDrop = sittingHeightDifference - scale * 0.2F;
      float bottomPoint = sittingHeightDifference - verticalDrop;
      boolean isInTransition = this.isInPoseTransition();
      boolean isSitting = this.isCamelSitting();
      if (isInTransition) {
         int animationDuration = isSitting ? 40 : 52;
         int halfPoint;
         float flexPointOffset;
         if (isSitting) {
            halfPoint = 28;
            flexPointOffset = isFront ? 0.5F : 0.1F;
         } else {
            halfPoint = isFront ? 24 : 32;
            flexPointOffset = isFront ? 0.6F : 0.35F;
         }

         float poseTime = Mth.clamp((float)this.getPoseTime() + partialTicks, 0.0F, animationDuration);
         boolean isFirstPart = poseTime < halfPoint;
         float part = isFirstPart ? poseTime / halfPoint : (poseTime - halfPoint) / (animationDuration - halfPoint);
         float flexPoint = sittingHeightDifference - flexPointOffset * verticalDrop;
         baseSitOffset += isSitting
            ? Mth.lerp(part, isFirstPart ? sittingHeightDifference : flexPoint, isFirstPart ? flexPoint : bottomPoint)
            : Mth.lerp(part, isFirstPart ? bottomPoint - sittingHeightDifference : bottomPoint - flexPoint, isFirstPart ? bottomPoint - flexPoint : 0.0F);
      }

      if (isSitting && !isInTransition) {
         baseSitOffset += bottomPoint;
      }

      return baseSitOffset;
   }

   @Override
   public Vec3 getLeashOffset(final float partialTicks) {
      EntityDimensions dimensions = this.getDimensions(this.getPose());
      float scale = this.getAgeScale();
      return new Vec3(0.0, this.getBodyAnchorAnimationYOffset(true, partialTicks, dimensions, scale) - 0.2F * scale, dimensions.width() * 0.56F);
   }

   @Override
   public int getMaxHeadYRot() {
      return 30;
   }

   @Override
   protected boolean canAddPassenger(final Entity passenger) {
      return this.getPassengers().size() <= 2;
   }

   public boolean isCamelSitting() {
      return this.entityData.get(LAST_POSE_CHANGE_TICK) < 0L;
   }

   public boolean isCamelVisuallySitting() {
      return this.getPoseTime() < 0L != this.isCamelSitting();
   }

   public boolean isInPoseTransition() {
      long poseTime = this.getPoseTime();
      return poseTime < (this.isCamelSitting() ? 40 : 52);
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
   public void resetLastPoseChangeTick(final long syncedPoseTickTime) {
      this.entityData.set(LAST_POSE_CHANGE_TICK, syncedPoseTickTime);
   }

   private void resetLastPoseChangeTickToFullStand(final long currentTime) {
      this.resetLastPoseChangeTick(Math.max(0L, currentTime - 52L - 1L));
   }

   public long getPoseTime() {
      return this.level().getGameTime() - Math.abs(this.entityData.get(LAST_POSE_CHANGE_TICK));
   }

   @Override
   protected Holder<SoundEvent> getEquipSound(final EquipmentSlot slot, final ItemStack stack, final Equippable equippable) {
      return slot == EquipmentSlot.SADDLE ? this.getSaddleSound() : super.getEquipSound(slot, stack, equippable);
   }

   protected Holder.Reference<SoundEvent> getSaddleSound() {
      return SoundEvents.CAMEL_SADDLE;
   }

   @Override
   public void onSyncedDataUpdated(final EntityDataAccessor<?> accessor) {
      if (!this.firstTick && DASH.equals(accessor)) {
         this.dashCooldown = this.dashCooldown == 0 ? 55 : this.dashCooldown;
      }

      super.onSyncedDataUpdated(accessor);
   }

   @Override
   public boolean isTamed() {
      return true;
   }

   @Override
   public void openCustomInventoryScreen(final Player player) {
      if (!this.level().isClientSide()) {
         player.openHorseInventory(this, this.inventory);
      }
   }

   @Override
   protected BodyRotationControl createBodyControl() {
      return new Camel.CamelBodyRotationControl(this);
   }

   private class CamelBodyRotationControl extends BodyRotationControl {
      public CamelBodyRotationControl(final Camel camel) {
         super(camel);
      }

      @Override
      public void clientTick() {
         if (!Camel.this.refuseToMove()) {
            super.clientTick();
         }
      }
   }

   private class CamelLookControl extends LookControl {
      private CamelLookControl() {
         super(Camel.this);
      }

      @Override
      public void tick() {
         if (!Camel.this.hasControllingPassenger()) {
            super.tick();
         }
      }
   }

   private static class CamelMoveControl<T extends Camel> extends MoveControl<T> {
      public CamelMoveControl(final T camel) {
         super(camel);
      }

      @Override
      public void tick() {
         if (this.operation == MoveControl.Operation.MOVE_TO
            && !this.mob.isLeashed()
            && this.mob.isCamelSitting()
            && !this.mob.isInPoseTransition()
            && this.mob.canCamelChangePose()) {
            this.mob.standUp();
         }

         super.tick();
      }
   }
}
