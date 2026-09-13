package net.minecraft.world.entity.monster.piglin;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;

public class StopHoldingItemIfNoLongerAdmiring {
   public static BehaviorControl<Piglin> create() {
      return BehaviorBuilder.create(
         p_259197_ -> p_259197_.group(p_259197_.absent(MemoryModuleType.ADMIRING_ITEM)).apply(p_259197_, p_259512_ -> (p_449723_, p_449724_, p_449725_) -> {
            if (!p_449724_.getOffhandItem().isEmpty() && !p_449724_.getOffhandItem().has(DataComponents.BLOCKS_ATTACKS)) {
               PiglinAi.stopHoldingOffHandItem(p_449723_, p_449724_, true);
               return true;
            } else {
               return false;
            }
         })
      );
   }
}
