package net.minecraft.world.entity.ai.behavior;

import java.util.Optional;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;

public class StartAttacking {
   public static <E extends Mob> BehaviorControl<E> create(final StartAttacking.TargetFinder<E> targetFinderFunction) {
      return create((level, body) -> true, targetFinderFunction);
   }

   public static <E extends Mob> BehaviorControl<E> create(
      final StartAttacking.StartAttackingCondition<E> canAttackPredicate, final StartAttacking.TargetFinder<E> targetFinderFunction
   ) {
      return BehaviorBuilder.create(
         i -> i.group(i.absent(MemoryModuleType.ATTACK_TARGET), i.registered(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE))
            .apply(i, (attackTarget, cantReachSince) -> (level, body, timestamp) -> {
               if (!canAttackPredicate.test(level, (E)body)) {
                  return false;
               }

               Optional<? extends LivingEntity> target = targetFinderFunction.get(level, (E)body);
               if (target.isEmpty()) {
                  return false;
               }

               LivingEntity targetEntity = target.get();
               if (!body.canAttack(targetEntity)) {
                  return false;
               }

               attackTarget.set(targetEntity);
               cantReachSince.erase();
               return true;
            })
      );
   }

   @FunctionalInterface
   public interface StartAttackingCondition<E> {
      boolean test(ServerLevel level, E body);
   }

   @FunctionalInterface
   public interface TargetFinder<E> {
      Optional<? extends LivingEntity> get(ServerLevel level, E body);
   }
}
