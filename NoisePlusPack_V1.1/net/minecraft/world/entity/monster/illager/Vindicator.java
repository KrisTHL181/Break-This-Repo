package net.minecraft.world.entity.monster.illager;

import java.util.EnumSet;
import java.util.function.Predicate;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.BreakDoorGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.util.GoalUtils;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.creaking.Creaking;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.providers.EnchantmentProvider;
import net.minecraft.world.item.enchantment.providers.VanillaEnchantmentProviders;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class Vindicator extends AbstractIllager {
   private static final String TAG_JOHNNY = "Johnny";
   static final Predicate<Difficulty> DOOR_BREAKING_PREDICATE = p_454740_ -> p_454740_ == Difficulty.NORMAL || p_454740_ == Difficulty.HARD;
   private static final boolean DEFAULT_JOHNNY = false;
   boolean isJohnny = false;

   public Vindicator(EntityType<? extends Vindicator> p_457448_, Level p_460698_) {
      super(p_457448_, p_460698_);
   }

   @Override
   protected void registerGoals() {
      super.registerGoals();
      this.goalSelector.addGoal(0, new FloatGoal(this));
      this.goalSelector.addGoal(1, new AvoidEntityGoal<>(this, Creaking.class, 8.0F, 1.0, 1.2));
      this.goalSelector.addGoal(2, new Vindicator.VindicatorBreakDoorGoal(this));
      this.goalSelector.addGoal(3, new AbstractIllager.RaiderOpenDoorGoal(this));
      this.goalSelector.addGoal(4, new Raider.HoldGroundAttackGoal(this, 10.0F));
      this.goalSelector.addGoal(5, new MeleeAttackGoal(this, 1.0, false));
      this.targetSelector.addGoal(1, new HurtByTargetGoal(this, Raider.class).setAlertOthers());
      this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
      this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, AbstractVillager.class, true));
      this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, IronGolem.class, true));
      this.targetSelector.addGoal(4, new Vindicator.VindicatorJohnnyAttackGoal(this));
      this.goalSelector.addGoal(8, new RandomStrollGoal(this, 0.6));
      this.goalSelector.addGoal(9, new LookAtPlayerGoal(this, Player.class, 3.0F, 1.0F));
      this.goalSelector.addGoal(10, new LookAtPlayerGoal(this, Mob.class, 8.0F));
   }

   @Override
   protected void customServerAiStep(ServerLevel p_458389_) {
      if (!this.isNoAi() && GoalUtils.hasGroundPathNavigation(this)) {
         boolean flag = p_458389_.isRaided(this.blockPosition());
         this.getNavigation().setCanOpenDoors(flag);
      }

      super.customServerAiStep(p_458389_);
   }

   public static AttributeSupplier.Builder createAttributes() {
      return Monster.createMonsterAttributes()
         .add(Attributes.MOVEMENT_SPEED, 0.35F)
         .add(Attributes.FOLLOW_RANGE, 12.0)
         .add(Attributes.MAX_HEALTH, 24.0)
         .add(Attributes.ATTACK_DAMAGE, 5.0);
   }

   @Override
   protected void addAdditionalSaveData(ValueOutput p_452002_) {
      super.addAdditionalSaveData(p_452002_);
      if (this.isJohnny) {
         p_452002_.putBoolean("Johnny", true);
      }
   }

   @Override
   public AbstractIllager.IllagerArmPose getArmPose() {
      if (this.isAggressive()) {
         return AbstractIllager.IllagerArmPose.ATTACKING;
      } else {
         return this.isCelebrating() ? AbstractIllager.IllagerArmPose.CELEBRATING : AbstractIllager.IllagerArmPose.CROSSED;
      }
   }

   @Override
   protected void readAdditionalSaveData(ValueInput p_450265_) {
      super.readAdditionalSaveData(p_450265_);
      this.isJohnny = p_450265_.getBooleanOr("Johnny", false);
   }

   @Override
   public SoundEvent getCelebrateSound() {
      return SoundEvents.VINDICATOR_CELEBRATE;
   }

   @Override
   public @Nullable SpawnGroupData finalizeSpawn(
      ServerLevelAccessor p_451206_, DifficultyInstance p_457997_, EntitySpawnReason p_455027_, @Nullable SpawnGroupData p_455702_
   ) {
      SpawnGroupData spawngroupdata = super.finalizeSpawn(p_451206_, p_457997_, p_455027_, p_455702_);
      this.getNavigation().setCanOpenDoors(true);
      RandomSource randomsource = p_451206_.getRandom();
      this.populateDefaultEquipmentSlots(randomsource, p_457997_);
      this.populateDefaultEquipmentEnchantments(p_451206_, randomsource, p_457997_);
      return spawngroupdata;
   }

   @Override
   protected void populateDefaultEquipmentSlots(RandomSource p_453927_, DifficultyInstance p_453272_) {
      if (this.getCurrentRaid() == null) {
         this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_AXE));
      }
   }

   @Override
   public void setCustomName(@Nullable Component p_456874_) {
      super.setCustomName(p_456874_);
      if (!this.isJohnny && p_456874_ != null && p_456874_.getString().equals("Johnny")) {
         this.isJohnny = true;
      }
   }

   @Override
   protected SoundEvent getAmbientSound() {
      return SoundEvents.VINDICATOR_AMBIENT;
   }

   @Override
   protected SoundEvent getDeathSound() {
      return SoundEvents.VINDICATOR_DEATH;
   }

   @Override
   protected SoundEvent getHurtSound(DamageSource p_455210_) {
      return SoundEvents.VINDICATOR_HURT;
   }

   @Override
   public void applyRaidBuffs(ServerLevel p_455492_, int p_455175_, boolean p_458853_) {
      ItemStack itemstack = new ItemStack(Items.IRON_AXE);
      Raid raid = this.getCurrentRaid();
      boolean flag = this.random.nextFloat() <= raid.getEnchantOdds();
      if (flag) {
         ResourceKey<EnchantmentProvider> resourcekey = p_455175_ > raid.getNumGroups(Difficulty.NORMAL)
            ? VanillaEnchantmentProviders.RAID_VINDICATOR_POST_WAVE_5
            : VanillaEnchantmentProviders.RAID_VINDICATOR;
         EnchantmentHelper.enchantItemFromProvider(
            itemstack, p_455492_.registryAccess(), resourcekey, p_455492_.getCurrentDifficultyAt(this.blockPosition()), this.random
         );
      }

      this.setItemSlot(EquipmentSlot.MAINHAND, itemstack);
   }

   static class VindicatorBreakDoorGoal extends BreakDoorGoal {
      public VindicatorBreakDoorGoal(Mob p_450866_) {
         super(p_450866_, 6, Vindicator.DOOR_BREAKING_PREDICATE);
         this.setFlags(EnumSet.of(Goal.Flag.MOVE));
      }

      @Override
      public boolean canContinueToUse() {
         Vindicator vindicator = (Vindicator)this.mob;
         return vindicator.hasActiveRaid() && super.canContinueToUse();
      }

      @Override
      public boolean canUse() {
         Vindicator vindicator = (Vindicator)this.mob;
         return vindicator.hasActiveRaid() && vindicator.random.nextInt(reducedTickDelay(10)) == 0 && super.canUse();
      }

      @Override
      public void start() {
         super.start();
         this.mob.setNoActionTime(0);
      }
   }

   static class VindicatorJohnnyAttackGoal extends NearestAttackableTargetGoal<LivingEntity> {
      public VindicatorJohnnyAttackGoal(Vindicator p_458344_) {
         super(p_458344_, LivingEntity.class, 0, true, true, (p_457251_, p_451815_) -> p_457251_.attackable());
      }

      @Override
      public boolean canUse() {
         return ((Vindicator)this.mob).isJohnny && super.canUse();
      }

      @Override
      public void start() {
         super.start();
         this.mob.setNoActionTime(0);
      }
   }
}
