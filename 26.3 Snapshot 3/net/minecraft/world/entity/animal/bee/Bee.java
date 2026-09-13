package net.minecraft.world.entity.animal.bee;

import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.PoiTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.TimeUtil;
import net.minecraft.util.VisibleForDebug;
import net.minecraft.util.debug.DebugBeeInfo;
import net.minecraft.util.debug.DebugSubscriptions;
import net.minecraft.util.debug.DebugValueSource;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.control.LookControl;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowParentGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.ResetUniversalAngerTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.util.AirAndWaterRandomPos;
import net.minecraft.world.entity.ai.util.AirRandomPos;
import net.minecraft.world.entity.ai.util.HoverRandomPos;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.FlowerBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTypes;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class Bee extends Animal implements NeutralMob {
   public static final float FLAP_DEGREES_PER_TICK = 120.32113F;
   public static final int TICKS_PER_FLAP = Mth.ceil(1.4959966F);
   private static final EntityDataAccessor<Byte> DATA_FLAGS_ID = SynchedEntityData.defineId(Bee.class, EntityDataSerializers.BYTE);
   private static final EntityDataAccessor<Long> DATA_ANGER_END_TIME = SynchedEntityData.defineId(Bee.class, EntityDataSerializers.LONG);
   private static final int FLAG_ROLL = 2;
   private static final int FLAG_HAS_STUNG = 4;
   private static final int FLAG_HAS_NECTAR = 8;
   private static final int STING_DEATH_COUNTDOWN = 1200;
   private static final int TICKS_BEFORE_GOING_TO_KNOWN_FLOWER = 600;
   private static final int TICKS_WITHOUT_NECTAR_BEFORE_GOING_HOME = 3600;
   private static final int MIN_ATTACK_DIST = 4;
   private static final int MAX_CROPS_GROWABLE = 10;
   private static final int POISON_SECONDS_NORMAL = 10;
   private static final int POISON_SECONDS_HARD = 18;
   private static final int TOO_FAR_DISTANCE = 48;
   private static final int HIVE_CLOSE_ENOUGH_DISTANCE = 2;
   private static final int RESTRICTED_WANDER_DISTANCE_REDUCTION = 24;
   private static final int DEFAULT_WANDER_DISTANCE_REDUCTION = 16;
   private static final int PATHFIND_TO_HIVE_WHEN_CLOSER_THAN = 16;
   private static final int HIVE_SEARCH_DISTANCE = 20;
   public static final String TAG_CROPS_GROWN_SINCE_POLLINATION = "CropsGrownSincePollination";
   public static final String TAG_CANNOT_ENTER_HIVE_TICKS = "CannotEnterHiveTicks";
   public static final String TAG_TICKS_SINCE_POLLINATION = "TicksSincePollination";
   public static final String TAG_HAS_STUNG = "HasStung";
   public static final String TAG_HAS_NECTAR = "HasNectar";
   public static final String TAG_FLOWER_POS = "flower_pos";
   public static final String TAG_HIVE_POS = "hive_pos";
   public static final boolean DEFAULT_HAS_NECTAR = false;
   private static final boolean DEFAULT_HAS_STUNG = false;
   private static final int DEFAULT_TICKS_SINCE_POLLINATION = 0;
   private static final int DEFAULT_CANNOT_ENTER_HIVE_TICKS = 0;
   private static final int DEFAULT_CROPS_GROWN_SINCE_POLLINATION = 0;
   private static final UniformInt PERSISTENT_ANGER_TIME = TimeUtil.rangeOfSeconds(20, 39);
   private @Nullable EntityReference<LivingEntity> persistentAngerTarget;
   private float rollAmount;
   private float rollAmountO;
   private int timeSinceSting;
   private int ticksWithoutNectarSinceExitingHive = 0;
   private int stayOutOfHiveCountdown = 0;
   private int numCropsGrownSincePollination = 0;
   private static final int COOLDOWN_BEFORE_LOCATING_NEW_HIVE = 200;
   private int remainingCooldownBeforeLocatingNewHive;
   private static final int COOLDOWN_BEFORE_LOCATING_NEW_FLOWER = 200;
   private static final int MIN_FIND_FLOWER_RETRY_COOLDOWN = 20;
   private static final int MAX_FIND_FLOWER_RETRY_COOLDOWN = 60;
   private int remainingCooldownBeforeLocatingNewFlower = Mth.nextInt(this.random, 20, 60);
   private @Nullable BlockPos savedFlowerPos;
   private @Nullable BlockPos hivePos;
   private Bee.BeePollinateGoal beePollinateGoal;
   private Bee.BeeGoToHiveGoal goToHiveGoal;
   private Bee.BeeGoToKnownFlowerGoal goToKnownFlowerGoal;
   private int underWaterTicks;

   public Bee(final EntityType<? extends Bee> type, final Level level) {
      super(type, level);
      this.moveControl = new FlyingMoveControl<>(this, 20, true);
      this.lookControl = new Bee.BeeLookControl(this);
      this.setPathfindingMalus(PathType.FIRE, -1.0F);
      this.setPathfindingMalus(PathType.WATER, -1.0F);
      this.setPathfindingMalus(PathType.WATER_BORDER, 16.0F);
      this.setPathfindingMalus(PathType.COCOA, -1.0F);
      this.setPathfindingMalus(PathType.FENCE, -1.0F);
   }

   @Override
   protected void defineSynchedData(final SynchedEntityData.Builder entityData) {
      super.defineSynchedData(entityData);
      entityData.define(DATA_FLAGS_ID, (byte)0);
      entityData.define(DATA_ANGER_END_TIME, -1L);
   }

   @Override
   public float getWalkTargetValue(final BlockPos pos, final LevelReader level) {
      return level.getBlockState(pos).isAir() ? 10.0F : 0.0F;
   }

   @Override
   protected void registerGoals() {
      this.goalSelector.addGoal(0, new Bee.BeeAttackGoal(this, 1.4F, true));
      this.goalSelector.addGoal(1, new Bee.BeeEnterHiveGoal());
      this.goalSelector.addGoal(2, new BreedGoal(this, 1.0));
      this.goalSelector.addGoal(3, new TemptGoal(this, 1.25, i -> i.is(ItemTags.BEE_FOOD), false));
      this.goalSelector.addGoal(3, new Bee.ValidateHiveGoal());
      this.goalSelector.addGoal(3, new Bee.ValidateFlowerGoal());
      this.beePollinateGoal = new Bee.BeePollinateGoal();
      this.goalSelector.addGoal(4, this.beePollinateGoal);
      this.goalSelector.addGoal(5, new FollowParentGoal(this, 1.25));
      this.goalSelector.addGoal(5, new Bee.BeeLocateHiveGoal());
      this.goToHiveGoal = new Bee.BeeGoToHiveGoal();
      this.goalSelector.addGoal(5, this.goToHiveGoal);
      this.goToKnownFlowerGoal = new Bee.BeeGoToKnownFlowerGoal();
      this.goalSelector.addGoal(6, this.goToKnownFlowerGoal);
      this.goalSelector.addGoal(7, new Bee.BeeGrowCropGoal());
      this.goalSelector.addGoal(8, new Bee.BeeWanderGoal());
      this.goalSelector.addGoal(9, new FloatGoal(this));
      this.targetSelector.addGoal(1, new Bee.BeeHurtByOtherGoal(this).setAlertOthers());
      this.targetSelector.addGoal(2, new Bee.BeeBecomeAngryTargetGoal(this));
      this.targetSelector.addGoal(3, new ResetUniversalAngerTargetGoal<>(this, true));
   }

   @Override
   protected void addAdditionalSaveData(final ValueOutput output) {
      super.addAdditionalSaveData(output);
      output.storeNullable("hive_pos", BlockPos.CODEC, this.hivePos);
      output.storeNullable("flower_pos", BlockPos.CODEC, this.savedFlowerPos);
      output.putBoolean("HasNectar", this.hasNectar());
      output.putBoolean("HasStung", this.hasStung());
      output.putInt("TicksSincePollination", this.ticksWithoutNectarSinceExitingHive);
      output.putInt("CannotEnterHiveTicks", this.stayOutOfHiveCountdown);
      output.putInt("CropsGrownSincePollination", this.numCropsGrownSincePollination);
      this.addPersistentAngerSaveData(output);
   }

   @Override
   protected void readAdditionalSaveData(final ValueInput input) {
      super.readAdditionalSaveData(input);
      this.setHasNectar(input.getBooleanOr("HasNectar", false));
      this.setHasStung(input.getBooleanOr("HasStung", false));
      this.ticksWithoutNectarSinceExitingHive = input.getIntOr("TicksSincePollination", 0);
      this.stayOutOfHiveCountdown = input.getIntOr("CannotEnterHiveTicks", 0);
      this.numCropsGrownSincePollination = input.getIntOr("CropsGrownSincePollination", 0);
      this.hivePos = input.<BlockPos>read("hive_pos", BlockPos.CODEC).orElse(null);
      this.savedFlowerPos = input.<BlockPos>read("flower_pos", BlockPos.CODEC).orElse(null);
      this.readPersistentAngerSaveData(this.level(), input);
   }

   @Override
   public boolean doHurtTarget(final ServerLevel level, final Entity target) {
      DamageSource damageSource = this.damageSources().sting(this);
      boolean wasHurt = target.hurtServer(level, damageSource, (int)this.getAttributeValue(Attributes.ATTACK_DAMAGE));
      if (wasHurt) {
         EnchantmentHelper.doPostAttackEffects(level, target, damageSource);
         if (target instanceof LivingEntity livingTarget) {
            livingTarget.setStingerCount(livingTarget.getStingerCount() + 1);
            int poisonTime = 0;
            if (this.level().getDifficulty() == Difficulty.NORMAL) {
               poisonTime = 10;
            } else if (this.level().getDifficulty() == Difficulty.HARD) {
               poisonTime = 18;
            }

            if (poisonTime > 0) {
               livingTarget.addEffect(new MobEffectInstance(MobEffects.POISON, poisonTime * 20, 0), this);
            }
         }

         this.setHasStung(true);
         this.stopBeingAngry();
         this.playSound(SoundEvents.BEE_STING, 1.0F, 1.0F);
      }

      return wasHurt;
   }

   @Override
   public void tick() {
      super.tick();
      if (this.hasNectar() && this.getCropsGrownSincePollination() < 10 && this.random.nextFloat() < 0.05F) {
         for (int i = 0; i < this.random.nextInt(2) + 1; i++) {
            this.spawnFluidParticle(
               this.level(), this.getX() - 0.3F, this.getX() + 0.3F, this.getZ() - 0.3F, this.getZ() + 0.3F, this.getY(0.5), ParticleTypes.FALLING_NECTAR
            );
         }
      }

      this.updateRollAmount();
   }

   private void spawnFluidParticle(
      final Level level, final double x1, final double x2, final double z1, final double z2, final double y, final ParticleOptions dripParticle
   ) {
      level.addParticle(dripParticle, Mth.lerp(level.getRandom().nextDouble(), x1, x2), y, Mth.lerp(level.getRandom().nextDouble(), z1, z2), 0.0, 0.0, 0.0);
   }

   private void pathfindRandomlyTowards(final BlockPos targetPos) {
      Vec3 targetVec = Vec3.atBottomCenterOf(targetPos);
      int yAdjust = 0;
      BlockPos beePos = this.blockPosition();
      int yDelta = (int)targetVec.y - beePos.getY();
      if (yDelta > 2) {
         yAdjust = 4;
      } else if (yDelta < -2) {
         yAdjust = -4;
      }

      int xzDist = 6;
      int yDist = 8;
      int dist = beePos.distManhattan(targetPos);
      if (dist < 15) {
         xzDist = dist / 2;
         yDist = dist / 2;
      }

      Vec3 nextPosTowards = AirRandomPos.getPosTowards(this, xzDist, yDist, yAdjust, targetVec, (float) (Math.PI / 10));
      if (nextPosTowards != null) {
         this.navigation.setMaxVisitedNodesMultiplier(0.5F);
         this.navigation.moveTo(nextPosTowards.x, nextPosTowards.y, nextPosTowards.z, 1.0);
      }
   }

   public @Nullable BlockPos getSavedFlowerPos() {
      return this.savedFlowerPos;
   }

   public boolean hasSavedFlowerPos() {
      return this.savedFlowerPos != null;
   }

   public void setSavedFlowerPos(final BlockPos savedFlowerPos) {
      this.savedFlowerPos = savedFlowerPos;
   }

   @VisibleForDebug
   public int getTravellingTicks() {
      return Math.max(this.goToHiveGoal.travellingTicks, this.goToKnownFlowerGoal.travellingTicks);
   }

   @VisibleForDebug
   public List<BlockPos> getBlacklistedHives() {
      return this.goToHiveGoal.blacklistedTargets;
   }

   private boolean isTiredOfLookingForNectar() {
      return this.ticksWithoutNectarSinceExitingHive > 3600;
   }

   private void dropHive() {
      this.hivePos = null;
      this.remainingCooldownBeforeLocatingNewHive = 200;
   }

   private void dropFlower() {
      this.savedFlowerPos = null;
      this.remainingCooldownBeforeLocatingNewFlower = Mth.nextInt(this.random, 20, 60);
   }

   private boolean wantsToEnterHive() {
      if (this.stayOutOfHiveCountdown <= 0 && !this.beePollinateGoal.isPollinating() && !this.hasStung() && this.getTarget() == null) {
         boolean wantsToEnterHive = this.hasNectar()
            || this.isTiredOfLookingForNectar()
            || this.level().environmentAttributes().getValue(EnvironmentAttributes.BEES_STAY_IN_HIVE, this.position());
         return wantsToEnterHive && !this.isHiveNearFire();
      } else {
         return false;
      }
   }

   public void setStayOutOfHiveCountdown(final int ticks) {
      this.stayOutOfHiveCountdown = ticks;
   }

   public float getRollAmount(final float a) {
      return Mth.lerp(a, this.rollAmountO, this.rollAmount);
   }

   private void updateRollAmount() {
      this.rollAmountO = this.rollAmount;
      if (this.isRolling()) {
         this.rollAmount = Math.min(1.0F, this.rollAmount + 0.2F);
      } else {
         this.rollAmount = Math.max(0.0F, this.rollAmount - 0.24F);
      }
   }

   @Override
   protected void customServerAiStep(final ServerLevel level) {
      boolean hasStung = this.hasStung();
      if (this.isInWater()) {
         this.underWaterTicks++;
      } else {
         this.underWaterTicks = 0;
      }

      if (this.underWaterTicks > 20) {
         this.hurtServer(level, this.damageSources().drown(), 1.0F);
      }

      if (hasStung) {
         this.timeSinceSting++;
         if (this.timeSinceSting % 5 == 0 && this.random.nextInt(Mth.clamp(1200 - this.timeSinceSting, 1, 1200)) == 0) {
            this.hurtServer(level, this.damageSources().generic(), this.getHealth());
         }
      }

      if (!this.hasNectar()) {
         this.ticksWithoutNectarSinceExitingHive++;
      }

      this.updatePersistentAnger(level, false);
   }

   public void resetTicksWithoutNectarSinceExitingHive() {
      this.ticksWithoutNectarSinceExitingHive = 0;
   }

   private boolean isHiveNearFire() {
      BeehiveBlockEntity beehiveBlockEntity = this.getBeehiveBlockEntity();
      return beehiveBlockEntity != null && beehiveBlockEntity.isFireNearby();
   }

   @Override
   public long getPersistentAngerEndTime() {
      return this.entityData.get(DATA_ANGER_END_TIME);
   }

   @Override
   public void setPersistentAngerEndTime(final long endTime) {
      this.entityData.set(DATA_ANGER_END_TIME, endTime);
   }

   @Override
   public @Nullable EntityReference<LivingEntity> getPersistentAngerTarget() {
      return this.persistentAngerTarget;
   }

   @Override
   public void setPersistentAngerTarget(final @Nullable EntityReference<LivingEntity> persistentAngerTarget) {
      this.persistentAngerTarget = persistentAngerTarget;
   }

   @Override
   public void startPersistentAngerTimer() {
      this.setTimeToRemainAngry(PERSISTENT_ANGER_TIME.sample(this.random));
   }

   private boolean doesHiveHaveSpace(final BlockPos hivePos) {
      return this.level().getBlockEntity(hivePos) instanceof BeehiveBlockEntity beehiveBlockEntity ? !beehiveBlockEntity.isFull() : false;
   }

   @VisibleForDebug
   public boolean hasHive() {
      return this.hivePos != null;
   }

   @VisibleForDebug
   public @Nullable BlockPos getHivePos() {
      return this.hivePos;
   }

   private int getCropsGrownSincePollination() {
      return this.numCropsGrownSincePollination;
   }

   private void resetNumCropsGrownSincePollination() {
      this.numCropsGrownSincePollination = 0;
   }

   private void incrementNumCropsGrownSincePollination() {
      this.numCropsGrownSincePollination++;
   }

   @Override
   public void aiStep() {
      super.aiStep();
      if (!this.level().isClientSide()) {
         if (this.stayOutOfHiveCountdown > 0) {
            this.stayOutOfHiveCountdown--;
         }

         if (this.remainingCooldownBeforeLocatingNewHive > 0) {
            this.remainingCooldownBeforeLocatingNewHive--;
         }

         if (this.remainingCooldownBeforeLocatingNewFlower > 0) {
            this.remainingCooldownBeforeLocatingNewFlower--;
         }

         boolean shouldRoll = this.isAngry() && !this.hasStung() && this.getTarget() != null && this.getTarget().distanceToSqr(this) < 4.0;
         this.setRolling(shouldRoll);
         if (this.tickCount % 20 == 0 && !this.isHiveValid()) {
            this.hivePos = null;
         }
      }
   }

   private @Nullable BeehiveBlockEntity getBeehiveBlockEntity() {
      if (this.hivePos == null) {
         return null;
      } else {
         return this.isTooFarAway(this.hivePos) ? null : this.level().getBlockEntity(this.hivePos, BlockEntityTypes.BEEHIVE).orElse(null);
      }
   }

   private boolean isHiveValid() {
      return this.getBeehiveBlockEntity() != null;
   }

   public boolean hasNectar() {
      return this.getFlag(8);
   }

   private void setHasNectar(final boolean hasNectar) {
      if (hasNectar) {
         this.resetTicksWithoutNectarSinceExitingHive();
      }

      this.setFlag(8, hasNectar);
   }

   public boolean hasStung() {
      return this.getFlag(4);
   }

   private void setHasStung(final boolean hasStung) {
      this.setFlag(4, hasStung);
   }

   private boolean isRolling() {
      return this.getFlag(2);
   }

   private void setRolling(final boolean rolling) {
      this.setFlag(2, rolling);
   }

   private boolean isTooFarAway(final BlockPos targetPos) {
      return !this.closerThan(targetPos, 48);
   }

   private void setFlag(final int flag, final boolean value) {
      if (value) {
         this.entityData.set(DATA_FLAGS_ID, (byte)(this.entityData.get(DATA_FLAGS_ID) | flag));
      } else {
         this.entityData.set(DATA_FLAGS_ID, (byte)(this.entityData.get(DATA_FLAGS_ID) & ~flag));
      }
   }

   private boolean getFlag(final int flag) {
      return (this.entityData.get(DATA_FLAGS_ID) & flag) != 0;
   }

   public static AttributeSupplier.Builder createAttributes() {
      return Animal.createAnimalAttributes()
         .add(Attributes.MAX_HEALTH, 10.0)
         .add(Attributes.FLYING_SPEED, 0.6F)
         .add(Attributes.MOVEMENT_SPEED, 0.3F)
         .add(Attributes.ATTACK_DAMAGE, 2.0);
   }

   @Override
   protected PathNavigation createNavigation(final Level level) {
      FlyingPathNavigation flyingPathNavigation = new FlyingPathNavigation(this, level) {
         @Override
         public boolean isStableDestination(final BlockPos pos) {
            return !this.level.getBlockState(pos.below()).isAir();
         }

         @Override
         public void tick() {
            if (!Bee.this.beePollinateGoal.isPollinating()) {
               super.tick();
            }
         }
      };
      flyingPathNavigation.setCanOpenDoors(false);
      flyingPathNavigation.setCanFloat(false);
      flyingPathNavigation.setRequiredPathLength(48.0F);
      return flyingPathNavigation;
   }

   @Override
   public InteractionResult mobInteract(final Player player, final InteractionHand hand) {
      ItemStack heldItem = player.getItemInHand(hand);
      if (this.isFood(heldItem) && heldItem.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof FlowerBlock flower) {
         MobEffectInstance effect = flower.getBeeInteractionEffect();
         if (effect != null) {
            this.usePlayerItem(player, hand, heldItem);
            if (!this.level().isClientSide()) {
               this.addEffect(effect);
            }

            return InteractionResult.SUCCESS;
         }
      }

      return super.mobInteract(player, hand);
   }

   @Override
   public boolean isFood(final ItemStack itemStack) {
      return itemStack.is(ItemTags.BEE_FOOD);
   }

   @Override
   protected void playStepSound(final BlockPos pos, final BlockState blockState) {
   }

   @Override
   protected SoundEvent getAmbientSound() {
      return null;
   }

   @Override
   protected SoundEvent getHurtSound(final DamageSource source) {
      return SoundEvents.BEE_HURT;
   }

   @Override
   protected SoundEvent getDeathSound() {
      return SoundEvents.BEE_DEATH;
   }

   @Override
   protected float getSoundVolume() {
      return 0.4F;
   }

   public @Nullable Bee getBreedOffspring(final ServerLevel level, final AgeableMob partner) {
      return EntityTypes.BEE.create(level, EntitySpawnReason.BREEDING);
   }

   @Override
   protected void checkFallDamage(final double ya, final boolean onGround, final BlockState onState, final BlockPos pos) {
   }

   @Override
   public boolean isFlapping() {
      return this.isFlying() && this.tickCount % TICKS_PER_FLAP == 0;
   }

   public boolean isFlying() {
      return !this.onGround();
   }

   @Override
   protected boolean omnidirectionalAirMover() {
      return true;
   }

   public void dropOffNectar() {
      this.setHasNectar(false);
      this.resetNumCropsGrownSincePollination();
   }

   @Override
   public boolean hurtServer(final ServerLevel level, final DamageSource source, final float damage) {
      if (this.isInvulnerableTo(level, source)) {
         return false;
      }

      this.beePollinateGoal.stopPollinating();
      return super.hurtServer(level, source, damage);
   }

   @Override
   protected void jumpInLiquid(final TagKey<Fluid> type) {
      this.setDeltaMovement(this.getDeltaMovement().add(0.0, 0.01, 0.0));
   }

   @Override
   public Vec3 getLeashOffset() {
      return new Vec3(0.0, 0.5F * this.getEyeHeight(), this.getBbWidth() * 0.2F);
   }

   private boolean closerThan(final BlockPos targetPos, final int distance) {
      return targetPos.closerThan(this.blockPosition(), distance);
   }

   public void setHivePos(final BlockPos hivePos) {
      this.hivePos = hivePos;
   }

   public static boolean attractsBees(final BlockState state) {
      if (state.is(BlockTags.BEE_ATTRACTIVE)) {
         if (state.getValueOrElse(BlockStateProperties.WATERLOGGED, false)) {
            return false;
         } else {
            return state.is(Blocks.SUNFLOWER) ? state.getValue(DoublePlantBlock.HALF) == DoubleBlockHalf.UPPER : true;
         }
      } else {
         return false;
      }
   }

   @Override
   public void registerDebugValues(final ServerLevel level, final DebugValueSource.Registration registration) {
      super.registerDebugValues(level, registration);
      registration.register(
         DebugSubscriptions.BEES,
         () -> new DebugBeeInfo(
            Optional.ofNullable(this.getHivePos()), Optional.ofNullable(this.getSavedFlowerPos()), this.getTravellingTicks(), this.getBlacklistedHives()
         )
      );
   }

   private abstract class BaseBeeGoal extends Goal {
      public abstract boolean canBeeUse();

      public abstract boolean canBeeContinueToUse();

      @Override
      public boolean canUse() {
         return this.canBeeUse() && !Bee.this.isAngry();
      }

      @Override
      public boolean canContinueToUse() {
         return this.canBeeContinueToUse() && !Bee.this.isAngry();
      }
   }

   private class BeeAttackGoal extends MeleeAttackGoal {
      public BeeAttackGoal(final PathfinderMob mob, final double speedModifier, final boolean trackTarget) {
         super(mob, speedModifier, trackTarget);
      }

      @Override
      public boolean canUse() {
         return super.canUse() && Bee.this.isAngry() && !Bee.this.hasStung();
      }

      @Override
      public boolean canContinueToUse() {
         return super.canContinueToUse() && Bee.this.isAngry() && !Bee.this.hasStung();
      }
   }

   private static class BeeBecomeAngryTargetGoal extends NearestAttackableTargetGoal<Player> {
      public BeeBecomeAngryTargetGoal(final Bee bee) {
         super(bee, Player.class, 10, true, false, bee::isAngryAt);
      }

      @Override
      public boolean canUse() {
         return this.beeCanTarget() && super.canUse();
      }

      @Override
      public boolean canContinueToUse() {
         boolean beeCanTarget = this.beeCanTarget();
         if (beeCanTarget && this.mob.getTarget() != null) {
            return super.canContinueToUse();
         }

         this.targetMob = null;
         return false;
      }

      private boolean beeCanTarget() {
         Bee bee = (Bee)this.mob;
         return bee.isAngry() && !bee.hasStung();
      }
   }

   private class BeeEnterHiveGoal extends Bee.BaseBeeGoal {
      @Override
      public boolean canBeeUse() {
         if (Bee.this.hivePos != null && Bee.this.wantsToEnterHive() && Bee.this.hivePos.closerToCenterThan(Bee.this.position(), 2.0)) {
            BeehiveBlockEntity beehiveBlockEntity = Bee.this.getBeehiveBlockEntity();
            if (beehiveBlockEntity != null) {
               if (!beehiveBlockEntity.isFull()) {
                  return true;
               }

               Bee.this.hivePos = null;
            }
         }

         return false;
      }

      @Override
      public boolean canBeeContinueToUse() {
         return false;
      }

      @Override
      public void start() {
         BeehiveBlockEntity beehiveBlockEntity = Bee.this.getBeehiveBlockEntity();
         if (beehiveBlockEntity != null) {
            beehiveBlockEntity.addOccupant(Bee.this);
         }
      }
   }

   @VisibleForDebug
   public class BeeGoToHiveGoal extends Bee.BaseBeeGoal {
      public static final int MAX_TRAVELLING_TICKS = 2400;
      private int travellingTicks;
      private static final int MAX_BLACKLISTED_TARGETS = 3;
      private final List<BlockPos> blacklistedTargets = Lists.newArrayList();
      private @Nullable Path lastPath;
      private static final int TICKS_BEFORE_HIVE_DROP = 60;
      private int ticksStuck;

      public BeeGoToHiveGoal() {
         this.setFlags(EnumSet.of(Goal.Flag.MOVE));
      }

      @Override
      public boolean canBeeUse() {
         return Bee.this.hivePos != null
            && !Bee.this.isTooFarAway(Bee.this.hivePos)
            && !Bee.this.hasHome()
            && Bee.this.wantsToEnterHive()
            && !this.hasReachedTarget(Bee.this.hivePos)
            && Bee.this.level().getBlockState(Bee.this.hivePos).is(BlockTags.BEEHIVES);
      }

      @Override
      public boolean canBeeContinueToUse() {
         return this.canBeeUse();
      }

      @Override
      public void start() {
         this.travellingTicks = 0;
         this.ticksStuck = 0;
         super.start();
      }

      @Override
      public void stop() {
         this.travellingTicks = 0;
         this.ticksStuck = 0;
         Bee.this.navigation.stop();
         Bee.this.navigation.resetMaxVisitedNodesMultiplier();
      }

      @Override
      public void tick() {
         if (Bee.this.hivePos != null) {
            this.travellingTicks++;
            if (this.travellingTicks > this.adjustedTickDelay(2400)) {
               this.dropAndBlacklistHive();
            } else if (!Bee.this.navigation.isInProgress()) {
               if (!Bee.this.closerThan(Bee.this.hivePos, 16)) {
                  if (Bee.this.isTooFarAway(Bee.this.hivePos)) {
                     Bee.this.dropHive();
                  } else {
                     Bee.this.pathfindRandomlyTowards(Bee.this.hivePos);
                  }
               } else {
                  boolean canReachAllTheWayToTarget = this.pathfindDirectlyTowards(Bee.this.hivePos);
                  if (!canReachAllTheWayToTarget) {
                     this.dropAndBlacklistHive();
                  } else if (this.lastPath != null && Bee.this.navigation.getPath().sameAs(this.lastPath)) {
                     this.ticksStuck++;
                     if (this.ticksStuck > 60) {
                        Bee.this.dropHive();
                        this.ticksStuck = 0;
                     }
                  } else {
                     this.lastPath = Bee.this.navigation.getPath();
                  }
               }
            }
         }
      }

      private boolean pathfindDirectlyTowards(final BlockPos targetPos) {
         int closeEnough = Bee.this.closerThan(targetPos, 3) ? 1 : 2;
         Bee.this.navigation.setMaxVisitedNodesMultiplier(10.0F);
         Bee.this.navigation.moveTo(targetPos.getX(), targetPos.getY(), targetPos.getZ(), closeEnough, 1.0);
         return Bee.this.navigation.getPath() != null && Bee.this.navigation.getPath().canReach();
      }

      private boolean isTargetBlacklisted(final BlockPos targetPos) {
         return this.blacklistedTargets.contains(targetPos);
      }

      private void blacklistTarget(final BlockPos targetPos) {
         this.blacklistedTargets.add(targetPos);

         while (this.blacklistedTargets.size() > 3) {
            this.blacklistedTargets.remove(0);
         }
      }

      private void clearBlacklist() {
         this.blacklistedTargets.clear();
      }

      private void dropAndBlacklistHive() {
         if (Bee.this.hivePos != null) {
            this.blacklistTarget(Bee.this.hivePos);
         }

         Bee.this.dropHive();
      }

      private boolean hasReachedTarget(final BlockPos targetPos) {
         if (Bee.this.closerThan(targetPos, 2)) {
            return true;
         }

         Path path = Bee.this.navigation.getPath();
         return path != null && path.getTarget().equals(targetPos) && path.canReach() && path.isDone();
      }
   }

   public class BeeGoToKnownFlowerGoal extends Bee.BaseBeeGoal {
      private static final int MAX_TRAVELLING_TICKS = 2400;
      private int travellingTicks;

      public BeeGoToKnownFlowerGoal() {
         this.setFlags(EnumSet.of(Goal.Flag.MOVE));
      }

      @Override
      public boolean canBeeUse() {
         return Bee.this.savedFlowerPos != null && !Bee.this.hasHome() && this.wantsToGoToKnownFlower() && !Bee.this.closerThan(Bee.this.savedFlowerPos, 2);
      }

      @Override
      public boolean canBeeContinueToUse() {
         return this.canBeeUse();
      }

      @Override
      public void start() {
         this.travellingTicks = 0;
         super.start();
      }

      @Override
      public void stop() {
         this.travellingTicks = 0;
         Bee.this.navigation.stop();
         Bee.this.navigation.resetMaxVisitedNodesMultiplier();
      }

      @Override
      public void tick() {
         if (Bee.this.savedFlowerPos != null) {
            this.travellingTicks++;
            if (this.travellingTicks > this.adjustedTickDelay(2400)) {
               Bee.this.dropFlower();
            } else if (!Bee.this.navigation.isInProgress()) {
               if (Bee.this.isTooFarAway(Bee.this.savedFlowerPos)) {
                  Bee.this.dropFlower();
               } else {
                  Bee.this.pathfindRandomlyTowards(Bee.this.savedFlowerPos);
               }
            }
         }
      }

      private boolean wantsToGoToKnownFlower() {
         return Bee.this.ticksWithoutNectarSinceExitingHive > 600;
      }
   }

   private class BeeGrowCropGoal extends Bee.BaseBeeGoal {
      private static final int GROW_CHANCE = 30;

      @Override
      public boolean canBeeUse() {
         if (Bee.this.getCropsGrownSincePollination() >= 10) {
            return false;
         } else {
            return Bee.this.random.nextFloat() < 0.3F ? false : Bee.this.hasNectar() && Bee.this.isHiveValid();
         }
      }

      @Override
      public boolean canBeeContinueToUse() {
         return this.canBeeUse();
      }

      @Override
      public void tick() {
         if (Bee.this.random.nextInt(this.adjustedTickDelay(30)) == 0) {
            for (int i = 1; i <= 2; i++) {
               BlockPos belowPos = Bee.this.blockPosition().below(i);
               BlockState belowState = Bee.this.level().getBlockState(belowPos);
               Block belowBlock = belowState.getBlock();
               BlockState growState = null;
               if (belowState.is(BlockTags.BEE_GROWABLES)) {
                  if (belowBlock instanceof CropBlock cropBlockBelow) {
                     if (!cropBlockBelow.isMaxAge(belowState)) {
                        growState = cropBlockBelow.getStateForAge(cropBlockBelow.getAge(belowState) + 1);
                     }
                  } else if (belowBlock instanceof StemBlock) {
                     int age = belowState.getValue(StemBlock.AGE);
                     if (age < 7) {
                        growState = belowState.setValue(StemBlock.AGE, age + 1);
                     }
                  } else if (belowState.is(Blocks.SWEET_BERRY_BUSH)) {
                     int age = belowState.getValue(SweetBerryBushBlock.AGE);
                     if (age < 3) {
                        growState = belowState.setValue(SweetBerryBushBlock.AGE, age + 1);
                     }
                  } else if (belowState.is(Blocks.CAVE_VINES) || belowState.is(Blocks.CAVE_VINES_PLANT)) {
                     BonemealableBlock bonemealableBlock = (BonemealableBlock)belowState.getBlock();
                     if (bonemealableBlock.isValidBonemealTarget(Bee.this.level(), belowPos, belowState)) {
                        bonemealableBlock.performBonemeal((ServerLevel)Bee.this.level(), Bee.this.random, belowPos, belowState);
                        growState = Bee.this.level().getBlockState(belowPos);
                     }
                  }

                  if (growState != null) {
                     Bee.this.level().levelEvent(2011, belowPos, 15);
                     Bee.this.level().setBlockAndUpdate(belowPos, growState);
                     Bee.this.incrementNumCropsGrownSincePollination();
                  }
               }
            }
         }
      }
   }

   private class BeeHurtByOtherGoal extends HurtByTargetGoal {
      public BeeHurtByOtherGoal(final Bee bee) {
         super(bee);
      }

      @Override
      public boolean canContinueToUse() {
         return Bee.this.isAngry() && super.canContinueToUse();
      }

      @Override
      protected void alertOther(final Mob other, final LivingEntity hurtByMob) {
         if (other instanceof Bee && this.mob.hasLineOfSight(hurtByMob)) {
            other.setTarget(hurtByMob);
         }
      }
   }

   private class BeeLocateHiveGoal extends Bee.BaseBeeGoal {
      @Override
      public boolean canBeeUse() {
         return Bee.this.remainingCooldownBeforeLocatingNewHive == 0 && !Bee.this.hasHive() && Bee.this.wantsToEnterHive();
      }

      @Override
      public boolean canBeeContinueToUse() {
         return false;
      }

      @Override
      public void start() {
         Bee.this.remainingCooldownBeforeLocatingNewHive = 200;
         List<BlockPos> hivesWithSpace = this.findNearbyHivesWithSpace();
         if (!hivesWithSpace.isEmpty()) {
            for (BlockPos posToCheck : hivesWithSpace) {
               if (!Bee.this.goToHiveGoal.isTargetBlacklisted(posToCheck)) {
                  Bee.this.hivePos = posToCheck;
                  return;
               }
            }

            Bee.this.goToHiveGoal.clearBlacklist();
            Bee.this.hivePos = hivesWithSpace.get(0);
         }
      }

      private List<BlockPos> findNearbyHivesWithSpace() {
         BlockPos beePos = Bee.this.blockPosition();
         PoiManager poiManager = ((ServerLevel)Bee.this.level()).getPoiManager();
         Stream<PoiRecord> nearbyHives = poiManager.getInRange(p -> p.is(PoiTypeTags.BEE_HOME), beePos, 20, PoiManager.Occupancy.ANY);
         return nearbyHives.map(PoiRecord::getPos)
            .filter(Bee.this::doesHiveHaveSpace)
            .sorted(Comparator.comparingDouble(pos -> pos.distSqr(beePos)))
            .collect(Collectors.toList());
      }
   }

   private class BeeLookControl extends LookControl {
      public BeeLookControl(final Mob mob) {
         super(mob);
      }

      @Override
      public void tick() {
         if (!Bee.this.isAngry()) {
            super.tick();
         }
      }

      @Override
      protected boolean resetXRotOnTick() {
         return !Bee.this.beePollinateGoal.isPollinating();
      }
   }

   private class BeePollinateGoal extends Bee.BaseBeeGoal {
      private static final int MIN_POLLINATION_TICKS = 400;
      private static final double ARRIVAL_THRESHOLD = 0.1;
      private static final int POSITION_CHANGE_CHANCE = 25;
      private static final float SPEED_MODIFIER = 0.35F;
      private static final float HOVER_HEIGHT_WITHIN_FLOWER = 0.6F;
      private static final float HOVER_POS_OFFSET = 0.33333334F;
      private static final int FLOWER_SEARCH_RADIUS = 5;
      private int successfulPollinatingTicks;
      private int lastSoundPlayedTick;
      private boolean pollinating;
      private @Nullable Vec3 hoverPos;
      private int pollinatingTicks;
      private static final int MAX_POLLINATING_TICKS = 600;
      private Long2LongOpenHashMap unreachableFlowerCache = new Long2LongOpenHashMap();

      public BeePollinateGoal() {
         this.setFlags(EnumSet.of(Goal.Flag.MOVE));
      }

      @Override
      public boolean canBeeUse() {
         if (Bee.this.remainingCooldownBeforeLocatingNewFlower > 0) {
            return false;
         } else if (Bee.this.hasNectar()) {
            return false;
         } else if (Bee.this.level().isRaining()) {
            return false;
         } else {
            Optional<BlockPos> nearbyPos = this.findNearbyFlower();
            if (nearbyPos.isPresent()) {
               Bee.this.savedFlowerPos = nearbyPos.get();
               Bee.this.navigation
                  .moveTo(Bee.this.savedFlowerPos.getX() + 0.5, Bee.this.savedFlowerPos.getY() + 0.5, Bee.this.savedFlowerPos.getZ() + 0.5, 1.2F);
               return true;
            } else {
               Bee.this.remainingCooldownBeforeLocatingNewFlower = Mth.nextInt(Bee.this.random, 20, 60);
               return false;
            }
         }
      }

      @Override
      public boolean canBeeContinueToUse() {
         if (!this.pollinating) {
            return false;
         } else if (!Bee.this.hasSavedFlowerPos()) {
            return false;
         } else if (Bee.this.level().isRaining()) {
            return false;
         } else {
            return this.hasPollinatedLongEnough() ? Bee.this.random.nextFloat() < 0.2F : true;
         }
      }

      private boolean hasPollinatedLongEnough() {
         return this.successfulPollinatingTicks > 400;
      }

      private boolean isPollinating() {
         return this.pollinating;
      }

      private void stopPollinating() {
         this.pollinating = false;
      }

      @Override
      public void start() {
         this.successfulPollinatingTicks = 0;
         this.pollinatingTicks = 0;
         this.lastSoundPlayedTick = 0;
         this.pollinating = true;
         Bee.this.resetTicksWithoutNectarSinceExitingHive();
      }

      @Override
      public void stop() {
         if (this.hasPollinatedLongEnough()) {
            Bee.this.setHasNectar(true);
         }

         this.pollinating = false;
         Bee.this.navigation.stop();
         Bee.this.remainingCooldownBeforeLocatingNewFlower = 200;
      }

      @Override
      public boolean requiresUpdateEveryTick() {
         return true;
      }

      @Override
      public void tick() {
         if (Bee.this.hasSavedFlowerPos()) {
            this.pollinatingTicks++;
            if (this.pollinatingTicks > 600) {
               Bee.this.dropFlower();
               this.pollinating = false;
               Bee.this.remainingCooldownBeforeLocatingNewFlower = 200;
            } else {
               Vec3 flowerPos = Vec3.atBottomCenterOf(Bee.this.savedFlowerPos).add(0.0, 0.6F, 0.0);
               if (flowerPos.distanceTo(Bee.this.position()) > 1.0) {
                  this.hoverPos = flowerPos;
                  this.setWantedPos();
               } else {
                  if (this.hoverPos == null) {
                     this.hoverPos = flowerPos;
                  }

                  boolean arrivedAtHoverPos = Bee.this.position().distanceTo(this.hoverPos) <= 0.1;
                  boolean shouldSetWantedPos = true;
                  if (!arrivedAtHoverPos && this.pollinatingTicks > 600) {
                     Bee.this.dropFlower();
                  } else {
                     if (arrivedAtHoverPos) {
                        boolean shouldChangeHoverPositions = Bee.this.random.nextInt(25) == 0;
                        if (shouldChangeHoverPositions) {
                           this.hoverPos = new Vec3(flowerPos.x() + this.getOffset(), flowerPos.y(), flowerPos.z() + this.getOffset());
                           Bee.this.navigation.stop();
                        } else {
                           shouldSetWantedPos = false;
                        }

                        Bee.this.getLookControl().setLookAt(flowerPos.x(), flowerPos.y(), flowerPos.z());
                     }

                     if (shouldSetWantedPos) {
                        this.setWantedPos();
                     }

                     this.successfulPollinatingTicks++;
                     if (Bee.this.random.nextFloat() < 0.05F && this.successfulPollinatingTicks > this.lastSoundPlayedTick + 60) {
                        this.lastSoundPlayedTick = this.successfulPollinatingTicks;
                        Bee.this.playSound(SoundEvents.BEE_POLLINATE, 1.0F, 1.0F);
                     }
                  }
               }
            }
         }
      }

      private void setWantedPos() {
         Bee.this.getMoveControl().setWantedPosition(this.hoverPos.x(), this.hoverPos.y(), this.hoverPos.z(), 0.35F);
      }

      private float getOffset() {
         return (Bee.this.random.nextFloat() * 2.0F - 1.0F) * 0.33333334F;
      }

      private Optional<BlockPos> findNearbyFlower() {
         Iterable<BlockPos> closestNearbyFlowers = BlockPos.withinManhattan(Bee.this.blockPosition(), 5, 5, 5);
         Long2LongOpenHashMap tempCache = new Long2LongOpenHashMap();

         for (BlockPos pos : closestNearbyFlowers) {
            long unreachableUntilTime = this.unreachableFlowerCache.getOrDefault(pos.asLong(), Long.MIN_VALUE);
            if (Bee.this.level().getGameTime() < unreachableUntilTime) {
               tempCache.put(pos.asLong(), unreachableUntilTime);
            } else if (Bee.attractsBees(Bee.this.level().getBlockState(pos))) {
               Path path = Bee.this.navigation.createPath(pos, 1);
               if (path != null && path.canReach()) {
                  return Optional.of(pos);
               }

               tempCache.put(pos.asLong(), Bee.this.level().getGameTime() + 600L);
            }
         }

         this.unreachableFlowerCache = tempCache;
         return Optional.empty();
      }
   }

   private class BeeWanderGoal extends Goal {
      public BeeWanderGoal() {
         this.setFlags(EnumSet.of(Goal.Flag.MOVE));
      }

      @Override
      public boolean canUse() {
         return Bee.this.navigation.isDone() && Bee.this.random.nextInt(10) == 0;
      }

      @Override
      public boolean canContinueToUse() {
         return Bee.this.navigation.isInProgress();
      }

      @Override
      public void start() {
         Vec3 targetPos = this.findPos();
         if (targetPos != null) {
            Bee.this.navigation.moveTo(Bee.this.navigation.createPath(BlockPos.containing(targetPos), 1), 1.0);
         }
      }

      private @Nullable Vec3 findPos() {
         Vec3 wanderDirection;
         if (Bee.this.isHiveValid() && !Bee.this.closerThan(Bee.this.hivePos, this.getWanderThreshold())) {
            Vec3 hivePosVec = Vec3.atCenterOf(Bee.this.hivePos);
            wanderDirection = hivePosVec.subtract(Bee.this.position()).normalize();
         } else {
            wanderDirection = Bee.this.getViewVector(0.0F);
         }

         int xzDist = 8;
         Vec3 groundBasedPosition = HoverRandomPos.getPos(Bee.this, 8, 7, wanderDirection.x, wanderDirection.z, (float) (Math.PI / 2), 3, 1);
         return groundBasedPosition != null
            ? groundBasedPosition
            : AirAndWaterRandomPos.getPos(Bee.this, 8, 4, -2, wanderDirection.x, wanderDirection.z, (float) (Math.PI / 2));
      }

      private int getWanderThreshold() {
         int distanceReduction = !Bee.this.hasHive() && !Bee.this.hasSavedFlowerPos() ? 16 : 24;
         return 48 - distanceReduction;
      }
   }

   private class ValidateFlowerGoal extends Bee.BaseBeeGoal {
      private final int validateFlowerCooldown = Mth.nextInt(Bee.this.random, 20, 40);
      private long lastValidateTick = -1L;

      @Override
      public void start() {
         if (Bee.this.savedFlowerPos != null && Bee.this.level().isLoaded(Bee.this.savedFlowerPos) && !this.isFlower(Bee.this.savedFlowerPos)) {
            Bee.this.dropFlower();
         }

         this.lastValidateTick = Bee.this.level().getGameTime();
      }

      @Override
      public boolean canBeeUse() {
         return Bee.this.level().getGameTime() > this.lastValidateTick + this.validateFlowerCooldown;
      }

      @Override
      public boolean canBeeContinueToUse() {
         return false;
      }

      private boolean isFlower(final BlockPos flowerPos) {
         return Bee.attractsBees(Bee.this.level().getBlockState(flowerPos));
      }
   }

   private class ValidateHiveGoal extends Bee.BaseBeeGoal {
      private final int VALIDATE_HIVE_COOLDOWN = Mth.nextInt(Bee.this.random, 20, 40);
      private long lastValidateTick = -1L;

      @Override
      public void start() {
         if (Bee.this.hivePos != null && Bee.this.level().isLoaded(Bee.this.hivePos) && !Bee.this.isHiveValid()) {
            Bee.this.dropHive();
         }

         this.lastValidateTick = Bee.this.level().getGameTime();
      }

      @Override
      public boolean canBeeUse() {
         return Bee.this.level().getGameTime() > this.lastValidateTick + this.VALIDATE_HIVE_COOLDOWN;
      }

      @Override
      public boolean canBeeContinueToUse() {
         return false;
      }
   }
}
