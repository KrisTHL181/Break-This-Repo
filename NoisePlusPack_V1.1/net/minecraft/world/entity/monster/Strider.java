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
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.level.pathfinder.PathFinder;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
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
   private final ItemBasedSteering steering = new ItemBasedSteering(this.entityData, DATA_BOOST_TIME);
   private @Nullable TemptGoal temptGoal;

   public Strider(EntityType<? extends Strider> p_33862_, Level p_33863_) {
      super(p_33862_, p_33863_);
      this.blocksBuilding = true;
      this.setPathfindingMalus(PathType.WATER, -1.0F);
      this.setPathfindingMalus(PathType.LAVA, 0.0F);
      this.setPathfindingMalus(PathType.DANGER_FIRE, 0.0F);
      this.setPathfindingMalus(PathType.DAMAGE_FIRE, 0.0F);
   }

   public static boolean checkStriderSpawnRules(
      EntityType<Strider> p_219129_, LevelAccessor p_219130_, EntitySpawnReason p_368751_, BlockPos p_219132_, RandomSource p_219133_
   ) {
      BlockPos.MutableBlockPos blockpos$mutableblockpos = p_219132_.mutable();

      do {
         blockpos$mutableblockpos.move(Direction.UP);
      } while (p_219130_.getFluidState(blockpos$mutableblockpos).is(FluidTags.LAVA));

      return p_219130_.getBlockState(blockpos$mutableblockpos).isAir();
   }

   @Override
   public void onSyncedDataUpdated(EntityDataAccessor<?> p_33900_) {
      if (DATA_BOOST_TIME.equals(p_33900_) && this.level().isClientSide()) {
         this.steering.onSynced();
      }

      super.onSyncedDataUpdated(p_33900_);
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder p_331129_) {
      super.defineSynchedData(p_331129_);
      p_331129_.define(DATA_BOOST_TIME, 0);
      p_331129_.define(DATA_SUFFOCATING, false);
   }

   @Override
   public boolean canUseSlot(EquipmentSlot p_396730_) {
      return p_396730_ != EquipmentSlot.SADDLE ? super.canUseSlot(p_396730_) : this.isAlive() && !this.isBaby();
   }

   @Override
   protected boolean canDispenserEquipIntoSlot(EquipmentSlot p_397501_) {
      return p_397501_ == EquipmentSlot.SADDLE || super.canDispenserEquipIntoSlot(p_397501_);
   }

   @Override
   protected Holder<SoundEvent> getEquipSound(EquipmentSlot p_392918_, ItemStack p_397621_, Equippable p_396978_) {
      return p_392918_ == EquipmentSlot.SADDLE ? SoundEvents.STRIDER_SADDLE : super.getEquipSound(p_392918_, p_397621_, p_396978_);
   }

   @Override
   protected void registerGoals() {
      this.goalSelector.addGoal(1, new PanicGoal(this, 1.65));
      this.goalSelector.addGoal(2, new BreedGoal(this, 1.0));
      this.temptGoal = new TemptGoal(this, 1.4, p_328939_ -> p_328939_.is(ItemTags.STRIDER_TEMPT_ITEMS), false);
      this.goalSelector.addGoal(3, this.temptGoal);
      this.goalSelector.addGoal(4, new Strider.StriderGoToLavaGoal(this, 1.0));
      this.goalSelector.addGoal(5, new FollowParentGoal(this, 1.0));
      this.goalSelector.addGoal(7, new RandomStrollGoal(this, 1.0, 60));
      this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
      this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
      this.goalSelector.addGoal(9, new LookAtPlayerGoal(this, Strider.class, 8.0F));
   }

   public void setSuffocating(boolean p_33952_) {
      this.entityData.set(DATA_SUFFOCATING, p_33952_);
      AttributeInstance attributeinstance = this.getAttribute(Attributes.MOVEMENT_SPEED);
      if (attributeinstance != null) {
         if (p_33952_) {
            attributeinstance.addOrUpdateTransientModifier(SUFFOCATING_MODIFIER);
         } else {
            attributeinstance.removeModifier(SUFFOCATING_MODIFIER_ID);
         }
      }
   }

   public boolean isSuffocating() {
      return this.entityData.get(DATA_SUFFOCATING);
   }

   @Override
   public boolean canStandOnFluid(FluidState p_204067_) {
      return p_204067_.is(FluidTags.LAVA);
   }

   @Override
   protected Vec3 getPassengerAttachmentPoint(Entity p_298003_, EntityDimensions p_300798_, float p_299514_) {
      if (!this.level().isClientSide()) {
         return super.getPassengerAttachmentPoint(p_298003_, p_300798_, p_299514_);
      }

      float f = Math.min(0.25F, this.walkAnimation.speed());
      float f1 = this.walkAnimation.position();
      float f2 = 0.12F * Mth.cos(f1 * 1.5F) * 2.0F * f;
      return super.getPassengerAttachmentPoint(p_298003_, p_300798_, p_299514_).add(0.0, f2 * p_299514_, 0.0);
   }

   @Override
   public boolean checkSpawnObstruction(LevelReader p_33880_) {
      return p_33880_.isUnobstructed(this);
   }

   @Override
   public @Nullable LivingEntity getControllingPassenger() {
      return this.isSaddled() && this.getFirstPassenger() instanceof Player player && player.isHolding(Items.WARPED_FUNGUS_ON_A_STICK)
         ? player
         : super.getControllingPassenger();
   }

   @Override
   public Vec3 getDismountLocationForPassenger(LivingEntity p_33908_) {
      Vec3[] avec3 = new Vec3[]{
         getCollisionHorizontalEscapeVector(this.getBbWidth(), p_33908_.getBbWidth(), p_33908_.getYRot()),
         getCollisionHorizontalEscapeVector(this.getBbWidth(), p_33908_.getBbWidth(), p_33908_.getYRot() - 22.5F),
         getCollisionHorizontalEscapeVector(this.getBbWidth(), p_33908_.getBbWidth(), p_33908_.getYRot() + 22.5F),
         getCollisionHorizontalEscapeVector(this.getBbWidth(), p_33908_.getBbWidth(), p_33908_.getYRot() - 45.0F),
         getCollisionHorizontalEscapeVector(this.getBbWidth(), p_33908_.getBbWidth(), p_33908_.getYRot() + 45.0F)
      };
      Set<BlockPos> set = Sets.newLinkedHashSet();
      double d0 = this.getBoundingBox().maxY;
      double d1 = this.getBoundingBox().minY - 0.5;
      BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();

      for (Vec3 vec3 : avec3) {
         blockpos$mutableblockpos.set(this.getX() + vec3.x, d0, this.getZ() + vec3.z);

         for (double d2 = d0; d2 > d1; d2--) {
            set.add(blockpos$mutableblockpos.immutable());
            blockpos$mutableblockpos.move(Direction.DOWN);
         }
      }

      for (BlockPos blockpos : set) {
         if (!this.level().getFluidState(blockpos).is(FluidTags.LAVA)) {
            double d3 = this.level().getBlockFloorHeight(blockpos);
            if (DismountHelper.isBlockFloorValid(d3)) {
               Vec3 vec31 = Vec3.upFromBottomCenterOf(blockpos, d3);
               UnmodifiableIterator var14 = p_33908_.getDismountPoses().iterator();

               while (var14.hasNext()) {
                  Pose pose = (Pose)var14.next();
                  AABB aabb = p_33908_.getLocalBoundsForPose(pose);
                  if (DismountHelper.canDismountTo(this.level(), p_33908_, aabb.move(vec31))) {
                     p_33908_.setPose(pose);
                     return vec31;
                  }
               }
            }
         }
      }

      return new Vec3(this.getX(), this.getBoundingBox().maxY, this.getZ());
   }

   @Override
   protected void tickRidden(Player p_278331_, Vec3 p_278234_) {
      this.setRot(p_278331_.getYRot(), p_278331_.getXRot() * 0.5F);
      this.yRotO = this.yBodyRot = this.yHeadRot = this.getYRot();
      this.steering.tickBoost();
      super.tickRidden(p_278331_, p_278234_);
   }

   @Override
   protected Vec3 getRiddenInput(Player p_278251_, Vec3 p_275578_) {
      return new Vec3(0.0, 0.0, 1.0);
   }

   @Override
   protected float getRiddenSpeed(Player p_278317_) {
      return (float)(this.getAttributeValue(Attributes.MOVEMENT_SPEED) * (this.isSuffocating() ? 0.35F : 0.55F) * this.steering.boostFactor());
   }

   @Override
   protected float nextStep() {
      return this.moveDist + 0.6F;
   }

   @Override
   protected void playStepSound(BlockPos p_33915_, BlockState p_33916_) {
      this.playSound(this.isInLava() ? SoundEvents.STRIDER_STEP_LAVA : SoundEvents.STRIDER_STEP, 1.0F, 1.0F);
   }

   @Override
   public boolean boost() {
      return this.steering.boost(this.getRandom());
   }

   @Override
   protected void checkFallDamage(double p_33870_, boolean p_33871_, BlockState p_33872_, BlockPos p_33873_) {
      if (this.isInLava()) {
         this.resetFallDistance();
      } else {
         super.checkFallDamage(p_33870_, p_33871_, p_33872_, p_33873_);
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
         BlockState blockstate = this.level().getBlockState(this.blockPosition());
         BlockState blockstate1 = this.getBlockStateOnLegacy();
         boolean flag = blockstate.is(BlockTags.STRIDER_WARM_BLOCKS)
            || blockstate1.is(BlockTags.STRIDER_WARM_BLOCKS)
            || this.getFluidHeight(FluidTags.LAVA) > 0.0;
         boolean flag1 = this.getVehicle() instanceof Strider strider && strider.isSuffocating();
         this.setSuffocating(!flag || flag1);
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
         CollisionContext collisioncontext = CollisionContext.of(this);
         if (collisioncontext.isAbove(LiquidBlock.SHAPE_STABLE, this.blockPosition(), true)
            && !this.level().getFluidState(this.blockPosition().above()).is(FluidTags.LAVA)) {
            this.setOnGround(true);
         } else {
            this.setDeltaMovement(this.getDeltaMovement().scale(0.5).add(0.0, 0.05, 0.0));
         }
      }
   }

   public static AttributeSupplier.Builder createAttributes() {
      return Animal.createAnimalAttributes().add(Attributes.MOVEMENT_SPEED, 0.175F);
   }

   @Override
   protected @Nullable SoundEvent getAmbientSound() {
      return !this.isPanicking() && !this.isBeingTempted() ? SoundEvents.STRIDER_AMBIENT : null;
   }

   @Override
   protected SoundEvent getHurtSound(DamageSource p_33934_) {
      return SoundEvents.STRIDER_HURT;
   }

   @Override
   protected SoundEvent getDeathSound() {
      return SoundEvents.STRIDER_DEATH;
   }

   @Override
   protected boolean canAddPassenger(Entity p_33950_) {
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
   protected PathNavigation createNavigation(Level p_33913_) {
      return new Strider.StriderPathNavigation(this, p_33913_);
   }

   @Override
   public float getWalkTargetValue(BlockPos p_33895_, LevelReader p_33896_) {
      if (p_33896_.getBlockState(p_33895_).getFluidState().is(FluidTags.LAVA)) {
         return 10.0F;
      } else {
         return this.isInLava() ? Float.NEGATIVE_INFINITY : 0.0F;
      }
   }

   public @Nullable Strider getBreedOffspring(ServerLevel p_149861_, AgeableMob p_149862_) {
      return EntityType.STRIDER.create(p_149861_, EntitySpawnReason.BREEDING);
   }

   @Override
   public boolean isFood(ItemStack p_33946_) {
      return p_33946_.is(ItemTags.STRIDER_FOOD);
   }

   @Override
   public InteractionResult mobInteract(Player p_33910_, InteractionHand p_33911_) {
      boolean flag = this.isFood(p_33910_.getItemInHand(p_33911_));
      if (!flag && this.isSaddled() && !this.isVehicle() && !p_33910_.isSecondaryUseActive()) {
         if (!this.level().isClientSide()) {
            p_33910_.startRiding(this);
         }

         return InteractionResult.SUCCESS;
      } else {
         InteractionResult interactionresult = super.mobInteract(p_33910_, p_33911_);
         if (!interactionresult.consumesAction()) {
            ItemStack itemstack = p_33910_.getItemInHand(p_33911_);
            return this.isEquippableInSlot(itemstack, EquipmentSlot.SADDLE) ? itemstack.interactLivingEntity(p_33910_, this, p_33911_) : InteractionResult.PASS;
         }

         if (flag && !this.isSilent()) {
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

         return interactionresult;
      }
   }

   @Override
   public Vec3 getLeashOffset() {
      return new Vec3(0.0, 0.6F * this.getEyeHeight(), this.getBbWidth() * 0.4F);
   }

   @Override
   public @Nullable SpawnGroupData finalizeSpawn(
      ServerLevelAccessor p_33887_, DifficultyInstance p_33888_, EntitySpawnReason p_370149_, @Nullable SpawnGroupData p_33890_
   ) {
      if (this.isBaby()) {
         return super.finalizeSpawn(p_33887_, p_33888_, p_370149_, p_33890_);
      }

      RandomSource randomsource = p_33887_.getRandom();
      if (randomsource.nextInt(30) == 0) {
         Mob mob = EntityType.ZOMBIFIED_PIGLIN.create(p_33887_.getLevel(), EntitySpawnReason.JOCKEY);
         if (mob != null) {
            p_33890_ = this.spawnJockey(p_33887_, p_33888_, mob, new Zombie.ZombieGroupData(Zombie.getSpawnAsBabyOdds(randomsource), false));
            mob.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.WARPED_FUNGUS_ON_A_STICK));
            this.setItemSlot(EquipmentSlot.SADDLE, new ItemStack(Items.SADDLE));
            this.setGuaranteedDrop(EquipmentSlot.SADDLE);
         }
      } else if (randomsource.nextInt(10) == 0) {
         AgeableMob ageablemob = EntityType.STRIDER.create(p_33887_.getLevel(), EntitySpawnReason.JOCKEY);
         if (ageablemob != null) {
            ageablemob.setAge(-24000);
            p_33890_ = this.spawnJockey(p_33887_, p_33888_, ageablemob, null);
         }
      } else {
         p_33890_ = new AgeableMob.AgeableMobGroupData(0.5F);
      }

      return super.finalizeSpawn(p_33887_, p_33888_, p_370149_, p_33890_);
   }

   private SpawnGroupData spawnJockey(ServerLevelAccessor p_33882_, DifficultyInstance p_33883_, Mob p_33884_, @Nullable SpawnGroupData p_33885_) {
      p_33884_.snapTo(this.getX(), this.getY(), this.getZ(), this.getYRot(), 0.0F);
      p_33884_.finalizeSpawn(p_33882_, p_33883_, EntitySpawnReason.JOCKEY, p_33885_);
      p_33884_.startRiding(this, true, false);
      return new AgeableMob.AgeableMobGroupData(0.0F);
   }

   static class StriderGoToLavaGoal extends MoveToBlockGoal {
      private final Strider strider;

      StriderGoToLavaGoal(Strider p_33955_, double p_33956_) {
         super(p_33955_, p_33956_, 8, 2);
         this.strider = p_33955_;
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
      protected boolean isValidTarget(LevelReader p_33963_, BlockPos p_33964_) {
         return p_33963_.getBlockState(p_33964_).is(Blocks.LAVA) && p_33963_.getBlockState(p_33964_.above()).isPathfindable(PathComputationType.LAND);
      }
   }

   static class StriderPathNavigation extends GroundPathNavigation {
      StriderPathNavigation(Strider p_33969_, Level p_33970_) {
         super(p_33969_, p_33970_);
      }

      @Override
      protected PathFinder createPathFinder(int p_33972_) {
         this.nodeEvaluator = new WalkNodeEvaluator();
         return new PathFinder(this.nodeEvaluator, p_33972_);
      }

      @Override
      protected boolean hasValidPathType(PathType p_330836_) {
         return p_330836_ != PathType.LAVA && p_330836_ != PathType.DAMAGE_FIRE && p_330836_ != PathType.DANGER_FIRE ? super.hasValidPathType(p_330836_) : true;
      }

      @Override
      public boolean isStableDestination(BlockPos p_33976_) {
         return this.level.getBlockState(p_33976_).is(Blocks.LAVA) || super.isStableDestination(p_33976_);
      }
   }
}
