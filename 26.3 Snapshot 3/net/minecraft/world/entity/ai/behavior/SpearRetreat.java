package net.minecraft.world.entity.ai.behavior;

import java.util.Map;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class SpearRetreat extends Behavior<PathfinderMob> {
   public static final int MIN_COOLDOWN_DISTANCE = 9;
   public static final int MAX_COOLDOWN_DISTANCE = 11;
   public static final int MAX_FLEEING_TIME = 100;
   private final double speedModifierWhenRepositioning;

   public SpearRetreat(final double speedModifierWhenRepositioning) {
      super(Map.of(MemoryModuleType.SPEAR_STATUS, MemoryStatus.VALUE_PRESENT), 100);
      this.speedModifierWhenRepositioning = speedModifierWhenRepositioning;
   }

   private @Nullable LivingEntity getTarget(final PathfinderMob mob) {
      return mob.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).orElse(null);
   }

   private boolean ableToAttack(final PathfinderMob mob) {
      return this.getTarget(mob) != null && mob.getMainHandItem().has(DataComponents.KINETIC_WEAPON);
   }

   protected boolean checkExtraStartConditions(final ServerLevel level, final PathfinderMob body) {
      if (this.ableToAttack(body) && !body.isUsingItem()) {
         if (body.getBrain().getMemory(MemoryModuleType.SPEAR_STATUS).orElse(SpearAttack.SpearStatus.APPROACH) != SpearAttack.SpearStatus.RETREAT) {
            return false;
         }

         LivingEntity target = this.getTarget(body);
         double targetDistSqr = body.distanceToSqr(target.getX(), target.getY(), target.getZ());
         int mountDistance = body.isPassenger() ? 2 : 0;
         double distance = Math.sqrt(targetDistSqr);
         Vec3 awayPos = LandRandomPos.getPosAway(
            body, Math.max(0.0, 9 + mountDistance - distance), Math.max(1.0, 11 + mountDistance - distance), 7, target.position()
         );
         if (awayPos == null) {
            return false;
         }

         body.getBrain().setMemory(MemoryModuleType.SPEAR_FLEEING_POSITION, awayPos);
         return true;
      } else {
         return false;
      }
   }

   protected void start(final ServerLevel level, final PathfinderMob body, final long timestamp) {
      body.setAggressive(true);
      body.getBrain().setMemory(MemoryModuleType.SPEAR_FLEEING_TIME, 0);
      super.start(level, body, timestamp);
   }

   protected boolean canStillUse(final ServerLevel level, final PathfinderMob body, final long timestamp) {
      return body.getBrain().getMemory(MemoryModuleType.SPEAR_FLEEING_TIME).orElse(100) < 100
         && body.getBrain().getMemory(MemoryModuleType.SPEAR_FLEEING_POSITION).isPresent()
         && !body.getNavigation().isDone()
         && this.ableToAttack(body);
   }

   protected void tick(final ServerLevel level, final PathfinderMob mob, final long timestamp) {
      LivingEntity target = this.getTarget(mob);
      float speedModifier = mob.getRootVehicle() instanceof Mob vehicleMob ? vehicleMob.chargeSpeedModifier() : 1.0F;
      mob.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new EntityTracker(target, true));
      mob.getBrain().setMemory(MemoryModuleType.SPEAR_FLEEING_TIME, mob.getBrain().getMemory(MemoryModuleType.SPEAR_FLEEING_TIME).orElse(0) + 1);
      mob.getBrain()
         .getMemory(MemoryModuleType.SPEAR_FLEEING_POSITION)
         .ifPresent(fleePos -> mob.getNavigation().moveTo(fleePos.x, fleePos.y, fleePos.z, speedModifier * this.speedModifierWhenRepositioning));
   }

   protected void stop(final ServerLevel level, final PathfinderMob body, final long timestamp) {
      body.getNavigation().stop();
      body.setAggressive(false);
      body.stopUsingItem();
      body.getBrain().eraseMemory(MemoryModuleType.SPEAR_FLEEING_TIME);
      body.getBrain().eraseMemory(MemoryModuleType.SPEAR_FLEEING_POSITION);
      body.getBrain().eraseMemory(MemoryModuleType.SPEAR_STATUS);
   }
}
