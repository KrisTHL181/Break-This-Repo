package net.minecraft.world.entity.ai.behavior.warden;

import com.google.common.collect.ImmutableMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.monster.warden.WardenAi;

public class Sniffing<E extends Warden> extends Behavior<E> {
   private static final double ANGER_FROM_SNIFFING_MAX_DISTANCE_XZ = 6.0;
   private static final double ANGER_FROM_SNIFFING_MAX_DISTANCE_Y = 20.0;

   public Sniffing(final int ticks) {
      super(
         ImmutableMap.of(
            MemoryModuleType.IS_SNIFFING,
            MemoryStatus.VALUE_PRESENT,
            MemoryModuleType.ATTACK_TARGET,
            MemoryStatus.VALUE_ABSENT,
            MemoryModuleType.WALK_TARGET,
            MemoryStatus.VALUE_ABSENT,
            MemoryModuleType.LOOK_TARGET,
            MemoryStatus.REGISTERED,
            MemoryModuleType.NEAREST_ATTACKABLE,
            MemoryStatus.REGISTERED,
            MemoryModuleType.DISTURBANCE_LOCATION,
            MemoryStatus.REGISTERED,
            MemoryModuleType.SNIFF_COOLDOWN,
            MemoryStatus.REGISTERED
         ),
         ticks
      );
   }

   protected boolean canStillUse(final ServerLevel level, final E body, final long timestamp) {
      return true;
   }

   protected void start(final ServerLevel level, final E body, final long timestamp) {
      body.playSound(SoundEvents.WARDEN_SNIFF, 5.0F, 1.0F);
   }

   protected void stop(final ServerLevel level, final E body, final long timestamp) {
      if (body.hasPose(Pose.SNIFFING)) {
         body.setPose(Pose.STANDING);
      }

      body.getBrain().eraseMemory(MemoryModuleType.IS_SNIFFING);
      body.getBrain().getMemory(MemoryModuleType.NEAREST_ATTACKABLE).filter(body::canTargetEntity).ifPresent(entity -> {
         if (body.closerThan(entity, 6.0, 20.0)) {
            body.increaseAngerAt(entity);
         }

         if (!body.getBrain().hasMemoryValue(MemoryModuleType.DISTURBANCE_LOCATION)) {
            WardenAi.setDisturbanceLocation(body, entity.blockPosition());
         }
      });
   }
}
