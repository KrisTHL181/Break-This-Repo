package net.minecraft.world.entity.monster.breeze;

import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.phys.Vec3;

public class Slide extends Behavior<Breeze> {
   public Slide() {
      super(
         Map.of(
            MemoryModuleType.ATTACK_TARGET,
            MemoryStatus.VALUE_PRESENT,
            MemoryModuleType.WALK_TARGET,
            MemoryStatus.VALUE_ABSENT,
            MemoryModuleType.BREEZE_JUMP_COOLDOWN,
            MemoryStatus.VALUE_ABSENT,
            MemoryModuleType.BREEZE_SHOOT,
            MemoryStatus.VALUE_ABSENT
         )
      );
   }

   protected boolean checkExtraStartConditions(final ServerLevel level, final Breeze breeze) {
      return breeze.onGround() && !breeze.isInWater() && breeze.getPose() == Pose.STANDING;
   }

   protected void start(final ServerLevel level, final Breeze breeze, final long timestamp) {
      LivingEntity enemy = breeze.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).orElse(null);
      if (enemy != null) {
         boolean isWithinInnerRing = breeze.withinInnerCircleRange(enemy.position());
         Vec3 position = null;
         if (isWithinInnerRing) {
            Vec3 position0 = DefaultRandomPos.getPosAway(breeze, 5, 5, enemy.position());
            if (position0 != null
               && BreezeUtil.hasLineOfSight(breeze, position0)
               && enemy.distanceToSqr(position0.x, position0.y, position0.z) > enemy.distanceToSqr(breeze)) {
               position = position0;
            }
         }

         if (position == null) {
            position = breeze.getRandom().nextBoolean()
               ? BreezeUtil.randomPointBehindTarget(enemy, breeze.getRandom())
               : randomPointInMiddleCircle(breeze, enemy);
         }

         breeze.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(BlockPos.containing(position), 0.6F, 1));
      }
   }

   private static Vec3 randomPointInMiddleCircle(final Breeze breeze, final LivingEntity enemy) {
      Vec3 direction = enemy.position().subtract(breeze.position());
      double distance = direction.length() - Mth.lerp(breeze.getRandom().nextDouble(), 8.0, 4.0);
      Vec3 target = direction.normalize().multiply(distance, distance, distance);
      return breeze.position().add(target);
   }
}
