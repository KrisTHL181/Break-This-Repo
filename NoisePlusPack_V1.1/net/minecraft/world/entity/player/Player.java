package net.minecraft.world.entity.player;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.math.IntMath;
import com.mojang.authlib.GameProfile;
import com.mojang.datafixers.util.Either;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.dialog.Dialog;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.server.players.NameAndId;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stat;
import net.minecraft.stats.Stats;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.util.Unit;
import net.minecraft.util.Util;
import net.minecraft.world.Container;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemStackWithSlot;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityEquipment;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.animal.nautilus.AbstractNautilus;
import net.minecraft.world.entity.animal.parrot.Parrot;
import net.minecraft.world.entity.boss.enderdragon.EnderDragonPart;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.warden.WardenSpawnTracker;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileDeflection;
import net.minecraft.world.entity.vehicle.minecart.MinecartCommandBlock;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.PlayerEnderChestContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemCooldowns;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.component.BlocksAttacks;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.CommandBlockEntity;
import net.minecraft.world.level.block.entity.ContainerOpenersCounter;
import net.minecraft.world.level.block.entity.JigsawBlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.StructureBlockEntity;
import net.minecraft.world.level.block.entity.TestBlockEntity;
import net.minecraft.world.level.block.entity.TestInstanceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Team;
import org.jspecify.annotations.Nullable;

public abstract class Player extends Avatar implements ContainerUser {
   public static final int MAX_HEALTH = 20;
   public static final int SLEEP_DURATION = 100;
   public static final int WAKE_UP_DURATION = 10;
   public static final int ENDER_SLOT_OFFSET = 200;
   public static final int HELD_ITEM_SLOT = 499;
   public static final int CRAFTING_SLOT_OFFSET = 500;
   public static final float DEFAULT_BLOCK_INTERACTION_RANGE = 4.5F;
   public static final float DEFAULT_ENTITY_INTERACTION_RANGE = 3.0F;
   private static final int CURRENT_IMPULSE_CONTEXT_RESET_GRACE_TIME_TICKS = 40;
   private static final EntityDataAccessor<Float> DATA_PLAYER_ABSORPTION_ID = SynchedEntityData.defineId(Player.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Integer> DATA_SCORE_ID = SynchedEntityData.defineId(Player.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<OptionalInt> DATA_SHOULDER_PARROT_LEFT = SynchedEntityData.defineId(
      Player.class, EntityDataSerializers.OPTIONAL_UNSIGNED_INT
   );
   private static final EntityDataAccessor<OptionalInt> DATA_SHOULDER_PARROT_RIGHT = SynchedEntityData.defineId(
      Player.class, EntityDataSerializers.OPTIONAL_UNSIGNED_INT
   );
   private static final short DEFAULT_SLEEP_TIMER = 0;
   private static final float DEFAULT_EXPERIENCE_PROGRESS = 0.0F;
   private static final int DEFAULT_EXPERIENCE_LEVEL = 0;
   private static final int DEFAULT_TOTAL_EXPERIENCE = 0;
   private static final int NO_ENCHANTMENT_SEED = 0;
   private static final int DEFAULT_SELECTED_SLOT = 0;
   private static final int DEFAULT_SCORE = 0;
   private static final boolean DEFAULT_IGNORE_FALL_DAMAGE_FROM_CURRENT_IMPULSE = false;
   private static final int DEFAULT_CURRENT_IMPULSE_CONTEXT_RESET_GRACE_TIME = 0;
   public static final float CREATIVE_ENTITY_INTERACTION_RANGE_MODIFIER_VALUE = 2.0F;
   final Inventory inventory;
   protected PlayerEnderChestContainer enderChestInventory = new PlayerEnderChestContainer();
   public final InventoryMenu inventoryMenu;
   public AbstractContainerMenu containerMenu;
   protected FoodData foodData = new FoodData();
   protected int jumpTriggerTime;
   public int takeXpDelay;
   private int sleepCounter = 0;
   protected boolean wasUnderwater;
   private final Abilities abilities = new Abilities();
   public int experienceLevel = 0;
   public int totalExperience = 0;
   public float experienceProgress = 0.0F;
   protected int enchantmentSeed = 0;
   protected final float defaultFlySpeed = 0.02F;
   private int lastLevelUpTime;
   private final GameProfile gameProfile;
   private boolean reducedDebugInfo;
   private ItemStack lastItemInMainHand = ItemStack.EMPTY;
   private final ItemCooldowns cooldowns = this.createItemCooldowns();
   private Optional<GlobalPos> lastDeathLocation = Optional.empty();
   public @Nullable FishingHook fishing;
   protected float hurtDir;
   public @Nullable Vec3 currentImpulseImpactPos;
   public @Nullable Entity currentExplosionCause;
   private boolean ignoreFallDamageFromCurrentImpulse = false;
   private int currentImpulseContextResetGraceTime = 0;

   public Player(Level p_250508_, GameProfile p_252153_) {
      super(EntityType.PLAYER, p_250508_);
      this.setUUID(p_252153_.id());
      this.gameProfile = p_252153_;
      this.inventory = new Inventory(this, this.equipment);
      this.inventoryMenu = new InventoryMenu(this.inventory, !p_250508_.isClientSide(), this);
      this.containerMenu = this.inventoryMenu;
   }

   @Override
   protected EntityEquipment createEquipment() {
      return new PlayerEquipment(this);
   }

   public boolean blockActionRestricted(Level p_36188_, BlockPos p_36189_, GameType p_36190_) {
      if (!p_36190_.isBlockPlacingRestricted()) {
         return false;
      }

      if (p_36190_ == GameType.SPECTATOR) {
         return true;
      }

      if (this.mayBuild()) {
         return false;
      }

      ItemStack itemstack = this.getMainHandItem();
      return itemstack.isEmpty() || !itemstack.canBreakBlockInAdventureMode(new BlockInWorld(p_36188_, p_36189_, false));
   }

   public static AttributeSupplier.Builder createAttributes() {
      return LivingEntity.createLivingAttributes()
         .add(Attributes.ATTACK_DAMAGE, 1.0)
         .add(Attributes.MOVEMENT_SPEED, 0.1F)
         .add(Attributes.ATTACK_SPEED)
         .add(Attributes.LUCK)
         .add(Attributes.BLOCK_INTERACTION_RANGE, 4.5)
         .add(Attributes.ENTITY_INTERACTION_RANGE, 3.0)
         .add(Attributes.BLOCK_BREAK_SPEED)
         .add(Attributes.SUBMERGED_MINING_SPEED)
         .add(Attributes.SNEAKING_SPEED)
         .add(Attributes.MINING_EFFICIENCY)
         .add(Attributes.SWEEPING_DAMAGE_RATIO)
         .add(Attributes.WAYPOINT_TRANSMIT_RANGE, 6.0E7)
         .add(Attributes.WAYPOINT_RECEIVE_RANGE, 6.0E7);
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder p_335298_) {
      super.defineSynchedData(p_335298_);
      p_335298_.define(DATA_PLAYER_ABSORPTION_ID, 0.0F);
      p_335298_.define(DATA_SCORE_ID, 0);
      p_335298_.define(DATA_SHOULDER_PARROT_LEFT, OptionalInt.empty());
      p_335298_.define(DATA_SHOULDER_PARROT_RIGHT, OptionalInt.empty());
   }

   @Override
   public void tick() {
      this.noPhysics = this.isSpectator();
      if (this.isSpectator() || this.isPassenger()) {
         this.setOnGround(false);
      }

      if (this.takeXpDelay > 0) {
         this.takeXpDelay--;
      }

      if (this.isSleeping()) {
         this.sleepCounter++;
         if (this.sleepCounter > 100) {
            this.sleepCounter = 100;
         }

         if (!this.level().isClientSide()
            && !this.level().environmentAttributes().getValue(EnvironmentAttributes.BED_RULE, this.position()).canSleep(this.level())) {
            this.stopSleepInBed(false, true);
         }
      } else if (this.sleepCounter > 0) {
         this.sleepCounter++;
         if (this.sleepCounter >= 110) {
            this.sleepCounter = 0;
         }
      }

      this.updateIsUnderwater();
      super.tick();
      int i = 29999999;
      this.attackStrengthTicker++;
      this.itemSwapTicker++;
      ItemStack itemstack = this.getMainHandItem();
      if (!ItemStack.matches(this.lastItemInMainHand, itemstack)) {
         if (!ItemStack.isSameItem(this.lastItemInMainHand, itemstack)) {
            this.resetAttackStrengthTicker();
         }

         this.lastItemInMainHand = itemstack.copy();
      }

      if (!this.isEyeInFluid(FluidTags.WATER) && this.isEquipped(Items.TURTLE_HELMET)) {
         this.turtleHelmetTick();
      }

      this.cooldowns.tick();
      this.updatePlayerPose();
      if (this.currentImpulseContextResetGraceTime > 0) {
         this.currentImpulseContextResetGraceTime--;
      }
   }

   @Override
   protected float getMaxHeadRotationRelativeToBody() {
      return this.isBlocking() ? 15.0F : super.getMaxHeadRotationRelativeToBody();
   }

   public boolean isSecondaryUseActive() {
      return this.isShiftKeyDown();
   }

   protected boolean wantsToStopRiding() {
      return this.isShiftKeyDown();
   }

   protected boolean isStayingOnGroundSurface() {
      return this.isShiftKeyDown();
   }

   protected boolean updateIsUnderwater() {
      this.wasUnderwater = this.isEyeInFluid(FluidTags.WATER);
      return this.wasUnderwater;
   }

   @Override
   public void onAboveBubbleColumn(boolean p_397973_, BlockPos p_391484_) {
      if (!this.getAbilities().flying) {
         super.onAboveBubbleColumn(p_397973_, p_391484_);
      }
   }

   @Override
   public void onInsideBubbleColumn(boolean p_369072_) {
      if (!this.getAbilities().flying) {
         super.onInsideBubbleColumn(p_369072_);
      }
   }

   private void turtleHelmetTick() {
      this.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 200, 0, false, false, true));
   }

