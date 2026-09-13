package com.mojang.datafixers.kinds;

import com.mojang.datafixers.FunctionType;

public interface Representable<T extends K1, C, Mu extends Representable.Mu> extends Functor<T, Mu> {
   static <F extends K1, C, Mu extends Representable.Mu> Representable<F, C, Mu> unbox(App<Mu, F> proofBox) {
      return (Representable<F, C, Mu>)proofBox;
   }

   <A> App<FunctionType.ReaderMu<C>, A> to(App<T, A> var1);

   <A> App<T, A> from(App<FunctionType.ReaderMu<C>, A> var1);

   interface Mu extends Functor.Mu {
   }
}
