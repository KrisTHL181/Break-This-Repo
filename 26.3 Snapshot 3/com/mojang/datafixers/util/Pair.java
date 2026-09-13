package com.mojang.datafixers.util;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.datafixers.kinds.CartesianLike;
import com.mojang.datafixers.kinds.K1;
import com.mojang.datafixers.kinds.Traversable;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public class Pair<F, S> implements App<Pair.Mu<S>, F> {
   private final F first;
   private final S second;

   public static <F, S> Pair<F, S> unbox(App<Pair.Mu<S>, F> box) {
      return (Pair<F, S>)box;
   }

   public Pair(F first, S second) {
      this.first = first;
      this.second = second;
   }

   public F getFirst() {
      return this.first;
   }

   public S getSecond() {
      return this.second;
   }

   public Pair<S, F> swap() {
      return of(this.second, this.first);
   }

   @Override
   public String toString() {
      return "(" + this.first + ", " + this.second + ")";
   }

   @Override
   public boolean equals(Object obj) {
      return !(obj instanceof Pair<?, ?> other) ? false : Objects.equals(this.first, other.first) && Objects.equals(this.second, other.second);
   }

   @Override
   public int hashCode() {
      return com.google.common.base.Objects.hashCode(new Object[]{this.first, this.second});
   }

   public <F2> Pair<F2, S> mapFirst(Function<? super F, ? extends F2> function) {
      return (Pair<F2, S>)of(function.apply(this.first), this.second);
   }

   public <S2> Pair<F, S2> mapSecond(Function<? super S, ? extends S2> function) {
      return (Pair<F, S2>)of(this.first, function.apply(this.second));
   }

   public static <F, S> Pair<F, S> of(F first, S second) {
      return new Pair<>(first, second);
   }

   public static <F, S> Collector<Pair<F, S>, ?, Map<F, S>> toMap() {
      return Collectors.toMap(Pair::getFirst, Pair::getSecond);
   }

   public static final class Instance<S2> implements Traversable<Pair.Mu<S2>, Pair.Instance.Mu<S2>>, CartesianLike<Pair.Mu<S2>, S2, Pair.Instance.Mu<S2>> {
      @Override
      public <T, R> App<Pair.Mu<S2>, R> map(Function<? super T, ? extends R> func, App<Pair.Mu<S2>, T> ts) {
         return (App<Pair.Mu<S2>, R>)Pair.unbox(ts).mapFirst(func);
      }

      @Override
      public <F extends K1, A, B> App<F, App<Pair.Mu<S2>, B>> traverse(
         Applicative<F, ?> applicative, Function<A, App<F, B>> function, App<Pair.Mu<S2>, A> input
      ) {
         Pair<A, S2> pair = Pair.unbox(input);
         return applicative.ap(b -> (App<Pair.Mu<S2>, B>)Pair.of(b, pair.second), function.apply(pair.first));
      }

      @Override
      public <A> App<Pair.Mu<S2>, A> to(App<Pair.Mu<S2>, A> input) {
         return input;
      }

      @Override
      public <A> App<Pair.Mu<S2>, A> from(App<Pair.Mu<S2>, A> input) {
         return input;
      }

      public static final class Mu<S2> implements Traversable.Mu, CartesianLike.Mu {
      }
   }

   public static final class Mu<S> implements K1 {
   }
}
