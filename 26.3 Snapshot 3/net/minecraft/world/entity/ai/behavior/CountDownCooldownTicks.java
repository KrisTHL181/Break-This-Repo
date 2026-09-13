package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;

public class CountDownCooldownTicks extends Behavior<LivingEntity> {
   private final MemoryModuleType<Integer> cooldownTicks;

   public CountDownCooldownTicks(final MemoryModuleType<Integer> cooldownTicks) {
      super(ImmutableMap.of(cooldownTicks, MemoryStatus.VALUE_PRESENT));
      this.cooldownTicks = cooldownTicks;
   }

   private Optional<Integer> getCooldownTickMemory(final LivingEntity body) {
      return body.getBrain().getMemory(this.cooldownTicks);
   }

   @Override
   protected boolean timedOut(final long timestamp) {
      return false;
   }

   @Override
   protected boolean canStillUse(final ServerLevel level, final LivingEntity body, final long timestamp) {
      Optional<Integer> calmDownTicks = this.getCooldownTickMemory(body);
      return calmDownTicks.isPresent() && calmDownTicks.get() > 0;
   }

   @Override
   protected void tick(final ServerLevel level, final LivingEntity body, final long timestamp) {
      Optional<Integer> calmDownTicks = this.getCooldownTickMemory(body);
      body.getBrain().setMemory(this.cooldownTicks, calmDownTicks.get() - 1);
   }

   @Override
   protected void stop(final ServerLevel level, final LivingEntity body, final long timestamp) {
      body.getBrain().eraseMemory(this.cooldownTicks);
   }
}
