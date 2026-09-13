package net.minecraft.world.entity.ai.behavior;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.entity.schedule.Activity;

public class ResetRaidStatus {
   public static BehaviorControl<LivingEntity> create() {
      return BehaviorBuilder.create(p_259870_ -> p_259870_.point((p_421687_, p_421688_, p_421689_) -> {
         if (p_421687_.random.nextInt(20) != 0) {
            return false;
         }

         Brain<?> brain = p_421688_.getBrain();
         Raid raid = p_421687_.getRaidAt(p_421688_.blockPosition());
         if (raid == null || raid.isStopped() || raid.isLoss()) {
            brain.setDefaultActivity(Activity.IDLE);
            brain.updateActivityFromSchedule(p_421687_.environmentAttributes(), p_421687_.getGameTime(), p_421688_.position());
         }

         return true;
      }));
   }
}
