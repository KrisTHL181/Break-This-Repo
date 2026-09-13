package net.minecraft.world.entity.monster;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.Vec3;

public class MagmaCube extends Slime {
   public MagmaCube(EntityType<? extends MagmaCube> p_32968_, Level p_32969_) {
      super(p_32968_, p_32969_);
   }

   public static AttributeSupplier.Builder createAttributes() {
      return Monster.createMonsterAttributes().add(Attributes.MOVEMENT_SPEED, 0.2F);
   }

   public static boolean checkMagmaCubeSpawnRules(
      EntityType<MagmaCube> p_219003_, LevelAccessor p_219004_, EntitySpawnReason p_363910_, BlockPos p_219006_, RandomSource p_219007_
   ) {
      return p_219004_.getDifficulty() != Difficulty.PEACEFUL;
   }

   @Override
   public void setSize(int p_32972_, boolean p_32973_) {
      super.setSize(p_32972_, p_32973_);
      this.getAttribute(Attributes.ARMOR).setBaseValue(p_32972_ * 3);
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
      Vec3 vec3 = this.getDeltaMovement();
      float f = this.getSize() * 0.1F;
      this.setDeltaMovement(vec3.x, this.getJumpPower() + f, vec3.z);
      this.needsSync = true;
   }

   @Override
   protected void jumpInLiquid(TagKey<Fluid> p_204065_) {
      if (p_204065_ == FluidTags.LAVA) {
         Vec3 vec3 = this.getDeltaMovement();
         this.setDeltaMovement(vec3.x, 0.22F + this.getSize() * 0.05F, vec3.z);
         this.needsSync = true;
      } else {
         super.jumpInLiquid(p_204065_);
      }
   }

   @Override
   protected boolean isDealsDamage() {
      return this.isEffectiveAi();
   }

   @Override
   protected float getAttackDamage() {
      return super.getAttackDamage() + 2.0F;
   }

   @Override
   protected SoundEvent getHurtSound(DamageSource p_32992_) {
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
}
