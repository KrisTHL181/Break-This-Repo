package com.mojang.datafixers.optics;

import com.mojang.datafixers.FunctionType;
import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.App2;
import com.mojang.datafixers.kinds.K2;
import com.mojang.datafixers.optics.profunctors.Cartesian;
import com.mojang.datafixers.optics.profunctors.ReCocartesian;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import java.util.function.Function;

public interface Forget<R, A, B> extends App2<Forget.Mu<R>, A, B> {
   static <R, A, B> Forget<R, A, B> unbox(App2<Forget.Mu<R>, A, B> box) {
      return (Forget<R, A, B>)box;
   }

   R run(A var1);

   final class Instance<R>
      implements Cartesian<Forget.Mu<R>, Forget.Instance.Mu<R>>,
      ReCocartesian<Forget.Mu<R>, Forget.Instance.Mu<R>>,
      App<Forget.Instance.Mu<R>, Forget.Mu<R>> {
      @Override
      public <A, B, C, D> FunctionType<App2<Forget.Mu<R>, A, B>, App2<Forget.Mu<R>, C, D>> dimap(Function<C, A> g, Function<B, D> h) {
         return input -> (App2<Forget.Mu<R>, C, D>)Optics.forget(c -> Forget.<R, A, B>unbox(input).run(g.apply((C)c)));
      }

      @Override
      public <A, B, C> App2<Forget.Mu<R>, Pair<A, C>, Pair<B, C>> first(App2<Forget.Mu<R>, A, B> input) {
         return Optics.forget(p -> Forget.unbox(input).run(p.getFirst()));
      }

      @Override
      public <A, B, C> App2<Forget.Mu<R>, Pair<C, A>, Pair<C, B>> second(App2<Forget.Mu<R>, A, B> input) {
         return Optics.forget(p -> Forget.unbox(input).run(p.getSecond()));
      }

      @Override
      public <A, B, C> App2<Forget.Mu<R>, A, B> unleft(App2<Forget.Mu<R>, Either<A, C>, Either<B, C>> input) {
         return Optics.forget(a -> Forget.unbox(input).run((Either<A, C>)Either.left(a)));
      }

      @Override
      public <A, B, C> App2<Forget.Mu<R>, A, B> unright(App2<Forget.Mu<R>, Either<C, A>, Either<C, B>> input) {
         return Optics.forget(a -> Forget.unbox(input).run(Either.right(a)));
      }

      public static final class Mu<R> implements Cartesian.Mu, ReCocartesian.Mu {
      }
   }

   final class Mu<R> implements K2 {
   }
}
