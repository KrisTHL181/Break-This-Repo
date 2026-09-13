package net.minecraft.world.level.storage.loot.functions;

import java.util.Arrays;
import java.util.function.Function;

public interface FunctionUserBuilder<T extends FunctionUserBuilder<T>> {
   T apply(LootItemFunction.Builder var1);

   default <E> T apply(Iterable<E> p_230985_, Function<E, LootItemFunction.Builder> p_230986_) {
      T t = this.unwrap();

      for (E e : p_230985_) {
         t = t.apply(p_230986_.apply(e));
      }

      return t;
   }

   default <E> T apply(E[] p_230988_, Function<E, LootItemFunction.Builder> p_230989_) {
      return this.apply(Arrays.asList(p_230988_), p_230989_);
   }

   T unwrap();
}
