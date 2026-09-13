package net.minecraft.world.entity.ai.behavior;

import java.util.Optional;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.NearestVisibleLivingEntities;

public class SetLookAndInteract {
   public static BehaviorControl<LivingEntity> create(final EntityType<?> type, final int interactionRange) {
      int interactionRangeSqr = interactionRange * interactionRange;
      return BehaviorBuilder.create(
         i -> i.group(
               i.registered(MemoryModuleType.LOOK_TARGET),
               i.absent(MemoryModuleType.INTERACTION_TARGET),
               i.present(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES)
            )
            .apply(
               i,
               (lookTarget, interactionTarget, nearestEntities) -> (level, body, timestamp) -> {
                  Optional<LivingEntity> closest = i.<NearestVisibleLivingEntities>get(nearestEntities)
                     .findClosest(e -> e.distanceToSqr(body) <= interactionRangeSqr && e.is(type));
                  if (closest.isEmpty()) {
                     return false;
                  }

                  LivingEntity closestEntity = closest.get();
                  interactionTarget.set(closestEntity);
                  lookTarget.set(new EntityTracker(closestEntity, true));
                  return true;
               }
            )
      );
   }
}
