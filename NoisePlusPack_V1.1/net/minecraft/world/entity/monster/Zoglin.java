package net.minecraft.world.entity.monster;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.ai.behavior.DoNothing;
import net.minecraft.world.entity.ai.behavior.LookAtTargetSink;
import net.minecraft.world.entity.ai.behavior.MeleeAttack;
import net.minecraft.world.entity.ai.behavior.MoveToTargetSink;
import net.minecraft.world.entity.ai.behavior.RandomStroll;
import net.minecraft.world.entity.ai.behavior.RunOne;
import net.minecraft.world.entity.ai.behavior.SetEntityLookTargetSometimes;
import net.minecraft.world.entity.ai.behavior.SetWalkTargetFromAttackTargetIfTargetOutOfReach;
import net.minecraft.world.entity.ai.behavior.SetWalkTargetFromLookTarget;
import net.minecraft.world.entity.ai.behavior.StartAttacking;
import net.minecraft.world.entity.ai.behavior.StopAttackingIfTargetInvalid;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.NearestVisibleLivingEntities;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.monster.hoglin.HoglinBase;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class Zoglin extends Monster implements HoglinBase {
   private static final EntityDataAccessor<Boolean> DATA_BABY_ID = SynchedEntityData.defineId(Zoglin.class, EntityDataSerializers.BOOLEAN);
   private static final int MAX_HEALTH = 40;
   private static final int ATTACK_KNOCKBACK = 1;
   private static final float KNOCKBACK_RESISTANCE = 0.6F;
   private static final int ATTACK_DAMAGE = 6;
   private static final float BABY_ATTACK_DAMAGE = 0.5F;
   private static final int ATTACK_INTERVAL = 40;
   private static final int BABY_ATTACK_INTERVAL = 15;
   private static final int ATTACK_DURATION = 200;
   private static final float MOVEMENT_SPEED_WHEN_FIGHTING = 0.3F;
   private static final float SPEED_MULTIPLIER_WHEN_IDLING = 0.4F;
   private static final boolean DEFAULT_BABY = false;
   private int attackAnimationRemainingTicks;
   protected static final ImmutableList<? extends SensorType<? extends Sensor<? super Zoglin>>> SENSOR_TYPES = ImmutableList.of(
      SensorType.NEAREST_LIVING_ENTITIES, SensorType.NEAREST_PLAYERS
   );
   protected static final ImmutableList<? extends MemoryModuleType<?>> MEMORY_TYPES = ImmutableList.of(
      MemoryModuleType.NEAREST_LIVING_ENTITIES,
      MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES,
      MemoryModuleType.NEAREST_VISIBLE_PLAYER,
      MemoryModuleType.NEAREST_VISIBLE_ATTACKABLE_PLAYER,
      MemoryModuleType.LOOK_TARGET,
      MemoryModuleType.WALK_TARGET,
      MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE,
      MemoryModuleType.PATH,
      MemoryModuleType.ATTACK_TARGET,
      MemoryModuleType.ATTACK_COOLING_DOWN
   );

   public Zoglin(EntityType<? extends Zoglin> p_34204_, Level p_34205_) {
      super(p_34204_, p_34205_);
      this.xpReward = 5;
   }

   @Override
   protected Brain.Provider<Zoglin> brainProvider() {
      return Brain.provider(MEMORY_TYPES, SENSOR_TYPES);
   }

   @Override
   protected Brain<?> makeBrain(Dynamic<?> p_34221_) {
      Brain<Zoglin> brain = this.brainProvider().makeBrain(p_34221_);
      initCoreActivity(brain);
      initIdleActivity(brain);
      initFightActivity(brain);
      brain.setCoreActivities(ImmutableSet.of(Activity.CORE));
      brain.setDefaultActivity(Activity.IDLE);
      brain.useDefaultActivity();
      return brain;
   }

   private static void initCoreActivity(Brain<Zoglin> p_34217_) {
      p_34217_.addActivity(Activity.CORE, 0, ImmutableList.of(new LookAtTargetSink(45, 90), new MoveToTargetSink()));
   }

   private static void initIdleActivity(Brain<Zoglin> p_34229_) {
      p_34229_.addActivity(
         Activity.IDLE,
         10,
         ImmutableList.of(
            StartAttacking.create((p_364948_, p_368246_) -> p_368246_.findNearestValidAttackTarget(p_364948_)),
            SetEntityLookTargetSometimes.create(8.0F, UniformInt.of(30, 60)),
            new RunOne(
               ImmutableList.of(
                  Pair.of(RandomStroll.stroll(0.4F), 2), Pair.of(SetWalkTargetFromLookTarget.create(0.4F, 3), 2), Pair.of(new DoNothing(30, 60), 1)
               )
            )
         )
      );
   }

   private static void initFightActivity(Brain<Zoglin> p_34237_) {
      p_34237_.addActivityAndRemoveMemoryWhenStopped(
         Activity.FIGHT,
         10,
         ImmutableList.of(
            SetWalkTargetFromAttackTargetIfTargetOutOfReach.create(1.0F),
            BehaviorBuilder.triggerIf(Zoglin::isAdult, MeleeAttack.create(40)),
            BehaviorBuilder.triggerIf(Zoglin::isBaby, MeleeAttack.create(15)),
            StopAttackingIfTargetInvalid.create()
         ),
         MemoryModuleType.ATTACK_TARGET
      );
   }

   private Optional<? extends LivingEntity> findNearestValidAttackTarget(ServerLevel p_363321_) {
      return this.getBrain()
         .getMemory(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES)
         .orElse(NearestVisibleLivingEntities.empty())
         .findClosest(p_365722_ -> this.isTargetable(p_363321_, p_365722_));
   }

   private boolean isTargetable(ServerLevel p_362768_, LivingEntity p_34253_) {
      EntityType<?> entitytype = p_34253_.getType();
      return entitytype != EntityType.ZOGLIN && entitytype != EntityType.CREEPER && Sensor.isEntityAttackable(p_362768_, this, p_34253_);
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder p_332153_) {
      super.defineSynchedData(p_332153_);
      p_332153_.define(DATA_BABY_ID, false);
   }

   @Override
   public void onSyncedDataUpdated(EntityDataAccessor<?> p_34225_) {
      super.onSyncedDataUpdated(p_34225_);
      if (DATA_BABY_ID.equals(p_34225_)) {
         this.refreshDimensions();
      }
   }

   @Override
   public @Nullable SpawnGroupData finalizeSpawn(
      ServerLevelAccessor p_370134_, DifficultyInstance p_367985_, EntitySpawnReason p_369650_, @Nullable SpawnGroupData p_362880_
   ) {
      if (p_370134_.getRandom().nextFloat() < 0.2F) {
         this.setBaby(true);
      }

      return super.finalizeSpawn(p_370134_, p_367985_, p_369650_, p_362880_);
   }

   public static AttributeSupplier.Builder createAttributes() {
      return Monster.createMonsterAttributes()
         .add(Attributes.MAX_HEALTH, 40.0)
         .add(Attributes.MOVEMENT_SPEED, 0.3F)
         .add(Attributes.KNOCKBACK_RESISTANCE, 0.6F)
         .add(Attributes.ATTACK_KNOCKBACK, 1.0)
         .add(Attributes.ATTACK_DAMAGE, 6.0);
   }

   public boolean isAdult() {
      return !this.isBaby();
   }

   @Override
   public boolean doHurtTarget(ServerLevel p_366973_, Entity p_34207_) {
      if (p_34207_ instanceof LivingEntity livingentity) {
         this.attackAnimationRemainingTicks = 10;
         p_366973_.broadcastEntityEvent(this, (byte)4);
         this.makeSound(SoundEvents.ZOGLIN_ATTACK);
         return HoglinBase.hurtAndThrowTarget(p_366973_, this, livingentity);
      } else {
         return false;
      }
   }

   @Override
   public boolean canBeLeashed() {
      return true;
   }

   @Override
   protected void blockedByItem(LivingEntity p_34246_) {
      if (!this.isBaby()) {
         HoglinBase.throwTarget(this, p_34246_);
      }
   }

   @Override
   public boolean hurtServer(ServerLevel p_369200_, DamageSource p_368778_, float p_370202_) {
      boolean flag = super.hurtServer(p_369200_, p_368778_, p_370202_);
      if (flag && p_368778_.getEntity() instanceof LivingEntity livingentity) {
         if (this.canAttack(livingentity) && !BehaviorUtils.isOtherTargetMuchFurtherAwayThanCurrentAttackTarget(this, livingentity, 4.0)) {
            this.setAttackTarget(livingentity);
         }

         return true;
      } else {
         return flag;
      }
   }

   private void setAttackTarget(LivingEntity p_34255_) {
      this.brain.eraseMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
      this.brain.setMemoryWithExpiry(MemoryModuleType.ATTACK_TARGET, p_34255_, 200L);
   }

   @Override
   public Brain<Zoglin> getBrain() {
      return (Brain<Zoglin>)super.getBrain();
   }

   protected void updateActivity() {
      Activity activity = this.brain.getActiveNonCoreActivity().orElse(null);
      this.brain.setActiveActivityToFirstValid(ImmutableList.of(Activity.FIGHT, Activity.IDLE));
      Activity activity1 = this.brain.getActiveNonCoreActivity().orElse(null);
      if (activity1 == Activity.FIGHT && activity != Activity.FIGHT) {
         this.playAngrySound();
      }

      this.setAggressive(this.brain.hasMemoryValue(MemoryModuleType.ATTACK_TARGET));
   }

   @Override
   protected void customServerAiStep(ServerLevel p_363122_) {
      ProfilerFiller profilerfiller = Profiler.get();
      profilerfiller.push("zoglinBrain");
      this.getBrain().tick(p_363122_, this);
      profilerfiller.pop();
      this.updateActivity();
   }

   @Override
   public void setBaby(boolean p_34227_) {
      this.getEntityData().set(DATA_BABY_ID, p_34227_);
      if (!this.level().isClientSide() && p_34227_) {
         this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(0.5);
      }
   }

   @Override
   public boolean isBaby() {
      return this.getEntityData().get(DATA_BABY_ID);
   }

   @Override
   public void aiStep() {
      if (this.attackAnimationRemainingTicks > 0) {
         this.attackAnimationRemainingTicks--;
      }

      super.aiStep();
   }

   @Override
   public void handleEntityEvent(byte p_34212_) {
      if (p_34212_ == 4) {
         this.attackAnimationRemainingTicks = 10;
         this.makeSound(SoundEvents.ZOGLIN_ATTACK);
      } else {
         super.handleEntityEvent(p_34212_);
      }
   }

   @Override
   public int getAttackAnimationRemainingTicks() {
      return this.attackAnimationRemainingTicks;
   }

   @Override
   protected SoundEvent getAmbientSound() {
      if (this.level().isClientSide()) {
         return null;
      } else {
         return this.brain.hasMemoryValue(MemoryModuleType.ATTACK_TARGET) ? SoundEvents.ZOGLIN_ANGRY : SoundEvents.ZOGLIN_AMBIENT;
      }
   }

   @Override
   protected SoundEvent getHurtSound(DamageSource p_34244_) {
      return SoundEvents.ZOGLIN_HURT;
   }

   @Override
   protected SoundEvent getDeathSound() {
      return SoundEvents.ZOGLIN_DEATH;
   }

   @Override
   protected void playStepSound(BlockPos p_34231_, BlockState p_34232_) {
      this.playSound(SoundEvents.ZOGLIN_STEP, 0.15F, 1.0F);
   }

   protected void playAngrySound() {
      this.makeSound(SoundEvents.ZOGLIN_ANGRY);
   }

   @Override
   public @Nullable LivingEntity getTarget() {
      return this.getTargetFromBrain();
   }

   @Override
   protected void addAdditionalSaveData(ValueOutput p_409877_) {
      super.addAdditionalSaveData(p_409877_);
      p_409877_.putBoolean("IsBaby", this.isBaby());
   }

   @Override
   protected void readAdditionalSaveData(ValueInput p_410476_) {
      super.readAdditionalSaveData(p_410476_);
      this.setBaby(p_410476_.getBooleanOr("IsBaby", false));
   }
}
