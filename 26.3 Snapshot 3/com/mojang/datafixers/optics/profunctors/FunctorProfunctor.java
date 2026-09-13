package com.mojang.datafixers.optics.profunctors;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.App2;
import com.mojang.datafixers.kinds.K1;
import com.mojang.datafixers.kinds.K2;
import com.mojang.datafixers.kinds.Kind2;

public interface FunctorProfunctor<T extends K1, P extends K2, Mu extends FunctorProfunctor.Mu<T>> extends Kind2<P, Mu> {
   static <T extends K1, P extends K2, Mu extends FunctorProfunctor.Mu<T>> FunctorProfunctor<T, P, Mu> unbox(App<Mu, P> proofBox) {
      return (FunctorProfunctor<T, P, Mu>)proofBox;
   }

   <A, B, F extends K1> App2<P, App<F, A>, App<F, B>> distribute(App<? extends T, F> var1, App2<P, A, B> var2);

   interface Mu<T extends K1> extends Kind2.Mu {
   }
}
