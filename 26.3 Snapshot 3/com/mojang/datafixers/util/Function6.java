package com.mojang.datafixers.util;

import java.util.function.BiFunction;
import java.util.function.Function;

public interface Function6<T1, T2, T3, T4, T5, T6, R> {
   R apply(T1 var1, T2 var2, T3 var3, T4 var4, T5 var5, T6 var6);

   default Function<T1, Function5<T2, T3, T4, T5, T6, R>> curry() {
      return t1 -> (t2, t3, t4, t5, t6) -> this.apply(t1, t2, t3, t4, t5, t6);
   }

   default BiFunction<T1, T2, Function4<T3, T4, T5, T6, R>> curry2() {
      return (t1, t2) -> (t3, t4, t5, t6) -> this.apply(t1, t2, t3, t4, t5, t6);
   }

   default Function3<T1, T2, T3, Function3<T4, T5, T6, R>> curry3() {
      return (t1, t2, t3) -> (t4, t5, t6) -> this.apply(t1, t2, t3, t4, t5, t6);
   }

   default Function4<T1, T2, T3, T4, BiFunction<T5, T6, R>> curry4() {
      return (t1, t2, t3, t4) -> (t5, t6) -> this.apply(t1, t2, t3, t4, t5, t6);
   }

   default Function5<T1, T2, T3, T4, T5, Function<T6, R>> curry5() {
      return (t1, t2, t3, t4, t5) -> t6 -> this.apply(t1, t2, t3, t4, t5, t6);
   }
}
