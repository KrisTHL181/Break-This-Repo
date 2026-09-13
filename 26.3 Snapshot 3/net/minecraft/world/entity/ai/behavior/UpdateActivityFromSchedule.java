package net.minecraft.world.entity.ai.behavior;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;

public class UpdateActivityFromSchedule {
   public static BehaviorControl<LivingEntity> create() {
      return BehaviorBuilder.create(i -> i.point((level, body, timestamp) -> {
         body.getBrain().updateActivityFromSchedule(level.environmentAttributes(), level.getGameTime(), body.position());
         return true;
      }));
   }
}