   private boolean isEquipped(Item p_365145_) {
      for (EquipmentSlot equipmentslot : EquipmentSlot.VALUES) {
         ItemStack itemstack = this.getItemBySlot(equipmentslot);
         Equippable equippable = itemstack.get(DataComponents.EQUIPPABLE);
         if (itemstack.is(p_365145_) && equippable != null && equippable.slot() == equipmentslot) {
            return true;
         }
      }

      return false;
   }

   protected ItemCooldowns createItemCooldowns() {
      return new ItemCooldowns();
   }

   protected void updatePlayerPose() {
      if (this.canPlayerFitWithinBlocksAndEntitiesWhen(Pose.SWIMMING)) {
         Pose pose = this.getDesiredPose();
         Pose pose1;
         if (this.isSpectator() || this.isPassenger() || this.canPlayerFitWithinBlocksAndEntitiesWhen(pose)) {
            pose1 = pose;
         } else if (this.canPlayerFitWithinBlocksAndEntitiesWhen(Pose.CROUCHING)) {
            pose1 = Pose.CROUCHING;
         } else {
            pose1 = Pose.SWIMMING;
         }

         this.setPose(pose1);
      }
   }

   private Pose getDesiredPose() {
      if (this.isSleeping()) {
         return Pose.SLEEPING;
      } else if (this.isSwimming()) {
         return Pose.SWIMMING;
      } else if (this.isFallFlying()) {
         return Pose.FALL_FLYING;
      } else if (this.isAutoSpinAttack()) {
         return Pose.SPIN_ATTACK;
      } else {
         return this.isShiftKeyDown() && !this.abilities.flying ? Pose.CROUCHING : Pose.STANDING;
      }
   }

   protected boolean canPlayerFitWithinBlocksAndEntitiesWhen(Pose p_297636_) {
      return this.level().noCollision(this, this.getDimensions(p_297636_).makeBoundingBox(this.position()).deflate(1.0E-7));
   }

   @Override
   protected SoundEvent getSwimSound() {
      return SoundEvents.PLAYER_SWIM;
   }

   @Override
   protected SoundEvent getSwimSplashSound() {
      return SoundEvents.PLAYER_SPLASH;
   }

   @Override
   protected SoundEvent getSwimHighSpeedSplashSound() {
      return SoundEvents.PLAYER_SPLASH_HIGH_SPEED;
   }

   @Override
   public int getDimensionChangingDelay() {
      return 10;
   }

   @Override
   public void playSound(SoundEvent p_36137_, float p_36138_, float p_36139_) {
      this.level().playSound(this, this.getX(), this.getY(), this.getZ(), p_36137_, this.getSoundSource(), p_36138_, p_36139_);
   }

   @Override
   public SoundSource getSoundSource() {
      return SoundSource.PLAYERS;
   }

   @Override
   protected int getFireImmuneTicks() {
      return 20;
   }

   @Override
   public void handleEntityEvent(byte p_36120_) {
      if (p_36120_ == 9) {
         this.completeUsingItem();
      } else if (p_36120_ == 23) {
         this.setReducedDebugInfo(false);
      } else if (p_36120_ == 22) {
         this.setReducedDebugInfo(true);
      } else {
         super.handleEntityEvent(p_36120_);
      }
   }

   protected void closeContainer() {
      this.containerMenu = this.inventoryMenu;
   }

   protected void doCloseContainer() {
   }

   @Override
   public void rideTick() {
      if (!this.level().isClientSide() && this.wantsToStopRiding() && this.isPassenger()) {
         this.stopRiding();
         this.setShiftKeyDown(false);
      } else {
         super.rideTick();
      }
   }

   @Override
   public void aiStep() {
      if (this.jumpTriggerTime > 0) {
         this.jumpTriggerTime--;
      }

      this.tickRegeneration();
      this.inventory.tick();
      if (this.abilities.flying && !this.isPassenger()) {
         this.resetFallDistance();
      }

      super.aiStep();
      this.updateSwingTime();
      this.yHeadRot = this.getYRot();
      this.setSpeed((float)this.getAttributeValue(Attributes.MOVEMENT_SPEED));
      if (this.getHealth() > 0.0F && !this.isSpectator()) {
         AABB aabb;
         if (this.isPassenger() && !this.getVehicle().isRemoved()) {
            aabb = this.getBoundingBox().minmax(this.getVehicle().getBoundingBox()).inflate(1.0, 0.0, 1.0);
         } else {
            aabb = this.getBoundingBox().inflate(1.0, 0.5, 1.0);
         }

         List<Entity> list = this.level().getEntities(this, aabb);
         List<Entity> list1 = Lists.newArrayList();

         for (Entity entity : list) {
            if (entity.getType() == EntityType.EXPERIENCE_ORB) {
               list1.add(entity);
            } else if (!entity.isRemoved()) {
               this.touch(entity);
            }
         }

         if (!list1.isEmpty()) {
            this.touch(Util.getRandom(list1, this.random));
         }
      }

      this.handleShoulderEntities();
   }

   protected void tickRegeneration() {
   }

   public void handleShoulderEntities() {
   }

   protected void removeEntitiesOnShoulder() {
   }

   private void touch(Entity p_36278_) {
      p_36278_.playerTouch(this);
   }

   public int getScore() {
      return this.entityData.get(DATA_SCORE_ID);
   }

   public void setScore(int p_36398_) {
      this.entityData.set(DATA_SCORE_ID, p_36398_);
   }

   public void increaseScore(int p_36402_) {
      int i = this.getScore();
      this.entityData.set(DATA_SCORE_ID, i + p_36402_);
   }

   public void startAutoSpinAttack(int p_204080_, float p_344736_, ItemStack p_343326_) {
      this.autoSpinAttackTicks = p_204080_;
      this.autoSpinAttackDmg = p_344736_;
      this.autoSpinAttackItemStack = p_343326_;
      if (!this.level().isClientSide()) {
         this.removeEntitiesOnShoulder();
         this.setLivingEntityFlag(4, true);
      }
   }

   @Override
   public ItemStack getWeaponItem() {
      return this.isAutoSpinAttack() && this.autoSpinAttackItemStack != null ? this.autoSpinAttackItemStack : super.getWeaponItem();
   }

   @Override
   public void die(DamageSource p_36152_) {
      super.die(p_36152_);
      this.reapplyPosition();
      if (!this.isSpectator() && this.level() instanceof ServerLevel serverlevel) {
         this.dropAllDeathLoot(serverlevel, p_36152_);
      }

      if (p_36152_ != null) {
         this.setDeltaMovement(
            -Mth.cos((this.getHurtDir() + this.getYRot()) * (float) (Math.PI / 180.0)) * 0.1F,
            0.1F,
            -Mth.sin((this.getHurtDir() + this.getYRot()) * (float) (Math.PI / 180.0)) * 0.1F
         );
      } else {
         this.setDeltaMovement(0.0, 0.1, 0.0);
      }

      this.awardStat(Stats.DEATHS);
      this.resetStat(Stats.CUSTOM.get(Stats.TIME_SINCE_DEATH));
      this.resetStat(Stats.CUSTOM.get(Stats.TIME_SINCE_REST));
      this.clearFire();
      this.setSharedFlagOnFire(false);
      this.setLastDeathLocation(Optional.of(GlobalPos.of(this.level().dimension(), this.blockPosition())));
   }

   @Override
   protected void dropEquipment(ServerLevel p_369623_) {
      super.dropEquipment(p_369623_);
      if (!p_369623_.getGameRules().get(GameRules.KEEP_INVENTORY)) {
         this.destroyVanishingCursedItems();
         this.inventory.dropAll();
      }
   }

   protected void destroyVanishingCursedItems() {
      for (int i = 0; i < this.inventory.getContainerSize(); i++) {
         ItemStack itemstack = this.inventory.getItem(i);
         if (!itemstack.isEmpty() && EnchantmentHelper.has(itemstack, EnchantmentEffectComponents.PREVENT_EQUIPMENT_DROP)) {
            this.inventory.removeItemNoUpdate(i);
         }
      }
   }

   @Override
   protected SoundEvent getHurtSound(DamageSource p_36310_) {
      return p_36310_.type().effects().sound();
   }

   @Override
   protected SoundEvent getDeathSound() {
      return SoundEvents.PLAYER_DEATH;
   }

