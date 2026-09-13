package net.minecraft.world.entity.ai.behavior.warden;

import com.google.common.collect.ImmutableMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.monster.warden.Warden;

public class Digging<E extends Warden> extends Behavior<E> {
   public Digging(final int ticks) {
      super(ImmutableMap.of(MemoryModuleType.ATTACK_TARGET, MemoryStatus.VALUE_ABSENT, MemoryModuleType.WALK_TARGET, MemoryStatus.VALUE_ABSENT), ticks);
   }

   protected boolean canStillUse(final ServerLevel level, final E body, final long timestamp) {
      return body.getRemovalReason() == null;
   }

   protected boolean checkExtraStartConditions(final ServerLevel level, final E body) {
      return body.onGround() || body.isInWater() || body.isInLava();
   }

   protected void start(final ServerLevel level, final E body, final long timestamp) {
      if (body.onGround()) {
         body.setPose(Pose.DIGGING);
         body.playSound(SoundEvents.WARDEN_DIG, 5.0F, 1.0F);
      } else {
         body.playSound(SoundEvents.WARDEN_AGITATED, 5.0F, 1.0F);
         this.stop(level, body, timestamp);
      }
   }

   protected void stop(final ServerLevel level, final E body, final long timestamp) {
      if (body.getRemovalReason() == null) {
         body.remove(Entity.RemovalReason.DISCARDED);
      }
   }
}
