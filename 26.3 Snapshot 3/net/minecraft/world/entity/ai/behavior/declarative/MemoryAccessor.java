package net.minecraft.world.entity.ai.behavior.declarative;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.K1;
import java.util.Optional;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;

public final class MemoryAccessor<F extends K1, Value> {
   private final Brain<?> brain;
   private final MemoryModuleType<Value> memoryType;
   private final App<F, Value> value;

   public MemoryAccessor(final Brain<?> brain, final MemoryModuleType<Value> memoryType, final App<F, Value> value) {
      this.brain = brain;
      this.memoryType = memoryType;
      this.value = value;
   }

   public App<F, Value> value() {
      return this.value;
   }

   public void set(final Value value) {
      this.brain.setMemory(this.memoryType, Optional.of(value));
   }

   public void setOrErase(final Optional<Value> value) {
      this.brain.setMemory(this.memoryType, value);
   }

   public void setWithExpiry(final Value value, final long timeToLive) {
      this.brain.setMemoryWithExpiry(this.memoryType, value, timeToLive);
   }

   public void erase() {
      this.brain.eraseMemory(this.memoryType);
   }
}
