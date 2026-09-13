package net.minecraft.world.entity.monster.piglin;

import java.util.Optional;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.item.ItemEntity;

public class StopAdmiringIfItemTooFarAway<E extends Piglin> {
   public static BehaviorControl<LivingEntity> create(final int maxDistanceToItem) {
      return BehaviorBuilder.create(
         i -> i.group(i.present(MemoryModuleType.ADMIRING_ITEM), i.registered(MemoryModuleType.NEAREST_VISIBLE_WANTED_ITEM))
            .apply(i, (admiring, nearest) -> (level, body, timestamp) -> {
               if (!body.getOffhandItem().isEmpty()) {
                  return false;
               }

               Optional<ItemEntity> nearestVisibleWantedItem = i.tryGet(nearest);
               if (nearestVisibleWantedItem.isPresent() && nearestVisibleWantedItem.get().closerThan(body, maxDistanceToItem)) {
                  return false;
               }

               admiring.erase();
               return true;
            })
      );
   }
}
