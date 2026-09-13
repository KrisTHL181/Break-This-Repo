package net.minecraft.world.entity.ai.behavior;

import java.util.Optional;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;

public class StartAttacking {
   public static <E extends Mob> BehaviorControl<E> create(StartAttacking.TargetFinder<E> p_368894_) {
      return create((p_362883_, p_24212_) -> true, p_368894_);
   }

   public static <E extends Mob> BehaviorControl<E> create(StartAttacking.StartAttackingCondition<E> p_363679_, StartAttacking.TargetFinder<E> p_360766_) {
      return BehaviorBuilder.create(
         p_258782_ -> p_258782_.group(p_258782_.absent(MemoryModuleType.ATTACK_TARGET), p_258782_.registered(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE))
            .apply(p_258782_, (p_258778_, p_258779_) -> (p_359048_, p_359049_, p_359050_) -> {
               if (!p_363679_.test(p_359048_, (E)p_359049_)) {
                  return false;
               }

               Optional<? extends LivingEntity> optional = p_360766_.get(p_359048_, (E)p_359049_);
               if (optional.isEmpty()) {
                  return false;
               }

               LivingEntity livingentity = optional.get();
               if (!p_359049_.canAttack(livingentity)) {
                  return false;
               }

               p_258778_.set(livingentity);
               p_258779_.erase();
               return true;
            })
      );
   }

   @FunctionalInterface
   public interface StartAttackingCondition<E> {
      boolean test(ServerLevel var1, E var2);
   }

   @FunctionalInterface
   public interface TargetFinder<E> {
      Optional<? extends LivingEntity> get(ServerLevel var1, E var2);
   }
}
