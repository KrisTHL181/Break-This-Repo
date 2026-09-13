package net.minecraft.world.entity.animal.equine;

import com.google.common.collect.UnmodifiableIterator;
import java.util.function.DoubleSupplier;
import java.util.function.IntUnaryOperator;
import net.minecraft.advancements.triggers.CriteriaTriggers;
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

public abstract class AbstractHorse extends Animal implements PlayerRideableJumping, HasCustomInventoryScreen, OwnableEntity {
   public static final int CHEST_SLOT_OFFSET = 499;
   public static final int INVENTORY_SLOT_OFFSET = 500;
   public static final double BREEDING_CROSS_FACTOR = 0.15;
   private static final float MIN_MOVEMENT_SPEED = (float)generateSpeed(() -> 0.0);
   private static final float MAX_MOVEMENT_SPEED = (float)generateSpeed(() -> 1.0);
   private static final float MIN_JUMP_STRENGTH = (float)generateJumpStrength(() -> 0.0);
   private static final float MAX_JUMP_STRENGTH = (float)generateJumpStrength(() -> 1.0);
   private static final float MIN_HEALTH = generateMaxHealth(i -> 0);
   private static final float MAX_HEALTH = generateMaxHealth(i -> i - 1);
   private static final float BACKWARDS_MOVE_SPEED_FACTOR = 0.25F;
   private static final float SIDEWAYS_MOVE_SPEED_FACTOR = 0.5F;
   private static final TargetingConditions.Selector PARENT_HORSE_SELECTOR = (target, level) -> target instanceof AbstractHorse horse && horse.isBred();
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
   protected static final float BABY_SCALE = 0.7F;
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

   protected AbstractHorse(final EntityType<? extends AbstractHorse> type, final Level level) {
      super(type, level);
      this.createInventory();
   }

   @Override
   protected void registerGoals() {
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
      this.goalSelector.addGoal(1, new AbstractHorse.MountPanicGoal(1.2));
      this.goalSelector.addGoal(3, new TemptGoal(this, 1.25, i -> i.is(ItemTags.HORSE_TEMPT_ITEMS), false));
   }

   @Override
   protected void defineSynchedData(final SynchedEntityData.Builder entityData) {
      super.defineSynchedData(entityData);
      entityData.define(DATA_ID_FLAGS, (byte)0);
   }

   protected boolean getFlag(final int flag) {
      return (this.entityData.get(DATA_ID_FLAGS) & flag) != 0;
   }

   protected void setFlag(final int flag, final boolean value) {
      byte current = this.entityData.get(DATA_ID_FLAGS);
      if (value) {
         this.entityData.set(DATA_ID_FLAGS, (byte)(current | flag));
      } else {
         this.entityData.set(DATA_ID_FLAGS, (byte)(current & ~flag));
      }
   }

   public boolean isTamed() {
      return this.getFlag(2);
   }

   @Override
   public @Nullable EntityReference<LivingEntity> getOwnerReference() {
      return this.owner;
   }

   public void setOwner(final @Nullable LivingEntity owner) {
      this.owner = EntityReference.of(owner);
   }

