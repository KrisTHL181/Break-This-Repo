package com.mojang.datafixers.optics;

import com.mojang.datafixers.FunctionType;
import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.App2;
import com.mojang.datafixers.kinds.K2;
import com.mojang.datafixers.optics.profunctors.AffineP;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import java.util.function.BiFunction;
import java.util.function.Function;

public interface ReForgetC<R, A, B> extends App2<ReForgetC.Mu<R>, A, B> {
   static <R, A, B> ReForgetC<R, A, B> unbox(App2<ReForgetC.Mu<R>, A, B> box) {
      return (ReForgetC<R, A, B>)box;
   }

   Either<Function<R, B>, BiFunction<A, R, B>> impl();

   default B run(A a, R r) {
      return this.impl().map(f -> f.apply(r), f -> f.apply(a, r));
   }

   final class Instance<R> implements AffineP<ReForgetC.Mu<R>, ReForgetC.Instance.Mu<R>>, App<ReForgetC.Instance.Mu<R>, ReForgetC.Mu<R>> {
      // ===== 修改：修复 dimap 方法，显式类型转换 =====
      @Override
      public <A, B, C, D> FunctionType<App2<ReForgetC.Mu<R>, A, B>, App2<ReForgetC.Mu<R>, C, D>> dimap(Function<C, A> g, Function<B, D> h) {
         return input -> {
            Either<Function<R, B>, BiFunction<A, R, B>> e = ReForgetC.<R, A, B>unbox(input).impl();
            return (App2<ReForgetC.Mu<R>, C, D>) Optics.reForgetC("dimap",
                e.<Function<R, D>, BiFunction<C, R, D>>map(
                    f -> Either.left(r -> h.apply(f.apply(r))),
                    f -> Either.right((c, r) -> h.apply(f.apply(g.apply(c), r)))
                )
            );
         };
      }

      @Override
      public <A, B, C> App2<ReForgetC.Mu<R>, Pair<A, C>, Pair<B, C>> first(App2<ReForgetC.Mu<R>, A, B> input) {
         return Optics.reForgetC(
            "first",
            ReForgetC.unbox(input)
               .impl()
               .map(
                  f -> Either.right((p, r) -> Pair.of(f.apply(r), p.getSecond())),
                  f -> Either.right((p, r) -> Pair.of(f.apply(p.getFirst(), r), p.getSecond()))
               )
         );
      }

      @Override
      public <A, B, C> App2<ReForgetC.Mu<R>, Pair<C, A>, Pair<C, B>> second(App2<ReForgetC.Mu<R>, A, B> input) {
         return Optics.reForgetC(
            "second",
            ReForgetC.unbox(input)
               .impl()
               .map(
                  f -> Either.right((p, r) -> Pair.of(p.getFirst(), f.apply(r))), f -> Either.right((p, r) -> Pair.of(p.getFirst(), f.apply(p.getSecond(), r)))
               )
         );
      }

      @Override
      public <A, B, C> App2<ReForgetC.Mu<R>, Either<A, C>, Either<B, C>> left(App2<ReForgetC.Mu<R>, A, B> input) {
         return Optics.reForgetC(
            "left",
            ReForgetC.unbox(input)
               .impl()
               .map(f -> Either.left(r -> (Either<B, C>)Either.left(f.apply(r))), f -> Either.right((p, r) -> p.mapLeft(a -> f.apply((A)a, (R)r))))
         );
      }

      @Override
      public <A, B, C> App2<ReForgetC.Mu<R>, Either<C, A>, Either<C, B>> right(App2<ReForgetC.Mu<R>, A, B> input) {
         return Optics.reForgetC(
            "right",
            ReForgetC.unbox(input)
               .impl()
               .map(f -> Either.left(r -> Either.right(f.apply(r))), f -> Either.right((p, r) -> p.mapRight(a -> f.apply((A)a, (R)r))))
         );
      }

      public static final class Mu<R> implements AffineP.Mu {
      }
   }

   final class Mu<R> implements K2 {
   }
}