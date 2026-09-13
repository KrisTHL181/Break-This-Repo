package com.mojang.datafixers.optics;

import com.mojang.datafixers.FunctionType;
import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.App2;
import com.mojang.datafixers.kinds.K2;
import com.mojang.datafixers.optics.profunctors.AffineP;
import com.mojang.datafixers.optics.profunctors.Cartesian;
import com.mojang.datafixers.optics.profunctors.Cocartesian;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import java.util.function.Function;

public interface Affine<S, T, A, B> extends App2<Affine.Mu<A, B>, S, T>, Optic<AffineP.Mu, S, T, A, B> {
   static <S, T, A, B> Affine<S, T, A, B> unbox(App2<Affine.Mu<A, B>, S, T> box) {
      return (Affine<S, T, A, B>)box;
   }

   Either<T, A> preview(S var1);

   T set(B var1, S var2);

   default <P extends K2> FunctionType<App2<P, A, B>, App2<P, S, T>> eval(App<? extends AffineP.Mu, P> proof) {
      Cartesian<P, ? extends AffineP.Mu> cartesian = Cartesian.unbox(proof);
      Cocartesian<P, ? extends AffineP.Mu> cocartesian = Cocartesian.unbox(proof);
      return input -> cartesian.dimap(
         cocartesian.left(cartesian.rmap(cartesian.first(input), p -> (B)this.set((B)p.getFirst(), (S)p.getSecond()))),
         s -> this.preview(s).map(Either::right, a -> Either.left(Pair.of(a, s))),
         Either::unwrap
      );
   }

   final class Instance<A2, B2> implements AffineP<Affine.Mu<A2, B2>, AffineP.Mu> {
      // ===== 修改：修复 dimap 方法，移除错误的类型变量引用 =====
      @Override
      public <A, B, C, D> FunctionType<App2<Affine.Mu<A2, B2>, A, B>, App2<Affine.Mu<A2, B2>, C, D>> dimap(Function<C, A> g, Function<B, D> h) {
         return affineBox -> Optics.<C, D, A2, B2>affine(
            c -> Affine.unbox(affineBox).preview(g.apply(c)).mapLeft(h),
            (b2, c) -> h.apply(Affine.unbox(affineBox).set(b2, g.apply(c)))
         );
      }

      // ===== 修改：修复 first 方法 =====
      @Override
      public <A, B, C> App2<Affine.Mu<A2, B2>, Pair<A, C>, Pair<B, C>> first(App2<Affine.Mu<A2, B2>, A, B> input) {
         Affine<A, B, A2, B2> affine = Affine.unbox(input);
         return Optics.<Pair<A, C>, Pair<B, C>, A2, B2>affine(
            pair -> affine.preview(pair.getFirst()).mapBoth(b -> Pair.of(b, pair.getSecond()), Function.identity()),
            (b2, pair) -> Pair.of(affine.set(b2, pair.getFirst()), pair.getSecond())
         );
      }

      // ===== 修改：修复 second 方法 =====
      @Override
      public <A, B, C> App2<Affine.Mu<A2, B2>, Pair<C, A>, Pair<C, B>> second(App2<Affine.Mu<A2, B2>, A, B> input) {
         Affine<A, B, A2, B2> affine = Affine.unbox(input);
         return Optics.<Pair<C, A>, Pair<C, B>, A2, B2>affine(
            pair -> affine.preview(pair.getSecond()).mapBoth(b -> Pair.of(pair.getFirst(), b), Function.identity()),
            (b2, pair) -> Pair.of(pair.getFirst(), affine.set(b2, pair.getSecond()))
         );
      }

      @Override
      public <A, B, C> App2<Affine.Mu<A2, B2>, Either<A, C>, Either<B, C>> left(App2<Affine.Mu<A2, B2>, A, B> input) {
         Affine<A, B, A2, B2> affine = Affine.unbox(input);
         return Optics.affine(
            either -> either.map(a -> (Either<Either<B, C>, A>)affine.preview((A)a).mapLeft(Either::left), c -> Either.left(Either.right((C)c))),
            (b, either) -> either.map(l -> Either.left(affine.set((B2)b, (A)l)), Either::right)
         );
      }

      @Override
      public <A, B, C> App2<Affine.Mu<A2, B2>, Either<C, A>, Either<C, B>> right(App2<Affine.Mu<A2, B2>, A, B> input) {
         Affine<A, B, A2, B2> affine = Affine.unbox(input);
         return Optics.affine(
            either -> either.map(c -> Either.left(Either.left((C)c)), a -> (Either<Either<C, B>, A>)affine.preview((A)a).mapLeft(Either::right)),
            (b, either) -> either.map(Either::left, r -> Either.right(affine.set((B2)b, (A)r)))
         );
      }
   }

   final class Mu<A, B> implements K2 {
   }
}