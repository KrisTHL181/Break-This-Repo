package com.mojang.datafixers.util;

import java.util.function.BiFunction;
import java.util.function.Function;

public interface Function8<T1, T2, T3, T4, T5, T6, T7, T8, R> {
   R apply(T1 var1, T2 var2, T3 var3, T4 var4, T5 var5, T6 var6, T7 var7, T8 var8);

   default Function<T1, Function7<T2, T3, T4, T5, T6, T7, T8, R>> curry() {
      return t1 -> (t2, t3, t4, t5, t6, t7, t8) -> this.apply(t1, t2, t3, t4, t5, t6, t7, t8);
   }

   default BiFunction<T1, T2, Function6<T3, T4, T5, T6, T7, T8, R>> curry2() {
      return (t1, t2) -> (t3, t4, t5, t6, t7, t8) -> this.apply(t1, t2, t3, t4, t5, t6, t7, t8);
   }

   default Function3<T1, T2, T3, Function5<T4, T5, T6, T7, T8, R>> curry3() {
      return (t1, t2, t3) -> (t4, t5, t6, t7, t8) -> this.apply(t1, t2, t3, t4, t5, t6, t7, t8);
   }

   default Function4<T1, T2, T3, T4, Function4<T5, T6, T7, T8, R>> curry4() {
      return (t1, t2, t3, t4) -> (t5, t6, t7, t8) -> this.apply(t1, t2, t3, t4, t5, t6, t7, t8);
   }

   default Function5<T1, T2, T3, T4, T5, Function3<T6, T7, T8, R>> curry5() {
      return (t1, t2, t3, t4, t5) -> (t6, t7, t8) -> this.apply(t1, t2, t3, t4, t5, t6, t7, t8);
   }

   default Function6<T1, T2, T3, T4, T5, T6, BiFunction<T7, T8, R>> curry6() {
      return (t1, t2, t3, t4, t5, t6) -> (t7, t8) -> this.apply(t1, t2, t3, t4, t5, t6, t7, t8);
   }

   default Function7<T1, T2, T3, T4, T5, T6, T7, Function<T8, R>> curry7() {
      return (t1, t2, t3, t4, t5, t6, t7) -> t8 -> this.apply(t1, t2, t3, t4, t5, t6, t7, t8);
   }
}