   public void setTamed(final boolean flag) {
      this.setFlag(2, flag);
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

   public void setBred(final boolean flag) {
      this.setFlag(8, flag);
   }

   @Override
   public boolean canUseSlot(final EquipmentSlot slot) {
      return slot != EquipmentSlot.SADDLE ? super.canUseSlot(slot) : this.isAlive() && !this.isBaby() && this.isTamed();
   }

   public void equipBodyArmor(final Player player, final ItemStack itemStack) {
      if (this.isEquippableInSlot(itemStack, EquipmentSlot.BODY)) {
         this.setItemSlotAndDropWhenKilled(EquipmentSlot.BODY, itemStack.consumeAndReturn(1, player));
      }
   }

   @Override
   protected boolean canDispenserEquipIntoSlot(final EquipmentSlot slot) {
      return (slot == EquipmentSlot.BODY || slot == EquipmentSlot.SADDLE) && this.isTamed() || super.canDispenserEquipIntoSlot(slot);
   }

   public int getTemper() {
      return this.temper;
   }

   public void setTemper(final int temper) {
      this.temper = temper;
   }

   public int modifyTemper(final int amount) {
      int temper = Mth.clamp(this.getTemper() + amount, 0, this.getMaxTemper());
      this.setTemper(temper);
      return temper;
   }

   @Override
   public boolean isPushable() {
      return !this.isVehicle();
   }

   private void eating() {
      this.openMouth();
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
   }

   @Override
   public boolean causeFallDamage(final double fallDistance, final float damageModifier, final DamageSource damageSource) {
      if (fallDistance > 1.0) {
         this.playSound(this.isBaby() ? SoundEvents.HORSE_LAND_BABY : SoundEvents.HORSE_LAND, 0.4F, 1.0F);
      }

      int dmg = this.calculateFallDamage(fallDistance, damageModifier);
      if (dmg <= 0) {
         return false;
      }

      this.hurt(damageSource, dmg);
      this.propagateFallToPassengers(fallDistance, damageModifier, damageSource);
      this.playBlockFallSound();
      return true;
   }

   public final int getInventorySize() {
      return AbstractMountInventoryMenu.getInventorySize(this.getInventoryColumns());
   }

   protected void createInventory() {
      SimpleContainer old = this.inventory;
      this.inventory = new SimpleContainer(this.getInventorySize());
      if (old != null) {
         int max = Math.min(old.getContainerSize(), this.inventory.getContainerSize());

         for (int slot = 0; slot < max; slot++) {
            ItemStack itemStack = old.getItem(slot);
            if (!itemStack.isEmpty()) {
               this.inventory.setItem(slot, itemStack.copy());
            }
         }
      }
   }

   @Override
   protected Holder<SoundEvent> getEquipSound(final EquipmentSlot slot, final ItemStack stack, final Equippable equippable) {
      return slot == EquipmentSlot.SADDLE ? SoundEvents.HORSE_SADDLE : super.getEquipSound(slot, stack, equippable);
   }

   @Override
   public boolean hurtServer(final ServerLevel level, final DamageSource source, final float damage) {
      boolean wasHurt = super.hurtServer(level, source, damage);
      if (wasHurt && this.random.nextInt(3) == 0) {
         this.standIfPossible();
      }

      return wasHurt;
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
   protected void playStepSound(final BlockPos pos, final BlockState blockState) {
      if (!blockState.liquid()) {
         BlockState aboveState = this.level().getBlockState(pos.above());
         SoundType soundType = blockState.getSoundType();
         if (aboveState.is(Blocks.SNOW)) {
            soundType = aboveState.getSoundType();
         }

         if (this.isVehicle() && this.canGallop) {
            this.gallopSoundCounter++;
            if (this.gallopSoundCounter > 5 && this.gallopSoundCounter % 3 == 0) {
               this.playGallopSound(soundType);
            } else if (this.gallopSoundCounter <= 5) {
               this.playSound(SoundEvents.HORSE_STEP_WOOD, soundType.getVolume() * 0.15F, soundType.getPitch());
            }
         } else if (this.isWoodSoundType(soundType)) {
            this.playSound(SoundEvents.HORSE_STEP_WOOD, soundType.getVolume() * 0.15F, soundType.getPitch());
         } else {
            this.playSound(this.isBaby() ? SoundEvents.HORSE_STEP_BABY : SoundEvents.HORSE_STEP, soundType.getVolume() * 0.15F, soundType.getPitch());
         }
      }
   }

   private boolean isWoodSoundType(final SoundType soundType) {
      return soundType == SoundType.WOOD
         || soundType == SoundType.NETHER_WOOD
         || soundType == SoundType.STEM
         || soundType == SoundType.CHERRY_WOOD
         || soundType == SoundType.BAMBOO_WOOD;
   }

   protected void playGallopSound(final SoundType soundType) {
      this.playSound(SoundEvents.HORSE_GALLOP, soundType.getVolume() * 0.15F, soundType.getPitch());
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
   public void openCustomInventoryScreen(final Player player) {
      if (!this.level().isClientSide() && (!this.isVehicle() || this.hasPassenger(player)) && this.isTamed()) {
         player.openHorseInventory(this, this.inventory);
      }
   }

   public InteractionResult fedFood(final Player player, final ItemStack itemStack) {
      boolean ateFood = this.handleEating(player, itemStack);
      if (ateFood) {
         itemStack.consume(1, player);
      }

      return !ateFood && !this.level().isClientSide() ? InteractionResult.PASS : InteractionResult.SUCCESS_SERVER;
   }

   protected boolean handleEating(final Player player, final ItemStack itemStack) {
      boolean itemUsed = false;
      float heal = 0.0F;
      int ageUp = 0;
      int temper = 0;
      if (itemStack.is(Items.WHEAT)) {
         heal = 2.0F;
         ageUp = 20;
         temper = 3;
      } else if (itemStack.is(Items.SUGAR)) {
         heal = 1.0F;
         ageUp = 30;
         temper = 3;
      } else if (itemStack.is(Items.HAY_BLOCK)) {
         heal = 20.0F;
         ageUp = 180;
      } else if (itemStack.is(Items.APPLE)) {
         heal = 3.0F;
         ageUp = 60;
         temper = 3;
      } else if (itemStack.is(Items.RED_MUSHROOM)) {
         heal = 3.0F;
         ageUp = 0;
         temper = 3;
      } else if (itemStack.is(Items.CARROT)) {
         heal = 3.0F;
         ageUp = 60;
         temper = 3;
      } else if (itemStack.is(Items.GOLDEN_CARROT)) {
         heal = 4.0F;
         ageUp = 60;
         temper = 5;
         if (!this.level().isClientSide() && this.isTamed() && this.getAge() == 0 && !this.isInLove()) {
            itemUsed = true;
            this.setInLove(player);
         }
      } else if (itemStack.is(Items.GOLDEN_APPLE) || itemStack.is(Items.ENCHANTED_GOLDEN_APPLE)) {
         heal = 10.0F;
         ageUp = 240;
         temper = 10;
         if (!this.level().isClientSide() && this.isTamed() && this.getAge() == 0 && !this.isInLove()) {
            itemUsed = true;
            this.setInLove(player);
         }
      }

      if (this.getHealth() < this.getMaxHealth() && heal > 0.0F) {
         this.heal(heal);
         itemUsed = true;
      }

      if (this.isBaby() && ageUp > 0 && !this.isAgeLocked()) {
         this.level().addParticle(ParticleTypes.HAPPY_VILLAGER, this.getRandomX(1.0), this.getRandomY() + 0.5, this.getRandomZ(1.0), 0.0, 0.0, 0.0);
         if (!this.level().isClientSide()) {
            this.ageUp(ageUp);
            itemUsed = true;
         }
      }

      if (temper > 0 && (itemUsed || !this.isTamed()) && this.getTemper() < this.getMaxTemper() && !this.level().isClientSide()) {
         this.modifyTemper(temper);
         itemUsed = true;
      }

      if (itemUsed) {
         this.eating();
         this.gameEvent(GameEvent.EAT);
      }

      return itemUsed;
   }

   protected void doPlayerRide(final Player player) {
      this.setEating(false);
      this.clearStanding();
      if (!this.level().isClientSide()) {
         player.setYRot(this.getYRot());
         player.setXRot(this.getXRot());
         player.startRiding(this);
      }
   }

   @Override
   public boolean isImmobile() {
      return super.isImmobile() && this.isVehicle() && this.isSaddled() || this.isEating() || this.isStanding();
   }

   @Override
   public boolean isFood(final ItemStack itemStack) {
      return itemStack.is(ItemTags.HORSE_FOOD);
   }

   private void moveTail() {
      this.tailCounter = 1;
   }

   @Override
   protected void dropEquipment(final ServerLevel level) {
      super.dropEquipment(level);
      if (this.inventory != null) {
         for (int i = 0; i < this.inventory.getContainerSize(); i++) {
            ItemStack itemStack = this.inventory.getItem(i);
            if (!itemStack.isEmpty() && !EnchantmentHelper.has(itemStack, EnchantmentEffectComponents.PREVENT_EQUIPMENT_DROP)) {
               this.spawnAtLocation(level, itemStack);
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
      if (this.level() instanceof ServerLevel level && this.isAlive()) {
         if (this.random.nextInt(900) == 0 && this.deathTime == 0) {
            this.heal(1.0F);
         }

         if (this.canEatGrass()) {
            if (!this.isEating()
               && !this.isVehicle()
               && this.random.nextInt(300) == 0
               && level.getBlockState(this.blockPosition().below()).is(Blocks.GRASS_BLOCK)) {
               this.setEating(true);
            }

            if (this.isEating() && ++this.eatingCounter > 50) {
               this.eatingCounter = 0;
               this.setEating(false);
            }
         }

         this.followMommy(level);
      }
   }

   protected void followMommy(final ServerLevel level) {
      if (this.isBred() && this.isBaby() && !this.isEating()) {
         LivingEntity mommy = level.getNearestEntity(
            AbstractHorse.class, MOMMY_TARGETING, this, this.getX(), this.getY(), this.getZ(), this.getBoundingBox().inflate(16.0)
         );
         if (mommy != null && this.distanceToSqr(mommy) > 4.0) {
            this.navigation.createPath(mommy, 0);
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
   public InteractionResult mobInteract(final Player player, final InteractionHand hand) {
      if (this.isVehicle() || this.isBaby()) {
         return super.mobInteract(player, hand);
      }

      if (this.isTamed() && player.isSecondaryUseActive()) {
         this.openCustomInventoryScreen(player);
         return InteractionResult.SUCCESS;
      }

      ItemStack itemStack = player.getItemInHand(hand);
      if (!itemStack.isEmpty()) {
         InteractionResult interactionResult = itemStack.interactLivingEntity(player, this, hand);
         if (interactionResult.consumesAction()) {
            return interactionResult;
         }

         if (this.isEquippableInSlot(itemStack, EquipmentSlot.BODY) && !this.isWearingBodyArmor()) {
            this.equipBodyArmor(player, itemStack);
            return InteractionResult.SUCCESS;
         }
      }

      this.doPlayerRide(player);
      return InteractionResult.SUCCESS;
   }

   private void openMouth() {
      if (!this.level().isClientSide()) {
         this.mouthCounter = 1;
         this.setFlag(64, true);
      }
   }

   public void setEating(final boolean flag) {
      this.setFlag(16, flag);
   }

   public void setStanding(final int ticks) {
      this.setEating(false);
      this.setFlag(32, true);
      this.standCounter = ticks;
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

   public boolean tameWithName(final Player player) {
      this.setOwner(player);
      this.setTamed(true);
      if (player instanceof ServerPlayer serverPlayer) {
         CriteriaTriggers.TAME_ANIMAL.trigger(serverPlayer, this);
      }

      this.level().broadcastEntityEvent(this, (byte)7);
      return true;
   }

   @Override
   protected void tickRidden(final Player controller, final Vec3 riddenInput) {
      super.tickRidden(controller, riddenInput);
      Vec2 rotation = this.getRiddenRotation(controller);
      this.setRot(rotation.y, rotation.x);
      this.yRotO = this.yBodyRot = this.yHeadRot = this.getYRot();
      if (this.isLocalInstanceAuthoritative()) {
         if (riddenInput.z <= 0.0) {
            this.gallopSoundCounter = 0;
         }

         if (this.onGround()) {
            if (this.playerJumpPendingScale > 0.0F && !this.isJumping()) {
               this.executeRidersJump(this.playerJumpPendingScale, riddenInput);
            }

            this.playerJumpPendingScale = 0.0F;
         }
      }
   }

   protected Vec2 getRiddenRotation(final LivingEntity controller) {
      return new Vec2(controller.getXRot() * 0.5F, controller.getYRot());
   }

   @Override
   protected void addPassenger(final Entity passenger) {
      super.addPassenger(passenger);
      passenger.absSnapRotationTo(this.getViewYRot(0.0F), this.getViewXRot(0.0F));
   }

   @Override
   protected Vec3 getRiddenInput(final Player controller, final Vec3 selfInput) {
      if (this.onGround() && this.playerJumpPendingScale == 0.0F && this.isStanding() && !this.allowStandSliding) {
         return Vec3.ZERO;
      }

      float sideways = controller.xxa * 0.5F;
      float forward = controller.zza;
      if (forward <= 0.0F) {
         forward *= 0.25F;
      }

      return new Vec3(sideways, 0.0, forward);
   }

   @Override
   protected float getRiddenSpeed(final Player controller) {
      return (float)this.getAttributeValue(Attributes.MOVEMENT_SPEED);
   }

   protected void executeRidersJump(final float amount, final Vec3 input) {
      double impulse = this.getJumpPower(amount);
      Vec3 movement = this.getDeltaMovement();
      this.setDeltaMovement(movement.x, impulse, movement.z);
      this.needsSync = true;
      if (input.z > 0.0) {
         float sin = Mth.sin(this.getYRot() * (float) (Math.PI / 180.0));
         float cos = Mth.cos(this.getYRot() * (float) (Math.PI / 180.0));
         this.setDeltaMovement(this.getDeltaMovement().add(-0.4F * sin * amount, 0.0, 0.4F * cos * amount));
      }
   }

   protected void playJumpSound() {
      this.playSound(SoundEvents.HORSE_JUMP, 0.4F, 1.0F);
   }

   @Override
   protected void addAdditionalSaveData(final ValueOutput output) {
      super.addAdditionalSaveData(output);
      output.putBoolean("EatingHaystack", this.isEating());
      output.putBoolean("Bred", this.isBred());
      output.putInt("Temper", this.getTemper());
      output.putBoolean("Tame", this.isTamed());
      EntityReference.store(this.owner, output, "Owner");
   }

   @Override
   protected void readAdditionalSaveData(final ValueInput input) {
      super.readAdditionalSaveData(input);
      this.setEating(input.getBooleanOr("EatingHaystack", false));
      this.setBred(input.getBooleanOr("Bred", false));
      this.setTemper(input.getIntOr("Temper", 0));
      this.setTamed(input.getBooleanOr("Tame", false));
      this.owner = EntityReference.readWithOldOwnerConversion(input, "Owner", this.level());
   }

   @Override
   public boolean canMate(final Animal partner) {
      return false;
   }

   protected boolean canParent() {
      return !this.isVehicle() && !this.isPassenger() && this.isTamed() && !this.isBaby() && this.getHealth() >= this.getMaxHealth() && this.isInLove();
   }

   public boolean isMobControlled() {
      return false;
   }

   @Override
   public @Nullable AgeableMob getBreedOffspring(final ServerLevel level, final AgeableMob partner) {
      return null;
   }

   protected void setOffspringAttributes(final AgeableMob partner, final AbstractHorse baby) {
      this.setOffspringAttribute(partner, baby, Attributes.MAX_HEALTH, MIN_HEALTH, MAX_HEALTH);
      this.setOffspringAttribute(partner, baby, Attributes.JUMP_STRENGTH, MIN_JUMP_STRENGTH, MAX_JUMP_STRENGTH);
      this.setOffspringAttribute(partner, baby, Attributes.MOVEMENT_SPEED, MIN_MOVEMENT_SPEED, MAX_MOVEMENT_SPEED);
   }

   private void setOffspringAttribute(
      final AgeableMob partner, final AbstractHorse baby, final Holder<Attribute> attribute, final double attributeRangeMin, final double attributeRangeMax
   ) {
      double newValue = createOffspringAttribute(
         this.getAttributeBaseValue(attribute), partner.getAttributeBaseValue(attribute), attributeRangeMin, attributeRangeMax, this.random
      );
      baby.getAttribute(attribute).setBaseValue(newValue);
   }

   public static double createOffspringAttribute(
      double parentAValue, double parentBValue, final double attributeRangeMin, final double attributeRangeMax, final RandomSource random
   ) {
      if (attributeRangeMax <= attributeRangeMin) {
         throw new IllegalArgumentException("Incorrect range for an attribute");
      } else {
         parentAValue = Mth.clamp(parentAValue, attributeRangeMin, attributeRangeMax);
         parentBValue = Mth.clamp(parentBValue, attributeRangeMin, attributeRangeMax);
         double margin = 0.15 * (attributeRangeMax - attributeRangeMin);
         double range = Math.abs(parentAValue - parentBValue) + margin * 2.0;
         double average = (parentAValue + parentBValue) / 2.0;
         double babyQuality = (random.nextDouble() + random.nextDouble() + random.nextDouble()) / 3.0 - 0.5;
         double newValue = average + range * babyQuality;
         if (newValue > attributeRangeMax) {
            double difference = newValue - attributeRangeMax;
            return attributeRangeMax - difference;
         } else if (newValue < attributeRangeMin) {
            double difference = attributeRangeMin - newValue;
            return attributeRangeMin + difference;
         } else {
            return newValue;
         }
      }
   }

   public float getEatAnim(final float a) {
      return Mth.lerp(a, this.eatAnimO, this.eatAnim);
   }

   public float getStandAnim(final float a) {
      return Mth.lerp(a, this.standAnimO, this.standAnim);
   }

   public float getMouthAnim(final float a) {
      return Mth.lerp(a, this.mouthAnimO, this.mouthAnim);
   }

   @Override
   public void onPlayerJump(int jumpAmount) {
      if (this.isSaddled()) {
         if (jumpAmount < 0) {
            jumpAmount = 0;
         } else {
            this.allowStandSliding = true;
            this.standIfPossible();
         }

         this.playerJumpPendingScale = this.getPlayerJumpPendingScale(jumpAmount);
      }
   }

   @Override
   public boolean canJump() {
      return this.isSaddled();
   }

   @Override
   public void handleStartJump(final int jumpScale) {
      this.allowStandSliding = true;
      this.standIfPossible();
      this.playJumpSound();
   }

   @Override
   public void handleStopJump() {
   }

   protected void spawnTamingParticles(final boolean success) {
      ParticleOptions particle = success ? ParticleTypes.HEART : ParticleTypes.SMOKE;

      for (int i = 0; i < 7; i++) {
         double xa = this.random.nextGaussian() * 0.02;
         double ya = this.random.nextGaussian() * 0.02;
         double za = this.random.nextGaussian() * 0.02;
         this.level().addParticle(particle, this.getRandomX(1.0), this.getRandomY() + 0.5, this.getRandomZ(1.0), xa, ya, za);
      }
   }

   @Override
   public void handleEntityEvent(final byte id) {
      if (id == 7) {
         this.spawnTamingParticles(true);
      } else if (id == 6) {
         this.spawnTamingParticles(false);
      } else {
         super.handleEntityEvent(id);
      }
   }

   @Override
   protected void positionRider(final Entity passenger, final Entity.MoveFunction moveFunction) {
      super.positionRider(passenger, moveFunction);
      if (passenger instanceof LivingEntity livingEntity) {
         livingEntity.yBodyRot = this.yBodyRot;
      }
   }

   protected static float generateMaxHealth(final IntUnaryOperator integerByBoundProvider) {
      return 15.0F + integerByBoundProvider.applyAsInt(8) + integerByBoundProvider.applyAsInt(9);
   }

   protected static double generateJumpStrength(final DoubleSupplier probabilityProvider) {
      return 0.4F + probabilityProvider.getAsDouble() * 0.2 + probabilityProvider.getAsDouble() * 0.2 + probabilityProvider.getAsDouble() * 0.2;
   }

   protected static double generateSpeed(final DoubleSupplier probabilityProvider) {
      return (0.45F + probabilityProvider.getAsDouble() * 0.3 + probabilityProvider.getAsDouble() * 0.3 + probabilityProvider.getAsDouble() * 0.3) * 0.25;
   }

   @Override
   public boolean onClimbable() {
      return false;
   }

   @Override
   public @Nullable SlotAccess getSlot(final int slot) {
      int inventorySlot = slot - 500;
      return inventorySlot >= 0 && inventorySlot < this.inventory.getContainerSize() ? this.inventory.getSlot(inventorySlot) : super.getSlot(slot);
   }

   @Override
   public @Nullable LivingEntity getControllingPassenger() {
      return this.isSaddled() && this.getFirstPassenger() instanceof Player passenger ? passenger : super.getControllingPassenger();
   }

   private @Nullable Vec3 getDismountLocationInDirection(final Vec3 direction, final LivingEntity passenger) {
      double targetX = this.getX() + direction.x;
      double targetY = this.getBoundingBox().minY;
      double targetZ = this.getZ() + direction.z;
      BlockPos.MutableBlockPos targetBlockPos = new BlockPos.MutableBlockPos();
      UnmodifiableIterator var10 = passenger.getDismountPoses().iterator();

      while (var10.hasNext()) {
         Pose dismountPose = (Pose)var10.next();
         targetBlockPos.set(targetX, targetY, targetZ);
         double dismountJumpLimit = this.getBoundingBox().maxY + 0.75;

         do {
            double blockFloorHeight = this.level().getBlockFloorHeight(targetBlockPos);
            if (targetBlockPos.getY() + blockFloorHeight > dismountJumpLimit) {
               break;
            }

            if (DismountHelper.isBlockFloorValid(blockFloorHeight)) {
               AABB poseCollisionBox = passenger.getLocalBoundsForPose(dismountPose);
               Vec3 location = new Vec3(targetX, targetBlockPos.getY() + blockFloorHeight, targetZ);
               if (DismountHelper.canDismountTo(this.level(), passenger, poseCollisionBox.move(location))) {
                  passenger.setPose(dismountPose);
                  return location;
               }
            }

            targetBlockPos.move(Direction.UP);
         } while (targetBlockPos.getY() < dismountJumpLimit);
      }

      return null;
   }

   @Override
   public Vec3 getDismountLocationForPassenger(final LivingEntity passenger) {
      Vec3 mainHandDirection = getCollisionHorizontalEscapeVector(
         this.getBbWidth(), passenger.getBbWidth(), this.getYRot() + (passenger.getMainArm() == HumanoidArm.RIGHT ? 90.0F : -90.0F)
      );
      Vec3 mainHandLocation = this.getDismountLocationInDirection(mainHandDirection, passenger);
      if (mainHandLocation != null) {
         return mainHandLocation;
      }

      Vec3 offHandDirection = getCollisionHorizontalEscapeVector(
         this.getBbWidth(), passenger.getBbWidth(), this.getYRot() + (passenger.getMainArm() == HumanoidArm.LEFT ? 90.0F : -90.0F)
      );
      Vec3 offHandLocation = this.getDismountLocationInDirection(offHandDirection, passenger);
      return offHandLocation != null ? offHandLocation : this.position();
   }

   protected void randomizeAttributes(final RandomSource random) {
   }

   @Override
   public @Nullable SpawnGroupData finalizeSpawn(
      final ServerLevelAccessor level, final DifficultyInstance difficulty, final EntitySpawnReason spawnReason, @Nullable SpawnGroupData groupData
   ) {
      if (groupData == null) {
         groupData = new AgeableMob.AgeableMobGroupData(0.2F);
      }

      this.randomizeAttributes(level.getRandom());
      return super.finalizeSpawn(level, difficulty, spawnReason, groupData);
   }

   public boolean hasInventoryChanged(final Container oldInventory) {
      return this.inventory != oldInventory;
   }

   public int getAmbientStandInterval() {
      return this.getAmbientSoundInterval();
   }

   @Override
   protected Vec3 getPassengerAttachmentPoint(final Entity passenger, final EntityDimensions dimensions, final float scale) {
      return super.getPassengerAttachmentPoint(passenger, dimensions, scale)
         .add(new Vec3(0.0, 0.15 * this.standAnimO * scale, -0.7 * this.standAnimO * scale).yRot(-this.getYRot() * (float) (Math.PI / 180.0)));
   }

   public int getInventoryColumns() {
      return 0;
   }

   private class MountPanicGoal extends PanicGoal {
      public MountPanicGoal(final double speedModifier) {
         super(AbstractHorse.this, speedModifier);
      }

      @Override
      public boolean shouldPanic() {
         return !AbstractHorse.this.isMobControlled() && super.shouldPanic();
      }
   }
}
