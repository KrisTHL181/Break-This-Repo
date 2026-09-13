package net.minecraft.world.entity.monster.illager;

import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.RangedCrossbowAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.CrossbowAttackMob;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.creaking.Creaking;
import net.minecraft.world.entity.npc.InventoryCarrier;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.providers.EnchantmentProvider;
import net.minecraft.world.item.enchantment.providers.VanillaEnchantmentProviders;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class Pillager extends AbstractIllager implements CrossbowAttackMob, InventoryCarrier {
   private static final EntityDataAccessor<Boolean> IS_CHARGING_CROSSBOW = SynchedEntityData.defineId(Pillager.class, EntityDataSerializers.BOOLEAN);
   private static final int INVENTORY_SIZE = 5;
   private static final int SLOT_OFFSET = 300;
   private final SimpleContainer inventory = new SimpleContainer(5);

   public Pillager(final EntityType<? extends Pillager> type, final Level level) {
      super(type, level);
   }

   @Override
   protected void registerGoals() {
      super.registerGoals();
      this.goalSelector.addGoal(0, new FloatGoal(this));
      this.goalSelector.addGoal(1, new AvoidEntityGoal<>(this, Creaking.class, 8.0F, 1.0, 1.2));
      this.goalSelector.addGoal(2, new Raider.HoldGroundAttackGoal(this, 10.0F));
      this.goalSelector.addGoal(3, new RangedCrossbowAttackGoal<>(this, 1.0, 8.0F));
      this.goalSelector.addGoal(8, new RandomStrollGoal(this, 0.6));
      this.goalSelector.addGoal(9, new LookAtPlayerGoal(this, Player.class, 15.0F, 1.0F));
      this.goalSelector.addGoal(10, new LookAtPlayerGoal(this, Mob.class, 15.0F));
      this.targetSelector.addGoal(1, new HurtByTargetGoal(this, Raider.class).setAlertOthers());
      this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
      this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, AbstractVillager.class, false));
      this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, IronGolem.class, true));
   }

   public static AttributeSupplier.Builder createAttributes() {
      return Monster.createMonsterAttributes()
         .add(Attributes.MOVEMENT_SPEED, 0.35F)
         .add(Attributes.MAX_HEALTH, 24.0)
         .add(Attributes.ATTACK_DAMAGE, 5.0)
         .add(Attributes.FOLLOW_RANGE, 32.0);
   }

   @Override
   protected void defineSynchedData(final SynchedEntityData.Builder entityData) {
      super.defineSynchedData(entityData);
      entityData.define(IS_CHARGING_CROSSBOW, false);
   }

   @Override
   public boolean canUseNonMeleeWeapon(final ItemStack item) {
      return item.getItem() == Items.CROSSBOW;
   }

   public boolean isChargingCrossbow() {
      return this.entityData.get(IS_CHARGING_CROSSBOW);
   }

   @Override
   public void setChargingCrossbow(final boolean isCharging) {
      this.entityData.set(IS_CHARGING_CROSSBOW, isCharging);
   }

   @Override
   public void onCrossbowAttackPerformed() {
      this.noActionTime = 0;
   }

   @Override
   public TagKey<Item> getPreferredWeaponType() {
      return ItemTags.PILLAGER_PREFERRED_WEAPONS;
   }

   @Override
   protected void addAdditionalSaveData(final ValueOutput output) {
      super.addAdditionalSaveData(output);
      this.writeInventoryToTag(output);
   }

   @Override
   public AbstractIllager.IllagerArmPose getArmPose() {
      if (this.isChargingCrossbow()) {
         return AbstractIllager.IllagerArmPose.CROSSBOW_CHARGE;
      } else if (this.isHolding(Items.CROSSBOW)) {
         return AbstractIllager.IllagerArmPose.CROSSBOW_HOLD;
      } else {
         return this.isAggressive() ? AbstractIllager.IllagerArmPose.ATTACKING : AbstractIllager.IllagerArmPose.NEUTRAL;
      }
   }

   @Override
   protected void readAdditionalSaveData(final ValueInput input) {
      super.readAdditionalSaveData(input);
      this.readInventoryFromTag(input);
      this.setCanPickUpLoot(true);
   }

   @Override
   public float getWalkTargetValue(final BlockPos pos, final LevelReader level) {
      return 0.0F;
   }

   @Override
   public int getMaxSpawnClusterSize() {
      return 1;
   }

   @Override
   public @Nullable SpawnGroupData finalizeSpawn(
      final ServerLevelAccessor level, final DifficultyInstance difficulty, final EntitySpawnReason spawnReason, final @Nullable SpawnGroupData groupData
   ) {
      RandomSource random = level.getRandom();
      this.populateDefaultEquipmentSlots(random, difficulty);
      this.populateDefaultEquipmentEnchantments(level, random, difficulty);
      return super.finalizeSpawn(level, difficulty, spawnReason, groupData);
   }

   @Override
   protected void populateDefaultEquipmentSlots(final RandomSource random, final DifficultyInstance difficulty) {
      this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.CROSSBOW));
   }

   @Override
   protected void enchantSpawnedWeapon(final ServerLevelAccessor level, final RandomSource random, final DifficultyInstance difficulty) {
      super.enchantSpawnedWeapon(level, random, difficulty);
      if (random.nextInt(300) == 0) {
         ItemStack weapon = this.getMainHandItem();
         if (weapon.is(Items.CROSSBOW)) {
            EnchantmentHelper.enchantItemFromProvider(weapon, level.registryAccess(), VanillaEnchantmentProviders.PILLAGER_SPAWN_CROSSBOW, difficulty, random);
         }
      }
   }

   @Override
   protected SoundEvent getAmbientSound() {
      return SoundEvents.PILLAGER_AMBIENT;
   }

   @Override
   protected SoundEvent getDeathSound() {
      return SoundEvents.PILLAGER_DEATH;
   }

   @Override
   protected SoundEvent getHurtSound(final DamageSource source) {
      return SoundEvents.PILLAGER_HURT;
   }

   @Override
   public void performRangedAttack(final LivingEntity target, final float power) {
      this.performCrossbowAttack(this, 1.6F);
   }

   @Override
   public SimpleContainer getInventory() {
      return this.inventory;
   }

   @Override
   protected void pickUpItem(final ServerLevel level, final ItemEntity entity) {
      ItemStack itemStack = entity.getItem();
      if (itemStack.getItem() instanceof BannerItem) {
         super.pickUpItem(level, entity);
      } else if (this.wantsItem(itemStack)) {
         this.onItemPickup(entity);
         ItemStack remainder = this.inventory.addItem(itemStack);
         if (remainder.isEmpty()) {
            entity.discard();
         } else {
            itemStack.setCount(remainder.getCount());
         }
      }
   }

   private boolean wantsItem(final ItemStack itemStack) {
      return this.hasActiveRaid() && itemStack.is(Items.BANNER.white());
   }

   @Override
   public @Nullable SlotAccess getSlot(final int slot) {
      int inventorySlot = slot - 300;
      return inventorySlot >= 0 && inventorySlot < this.inventory.getContainerSize() ? this.inventory.getSlot(inventorySlot) : super.getSlot(slot);
   }

   @Override
   public void applyRaidBuffs(final ServerLevel level, final int wave, final boolean isCaptain) {
      Raid raid = this.getCurrentRaid();
      boolean shouldEnchant = this.random.nextFloat() <= raid.getEnchantOdds();
      if (shouldEnchant) {
         ItemStack crossbow = new ItemStack(Items.CROSSBOW);
         ResourceKey<EnchantmentProvider> provider;
         if (wave > raid.getNumGroups(Difficulty.NORMAL)) {
            provider = VanillaEnchantmentProviders.RAID_PILLAGER_POST_WAVE_5;
         } else if (wave > raid.getNumGroups(Difficulty.EASY)) {
            provider = VanillaEnchantmentProviders.RAID_PILLAGER_POST_WAVE_3;
         } else {
            provider = null;
         }

         if (provider != null) {
            EnchantmentHelper.enchantItemFromProvider(
               crossbow, level.registryAccess(), provider, level.getCurrentDifficultyAt(this.blockPosition()), this.getRandom()
            );
            this.setItemSlot(EquipmentSlot.MAINHAND, crossbow);
         }
      }
   }

   @Override
   public SoundEvent getCelebrateSound() {
      return SoundEvents.PILLAGER_CELEBRATE;
   }
}
