package net.minecraft.world.entity.ai.behavior.warden;

import java.util.Optional;
import java.util.function.Function;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.monster.warden.Warden;

public class SetRoarTarget {
   public static <E extends Warden> BehaviorControl<E> create(Function<E, Optional<? extends LivingEntity>> p_260275_) {
      return BehaviorBuilder.create(
         p_258921_ -> p_258921_.group(
               p_258921_.absent(MemoryModuleType.ROAR_TARGET),
               p_258921_.absent(MemoryModuleType.ATTACK_TARGET),
               p_258921_.registered(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE)
            )
            .apply(p_258921_, (p_258929_, p_258930_, p_258931_) -> (p_258925_, p_258926_, p_258927_) -> {
               Optional<? extends LivingEntity> optional = p_260275_.apply((E)p_258926_);
               if (optional.filter(p_258926_::canTargetEntity).isEmpty()) {
                  return false;
               }

               p_258929_.set(optional.get());
               p_258931_.erase();
               return true;
            })
      );
   }
}
