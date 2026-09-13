package net.minecraft.world.entity.monster.piglin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.item.ItemEntity;

public class StartAdmiringItemIfSeen {
   public static BehaviorControl<LivingEntity> create(final int admireDuration) {
      return BehaviorBuilder.create(
         i -> i.group(
               i.present(MemoryModuleType.NEAREST_VISIBLE_WANTED_ITEM),
               i.absent(MemoryModuleType.ADMIRING_ITEM),
               i.absent(MemoryModuleType.ADMIRING_DISABLED),
               i.absent(MemoryModuleType.DISABLE_WALK_TO_ADMIRE_ITEM)
            )
            .apply(i, (nearestItem, admiring, admiringDisabled, walkDisabled) -> (level, body, timestamp) -> {
               ItemEntity itemEntity = i.get(nearestItem);
               if (!PiglinAi.isLovedItem(itemEntity.getItem())) {
                  return false;
               }

               admiring.setWithExpiry(true, admireDuration);
               return true;
            })
      );
   }
}
