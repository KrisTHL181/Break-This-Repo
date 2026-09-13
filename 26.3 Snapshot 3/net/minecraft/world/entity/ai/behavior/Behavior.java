package net.minecraft.world.entity.ai.behavior;

import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;

public abstract class Behavior<E extends LivingEntity> implements BehaviorControl<E> {
   public static final int DEFAULT_DURATION = 60;
   protected final Map<MemoryModuleType<?>, MemoryStatus> entryCondition;
   private Behavior.Status status = Behavior.Status.STOPPED;
   private long endTimestamp;
   private final int minDuration;
   private final int maxDuration;

   public Behavior(final Map<MemoryModuleType<?>, MemoryStatus> entryCondition) {
      this(entryCondition, 60);
   }

   public Behavior(final Map<MemoryModuleType<?>, MemoryStatus> entryCondition, final int timeOutDuration) {
      this(entryCondition, timeOutDuration, timeOutDuration);
   }

   public Behavior(final Map<MemoryModuleType<?>, MemoryStatus> entryCondition, final int minDuration, final int maxDuration) {
      this.minDuration = minDuration;
      this.maxDuration = maxDuration;
      this.entryCondition = entryCondition;
   }

   @Override
   public Behavior.Status getStatus() {
      return this.status;
   }

   @Override
   public Set<MemoryModuleType<?>> getRequiredMemories() {
      return this.entryCondition.keySet();
   }

   @Override
   public final boolean tryStart(final ServerLevel level, final E body, final long timestamp) {
      if (this.hasRequiredMemories(body) && this.checkExtraStartConditions(level, body)) {
         this.status = Behavior.Status.RUNNING;
         int duration = this.minDuration + level.getRandom().nextInt(this.maxDuration + 1 - this.minDuration);
         this.endTimestamp = timestamp + duration;
         this.start(level, body, timestamp);
         return true;
      } else {
         return false;
      }
   }

   protected void start(final ServerLevel level, final E body, final long timestamp) {
   }

   @Override
   public final void tickOrStop(final ServerLevel level, final E body, final long timestamp) {
      if (!this.timedOut(timestamp) && this.canStillUse(level, body, timestamp)) {
         this.tick(level, body, timestamp);
      } else {
         this.doStop(level, body, timestamp);
      }
   }

   protected void tick(final ServerLevel level, final E body, final long timestamp) {
   }

   @Override
   public final void doStop(final ServerLevel level, final E body, final long timestamp) {
      this.status = Behavior.Status.STOPPED;
      this.stop(level, body, timestamp);
   }

   protected void stop(final ServerLevel level, final E body, final long timestamp) {
   }

   protected boolean canStillUse(final ServerLevel level, final E body, final long timestamp) {
      return false;
   }

   protected boolean timedOut(final long timestamp) {
      return timestamp > this.endTimestamp;
   }

   protected boolean checkExtraStartConditions(final ServerLevel level, final E body) {
      return true;
   }

   @Override
   public String debugString() {
      return this.getClass().getSimpleName();
   }

   protected boolean hasRequiredMemories(final E body) {
      for (Entry<MemoryModuleType<?>, MemoryStatus> entry : this.entryCondition.entrySet()) {
         MemoryModuleType<?> memoryType = entry.getKey();
         MemoryStatus requiredStatus = entry.getValue();
         if (!body.getBrain().checkMemory(memoryType, requiredStatus)) {
            return false;
         }
      }

      return true;
   }

   public enum Status {
      STOPPED,
      RUNNING;
   }
}
