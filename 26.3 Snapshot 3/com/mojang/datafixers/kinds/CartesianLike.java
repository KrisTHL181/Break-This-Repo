package com.mojang.datafixers.kinds;

import com.mojang.datafixers.util.Pair;
import java.util.function.Function;

public interface CartesianLike<T extends K1, C, Mu extends CartesianLike.Mu> extends Functor<T, Mu>, Traversable<T, Mu> {
   static <F extends K1, C, Mu extends CartesianLike.Mu> CartesianLike<F, C, Mu> unbox(App<Mu, F> proofBox) {
      return (CartesianLike<F, C, Mu>)proofBox;
   }

   <A> App<Pair.Mu<C>, A> to(App<T, A> var1);

   <A> App<T, A> from(App<Pair.Mu<C>, A> var1);

   // ===== 修改：移除错误的强制转换 =====
   @Override
   default <F extends K1, A, B> App<F, App<T, B>> traverse(Applicative<F, ?> applicative, Function<A, App<F, B>> function, App<T, A> input) {
      return applicative.map(this::from, new Pair.Instance<C>().traverse(applicative, function, this.to(input)));
   }

   interface Mu extends Functor.Mu, Traversable.Mu {
   }
}