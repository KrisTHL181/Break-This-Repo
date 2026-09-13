package com.mojang.datafixers.optics;

import com.mojang.datafixers.FunctionType;
import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.App2;
import com.mojang.datafixers.kinds.K2;
import com.mojang.datafixers.optics.profunctors.AffineP;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import java.util.function.Function;

interface ForgetE<R, A, B> extends App2<ForgetE.Mu<R>, A, B> {
   static <R, A, B> ForgetE<R, A, B> unbox(App2<ForgetE.Mu<R>, A, B> box) {
      return (ForgetE<R, A, B>)box;
   }

   Either<B, R> run(A var1);

   final class Instance<R> implements AffineP<ForgetE.Mu<R>, ForgetE.Instance.Mu<R>>, App<ForgetE.Instance.Mu<R>, ForgetE.Mu<R>> {
      @Override
      public <A, B, C, D> FunctionType<App2<ForgetE.Mu<R>, A, B>, App2<ForgetE.Mu<R>, C, D>> dimap(Function<C, A> g, Function<B, D> h) {
         return input -> (App2<ForgetE.Mu<R>, C, D>)Optics.forgetE(c -> ForgetE.<R, A, B>unbox(input).run(g.apply((C)c)).mapLeft(h));
      }

      @Override
      public <A, B, C> App2<ForgetE.Mu<R>, Pair<A, C>, Pair<B, C>> first(App2<ForgetE.Mu<R>, A, B> input) {
         return Optics.forgetE(p -> ForgetE.unbox(input).run(p.getFirst()).mapLeft(b -> Pair.of((B)b, (C)p.getSecond())));
      }

      @Override
      public <A, B, C> App2<ForgetE.Mu<R>, Pair<C, A>, Pair<C, B>> second(App2<ForgetE.Mu<R>, A, B> input) {
         return Optics.forgetE(p -> ForgetE.unbox(input).run(p.getSecond()).mapLeft(b -> Pair.of((C)p.getFirst(), (B)b)));
      }

      @Override
      public <A, B, C> App2<ForgetE.Mu<R>, Either<A, C>, Either<B, C>> left(App2<ForgetE.Mu<R>, A, B> input) {
         return Optics.forgetE(e -> e.map(l -> ForgetE.unbox(input).run((A)l).mapLeft(Either::left), r -> Either.left((Either<B, C>)Either.right(r))));
      }

      @Override
      public <A, B, C> App2<ForgetE.Mu<R>, Either<C, A>, Either<C, B>> right(App2<ForgetE.Mu<R>, A, B> input) {
         return Optics.forgetE(e -> e.map(l -> Either.left((Either<C, B>)Either.left((C)l)), r -> ForgetE.unbox(input).run((A)r).mapLeft(Either::right)));
      }

      static final class Mu<R> implements AffineP.Mu {
      }
   }

   final class Mu<R> implements K2 {
   }
}
