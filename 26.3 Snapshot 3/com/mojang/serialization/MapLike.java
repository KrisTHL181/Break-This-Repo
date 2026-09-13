package com.mojang.serialization;

import com.mojang.datafixers.util.Pair;
import java.util.Map;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;

public interface MapLike<T> {
   MapLike<Object> EMPTY = new MapLike<Object>() {
      @Nullable
      @Override
      public Object get(Object key) {
         return null;
      }

      @Nullable
      @Override
      public Object get(String key) {
         return null;
      }

      @Override
      public Stream<Pair<Object, Object>> entries() {
         return Stream.empty();
      }

      @Override
      public String toString() {
         return "EmptyMapLike";
      }
   };

   static <T> MapLike<T> empty() {
      return (MapLike<T>)EMPTY;
   }

   @Nullable
   T get(T var1);

   @Nullable
   T get(String var1);

   Stream<Pair<T, T>> entries();

   static <T> MapLike<T> forMap(final Map<T, T> map, final DynamicOps<T> ops) {
      return map.isEmpty() ? empty() : new MapLike<T>() {
         @Nullable
         @Override
         public T get(T key) {
            return map.get(key);
         }

         @Nullable
         @Override
         public T get(String key) {
            return (T)this.get(ops.createString(key));
         }

         @Override
         public Stream<Pair<T, T>> entries() {
            return map.entrySet().stream().map(e -> Pair.of(e.getKey(), e.getValue()));
         }

         @Override
         public String toString() {
            return "MapLike[" + map + "]";
         }
      };
   }
}
