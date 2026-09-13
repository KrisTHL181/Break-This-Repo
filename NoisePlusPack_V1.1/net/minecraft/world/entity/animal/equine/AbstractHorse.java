package net.minecraft.world.entity.animal.equine;

import com.google.common.collect.UnmodifiableIterator;
import java.util.function.DoubleSupplier;
import java.util.function.IntUnaryOperator;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HasCustomInventoryScreen;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.PlayerRideableJumping;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowParentGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStandGoal;
import net.minecraft.world.entity.ai.goal.RunAroundLikeCrazyGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.DismountHelper;
import net.minecraft.world.inventory.AbstractMountInventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public abstract class AbstractHorse extends Animal implements HasCustomInventoryScreen, OwnableEntity, PlayerRideableJumping {
   public static final int CHEST_SLOT_OFFSET = 499;
   public static final int INVENTORY_SLOT_OFFSET = 500;
   public static final double BREEDING_CROSS_FACTOR = 0.15;
   private static final float MIN_MOVEMENT_SPEED = (float)generateSpeed(() -> 0.0);
   private static final float MAX_MOVEMENT_SPEED = (float)generateSpeed(() -> 1.0);
   private static final float MIN_JUMP_STRENGTH = (float)generateJumpStrength(() -> 0.0);
   private static final float MAX_JUMP_STRENGTH = (float)generateJumpStrength(() -> 1.0);
   private static final float MIN_HEALTH = generateMaxHealth(p_459031_ -> 0);
   private static final float MAX_HEALTH = generateMaxHealth(p_455972_ -> p_455972_ - 1);
   private static final float BACKWARDS_MOVE_SPEED_FACTOR = 0.25F;
   private static final float SIDEWAYS_MOVE_SPEED_FACTOR = 0.5F;
   private static final TargetingConditions.Selector PARENT_HORSE_SELECTOR = (p_455065_, p_450647_) -> p_455065_ instanceof AbstractHorse abstracthorse
      && abstracthorse.isBred();
   private static final TargetingConditions MOMMY_TARGETING = TargetingConditions.forNonCombat()
      .range(16.0)
      .ignoreLineOfSight()
      .selector(PARENT_HORSE_SELECTOR);
   private static final EntityDataAccessor<Byte> DATA_ID_FLAGS = SynchedEntityData.defineId(AbstractHorse.class, EntityDataSerializers.BYTE);
   private static final int FLAG_TAME = 2;
   private static final int FLAG_BRED = 8;
   private static final int FLAG_EATING = 16;
   private static final int FLAG_STANDING = 32;
   private static final int FLAG_OPEN_MOUTH = 64;
   public static final int INVENTORY_ROWS = 3;
   private static final int DEFAULT_TEMPER = 0;
   private static final boolean DEFAULT_EATING_HAYSTACK = false;
   private static final boolean DEFAULT_BRED = false;
   private static final boolean DEFAULT_TAME = false;
   private int eatingCounter;
   private int mouthCounter;
   private int standCounter;
   public int tailCounter;
   public int sprintCounter;
   protected SimpleContainer inventory;
   protected int temper = 0;
   protected float playerJumpPendingScale;
   protected boolean allowStandSliding;
   private float eatAnim;
   private float eatAnimO;
   private float standAnim;
   private float standAnimO;
   private float mouthAnim;
   private float mouthAnimO;
   protected boolean canGallop = true;
   protected int gallopSoundCounter;
   private @Nullable EntityReference<LivingEntity> owner;

   protected AbstractHorse(EntityType<? extends AbstractHorse> p_450328_, Level p_453674_) {
      super(p_450328_, p_453674_);
      this.createInventory();
   }

   @Override
   protected void registerGoals() {
      this.goalSelector.addGoal(1, new AbstractHorse.MountPanicGoal(1.2));
      this.goalSelector.addGoal(1, new RunAroundLikeCrazyGoal(this, 1.2));
      this.goalSelector.addGoal(2, new BreedGoal(this, 1.0, AbstractHorse.class));
      this.goalSelector.addGoal(4, new FollowParentGoal(this, 1.0));
      this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.7));
      this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 6.0F));
      this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
      if (this.canPerformRearing()) {
         this.goalSelector.addGoal(9, new RandomStandGoal(this));
      }

      this.addBehaviourGoals();
   }

   protected void addBehaviourGoals() {
      this.goalSelector.addGoal(0, new FloatGoal(this));
      this.goalSelector.addGoal(3, new TemptGoal(this, 1.25, p_451750_ -> p_451750_.is(ItemTags.HORSE_TEMPT_ITEMS), false));
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder p_454042_) {
      super.defineSynchedData(p_454042_);
      p_454042_.define(DATA_ID_FLAGS, (byte)0);
   }

   protected boolean getFlag(int p_457333_) {
      return (this.entityData.get(DATA_ID_FLAGS) & p_457333_) != 0;
   }

   protected void setFlag(int p_453080_, boolean p_460095_) {
      byte b0 = this.entityData.get(DATA_ID_FLAGS);
      if (p_460095_) {
         this.entityData.set(DATA_ID_FLAGS, (byte)(b0 | p_453080_));
      } else {
         this.entityData.set(DATA_ID_FLAGS, (byte)(b0 & ~p_453080_));
      }
   }

   public boolean isTamed() {
      return this.getFlag(2);
   }

   @Override
   public @Nullable EntityReference<LivingEntity> getOwnerReference() {
      return this.owner;
   }

   public void setOwner(@Nullable LivingEntity p_455828_) {
      this.owner = EntityReference.of(p_455828_);
   }

   public void setTamed(boolean p_451905_) {
      this.setFlag(2, p_451905_);
   }

   @Override
   public void onElasticLeashPull() {
      super.onElasticLeashPull();
      if (this.isEating()) {
         this.setEating(false);
      }
   }

   @Override
   public boolean supportQuadLeash() {
      return true;
   }

   @Override
   public Vec3[] getQuadLeashOffsets() {
      return Leashable.createQuadLeashOffsets(this, 0.04, 0.52, 0.23, 0.87);
   }

   public boolean isEating() {
      return this.getFlag(16);
   }

   public boolean isStanding() {
      return this.getFlag(32);
   }

   public boolean isBred() {
      return this.getFlag(8);
   }

   public void setBred(boolean p_458838_) {
      this.setFlag(8, p_458838_);
   }

   @Override
   public boolean canUseSlot(EquipmentSlot p_460425_) {
      return p_460425_ != EquipmentSlot.SADDLE ? super.canUseSlot(p_460425_) : this.isAlive() && !this.isBaby() && this.isTamed();
   }

   public void equipBodyArmor(Player p_459711_, ItemStack p_452005_) {
      if (this.isEquippableInSlot(p_452005_, EquipmentSlot.BODY)) {
         this.setBodyArmorItem(p_452005_.consumeAndReturn(1, p_459711_));
      }
   }

   @Override
   protected boolean canDispenserEquipIntoSlot(EquipmentSlot p_453167_) {
      return (p_453167_ == EquipmentSlot.BODY || p_453167_ == EquipmentSlot.SADDLE) && this.isTamed() || super.canDispenserEquipIntoSlot(p_453167_);
   }

   public int getTemper() {
      return this.temper;
   }

   public void setTemper(int p_453741_) {
      this.temper = p_453741_;
   }

   public int modifyTemper(int p_461013_) {
      int i = Mth.clamp(this.getTemper() + p_461013_, 0, this.getMaxTemper());
      this.setTemper(i);
      return i;
   }

   @Override
   public boolean isPushable() {
      return !this.isVehicle();
   }

   private void eating() {
      this.openMouth();
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
   }

   @Override
   public boolean causeFallDamage(double p_454114_, float p_458836_, DamageSource p_450439_) {
      if (p_454114_ > 1.0) {
         this.playSound(SoundEvents.HORSE_LAND, 0.4F, 1.0F);
      }

      int i = this.calculateFallDamage(p_454114_, p_458836_);
      if (i <= 0) {
         return false;
      }

      this.hurt(p_450439_, i);
      this.propagateFallToPassengers(p_454114_, p_458836_, p_450439_);
      this.playBlockFallSound();
      return true;
   }

   public final int getInventorySize() {
      return AbstractMountInventoryMenu.getInventorySize(this.getInventoryColumns());
   }

   protected void createInventory() {
      SimpleContainer simplecontainer = this.inventory;
      this.inventory = new SimpleContainer(this.getInventorySize());
      if (simplecontainer != null) {
         int i = Math.min(simplecontainer.getContainerSize(), this.inventory.getContainerSize());

         for (int j = 0; j < i; j++) {
            ItemStack itemstack = simplecontainer.getItem(j);
            if (!itemstack.isEmpty()) {
               this.inventory.setItem(j, itemstack.copy());
            }
         }
      }
   }

   @Override
   protected Holder<SoundEvent> getEquipSound(EquipmentSlot p_455969_, ItemStack p_455433_, Equippable p_456229_) {
      return p_455969_ == EquipmentSlot.SADDLE ? SoundEvents.HORSE_SADDLE : super.getEquipSound(p_455969_, p_455433_, p_456229_);
   }

   @Override
   public boolean hurtServer(ServerLevel p_459339_, DamageSource p_455182_, float p_452865_) {
      boolean flag = super.hurtServer(p_459339_, p_455182_, p_452865_);
      if (flag && this.random.nextInt(3) == 0) {
         this.standIfPossible();
      }

      return flag;
   }

   protected boolean canPerformRearing() {
      return true;
   }

   protected @Nullable SoundEvent getEatingSound() {
      return null;
   }

   protected @Nullable SoundEvent getAngrySound() {
      return null;
   }

   @Override
   protected void playStepSound(BlockPos p_457686_, BlockState p_459257_) {
      if (!p_459257_.liquid()) {
         BlockState blockstate = this.level().getBlockState(p_457686_.above());
         SoundType soundtype = p_459257_.getSoundType();
         if (blockstate.is(Blocks.SNOW)) {
            soundtype = blockstate.getSoundType();
         }

         if (this.isVehicle() && this.canGallop) {
            this.gallopSoundCounter++;
            if (this.gallopSoundCounter > 5 && this.gallopSoundCounter % 3 == 0) {
               this.playGallopSound(soundtype);
            } else if (this.gallopSoundCounter <= 5) {
               this.playSound(SoundEvents.HORSE_STEP_WOOD, soundtype.getVolume() * 0.15F, soundtype.getPitch());
            }
         } else if (this.isWoodSoundType(soundtype)) {
            this.playSound(SoundEvents.HORSE_STEP_WOOD, soundtype.getVolume() * 0.15F, soundtype.getPitch());
         } else {
            this.playSound(SoundEvents.HORSE_STEP, soundtype.getVolume() * 0.15F, soundtype.getPitch());
         }
      }
   }

   private boolean isWoodSoundType(SoundType p_453926_) {
      return p_453926_ == SoundType.WOOD
         || p_453926_ == SoundType.NETHER_WOOD
         || p_453926_ == SoundType.STEM
         || p_453926_ == SoundType.CHERRY_WOOD
         || p_453926_ == SoundType.BAMBOO_WOOD;
   }

   protected void playGallopSound(SoundType p_454858_) {
      this.playSound(SoundEvents.HORSE_GALLOP, p_454858_.getVolume() * 0.15F, p_454858_.getPitch());
   }

   public static AttributeSupplier.Builder createBaseHorseAttributes() {
      return Animal.createAnimalAttributes()
         .add(Attributes.JUMP_STRENGTH, 0.7)
         .add(Attributes.MAX_HEALTH, 53.0)
         .add(Attributes.MOVEMENT_SPEED, 0.225F)
         .add(Attributes.STEP_HEIGHT, 1.0)
         .add(Attributes.SAFE_FALL_DISTANCE, 6.0)
         .add(Attributes.FALL_DAMAGE_MULTIPLIER, 0.5);
   }

   @Override
   public int getMaxSpawnClusterSize() {
      return 6;
   }

   public int getMaxTemper() {
      return 100;
   }

   @Override
   protected float getSoundVolume() {
      return 0.8F;
   }

   @Override
   public int getAmbientSoundInterval() {
      return 400;
   }

   @Override
   public void openCustomInventoryScreen(Player p_458597_) {
      if (!this.level().isClientSide() && (!this.isVehicle() || this.hasPassenger(p_458597_)) && this.isTamed()) {
         p_458597_.openHorseInventory(this, this.inventory);
      }
   }

   public InteractionResult fedFood(Player p_457822_, ItemStack p_458672_) {
      boolean flag = this.handleEating(p_457822_, p_458672_);
      if (flag) {
         p_458672_.consume(1, p_457822_);
      }

      return (InteractionResult)(!flag && !this.level().isClientSide() ? InteractionResult.PASS : InteractionResult.SUCCESS_SERVER);
   }

   protected boolean handleEating(Player p_458677_, ItemStack p_457201_) {
      boolean flag = false;
      float f = 0.0F;
      int i = 0;
      int j = 0;
      if (p_457201_.is(Items.WHEAT)) {
         f = 2.0F;
         i = 20;
         j = 3;
      } else if (p_457201_.is(Items.SUGAR)) {
         f = 1.0F;
         i = 30;
         j = 3;
      } else if (p_457201_.is(Blocks.HAY_BLOCK.asItem())) {
         f = 20.0F;
         i = 180;
      } else if (p_457201_.is(Items.APPLE)) {
         f = 3.0F;
         i = 60;
         j = 3;
      } else if (p_457201_.is(Items.RED_MUSHROOM)) {
         f = 3.0F;
         i = 0;
         j = 3;
      } else if (p_457201_.is(Items.CARROT)) {
         f = 3.0F;
         i = 60;
         j = 3;
      } else if (p_457201_.is(Items.GOLDEN_CARROT)) {
         f = 4.0F;
         i = 60;
         j = 5;
         if (!this.level().isClientSide() && this.isTamed() && this.getAge() == 0 && !this.isInLove()) {
            flag = true;
            this.setInLove(p_458677_);
         }
      } else if (p_457201_.is(Items.GOLDEN_APPLE) || p_457201_.is(Items.ENCHANTED_GOLDEN_APPLE)) {
         f = 10.0F;
         i = 240;
         j = 10;
         if (!this.level().isClientSide() && this.isTamed() && this.getAge() == 0 && !this.isInLove()) {
            flag = true;
            this.setInLove(p_458677_);
         }
      }

      if (this.getHealth() < this.getMaxHealth() && f > 0.0F) {
         this.heal(f);
         flag = true;
      }

      if (this.isBaby() && i > 0) {
         this.level().addParticle(ParticleTypes.HAPPY_VILLAGER, this.getRandomX(1.0), this.getRandomY() + 0.5, this.getRandomZ(1.0), 0.0, 0.0, 0.0);
         if (!this.level().isClientSide()) {
            this.ageUp(i);
            flag = true;
         }
      }

      if (j > 0 && (flag || !this.isTamed()) && this.getTemper() < this.getMaxTemper() && !this.level().isClientSide()) {
         this.modifyTemper(j);
         flag = true;
      }

      if (flag) {
         this.eating();
         this.gameEvent(GameEvent.EAT);
      }

      return flag;
   }

   protected void doPlayerRide(Player p_453522_) {
      this.setEating(false);
      this.clearStanding();
      if (!this.level().isClientSide()) {
         p_453522_.setYRot(this.getYRot());
         p_453522_.setXRot(this.getXRot());
         p_453522_.startRiding(this);
      }
   }

   @Override
   public boolean isImmobile() {
      return super.isImmobile() && this.isVehicle() && this.isSaddled() || this.isEating() || this.isStanding();
   }

   @Override
   public boolean isFood(ItemStack p_454409_) {
      return p_454409_.is(ItemTags.HORSE_FOOD);
   }

   private void moveTail() {
      this.tailCounter = 1;
   }

   @Override
   protected void dropEquipment(ServerLevel p_452750_) {
      super.dropEquipment(p_452750_);
      if (this.inventory != null) {
         for (int i = 0; i < this.inventory.getContainerSize(); i++) {
            ItemStack itemstack = this.inventory.getItem(i);
            if (!itemstack.isEmpty() && !EnchantmentHelper.has(itemstack, EnchantmentEffectComponents.PREVENT_EQUIPMENT_DROP)) {
               this.spawnAtLocation(p_452750_, itemstack);
            }
         }
      }
   }

   @Override
   public void aiStep() {
      if (this.random.nextInt(200) == 0) {
         this.moveTail();
      }

      super.aiStep();
      if (this.level() instanceof ServerLevel serverlevel && this.isAlive()) {
         if (this.random.nextInt(900) == 0 && this.deathTime == 0) {
            this.heal(1.0F);
         }

         if (this.canEatGrass()) {
            if (!this.isEating()
               && !this.isVehicle()
               && this.random.nextInt(300) == 0
               && serverlevel.getBlockState(this.blockPosition().below()).is(Blocks.GRASS_BLOCK)) {
               this.setEating(true);
            }

            if (this.isEating() && ++this.eatingCounter > 50) {
               this.eatingCounter = 0;
               this.setEating(false);
            }
         }

         this.followMommy(serverlevel);
      }
   }

   protected void followMommy(ServerLevel p_453735_) {
      if (this.isBred() && this.isBaby() && !this.isEating()) {
         LivingEntity livingentity = p_453735_.getNearestEntity(
            AbstractHorse.class, MOMMY_TARGETING, this, this.getX(), this.getY(), this.getZ(), this.getBoundingBox().inflate(16.0)
         );
         if (livingentity != null && this.distanceToSqr(livingentity) > 4.0) {
            this.navigation.createPath(livingentity, 0);
         }
      }
   }

   public boolean canEatGrass() {
      return true;
   }

   @Override
   public void tick() {
      super.tick();
      if (this.mouthCounter > 0 && ++this.mouthCounter > 30) {
         this.mouthCounter = 0;
         this.setFlag(64, false);
      }

      if (this.standCounter > 0 && --this.standCounter <= 0) {
         this.clearStanding();
      }

      if (this.tailCounter > 0 && ++this.tailCounter > 8) {
         this.tailCounter = 0;
      }

      if (this.sprintCounter > 0) {
         this.sprintCounter++;
         if (this.sprintCounter > 300) {
            this.sprintCounter = 0;
         }
      }

      this.eatAnimO = this.eatAnim;
      if (this.isEating()) {
         this.eatAnim = this.eatAnim + ((1.0F - this.eatAnim) * 0.4F + 0.05F);
         if (this.eatAnim > 1.0F) {
            this.eatAnim = 1.0F;
         }
      } else {
         this.eatAnim = this.eatAnim + ((0.0F - this.eatAnim) * 0.4F - 0.05F);
         if (this.eatAnim < 0.0F) {
            this.eatAnim = 0.0F;
         }
      }

      this.standAnimO = this.standAnim;
      if (this.isStanding()) {
         this.eatAnim = 0.0F;
         this.eatAnimO = this.eatAnim;
         this.standAnim = this.standAnim + ((1.0F - this.standAnim) * 0.4F + 0.05F);
         if (this.standAnim > 1.0F) {
            this.standAnim = 1.0F;
         }
      } else {
         this.allowStandSliding = false;
         this.standAnim = this.standAnim + ((0.8F * this.standAnim * this.standAnim * this.standAnim - this.standAnim) * 0.6F - 0.05F);
         if (this.standAnim < 0.0F) {
            this.standAnim = 0.0F;
         }
      }

      this.mouthAnimO = this.mouthAnim;
      if (this.getFlag(64)) {
         this.mouthAnim = this.mouthAnim + ((1.0F - this.mouthAnim) * 0.7F + 0.05F);
         if (this.mouthAnim > 1.0F) {
            this.mouthAnim = 1.0F;
         }
      } else {
         this.mouthAnim = this.mouthAnim + ((0.0F - this.mouthAnim) * 0.7F - 0.05F);
         if (this.mouthAnim < 0.0F) {
            this.mouthAnim = 0.0F;
         }
      }
   }

   @Override
   public InteractionResult mobInteract(Player p_460853_, InteractionHand p_450811_) {
      if (this.isVehicle() || this.isBaby()) {
         return super.mobInteract(p_460853_, p_450811_);
      }

      if (this.isTamed() && p_460853_.isSecondaryUseActive()) {
         this.openCustomInventoryScreen(p_460853_);
         return InteractionResult.SUCCESS;
      }

      ItemStack itemstack = p_460853_.getItemInHand(p_450811_);
      if (!itemstack.isEmpty()) {
         InteractionResult interactionresult = itemstack.interactLivingEntity(p_460853_, this, p_450811_);
         if (interactionresult.consumesAction()) {
            return interactionresult;
         }

         if (this.isEquippableInSlot(itemstack, EquipmentSlot.BODY) && !this.isWearingBodyArmor()) {
            this.equipBodyArmor(p_460853_, itemstack);
            return InteractionResult.SUCCESS;
         }
      }

      this.doPlayerRide(p_460853_);
      return InteractionResult.SUCCESS;
   }

   private void openMouth() {
      if (!this.level().isClientSide()) {
         this.mouthCounter = 1;
         this.setFlag(64, true);
      }
   }

   public void setEating(boolean p_454511_) {
      this.setFlag(16, p_454511_);
   }

   public void setStanding(int p_457111_) {
      this.setEating(false);
      this.setFlag(32, true);
      this.standCounter = p_457111_;
   }

   public void clearStanding() {
      this.setFlag(32, false);
      this.standCounter = 0;
   }

   public @Nullable SoundEvent getAmbientStandSound() {
      return this.getAmbientSound();
   }

   public void standIfPossible() {
      if (this.canPerformRearing() && (this.isEffectiveAi() || !this.level().isClientSide())) {
         this.setStanding(20);
      }
   }

   public void makeMad() {
      if (!this.isStanding() && !this.level().isClientSide()) {
         this.standIfPossible();
         this.makeSound(this.getAngrySound());
      }
   }

   public boolean tameWithName(Player p_460124_) {
      this.setOwner(p_460124_);
      this.setTamed(true);
      if (p_460124_ instanceof ServerPlayer) {
         CriteriaTriggers.TAME_ANIMAL.trigger((ServerPlayer)p_460124_, this);
      }

      this.level().broadcastEntityEvent(this, (byte)7);
      return true;
   }

   @Override
   protected void tickRidden(Player p_450804_, Vec3 p_451297_) {
      super.tickRidden(p_450804_, p_451297_);
      Vec2 vec2 = this.getRiddenRotation(p_450804_);
      this.setRot(vec2.y, vec2.x);
      this.yRotO = this.yBodyRot = this.yHeadRot = this.getYRot();
      if (this.isLocalInstanceAuthoritative()) {
         if (p_451297_.z <= 0.0) {
            this.gallopSoundCounter = 0;
         }

         if (this.onGround()) {
            if (this.playerJumpPendingScale > 0.0F && !this.isJumping()) {
               this.executeRidersJump(this.playerJumpPendingScale, p_451297_);
            }

            this.playerJumpPendingScale = 0.0F;
         }
      }
   }

   protected Vec2 getRiddenRotation(LivingEntity p_452212_) {
      return new Vec2(p_452212_.getXRot() * 0.5F, p_452212_.getYRot());
   }

   @Override
   protected void addPassenger(Entity p_458224_) {
      super.addPassenger(p_458224_);
      p_458224_.absSnapRotationTo(this.getViewYRot(0.0F), this.getViewXRot(0.0F));
   }

   @Override
   protected Vec3 getRiddenInput(Player p_458944_, Vec3 p_453471_) {
      if (this.onGround() && this.playerJumpPendingScale == 0.0F && this.isStanding() && !this.allowStandSliding) {
         return Vec3.ZERO;
      }

      float f = p_458944_.xxa * 0.5F;
      float f1 = p_458944_.zza;
      if (f1 <= 0.0F) {
         f1 *= 0.25F;
      }

      return new Vec3(f, 0.0, f1);
   }

   @Override
   protected float getRiddenSpeed(Player p_450944_) {
      return (float)this.getAttributeValue(Attributes.MOVEMENT_SPEED);
   }

   protected void executeRidersJump(float p_452386_, Vec3 p_450466_) {
      double d0 = this.getJumpPower(p_452386_);
      Vec3 vec3 = this.getDeltaMovement();
      this.setDeltaMovement(vec3.x, d0, vec3.z);
      this.needsSync = true;
      if (p_450466_.z > 0.0) {
         float f = Mth.sin(this.getYRot() * (float) (Math.PI / 180.0));
         float f1 = Mth.cos(this.getYRot() * (float) (Math.PI / 180.0));
         this.setDeltaMovement(this.getDeltaMovement().add(-0.4F * f * p_452386_, 0.0, 0.4F * f1 * p_452386_));
      }
   }

   protected void playJumpSound() {
      this.playSound(SoundEvents.HORSE_JUMP, 0.4F, 1.0F);
   }

   @Override
   protected void addAdditionalSaveData(ValueOutput p_456413_) {
      super.addAdditionalSaveData(p_456413_);
      p_456413_.putBoolean("EatingHaystack", this.isEating());
      p_456413_.putBoolean("Bred", this.isBred());
      p_456413_.putInt("Temper", this.getTemper());
      p_456413_.putBoolean("Tame", this.isTamed());
      EntityReference.store(this.owner, p_456413_, "Owner");
   }

   @Override
   protected void readAdditionalSaveData(ValueInput p_458457_) {
      super.readAdditionalSaveData(p_458457_);
      this.setEating(p_458457_.getBooleanOr("EatingHaystack", false));
      this.setBred(p_458457_.getBooleanOr("Bred", false));
      this.setTemper(p_458457_.getIntOr("Temper", 0));
      this.setTamed(p_458457_.getBooleanOr("Tame", false));
      this.owner = EntityReference.readWithOldOwnerConversion(p_458457_, "Owner", this.level());
   }

   @Override
   public boolean canMate(Animal p_454620_) {
      return false;
   }

   protected boolean canParent() {
      return !this.isVehicle() && !this.isPassenger() && this.isTamed() && !this.isBaby() && this.getHealth() >= this.getMaxHealth() && this.isInLove();
   }

   public boolean isMobControlled() {
      return false;
   }

   @Override
   public @Nullable AgeableMob getBreedOffspring(ServerLevel p_458923_, AgeableMob p_452837_) {
      return null;
   }

   protected void setOffspringAttributes(AgeableMob p_459309_, AbstractHorse p_451325_) {
      this.setOffspringAttribute(p_459309_, p_451325_, Attributes.MAX_HEALTH, MIN_HEALTH, MAX_HEALTH);
      this.setOffspringAttribute(p_459309_, p_451325_, Attributes.JUMP_STRENGTH, MIN_JUMP_STRENGTH, MAX_JUMP_STRENGTH);
      this.setOffspringAttribute(p_459309_, p_451325_, Attributes.MOVEMENT_SPEED, MIN_MOVEMENT_SPEED, MAX_MOVEMENT_SPEED);
   }

   private void setOffspringAttribute(AgeableMob p_456077_, AbstractHorse p_452584_, Holder<Attribute> p_453385_, double p_456196_, double p_460692_) {
      double d0 = createOffspringAttribute(this.getAttributeBaseValue(p_453385_), p_456077_.getAttributeBaseValue(p_453385_), p_456196_, p_460692_, this.random);
      p_452584_.getAttribute(p_453385_).setBaseValue(d0);
   }

   static double createOffspringAttribute(double p_453132_, double p_455194_, double p_458661_, double p_453138_, RandomSource p_451435_) {
      if (p_453138_ <= p_458661_) {
         throw new IllegalArgumentException("Incorrect range for an attribute");
      } else {
         p_453132_ = Mth.clamp(p_453132_, p_458661_, p_453138_);
         p_455194_ = Mth.clamp(p_455194_, p_458661_, p_453138_);
         double d0 = 0.15 * (p_453138_ - p_458661_);
         double d1 = Math.abs(p_453132_ - p_455194_) + d0 * 2.0;
         double d2 = (p_453132_ + p_455194_) / 2.0;
         double d3 = (p_451435_.nextDouble() + p_451435_.nextDouble() + p_451435_.nextDouble()) / 3.0 - 0.5;
         double d4 = d2 + d1 * d3;
         if (d4 > p_453138_) {
            double d6 = d4 - p_453138_;
            return p_453138_ - d6;
         } else if (d4 < p_458661_) {
            double d5 = p_458661_ - d4;
            return p_458661_ + d5;
         } else {
            return d4;
         }
      }
   }

   public float getEatAnim(float p_456728_) {
      return Mth.lerp(p_456728_, this.eatAnimO, this.eatAnim);
   }

   public float getStandAnim(float p_452208_) {
      return Mth.lerp(p_452208_, this.standAnimO, this.standAnim);
   }

   public float getMouthAnim(float p_459054_) {
      return Mth.lerp(p_459054_, this.mouthAnimO, this.mouthAnim);
   }

   @Override
   public void onPlayerJump(int p_451314_) {
      if (this.isSaddled()) {
         if (p_451314_ < 0) {
            p_451314_ = 0;
         } else {
            this.allowStandSliding = true;
            this.standIfPossible();
         }

         this.playerJumpPendingScale = this.getPlayerJumpPendingScale(p_451314_);
      }
   }

   @Override
   public boolean canJump() {
      return this.isSaddled();
   }

   @Override
   public void handleStartJump(int p_458894_) {
      this.allowStandSliding = true;
      this.standIfPossible();
      this.playJumpSound();
   }

   @Override
   public void handleStopJump() {
   }

   protected void spawnTamingParticles(boolean p_454990_) {
      ParticleOptions particleoptions = p_454990_ ? ParticleTypes.HEART : ParticleTypes.SMOKE;

      for (int i = 0; i < 7; i++) {
         double d0 = this.random.nextGaussian() * 0.02;
         double d1 = this.random.nextGaussian() * 0.02;
         double d2 = this.random.nextGaussian() * 0.02;
         this.level().addParticle(particleoptions, this.getRandomX(1.0), this.getRandomY() + 0.5, this.getRandomZ(1.0), d0, d1, d2);
      }
   }

   @Override
   public void handleEntityEvent(byte p_455931_) {
      if (p_455931_ == 7) {
         this.spawnTamingParticles(true);
      } else if (p_455931_ == 6) {
         this.spawnTamingParticles(false);
      } else {
         super.handleEntityEvent(p_455931_);
      }
   }

   @Override
   protected void positionRider(Entity p_453567_, Entity.MoveFunction p_459403_) {
      super.positionRider(p_453567_, p_459403_);
      if (p_453567_ instanceof LivingEntity) {
         ((LivingEntity)p_453567_).yBodyRot = this.yBodyRot;
      }
   }

   protected static float generateMaxHealth(IntUnaryOperator p_450650_) {
      return 15.0F + p_450650_.applyAsInt(8) + p_450650_.applyAsInt(9);
   }

   protected static double generateJumpStrength(DoubleSupplier p_452712_) {
      return 0.4F + p_452712_.getAsDouble() * 0.2 + p_452712_.getAsDouble() * 0.2 + p_452712_.getAsDouble() * 0.2;
   }

   protected static double generateSpeed(DoubleSupplier p_452700_) {
      return (0.45F + p_452700_.getAsDouble() * 0.3 + p_452700_.getAsDouble() * 0.3 + p_452700_.getAsDouble() * 0.3) * 0.25;
   }

   @Override
   public boolean onClimbable() {
      return false;
   }

   @Override
   public @Nullable SlotAccess getSlot(int p_458067_) {
      int i = p_458067_ - 500;
      return i >= 0 && i < this.inventory.getContainerSize() ? this.inventory.getSlot(i) : super.getSlot(p_458067_);
   }

   @Override
   public @Nullable LivingEntity getControllingPassenger() {
      return this.isSaddled() && this.getFirstPassenger() instanceof Player player ? player : super.getControllingPassenger();
   }

   private @Nullable Vec3 getDismountLocationInDirection(Vec3 p_456954_, LivingEntity p_458407_) {
      double d0 = this.getX() + p_456954_.x;
      double d1 = this.getBoundingBox().minY;
      double d2 = this.getZ() + p_456954_.z;
      BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();
      UnmodifiableIterator var10 = p_458407_.getDismountPoses().iterator();

      while (var10.hasNext()) {
         Pose pose = (Pose)var10.next();
         blockpos$mutableblockpos.set(d0, d1, d2);
         double d3 = this.getBoundingBox().maxY + 0.75;

         do {
            double d4 = this.level().getBlockFloorHeight(blockpos$mutableblockpos);
            if (blockpos$mutableblockpos.getY() + d4 > d3) {
               break;
            }

            if (DismountHelper.isBlockFloorValid(d4)) {
               AABB aabb = p_458407_.getLocalBoundsForPose(pose);
               Vec3 vec3 = new Vec3(d0, blockpos$mutableblockpos.getY() + d4, d2);
               if (DismountHelper.canDismountTo(this.level(), p_458407_, aabb.move(vec3))) {
                  p_458407_.setPose(pose);
                  return vec3;
               }
            }

            blockpos$mutableblockpos.move(Direction.UP);
         } while (blockpos$mutableblockpos.getY() < d3);
      }

      return null;
   }

   @Override
   public Vec3 getDismountLocationForPassenger(LivingEntity p_457634_) {
      Vec3 vec3 = getCollisionHorizontalEscapeVector(
         this.getBbWidth(), p_457634_.getBbWidth(), this.getYRot() + (p_457634_.getMainArm() == HumanoidArm.RIGHT ? 90.0F : -90.0F)
      );
      Vec3 vec31 = this.getDismountLocationInDirection(vec3, p_457634_);
      if (vec31 != null) {
         return vec31;
      }

      Vec3 vec32 = getCollisionHorizontalEscapeVector(
         this.getBbWidth(), p_457634_.getBbWidth(), this.getYRot() + (p_457634_.getMainArm() == HumanoidArm.LEFT ? 90.0F : -90.0F)
      );
      Vec3 vec33 = this.getDismountLocationInDirection(vec32, p_457634_);
      return vec33 != null ? vec33 : this.position();
   }

   protected void randomizeAttributes(RandomSource p_454146_) {
   }

   @Override
   public @Nullable SpawnGroupData finalizeSpawn(
      ServerLevelAccessor p_454930_, DifficultyInstance p_453210_, EntitySpawnReason p_459316_, @Nullable SpawnGroupData p_452427_
   ) {
      if (p_452427_ == null) {
         p_452427_ = new AgeableMob.AgeableMobGroupData(0.2F);
      }

      this.randomizeAttributes(p_454930_.getRandom());
      return super.finalizeSpawn(p_454930_, p_453210_, p_459316_, p_452427_);
   }

   public boolean hasInventoryChanged(Container p_457588_) {
      return this.inventory != p_457588_;
   }

   public int getAmbientStandInterval() {
      return this.getAmbientSoundInterval();
   }

   @Override
   protected Vec3 getPassengerAttachmentPoint(Entity p_458105_, EntityDimensions p_453247_, float p_460133_) {
      return super.getPassengerAttachmentPoint(p_458105_, p_453247_, p_460133_)
         .add(new Vec3(0.0, 0.15 * this.standAnimO * p_460133_, -0.7 * this.standAnimO * p_460133_).yRot(-this.getYRot() * (float) (Math.PI / 180.0)));
   }

   public int getInventoryColumns() {
      return 0;
   }

   class MountPanicGoal extends PanicGoal {
      public MountPanicGoal(final double p_460750_) {
         super(AbstractHorse.this, p_460750_);
      }

      @Override
      public boolean shouldPanic() {
         return !AbstractHorse.this.isMobControlled() && super.shouldPanic();
      }
   }
}
