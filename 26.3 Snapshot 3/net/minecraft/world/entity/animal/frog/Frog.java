package net.minecraft.world.entity.animal.frog;

import java.util.List;
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
import net.minecraft.world.entity.EntityTypes;
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
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.cubemob.AbstractCubeMob;
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
   private static final Brain.Provider<Frog> BRAIN_PROVIDER = Brain.provider(
      List.of(SensorType.NEAREST_LIVING_ENTITIES, SensorType.HURT_BY, SensorType.FROG_ATTACKABLES, SensorType.FROG_TEMPTATIONS, SensorType.IS_IN_WATER),
      var0 -> FrogAi.getActivities()
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

   public Frog(final EntityType<? extends Animal> type, final Level level) {
      super(type, level);
      this.lookControl = new Frog.FrogLookControl(this);
      this.setPathfindingMalus(PathType.WATER, 4.0F);
      this.setPathfindingMalus(PathType.TRAPDOOR, -1.0F);
      this.moveControl = new SmoothSwimmingMoveControl<>(this, 85, 10, 0.02F, 0.1F, true);
   }

   @Override
   protected Brain<Frog> makeBrain(final Brain.Packed packedBrain) {
      return BRAIN_PROVIDER.makeBrain(this, packedBrain);
   }

   @Override
   public Brain<Frog> getBrain() {
      return super.getBrain();
   }

   @Override
   protected void defineSynchedData(final SynchedEntityData.Builder entityData) {
      super.defineSynchedData(entityData);
      Registry<FrogVariant> variants = this.registryAccess().lookupOrThrow(Registries.FROG_VARIANT);
      entityData.define(DATA_VARIANT_ID, VariantUtils.getDefaultOrAny(this.registryAccess(), DEFAULT_VARIANT));
      entityData.define(DATA_TONGUE_TARGET_ID, OptionalInt.empty());
   }

   public void eraseTongueTarget() {
      this.entityData.set(DATA_TONGUE_TARGET_ID, OptionalInt.empty());
   }

   public Optional<Entity> getTongueTarget() {
      return this.entityData.get(DATA_TONGUE_TARGET_ID).stream().mapToObj(this.level()::getEntity).filter(Objects::nonNull).findFirst();
   }

   public void setTongueTarget(final Entity target) {
      this.entityData.set(DATA_TONGUE_TARGET_ID, OptionalInt.of(target.getId()));
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

   private void setVariant(final Holder<FrogVariant> variant) {
      this.entityData.set(DATA_VARIANT_ID, variant);
   }

   @Override
   public <T> @Nullable T get(final DataComponentType<? extends T> type) {
      return type == DataComponents.FROG_VARIANT ? castComponentValue((DataComponentType<T>)type, this.getVariant()) : super.get(type);
   }

   @Override
   protected void applyImplicitComponents(final DataComponentGetter components) {
      this.applyImplicitComponentIfPresent(components, DataComponents.FROG_VARIANT);
      super.applyImplicitComponents(components);
   }

   @Override
   protected <T> boolean applyImplicitComponent(final DataComponentType<T> type, final T value) {
      if (type == DataComponents.FROG_VARIANT) {
         this.setVariant(castComponentValue(DataComponents.FROG_VARIANT, value));
         return true;
      } else {
         return super.applyImplicitComponent(type, value);
      }
   }

   @Override
   protected void addAdditionalSaveData(final ValueOutput output) {
      super.addAdditionalSaveData(output);
      VariantUtils.writeVariant(output, this.getVariant());
   }

   @Override
   protected void readAdditionalSaveData(final ValueInput input) {
      super.readAdditionalSaveData(input);
      VariantUtils.readVariant(input, Registries.FROG_VARIANT).ifPresent(this::setVariant);
   }

   @Override
   protected void customServerAiStep(final ServerLevel level) {
      ProfilerFiller profiler = Profiler.get();
      profiler.push("frogBrain");
      this.getBrain().tick(level, this);
      profiler.pop();
      profiler.push("frogActivityUpdate");
      FrogAi.updateActivity(this);
      profiler.pop();
      super.customServerAiStep(level);
   }

   @Override
   public void tick() {
      if (this.level().isClientSide()) {
         this.swimIdleAnimationState.animateWhen(this.isInWater() && !this.walkAnimation.isMoving(), this.tickCount);
      }

      super.tick();
   }

   @Override
   public void onSyncedDataUpdated(final EntityDataAccessor<?> accessor) {
      if (DATA_POSE.equals(accessor)) {
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

      super.onSyncedDataUpdated(accessor);
   }

   @Override
   protected void updateWalkAnimation(final float distance) {
      float targetSpeed;
      if (this.jumpAnimationState.isStarted()) {
         targetSpeed = 0.0F;
      } else {
         targetSpeed = Math.min(distance * 25.0F, 1.0F);
      }

      this.walkAnimation.update(targetSpeed, 0.4F, this.isBaby() ? 3.0F : 1.0F);
   }

   @Override
   public void playEatingSound() {
      this.level().playSound(null, this, SoundEvents.FROG_EAT, SoundSource.NEUTRAL, 2.0F, 1.0F);
   }

   @Override
   public @Nullable AgeableMob getBreedOffspring(final ServerLevel level, final AgeableMob partner) {
      Frog frog = EntityTypes.FROG.create(level, EntitySpawnReason.BREEDING);
      if (frog != null) {
         FrogAi.initMemories(frog, level.getRandom());
      }

      return frog;
   }

   @Override
   protected boolean canBeABaby() {
      return false;
   }

   @Override
   public void spawnChildFromBreeding(final ServerLevel level, final Animal partner) {
      this.finalizeSpawnChildFromBreeding(level, partner, null);
      this.getBrain().setMemory(MemoryModuleType.IS_PREGNANT, Unit.INSTANCE);
   }

   @Override
   public SpawnGroupData finalizeSpawn(
      final ServerLevelAccessor level, final DifficultyInstance difficulty, final EntitySpawnReason spawnReason, final @Nullable SpawnGroupData groupData
   ) {
      VariantUtils.selectVariantToSpawn(SpawnContext.create(level, this.blockPosition()), Registries.FROG_VARIANT).ifPresent(this::setVariant);
      FrogAi.initMemories(this, level.getRandom());
      return super.finalizeSpawn(level, difficulty, spawnReason, groupData);
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
   protected @Nullable SoundEvent getHurtSound(final DamageSource source) {
      return SoundEvents.FROG_HURT;
   }

   @Override
   protected @Nullable SoundEvent getDeathSound() {
      return SoundEvents.FROG_DEATH;
   }

   @Override
   protected void playStepSound(final BlockPos pos, final BlockState blockState) {
      this.playSound(SoundEvents.FROG_STEP, 0.15F, 1.0F);
   }

   @Override
   public boolean isPushedByFluid() {
      return false;
   }

   @Override
   protected int calculateFallDamage(final double fallDistance, final float damageModifier) {
      return super.calculateFallDamage(fallDistance, damageModifier) - 5;
   }

   @Override
   protected void travelInWater(final Vec3 input, final double baseGravity, final boolean isFalling, final double oldY) {
      this.moveRelative(this.getSpeed(), input);
      this.move(MoverType.SELF, this.getDeltaMovement());
      this.setDeltaMovement(this.getDeltaMovement().scale(0.9));
   }

   public static boolean canEat(final LivingEntity entity) {
      return entity instanceof AbstractCubeMob cubeMob && cubeMob.getSize() != 1 ? false : entity.is(EntityTypeTags.FROG_FOOD);
   }

   @Override
   protected PathNavigation createNavigation(final Level level) {
      return new Frog.FrogPathNavigation(this, level);
   }

   @Override
   public @Nullable LivingEntity getTarget() {
      return this.getTargetFromBrain();
   }

   @Override
   public boolean isFood(final ItemStack itemStack) {
      return itemStack.is(ItemTags.FROG_FOOD);
   }

   public static boolean checkFrogSpawnRules(
      final EntityType<? extends Animal> type, final LevelAccessor level, final EntitySpawnReason spawnReason, final BlockPos pos, final RandomSource random
   ) {
      return level.getBlockState(pos.below()).is(BlockTags.FROGS_SPAWNABLE_ON) && isBrightEnoughToSpawn(level, pos);
   }

   private class FrogLookControl extends LookControl {
      public FrogLookControl(final Mob mob) {
         super(mob);
      }

      @Override
      protected boolean resetXRotOnTick() {
         return Frog.this.getTongueTarget().isEmpty();
      }
   }

   private static class FrogNodeEvaluator extends AmphibiousNodeEvaluator {
      private final BlockPos.MutableBlockPos belowPos = new BlockPos.MutableBlockPos();

      public FrogNodeEvaluator(final boolean prefersShallowSwimming) {
         super(prefersShallowSwimming);
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
      public PathType getPathType(final PathfindingContext context, final int x, final int y, final int z) {
         this.belowPos.set(x, y - 1, z);
         BlockState belowState = context.getBlockState(this.belowPos);
         return belowState.is(BlockTags.FROG_PREFER_JUMP_TO) ? PathType.OPEN : super.getPathType(context, x, y, z);
      }
   }

   private static class FrogPathNavigation extends AmphibiousPathNavigation {
      public FrogPathNavigation(final Frog mob, final Level level) {
         super(mob, level);
      }

      @Override
      public boolean canCutCorner(final PathType pathType) {
         return pathType != PathType.WATER_BORDER && super.canCutCorner(pathType);
      }

      @Override
      protected PathFinder createPathFinder(final int maxVisitedNodes) {
         this.nodeEvaluator = new Frog.FrogNodeEvaluator(true);
         return new PathFinder(this.nodeEvaluator, maxVisitedNodes);
      }
   }
}
