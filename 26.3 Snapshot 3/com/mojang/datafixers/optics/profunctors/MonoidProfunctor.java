package com.mojang.datafixers.optics.profunctors;

import com.mojang.datafixers.FunctionType;
import com.mojang.datafixers.kinds.App2;
import com.mojang.datafixers.kinds.K2;
import com.mojang.datafixers.optics.Procompose;
import java.util.function.Supplier;

public interface MonoidProfunctor<P extends K2, Mu extends MonoidProfunctor.Mu> extends Profunctor<P, Mu> {
   <A, B> App2<P, A, B> zero(App2<FunctionType.Mu, A, B> var1);

   <A, B> App2<P, A, B> plus(App2<Procompose.Mu<P, P>, A, B> var1);

   default <A, B, C> App2<P, A, C> compose(App2<P, B, C> first, Supplier<App2<P, A, B>> second) {
      return this.plus(new Procompose<>(second, first));
   }

   interface Mu extends Profunctor.Mu {
   }
}
