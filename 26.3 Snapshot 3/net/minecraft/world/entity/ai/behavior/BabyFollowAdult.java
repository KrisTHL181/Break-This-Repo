package net.minecraft.world.entity.ai.behavior;

import java.util.function.Function;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;

public class BabyFollowAdult {
   public static OneShot<LivingEntity> create(final UniformInt followRange, final float speedModifier) {
      return create(followRange, mob -> speedModifier, MemoryModuleType.NEAREST_VISIBLE_ADULT, false);
   }

   public static OneShot<LivingEntity> create(
      final UniformInt followRange,
      final Function<LivingEntity, Float> speedModifier,
      final MemoryModuleType<? extends LivingEntity> nearestVisibleType,
      final boolean targetEye
   ) {
      return BehaviorBuilder.create(
         i -> i.group(i.present(nearestVisibleType), i.registered(MemoryModuleType.LOOK_TARGET), i.absent(MemoryModuleType.WALK_TARGET))
            .apply(i, (nearestAdult, lookTarget, walkTarget) -> (level, body, timestamp) -> {
               if (!body.isBaby()) {
                  return false;
               } else {
                  LivingEntity adult = i.get(nearestAdult);
                  if (body.closerThan(adult, followRange.maxInclusive() + 1) && !body.closerThan(adult, followRange.minInclusive())) {
                     WalkTarget target = new WalkTarget(
                        new EntityTracker(adult, targetEye, targetEye), speedModifier.apply(body), followRange.minInclusive() - 1
                     );
                     lookTarget.set(new EntityTracker(adult, true, targetEye));
                     walkTarget.set(target);
                     return true;
                  } else {
                     return false;
                  }
               }
            })
      );
   }
}
