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

   protected AbstractSkeleton(EntityType<? extends AbstractSkeleton> p_454060_, Level p_450530_) {
      super(p_454060_, p_450530_);
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
   protected void playStepSound(BlockPos p_460086_, BlockState p_453511_) {
      this.playSound(this.getStepSound(), 0.15F, 1.0F);
   }

   abstract SoundEvent getStepSound();

   @Override
   public void rideTick() {
      super.rideTick();
      if (this.getControlledVehicle() instanceof PathfinderMob pathfindermob) {
         this.yBodyRot = pathfindermob.yBodyRot;
      }
   }

   @Override
   protected void populateDefaultEquipmentSlots(RandomSource p_455903_, DifficultyInstance p_453701_) {
      super.populateDefaultEquipmentSlots(p_455903_, p_453701_);
      this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
   }

   @Override
   public @Nullable SpawnGroupData finalizeSpawn(
      ServerLevelAccessor p_460416_, DifficultyInstance p_458464_, EntitySpawnReason p_454473_, @Nullable SpawnGroupData p_455064_
   ) {
      p_455064_ = super.finalizeSpawn(p_460416_, p_458464_, p_454473_, p_455064_);
      RandomSource randomsource = p_460416_.getRandom();
      this.populateDefaultEquipmentSlots(randomsource, p_458464_);
      this.populateDefaultEquipmentEnchantments(p_460416_, randomsource, p_458464_);
      this.reassessWeaponGoal();
      this.setCanPickUpLoot(randomsource.nextFloat() < 0.55F * p_458464_.getSpecialMultiplier());
      if (this.getItemBySlot(EquipmentSlot.HEAD).isEmpty() && SpecialDates.isHalloween() && randomsource.nextFloat() < 0.25F) {
         this.setItemSlot(EquipmentSlot.HEAD, new ItemStack(randomsource.nextFloat() < 0.1F ? Blocks.JACK_O_LANTERN : Blocks.CARVED_PUMPKIN));
         this.setDropChance(EquipmentSlot.HEAD, 0.0F);
      }

      return p_455064_;
   }

   public void reassessWeaponGoal() {
      if (this.level() != null && !this.level().isClientSide()) {
         this.goalSelector.removeGoal(this.meleeGoal);
         this.goalSelector.removeGoal(this.bowGoal);
         ItemStack itemstack = this.getItemInHand(ProjectileUtil.getWeaponHoldingHand(this, Items.BOW));
         if (itemstack.is(Items.BOW)) {
            int i = this.getHardAttackInterval();
            if (this.level().getDifficulty() != Difficulty.HARD) {
               i = this.getAttackInterval();
            }

            this.bowGoal.setMinAttackInterval(i);
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
   public void performRangedAttack(LivingEntity p_454925_, float p_452827_) {
      ItemStack itemstack = this.getItemInHand(ProjectileUtil.getWeaponHoldingHand(this, Items.BOW));
      ItemStack itemstack1 = this.getProjectile(itemstack);
      AbstractArrow abstractarrow = this.getArrow(itemstack1, p_452827_, itemstack);
      double d0 = p_454925_.getX() - this.getX();
      double d1 = p_454925_.getY(0.3333333333333333) - abstractarrow.getY();
      double d2 = p_454925_.getZ() - this.getZ();
      double d3 = Math.sqrt(d0 * d0 + d2 * d2);
      if (this.level() instanceof ServerLevel serverlevel) {
         Projectile.spawnProjectileUsingShoot(
            abstractarrow, serverlevel, itemstack1, d0, d1 + d3 * 0.2F, d2, 1.6F, 14 - serverlevel.getDifficulty().getId() * 4
         );
      }

      this.playSound(SoundEvents.SKELETON_SHOOT, 1.0F, 1.0F / (this.getRandom().nextFloat() * 0.4F + 0.8F));
   }

   protected AbstractArrow getArrow(ItemStack p_454991_, float p_456360_, @Nullable ItemStack p_452337_) {
      return ProjectileUtil.getMobArrow(this, p_454991_, p_456360_, p_452337_);
   }

   @Override
   public boolean canUseNonMeleeWeapon(ItemStack p_454490_) {
      return p_454490_.getItem() == Items.BOW;
   }

   @Override
   public TagKey<Item> getPreferredWeaponType() {
      return ItemTags.SKELETON_PREFERRED_WEAPONS;
   }

   @Override
   protected void readAdditionalSaveData(ValueInput p_453230_) {
      super.readAdditionalSaveData(p_453230_);
      this.reassessWeaponGoal();
   }

   @Override
   public void onEquipItem(EquipmentSlot p_459112_, ItemStack p_454489_, ItemStack p_454860_) {
      super.onEquipItem(p_459112_, p_454489_, p_454860_);
      if (!this.level().isClientSide()) {
         this.reassessWeaponGoal();
      }
   }

   public boolean isShaking() {
      return this.isFullyFrozen();
   }

   @Override
   public boolean wantsToPickUp(ServerLevel p_455441_, ItemStack p_454470_) {
      return p_454470_.is(ItemTags.SPEARS) ? false : super.wantsToPickUp(p_455441_, p_454470_);
   }
}
