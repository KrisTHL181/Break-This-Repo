package net.minecraft.world.entity.ai.behavior;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;

public class UpdateActivityFromSchedule {
   public static BehaviorControl<LivingEntity> create() {
      return BehaviorBuilder.create(p_259429_ -> p_259429_.point((p_449563_, p_449564_, p_449565_) -> {
         p_449564_.getBrain().updateActivityFromSchedule(p_449563_.environmentAttributes(), p_449563_.getGameTime(), p_449564_.position());
         return true;
      }));
   }
}
