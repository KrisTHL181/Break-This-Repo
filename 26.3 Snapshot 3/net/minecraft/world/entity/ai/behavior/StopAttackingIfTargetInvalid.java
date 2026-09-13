package net.minecraft.world.entity.ai.behavior;

import java.util.Optional;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;

public class StopAttackingIfTargetInvalid {
   private static final int TIMEOUT_TO_GET_WITHIN_ATTACK_RANGE = 200;

   public static <E extends Mob> BehaviorControl<E> create(final StopAttackingIfTargetInvalid.TargetErasedCallback<E> onTargetErased) {
      return create((level, entity) -> false, onTargetErased, true);
   }

   public static <E extends Mob> BehaviorControl<E> create(final StopAttackingIfTargetInvalid.StopAttackCondition stopAttackingWhen) {
      return create(stopAttackingWhen, (level, body, target) -> {}, true);
   }

   public static <E extends Mob> BehaviorControl<E> create() {
      return create((level, entity) -> false, (level, body, target) -> {}, true);
   }

   public static <E extends Mob> BehaviorControl<E> create(
      final StopAttackingIfTargetInvalid.StopAttackCondition stopAttackingWhen,
      final StopAttackingIfTargetInvalid.TargetErasedCallback<E> onTargetErased,
      final boolean canGrowTiredOfTryingToReachTarget
   ) {
      return BehaviorBuilder.create(
         i -> i.group(i.present(MemoryModuleType.ATTACK_TARGET), i.registered(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE))
            .apply(
               i,
               (attackTarget, cantReachSince) -> (level, body, timestamp) -> {
                  LivingEntity target = i.get(attackTarget);
                  if (body.canAttack(target)
                     && (!canGrowTiredOfTryingToReachTarget || !isTiredOfTryingToReachTarget(body, i.tryGet(cantReachSince)))
                     && target.isAlive()
                     && target.level() == body.level()
                     && !stopAttackingWhen.test(level, target)) {
                     return true;
                  }

                  onTargetErased.accept(level, (E)body, target);
                  attackTarget.erase();
                  return true;
               }
            )
      );
   }

   private static boolean isTiredOfTryingToReachTarget(final LivingEntity body, final Optional<Long> cantReachSince) {
      return cantReachSince.isPresent() && body.level().getGameTime() - cantReachSince.get() > 200L;
   }

   @FunctionalInterface
   public interface StopAttackCondition {
      boolean test(ServerLevel level, LivingEntity target);
   }

   @FunctionalInterface
   public interface TargetErasedCallback<E> {
      void accept(ServerLevel level, E body, LivingEntity target);
   }
}
