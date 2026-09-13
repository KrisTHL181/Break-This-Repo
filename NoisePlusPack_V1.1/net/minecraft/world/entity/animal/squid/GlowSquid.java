package net.minecraft.world.entity.animal.squid;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class GlowSquid extends Squid {
   private static final EntityDataAccessor<Integer> DATA_DARK_TICKS_REMAINING = SynchedEntityData.defineId(GlowSquid.class, EntityDataSerializers.INT);
   private static final int DEFAULT_DARK_TICKS_REMAINING = 0;

   public GlowSquid(EntityType<? extends GlowSquid> p_459164_, Level p_460840_) {
      super(p_459164_, p_460840_);
   }

   @Override
   protected ParticleOptions getInkParticle() {
      return ParticleTypes.GLOW_SQUID_INK;
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder p_458926_) {
      super.defineSynchedData(p_458926_);
      p_458926_.define(DATA_DARK_TICKS_REMAINING, 0);
   }

   @Override
   public @Nullable AgeableMob getBreedOffspring(ServerLevel p_457193_, AgeableMob p_450861_) {
      return EntityType.GLOW_SQUID.create(p_457193_, EntitySpawnReason.BREEDING);
   }

   @Override
   protected SoundEvent getSquirtSound() {
      return SoundEvents.GLOW_SQUID_SQUIRT;
   }

   @Override
   protected SoundEvent getAmbientSound() {
      return SoundEvents.GLOW_SQUID_AMBIENT;
   }

   @Override
   protected SoundEvent getHurtSound(DamageSource p_457781_) {
      return SoundEvents.GLOW_SQUID_HURT;
   }

   @Override
   protected SoundEvent getDeathSound() {
      return SoundEvents.GLOW_SQUID_DEATH;
   }

   @Override
   protected void addAdditionalSaveData(ValueOutput p_452979_) {
      super.addAdditionalSaveData(p_452979_);
      p_452979_.putInt("DarkTicksRemaining", this.getDarkTicksRemaining());
   }

   @Override
   protected void readAdditionalSaveData(ValueInput p_458696_) {
      super.readAdditionalSaveData(p_458696_);
      this.setDarkTicks(p_458696_.getIntOr("DarkTicksRemaining", 0));
   }

   @Override
   public void aiStep() {
      super.aiStep();
      int i = this.getDarkTicksRemaining();
      if (i > 0) {
         this.setDarkTicks(i - 1);
      }

      this.level().addParticle(ParticleTypes.GLOW, this.getRandomX(0.6), this.getRandomY(), this.getRandomZ(0.6), 0.0, 0.0, 0.0);
   }

   @Override
   public boolean hurtServer(ServerLevel p_450234_, DamageSource p_454030_, float p_460875_) {
      boolean flag = super.hurtServer(p_450234_, p_454030_, p_460875_);
      if (flag) {
         this.setDarkTicks(100);
      }

      return flag;
   }

   private void setDarkTicks(int p_453236_) {
      this.entityData.set(DATA_DARK_TICKS_REMAINING, p_453236_);
   }

   public int getDarkTicksRemaining() {
      return this.entityData.get(DATA_DARK_TICKS_REMAINING);
   }

   public static boolean checkGlowSquidSpawnRules(
      EntityType<? extends LivingEntity> p_458336_, ServerLevelAccessor p_457618_, EntitySpawnReason p_456677_, BlockPos p_455023_, RandomSource p_457597_
   ) {
      return p_455023_.getY() <= p_457618_.getSeaLevel() - 33
         && p_457618_.getRawBrightness(p_455023_, 0) == 0
         && p_457618_.getBlockState(p_455023_).is(Blocks.WATER);
   }
}
