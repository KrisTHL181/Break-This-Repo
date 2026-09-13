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
   private static final Predicate<Difficulty> DOOR_BREAKING_PREDICATE = d -> d == Difficulty.NORMAL || d == Difficulty.HARD;
   private static final boolean DEFAULT_JOHNNY = false;
   private boolean isJohnny = false;

   public Vindicator(final EntityType<? extends Vindicator> type, final Level level) {
      super(type, level);
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
   protected void customServerAiStep(final ServerLevel level) {
      if (!this.isNoAi() && GoalUtils.hasGroundPathNavigation(this)) {
         boolean canOpenDoors = level.isRaided(this.blockPosition());
         this.getNavigation().setCanOpenDoors(canOpenDoors);
      }

      super.customServerAiStep(level);
   }

   public static AttributeSupplier.Builder createAttributes() {
      return Monster.createMonsterAttributes()
         .add(Attributes.MOVEMENT_SPEED, 0.35F)
         .add(Attributes.FOLLOW_RANGE, 12.0)
         .add(Attributes.MAX_HEALTH, 24.0)
         .add(Attributes.ATTACK_DAMAGE, 5.0);
   }

   @Override
   protected void addAdditionalSaveData(final ValueOutput output) {
      super.addAdditionalSaveData(output);
      if (this.isJohnny) {
         output.putBoolean("Johnny", true);
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
   protected void readAdditionalSaveData(final ValueInput input) {
      super.readAdditionalSaveData(input);
      this.isJohnny = input.getBooleanOr("Johnny", false);
   }

   @Override
   public SoundEvent getCelebrateSound() {
      return SoundEvents.VINDICATOR_CELEBRATE;
   }

   @Override
   public @Nullable SpawnGroupData finalizeSpawn(
      final ServerLevelAccessor level, final DifficultyInstance difficulty, final EntitySpawnReason spawnReason, final @Nullable SpawnGroupData groupData
   ) {
      SpawnGroupData spawnGroupData = super.finalizeSpawn(level, difficulty, spawnReason, groupData);
      this.getNavigation().setCanOpenDoors(true);
      RandomSource random = level.getRandom();
      this.populateDefaultEquipmentSlots(random, difficulty);
      this.populateDefaultEquipmentEnchantments(level, random, difficulty);
      return spawnGroupData;
   }

   @Override
   protected void populateDefaultEquipmentSlots(final RandomSource random, final DifficultyInstance difficulty) {
      if (this.getCurrentRaid() == null) {
         this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_AXE));
      }
   }

   @Override
   public void setCustomName(final @Nullable Component name) {
      super.setCustomName(name);
      if (!this.isJohnny && name != null && name.getString().equals("Johnny")) {
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
   protected SoundEvent getHurtSound(final DamageSource source) {
      return SoundEvents.VINDICATOR_HURT;
   }

   @Override
   public void applyRaidBuffs(final ServerLevel level, final int wave, final boolean isCaptain) {
      ItemStack axe = new ItemStack(Items.IRON_AXE);
      Raid raid = this.getCurrentRaid();
      boolean shouldEnchant = this.random.nextFloat() <= raid.getEnchantOdds();
      if (shouldEnchant) {
         ResourceKey<EnchantmentProvider> provider = wave > raid.getNumGroups(Difficulty.NORMAL)
            ? VanillaEnchantmentProviders.RAID_VINDICATOR_POST_WAVE_5
            : VanillaEnchantmentProviders.RAID_VINDICATOR;
         EnchantmentHelper.enchantItemFromProvider(axe, level.registryAccess(), provider, level.getCurrentDifficultyAt(this.blockPosition()), this.random);
      }

      this.setItemSlot(EquipmentSlot.MAINHAND, axe);
   }

   private static class VindicatorBreakDoorGoal extends BreakDoorGoal {
      public VindicatorBreakDoorGoal(final Mob mob) {
         super(mob, 6, Vindicator.DOOR_BREAKING_PREDICATE);
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

   private static class VindicatorJohnnyAttackGoal extends NearestAttackableTargetGoal<LivingEntity> {
      public VindicatorJohnnyAttackGoal(final Vindicator mob) {
         super(mob, LivingEntity.class, 0, true, true, (target, level) -> target.attackable());
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
