package net.minecraft.core.component;

import com.google.common.collect.Iterators;
import com.google.common.collect.Sets;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import it.unimi.dsi.fastutil.objects.Reference2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMaps;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Spliterators;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.jspecify.annotations.Nullable;

public interface DataComponentMap extends Iterable<TypedDataComponent<?>>, DataComponentGetter {
   DataComponentMap EMPTY = new DataComponentMap() {
      @Override
      public <T> @Nullable T get(DataComponentType<? extends T> p_331068_) {
         return null;
      }

      @Override
      public Set<DataComponentType<?>> keySet() {
         return Set.of();
      }

      @Override
      public Iterator<TypedDataComponent<?>> iterator() {
         return Collections.emptyIterator();
      }
   };
   Codec<DataComponentMap> CODEC = makeCodecFromMap(DataComponentType.VALUE_MAP_CODEC);

   static Codec<DataComponentMap> makeCodec(Codec<DataComponentType<?>> p_343101_) {
      return makeCodecFromMap(Codec.dispatchedMap(p_343101_, DataComponentType::codecOrThrow));
   }

   static Codec<DataComponentMap> makeCodecFromMap(Codec<Map<DataComponentType<?>, Object>> p_343378_) {
      return p_343378_.flatComapMap(DataComponentMap.Builder::buildFromMapTrusted, p_329446_ -> {
         int i = p_329446_.size();
         if (i == 0) {
            return DataResult.success(Reference2ObjectMaps.emptyMap());
         }

         Reference2ObjectMap<DataComponentType<?>, Object> reference2objectmap = new Reference2ObjectArrayMap(i);

         for (TypedDataComponent<?> typeddatacomponent : p_329446_) {
            if (!typeddatacomponent.type().isTransient()) {
               reference2objectmap.put(typeddatacomponent.type(), typeddatacomponent.value());
            }
         }

         return DataResult.success(reference2objectmap);
      });
   }

   static DataComponentMap composite(final DataComponentMap p_329885_, final DataComponentMap p_330534_) {
      return new DataComponentMap() {
         @Override
         public <T> @Nullable T get(DataComponentType<? extends T> p_330817_) {
            T t = p_330534_.get(p_330817_);
            return t != null ? t : p_329885_.get(p_330817_);
         }

         @Override
         public Set<DataComponentType<?>> keySet() {
            return Sets.union(p_329885_.keySet(), p_330534_.keySet());
         }
      };
   }

   static DataComponentMap.Builder builder() {
      return new DataComponentMap.Builder();
   }

   Set<DataComponentType<?>> keySet();

   default boolean has(DataComponentType<?> p_334046_) {
      return this.get(p_334046_) != null;
   }

   @Override
   default Iterator<TypedDataComponent<?>> iterator() {
      return Iterators.transform(this.keySet().iterator(), p_389658_ -> Objects.requireNonNull(this.getTyped(p_389658_)));
   }

   default Stream<TypedDataComponent<?>> stream() {
      return StreamSupport.stream(Spliterators.spliterator(this.iterator(), this.size(), 1345), false);
   }

   default int size() {
      return this.keySet().size();
   }

   default boolean isEmpty() {
      return this.size() == 0;
   }

   default DataComponentMap filter(final Predicate<DataComponentType<?>> p_329403_) {
      return new DataComponentMap() {
         @Override
         public <T> @Nullable T get(DataComponentType<? extends T> p_329684_) {
            return p_329403_.test(p_329684_) ? DataComponentMap.this.get(p_329684_) : null;
         }

         @Override
         public Set<DataComponentType<?>> keySet() {
            return Sets.filter(DataComponentMap.this.keySet(), p_329403_::test);
         }
      };
   }

   class Builder {
      private final Reference2ObjectMap<DataComponentType<?>, Object> map = new Reference2ObjectArrayMap();

      Builder() {
      }

      public <T> DataComponentMap.Builder set(DataComponentType<T> p_336133_, @Nullable T p_329579_) {
         this.setUnchecked(p_336133_, p_329579_);
         return this;
      }

      <T> void setUnchecked(DataComponentType<T> p_331443_, @Nullable Object p_334337_) {
         if (p_334337_ != null) {
            this.map.put(p_331443_, p_334337_);
         } else {
            this.map.remove(p_331443_);
         }
      }

      public DataComponentMap.Builder addAll(DataComponentMap p_335426_) {
         for (TypedDataComponent<?> typeddatacomponent : p_335426_) {
            this.map.put(typeddatacomponent.type(), typeddatacomponent.value());
         }

         return this;
      }

      public DataComponentMap build() {
         return buildFromMapTrusted(this.map);
      }

      private static DataComponentMap buildFromMapTrusted(Map<DataComponentType<?>, Object> p_330455_) {
         if (p_330455_.isEmpty()) {
            return DataComponentMap.EMPTY;
         } else {
            return p_330455_.size() < 8
               ? new DataComponentMap.Builder.SimpleMap(new Reference2ObjectArrayMap(p_330455_))
               : new DataComponentMap.Builder.SimpleMap(new Reference2ObjectOpenHashMap(p_330455_));
         }
      }

      record SimpleMap(Reference2ObjectMap<DataComponentType<?>, Object> map) implements DataComponentMap {
         @Override
         public <T> @Nullable T get(DataComponentType<? extends T> p_335671_) {
            return (T)this.map.get(p_335671_);
         }

         @Override
         public boolean has(DataComponentType<?> p_335479_) {
            return this.map.containsKey(p_335479_);
         }

         @Override
         public Set<DataComponentType<?>> keySet() {
            return this.map.keySet();
         }

         @Override
         public Iterator<TypedDataComponent<?>> iterator() {
            return Iterators.transform(Reference2ObjectMaps.fastIterator(this.map), TypedDataComponent::fromEntryUnchecked);
         }

         @Override
         public int size() {
            return this.map.size();
         }

         @Override
         public String toString() {
            return this.map.toString();
         }
      }
   }
}
