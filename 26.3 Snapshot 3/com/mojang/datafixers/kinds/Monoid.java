package com.mojang.datafixers.kinds;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import java.util.List;

public interface Monoid<T> {
   T point();

   T add(T var1, T var2);

   static <T> Monoid<List<T>> listMonoid() {
      return new Monoid<List<T>>() {
         public List<T> point() {
            return ImmutableList.of();
         }

         public List<T> add(List<T> first, List<T> second) {
            Builder<T> builder = ImmutableList.builder();
            builder.addAll(first);
            builder.addAll(second);
            return builder.build();
         }
      };
   }
}
