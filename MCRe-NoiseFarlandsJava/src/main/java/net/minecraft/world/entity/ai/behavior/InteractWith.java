package net.minecraft.world.entity.ai.behavior;

import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.NearestVisibleLivingEntities;
import net.minecraft.world.entity.ai.memory.WalkTarget;

public class InteractWith {
    public static <T extends LivingEntity> BehaviorControl<LivingEntity> of(
        final EntityType<? extends T> type,
        final int interactionRange,
        final MemoryModuleType<T> interactionTarget,
        final float speedModifier,
        final int stopDistance
    ) {
        return of(type, interactionRange, mob -> true, mob -> true, interactionTarget, speedModifier, stopDistance);
    }

    public static <E extends LivingEntity, T extends LivingEntity> BehaviorControl<E> of(
        final EntityType<? extends T> type,
        final int interactionRange,
        final Predicate<E> selfFilter,
        final Predicate<T> targetFilter,
        final MemoryModuleType<T> interactionTarget,
        final float speedModifier,
        final int stopDistance
    ) {
        int interactionRangeSqr = interactionRange * interactionRange;
        Predicate<LivingEntity> isTargetValid = mob -> mob.is(type) && targetFilter.test((T)mob);
        return BehaviorBuilder.create(
            i -> i.group(
                    i.registered(interactionTarget),
                    i.registered(MemoryModuleType.LOOK_TARGET),
                    i.absent(MemoryModuleType.WALK_TARGET),
                    i.present(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES)
                )
                .apply(i, (target, lookTarget, walkTarget, nearestEntities) -> (level, body, timestamp) -> {
                    NearestVisibleLivingEntities entities = i.get(nearestEntities);
                    if (selfFilter.test(body) && entities.contains(isTargetValid)) {
                        Optional<LivingEntity> closest = entities.findClosest(mob -> mob.distanceToSqr(body) <= interactionRangeSqr && isTargetValid.test(mob));
                        closest.ifPresent(mob -> {
                            target.set((T)mob);
                            lookTarget.set(new EntityTracker(mob, true));
                            walkTarget.set(new WalkTarget(new EntityTracker(mob, false), speedModifier, stopDistance));
                        });
                        return true;
                    } else {
                        return false;
                    }
                })
        );
    }
}
