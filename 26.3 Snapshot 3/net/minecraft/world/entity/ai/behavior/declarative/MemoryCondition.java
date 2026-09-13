package net.minecraft.world.entity.ai.behavior.declarative;

import com.mojang.datafixers.kinds.Const;
import com.mojang.datafixers.kinds.IdF;
import com.mojang.datafixers.kinds.K1;
import com.mojang.datafixers.kinds.OptionalBox;
import com.mojang.datafixers.kinds.Const.Mu;
import com.mojang.datafixers.util.Unit;
import java.util.Optional;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import org.jspecify.annotations.Nullable;

public interface MemoryCondition<F extends K1, Value> {
   MemoryModuleType<Value> memory();

   MemoryStatus condition();

   @Nullable MemoryAccessor<F, Value> createAccessor(Brain<?> brain, final Optional<Value> value);

   record Absent<Value>(MemoryModuleType<Value> memory) implements MemoryCondition<Mu<Unit>, Value> {
      @Override
      public MemoryStatus condition() {
         return MemoryStatus.VALUE_ABSENT;
      }

      @Override
      public MemoryAccessor<Mu<Unit>, Value> createAccessor(final Brain<?> brain, final Optional<Value> value) {
         return value.isPresent() ? null : new MemoryAccessor<>(brain, this.memory, Const.create(Unit.INSTANCE));
      }
   }

   record Present<Value>(MemoryModuleType<Value> memory) implements MemoryCondition<com.mojang.datafixers.kinds.IdF.Mu, Value> {
      @Override
      public MemoryStatus condition() {
         return MemoryStatus.VALUE_PRESENT;
      }

      @Override
      public MemoryAccessor<com.mojang.datafixers.kinds.IdF.Mu, Value> createAccessor(final Brain<?> brain, final Optional<Value> value) {
         return value.isEmpty() ? null : new MemoryAccessor<>(brain, this.memory, IdF.create(value.get()));
      }
   }

   record Registered<Value>(MemoryModuleType<Value> memory) implements MemoryCondition<com.mojang.datafixers.kinds.OptionalBox.Mu, Value> {
      @Override
      public MemoryStatus condition() {
         return MemoryStatus.REGISTERED;
      }

      @Override
      public MemoryAccessor<com.mojang.datafixers.kinds.OptionalBox.Mu, Value> createAccessor(final Brain<?> brain, final Optional<Value> value) {
         return new MemoryAccessor<>(brain, this.memory, OptionalBox.create(value));
      }
   }
}
