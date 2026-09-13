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
   private final double speedModifierWhenCharging;
   private final double speedModifierWhenRepositioning;
   private final float targetInRangeRadiusSq;

   public SpearAttack(final double speedModifierWhenCharging, final double speedModifierWhenRepositioning, final float targetInRangeRadius) {
      super(Map.of(MemoryModuleType.SPEAR_STATUS, MemoryStatus.VALUE_PRESENT));
      this.speedModifierWhenCharging = speedModifierWhenCharging;
      this.speedModifierWhenRepositioning = speedModifierWhenRepositioning;
      this.targetInRangeRadiusSq = targetInRangeRadius * targetInRangeRadius;
   }

   private @Nullable LivingEntity getTarget(final PathfinderMob mob) {
      return mob.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).orElse(null);
   }

   private boolean ableToAttack(final PathfinderMob mob) {
      return this.getTarget(mob) != null && mob.getMainHandItem().has(DataComponents.KINETIC_WEAPON);
   }

   private int getKineticWeaponUseDuration(final PathfinderMob mob) {
      return Optional.ofNullable(mob.getMainHandItem().get(DataComponents.KINETIC_WEAPON)).map(KineticWeapon::computeDamageUseDuration).orElse(0);
   }

   protected boolean checkExtraStartConditions(final ServerLevel level, final PathfinderMob body) {
      return body.getBrain().getMemory(MemoryModuleType.SPEAR_STATUS).orElse(SpearAttack.SpearStatus.APPROACH) == SpearAttack.SpearStatus.CHARGING
         && this.ableToAttack(body)
         && !body.isUsingItem();
   }

   protected void start(final ServerLevel level, final PathfinderMob body, final long timestamp) {
      body.setAggressive(true);
      body.getBrain().setMemory(MemoryModuleType.SPEAR_ENGAGE_TIME, this.getKineticWeaponUseDuration(body));
      body.getBrain().eraseMemory(MemoryModuleType.SPEAR_CHARGE_POSITION);
      body.startUsingItem(InteractionHand.MAIN_HAND);
      super.start(level, body, timestamp);
   }

   protected boolean canStillUse(final ServerLevel level, final PathfinderMob body, final long timestamp) {
      return body.getBrain().getMemory(MemoryModuleType.SPEAR_ENGAGE_TIME).orElse(0) > 0 && this.ableToAttack(body);
   }

   protected void tick(final ServerLevel level, final PathfinderMob mob, final long timestamp) {
      LivingEntity target = this.getTarget(mob);
      double targetDistSqr = mob.distanceToSqr(target.getX(), target.getY(), target.getZ());
      Entity mount = mob.getRootVehicle();
      float speedModifier = 1.0F;
      if (mount instanceof Mob vehicleMob) {
         speedModifier = vehicleMob.chargeSpeedModifier();
      }

      int mountDistance = mob.isPassenger() ? 2 : 0;
      mob.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new EntityTracker(target, true));
      mob.getBrain().setMemory(MemoryModuleType.SPEAR_ENGAGE_TIME, mob.getBrain().getMemory(MemoryModuleType.SPEAR_ENGAGE_TIME).orElse(0) - 1);
      Vec3 awayPos = mob.getBrain().getMemory(MemoryModuleType.SPEAR_CHARGE_POSITION).orElse(null);
      if (awayPos != null) {
         mob.getNavigation().moveTo(awayPos.x, awayPos.y, awayPos.z, speedModifier * this.speedModifierWhenRepositioning);
         if (mob.getNavigation().isDone()) {
            mob.getBrain().eraseMemory(MemoryModuleType.SPEAR_CHARGE_POSITION);
         }
      } else {
         mob.getNavigation().moveTo(target, speedModifier * this.speedModifierWhenCharging);
         if (targetDistSqr < this.targetInRangeRadiusSq || mob.getNavigation().isDone()) {
            double distance = Math.sqrt(targetDistSqr);
            Vec3 newAwayPos = LandRandomPos.getPosAway(mob, 6 + mountDistance - distance, 7 + mountDistance - distance, 7, target.position());
            mob.getBrain().setMemory(MemoryModuleType.SPEAR_CHARGE_POSITION, newAwayPos);
         }
      }
   }

   protected void stop(final ServerLevel level, final PathfinderMob body, final long timestamp) {
      body.getNavigation().stop();
      body.stopUsingItem();
      body.getBrain().eraseMemory(MemoryModuleType.SPEAR_CHARGE_POSITION);
      body.getBrain().eraseMemory(MemoryModuleType.SPEAR_ENGAGE_TIME);
      body.getBrain().setMemory(MemoryModuleType.SPEAR_STATUS, SpearAttack.SpearStatus.RETREAT);
   }

   @Override
   protected boolean timedOut(final long timestamp) {
      return false;
   }

   public enum SpearStatus {
      APPROACH,
      CHARGING,
      RETREAT;
   }
}
