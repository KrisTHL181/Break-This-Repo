package net.minecraft.world.entity.monster.piglin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.item.ItemEntity;

public class StartAdmiringItemIfSeen {
   public static BehaviorControl<LivingEntity> create(int p_259418_) {
      return BehaviorBuilder.create(
         p_259264_ -> p_259264_.group(
               p_259264_.present(MemoryModuleType.NEAREST_VISIBLE_WANTED_ITEM),
               p_259264_.absent(MemoryModuleType.ADMIRING_ITEM),
               p_259264_.absent(MemoryModuleType.ADMIRING_DISABLED),
               p_259264_.absent(MemoryModuleType.DISABLE_WALK_TO_ADMIRE_ITEM)
            )
            .apply(p_259264_, (p_259343_, p_260195_, p_259697_, p_259511_) -> (p_260130_, p_259946_, p_259235_) -> {
               ItemEntity itementity = p_259264_.get(p_259343_);
               if (!PiglinAi.isLovedItem(itementity.getItem())) {
                  return false;
               }

               p_260195_.setWithExpiry(true, p_259418_);
               return true;
            })
      );
   }
}
