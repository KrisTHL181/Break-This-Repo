package net.minecraft.world.entity.ai.behavior;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;

public class StayCloseToTarget {
   public static BehaviorControl<LivingEntity> create(
      final Function<LivingEntity, Optional<PositionTracker>> targetPositionGetter,
      final Predicate<LivingEntity> shouldRunPredicate,
      final int closeEnough,
      final int tooFar,
      final float speedModifier
   ) {
      return BehaviorBuilder.create(
         i -> i.group(i.registered(MemoryModuleType.LOOK_TARGET), i.registered(MemoryModuleType.WALK_TARGET))
            .apply(i, (lookTarget, walkTarget) -> (level, body, timestamp) -> {
               Optional<PositionTracker> targetPosition = targetPositionGetter.apply(body);
               if (!targetPosition.isEmpty() && shouldRunPredicate.test(body)) {
                  PositionTracker positionTracker = targetPosition.get();
                  if (body.position().closerThan(positionTracker.currentPosition(), tooFar)) {
                     return false;
                  }

                  PositionTracker target = targetPosition.get();
                  lookTarget.set(target);
                  walkTarget.set(new WalkTarget(target, speedModifier, closeEnough));
                  return true;
               } else {
                  return false;
               }
            })
      );
   }
}
