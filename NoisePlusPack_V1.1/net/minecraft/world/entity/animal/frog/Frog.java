package net.minecraft.world.entity.animal.frog;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Dynamic;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Unit;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.LookControl;
import net.minecraft.world.entity.ai.control.SmoothSwimmingMoveControl;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.navigation.AmphibiousPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.variant.SpawnContext;
import net.minecraft.world.entity.variant.VariantUtils;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.AmphibiousNodeEvaluator;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.PathFinder;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.PathfindingContext;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class Frog extends Animal {
   protected static final ImmutableList<SensorType<? extends Sensor<? super Frog>>> SENSOR_TYPES = ImmutableList.of(
      SensorType.NEAREST_LIVING_ENTITIES, SensorType.HURT_BY, SensorType.FROG_ATTACKABLES, SensorType.FROG_TEMPTATIONS, SensorType.IS_IN_WATER
   );
   protected static final ImmutableList<MemoryModuleType<?>> MEMORY_TYPES = ImmutableList.of(
      MemoryModuleType.LOOK_TARGET,
      MemoryModuleType.NEAREST_LIVING_ENTITIES,
      MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES,
      MemoryModuleType.WALK_TARGET,
      MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE,
      MemoryModuleType.PATH,
      MemoryModuleType.BREED_TARGET,
      MemoryModuleType.LONG_JUMP_COOLDOWN_TICKS,
      MemoryModuleType.LONG_JUMP_MID_JUMP,
      MemoryModuleType.ATTACK_TARGET,
      MemoryModuleType.TEMPTING_PLAYER,
      MemoryModuleType.TEMPTATION_COOLDOWN_TICKS,
      new MemoryModuleType[]{
         MemoryModuleType.IS_TEMPTED,
         MemoryModuleType.HURT_BY,
         MemoryModuleType.HURT_BY_ENTITY,
         MemoryModuleType.NEAREST_ATTACKABLE,
         MemoryModuleType.IS_IN_WATER,
         MemoryModuleType.IS_PREGNANT,
         MemoryModuleType.IS_PANICKING,
         MemoryModuleType.UNREACHABLE_TONGUE_TARGETS
      }
   );
   private static final EntityDataAccessor<Holder<FrogVariant>> DATA_VARIANT_ID = SynchedEntityData.defineId(Frog.class, EntityDataSerializers.FROG_VARIANT);
   private static final EntityDataAccessor<OptionalInt> DATA_TONGUE_TARGET_ID = SynchedEntityData.defineId(
      Frog.class, EntityDataSerializers.OPTIONAL_UNSIGNED_INT
   );
   private static final int FROG_FALL_DAMAGE_REDUCTION = 5;
   private static final ResourceKey<FrogVariant> DEFAULT_VARIANT = FrogVariants.TEMPERATE;
   public final AnimationState jumpAnimationState = new AnimationState();
   public final AnimationState croakAnimationState = new AnimationState();
   public final AnimationState tongueAnimationState = new AnimationState();
   public final AnimationState swimIdleAnimationState = new AnimationState();

   public Frog(EntityType<? extends Animal> p_218470_, Level p_218471_) {
      super(p_218470_, p_218471_);
      this.lookControl = new Frog.FrogLookControl(this);
      this.setPathfindingMalus(PathType.WATER, 4.0F);
      this.setPathfindingMalus(PathType.TRAPDOOR, -1.0F);
      this.moveControl = new SmoothSwimmingMoveControl(this, 85, 10, 0.02F, 0.1F, true);
   }

   @Override
   protected Brain.Provider<Frog> brainProvider() {
      return Brain.provider(MEMORY_TYPES, SENSOR_TYPES);
   }

   @Override
   protected Brain<?> makeBrain(Dynamic<?> p_218494_) {
      return FrogAi.makeBrain(this.brainProvider().makeBrain(p_218494_));
   }

   @Override
   public Brain<Frog> getBrain() {
      return (Brain<Frog>)super.getBrain();
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder p_332901_) {
      super.defineSynchedData(p_332901_);
      Registry<FrogVariant> registry = this.registryAccess().lookupOrThrow(Registries.FROG_VARIANT);
      p_332901_.define(DATA_VARIANT_ID, VariantUtils.getDefaultOrAny(this.registryAccess(), DEFAULT_VARIANT));
      p_332901_.define(DATA_TONGUE_TARGET_ID, OptionalInt.empty());
   }

   public void eraseTongueTarget() {
      this.entityData.set(DATA_TONGUE_TARGET_ID, OptionalInt.empty());
   }

   public Optional<Entity> getTongueTarget() {
      return this.entityData.get(DATA_TONGUE_TARGET_ID).stream().mapToObj(this.level()::getEntity).filter(Objects::nonNull).findFirst();
   }

   public void setTongueTarget(Entity p_218482_) {
      this.entityData.set(DATA_TONGUE_TARGET_ID, OptionalInt.of(p_218482_.getId()));
   }

   @Override
   public int getHeadRotSpeed() {
      return 35;
   }

   @Override
   public int getMaxHeadYRot() {
      return 5;
   }

   public Holder<FrogVariant> getVariant() {
      return this.entityData.get(DATA_VARIANT_ID);
   }

   private void setVariant(Holder<FrogVariant> p_329156_) {
      this.entityData.set(DATA_VARIANT_ID, p_329156_);
   }

   @Override
   public <T> @Nullable T get(DataComponentType<? extends T> p_395428_) {
      return p_395428_ == DataComponents.FROG_VARIANT ? castComponentValue((DataComponentType<T>)p_395428_, this.getVariant()) : super.get(p_395428_);
   }

   @Override
   protected void applyImplicitComponents(DataComponentGetter p_396714_) {
      this.applyImplicitComponentIfPresent(p_396714_, DataComponents.FROG_VARIANT);
      super.applyImplicitComponents(p_396714_);
   }

   @Override
   protected <T> boolean applyImplicitComponent(DataComponentType<T> p_395194_, T p_391742_) {
      if (p_395194_ == DataComponents.FROG_VARIANT) {
         this.setVariant(castComponentValue(DataComponents.FROG_VARIANT, p_391742_));
         return true;
      } else {
         return super.applyImplicitComponent(p_395194_, p_391742_);
      }
   }

   @Override
   protected void addAdditionalSaveData(ValueOutput p_406919_) {
      super.addAdditionalSaveData(p_406919_);
      VariantUtils.writeVariant(p_406919_, this.getVariant());
   }

   @Override
   protected void readAdditionalSaveData(ValueInput p_408198_) {
      super.readAdditionalSaveData(p_408198_);
      VariantUtils.readVariant(p_408198_, Registries.FROG_VARIANT).ifPresent(this::setVariant);
   }

   @Override
   protected void customServerAiStep(ServerLevel p_364897_) {
      ProfilerFiller profilerfiller = Profiler.get();
      profilerfiller.push("frogBrain");
      this.getBrain().tick(p_364897_, this);
      profilerfiller.pop();
      profilerfiller.push("frogActivityUpdate");
      FrogAi.updateActivity(this);
      profilerfiller.pop();
      super.customServerAiStep(p_364897_);
   }

   @Override
   public void tick() {
      if (this.level().isClientSide()) {
         this.swimIdleAnimationState.animateWhen(this.isInWater() && !this.walkAnimation.isMoving(), this.tickCount);
      }

      super.tick();
   }

   @Override
   public void onSyncedDataUpdated(EntityDataAccessor<?> p_218498_) {
      if (DATA_POSE.equals(p_218498_)) {
         Pose pose = this.getPose();
         if (pose == Pose.LONG_JUMPING) {
            this.jumpAnimationState.start(this.tickCount);
         } else {
            this.jumpAnimationState.stop();
         }

         if (pose == Pose.CROAKING) {
            this.croakAnimationState.start(this.tickCount);
         } else {
            this.croakAnimationState.stop();
         }

         if (pose == Pose.USING_TONGUE) {
            this.tongueAnimationState.start(this.tickCount);
         } else {
            this.tongueAnimationState.stop();
         }
      }

      super.onSyncedDataUpdated(p_218498_);
   }

   @Override
   protected void updateWalkAnimation(float p_268239_) {
      float f;
      if (this.jumpAnimationState.isStarted()) {
         f = 0.0F;
      } else {
         f = Math.min(p_268239_ * 25.0F, 1.0F);
      }

      this.walkAnimation.update(f, 0.4F, this.isBaby() ? 3.0F : 1.0F);
   }

   @Override
   public void playEatingSound() {
      this.level().playSound(null, this, SoundEvents.FROG_EAT, SoundSource.NEUTRAL, 2.0F, 1.0F);
   }

   @Override
   public @Nullable AgeableMob getBreedOffspring(ServerLevel p_218476_, AgeableMob p_218477_) {
      Frog frog = EntityType.FROG.create(p_218476_, EntitySpawnReason.BREEDING);
      if (frog != null) {
         FrogAi.initMemories(frog, p_218476_.getRandom());
      }

      return frog;
   }

   @Override
   public boolean isBaby() {
      return false;
   }

   @Override
   public void setBaby(boolean p_218500_) {
   }

   @Override
   public void spawnChildFromBreeding(ServerLevel p_218479_, Animal p_218480_) {
      this.finalizeSpawnChildFromBreeding(p_218479_, p_218480_, null);
      this.getBrain().setMemory(MemoryModuleType.IS_PREGNANT, Unit.INSTANCE);
   }

   @Override
   public SpawnGroupData finalizeSpawn(
      ServerLevelAccessor p_218488_, DifficultyInstance p_218489_, EntitySpawnReason p_366918_, @Nullable SpawnGroupData p_218491_
   ) {
      VariantUtils.selectVariantToSpawn(SpawnContext.create(p_218488_, this.blockPosition()), Registries.FROG_VARIANT).ifPresent(this::setVariant);
      FrogAi.initMemories(this, p_218488_.getRandom());
      return super.finalizeSpawn(p_218488_, p_218489_, p_366918_, p_218491_);
   }

   public static AttributeSupplier.Builder createAttributes() {
      return Animal.createAnimalAttributes()
         .add(Attributes.MOVEMENT_SPEED, 1.0)
         .add(Attributes.MAX_HEALTH, 10.0)
         .add(Attributes.ATTACK_DAMAGE, 10.0)
         .add(Attributes.STEP_HEIGHT, 1.0);
   }

   @Override
   protected @Nullable SoundEvent getAmbientSound() {
      return SoundEvents.FROG_AMBIENT;
   }

   @Override
   protected @Nullable SoundEvent getHurtSound(DamageSource p_218510_) {
      return SoundEvents.FROG_HURT;
   }

   @Override
   protected @Nullable SoundEvent getDeathSound() {
      return SoundEvents.FROG_DEATH;
   }

   @Override
   protected void playStepSound(BlockPos p_218505_, BlockState p_218506_) {
      this.playSound(SoundEvents.FROG_STEP, 0.15F, 1.0F);
   }

   @Override
   public boolean isPushedByFluid() {
      return false;
   }

   @Override
   protected int calculateFallDamage(double p_393819_, float p_218519_) {
      return super.calculateFallDamage(p_393819_, p_218519_) - 5;
   }

   @Override
   protected void travelInWater(Vec3 p_457063_, double p_457045_, boolean p_457410_, double p_450890_) {
      this.moveRelative(this.getSpeed(), p_457063_);
      this.move(MoverType.SELF, this.getDeltaMovement());
      this.setDeltaMovement(this.getDeltaMovement().scale(0.9));
   }

   public static boolean canEat(LivingEntity p_218533_) {
      return p_218533_ instanceof Slime slime && slime.getSize() != 1 ? false : p_218533_.getType().is(EntityTypeTags.FROG_FOOD);
   }

   @Override
   protected PathNavigation createNavigation(Level p_218486_) {
      return new Frog.FrogPathNavigation(this, p_218486_);
   }

   @Override
   public @Nullable LivingEntity getTarget() {
      return this.getTargetFromBrain();
   }

   @Override
   public boolean isFood(ItemStack p_218535_) {
      return p_218535_.is(ItemTags.FROG_FOOD);
   }

   public static boolean checkFrogSpawnRules(
      EntityType<? extends Animal> p_218512_, LevelAccessor p_218513_, EntitySpawnReason p_369828_, BlockPos p_218515_, RandomSource p_218516_
   ) {
      return p_218513_.getBlockState(p_218515_.below()).is(BlockTags.FROGS_SPAWNABLE_ON) && isBrightEnoughToSpawn(p_218513_, p_218515_);
   }

   class FrogLookControl extends LookControl {
      FrogLookControl(final Mob p_218544_) {
         super(p_218544_);
      }

      @Override
      protected boolean resetXRotOnTick() {
         return Frog.this.getTongueTarget().isEmpty();
      }
   }

   static class FrogNodeEvaluator extends AmphibiousNodeEvaluator {
      private final BlockPos.MutableBlockPos belowPos = new BlockPos.MutableBlockPos();

      public FrogNodeEvaluator(boolean p_218548_) {
         super(p_218548_);
      }

      @Override
      public Node getStart() {
         return !this.mob.isInWater()
            ? super.getStart()
            : this.getStartNode(
               new BlockPos(Mth.floor(this.mob.getBoundingBox().minX), Mth.floor(this.mob.getBoundingBox().minY), Mth.floor(this.mob.getBoundingBox().minZ))
            );
      }

      @Override
      public PathType getPathType(PathfindingContext p_329957_, int p_331836_, int p_331056_, int p_330649_) {
         this.belowPos.set(p_331836_, p_331056_ - 1, p_330649_);
         BlockState blockstate = p_329957_.getBlockState(this.belowPos);
         return blockstate.is(BlockTags.FROG_PREFER_JUMP_TO) ? PathType.OPEN : super.getPathType(p_329957_, p_331836_, p_331056_, p_330649_);
      }
   }

   static class FrogPathNavigation extends AmphibiousPathNavigation {
      FrogPathNavigation(Frog p_218556_, Level p_218557_) {
         super(p_218556_, p_218557_);
      }

      @Override
      public boolean canCutCorner(PathType p_330883_) {
         return p_330883_ != PathType.WATER_BORDER && super.canCutCorner(p_330883_);
      }

      @Override
      protected PathFinder createPathFinder(int p_218559_) {
         this.nodeEvaluator = new Frog.FrogNodeEvaluator(true);
         return new PathFinder(this.nodeEvaluator, p_218559_);
      }
   }
}
