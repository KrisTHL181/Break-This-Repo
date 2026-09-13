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

   public Sniffing(int p_217647_) {
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
         p_217647_
      );
   }

   protected boolean canStillUse(ServerLevel p_217653_, E p_217654_, long p_217655_) {
      return true;
   }

   protected void start(ServerLevel p_217664_, E p_217665_, long p_217666_) {
      p_217665_.playSound(SoundEvents.WARDEN_SNIFF, 5.0F, 1.0F);
   }

   protected void stop(ServerLevel p_217672_, E p_217673_, long p_217674_) {
      if (p_217673_.hasPose(Pose.SNIFFING)) {
         p_217673_.setPose(Pose.STANDING);
      }

      p_217673_.getBrain().eraseMemory(MemoryModuleType.IS_SNIFFING);
      p_217673_.getBrain().getMemory(MemoryModuleType.NEAREST_ATTACKABLE).filter(p_217673_::canTargetEntity).ifPresent(p_449586_ -> {
         if (p_217673_.closerThan(p_449586_, 6.0, 20.0)) {
            p_217673_.increaseAngerAt(p_449586_);
         }

         if (!p_217673_.getBrain().hasMemoryValue(MemoryModuleType.DISTURBANCE_LOCATION)) {
            WardenAi.setDisturbanceLocation(p_217673_, p_449586_.blockPosition());
         }
      });
   }
}
