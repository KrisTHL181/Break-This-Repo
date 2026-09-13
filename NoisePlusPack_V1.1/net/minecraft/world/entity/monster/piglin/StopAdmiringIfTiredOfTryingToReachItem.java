package net.minecraft.world.entity.monster.piglin;

import java.util.Optional;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;

public class StopAdmiringIfTiredOfTryingToReachItem {
   public static BehaviorControl<LivingEntity> create(int p_259110_, int p_259200_) {
      return BehaviorBuilder.create(
         p_260320_ -> p_260320_.group(
               p_260320_.present(MemoryModuleType.ADMIRING_ITEM),
               p_260320_.present(MemoryModuleType.NEAREST_VISIBLE_WANTED_ITEM),
               p_260320_.registered(MemoryModuleType.TIME_TRYING_TO_REACH_ADMIRE_ITEM),
               p_260320_.registered(MemoryModuleType.DISABLE_WALK_TO_ADMIRE_ITEM)
            )
            .apply(p_260320_, (p_260184_, p_259407_, p_259388_, p_259580_) -> (p_259044_, p_259229_, p_259125_) -> {
               if (!p_259229_.getOffhandItem().isEmpty()) {
                  return false;
               }

               Optional<Integer> optional = p_260320_.tryGet(p_259388_);
               if (optional.isEmpty()) {
                  p_259388_.set(0);
               } else {
                  int i = optional.get();
                  if (i > p_259110_) {
                     p_260184_.erase();
                     p_259388_.erase();
                     p_259580_.setWithExpiry(true, p_259200_);
                  } else {
                     p_259388_.set(i + 1);
                  }
               }

               return true;
            })
      );
   }
}
