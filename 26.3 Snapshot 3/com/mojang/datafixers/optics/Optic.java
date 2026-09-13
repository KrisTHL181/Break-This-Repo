package com.mojang.datafixers.optics;

import com.google.common.reflect.TypeToken;
import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.App2;
import com.mojang.datafixers.kinds.K1;
import com.mojang.datafixers.kinds.K2;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public interface Optic<Proof extends K1, S, T, A, B> {
   <P extends K2> Function<App2<P, A, B>, App2<P, S, T>> eval(App<? extends Proof, P> var1);

   // ===== 修改：使用 if-else 分支并显式类型转换 =====
   default <Proof2 extends K1> Optional<Optic<? super Proof2, S, T, A, B>> upCast(Set<TypeToken<? extends K1>> proofBounds, TypeToken<Proof2> proof) {
      if (proofBounds.stream().allMatch(bound -> bound.isSupertypeOf(proof))) {
         return Optional.<Optic<? super Proof2, S, T, A, B>>of(this);
      } else {
         return Optional.empty();
      }
   }

   record CompositionOptic<Proof extends K1, S, T, A, B>(List<? extends Optic<? super Proof, ?, ?, ?, ?>> optics) implements Optic<Proof, S, T, A, B> {
      @Override
      public <P extends K2> Function<App2<P, A, B>, App2<P, S, T>> eval(App<? extends Proof, P> proof) {
         List<Function<? extends App2<P, ?, ?>, ? extends App2<P, ?, ?>>> functions = new ArrayList<>(this.optics.size());

         for (int i = this.optics.size() - 1; i >= 0; i--) {
            functions.add(this.optics.get(i).eval(proof));
         }

         return input -> {
            App2<P, ?, ?> result = input;

            for (Function<? extends App2<P, ?, ?>, ? extends App2<P, ?, ?>> function : functions) {
               result = applyUnchecked(function, result);
            }

            return (App2<P, S, T>)result;
         };
      }

      private static <P extends K2, T extends App2<P, ?, ?>> App2<P, ?, ?> applyUnchecked(Function<T, ? extends App2<P, ?, ?>> function, App2<P, ?, ?> input) {
         return (App2<P, ?, ?>)function.apply((T)input);
      }

      @Override
      public String toString() {
         return "(" + this.optics.stream().map(Object::toString).collect(Collectors.joining(" ◦ ")) + ")";
      }
   }
}