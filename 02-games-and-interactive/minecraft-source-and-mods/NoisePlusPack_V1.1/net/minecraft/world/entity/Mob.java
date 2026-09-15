package net.minecraft.world.entity;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Maps;
import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Vec3i;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.debug.DebugBrainDump;
import net.minecraft.util.debug.DebugGoalInfo;
import net.minecraft.util.debug.DebugPathInfo;
import net.minecraft.util.debug.DebugSubscriptions;
import net.minecraft.util.debug.DebugValueSource;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.Container;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.BodyRotationControl;
import net.minecraft.world.entity.ai.control.JumpControl;
import net.minecraft.world.entity.ai.control.LookControl;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.sensing.Sensing;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.component.AttackRange;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.UseRemainder;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.enchantment.providers.VanillaEnchantmentProviders;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.ticks.ContainerSingleItem;
import org.jspecify.annotations.Nullable;

public abstract class Mob extends LivingEntity implements EquipmentUser, Leashable, Targeting {
   private static final EntityDataAccessor<Byte> DATA_MOB_FLAGS_ID = SynchedEntityData.defineId(Mob.class, EntityDataSerializers.BYTE);
   private static final int MOB_FLAG_NO_AI = 1;
   private static final int MOB_FLAG_LEFTHANDED = 2;
   private static final int MOB_FLAG_AGGRESSIVE = 4;
   protected static final int PICKUP_REACH = 1;
   private static final Vec3i ITEM_PICKUP_REACH = new Vec3i(1, 0, 1);
   private static final List<EquipmentSlot> EQUIPMENT_POPULATION_ORDER = List.of(
      EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
   );
   public static final float MAX_WEARING_ARMOR_CHANCE = 0.15F;
   public static final float WEARING_ARMOR_UPGRADE_MATERIAL_CHANCE = 0.1087F;
   public static final float WEARING_ARMOR_UPGRADE_MATERIAL_ATTEMPTS = 3.0F;
   public static final float MAX_PICKUP_LOOT_CHANCE = 0.55F;
   public static final float MAX_ENCHANTED_ARMOR_CHANCE = 0.5F;
   public static final float MAX_ENCHANTED_WEAPON_CHANCE = 0.25F;
   public static final int UPDATE_GOAL_SELECTOR_EVERY_N_TICKS = 2;
   private static final double DEFAULT_ATTACK_REACH = Math.sqrt(2.04F) - 0.6F;
   private static final boolean DEFAULT_CAN_PICK_UP_LOOT = false;
   private static final boolean DEFAULT_PERSISTENCE_REQUIRED = false;
   private static final boolean DEFAULT_LEFT_HANDED = false;
   private static final boolean DEFAULT_NO_AI = false;
   protected static final Identifier RANDOM_SPAWN_BONUS_ID = Identifier.withDefaultNamespace("random_spawn_bonus");
   public static final String TAG_DROP_CHANCES = "drop_chances";
   public static final String TAG_LEFT_HANDED = "LeftHanded";
   public static final String TAG_CAN_PICK_UP_LOOT = "CanPickUpLoot";
   public static final String TAG_NO_AI = "NoAI";
   public int ambientSoundTime;
   protected int xpReward;
   protected LookControl lookControl;
   protected MoveControl moveControl;
   protected JumpControl jumpControl;
   private final BodyRotationControl bodyRotationControl;
   protected PathNavigation navigation;
   protected final GoalSelector goalSelector;
   protected final GoalSelector targetSelector;
   private @Nullable LivingEntity target;
   private final Sensing sensing;
   private DropChances dropChances = DropChances.DEFAULT;
   private boolean canPickUpLoot = false;
   private boolean persistenceRequired = false;
   private final Map<PathType, Float> pathfindingMalus = Maps.newEnumMap(PathType.class);
   private Optional<ResourceKey<LootTable>> lootTable = Optional.empty();
   private long lootTableSeed;
   private Leashable.@Nullable LeashData leashData;
   private BlockPos homePosition = BlockPos.ZERO;
   private int homeRadius = -1;

   protected Mob(EntityType<? extends Mob> p_21368_, Level p_21369_) {
      super(p_21368_, p_21369_);
      this.goalSelector = new GoalSelector();
      this.targetSelector = new GoalSelector();
      this.lookControl = new LookControl(this);
      this.moveControl = new MoveControl(this);
      this.jumpControl = new JumpControl(this);
      this.bodyRotationControl = this.createBodyControl();
      this.navigation = this.createNavigation(p_21369_);
      this.sensing = new Sensing(this);
      if (p_21369_ instanceof ServerLevel) {
         this.registerGoals();
      }
   }

   protected void registerGoals() {
   }

   public static AttributeSupplier.Builder createMobAttributes() {
      return LivingEntity.createLivingAttributes().add(Attributes.FOLLOW_RANGE, 16.0);
   }

   protected PathNavigation createNavigation(Level p_21480_) {
      return new GroundPathNavigation(this, p_21480_);
   }

   protected boolean shouldPassengersInheritMalus() {
      return false;
   }

   public float getPathfindingMalus(PathType p_334857_) {
      Mob mob;
      if (this.getControlledVehicle() instanceof Mob mob1 && mob1.shouldPassengersInheritMalus()) {
         mob = mob1;
      } else {
         mob = this;
      }

      Float f = mob.pathfindingMalus.get(p_334857_);
      return f == null ? p_334857_.getMalus() : f;
   }

   public void setPathfindingMalus(PathType p_332507_, float p_21443_) {
      this.pathfindingMalus.put(p_332507_, p_21443_);
   }

   public void onPathfindingStart() {
   }

   public void onPathfindingDone() {
   }

   protected BodyRotationControl createBodyControl() {
      return new BodyRotationControl(this);
   }

   public LookControl getLookControl() {
      return this.lookControl;
   }

   public MoveControl getMoveControl() {
      return this.getControlledVehicle() instanceof Mob mob ? mob.getMoveControl() : this.moveControl;
   }

   public JumpControl getJumpControl() {
      return this.jumpControl;
   }

   public PathNavigation getNavigation() {
      return this.getControlledVehicle() instanceof Mob mob ? mob.getNavigation() : this.navigation;
   }

   @Override
   public @Nullable LivingEntity getControllingPassenger() {
      Entity entity = this.getFirstPassenger();
      return !this.isNoAi() && entity instanceof Mob mob && entity.canControlVehicle() ? mob : null;
   }

   public Sensing getSensing() {
      return this.sensing;
   }

   @Override
   public @Nullable LivingEntity getTarget() {
      return this.target;
   }

