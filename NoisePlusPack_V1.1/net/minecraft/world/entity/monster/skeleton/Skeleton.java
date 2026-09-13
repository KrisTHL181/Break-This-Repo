package net.minecraft.world.entity.monster.skeleton;

import com.google.common.annotations.VisibleForTesting;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.ConversionParams;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class Skeleton extends AbstractSkeleton {
   private static final int TOTAL_CONVERSION_TIME = 300;
   private static final EntityDataAccessor<Boolean> DATA_STRAY_CONVERSION_ID = SynchedEntityData.defineId(Skeleton.class, EntityDataSerializers.BOOLEAN);
   public static final String CONVERSION_TAG = "StrayConversionTime";
   private static final int NOT_CONVERTING = -1;
   private int inPowderSnowTime;
   private int conversionTime;

   public Skeleton(EntityType<? extends Skeleton> p_459359_, Level p_454273_) {
      super(p_459359_, p_454273_);
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder p_460991_) {
      super.defineSynchedData(p_460991_);
      p_460991_.define(DATA_STRAY_CONVERSION_ID, false);
   }

   public boolean isFreezeConverting() {
      return this.getEntityData().get(DATA_STRAY_CONVERSION_ID);
   }

   public void setFreezeConverting(boolean p_453160_) {
      this.entityData.set(DATA_STRAY_CONVERSION_ID, p_453160_);
   }

   @Override
   public boolean isShaking() {
      return this.isFreezeConverting();
   }

   @Override
   public void tick() {
      if (!this.level().isClientSide() && this.isAlive() && !this.isNoAi()) {
         if (this.isInPowderSnow) {
            if (this.isFreezeConverting()) {
               this.conversionTime--;
               if (this.conversionTime < 0) {
                  this.doFreezeConversion();
               }
            } else {
               this.inPowderSnowTime++;
               if (this.inPowderSnowTime >= 140) {
                  this.startFreezeConversion(300);
               }
            }
         } else {
            this.inPowderSnowTime = -1;
            this.setFreezeConverting(false);
         }
      }

      super.tick();
   }

   @Override
   protected void addAdditionalSaveData(ValueOutput p_457724_) {
      super.addAdditionalSaveData(p_457724_);
      p_457724_.putInt("StrayConversionTime", this.isFreezeConverting() ? this.conversionTime : -1);
   }

   @Override
   protected void readAdditionalSaveData(ValueInput p_455788_) {
      super.readAdditionalSaveData(p_455788_);
      int i = p_455788_.getIntOr("StrayConversionTime", -1);
      if (i != -1) {
         this.startFreezeConversion(i);
      } else {
         this.setFreezeConverting(false);
      }
   }

   @VisibleForTesting
   public void startFreezeConversion(int p_457077_) {
      this.conversionTime = p_457077_;
      this.setFreezeConverting(true);
   }

   protected void doFreezeConversion() {
      this.convertTo(EntityType.STRAY, ConversionParams.single(this, true, true), p_451505_ -> {
         if (!this.isSilent()) {
            this.level().levelEvent(null, 1048, this.blockPosition(), 0);
         }
      });
   }

   @Override
   public boolean canFreeze() {
      return false;
   }

   @Override
   protected SoundEvent getAmbientSound() {
      return SoundEvents.SKELETON_AMBIENT;
   }

   @Override
   protected SoundEvent getHurtSound(DamageSource p_450765_) {
      return SoundEvents.SKELETON_HURT;
   }

   @Override
   protected SoundEvent getDeathSound() {
      return SoundEvents.SKELETON_DEATH;
   }

   @Override
   SoundEvent getStepSound() {
      return SoundEvents.SKELETON_STEP;
   }
}
