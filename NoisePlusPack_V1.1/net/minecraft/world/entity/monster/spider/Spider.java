package net.minecraft.world.entity.monster.spider;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.navigation.WallClimberNavigation;
import net.minecraft.world.entity.animal.armadillo.Armadillo;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.skeleton.Skeleton;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class Spider extends Monster {
   private static final EntityDataAccessor<Byte> DATA_FLAGS_ID = SynchedEntityData.defineId(Spider.class, EntityDataSerializers.BYTE);
   private static final float SPIDER_SPECIAL_EFFECT_CHANCE = 0.1F;

   public Spider(EntityType<? extends Spider> p_454304_, Level p_451888_) {
      super(p_454304_, p_451888_);
   }

   @Override
   protected void registerGoals() {
      this.goalSelector.addGoal(1, new FloatGoal(this));
      this.goalSelector.addGoal(2, new AvoidEntityGoal<>(this, Armadillo.class, 6.0F, 1.0, 1.2, p_459016_ -> !((Armadillo)p_459016_).isScared()));
      this.goalSelector.addGoal(3, new LeapAtTargetGoal(this, 0.4F));
      this.goalSelector.addGoal(4, new Spider.SpiderAttackGoal(this));
      this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
      this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
      this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));
      this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
      this.targetSelector.addGoal(2, new Spider.SpiderTargetGoal<>(this, Player.class));
      this.targetSelector.addGoal(3, new Spider.SpiderTargetGoal<>(this, IronGolem.class));
   }

   @Override
   protected PathNavigation createNavigation(Level p_460282_) {
      return new WallClimberNavigation(this, p_460282_);
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder p_458943_) {
      super.defineSynchedData(p_458943_);
      p_458943_.define(DATA_FLAGS_ID, (byte)0);
   }

   @Override
   public void tick() {
      super.tick();
      if (!this.level().isClientSide()) {
         this.setClimbing(this.horizontalCollision);
      }
   }

   public static AttributeSupplier.Builder createAttributes() {
      return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 16.0).add(Attributes.MOVEMENT_SPEED, 0.3F);
   }

   @Override
   protected SoundEvent getAmbientSound() {
      return SoundEvents.SPIDER_AMBIENT;
   }

   @Override
   protected SoundEvent getHurtSound(DamageSource p_456758_) {
      return SoundEvents.SPIDER_HURT;
   }

   @Override
   protected SoundEvent getDeathSound() {
      return SoundEvents.SPIDER_DEATH;
   }

   @Override
   protected void playStepSound(BlockPos p_455758_, BlockState p_452783_) {
      this.playSound(SoundEvents.SPIDER_STEP, 0.15F, 1.0F);
   }

   @Override
   public boolean onClimbable() {
      return this.isClimbing();
   }

   @Override
   public void makeStuckInBlock(BlockState p_458290_, Vec3 p_456247_) {
      if (!p_458290_.is(Blocks.COBWEB)) {
         super.makeStuckInBlock(p_458290_, p_456247_);
      }
   }

   @Override
   public boolean canBeAffected(MobEffectInstance p_454269_) {
      return p_454269_.is(MobEffects.POISON) ? false : super.canBeAffected(p_454269_);
   }

   public boolean isClimbing() {
      return (this.entityData.get(DATA_FLAGS_ID) & 1) != 0;
   }

   public void setClimbing(boolean p_460510_) {
      byte b0 = this.entityData.get(DATA_FLAGS_ID);
      if (p_460510_) {
         b0 = (byte)(b0 | 1);
      } else {
         b0 = (byte)(b0 & -2);
      }

      this.entityData.set(DATA_FLAGS_ID, b0);
   }

   @Override
   public @Nullable SpawnGroupData finalizeSpawn(
      ServerLevelAccessor p_457601_, DifficultyInstance p_458726_, EntitySpawnReason p_454152_, @Nullable SpawnGroupData p_452876_
   ) {
      p_452876_ = super.finalizeSpawn(p_457601_, p_458726_, p_454152_, p_452876_);
      RandomSource randomsource = p_457601_.getRandom();
      if (randomsource.nextInt(100) == 0) {
         Skeleton skeleton = EntityType.SKELETON.create(this.level(), EntitySpawnReason.JOCKEY);
         if (skeleton != null) {
            skeleton.snapTo(this.getX(), this.getY(), this.getZ(), this.getYRot(), 0.0F);
            skeleton.finalizeSpawn(p_457601_, p_458726_, p_454152_, null);
            skeleton.startRiding(this, false, false);
         }
      }

      if (p_452876_ == null) {
         p_452876_ = new Spider.SpiderEffectsGroupData();
         if (p_457601_.getDifficulty() == Difficulty.HARD && randomsource.nextFloat() < 0.1F * p_458726_.getSpecialMultiplier()) {
            ((Spider.SpiderEffectsGroupData)p_452876_).setRandomEffect(randomsource);
         }
      }

      if (p_452876_ instanceof Spider.SpiderEffectsGroupData spider$spidereffectsgroupdata) {
         Holder<MobEffect> holder = spider$spidereffectsgroupdata.effect;
         if (holder != null) {
            this.addEffect(new MobEffectInstance(holder, -1));
         }
      }

      return p_452876_;
   }

   @Override
   public Vec3 getVehicleAttachmentPoint(Entity p_459990_) {
      return p_459990_.getBbWidth() <= this.getBbWidth() ? new Vec3(0.0, 0.3125 * this.getScale(), 0.0) : super.getVehicleAttachmentPoint(p_459990_);
   }

   static class SpiderAttackGoal extends MeleeAttackGoal {
      public SpiderAttackGoal(Spider p_454540_) {
         super(p_454540_, 1.0, true);
      }

      @Override
      public boolean canUse() {
         return super.canUse() && !this.mob.isVehicle();
      }

      @Override
      public boolean canContinueToUse() {
         float f = this.mob.getLightLevelDependentMagicValue();
         if (f >= 0.5F && this.mob.getRandom().nextInt(100) == 0) {
            this.mob.setTarget(null);
            return false;
         } else {
            return super.canContinueToUse();
         }
      }
   }

   public static class SpiderEffectsGroupData implements SpawnGroupData {
      public @Nullable Holder<MobEffect> effect;

      public void setRandomEffect(RandomSource p_457259_) {
         int i = p_457259_.nextInt(5);
         if (i <= 1) {
            this.effect = MobEffects.SPEED;
         } else if (i <= 2) {
            this.effect = MobEffects.STRENGTH;
         } else if (i <= 3) {
            this.effect = MobEffects.REGENERATION;
         } else if (i <= 4) {
            this.effect = MobEffects.INVISIBILITY;
         }
      }
   }

   static class SpiderTargetGoal<T extends LivingEntity> extends NearestAttackableTargetGoal<T> {
      public SpiderTargetGoal(Spider p_452591_, Class<T> p_461018_) {
         super(p_452591_, p_461018_, true);
      }

      @Override
      public boolean canUse() {
         float f = this.mob.getLightLevelDependentMagicValue();
         return f >= 0.5F ? false : super.canUse();
      }
   }
}
