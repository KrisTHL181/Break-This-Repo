package net.minecraft.world.entity.ai.memory;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterators;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import java.util.Iterator;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.core.registries.BuiltInRegistries;
import org.jspecify.annotations.Nullable;

public final class MemoryMap implements Iterable<MemoryMap.Value<?>> {
   private static final Codec<MemoryModuleType<?>> SERIALIZABLE_MEMORY_MODULE_CODEC = BuiltInRegistries.MEMORY_MODULE_TYPE
      .byNameCodec()
      .validate(type -> type.canSerialize() ? DataResult.success(type) : DataResult.error(() -> "Memory module " + type + " cannot be encoded"));
   public static final Codec<MemoryMap> CODEC = Codec.dispatchedMap(SERIALIZABLE_MEMORY_MODULE_CODEC, type -> (Codec)type.getCodec().orElseThrow())
      .xmap(MemoryMap::new, m -> m.memories);
   public static final MemoryMap EMPTY = new MemoryMap(Map.of());
   private final Map<MemoryModuleType<?>, ExpirableValue<?>> memories;

   private MemoryMap(final Map<MemoryModuleType<?>, ExpirableValue<?>> memories) {
      this.memories = Map.copyOf(memories);
   }

   public static MemoryMap of(final Stream<MemoryMap.Value<?>> memories) {
      return new MemoryMap(memories.collect(Collectors.toMap(MemoryMap.Value::type, MemoryMap.Value::value)));
   }

   public <U> @Nullable ExpirableValue<U> get(final MemoryModuleType<U> type) {
      return (ExpirableValue<U>)this.memories.get(type);
   }

   @Override
   public boolean equals(final Object obj) {
      return obj instanceof MemoryMap map && this.memories.equals(map.memories);
   }

   @Override
   public int hashCode() {
      return this.memories.hashCode();
   }

   @Override
   public String toString() {
      return this.memories.toString();
   }

   @Override
   public Iterator<MemoryMap.Value<?>> iterator() {
      return Iterators.transform(
         this.memories.entrySet().iterator(), entry -> MemoryMap.Value.createUnchecked((MemoryModuleType)entry.getKey(), (ExpirableValue<?>)entry.getValue())
      );
   }

   public static class Builder {
      private final com.google.common.collect.ImmutableMap.Builder<MemoryModuleType<?>, ExpirableValue<?>> builder = ImmutableMap.builder();

      public <U> MemoryMap.Builder add(final MemoryModuleType<U> type, final ExpirableValue<U> value) {
         this.builder.put(type, value);
         return this;
      }

      public MemoryMap build() {
         return new MemoryMap(this.builder.buildOrThrow());
      }
   }

   public record Value<U>(MemoryModuleType<U> type, ExpirableValue<U> value) {
      public static <U> MemoryMap.Value<U> createUnchecked(final MemoryModuleType<U> type, final ExpirableValue<?> value) {
         return new MemoryMap.Value<>(type, (ExpirableValue<U>)value);
      }
   }
}
