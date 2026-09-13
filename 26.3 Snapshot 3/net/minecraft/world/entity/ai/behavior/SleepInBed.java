package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.level.block.AbstractBedBlock;
import net.minecraft.world.level.block.state.BlockState;

public class SleepInBed extends Behavior<LivingEntity> {
   public static final int COOLDOWN_AFTER_BEING_WOKEN = 100;
   private long nextOkStartTime;

   public SleepInBed() {
      super(
         ImmutableMap.of(
            MemoryModuleType.HOME,
            MemoryStatus.VALUE_PRESENT,
            MemoryModuleType.LAST_WOKEN,
            MemoryStatus.REGISTERED,
            MemoryModuleType.LAST_SLEPT,
            MemoryStatus.REGISTERED,
            MemoryModuleType.WALK_TARGET,
            MemoryStatus.REGISTERED,
            MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE,
            MemoryStatus.REGISTERED
         )
      );
   }

   @Override
   protected boolean checkExtraStartConditions(final ServerLevel level, final LivingEntity body) {
      if (body.isPassenger()) {
         return false;
      }

      Brain<?> brain = body.getBrain();
      GlobalPos target = brain.getMemory(MemoryModuleType.HOME).get();
      if (level.dimension() != target.dimension()) {
         return false;
      }

      Optional<Long> lastWokenMemory = brain.getMemory(MemoryModuleType.LAST_WOKEN);
      if (lastWokenMemory.isPresent()) {
         long timeSinceLastWoken = level.getGameTime() - lastWokenMemory.get();
         if (timeSinceLastWoken > 0L && timeSinceLastWoken < 100L) {
            return false;
         }
      }

      BlockState blockState = level.getBlockState(target.pos());
      return target.pos().closerToCenterThan(body.position(), 2.0)
         && blockState.is(BlockTags.VILLAGERS_CAN_SLEEP_ON_BED)
         && !blockState.getValue(AbstractBedBlock.OCCUPIED);
   }

   @Override
   protected boolean canStillUse(final ServerLevel level, final LivingEntity body, final long timestamp) {
      Optional<GlobalPos> memory = body.getBrain().getMemory(MemoryModuleType.HOME);
      if (memory.isEmpty()) {
         return false;
      }

      BlockPos bedPos = memory.get().pos();
      return body.getBrain().isActive(Activity.REST) && body.getY() > bedPos.getY() + 0.4 && bedPos.closerToCenterThan(body.position(), 1.14);
   }

   @Override
   protected void start(final ServerLevel level, final LivingEntity body, final long timestamp) {
      if (timestamp > this.nextOkStartTime) {
         Brain<?> brain = body.getBrain();
         if (brain.hasMemoryValue(MemoryModuleType.DOORS_TO_CLOSE)) {
            Set<GlobalPos> doors = brain.getMemory(MemoryModuleType.DOORS_TO_CLOSE).get();
            Optional<List<LivingEntity>> nearestEntities;
            if (brain.hasMemoryValue(MemoryModuleType.NEAREST_LIVING_ENTITIES)) {
               nearestEntities = brain.getMemory(MemoryModuleType.NEAREST_LIVING_ENTITIES);
            } else {
               nearestEntities = Optional.empty();
            }

            InteractWithDoor.closeDoorsThatIHaveOpenedOrPassedThrough(level, body, null, null, doors, nearestEntities);
         }

         body.startSleeping(body.getBrain().getMemory(MemoryModuleType.HOME).get().pos());
         brain.setMemory(MemoryModuleType.LAST_SLEPT, timestamp);
         brain.eraseMemory(MemoryModuleType.WALK_TARGET);
         brain.eraseMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
      }
   }

   @Override
   protected boolean timedOut(final long timestamp) {
      return false;
   }

   @Override
   protected void stop(final ServerLevel level, final LivingEntity body, final long timestamp) {
      if (body.isSleeping()) {
         body.stopSleeping();
         this.nextOkStartTime = timestamp + 40L;
      }
   }
}
