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

   public Pillager(EntityType<? extends Pillager> p_460359_, Level p_459618_) {
      super(p_460359_, p_459618_);
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
   protected void defineSynchedData(SynchedEntityData.Builder p_450299_) {
      super.defineSynchedData(p_450299_);
      p_450299_.define(IS_CHARGING_CROSSBOW, false);
   }

   @Override
   public boolean canUseNonMeleeWeapon(ItemStack p_455634_) {
      return p_455634_.getItem() == Items.CROSSBOW;
   }

   public boolean isChargingCrossbow() {
      return this.entityData.get(IS_CHARGING_CROSSBOW);
   }

   @Override
   public void setChargingCrossbow(boolean p_450617_) {
      this.entityData.set(IS_CHARGING_CROSSBOW, p_450617_);
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
   protected void addAdditionalSaveData(ValueOutput p_451320_) {
      super.addAdditionalSaveData(p_451320_);
      this.writeInventoryToTag(p_451320_);
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
   protected void readAdditionalSaveData(ValueInput p_458054_) {
      super.readAdditionalSaveData(p_458054_);
      this.readInventoryFromTag(p_458054_);
      this.setCanPickUpLoot(true);
   }

   @Override
   public float getWalkTargetValue(BlockPos p_456039_, LevelReader p_454769_) {
      return 0.0F;
   }

   @Override
   public int getMaxSpawnClusterSize() {
      return 1;
   }

   @Override
   public @Nullable SpawnGroupData finalizeSpawn(
      ServerLevelAccessor p_459679_, DifficultyInstance p_453120_, EntitySpawnReason p_452725_, @Nullable SpawnGroupData p_451802_
   ) {
      RandomSource randomsource = p_459679_.getRandom();
      this.populateDefaultEquipmentSlots(randomsource, p_453120_);
      this.populateDefaultEquipmentEnchantments(p_459679_, randomsource, p_453120_);
      return super.finalizeSpawn(p_459679_, p_453120_, p_452725_, p_451802_);
   }

   @Override
   protected void populateDefaultEquipmentSlots(RandomSource p_454603_, DifficultyInstance p_451969_) {
      this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.CROSSBOW));
   }

   @Override
   protected void enchantSpawnedWeapon(ServerLevelAccessor p_452382_, RandomSource p_450159_, DifficultyInstance p_453919_) {
      super.enchantSpawnedWeapon(p_452382_, p_450159_, p_453919_);
      if (p_450159_.nextInt(300) == 0) {
         ItemStack itemstack = this.getMainHandItem();
         if (itemstack.is(Items.CROSSBOW)) {
            EnchantmentHelper.enchantItemFromProvider(
               itemstack, p_452382_.registryAccess(), VanillaEnchantmentProviders.PILLAGER_SPAWN_CROSSBOW, p_453919_, p_450159_
            );
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
   protected SoundEvent getHurtSound(DamageSource p_456626_) {
      return SoundEvents.PILLAGER_HURT;
   }

   @Override
   public void performRangedAttack(LivingEntity p_451038_, float p_460531_) {
      this.performCrossbowAttack(this, 1.6F);
   }

   @Override
   public SimpleContainer getInventory() {
      return this.inventory;
   }

   @Override
   protected void pickUpItem(ServerLevel p_451687_, ItemEntity p_453416_) {
      ItemStack itemstack = p_453416_.getItem();
      if (itemstack.getItem() instanceof BannerItem) {
         super.pickUpItem(p_451687_, p_453416_);
      } else if (this.wantsItem(itemstack)) {
         this.onItemPickup(p_453416_);
         ItemStack itemstack1 = this.inventory.addItem(itemstack);
         if (itemstack1.isEmpty()) {
            p_453416_.discard();
         } else {
            itemstack.setCount(itemstack1.getCount());
         }
      }
   }

   private boolean wantsItem(ItemStack p_455962_) {
      return this.hasActiveRaid() && p_455962_.is(Items.WHITE_BANNER);
   }

   @Override
   public @Nullable SlotAccess getSlot(int p_452816_) {
      int i = p_452816_ - 300;
      return i >= 0 && i < this.inventory.getContainerSize() ? this.inventory.getSlot(i) : super.getSlot(p_452816_);
   }

   @Override
   public void applyRaidBuffs(ServerLevel p_450394_, int p_450630_, boolean p_452286_) {
      Raid raid = this.getCurrentRaid();
      boolean flag = this.random.nextFloat() <= raid.getEnchantOdds();
      if (flag) {
         ItemStack itemstack = new ItemStack(Items.CROSSBOW);
         ResourceKey<EnchantmentProvider> resourcekey;
         if (p_450630_ > raid.getNumGroups(Difficulty.NORMAL)) {
            resourcekey = VanillaEnchantmentProviders.RAID_PILLAGER_POST_WAVE_5;
         } else if (p_450630_ > raid.getNumGroups(Difficulty.EASY)) {
            resourcekey = VanillaEnchantmentProviders.RAID_PILLAGER_POST_WAVE_3;
         } else {
            resourcekey = null;
         }

         if (resourcekey != null) {
            EnchantmentHelper.enchantItemFromProvider(
               itemstack, p_450394_.registryAccess(), resourcekey, p_450394_.getCurrentDifficultyAt(this.blockPosition()), this.getRandom()
            );
            this.setItemSlot(EquipmentSlot.MAINHAND, itemstack);
         }
      }
   }

   @Override
   public SoundEvent getCelebrateSound() {
      return SoundEvents.PILLAGER_CELEBRATE;
   }
}
