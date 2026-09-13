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
   private final double speedModifierWhenRepositioning;
   private final float approachDistanceSq;

   public SpearApproach(final double speedModifierWhenRepositioning, final float approachDistance) {
      super(Map.of(MemoryModuleType.SPEAR_STATUS, MemoryStatus.VALUE_ABSENT));
      this.speedModifierWhenRepositioning = speedModifierWhenRepositioning;
      this.approachDistanceSq = approachDistance * approachDistance;
   }

   private boolean ableToAttack(final PathfinderMob mob) {
      return this.getTarget(mob) != null && mob.getMainHandItem().has(DataComponents.KINETIC_WEAPON);
   }

   protected boolean checkExtraStartConditions(final ServerLevel level, final PathfinderMob body) {
      return this.ableToAttack(body) && !body.isUsingItem();
   }

   protected void start(final ServerLevel level, final PathfinderMob body, final long timestamp) {
      body.setAggressive(true);
      body.getBrain().setMemory(MemoryModuleType.SPEAR_STATUS, SpearAttack.SpearStatus.APPROACH);
      super.start(level, body, timestamp);
   }

   private @Nullable LivingEntity getTarget(final PathfinderMob mob) {
      return mob.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).orElse(null);
   }

   protected boolean canStillUse(final ServerLevel level, final PathfinderMob body, final long timestamp) {
      return this.ableToAttack(body) && this.farEnough(body);
   }

   private boolean farEnough(final PathfinderMob mob) {
      LivingEntity target = this.getTarget(mob);
      double targetDistSqr = mob.distanceToSqr(target.getX(), target.getY(), target.getZ());
      return targetDistSqr > this.approachDistanceSq;
   }

   protected void tick(final ServerLevel level, final PathfinderMob mob, final long timestamp) {
      LivingEntity target = this.getTarget(mob);
      Entity mount = mob.getRootVehicle();
      float speedModifier = 1.0F;
      if (mount instanceof Mob vehicleMob) {
         speedModifier = vehicleMob.chargeSpeedModifier();
      }

      mob.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new EntityTracker(target, true));
      mob.getNavigation().moveTo(target, speedModifier * this.speedModifierWhenRepositioning);
   }

   protected void stop(final ServerLevel level, final PathfinderMob body, final long timestamp) {
      body.getNavigation().stop();
      body.getBrain().setMemory(MemoryModuleType.SPEAR_STATUS, SpearAttack.SpearStatus.CHARGING);
   }

   @Override
   protected boolean timedOut(final long timestamp) {
      return false;
   }
}
