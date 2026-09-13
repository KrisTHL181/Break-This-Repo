package com.mojang.datafixers.kinds;

import java.util.function.BiFunction;
import java.util.function.Function;

public final class Const<C, T> implements App<Const.Mu<C>, T> {
   private final C value;

   // ===== 修改：强制转换加上泛型参数 =====
   public static <C, T> C unbox(App<Const.Mu<C>, T> box) {
      return ((Const<C, T>)box).value;
   }

   public static <C, T> Const<C, T> create(C value) {
      return new Const<>(value);
   }

   Const(C value) {
      this.value = value;
   }

   public static final class Instance<C> implements Applicative<Const.Mu<C>, Const.Instance.Mu<C>> {
      private final Monoid<C> monoid;

      public Instance(Monoid<C> monoid) {
         this.monoid = monoid;
      }

      @Override
      public <T, R> App<Const.Mu<C>, R> map(Function<? super T, ? extends R> func, App<Const.Mu<C>, T> ts) {
         return (App<Const.Mu<C>, R>)Const.create(Const.unbox(ts));
      }

      @Override
      public <A> App<Const.Mu<C>, A> point(A a) {
         return (App<Const.Mu<C>, A>)Const.create(this.monoid.point());
      }

      @Override
      public <A, R> Function<App<Const.Mu<C>, A>, App<Const.Mu<C>, R>> lift1(App<Const.Mu<C>, Function<A, R>> function) {
         return a -> (App<Const.Mu<C>, R>)Const.create(this.monoid.add(Const.unbox(function), Const.unbox(a)));
      }

      @Override
      public <A, B, R> BiFunction<App<Const.Mu<C>, A>, App<Const.Mu<C>, B>, App<Const.Mu<C>, R>> lift2(App<Const.Mu<C>, BiFunction<A, B, R>> function) {
         return (a, b) -> (App<Const.Mu<C>, R>)Const.create(this.monoid.add(Const.unbox(function), this.monoid.add(Const.unbox(a), Const.unbox(b))));
      }

      public static final class Mu<C> implements Applicative.Mu {
      }
   }

   public static final class Mu<C> implements K1 {
   }
}