   public void handleCreativeModeItemDrop(ItemStack p_369068_) {
   }

   public @Nullable ItemEntity drop(ItemStack p_36177_, boolean p_36178_) {
      return this.drop(p_36177_, false, p_36178_);
   }

   public float getDestroySpeed(BlockState p_36282_) {
      float f = this.inventory.getSelectedItem().getDestroySpeed(p_36282_);
      if (f > 1.0F) {
         f += (float)this.getAttributeValue(Attributes.MINING_EFFICIENCY);
      }

      if (MobEffectUtil.hasDigSpeed(this)) {
         f *= 1.0F + (MobEffectUtil.getDigSpeedAmplification(this) + 1) * 0.2F;
      }

      if (this.hasEffect(MobEffects.MINING_FATIGUE)) {
         float f1 = switch (this.getEffect(MobEffects.MINING_FATIGUE).getAmplifier()) {
            case 0 -> 0.3F;
            case 1 -> 0.09F;
            case 2 -> 0.0027F;
            default -> 8.1E-4F;
         };
         f *= f1;
      }

      f *= (float)this.getAttributeValue(Attributes.BLOCK_BREAK_SPEED);
      if (this.isEyeInFluid(FluidTags.WATER)) {
         f *= (float)this.getAttribute(Attributes.SUBMERGED_MINING_SPEED).getValue();
      }

      if (!this.onGround()) {
         f /= 5.0F;
      }

      return f;
   }

   public boolean hasCorrectToolForDrops(BlockState p_36299_) {
      return !p_36299_.requiresCorrectToolForDrops() || this.inventory.getSelectedItem().isCorrectToolForDrops(p_36299_);
   }

   @Override
   protected void readAdditionalSaveData(ValueInput p_410352_) {
      super.readAdditionalSaveData(p_410352_);
      this.setUUID(this.gameProfile.id());
      this.inventory.load(p_410352_.listOrEmpty("Inventory", ItemStackWithSlot.CODEC));
      this.inventory.setSelectedSlot(p_410352_.getIntOr("SelectedItemSlot", 0));
      this.sleepCounter = p_410352_.getShortOr("SleepTimer", (short)0);
      this.experienceProgress = p_410352_.getFloatOr("XpP", 0.0F);
      this.experienceLevel = p_410352_.getIntOr("XpLevel", 0);
      this.totalExperience = p_410352_.getIntOr("XpTotal", 0);
      this.enchantmentSeed = p_410352_.getIntOr("XpSeed", 0);
      if (this.enchantmentSeed == 0) {
         this.enchantmentSeed = this.random.nextInt();
      }

      this.setScore(p_410352_.getIntOr("Score", 0));
      this.foodData.readAdditionalSaveData(p_410352_);
      p_410352_.<Abilities.Packed>read("abilities", Abilities.Packed.CODEC).ifPresent(this.abilities::apply);
      this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(this.abilities.getWalkingSpeed());
      this.enderChestInventory.fromSlots(p_410352_.listOrEmpty("EnderItems", ItemStackWithSlot.CODEC));
      this.setLastDeathLocation(p_410352_.read("LastDeathLocation", GlobalPos.CODEC));
      this.currentImpulseImpactPos = p_410352_.<Vec3>read("current_explosion_impact_pos", Vec3.CODEC).orElse(null);
      this.ignoreFallDamageFromCurrentImpulse = p_410352_.getBooleanOr("ignore_fall_damage_from_current_explosion", false);
      this.currentImpulseContextResetGraceTime = p_410352_.getIntOr("current_impulse_context_reset_grace_time", 0);
   }

   @Override
   protected void addAdditionalSaveData(ValueOutput p_406026_) {
      super.addAdditionalSaveData(p_406026_);
      NbtUtils.addCurrentDataVersion(p_406026_);
      this.inventory.save(p_406026_.list("Inventory", ItemStackWithSlot.CODEC));
      p_406026_.putInt("SelectedItemSlot", this.inventory.getSelectedSlot());
      p_406026_.putShort("SleepTimer", (short)this.sleepCounter);
      p_406026_.putFloat("XpP", this.experienceProgress);
      p_406026_.putInt("XpLevel", this.experienceLevel);
      p_406026_.putInt("XpTotal", this.totalExperience);
      p_406026_.putInt("XpSeed", this.enchantmentSeed);
      p_406026_.putInt("Score", this.getScore());
      this.foodData.addAdditionalSaveData(p_406026_);
      p_406026_.store("abilities", Abilities.Packed.CODEC, this.abilities.pack());
      this.enderChestInventory.storeAsSlots(p_406026_.list("EnderItems", ItemStackWithSlot.CODEC));
      this.lastDeathLocation.ifPresent(p_405554_ -> p_406026_.store("LastDeathLocation", GlobalPos.CODEC, p_405554_));
      p_406026_.storeNullable("current_explosion_impact_pos", Vec3.CODEC, this.currentImpulseImpactPos);
      p_406026_.putBoolean("ignore_fall_damage_from_current_explosion", this.ignoreFallDamageFromCurrentImpulse);
      p_406026_.putInt("current_impulse_context_reset_grace_time", this.currentImpulseContextResetGraceTime);
   }

   @Override
   public boolean isInvulnerableTo(ServerLevel p_360775_, DamageSource p_36249_) {
      if (super.isInvulnerableTo(p_360775_, p_36249_)) {
         return true;
      } else if (p_36249_.is(DamageTypeTags.IS_DROWNING)) {
         return !p_360775_.getGameRules().get(GameRules.DROWNING_DAMAGE);
      } else if (p_36249_.is(DamageTypeTags.IS_FALL)) {
         return !p_360775_.getGameRules().get(GameRules.FALL_DAMAGE);
      } else if (p_36249_.is(DamageTypeTags.IS_FIRE)) {
         return !p_360775_.getGameRules().get(GameRules.FIRE_DAMAGE);
      } else {
         return p_36249_.is(DamageTypeTags.IS_FREEZING) ? !p_360775_.getGameRules().get(GameRules.FREEZE_DAMAGE) : false;
      }
   }

   @Override
   public boolean hurtServer(ServerLevel p_369360_, DamageSource p_364544_, float p_368576_) {
      if (this.isInvulnerableTo(p_369360_, p_364544_)) {
         return false;
      }

      if (this.abilities.invulnerable && !p_364544_.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
         return false;
      }

      this.noActionTime = 0;
      if (this.isDeadOrDying()) {
         return false;
      }

      this.removeEntitiesOnShoulder();
      if (p_364544_.scalesWithDifficulty()) {
         if (p_369360_.getDifficulty() == Difficulty.PEACEFUL) {
            p_368576_ = 0.0F;
         }

         if (p_369360_.getDifficulty() == Difficulty.EASY) {
            p_368576_ = Math.min(p_368576_ / 2.0F + 1.0F, p_368576_);
         }

         if (p_369360_.getDifficulty() == Difficulty.HARD) {
            p_368576_ = p_368576_ * 3.0F / 2.0F;
         }
      }

      return p_368576_ == 0.0F ? false : super.hurtServer(p_369360_, p_364544_, p_368576_);
   }

   @Override
   protected void blockUsingItem(ServerLevel p_395321_, LivingEntity p_395720_) {
      super.blockUsingItem(p_395321_, p_395720_);
      ItemStack itemstack = this.getItemBlockingWith();
      BlocksAttacks blocksattacks = itemstack != null ? itemstack.get(DataComponents.BLOCKS_ATTACKS) : null;
      float f = p_395720_.getSecondsToDisableBlocking();
      if (f > 0.0F && blocksattacks != null) {
         blocksattacks.disable(p_395321_, this, f, itemstack);
      }
   }

   @Override
   public boolean canBeSeenAsEnemy() {
      return !this.getAbilities().invulnerable && super.canBeSeenAsEnemy();
   }

   public boolean canHarmPlayer(Player p_36169_) {
      Team team = this.getTeam();
      Team team1 = p_36169_.getTeam();
      if (team == null) {
         return true;
      } else {
         return !team.isAlliedTo(team1) ? true : team.isAllowFriendlyFire();
      }
   }

   @Override
   protected void hurtArmor(DamageSource p_36251_, float p_36252_) {
      this.doHurtEquipment(p_36251_, p_36252_, EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD);
   }

   @Override
   protected void hurtHelmet(DamageSource p_150103_, float p_150104_) {
      this.doHurtEquipment(p_150103_, p_150104_, EquipmentSlot.HEAD);
   }

   @Override
   protected void actuallyHurt(ServerLevel p_365751_, DamageSource p_36312_, float p_36313_) {
      if (!this.isInvulnerableTo(p_365751_, p_36312_)) {
         p_36313_ = this.getDamageAfterArmorAbsorb(p_36312_, p_36313_);
         p_36313_ = this.getDamageAfterMagicAbsorb(p_36312_, p_36313_);
         float f1 = Math.max(p_36313_ - this.getAbsorptionAmount(), 0.0F);
         this.setAbsorptionAmount(this.getAbsorptionAmount() - (p_36313_ - f1));
         float f = p_36313_ - f1;
         if (f > 0.0F && f < 3.4028235E37F) {
            this.awardStat(Stats.DAMAGE_ABSORBED, Math.round(f * 10.0F));
         }

         if (f1 != 0.0F) {
            this.causeFoodExhaustion(p_36312_.getFoodExhaustion());
            this.getCombatTracker().recordDamage(p_36312_, f1);
            this.setHealth(this.getHealth() - f1);
            if (f1 < 3.4028235E37F) {
               this.awardStat(Stats.DAMAGE_TAKEN, Math.round(f1 * 10.0F));
            }

            this.gameEvent(GameEvent.ENTITY_DAMAGE);
         }
      }
   }

