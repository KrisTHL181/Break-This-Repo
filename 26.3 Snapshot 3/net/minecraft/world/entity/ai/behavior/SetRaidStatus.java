package net.minecraft.world.entity.ai.behavior;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.entity.schedule.Activity;

public class SetRaidStatus {
   public static BehaviorControl<LivingEntity> create() {
      return BehaviorBuilder.create(i -> i.point((level, body, timestamp) -> {
         if (level.getRandom().nextInt(20) != 0) {
            return false;
         }

         Brain<?> brain = body.getBrain();
         Raid nearbyRaid = level.getRaidAt(body.blockPosition());
         if (nearbyRaid != null) {
            if (nearbyRaid.hasFirstWaveSpawned() && !nearbyRaid.isBetweenWaves()) {
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
