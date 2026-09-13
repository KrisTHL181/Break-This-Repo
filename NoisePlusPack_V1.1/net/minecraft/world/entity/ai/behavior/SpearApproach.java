package net.minecraft.world.entity.ai.behavior;

import java.util.Map;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import org.jspecify.annotations.Nullable;

public class SpearApproach extends Behavior<PathfinderMob> {
   double speedModifierWhenRepositioning;
   float approachDistanceSq;

   public SpearApproach(double p_458469_, float p_454475_) {
      super(Map.of(MemoryModuleType.SPEAR_STATUS, MemoryStatus.VALUE_ABSENT));
      this.speedModifierWhenRepositioning = p_458469_;
      this.approachDistanceSq = p_454475_ * p_454475_;
   }

   private boolean ableToAttack(PathfinderMob p_454936_) {
      return this.getTarget(p_454936_) != null && p_454936_.getMainHandItem().has(DataComponents.KINETIC_WEAPON);
   }

   protected boolean checkExtraStartConditions(ServerLevel p_454688_, PathfinderMob p_458907_) {
      return this.ableToAttack(p_458907_) && !p_458907_.isUsingItem();
   }

   protected void start(ServerLevel p_450860_, PathfinderMob p_452398_, long p_456318_) {
      p_452398_.setAggressive(true);
      p_452398_.getBrain().setMemory(MemoryModuleType.SPEAR_STATUS, SpearAttack.SpearStatus.APPROACH);
      super.start(p_450860_, p_452398_, p_456318_);
   }

   private @Nullable LivingEntity getTarget(PathfinderMob p_455753_) {
      return p_455753_.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).orElse(null);
   }

   protected boolean canStillUse(ServerLevel p_455451_, PathfinderMob p_450925_, long p_455359_) {
      return this.ableToAttack(p_450925_) && this.farEnough(p_450925_);
   }

   private boolean farEnough(PathfinderMob p_457974_) {
      LivingEntity livingentity = this.getTarget(p_457974_);
      double d0 = p_457974_.distanceToSqr(livingentity.getX(), livingentity.getY(), livingentity.getZ());
      return d0 > this.approachDistanceSq;
   }

   protected void tick(ServerLevel p_451063_, PathfinderMob p_453727_, long p_455411_) {
      LivingEntity livingentity = this.getTarget(p_453727_);
      Entity entity = p_453727_.getRootVehicle();
      float f = 1.0F;
      if (entity instanceof Mob mob) {
         f = mob.chargeSpeedModifier();
      }

      p_453727_.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new EntityTracker(livingentity, true));
      p_453727_.getNavigation().moveTo(livingentity, f * this.speedModifierWhenRepositioning);
   }

   protected void stop(ServerLevel p_457089_, PathfinderMob p_460697_, long p_458673_) {
      p_460697_.getNavigation().stop();
      p_460697_.getBrain().setMemory(MemoryModuleType.SPEAR_STATUS, SpearAttack.SpearStatus.CHARGING);
   }

   @Override
   protected boolean timedOut(long p_454783_) {
      return false;
   }
}
