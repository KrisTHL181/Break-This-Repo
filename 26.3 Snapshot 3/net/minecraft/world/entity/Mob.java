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
import net.minecraft.advancements.triggers.CriteriaTriggers;
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
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.VisibleForDebug;
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
import net.minecraft.world.item.equipment.Equippable;
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

public abstract class Mob extends LivingEntity implements Targeting, EquipmentUser, Leashable {
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
   public static final String TAG_PERSISTENCE_REQUIRED = "PersistenceRequired";
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

   protected Mob(final EntityType<? extends Mob> type, final Level level) {
      super(type, level);
      this.goalSelector = new GoalSelector();
      this.targetSelector = new GoalSelector();
      this.lookControl = new LookControl(this);
      this.moveControl = new MoveControl<>(this);
      this.jumpControl = new JumpControl(this);
      this.bodyRotationControl = this.createBodyControl();
      this.navigation = this.createNavigation(level);
      this.sensing = new Sensing(this);
      if (level instanceof ServerLevel) {
         this.registerGoals();
      }
   }

   protected void registerGoals() {
   }

   public static AttributeSupplier.Builder createMobAttributes() {
      return LivingEntity.createLivingAttributes().add(Attributes.FOLLOW_RANGE, 16.0);
   }

   protected PathNavigation createNavigation(final Level level) {
      return new GroundPathNavigation(this, level);
   }

   protected boolean shouldPassengersInheritMalus() {
      return false;
   }

   public float getPathfindingMalus(final PathType pathType) {
      Mob inheritFrom;
      if (this.getControlledVehicle() instanceof Mob riding && riding.shouldPassengersInheritMalus()) {
         inheritFrom = riding;
      } else {
         inheritFrom = this;
      }

      Float malus = inheritFrom.pathfindingMalus.get(pathType);
      return malus == null ? pathType.getMalus() : malus;
   }

   public void setPathfindingMalus(final PathType pathType, final float cost) {
      this.pathfindingMalus.put(pathType, cost);
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
      return this.getControlledVehicle() instanceof Mob riding ? riding.getMoveControl() : this.moveControl;
   }

   public JumpControl getJumpControl() {
      return this.jumpControl;
   }

   public PathNavigation getNavigation() {
      return this.getControlledVehicle() instanceof Mob riding ? riding.getNavigation() : this.navigation;
   }

   @Override
   public @Nullable LivingEntity getControllingPassenger() {
      Entity firstPassenger = this.getFirstPassenger();
      return !this.isNoAi() && firstPassenger instanceof Mob passenger && firstPassenger.canControlVehicle() ? passenger : null;
   }

   public Sensing getSensing() {
      return this.sensing;
   }

   @Override
   public @Nullable LivingEntity getTarget() {
      return this.asValidTarget(this.target);
   }

   public @Nullable LivingEntity getTargetUnchecked() {
      return this.target;
   }

   protected @Nullable LivingEntity asValidTarget(final @Nullable LivingEntity target) {
      if (target instanceof Player player && (player.isCreative() || player.isSpectator())) {
         return null;
      } else {
         return target != null && !this.canAttack(target) ? null : target;
      }
   }

