package com.mojang.datafixers.util;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.datafixers.kinds.CocartesianLike;
import com.mojang.datafixers.kinds.K1;
import com.mojang.datafixers.kinds.Traversable;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

public abstract class Either<L, R> implements App<Either.Mu<R>, L> {
   public static <L, R> Either<L, R> unbox(App<Either.Mu<R>, L> box) {
      return (Either<L, R>)box;
   }

   private Either() {
   }

   public abstract <C, D> Either<C, D> mapBoth(Function<? super L, ? extends C> var1, Function<? super R, ? extends D> var2);

   public abstract <T> T map(Function<? super L, ? extends T> var1, Function<? super R, ? extends T> var2);

   public abstract Either<L, R> ifLeft(Consumer<? super L> var1);

   public abstract Either<L, R> ifRight(Consumer<? super R> var1);

   public abstract Optional<L> left();

   public abstract Optional<R> right();

   public <T> Either<T, R> mapLeft(Function<? super L, ? extends T> l) {
      return this.map(t -> (Either<T, R>)left(l.apply(t)), Either::right);
   }

   public <T> Either<L, T> mapRight(Function<? super R, ? extends T> l) {
      return this.map(Either::left, t -> (Either<L, T>)right(l.apply(t)));
   }

   public static <L, R> Either<L, R> left(L value) {
      return new Either.Left<>(value);
   }

   public static <L, R> Either<L, R> right(R value) {
      return new Either.Right<>(value);
   }

   public L orThrow() {
      return this.map(l -> (L)l, r -> {
         if (r instanceof Throwable) {
            throw new RuntimeException((Throwable)r);
         } else {
            throw new RuntimeException(r.toString());
         }
      });
   }

   public Either<R, L> swap() {
      return this.map(Either::right, Either::left);
   }

   public <L2> Either<L2, R> flatMap(Function<L, Either<L2, R>> function) {
      return this.map(function, Either::right);
   }

   public static <U> U unwrap(Either<? extends U, ? extends U> either) {
      return either.map(Function.identity(), Function.identity());
   }

   public static final class Instance<R2>
      implements Applicative<Either.Mu<R2>, Either.Instance.Mu<R2>>,
      Traversable<Either.Mu<R2>, Either.Instance.Mu<R2>>,
      CocartesianLike<Either.Mu<R2>, R2, Either.Instance.Mu<R2>> {
      @Override
      public <T, R> App<Either.Mu<R2>, R> map(Function<? super T, ? extends R> func, App<Either.Mu<R2>, T> ts) {
         return (App<Either.Mu<R2>, R>)Either.unbox(ts).mapLeft(func);
      }

      @Override
      public <A> App<Either.Mu<R2>, A> point(A a) {
         return (App<Either.Mu<R2>, A>)Either.left(a);
      }

      @Override
      public <A, R> Function<App<Either.Mu<R2>, A>, App<Either.Mu<R2>, R>> lift1(App<Either.Mu<R2>, Function<A, R>> function) {
         return a -> (App<Either.Mu<R2>, R>)Either.unbox(function).flatMap(f -> (Either<R, R2>)Either.unbox(a).mapLeft(f));
      }

      // ===== 修改：去掉泛型指定，直接使用 Either.unbox =====
      @Override
      public <A, B, R> BiFunction<App<Either.Mu<R2>, A>, App<Either.Mu<R2>, B>, App<Either.Mu<R2>, R>> lift2(App<Either.Mu<R2>, BiFunction<A, B, R>> function) {
         return (a, b) -> (App<Either.Mu<R2>, R>)Either.unbox(function)
            .flatMap(f -> Either.unbox(a).flatMap(av -> Either.unbox(b).mapLeft(bv -> f.apply(av, bv))));
      }

      @Override
      public <F extends K1, A, B> App<F, App<Either.Mu<R2>, B>> traverse(
         Applicative<F, ?> applicative, Function<A, App<F, B>> function, App<Either.Mu<R2>, A> input
      ) {
         return Either.unbox(input).map(l -> {
            App<F, B> b = function.apply((A)l);
            return applicative.ap(Either::left, (App<F, L>)b);
         }, r -> applicative.point((App<Either.Mu<R2>, B>)Either.right(r)));
      }

      @Override
      public <A> App<Either.Mu<R2>, A> to(App<Either.Mu<R2>, A> input) {
         return input;
      }

      @Override
      public <A> App<Either.Mu<R2>, A> from(App<Either.Mu<R2>, A> input) {
         return input;
      }

      public static final class Mu<R2> implements Applicative.Mu, Traversable.Mu, CocartesianLike.Mu {
      }
   }

   private static final class Left<L, R> extends Either<L, R> {
      private final L value;

      public Left(L value) {
         this.value = value;
      }

      @Override
      public <C, D> Either<C, D> mapBoth(Function<? super L, ? extends C> f1, Function<? super R, ? extends D> f2) {
         return (Either<C, D>)(new Either.Left<>(f1.apply(this.value)));
      }

      @Override
      public <T> T map(Function<? super L, ? extends T> l, Function<? super R, ? extends T> r) {
         return (T)l.apply(this.value);
      }

      @Override
      public Either<L, R> ifLeft(Consumer<? super L> consumer) {
         consumer.accept(this.value);
         return this;
      }

      @Override
      public Either<L, R> ifRight(Consumer<? super R> consumer) {
         return this;
      }

      @Override
      public Optional<L> left() {
         return Optional.of(this.value);
      }

      @Override
      public Optional<R> right() {
         return Optional.empty();
      }

      @Override
      public String toString() {
         return "Left[" + this.value + "]";
      }

      @Override
      public boolean equals(Object o) {
         if (this == o) {
            return true;
         } else if (o != null && this.getClass() == o.getClass()) {
            Either.Left<?, ?> left = (Either.Left<?, ?>)o;
            return Objects.equals(this.value, left.value);
         } else {
            return false;
         }
      }

      @Override
      public int hashCode() {
         return this.value.hashCode();
      }
   }

   public static final class Mu<R> implements K1 {
   }

   private static final class Right<L, R> extends Either<L, R> {
      private final R value;

      public Right(R value) {
         this.value = value;
      }

      @Override
      public <C, D> Either<C, D> mapBoth(Function<? super L, ? extends C> f1, Function<? super R, ? extends D> f2) {
         return (Either<C, D>)(new Either.Right<>(f2.apply(this.value)));
      }

      @Override
      public <T> T map(Function<? super L, ? extends T> l, Function<? super R, ? extends T> r) {
         return (T)r.apply(this.value);
      }

      @Override
      public Either<L, R> ifLeft(Consumer<? super L> consumer) {
         return this;
      }

      @Override
      public Either<L, R> ifRight(Consumer<? super R> consumer) {
         consumer.accept(this.value);
         return this;
      }

      @Override
      public Optional<L> left() {
         return Optional.empty();
      }

      @Override
      public Optional<R> right() {
         return Optional.of(this.value);
      }

      @Override
      public String toString() {
         return "Right[" + this.value + "]";
      }

      @Override
      public boolean equals(Object o) {
         if (this == o) {
            return true;
         } else if (o != null && this.getClass() == o.getClass()) {
            Either.Right<?, ?> right = (Either.Right<?, ?>)o;
            return Objects.equals(this.value, right.value);
         } else {
            return false;
         }
      }

      @Override
      public int hashCode() {
         return this.value.hashCode();
      }
   }
}