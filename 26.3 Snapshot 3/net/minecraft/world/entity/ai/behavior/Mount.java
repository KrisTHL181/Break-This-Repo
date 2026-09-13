package net.minecraft.world.entity.ai.behavior;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;

public class Mount {
   private static final int CLOSE_ENOUGH_TO_START_RIDING_DIST = 1;

   public static BehaviorControl<LivingEntity> create(final float speedModifier) {
      return BehaviorBuilder.create(
         i -> i.group(i.registered(MemoryModuleType.LOOK_TARGET), i.absent(MemoryModuleType.WALK_TARGET), i.present(MemoryModuleType.RIDE_TARGET))
            .apply(i, (lookTarget, walkTarget, rideTarget) -> (level, body, timestamp) -> {
               if (body.isPassenger()) {
                  return false;
               }

               Entity ridableEntity = i.get(rideTarget);
               if (ridableEntity.closerThan(body, 1.0)) {
                  body.startRiding(ridableEntity);
               } else {
                  lookTarget.set(new EntityTracker(ridableEntity, true));
                  walkTarget.set(new WalkTarget(new EntityTracker(ridableEntity, false), speedModifier, 1));
               }

               return true;
            })
      );
   }
}
