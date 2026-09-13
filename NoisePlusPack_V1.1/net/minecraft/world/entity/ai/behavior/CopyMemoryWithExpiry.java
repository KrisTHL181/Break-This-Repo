package net.minecraft.world.entity.ai.behavior;

import java.util.function.Predicate;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;

public class CopyMemoryWithExpiry {
   public static <E extends LivingEntity, T> BehaviorControl<E> create(
      Predicate<E> p_260270_, MemoryModuleType<? extends T> p_260344_, MemoryModuleType<T> p_260014_, UniformInt p_259596_
   ) {
      return BehaviorBuilder.create(
         p_260141_ -> p_260141_.group(p_260141_.present(p_260344_), p_260141_.absent(p_260014_))
            .apply(p_260141_, (p_259306_, p_259907_) -> (p_264887_, p_264888_, p_264889_) -> {
               if (!p_260270_.test((E)p_264888_)) {
                  return false;
               }

               p_259907_.setWithExpiry(p_260141_.get(p_259306_), p_259596_.sample(p_264887_.random));
               return true;
            })
      );
   }
}
