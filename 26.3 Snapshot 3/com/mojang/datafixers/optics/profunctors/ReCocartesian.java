package com.mojang.datafixers.optics.profunctors;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.App2;
import com.mojang.datafixers.kinds.K2;
import com.mojang.datafixers.util.Either;

public interface ReCocartesian<P extends K2, Mu extends ReCocartesian.Mu> extends Profunctor<P, Mu> {
   static <P extends K2, Proof extends ReCocartesian.Mu> ReCocartesian<P, Proof> unbox(App<Proof, P> proofBox) {
      return (ReCocartesian<P, Proof>)proofBox;
   }

   <A, B, C> App2<P, A, B> unleft(App2<P, Either<A, C>, Either<B, C>> var1);

   <A, B, C> App2<P, A, B> unright(App2<P, Either<C, A>, Either<C, B>> var1);

   interface Mu extends Profunctor.Mu {
   }
}