   protected final @Nullable LivingEntity getTargetFromBrain() {
      return this.asValidTarget(this.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).orElse(null));
   }

   public void setTarget(final @Nullable LivingEntity target) {
      this.target = this.asValidTarget(target);
   }

   @Override
   public boolean canAttack(final LivingEntity target) {
      return !target.is(EntityTypes.GHAST) && super.canAttack(target);
   }

   public boolean canUseNonMeleeWeapon(final ItemStack item) {
      return false;
   }

   public void ate() {
      this.gameEvent(GameEvent.EAT);
   }

   @Override
   protected void defineSynchedData(final SynchedEntityData.Builder entityData) {
      super.defineSynchedData(entityData);
      entityData.define(DATA_MOB_FLAGS_ID, (byte)0);
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
      ProfilerFiller profiler = Profiler.get();
      profiler.push("mobBaseTick");
      if (this.isAlive() && this.random.nextInt(1000) < this.ambientSoundTime++) {
         this.resetAmbientSoundTime();
         this.playAmbientSound();
      }

      profiler.pop();
   }

   @Override
   protected void playHurtSound(final DamageSource source) {
      this.resetAmbientSoundTime();
      super.playHurtSound(source);
   }

   private void resetAmbientSoundTime() {
      this.ambientSoundTime = -this.getAmbientSoundInterval();
   }

   @Override
   protected int getBaseExperienceReward(final ServerLevel level) {
      if (this.xpReward > 0) {
         int result = this.xpReward;

         for (EquipmentSlot slot : EquipmentSlot.VALUES) {
            if (slot.canIncreaseExperience()) {
               ItemStack item = this.getItemBySlot(slot);
               if (!item.isEmpty() && this.dropChances.byEquipment(slot) <= 1.0F) {
                  result += 1 + this.random.nextInt(3);
               }
            }
         }

         return result;
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
   public void handleEntityEvent(final byte id) {
      if (id == 20) {
         this.spawnAnim();
      } else {
         super.handleEntityEvent(id);
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
      boolean noController = !(this.getControllingPassenger() instanceof Mob);
      boolean notInBoat = !(this.getVehicle() instanceof AbstractBoat);
      this.goalSelector.setControlFlag(Goal.Flag.MOVE, noController);
      this.goalSelector.setControlFlag(Goal.Flag.JUMP, noController && notInBoat);
      this.goalSelector.setControlFlag(Goal.Flag.LOOK, noController);
   }

   @Override
   protected void tickHeadTurn(final float yBodyRotT) {
      this.bodyRotationControl.clientTick();
   }

   protected @Nullable SoundEvent getAmbientSound() {
      return null;
   }

   @Override
   protected void addAdditionalSaveData(final ValueOutput output) {
      super.addAdditionalSaveData(output);
      output.putBoolean("CanPickUpLoot", this.canPickUpLoot());
      output.putBoolean("PersistenceRequired", this.persistenceRequired);
      if (!this.dropChances.equals(DropChances.DEFAULT)) {
         output.store("drop_chances", DropChances.CODEC, this.dropChances);
      }

      this.writeLeashData(output, this.leashData);
      if (this.hasHome()) {
         output.putInt("home_radius", this.homeRadius);
         output.store("home_pos", BlockPos.CODEC, this.homePosition);
      }

      output.putBoolean("LeftHanded", this.isLeftHanded());
      this.lootTable.ifPresent(lootTable -> output.store("DeathLootTable", LootTable.KEY_CODEC, lootTable));
      if (this.lootTableSeed != 0L) {
         output.putLong("DeathLootTableSeed", this.lootTableSeed);
      }

      if (this.isNoAi()) {
         output.putBoolean("NoAI", this.isNoAi());
      }
   }

   @Override
   protected void readAdditionalSaveData(final ValueInput input) {
      super.readAdditionalSaveData(input);
      this.setCanPickUpLoot(input.getBooleanOr("CanPickUpLoot", false));
      this.persistenceRequired = input.getBooleanOr("PersistenceRequired", false);
      this.dropChances = input.<DropChances>read("drop_chances", DropChances.CODEC).orElse(DropChances.DEFAULT);
      this.readLeashData(input);
      this.homeRadius = input.getIntOr("home_radius", -1);
      if (this.homeRadius >= 0) {
         this.homePosition = input.<BlockPos>read("home_pos", BlockPos.CODEC).orElse(BlockPos.ZERO);
      }

      this.setLeftHanded(input.getBooleanOr("LeftHanded", false));
      this.lootTable = input.read("DeathLootTable", LootTable.KEY_CODEC);
      this.lootTableSeed = input.getLongOr("DeathLootTableSeed", 0L);
      this.setNoAi(input.getBooleanOr("NoAI", false));
   }

   @Override
   protected void dropFromLootTable(final ServerLevel level, final DamageSource source, final boolean playerKilled) {
      super.dropFromLootTable(level, source, playerKilled);
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

   public void setZza(final float zza) {
      this.zza = zza;
   }

   public void setYya(final float yya) {
      this.yya = yya;
   }

   public void setXxa(final float xxa) {
      this.xxa = xxa;
   }

   @Override
   public void setSpeed(final float speed) {
      super.setSpeed(speed);
      this.setZza(speed);
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
      if (this.is(EntityTypeTags.BURN_IN_DAYLIGHT)) {
         this.burnUndead();
      }

      ProfilerFiller profiler = Profiler.get();
      profiler.push("looting");
      if (this.level() instanceof ServerLevel serverLevel
         && this.canPickUpLoot()
         && this.isAlive()
         && !this.dead
         && serverLevel.getGameRules().get(GameRules.MOB_GRIEFING)) {
         Vec3i pickupReach = this.getPickupReach();

         for (ItemEntity entity : this.level()
            .getEntitiesOfClass(ItemEntity.class, this.getBoundingBox().inflate(pickupReach.getX(), pickupReach.getY(), pickupReach.getZ()))) {
            if (!entity.isRemoved() && !entity.getItem().isEmpty() && !entity.hasPickUpDelay() && this.wantsToPickUp(serverLevel, entity.getItem())) {
               this.pickUpItem(serverLevel, entity);
            }
         }
      }

      profiler.pop();
   }

   protected EquipmentSlot sunProtectionSlot() {
      return EquipmentSlot.HEAD;
   }

   private void burnUndead() {
      if (this.isAlive() && this.isSunBurnTick()) {
         EquipmentSlot slot = this.sunProtectionSlot();
         ItemStack sunBlocker = this.getItemBySlot(slot);
         if (!sunBlocker.isEmpty()) {
            if (sunBlocker.isDamageableItem()) {
               Item sunBlockerItem = sunBlocker.getItem();
               sunBlocker.setDamageValue(sunBlocker.getDamageValue() + this.random.nextInt(2));
               if (sunBlocker.getDamageValue() >= sunBlocker.getMaxDamage()) {
                  this.onEquippedItemBroken(sunBlockerItem, slot);
                  this.setItemSlot(slot, ItemStack.EMPTY);
               }
            }
         } else {
            this.igniteForSeconds(8.0F);
         }
      }
   }

   private boolean isSunBurnTick() {
      if (!this.level().isClientSide() && this.level().environmentAttributes().getValue(EnvironmentAttributes.MONSTERS_BURN, this.position())) {
         float br = this.getLightLevelDependentMagicValue();
         BlockPos roundedPos = BlockPos.containing(this.getX(), this.getEyeY(), this.getZ());
         boolean isInNonBurnableBlock = this.isInWaterOrRain() || this.isInPowderSnow || this.wasInPowderSnow;
         if (br > 0.5F && this.random.nextFloat() * 30.0F < (br - 0.4F) * 2.0F && !isInNonBurnableBlock && this.level().canSeeSky(roundedPos)) {
            return true;
         }
      }

      return false;
   }

   protected Vec3i getPickupReach() {
      return ITEM_PICKUP_REACH;
   }

   protected void pickUpItem(final ServerLevel level, final ItemEntity entity) {
      ItemStack itemStack = entity.getItem();
      ItemStack equippedWithStack = this.equipItemIfPossible(level, itemStack.copy());
      if (!equippedWithStack.isEmpty()) {
         this.onItemPickup(entity);
         this.take(entity, equippedWithStack.getCount());
         itemStack.shrink(equippedWithStack.getCount());
         if (itemStack.isEmpty()) {
            entity.discard();
         }
      }
   }

   public ItemStack equipItemIfPossible(final ServerLevel level, final ItemStack itemStack) {
      EquipmentSlot slot = this.getEquipmentSlotForItem(itemStack);
      if (!this.isEquippableInSlot(itemStack, slot)) {
         return ItemStack.EMPTY;
      }

      ItemStack current = this.getItemBySlot(slot);
      boolean canReplace = this.canReplaceCurrentItem(itemStack, current, slot);
      if (slot.isArmor() && !canReplace) {
         slot = EquipmentSlot.MAINHAND;
         current = this.getItemBySlot(slot);
         canReplace = current.isEmpty();
      }

      if (canReplace && this.canHoldItem(itemStack)) {
         double dropChance = this.dropChances.byEquipment(slot);
         if (!current.isEmpty() && Math.max(this.random.nextFloat() - 0.1F, 0.0F) < dropChance) {
            this.spawnAtLocation(level, current);
         }

         ItemStack toEquip = slot.limit(itemStack);
         this.setItemSlotAndDropWhenKilled(slot, toEquip);
         this.persistenceRequired = true;
         return toEquip;
      } else {
         return ItemStack.EMPTY;
      }
   }

   protected void setItemSlotAndDropWhenKilled(final EquipmentSlot slot, final ItemStack itemStack) {
      this.setItemSlot(slot, itemStack);
      this.setGuaranteedDrop(slot);
   }

   protected boolean canShearEquipment(final Player player) {
      return !this.isVehicle();
   }

   protected boolean attemptToShearEquipment(final Player player, final InteractionHand hand, final ItemStack heldItem) {
      for (EquipmentSlot slot : EquipmentSlot.VALUES) {
         ItemStack itemStack = this.getItemBySlot(slot);
         Equippable equippable = itemStack.get(DataComponents.EQUIPPABLE);
         if (equippable != null
            && equippable.canBeSheared()
            && (!EnchantmentHelper.has(itemStack, EnchantmentEffectComponents.PREVENT_ARMOR_CHANGE) || player.isCreative())) {
            this.shearItem(player, hand, heldItem, slot, itemStack);
            this.playSound(equippable.shearingSound().value());
            return true;
         }
      }

      return false;
   }

   protected void shearItem(
      final Player player, final InteractionHand hand, final ItemStack heldItem, final EquipmentSlot slot, final ItemStack itemStackToShear
   ) {
      heldItem.hurtAndBreak(1, player, hand.asEquipmentSlot());
      Vec3 equipmentSpawnOffset = this.getAttachments().getAverage(EntityAttachment.PASSENGER);
      this.setItemSlot(slot, ItemStack.EMPTY);
      this.gameEvent(GameEvent.SHEAR, player);
      if (this.level() instanceof ServerLevel serverLevel) {
         this.spawnAtLocation(serverLevel, itemStackToShear, equipmentSpawnOffset);
         CriteriaTriggers.PLAYER_SHEARED_EQUIPMENT.trigger((ServerPlayer)player, itemStackToShear, this);
      }
   }

   public void setGuaranteedDrop(final EquipmentSlot slot) {
      this.dropChances = this.dropChances.withGuaranteedDrop(slot);
   }

   protected boolean canReplaceCurrentItem(final ItemStack newItemStack, final ItemStack currentItemStack, final EquipmentSlot slot) {
      if (currentItemStack.isEmpty()) {
         return true;
      } else if (slot.isArmor()) {
         return this.compareArmor(newItemStack, currentItemStack, slot);
      } else {
         return slot == EquipmentSlot.MAINHAND ? this.compareWeapons(newItemStack, currentItemStack, slot) : false;
      }
   }

   private boolean compareArmor(final ItemStack newItemStack, final ItemStack currentItemStack, final EquipmentSlot slot) {
      if (EnchantmentHelper.has(currentItemStack, EnchantmentEffectComponents.PREVENT_ARMOR_CHANGE)) {
         return false;
      } else {
         double newDefense = this.getApproximateAttributeWith(newItemStack, Attributes.ARMOR, slot);
         double oldDefense = this.getApproximateAttributeWith(currentItemStack, Attributes.ARMOR, slot);
         double newToughness = this.getApproximateAttributeWith(newItemStack, Attributes.ARMOR_TOUGHNESS, slot);
         double oldToughness = this.getApproximateAttributeWith(currentItemStack, Attributes.ARMOR_TOUGHNESS, slot);
         if (newDefense != oldDefense) {
            return newDefense > oldDefense;
         } else {
            return newToughness != oldToughness ? newToughness > oldToughness : this.canReplaceEqualItem(newItemStack, currentItemStack);
         }
      }
   }

   private boolean compareWeapons(final ItemStack newItemStack, final ItemStack currentItemStack, final EquipmentSlot slot) {
      TagKey<Item> preferredWeaponType = this.getPreferredWeaponType();
      if (preferredWeaponType != null) {
         if (currentItemStack.is(preferredWeaponType) && !newItemStack.is(preferredWeaponType)) {
            return false;
         }

         if (!currentItemStack.is(preferredWeaponType) && newItemStack.is(preferredWeaponType)) {
            return true;
         }
      }

      double newAttackDamage = this.getApproximateAttributeWith(newItemStack, Attributes.ATTACK_DAMAGE, slot);
      double oldAttackDamage = this.getApproximateAttributeWith(currentItemStack, Attributes.ATTACK_DAMAGE, slot);
      return newAttackDamage != oldAttackDamage ? newAttackDamage > oldAttackDamage : this.canReplaceEqualItem(newItemStack, currentItemStack);
   }

   private double getApproximateAttributeWith(final ItemStack itemStack, final Holder<Attribute> attribute, final EquipmentSlot slot) {
      double baseValue = this.getAttributes().hasAttribute(attribute) ? this.getAttributeBaseValue(attribute) : 0.0;
      ItemAttributeModifiers attributeModifiers = itemStack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
      return attributeModifiers.compute(attribute, baseValue, slot);
   }

   public boolean canReplaceEqualItem(final ItemStack newItemStack, final ItemStack currentItemStack) {
      Set<Entry<Holder<Enchantment>>> currentEnchantments = currentItemStack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY).entrySet();
      Set<Entry<Holder<Enchantment>>> newEnchantments = newItemStack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY).entrySet();
      if (newEnchantments.size() != currentEnchantments.size()) {
         return newEnchantments.size() > currentEnchantments.size();
      }

      int newDamageValue = newItemStack.getDamageValue();
      int currentDamageValue = currentItemStack.getDamageValue();
      return newDamageValue != currentDamageValue
         ? newDamageValue < currentDamageValue
         : newItemStack.has(DataComponents.CUSTOM_NAME) && !currentItemStack.has(DataComponents.CUSTOM_NAME);
   }

   public boolean canHoldItem(final ItemStack itemStack) {
      return true;
   }

   public boolean wantsToPickUp(final ServerLevel level, final ItemStack itemStack) {
      return this.canHoldItem(itemStack);
   }

   public @Nullable TagKey<Item> getPreferredWeaponType() {
      return null;
   }

   public boolean removeWhenFarAway(final double distSqr) {
      return true;
   }

   public boolean requiresCustomPersistence() {
      return this.isPassenger() || this.isLeashed();
   }

   @Override
   public void checkDespawn() {
      if (this.level().getDifficulty() == Difficulty.PEACEFUL && !this.getType().isAllowedInPeaceful()) {
         this.discard();
      } else {
         boolean isPersistent = this.isPersistenceRequired() || this.requiresCustomPersistence();
         Entity player = this.level().getNearestPlayer(this, -1.0);
         if (player != null) {
            double distSqr = player.distanceToSqr(this);
            int instantDespawnDistance = this.getType().getCategory().getDespawnDistance();
            int despawnDistanceSqr = instantDespawnDistance * instantDespawnDistance;
            if (!isPersistent && distSqr > despawnDistanceSqr && this.removeWhenFarAway(distSqr)) {
               this.discard();
            }

            int noDespawnDistance = this.getType().getCategory().getNoDespawnDistance();
            int noDespawnDistanceSqr = noDespawnDistance * noDespawnDistance;
            if (!isPersistent && this.noActionTime > 600 && this.random.nextInt(800) == 0 && distSqr > noDespawnDistanceSqr && this.removeWhenFarAway(distSqr)) {
               this.discard();
            } else if (distSqr < noDespawnDistanceSqr) {
               this.noActionTime = 0;
            }
         }
      }
   }

   @Override
   protected final void serverAiStep() {
      this.noActionTime++;
      ProfilerFiller profiler = Profiler.get();
      profiler.push("sensing");
      this.sensing.tick();
      profiler.pop();
      int idBasedTickCount = this.tickCount + this.getId();
      if (idBasedTickCount % 2 != 0 && this.tickCount > 1) {
         profiler.push("targetSelector");
         this.targetSelector.tickRunningGoals(false);
         profiler.pop();
         profiler.push("goalSelector");
         this.goalSelector.tickRunningGoals(false);
         profiler.pop();
      } else {
         profiler.push("targetSelector");
         this.targetSelector.tick();
         profiler.pop();
         profiler.push("goalSelector");
         this.goalSelector.tick();
         profiler.pop();
      }

      profiler.push("navigation");
      this.navigation.tick();
      profiler.pop();
      profiler.push("mob tick");
      this.customServerAiStep((ServerLevel)this.level());
      profiler.pop();
      profiler.push("controls");
      profiler.push("move");
      this.moveControl.tick();
      profiler.popPush("look");
      this.lookControl.tick();
      profiler.popPush("jump");
      this.jumpControl.tick();
      profiler.pop();
      profiler.pop();
   }

   protected void customServerAiStep(final ServerLevel level) {
   }

   public int getMaxHeadXRot() {
      return 40;
   }

   public int getMaxHeadYRot() {
      return 75;
   }

   public void clampHeadRotationToBody() {
      float limit = this.getMaxHeadYRot();
      float headYRot = this.getYHeadRot();
      float delta = Mth.wrapDegrees(this.yBodyRot - headYRot);
      float targetDelta = Mth.clamp(Mth.wrapDegrees(this.yBodyRot - headYRot), -limit, limit);
      float newHeadYRot = headYRot + delta - targetDelta;
      this.setYHeadRot(newHeadYRot);
   }

   public int getHeadRotSpeed() {
      return 10;
   }

   public void lookAt(final Entity entity, final float yMax, final float xMax) {
      double xd = entity.getX() - this.getX();
      double zd = entity.getZ() - this.getZ();
      double yd;
      if (entity instanceof LivingEntity mob) {
         yd = mob.getEyeY() - this.getEyeY();
      } else {
         yd = (entity.getBoundingBox().minY + entity.getBoundingBox().maxY) / 2.0 - this.getEyeY();
      }

      double sd = Math.sqrt(xd * xd + zd * zd);
      float yRotD = (float)(Mth.atan2(zd, xd) * 180.0F / (float)Math.PI) - 90.0F;
      float xRotD = (float)(-(Mth.atan2(yd, sd) * 180.0F / (float)Math.PI));
      this.setXRot(this.rotlerp(this.getXRot(), xRotD, xMax));
      this.setYRot(this.rotlerp(this.getYRot(), yRotD, yMax));
   }

   private float rotlerp(final float a, final float b, final float max) {
      float diff = Mth.wrapDegrees(b - a);
      if (diff > max) {
         diff = max;
      }

      if (diff < -max) {
         diff = -max;
      }

      return a + diff;
   }

   public static boolean checkMobSpawnRules(
      final EntityType<? extends Mob> type, final LevelAccessor level, final EntitySpawnReason spawnReason, final BlockPos pos, final RandomSource random
   ) {
      BlockPos below = pos.below();
      return EntitySpawnReason.isSpawner(spawnReason) || level.getBlockState(below).isValidSpawn(level, below, type);
   }

   public boolean checkSpawnRules(final LevelAccessor level, final EntitySpawnReason spawnReason) {
      return true;
   }

   public boolean checkSpawnObstruction(final LevelReader level) {
      return !level.containsAnyLiquid(this.getBoundingBox()) && level.isUnobstructed(this);
   }

   public int getMaxSpawnClusterSize() {
      return 4;
   }

   public boolean isMaxGroupSizeReached(final int groupSize) {
      return false;
   }

   @Override
   public int getMaxFallDistance() {
      if (this.getTarget() == null) {
         return this.getComfortableFallDistance(0.0F);
      }

      int sacrifice = (int)(this.getHealth() - this.getMaxHealth() * 0.33F);
      sacrifice -= (3 - this.level().getDifficulty().getId()) * 4;
      if (sacrifice < 0) {
         sacrifice = 0;
      }

      return this.getComfortableFallDistance(sacrifice);
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

   private boolean hasValidEquippableItemForSlot(final EquipmentSlot slot) {
      return this.hasItemInSlot(slot) && this.isEquippableInSlot(this.getItemBySlot(slot), slot);
   }

   public Container createEquipmentSlotContainer(final EquipmentSlot slot) {
      return new ContainerSingleItem() {
         @Override
         public ItemStack getTheItem() {
            return Mob.this.getItemBySlot(slot);
         }

         @Override
         public void setTheItem(final ItemStack itemStack) {
            Mob.this.setItemSlot(slot, itemStack);
            if (!itemStack.isEmpty()) {
               Mob.this.setGuaranteedDrop(slot);
               Mob.this.setPersistenceRequired();
            }
         }

         @Override
         public void setChanged() {
         }

         @Override
         public boolean stillValid(final Player player) {
            return player.getVehicle() == Mob.this || player.isWithinEntityInteractionRange(Mob.this, 4.0);
         }
      };
   }

   @Override
   protected void dropCustomDeathLoot(final ServerLevel level, final DamageSource source, final boolean killedByPlayer) {
      super.dropCustomDeathLoot(level, source, killedByPlayer);

      for (EquipmentSlot slot : EquipmentSlot.VALUES) {
         ItemStack itemStack = this.getItemBySlot(slot);
         float dropChance = this.dropChances.byEquipment(slot);
         if (dropChance != 0.0F) {
            boolean preserve = this.dropChances.isPreserved(slot);
            if (source.getEntity() instanceof LivingEntity livingSource && this.level() instanceof ServerLevel serverLevel) {
               dropChance = EnchantmentHelper.processEquipmentDropChance(serverLevel, livingSource, source, dropChance);
            }

            if (!itemStack.isEmpty()
               && !EnchantmentHelper.has(itemStack, EnchantmentEffectComponents.PREVENT_EQUIPMENT_DROP)
               && (killedByPlayer || preserve)
               && this.random.nextFloat() < dropChance) {
               if (!preserve && itemStack.isDamageableItem()) {
                  itemStack.setDamageValue(itemStack.getMaxDamage() - this.random.nextInt(1 + this.random.nextInt(Math.max(itemStack.getMaxDamage() - 3, 1))));
               }

               this.spawnAtLocation(level, itemStack);
               this.setItemSlot(slot, ItemStack.EMPTY);
            }
         }
      }
   }

   public DropChances getDropChances() {
      return this.dropChances;
   }

   public void dropPreservedEquipment(final ServerLevel level) {
      this.dropPreservedEquipment(level, stack -> true);
   }

   public Set<EquipmentSlot> dropPreservedEquipment(final ServerLevel level, final Predicate<ItemStack> shouldDrop) {
      Set<EquipmentSlot> slotsPreventedFromDropping = new HashSet<>();

      for (EquipmentSlot slot : EquipmentSlot.VALUES) {
         ItemStack itemStack = this.getItemBySlot(slot);
         if (!itemStack.isEmpty()) {
            if (!shouldDrop.test(itemStack)) {
               slotsPreventedFromDropping.add(slot);
            } else if (this.dropChances.isPreserved(slot)) {
               this.setItemSlot(slot, ItemStack.EMPTY);
               this.spawnAtLocation(level, itemStack);
            }
         }
      }

      return slotsPreventedFromDropping;
   }

   private LootParams createEquipmentParams(final ServerLevel serverLevel) {
      return new LootParams.Builder(serverLevel)
         .withParameter(LootContextParams.ORIGIN, this.position())
         .withParameter(LootContextParams.THIS_ENTITY, this)
         .create(LootContextParamSets.EQUIPMENT);
   }

   public void equip(final EquipmentTable equipment) {
      this.equip(equipment.lootTable(), equipment.slotDropChances());
   }

   public void equip(final ResourceKey<LootTable> lootTable, final Map<EquipmentSlot, Float> dropChances) {
      if (this.level() instanceof ServerLevel serverLevel) {
         this.equip(lootTable, this.createEquipmentParams(serverLevel), dropChances);
      }
   }

   protected void populateDefaultEquipmentSlots(final RandomSource random, final DifficultyInstance difficulty) {
      if (random.nextFloat() < 0.15F * difficulty.getSpecialMultiplier()) {
         int armorType = random.nextInt(3);

         for (int i = 1; i <= 3.0F; i++) {
            if (random.nextFloat() < 0.1087F) {
               armorType++;
            }
         }

         float partialChance = this.level().getDifficulty() == Difficulty.HARD ? 0.1F : 0.25F;
         boolean first = true;

         for (EquipmentSlot slot : EQUIPMENT_POPULATION_ORDER) {
            ItemStack itemStack = this.getItemBySlot(slot);
            if (!first && random.nextFloat() < partialChance) {
               break;
            }

            first = false;
            if (itemStack.isEmpty()) {
               Item equip = getEquipmentForSlot(slot, armorType);
               if (equip != null) {
                  this.setItemSlot(slot, new ItemStack(equip));
               }
            }
         }
      }
   }

   public static @Nullable Item getEquipmentForSlot(final EquipmentSlot slot, final int type) {
      switch (slot) {
         case HEAD:
            if (type == 0) {
               return Items.LEATHER_HELMET;
            } else if (type == 1) {
               return Items.COPPER_HELMET;
            } else if (type == 2) {
               return Items.GOLDEN_HELMET;
            } else if (type == 3) {
               return Items.CHAINMAIL_HELMET;
            } else if (type == 4) {
               return Items.IRON_HELMET;
            } else if (type == 5) {
               return Items.DIAMOND_HELMET;
            }
         case CHEST:
            if (type == 0) {
               return Items.LEATHER_CHESTPLATE;
            } else if (type == 1) {
               return Items.COPPER_CHESTPLATE;
            } else if (type == 2) {
               return Items.GOLDEN_CHESTPLATE;
            } else if (type == 3) {
               return Items.CHAINMAIL_CHESTPLATE;
            } else if (type == 4) {
               return Items.IRON_CHESTPLATE;
            } else if (type == 5) {
               return Items.DIAMOND_CHESTPLATE;
            }
         case LEGS:
            if (type == 0) {
               return Items.LEATHER_LEGGINGS;
            } else if (type == 1) {
               return Items.COPPER_LEGGINGS;
            } else if (type == 2) {
               return Items.GOLDEN_LEGGINGS;
            } else if (type == 3) {
               return Items.CHAINMAIL_LEGGINGS;
            } else if (type == 4) {
               return Items.IRON_LEGGINGS;
            } else if (type == 5) {
               return Items.DIAMOND_LEGGINGS;
            }
         case FEET:
            if (type == 0) {
               return Items.LEATHER_BOOTS;
            } else if (type == 1) {
               return Items.COPPER_BOOTS;
            } else if (type == 2) {
               return Items.GOLDEN_BOOTS;
            } else if (type == 3) {
               return Items.CHAINMAIL_BOOTS;
            } else if (type == 4) {
               return Items.IRON_BOOTS;
            } else if (type == 5) {
               return Items.DIAMOND_BOOTS;
            }
         default:
            return null;
      }
   }

   protected void populateDefaultEquipmentEnchantments(final ServerLevelAccessor level, final RandomSource random, final DifficultyInstance localDifficulty) {
      this.enchantSpawnedWeapon(level, random, localDifficulty);

      for (EquipmentSlot slot : EquipmentSlot.VALUES) {
         if (slot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR) {
            this.enchantSpawnedArmor(level, random, slot, localDifficulty);
         }
      }
   }

   protected void enchantSpawnedWeapon(final ServerLevelAccessor level, final RandomSource random, final DifficultyInstance difficulty) {
      this.enchantSpawnedEquipment(level, EquipmentSlot.MAINHAND, random, 0.25F, difficulty);
   }

   protected void enchantSpawnedArmor(final ServerLevelAccessor level, final RandomSource random, final EquipmentSlot slot, final DifficultyInstance difficulty) {
      this.enchantSpawnedEquipment(level, slot, random, 0.5F, difficulty);
   }

   private void enchantSpawnedEquipment(
      final ServerLevelAccessor level, final EquipmentSlot slot, final RandomSource random, final float chance, final DifficultyInstance difficulty
   ) {
      ItemStack itemStack = this.getItemBySlot(slot);
      if (!itemStack.isEmpty() && random.nextFloat() < chance * difficulty.getSpecialMultiplier()) {
         EnchantmentHelper.enchantItemFromProvider(itemStack, level.registryAccess(), VanillaEnchantmentProviders.MOB_SPAWN_EQUIPMENT, difficulty, random);
         this.setItemSlot(slot, itemStack);
      }
   }

   public @Nullable SpawnGroupData finalizeSpawn(
      final ServerLevelAccessor level, final DifficultyInstance difficulty, final EntitySpawnReason spawnReason, final @Nullable SpawnGroupData groupData
   ) {
      RandomSource random = level.getRandom();
      AttributeInstance followRange = Objects.requireNonNull(this.getAttribute(Attributes.FOLLOW_RANGE));
      if (!followRange.hasModifier(RANDOM_SPAWN_BONUS_ID)) {
         followRange.addPermanentModifier(
            new AttributeModifier(RANDOM_SPAWN_BONUS_ID, random.triangle(0.0, 0.11485000000000001), AttributeModifier.Operation.ADD_MULTIPLIED_BASE)
         );
      }

      this.setLeftHanded(random.nextFloat() < 0.05F);
      return groupData;
   }

   public void setPersistenceRequired() {
      this.persistenceRequired = true;
   }

   @Override
   public void setDropChance(final EquipmentSlot slot, final float percent) {
      this.dropChances = this.dropChances.withEquipmentChance(slot, percent);
   }

   @Override
   public boolean canPickUpLoot() {
      return this.canPickUpLoot;
   }

   public void setCanPickUpLoot(final boolean canPickUpLoot) {
      this.canPickUpLoot = canPickUpLoot;
   }

   @Override
   protected boolean canDispenserEquipIntoSlot(final EquipmentSlot slot) {
      return this.canPickUpLoot();
   }

   public boolean isPersistenceRequired() {
      return this.persistenceRequired;
   }

   @Override
   public InteractionResult interact(final Player player, final InteractionHand hand, final Vec3 location) {
      if (!this.isAlive()) {
         return InteractionResult.PASS;
      } else {
         InteractionResult interactionResult = this.checkAndHandleImportantInteractions(player, hand);
         if (interactionResult.consumesAction()) {
            this.gameEvent(GameEvent.ENTITY_INTERACT, player);
            return interactionResult;
         } else {
            InteractionResult superReaction = super.interact(player, hand, location);
            if (superReaction != InteractionResult.PASS) {
               return superReaction;
            } else {
               interactionResult = this.mobInteract(player, hand);
               if (interactionResult.consumesAction()) {
                  this.gameEvent(GameEvent.ENTITY_INTERACT, player);
                  return interactionResult;
               } else {
                  return InteractionResult.PASS;
               }
            }
         }
      }
   }

   private InteractionResult checkAndHandleImportantInteractions(final Player player, final InteractionHand hand) {
      ItemStack itemStack = player.getItemInHand(hand);
      if (itemStack.is(Items.NAME_TAG)) {
         InteractionResult nameTagInteractionResult = itemStack.interactLivingEntity(player, this, hand);
         if (nameTagInteractionResult.consumesAction()) {
            return nameTagInteractionResult;
         }
      }

      if (itemStack.getItem() instanceof SpawnEggItem) {
         if (this.level() instanceof ServerLevel serverLevel) {
            Optional<Mob> offspring = SpawnEggItem.spawnOffspringFromSpawnEgg(
               player, this, (EntityType<? extends Mob>)this.getType(), serverLevel, this.position(), itemStack
            );
            offspring.ifPresent(mob -> this.onOffspringSpawnedFromEgg(player, mob));
            if (offspring.isEmpty()) {
               return InteractionResult.PASS;
            }
         }

         return InteractionResult.SUCCESS_SERVER;
      } else {
         return InteractionResult.PASS;
      }
   }

   protected void onOffspringSpawnedFromEgg(final Player spawner, final Mob offspring) {
   }

   protected InteractionResult mobInteract(final Player player, final InteractionHand hand) {
      return InteractionResult.PASS;
   }

   protected void usePlayerItem(final Player player, final InteractionHand hand, final ItemStack itemStack) {
      int beforeUseCount = itemStack.getCount();
      UseRemainder useRemainder = itemStack.get(DataComponents.USE_REMAINDER);
      itemStack.consume(1, player);
      if (useRemainder != null) {
         ItemStack newHandStack = useRemainder.convertIntoRemainder(
            itemStack, beforeUseCount, player.hasInfiniteMaterials(), player::handleExtraItemsCreatedOnUse
         );
         player.setItemInHand(hand, newHandStack);
      }
   }

   public boolean isWithinHome() {
      return this.isWithinHome(this.blockPosition());
   }

   public boolean isWithinHome(final BlockPos pos) {
      return this.homeRadius == -1 ? true : this.homePosition.distSqr(pos) < this.homeRadius * this.homeRadius;
   }

   public boolean isWithinHome(final Vec3 pos) {
      return this.homeRadius == -1 ? true : this.homePosition.distToCenterSqr(pos) < this.homeRadius * this.homeRadius;
   }

   public void setHomeTo(final BlockPos newCenter, final int radius) {
      this.homePosition = newCenter;
      this.homeRadius = radius;
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
      final EntityType<T> entityType,
      final ConversionParams params,
      final EntitySpawnReason spawnReason,
      final ConversionParams.AfterConversion<T> afterConversion
   ) {
      if (this.isRemoved()) {
         return null;
      }

      T newMob = (T)entityType.create(this.level(), spawnReason);
      if (newMob == null) {
         return null;
      }

      params.type().convert(this, newMob, params);
      afterConversion.finalizeConversion(newMob);
      if (this.level() instanceof ServerLevel serverLevel) {
         serverLevel.addFreshEntity(newMob);
      }

      if (params.type().shouldDiscardAfterConversion()) {
         this.discard();
      }

      return newMob;
   }

   public <T extends Mob> @Nullable T convertTo(
      final EntityType<T> entityType, final ConversionParams params, final ConversionParams.AfterConversion<T> afterConversion
   ) {
      return this.convertTo(entityType, params, EntitySpawnReason.CONVERSION, afterConversion);
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
   public void setLeashData(final Leashable.@Nullable LeashData leashData) {
      this.leashData = leashData;
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
   public boolean startRiding(final Entity entity, final boolean force, final boolean sendEventAndTriggers) {
      boolean result = super.startRiding(entity, force, sendEventAndTriggers);
      if (result && this.isLeashed()) {
         this.dropLeash();
      }

      return result;
   }

   @Override
   public boolean isEffectiveAi() {
      return super.isEffectiveAi() && !this.isNoAi();
   }

   public void setNoAi(final boolean flag) {
      byte val = this.entityData.get(DATA_MOB_FLAGS_ID);
      this.entityData.set(DATA_MOB_FLAGS_ID, flag ? (byte)(val | 1) : (byte)(val & -2));
   }

   public void setLeftHanded(final boolean flag) {
      byte val = this.entityData.get(DATA_MOB_FLAGS_ID);
      this.entityData.set(DATA_MOB_FLAGS_ID, flag ? (byte)(val | 2) : (byte)(val & -3));
   }

   public void setAggressive(final boolean flag) {
      byte val = this.entityData.get(DATA_MOB_FLAGS_ID);
      this.entityData.set(DATA_MOB_FLAGS_ID, flag ? (byte)(val | 4) : (byte)(val & -5));
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

   public void setBaby(final boolean baby) {
   }

   @Override
   public HumanoidArm getMainArm() {
      return this.isLeftHanded() ? HumanoidArm.LEFT : HumanoidArm.RIGHT;
   }

   public boolean isWithinMeleeAttackRange(final LivingEntity target) {
      AttackRange attackRange = this.getActiveItem().get(DataComponents.ATTACK_RANGE);
      double maxRange;
      double minRange;
      if (attackRange == null) {
         maxRange = DEFAULT_ATTACK_REACH;
         minRange = 0.0;
      } else {
         maxRange = attackRange.effectiveMaxRange(this);
         minRange = attackRange.effectiveMinRange(this);
      }

      AABB hitbox = target.getHitbox();
      return this.getAttackBoundingBox(maxRange).intersects(hitbox) && (minRange <= 0.0 || !this.getAttackBoundingBox(minRange).intersects(hitbox));
   }

   protected AABB getAttackBoundingBox(final double horizontalExpansion) {
      Entity vehicle = this.getVehicle();
      AABB aabb;
      if (vehicle != null) {
         AABB mountAabb = vehicle.getBoundingBox();
         AABB ownAabb = this.getBoundingBox();
         aabb = new AABB(
            Math.min(ownAabb.minX, mountAabb.minX),
            ownAabb.minY,
            Math.min(ownAabb.minZ, mountAabb.minZ),
            Math.max(ownAabb.maxX, mountAabb.maxX),
            ownAabb.maxY,
            Math.max(ownAabb.maxZ, mountAabb.maxZ)
         );
      } else {
         aabb = this.getBoundingBox();
      }

      return aabb.inflate(horizontalExpansion, 0.0, horizontalExpansion);
   }

   @Override
   public boolean doHurtTarget(final ServerLevel level, final Entity target) {
      float dmg = (float)this.getAttributeValue(Attributes.ATTACK_DAMAGE);
      ItemStack weaponItem = this.getWeaponItem();
      DamageSource damageSource = weaponItem.getDamageSource(this);
      dmg = EnchantmentHelper.modifyDamage(level, weaponItem, target, damageSource, dmg);
      dmg += weaponItem.getItem().getAttackDamageBonus(target, dmg, damageSource);
      Vec3 oldMovement = target.getDeltaMovement();
      boolean wasHurt = target.hurtServer(level, damageSource, dmg);
      if (wasHurt) {
         this.causeExtraKnockback(target, this.getKnockback(target, damageSource), oldMovement, damageSource, dmg, true);
         if (target instanceof LivingEntity livingTarget) {
            weaponItem.hurtEnemy(livingTarget, this);
         }

         EnchantmentHelper.doPostAttackEffects(level, target, damageSource);
         this.setLastHurtMob(target);
         this.playAttackSound();
      }

      this.postPiercingAttack();
      return wasHurt;
   }

   @Override
   protected void jumpInLiquid(final TagKey<Fluid> type) {
      if (this.getNavigation().canFloat()) {
         super.jumpInLiquid(type);
      } else {
         this.setDeltaMovement(this.getDeltaMovement().add(0.0, 0.3, 0.0));
      }
   }

   @VisibleForTesting
   public void removeFreeWill() {
      this.removeAllGoals(goal -> true);
      this.getBrain().removeAllBehaviors();
   }

   @VisibleForDebug
   @VisibleForTesting
   public GoalSelector getGoalSelector() {
      return this.goalSelector;
   }

   public void removeAllGoals(final Predicate<Goal> predicate) {
      this.goalSelector.removeAllGoals(predicate);
   }

   @Override
   protected void removeAfterChangingDimensions() {
      super.removeAfterChangingDimensions();

      for (EquipmentSlot slot : EquipmentSlot.VALUES) {
         ItemStack itemStack = this.getItemBySlot(slot);
         if (!itemStack.isEmpty()) {
            itemStack.setCount(0);
         }
      }
   }

   @Override
   public @Nullable ItemStack getPickResult() {
      return SpawnEggItem.byId(this.getType()).map(ItemStack::new).orElse(null);
   }

   @Override
   protected void onAttributeUpdated(final Holder<Attribute> attribute) {
      super.onAttributeUpdated(attribute);
      if (attribute.is(Attributes.FOLLOW_RANGE) || attribute.is(Attributes.TEMPT_RANGE)) {
         this.getNavigation().updatePathfinderMaxVisitedNodes();
      }
   }

   @Override
   public void registerDebugValues(final ServerLevel level, final DebugValueSource.Registration registration) {
      registration.register(DebugSubscriptions.ENTITY_PATHS, () -> {
         Path path = this.getNavigation().getPath();
         return path != null && path.debugData() != null ? new DebugPathInfo(path.copy(), this.getNavigation().getMaxDistanceToWaypoint()) : null;
      });
      registration.register(
         DebugSubscriptions.GOAL_SELECTORS,
         () -> {
            Set<WrappedGoal> availableGoals = this.goalSelector.getAvailableGoals();
            List<DebugGoalInfo.DebugGoal> goalInfo = new ArrayList<>(availableGoals.size());
            availableGoals.forEach(
               goal -> goalInfo.add(new DebugGoalInfo.DebugGoal(goal.getPriority(), goal.isRunning(), goal.getGoal().getClass().getSimpleName()))
            );
            return new DebugGoalInfo(goalInfo);
         }
      );
      if (!this.brain.isBrainDead()) {
         registration.register(DebugSubscriptions.BRAINS, () -> DebugBrainDump.takeBrainDump(level, this));
      }
   }

   public float chargeSpeedModifier() {
      return 1.0F;
   }
}
