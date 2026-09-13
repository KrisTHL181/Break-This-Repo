package com.mojang.datafixers.kinds;

import java.util.function.Function;

public interface Traversable<T extends K1, Mu extends Traversable.Mu> extends Functor<T, Mu> {
   static <F extends K1, Mu extends Traversable.Mu> Traversable<F, Mu> unbox(App<Mu, F> proofBox) {
      return (Traversable<F, Mu>)proofBox;
   }

   <F extends K1, A, B> App<F, App<T, B>> traverse(Applicative<F, ?> var1, Function<A, App<F, B>> var2, App<T, A> var3);

   default <F extends K1, A> App<F, App<T, A>> flip(Applicative<F, ?> applicative, App<T, App<F, A>> input) {
      return this.traverse(applicative, Function.identity(), input);
   }

   interface Mu extends Functor.Mu {
   }
}
