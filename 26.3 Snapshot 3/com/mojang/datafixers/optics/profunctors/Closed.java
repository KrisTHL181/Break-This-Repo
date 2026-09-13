package com.mojang.datafixers.optics.profunctors;

import com.google.common.reflect.TypeToken;
import com.mojang.datafixers.FunctionType;
import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.App2;
import com.mojang.datafixers.kinds.K2;

public interface Closed<P extends K2, Mu extends Closed.Mu> extends Profunctor<P, Mu> {
   static <P extends K2, Proof extends Closed.Mu> Closed<P, Proof> unbox(App<Proof, P> proofBox) {
      return (Closed<P, Proof>)proofBox;
   }

   <A, B, X> App2<P, FunctionType<X, A>, FunctionType<X, B>> closed(App2<P, A, B> var1);

   interface Mu extends Profunctor.Mu {
      TypeToken<Closed.Mu> TYPE_TOKEN = new TypeToken<Closed.Mu>() {};
   }
}
