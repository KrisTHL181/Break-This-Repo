package net.minecraft.core.component;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.ImmutableMultimap.Builder;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Predicate;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;

public class DataComponentLookup<T> {
   private final Iterable<? extends Holder<T>> elements;
   private volatile Map<DataComponentType<?>, DataComponentLookup.ComponentStorage<?, T>> cache = Map.of();

   public DataComponentLookup(final Iterable<? extends Holder<T>> elements) {
      this.elements = elements;
   }

   private <C> DataComponentLookup.@Nullable ComponentStorage<C, T> getFromCache(final DataComponentType<C> type) {
      return (DataComponentLookup.ComponentStorage<C, T>)this.cache.get(type);
   }

   private <C> DataComponentLookup.ComponentStorage<C, T> getOrCreateStorage(final DataComponentType<C> type) {
      DataComponentLookup.ComponentStorage<C, T> existingStorage = this.getFromCache(type);
      if (existingStorage != null) {
         return existingStorage;
      }

      DataComponentLookup.ComponentStorage<C, T> newStorage = this.scanForComponents(type);
      synchronized (this) {
         DataComponentLookup.ComponentStorage<C, T> foreignStorage = this.getFromCache(type);
         if (foreignStorage != null) {
            return foreignStorage;
         }

         this.cache = Util.copyAndPut(this.cache, type, newStorage);
         return newStorage;
      }
   }

   private <C> DataComponentLookup.ComponentStorage<C, T> scanForComponents(final DataComponentType<C> type) {
      Builder<C, Holder<T>> results = ImmutableMultimap.builder();

      for (Holder<T> element : this.elements) {
         C componentValue = element.components().get(type);
         if (componentValue != null) {
            results.put(componentValue, element);
         }
      }

      return new DataComponentLookup.ComponentStorage<>(results.build());
   }

   public <C> Stream<Holder<T>> findMatching(final DataComponentType<C> type, final Predicate<C> predicate) {
      return this.getOrCreateStorage(type).findMatching(predicate);
   }

   public <C> Collection<Holder<T>> findAll(final DataComponentType<C> type, final C value) {
      return this.getOrCreateStorage(type).findAll(value);
   }

   public <C> Collection<Holder<T>> findAll(final DataComponentType<C> type) {
      return this.getOrCreateStorage(type).valueToComponent.values();
   }

   private record ComponentStorage<C, T>(Multimap<C, Holder<T>> valueToComponent) {
      public Collection<Holder<T>> findAll(final C value) {
         return this.valueToComponent.get(value);
      }

      public Stream<Holder<T>> findMatching(final Predicate<C> predicate) {
         return this.valueToComponent.isEmpty()
            ? Stream.empty()
            : this.valueToComponent.entries().stream().filter(e -> predicate.test((C)e.getKey())).map(Entry::getValue);
      }
   }
}
