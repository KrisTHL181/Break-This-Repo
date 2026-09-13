package com.mojang.datafixers.kinds;

import java.util.function.BiFunction;
import java.util.function.Function;

public final class IdF<A> implements App<IdF.Mu, A> {
   protected final A value;

   IdF(A value) {
      this.value = value;
   }

   public A value() {
      return this.value;
   }

   // ===== 修改：强制转换加上泛型参数 =====
   public static <A> A get(App<IdF.Mu, A> box) {
      return ((IdF<A>)box).value;
   }

   public static <A> IdF<A> create(A a) {
      return new IdF<>(a);
   }

   public enum Instance implements Functor<IdF.Mu, IdF.Instance.Mu>, Applicative<IdF.Mu, IdF.Instance.Mu> {
      INSTANCE;

      @Override
      public <T, R> App<IdF.Mu, R> map(Function<? super T, ? extends R> func, App<IdF.Mu, T> ts) {
         IdF<T> idF = (IdF<T>)ts;
         return new IdF<>(func.apply(idF.value));
      }

      @Override
      public <A> App<IdF.Mu, A> point(A a) {
         return IdF.create(a);
      }

      @Override
      public <A, R> Function<App<IdF.Mu, A>, App<IdF.Mu, R>> lift1(App<IdF.Mu, Function<A, R>> function) {
         return a -> IdF.create(IdF.get(function).apply(IdF.get(a)));
      }

      @Override
      public <A, B, R> BiFunction<App<IdF.Mu, A>, App<IdF.Mu, B>, App<IdF.Mu, R>> lift2(App<IdF.Mu, BiFunction<A, B, R>> function) {
         return (a, b) -> IdF.create(IdF.get(function).apply(IdF.get(a), IdF.get(b)));
      }

      public static final class Mu implements Functor.Mu, Applicative.Mu {
      }
   }

   public static final class Mu implements K1 {
   }
}