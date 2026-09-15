package com.mojang.datafixers.kinds;

import java.util.function.Function;

public interface Functor<F extends K1, Mu extends Functor.Mu> extends Kind1<F, Mu> {
   static <F extends K1, Mu extends Functor.Mu> Functor<F, Mu> unbox(App<Mu, F> proofBox) {
      return (Functor<F, Mu>)proofBox;
   }

   <T, R> App<F, R> map(Function<? super T, ? extends R> var1, App<F, T> var2);

   interface Mu extends Kind1.Mu {
   }
}
