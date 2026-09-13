package com.mojang.datafixers.optics;

import com.mojang.datafixers.FunctionType;
import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.App2;
import com.mojang.datafixers.kinds.K2;
import com.mojang.datafixers.optics.profunctors.Cocartesian;
import com.mojang.datafixers.util.Either;
import java.util.function.Function;

public interface Prism<S, T, A, B> extends App2<Prism.Mu<A, B>, S, T>, Optic<Cocartesian.Mu, S, T, A, B> {
   static <S, T, A, B> Prism<S, T, A, B> unbox(App2<Prism.Mu<A, B>, S, T> box) {
      return (Prism<S, T, A, B>)box;
   }

   Either<T, A> match(S var1);

   T build(B var1);

   default <P extends K2> FunctionType<App2<P, A, B>, App2<P, S, T>> eval(App<? extends Cocartesian.Mu, P> proof) {
      Cocartesian<P, ? extends Cocartesian.Mu> cocartesian = Cocartesian.unbox(proof);
      return input -> cocartesian.dimap(cocartesian.right(input), this::match, a -> a.map(Function.identity(), this::build));
   }

   final class Instance<A2, B2> implements Cocartesian<Prism.Mu<A2, B2>, Cocartesian.Mu> {
      // ===== 修改：修复 dimap 方法，移除错误的类型变量引用 =====
      @Override
      public <A, B, C, D> FunctionType<App2<Prism.Mu<A2, B2>, A, B>, App2<Prism.Mu<A2, B2>, C, D>> dimap(Function<C, A> g, Function<B, D> h) {
         return prismBox -> Optics.<C, D, A2, B2>prism(
            c -> Prism.unbox(prismBox).match(g.apply(c)).mapLeft(h),
            b -> h.apply(Prism.unbox(prismBox).build(b))
         );
      }

      // ===== 修改：修复 left 方法 =====
      @Override
      public <A, B, C> App2<Prism.Mu<A2, B2>, Either<A, C>, Either<B, C>> left(App2<Prism.Mu<A2, B2>, A, B> input) {
         Prism<A, B, A2, B2> prism = Prism.unbox(input);
         return Optics.<Either<A, C>, Either<B, C>, A2, B2>prism(
            either -> either.map(a -> prism.match(a).mapLeft(Either::left), c -> Either.left(Either.right(c))),
            b -> Either.left(prism.build(b))
         );
      }

      // ===== 修改：修复 right 方法 =====
      @Override
      public <A, B, C> App2<Prism.Mu<A2, B2>, Either<C, A>, Either<C, B>> right(App2<Prism.Mu<A2, B2>, A, B> input) {
         Prism<A, B, A2, B2> prism = Prism.unbox(input);
         return Optics.<Either<C, A>, Either<C, B>, A2, B2>prism(
            either -> either.map(c -> Either.left(Either.left(c)), a -> prism.match(a).mapLeft(Either::right)),
            b -> Either.right(prism.build(b))
         );
      }
   }

   final class Mu<A, B> implements K2 {
   }
}