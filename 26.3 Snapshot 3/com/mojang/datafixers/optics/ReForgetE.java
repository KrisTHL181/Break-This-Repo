package com.mojang.datafixers.optics;

import com.mojang.datafixers.FunctionType;
import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.App2;
import com.mojang.datafixers.kinds.K2;
import com.mojang.datafixers.optics.profunctors.Cocartesian;
import com.mojang.datafixers.util.Either;
import java.util.function.Function;

interface ReForgetE<R, A, B> extends App2<ReForgetE.Mu<R>, A, B> {
   static <R, A, B> ReForgetE<R, A, B> unbox(App2<ReForgetE.Mu<R>, A, B> box) {
      return (ReForgetE<R, A, B>)box;
   }

   B run(Either<A, R> var1);

   final class Instance<R> implements Cocartesian<ReForgetE.Mu<R>, ReForgetE.Instance.Mu<R>>, App<ReForgetE.Instance.Mu<R>, ReForgetE.Mu<R>> {
      // ===== 修改：移除错误的显式类型参数和强制转换 =====
      @Override
      public <A, B, C, D> FunctionType<App2<ReForgetE.Mu<R>, A, B>, App2<ReForgetE.Mu<R>, C, D>> dimap(Function<C, A> g, Function<B, D> h) {
         return input -> Optics.<R, C, D>reForgetE("dimap", e -> {
            Either<A, R> either = e.mapLeft(g);
            return h.apply(ReForgetE.unbox(input).run(either));
         });
      }

      // ===== 修改：移除多余的强制转换 =====
      @Override
      public <A, B, C> App2<ReForgetE.Mu<R>, Either<A, C>, Either<B, C>> left(App2<ReForgetE.Mu<R>, A, B> input) {
         ReForgetE<R, A, B> reForgetE = ReForgetE.unbox(input);
         return Optics.<R, Either<A, C>, Either<B, C>>reForgetE(
            "left",
            e -> e.map(
               e2 -> e2.map(a -> Either.left(reForgetE.run(Either.left(a))), Either::right),
               r -> Either.left(reForgetE.run(Either.right(r)))
            )
         );
      }

      // ===== 修改：移除多余的强制转换 =====
      @Override
      public <A, B, C> App2<ReForgetE.Mu<R>, Either<C, A>, Either<C, B>> right(App2<ReForgetE.Mu<R>, A, B> input) {
         ReForgetE<R, A, B> reForgetE = ReForgetE.unbox(input);
         return Optics.<R, Either<C, A>, Either<C, B>>reForgetE(
            "right",
            e -> e.map(
               e2 -> e2.map(Either::left, a -> Either.right(reForgetE.run(Either.left(a)))),
               r -> Either.right(reForgetE.run(Either.right(r)))
            )
         );
      }

      static final class Mu<R> implements Cocartesian.Mu {
      }
   }

   final class Mu<R> implements K2 {
   }
}