package net.minecraft.world.entity.animal.nautilus;

import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityAttachment;
import net.minecraft.world.entity.EntityAttachments;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

public class Nautilus extends AbstractNautilus {
   private static final int NAUTILUS_TOTAL_AIR_SUPPLY = 300;
   private static final EntityDimensions BABY_DIMENSIONS = EntityTypes.NAUTILUS
      .getDimensions()
      .scale(0.5F)
      .withAttachments(EntityAttachments.builder().attach(EntityAttachment.PASSENGER, 0.0F, 0.5F, 0.0F));
   private static final Brain.Provider<Nautilus> BRAIN_PROVIDER = Brain.provider(
      List.of(MemoryModuleType.ANGRY_AT, MemoryModuleType.ATTACK_TARGET_COOLDOWN),
      List.of(SensorType.NEAREST_LIVING_ENTITIES, SensorType.NEAREST_ADULT, SensorType.NEAREST_PLAYERS, SensorType.HURT_BY, SensorType.NAUTILUS_TEMPTATIONS),
      var0 -> NautilusAi.getActivities()
   );

   public Nautilus(final EntityType<? extends Nautilus> type, final Level level) {
      super(type, level);
   }

   @Override
   protected Brain<Nautilus> makeBrain(final Brain.Packed packedBrain) {
      return BRAIN_PROVIDER.makeBrain(this, packedBrain);
   }

   @Override
   public Brain<Nautilus> getBrain() {
      return super.getBrain();
   }

   public @Nullable Nautilus getBreedOffspring(final ServerLevel level, final AgeableMob partner) {
      Nautilus baby = EntityTypes.NAUTILUS.create(level, EntitySpawnReason.BREEDING);
      if (baby != null && this.isTame()) {
         baby.setOwnerReference(this.getOwnerReference());
         baby.setTame(true, true);
      }

      return baby;
   }

   @Override
   public EntityDimensions getDefaultDimensions(final Pose pose) {
      return this.isBaby() ? BABY_DIMENSIONS : super.getDefaultDimensions(pose);
   }

   @Override
   protected void customServerAiStep(final ServerLevel level) {
      ProfilerFiller profiler = Profiler.get();
      profiler.push("nautilusBrain");
      this.getBrain().tick(level, this);
      profiler.pop();
      profiler.push("nautilusActivityUpdate");
      NautilusAi.updateActivity(this);
      profiler.pop();
      super.customServerAiStep(level);
   }

   @Override
   protected SoundEvent getAmbientSound() {
      if (this.isBaby()) {
         return this.isUnderWater() ? SoundEvents.BABY_NAUTILUS_AMBIENT : SoundEvents.BABY_NAUTILUS_AMBIENT_ON_LAND;
      } else {
         return this.isUnderWater() ? SoundEvents.NAUTILUS_AMBIENT : SoundEvents.NAUTILUS_AMBIENT_ON_LAND;
      }
   }

   @Override
   protected SoundEvent getHurtSound(final DamageSource source) {
      if (this.isBaby()) {
         return this.isUnderWater() ? SoundEvents.BABY_NAUTILUS_HURT : SoundEvents.BABY_NAUTILUS_HURT_ON_LAND;
      } else {
         return this.isUnderWater() ? SoundEvents.NAUTILUS_HURT : SoundEvents.NAUTILUS_HURT_ON_LAND;
      }
   }

   @Override
   protected SoundEvent getDeathSound() {
      if (this.isBaby()) {
         return this.isUnderWater() ? SoundEvents.BABY_NAUTILUS_DEATH : SoundEvents.BABY_NAUTILUS_DEATH_ON_LAND;
      } else {
         return this.isUnderWater() ? SoundEvents.NAUTILUS_DEATH : SoundEvents.NAUTILUS_DEATH_ON_LAND;
      }
   }

   @Override
   protected SoundEvent getDashSound() {
      return this.isUnderWater() ? SoundEvents.NAUTILUS_DASH : SoundEvents.NAUTILUS_DASH_ON_LAND;
   }

   @Override
   protected SoundEvent getDashReadySound() {
      return this.isUnderWater() ? SoundEvents.NAUTILUS_DASH_READY : SoundEvents.NAUTILUS_DASH_READY_ON_LAND;
   }

   @Override
   protected void playEatingSound() {
      SoundEvent nautilusEatSound = this.isBaby() ? SoundEvents.BABY_NAUTILUS_EAT : SoundEvents.NAUTILUS_EAT;
      this.makeSound(nautilusEatSound);
   }

   @Override
   protected SoundEvent getSwimSound() {
      return this.isBaby() ? SoundEvents.BABY_NAUTILUS_SWIM : SoundEvents.NAUTILUS_SWIM;
   }

   @Override
   public int getMaxAirSupply() {
      return 300;
   }

   protected void handleAirSupply(final ServerLevel level, final int preTickAirSupply) {
      if (this.isAlive() && !this.isInWater()) {
         this.setAirSupply(preTickAirSupply - 1);
         if (this.getAirSupply() <= -20) {
            this.setAirSupply(0);
            this.hurtServer(level, this.damageSources().dryOut(), 2.0F);
         }
      } else {
         this.setAirSupply(300);
      }
   }

   @Override
   public void baseTick() {
      int airSupply = this.getAirSupply();
      super.baseTick();
      if (!this.isNoAi() && this.level() instanceof ServerLevel serverLevel) {
         this.handleAirSupply(serverLevel, airSupply);
      }
   }

   @Override
   public boolean canBeLeashed() {
      return !this.isAggravated();
   }
}
