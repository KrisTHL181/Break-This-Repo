package net.minecraft.world.entity.ai.behavior;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.entity.schedule.Activity;

public class SetRaidStatus {
   public static BehaviorControl<LivingEntity> create() {
      return BehaviorBuilder.create(p_259382_ -> p_259382_.point((p_260026_, p_260271_, p_259518_) -> {
         if (p_260026_.random.nextInt(20) != 0) {
            return false;
         }

         Brain<?> brain = p_260271_.getBrain();
         Raid raid = p_260026_.getRaidAt(p_260271_.blockPosition());
         if (raid != null) {
            if (raid.hasFirstWaveSpawned() && !raid.isBetweenWaves()) {
               brain.setDefaultActivity(Activity.RAID);
               brain.setActiveActivityIfPossible(Activity.RAID);
            } else {
               brain.setDefaultActivity(Activity.PRE_RAID);
               brain.setActiveActivityIfPossible(Activity.PRE_RAID);
            }
         }

         return true;
      }));
   }
}
