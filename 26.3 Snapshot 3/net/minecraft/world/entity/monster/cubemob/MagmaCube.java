package net.minecraft.world.entity.monster.cubemob;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class MagmaCube extends AbstractCubeMob implements Enemy {
   public MagmaCube(final EntityType<? extends MagmaCube> type, final Level level) {
      super(type, level);
   }

   @Override
   protected void addBehaviourGoals() {
      this.goalSelector.addGoal(2, new AbstractCubeMob.CubeMobAttackGoal(this));
   }

   @Override
   protected void addTargetingGoals() {
      this.targetSelector
         .addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false, (target, level) -> Math.abs(target.getY() - this.getY()) <= 4.0));
      this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, IronGolem.class, true));
   }

   public static AttributeSupplier.Builder createAttributes() {
      return Monster.createMonsterAttributes().add(Attributes.MOVEMENT_SPEED, 0.2F);
   }

   public static boolean checkMagmaCubeSpawnRules(
      final EntityType<MagmaCube> type, final LevelAccessor level, final EntitySpawnReason spawnReason, final BlockPos pos, final RandomSource random
   ) {
      return level.getDifficulty() != Difficulty.PEACEFUL;
   }

   @Override
   public void setSize(final int size, final boolean updateHealth) {
      super.setSize(size, updateHealth);
      int actualSize = this.entityData.get(ID_SIZE);
      this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(actualSize);
      this.getAttribute(Attributes.ARMOR).setBaseValue(size * 3);
      this.xpReward = actualSize;
   }

   @Override
   public float getLightLevelDependentMagicValue() {
      return 1.0F;
   }

   @Override
   protected ParticleOptions getParticleType() {
      return ParticleTypes.FLAME;
   }

   @Override
   public boolean isOnFire() {
      return false;
   }

   @Override
   protected int getJumpDelay() {
      return super.getJumpDelay() * 4;
   }

   @Override
   protected void decreaseSquish() {
      this.targetSquish *= 0.9F;
   }

   @Override
   public void jumpFromGround() {
      Vec3 movement = this.getDeltaMovement();
      float sizeJumpBoostPower = this.getSize() * 0.1F;
      this.setDeltaMovement(movement.x, this.getJumpPower() + sizeJumpBoostPower, movement.z);
      this.needsSync = true;
   }

   @Override
   protected void jumpInLiquid(final TagKey<Fluid> type) {
      if (type == FluidTags.LAVA) {
         Vec3 movement = this.getDeltaMovement();
         this.setDeltaMovement(movement.x, 0.22F + this.getSize() * 0.05F, movement.z);
         this.needsSync = true;
      } else {
         super.jumpInLiquid(type);
      }
   }

   @Override
   protected boolean canDealDamage() {
      return this.isEffectiveAi();
   }

   @Override
   protected float getAttackDamage() {
      return super.getAttackDamage() + 2.0F;
   }

   @Override
   protected SoundEvent getHurtSound(final DamageSource source) {
      return this.isTiny() ? SoundEvents.MAGMA_CUBE_HURT_SMALL : SoundEvents.MAGMA_CUBE_HURT;
   }

   @Override
   protected SoundEvent getDeathSound() {
      return this.isTiny() ? SoundEvents.MAGMA_CUBE_DEATH_SMALL : SoundEvents.MAGMA_CUBE_DEATH;
   }

   @Override
   protected SoundEvent getSquishSound() {
      return this.isTiny() ? SoundEvents.MAGMA_CUBE_SQUISH_SMALL : SoundEvents.MAGMA_CUBE_SQUISH;
   }

   @Override
   protected SoundEvent getJumpSound() {
      return SoundEvents.MAGMA_CUBE_JUMP;
   }

   @Override
   protected boolean canBeABaby() {
      return false;
   }

   @Override
   public @Nullable SpawnGroupData finalizeSpawn(
      final ServerLevelAccessor level, final DifficultyInstance difficulty, final EntitySpawnReason spawnReason, @Nullable SpawnGroupData groupData
   ) {
      if (groupData == null) {
         groupData = new AgeableMob.AgeableMobGroupData(false);
      }

      return super.finalizeSpawn(level, difficulty, spawnReason, groupData);
   }
}
