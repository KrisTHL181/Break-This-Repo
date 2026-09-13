package net.minecraft.world.entity.ai.behavior;

import java.util.function.Predicate;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;

public class CopyMemoryWithExpiry {
   public static <E extends LivingEntity, T> BehaviorControl<E> create(
      final Predicate<E> copyIfTrue, final MemoryModuleType<? extends T> sourceMemory, final MemoryModuleType<T> targetMemory, final UniformInt durationOfCopy
   ) {
      return BehaviorBuilder.create(i -> i.group(i.present(sourceMemory), i.absent(targetMemory)).apply(i, (source, target) -> (level, body, timestamp) -> {
         if (!copyIfTrue.test((E)body)) {
            return false;
         }

         target.setWithExpiry(i.get(source), durationOfCopy.sample(level.getRandom()));
         return true;
      }));
   }
}
