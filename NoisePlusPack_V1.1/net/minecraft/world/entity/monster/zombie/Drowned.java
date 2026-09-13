package net.minecraft.world.entity.monster.zombie;

import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.MoveToBlockGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.ai.goal.ZombieAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.AmphibiousPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.animal.nautilus.ZombieNautilus;
import net.minecraft.world.entity.animal.turtle.Turtle;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.ThrownTrident;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class Drowned extends Zombie implements RangedAttackMob {
   public static final float NAUTILUS_SHELL_CHANCE = 0.03F;
   private static final float ZOMBIE_NAUTILUS_JOCKEY_CHANCE = 0.5F;
   boolean searchingForLand;

   public Drowned(EntityType<? extends Drowned> p_452510_, Level p_461002_) {
      super(p_452510_, p_461002_);
      this.moveControl = new Drowned.DrownedMoveControl(this);
      this.setPathfindingMalus(PathType.WATER, 0.0F);
   }

   public static AttributeSupplier.Builder createAttributes() {
      return Zombie.createAttributes().add(Attributes.STEP_HEIGHT, 1.0);
   }

   @Override
   protected PathNavigation createNavigation(Level p_456289_) {
      return new AmphibiousPathNavigation(this, p_456289_);
   }

   @Override
   protected void addBehaviourGoals() {
      this.goalSelector.addGoal(1, new Drowned.DrownedGoToWaterGoal(this, 1.0));
      this.goalSelector.addGoal(2, new Drowned.DrownedTridentAttackGoal(this, 1.0, 40, 10.0F));
      this.goalSelector.addGoal(2, new Drowned.DrownedAttackGoal(this, 1.0, false));
      this.goalSelector.addGoal(5, new Drowned.DrownedGoToBeachGoal(this, 1.0));
      this.goalSelector.addGoal(6, new Drowned.DrownedSwimUpGoal(this, 1.0, this.level().getSeaLevel()));
      this.goalSelector.addGoal(7, new RandomStrollGoal(this, 1.0));
      this.targetSelector.addGoal(1, new HurtByTargetGoal(this, Drowned.class).setAlertOthers(ZombifiedPiglin.class));
      this.targetSelector
         .addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false, (p_456880_, p_452250_) -> this.okTarget(p_456880_)));
      this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, AbstractVillager.class, false));
      this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, IronGolem.class, true));
      this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Axolotl.class, true, false));
      this.targetSelector.addGoal(5, new NearestAttackableTargetGoal<>(this, Turtle.class, 10, true, false, Turtle.BABY_ON_LAND_SELECTOR));
   }

   @Override
   public SpawnGroupData finalizeSpawn(
      ServerLevelAccessor p_452950_, DifficultyInstance p_452906_, EntitySpawnReason p_457606_, @Nullable SpawnGroupData p_450440_
   ) {
      p_450440_ = super.finalizeSpawn(p_452950_, p_452906_, p_457606_, p_450440_);
      if (this.getItemBySlot(EquipmentSlot.OFFHAND).isEmpty() && p_452950_.getRandom().nextFloat() < 0.03F) {
         this.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.NAUTILUS_SHELL));
         this.setGuaranteedDrop(EquipmentSlot.OFFHAND);
      }

      if ((p_457606_ == EntitySpawnReason.NATURAL || p_457606_ == EntitySpawnReason.STRUCTURE)
         && this.getMainHandItem().is(Items.TRIDENT)
         && p_452950_.getRandom().nextFloat() < 0.5F
         && !this.isBaby()
         && !p_452950_.getBiome(this.blockPosition()).is(BiomeTags.MORE_FREQUENT_DROWNED_SPAWNS)) {
         ZombieNautilus zombienautilus = EntityType.ZOMBIE_NAUTILUS.create(this.level(), EntitySpawnReason.JOCKEY);
         if (zombienautilus != null) {
            if (p_457606_ == EntitySpawnReason.STRUCTURE) {
               zombienautilus.setPersistenceRequired();
            }

            zombienautilus.snapTo(this.getX(), this.getY(), this.getZ(), this.getYRot(), 0.0F);
            zombienautilus.finalizeSpawn(p_452950_, p_452906_, p_457606_, null);
            this.startRiding(zombienautilus, false, false);
            p_452950_.addFreshEntity(zombienautilus);
         }
      }

      return p_450440_;
   }

   public static boolean checkDrownedSpawnRules(
      EntityType<Drowned> p_455235_, ServerLevelAccessor p_452918_, EntitySpawnReason p_456173_, BlockPos p_456238_, RandomSource p_455425_
   ) {
      if (!p_452918_.getFluidState(p_456238_.below()).is(FluidTags.WATER) && !EntitySpawnReason.isSpawner(p_456173_)) {
         return false;
      } else {
         Holder<Biome> holder = p_452918_.getBiome(p_456238_);
         boolean flag = p_452918_.getDifficulty() != Difficulty.PEACEFUL
            && (EntitySpawnReason.ignoresLightRequirements(p_456173_) || isDarkEnoughToSpawn(p_452918_, p_456238_, p_455425_))
            && (EntitySpawnReason.isSpawner(p_456173_) || p_452918_.getFluidState(p_456238_).is(FluidTags.WATER));
         if (!flag || !EntitySpawnReason.isSpawner(p_456173_) && p_456173_ != EntitySpawnReason.REINFORCEMENT) {
            return holder.is(BiomeTags.MORE_FREQUENT_DROWNED_SPAWNS)
               ? p_455425_.nextInt(15) == 0 && flag
               : p_455425_.nextInt(40) == 0 && isDeepEnoughToSpawn(p_452918_, p_456238_) && flag;
         } else {
            return true;
         }
      }
   }

   private static boolean isDeepEnoughToSpawn(LevelAccessor p_454021_, BlockPos p_453512_) {
      return p_453512_.getY() < p_454021_.getSeaLevel() - 5;
   }

   @Override
   protected SoundEvent getAmbientSound() {
      return this.isInWater() ? SoundEvents.DROWNED_AMBIENT_WATER : SoundEvents.DROWNED_AMBIENT;
   }

   @Override
   protected SoundEvent getHurtSound(DamageSource p_460674_) {
      return this.isInWater() ? SoundEvents.DROWNED_HURT_WATER : SoundEvents.DROWNED_HURT;
   }

   @Override
   protected SoundEvent getDeathSound() {
      return this.isInWater() ? SoundEvents.DROWNED_DEATH_WATER : SoundEvents.DROWNED_DEATH;
   }

   @Override
   protected SoundEvent getStepSound() {
      return SoundEvents.DROWNED_STEP;
   }

   @Override
   protected SoundEvent getSwimSound() {
      return SoundEvents.DROWNED_SWIM;
   }

   @Override
   protected boolean canSpawnInLiquids() {
      return true;
   }

   @Override
   protected void populateDefaultEquipmentSlots(RandomSource p_455908_, DifficultyInstance p_459614_) {
      if (p_455908_.nextFloat() > 0.9) {
         int i = p_455908_.nextInt(16);
         if (i < 10) {
            this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.TRIDENT));
         } else {
            this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.FISHING_ROD));
         }
      }
   }

   @Override
   protected boolean canReplaceCurrentItem(ItemStack p_453941_, ItemStack p_453436_, EquipmentSlot p_459236_) {
      return p_453436_.is(Items.NAUTILUS_SHELL) ? false : super.canReplaceCurrentItem(p_453941_, p_453436_, p_459236_);
   }

   @Override
   protected boolean convertsInWater() {
      return false;
   }

   @Override
   public boolean checkSpawnObstruction(LevelReader p_459600_) {
      return p_459600_.isUnobstructed(this);
   }

   public boolean okTarget(@Nullable LivingEntity p_456877_) {
      return p_456877_ != null ? !this.level().isBrightOutside() || p_456877_.isInWater() : false;
   }

   @Override
   public boolean isPushedByFluid() {
      return !this.isSwimming();
   }

   boolean wantsToSwim() {
      if (this.searchingForLand) {
         return true;
      }

      LivingEntity livingentity = this.getTarget();
      return livingentity != null && livingentity.isInWater();
   }

   @Override
   protected void travelInWater(Vec3 p_453457_, double p_451610_, boolean p_458225_, double p_459933_) {
      if (this.isUnderWater() && this.wantsToSwim()) {
         this.moveRelative(0.01F, p_453457_);
         this.move(MoverType.SELF, this.getDeltaMovement());
         this.setDeltaMovement(this.getDeltaMovement().scale(0.9));
      } else {
         super.travelInWater(p_453457_, p_451610_, p_458225_, p_459933_);
      }
   }

   @Override
   public void updateSwimming() {
      if (!this.level().isClientSide()) {
         this.setSwimming(this.isEffectiveAi() && this.isUnderWater() && this.wantsToSwim());
      }
   }

   @Override
   public boolean isVisuallySwimming() {
      return this.isSwimming() && !this.isPassenger();
   }

   protected boolean closeToNextPos() {
      Path path = this.getNavigation().getPath();
      if (path != null) {
         BlockPos blockpos = path.getTarget();
         if (blockpos != null) {
            double d0 = this.distanceToSqr(blockpos.getX(), blockpos.getY(), blockpos.getZ());
            if (d0 < 4.0) {
               return true;
            }
         }
      }

      return false;
   }

   @Override
   public void performRangedAttack(LivingEntity p_459849_, float p_451036_) {
      ItemStack itemstack = this.getMainHandItem();
      ItemStack itemstack1 = itemstack.is(Items.TRIDENT) ? itemstack : new ItemStack(Items.TRIDENT);
      ThrownTrident throwntrident = new ThrownTrident(this.level(), this, itemstack1);
      double d0 = p_459849_.getX() - this.getX();
      double d1 = p_459849_.getY(0.3333333333333333) - throwntrident.getY();
      double d2 = p_459849_.getZ() - this.getZ();
      double d3 = Math.sqrt(d0 * d0 + d2 * d2);
      if (this.level() instanceof ServerLevel serverlevel) {
         Projectile.spawnProjectileUsingShoot(
            throwntrident, serverlevel, itemstack1, d0, d1 + d3 * 0.2F, d2, 1.6F, 14 - this.level().getDifficulty().getId() * 4
         );
      }

      this.playSound(SoundEvents.DROWNED_SHOOT, 1.0F, 1.0F / (this.getRandom().nextFloat() * 0.4F + 0.8F));
   }

   @Override
   public TagKey<Item> getPreferredWeaponType() {
      return ItemTags.DROWNED_PREFERRED_WEAPONS;
   }

   public void setSearchingForLand(boolean p_454520_) {
      this.searchingForLand = p_454520_;
   }

   @Override
   public void rideTick() {
      super.rideTick();
      if (this.getControlledVehicle() instanceof PathfinderMob pathfindermob) {
         this.yBodyRot = pathfindermob.yBodyRot;
      }
   }

   @Override
   public boolean wantsToPickUp(ServerLevel p_450619_, ItemStack p_460612_) {
      return p_460612_.is(ItemTags.SPEARS) ? false : super.wantsToPickUp(p_450619_, p_460612_);
   }

   static class DrownedAttackGoal extends ZombieAttackGoal {
      private final Drowned drowned;

      public DrownedAttackGoal(Drowned p_458908_, double p_454034_, boolean p_460968_) {
         super(p_458908_, p_454034_, p_460968_);
         this.drowned = p_458908_;
      }

      @Override
      public boolean canUse() {
         return super.canUse() && this.drowned.okTarget(this.drowned.getTarget());
      }

      @Override
      public boolean canContinueToUse() {
         return super.canContinueToUse() && this.drowned.okTarget(this.drowned.getTarget());
      }
   }

   static class DrownedGoToBeachGoal extends MoveToBlockGoal {
      private final Drowned drowned;

      public DrownedGoToBeachGoal(Drowned p_460897_, double p_456325_) {
         super(p_460897_, p_456325_, 8, 2);
         this.drowned = p_460897_;
      }

      @Override
      public boolean canUse() {
         return super.canUse()
            && !this.drowned.level().isBrightOutside()
            && this.drowned.isInWater()
            && this.drowned.getY() >= this.drowned.level().getSeaLevel() - 3;
      }

      @Override
      public boolean canContinueToUse() {
         return super.canContinueToUse();
      }

      @Override
      protected boolean isValidTarget(LevelReader p_456575_, BlockPos p_458116_) {
         BlockPos blockpos = p_458116_.above();
         return p_456575_.isEmptyBlock(blockpos) && p_456575_.isEmptyBlock(blockpos.above())
            ? p_456575_.getBlockState(p_458116_).entityCanStandOn(p_456575_, p_458116_, this.drowned)
            : false;
      }

      @Override
      public void start() {
         this.drowned.setSearchingForLand(false);
         super.start();
      }

      @Override
      public void stop() {
         super.stop();
      }
   }

   static class DrownedGoToWaterGoal extends Goal {
      private final PathfinderMob mob;
      private double wantedX;
      private double wantedY;
      private double wantedZ;
      private final double speedModifier;
      private final Level level;

      public DrownedGoToWaterGoal(PathfinderMob p_454478_, double p_460118_) {
         this.mob = p_454478_;
         this.speedModifier = p_460118_;
         this.level = p_454478_.level();
         this.setFlags(EnumSet.of(Goal.Flag.MOVE));
      }

      @Override
      public boolean canUse() {
         if (!this.level.isBrightOutside()) {
            return false;
         }

         if (this.mob.isInWater()) {
            return false;
         }

         Vec3 vec3 = this.getWaterPos();
         if (vec3 == null) {
            return false;
         }

         this.wantedX = vec3.x;
         this.wantedY = vec3.y;
         this.wantedZ = vec3.z;
         return true;
      }

      @Override
      public boolean canContinueToUse() {
         return !this.mob.getNavigation().isDone();
      }

      @Override
      public void start() {
         this.mob.getNavigation().moveTo(this.wantedX, this.wantedY, this.wantedZ, this.speedModifier);
      }

      private @Nullable Vec3 getWaterPos() {
         RandomSource randomsource = this.mob.getRandom();
         BlockPos blockpos = this.mob.blockPosition();

         for (int i = 0; i < 10; i++) {
            BlockPos blockpos1 = blockpos.offset(randomsource.nextInt(20) - 10, 2 - randomsource.nextInt(8), randomsource.nextInt(20) - 10);
            if (this.level.getBlockState(blockpos1).is(Blocks.WATER)) {
               return Vec3.atBottomCenterOf(blockpos1);
            }
         }

         return null;
      }
   }

   static class DrownedMoveControl extends MoveControl {
      private final Drowned drowned;

      public DrownedMoveControl(Drowned p_451321_) {
         super(p_451321_);
         this.drowned = p_451321_;
      }

      @Override
      public void tick() {
         LivingEntity livingentity = this.drowned.getTarget();
         if (this.drowned.wantsToSwim() && this.drowned.isInWater()) {
            if (livingentity != null && livingentity.getY() > this.drowned.getY() || this.drowned.searchingForLand) {
               this.drowned.setDeltaMovement(this.drowned.getDeltaMovement().add(0.0, 0.002, 0.0));
            }

            if (this.operation != MoveControl.Operation.MOVE_TO || this.drowned.getNavigation().isDone()) {
               this.drowned.setSpeed(0.0F);
               return;
            }

            double d0 = this.wantedX - this.drowned.getX();
            double d1 = this.wantedY - this.drowned.getY();
            double d2 = this.wantedZ - this.drowned.getZ();
            double d3 = Math.sqrt(d0 * d0 + d1 * d1 + d2 * d2);
            d1 /= d3;
            float f = (float)(Mth.atan2(d2, d0) * 180.0 / (float) Math.PI) - 90.0F;
            this.drowned.setYRot(this.rotlerp(this.drowned.getYRot(), f, 90.0F));
            this.drowned.yBodyRot = this.drowned.getYRot();
            float f1 = (float)(this.speedModifier * this.drowned.getAttributeValue(Attributes.MOVEMENT_SPEED));
            float f2 = Mth.lerp(0.125F, this.drowned.getSpeed(), f1);
            this.drowned.setSpeed(f2);
            this.drowned.setDeltaMovement(this.drowned.getDeltaMovement().add(f2 * d0 * 0.005, f2 * d1 * 0.1, f2 * d2 * 0.005));
         } else {
            if (!this.drowned.onGround()) {
               this.drowned.setDeltaMovement(this.drowned.getDeltaMovement().add(0.0, -0.008, 0.0));
            }

            super.tick();
         }
      }
   }

   static class DrownedSwimUpGoal extends Goal {
      private final Drowned drowned;
      private final double speedModifier;
      private final int seaLevel;
      private boolean stuck;

      public DrownedSwimUpGoal(Drowned p_455117_, double p_459958_, int p_453411_) {
         this.drowned = p_455117_;
         this.speedModifier = p_459958_;
         this.seaLevel = p_453411_;
      }

      @Override
      public boolean canUse() {
         return !this.drowned.level().isBrightOutside() && this.drowned.isInWater() && this.drowned.getY() < this.seaLevel - 2;
      }

      @Override
      public boolean canContinueToUse() {
         return this.canUse() && !this.stuck;
      }

      @Override
      public void tick() {
         if (this.drowned.getY() < this.seaLevel - 1 && (this.drowned.getNavigation().isDone() || this.drowned.closeToNextPos())) {
            Vec3 vec3 = DefaultRandomPos.getPosTowards(
               this.drowned, 4, 8, new Vec3(this.drowned.getX(), this.seaLevel - 1, this.drowned.getZ()), (float) (Math.PI / 2)
            );
            if (vec3 == null) {
               this.stuck = true;
               return;
            }

            this.drowned.getNavigation().moveTo(vec3.x, vec3.y, vec3.z, this.speedModifier);
         }
      }

      @Override
      public void start() {
         this.drowned.setSearchingForLand(true);
         this.stuck = false;
      }

      @Override
      public void stop() {
         this.drowned.setSearchingForLand(false);
      }
   }

   static class DrownedTridentAttackGoal extends RangedAttackGoal {
      private final Drowned drowned;

      public DrownedTridentAttackGoal(RangedAttackMob p_460140_, double p_451978_, int p_450729_, float p_460408_) {
         super(p_460140_, p_451978_, p_450729_, p_460408_);
         this.drowned = (Drowned)p_460140_;
      }

      @Override
      public boolean canUse() {
         return super.canUse() && this.drowned.getMainHandItem().is(Items.TRIDENT);
      }

      @Override
      public void start() {
         super.start();
         this.drowned.setAggressive(true);
         this.drowned.startUsingItem(InteractionHand.MAIN_HAND);
      }

      @Override
      public void stop() {
         super.stop();
         this.drowned.stopUsingItem();
         this.drowned.setAggressive(false);
      }
   }
}
