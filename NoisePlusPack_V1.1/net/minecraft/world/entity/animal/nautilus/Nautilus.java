package net.minecraft.world.entity.animal.nautilus;

import com.mojang.serialization.Dynamic;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

public class Nautilus extends AbstractNautilus {
   private static final int NAUTILUS_TOTAL_AIR_SUPPLY = 300;

   public Nautilus(EntityType<? extends Nautilus> p_450413_, Level p_453580_) {
      super(p_450413_, p_453580_);
   }

   @Override
   protected Brain.Provider<Nautilus> brainProvider() {
      return NautilusAi.brainProvider();
   }

   @Override
   protected Brain<?> makeBrain(Dynamic<?> p_453780_) {
      return NautilusAi.makeBrain(this.brainProvider().makeBrain(p_453780_));
   }

   @Override
   public Brain<Nautilus> getBrain() {
      return (Brain<Nautilus>)super.getBrain();
   }

   public @Nullable Nautilus getBreedOffspring(ServerLevel p_458366_, AgeableMob p_460184_) {
      Nautilus nautilus = EntityType.NAUTILUS.create(p_458366_, EntitySpawnReason.BREEDING);
      if (nautilus != null && this.isTame()) {
         nautilus.setOwnerReference(this.getOwnerReference());
         nautilus.setTame(true, true);
      }

      return nautilus;
   }

   @Override
   protected void customServerAiStep(ServerLevel p_459285_) {
      ProfilerFiller profilerfiller = Profiler.get();
      profilerfiller.push("nautilusBrain");
      this.getBrain().tick(p_459285_, this);
      profilerfiller.pop();
      profilerfiller.push("nautilusActivityUpdate");
      NautilusAi.updateActivity(this);
      profilerfiller.pop();
      super.customServerAiStep(p_459285_);
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
   protected SoundEvent getHurtSound(DamageSource p_451180_) {
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
      SoundEvent soundevent = this.isBaby() ? SoundEvents.BABY_NAUTILUS_EAT : SoundEvents.NAUTILUS_EAT;
      this.makeSound(soundevent);
   }

   @Override
   protected SoundEvent getSwimSound() {
      return this.isBaby() ? SoundEvents.BABY_NAUTILUS_SWIM : SoundEvents.NAUTILUS_SWIM;
   }

   @Override
   public int getMaxAirSupply() {
      return 300;
   }

   protected void handleAirSupply(ServerLevel p_458121_, int p_453134_) {
      if (this.isAlive() && !this.isInWater()) {
         this.setAirSupply(p_453134_ - 1);
         if (this.getAirSupply() <= -20) {
            this.setAirSupply(0);
            this.hurtServer(p_458121_, this.damageSources().dryOut(), 2.0F);
         }
      } else {
         this.setAirSupply(300);
      }
   }

   @Override
   public void baseTick() {
      int i = this.getAirSupply();
      super.baseTick();
      if (!this.isNoAi() && this.level() instanceof ServerLevel serverlevel) {
         this.handleAirSupply(serverlevel, i);
      }
   }

   @Override
   public boolean canBeLeashed() {
      return !this.isAggravated();
   }
}
