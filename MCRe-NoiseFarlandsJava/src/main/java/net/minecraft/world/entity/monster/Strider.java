package net.minecraft.world.entity.monster;

import com.google.common.collect.Sets;
import com.google.common.collect.UnmodifiableIterator;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityAttachment;
import net.minecraft.world.entity.EntityAttachments;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ItemBasedSteering;
import net.minecraft.world.entity.ItemSteerable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FollowParentGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MoveToBlockGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.DismountHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.level.pathfinder.PathFinder;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class Strider extends Animal implements ItemSteerable {
   private static final Identifier SUFFOCATING_MODIFIER_ID = Identifier.withDefaultNamespace("suffocating");
   private static final AttributeModifier SUFFOCATING_MODIFIER = new AttributeModifier(
      SUFFOCATING_MODIFIER_ID, -0.34F, AttributeModifier.Operation.ADD_MULTIPLIED_BASE
   );
   private static final float SUFFOCATE_STEERING_MODIFIER = 0.35F;
   private static final float STEERING_MODIFIER = 0.55F;
   private static final EntityDataAccessor<Integer> DATA_BOOST_TIME = SynchedEntityData.defineId(Strider.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Boolean> DATA_SUFFOCATING = SynchedEntityData.defineId(Strider.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDimensions BABY_DIMENSIONS = EntityDimensions.scalable(0.45F, 0.85F)
      .withEyeHeight(0.4375F)
      .withAttachments(EntityAttachments.builder().attach(EntityAttachment.PASSENGER, 0.0F, 0.65625F, 0.0F));
   private final ItemBasedSteering steering = new ItemBasedSteering(this.entityData, DATA_BOOST_TIME);
   private @Nullable TemptGoal temptGoal;

   public Strider(final EntityType<? extends Strider> strider, final Level level) {
      super(strider, level);
      this.blocksBuilding = true;
      this.setPathfindingMalus(PathType.WATER, -1.0F);
      this.setPathfindingMalus(PathType.LAVA, 0.0F);
      this.setPathfindingMalus(PathType.FIRE_IN_NEIGHBOR, 0.0F);
      this.setPathfindingMalus(PathType.FIRE, 0.0F);
   }

   public static boolean checkStriderSpawnRules(
      final EntityType<Strider> ignoredType,
      final LevelAccessor level,
      final EntitySpawnReason ignoredSpawnType,
      final BlockPos pos,
      final RandomSource ignoredRandom
   ) {
      BlockPos.MutableBlockPos checkPos = pos.mutable();

      do {
         checkPos.move(Direction.UP);
      } while (level.getFluidState(checkPos).is(FluidTags.LAVA));

      return level.getBlockState(checkPos).isAir();
   }

   @Override
   public void onSyncedDataUpdated(final EntityDataAccessor<?> accessor) {
      if (DATA_BOOST_TIME.equals(accessor) && this.level().isClientSide()) {
         this.steering.onSynced();
      }

      super.onSyncedDataUpdated(accessor);
   }

   @Override
   protected void defineSynchedData(final SynchedEntityData.Builder entityData) {
      super.defineSynchedData(entityData);
      entityData.define(DATA_BOOST_TIME, 0);
      entityData.define(DATA_SUFFOCATING, false);
   }

   @Override
   public boolean canUseSlot(final EquipmentSlot slot) {
      return slot != EquipmentSlot.SADDLE ? super.canUseSlot(slot) : this.isAlive() && !this.isBaby();
   }

   @Override
   protected boolean canDispenserEquipIntoSlot(final EquipmentSlot slot) {
      return slot == EquipmentSlot.SADDLE || super.canDispenserEquipIntoSlot(slot);
   }

   @Override
   protected Holder<SoundEvent> getEquipSound(final EquipmentSlot slot, final ItemStack stack, final Equippable equippable) {
      return slot == EquipmentSlot.SADDLE ? SoundEvents.STRIDER_SADDLE : super.getEquipSound(slot, stack, equippable);
   }

   @Override
   protected void registerGoals() {
      this.goalSelector.addGoal(1, new PanicGoal(this, 1.65));
      this.goalSelector.addGoal(2, new BreedGoal(this, 1.0));
      this.temptGoal = new TemptGoal(this, 1.4, i -> i.is(ItemTags.STRIDER_TEMPT_ITEMS), false);
      this.goalSelector.addGoal(3, this.temptGoal);
      this.goalSelector.addGoal(4, new Strider.StriderGoToLavaGoal(this, 1.0));
      this.goalSelector.addGoal(5, new FollowParentGoal(this, 1.0));
      this.goalSelector.addGoal(7, new RandomStrollGoal(this, 1.0, 60));
      this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
      this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
      this.goalSelector.addGoal(9, new LookAtPlayerGoal(this, Strider.class, 8.0F));
   }

   public void setSuffocating(final boolean flag) {
      this.entityData.set(DATA_SUFFOCATING, flag);
      AttributeInstance attribute = this.getAttribute(Attributes.MOVEMENT_SPEED);
      if (attribute != null) {
         if (flag) {
            attribute.addOrUpdateTransientModifier(SUFFOCATING_MODIFIER);
         } else {
            attribute.removeModifier(SUFFOCATING_MODIFIER_ID);
         }
      }
   }

   public boolean isSuffocating() {
      return this.entityData.get(DATA_SUFFOCATING);
   }

   @Override
   public boolean canStandOnFluid(final FluidState fluid) {
      return fluid.is(FluidTags.LAVA);
   }

   @Override
   public EntityDimensions getDefaultDimensions(final Pose pose) {
      return this.isBaby() ? BABY_DIMENSIONS : super.getDefaultDimensions(pose);
   }

   @Override
   protected Vec3 getPassengerAttachmentPoint(final Entity passenger, final EntityDimensions dimensions, final float scale) {
      if (!this.level().isClientSide()) {
         return super.getPassengerAttachmentPoint(passenger, dimensions, scale);
      }

      float animSpeed = Math.min(0.25F, this.walkAnimation.speed());
      float animPos = this.walkAnimation.position();
      float offset = 0.12F * Mth.cos(animPos * 1.5F) * 2.0F * animSpeed;
      return super.getPassengerAttachmentPoint(passenger, dimensions, scale).add(0.0, offset * scale, 0.0);
   }

   @Override
   public boolean checkSpawnObstruction(final LevelReader level) {
      return level.isUnobstructed(this);
   }

   @Override
   public @Nullable LivingEntity getControllingPassenger() {
      return this.isSaddled() && this.getFirstPassenger() instanceof Player player && player.isHolding(Items.WARPED_FUNGUS_ON_A_STICK)
         ? player
         : super.getControllingPassenger();
   }

   @Override
   public Vec3 getDismountLocationForPassenger(final LivingEntity passenger) {
      Vec3[] directions = new Vec3[]{
         getCollisionHorizontalEscapeVector(this.getBbWidth(), passenger.getBbWidth(), passenger.getYRot()),
         getCollisionHorizontalEscapeVector(this.getBbWidth(), passenger.getBbWidth(), passenger.getYRot() - 22.5F),
         getCollisionHorizontalEscapeVector(this.getBbWidth(), passenger.getBbWidth(), passenger.getYRot() + 22.5F),
         getCollisionHorizontalEscapeVector(this.getBbWidth(), passenger.getBbWidth(), passenger.getYRot() - 45.0F),
         getCollisionHorizontalEscapeVector(this.getBbWidth(), passenger.getBbWidth(), passenger.getYRot() + 45.0F)
      };
      Set<BlockPos> targetBlockPositions = Sets.newLinkedHashSet();
      double colliderTop = this.getBoundingBox().maxY;
      double colliderBottom = this.getBoundingBox().minY - 0.5;
      BlockPos.MutableBlockPos blockPos = new BlockPos.MutableBlockPos();

      for (Vec3 direction : directions) {
         blockPos.set(this.getX() + direction.x, colliderTop, this.getZ() + direction.z);

         for (double y = colliderTop; y > colliderBottom; y--) {
            targetBlockPositions.add(blockPos.immutable());
            blockPos.move(Direction.DOWN);
         }
      }

      for (BlockPos targetBlockPos : targetBlockPositions) {
         if (!this.level().getFluidState(targetBlockPos).is(FluidTags.LAVA)) {
            double blockFloorHeight = this.level().getBlockFloorHeight(targetBlockPos);
            if (DismountHelper.isBlockFloorValid(blockFloorHeight)) {
               Vec3 location = Vec3.upFromBottomCenterOf(targetBlockPos, blockFloorHeight);
               UnmodifiableIterator var14 = passenger.getDismountPoses().iterator();

               while (var14.hasNext()) {
                  Pose dismountPose = (Pose)var14.next();
                  AABB poseCollisionBox = passenger.getLocalBoundsForPose(dismountPose);
                  if (DismountHelper.canDismountTo(this.level(), passenger, poseCollisionBox.move(location))) {
                     passenger.setPose(dismountPose);
                     return location;
                  }
               }
            }
         }
      }

      return new Vec3(this.getX(), this.getBoundingBox().maxY, this.getZ());
   }

   @Override
   protected void tickRidden(final Player controller, final Vec3 riddenInput) {
      this.setRot(controller.getYRot(), controller.getXRot() * 0.5F);
      this.yRotO = this.yBodyRot = this.yHeadRot = this.getYRot();
      this.steering.tickBoost();
      super.tickRidden(controller, riddenInput);
   }

   @Override
   protected Vec3 getRiddenInput(final Player controller, final Vec3 selfInput) {
      return new Vec3(0.0, 0.0, 1.0);
   }

   @Override
   protected float getRiddenSpeed(final Player controller) {
      return (float)(this.getAttributeValue(Attributes.MOVEMENT_SPEED) * (this.isSuffocating() ? 0.35F : 0.55F) * this.steering.boostFactor());
   }

   @Override
   protected float nextStep() {
      return this.moveDist + 0.6F;
   }

   @Override
   protected void playStepSound(final BlockPos pos, final BlockState blockState) {
      this.playSound(this.isInLava() ? SoundEvents.STRIDER_STEP_LAVA : SoundEvents.STRIDER_STEP, 1.0F, 1.0F);
   }

   @Override
   public boolean boost() {
      return this.steering.boost(this.getRandom());
   }

   @Override
   protected void checkFallDamage(final double ya, final boolean onGround, final BlockState onState, final BlockPos pos) {
      if (this.isInLava()) {
         this.resetFallDistance();
      } else {
         super.checkFallDamage(ya, onGround, onState, pos);
      }
   }

   @Override
   public void tick() {
      if (this.isBeingTempted() && this.random.nextInt(140) == 0) {
         this.makeSound(SoundEvents.STRIDER_HAPPY);
      } else if (this.isPanicking() && this.random.nextInt(60) == 0) {
         this.makeSound(SoundEvents.STRIDER_RETREAT);
      }

      if (!this.isNoAi()) {
         BlockState stateInside = this.level().getBlockState(this.blockPosition());
         BlockState stateOn = this.getBlockStateOnLegacy();
         boolean inWarmBlocks = stateInside.is(BlockTags.STRIDER_WARM_BLOCKS)
            || stateOn.is(BlockTags.STRIDER_WARM_BLOCKS)
            || this.getFluidHeight(FluidTags.LAVA) > 0.0;
         boolean onWarmStrider = this.getVehicle() instanceof Strider strider && !strider.isSuffocating();
         this.setSuffocating(!inWarmBlocks && !onWarmStrider);
      }

      super.tick();
      this.floatStrider();
   }

   private boolean isBeingTempted() {
      return this.temptGoal != null && this.temptGoal.isRunning();
   }

   @Override
   protected boolean shouldPassengersInheritMalus() {
      return true;
   }

   private void floatStrider() {
      if (this.isInLava()) {
         CollisionContext context = CollisionContext.of(this);
         if (context.isAbove(this.getLiquidCollisionShape(), this.blockPosition(), true)
            && !this.level().getFluidState(this.blockPosition().above()).is(FluidTags.LAVA)) {
            this.setOnGround(true);
         } else {
            this.setDeltaMovement(this.getDeltaMovement().scale(0.5).add(0.0, 0.05, 0.0));
         }
      }
   }

   @Override
   public VoxelShape getLiquidCollisionShape() {
      return Block.column(16.0, 0.0, 8.0);
   }

   public static AttributeSupplier.Builder createAttributes() {
      return Animal.createAnimalAttributes().add(Attributes.MOVEMENT_SPEED, 0.175F);
   }

   @Override
   protected @Nullable SoundEvent getAmbientSound() {
      return !this.isPanicking() && !this.isBeingTempted() ? SoundEvents.STRIDER_AMBIENT : null;
   }

   @Override
   protected SoundEvent getHurtSound(final DamageSource source) {
      return SoundEvents.STRIDER_HURT;
   }

   @Override
   protected SoundEvent getDeathSound() {
      return SoundEvents.STRIDER_DEATH;
   }

   @Override
   protected boolean canAddPassenger(final Entity passenger) {
      return !this.isVehicle() && !this.isEyeInFluid(FluidTags.LAVA);
   }

   @Override
   public boolean isSensitiveToWater() {
      return true;
   }

   @Override
   public boolean isOnFire() {
      return false;
   }

   @Override
   protected PathNavigation createNavigation(final Level level) {
      return new Strider.StriderPathNavigation(this, level);
   }

   @Override
   public float getWalkTargetValue(final BlockPos pos, final LevelReader level) {
      if (level.getBlockState(pos).getFluidState().is(FluidTags.LAVA)) {
         return 10.0F;
      } else {
         return this.isInLava() ? Float.NEGATIVE_INFINITY : 0.0F;
      }
   }

   public @Nullable Strider getBreedOffspring(final ServerLevel level, final AgeableMob partner) {
      return EntityTypes.STRIDER.create(level, EntitySpawnReason.BREEDING);
   }

   @Override
   public boolean isFood(final ItemStack itemStack) {
      return itemStack.is(ItemTags.STRIDER_FOOD);
   }

   @Override
   public InteractionResult mobInteract(final Player player, final InteractionHand hand) {
      boolean hasFood = this.isFood(player.getItemInHand(hand));
      if (!hasFood && this.isSaddled() && !this.isVehicle() && !player.isSecondaryUseActive()) {
         if (!this.level().isClientSide()) {
            player.startRiding(this);
         }

         return InteractionResult.SUCCESS;
      } else {
         InteractionResult interactionResult = super.mobInteract(player, hand);
         if (!interactionResult.consumesAction()) {
            ItemStack itemStack = player.getItemInHand(hand);
            return this.isEquippableInSlot(itemStack, EquipmentSlot.SADDLE) ? itemStack.interactLivingEntity(player, this, hand) : InteractionResult.PASS;
         }

         if (hasFood && !this.isSilent()) {
            this.level()
               .playSound(
                  null,
                  this.getX(),
                  this.getY(),
                  this.getZ(),
                  SoundEvents.STRIDER_EAT,
                  this.getSoundSource(),
                  1.0F,
                  1.0F + (this.random.nextFloat() - this.random.nextFloat()) * 0.2F
               );
         }

         return interactionResult;
      }
   }

   @Override
   public Vec3 getLeashOffset() {
      return new Vec3(0.0, 0.6F * this.getEyeHeight(), this.getBbWidth() * 0.4F);
   }

   @Override
   public @Nullable SpawnGroupData finalizeSpawn(
      final ServerLevelAccessor level, final DifficultyInstance difficulty, final EntitySpawnReason spawnReason, @Nullable SpawnGroupData groupData
   ) {
      if (this.isBaby()) {
         return super.finalizeSpawn(level, difficulty, spawnReason, groupData);
      }

      RandomSource random = level.getRandom();
      if (random.nextInt(30) == 0) {
         Mob jockey = EntityTypes.ZOMBIFIED_PIGLIN.create(level.getLevel(), EntitySpawnReason.JOCKEY);
         if (jockey != null) {
            groupData = this.spawnJockey(level, difficulty, jockey, new Zombie.ZombieGroupData(Zombie.getSpawnAsBabyOdds(random), false));
            jockey.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.WARPED_FUNGUS_ON_A_STICK));
            this.setItemSlot(EquipmentSlot.SADDLE, new ItemStack(Items.SADDLE));
            this.setGuaranteedDrop(EquipmentSlot.SADDLE);
         }
      } else if (random.nextInt(10) == 0) {
         AgeableMob jockey = EntityTypes.STRIDER.create(level.getLevel(), EntitySpawnReason.JOCKEY);
         if (jockey != null) {
            jockey.setAge(-24000);
            groupData = this.spawnJockey(level, difficulty, jockey, null);
         }
      } else {
         groupData = new AgeableMob.AgeableMobGroupData(0.5F);
      }

      return super.finalizeSpawn(level, difficulty, spawnReason, groupData);
   }

   private SpawnGroupData spawnJockey(
      final ServerLevelAccessor level, final DifficultyInstance difficulty, final Mob jockey, final @Nullable SpawnGroupData jockeyGroupData
   ) {
      jockey.snapTo(this.getX(), this.getY(), this.getZ(), this.getYRot(), 0.0F);
      jockey.finalizeSpawn(level, difficulty, EntitySpawnReason.JOCKEY, jockeyGroupData);
      jockey.startRiding(this, true, false);
      return new AgeableMob.AgeableMobGroupData(0.0F);
   }

   private static class StriderGoToLavaGoal extends MoveToBlockGoal {
      private final Strider strider;

      private StriderGoToLavaGoal(final Strider strider, final double speedModifier) {
         super(strider, speedModifier, 8, 2);
         this.strider = strider;
      }

      @Override
      public BlockPos getMoveToTarget() {
         return this.blockPos;
      }

      @Override
      public boolean canContinueToUse() {
         return !this.strider.isInLava() && this.isValidTarget(this.strider.level(), this.blockPos);
      }

      @Override
      public boolean canUse() {
         return !this.strider.isInLava() && super.canUse();
      }

      @Override
      public boolean shouldRecalculatePath() {
         return this.tryTicks % 20 == 0;
      }

      @Override
      protected boolean isValidTarget(final LevelReader level, final BlockPos pos) {
         return level.getBlockState(pos).is(Blocks.LAVA) && level.getBlockState(pos.above()).isPathfindable(PathComputationType.LAND);
      }
   }

   private static class StriderPathNavigation extends GroundPathNavigation {
      public StriderPathNavigation(final Strider mob, final Level level) {
         super(mob, level);
      }

      @Override
      protected PathFinder createPathFinder(final int maxVisitedNodes) {
         this.nodeEvaluator = new WalkNodeEvaluator();
         return new PathFinder(this.nodeEvaluator, maxVisitedNodes);
      }

      @Override
      public boolean isStableDestination(final BlockPos pos) {
         return this.level.getBlockState(pos).is(Blocks.LAVA) || super.isStableDestination(pos);
      }
   }
}