   protected final @Nullable LivingEntity getTargetFromBrain() {
      return this.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).orElse(null);
   }

   public void setTarget(@Nullable LivingEntity p_21544_) {
      this.target = p_21544_;
   }

   @Override
   public boolean canAttackType(EntityType<?> p_21399_) {
      return p_21399_ != EntityType.GHAST;
   }

   public boolean canUseNonMeleeWeapon(ItemStack p_460895_) {
      return false;
   }

   public void ate() {
      this.gameEvent(GameEvent.EAT);
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder p_335882_) {
      super.defineSynchedData(p_335882_);
      p_335882_.define(DATA_MOB_FLAGS_ID, (byte)0);
   }

   public int getAmbientSoundInterval() {
      return 80;
   }

   public void playAmbientSound() {
      this.makeSound(this.getAmbientSound());
   }

   @Override
   public void baseTick() {
      super.baseTick();
      ProfilerFiller profilerfiller = Profiler.get();
      profilerfiller.push("mobBaseTick");
      if (this.isAlive() && this.random.nextInt(1000) < this.ambientSoundTime++) {
         this.resetAmbientSoundTime();
         this.playAmbientSound();
      }

      profilerfiller.pop();
   }

   @Override
   protected void playHurtSound(DamageSource p_21493_) {
      this.resetAmbientSoundTime();
      super.playHurtSound(p_21493_);
   }

   private void resetAmbientSoundTime() {
      this.ambientSoundTime = -this.getAmbientSoundInterval();
   }

   @Override
   protected int getBaseExperienceReward(ServerLevel p_369877_) {
      if (this.xpReward > 0) {
         int i = this.xpReward;

         for (EquipmentSlot equipmentslot : EquipmentSlot.VALUES) {
            if (equipmentslot.canIncreaseExperience()) {
               ItemStack itemstack = this.getItemBySlot(equipmentslot);
               if (!itemstack.isEmpty() && this.dropChances.byEquipment(equipmentslot) <= 1.0F) {
                  i += 1 + this.random.nextInt(3);
               }
            }
         }

         return i;
      } else {
         return this.xpReward;
      }
   }

   public void spawnAnim() {
      if (this.level().isClientSide()) {
         this.makePoofParticles();
      } else {
         this.level().broadcastEntityEvent(this, (byte)20);
      }
   }

   @Override
   public void handleEntityEvent(byte p_21375_) {
      if (p_21375_ == 20) {
         this.spawnAnim();
      } else {
         super.handleEntityEvent(p_21375_);
      }
   }

   @Override
   public void tick() {
      super.tick();
      if (!this.level().isClientSide() && this.tickCount % 5 == 0) {
         this.updateControlFlags();
      }
   }

   protected void updateControlFlags() {
      boolean flag = !(this.getControllingPassenger() instanceof Mob);
      boolean flag1 = !(this.getVehicle() instanceof AbstractBoat);
      this.goalSelector.setControlFlag(Goal.Flag.MOVE, flag);
      this.goalSelector.setControlFlag(Goal.Flag.JUMP, flag && flag1);
      this.goalSelector.setControlFlag(Goal.Flag.LOOK, flag);
   }

   @Override
   protected void tickHeadTurn(float p_21538_) {
      this.bodyRotationControl.clientTick();
   }

   protected @Nullable SoundEvent getAmbientSound() {
      return null;
   }

   @Override
   protected void addAdditionalSaveData(ValueOutput p_409238_) {
      super.addAdditionalSaveData(p_409238_);
      p_409238_.putBoolean("CanPickUpLoot", this.canPickUpLoot());
      p_409238_.putBoolean("PersistenceRequired", this.persistenceRequired);
      if (!this.dropChances.equals(DropChances.DEFAULT)) {
         p_409238_.store("drop_chances", DropChances.CODEC, this.dropChances);
      }

      this.writeLeashData(p_409238_, this.leashData);
      if (this.hasHome()) {
         p_409238_.putInt("home_radius", this.homeRadius);
         p_409238_.store("home_pos", BlockPos.CODEC, this.homePosition);
      }

      p_409238_.putBoolean("LeftHanded", this.isLeftHanded());
      this.lootTable.ifPresent(p_405293_ -> p_409238_.store("DeathLootTable", LootTable.KEY_CODEC, p_405293_));
      if (this.lootTableSeed != 0L) {
         p_409238_.putLong("DeathLootTableSeed", this.lootTableSeed);
      }

      if (this.isNoAi()) {
         p_409238_.putBoolean("NoAI", this.isNoAi());
      }
   }

   @Override
   protected void readAdditionalSaveData(ValueInput p_408489_) {
      super.readAdditionalSaveData(p_408489_);
      this.setCanPickUpLoot(p_408489_.getBooleanOr("CanPickUpLoot", false));
      this.persistenceRequired = p_408489_.getBooleanOr("PersistenceRequired", false);
      this.dropChances = p_408489_.<DropChances>read("drop_chances", DropChances.CODEC).orElse(DropChances.DEFAULT);
      this.readLeashData(p_408489_);
      this.homeRadius = p_408489_.getIntOr("home_radius", -1);
      if (this.homeRadius >= 0) {
         this.homePosition = p_408489_.<BlockPos>read("home_pos", BlockPos.CODEC).orElse(BlockPos.ZERO);
      }

      this.setLeftHanded(p_408489_.getBooleanOr("LeftHanded", false));
      this.lootTable = p_408489_.read("DeathLootTable", LootTable.KEY_CODEC);
      this.lootTableSeed = p_408489_.getLongOr("DeathLootTableSeed", 0L);
      this.setNoAi(p_408489_.getBooleanOr("NoAI", false));
   }

   @Override
   protected void dropFromLootTable(ServerLevel p_367479_, DamageSource p_21389_, boolean p_21390_) {
      super.dropFromLootTable(p_367479_, p_21389_, p_21390_);
      this.lootTable = Optional.empty();
   }

   @Override
   public final Optional<ResourceKey<LootTable>> getLootTable() {
      return this.lootTable.isPresent() ? this.lootTable : super.getLootTable();
   }

   @Override
   public long getLootTableSeed() {
      return this.lootTableSeed;
   }

   public void setZza(float p_21565_) {
      this.zza = p_21565_;
   }

   public void setYya(float p_21568_) {
      this.yya = p_21568_;
   }

   public void setXxa(float p_21571_) {
      this.xxa = p_21571_;
   }

   @Override
   public void setSpeed(float p_21556_) {
      super.setSpeed(p_21556_);
      this.setZza(p_21556_);
   }

   public void stopInPlace() {
      this.getNavigation().stop();
      this.setXxa(0.0F);
      this.setYya(0.0F);
      this.setSpeed(0.0F);
      this.setDeltaMovement(0.0, 0.0, 0.0);
      this.resetAngularLeashMomentum();
   }

   @Override
   public void aiStep() {
      super.aiStep();
      if (this.getType().is(EntityTypeTags.BURN_IN_DAYLIGHT)) {
         this.burnUndead();
      }

      ProfilerFiller profilerfiller = Profiler.get();
      profilerfiller.push("looting");
      if (this.level() instanceof ServerLevel serverlevel
         && this.canPickUpLoot()
         && this.isAlive()
         && !this.dead
         && serverlevel.getGameRules().get(GameRules.MOB_GRIEFING)) {
         Vec3i vec3i = this.getPickupReach();

         for (ItemEntity itementity : this.level()
            .getEntitiesOfClass(ItemEntity.class, this.getBoundingBox().inflate(vec3i.getX(), vec3i.getY(), vec3i.getZ()))) {
            if (!itementity.isRemoved()
               && !itementity.getItem().isEmpty()
               && !itementity.hasPickUpDelay()
               && this.wantsToPickUp(serverlevel, itementity.getItem())) {
               this.pickUpItem(serverlevel, itementity);
            }
         }
      }

      profilerfiller.pop();
   }

   protected EquipmentSlot sunProtectionSlot() {
      return EquipmentSlot.HEAD;
   }

   private void burnUndead() {
      if (this.isAlive() && this.isSunBurnTick()) {
         EquipmentSlot equipmentslot = this.sunProtectionSlot();
         ItemStack itemstack = this.getItemBySlot(equipmentslot);
         if (!itemstack.isEmpty()) {
            if (itemstack.isDamageableItem()) {
               Item item = itemstack.getItem();
               itemstack.setDamageValue(itemstack.getDamageValue() + this.random.nextInt(2));
               if (itemstack.getDamageValue() >= itemstack.getMaxDamage()) {
                  this.onEquippedItemBroken(item, equipmentslot);
                  this.setItemSlot(equipmentslot, ItemStack.EMPTY);
               }
            }
         } else {
            this.igniteForSeconds(8.0F);
         }
      }
   }

   private boolean isSunBurnTick() {
      if (!this.level().isClientSide() && this.level().environmentAttributes().getValue(EnvironmentAttributes.MONSTERS_BURN, this.position())) {
         float f = this.getLightLevelDependentMagicValue();
         BlockPos blockpos = BlockPos.containing(this.getX(), this.getEyeY(), this.getZ());
         boolean flag = this.isInWaterOrRain() || this.isInPowderSnow || this.wasInPowderSnow;
         if (f > 0.5F && this.random.nextFloat() * 30.0F < (f - 0.4F) * 2.0F && !flag && this.level().canSeeSky(blockpos)) {
            return true;
         }
      }

      return false;
   }

   protected Vec3i getPickupReach() {
      return ITEM_PICKUP_REACH;
   }

   protected void pickUpItem(ServerLevel p_363972_, ItemEntity p_21471_) {
      ItemStack itemstack = p_21471_.getItem();
      ItemStack itemstack1 = this.equipItemIfPossible(p_363972_, itemstack.copy());
      if (!itemstack1.isEmpty()) {
         this.onItemPickup(p_21471_);
         this.take(p_21471_, itemstack1.getCount());
         itemstack.shrink(itemstack1.getCount());
         if (itemstack.isEmpty()) {
            p_21471_.discard();
         }
      }
   }

   public ItemStack equipItemIfPossible(ServerLevel p_362503_, ItemStack p_255842_) {
      EquipmentSlot equipmentslot = this.getEquipmentSlotForItem(p_255842_);
      if (!this.isEquippableInSlot(p_255842_, equipmentslot)) {
         return ItemStack.EMPTY;
      }

      ItemStack itemstack = this.getItemBySlot(equipmentslot);
      boolean flag = this.canReplaceCurrentItem(p_255842_, itemstack, equipmentslot);
      if (equipmentslot.isArmor() && !flag) {
         equipmentslot = EquipmentSlot.MAINHAND;
         itemstack = this.getItemBySlot(equipmentslot);
         flag = itemstack.isEmpty();
      }

      if (flag && this.canHoldItem(p_255842_)) {
         double d0 = this.dropChances.byEquipment(equipmentslot);
         if (!itemstack.isEmpty() && Math.max(this.random.nextFloat() - 0.1F, 0.0F) < d0) {
            this.spawnAtLocation(p_362503_, itemstack);
         }

         ItemStack itemstack1 = equipmentslot.limit(p_255842_);
         this.setItemSlotAndDropWhenKilled(equipmentslot, itemstack1);
         return itemstack1;
      } else {
         return ItemStack.EMPTY;
      }
   }

   protected void setItemSlotAndDropWhenKilled(EquipmentSlot p_21469_, ItemStack p_21470_) {
      this.setItemSlot(p_21469_, p_21470_);
      this.setGuaranteedDrop(p_21469_);
      this.persistenceRequired = true;
   }

   protected boolean canShearEquipment(Player p_409472_) {
      return !this.isVehicle();
   }

   public void setGuaranteedDrop(EquipmentSlot p_21509_) {
      this.dropChances = this.dropChances.withGuaranteedDrop(p_21509_);
   }

   protected boolean canReplaceCurrentItem(ItemStack p_21428_, ItemStack p_21429_, EquipmentSlot p_362798_) {
      if (p_21429_.isEmpty()) {
         return true;
      } else if (p_362798_.isArmor()) {
         return this.compareArmor(p_21428_, p_21429_, p_362798_);
      } else {
         return p_362798_ == EquipmentSlot.MAINHAND ? this.compareWeapons(p_21428_, p_21429_, p_362798_) : false;
      }
   }

   private boolean compareArmor(ItemStack p_377405_, ItemStack p_378380_, EquipmentSlot p_376197_) {
      if (EnchantmentHelper.has(p_378380_, EnchantmentEffectComponents.PREVENT_ARMOR_CHANGE)) {
         return false;
      } else {
         double d0 = this.getApproximateAttributeWith(p_377405_, Attributes.ARMOR, p_376197_);
         double d1 = this.getApproximateAttributeWith(p_378380_, Attributes.ARMOR, p_376197_);
         double d2 = this.getApproximateAttributeWith(p_377405_, Attributes.ARMOR_TOUGHNESS, p_376197_);
         double d3 = this.getApproximateAttributeWith(p_378380_, Attributes.ARMOR_TOUGHNESS, p_376197_);
         if (d0 != d1) {
            return d0 > d1;
         } else {
            return d2 != d3 ? d2 > d3 : this.canReplaceEqualItem(p_377405_, p_378380_);
         }
      }
   }

   private boolean compareWeapons(ItemStack p_376507_, ItemStack p_378437_, EquipmentSlot p_378407_) {
      TagKey<Item> tagkey = this.getPreferredWeaponType();
      if (tagkey != null) {
         if (p_378437_.is(tagkey) && !p_376507_.is(tagkey)) {
            return false;
         }

         if (!p_378437_.is(tagkey) && p_376507_.is(tagkey)) {
            return true;
         }
      }

      double d0 = this.getApproximateAttributeWith(p_376507_, Attributes.ATTACK_DAMAGE, p_378407_);
      double d1 = this.getApproximateAttributeWith(p_378437_, Attributes.ATTACK_DAMAGE, p_378407_);
      return d0 != d1 ? d0 > d1 : this.canReplaceEqualItem(p_376507_, p_378437_);
   }

   private double getApproximateAttributeWith(ItemStack p_363720_, Holder<Attribute> p_366827_, EquipmentSlot p_366430_) {
      double d0 = this.getAttributes().hasAttribute(p_366827_) ? this.getAttributeBaseValue(p_366827_) : 0.0;
      ItemAttributeModifiers itemattributemodifiers = p_363720_.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
      return itemattributemodifiers.compute(p_366827_, d0, p_366430_);
   }

   public boolean canReplaceEqualItem(ItemStack p_21478_, ItemStack p_21479_) {
      Set<Entry<Holder<Enchantment>>> set = p_21479_.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY).entrySet();
      Set<Entry<Holder<Enchantment>>> set1 = p_21478_.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY).entrySet();
      if (set1.size() != set.size()) {
         return set1.size() > set.size();
      }

      int i = p_21478_.getDamageValue();
      int j = p_21479_.getDamageValue();
      return i != j ? i < j : p_21478_.has(DataComponents.CUSTOM_NAME) && !p_21479_.has(DataComponents.CUSTOM_NAME);
   }

   public boolean canHoldItem(ItemStack p_21545_) {
      return true;
   }

   public boolean wantsToPickUp(ServerLevel p_367521_, ItemStack p_21546_) {
      return this.canHoldItem(p_21546_);
   }

   public @Nullable TagKey<Item> getPreferredWeaponType() {
      return null;
   }

   public boolean removeWhenFarAway(double p_21542_) {
      return true;
   }

   public boolean requiresCustomPersistence() {
      return this.isPassenger();
   }

   @Override
   public void checkDespawn() {
      if (this.level().getDifficulty() == Difficulty.PEACEFUL && !this.getType().isAllowedInPeaceful()) {
         this.discard();
      } else if (!this.isPersistenceRequired() && !this.requiresCustomPersistence()) {
         Entity entity = this.level().getNearestPlayer(this, -1.0);
         if (entity != null) {
            double d0 = entity.distanceToSqr(this);
            int i = this.getType().getCategory().getDespawnDistance();
            int j = i * i;
            if (d0 > j && this.removeWhenFarAway(d0)) {
               this.discard();
            }

            int k = this.getType().getCategory().getNoDespawnDistance();
            int l = k * k;
            if (this.noActionTime > 600 && this.random.nextInt(800) == 0 && d0 > l && this.removeWhenFarAway(d0)) {
               this.discard();
            } else if (d0 < l) {
               this.noActionTime = 0;
            }
         }
      } else {
         this.noActionTime = 0;
      }
   }

   @Override
   protected final void serverAiStep() {
      this.noActionTime++;
      ProfilerFiller profilerfiller = Profiler.get();
      profilerfiller.push("sensing");
      this.sensing.tick();
      profilerfiller.pop();
      int i = this.tickCount + this.getId();
      if (i % 2 != 0 && this.tickCount > 1) {
         profilerfiller.push("targetSelector");
         this.targetSelector.tickRunningGoals(false);
         profilerfiller.pop();
         profilerfiller.push("goalSelector");
         this.goalSelector.tickRunningGoals(false);
         profilerfiller.pop();
      } else {
         profilerfiller.push("targetSelector");
         this.targetSelector.tick();
         profilerfiller.pop();
         profilerfiller.push("goalSelector");
         this.goalSelector.tick();
         profilerfiller.pop();
      }

      profilerfiller.push("navigation");
      this.navigation.tick();
      profilerfiller.pop();
      profilerfiller.push("mob tick");
      this.customServerAiStep((ServerLevel)this.level());
      profilerfiller.pop();
      profilerfiller.push("controls");
      profilerfiller.push("move");
      this.moveControl.tick();
      profilerfiller.popPush("look");
      this.lookControl.tick();
      profilerfiller.popPush("jump");
      this.jumpControl.tick();
      profilerfiller.pop();
      profilerfiller.pop();
   }

   protected void customServerAiStep(ServerLevel p_361697_) {
   }

   public int getMaxHeadXRot() {
      return 40;
   }

   public int getMaxHeadYRot() {
      return 75;
   }

   protected void clampHeadRotationToBody() {
      float f = this.getMaxHeadYRot();
      float f1 = this.getYHeadRot();
      float f2 = Mth.wrapDegrees(this.yBodyRot - f1);
      float f3 = Mth.clamp(Mth.wrapDegrees(this.yBodyRot - f1), -f, f);
      float f4 = f1 + f2 - f3;
      this.setYHeadRot(f4);
   }

   public int getHeadRotSpeed() {
      return 10;
   }

   public void lookAt(Entity p_21392_, float p_21393_, float p_21394_) {
      double d0 = p_21392_.getX() - this.getX();
      double d2 = p_21392_.getZ() - this.getZ();
      double d1;
      if (p_21392_ instanceof LivingEntity livingentity) {
         d1 = livingentity.getEyeY() - this.getEyeY();
      } else {
         d1 = (p_21392_.getBoundingBox().minY + p_21392_.getBoundingBox().maxY) / 2.0 - this.getEyeY();
      }

      double d3 = Math.sqrt(d0 * d0 + d2 * d2);
      float f = (float)(Mth.atan2(d2, d0) * 180.0 / (float) Math.PI) - 90.0F;
      float f1 = (float)(-(Mth.atan2(d1, d3) * 180.0 / (float) Math.PI));
      this.setXRot(this.rotlerp(this.getXRot(), f1, p_21394_));
      this.setYRot(this.rotlerp(this.getYRot(), f, p_21393_));
   }

   private float rotlerp(float p_21377_, float p_21378_, float p_21379_) {
      float f = Mth.wrapDegrees(p_21378_ - p_21377_);
      if (f > p_21379_) {
         f = p_21379_;
      }

      if (f < -p_21379_) {
         f = -p_21379_;
      }

      return p_21377_ + f;
   }

   public static boolean checkMobSpawnRules(
      EntityType<? extends Mob> p_217058_, LevelAccessor p_217059_, EntitySpawnReason p_362165_, BlockPos p_217061_, RandomSource p_217062_
   ) {
      BlockPos blockpos = p_217061_.below();
      return EntitySpawnReason.isSpawner(p_362165_) || p_217059_.getBlockState(blockpos).isValidSpawn(p_217059_, blockpos, p_217058_);
   }

   public boolean checkSpawnRules(LevelAccessor p_21431_, EntitySpawnReason p_366364_) {
      return true;
   }

   public boolean checkSpawnObstruction(LevelReader p_21433_) {
      return !p_21433_.containsAnyLiquid(this.getBoundingBox()) && p_21433_.isUnobstructed(this);
   }

   public int getMaxSpawnClusterSize() {
      return 4;
   }

   public boolean isMaxGroupSizeReached(int p_21489_) {
      return false;
   }

   @Override
   public int getMaxFallDistance() {
      if (this.getTarget() == null) {
         return this.getComfortableFallDistance(0.0F);
      }

      int i = (int)(this.getHealth() - this.getMaxHealth() * 0.33F);
      i -= (3 - this.level().getDifficulty().getId()) * 4;
      if (i < 0) {
         i = 0;
      }

      return this.getComfortableFallDistance(i);
   }

   public ItemStack getBodyArmorItem() {
      return this.getItemBySlot(EquipmentSlot.BODY);
   }

   public boolean isSaddled() {
      return this.hasValidEquippableItemForSlot(EquipmentSlot.SADDLE);
   }

   public boolean isWearingBodyArmor() {
      return this.hasValidEquippableItemForSlot(EquipmentSlot.BODY);
   }

   private boolean hasValidEquippableItemForSlot(EquipmentSlot p_408724_) {
      return this.hasItemInSlot(p_408724_) && this.isEquippableInSlot(this.getItemBySlot(p_408724_), p_408724_);
   }

   public void setBodyArmorItem(ItemStack p_333947_) {
      this.setItemSlotAndDropWhenKilled(EquipmentSlot.BODY, p_333947_);
   }

   public Container createEquipmentSlotContainer(final EquipmentSlot p_395210_) {
      return new ContainerSingleItem() {
         @Override
         public ItemStack getTheItem() {
            return Mob.this.getItemBySlot(p_395210_);
         }

         @Override
         public void setTheItem(ItemStack p_397258_) {
            Mob.this.setItemSlot(p_395210_, p_397258_);
            if (!p_397258_.isEmpty()) {
               Mob.this.setGuaranteedDrop(p_395210_);
               Mob.this.setPersistenceRequired();
            }
         }

         @Override
         public void setChanged() {
         }

         @Override
         public boolean stillValid(Player p_392053_) {
            return p_392053_.getVehicle() == Mob.this || p_392053_.isWithinEntityInteractionRange(Mob.this, 4.0);
         }
      };
   }

   @Override
   protected void dropCustomDeathLoot(ServerLevel p_345102_, DamageSource p_21385_, boolean p_21387_) {
      super.dropCustomDeathLoot(p_345102_, p_21385_, p_21387_);

      for (EquipmentSlot equipmentslot : EquipmentSlot.VALUES) {
         ItemStack itemstack = this.getItemBySlot(equipmentslot);
         float f = this.dropChances.byEquipment(equipmentslot);
         if (f != 0.0F) {
            boolean flag = this.dropChances.isPreserved(equipmentslot);
            if (p_21385_.getEntity() instanceof LivingEntity livingentity && this.level() instanceof ServerLevel serverlevel) {
               f = EnchantmentHelper.processEquipmentDropChance(serverlevel, livingentity, p_21385_, f);
            }

            if (!itemstack.isEmpty()
               && !EnchantmentHelper.has(itemstack, EnchantmentEffectComponents.PREVENT_EQUIPMENT_DROP)
               && (p_21387_ || flag)
               && this.random.nextFloat() < f) {
               if (!flag && itemstack.isDamageableItem()) {
                  itemstack.setDamageValue(itemstack.getMaxDamage() - this.random.nextInt(1 + this.random.nextInt(Math.max(itemstack.getMaxDamage() - 3, 1))));
               }

               this.spawnAtLocation(p_345102_, itemstack);
               this.setItemSlot(equipmentslot, ItemStack.EMPTY);
            }
         }
      }
   }

   public DropChances getDropChances() {
      return this.dropChances;
   }

   public void dropPreservedEquipment(ServerLevel p_364926_) {
      this.dropPreservedEquipment(p_364926_, p_343352_ -> true);
   }

   public Set<EquipmentSlot> dropPreservedEquipment(ServerLevel p_367808_, Predicate<ItemStack> p_361335_) {
      Set<EquipmentSlot> set = new HashSet<>();

      for (EquipmentSlot equipmentslot : EquipmentSlot.VALUES) {
         ItemStack itemstack = this.getItemBySlot(equipmentslot);
         if (!itemstack.isEmpty()) {
            if (!p_361335_.test(itemstack)) {
               set.add(equipmentslot);
            } else if (this.dropChances.isPreserved(equipmentslot)) {
               this.setItemSlot(equipmentslot, ItemStack.EMPTY);
               this.spawnAtLocation(p_367808_, itemstack);
            }
         }
      }

      return set;
   }

   private LootParams createEquipmentParams(ServerLevel p_331909_) {
      return new LootParams.Builder(p_331909_)
         .withParameter(LootContextParams.ORIGIN, this.position())
         .withParameter(LootContextParams.THIS_ENTITY, this)
         .create(LootContextParamSets.EQUIPMENT);
   }

   public void equip(EquipmentTable p_332456_) {
      this.equip(p_332456_.lootTable(), p_332456_.slotDropChances());
   }

   public void equip(ResourceKey<LootTable> p_328521_, Map<EquipmentSlot, Float> p_335710_) {
      if (this.level() instanceof ServerLevel serverlevel) {
         this.equip(p_328521_, this.createEquipmentParams(serverlevel), p_335710_);
      }
   }

   protected void populateDefaultEquipmentSlots(RandomSource p_217055_, DifficultyInstance p_217056_) {
      if (p_217055_.nextFloat() < 0.15F * p_217056_.getSpecialMultiplier()) {
         int i = p_217055_.nextInt(3);

         for (int j = 1; j <= 3.0F; j++) {
            if (p_217055_.nextFloat() < 0.1087F) {
               i++;
            }
         }

         float f = this.level().getDifficulty() == Difficulty.HARD ? 0.1F : 0.25F;
         boolean flag = true;

         for (EquipmentSlot equipmentslot : EQUIPMENT_POPULATION_ORDER) {
            ItemStack itemstack = this.getItemBySlot(equipmentslot);
            if (!flag && p_217055_.nextFloat() < f) {
               break;
            }

            flag = false;
            if (itemstack.isEmpty()) {
               Item item = getEquipmentForSlot(equipmentslot, i);
               if (item != null) {
                  this.setItemSlot(equipmentslot, new ItemStack(item));
               }
            }
         }
      }
   }

   public static @Nullable Item getEquipmentForSlot(EquipmentSlot p_21413_, int p_21414_) {
      switch (p_21413_) {
         case HEAD:
            if (p_21414_ == 0) {
               return Items.LEATHER_HELMET;
            } else if (p_21414_ == 1) {
               return Items.COPPER_HELMET;
            } else if (p_21414_ == 2) {
               return Items.GOLDEN_HELMET;
            } else if (p_21414_ == 3) {
               return Items.CHAINMAIL_HELMET;
            } else if (p_21414_ == 4) {
               return Items.IRON_HELMET;
            } else if (p_21414_ == 5) {
               return Items.DIAMOND_HELMET;
            }
         case CHEST:
            if (p_21414_ == 0) {
               return Items.LEATHER_CHESTPLATE;
            } else if (p_21414_ == 1) {
               return Items.COPPER_CHESTPLATE;
            } else if (p_21414_ == 2) {
               return Items.GOLDEN_CHESTPLATE;
            } else if (p_21414_ == 3) {
               return Items.CHAINMAIL_CHESTPLATE;
            } else if (p_21414_ == 4) {
               return Items.IRON_CHESTPLATE;
            } else if (p_21414_ == 5) {
               return Items.DIAMOND_CHESTPLATE;
            }
         case LEGS:
            if (p_21414_ == 0) {
               return Items.LEATHER_LEGGINGS;
            } else if (p_21414_ == 1) {
               return Items.COPPER_LEGGINGS;
            } else if (p_21414_ == 2) {
               return Items.GOLDEN_LEGGINGS;
            } else if (p_21414_ == 3) {
               return Items.CHAINMAIL_LEGGINGS;
            } else if (p_21414_ == 4) {
               return Items.IRON_LEGGINGS;
            } else if (p_21414_ == 5) {
               return Items.DIAMOND_LEGGINGS;
            }
         case FEET:
            if (p_21414_ == 0) {
               return Items.LEATHER_BOOTS;
            } else if (p_21414_ == 1) {
               return Items.COPPER_BOOTS;
            } else if (p_21414_ == 2) {
               return Items.GOLDEN_BOOTS;
            } else if (p_21414_ == 3) {
               return Items.CHAINMAIL_BOOTS;
            } else if (p_21414_ == 4) {
               return Items.IRON_BOOTS;
            } else if (p_21414_ == 5) {
               return Items.DIAMOND_BOOTS;
            }
         default:
            return null;
      }
   }

   protected void populateDefaultEquipmentEnchantments(ServerLevelAccessor p_344674_, RandomSource p_217063_, DifficultyInstance p_217064_) {
      this.enchantSpawnedWeapon(p_344674_, p_217063_, p_217064_);

      for (EquipmentSlot equipmentslot : EquipmentSlot.VALUES) {
         if (equipmentslot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR) {
            this.enchantSpawnedArmor(p_344674_, p_217063_, equipmentslot, p_217064_);
         }
      }
   }

   protected void enchantSpawnedWeapon(ServerLevelAccessor p_344989_, RandomSource p_217049_, DifficultyInstance p_344491_) {
      this.enchantSpawnedEquipment(p_344989_, EquipmentSlot.MAINHAND, p_217049_, 0.25F, p_344491_);
   }

   protected void enchantSpawnedArmor(ServerLevelAccessor p_342770_, RandomSource p_217052_, EquipmentSlot p_217054_, DifficultyInstance p_342649_) {
      this.enchantSpawnedEquipment(p_342770_, p_217054_, p_217052_, 0.5F, p_342649_);
   }

   private void enchantSpawnedEquipment(
      ServerLevelAccessor p_342440_, EquipmentSlot p_344135_, RandomSource p_344290_, float p_343248_, DifficultyInstance p_345046_
   ) {
      ItemStack itemstack = this.getItemBySlot(p_344135_);
      if (!itemstack.isEmpty() && p_344290_.nextFloat() < p_343248_ * p_345046_.getSpecialMultiplier()) {
         EnchantmentHelper.enchantItemFromProvider(itemstack, p_342440_.registryAccess(), VanillaEnchantmentProviders.MOB_SPAWN_EQUIPMENT, p_345046_, p_344290_);
         this.setItemSlot(p_344135_, itemstack);
      }
   }

   public @Nullable SpawnGroupData finalizeSpawn(
      ServerLevelAccessor p_21434_, DifficultyInstance p_21435_, EntitySpawnReason p_369316_, @Nullable SpawnGroupData p_21437_
   ) {
      RandomSource randomsource = p_21434_.getRandom();
      AttributeInstance attributeinstance = Objects.requireNonNull(this.getAttribute(Attributes.FOLLOW_RANGE));
      if (!attributeinstance.hasModifier(RANDOM_SPAWN_BONUS_ID)) {
         attributeinstance.addPermanentModifier(
            new AttributeModifier(RANDOM_SPAWN_BONUS_ID, randomsource.triangle(0.0, 0.11485000000000001), AttributeModifier.Operation.ADD_MULTIPLIED_BASE)
         );
      }

      this.setLeftHanded(randomsource.nextFloat() < 0.05F);
      return p_21437_;
   }

   public void setPersistenceRequired() {
      this.persistenceRequired = true;
   }

   @Override
   public void setDropChance(EquipmentSlot p_21410_, float p_21411_) {
      this.dropChances = this.dropChances.withEquipmentChance(p_21410_, p_21411_);
   }

   @Override
   public boolean canPickUpLoot() {
      return this.canPickUpLoot;
   }

   public void setCanPickUpLoot(boolean p_21554_) {
      this.canPickUpLoot = p_21554_;
   }

   @Override
   protected boolean canDispenserEquipIntoSlot(EquipmentSlot p_367943_) {
      return this.canPickUpLoot();
   }

   public boolean isPersistenceRequired() {
      return this.persistenceRequired;
   }

   @Override
   public InteractionResult interact(Player p_21420_, InteractionHand p_21421_) {
      if (!this.isAlive()) {
         return InteractionResult.PASS;
      } else {
         InteractionResult interactionresult = this.checkAndHandleImportantInteractions(p_21420_, p_21421_);
         if (interactionresult.consumesAction()) {
            this.gameEvent(GameEvent.ENTITY_INTERACT, p_21420_);
            return interactionresult;
         } else {
            InteractionResult interactionresult1 = super.interact(p_21420_, p_21421_);
            if (interactionresult1 != InteractionResult.PASS) {
               return interactionresult1;
            } else {
               interactionresult = this.mobInteract(p_21420_, p_21421_);
               if (interactionresult.consumesAction()) {
                  this.gameEvent(GameEvent.ENTITY_INTERACT, p_21420_);
                  return interactionresult;
               } else {
                  return InteractionResult.PASS;
               }
            }
         }
      }
   }

   private InteractionResult checkAndHandleImportantInteractions(Player p_21500_, InteractionHand p_21501_) {
      ItemStack itemstack = p_21500_.getItemInHand(p_21501_);
      if (itemstack.is(Items.NAME_TAG)) {
         InteractionResult interactionresult = itemstack.interactLivingEntity(p_21500_, this, p_21501_);
         if (interactionresult.consumesAction()) {
            return interactionresult;
         }
      }

      if (itemstack.getItem() instanceof SpawnEggItem spawneggitem) {
         if (this.level() instanceof ServerLevel) {
            Optional<Mob> optional = spawneggitem.spawnOffspringFromSpawnEgg(
               p_21500_, this, (EntityType<? extends Mob>)this.getType(), (ServerLevel)this.level(), this.position(), itemstack
            );
            optional.ifPresent(p_21476_ -> this.onOffspringSpawnedFromEgg(p_21500_, p_21476_));
            if (optional.isEmpty()) {
               return InteractionResult.PASS;
            }
         }

         return InteractionResult.SUCCESS_SERVER;
      } else {
         return InteractionResult.PASS;
      }
   }

   protected void onOffspringSpawnedFromEgg(Player p_21422_, Mob p_21423_) {
   }

   protected InteractionResult mobInteract(Player p_21472_, InteractionHand p_21473_) {
      return InteractionResult.PASS;
   }

   protected void usePlayerItem(Player p_423933_, InteractionHand p_427432_, ItemStack p_423963_) {
      int i = p_423963_.getCount();
      UseRemainder useremainder = p_423963_.get(DataComponents.USE_REMAINDER);
      p_423963_.consume(1, p_423933_);
      if (useremainder != null) {
         ItemStack itemstack = useremainder.convertIntoRemainder(p_423963_, i, p_423933_.hasInfiniteMaterials(), p_423933_::handleExtraItemsCreatedOnUse);
         p_423933_.setItemInHand(p_427432_, itemstack);
      }
   }

   public boolean isWithinHome() {
      return this.isWithinHome(this.blockPosition());
   }

   public boolean isWithinHome(BlockPos p_409461_) {
      return this.homeRadius == -1 ? true : this.homePosition.distSqr(p_409461_) < this.homeRadius * this.homeRadius;
   }

   public boolean isWithinHome(Vec3 p_407594_) {
      return this.homeRadius == -1 ? true : this.homePosition.distToCenterSqr(p_407594_) < this.homeRadius * this.homeRadius;
   }

   public void setHomeTo(BlockPos p_408245_, int p_410742_) {
      this.homePosition = p_408245_;
      this.homeRadius = p_410742_;
   }

   public BlockPos getHomePosition() {
      return this.homePosition;
   }

   public int getHomeRadius() {
      return this.homeRadius;
   }

   public void clearHome() {
      this.homeRadius = -1;
   }

   public boolean hasHome() {
      return this.homeRadius != -1;
   }

   public <T extends Mob> @Nullable T convertTo(
      EntityType<T> p_21407_, ConversionParams p_365288_, EntitySpawnReason p_367052_, ConversionParams.AfterConversion<T> p_368263_
   ) {
      if (this.isRemoved()) {
         return null;
      }

      T t = (T)p_21407_.create(this.level(), p_367052_);
      if (t == null) {
         return null;
      }

      p_365288_.type().convert(this, t, p_365288_);
      p_368263_.finalizeConversion(t);
      if (this.level() instanceof ServerLevel serverlevel) {
         serverlevel.addFreshEntity(t);
      }

      if (p_365288_.type().shouldDiscardAfterConversion()) {
         this.discard();
      }

      return t;
   }

   public <T extends Mob> @Nullable T convertTo(EntityType<T> p_364522_, ConversionParams p_368972_, ConversionParams.AfterConversion<T> p_362927_) {
      return this.convertTo(p_364522_, p_368972_, EntitySpawnReason.CONVERSION, p_362927_);
   }

   @Override
   public Leashable.@Nullable LeashData getLeashData() {
      return this.leashData;
   }

   private void resetAngularLeashMomentum() {
      if (this.leashData != null) {
         this.leashData.angularMomentum = 0.0;
      }
   }

   @Override
   public void setLeashData(Leashable.@Nullable LeashData p_344337_) {
      this.leashData = p_344337_;
   }

   @Override
   public void onLeashRemoved() {
      if (this.getLeashData() == null) {
         this.clearHome();
      }
   }

   @Override
   public void leashTooFarBehaviour() {
      Leashable.super.leashTooFarBehaviour();
      this.goalSelector.disableControlFlag(Goal.Flag.MOVE);
   }

   @Override
   public boolean canBeLeashed() {
      return !(this instanceof Enemy);
   }

   @Override
   public boolean startRiding(Entity p_21396_, boolean p_21397_, boolean p_430101_) {
      boolean flag = super.startRiding(p_21396_, p_21397_, p_430101_);
      if (flag && this.isLeashed()) {
         this.dropLeash();
      }

      return flag;
   }

   @Override
   public boolean isEffectiveAi() {
      return super.isEffectiveAi() && !this.isNoAi();
   }

   public void setNoAi(boolean p_21558_) {
      byte b0 = this.entityData.get(DATA_MOB_FLAGS_ID);
      this.entityData.set(DATA_MOB_FLAGS_ID, p_21558_ ? (byte)(b0 | 1) : (byte)(b0 & -2));
   }

   public void setLeftHanded(boolean p_21560_) {
      byte b0 = this.entityData.get(DATA_MOB_FLAGS_ID);
      this.entityData.set(DATA_MOB_FLAGS_ID, p_21560_ ? (byte)(b0 | 2) : (byte)(b0 & -3));
   }

   public void setAggressive(boolean p_21562_) {
      byte b0 = this.entityData.get(DATA_MOB_FLAGS_ID);
      this.entityData.set(DATA_MOB_FLAGS_ID, p_21562_ ? (byte)(b0 | 4) : (byte)(b0 & -5));
   }

   public boolean isNoAi() {
      return (this.entityData.get(DATA_MOB_FLAGS_ID) & 1) != 0;
   }

   public boolean isLeftHanded() {
      return (this.entityData.get(DATA_MOB_FLAGS_ID) & 2) != 0;
   }

   public boolean isAggressive() {
      return (this.entityData.get(DATA_MOB_FLAGS_ID) & 4) != 0;
   }

   public void setBaby(boolean p_21451_) {
   }

   @Override
   public HumanoidArm getMainArm() {
      return this.isLeftHanded() ? HumanoidArm.LEFT : HumanoidArm.RIGHT;
   }

   public boolean isWithinMeleeAttackRange(LivingEntity p_217067_) {
      AttackRange attackrange = this.getActiveItem().get(DataComponents.ATTACK_RANGE);
      double d0;
      double d1;
      if (attackrange == null) {
         d0 = DEFAULT_ATTACK_REACH;
         d1 = 0.0;
      } else {
         d0 = attackrange.effectiveMaxRange(this);
         d1 = attackrange.effectiveMinRange(this);
      }

      AABB aabb = p_217067_.getHitbox();
      return this.getAttackBoundingBox(d0).intersects(aabb) && (d1 <= 0.0 || !this.getAttackBoundingBox(d1).intersects(aabb));
   }

   protected AABB getAttackBoundingBox(double p_457993_) {
      Entity entity = this.getVehicle();
      AABB aabb;
      if (entity != null) {
         AABB aabb1 = entity.getBoundingBox();
         AABB aabb2 = this.getBoundingBox();
         aabb = new AABB(
            Math.min(aabb2.minX, aabb1.minX),
            aabb2.minY,
            Math.min(aabb2.minZ, aabb1.minZ),
            Math.max(aabb2.maxX, aabb1.maxX),
            aabb2.maxY,
            Math.max(aabb2.maxZ, aabb1.maxZ)
         );
      } else {
         aabb = this.getBoundingBox();
      }

      return aabb.inflate(p_457993_, 0.0, p_457993_);
   }

   @Override
   public boolean doHurtTarget(ServerLevel p_365421_, Entity p_21372_) {
      float f = (float)this.getAttributeValue(Attributes.ATTACK_DAMAGE);
      ItemStack itemstack = this.getWeaponItem();
      DamageSource damagesource = itemstack.getDamageSource(this, () -> this.damageSources().mobAttack(this));
      f = EnchantmentHelper.modifyDamage(p_365421_, itemstack, p_21372_, damagesource, f);
      f += itemstack.getItem().getAttackDamageBonus(p_21372_, f, damagesource);
      Vec3 vec3 = p_21372_.getDeltaMovement();
      boolean flag = p_21372_.hurtServer(p_365421_, damagesource, f);
      if (flag) {
         this.causeExtraKnockback(p_21372_, this.getKnockback(p_21372_, damagesource), vec3);
         if (p_21372_ instanceof LivingEntity livingentity) {
            itemstack.hurtEnemy(livingentity, this);
         }

         EnchantmentHelper.doPostAttackEffects(p_365421_, p_21372_, damagesource);
         this.setLastHurtMob(p_21372_);
         this.playAttackSound();
      }

      this.lungeForwardMaybe();
      return flag;
   }

   @Override
   protected void jumpInLiquid(TagKey<Fluid> p_204045_) {
      if (this.getNavigation().canFloat()) {
         super.jumpInLiquid(p_204045_);
      } else {
         this.setDeltaMovement(this.getDeltaMovement().add(0.0, 0.3, 0.0));
      }
   }

   @VisibleForTesting
   public void removeFreeWill() {
      this.removeAllGoals(p_341273_ -> true);
      this.getBrain().removeAllBehaviors();
   }

   public void removeAllGoals(Predicate<Goal> p_262667_) {
      this.goalSelector.removeAllGoals(p_262667_);
   }

   @Override
   protected void removeAfterChangingDimensions() {
      super.removeAfterChangingDimensions();

      for (EquipmentSlot equipmentslot : EquipmentSlot.VALUES) {
         ItemStack itemstack = this.getItemBySlot(equipmentslot);
         if (!itemstack.isEmpty()) {
            itemstack.setCount(0);
         }
      }
   }

   @Override
   public @Nullable ItemStack getPickResult() {
      SpawnEggItem spawneggitem = SpawnEggItem.byId(this.getType());
      return spawneggitem == null ? null : new ItemStack(spawneggitem);
   }

   @Override
   protected void onAttributeUpdated(Holder<Attribute> p_365996_) {
      super.onAttributeUpdated(p_365996_);
      if (p_365996_.is(Attributes.FOLLOW_RANGE) || p_365996_.is(Attributes.TEMPT_RANGE)) {
         this.getNavigation().updatePathfinderMaxVisitedNodes();
      }
   }

   @Override
   public void registerDebugValues(ServerLevel p_430222_, DebugValueSource.Registration p_428280_) {
      p_428280_.register(DebugSubscriptions.ENTITY_PATHS, () -> {
         Path path = this.getNavigation().getPath();
         return path != null && path.debugData() != null ? new DebugPathInfo(path.copy(), this.getNavigation().getMaxDistanceToWaypoint()) : null;
      });
      p_428280_.register(
         DebugSubscriptions.GOAL_SELECTORS,
         () -> {
            Set<WrappedGoal> set = this.goalSelector.getAvailableGoals();
            List<DebugGoalInfo.DebugGoal> list = new ArrayList<>(set.size());
            set.forEach(
               p_421601_ -> list.add(
                  new DebugGoalInfo.DebugGoal(p_421601_.getPriority(), p_421601_.isRunning(), p_421601_.getGoal().getClass().getSimpleName())
               )
            );
            return new DebugGoalInfo(list);
         }
      );
      if (!this.brain.isBrainDead()) {
         p_428280_.register(DebugSubscriptions.BRAINS, () -> DebugBrainDump.takeBrainDump(p_430222_, this));
      }
   }

   public float chargeSpeedModifier() {
      return 1.0F;
   }
}