   public boolean isTextFilteringEnabled() {
      return false;
   }

   public void openTextEdit(SignBlockEntity p_36193_, boolean p_277837_) {
   }

   public void openMinecartCommandBlock(MinecartCommandBlock p_455589_) {
   }

   public void openCommandBlock(CommandBlockEntity p_36191_) {
   }

   public void openStructureBlock(StructureBlockEntity p_36194_) {
   }

   public void openTestBlock(TestBlockEntity p_396402_) {
   }

   public void openTestInstanceBlock(TestInstanceBlockEntity p_391756_) {
   }

   public void openJigsawBlock(JigsawBlockEntity p_36192_) {
   }

   public void openHorseInventory(AbstractHorse p_456666_, Container p_36168_) {
   }

   public void openNautilusInventory(AbstractNautilus p_455421_, Container p_460700_) {
   }

   public OptionalInt openMenu(@Nullable MenuProvider p_36150_) {
      return OptionalInt.empty();
   }

   public void openDialog(Holder<Dialog> p_410470_) {
   }

   public void sendMerchantOffers(int p_36121_, MerchantOffers p_36122_, int p_36123_, int p_36124_, boolean p_36125_, boolean p_36126_) {
   }

   public void openItemGui(ItemStack p_36174_, InteractionHand p_36175_) {
   }

   public InteractionResult interactOn(Entity p_36158_, InteractionHand p_36159_) {
      if (this.isSpectator()) {
         if (p_36158_ instanceof MenuProvider) {
            this.openMenu((MenuProvider)p_36158_);
         }

         return InteractionResult.PASS;
      } else {
         ItemStack itemstack = this.getItemInHand(p_36159_);
         ItemStack itemstack1 = itemstack.copy();
         InteractionResult interactionresult = p_36158_.interact(this, p_36159_);
         if (interactionresult.consumesAction()) {
            if (this.hasInfiniteMaterials() && itemstack == this.getItemInHand(p_36159_) && itemstack.getCount() < itemstack1.getCount()) {
               itemstack.setCount(itemstack1.getCount());
            }

            return interactionresult;
         } else {
            if (!itemstack.isEmpty() && p_36158_ instanceof LivingEntity) {
               if (this.hasInfiniteMaterials()) {
                  itemstack = itemstack1;
               }

               InteractionResult interactionresult1 = itemstack.interactLivingEntity(this, (LivingEntity)p_36158_, p_36159_);
               if (interactionresult1.consumesAction()) {
                  this.level().gameEvent(GameEvent.ENTITY_INTERACT, p_36158_.position(), GameEvent.Context.of(this));
                  if (itemstack.isEmpty() && !this.hasInfiniteMaterials()) {
                     this.setItemInHand(p_36159_, ItemStack.EMPTY);
                  }

                  return interactionresult1;
               }
            }

            return InteractionResult.PASS;
         }
      }
   }

   @Override
   public void removeVehicle() {
      super.removeVehicle();
      this.boardingCooldown = 0;
   }

   @Override
   protected boolean isImmobile() {
      return super.isImmobile() || this.isSleeping();
   }

   @Override
   public boolean isAffectedByFluids() {
      return !this.abilities.flying;
   }

   @Override
   protected Vec3 maybeBackOffFromEdge(Vec3 p_36201_, MoverType p_36202_) {
      float f = this.maxUpStep();
      if (!this.abilities.flying
         && !(p_36201_.y > 0.0)
         && (p_36202_ == MoverType.SELF || p_36202_ == MoverType.PLAYER)
         && this.isStayingOnGroundSurface()
         && this.isAboveGround(f)) {
         double d0 = p_36201_.x;
         double d1 = p_36201_.z;
         double d2 = 0.05;
         double d3 = Math.signum(d0) * 0.05;
         double d4 = Math.signum(d1) * 0.05;

         while (d0 != 0.0 && this.canFallAtLeast(d0, 0.0, f)) {
            if (Math.abs(d0) <= 0.05) {
               d0 = 0.0;
               break;
            }

            d0 -= d3;
         }

         while (d1 != 0.0 && this.canFallAtLeast(0.0, d1, f)) {
            if (Math.abs(d1) <= 0.05) {
               d1 = 0.0;
               break;
            }

            d1 -= d4;
         }

         while (d0 != 0.0 && d1 != 0.0 && this.canFallAtLeast(d0, d1, f)) {
            if (Math.abs(d0) <= 0.05) {
               d0 = 0.0;
            } else {
               d0 -= d3;
            }

            if (Math.abs(d1) <= 0.05) {
               d1 = 0.0;
            } else {
               d1 -= d4;
            }
         }

         return new Vec3(d0, p_36201_.y, d1);
      } else {
         return p_36201_;
      }
   }

   private boolean isAboveGround(float p_328745_) {
      return this.onGround() || this.fallDistance < p_328745_ && !this.canFallAtLeast(0.0, 0.0, p_328745_ - this.fallDistance);
   }

   private boolean canFallAtLeast(double p_333341_, double p_331138_, double p_396282_) {
      AABB aabb = this.getBoundingBox();
      return this.level()
         .noCollision(
            this,
            new AABB(
               aabb.minX + 1.0E-7 + p_333341_,
               aabb.minY - p_396282_ - 1.0E-7,
               aabb.minZ + 1.0E-7 + p_331138_,
               aabb.maxX - 1.0E-7 + p_333341_,
               aabb.minY,
               aabb.maxZ - 1.0E-7 + p_331138_
            )
         );
   }

   public void attack(Entity p_36347_) {
      if (!this.cannotAttack(p_36347_)) {
         float f = this.isAutoSpinAttack() ? this.autoSpinAttackDmg : (float)this.getAttributeValue(Attributes.ATTACK_DAMAGE);
         ItemStack itemstack = this.getWeaponItem();
         DamageSource damagesource = this.createAttackSource(itemstack);
         float f1 = this.getAttackStrengthScale(0.5F);
         float f2 = f1 * (this.getEnchantedDamage(p_36347_, f, damagesource) - f);
         f *= this.baseDamageScaleFactor();
         this.onAttack();
         if (!this.deflectProjectile(p_36347_)) {
            if (f > 0.0F || f2 > 0.0F) {
               boolean flag = f1 > 0.9F;
               boolean flag1;
               if (this.isSprinting() && flag) {
                  this.playServerSideSound(SoundEvents.PLAYER_ATTACK_KNOCKBACK);
                  flag1 = true;
               } else {
                  flag1 = false;
               }

               f += itemstack.getItem().getAttackDamageBonus(p_36347_, f, damagesource);
               boolean flag2 = flag && this.canCriticalAttack(p_36347_);
               if (flag2) {
                  f *= 1.5F;
               }

               float f3 = f + f2;
               boolean flag3 = this.isSweepAttack(flag, flag2, flag1);
               float f4 = 0.0F;
               if (p_36347_ instanceof LivingEntity livingentity) {
                  f4 = livingentity.getHealth();
               }

               Vec3 vec3 = p_36347_.getDeltaMovement();
               boolean flag4 = p_36347_.hurtOrSimulate(damagesource, f3);
               if (flag4) {
                  this.causeExtraKnockback(p_36347_, this.getKnockback(p_36347_, damagesource) + (flag1 ? 0.5F : 0.0F), vec3);
                  if (flag3) {
                     this.doSweepAttack(p_36347_, f, damagesource, f1);
                  }

                  this.attackVisualEffects(p_36347_, flag2, flag3, flag, false, f2);
                  this.setLastHurtMob(p_36347_);
                  this.itemAttackInteraction(p_36347_, itemstack, damagesource, true);
                  this.damageStatsAndHearts(p_36347_, f4);
                  this.causeFoodExhaustion(0.1F);
               } else {
                  this.playServerSideSound(SoundEvents.PLAYER_ATTACK_NODAMAGE);
               }
            }

            this.lungeForwardMaybe();
         }
      }
   }

   private void playServerSideSound(SoundEvent p_459418_) {
      this.level().playSound(null, this.getX(), this.getY(), this.getZ(), p_459418_, this.getSoundSource(), 1.0F, 1.0F);
   }

   private DamageSource createAttackSource(ItemStack p_452215_) {
      return p_452215_.getDamageSource(this, () -> this.damageSources().playerAttack(this));
   }

   private boolean cannotAttack(Entity p_454639_) {
      return !p_454639_.isAttackable() ? true : p_454639_.skipAttackInteraction(this);
   }

