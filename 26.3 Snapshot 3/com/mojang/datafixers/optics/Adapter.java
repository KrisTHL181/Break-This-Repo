package com.mojang.datafixers.optics;

import com.mojang.datafixers.FunctionType;
import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.App2;
import com.mojang.datafixers.kinds.K2;
import com.mojang.datafixers.optics.profunctors.Profunctor;
import java.util.function.Function;

public interface Adapter<S, T, A, B> extends App2<Adapter.Mu<A, B>, S, T>, Optic<Profunctor.Mu, S, T, A, B> {
   static <S, T, A, B> Adapter<S, T, A, B> unbox(App2<Adapter.Mu<A, B>, S, T> box) {
      return (Adapter<S, T, A, B>)box;
   }

   A from(S var1);

   T to(B var1);

   default <P extends K2> FunctionType<App2<P, A, B>, App2<P, S, T>> eval(App<? extends Profunctor.Mu, P> proofBox) {
      Profunctor<P, ? extends Profunctor.Mu> proof = Profunctor.unbox(proofBox);
      return a -> proof.dimap(a, this::from, this::to);
   }

   final class Instance<A2, B2> implements Profunctor<Adapter.Mu<A2, B2>, Profunctor.Mu> {
      // ===== 修改：修复 dimap 方法，移除错误的类型变量引用 =====
      @Override
      public <A, B, C, D> FunctionType<App2<Adapter.Mu<A2, B2>, A, B>, App2<Adapter.Mu<A2, B2>, C, D>> dimap(Function<C, A> g, Function<B, D> h) {
         return a -> Optics.<C, D, A2, B2>adapter(
            c -> Adapter.unbox(a).from(g.apply(c)),
            b2 -> h.apply(Adapter.unbox(a).to(b2))
         );
      }
   }

   final class Mu<A, B> implements K2 {
   }
}