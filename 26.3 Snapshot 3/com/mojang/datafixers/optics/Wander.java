package com.mojang.datafixers.optics;

import com.mojang.datafixers.FunctionType;
import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.datafixers.kinds.K1;

public interface Wander<S, T, A, B> {
   <F extends K1> FunctionType<S, App<F, T>> wander(Applicative<F, ?> var1, FunctionType<A, App<F, B>> var2);
}
