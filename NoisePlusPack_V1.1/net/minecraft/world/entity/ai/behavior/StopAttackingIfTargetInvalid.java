package net.minecraft.world.entity.ai.behavior;

import java.util.Optional;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;

public class StopAttackingIfTargetInvalid {
   private static final int TIMEOUT_TO_GET_WITHIN_ATTACK_RANGE = 200;

   public static <E extends Mob> BehaviorControl<E> create(StopAttackingIfTargetInvalid.TargetErasedCallback<E> p_362440_) {
      return create((p_364423_, p_147988_) -> false, p_362440_, true);
   }

   public static <E extends Mob> BehaviorControl<E> create(StopAttackingIfTargetInvalid.StopAttackCondition p_361383_) {
      return create(p_361383_, (p_363632_, p_217411_, p_217412_) -> {}, true);
   }

   public static <E extends Mob> BehaviorControl<E> create() {
      return create((p_367631_, p_147986_) -> false, (p_363605_, p_217408_, p_217409_) -> {}, true);
   }

   public static <E extends Mob> BehaviorControl<E> create(
      StopAttackingIfTargetInvalid.StopAttackCondition p_363585_, StopAttackingIfTargetInvalid.TargetErasedCallback<E> p_365345_, boolean p_260319_
   ) {
      return BehaviorBuilder.create(
         p_258801_ -> p_258801_.group(p_258801_.present(MemoryModuleType.ATTACK_TARGET), p_258801_.registered(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE))
            .apply(
               p_258801_,
               (p_258787_, p_258788_) -> (p_359057_, p_359058_, p_359059_) -> {
                  LivingEntity livingentity = p_258801_.get(p_258787_);
                  if (p_359058_.canAttack(livingentity)
                     && (!p_260319_ || !isTiredOfTryingToReachTarget(p_359058_, p_258801_.tryGet(p_258788_)))
                     && livingentity.isAlive()
                     && livingentity.level() == p_359058_.level()
                     && !p_363585_.test(p_359057_, livingentity)) {
                     return true;
                  }

                  p_365345_.accept(p_359057_, (E)p_359058_, livingentity);
                  p_258787_.erase();
                  return true;
               }
            )
      );
   }

   private static boolean isTiredOfTryingToReachTarget(LivingEntity p_259416_, Optional<Long> p_259377_) {
      return p_259377_.isPresent() && p_259416_.level().getGameTime() - p_259377_.get() > 200L;
   }

   @FunctionalInterface
   public interface StopAttackCondition {
      boolean test(ServerLevel var1, LivingEntity var2);
   }

   @FunctionalInterface
   public interface TargetErasedCallback<E> {
      void accept(ServerLevel var1, E var2, LivingEntity var3);
   }
}
