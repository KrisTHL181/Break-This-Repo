package com.mojang.datafixers.optics;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.mojang.datafixers.FunctionType;
import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.datafixers.kinds.K1;
import java.util.List;

public final class ListTraversal<A, B> implements Traversal<List<A>, List<B>, A, B> {
   static final ListTraversal<?, ?> INSTANCE = new ListTraversal();

   private ListTraversal() {
   }

   @Override
   public <F extends K1> FunctionType<List<A>, App<F, List<B>>> wander(Applicative<F, ?> applicative, FunctionType<A, App<F, B>> input) {
      return as -> {
         App<F, Builder<B>> result = applicative.point(ImmutableList.builder());

         for (A a : as) {
            result = applicative.ap2(applicative.point(Builder::add), result, input.apply(a));
         }

         return applicative.map(Builder::build, result);
      };
   }

   @Override
   public String toString() {
      return "ListTraversal";
   }
}
