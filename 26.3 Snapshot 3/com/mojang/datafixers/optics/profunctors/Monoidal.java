package com.mojang.datafixers.optics.profunctors;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.App2;
import com.mojang.datafixers.kinds.K2;
import com.mojang.datafixers.util.Pair;
import java.util.function.Supplier;

public interface Monoidal<P extends K2, Mu extends Monoidal.Mu> extends Profunctor<P, Mu> {
   static <P extends K2, Proof extends Monoidal.Mu> Monoidal<P, Proof> unbox(App<Proof, P> proofBox) {
      return (Monoidal<P, Proof>)proofBox;
   }

   <A, B, C, D> App2<P, Pair<A, C>, Pair<B, D>> par(App2<P, A, B> var1, Supplier<App2<P, C, D>> var2);

   App2<P, Void, Void> empty();

   interface Mu extends Profunctor.Mu {
   }
}
