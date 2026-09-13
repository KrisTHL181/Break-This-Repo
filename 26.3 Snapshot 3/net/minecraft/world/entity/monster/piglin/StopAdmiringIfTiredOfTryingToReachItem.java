package net.minecraft.world.entity.monster.piglin;

import java.util.Optional;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;

public class StopAdmiringIfTiredOfTryingToReachItem {
   public static BehaviorControl<LivingEntity> create(final int maxTimeToReachItem, final int disableTime) {
      return BehaviorBuilder.create(
         i -> i.group(
               i.present(MemoryModuleType.ADMIRING_ITEM),
               i.present(MemoryModuleType.NEAREST_VISIBLE_WANTED_ITEM),
               i.registered(MemoryModuleType.TIME_TRYING_TO_REACH_ADMIRE_ITEM),
               i.registered(MemoryModuleType.DISABLE_WALK_TO_ADMIRE_ITEM)
            )
            .apply(i, (admiring, nearestVisible, time, disableWalk) -> (level, body, timestamp) -> {
               if (!body.getOffhandItem().isEmpty()) {
                  return false;
               }

               Optional<Integer> tryReachItemTimeOptional = i.tryGet(time);
               if (tryReachItemTimeOptional.isEmpty()) {
                  time.set(0);
               } else {
                  int timeTryingToReach = tryReachItemTimeOptional.get();
                  if (timeTryingToReach > maxTimeToReachItem) {
                     admiring.erase();
                     time.erase();
                     disableWalk.setWithExpiry(true, disableTime);
                  } else {
                     time.set(timeTryingToReach + 1);
                  }
               }

               return true;
            })
      );
   }
}
