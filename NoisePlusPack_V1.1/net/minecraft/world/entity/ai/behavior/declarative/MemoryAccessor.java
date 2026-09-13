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

   public MemoryAccessor(Brain<?> p_259443_, MemoryModuleType<Value> p_259809_, App<F, Value> p_259295_) {
      this.brain = p_259443_;
      this.memoryType = p_259809_;
      this.value = p_259295_;
   }

   public App<F, Value> value() {
      return this.value;
   }

   public void set(Value p_259728_) {
      this.brain.setMemory(this.memoryType, Optional.of(p_259728_));
   }

   public void setOrErase(Optional<Value> p_259943_) {
      this.brain.setMemory(this.memoryType, p_259943_);
   }

   public void setWithExpiry(Value p_259027_, long p_260310_) {
      this.brain.setMemoryWithExpiry(this.memoryType, p_259027_, p_260310_);
   }

   public void erase() {
      this.brain.eraseMemory(this.memoryType);
   }
}
