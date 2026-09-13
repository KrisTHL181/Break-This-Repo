package com.mojang.datafixers.kinds;

import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

public final class OptionalBox<T> implements App<OptionalBox.Mu, T> {
   private final Optional<T> value;

   public static <T> Optional<T> unbox(App<OptionalBox.Mu, T> box) {
      return ((OptionalBox)box).value;
   }

   public static <T> OptionalBox<T> create(Optional<T> value) {
      return new OptionalBox<>(value);
   }

   private OptionalBox(Optional<T> value) {
      this.value = value;
   }

   public enum Instance implements Applicative<OptionalBox.Mu, OptionalBox.Instance.Mu>, Traversable<OptionalBox.Mu, OptionalBox.Instance.Mu> {
      INSTANCE;

      @Override
      public <T, R> App<OptionalBox.Mu, R> map(Function<? super T, ? extends R> func, App<OptionalBox.Mu, T> ts) {
         return OptionalBox.create(OptionalBox.unbox(ts).map(func));
      }

      @Override
      public <A> App<OptionalBox.Mu, A> point(A a) {
         return OptionalBox.create(Optional.of(a));
      }

      @Override
      public <A, R> Function<App<OptionalBox.Mu, A>, App<OptionalBox.Mu, R>> lift1(App<OptionalBox.Mu, Function<A, R>> function) {
         return a -> OptionalBox.create(OptionalBox.unbox(function).flatMap(f -> OptionalBox.unbox(a).map(f)));
      }

      // ===== 修改：移除错误的类型参数和强制转换 =====
      @Override
      public <A, B, R> BiFunction<App<OptionalBox.Mu, A>, App<OptionalBox.Mu, B>, App<OptionalBox.Mu, R>> lift2(
         App<OptionalBox.Mu, BiFunction<A, B, R>> function
      ) {
         return (a, b) -> OptionalBox.create(
            OptionalBox.unbox(function).flatMap(f -> OptionalBox.unbox(a).flatMap(av -> OptionalBox.unbox(b).map(bv -> f.apply(av, bv))))
         );
      }

      @Override
      public <F extends K1, A, B> App<F, App<OptionalBox.Mu, B>> traverse(
         Applicative<F, ?> applicative, Function<A, App<F, B>> function, App<OptionalBox.Mu, A> input
      ) {
         Optional<App<F, B>> traversed = OptionalBox.unbox(input).map(function);
         return traversed.isPresent()
            ? applicative.map(b -> OptionalBox.create(Optional.of(b)), traversed.get())
            : applicative.point(OptionalBox.create(Optional.empty()));
      }

      public static final class Mu implements Applicative.Mu, Traversable.Mu {
      }
   }

   public static final class Mu implements K1 {
   }
}