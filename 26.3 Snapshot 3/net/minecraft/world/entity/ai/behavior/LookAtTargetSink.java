package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;

public class LookAtTargetSink extends Behavior<Mob> {
   public LookAtTargetSink(final int minDuration, final int maxDuration) {
      super(ImmutableMap.of(MemoryModuleType.LOOK_TARGET, MemoryStatus.VALUE_PRESENT), minDuration, maxDuration);
   }

   protected boolean canStillUse(final ServerLevel level, final Mob body, final long timestamp) {
      return body.getBrain().getMemory(MemoryModuleType.LOOK_TARGET).filter(pos -> pos.isVisibleBy(body)).isPresent();
   }

   protected void stop(final ServerLevel level, final Mob body, final long timestamp) {
      body.getBrain().eraseMemory(MemoryModuleType.LOOK_TARGET);
   }

   protected void tick(final ServerLevel level, final Mob body, final long timestamp) {
      body.getBrain().getMemory(MemoryModuleType.LOOK_TARGET).ifPresent(target -> body.getLookControl().setLookAt(target.currentPosition()));
   }
}
