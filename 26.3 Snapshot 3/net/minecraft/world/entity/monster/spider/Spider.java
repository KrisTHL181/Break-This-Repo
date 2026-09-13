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
import net.minecraft.world.entity.EntityTypes;
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

   public Spider(final EntityType<? extends Spider> type, final Level level) {
      super(type, level);
   }

   @Override
   protected void registerGoals() {
      this.goalSelector.addGoal(1, new FloatGoal(this));
      this.goalSelector.addGoal(2, new AvoidEntityGoal<>(this, Armadillo.class, 6.0F, 1.0, 1.2, entity -> !((Armadillo)entity).isScared()));
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
   protected PathNavigation createNavigation(final Level level) {
      return new WallClimberNavigation(this, level);
   }

   @Override
   protected void defineSynchedData(final SynchedEntityData.Builder entityData) {
      super.defineSynchedData(entityData);
      entityData.define(DATA_FLAGS_ID, (byte)0);
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
   protected SoundEvent getHurtSound(final DamageSource source) {
      return SoundEvents.SPIDER_HURT;
   }

   @Override
   protected SoundEvent getDeathSound() {
      return SoundEvents.SPIDER_DEATH;
   }

   @Override
   protected void playStepSound(final BlockPos pos, final BlockState blockState) {
      this.playSound(SoundEvents.SPIDER_STEP, 0.15F, 1.0F);
   }

   @Override
   public boolean onClimbable() {
      return this.isClimbing();
   }

   @Override
   public void makeStuckInBlock(final BlockState state, final Vec3 speedMultiplier) {
      if (!state.is(Blocks.COBWEB)) {
         super.makeStuckInBlock(state, speedMultiplier);
      }
   }

   @Override
   public boolean canBeAffected(final MobEffectInstance newEffect) {
      return newEffect.is(MobEffects.POISON) ? false : super.canBeAffected(newEffect);
   }

   public boolean isClimbing() {
      return (this.entityData.get(DATA_FLAGS_ID) & 1) != 0;
   }

   public void setClimbing(final boolean value) {
      byte flags = this.entityData.get(DATA_FLAGS_ID);
      if (value) {
         flags = (byte)(flags | 1);
      } else {
         flags = (byte)(flags & -2);
      }

      this.entityData.set(DATA_FLAGS_ID, flags);
   }

   @Override
   public @Nullable SpawnGroupData finalizeSpawn(
      final ServerLevelAccessor level, final DifficultyInstance difficulty, final EntitySpawnReason spawnReason, @Nullable SpawnGroupData groupData
   ) {
      groupData = super.finalizeSpawn(level, difficulty, spawnReason, groupData);
      RandomSource random = level.getRandom();
      if (random.nextInt(100) == 0) {
         Skeleton skeleton = EntityTypes.SKELETON.create(this.level(), EntitySpawnReason.JOCKEY);
         if (skeleton != null) {
            skeleton.snapTo(this.getX(), this.getY(), this.getZ(), this.getYRot(), 0.0F);
            skeleton.finalizeSpawn(level, difficulty, spawnReason, null);
            skeleton.startRiding(this, false, false);
         }
      }

      if (groupData == null) {
         groupData = new Spider.SpiderEffectsGroupData();
         if (level.getDifficulty() == Difficulty.HARD && random.nextFloat() < 0.1F * difficulty.getSpecialMultiplier()) {
            ((Spider.SpiderEffectsGroupData)groupData).setRandomEffect(random);
         }
      }

      if (groupData instanceof Spider.SpiderEffectsGroupData spiderEffectsGroupData) {
         Holder<MobEffect> effect = spiderEffectsGroupData.effect;
         if (effect != null) {
            this.addEffect(new MobEffectInstance(effect, -1));
         }
      }

      return groupData;
   }

   @Override
   public Vec3 getVehicleAttachmentPoint(final Entity vehicle) {
      return vehicle.getBbWidth() <= this.getBbWidth() ? new Vec3(0.0, 0.3125 * this.getScale(), 0.0) : super.getVehicleAttachmentPoint(vehicle);
   }

   private static class SpiderAttackGoal extends MeleeAttackGoal {
      public SpiderAttackGoal(final Spider mob) {
         super(mob, 1.0, true);
      }

      @Override
      public boolean canUse() {
         return super.canUse() && !this.mob.isVehicle();
      }

      @Override
      public boolean canContinueToUse() {
         float br = this.mob.getLightLevelDependentMagicValue();
         if (br >= 0.5F && this.mob.getRandom().nextInt(100) == 0) {
            this.mob.setTarget(null);
            return false;
         } else {
            return super.canContinueToUse();
         }
      }
   }

   public static class SpiderEffectsGroupData implements SpawnGroupData {
      public @Nullable Holder<MobEffect> effect;

      public void setRandomEffect(final RandomSource random) {
         int selection = random.nextInt(5);
         if (selection <= 1) {
            this.effect = MobEffects.SPEED;
         } else if (selection <= 2) {
            this.effect = MobEffects.STRENGTH;
         } else if (selection <= 3) {
            this.effect = MobEffects.REGENERATION;
         } else if (selection <= 4) {
            this.effect = MobEffects.INVISIBILITY;
         }
      }
   }

   private static class SpiderTargetGoal<T extends LivingEntity> extends NearestAttackableTargetGoal<T> {
      public SpiderTargetGoal(final Spider mob, final Class<T> targetType) {
         super(mob, targetType, true);
      }

      @Override
      public boolean canUse() {
         float br = this.mob.getLightLevelDependentMagicValue();
         return br >= 0.5F ? false : super.canUse();
      }
   }
}
