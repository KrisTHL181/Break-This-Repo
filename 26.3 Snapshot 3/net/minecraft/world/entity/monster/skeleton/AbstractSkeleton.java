package net.minecraft.world.entity.monster.skeleton;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.util.SpecialDates;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FleeSunGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RangedBowAttackGoal;
import net.minecraft.world.entity.ai.goal.RestrictSunGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.animal.turtle.Turtle;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import org.jspecify.annotations.Nullable;

public abstract class AbstractSkeleton extends Monster implements RangedAttackMob {
   private static final int HARD_ATTACK_INTERVAL = 20;
   private static final int NORMAL_ATTACK_INTERVAL = 40;
   protected static final int INCREASED_HARD_ATTACK_INTERVAL = 50;
   protected static final int INCREASED_NORMAL_ATTACK_INTERVAL = 70;
   private final RangedBowAttackGoal<AbstractSkeleton> bowGoal = new RangedBowAttackGoal<>(this, 1.0, 20, 15.0F);
   private final MeleeAttackGoal meleeGoal = new MeleeAttackGoal(this, 1.2, false) {
      @Override
      public void stop() {
         super.stop();
         AbstractSkeleton.this.setAggressive(false);
      }

      @Override
      public void start() {
         super.start();
         AbstractSkeleton.this.setAggressive(true);
      }
   };

   protected AbstractSkeleton(final EntityType<? extends AbstractSkeleton> type, final Level level) {
      super(type, level);
      this.reassessWeaponGoal();
   }

   @Override
   protected void registerGoals() {
      this.goalSelector.addGoal(2, new RestrictSunGoal(this));
      this.goalSelector.addGoal(3, new FleeSunGoal(this, 1.0));
      this.goalSelector.addGoal(3, new AvoidEntityGoal<>(this, Wolf.class, 6.0F, 1.0, 1.2));
      this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 1.0));
      this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
      this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));
      this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
      this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
      this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, IronGolem.class, true));
      this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Turtle.class, 10, true, false, Turtle.BABY_ON_LAND_SELECTOR));
   }

   public static AttributeSupplier.Builder createAttributes() {
      return Monster.createMonsterAttributes().add(Attributes.MOVEMENT_SPEED, 0.25);
   }

   @Override
   protected void playStepSound(final BlockPos pos, final BlockState blockState) {
      this.playSound(this.getStepSound(), 0.15F, 1.0F);
   }

   protected abstract SoundEvent getStepSound();

   @Override
   public void rideTick() {
      super.rideTick();
      if (this.getControlledVehicle() instanceof PathfinderMob entity) {
         this.yBodyRot = entity.yBodyRot;
      }
   }

   @Override
   protected void populateDefaultEquipmentSlots(final RandomSource random, final DifficultyInstance difficulty) {
      super.populateDefaultEquipmentSlots(random, difficulty);
      this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
   }

   @Override
   public @Nullable SpawnGroupData finalizeSpawn(
      final ServerLevelAccessor level, final DifficultyInstance difficulty, final EntitySpawnReason spawnReason, @Nullable SpawnGroupData groupData
   ) {
      groupData = super.finalizeSpawn(level, difficulty, spawnReason, groupData);
      RandomSource random = level.getRandom();
      this.populateDefaultEquipmentSlots(random, difficulty);
      this.populateDefaultEquipmentEnchantments(level, random, difficulty);
      this.reassessWeaponGoal();
      this.setCanPickUpLoot(random.nextFloat() < 0.55F * difficulty.getSpecialMultiplier());
      if (this.getItemBySlot(EquipmentSlot.HEAD).isEmpty() && SpecialDates.isHalloween() && random.nextFloat() < 0.25F) {
         this.setItemSlot(EquipmentSlot.HEAD, new ItemStack(random.nextFloat() < 0.1F ? Blocks.JACK_O_LANTERN : Blocks.CARVED_PUMPKIN));
         this.setDropChance(EquipmentSlot.HEAD, 0.0F);
      }

      return groupData;
   }

   public void reassessWeaponGoal() {
      if (this.level() != null && !this.level().isClientSide()) {
         this.goalSelector.removeGoal(this.meleeGoal);
         this.goalSelector.removeGoal(this.bowGoal);
         ItemStack usedWeapon = this.getItemInHand(ProjectileUtil.getWeaponHoldingHand(this, Items.BOW));
         if (usedWeapon.is(Items.BOW)) {
            int minAttackInterval = this.getHardAttackInterval();
            if (this.level().getDifficulty() != Difficulty.HARD) {
               minAttackInterval = this.getAttackInterval();
            }

            this.bowGoal.setMinAttackInterval(minAttackInterval);
            this.goalSelector.addGoal(4, this.bowGoal);
         } else {
            this.goalSelector.addGoal(4, this.meleeGoal);
         }
      }
   }

   protected int getHardAttackInterval() {
      return 20;
   }

   protected int getAttackInterval() {
      return 40;
   }

   @Override
   public void performRangedAttack(final LivingEntity target, final float power) {
      ItemStack bowItem = this.getItemInHand(ProjectileUtil.getWeaponHoldingHand(this, Items.BOW));
      ItemStack projectile = this.getProjectile(bowItem);
      AbstractArrow arrow = this.getArrow(projectile, power, bowItem);
      double xd = target.getX() - this.getX();
      double yd = target.getY(0.3333333333333333) - arrow.getY();
      double zd = target.getZ() - this.getZ();
      double distanceToTarget = Math.sqrt(xd * xd + zd * zd);
      if (this.level() instanceof ServerLevel serverLevel) {
         Projectile.spawnProjectileUsingShoot(
            arrow, serverLevel, projectile, xd, yd + distanceToTarget * 0.2F, zd, 1.6F, this.rangedAttackUncertainty(serverLevel)
         );
      }

      this.playSound(SoundEvents.SKELETON_SHOOT, 1.0F, 1.0F / (this.getRandom().nextFloat() * 0.4F + 0.8F));
   }

   protected AbstractArrow getArrow(final ItemStack projectile, final float power, final @Nullable ItemStack firingWeapon) {
      return ProjectileUtil.getMobArrow(this, projectile, power, firingWeapon);
   }

   @Override
   public boolean canUseNonMeleeWeapon(final ItemStack item) {
      return item.getItem() == Items.BOW;
   }

   @Override
   public TagKey<Item> getPreferredWeaponType() {
      return ItemTags.SKELETON_PREFERRED_WEAPONS;
   }

   @Override
   protected void readAdditionalSaveData(final ValueInput input) {
      super.readAdditionalSaveData(input);
      this.reassessWeaponGoal();
   }

   @Override
   public void onEquipItem(final EquipmentSlot slot, final ItemStack oldStack, final ItemStack stack) {
      super.onEquipItem(slot, oldStack, stack);
      if (!this.level().isClientSide()) {
         this.reassessWeaponGoal();
      }
   }

   public boolean isShaking() {
      return this.isFullyFrozen();
   }

   @Override
   public boolean wantsToPickUp(final ServerLevel level, final ItemStack itemStack) {
      return itemStack.is(ItemTags.SPEARS) ? false : super.wantsToPickUp(level, itemStack);
   }
}
