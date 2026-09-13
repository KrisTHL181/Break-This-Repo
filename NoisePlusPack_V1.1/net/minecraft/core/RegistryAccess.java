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
   <E> Optional<Registry<E>> lookup(ResourceKey<? extends Registry<? extends E>> var1);

   default <E> Registry<E> lookupOrThrow(ResourceKey<? extends Registry<? extends E>> p_369484_) {
      return this.lookup(p_369484_).orElseThrow(() -> new IllegalStateException("Missing registry: " + p_369484_));
   }

   Stream<RegistryAccess.RegistryEntry<?>> registries();

   @Override
   default Stream<ResourceKey<? extends Registry<?>>> listRegistryKeys() {
      return this.registries().map(p_358094_ -> p_358094_.key);
   }

   static RegistryAccess.Frozen fromRegistryOfRegistries(final Registry<? extends Registry<?>> p_206166_) {
      return new RegistryAccess.Frozen() {
         @Override
         public <T> Optional<Registry<T>> lookup(ResourceKey<? extends Registry<? extends T>> p_206220_) {
            Registry<Registry<T>> registry = (Registry<Registry<T>>)p_206166_;
            return registry.getOptional((ResourceKey<Registry<T>>)p_206220_);
         }

         @Override
         public Stream<RegistryAccess.RegistryEntry<?>> registries() {
            return p_206166_.entrySet().stream().map(RegistryAccess.RegistryEntry::fromMapEntry);
         }

         @Override
         public RegistryAccess.Frozen freeze() {
            return this;
         }
      };
   }

   default RegistryAccess.Frozen freeze() {
      class FrozenAccess extends RegistryAccess.ImmutableRegistryAccess implements RegistryAccess.Frozen {
         protected FrozenAccess(final Stream<RegistryAccess.RegistryEntry<?>> p_252031_) {
            super(p_252031_);
         }
      }

      return new FrozenAccess(this.registries().map(RegistryAccess.RegistryEntry::freeze));
   }

   interface Frozen extends RegistryAccess {
   }

   class ImmutableRegistryAccess implements RegistryAccess {
      private final Map<? extends ResourceKey<? extends Registry<?>>, ? extends Registry<?>> registries;

      public ImmutableRegistryAccess(List<? extends Registry<?>> p_248540_) {
         this.registries = p_248540_.stream().collect(Collectors.toUnmodifiableMap(Registry::key, p_247993_ -> p_247993_));
      }

      public ImmutableRegistryAccess(Map<? extends ResourceKey<? extends Registry<?>>, ? extends Registry<?>> p_206225_) {
         this.registries = Map.copyOf(p_206225_);
      }

      public ImmutableRegistryAccess(Stream<RegistryAccess.RegistryEntry<?>> p_206227_) {
         this.registries = p_206227_.collect(ImmutableMap.toImmutableMap(RegistryAccess.RegistryEntry::key, RegistryAccess.RegistryEntry::value));
      }

      @Override
      public <E> Optional<Registry<E>> lookup(ResourceKey<? extends Registry<? extends E>> p_206229_) {
         return Optional.ofNullable(this.registries.get(p_206229_)).map(p_206232_ -> (Registry<E>)p_206232_);
      }

      @Override
      public Stream<RegistryAccess.RegistryEntry<?>> registries() {
         return this.registries.entrySet().stream().map(RegistryAccess.RegistryEntry::fromMapEntry);
      }
   }

   record RegistryEntry<T>(ResourceKey<? extends Registry<T>> key, Registry<T> value) {
      private static <T, R extends Registry<? extends T>> RegistryAccess.RegistryEntry<T> fromMapEntry(
         Entry<? extends ResourceKey<? extends Registry<?>>, R> p_206242_
      ) {
         return fromUntyped((ResourceKey<? extends Registry<?>>)p_206242_.getKey(), p_206242_.getValue());
      }

      private static <T> RegistryAccess.RegistryEntry<T> fromUntyped(ResourceKey<? extends Registry<?>> p_206244_, Registry<?> p_206245_) {
         return new RegistryAccess.RegistryEntry<>((ResourceKey<? extends Registry<T>>)p_206244_, (Registry<T>)p_206245_);
      }

      private RegistryAccess.RegistryEntry<T> freeze() {
         return new RegistryAccess.RegistryEntry<>(this.key, this.value.freeze());
      }
   }
}
