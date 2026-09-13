package net.minecraft.world.entity.ai.behavior;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.schedule.Activity;

public class WakeUp {
   public static BehaviorControl<LivingEntity> create() {
      return BehaviorBuilder.create(i -> i.point((level, body, timestamp) -> {
         if (!body.getBrain().isActive(Activity.REST) && body.isSleeping()) {
            body.stopSleeping();
            return true;
         } else {
            return false;
         }
      }));
   }
}
