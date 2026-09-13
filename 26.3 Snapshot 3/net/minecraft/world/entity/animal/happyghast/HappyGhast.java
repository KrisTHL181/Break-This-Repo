package net.minecraft.world.entity.animal.happyghast;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundEntityPositionSyncPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.BodyRotationControl;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.control.LookControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class HappyGhast extends Animal {
   public static final float BABY_SCALE = 0.2375F;
   public static final int WANDER_GROUND_DISTANCE = 16;
   public static final int SMALL_RESTRICTION_RADIUS = 32;
   public static final int LARGE_RESTRICTION_RADIUS = 64;
   public static final int RESTRICTION_RADIUS_BUFFER = 16;
   public static final int FAST_HEALING_TICKS = 20;
   public static final int SLOW_HEALING_TICKS = 600;
   public static final int MAX_PASSANGERS = 4;
   private static final int STILL_TIMEOUT_ON_LOAD_GRACE_PERIOD = 60;
   private static final int MAX_STILL_TIMEOUT = 10;
   private static final Brain.Provider<HappyGhast> BRAIN_PROVIDER = Brain.provider(
      List.of(
         SensorType.NEAREST_LIVING_ENTITIES, SensorType.HURT_BY, SensorType.FOOD_TEMPTATIONS, SensorType.NEAREST_ADULT_ANY_TYPE, SensorType.NEAREST_PLAYERS
      ),
      var0 -> HappyGhastAi.getActivities()
   );
   public static final float SPEED_MULTIPLIER_WHEN_PANICKING = 2.0F;
   private int leashHolderTime = 0;
   private int serverStillTimeout;
   private static final EntityDataAccessor<Boolean> IS_LEASH_HOLDER = SynchedEntityData.defineId(HappyGhast.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<Boolean> STAYS_STILL = SynchedEntityData.defineId(HappyGhast.class, EntityDataSerializers.BOOLEAN);
   private static final float MAX_SCALE = 1.0F;
   private static final EntityDimensions BABY_DIMENSIONS = EntityTypes.HAPPY_GHAST.getDimensions().scale(0.2375F).withEyeHeight(0.46875F);

   public HappyGhast(final EntityType<? extends HappyGhast> type, final Level level) {
      super(type, level);
      this.moveControl = new Ghast.GhastMoveControl<>(this, true, this::isOnStillTimeout);
      this.lookControl = new HappyGhast.HappyGhastLookControl();
   }

   private void setServerStillTimeout(final int serverStillTimeout) {
      if (this.serverStillTimeout <= 0 && serverStillTimeout > 0 && this.level() instanceof ServerLevel serverLevel) {
         this.syncPacketPositionCodec(this.getX(), this.getY(), this.getZ());
         serverLevel.getChunkSource().chunkMap.sendToTrackingPlayers(this, ClientboundEntityPositionSyncPacket.of(this));
      }

      this.serverStillTimeout = serverStillTimeout;
      this.syncStayStillFlag();
   }

   private PathNavigation createBabyNavigation(final Level level) {
      return new HappyGhast.BabyFlyingPathNavigation(this, level);
   }

   @Override
   protected void registerGoals() {
      this.goalSelector.addGoal(3, new HappyGhast.HappyGhastFloatGoal());
      this.goalSelector
         .addGoal(
            4,
            new TemptGoal.ForNonPathfinders(
               this,
               1.0,
               itemStack -> !this.isWearingBodyArmor() && !this.isBaby()
                  ? itemStack.is(ItemTags.HAPPY_GHAST_TEMPT_ITEMS)
                  : itemStack.is(ItemTags.HAPPY_GHAST_FOOD),
               false,
               7.0
            )
         );
      this.goalSelector.addGoal(5, new Ghast.RandomFloatAroundGoal(this, 16));
   }

   private void adultGhastSetup() {
      this.moveControl = new Ghast.GhastMoveControl<>(this, true, this::isOnStillTimeout);
      this.lookControl = new HappyGhast.HappyGhastLookControl();
      this.navigation = this.createNavigation(this.level());
      if (this.level() instanceof ServerLevel serverLevel) {
         this.removeAllGoals(goal -> true);
         this.registerGoals();
         this.getBrain().stopAll(serverLevel, this);
         this.brain.clearMemories();
      }
   }

   private void babyGhastSetup() {
      this.moveControl = new FlyingMoveControl<>(this, 180, true);
      this.lookControl = new LookControl(this);
      this.navigation = this.createBabyNavigation(this.level());
      this.setServerStillTimeout(0);
      this.removeAllGoals(goal -> true);
   }

   @Override
   protected void ageBoundaryReached() {
      if (this.isBaby()) {
         this.babyGhastSetup();
      } else {
         this.adultGhastSetup();
      }

      super.ageBoundaryReached();
   }

   public static AttributeSupplier.Builder createAttributes() {
      return Animal.createAnimalAttributes()
         .add(Attributes.MAX_HEALTH, 20.0)
         .add(Attributes.TEMPT_RANGE, 16.0)
         .add(Attributes.FLYING_SPEED, 0.05)
         .add(Attributes.MOVEMENT_SPEED, 0.05)
         .add(Attributes.FOLLOW_RANGE, 16.0)
         .add(Attributes.CAMERA_DISTANCE, 8.0);
   }

   @Override
   protected float sanitizeScale(final float scale) {
      return Math.min(scale, 1.0F);
   }

   @Override
   protected void checkFallDamage(final double ya, final boolean onGround, final BlockState onState, final BlockPos pos) {
   }

   @Override
   public boolean onClimbable() {
      return false;
   }

   @Override
   public void travel(final Vec3 input) {
      float speed = (float)this.getAttributeValue(Attributes.FLYING_SPEED) * 5.0F / 3.0F;
      this.travelFlying(input, speed, speed, speed);
   }

   @Override
   public float getWalkTargetValue(final BlockPos pos, final LevelReader level) {
      if (!level.isEmptyBlock(pos)) {
         return 0.0F;
      } else {
         return level.isEmptyBlock(pos.below()) && !level.isEmptyBlock(pos.below(2)) ? 10.0F : 5.0F;
      }
   }

   @Override
   public boolean canBreatheUnderwater() {
      return this.isBaby() ? true : super.canBreatheUnderwater();
   }

   @Override
   protected boolean shouldStayCloseToLeashHolder() {
      return false;
   }

   @Override
   protected void playStepSound(final BlockPos pos, final BlockState blockState) {
   }

   @Override
   public float getVoicePitch() {
      return 1.0F;
   }

   @Override
   public SoundSource getSoundSource() {
      return SoundSource.NEUTRAL;
   }

   @Override
   public int getAmbientSoundInterval() {
      int interval = super.getAmbientSoundInterval();
      return this.isVehicle() ? interval * 6 : interval;
   }

   @Override
   protected SoundEvent getAmbientSound() {
      return this.isBaby() ? SoundEvents.GHASTLING_AMBIENT : SoundEvents.HAPPY_GHAST_AMBIENT;
   }

   @Override
   protected SoundEvent getHurtSound(final DamageSource source) {
      return this.isBaby() ? SoundEvents.GHASTLING_HURT : SoundEvents.HAPPY_GHAST_HURT;
   }

   @Override
   protected SoundEvent getDeathSound() {
      return this.isBaby() ? SoundEvents.GHASTLING_DEATH : SoundEvents.HAPPY_GHAST_DEATH;
   }

   @Override
   protected float getSoundVolume() {
      return this.isBaby() ? 1.0F : 4.0F;
   }

   @Override
   public int getMaxSpawnClusterSize() {
      return 1;
   }

   @Override
   public EntityDimensions getDefaultDimensions(final Pose pose) {
      return this.isBaby() ? BABY_DIMENSIONS : super.getDefaultDimensions(pose);
   }

   @Override
   public @Nullable AgeableMob getBreedOffspring(final ServerLevel level, final AgeableMob partner) {
      return EntityTypes.HAPPY_GHAST.create(level, EntitySpawnReason.BREEDING);
   }

   @Override
   public boolean canFallInLove() {
      return false;
   }

   @Override
   public float getAgeScale() {
      return this.isBaby() ? 0.2375F : 1.0F;
   }

   @Override
   public boolean isFood(final ItemStack itemStack) {
      return itemStack.is(ItemTags.HAPPY_GHAST_FOOD);
   }

   @Override
   public boolean canUseSlot(final EquipmentSlot slot) {
      return slot != EquipmentSlot.BODY ? super.canUseSlot(slot) : this.isAlive() && !this.isBaby();
   }

   @Override
   protected boolean canDispenserEquipIntoSlot(final EquipmentSlot slot) {
      return slot == EquipmentSlot.BODY;
   }

   @Override
   public InteractionResult mobInteract(final Player player, final InteractionHand hand) {
      if (this.isBaby()) {
         return super.mobInteract(player, hand);
      }

      ItemStack itemStack = player.getItemInHand(hand);
      if (!itemStack.isEmpty()) {
         InteractionResult interactionResult = itemStack.interactLivingEntity(player, this, hand);
         if (interactionResult.consumesAction()) {
            return interactionResult;
         }
      }

      if (this.isWearingBodyArmor() && !player.isSecondaryUseActive()) {
         this.doPlayerRide(player);
         return InteractionResult.SUCCESS;
      } else {
         return super.mobInteract(player, hand);
      }
   }

   private void doPlayerRide(final Player player) {
      if (!this.level().isClientSide()) {
         player.startRiding(this);
      }
   }

   @Override
   protected void addPassenger(final Entity passenger) {
      if (!this.isVehicle()) {
         this.playSound(SoundEvents.HARNESS_GOGGLES_DOWN);
      }

      super.addPassenger(passenger);
      if (!this.level().isClientSide()) {
         if (!this.scanPlayerAboveGhast()) {
            this.setServerStillTimeout(0);
         } else if (this.serverStillTimeout > 10) {
            this.setServerStillTimeout(10);
         }
      }
   }

   @Override
   protected void removePassenger(final Entity passenger) {
      super.removePassenger(passenger);
      if (!this.level().isClientSide()) {
         this.setServerStillTimeout(10);
      }

      if (!this.isVehicle()) {
         this.clearHome();
         this.playSound(SoundEvents.HARNESS_GOGGLES_UP);
      }
   }

   @Override
   protected boolean canAddPassenger(final Entity passenger) {
      return this.getPassengers().size() < 4;
   }

   @Override
   public @Nullable LivingEntity getControllingPassenger() {
      return this.isWearingBodyArmor() && !this.isOnStillTimeout() && this.getFirstPassenger() instanceof Player player
         ? player
         : super.getControllingPassenger();
   }

   @Override
   protected Vec3 getRiddenInput(final Player controller, final Vec3 selfInput) {
      float strafe = controller.xxa;
      float forward = 0.0F;
      float up = 0.0F;
      if (controller.zza != 0.0F) {
         float forwardLook = Mth.cos(controller.getXRot() * (float) (Math.PI / 180.0));
         float upLook = -Mth.sin(controller.getXRot() * (float) (Math.PI / 180.0));
         if (controller.zza < 0.0F) {
            forwardLook *= -0.5F;
            upLook *= -0.5F;
         }

         up = upLook;
         forward = forwardLook;
      }

      if (controller.isJumping()) {
         up += 0.5F;
      }

      return new Vec3(strafe, up, forward).scale(3.9F * this.getAttributeValue(Attributes.FLYING_SPEED));
   }

   protected Vec2 getRiddenRotation(final LivingEntity controller) {
      return new Vec2(controller.getXRot() * 0.5F, controller.getYRot());
   }

   @Override
   protected void tickRidden(final Player controller, final Vec3 riddenInput) {
      super.tickRidden(controller, riddenInput);
      Vec2 rotation = this.getRiddenRotation(controller);
      float yRot = this.getYRot();
      float diff = Mth.wrapDegrees(rotation.y - yRot);
      float turnSpeed = 0.08F;
      yRot += diff * 0.08F;
      this.setRot(yRot, rotation.x);
      this.yRotO = this.yBodyRot = this.yHeadRot = yRot;
   }

   @Override
   protected Brain<HappyGhast> makeBrain(final Brain.Packed packedBrain) {
      return BRAIN_PROVIDER.makeBrain(this, packedBrain);
   }

   @Override
   public Brain<HappyGhast> getBrain() {
      return super.getBrain();
   }

   @Override
   protected void customServerAiStep(final ServerLevel level) {
      if (this.isBaby()) {
         ProfilerFiller profiler = Profiler.get();
         profiler.push("happyGhastBrain");
         this.getBrain().tick(level, this);
         profiler.pop();
         profiler.push("happyGhastActivityUpdate");
         HappyGhastAi.updateActivity(this);
         profiler.pop();
      }

      this.checkRestriction();
      super.customServerAiStep(level);
   }

   @Override
   public void tick() {
      super.tick();
      if (!this.level().isClientSide()) {
         if (this.leashHolderTime > 0) {
            this.leashHolderTime--;
         }

         this.setLeashHolder(this.leashHolderTime > 0);
         if (this.serverStillTimeout > 0) {
            if (this.tickCount > 60) {
               this.serverStillTimeout--;
            }

            this.setServerStillTimeout(this.serverStillTimeout);
         }

         if (this.scanPlayerAboveGhast()) {
            this.setServerStillTimeout(10);
         }
      }
   }

   @Override
   public void aiStep() {
      if (!this.level().isClientSide()) {
         this.setRequiresPrecisePosition(this.isOnStillTimeout());
      }

      super.aiStep();
      this.continuousHeal();
   }

   private int getHappyGhastRestrictionRadius() {
      return !this.isBaby() && this.getItemBySlot(EquipmentSlot.BODY).isEmpty() ? 64 : 32;
   }

   private void checkRestriction() {
      if (!this.isLeashed() && !this.isVehicle()) {
         int radius = this.getHappyGhastRestrictionRadius();
         if (!this.hasHome() || !this.getHomePosition().closerThan(this.blockPosition(), radius + 16) || radius != this.getHomeRadius()) {
            this.setHomeTo(this.blockPosition(), radius);
         }
      }
   }

   private void continuousHeal() {
      if (this.level() instanceof ServerLevel level && this.isAlive() && this.deathTime == 0 && this.getMaxHealth() != this.getHealth()) {
         boolean isFastHealing = this.isInClouds() || level.precipitationAt(this.blockPosition()) != Biome.Precipitation.NONE;
         if (this.tickCount % (isFastHealing ? 20 : 600) == 0) {
            this.heal(1.0F);
         }
      }
   }

   @Override
   protected void defineSynchedData(final SynchedEntityData.Builder entityData) {
      super.defineSynchedData(entityData);
      entityData.define(IS_LEASH_HOLDER, false);
      entityData.define(STAYS_STILL, false);
   }

   private void setLeashHolder(final boolean isLeashHolder) {
      this.entityData.set(IS_LEASH_HOLDER, isLeashHolder);
   }

   public boolean isLeashHolder() {
      return this.entityData.get(IS_LEASH_HOLDER);
   }

   private void syncStayStillFlag() {
      this.entityData.set(STAYS_STILL, this.serverStillTimeout > 0);
   }

   public boolean staysStill() {
      return this.entityData.get(STAYS_STILL);
   }

   @Override
   public boolean supportQuadLeashAsHolder() {
      return true;
   }

   @Override
   public Vec3[] getQuadLeashHolderOffsets() {
      return Leashable.createQuadLeashOffsets(this, -0.03125, 0.4375, 0.46875, 0.03125);
   }

   @Override
   public Vec3 getLeashOffset() {
      return Vec3.ZERO;
   }

   @Override
   public double leashElasticDistance() {
      return 10.0;
   }

   @Override
   public double leashSnapDistance() {
      return 16.0;
   }

   @Override
   public void onElasticLeashPull() {
      super.onElasticLeashPull();
      this.getMoveControl().setWait();
   }

   @Override
   public void notifyLeashHolder(final Leashable entity) {
      if (entity.supportQuadLeash()) {
         this.leashHolderTime = 5;
      }
   }

   @Override
   public void addAdditionalSaveData(final ValueOutput tag) {
      super.addAdditionalSaveData(tag);
      tag.putInt("still_timeout", this.serverStillTimeout);
   }

   @Override
   public void readAdditionalSaveData(final ValueInput tag) {
      super.readAdditionalSaveData(tag);
      this.setServerStillTimeout(tag.getIntOr("still_timeout", 0));
   }

   public boolean isOnStillTimeout() {
      return this.staysStill() || this.serverStillTimeout > 0;
   }

   private boolean scanPlayerAboveGhast() {
      AABB happyGhastBb = this.getBoundingBox();
      AABB ghastDetectionBox = new AABB(
         happyGhastBb.minX - 1.0,
         happyGhastBb.maxY - 1.0E-5F,
         happyGhastBb.minZ - 1.0,
         happyGhastBb.maxX + 1.0,
         happyGhastBb.maxY + happyGhastBb.getYsize() / 2.0,
         happyGhastBb.maxZ + 1.0
      );

      for (Player player : this.level().players()) {
         if (!player.isSpectator()) {
            Entity rootVehicle = player.getRootVehicle();
            if (!(rootVehicle instanceof HappyGhast) && ghastDetectionBox.contains(rootVehicle.position())) {
               return true;
            }
         }
      }

      return false;
   }

   @Override
   protected BodyRotationControl createBodyControl() {
      return new HappyGhast.HappyGhastBodyRotationControl();
   }

   @Override
   public boolean canBeCollidedWith(final @Nullable Entity other) {
      if (!this.isBaby() && this.isAlive()) {
         if (this.level().isClientSide() && other instanceof Player && other.position().y >= this.getBoundingBox().maxY) {
            return true;
         } else {
            return this.isVehicle() && other instanceof HappyGhast ? true : this.isOnStillTimeout();
         }
      } else {
         return false;
      }
   }

   @Override
   public boolean isFlyingVehicle() {
      return !this.isBaby();
   }

   @Override
   public Vec3 getDismountLocationForPassenger(final LivingEntity passenger) {
      return new Vec3(this.getX(), this.getBoundingBox().maxY, this.getZ());
   }

   private static class BabyFlyingPathNavigation extends FlyingPathNavigation {
      public BabyFlyingPathNavigation(final HappyGhast mob, final Level level) {
         super(mob, level);
         this.setCanOpenDoors(false);
         this.setCanFloat(true);
         this.setRequiredPathLength(48.0F);
      }

      @Override
      protected boolean canMoveDirectly(final Vec3 startPos, final Vec3 stopPos) {
         return isClearForMovementBetween(this.mob, startPos, stopPos, false);
      }
   }

   private class HappyGhastBodyRotationControl extends BodyRotationControl {
      public HappyGhastBodyRotationControl() {
         super(HappyGhast.this);
      }

      @Override
      public void clientTick() {
         if (HappyGhast.this.isVehicle()) {
            HappyGhast.this.yHeadRot = HappyGhast.this.getYRot();
            HappyGhast.this.yBodyRot = HappyGhast.this.yHeadRot;
         }

         super.clientTick();
      }
   }

   private class HappyGhastFloatGoal extends FloatGoal {
      public HappyGhastFloatGoal() {
         super(HappyGhast.this);
      }

      @Override
      public boolean canUse() {
         return !HappyGhast.this.isOnStillTimeout() && super.canUse();
      }
   }

   private class HappyGhastLookControl extends LookControl {
      private HappyGhastLookControl() {
         super(HappyGhast.this);
      }

      @Override
      public void tick() {
         if (HappyGhast.this.isOnStillTimeout()) {
            float closeAngle = Mth.wrapDegrees90(HappyGhast.this.getYRot());
            HappyGhast.this.setYRot(HappyGhast.this.getYRot() - closeAngle);
            HappyGhast.this.setYHeadRot(HappyGhast.this.getYRot());
         } else if (this.lookAtCooldown > 0) {
            this.lookAtCooldown--;
            double xdd = this.wantedX - HappyGhast.this.getX();
            double zdd = this.wantedZ - HappyGhast.this.getZ();
            HappyGhast.this.setYRot(-((float)Mth.atan2(xdd, zdd)) * (180.0F / (float)Math.PI));
            HappyGhast.this.yBodyRot = HappyGhast.this.getYRot();
            HappyGhast.this.yHeadRot = HappyGhast.this.yBodyRot;
         } else {
            Ghast.faceMovementDirection(this.mob);
         }
      }
   }
}
