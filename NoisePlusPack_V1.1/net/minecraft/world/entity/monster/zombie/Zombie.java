package net.minecraft.world.entity.monster.zombie;

import com.google.common.annotations.VisibleForTesting;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.SpecialDates;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.ConversionParams;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.BreakDoorGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MoveThroughVillageGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RemoveBlockGoal;
import net.minecraft.world.entity.ai.goal.SpearUseGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.ZombieAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.animal.turtle.Turtle;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class Zombie extends Monster {
   private static final Identifier SPEED_MODIFIER_BABY_ID = Identifier.withDefaultNamespace("baby");
   private static final AttributeModifier SPEED_MODIFIER_BABY = new AttributeModifier(
      SPEED_MODIFIER_BABY_ID, 0.5, AttributeModifier.Operation.ADD_MULTIPLIED_BASE
   );
   private static final Identifier REINFORCEMENT_CALLER_CHARGE_ID = Identifier.withDefaultNamespace("reinforcement_caller_charge");
   private static final AttributeModifier ZOMBIE_REINFORCEMENT_CALLEE_CHARGE = new AttributeModifier(
      Identifier.withDefaultNamespace("reinforcement_callee_charge"), -0.05F, AttributeModifier.Operation.ADD_VALUE
   );
   private static final Identifier LEADER_ZOMBIE_BONUS_ID = Identifier.withDefaultNamespace("leader_zombie_bonus");
   private static final Identifier ZOMBIE_RANDOM_SPAWN_BONUS_ID = Identifier.withDefaultNamespace("zombie_random_spawn_bonus");
   private static final EntityDataAccessor<Boolean> DATA_BABY_ID = SynchedEntityData.defineId(Zombie.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<Integer> DATA_SPECIAL_TYPE_ID = SynchedEntityData.defineId(Zombie.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Boolean> DATA_DROWNED_CONVERSION_ID = SynchedEntityData.defineId(Zombie.class, EntityDataSerializers.BOOLEAN);
   public static final float ZOMBIE_LEADER_CHANCE = 0.05F;
   public static final int REINFORCEMENT_ATTEMPTS = 50;
   public static final int REINFORCEMENT_RANGE_MAX = 40;
   public static final int REINFORCEMENT_RANGE_MIN = 7;
   private static final int NOT_CONVERTING = -1;
   private static final EntityDimensions BABY_DIMENSIONS = EntityType.ZOMBIE.getDimensions().scale(0.5F).withEyeHeight(0.93F);
   private static final float BREAK_DOOR_CHANCE = 0.1F;
   private static final Predicate<Difficulty> DOOR_BREAKING_PREDICATE = p_460272_ -> p_460272_ == Difficulty.HARD;
   private static final boolean DEFAULT_BABY = false;
   private static final boolean DEFAULT_CAN_BREAK_DOORS = false;
   private static final int DEFAULT_IN_WATER_TIME = 0;
   private final BreakDoorGoal breakDoorGoal = new BreakDoorGoal(this, DOOR_BREAKING_PREDICATE);
   private boolean canBreakDoors = false;
   private int inWaterTime = 0;
   private int conversionTime;

   public Zombie(EntityType<? extends Zombie> p_460631_, Level p_455477_) {
      super(p_460631_, p_455477_);
   }

   public Zombie(Level p_453302_) {
      this(EntityType.ZOMBIE, p_453302_);
   }

   @Override
   protected void registerGoals() {
      this.goalSelector.addGoal(4, new Zombie.ZombieAttackTurtleEggGoal(this, 1.0, 3));
      this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
      this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
      this.addBehaviourGoals();
   }

   protected void addBehaviourGoals() {
      this.goalSelector.addGoal(2, new SpearUseGoal<>(this, 1.0, 1.0, 10.0F, 2.0F));
      this.goalSelector.addGoal(3, new ZombieAttackGoal(this, 1.0, false));
      this.goalSelector.addGoal(6, new MoveThroughVillageGoal(this, 1.0, true, 4, this::canBreakDoors));
      this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 1.0));
      this.targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers(ZombifiedPiglin.class));
      this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
      this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, AbstractVillager.class, false));
      this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, IronGolem.class, true));
      this.targetSelector.addGoal(5, new NearestAttackableTargetGoal<>(this, Turtle.class, 10, true, false, Turtle.BABY_ON_LAND_SELECTOR));
   }

   public static AttributeSupplier.Builder createAttributes() {
      return Monster.createMonsterAttributes()
         .add(Attributes.FOLLOW_RANGE, 35.0)
         .add(Attributes.MOVEMENT_SPEED, 0.23F)
         .add(Attributes.ATTACK_DAMAGE, 3.0)
         .add(Attributes.ARMOR, 2.0)
         .add(Attributes.SPAWN_REINFORCEMENTS_CHANCE);
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder p_455070_) {
      super.defineSynchedData(p_455070_);
      p_455070_.define(DATA_BABY_ID, false);
      p_455070_.define(DATA_SPECIAL_TYPE_ID, 0);
      p_455070_.define(DATA_DROWNED_CONVERSION_ID, false);
   }

   public boolean isUnderWaterConverting() {
      return this.getEntityData().get(DATA_DROWNED_CONVERSION_ID);
   }

   public boolean canBreakDoors() {
      return this.canBreakDoors;
   }

   public void setCanBreakDoors(boolean p_455101_) {
      if (this.navigation.canNavigateGround()) {
         if (this.canBreakDoors != p_455101_) {
            this.canBreakDoors = p_455101_;
            this.navigation.setCanOpenDoors(p_455101_);
            if (p_455101_) {
               this.goalSelector.addGoal(1, this.breakDoorGoal);
            } else {
               this.goalSelector.removeGoal(this.breakDoorGoal);
            }
         }
      } else if (this.canBreakDoors) {
         this.goalSelector.removeGoal(this.breakDoorGoal);
         this.canBreakDoors = false;
      }
   }

   @Override
   public boolean isBaby() {
      return this.getEntityData().get(DATA_BABY_ID);
   }

   @Override
   protected int getBaseExperienceReward(ServerLevel p_455516_) {
      if (this.isBaby()) {
         this.xpReward = (int)(this.xpReward * 2.5);
      }

      return super.getBaseExperienceReward(p_455516_);
   }

   @Override
   public void setBaby(boolean p_458526_) {
      this.getEntityData().set(DATA_BABY_ID, p_458526_);
      if (this.level() != null && !this.level().isClientSide()) {
         AttributeInstance attributeinstance = this.getAttribute(Attributes.MOVEMENT_SPEED);
         attributeinstance.removeModifier(SPEED_MODIFIER_BABY_ID);
         if (p_458526_) {
            attributeinstance.addTransientModifier(SPEED_MODIFIER_BABY);
         }
      }
   }

   @Override
   public void onSyncedDataUpdated(EntityDataAccessor<?> p_451552_) {
      if (DATA_BABY_ID.equals(p_451552_)) {
         this.refreshDimensions();
      }

      super.onSyncedDataUpdated(p_451552_);
   }

   protected boolean convertsInWater() {
      return true;
   }

   @Override
   public void tick() {
      if (this.level() instanceof ServerLevel serverlevel && this.isAlive() && !this.isNoAi()) {
         if (this.isUnderWaterConverting()) {
            this.conversionTime--;
            if (this.conversionTime < 0) {
               this.doUnderWaterConversion(serverlevel);
            }
         } else if (this.convertsInWater()) {
            if (this.isEyeInFluid(FluidTags.WATER)) {
               this.inWaterTime++;
               if (this.inWaterTime >= 600) {
                  this.startUnderWaterConversion(300);
               }
            } else {
               this.inWaterTime = -1;
            }
         }
      }

      super.tick();
   }

   private void startUnderWaterConversion(int p_458593_) {
      this.conversionTime = p_458593_;
      this.getEntityData().set(DATA_DROWNED_CONVERSION_ID, true);
   }

   protected void doUnderWaterConversion(ServerLevel p_455392_) {
      this.convertToZombieType(p_455392_, EntityType.DROWNED);
      if (!this.isSilent()) {
         p_455392_.levelEvent(null, 1040, this.blockPosition(), 0);
      }
   }

   protected void convertToZombieType(ServerLevel p_455508_, EntityType<? extends Zombie> p_460671_) {
      this.convertTo(
         p_460671_,
         ConversionParams.single(this, true, true),
         p_451761_ -> p_451761_.handleAttributes(p_455508_.getCurrentDifficultyAt(p_451761_.blockPosition()).getSpecialMultiplier())
      );
   }

   @VisibleForTesting
   public boolean convertVillagerToZombieVillager(ServerLevel p_454968_, Villager p_452932_) {
      ZombieVillager zombievillager = p_452932_.convertTo(
         EntityType.ZOMBIE_VILLAGER,
         ConversionParams.single(p_452932_, true, true),
         p_458247_ -> {
            p_458247_.finalizeSpawn(
               p_454968_, p_454968_.getCurrentDifficultyAt(p_458247_.blockPosition()), EntitySpawnReason.CONVERSION, new Zombie.ZombieGroupData(false, true)
            );
            p_458247_.setVillagerData(p_452932_.getVillagerData());
            p_458247_.setGossips(p_452932_.getGossips().copy());
            p_458247_.setTradeOffers(p_452932_.getOffers().copy());
            p_458247_.setVillagerXp(p_452932_.getVillagerXp());
            if (!this.isSilent()) {
               p_454968_.levelEvent(null, 1026, this.blockPosition(), 0);
            }
         }
      );
      return zombievillager != null;
   }

   protected boolean isSunSensitive() {
      return true;
   }

   @Override
   public boolean hurtServer(ServerLevel p_452619_, DamageSource p_458181_, float p_457231_) {
      if (!super.hurtServer(p_452619_, p_458181_, p_457231_)) {
         return false;
      }

      LivingEntity livingentity = this.getTarget();
      if (livingentity == null && p_458181_.getEntity() instanceof LivingEntity) {
         livingentity = (LivingEntity)p_458181_.getEntity();
      }

      if (livingentity != null
         && p_452619_.getDifficulty() == Difficulty.HARD
         && this.random.nextFloat() < this.getAttributeValue(Attributes.SPAWN_REINFORCEMENTS_CHANCE)
         && p_452619_.isSpawningMonsters()) {
         int i = Mth.floor(this.getX());
         int j = Mth.floor(this.getY());
         int k = Mth.floor(this.getZ());
         EntityType<? extends Zombie> entitytype = this.getType();
         Zombie zombie = entitytype.create(p_452619_, EntitySpawnReason.REINFORCEMENT);
         if (zombie == null) {
            return true;
         }

         for (int l = 0; l < 50; l++) {
            int i1 = i + Mth.nextInt(this.random, 7, 40) * Mth.nextInt(this.random, -1, 1);
            int j1 = j + Mth.nextInt(this.random, 7, 40) * Mth.nextInt(this.random, -1, 1);
            int k1 = k + Mth.nextInt(this.random, 7, 40) * Mth.nextInt(this.random, -1, 1);
            BlockPos blockpos = new BlockPos(i1, j1, k1);
            if (SpawnPlacements.isSpawnPositionOk(entitytype, p_452619_, blockpos)
               && SpawnPlacements.checkSpawnRules(entitytype, p_452619_, EntitySpawnReason.REINFORCEMENT, blockpos, p_452619_.random)) {
               zombie.setPos(i1, j1, k1);
               if (!p_452619_.hasNearbyAlivePlayer(i1, j1, k1, 7.0)
                  && p_452619_.isUnobstructed(zombie)
                  && p_452619_.noCollision(zombie)
                  && (zombie.canSpawnInLiquids() || !p_452619_.containsAnyLiquid(zombie.getBoundingBox()))) {
                  zombie.setTarget(livingentity);
                  zombie.finalizeSpawn(p_452619_, p_452619_.getCurrentDifficultyAt(zombie.blockPosition()), EntitySpawnReason.REINFORCEMENT, null);
                  p_452619_.addFreshEntityWithPassengers(zombie);
                  AttributeInstance attributeinstance = this.getAttribute(Attributes.SPAWN_REINFORCEMENTS_CHANCE);
                  AttributeModifier attributemodifier = attributeinstance.getModifier(REINFORCEMENT_CALLER_CHARGE_ID);
                  double d0 = attributemodifier != null ? attributemodifier.amount() : 0.0;
                  attributeinstance.removeModifier(REINFORCEMENT_CALLER_CHARGE_ID);
                  attributeinstance.addPermanentModifier(
                     new AttributeModifier(REINFORCEMENT_CALLER_CHARGE_ID, d0 - 0.05, AttributeModifier.Operation.ADD_VALUE)
                  );
                  zombie.getAttribute(Attributes.SPAWN_REINFORCEMENTS_CHANCE).addPermanentModifier(ZOMBIE_REINFORCEMENT_CALLEE_CHARGE);
                  break;
               }
            }
         }
      }

      return true;
   }

   @Override
   public boolean doHurtTarget(ServerLevel p_456599_, Entity p_451313_) {
      boolean flag = super.doHurtTarget(p_456599_, p_451313_);
      if (flag) {
         float f = p_456599_.getCurrentDifficultyAt(this.blockPosition()).getEffectiveDifficulty();
         if (this.getMainHandItem().isEmpty() && this.isOnFire() && this.random.nextFloat() < f * 0.3F) {
            p_451313_.igniteForSeconds(2 * (int)f);
         }
      }

      return flag;
   }

   @Override
   protected SoundEvent getAmbientSound() {
      return SoundEvents.ZOMBIE_AMBIENT;
   }

   @Override
   protected SoundEvent getHurtSound(DamageSource p_456726_) {
      return SoundEvents.ZOMBIE_HURT;
   }

   @Override
   protected SoundEvent getDeathSound() {
      return SoundEvents.ZOMBIE_DEATH;
   }

   protected SoundEvent getStepSound() {
      return SoundEvents.ZOMBIE_STEP;
   }

   @Override
   protected void playStepSound(BlockPos p_458858_, BlockState p_454407_) {
      this.playSound(this.getStepSound(), 0.15F, 1.0F);
   }

   @Override
   public EntityType<? extends Zombie> getType() {
      return (EntityType<? extends Zombie>)super.getType();
   }

   protected boolean canSpawnInLiquids() {
      return false;
   }

   @Override
   protected void populateDefaultEquipmentSlots(RandomSource p_453594_, DifficultyInstance p_459165_) {
      super.populateDefaultEquipmentSlots(p_453594_, p_459165_);
      if (p_453594_.nextFloat() < (this.level().getDifficulty() == Difficulty.HARD ? 0.05F : 0.01F)) {
         int i = p_453594_.nextInt(6);
         if (i == 0) {
            this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
         } else if (i == 1) {
            this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SPEAR));
         } else {
            this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SHOVEL));
         }
      }
   }

   @Override
   protected void addAdditionalSaveData(ValueOutput p_451299_) {
      super.addAdditionalSaveData(p_451299_);
      p_451299_.putBoolean("IsBaby", this.isBaby());
      p_451299_.putBoolean("CanBreakDoors", this.canBreakDoors());
      p_451299_.putInt("InWaterTime", this.isInWater() ? this.inWaterTime : -1);
      p_451299_.putInt("DrownedConversionTime", this.isUnderWaterConverting() ? this.conversionTime : -1);
   }

   @Override
   protected void readAdditionalSaveData(ValueInput p_458523_) {
      super.readAdditionalSaveData(p_458523_);
      this.setBaby(p_458523_.getBooleanOr("IsBaby", false));
      this.setCanBreakDoors(p_458523_.getBooleanOr("CanBreakDoors", false));
      this.inWaterTime = p_458523_.getIntOr("InWaterTime", 0);
      int i = p_458523_.getIntOr("DrownedConversionTime", -1);
      if (i != -1) {
         this.startUnderWaterConversion(i);
      } else {
         this.getEntityData().set(DATA_DROWNED_CONVERSION_ID, false);
      }
   }

   @Override
   public boolean killedEntity(ServerLevel p_451371_, LivingEntity p_457191_, DamageSource p_454331_) {
      boolean flag = super.killedEntity(p_451371_, p_457191_, p_454331_);
      if ((p_451371_.getDifficulty() == Difficulty.NORMAL || p_451371_.getDifficulty() == Difficulty.HARD) && p_457191_ instanceof Villager villager) {
         if (p_451371_.getDifficulty() != Difficulty.HARD && this.random.nextBoolean()) {
            return flag;
         }

         if (this.convertVillagerToZombieVillager(p_451371_, villager)) {
            flag = false;
         }
      }

      return flag;
   }

   @Override
   public EntityDimensions getDefaultDimensions(Pose p_454217_) {
      return this.isBaby() ? BABY_DIMENSIONS : super.getDefaultDimensions(p_454217_);
   }

   @Override
   public boolean canHoldItem(ItemStack p_451132_) {
      return p_451132_.is(ItemTags.EGGS) && this.isBaby() && this.isPassenger() ? false : super.canHoldItem(p_451132_);
   }

   @Override
   public boolean wantsToPickUp(ServerLevel p_459862_, ItemStack p_455614_) {
      return p_455614_.is(Items.GLOW_INK_SAC) ? false : super.wantsToPickUp(p_459862_, p_455614_);
   }

   @Override
   public @Nullable SpawnGroupData finalizeSpawn(
      ServerLevelAccessor p_454121_, DifficultyInstance p_450274_, EntitySpawnReason p_457483_, @Nullable SpawnGroupData p_459544_
   ) {
      RandomSource randomsource = p_454121_.getRandom();
      p_459544_ = super.finalizeSpawn(p_454121_, p_450274_, p_457483_, p_459544_);
      float f = p_450274_.getSpecialMultiplier();
      if (p_457483_ != EntitySpawnReason.CONVERSION) {
         this.setCanPickUpLoot(randomsource.nextFloat() < 0.55F * f);
      }

      if (p_459544_ == null) {
         p_459544_ = new Zombie.ZombieGroupData(getSpawnAsBabyOdds(randomsource), true);
      }

      if (p_459544_ instanceof Zombie.ZombieGroupData zombie$zombiegroupdata) {
         if (zombie$zombiegroupdata.isBaby) {
            this.setBaby(true);
            if (zombie$zombiegroupdata.canSpawnJockey) {
               if (randomsource.nextFloat() < 0.05) {
                  List<Chicken> list = p_454121_.getEntitiesOfClass(
                     Chicken.class, this.getBoundingBox().inflate(5.0, 3.0, 5.0), EntitySelector.ENTITY_NOT_BEING_RIDDEN
                  );
                  if (!list.isEmpty()) {
                     Chicken chicken = list.get(0);
                     chicken.setChickenJockey(true);
                     this.startRiding(chicken, false, false);
                  }
               } else if (randomsource.nextFloat() < 0.05) {
                  Chicken chicken1 = EntityType.CHICKEN.create(this.level(), EntitySpawnReason.JOCKEY);
                  if (chicken1 != null) {
                     chicken1.snapTo(this.getX(), this.getY(), this.getZ(), this.getYRot(), 0.0F);
                     chicken1.finalizeSpawn(p_454121_, p_450274_, EntitySpawnReason.JOCKEY, null);
                     chicken1.setChickenJockey(true);
                     this.startRiding(chicken1, false, false);
                     p_454121_.addFreshEntity(chicken1);
                  }
               }
            }
         }

         this.setCanBreakDoors(randomsource.nextFloat() < f * 0.1F);
         if (p_457483_ != EntitySpawnReason.CONVERSION) {
            this.populateDefaultEquipmentSlots(randomsource, p_450274_);
            this.populateDefaultEquipmentEnchantments(p_454121_, randomsource, p_450274_);
         }
      }

      if (this.getItemBySlot(EquipmentSlot.HEAD).isEmpty() && SpecialDates.isHalloween() && randomsource.nextFloat() < 0.25F) {
         this.setItemSlot(EquipmentSlot.HEAD, new ItemStack(randomsource.nextFloat() < 0.1F ? Blocks.JACK_O_LANTERN : Blocks.CARVED_PUMPKIN));
         this.setDropChance(EquipmentSlot.HEAD, 0.0F);
      }

      this.handleAttributes(f);
      return p_459544_;
   }

   @VisibleForTesting
   public void setInWaterTime(int p_451473_) {
      this.inWaterTime = p_451473_;
   }

   @VisibleForTesting
   public void setConversionTime(int p_452869_) {
      this.conversionTime = p_452869_;
   }

   public static boolean getSpawnAsBabyOdds(RandomSource p_452443_) {
      return p_452443_.nextFloat() < 0.05F;
   }

   protected void handleAttributes(float p_460592_) {
      this.randomizeReinforcementsChance();
      this.getAttribute(Attributes.KNOCKBACK_RESISTANCE)
         .addOrReplacePermanentModifier(new AttributeModifier(RANDOM_SPAWN_BONUS_ID, this.random.nextDouble() * 0.05F, AttributeModifier.Operation.ADD_VALUE));
      double d0 = this.random.nextDouble() * 1.5 * p_460592_;
      if (d0 > 1.0) {
         this.getAttribute(Attributes.FOLLOW_RANGE)
            .addOrReplacePermanentModifier(new AttributeModifier(ZOMBIE_RANDOM_SPAWN_BONUS_ID, d0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
      }

      if (this.random.nextFloat() < p_460592_ * 0.05F) {
         this.getAttribute(Attributes.SPAWN_REINFORCEMENTS_CHANCE)
            .addOrReplacePermanentModifier(
               new AttributeModifier(LEADER_ZOMBIE_BONUS_ID, this.random.nextDouble() * 0.25 + 0.5, AttributeModifier.Operation.ADD_VALUE)
            );
         this.getAttribute(Attributes.MAX_HEALTH)
            .addOrReplacePermanentModifier(
               new AttributeModifier(LEADER_ZOMBIE_BONUS_ID, this.random.nextDouble() * 3.0 + 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
            );
         this.setCanBreakDoors(true);
      }
   }

   protected void randomizeReinforcementsChance() {
      this.getAttribute(Attributes.SPAWN_REINFORCEMENTS_CHANCE).setBaseValue(this.random.nextDouble() * 0.1F);
   }

   class ZombieAttackTurtleEggGoal extends RemoveBlockGoal {
      ZombieAttackTurtleEggGoal(final PathfinderMob p_459917_, final double p_454531_, final int p_457131_) {
         super(Blocks.TURTLE_EGG, p_459917_, p_454531_, p_457131_);
      }

      @Override
      public void playDestroyProgressSound(LevelAccessor p_454663_, BlockPos p_458578_) {
         p_454663_.playSound(null, p_458578_, SoundEvents.ZOMBIE_DESTROY_EGG, SoundSource.HOSTILE, 0.5F, 0.9F + Zombie.this.random.nextFloat() * 0.2F);
      }

      @Override
      public void playBreakSound(Level p_456034_, BlockPos p_451363_) {
         p_456034_.playSound(null, p_451363_, SoundEvents.TURTLE_EGG_BREAK, SoundSource.BLOCKS, 0.7F, 0.9F + p_456034_.random.nextFloat() * 0.2F);
      }

      @Override
      public double acceptedDistance() {
         return 1.14;
      }
   }

   public static class ZombieGroupData implements SpawnGroupData {
      public final boolean isBaby;
      public final boolean canSpawnJockey;

      public ZombieGroupData(boolean p_457951_, boolean p_454427_) {
         this.isBaby = p_457951_;
         this.canSpawnJockey = p_454427_;
      }
   }
}
