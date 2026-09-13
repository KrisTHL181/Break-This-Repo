package net.minecraft.world.entity.ai.behavior;

import java.util.Map;
import java.util.Optional;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.item.component.KineticWeapon;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class SpearAttack extends Behavior<PathfinderMob> {
   public static final int MIN_REPOSITION_DISTANCE = 6;
   public static final int MAX_REPOSITION_DISTANCE = 7;
   double speedModifierWhenCharging;
   double speedModifierWhenRepositioning;
   float approachDistanceSq;
   float targetInRangeRadiusSq;

   public SpearAttack(double p_452321_, double p_460141_, float p_460072_, float p_459035_) {
      super(Map.of(MemoryModuleType.SPEAR_STATUS, MemoryStatus.VALUE_PRESENT));
      this.speedModifierWhenCharging = p_452321_;
      this.speedModifierWhenRepositioning = p_460141_;
      this.approachDistanceSq = p_460072_ * p_460072_;
      this.targetInRangeRadiusSq = p_459035_ * p_459035_;
   }

   private @Nullable LivingEntity getTarget(PathfinderMob p_454824_) {
      return p_454824_.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).orElse(null);
   }

   private boolean ableToAttack(PathfinderMob p_460154_) {
      return this.getTarget(p_460154_) != null && p_460154_.getMainHandItem().has(DataComponents.KINETIC_WEAPON);
   }

   private int getKineticWeaponUseDuration(PathfinderMob p_456332_) {
      return Optional.ofNullable(p_456332_.getMainHandItem().get(DataComponents.KINETIC_WEAPON)).map(KineticWeapon::computeDamageUseDuration).orElse(0);
   }

   protected boolean checkExtraStartConditions(ServerLevel p_455091_, PathfinderMob p_455878_) {
      return p_455878_.getBrain().getMemory(MemoryModuleType.SPEAR_STATUS).orElse(SpearAttack.SpearStatus.APPROACH) == SpearAttack.SpearStatus.CHARGING
         && this.ableToAttack(p_455878_)
         && !p_455878_.isUsingItem();
   }

   protected void start(ServerLevel p_450560_, PathfinderMob p_451345_, long p_459366_) {
      p_451345_.setAggressive(true);
      p_451345_.getBrain().setMemory(MemoryModuleType.SPEAR_ENGAGE_TIME, this.getKineticWeaponUseDuration(p_451345_));
      p_451345_.getBrain().eraseMemory(MemoryModuleType.SPEAR_CHARGE_POSITION);
      p_451345_.startUsingItem(InteractionHand.MAIN_HAND);
      super.start(p_450560_, p_451345_, p_459366_);
   }

   protected boolean canStillUse(ServerLevel p_452265_, PathfinderMob p_452312_, long p_454139_) {
      return p_452312_.getBrain().getMemory(MemoryModuleType.SPEAR_ENGAGE_TIME).orElse(0) > 0 && this.ableToAttack(p_452312_);
   }

   protected void tick(ServerLevel p_454513_, PathfinderMob p_456115_, long p_451165_) {
      LivingEntity livingentity = this.getTarget(p_456115_);
      double d0 = p_456115_.distanceToSqr(livingentity.getX(), livingentity.getY(), livingentity.getZ());
      Entity entity = p_456115_.getRootVehicle();
      float f = 1.0F;
      if (entity instanceof Mob mob) {
         f = mob.chargeSpeedModifier();
      }

      int i = p_456115_.isPassenger() ? 2 : 0;
      p_456115_.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new EntityTracker(livingentity, true));
      p_456115_.getBrain().setMemory(MemoryModuleType.SPEAR_ENGAGE_TIME, p_456115_.getBrain().getMemory(MemoryModuleType.SPEAR_ENGAGE_TIME).orElse(0) - 1);
      Vec3 vec3 = p_456115_.getBrain().getMemory(MemoryModuleType.SPEAR_CHARGE_POSITION).orElse(null);
      if (vec3 != null) {
         p_456115_.getNavigation().moveTo(vec3.x, vec3.y, vec3.z, f * this.speedModifierWhenRepositioning);
         if (p_456115_.getNavigation().isDone()) {
            p_456115_.getBrain().eraseMemory(MemoryModuleType.SPEAR_CHARGE_POSITION);
         }
      } else {
         p_456115_.getNavigation().moveTo(livingentity, f * this.speedModifierWhenCharging);
         if (d0 < this.targetInRangeRadiusSq || p_456115_.getNavigation().isDone()) {
            double d1 = Math.sqrt(d0);
            Vec3 vec31 = LandRandomPos.getPosAway(p_456115_, 6 + i - d1, 7 + i - d1, 7, livingentity.position());
            p_456115_.getBrain().setMemory(MemoryModuleType.SPEAR_CHARGE_POSITION, vec31);
         }
      }
   }

   protected void stop(ServerLevel p_457160_, PathfinderMob p_451535_, long p_455457_) {
      p_451535_.getNavigation().stop();
      p_451535_.stopUsingItem();
      p_451535_.getBrain().eraseMemory(MemoryModuleType.SPEAR_CHARGE_POSITION);
      p_451535_.getBrain().eraseMemory(MemoryModuleType.SPEAR_ENGAGE_TIME);
      p_451535_.getBrain().setMemory(MemoryModuleType.SPEAR_STATUS, SpearAttack.SpearStatus.RETREAT);
   }

   @Override
   protected boolean timedOut(long p_458511_) {
      return false;
   }

   public enum SpearStatus {
      APPROACH,
      CHARGING,
      RETREAT;
   }
}
