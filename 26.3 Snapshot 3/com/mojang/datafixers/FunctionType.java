package com.mojang.datafixers;

import com.google.common.reflect.TypeToken;
import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.App2;
import com.mojang.datafixers.kinds.Functor;
import com.mojang.datafixers.kinds.IdF;
import com.mojang.datafixers.kinds.K1;
import com.mojang.datafixers.kinds.K2;
import com.mojang.datafixers.kinds.Representable;
import com.mojang.datafixers.optics.Optics;
import com.mojang.datafixers.optics.Procompose;
import com.mojang.datafixers.optics.Wander;
import com.mojang.datafixers.optics.profunctors.Mapping;
import com.mojang.datafixers.optics.profunctors.MonoidProfunctor;
import com.mojang.datafixers.optics.profunctors.Monoidal;
import com.mojang.datafixers.optics.profunctors.TraversalP;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import java.util.function.Function;
import java.util.function.Supplier;
import org.jspecify.annotations.NonNull;

public interface FunctionType<A, B> extends Function<A, B>, App2<FunctionType.Mu, A, B>, App<FunctionType.ReaderMu<A>, B> {
   static <A, B> FunctionType<A, B> create(Function<? super A, ? extends B> function) {
      return function::apply;
   }

   static <A, B> Function<A, B> unbox(App2<FunctionType.Mu, A, B> box) {
      return (FunctionType)box;
   }

   static <A, B> Function<A, B> unbox(App<FunctionType.ReaderMu<A>, B> box) {
      return (FunctionType)box;
   }

   @NonNull
   @Override
   B apply(@NonNull A var1);

   enum Instance
      implements TraversalP<FunctionType.Mu, FunctionType.Instance.Mu>,
      MonoidProfunctor<FunctionType.Mu, FunctionType.Instance.Mu>,
      Mapping<FunctionType.Mu, FunctionType.Instance.Mu>,
      Monoidal<FunctionType.Mu, FunctionType.Instance.Mu>,
      App<FunctionType.Instance.Mu, FunctionType.Mu> {
      INSTANCE;

      @Override
      public <A, B, C, D> FunctionType<App2<FunctionType.Mu, A, B>, App2<FunctionType.Mu, C, D>> dimap(Function<C, A> g, Function<B, D> h) {
         return f -> FunctionType.create(h.compose(Optics.getFunc(f)).compose(g));
      }

      @Override
      public <A, B, C> App2<FunctionType.Mu, Pair<A, C>, Pair<B, C>> first(App2<FunctionType.Mu, A, B> input) {
         return FunctionType.create(p -> Pair.of(Optics.getFunc(input).apply(p.getFirst()), p.getSecond()));
      }

      @Override
      public <A, B, C> App2<FunctionType.Mu, Pair<C, A>, Pair<C, B>> second(App2<FunctionType.Mu, A, B> input) {
         return FunctionType.create(p -> Pair.of(p.getFirst(), Optics.getFunc(input).apply(p.getSecond())));
      }

      @Override
      public <S, T, A, B> App2<FunctionType.Mu, S, T> wander(Wander<S, T, A, B> wander, App2<FunctionType.Mu, A, B> input) {
         return FunctionType.create(s -> IdF.get(wander.<IdF.Mu>wander(IdF.Instance.INSTANCE, a -> IdF.create(Optics.getFunc(input).apply(a))).apply((S)s)));
      }

      @Override
      public <A, B, C> App2<FunctionType.Mu, Either<A, C>, Either<B, C>> left(App2<FunctionType.Mu, A, B> input) {
         return FunctionType.create(either -> either.mapLeft(Optics.getFunc(input)));
      }

      @Override
      public <A, B, C> App2<FunctionType.Mu, Either<C, A>, Either<C, B>> right(App2<FunctionType.Mu, A, B> input) {
         return FunctionType.create(either -> either.mapRight(Optics.getFunc(input)));
      }

      @Override
      public <A, B, C, D> App2<FunctionType.Mu, Pair<A, C>, Pair<B, D>> par(App2<FunctionType.Mu, A, B> first, Supplier<App2<FunctionType.Mu, C, D>> second) {
         return FunctionType.create(pair -> Pair.of(Optics.getFunc(first).apply(pair.getFirst()), Optics.getFunc(second.get()).apply(pair.getSecond())));
      }

      @Override
      public App2<FunctionType.Mu, Void, Void> empty() {
         return FunctionType.create(Function.identity());
      }

      @Override
      public <A, B> App2<FunctionType.Mu, A, B> zero(App2<FunctionType.Mu, A, B> func) {
         return func;
      }

      @Override
      public <A, B> App2<FunctionType.Mu, A, B> plus(App2<Procompose.Mu<FunctionType.Mu, FunctionType.Mu>, A, B> input) {
         Procompose<FunctionType.Mu, FunctionType.Mu, A, B, ?> cmp = Procompose.unbox(input);
         return this.cap(cmp);
      }

      private <A, B, C> App2<FunctionType.Mu, A, B> cap(Procompose<FunctionType.Mu, FunctionType.Mu, A, B, C> cmp) {
         return FunctionType.create(Optics.getFunc(cmp.second()).compose(Optics.getFunc(cmp.first().get())));
      }

      @Override
      public <A, B, F extends K1> App2<FunctionType.Mu, App<F, A>, App<F, B>> mapping(Functor<F, ?> functor, App2<FunctionType.Mu, A, B> input) {
         return FunctionType.create(fa -> functor.map(Optics.getFunc(input), (App<F, A>)fa));
      }

      public static final class Mu implements TraversalP.Mu, MonoidProfunctor.Mu, Mapping.Mu, Monoidal.Mu {
         public static final TypeToken<FunctionType.Instance.Mu> TYPE_TOKEN = new TypeToken<FunctionType.Instance.Mu>() {};
      }
   }

   final class Mu implements K2 {
   }

   final class ReaderInstance<R> implements Representable<FunctionType.ReaderMu<R>, R, FunctionType.ReaderInstance.Mu<R>> {
      @Override
      public <T, R2> App<FunctionType.ReaderMu<R>, R2> map(Function<? super T, ? extends R2> func, App<FunctionType.ReaderMu<R>, T> ts) {
         return FunctionType.create(func.compose(FunctionType.unbox(ts)));
      }

      @Override
      public <B> App<FunctionType.ReaderMu<R>, B> to(App<FunctionType.ReaderMu<R>, B> input) {
         return input;
      }

      @Override
      public <B> App<FunctionType.ReaderMu<R>, B> from(App<FunctionType.ReaderMu<R>, B> input) {
         return input;
      }

      public static final class Mu<A> implements Representable.Mu {
      }
   }

   final class ReaderMu<A> implements K1 {
   }
}
