package net.minecraft.world.entity.ai.behavior;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.entity.schedule.Activity;

public class ReactToBell {
   public static BehaviorControl<LivingEntity> create() {
      return BehaviorBuilder.create(
         p_259349_ -> p_259349_.group(p_259349_.present(MemoryModuleType.HEARD_BELL_TIME)).apply(p_259349_, p_259472_ -> (p_449530_, p_449531_, p_449532_) -> {
            Raid raid = p_449530_.getRaidAt(p_449531_.blockPosition());
            if (raid == null) {
               p_449531_.getBrain().setActiveActivityIfPossible(Activity.HIDE);
            }

            return true;
         })
      );
   }
}
