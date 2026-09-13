package net.minecraft.world.entity.ai.behavior;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.NearestVisibleLivingEntities;

public class BackUpIfTooClose {
   public static OneShot<Mob> create(final int tooCloseDistance, final float strafeSpeed) {
      return BehaviorBuilder.create(
         i -> i.group(
               i.absent(MemoryModuleType.WALK_TARGET),
               i.registered(MemoryModuleType.LOOK_TARGET),
               i.present(MemoryModuleType.ATTACK_TARGET),
               i.present(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES)
            )
            .apply(i, (walkTarget, lookTarget, attackTarget, nearestVisible) -> (level, body, timestamp) -> {
               LivingEntity target = i.get(attackTarget);
               if (target.closerThan(body, tooCloseDistance) && i.<NearestVisibleLivingEntities>get(nearestVisible).contains(target)) {
                  lookTarget.set(new EntityTracker(target, true));
                  body.getMoveControl().strafe(-strafeSpeed, 0.0F);
                  body.setYRot(Mth.rotateIfNecessary(body.getYRot(), body.yHeadRot, 0.0F));
                  return true;
               } else {
                  return false;
               }
            })
      );
   }
}
