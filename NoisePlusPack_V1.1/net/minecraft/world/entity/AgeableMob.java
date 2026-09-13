package net.minecraft.world.entity;

import com.google.common.annotations.VisibleForTesting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public abstract class AgeableMob extends PathfinderMob {
   private static final EntityDataAccessor<Boolean> DATA_BABY_ID = SynchedEntityData.defineId(AgeableMob.class, EntityDataSerializers.BOOLEAN);
   public static final int BABY_START_AGE = -24000;
   private static final int FORCED_AGE_PARTICLE_TICKS = 40;
   protected static final int DEFAULT_AGE = 0;
   protected static final int DEFAULT_FORCED_AGE = 0;
   protected int age = 0;
   protected int forcedAge = 0;
   protected int forcedAgeTimer;

   protected AgeableMob(EntityType<? extends AgeableMob> p_146738_, Level p_146739_) {
      super(p_146738_, p_146739_);
   }

   @Override
   public @Nullable SpawnGroupData finalizeSpawn(
      ServerLevelAccessor p_146746_, DifficultyInstance p_146747_, EntitySpawnReason p_366700_, @Nullable SpawnGroupData p_146749_
   ) {
      if (p_146749_ == null) {
         p_146749_ = new AgeableMob.AgeableMobGroupData(true);
      }

      AgeableMob.AgeableMobGroupData ageablemob$ageablemobgroupdata = (AgeableMob.AgeableMobGroupData)p_146749_;
      if (ageablemob$ageablemobgroupdata.isShouldSpawnBaby()
         && ageablemob$ageablemobgroupdata.getGroupSize() > 0
         && p_146746_.getRandom().nextFloat() <= ageablemob$ageablemobgroupdata.getBabySpawnChance()) {
         this.setAge(-24000);
      }

      ageablemob$ageablemobgroupdata.increaseGroupSizeByOne();
      return super.finalizeSpawn(p_146746_, p_146747_, p_366700_, p_146749_);
   }

   public abstract @Nullable AgeableMob getBreedOffspring(ServerLevel var1, AgeableMob var2);

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder p_333447_) {
      super.defineSynchedData(p_333447_);
      p_333447_.define(DATA_BABY_ID, false);
   }

   public boolean canBreed() {
      return false;
   }

   public int getAge() {
      if (this.level().isClientSide()) {
         return this.entityData.get(DATA_BABY_ID) ? -1 : 1;
      } else {
         return this.age;
      }
   }

   public void ageUp(int p_146741_, boolean p_146742_) {
      int i = this.getAge();
      i += p_146741_ * 20;
      if (i > 0) {
         i = 0;
      }

      int j = i - i;
      this.setAge(i);
      if (p_146742_) {
         this.forcedAge += j;
         if (this.forcedAgeTimer == 0) {
            this.forcedAgeTimer = 40;
         }
      }

      if (this.getAge() == 0) {
         this.setAge(this.forcedAge);
      }
   }

   public void ageUp(int p_146759_) {
      this.ageUp(p_146759_, false);
   }

   public void setAge(int p_146763_) {
      int i = this.getAge();
      this.age = p_146763_;
      if (i < 0 && p_146763_ >= 0 || i >= 0 && p_146763_ < 0) {
         this.entityData.set(DATA_BABY_ID, p_146763_ < 0);
         this.ageBoundaryReached();
      }
   }

   @Override
   protected void addAdditionalSaveData(ValueOutput p_407948_) {
      super.addAdditionalSaveData(p_407948_);
      p_407948_.putInt("Age", this.getAge());
      p_407948_.putInt("ForcedAge", this.forcedAge);
   }

   @Override
   protected void readAdditionalSaveData(ValueInput p_409007_) {
      super.readAdditionalSaveData(p_409007_);
      this.setAge(p_409007_.getIntOr("Age", 0));
      this.forcedAge = p_409007_.getIntOr("ForcedAge", 0);
   }

   @Override
   public void onSyncedDataUpdated(EntityDataAccessor<?> p_146754_) {
      if (DATA_BABY_ID.equals(p_146754_)) {
         this.refreshDimensions();
      }

      super.onSyncedDataUpdated(p_146754_);
   }

   @Override
   public void aiStep() {
      super.aiStep();
      if (this.level().isClientSide()) {
         if (this.forcedAgeTimer > 0) {
            if (this.forcedAgeTimer % 4 == 0) {
               this.level().addParticle(ParticleTypes.HAPPY_VILLAGER, this.getRandomX(1.0), this.getRandomY() + 0.5, this.getRandomZ(1.0), 0.0, 0.0, 0.0);
            }

            this.forcedAgeTimer--;
         }
      } else if (this.isAlive()) {
         int i = this.getAge();
         if (i < 0) {
            this.setAge(++i);
         } else if (i > 0) {
            this.setAge(--i);
         }
      }
   }

   protected void ageBoundaryReached() {
      if (!this.isBaby() && this.isPassenger() && this.getVehicle() instanceof AbstractBoat abstractboat && !abstractboat.hasEnoughSpaceFor(this)) {
         this.stopRiding();
      }
   }

   @Override
   public boolean isBaby() {
      return this.getAge() < 0;
   }

   @Override
   public void setBaby(boolean p_146756_) {
      this.setAge(p_146756_ ? -24000 : 0);
   }

   public static int getSpeedUpSecondsWhenFeeding(int p_216968_) {
      return (int)(p_216968_ / 20 * 0.1F);
   }

   @VisibleForTesting
   public int getForcedAge() {
      return this.forcedAge;
   }

   @VisibleForTesting
   public int getForcedAgeTimer() {
      return this.forcedAgeTimer;
   }

   public static class AgeableMobGroupData implements SpawnGroupData {
      private int groupSize;
      private final boolean shouldSpawnBaby;
      private final float babySpawnChance;

      public AgeableMobGroupData(boolean p_146775_, float p_146776_) {
         this.shouldSpawnBaby = p_146775_;
         this.babySpawnChance = p_146776_;
      }

      public AgeableMobGroupData(boolean p_146773_) {
         this(p_146773_, 0.05F);
      }

      public AgeableMobGroupData(float p_146771_) {
         this(true, p_146771_);
      }

      public int getGroupSize() {
         return this.groupSize;
      }

      public void increaseGroupSizeByOne() {
         this.groupSize++;
      }

      public boolean isShouldSpawnBaby() {
         return this.shouldSpawnBaby;
      }

      public float getBabySpawnChance() {
         return this.babySpawnChance;
      }
   }
}
