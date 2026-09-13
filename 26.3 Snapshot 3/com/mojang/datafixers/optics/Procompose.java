package com.mojang.datafixers.optics;

import com.mojang.datafixers.FunctionType;
import com.mojang.datafixers.kinds.App2;
import com.mojang.datafixers.kinds.K2;
import com.mojang.datafixers.optics.profunctors.Profunctor;
import java.util.function.Function;
import java.util.function.Supplier;

public final class Procompose<F extends K2, G extends K2, A, B, C> implements App2<Procompose.Mu<F, G>, A, B> {
   private final Supplier<App2<F, A, C>> first;
   private final App2<G, C, B> second;

   public Procompose(Supplier<App2<F, A, C>> first, App2<G, C, B> second) {
      this.first = first;
      this.second = second;
   }

   public static <F extends K2, G extends K2, A, B> Procompose<F, G, A, B, ?> unbox(App2<Procompose.Mu<F, G>, A, B> box) {
      return (Procompose<F, G, A, B, ?>)box;
   }

   public Supplier<App2<F, A, C>> first() {
      return this.first;
   }

   public App2<G, C, B> second() {
      return this.second;
   }

   public static final class Mu<F extends K2, G extends K2> implements K2 {
   }

   static final class ProfunctorInstance<F extends K2, G extends K2> implements Profunctor<Procompose.Mu<F, G>, Profunctor.Mu> {
      private final Profunctor<F, Profunctor.Mu> p1;
      private final Profunctor<G, Profunctor.Mu> p2;

      ProfunctorInstance(Profunctor<F, Profunctor.Mu> p1, Profunctor<G, Profunctor.Mu> p2) {
         this.p1 = p1;
         this.p2 = p2;
      }

      @Override
      public <A, B, C, D> FunctionType<App2<Procompose.Mu<F, G>, A, B>, App2<Procompose.Mu<F, G>, C, D>> dimap(Function<C, A> g, Function<B, D> h) {
         return cmp -> this.cap(Procompose.unbox(cmp), g, h);
      }

      // ===== 修改：移除错误的强制转换 =====
      private <A, B, C, D, E> App2<Procompose.Mu<F, G>, C, D> cap(Procompose<F, G, A, B, E> cmp, Function<C, A> g, Function<B, D> h) {
         return new Procompose<>(
            () -> this.p1.dimap(g, Function.identity()).apply(cmp.first.get()),
            this.p2.dimap(Function.identity(), h).apply(cmp.second)
         );
      }
   }
}