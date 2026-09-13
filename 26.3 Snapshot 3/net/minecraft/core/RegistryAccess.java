package net.minecraft.core;

import com.google.common.collect.ImmutableMap;
import com.mojang.logging.LogUtils;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.resources.ResourceKey;
import org.slf4j.Logger;

public interface RegistryAccess extends HolderLookup.Provider {
   Logger LOGGER = LogUtils.getLogger();
   RegistryAccess.Frozen EMPTY = new RegistryAccess.ImmutableRegistryAccess(Map.of()).freeze();

   @Override
   <E> Optional<Registry<E>> lookup(final ResourceKey<? extends Registry<? extends E>> registryKey);

   default <E> Registry<E> lookupOrThrow(final ResourceKey<? extends Registry<? extends E>> name) {
      return this.lookup(name).orElseThrow(() -> new IllegalStateException("Missing registry: " + name));
   }

   Stream<RegistryAccess.RegistryEntry<?>> registries();

   @Override
   default Stream<ResourceKey<? extends Registry<?>>> listRegistryKeys() {
      return this.registries().map(e -> e.key);
   }

   static RegistryAccess.Frozen fromRegistryOfRegistries(final Registry<? extends Registry<?>> registries) {
      return new RegistryAccess.Frozen() {
         @Override
         public <T> Optional<Registry<T>> lookup(final ResourceKey<? extends Registry<? extends T>> registryKey) {
            Registry<Registry<T>> registry = (Registry<Registry<T>>)registries;
            return registry.getOptional((ResourceKey<Registry<T>>)registryKey);
         }

         @Override
         public Stream<RegistryAccess.RegistryEntry<?>> registries() {
            return registries.entrySet().stream().map(RegistryAccess.RegistryEntry::fromMapEntry);
         }

         @Override
         public RegistryAccess.Frozen freeze() {
            return this;
         }
      };
   }

   default RegistryAccess.Frozen freeze() {
      class FrozenAccess extends RegistryAccess.ImmutableRegistryAccess implements RegistryAccess.Frozen {
         protected FrozenAccess(final Stream<RegistryAccess.RegistryEntry<?>> entries) {
            super(entries);
         }
      }

      return new FrozenAccess(this.registries().map(RegistryAccess.RegistryEntry::freeze));
   }

   interface Frozen extends RegistryAccess {
   }

   class ImmutableRegistryAccess implements RegistryAccess {
      private final Map<? extends ResourceKey<? extends Registry<?>>, ? extends Registry<?>> registries;

      public ImmutableRegistryAccess(final List<? extends Registry<?>> registries) {
         this.registries = registries.stream().collect(Collectors.toUnmodifiableMap(Registry::key, v -> v));
      }

      public ImmutableRegistryAccess(final Map<? extends ResourceKey<? extends Registry<?>>, ? extends Registry<?>> registries) {
         this.registries = Map.copyOf(registries);
      }

      public ImmutableRegistryAccess(final Stream<RegistryAccess.RegistryEntry<?>> entries) {
         this.registries = entries.collect(ImmutableMap.toImmutableMap(RegistryAccess.RegistryEntry::key, RegistryAccess.RegistryEntry::value));
      }

      @Override
      public <E> Optional<Registry<E>> lookup(final ResourceKey<? extends Registry<? extends E>> registryKey) {
         return Optional.ofNullable(this.registries.get(registryKey)).map(r -> (Registry<E>)r);
      }

      @Override
      public Stream<RegistryAccess.RegistryEntry<?>> registries() {
         return this.registries.entrySet().stream().map(RegistryAccess.RegistryEntry::fromMapEntry);
      }
   }

   record RegistryEntry<T>(ResourceKey<? extends Registry<T>> key, Registry<T> value) {
      private static <T, R extends Registry<? extends T>> RegistryAccess.RegistryEntry<T> fromMapEntry(
         final Entry<? extends ResourceKey<? extends Registry<?>>, R> e
      ) {
         return fromUntyped((ResourceKey<? extends Registry<?>>)e.getKey(), e.getValue());
      }

      private static <T> RegistryAccess.RegistryEntry<T> fromUntyped(final ResourceKey<? extends Registry<?>> key, final Registry<?> value) {
         return new RegistryAccess.RegistryEntry<>((ResourceKey<? extends Registry<T>>)key, (Registry<T>)value);
      }

      private RegistryAccess.RegistryEntry<T> freeze() {
         return new RegistryAccess.RegistryEntry<>(this.key, this.value.freeze());
      }
   }
}
