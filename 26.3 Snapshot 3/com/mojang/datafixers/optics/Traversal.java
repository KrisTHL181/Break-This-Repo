package com.mojang.datafixers.optics;

import com.mojang.datafixers.FunctionType;
import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.App2;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.datafixers.kinds.K1;
import com.mojang.datafixers.kinds.K2;
import com.mojang.datafixers.optics.profunctors.TraversalP;
import java.util.function.Function;

public interface Traversal<S, T, A, B> extends Wander<S, T, A, B>, App2<Traversal.Mu<A, B>, S, T>, Optic<TraversalP.Mu, S, T, A, B> {
   static <S, T, A, B> Traversal<S, T, A, B> unbox(App2<Traversal.Mu<A, B>, S, T> box) {
      return (Traversal<S, T, A, B>)box;
   }

   default <P extends K2> FunctionType<App2<P, A, B>, App2<P, S, T>> eval(App<? extends TraversalP.Mu, P> proof) {
      TraversalP<P, ? extends TraversalP.Mu> proof1 = TraversalP.unbox(proof);
      return input -> proof1.wander(this, input);
   }

   final class Instance<A2, B2> implements TraversalP<Traversal.Mu<A2, B2>, TraversalP.Mu> {
      // ===== 修改：修复 dimap 方法，移除错误的类型变量引用 =====
      @Override
      public <A, B, C, D> FunctionType<App2<Traversal.Mu<A2, B2>, A, B>, App2<Traversal.Mu<A2, B2>, C, D>> dimap(Function<C, A> g, Function<B, D> h) {
         return tr -> new Traversal<C, D, A2, B2>() {
            @Override
            public <F extends K1> FunctionType<C, App<F, D>> wander(Applicative<F, ?> applicative, FunctionType<A2, App<F, B2>> input) {
               return c -> applicative.map(h, Traversal.unbox(tr).wander(applicative, input).apply(g.apply(c)));
            }
         };
      }

      @Override
      public <S, T, A, B> App2<Traversal.Mu<A2, B2>, S, T> wander(final Wander<S, T, A, B> wander, final App2<Traversal.Mu<A2, B2>, A, B> input) {
         return new Traversal<S, T, A2, B2>() {
            @Override
            public <F extends K1> FunctionType<S, App<F, T>> wander(Applicative<F, ?> applicative, FunctionType<A2, App<F, B2>> function) {
               return wander.wander(applicative, Traversal.unbox(input).wander(applicative, function));
            }
         };
      }
   }

   final class Mu<A, B> implements K2 {
   }
}