   private boolean deflectProjectile(Entity p_453223_) {
      if (p_453223_.getType().is(EntityTypeTags.REDIRECTABLE_PROJECTILE)
         && p_453223_ instanceof Projectile projectile
         && projectile.deflect(ProjectileDeflection.AIM_DEFLECT, this, EntityReference.of(this), true)) {
         this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.PLAYER_ATTACK_NODAMAGE, this.getSoundSource());
         return true;
      } else {
         return false;
      }
   }

   private boolean canCriticalAttack(Entity p_452012_) {
      return this.fallDistance > 0.0
         && !this.onGround()
         && !this.onClimbable()
         && !this.isInWater()
         && !this.isMobilityRestricted()
         && !this.isPassenger()
         && p_452012_ instanceof LivingEntity
         && !this.isSprinting();
   }

   private boolean isSweepAttack(boolean p_457332_, boolean p_453720_, boolean p_458185_) {
      if (p_457332_ && !p_453720_ && !p_458185_ && this.onGround()) {
         double d0 = this.getKnownMovement().horizontalDistanceSqr();
         double d1 = this.getSpeed() * 2.5;
         if (d0 < Mth.square(d1)) {
            return this.getItemInHand(InteractionHand.MAIN_HAND).is(ItemTags.SWORDS);
         }
      }

      return false;
   }

   private void attackVisualEffects(Entity p_451514_, boolean p_454485_, boolean p_460975_, boolean p_456870_, boolean p_455228_, float p_450513_) {
      if (p_454485_) {
         this.playServerSideSound(SoundEvents.PLAYER_ATTACK_CRIT);
         this.crit(p_451514_);
      }

      if (!p_454485_ && !p_460975_ && !p_455228_) {
         this.playServerSideSound(p_456870_ ? SoundEvents.PLAYER_ATTACK_STRONG : SoundEvents.PLAYER_ATTACK_WEAK);
      }

      if (p_450513_ > 0.0F) {
         this.magicCrit(p_451514_);
      }
   }

   private void damageStatsAndHearts(Entity p_459036_, float p_458229_) {
      if (p_459036_ instanceof LivingEntity) {
         float f = p_458229_ - ((LivingEntity)p_459036_).getHealth();
         this.awardStat(Stats.DAMAGE_DEALT, Math.round(f * 10.0F));
         if (this.level() instanceof ServerLevel && f > 2.0F) {
            int i = (int)(f * 0.5);
            ((ServerLevel)this.level())
               .sendParticles(ParticleTypes.DAMAGE_INDICATOR, p_459036_.getX(), p_459036_.getY(0.5), p_459036_.getZ(), i, 0.1, 0.0, 0.1, 0.2);
         }
      }
   }

   private void itemAttackInteraction(Entity p_456980_, ItemStack p_453550_, DamageSource p_453296_, boolean p_457792_) {
      Entity entity = p_456980_;
      if (p_456980_ instanceof EnderDragonPart) {
         entity = ((EnderDragonPart)p_456980_).parentMob;
      }

      boolean flag = false;
      if (this.level() instanceof ServerLevel serverlevel) {
         if (entity instanceof LivingEntity livingentity) {
            flag = p_453550_.hurtEnemy(livingentity, this);
         }

         if (p_457792_) {
            EnchantmentHelper.doPostAttackEffectsWithItemSource(serverlevel, p_456980_, p_453296_, p_453550_);
         }
      }

      if (!this.level().isClientSide() && !p_453550_.isEmpty() && entity instanceof LivingEntity) {
         if (flag) {
            p_453550_.postHurtEnemy((LivingEntity)entity, this);
         }

         if (p_453550_.isEmpty()) {
            if (p_453550_ == this.getMainHandItem()) {
               this.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            } else {
               this.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
            }
         }
      }
   }

   @Override
   public void causeExtraKnockback(Entity p_459829_, float p_454175_, Vec3 p_453113_) {
      if (p_454175_ > 0.0F) {
         if (p_459829_ instanceof LivingEntity livingentity) {
            livingentity.knockback(p_454175_, Mth.sin(this.getYRot() * (float) (Math.PI / 180.0)), -Mth.cos(this.getYRot() * (float) (Math.PI / 180.0)));
         } else {
            p_459829_.push(
               -Mth.sin(this.getYRot() * (float) (Math.PI / 180.0)) * p_454175_, 0.1, Mth.cos(this.getYRot() * (float) (Math.PI / 180.0)) * p_454175_
            );
         }

         this.setDeltaMovement(this.getDeltaMovement().multiply(0.6, 1.0, 0.6));
         this.setSprinting(false);
      }

      if (p_459829_ instanceof ServerPlayer && p_459829_.hurtMarked) {
         ((ServerPlayer)p_459829_).connection.send(new ClientboundSetEntityMotionPacket(p_459829_));
         p_459829_.hurtMarked = false;
         p_459829_.setDeltaMovement(p_453113_);
      }
   }

   @Override
   public float getVoicePitch() {
      return 1.0F;
   }

   private void doSweepAttack(Entity p_451034_, float p_451630_, DamageSource p_460330_, float p_451437_) {
      this.playServerSideSound(SoundEvents.PLAYER_ATTACK_SWEEP);
      if (this.level() instanceof ServerLevel serverlevel) {
         float f = 1.0F + (float)this.getAttributeValue(Attributes.SWEEPING_DAMAGE_RATIO) * p_451630_;

         for (LivingEntity livingentity : this.level().getEntitiesOfClass(LivingEntity.class, p_451034_.getBoundingBox().inflate(1.0, 0.25, 1.0))) {
            if (livingentity != this
               && livingentity != p_451034_
               && !this.isAlliedTo(livingentity)
               && !(livingentity instanceof ArmorStand armorstand && armorstand.isMarker())
               && this.distanceToSqr(livingentity) < 9.0) {
               float f1 = this.getEnchantedDamage(livingentity, f, p_460330_) * p_451437_;
               if (livingentity.hurtServer(serverlevel, p_460330_, f1)) {
                  livingentity.knockback(0.4F, Mth.sin(this.getYRot() * (float) (Math.PI / 180.0)), -Mth.cos(this.getYRot() * (float) (Math.PI / 180.0)));
                  EnchantmentHelper.doPostAttackEffects(serverlevel, livingentity, p_460330_);
               }
            }
         }

         double d0 = -Mth.sin(this.getYRot() * (float) (Math.PI / 180.0));
         double d1 = Mth.cos(this.getYRot() * (float) (Math.PI / 180.0));
         serverlevel.sendParticles(ParticleTypes.SWEEP_ATTACK, this.getX() + d0, this.getY(0.5), this.getZ() + d1, 0, d0, 0.0, d1, 0.0);
      }
   }

   protected float getEnchantedDamage(Entity p_344881_, float p_345044_, DamageSource p_343261_) {
      return p_345044_;
   }

   @Override
   protected void doAutoAttackOnTouch(LivingEntity p_36355_) {
      this.attack(p_36355_);
   }

   public void crit(Entity p_36156_) {
   }

   private float baseDamageScaleFactor() {
      float f = this.getAttackStrengthScale(0.5F);
      return 0.2F + f * f * 0.8F;
   }

   @Override
   public boolean stabAttack(EquipmentSlot p_455987_, Entity p_455965_, float p_450770_, boolean p_456312_, boolean p_459980_, boolean p_456117_) {
      if (this.cannotAttack(p_455965_)) {
         return false;
      }

      ItemStack itemstack = this.getItemBySlot(p_455987_);
      DamageSource damagesource = this.createAttackSource(itemstack);
      float f = this.getEnchantedDamage(p_455965_, p_450770_, damagesource) - p_450770_;
      if (!this.isUsingItem() || this.getUsedItemHand().asEquipmentSlot() != p_455987_) {
         f *= this.getAttackStrengthScale(0.5F);
         p_450770_ *= this.baseDamageScaleFactor();
      }

      if (p_459980_ && this.deflectProjectile(p_455965_)) {
         return true;
      }

      float f1 = p_456312_ ? p_450770_ + f : 0.0F;
      float f2 = 0.0F;
      if (p_455965_ instanceof LivingEntity livingentity) {
         f2 = livingentity.getHealth();
      }

      Vec3 vec3 = p_455965_.getDeltaMovement();
      boolean flag = p_456312_ && p_455965_.hurtOrSimulate(damagesource, f1);
      if (p_459980_) {
         this.causeExtraKnockback(p_455965_, 0.4F + this.getKnockback(p_455965_, damagesource), vec3);
      }

      boolean flag1 = false;
      if (p_456117_ && p_455965_.isPassenger()) {
         flag1 = true;
         p_455965_.stopRiding();
      }

      if (!flag && !p_459980_ && !flag1) {
         return false;
      }

      this.attackVisualEffects(p_455965_, false, false, p_456312_, true, f);
      this.setLastHurtMob(p_455965_);
      this.itemAttackInteraction(p_455965_, itemstack, damagesource, flag);
      this.damageStatsAndHearts(p_455965_, f2);
      this.causeFoodExhaustion(0.1F);
      return true;
   }

   public void magicCrit(Entity p_36253_) {
   }

   @Override
   public void remove(Entity.RemovalReason p_150097_) {
      super.remove(p_150097_);
      this.inventoryMenu.removed(this);
      if (this.hasContainerOpen()) {
         this.doCloseContainer();
      }
   }

   @Override
   public boolean isClientAuthoritative() {
      return true;
   }

   @Override
   protected boolean isLocalClientAuthoritative() {
      return this.isLocalPlayer();
   }

   public boolean isLocalPlayer() {
      return false;
   }

   @Override
   public boolean canSimulateMovement() {
      return !this.level().isClientSide() || this.isLocalPlayer();
   }

   @Override
   public boolean isEffectiveAi() {
      return !this.level().isClientSide() || this.isLocalPlayer();
   }

   public GameProfile getGameProfile() {
      return this.gameProfile;
   }

   public NameAndId nameAndId() {
      return new NameAndId(this.gameProfile);
   }

   public Inventory getInventory() {
      return this.inventory;
   }

   public Abilities getAbilities() {
      return this.abilities;
   }

   @Override
   public boolean hasInfiniteMaterials() {
      return this.abilities.instabuild;
   }

   public boolean preventsBlockDrops() {
      return this.abilities.instabuild;
   }

   public void updateTutorialInventoryAction(ItemStack p_150098_, ItemStack p_150099_, ClickAction p_150100_) {
   }

   public boolean hasContainerOpen() {
      return this.containerMenu != this.inventoryMenu;
   }

   public boolean canDropItems() {
      return true;
   }

   public Either<Player.BedSleepingProblem, Unit> startSleepInBed(BlockPos p_36203_) {
      this.startSleeping(p_36203_);
      this.sleepCounter = 0;
      return Either.right(Unit.INSTANCE);
   }

   public void stopSleepInBed(boolean p_36226_, boolean p_36227_) {
      super.stopSleeping();
      if (this.level() instanceof ServerLevel && p_36227_) {
         ((ServerLevel)this.level()).updateSleepingPlayerList();
      }

      this.sleepCounter = p_36226_ ? 0 : 100;
   }

   @Override
   public void stopSleeping() {
      this.stopSleepInBed(true, true);
   }

   public boolean isSleepingLongEnough() {
      return this.isSleeping() && this.sleepCounter >= 100;
   }

   public int getSleepTimer() {
      return this.sleepCounter;
   }

   public void displayClientMessage(Component p_36216_, boolean p_36217_) {
   }

   public void awardStat(Identifier p_460005_) {
      this.awardStat(Stats.CUSTOM.get(p_460005_));
   }

   public void awardStat(Identifier p_450429_, int p_36224_) {
      this.awardStat(Stats.CUSTOM.get(p_450429_), p_36224_);
   }

   public void awardStat(Stat<?> p_36247_) {
      this.awardStat(p_36247_, 1);
   }

   public void awardStat(Stat<?> p_36145_, int p_36146_) {
   }

   public void resetStat(Stat<?> p_36144_) {
   }

   public int awardRecipes(Collection<RecipeHolder<?>> p_36213_) {
      return 0;
   }

   public void triggerRecipeCrafted(RecipeHolder<?> p_298309_, List<ItemStack> p_283609_) {
   }

   public void awardRecipesByKey(List<ResourceKey<Recipe<?>>> p_312830_) {
   }

   public int resetRecipes(Collection<RecipeHolder<?>> p_36263_) {
      return 0;
   }

   @Override
   public void travel(Vec3 p_36359_) {
      if (this.isPassenger()) {
         super.travel(p_36359_);
      } else {
         if (this.isSwimming()) {
            double d0 = this.getLookAngle().y;
            double d1 = d0 < -0.2 ? 0.085 : 0.06;
            if (d0 <= 0.0 || this.jumping || !this.level().getFluidState(BlockPos.containing(this.getX(), this.getY() + 1.0 - 0.1, this.getZ())).isEmpty()) {
               Vec3 vec3 = this.getDeltaMovement();
               this.setDeltaMovement(vec3.add(0.0, (d0 - vec3.y) * d1, 0.0));
            }
         }

         if (this.getAbilities().flying) {
            double d2 = this.getDeltaMovement().y;
            super.travel(p_36359_);
            this.setDeltaMovement(this.getDeltaMovement().with(Direction.Axis.Y, d2 * 0.6));
         } else {
            super.travel(p_36359_);
         }
      }
   }

   @Override
   protected boolean canGlide() {
      return !this.abilities.flying && super.canGlide();
   }

   @Override
   public void updateSwimming() {
      if (this.abilities.flying) {
         this.setSwimming(false);
      } else {
         super.updateSwimming();
      }
   }

   protected boolean freeAt(BlockPos p_36351_) {
      return !this.level().getBlockState(p_36351_).isSuffocating(this.level(), p_36351_);
   }

   @Override
   public float getSpeed() {
      return (float)this.getAttributeValue(Attributes.MOVEMENT_SPEED);
   }

   @Override
   public boolean causeFallDamage(double p_391627_, float p_150093_, DamageSource p_150095_) {
      if (this.abilities.mayfly) {
         return false;
      }

      if (p_391627_ >= 2.0) {
         this.awardStat(Stats.FALL_ONE_CM, (int)Math.round(p_391627_ * 100.0));
      }

      boolean flag = this.currentImpulseImpactPos != null && this.ignoreFallDamageFromCurrentImpulse;
      double d0;
      if (flag) {
         d0 = Math.min(p_391627_, this.currentImpulseImpactPos.y - this.getY());
         boolean flag1 = d0 <= 0.0;
         if (flag1) {
            this.resetCurrentImpulseContext();
         } else {
            this.tryResetCurrentImpulseContext();
         }
      } else {
         d0 = p_391627_;
      }

      if (d0 > 0.0 && super.causeFallDamage(d0, p_150093_, p_150095_)) {
         this.resetCurrentImpulseContext();
         return true;
      } else {
         this.propagateFallToPassengers(p_391627_, p_150093_, p_150095_);
         return false;
      }
   }

   public boolean tryToStartFallFlying() {
      if (!this.isFallFlying() && this.canGlide() && !this.isInWater()) {
         this.startFallFlying();
         return true;
      } else {
         return false;
      }
   }

   public void startFallFlying() {
      this.setSharedFlag(7, true);
   }

   @Override
   protected void doWaterSplashEffect() {
      if (!this.isSpectator()) {
         super.doWaterSplashEffect();
      }
   }

   @Override
   protected void playStepSound(BlockPos p_282121_, BlockState p_282194_) {
      if (this.isInWater()) {
         this.waterSwimSound();
         this.playMuffledStepSound(p_282194_);
      } else {
         BlockPos blockpos = this.getPrimaryStepSoundBlockPos(p_282121_);
         if (!p_282121_.equals(blockpos)) {
            BlockState blockstate = this.level().getBlockState(blockpos);
            if (blockstate.is(BlockTags.COMBINATION_STEP_SOUND_BLOCKS)) {
               this.playCombinationStepSounds(blockstate, p_282194_);
            } else {
               super.playStepSound(blockpos, blockstate);
            }
         } else {
            super.playStepSound(p_282121_, p_282194_);
         }
      }
   }

   @Override
   public LivingEntity.Fallsounds getFallSounds() {
      return new LivingEntity.Fallsounds(SoundEvents.PLAYER_SMALL_FALL, SoundEvents.PLAYER_BIG_FALL);
   }

   @Override
   public boolean killedEntity(ServerLevel p_219735_, LivingEntity p_219736_, DamageSource p_426979_) {
      this.awardStat(Stats.ENTITY_KILLED.get(p_219736_.getType()));
      return true;
   }

   @Override
   public void makeStuckInBlock(BlockState p_36196_, Vec3 p_36197_) {
      if (!this.abilities.flying) {
         super.makeStuckInBlock(p_36196_, p_36197_);
      }

      this.tryResetCurrentImpulseContext();
   }

   public void giveExperiencePoints(int p_36291_) {
      this.increaseScore(p_36291_);
      this.experienceProgress = this.experienceProgress + (float)p_36291_ / this.getXpNeededForNextLevel();
      this.totalExperience = Mth.clamp(this.totalExperience + p_36291_, 0, Integer.MAX_VALUE);

      while (this.experienceProgress < 0.0F) {
         float f = this.experienceProgress * this.getXpNeededForNextLevel();
         if (this.experienceLevel > 0) {
            this.giveExperienceLevels(-1);
            this.experienceProgress = 1.0F + f / this.getXpNeededForNextLevel();
         } else {
            this.giveExperienceLevels(-1);
            this.experienceProgress = 0.0F;
         }
      }

      while (this.experienceProgress >= 1.0F) {
         this.experienceProgress = (this.experienceProgress - 1.0F) * this.getXpNeededForNextLevel();
         this.giveExperienceLevels(1);
         this.experienceProgress = this.experienceProgress / this.getXpNeededForNextLevel();
      }
   }

   public int getEnchantmentSeed() {
      return this.enchantmentSeed;
   }

   public void onEnchantmentPerformed(ItemStack p_36172_, int p_36173_) {
      this.experienceLevel -= p_36173_;
      if (this.experienceLevel < 0) {
         this.experienceLevel = 0;
         this.experienceProgress = 0.0F;
         this.totalExperience = 0;
      }

      this.enchantmentSeed = this.random.nextInt();
   }

   public void giveExperienceLevels(int p_36276_) {
      this.experienceLevel = IntMath.saturatedAdd(this.experienceLevel, p_36276_);
      if (this.experienceLevel < 0) {
         this.experienceLevel = 0;
         this.experienceProgress = 0.0F;
         this.totalExperience = 0;
      }

      if (p_36276_ > 0 && this.experienceLevel % 5 == 0 && this.lastLevelUpTime < this.tickCount - 100.0F) {
         float f = this.experienceLevel > 30 ? 1.0F : this.experienceLevel / 30.0F;
         this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.PLAYER_LEVELUP, this.getSoundSource(), f * 0.75F, 1.0F);
         this.lastLevelUpTime = this.tickCount;
      }
   }

   public int getXpNeededForNextLevel() {
      if (this.experienceLevel >= 30) {
         return 112 + (this.experienceLevel - 30) * 9;
      } else {
         return this.experienceLevel >= 15 ? 37 + (this.experienceLevel - 15) * 5 : 7 + this.experienceLevel * 2;
      }
   }

   public void causeFoodExhaustion(float p_36400_) {
      if (!this.abilities.invulnerable && !this.level().isClientSide()) {
         this.foodData.addExhaustion(p_36400_);
      }
   }

   @Override
   public void lungeForwardMaybe() {
      if (this.hasEnoughFoodToDoExhaustiveManoeuvres()) {
         super.lungeForwardMaybe();
      }
   }

   protected boolean hasEnoughFoodToDoExhaustiveManoeuvres() {
      return this.getFoodData().hasEnoughFood() || this.getAbilities().mayfly;
   }

   public Optional<WardenSpawnTracker> getWardenSpawnTracker() {
      return Optional.empty();
   }

   public FoodData getFoodData() {
      return this.foodData;
   }

   public boolean canEat(boolean p_36392_) {
      return this.abilities.invulnerable || p_36392_ || this.foodData.needsFood();
   }

   public boolean isHurt() {
      return this.getHealth() > 0.0F && this.getHealth() < this.getMaxHealth();
   }

   public boolean mayBuild() {
      return this.abilities.mayBuild;
   }

   public boolean mayUseItemAt(BlockPos p_36205_, Direction p_36206_, ItemStack p_36207_) {
      if (this.abilities.mayBuild) {
         return true;
      }

      BlockPos blockpos = p_36205_.relative(p_36206_.getOpposite());
      BlockInWorld blockinworld = new BlockInWorld(this.level(), blockpos, false);
      return p_36207_.canPlaceOnBlockInAdventureMode(blockinworld);
   }

   @Override
   protected int getBaseExperienceReward(ServerLevel p_361105_) {
      return !p_361105_.getGameRules().get(GameRules.KEEP_INVENTORY) && !this.isSpectator() ? Math.min(this.experienceLevel * 7, 100) : 0;
   }

   @Override
   protected boolean isAlwaysExperienceDropper() {
      return true;
   }

   @Override
   public boolean shouldShowName() {
      return true;
   }

   @Override
   protected Entity.MovementEmission getMovementEmission() {
      return !this.abilities.flying && (!this.onGround() || !this.isDiscrete()) ? Entity.MovementEmission.ALL : Entity.MovementEmission.NONE;
   }

   public void onUpdateAbilities() {
   }

   @Override
   public Component getName() {
      return Component.literal(this.gameProfile.name());
   }

   @Override
   public String getPlainTextName() {
      return this.gameProfile.name();
   }

   public PlayerEnderChestContainer getEnderChestInventory() {
      return this.enderChestInventory;
   }

   @Override
   protected boolean doesEmitEquipEvent(EquipmentSlot p_219741_) {
      return p_219741_.getType() == EquipmentSlot.Type.HUMANOID_ARMOR;
   }

   public boolean addItem(ItemStack p_36357_) {
      return this.inventory.add(p_36357_);
   }

   public abstract @Nullable GameType gameMode();

   @Override
   public boolean isSpectator() {
      return this.gameMode() == GameType.SPECTATOR;
   }

   @Override
   public boolean canBeHitByProjectile() {
      return !this.isSpectator() && super.canBeHitByProjectile();
   }

   @Override
   public boolean isSwimming() {
      return !this.abilities.flying && !this.isSpectator() && super.isSwimming();
   }

   public boolean isCreative() {
      return this.gameMode() == GameType.CREATIVE;
   }

   @Override
   public boolean isPushedByFluid() {
      return !this.abilities.flying;
   }

   @Override
   public Component getDisplayName() {
      MutableComponent mutablecomponent = PlayerTeam.formatNameForTeam(this.getTeam(), this.getName());
      return this.decorateDisplayNameComponent(mutablecomponent);
   }

   private MutableComponent decorateDisplayNameComponent(MutableComponent p_36219_) {
      String s = this.getGameProfile().name();
      return p_36219_.withStyle(
         p_449729_ -> p_449729_.withClickEvent(new ClickEvent.SuggestCommand("/tell " + s + " ")).withHoverEvent(this.createHoverEvent()).withInsertion(s)
      );
   }

   @Override
   public String getScoreboardName() {
      return this.getGameProfile().name();
   }

   @Override
   protected void internalSetAbsorptionAmount(float p_301235_) {
      this.getEntityData().set(DATA_PLAYER_ABSORPTION_ID, p_301235_);
   }

   @Override
   public float getAbsorptionAmount() {
      return this.getEntityData().get(DATA_PLAYER_ABSORPTION_ID);
   }

   @Override
   public @Nullable SlotAccess getSlot(int p_150112_) {
      if (p_150112_ == 499) {
         return new SlotAccess() {
            @Override
            public ItemStack get() {
               return Player.this.containerMenu.getCarried();
            }

            @Override
            public boolean set(ItemStack p_333834_) {
               Player.this.containerMenu.setCarried(p_333834_);
               return true;
            }
         };
      }

      final int i = p_150112_ - 500;
      if (i >= 0 && i < 4) {
         return new SlotAccess() {
            @Override
            public ItemStack get() {
               return Player.this.inventoryMenu.getCraftSlots().getItem(i);
            }

            @Override
            public boolean set(ItemStack p_333999_) {
               Player.this.inventoryMenu.getCraftSlots().setItem(i, p_333999_);
               Player.this.inventoryMenu.slotsChanged(Player.this.inventory);
               return true;
            }
         };
      }

      if (p_150112_ >= 0 && p_150112_ < this.inventory.getNonEquipmentItems().size()) {
         return this.inventory.getSlot(p_150112_);
      }

      int j = p_150112_ - 200;
      return j >= 0 && j < this.enderChestInventory.getContainerSize() ? this.enderChestInventory.getSlot(j) : super.getSlot(p_150112_);
   }

   public boolean isReducedDebugInfo() {
      return this.reducedDebugInfo;
   }

   public void setReducedDebugInfo(boolean p_36394_) {
      this.reducedDebugInfo = p_36394_;
   }

   @Override
   public void setRemainingFireTicks(int p_36353_) {
      super.setRemainingFireTicks(this.abilities.invulnerable ? Math.min(p_36353_, 1) : p_36353_);
   }

   protected static Optional<Parrot.Variant> extractParrotVariant(CompoundTag p_427462_) {
      if (!p_427462_.isEmpty()) {
         EntityType<?> entitytype = p_427462_.<EntityType<?>>read("id", EntityType.CODEC).orElse(null);
         if (entitytype == EntityType.PARROT) {
            return p_427462_.read("Variant", Parrot.Variant.LEGACY_CODEC);
         }
      }

      return Optional.empty();
   }

   protected static OptionalInt convertParrotVariant(Optional<Parrot.Variant> p_430527_) {
      return p_430527_.<OptionalInt>map(p_449730_ -> OptionalInt.of(p_449730_.getId())).orElse(OptionalInt.empty());
   }

   private static Optional<Parrot.Variant> convertParrotVariant(OptionalInt p_429721_) {
      return p_429721_.isPresent() ? Optional.of(Parrot.Variant.byId(p_429721_.getAsInt())) : Optional.empty();
   }

   public void setShoulderParrotLeft(Optional<Parrot.Variant> p_429312_) {
      this.entityData.set(DATA_SHOULDER_PARROT_LEFT, convertParrotVariant(p_429312_));
   }

   public Optional<Parrot.Variant> getShoulderParrotLeft() {
      return convertParrotVariant(this.entityData.get(DATA_SHOULDER_PARROT_LEFT));
   }

   public void setShoulderParrotRight(Optional<Parrot.Variant> p_425770_) {
      this.entityData.set(DATA_SHOULDER_PARROT_RIGHT, convertParrotVariant(p_425770_));
   }

   public Optional<Parrot.Variant> getShoulderParrotRight() {
      return convertParrotVariant(this.entityData.get(DATA_SHOULDER_PARROT_RIGHT));
   }

   public float getCurrentItemAttackStrengthDelay() {
      return (float)(1.0 / this.getAttributeValue(Attributes.ATTACK_SPEED) * 20.0);
   }

   public boolean cannotAttackWithItem(ItemStack p_455927_, int p_456383_) {
      float f = p_455927_.getOrDefault(DataComponents.MINIMUM_ATTACK_CHARGE, 0.0F);
      float f1 = (this.attackStrengthTicker + p_456383_) / this.getCurrentItemAttackStrengthDelay();
      return f > 0.0F && f1 < f;
   }

   public float getAttackStrengthScale(float p_36404_) {
      return Mth.clamp((this.attackStrengthTicker + p_36404_) / this.getCurrentItemAttackStrengthDelay(), 0.0F, 1.0F);
   }

   public float getItemSwapScale(float p_459344_) {
      return Mth.clamp((this.itemSwapTicker + p_459344_) / this.getCurrentItemAttackStrengthDelay(), 0.0F, 1.0F);
   }

   public void resetAttackStrengthTicker() {
      this.attackStrengthTicker = 0;
      this.itemSwapTicker = 0;
   }

   @Override
   public void onAttack() {
      this.resetOnlyAttackStrengthTicker();
      super.onAttack();
   }

   public void resetOnlyAttackStrengthTicker() {
      this.attackStrengthTicker = 0;
   }

   public ItemCooldowns getCooldowns() {
      return this.cooldowns;
   }

   @Override
   protected float getBlockSpeedFactor() {
      return !this.abilities.flying && !this.isFallFlying() ? super.getBlockSpeedFactor() : 1.0F;
   }

   @Override
   public float getLuck() {
      return (float)this.getAttributeValue(Attributes.LUCK);
   }

   public boolean canUseGameMasterBlocks() {
      return this.abilities.instabuild && this.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
   }

   public PermissionSet permissions() {
      return PermissionSet.NO_PERMISSIONS;
   }

   @Override
   public ImmutableList<Pose> getDismountPoses() {
      return ImmutableList.of(Pose.STANDING, Pose.CROUCHING, Pose.SWIMMING);
   }

   @Override
   public ItemStack getProjectile(ItemStack p_36349_) {
      if (!(p_36349_.getItem() instanceof ProjectileWeaponItem)) {
         return ItemStack.EMPTY;
      }

      Predicate<ItemStack> predicate = ((ProjectileWeaponItem)p_36349_.getItem()).getSupportedHeldProjectiles();
      ItemStack itemstack = ProjectileWeaponItem.getHeldProjectile(this, predicate);
      if (!itemstack.isEmpty()) {
         return itemstack;
      }

      predicate = ((ProjectileWeaponItem)p_36349_.getItem()).getAllSupportedProjectiles();

      for (int i = 0; i < this.inventory.getContainerSize(); i++) {
         ItemStack itemstack1 = this.inventory.getItem(i);
         if (predicate.test(itemstack1)) {
            return itemstack1;
         }
      }

      return this.hasInfiniteMaterials() ? new ItemStack(Items.ARROW) : ItemStack.EMPTY;
   }

   @Override
   public Vec3 getRopeHoldPosition(float p_36374_) {
      double d0 = 0.22 * (this.getMainArm() == HumanoidArm.RIGHT ? -1.0 : 1.0);
      float f = Mth.lerp(p_36374_ * 0.5F, this.getXRot(), this.xRotO) * (float) (Math.PI / 180.0);
      float f1 = Mth.lerp(p_36374_, this.yBodyRotO, this.yBodyRot) * (float) (Math.PI / 180.0);
      if (this.isFallFlying() || this.isAutoSpinAttack()) {
         Vec3 vec31 = this.getViewVector(p_36374_);
         Vec3 vec3 = this.getDeltaMovement();
         double d6 = vec3.horizontalDistanceSqr();
         double d3 = vec31.horizontalDistanceSqr();
         float f2;
         if (d6 > 0.0 && d3 > 0.0) {
            double d4 = (vec3.x * vec31.x + vec3.z * vec31.z) / Math.sqrt(d6 * d3);
            double d5 = vec3.x * vec31.z - vec3.z * vec31.x;
            f2 = (float)(Math.signum(d5) * Math.acos(d4));
         } else {
            f2 = 0.0F;
         }

         return this.getPosition(p_36374_).add(new Vec3(d0, -0.11, 0.85).zRot(-f2).xRot(-f).yRot(-f1));
      } else {
         if (this.isVisuallySwimming()) {
            return this.getPosition(p_36374_).add(new Vec3(d0, 0.2, -0.15).xRot(-f).yRot(-f1));
         }

         double d1 = this.getBoundingBox().getYsize() - 1.0;
         double d2 = this.isCrouching() ? -0.2 : 0.07;
         return this.getPosition(p_36374_).add(new Vec3(d0, d1, d2).yRot(-f1));
      }
   }

   @Override
   public boolean isAlwaysTicking() {
      return true;
   }

   public boolean isScoping() {
      return this.isUsingItem() && this.getUseItem().is(Items.SPYGLASS);
   }

   @Override
   public boolean shouldBeSaved() {
      return false;
   }

   public Optional<GlobalPos> getLastDeathLocation() {
      return this.lastDeathLocation;
   }

   public void setLastDeathLocation(Optional<GlobalPos> p_219750_) {
      this.lastDeathLocation = p_219750_;
   }

   @Override
   public float getHurtDir() {
      return this.hurtDir;
   }

   @Override
   public void animateHurt(float p_265280_) {
      super.animateHurt(p_265280_);
      this.hurtDir = p_265280_;
   }

   public boolean isMobilityRestricted() {
      return this.hasEffect(MobEffects.BLINDNESS);
   }

   @Override
   public boolean canSprint() {
      return true;
   }

   @Override
   protected float getFlyingSpeed() {
      if (this.abilities.flying && !this.isPassenger()) {
         return this.isSprinting() ? this.abilities.getFlyingSpeed() * 2.0F : this.abilities.getFlyingSpeed();
      } else {
         return this.isSprinting() ? 0.025999999F : 0.02F;
      }
   }

   @Override
   public boolean hasContainerOpen(ContainerOpenersCounter p_430021_, BlockPos p_426016_) {
      return p_430021_.isOwnContainer(this);
   }

   @Override
   public double getContainerInteractionRange() {
      return this.blockInteractionRange();
   }

   public double blockInteractionRange() {
      return this.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE);
   }

   public double entityInteractionRange() {
      return this.getAttributeValue(Attributes.ENTITY_INTERACTION_RANGE);
   }

   public boolean isWithinEntityInteractionRange(Entity p_452142_, double p_459585_) {
      return p_452142_.isRemoved() ? false : this.isWithinEntityInteractionRange(p_452142_.getBoundingBox(), p_459585_);
   }

   public boolean isWithinEntityInteractionRange(AABB p_458236_, double p_450766_) {
      double d0 = this.entityInteractionRange() + p_450766_;
      double d1 = p_458236_.distanceToSqr(this.getEyePosition());
      return d1 < d0 * d0;
   }

   public boolean isWithinAttackRange(AABB p_459777_, double p_452454_) {
      return this.entityAttackRange().isInRange(this, p_459777_, p_452454_);
   }

   public boolean isWithinBlockInteractionRange(BlockPos p_457395_, double p_454293_) {
      double d0 = this.blockInteractionRange() + p_454293_;
      return new AABB(p_457395_).distanceToSqr(this.getEyePosition()) < d0 * d0;
   }

   public void setIgnoreFallDamageFromCurrentImpulse(boolean p_344459_) {
      this.ignoreFallDamageFromCurrentImpulse = p_344459_;
      if (p_344459_) {
         this.applyPostImpulseGraceTime(40);
      } else {
         this.currentImpulseContextResetGraceTime = 0;
      }
   }

   public void applyPostImpulseGraceTime(int p_453563_) {
      this.currentImpulseContextResetGraceTime = Math.max(this.currentImpulseContextResetGraceTime, p_453563_);
   }

   public boolean isIgnoringFallDamageFromCurrentImpulse() {
      return this.ignoreFallDamageFromCurrentImpulse;
   }

   public void tryResetCurrentImpulseContext() {
      if (this.currentImpulseContextResetGraceTime == 0) {
         this.resetCurrentImpulseContext();
      }
   }

   public boolean isInPostImpulseGraceTime() {
      return this.currentImpulseContextResetGraceTime > 0;
   }

   public void resetCurrentImpulseContext() {
      this.currentImpulseContextResetGraceTime = 0;
      this.currentExplosionCause = null;
      this.currentImpulseImpactPos = null;
      this.ignoreFallDamageFromCurrentImpulse = false;
   }

   public boolean shouldRotateWithMinecart() {
      return false;
   }

   @Override
   public boolean onClimbable() {
      return this.abilities.flying ? false : super.onClimbable();
   }

   public String debugInfo() {
      return MoreObjects.toStringHelper(this)
         .add("name", this.getPlainTextName())
         .add("id", this.getId())
         .add("pos", this.position())
         .add("mode", this.gameMode())
         .add("permission", this.permissions())
         .toString();
   }

   public record BedSleepingProblem(@Nullable Component message) {
      public static final Player.BedSleepingProblem TOO_FAR_AWAY = new Player.BedSleepingProblem(Component.translatable("block.minecraft.bed.too_far_away"));
      public static final Player.BedSleepingProblem OBSTRUCTED = new Player.BedSleepingProblem(Component.translatable("block.minecraft.bed.obstructed"));
      public static final Player.BedSleepingProblem OTHER_PROBLEM = new Player.BedSleepingProblem(null);
      public static final Player.BedSleepingProblem NOT_SAFE = new Player.BedSleepingProblem(Component.translatable("block.minecraft.bed.not_safe"));
   }
}
